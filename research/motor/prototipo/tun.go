package main

import (
	"os"
	"unsafe"

	"golang.org/x/sys/unix"
)

// openTun attaches to an existing TUN interface (no packet-info header), like
// the fd TestNetworkManager hands the app on Android.
func openTun(name string) (int, error) {
	fd, err := unix.Open("/dev/net/tun", os.O_RDWR, 0)
	if err != nil {
		return -1, err
	}
	var req [unix.IFNAMSIZ + 64]byte
	copy(req[:], name)
	*(*uint16)(unsafe.Pointer(&req[unix.IFNAMSIZ])) = unix.IFF_TUN | unix.IFF_NO_PI
	if _, _, errno := unix.Syscall(unix.SYS_IOCTL, uintptr(fd), unix.TUNSETIFF, uintptr(unsafe.Pointer(&req[0]))); errno != 0 {
		unix.Close(fd)
		return -1, errno
	}
	return fd, nil
}
