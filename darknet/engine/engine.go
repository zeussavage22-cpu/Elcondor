// Package engine moves hotspot traffic from DarkNet's TUN out to the internet.
//
// Android routes every hotspot client into a TUN that this process owns. Each
// packet read from it is handled on one of these paths:
//
//   - Fast path (TCP, IPv4 and, once EnableIPv6 succeeds, IPv6): the packet's
//     addresses are rewritten so Android's own kernel TCP terminates the
//     connection on a local listener, and that socket is spliced to an ordinary
//     outbound socket. See fastpath.go.
//   - UDP relay (plain UDP, with the fast path on): one kernel UDP socket per
//     flow, no userspace stack. See udprelay.go.
//   - Userspace path (everything else, and all traffic if the fast path is off or
//     fails its start-up probe): a gVisor network stack terminates the flow and
//     relays it over an ordinary socket. See userspace.go.
//
// Outbound sockets are ordinary sockets of this process, optionally pinned to
// one or several Android networks, with BBR when the kernel allows it. See
// outbound.go.
//
// The exported surface is small and uses only types gomobile can bind.
package engine

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"net/netip"
	"runtime"
	"sync"
	"sync/atomic"
	"time"

	"golang.org/x/sys/unix"
)

// Version identifies the engine build in logs.
const Version = "darknet-engine 1.1"

// Session is one running engine bound to one TUN file descriptor.
type Session struct {
	fd       int
	cancel   context.CancelFunc
	user     *userspace
	fast     *fastPath // IPv4 TCP
	fast6    atomic.Pointer[fastPath]
	udp      *udpRelay
	out      *outbound
	stopOnce sync.Once
}

// Start attaches the engine to tunFD, an open TUN (no packet-info header).
// It does not take ownership of the descriptor: the caller closes it after Stop.
//
// tunAddr4 is the TUN's IPv4 address and fakeAddr4 an unused address in the
// same subnet (the fast path's NAT peer). With fast false, or if the fast path
// fails its probe, every packet uses the userspace path.
func Start(tunFD int, mtu int, tunAddr4 string, fakeAddr4 string, fast bool) (*Session, error) {
	if tunFD < 0 {
		return nil, errors.New("engine.Start: invalid TUN descriptor")
	}
	if mtu < 1280 || mtu > 65535 {
		return nil, fmt.Errorf("engine.Start: unsupported MTU %d", mtu)
	}

	// Android hands the TUN over non-blocking. Reads must block (or poll):
	// spinning on EAGAIN pins a core at 100% and heats the phone.
	if err := unix.SetNonblock(tunFD, false); err != nil {
		return nil, fmt.Errorf("engine.Start: make TUN blocking: %w", err)
	}

	out := newOutbound()
	user, err := newUserspace(uint32(mtu), out)
	if err != nil {
		return nil, err
	}

	ctx, cancel := context.WithCancel(context.Background())
	s := &Session{fd: tunFD, cancel: cancel, user: user, out: out}

	if fast {
		tunAddr, err1 := netip.ParseAddr(tunAddr4)
		fakeAddr, err2 := netip.ParseAddr(fakeAddr4)
		if err1 == nil && err2 == nil && tunAddr.Is4() && fakeAddr.Is4() {
			if fp, err := newFastPath(tunFD, tunAddr, fakeAddr, out); err == nil {
				s.fast = fp
			}
		}
		s.udp = newUDPRelay(tunFD, out)
	}

	go s.writeLoop(ctx)
	go s.readLoop(ctx)

	if s.fast != nil && !s.fast.probe(2*time.Second) {
		s.fast.shutdown()
	}
	return s, nil
}

// FastPathActive reports whether IPv4 TCP is using the kernel fast path.
func (s *Session) FastPathActive() bool {
	return s.fast != nil && s.fast.enabled.Load()
}

// EnableIPv6 starts the fast path for IPv6 TCP on the TUN's IPv6 address, with
// fakeAddr6 an unused address in the same prefix. It reports whether the
// start-up probe passed; if not, IPv6 TCP stays on the userspace path.
func (s *Session) EnableIPv6(tunAddr6, fakeAddr6 string) bool {
	if s.udp == nil || s.fast6.Load() != nil {
		return s.FastPath6Active()
	}
	tunAddr, err1 := netip.ParseAddr(tunAddr6)
	fakeAddr, err2 := netip.ParseAddr(fakeAddr6)
	if err1 != nil || err2 != nil || !tunAddr.Is6() || !fakeAddr.Is6() {
		return false
	}
	var fp *fastPath
	var err error
	// A fresh IPv6 address can take a moment before it accepts a bind.
	for i := 0; i < 10; i++ {
		if fp, err = newFastPath(s.fd, tunAddr, fakeAddr, s.out); err == nil {
			break
		}
		time.Sleep(200 * time.Millisecond)
	}
	if err != nil {
		return false
	}
	s.fast6.Store(fp)
	if !fp.probe(2 * time.Second) {
		fp.shutdown()
		return false
	}
	return true
}

// FastPath6Active reports whether IPv6 TCP is using the kernel fast path.
func (s *Session) FastPath6Active() bool {
	fp := s.fast6.Load()
	return fp != nil && fp.enabled.Load()
}

// UDPRelayActive reports whether plain UDP skips the userspace stack.
func (s *Session) UDPRelayActive() bool {
	return s.udp != nil && s.udp.enabled.Load()
}

