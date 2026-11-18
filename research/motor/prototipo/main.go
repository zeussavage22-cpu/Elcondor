// Prototype "system stack" engine.
//
// TCP is not terminated in userspace. Each client packet read from the TUN is
// rewritten so it is addressed to a local listener (src = fake peer F:natPort,
// dst = tun address A:listenPort) and written back into the TUN. The kernel's
// own TCP stack completes the handshake with the listener; on accept, the NAT
// table recovers the original destination, the engine dials it with a normal
// kernel socket and the two kernel sockets are spliced together. Replies from
// the listener (dst = F) are rewritten back to look like they come from the
// original destination. UDP is relayed in userspace per flow.
package main

import (
	"encoding/binary"
	"flag"
	"fmt"
	"io"
	"net"
	"net/netip"
	"os"
	"os/signal"
	"sync"
	"time"

	"golang.org/x/sys/unix"
)

type flowKey struct {
	src, dst         netip.Addr
	srcPort, dstPort uint16
}

type session struct {
	key     flowKey
	natPort uint16
	created time.Time
}

type engine struct {
	fd         int
	tunAddr    netip.Addr // A
	fakeAddr   netip.Addr // F
	listenPort uint16

	mu      sync.RWMutex
	byKey   map[flowKey]*session
	byPort  map[uint16]*session
	nextNat uint16

	udpMu sync.Mutex
	udp   map[flowKey]*net.UDPConn

}

func main() {
	tunName := flag.String("tun", "tun0", "tun interface")
	tunAddr := flag.String("addr", "10.0.0.2", "tun address")
	fakeAddr := flag.String("fake", "10.0.0.3", "unused address in the tun subnet")
	port := flag.Int("port", 7777, "listener port")
	flag.Parse()

	fd, err := openTun(*tunName)
	if err != nil {
		panic(err)
	}
	e := &engine{
		fd:         fd,
		tunAddr:    netip.MustParseAddr(*tunAddr),
		fakeAddr:   netip.MustParseAddr(*fakeAddr),
		listenPort: uint16(*port),
		byKey:      map[flowKey]*session{},
		byPort:     map[uint16]*session{},
		nextNat:    20000,
		udp:        map[flowKey]*net.UDPConn{},
	}

	ln, err := net.ListenTCP("tcp4", &net.TCPAddr{IP: e.tunAddr.AsSlice(), Port: *port})
	if err != nil {
		panic(err)
	}
	go e.acceptLoop(ln)
	go e.gcLoop()
	go e.readLoop()

	fmt.Println("system engine up")
	c := make(chan os.Signal, 1)
	signal.Notify(c, os.Interrupt)
	<-c
}

func (e *engine) write(pkt []byte) {
	unix.Write(e.fd, pkt)
}

const workers = 4

var bufPool = sync.Pool{New: func() any { b := make([]byte, 65536); return &b }}

type job struct {
	buf *[]byte
	n   int
}

func (e *engine) readLoop() {
	queues := make([]chan job, workers)
	for i := range queues {
		queues[i] = make(chan job, 1024)
		go e.worker(queues[i])
	}
	for {
		b := bufPool.Get().(*[]byte)
		n, err := unix.Read(e.fd, *b)
		if err != nil {
			bufPool.Put(b)
			if err == unix.EINTR || err == unix.EAGAIN {
				continue
			}
			panic(err)
		}
		queues[flowHash((*b)[:n])%workers] <- job{b, n}
	}
}

// flowHash keeps every packet of one connection (both directions) on one worker.
func flowHash(p []byte) uint32 {
	if len(p) < 24 || p[0]>>4 != 4 {
		return 0
	}
	ihl := int(p[0]&0x0f) * 4
	if len(p) < ihl+4 {
		return 0
	}
	a := binary.BigEndian.Uint32(p[12:16]) ^ binary.BigEndian.Uint32(p[16:20])
	b := uint32(binary.BigEndian.Uint16(p[ihl:])) ^ uint32(binary.BigEndian.Uint16(p[ihl+2:]))
	h := a ^ b
	h ^= h >> 16
	h *= 0x45d9f3b
	h ^= h >> 16
	return h
}

func (e *engine) worker(q chan job) {
	for j := range q {
		e.process((*j.buf)[:j.n])
		bufPool.Put(j.buf)
	}
}

func (e *engine) process(pkt []byte) {
	n := len(pkt)
	{
		if n < 20 || pkt[0]>>4 != 4 {
			return // prototype: IPv4 only
		}
		ihl := int(pkt[0]&0x0f) * 4
		if n < ihl+8 {
			return
		}
		switch pkt[9] {
		case unix.IPPROTO_TCP:
			if n >= ihl+20 {
				e.handleTCP(pkt, ihl)
			}
		case unix.IPPROTO_UDP:
			e.handleUDP(pkt, ihl)
		}
	}
}

