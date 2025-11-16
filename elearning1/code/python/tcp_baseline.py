import argparse
import socket
import time


def run_server(host: str, port: int):
    srv = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    # Không đặt bất kỳ tùy chọn tối ưu TCP nào (giữ mặc định hệ điều hành)
    srv.bind((host, port))
    srv.listen(128)
    print(f"Baseline server listening on {host}:{port}")
    try:
        while True:
            conn, addr = srv.accept()
            # Không set TCP_NODELAY, KeepAlive, buffers...
            try:
                data = conn.recv(65536)
                if data:
                    conn.sendall(data)
            finally:
                conn.close()
    finally:
        srv.close()


def run_client(host: str, port: int, message: str):
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    # Không đặt bất kỳ tùy chọn tối ưu TCP nào
    start = time.time()
    s.connect((host, port))
    s.sendall(message.encode("utf-8"))
    echo = s.recv(65536)
    rtt_ms = (time.time() - start) * 1000
    print(f"Echo: {echo.decode('utf-8', 'ignore')}")
    print(f"Approx RTT (baseline): {rtt_ms:.2f} ms")
    s.close()


if __name__ == "__main__":
    p = argparse.ArgumentParser(description="Baseline TCP without tunings")
    p.add_argument("mode", choices=["server", "client"]) 
    p.add_argument("host")
    p.add_argument("port", type=int)
    p.add_argument("message", nargs="?", default="hello tcp")
    args = p.parse_args()

    if args.mode == "server":
        run_server(args.host, args.port)
    else:
        run_client(args.host, args.port, args.message)
