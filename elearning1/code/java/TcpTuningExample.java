import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TcpTuningExample {
    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: java TcpTuningExample <host> <port> <message>");
            return;
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String message = args[2];

        Socket socket = new Socket();
        // Tuning options
        socket.setTcpNoDelay(true);
        socket.setKeepAlive(true);
        socket.setSendBufferSize(1 << 16);
        socket.setReceiveBufferSize(1 << 16);
        try {
            socket.setTrafficClass(0x10); // low delay DSCP (best-effort networks may ignore)
        } catch (Exception ignored) {}

        long start = System.nanoTime();
        socket.connect(new InetSocketAddress(host, port), 5000);

        OutputStream out = socket.getOutputStream();
        InputStream in = socket.getInputStream();
        out.write(message.getBytes("UTF-8"));
        out.flush();
        byte[] buf = new byte[65536];
        int n = in.read(buf);
        long rttMicros = (System.nanoTime() - start) / 1000;
        System.out.println("Echo: " + new String(buf, 0, Math.max(0, n), "UTF-8"));
        System.out.printf("Approx RTT: %.2f ms\n", rttMicros / 1000.0);
        socket.close();
    }
}
