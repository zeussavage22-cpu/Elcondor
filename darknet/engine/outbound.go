package engine

import (
	"encoding/json"
	"errors"
	"net"
	"strconv"
	"strings"
	"sync"
	"sync/atomic"
	"syscall"
	"time"

	"golang.org/x/sys/unix"
)

// outbound opens the engine's sockets to the internet.
//
// With no networks set it uses the process default network. With one it pins
// every socket to it (so traffic fails instead of leaking if a VPN drops). With
// several it spreads new connections across them in turn and, if a network
// fails to connect, tries the next one: two networks (Wi-Fi and mobile data)
// add up their capacity across connections. One connection always stays on
// one network.
//
// It also asks the kernel for a congestion control algorithm (BBR) on every
// TCP socket, when one is set and the kernel allows it.
type outbound struct {
	mu       sync.RWMutex
	handles  []uint64
	dials    map[uint64]*atomic.Int64
	next     atomic.Uint32
	cc       atomic.Value // requested algorithm, string
	ccActive atomic.Value // what the kernel actually uses, string
}

const (
	dialTimeout      = 10 * time.Second
	dialTimeoutMulti = 5 * time.Second
)

func newOutbound() *outbound {
	o := &outbound{dials: map[uint64]*atomic.Int64{0: {}}}
	o.cc.Store("")
	o.ccActive.Store("")
	return o
}

func (o *outbound) setNetworks(handles []uint64) {
	o.mu.Lock()
	defer o.mu.Unlock()
	o.handles = append([]uint64(nil), handles...)
	for _, h := range handles {
		if o.dials[h] == nil {
			o.dials[h] = &atomic.Int64{}
		}
	}
}

func (o *outbound) networks() []uint64 {
	o.mu.RLock()
	defer o.mu.RUnlock()
	if len(o.handles) == 0 {
		return []uint64{0}
	}
	return o.handles
}

// Dial connects to addr, choosing the network as described on outbound.
func (o *outbound) Dial(network, addr string) (net.Conn, error) {
	nets := o.networks()
	timeout := dialTimeout
	if len(nets) > 1 {
		timeout = dialTimeoutMulti
	}
	start := int(o.next.Add(1))
	var firstErr error
	for i := range nets {
		h := nets[(start+i)%len(nets)]
		d := net.Dialer{Timeout: timeout, KeepAlive: 30 * time.Second, Control: o.control(h)}
		c, err := d.Dial(network, addr)
		if err == nil {
			o.mu.RLock()
			if n := o.dials[h]; n != nil {
				n.Add(1)
			}
			o.mu.RUnlock()
			return c, nil
		}
		if firstErr == nil {
			firstErr = err
		}
		if errors.Is(err, syscall.ECONNREFUSED) {
			break // the host itself said no: another network won't change that
		}
	}
	return nil, firstErr
}

// control runs before connect(2), the only point where pinning still decides
// the route. A failed pin fails the dial, so pinned traffic never leaks.
func (o *outbound) control(handle uint64) func(string, string, syscall.RawConn) error {
	return func(network, _ string, conn syscall.RawConn) error {
		var pinErr error
		err := conn.Control(func(fd uintptr) {
			if handle != 0 {
				if pinErr = pinToNetwork(handle, fd); pinErr != nil {
					return
				}
			}
			if strings.HasPrefix(network, "tcp") {
				o.applyCongestion(int(fd))
			}
		})
		if err != nil {
			return err
		}
		return pinErr
	}
}

func (o *outbound) applyCongestion(fd int) {
	want := o.cc.Load().(string)
	if want == "" {
		return
	}
	setErr := unix.SetsockoptString(fd, unix.IPPROTO_TCP, unix.TCP_CONGESTION, want)
	got, err := unix.GetsockoptString(fd, unix.IPPROTO_TCP, unix.TCP_CONGESTION)
	if err != nil {
		return
	}
	got = strings.TrimRight(got, "\x00")
	if setErr != nil && got != want {
		got += " (" + want + " no permitido: " + setErr.Error() + ")"
	}
	o.ccActive.Store(got)
}

// stats is JSON {"<network handle>": connections opened, ...}; "0" is the default network.
func (o *outbound) stats() string {
	o.mu.RLock()
	defer o.mu.RUnlock()
	out := map[string]int64{}
	for h, n := range o.dials {
		if v := n.Load(); v > 0 || h == 0 {
			out[strconv.FormatUint(h, 10)] = v
		}
	}
	b, _ := json.Marshal(out)
	return string(b)
}

// parseHandles reads "h1,h2,…" (empty: default network).
func parseHandles(csv string) []uint64 {
	var out []uint64
	for _, f := range strings.Split(csv, ",") {
		if f = strings.TrimSpace(f); f == "" {
			continue
		}
		if h, err := strconv.ParseUint(f, 10, 64); err == nil && h != 0 {
			out = append(out, h)
		}
	}
	return out
}
