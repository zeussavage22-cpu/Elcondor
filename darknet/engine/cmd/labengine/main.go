//go:build linux && !android

// labengine runs the engine on an existing TUN for the network lab (darknet/lab).
package main

import (
	"flag"
	"fmt"
	"os"
	"os/signal"
	"unsafe"

	"darknet/engine"
	"golang.org/x/sys/unix"
)

func main() {
	tun := flag.String("tun", "tun0", "TUN interface")
	addr := flag.String("addr", "10.0.0.2", "TUN IPv4 address")
	fake := flag.String("fake", "10.0.0.3", "unused address in the TUN subnet")
	fast := flag.Bool("fast", true, "use the kernel fast path")
	nonblock := flag.Bool("nonblock", false, "open the TUN non-blocking, as Android hands it over")
	addr6 := flag.String("addr6", "", "TUN IPv6 address (enables the IPv6 fast path)")
	fake6 := flag.String("fake6", "", "unused IPv6 address in the TUN prefix")
	cc := flag.String("cc", "", "TCP congestion control for outbound sockets, e.g. bbr")
	block := flag.Bool("block", false, "start with the kill switch on")
	flag.Parse()

	fd, err := unix.Open("/dev/net/tun", unix.O_RDWR, 0)
	if err != nil {
		panic(err)
	}
	var req [unix.IFNAMSIZ + 64]byte
	copy(req[:], *tun)
	*(*uint16)(unsafe.Pointer(&req[unix.IFNAMSIZ])) = unix.IFF_TUN | unix.IFF_NO_PI
	if _, _, errno := unix.Syscall(unix.SYS_IOCTL, uintptr(fd), unix.TUNSETIFF, uintptr(unsafe.Pointer(&req[0]))); errno != 0 {
		panic(errno)
	}

	if *nonblock {
		unix.SetNonblock(fd, true)
	}
	s, err := engine.Start(fd, 1500, *addr, *fake, *fast)
	if err != nil {
		panic(err)
	}
	s.SetCongestion(*cc)
	s.SetBlocked(*block)
	fmt.Printf("engine up, fast path active: %v\n", s.FastPathActive())
	if *addr6 != "" {
		fmt.Printf("ipv6 fast path active: %v\n", s.EnableIPv6(*addr6, *fake6))
	}
	fmt.Printf("udp relay active: %v\n", s.UDPRelayActive())
	c := make(chan os.Signal, 1)
	signal.Notify(c, os.Interrupt, unix.SIGTERM)
	<-c
	fmt.Println("stats:", s.Stats())
	s.Stop()
}