func addr4(b []byte) netip.Addr { return netip.AddrFrom4([4]byte(b[:4])) }

func (e *engine) handleTCP(pkt []byte, ihl int) {
	src, dst := addr4(pkt[12:16]), addr4(pkt[16:20])
	tcp := pkt[ihl:]
	srcPort := binary.BigEndian.Uint16(tcp[0:2])
	dstPort := binary.BigEndian.Uint16(tcp[2:4])

	if dst == e.fakeAddr {
		// Reply from the local listener: back to the client, from the real destination.
		e.mu.RLock()
		s := e.byPort[dstPort]
		e.mu.RUnlock()
		if s == nil || srcPort != e.listenPort {
			return
		}
		rewrite(pkt, ihl, s.key.dst, s.key.dstPort, s.key.src, s.key.srcPort)
		e.write(pkt)
		return
	}

	key := flowKey{src: src, dst: dst, srcPort: srcPort, dstPort: dstPort}
	e.mu.RLock()
	s := e.byKey[key]
	e.mu.RUnlock()
	if s != nil {
		rewrite(pkt, ihl, e.fakeAddr, s.natPort, e.tunAddr, e.listenPort)
		e.write(pkt)
		return
	}
	e.mu.Lock()
	s = e.byKey[key]
	if s == nil {
		if tcp[13]&0x02 == 0 { // not a SYN and no session: ignore
			e.mu.Unlock()
			return
		}
		s = e.newSession(key)
	}
	nat := s.natPort
	e.mu.Unlock()

	rewrite(pkt, ihl, e.fakeAddr, nat, e.tunAddr, e.listenPort)
	e.write(pkt)
}

func (e *engine) newSession(key flowKey) *session {
	for i := 0; i < 40000; i++ {
		e.nextNat++
		if e.nextNat < 20000 {
			e.nextNat = 20000
		}
		if e.byPort[e.nextNat] == nil {
			break
		}
	}
	s := &session{key: key, natPort: e.nextNat, created: time.Now()}
	e.byKey[key] = s
	e.byPort[s.natPort] = s
	return s
}

func (e *engine) drop(s *session) {
	e.mu.Lock()
	if e.byPort[s.natPort] == s {
		delete(e.byPort, s.natPort)
	}
	if e.byKey[s.key] == s {
		delete(e.byKey, s.key)
	}
	e.mu.Unlock()
}

func (e *engine) acceptLoop(ln *net.TCPListener) {
	for {
		c, err := ln.AcceptTCP()
		if err != nil {
			continue
		}
		go e.serve(c)
	}
}

func (e *engine) serve(client *net.TCPConn) {
	port := uint16(client.RemoteAddr().(*net.TCPAddr).Port)
	e.mu.Lock()
	s := e.byPort[port]
	e.mu.Unlock()
	if s == nil {
		client.Close()
		return
	}
	defer e.drop(s)

	upstream, err := net.DialTimeout("tcp", netip.AddrPortFrom(s.key.dst, s.key.dstPort).String(), 10*time.Second)
	if err != nil {
		client.SetLinger(0)
		client.Close()
		return
	}
	up := upstream.(*net.TCPConn)

	// Kernel socket to kernel socket: io.Copy uses splice(2), zero-copy.
	var wg sync.WaitGroup
	wg.Add(2)
	go func() { io.Copy(up, client); up.CloseWrite(); wg.Done() }()
	go func() { io.Copy(client, up); client.CloseWrite(); wg.Done() }()
	wg.Wait()
	up.Close()
	client.Close()
}

// gcLoop drops sessions whose handshake never reached the listener.
func (e *engine) gcLoop() {
	for range time.Tick(30 * time.Second) {
		e.mu.Lock()
		for k, s := range e.byKey {
			if time.Since(s.created) > 10*time.Minute {
				delete(e.byKey, k)
				delete(e.byPort, s.natPort)
			}
		}
		e.mu.Unlock()
	}
}

