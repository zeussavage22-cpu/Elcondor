package engine

import (
	"context"
	"errors"
	"io"
	"net"
	"strconv"
	"sync"
	"time"

	"gvisor.dev/gvisor/pkg/buffer"
	"gvisor.dev/gvisor/pkg/tcpip"
	"gvisor.dev/gvisor/pkg/tcpip/adapters/gonet"
	"gvisor.dev/gvisor/pkg/tcpip/header"
	"gvisor.dev/gvisor/pkg/tcpip/link/channel"
	"gvisor.dev/gvisor/pkg/tcpip/network/ipv4"
	"gvisor.dev/gvisor/pkg/tcpip/network/ipv6"
	"gvisor.dev/gvisor/pkg/tcpip/stack"
	"gvisor.dev/gvisor/pkg/tcpip/transport/icmp"
	"gvisor.dev/gvisor/pkg/tcpip/transport/tcp"
	"gvisor.dev/gvisor/pkg/tcpip/transport/udp"
	"gvisor.dev/gvisor/pkg/waiter"
)

const (
	userNIC        tcpip.NICID = 1
	maxPendingTCP              = 1024
	udpIdleTimeout             = 2 * time.Minute
	relayBuffer                = 64 << 10
)

// userspace terminates flows in a gVisor stack and relays them over real sockets.
type userspace struct {
	stack  *stack.Stack
	link   *channel.Endpoint
	dialer *net.Dialer
}

func newUserspace(mtu uint32, dialer *net.Dialer) (*userspace, error) {
	s := stack.New(stack.Options{
		NetworkProtocols: []stack.NetworkProtocolFactory{ipv4.NewProtocol, ipv6.NewProtocol},
		TransportProtocols: []stack.TransportProtocolFactory{
			tcp.NewProtocol, udp.NewProtocol, icmp.NewProtocol4, icmp.NewProtocol6,
		},
	})
	tuneStack(s)

	link := channel.New(1024, mtu, "")
	if err := s.CreateNIC(userNIC, link); err != nil {
		s.Close()
		return nil, errors.New("engine: create userspace NIC: " + err.String())
	}
	// These flows are addressed to hosts on the internet, not to this stack:
	// accept every destination and answer as any source.
	s.SetPromiscuousMode(userNIC, true)
	s.SetSpoofing(userNIC, true)
	s.SetRouteTable([]tcpip.Route{
		{Destination: header.IPv4EmptySubnet, NIC: userNIC},
		{Destination: header.IPv6EmptySubnet, NIC: userNIC},
	})

	u := &userspace{stack: s, link: link, dialer: dialer}

	tcpForwarder := tcp.NewForwarder(s, 0, maxPendingTCP, u.forwardTCP)
	s.SetTransportProtocolHandler(tcp.ProtocolNumber, tcpForwarder.HandlePacket)
	udpForwarder := udp.NewForwarder(s, u.forwardUDP)
	s.SetTransportProtocolHandler(udp.ProtocolNumber, udpForwarder.HandlePacket)
	return u, nil
}

// tuneStack turns on what a modern kernel TCP does by default: selective acks,
// RACK loss detection, CUBIC and receive-buffer auto-tuning.
func tuneStack(s *stack.Stack) {
	sack := tcpip.TCPSACKEnabled(true)
	s.SetTransportProtocolOption(tcp.ProtocolNumber, &sack)
	rack := tcpip.TCPRecovery(tcpip.TCPRACKLossDetection)
	s.SetTransportProtocolOption(tcp.ProtocolNumber, &rack)
	cubic := tcpip.CongestionControlOption("cubic")
	s.SetTransportProtocolOption(tcp.ProtocolNumber, &cubic)
	autotune := tcpip.TCPModerateReceiveBufferOption(true)
	s.SetTransportProtocolOption(tcp.ProtocolNumber, &autotune)
	rcv := tcpip.TCPReceiveBufferSizeRangeOption{Min: 4 << 10, Default: 1 << 20, Max: 8 << 20}
	s.SetTransportProtocolOption(tcp.ProtocolNumber, &rcv)
	snd := tcpip.TCPSendBufferSizeRangeOption{Min: 4 << 10, Default: 1 << 20, Max: 8 << 20}
	s.SetTransportProtocolOption(tcp.ProtocolNumber, &snd)
}

