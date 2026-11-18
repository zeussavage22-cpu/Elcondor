package engine

import (
	"encoding/binary"
	"net"
	"net/netip"
	"sync"
	"sync/atomic"
	"time"

	"golang.org/x/sys/unix"
)

// fastPath hands IPv4 TCP to the kernel.
//
// A client packet A:p → R:r read from the TUN is rewritten to F:n → A:L (F an
// unused address in the TUN subnet, n a NAT port, L a local listener port) and
// written back into the TUN. The kernel's TCP completes the handshake with the
// listener; on accept, n gives back R:r, which is dialed with an ordinary socket
// and spliced to the accepted one. The listener's replies (to F) are rewritten
// back to R:r → A:p. Android gives an accepted socket the network mark of its
// SYN (tcp_fwmark_accept), so those replies route back into the TUN.
type fastPath struct {
	fd         int
	tunAddr    netip.Addr // A
	fakeAddr   netip.Addr // F
	listenPort uint16     // L
	listener   *net.TCPListener
	dialer     *net.Dialer
	enabled    atomic.Bool

	mu      sync.RWMutex
	byFlow  map[flow]*natEntry
	byPort  map[uint16]*natEntry
	nextNat uint16

	probed chan struct{}

	bytesUp, bytesDown atomic.Int64
}

type flow struct {
	src, dst         netip.Addr
	srcPort, dstPort uint16
}

type natEntry struct {
	flow    flow
	port    uint16
	created time.Time
	probe   bool
}

const (
	natFirst  = 20000
	natLast   = 60000
	probeNat  = 19999
	natLinger = 60 * time.Second // keep a mapping after close for the FIN/ACK exchange
	natStale  = 10 * time.Minute // a handshake that never reached the listener
)

func newFastPath(fd int, tunAddr, fakeAddr netip.Addr, dialer *net.Dialer) (*fastPath, error) {
	ln, err := net.ListenTCP("tcp4", &net.TCPAddr{IP: tunAddr.AsSlice(), Port: 0})
	if err != nil {
		return nil, err
	}
	f := &fastPath{
		fd:         fd,
		tunAddr:    tunAddr,
		fakeAddr:   fakeAddr,
		listenPort: uint16(ln.Addr().(*net.TCPAddr).Port),
		listener:   ln,
		dialer:     dialer,
		byFlow:     map[flow]*natEntry{},
		byPort:     map[uint16]*natEntry{},
		nextNat:    natFirst,
		probed:     make(chan struct{}, 1),
	}
	f.enabled.Store(true)
	go f.acceptLoop()
	go f.sweep()
	return f, nil
}

func (f *fastPath) shutdown() {
	if f.enabled.Swap(false) {
		f.listener.Close()
	}
}

// probe sends a SYN exactly as a rewritten client packet would look, completes
// the handshake, and waits for the listener to accept it. Success proves the
// platform delivers rewritten packets to the kernel and routes replies back.
func (f *fastPath) probe(timeout time.Duration) bool {
	entry := &natEntry{port: probeNat, created: time.Now(), probe: true}
	f.mu.Lock()
	f.byPort[probeNat] = entry
	f.mu.Unlock()
	defer func() {
		f.mu.Lock()
		delete(f.byPort, probeNat)
		f.mu.Unlock()
	}()

	syn := tcpSegment4(f.fakeAddr, probeNat, f.tunAddr, f.listenPort, 1, 0, flagSYN)
	unix.Write(f.fd, syn)
	retry := time.NewTicker(400 * time.Millisecond)
	defer retry.Stop()
	deadline := time.After(timeout)
	for {
		select {
		case <-f.probed:
			return true
		case <-retry.C:
			unix.Write(f.fd, syn)
		case <-deadline:
			return false
		}
	}
}

const (
	flagSYN = 0x02
	flagACK = 0x10
)