// FastBytesUp is client→internet traffic the fast path carried. Fast-path packets
// cross the TUN twice (read, then written back rewritten), so the interface's
// own counters include them in both directions: real upload is TUN tx minus
// FastBytesDown, and real download is TUN rx minus FastBytesUp.
func (s *Session) FastBytesUp() int64 {
	var n int64
	if s.fast != nil {
		n += s.fast.bytesUp.Load()
	}
	if fp := s.fast6.Load(); fp != nil {
		n += fp.bytesUp.Load()
	}
	return n
}

// FastBytesDown is internet→client traffic the fast path carried.
func (s *Session) FastBytesDown() int64 {
	var n int64
	if s.fast != nil {
		n += s.fast.bytesDown.Load()
	}
	if fp := s.fast6.Load(); fp != nil {
		n += fp.bytesDown.Load()
	}
	return n
}

// SetNetwork pins every new outbound socket to an Android network handle (from
// Network.getNetworkHandle), so traffic fails instead of leaking if that network
// (a VPN) goes away. 0 unpins.
func (s *Session) SetNetwork(handle int64) {
	if handle == 0 {
		s.out.setNetworks(nil)
	} else {
		s.out.setNetworks([]uint64{uint64(handle)})
	}
}

// SetNetworks spreads new connections over several Android networks, given as
// comma-separated network handles ("" goes back to the default network).
func (s *Session) SetNetworks(handles string) {
	s.out.setNetworks(parseHandles(handles))
}

// SetCongestion asks for a TCP congestion control algorithm ("bbr") on new
// outbound TCP sockets; "" leaves the kernel default.
func (s *Session) SetCongestion(name string) {
	s.out.cc.Store(name)
}

// Congestion is the algorithm the kernel last gave an outbound socket, with
// the reason when it refused the requested one ("" until the first connection).
func (s *Session) Congestion() string {
	return s.out.ccActive.Load().(string)
}

// Stats is a JSON summary of the engine for diagnostics.
func (s *Session) Stats() string {
	st := map[string]any{
		"fast4":      s.FastPathActive(),
		"fast6":      s.FastPath6Active(),
		"udp":        s.UDPRelayActive(),
		"fastUp":     s.FastBytesUp(),
		"fastDown":   s.FastBytesDown(),
		"congestion": s.Congestion(),
	}
	if s.udp != nil {
		st["udpFlows"] = s.udp.active()
		st["udpFlowsOpened"] = s.udp.flowsOpened.Load()
		st["udpUp"] = s.udp.bytesUp.Load()
		st["udpDown"] = s.udp.bytesDown.Load()
	}
	var nets map[string]int64
	json.Unmarshal([]byte(s.out.stats()), &nets)
	st["networks"] = nets
	b, _ := json.Marshal(st)
	return string(b)
}

// Stop shuts the engine down. The TUN descriptor stays open for the caller.
func (s *Session) Stop() {
	s.stopOnce.Do(func() {
		s.cancel()
		if s.fast != nil {
			s.fast.shutdown()
		}
		if fp := s.fast6.Load(); fp != nil {
			fp.shutdown()
		}
		if s.udp != nil {
			s.udp.shutdown()
		}
		s.user.close()
	})
}

// ---- packet pump ------------------------------------------------------------

type packet struct {
	data [65536]byte
	n    int
}

var packetPool = sync.Pool{New: func() any { return new(packet) }}

func workerCount() int {
	n := runtime.NumCPU() / 2
	if n < 2 {
		return 2
	}
	if n > 4 {
		return 4
	}
	return n
}

// readLoop reads packets off the TUN and hands each to a worker chosen by its
// flow, so packets of one connection stay in order while flows use many cores.
func (s *Session) readLoop(ctx context.Context) {
	queues := make([]chan *packet, workerCount())
	for i := range queues {
		queues[i] = make(chan *packet, 512)
		go s.worker(queues[i])
	}
	defer func() {
		for _, q := range queues {
			close(q)
		}
	}()

	for ctx.Err() == nil {
		p := packetPool.Get().(*packet)
		n, err := unix.Read(s.fd, p.data[:])
		if err != nil {
			packetPool.Put(p)
			switch err {
			case unix.EINTR:
				continue
			case unix.EAGAIN:
				waitReadable(s.fd)
				continue
			}
			return // descriptor closed: the session is over
		}
		if n == 0 {
			packetPool.Put(p)
			continue
		}
		p.n = n
		queues[flowHash(p.data[:n])%uint32(len(queues))] <- p
	}
}

func (s *Session) worker(q chan *packet) {
	for p := range q {
		pkt := p.data[:p.n]
		if !s.route(pkt) {
			s.user.inject(pkt)
		}
		packetPool.Put(p)
	}
}

// route hands a packet to the fast path or the UDP relay; false means userspace.
func (s *Session) route(pkt []byte) bool {
	if len(pkt) == 0 {
		return false
	}
	if pkt[0]>>4 == 6 {
		if fp := s.fast6.Load(); fp != nil && fp.handle(pkt) {
			return true
		}
	} else if s.fast != nil && s.fast.handle(pkt) {
		return true
	}
	return s.udp != nil && s.udp.handle(pkt)
}

// writeLoop sends what the userspace stack produces back into the TUN.
func (s *Session) writeLoop(ctx context.Context) {
	for {
		out := s.user.next(ctx)
		if out == nil {
			return
		}
		unix.Write(s.fd, out)
	}
}

// waitReadable sleeps until the descriptor has a packet (or half a second
// passes), so a non-blocking descriptor never turns the read loop into a spin.
func waitReadable(fd int) {
	fds := []unix.PollFd{{Fd: int32(fd), Events: unix.POLLIN}}
	unix.Poll(fds, 500)
}
