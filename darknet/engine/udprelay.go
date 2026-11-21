package engine

import (
	"encoding/binary"
	"errors"
	"net"
	"net/netip"
	"os"
	"sync"
	"sync/atomic"
	"time"

	"golang.org/x/sys/unix"
)

// udpRelay carries UDP (QUIC/HTTP3, games, calls, DNS) without the userspace
// TCP/IP stack: each client flow gets one connected kernel UDP socket. A
// datagram read from the TUN is sent as-is through its flow's socket; a reply is
// wrapped in fresh IP and UDP headers and written back into the TUN. That is
// one copy and one system call per datagram each way.
//
// Fragmented IPv4 datagrams and IPv6 packets with extension headers go to the
// userspace stack, which reassembles them.
type udpRelay struct {
	fd      int
	dialer  *outbound
	enabled atomic.Bool

	mu    sync.Mutex
	flows map[udpKey]*udpFlow

	bytesUp, bytesDown atomic.Int64
	flowsOpened        atomic.Int64
}

type udpKey struct {
	src, dst netip.AddrPort
}

type udpFlow struct {
	conn net.Conn
	last atomic.Int64 // unix nanoseconds of the last datagram either way
}

const (
	udpMaxFlows = 8192
	udpIdle     = 2 * time.Minute
	udpIdleDNS  = 20 * time.Second
)

func newUDPRelay(fd int, dialer *outbound) *udpRelay {
	r := &udpRelay{fd: fd, dialer: dialer, flows: map[udpKey]*udpFlow{}}
	r.enabled.Store(true)
	return r
}

// parseUDP returns a datagram's endpoints and payload, or ok=false.
func parseUDP(pkt []byte) (key udpKey, payload []byte, ok bool) {
	if len(pkt) < 1 {
		return key, nil, false
	}
	var off int
	var src, dst netip.Addr
	switch pkt[0] >> 4 {
	case 4:
		if len(pkt) < 28 || pkt[9] != unix.IPPROTO_UDP {
			return key, nil, false
		}
		off = int(pkt[0]&0x0f) * 4
		total := int(binary.BigEndian.Uint16(pkt[2:4]))
		if off < 20 || total > len(pkt) || total < off+8 || binary.BigEndian.Uint16(pkt[6:8])&0x3fff != 0 {
			return key, nil, false
		}
		pkt = pkt[:total]
		src = netip.AddrFrom4([4]byte(pkt[12:16]))
		dst = netip.AddrFrom4([4]byte(pkt[16:20]))
	case 6:
		if len(pkt) < 48 || pkt[6] != unix.IPPROTO_UDP {
			return key, nil, false
		}
		off = 40
		total := 40 + int(binary.BigEndian.Uint16(pkt[4:6]))
		if total > len(pkt) || total < 48 {
			return key, nil, false
		}
		pkt = pkt[:total]
		src = netip.AddrFrom16([16]byte(pkt[8:24]))
		dst = netip.AddrFrom16([16]byte(pkt[24:40]))
	default:
		return key, nil, false
	}
	udp := pkt[off:]
	length := int(binary.BigEndian.Uint16(udp[4:6]))
	if length < 8 || length > len(udp) {
		return key, nil, false
	}
	key = udpKey{
		src: netip.AddrPortFrom(src, binary.BigEndian.Uint16(udp[0:2])),
		dst: netip.AddrPortFrom(dst, binary.BigEndian.Uint16(udp[2:4])),
	}
	if !dst.IsGlobalUnicast() && !dst.IsPrivate() {
		return key, nil, false // multicast, broadcast, link-local: not for the internet
	}
	return key, udp[8:length], true
}

// handle takes plain UDP; false sends the packet to the userspace path.
func (r *udpRelay) handle(pkt []byte) bool {
	if !r.enabled.Load() {
		return false
	}
	key, payload, ok := parseUDP(pkt)
	if !ok {
		return false
	}
	r.mu.Lock()
	f := r.flows[key]
	if f == nil {
		if len(r.flows) >= udpMaxFlows {
			r.mu.Unlock()
			return false
		}
		conn, err := r.dialer.Dial("udp", key.dst.String())
		if err != nil {
			r.mu.Unlock()
			return true // drop, as a lost datagram: UDP senders retry
		}
		f = &udpFlow{conn: conn}
		r.flows[key] = f
		r.flowsOpened.Add(1)
		go r.pump(key, f)
	}
	r.mu.Unlock()
	f.last.Store(time.Now().UnixNano())
	if n, err := f.conn.Write(payload); err == nil {
		r.bytesUp.Add(int64(n))
	}
	return true
}

// pump returns one flow's replies to the client until it goes idle.
func (r *udpRelay) pump(key udpKey, f *udpFlow) {
	defer func() {
		r.mu.Lock()
		if r.flows[key] == f {
			delete(r.flows, key)
		}
		r.mu.Unlock()
		f.conn.Close()
	}()
	idle := udpIdle
	if key.dst.Port() == 53 {
		idle = udpIdleDNS
	}
	v6 := key.src.Addr().Is6()
	room := udpHeaderRoom(v6)
	buf := make([]byte, room+65535)
	for r.enabled.Load() {
		f.conn.SetReadDeadline(time.Now().Add(idle))
		n, err := f.conn.Read(buf[room:])
		if err != nil {
			if errors.Is(err, os.ErrDeadlineExceeded) && time.Since(time.Unix(0, f.last.Load())) < idle {
				continue // the client still sends: keep the flow
			}
			return
		}
		f.last.Store(time.Now().UnixNano())
		unix.Write(r.fd, udpPacket(buf, key.dst, key.src, n))
		r.bytesDown.Add(int64(n))
	}
}

func (r *udpRelay) shutdown() {
	r.enabled.Store(false)
	r.mu.Lock()
	for k, f := range r.flows {
		f.conn.Close()
		delete(r.flows, k)
	}
	r.mu.Unlock()
}

func (r *udpRelay) active() int {
	r.mu.Lock()
	defer r.mu.Unlock()
	return len(r.flows)
}
