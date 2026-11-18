package engine

import (
	"sync/atomic"
	"syscall"
)

// binding pins new outbound sockets to an Android network. Zero means the
// process default network.
type binding struct {
	handle atomic.Uint64
}

func (b *binding) set(handle uint64) { b.handle.Store(handle) }

// control runs before connect(2), the only point where pinning still decides
// the route. A failed pin fails the dial, so pinned traffic never leaks.
func (b *binding) control(_, _ string, conn syscall.RawConn) error {
	handle := b.handle.Load()
	if handle == 0 {
		return nil
	}
	var pinErr error
	if err := conn.Control(func(fd uintptr) { pinErr = pinToNetwork(handle, fd) }); err != nil {
		return err
	}
	return pinErr
}
