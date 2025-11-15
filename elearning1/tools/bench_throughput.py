import argparse
import socket
import time
import os


def echo_once(host: str, port: int, payload: bytes, timeout: float) -> float:
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.settimeout(timeout)
    start = time.perf_counter()
    s.connect((host, port))
    s.sendall(payload)
    _ = s.recv(65536)
    dt = time.perf_counter() - start
    s.close()
    return dt


def main():
    ap = argparse.ArgumentParser(description="Throughput benchmark via repeated echo transfers")
    ap.add_argument("host")
    ap.add_argument("port", type=int)
    ap.add_argument("--bytes", type=int, default=50_000_000, help="Total bytes to transfer (approx)")
    ap.add_argument("--chunk", type=int, default=32_768, help="Bytes per request (<=64KB fits example server)")
    ap.add_argument("--timeout", type=float, default=10.0)
    args = ap.parse_args()

    chunk = max(1, min(args.chunk, 65536))
    iters = max(1, args.bytes // chunk)
    payload = os.urandom(chunk)

    total_bytes = 0
    start = time.perf_counter()
    for _ in range(iters):
        dt = echo_once(args.host, args.port, payload, args.timeout)
        total_bytes += chunk
    elapsed = time.perf_counter() - start

    mb = total_bytes / (1024.0 * 1024.0)
    mbps = (mb * 8) / elapsed
    print(f"Transferred ~{mb:.2f} MiB in {elapsed:.3f} s | ~{mb/elapsed:.2f} MiB/s | ~{mbps:.2f} Mbit/s")
    print(f"Chunk: {chunk} bytes, Iters: {iters}")


if __name__ == "__main__":
    main()
