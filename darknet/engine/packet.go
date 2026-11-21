package engine

import (
	"encoding/binary"
	"net/netip"

	"golang.org/x/sys/unix"
)

// rewrite4 sets an IPv4 TCP packet's addresses and ports in place and updates
// both checksums incrementally (RFC 1624), without touching the payload.
func rewrite4(pkt []byte, ihl int, src netip.Addr, srcPort uint16, dst netip.Addr, dstPort uint16) {
	seg := pkt[ihl:]
	ipSum := binary.BigEndian.Uint16(pkt[10:12])
	tcpSum := binary.BigEndian.Uint16(seg[16:18])

	put := func(b []byte, off int, v uint16, pseudo bool) {
		old := binary.BigEndian.Uint16(b[off:])
		if pseudo {
			ipSum = adjust(ipSum, old, v)
		}
		tcpSum = adjust(tcpSum, old, v)
		binary.BigEndian.PutUint16(b[off:], v)
	}
	s4, d4 := src.As4(), dst.As4()
	put(pkt, 12, binary.BigEndian.Uint16(s4[0:]), true)
	put(pkt, 14, binary.BigEndian.Uint16(s4[2:]), true)
	put(pkt, 16, binary.BigEndian.Uint16(d4[0:]), true)
	put(pkt, 18, binary.BigEndian.Uint16(d4[2:]), true)
	put(seg, 0, srcPort, false)
	put(seg, 2, dstPort, false)

	binary.BigEndian.PutUint16(pkt[10:12], ipSum)
	binary.BigEndian.PutUint16(seg[16:18], tcpSum)
}

// adjust updates a one's-complement checksum for one changed 16-bit word.
func adjust(sum, old, new uint16) uint16 {
	s := uint32(^sum) + uint32(^old) + uint32(new)
	s = (s & 0xffff) + (s >> 16)
	s = (s & 0xffff) + (s >> 16)
	return ^uint16(s)
}

// tcpSegment4 builds an IPv4 TCP segment with no payload.
func tcpSegment4(src netip.Addr, srcPort uint16, dst netip.Addr, dstPort uint16, seq, ack uint32, flags byte) []byte {
	pkt := make([]byte, 40)
	pkt[0] = 0x45
	binary.BigEndian.PutUint16(pkt[2:], 40)
	pkt[8] = 64
	pkt[9] = unix.IPPROTO_TCP
	s4, d4 := src.As4(), dst.As4()
	copy(pkt[12:16], s4[:])
	copy(pkt[16:20], d4[:])
	binary.BigEndian.PutUint16(pkt[10:], checksum(pkt[:20], 0))

	seg := pkt[20:]
	binary.BigEndian.PutUint16(seg[0:], srcPort)
	binary.BigEndian.PutUint16(seg[2:], dstPort)
	binary.BigEndian.PutUint32(seg[4:], seq)
	binary.BigEndian.PutUint32(seg[8:], ack)
	seg[12] = 5 << 4
	seg[13] = flags
	binary.BigEndian.PutUint16(seg[14:], 65535)
	binary.BigEndian.PutUint16(seg[16:], checksum(seg, pseudoHeader4(pkt, 20)))
	return pkt
}

