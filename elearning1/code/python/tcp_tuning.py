import argparse
import socket
import sys
import time


def set_common_opts(sock: socket.socket):
    try:
        sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
    except Exception:
        pass
    try:
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_KEEPALIVE, 1)
    except Exception:
        pass
    try:
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_SNDBUF, 1 << 16)
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_RCVBUF, 1 << 16)
    except Exception:
        pass
    # Optional Linux-only tunings
    for optname, level in (("TCP_QUICKACK", socket.IPPROTO_TCP), ("TCP_USER_TIMEOUT", socket.IPPROTO_TCP)):
        try:
            opt = getattr(socket, optname)
            if optname == "TCP_USER_TIMEOUT":
                sock.setsockopt(level, opt, 30_000)  # 30s
            else:
                sock.setsockopt(level, opt, 1)
        except Exception:
            continue


def run_server(host: str, port: int):
    srv = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
        try:
            srv.setsockopt(socket.SOL_SOCKET, getattr(socket, "SO_REUSEPORT"), 1)
        except Exception:
            pass
        srv.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        srv.bind((host, port))
        srv.listen(128)
        print(f"Server listening on {host}:{port}")
        while True:
            conn, addr = srv.accept()
            set_common_opts(conn)
            conn.settimeout(20)
            try:
                data = conn.recv(65536)
                if not data:
                    conn.close()
                    continue
                conn.sendall(data)
            finally:
                conn.close()
    finally:
        srv.close()


def run_client(host: str, port: int, message: str):
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    set_common_opts(s)
    s.settimeout(10)
    start = time.time()
    s.connect((host, port))
    s.sendall(message.encode("utf-8"))
    echo = s.recv(65536)
    rtt_ms = (time.time() - start) * 1000
    print(f"Echo: {echo.decode('utf-8', 'ignore')}")
    print(f"Approx RTT: {rtt_ms:.2f} ms")
    s.close()


if __name__ == "__main__":
    p = argparse.ArgumentParser()
    p.add_argument("mode", choices=["server", "client"]) 
    p.add_argument("host")
    p.add_argument("port", type=int)
    p.add_argument("message", nargs="?", default="hello tcp")
    args = p.parse_args()
    if args.mode == "server":
        run_server(args.host, args.port)
    else:
        run_client(args.host, args.port, args.message)