func (u *userspace) inject(pkt []byte) {
	var proto tcpip.NetworkProtocolNumber
	switch pkt[0] >> 4 {
	case 4:
		proto = ipv4.ProtocolNumber
	case 6:
		proto = ipv6.ProtocolNumber
	default:
		return
	}
	pb := stack.NewPacketBuffer(stack.PacketBufferOptions{
		Payload: buffer.MakeWithData(append([]byte(nil), pkt...)),
	})
	u.link.InjectInbound(proto, pb)
	pb.DecRef()
}

// next blocks for the next packet the stack wants on the TUN; nil when ctx ends.
func (u *userspace) next(ctx context.Context) []byte {
	pb := u.link.ReadContext(ctx)
	if pb == nil {
		return nil
	}
	view := pb.ToView()
	out := append([]byte(nil), view.AsSlice()...)
	view.Release()
	pb.DecRef()
	return out
}

func (u *userspace) close() {
	u.link.Close()
	u.stack.Close()
}

func target(addr tcpip.Address, port uint16) string {
	return net.JoinHostPort(net.IP(addr.AsSlice()).String(), strconv.Itoa(int(port)))
}

// forwardTCP dials the real destination first, so a refused or unreachable
// host turns into a reset for the client instead of a handshake that then dies.
func (u *userspace) forwardTCP(req *tcp.ForwarderRequest) {
	id := req.ID()
	upstream, err := u.dialer.Dial("tcp", target(id.LocalAddress, id.LocalPort))
	if err != nil {
		req.Complete(true)
		return
	}
	var wq waiter.Queue
	ep, tcpErr := req.CreateEndpoint(&wq)
	if tcpErr != nil {
		upstream.Close()
		req.Complete(true)
		return
	}
	req.Complete(false)
	ep.SocketOptions().SetKeepAlive(true)

	client := gonet.NewTCPConn(&wq, ep)
	go relayStreams(client, upstream.(*net.TCPConn))
}

// relayStreams copies both ways and passes a half-close on to the other side.
func relayStreams(client *gonet.TCPConn, upstream *net.TCPConn) {
	var wg sync.WaitGroup
	wg.Add(2)
	go func() {
		defer wg.Done()
		buf := make([]byte, relayBuffer)
		io.CopyBuffer(upstream, client, buf)
		upstream.CloseWrite()
	}()
	go func() {
		defer wg.Done()
		buf := make([]byte, relayBuffer)
		io.CopyBuffer(client, upstream, buf)
		client.CloseWrite()
	}()
	wg.Wait()
	client.Close()
	upstream.Close()
}

// forwardUDP relays one UDP flow; returning false makes the stack answer with
// ICMP port unreachable, the right reply when the flow cannot be set up.
func (u *userspace) forwardUDP(req *udp.ForwarderRequest) bool {
	id := req.ID()
	var wq waiter.Queue
	ep, tcpErr := req.CreateEndpoint(&wq)
	if tcpErr != nil {
		return false
	}
	client := gonet.NewUDPConn(&wq, ep)
	go func() {
		upstream, err := u.dialer.Dial("udp", target(id.LocalAddress, id.LocalPort))
		if err != nil {
			client.Close()
			return
		}
		relayDatagrams(client, upstream)
	}()
	return true
}

func relayDatagrams(client, upstream net.Conn) {
	done := make(chan struct{}, 2)
	pump := func(dst, src net.Conn) {
		buf := make([]byte, 65535)
		for {
			src.SetReadDeadline(time.Now().Add(udpIdleTimeout))
			n, err := src.Read(buf)
			if n > 0 {
				if _, werr := dst.Write(buf[:n]); werr != nil {
					break
				}
			}
			if err != nil {
				break
			}
		}
		done <- struct{}{}
	}
	go pump(upstream, client)
	go pump(client, upstream)
	<-done
	client.Close()
	upstream.Close()
}
