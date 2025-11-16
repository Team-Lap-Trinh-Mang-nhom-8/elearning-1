using System;
using System.Net;
using System.Net.Sockets;
using System.Text;

class TcpTuningExample
{
    static void Main(string[] args)
    {
        if (args.Length < 3)
        {
            Console.WriteLine("Usage: dotnet run -- <host> <port> <message>");
            return;
        }
        string host = args[0];
        int port = int.Parse(args[1]);
        string message = args[2];

        var ip = Dns.GetHostEntry(host).AddressList[0];
        var endPoint = new IPEndPoint(ip, port);

        using var socket = new Socket(endPoint.AddressFamily, SocketType.Stream, ProtocolType.Tcp);
        socket.NoDelay = true;
        socket.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.KeepAlive, true);
        socket.SendBufferSize = 1 << 16;
        socket.ReceiveBufferSize = 1 << 16;
        try {
            socket.Ttl = 64; // example reachable option
        } catch {}

        var start = DateTime.UtcNow;
        socket.Connect(endPoint);

        var data = Encoding.UTF8.GetBytes(message);
        socket.Send(data);
        var buf = new byte[65536];
        int n = socket.Receive(buf);
        var rtt = DateTime.UtcNow - start;
        Console.WriteLine("Echo: " + Encoding.UTF8.GetString(buf, 0, n));
        Console.WriteLine($"Approx RTT: {rtt.TotalMilliseconds:F2} ms");
    }
}
