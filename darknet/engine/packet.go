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
