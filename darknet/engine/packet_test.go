package engine

import (
	"encoding/binary"
	"net/netip"
	"testing"
)

func checksumsValid(t *testing.T, pkt []byte) {
	t.Helper()
	if checksum(pkt[:20], 0) != 0 {
		t.Fatal("IPv4 header checksum is wrong")
	}
	if checksum(pkt[20:], pseudoHeader4(pkt, len(pkt)-20)) != 0 {
		t.Fatal("TCP checksum is wrong")
	}
}

func withPayload(pkt []byte, payload string) []byte {
	pkt = append(pkt, payload...)
	binary.BigEndian.PutUint16(pkt[2:], uint16(len(pkt)))
	binary.BigEndian.PutUint16(pkt[10:], 0)
	binary.BigEndian.PutUint16(pkt[10:], checksum(pkt[:20], 0))
	binary.BigEndian.PutUint16(pkt[36:], 0)
	binary.BigEndian.PutUint16(pkt[36:], checksum(pkt[20:], pseudoHeader4(pkt, len(pkt)-20)))
	return pkt
}

func TestSegmentChecksums(t *testing.T) {
	checksumsValid(t, tcpSegment4(netip.MustParseAddr("192.0.2.3"), 19999, netip.MustParseAddr("192.0.2.2"), 40000, 1, 0, flagSYN))
}

func TestRewriteRoundTripKeepsChecksums(t *testing.T) {
	client, remote := netip.MustParseAddr("192.0.2.2"), netip.MustParseAddr("142.250.80.46")
	pkt := withPayload(tcpSegment4(client, 51234, remote, 443, 7, 9, 0x18), "hola darknet")
	checksumsValid(t, pkt)

	rewrite4(pkt, 20, netip.MustParseAddr("192.0.2.3"), 20001, client, 40000)
	checksumsValid(t, pkt)
	if binary.BigEndian.Uint16(pkt[20:]) != 20001 || binary.BigEndian.Uint16(pkt[22:]) != 40000 {
		t.Fatal("ports not rewritten")
	}
	rewrite4(pkt, 20, remote, 443, client, 51234)
	checksumsValid(t, pkt)
	if string(pkt[40:]) != "hola darknet" {
		t.Fatal("payload changed")
	}
}

func TestFlowHashStablePerFlow(t *testing.T) {
	a := tcpSegment4(netip.MustParseAddr("192.0.2.2"), 50000, netip.MustParseAddr("1.1.1.1"), 443, 1, 0, flagSYN)
	b := tcpSegment4(netip.MustParseAddr("192.0.2.2"), 50000, netip.MustParseAddr("1.1.1.1"), 443, 77, 3, flagACK)
	if flowHash(a) != flowHash(b) {
		t.Fatal("one flow hashed to two workers")
	}
}

func TestNatPortsUnique(t *testing.T) {
	f := &fastPath{byFlow: map[flow]*natEntry{}, byPort: map[uint16]*natEntry{}, nextNat: natFirst}
	seen := map[uint16]bool{}
	for i := 0; i < 5000; i++ {
		e := f.allocate(flow{src: netip.MustParseAddr("192.0.2.2"), dst: netip.MustParseAddr("8.8.8.8"), srcPort: uint16(i), dstPort: 53})
		if e == nil || seen[e.port] || e.port < natFirst || e.port > natLast {
			t.Fatalf("bad NAT port at %d", i)
		}
		seen[e.port] = true
	}
}

func TestStartRejectsBadInput(t *testing.T) {
	if _, err := Start(-1, 1500, "192.0.2.2", "192.0.2.3", true); err == nil {
		t.Fatal("negative fd accepted")
	}
	if _, err := Start(3, 100, "192.0.2.2", "192.0.2.3", true); err == nil {
		t.Fatal("tiny MTU accepted")
	}
}

func TestUDPPacketRoundTrip(t *testing.T) {
	for _, tc := range []struct{ src, dst string }{
		{"203.0.113.2:443", "10.0.0.2:5353"},
		{"[2001:db8:1::5]:443", "[2001:db8::2]:40000"},
	} {
		src, dst := netip.MustParseAddrPort(tc.src), netip.MustParseAddrPort(tc.dst)
		room := udpHeaderRoom(src.Addr().Is6())
		buf := make([]byte, room+1500)
		payload := []byte("hello quic, this payload has an odd length!")
		copy(buf[room:], payload)
		pkt := udpPacket(buf, src, dst, len(payload))

		key, got, ok := parseUDP(pkt)
		if !ok || key.src != src || key.dst != dst || string(got) != string(payload) {
			t.Fatalf("%s: parse gave %v %v %q", tc.src, ok, key, got)
		}
		udp := pkt[room-8:]
		var pseudo uint32
		if src.Addr().Is6() {
			pseudo = pseudoHeader6(pkt, 17, len(udp))
		} else {
			if checksum(pkt[:20], 0) != 0 {
				t.Fatalf("bad IPv4 header checksum")
			}
			for i := 12; i < 20; i += 2 {
				pseudo += uint32(binary.BigEndian.Uint16(pkt[i:]))
			}
			pseudo += 17 + uint32(len(udp))
		}
		if checksum(udp, pseudo) != 0 {
			t.Fatalf("%s: bad UDP checksum", tc.src)
		}
	}
}

func TestParseUDPRejectsFragmentsAndMulticast(t *testing.T) {
	src, dst := netip.MustParseAddrPort("10.0.0.2:5000"), netip.MustParseAddrPort("203.0.113.2:53")
	buf := make([]byte, 28+4)
	pkt := udpPacket(buf, src, dst, 4)
	frag := append([]byte(nil), pkt...)
	frag[6] = 0x20 // more fragments
	if _, _, ok := parseUDP(frag); ok {
		t.Fatal("fragment accepted")
	}
	mc := udpPacket(make([]byte, 32), src, netip.MustParseAddrPort("239.255.255.250:1900"), 4)
	if _, _, ok := parseUDP(mc); ok {
		t.Fatal("multicast accepted")
	}
}

func TestRewrite6KeepsChecksumValid(t *testing.T) {
	a, b := netip.MustParseAddr("2001:db8::2"), netip.MustParseAddr("2001:db8:5::9")
	pkt := tcpSegment6(a, 1234, b, 443, 7, 0, flagSYN)
	f, l := netip.MustParseAddr("2001:db8::3"), netip.MustParseAddr("2001:db8::2")
	rewrite6(pkt, f, 20001, l, 41000)
	if checksum(pkt[40:], pseudoHeader6(pkt, 6, 20)) != 0 {
		t.Fatal("checksum broken by rewrite6")
	}
	want := tcpSegment6(f, 20001, l, 41000, 7, 0, flagSYN)
	if string(want) != string(pkt) {
		t.Fatal("rewrite6 differs from a freshly built segment")
	}
}

func TestParseHandles(t *testing.T) {
	got := parseHandles(" 12, ,0,345 ")
	if len(got) != 2 || got[0] != 12 || got[1] != 345 {
		t.Fatalf("got %v", got)
	}
}
