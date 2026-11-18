// Package engine moves hotspot traffic from DarkNet's TUN out to the internet.
//
// Android routes every hotspot client into a TUN that this process owns. Each
// packet read from it is handled on one of two paths:
//
//   - Fast path (IPv4 TCP): the packet's addresses are rewritten so Android's own
//     kernel TCP terminates the connection on a local listener, and that socket
//     is spliced to an ordinary outbound socket. See fastpath.go.
//   - Userspace path (everything else, and all traffic if the fast path is off or
//     fails its start-up probe): a gVisor network stack terminates the flow and
//     relays it over an ordinary socket. See userspace.go.
//
// Outbound sockets are ordinary sockets of this process, optionally pinned to a
// given Android network (a VPN) with SetNetwork.
//
// The exported surface is small and uses only types gomobile can bind.
package engine

import (
	"context"
	"errors"
	"fmt"
	"net"
	"net/netip"
	"runtime"
	"sync"
	"time"

	"golang.org/x/sys/unix"
)

// Version identifies the engine build in logs.
const Version = "darknet-engine 1.0"

// Session is one running engine bound to one TUN file descriptor.
type Session struct {
	fd       int
	cancel   context.CancelFunc
	user     *userspace
	fast     *fastPath
	binding  *binding
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

	b := &binding{}
	dialer := &net.Dialer{Timeout: 10 * time.Second, KeepAlive: 30 * time.Second, Control: b.control}

	user, err := newUserspace(uint32(mtu), dialer)
	if err != nil {
		return nil, err
	}

	ctx, cancel := context.WithCancel(context.Background())
	s := &Session{fd: tunFD, cancel: cancel, user: user, binding: b}

	if fast {
		tunAddr, err1 := netip.ParseAddr(tunAddr4)
		fakeAddr, err2 := netip.ParseAddr(fakeAddr4)
		if err1 == nil && err2 == nil && tunAddr.Is4() && fakeAddr.Is4() {
			if fp, err := newFastPath(tunFD, tunAddr, fakeAddr, dialer); err == nil {
				s.fast = fp
			}
		}
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

// FastBytesUp is client→internet traffic the fast path carried. Fast-path packets
// cross the TUN twice (read, then written back rewritten), so the interface's
// own counters include them in both directions: real upload is TUN tx minus
// FastBytesDown, and real download is TUN rx minus FastBytesUp.
func (s *Session) FastBytesUp() int64 {
	if s.fast == nil {
		return 0
	}
	return s.fast.bytesUp.Load()
}

// FastBytesDown is internet→client traffic the fast path carried.
func (s *Session) FastBytesDown() int64 {
	if s.fast == nil {
		return 0
	}
	return s.fast.bytesDown.Load()
}

// SetNetwork pins every new outbound socket to an Android network handle (from
// Network.getNetworkHandle), so traffic fails instead of leaking if that network
// (a VPN) goes away. 0 unpins.
func (s *Session) SetNetwork(handle int64) {
	s.binding.set(uint64(handle))
}

// Stop shuts the engine down. The TUN descriptor stays open for the caller.
func (s *Session) Stop() {
	s.stopOnce.Do(func() {
		s.cancel()
		if s.fast != nil {
			s.fast.shutdown()
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
			if err == unix.EINTR || err == unix.EAGAIN {
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
		if s.fast == nil || !s.fast.handle(pkt) {
			s.user.inject(pkt)
		}
		packetPool.Put(p)
	}
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
