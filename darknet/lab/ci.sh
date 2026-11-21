#!/bin/bash
# Runs the engine in a network lab that mirrors Android tethering and checks
# that data arrives intact on both paths. Needs root (netns, TUN, iptables).
set -euo pipefail
cd "$(dirname "$0")"
LAB=$(pwd)
ENGINE=${1:?usage: ci.sh <labengine binary>}
# IPv6 checks need a kernel with IPv6 (GitHub's runners have it).
V6=false; [ -d /proc/sys/net/ipv6 ] && V6=true
V6FLAGS=(); $V6 && V6FLAGS=(-addr6 2001:db8::2 -fake6 2001:db8::3)

./lab.sh down || true
./lab.sh up

head -c 50000000 /dev/urandom > "$LAB/blob.bin"
WANT=$(sha256sum "$LAB/blob.bin" | cut -c1-64)
ip netns exec srv python3 -m http.server 8080 --bind 203.0.113.2 --directory "$LAB" >/dev/null 2>&1 &
HTTP=$!
ip netns exec srv python3 "$LAB/udpcheck.py" server 203.0.113.2 9000 &
PIDS="$HTTP $!"
if $V6; then
  ip netns exec srv python3 -m http.server 8080 --bind fd13::2 --directory "$LAB" >/dev/null 2>&1 &
  PIDS="$PIDS $!"
  ip netns exec srv python3 "$LAB/udpcheck.py" server fd13::2 9000 &
  PIDS="$PIDS $!"
fi
for _ in $(seq 1 40); do ip netns exec phone curl -s -m 1 -o /dev/null http://203.0.113.2:8080/ && break; sleep 0.25; done
if $V6; then for _ in $(seq 1 40); do ip netns exec phone curl -s -m 1 -o /dev/null "http://[fd13::2]:8080/" && break; sleep 0.25; done; fi

check() { # mode
  # -nonblock: Android hands the TUN over non-blocking.
  ip netns exec phone "$ENGINE" -fast="$1" -nonblock=true "${V6FLAGS[@]}" -cc bbr > "$LAB/engine.log" 2>&1 &
  local pid=$!
  sleep 3
  cat "$LAB/engine.log"
  local t0 t1 idle
  t0=$(awk '{print $14+$15}' /proc/$pid/stat); sleep 4; t1=$(awk '{print $14+$15}' /proc/$pid/stat)
  idle=$(( (t1 - t0) * 100 / 400 ))
  echo "idle CPU: ${idle}% of one core"
  [ "$idle" -le 10 ] || { echo "FAIL: engine burns CPU while idle (fast=$1)"; kill $pid; exit 1; }
  if [ "$1" = true ]; then
    wants=("engine up, fast path active: true" "udp relay active: true")
    $V6 && wants+=("ipv6 fast path active: true")
    for want in "${wants[@]}"; do
      grep -q "$want" "$LAB/engine.log" || { echo "FAIL: expected '$want'"; kill $pid; exit 1; }
    done
  fi
  local got
  got=$(ip netns exec cli curl -s -m 60 http://203.0.113.2:8080/blob.bin | sha256sum | cut -c1-64)
  [ "$got" = "$WANT" ] || { echo "FAIL: download corrupted (fast=$1)"; kill $pid; exit 1; }
  ip netns exec cli python3 "$LAB/udpcheck.py" client 203.0.113.2 9000 2000 || { echo "FAIL: UDP IPv4 (fast=$1)"; kill $pid; exit 1; }
  if $V6; then
    got=$(ip netns exec cli curl -s -m 60 "http://[fd13::2]:8080/blob.bin" | sha256sum | cut -c1-64)
    [ "$got" = "$WANT" ] || { echo "FAIL: IPv6 download corrupted (fast=$1)"; kill $pid; exit 1; }
    ip netns exec cli python3 "$LAB/udpcheck.py" client fd13::2 9000 2000 || { echo "FAIL: UDP IPv6 (fast=$1)"; kill $pid; exit 1; }
  else
    echo "(no IPv6 in this kernel: IPv6 checks skipped)"
  fi
  local ok
  ok=$(seq 1 100 | xargs -P 20 -I{} ip netns exec cli curl -s -m 10 -o /dev/null -w "%{http_code}\n" http://203.0.113.2:8080/ | grep -c 200 || true)
  [ "$ok" = 100 ] || { echo "FAIL: $ok/100 parallel requests (fast=$1)"; kill $pid; exit 1; }
  echo "OK fast=$1: 50 MB intact, UDP intact, 100/100 parallel requests (IPv6 checked: $V6)"
  kill -INT $pid; wait $pid 2>/dev/null || true
  grep "stats:" "$LAB/engine.log" || true
  sleep 1
}

check true
check false
kill $PIDS
./lab.sh down
