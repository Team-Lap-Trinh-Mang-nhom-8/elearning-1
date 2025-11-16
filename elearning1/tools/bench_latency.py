import argparse
import socket
import time
import statistics as stats
import os


def measure_rtt(host: str, port: int, payload: bytes, timeout: float = 5.0) -> float:
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.settimeout(timeout)
    start = time.perf_counter()
    s.connect((host, port))
    s.sendall(payload)
    _ = s.recv(65536)
    dt = time.perf_counter() - start
    s.close()
    return dt * 1000.0  # ms


def main():
    ap = argparse.ArgumentParser(description="Latency benchmark against echo server")
    ap.add_argument("host")
    ap.add_argument("port", type=int)
    ap.add_argument("--runs", type=int, default=50, help="Number of requests")
    ap.add_argument("--size", type=int, default=16, help="Payload size in bytes")
    ap.add_argument("--timeout", type=float, default=5.0, help="Socket timeout (seconds)")
    ap.add_argument("--csv", type=str, default=None, help="Optional CSV output path")
    args = ap.parse_args()

    payload = os.urandom(max(1, args.size))

    rtts = []
    for i in range(args.runs):
        try:
            rtt = measure_rtt(args.host, args.port, payload, args.timeout)
            rtts.append(rtt)
        except Exception as e:
            print(f"Run {i+1} failed: {e}")

    if not rtts:
        print("No successful runs.")
        return 1

    rtts_sorted = sorted(rtts)
    def pct(p):
        k = max(0, min(len(rtts_sorted) - 1, int(round((p/100.0)*(len(rtts_sorted)-1)))))
        return rtts_sorted[k]

    print(f"Runs: {len(rtts)} | Size: {args.size} bytes")
    print(f"min: {min(rtts):.3f} ms, p50: {stats.median(rtts):.3f} ms, p95: {pct(95):.3f} ms, p99: {pct(99):.3f} ms, max: {max(rtts):.3f} ms, mean: {stats.mean(rtts):.3f} ms")

    if args.csv:
        try:
            with open(args.csv, "w", encoding="utf-8") as f:
                f.write("run,rtt_ms\n")
                for i, r in enumerate(rtts, 1):
                    f.write(f"{i},{r:.3f}\n")
            print(f"Wrote CSV to {args.csv}")
        except Exception as e:
            print(f"Failed to write CSV: {e}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
