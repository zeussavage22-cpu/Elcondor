#!/bin/bash
# usage: bench.sh <engine-binary> <label>
L=$(dirname $0); cd $L
pkill iperf3; pkill -f "http.server 8080"; sleep 0.3
ip netns exec srv iperf3 -s -D
ip netns exec srv python3 -m http.server 8080 --bind 203.0.113.2 >/dev/null 2>&1 &
sleep 0.7
ip netns exec phone $1 > engine.log 2>&1 & EP=$!
sleep 1
cpu() { awk '{print $14+$15}' /proc/$EP/stat; }
run() { # name, iperf args
  local t0=$(cpu); local r=$(ip netns exec cli iperf3 -c 203.0.113.2 $2 -f m 2>&1 | grep -E "receiver" | tail -1 | awk '{for(i=1;i<=NF;i++) if($i=="Mbits/sec") print $(i-1)}')
  local t1=$(cpu); printf "%-28s %8s Mbit/s   cpu %4s ticks\n" "$1" "${r:-FAIL}" "$((t1-t0))"
}
echo "== $2"
run "TCP bajada 1 conexión" "-R -t 6"
run "TCP bajada 8 conexiones" "-R -t 6 -P 8"
run "TCP subida 1 conexión" "-t 6"
u=$(ip netns exec cli iperf3 -c 203.0.113.2 -u -b 400M -R -t 5 -f m 2>&1 | grep receiver | awk '{for(i=1;i<=NF;i++){if($i=="Mbits/sec") r=$(i-1); if($i ~ /%\)/) l=$i}} END{print r" Mbit/s, pérdida "l}')
echo "UDP bajada a 400 Mbit/s       $u"
t0=$(date +%s.%N); h=$(ip netns exec cli curl -s http://203.0.113.2:8080/blob.bin | sha256sum | cut -c1-16); t1=$(date +%s.%N)
[ "$h" = "$(cat blob.sha)" ] && echo "Integridad 200 MB por HTTP     OK ($(python3 -c "print(round($t1-$t0,2))") s)" || echo "Integridad 200 MB por HTTP     CORRUPTO"
ok=0; for i in $(seq 1 200); do ip netns exec cli curl -s -m 5 -o /dev/null -w "%{http_code}" http://203.0.113.2:8080/blob.sha 2>/dev/null | grep -q 200 && ok=$((ok+1)); done
echo "200 conexiones cortas seguidas $ok/200 OK"
kill -INT $EP; wait $EP 2>/dev/null
pkill iperf3; pkill -f "http.server 8080"