func pseudoHeader4(pkt []byte, tcpLen int) uint32 {
	sum := uint32(0)
	for i := 12; i < 20; i += 2 {
		sum += uint32(binary.BigEndian.Uint16(pkt[i:]))
	}
	return sum + unix.IPPROTO_TCP + uint32(tcpLen)
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

// flowHash gives every packet of one flow (same addresses and ports) the same value.
func flowHash(p []byte) uint32 {
	var h uint32
	switch {
	case len(p) >= 24 && p[0]>>4 == 4:
		ihl := int(p[0]&0x0f) * 4
		h = binary.BigEndian.Uint32(p[12:16]) ^ binary.BigEndian.Uint32(p[16:20])
		if len(p) >= ihl+4 {
			h ^= binary.BigEndian.Uint32(p[ihl : ihl+4])
		}
	case len(p) >= 44 && p[0]>>4 == 6:
		for i := 8; i < 40; i += 4 {
			h ^= binary.BigEndian.Uint32(p[i : i+4])
		}
		h ^= binary.BigEndian.Uint32(p[40:44])
	}
	h ^= h >> 16
	h *= 0x45d9f3b
	h ^= h >> 16
	return h
}

// rewrite6 is rewrite4 for IPv6 TCP (no IP header checksum; the TCP checksum
// covers both addresses through the pseudo-header).
func rewrite6(pkt []byte, src netip.Addr, srcPort uint16, dst netip.Addr, dstPort uint16) {
	seg := pkt[40:]
	sum := binary.BigEndian.Uint16(seg[16:18])
	put := func(b []byte, off int, v uint16) {
		sum = adjust(sum, binary.BigEndian.Uint16(b[off:]), v)
		binary.BigEndian.PutUint16(b[off:], v)
	}
	s16, d16 := src.As16(), dst.As16()
	for i := 0; i < 16; i += 2 {
		put(pkt, 8+i, binary.BigEndian.Uint16(s16[i:]))
		put(pkt, 24+i, binary.BigEndian.Uint16(d16[i:]))
	}
	put(seg, 0, srcPort)
	put(seg, 2, dstPort)
	binary.BigEndian.PutUint16(seg[16:18], sum)
}

// tcpSegment6 builds an IPv6 TCP segment with no payload.
func tcpSegment6(src netip.Addr, srcPort uint16, dst netip.Addr, dstPort uint16, seq, ack uint32, flags byte) []byte {
	pkt := make([]byte, 60)
	pkt[0] = 0x60
	binary.BigEndian.PutUint16(pkt[4:], 20)
	pkt[6] = unix.IPPROTO_TCP
	pkt[7] = 64
	s16, d16 := src.As16(), dst.As16()
	copy(pkt[8:24], s16[:])
	copy(pkt[24:40], d16[:])
	seg := pkt[40:]
	binary.BigEndian.PutUint16(seg[0:], srcPort)
	binary.BigEndian.PutUint16(seg[2:], dstPort)
	binary.BigEndian.PutUint32(seg[4:], seq)
	binary.BigEndian.PutUint32(seg[8:], ack)
	seg[12] = 5 << 4
	seg[13] = flags
	binary.BigEndian.PutUint16(seg[14:], 65535)
	binary.BigEndian.PutUint16(seg[16:], checksum(seg, pseudoHeader6(pkt, unix.IPPROTO_TCP, 20)))
	return pkt
}

func pseudoHeader6(pkt []byte, proto byte, length int) uint32 {
	sum := uint32(0)
	for i := 8; i < 40; i += 2 {
		sum += uint32(binary.BigEndian.Uint16(pkt[i:]))
	}
	return sum + uint32(proto) + uint32(length)
}

// udpHeaderRoom is how much space udpPacket needs in front of the payload.
func udpHeaderRoom(v6 bool) int {
	if v6 {
		return 48
	}
	return 28
}

// udpPacket writes IP and UDP headers from src to dst in front of the n-byte
// payload that already sits at buf[udpHeaderRoom:], and returns the packet.
func udpPacket(buf []byte, src, dst netip.AddrPort, n int) []byte {
	v6 := src.Addr().Is6() && !src.Addr().Is4In6()
	room := udpHeaderRoom(v6)
	pkt := buf[:room+n]
	udp := pkt[room-8:]
	binary.BigEndian.PutUint16(udp[0:], src.Port())
	binary.BigEndian.PutUint16(udp[2:], dst.Port())
	binary.BigEndian.PutUint16(udp[4:], uint16(8+n))
	udp[6], udp[7] = 0, 0
	var pseudo uint32
	if v6 {
		for i := range pkt[:40] {
			pkt[i] = 0
		}
		pkt[0] = 0x60
		binary.BigEndian.PutUint16(pkt[4:], uint16(8+n))
		pkt[6] = unix.IPPROTO_UDP
		pkt[7] = 64
		s16, d16 := src.Addr().As16(), dst.Addr().As16()
		copy(pkt[8:24], s16[:])
		copy(pkt[24:40], d16[:])
		pseudo = pseudoHeader6(pkt, unix.IPPROTO_UDP, 8+n)
	} else {
		for i := range pkt[:20] {
			pkt[i] = 0
		}
		pkt[0] = 0x45
		binary.BigEndian.PutUint16(pkt[2:], uint16(room+n))
		pkt[8] = 64
		pkt[9] = unix.IPPROTO_UDP
		s4, d4 := src.Addr().Unmap().As4(), dst.Addr().Unmap().As4()
		copy(pkt[12:16], s4[:])
		copy(pkt[16:20], d4[:])
		binary.BigEndian.PutUint16(pkt[10:], checksum(pkt[:20], 0))
		for i := 12; i < 20; i += 2 {
			pseudo += uint32(binary.BigEndian.Uint16(pkt[i:]))
		}
		pseudo += unix.IPPROTO_UDP + uint32(8+n)
	}
	sum := checksum(udp, pseudo)
	if sum == 0 {
		sum = 0xffff // zero means "no checksum" in UDP
	}
	binary.BigEndian.PutUint16(udp[6:], sum)
	return pkt
}
