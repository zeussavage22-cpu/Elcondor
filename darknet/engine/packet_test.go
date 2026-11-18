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
