import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.security.SecureRandom;

public class TcpLatencyBench {
    static class Args {
        String host;
        int port;
        int runs = 50;
        int size = 16;
        double timeoutSec = 5.0;
        String csv = null;
        boolean tuned = false;
    }

    public static void main(String[] argv) throws Exception {
        Locale.setDefault(Locale.US);
        Args args = parse(argv);
        if (args == null) {
            printUsage();
            return;
        }

        byte[] payload = new byte[Math.max(1, args.size)];
        new SecureRandom().nextBytes(payload);

        List<Double> rtts = new ArrayList<>();
        for (int i = 0; i < args.runs; i++) {
            try {
                double rtt = measureOnce(args.host, args.port, payload, args.timeoutSec, args.tuned);
                rtts.add(rtt);
            } catch (Exception e) {
                System.out.println("Run " + (i + 1) + " failed: " + e.getMessage());
            }
        }

        if (rtts.isEmpty()) {
            System.out.println("No successful runs.");
            return;
        }

        double[] arr = rtts.stream().mapToDouble(Double::doubleValue).toArray();
        Arrays.sort(arr);
        System.out.printf("Runs: %d | Size: %d bytes | Tuned: %s\n", arr.length, args.size, args.tuned);
        System.out.printf(
                "min: %.3f ms, p50: %.3f ms, p95: %.3f ms, p99: %.3f ms, max: %.3f ms, mean: %.3f ms\n",
                arr[0], pct(arr, 50), pct(arr, 95), pct(arr, 99), arr[arr.length - 1], mean(arr));

        if (args.csv != null) {
            try (FileWriter fw = new FileWriter(args.csv)) {
                fw.write("run,rtt_ms\n");
                for (int i = 0; i < arr.length; i++) {
                    fw.write((i + 1) + "," + String.format(Locale.US, "%.3f", arr[i]) + "\n");
                }
            }
            System.out.println("Wrote CSV to " + args.csv);
        }
    }

    static double measureOnce(String host, int port, byte[] payload, double timeoutSec, boolean tuned) throws Exception {
        Socket s = new Socket();
        if (tuned) applyTunings(s);
        int timeoutMs = (int) Math.max(1, Math.round(timeoutSec * 1000.0));
        long start = System.nanoTime();
        s.connect(new InetSocketAddress(host, port), timeoutMs);
        s.setSoTimeout(timeoutMs);
        OutputStream out = s.getOutputStream();
        InputStream in = s.getInputStream();
        out.write(payload);
        out.flush();
        byte[] buf = new byte[65536];
        int n = in.read(buf);
        if (n < 0) throw new RuntimeException("EOF");
        double ms = (System.nanoTime() - start) / 1_000_000.0;
        s.close();
        return ms;
    }

    static void applyTunings(Socket socket) {
        try { socket.setTcpNoDelay(true); } catch (Exception ignored) {}
        try { socket.setKeepAlive(true); } catch (Exception ignored) {}
        try { socket.setSendBufferSize(1 << 16); } catch (Exception ignored) {}
        try { socket.setReceiveBufferSize(1 << 16); } catch (Exception ignored) {}
        try { socket.setTrafficClass(0x10); } catch (Exception ignored) {}
    }

    static double pct(double[] arr, int p) {
        int idx = Math.max(0, Math.min(arr.length - 1, (int) Math.round((p / 100.0) * (arr.length - 1))));
        return arr[idx];
    }

    static double mean(double[] arr) {
        double s = 0;
        for (double v : arr) s += v;
        return s / arr.length;
    }

    static void printUsage() {
        System.out.println("Usage: java TcpLatencyBench <host> <port> [--runs N] [--size BYTES] [--timeout SEC] [--csv PATH] [--tuned true|false]");
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
            String key = k;
            String val = (i < argv.length) ? argv[i] : null;
            if ("--runs".equals(key) && val != null) { a.runs = Integer.parseInt(val); i++; }
            else if ("--size".equals(key) && val != null) { a.size = Integer.parseInt(val); i++; }
            else if ("--timeout".equals(key) && val != null) { a.timeoutSec = Double.parseDouble(val); i++; }
            else if ("--csv".equals(key) && val != null) { a.csv = val; i++; }
            else if ("--tuned".equals(key) && val != null) { a.tuned = Boolean.parseBoolean(val); i++; }
        }
        return a;
    }
}
