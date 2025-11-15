package main

import (
	"bufio"
	"fmt"
	"net"
	"os"
	"time"
)

func main() {
	if len(os.Args) < 4 {
		fmt.Println("Usage: go run tcp_tuning.go <host> <port> <message>")
		return
	}
	host := os.Args[1]
	port := os.Args[2]
	message := os.Args[3]

	addr, err := net.ResolveTCPAddr("tcp", net.JoinHostPort(host, port))
	if err != nil {
		panic(err)
	}

	conn, err := net.DialTCP("tcp", nil, addr)
	if err != nil {
		panic(err)
	}
	defer conn.Close()

	conn.SetNoDelay(true)
	conn.SetKeepAlive(true)
	conn.SetKeepAlivePeriod(30 * time.Second)
	conn.SetReadBuffer(1 << 16)
	conn.SetWriteBuffer(1 << 16)

	start := time.Now()
	if _, err := conn.Write([]byte(message)); err != nil {
		panic(err)
	}
	conn.SetReadDeadline(time.Now().Add(10 * time.Second))
	r := bufio.NewReader(conn)
	resp, err := r.ReadString('\n')
	if err != nil {
		// Try reading bytes if newline wasn't sent
		b := make([]byte, 1024)
		n, _ := conn.Read(b)
		resp = string(b[:n])
	}
	fmt.Printf("Echo: %s\n", resp)
	fmt.Printf("Approx RTT: %.2f ms\n", time.Since(start).Seconds()*1000)
}
