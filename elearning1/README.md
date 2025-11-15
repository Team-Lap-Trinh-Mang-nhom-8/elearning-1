# Tối ưu hóa TCP — Bảng so sánh và ví dụ mã nguồn

Nội dung bao gồm:
- File Excel so sánh các kỹ thuật tối ưu TCP: `assets/tcp_optimization_comparison.xlsx`.
- 5 ví dụ mã nguồn cho các ngôn ngữ: Python, Java, C#, Go, Node.js minh họa các thiết lập phổ biến: TCP_NODELAY, KeepAlive, kích thước buffer, timeout, DSCP...


## Chạy các ví dụ

Trước tiên chạy server tối giản bằng Python (echo) rồi chạy client ở các ngôn ngữ khác để quan sát RTT.

```powershell
# Server echo (Python)
python .\code\python\tcp_tuning.py server 127.0.0.1 9000
```

Sau đó ở cửa sổ khác, chạy từng client:

```powershell
# Python client
python .\code\python\tcp_tuning.py client 127.0.0.1 9000 "hello tcp"

# Java client (cần JDK)
javac .\code\java\TcpTuningExample.java
java -cp .\code\java TcpTuningExample 127.0.0.1 9000 "hello tcp"

# C# client (cần .NET SDK)
dotnet new console -n TcpClientTemp -o .\code\csharp\tmp | Out-Null
Copy-Item .\code\csharp\TcpTuningExample.cs .\code\csharp\tmp\Program.cs -Force
Push-Location .\code\csharp\tmp; dotnet run -- 127.0.0.1 9000 "hello tcp"; Pop-Location

# Go client (cần Go)
go run .\code\go\tcp_tuning.go 127.0.0.1 9000 "hello tcp"

# Node.js client (cần Node)
node .\code\nodejs\tcp_tuning.js 127.0.0.1 9000 "hello tcp"
```



## 2 file Benchmark dùng để so sánh tốc độ của phương pháp tối ưu TCP

Để thấy khác biệt , dùng benchmark :

```powershell
# Latency benchmark: gửi N lần gói nhỏ, tính p95/p99
python .\tools\bench_latency.py 127.0.0.1 9000 --runs 200 --size 16
python .\tools\bench_latency.py 127.0.0.1 9001 --runs 200 --size 16

# Throughput benchmark (lặp nhiều gói ~32KB)
python .\tools\bench_throughput.py 127.0.0.1 9000 --bytes 50000000 --chunk 32768
python .\tools\bench_throughput.py 127.0.0.1 9001 --bytes 50000000 --chunk 32768
```

Mẹo để tạo khác biệt rõ rệt:
- Tắt/đổi `TCP_NODELAY` tác động lên gói rất nhỏ và workload request/response; trên localhost độ trễ nền rất thấp nên chênh lệch nhỏ.
- Tăng `--runs` và `--size` phù hợp với trường hợp; hoặc thử chạy qua mạng thật (không phải localhost) hay thêm độ trễ nhân tạo.
- Với throughput, thử tăng `--bytes` và kích hoạt tự động tăng buffer của HĐH; trên đường truyền có BDP lớn sẽ thấy khác biệt hơn.

## Kỹ thuật nổi bật trong Excel
- TCP_NODELAY: giảm độ trễ cho gói nhỏ; đổi lại tăng overhead gói.
- SO_KEEPALIVE: phát hiện peer chết; cần tinh chỉnh timer ở OS.
- SO_SNDBUF/SO_RCVBUF: tối ưu throughput cho đường truyền độ trễ lớn.
- Congestion control (TCP_CONGESTION): chọn thuật toán (cubic/BBR) — khả dụng theo OS.
- TCP Fast Open: giảm một RTT khi kết nối lại — phụ thuộc cấu hình hệ thống.
- SO_REUSEPORT: mở rộng đa lõi ở server accept path (Linux/BSD/macOS).
- DSCP/TOS: đánh dấu QoS ở mạng có hỗ trợ.
- TCP_USER_TIMEOUT: fail nhanh khi dữ liệu không được ACK (Linux).

Ghi chú: Một số tuỳ chọn phụ thuộc HĐH. Trong mã, các lệnh `setsockopt` có `try/catch` để không làm hỏng chương trình trên hệ không hỗ trợ.
