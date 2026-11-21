#!/bin/bash
# Lab that mirrors Android tethering through a test-network TUN:
#   cli (hotspot client) --veth--> phone (NAT + policy route into tun0) --[engine]--> srv (internet)
set -e
case "$1" in
up)
  for n in cli phone srv; do ip netns add $n; done
  ip link add c0 netns cli type veth peer name c1 netns phone
  ip link add s0 netns srv type veth peer name s1 netns phone
  ip -n cli addr add 192.168.43.10/24 dev c0; ip -n cli link set c0 up; ip -n cli link set lo up
  ip -n cli route add default via 192.168.43.1
  ip -n phone addr add 192.168.43.1/24 dev c1; ip -n phone link set c1 up; ip -n phone link set lo up
  ip -n phone addr add 203.0.113.1/24 dev s1; ip -n phone link set s1 up
  ip -n srv addr add 203.0.113.2/24 dev s0; ip -n srv link set s0 up; ip -n srv link set lo up
  ip -n srv route add default via 203.0.113.1
  ip netns exec phone sysctl -qw net.ipv4.ip_forward=1
  ip netns exec phone sysctl -qw net.ipv4.conf.all.rp_filter=0
  # The TUN the engine owns (Android: TestNetworkManager.createTunInterface)
  ip netns exec phone ip tuntap add dev tun0 mode tun
  ip -n phone addr add 10.0.0.2/24 dev tun0; ip -n phone link set tun0 up
  ip netns exec phone sysctl -qw net.ipv4.conf.tun0.rp_filter=0
  # Tethering: client traffic goes out the preferred upstream (the TUN), masqueraded
  ip -n phone rule add from 192.168.43.0/24 lookup 100
  ip -n phone route add default dev tun0 table 100
  ip netns exec phone iptables -t nat -A POSTROUTING -s 192.168.43.0/24 -o tun0 -j MASQUERADE
  # IPv6: the client network is NATed into the TUN the same way, so the engine
  # sees the TUN's own address as the source, as with IPv4.
  if [ -d /proc/sys/net/ipv6 ]; then
  for n in cli phone srv; do ip netns exec $n sysctl -qw net.ipv6.conf.all.accept_dad=0 net.ipv6.conf.default.accept_dad=0; done
  ip netns exec phone sysctl -qw net.ipv6.conf.all.forwarding=1
  ip -n cli addr add fd43::10/64 dev c0 nodad; ip -n cli -6 route add default via fd43::1
  ip -n phone addr add fd43::1/64 dev c1 nodad
  ip -n phone addr add fd13::1/64 dev s1 nodad
  ip -n srv addr add fd13::2/64 dev s0 nodad; ip -n srv -6 route add default via fd13::1
  ip -n phone addr add 2001:db8::2/64 dev tun0 nodad
  ip -n phone -6 rule add from fd43::/64 lookup 100
  ip -n phone -6 route add default dev tun0 table 100
  ip netns exec phone ip6tables -t nat -A POSTROUTING -s fd43::/64 -o tun0 -j MASQUERADE
  fi
  ;;
down)
  for n in cli phone srv; do ip netns del $n 2>/dev/null || true; done
  ;;
esac