// handle takes IPv4 TCP; it returns false for packets the userspace path should get.
func (f *fastPath) handle(pkt []byte) bool {
	if !f.enabled.Load() || len(pkt) < 20 || pkt[0]>>4 != 4 || pkt[9] != unix.IPPROTO_TCP {
		return false
	}
	ihl := int(pkt[0]&0x0f) * 4
	if ihl < 20 || len(pkt) < ihl+20 || binary.BigEndian.Uint16(pkt[6:8])&0x3fff != 0 {
		return false // malformed or fragmented: leave it to the userspace stack
	}
	src := netip.AddrFrom4([4]byte(pkt[12:16]))
	dst := netip.AddrFrom4([4]byte(pkt[16:20]))
	seg := pkt[ihl:]
	srcPort := binary.BigEndian.Uint16(seg[0:2])
	dstPort := binary.BigEndian.Uint16(seg[2:4])
	flags := seg[13]

	if dst == f.fakeAddr {
		f.mu.RLock()
		entry := f.byPort[dstPort]
		f.mu.RUnlock()
		switch {
		case entry == nil || srcPort != f.listenPort:
			// stray: never let the fake address reach the userspace stack
		case entry.probe:
			if flags&(flagSYN|flagACK) == flagSYN|flagACK {
				seq := binary.BigEndian.Uint32(seg[4:8])
				unix.Write(f.fd, tcpSegment4(f.fakeAddr, probeNat, f.tunAddr, f.listenPort, 2, seq+1, flagACK))
			}
		default:
			rewrite4(pkt, ihl, entry.flow.dst, entry.flow.dstPort, entry.flow.src, entry.flow.srcPort)
			unix.Write(f.fd, pkt)
			f.bytesDown.Add(int64(len(pkt)))
		}
		return true
	}

	key := flow{src: src, dst: dst, srcPort: srcPort, dstPort: dstPort}
	f.mu.RLock()
	entry := f.byFlow[key]
	f.mu.RUnlock()
	if entry == nil {
		if flags&(flagSYN|flagACK) != flagSYN {
			return false // a flow the userspace stack already owns
		}
		f.mu.Lock()
		if entry = f.byFlow[key]; entry == nil {
			entry = f.allocate(key)
		}
		f.mu.Unlock()
		if entry == nil {
			return false // NAT ports exhausted
		}
	}
	rewrite4(pkt, ihl, f.fakeAddr, entry.port, f.tunAddr, f.listenPort)
	unix.Write(f.fd, pkt)
	f.bytesUp.Add(int64(len(pkt)))
	return true
}

func (f *fastPath) allocate(key flow) *natEntry {
	for i := 0; i <= natLast-natFirst; i++ {
		f.nextNat++
		if f.nextNat < natFirst || f.nextNat > natLast {
			f.nextNat = natFirst
		}
		if f.byPort[f.nextNat] == nil {
			e := &natEntry{flow: key, port: f.nextNat, created: time.Now()}
			f.byFlow[key] = e
			f.byPort[e.port] = e
			return e
		}
	}
	return nil
}

func (f *fastPath) release(e *natEntry) {
	f.mu.Lock()
	if f.byPort[e.port] == e {
		delete(f.byPort, e.port)
	}
	if f.byFlow[e.flow] == e {
		delete(f.byFlow, e.flow)
	}
	f.mu.Unlock()
}

func (f *fastPath) acceptLoop() {
	for {
		conn, err := f.listener.AcceptTCP()
		if err != nil {
			if !f.enabled.Load() {
				return
			}
			time.Sleep(10 * time.Millisecond)
			continue
		}
		go f.serve(conn)
	}
}

func (f *fastPath) serve(client *net.TCPConn) {
	port := uint16(client.RemoteAddr().(*net.TCPAddr).Port)
	f.mu.RLock()
	e := f.byPort[port]
	f.mu.RUnlock()
	if e == nil {
		client.Close()
		return
	}
	if e.probe {
		select {
		case f.probed <- struct{}{}:
		default:
		}
		client.SetLinger(0)
		client.Close()
		return
	}
	defer time.AfterFunc(natLinger, func() { f.release(e) })

	up, err := f.dialer.Dial("tcp", netip.AddrPortFrom(e.flow.dst, e.flow.dstPort).String())
	if err != nil {
		client.SetLinger(0) // reset: the client fails fast, as with a refused connection
		client.Close()
		return
	}
	splice(client, up.(*net.TCPConn))
}

// splice joins two kernel sockets; between *net.TCPConn the Go runtime uses
// splice(2), so the data never passes through this process. Half-closes pass on.
func splice(a, b *net.TCPConn) {
	var wg sync.WaitGroup
	wg.Add(2)
	go func() { defer wg.Done(); b.ReadFrom(a); b.CloseWrite() }()
	go func() { defer wg.Done(); a.ReadFrom(b); a.CloseWrite() }()
	wg.Wait()
	a.Close()
	b.Close()
}

func (f *fastPath) sweep() {
	tick := time.NewTicker(30 * time.Second)
	defer tick.Stop()
	for range tick.C {
		if !f.enabled.Load() {
			return
		}
		f.mu.Lock()
		for k, e := range f.byFlow {
			if time.Since(e.created) > natStale {
				delete(f.byFlow, k)
				delete(f.byPort, e.port)
			}
		}
		f.mu.Unlock()
	}
}
