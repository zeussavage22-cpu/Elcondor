#!/usr/bin/env python3
"""UDP check for the lab. server HOST PORT: echo forever.
client HOST PORT COUNT: send COUNT datagrams of mixed sizes, verify the echoes,
print how many came back intact, and exit 1 if fewer than 98%."""
import hashlib, os, socket, sys, threading, time

def server(host, port):
    fam = socket.AF_INET6 if ":" in host else socket.AF_INET
    s = socket.socket(fam, socket.SOCK_DGRAM)
    s.bind((host, port))
    while True:
        data, addr = s.recvfrom(65535)
        s.sendto(data, addr)

def client(host, port, count):
    fam = socket.AF_INET6 if ":" in host else socket.AF_INET
    s = socket.socket(fam, socket.SOCK_DGRAM)
    s.setsockopt(socket.SOL_SOCKET, socket.SO_RCVBUF, 4 << 20)
    s.connect((host, port))
    s.settimeout(0.5)
    sent = {}
    got = set()
    done = threading.Event()

    def receive():  # read while sending, so our own buffer never overflows
        deadline = None
        while len(got) < count:
            if done.is_set():
                deadline = deadline or time.time() + 5
                if time.time() > deadline:
                    break
            try:
                data = s.recv(65535)
            except socket.timeout:
                continue
            i = int.from_bytes(data[:4], "big")
            if sent.get(i) == data and hashlib.sha256(data[36:]).digest() == data[4:36]:
                got.add(i)

    reader = threading.Thread(target=receive)
    reader.start()
    for i in range(count):
        body = os.urandom(40 + (i * 37) % 1360)
        msg = i.to_bytes(4, "big") + hashlib.sha256(body).digest() + body
        sent[i] = msg
        s.send(msg)
        if i % 50 == 49:
            time.sleep(0.005)  # don't overflow the lab's queues
    done.set()
    reader.join()
    print(f"{len(got)}/{count} datagrams intact via {host}")
    sys.exit(0 if len(got) >= count * 0.98 else 1)

if __name__ == "__main__":
    mode, host, port = sys.argv[1], sys.argv[2], int(sys.argv[3])
    server(host, port) if mode == "server" else client(host, port, int(sys.argv[4]))