// rewrite changes addresses/ports in place with incremental checksum updates (RFC 1624).
func rewrite(pkt []byte, ihl int, src netip.Addr, srcPort uint16, dst netip.Addr, dstPort uint16) {
	tcp := pkt[ihl:]
	ipSum := binary.BigEndian.Uint16(pkt[10:12])
	tcpSum := binary.BigEndian.Uint16(tcp[16:18])

	s4, d4 := src.As4(), dst.As4()
	for i := 0; i < 4; i += 2 {
		for _, f := range []struct {
			off int
			val uint16
		}{{12 + i, binary.BigEndian.Uint16(s4[i:])}, {16 + i, binary.BigEndian.Uint16(d4[i:])}} {
			old := binary.BigEndian.Uint16(pkt[f.off:])
			ipSum = csumAdjust(ipSum, old, f.val)
			tcpSum = csumAdjust(tcpSum, old, f.val)
			binary.BigEndian.PutUint16(pkt[f.off:], f.val)
		}
	}
	tcpSum = csumAdjust(tcpSum, binary.BigEndian.Uint16(tcp[0:2]), srcPort)
	binary.BigEndian.PutUint16(tcp[0:2], srcPort)
	tcpSum = csumAdjust(tcpSum, binary.BigEndian.Uint16(tcp[2:4]), dstPort)
	binary.BigEndian.PutUint16(tcp[2:4], dstPort)

	binary.BigEndian.PutUint16(pkt[10:12], ipSum)
	binary.BigEndian.PutUint16(tcp[16:18], tcpSum)
}

func csumAdjust(sum, old, new uint16) uint16 {
	// HC' = ~(~HC + ~m + m')
	s := uint32(^sum) + uint32(^old) + uint32(new)
	s = (s & 0xffff) + (s >> 16)
	s = (s & 0xffff) + (s >> 16)
	return ^uint16(s)
}

// ---- UDP: userspace relay per flow ----

func (e *engine) handleUDP(pkt []byte, ihl int) {
	udp := pkt[ihl:]
	key := flowKey{
		src: addr4(pkt[12:16]), dst: addr4(pkt[16:20]),
		srcPort: binary.BigEndian.Uint16(udp[0:2]), dstPort: binary.BigEndian.Uint16(udp[2:4]),
	}
	length := int(binary.BigEndian.Uint16(udp[4:6]))
	if length < 8 || ihl+length > len(pkt) {
		return
	}
	payload := udp[8:length]

	e.udpMu.Lock()
	conn := e.udp[key]
	if conn == nil {
		c, err := net.DialUDP("udp4", nil, net.UDPAddrFromAddrPort(netip.AddrPortFrom(key.dst, key.dstPort)))
		if err != nil {
			e.udpMu.Unlock()
			return
		}
		conn = c
		e.udp[key] = conn
		go e.udpReplies(key, conn)
	}
	e.udpMu.Unlock()
	conn.Write(payload)
}

func (e *engine) udpReplies(key flowKey, conn *net.UDPConn) {
	defer func() {
		e.udpMu.Lock()
		delete(e.udp, key)
		e.udpMu.Unlock()
		conn.Close()
	}()
	buf := make([]byte, 65535)
	out := make([]byte, 65535+28)
	for {
		conn.SetReadDeadline(time.Now().Add(60 * time.Second))
		n, err := conn.Read(buf)
		if err != nil {
			return
		}
		total := 28 + n
		pkt := out[:total]
		pkt[0], pkt[1] = 0x45, 0
		binary.BigEndian.PutUint16(pkt[2:], uint16(total))
		binary.BigEndian.PutUint32(pkt[4:], 0)
		pkt[8], pkt[9] = 64, unix.IPPROTO_UDP
		pkt[10], pkt[11] = 0, 0
		s4, d4 := key.dst.As4(), key.src.As4()
		copy(pkt[12:], s4[:])
		copy(pkt[16:], d4[:])
		binary.BigEndian.PutUint16(pkt[10:], checksum(pkt[:20], 0))
		u := pkt[20:]
		binary.BigEndian.PutUint16(u[0:], key.dstPort)
		binary.BigEndian.PutUint16(u[2:], key.srcPort)
		binary.BigEndian.PutUint16(u[4:], uint16(8+n))
		u[6], u[7] = 0, 0
		copy(u[8:], buf[:n])
		pseudo := uint32(0)
		for i := 12; i < 20; i += 2 {
			pseudo += uint32(binary.BigEndian.Uint16(pkt[i:]))
		}
		pseudo += unix.IPPROTO_UDP + uint32(8+n)
		cs := checksum(u[:8+n], pseudo)
		if cs == 0 {
			cs = 0xffff
		}
		binary.BigEndian.PutUint16(u[6:], cs)
		e.write(pkt)
	}
}

func checksum(b []byte, initial uint32) uint16 {
	sum := initial
	for i := 0; i+1 < len(b); i += 2 {
		sum += uint32(binary.BigEndian.Uint16(b[i:]))
	}
	if len(b)%2 == 1 {
		sum += uint32(b[len(b)-1]) << 8
	}
	for sum>>16 != 0 {
		sum = (sum & 0xffff) + (sum >> 16)
	}
	return ^uint16(sum)
}
