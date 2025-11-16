import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.Locale;

public class TcpThroughputBench {
    static class Args {
        String host;
        int port;
        long totalBytes = 50_000_000L;
        int chunk = 32_768;
        double timeoutSec = 10.0;
        boolean tuned = false;
    }

    public static void main(String[] argv) throws Exception {
        Locale.setDefault(Locale.US);
        Args args = parse(argv);
        if (args == null) {
            printUsage();
            return;
        }

        int chunk = Math.max(1, Math.min(args.chunk, 65536));
        long iters = Math.max(1, args.totalBytes / chunk);
        byte[] payload = new byte[chunk];
        new SecureRandom().nextBytes(payload);

        long total = 0;
        long t0 = System.nanoTime();
        for (long i = 0; i < iters; i++) {
            echoOnce(args.host, args.port, payload, args.timeoutSec, args.tuned);
            total += chunk;
        }
        double elapsed = (System.nanoTime() - t0) / 1_000_000_000.0;
        double mib = total / (1024.0 * 1024.0);
        double mibps = mib / elapsed;
        double mbitps = (mib * 8.0) / elapsed;
        System.out.printf("Transferred ~%.2f MiB in %.3f s | ~%.2f MiB/s | ~%.2f Mbit/s\n", mib, elapsed, mibps, mbitps);
        System.out.printf("Chunk: %d bytes, Iters: %d | Tuned: %s\n", chunk, (int) iters, args.tuned);
    }

    static void echoOnce(String host, int port, byte[] payload, double timeoutSec, boolean tuned) throws Exception {
        Socket s = new Socket();
        if (tuned) applyTunings(s);
        int timeoutMs = (int) Math.max(1, Math.round(timeoutSec * 1000.0));
        s.connect(new InetSocketAddress(host, port), timeoutMs);
        s.setSoTimeout(timeoutMs);
        OutputStream out = s.getOutputStream();
        InputStream in = s.getInputStream();
        out.write(payload);
        out.flush();
        byte[] buf = new byte[65536];
        int n = in.read(buf);
        if (n < 0) throw new RuntimeException("EOF");
        s.close();
    }

    static void applyTunings(Socket socket) {
        try { socket.setTcpNoDelay(true); } catch (Exception ignored) {}
        try { socket.setKeepAlive(true); } catch (Exception ignored) {}
        try { socket.setSendBufferSize(1 << 16); } catch (Exception ignored) {}
        try { socket.setReceiveBufferSize(1 << 16); } catch (Exception ignored) {}
        try { socket.setTrafficClass(0x10); } catch (Exception ignored) {}
    }

    static void printUsage() {
        System.out.println("Usage: java TcpThroughputBench <host> <port> [--bytes N] [--chunk BYTES] [--timeout SEC] [--tuned true|false]");
    }

    static Args parse(String[] argv) {
        if (argv.length < 2) return null;
        Args a = new Args();
        int i = 0;
        a.host = argv[i++];
        try { a.port = Integer.parseInt(argv[i++]); } catch (Exception e) { return null; }
        while (i < argv.length) {
            String k = argv[i++];
            if (!k.startsWith("--")) continue;
            String val = (i < argv.length) ? argv[i] : null;
            if ("--bytes".equals(k) && val != null) { a.totalBytes = Long.parseLong(val); i++; }
            else if ("--chunk".equals(k) && val != null) { a.chunk = Integer.parseInt(val); i++; }
            else if ("--timeout".equals(k) && val != null) { a.timeoutSec = Double.parseDouble(val); i++; }
            else if ("--tuned".equals(k) && val != null) { a.tuned = Boolean.parseBoolean(val); i++; }
        }
        return a;
    }
}
