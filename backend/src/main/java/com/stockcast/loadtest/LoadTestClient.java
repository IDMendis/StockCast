package com.stockcast.loadtest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Load Testing Client for StockCast
 * 
 * Simulates multiple concurrent clients connecting to the server,
 * subscribing to stock updates, and monitoring message delivery.
 * 
 * Usage:
 *   java -cp target/classes com.stockcast.loadtest.LoadTestClient <numClients> <tickers> [options]
 * 
 * Example:
 *   java -cp target/classes com.stockcast.loadtest.LoadTestClient 100 AAPL,GOOG -duration 60
 */
public class LoadTestClient {
    
    private final String host;
    private final int port;
    private final int numClients;
    private final String[] tickers;
    private final int durationSeconds;
    private final int delayMs;
    private final boolean verbose;
    
    // Metrics
    private final AtomicInteger connectedClients = new AtomicInteger(0);
    private final AtomicInteger failedClients = new AtomicInteger(0);
    private final AtomicLong messagesReceived = new AtomicLong(0);
    private final AtomicLong bytesReceived = new AtomicLong(0);
    private final AtomicInteger errors = new AtomicInteger(0);
    
    private ExecutorService executor;
    private final List<SimulatedClient> clients = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean running = false;

    public LoadTestClient(String host, int port, int numClients, String[] tickers, 
                         int durationSeconds, int delayMs, boolean verbose) {
        this.host = host;
        this.port = port;
        this.numClients = numClients;
        this.tickers = tickers;
        this.durationSeconds = durationSeconds;
        this.delayMs = delayMs;
        this.verbose = verbose;
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            printHelp();
            return;
        }

        try {
            int numClients = Integer.parseInt(args[0]);
            String[] tickers = args[1].split(",");
            
            String host = "localhost";
            int port = 9090;
            int duration = Integer.MAX_VALUE;
            int delay = 1000;
            boolean verbose = false;

            // Parse optional arguments
            for (int i = 2; i < args.length; i++) {
                switch (args[i]) {
                    case "-host":
                        if (i + 1 < args.length) host = args[++i];
                        break;
                    case "-port":
                        if (i + 1 < args.length) port = Integer.parseInt(args[++i]);
                        break;
                    case "-duration":
                        if (i + 1 < args.length) duration = Integer.parseInt(args[++i]);
                        break;
                    case "-delay":
                        if (i + 1 < args.length) delay = Integer.parseInt(args[++i]);
                        break;
                    case "-verbose":
                        verbose = true;
                        break;
                    case "-help":
                        printHelp();
                        return;
                }
            }

            LoadTestClient loadTest = new LoadTestClient(host, port, numClients, tickers, duration, delay, verbose);
            loadTest.run();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printHelp() {
        System.out.println("StockCast Load Test Client");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  java -cp target/classes com.stockcast.loadtest.LoadTestClient <numClients> <tickers> [options]");
        System.out.println();
        System.out.println("Arguments:");
        System.out.println("  numClients    Number of concurrent clients (required)");
        System.out.println("  tickers       Comma-separated list of tickers (required)");
        System.out.println("                Example: AAPL,GOOG,MSFT");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -host <hostname>    Server hostname (default: localhost)");
        System.out.println("  -port <port>        Server port (default: 9090)");
        System.out.println("  -duration <seconds> Test duration (default: infinite)");
        System.out.println("  -delay <ms>         Delay between operations (default: 1000ms)");
        System.out.println("  -verbose            Print detailed logs");
        System.out.println("  -help               Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  # Light load: 10 clients for 30 seconds");
        System.out.println("  java -cp target/classes com.stockcast.loadtest.LoadTestClient 10 AAPL -duration 30");
        System.out.println();
        System.out.println("  # Medium load: 100 clients, all tickers, 2 minutes");
        System.out.println("  java -cp target/classes com.stockcast.loadtest.LoadTestClient 100 AAPL,GOOG,MSFT,AMZN,TSLA -duration 120");
        System.out.println();
        System.out.println("  # Heavy load with verbose output");
        System.out.println("  java -cp target/classes com.stockcast.loadtest.LoadTestClient 500 AAPL -duration 180 -verbose");
    }

    private void run() {
        System.out.println("=====================================================");
        System.out.println("StockCast Load Test Client");
        System.out.println("=====================================================");
        System.out.println("Configuration:");
        System.out.println("  Server: " + host + ":" + port);
        System.out.println("  Clients: " + numClients);
        System.out.println("  Tickers: " + String.join(", ", tickers));
        System.out.println("  Duration: " + (durationSeconds == Integer.MAX_VALUE ? "infinite" : durationSeconds + "s"));
        System.out.println("  Verbose: " + verbose);
        System.out.println("=====================================================");
        System.out.println();

        running = true;
        executor = Executors.newFixedThreadPool(Math.min(numClients, 50));
        long startTime = System.currentTimeMillis();

        // Create and start clients
        System.out.println("Starting " + numClients + " clients...");
        for (int i = 0; i < numClients; i++) {
            SimulatedClient client = new SimulatedClient(i + 1, host, port, tickers, delayMs, verbose);
            clients.add(client);
            executor.submit(() -> {
                try {
                    client.run();
                    connectedClients.incrementAndGet();
                } catch (Exception e) {
                    failedClients.incrementAndGet();
                    if (verbose) {
                        System.err.println("Client error: " + e.getMessage());
                    }
                }
            });
        }

        // Monitor loop
        System.out.println();
        System.out.println("--- Load Test Running ---");
        System.out.println();

        long nextReportTime = System.currentTimeMillis() + 5000;

        while (running && (System.currentTimeMillis() - startTime) < (durationSeconds * 1000L)) {
            try {
                Thread.sleep(500);

                long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                
                if (System.currentTimeMillis() >= nextReportTime) {
                    System.out.printf("Uptime: %2ds | Connected: %3d | Messages: %6d | Errors: %3d%n",
                            elapsed,
                            connectedClients.get(),
                            messagesReceived.get(),
                            errors.get());
                    nextReportTime = System.currentTimeMillis() + 5000;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Shutdown
        System.out.println();
        System.out.println("--- Test Ended ---");
        System.out.println();
        
        running = false;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Print final report
        long totalDuration = System.currentTimeMillis() - startTime;
        printSummary(totalDuration);
    }

    private void printSummary(long totalDurationMs) {
        System.out.println("=====================================================");
        System.out.println("Load Test Summary");
        System.out.println("=====================================================");
        System.out.printf("Duration:           %.1f seconds%n", totalDurationMs / 1000.0);
        System.out.printf("Clients Connected:  %d/%d%n", connectedClients.get(), numClients);
        System.out.printf("Clients Failed:     %d%n", failedClients.get());
        System.out.printf("Total Messages:     %d%n", messagesReceived.get());
        System.out.printf("Total Bytes:        %.2f MB%n", bytesReceived.get() / 1024.0 / 1024.0);
        System.out.printf("Errors:             %d%n", errors.get());
        
        if (messagesReceived.get() > 0 && totalDurationMs > 0) {
            double msgPerSecond = (messagesReceived.get() * 1000.0) / totalDurationMs;
            double mbPerSecond = (bytesReceived.get() / 1024.0 / 1024.0) / (totalDurationMs / 1000.0);
            System.out.printf("Throughput:         %.0f msg/s, %.2f MB/s%n", msgPerSecond, mbPerSecond);
        }
        System.out.println("=====================================================");
        System.out.println();

        if (errors.get() == 0 && connectedClients.get() == numClients) {
            System.out.println("✓ Load test PASSED");
        } else {
            System.out.println("✗ Load test FAILED");
        }
    }

    /**
     * Simulated client that connects to the server and receives updates
     */
    private class SimulatedClient {
        private final int clientNumber;
        private final String host;
        private final int port;
        private final String[] tickers;
        private final int delayMs;
        private final boolean verbose;
        private SocketChannel channel;

        public SimulatedClient(int clientNumber, String host, int port, String[] tickers, int delayMs, boolean verbose) {
            this.clientNumber = clientNumber;
            this.host = host;
            this.port = port;
            this.tickers = tickers;
            this.delayMs = delayMs;
            this.verbose = verbose;
        }

        public void run() throws IOException, InterruptedException {
            try {
                // Connect
                channel = SocketChannel.open();
                channel.connect(new InetSocketAddress(host, port));
                channel.configureBlocking(true);

                if (verbose) {
                    System.out.println("[Client-" + clientNumber + "] Connected");
                }

                // Send welcome message read
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                int bytesRead = channel.read(buffer);
                if (bytesRead > 0) {
                    buffer.flip();
                    String welcome = StandardCharsets.UTF_8.decode(buffer).toString();
                    if (verbose) {
                        System.out.println("[Client-" + clientNumber + "] " + welcome.trim());
                    }
                }

                // Subscribe to tickers
                String subscription = "SUBSCRIBE|" + String.join(",", tickers) + "\n";
                channel.write(ByteBuffer.wrap(subscription.getBytes(StandardCharsets.UTF_8)));
                if (verbose) {
                    System.out.println("[Client-" + clientNumber + "] Subscribed to " + String.join(",", tickers));
                }

                // Receive messages
                ByteBuffer msgBuffer = ByteBuffer.allocate(4096);
                while (running) {
                    msgBuffer.clear();
                    int read = channel.read(msgBuffer);
                    if (read > 0) {
                        bytesReceived.addAndGet(read);
                        // Count messages (rough estimate: split by newline)
                        msgBuffer.flip();
                        String data = StandardCharsets.UTF_8.decode(msgBuffer).toString();
                        long count = data.lines().count();
                        messagesReceived.addAndGet(count);
                    }
                    Thread.sleep(delayMs);
                }
            } catch (Exception e) {
                errors.incrementAndGet();
                if (verbose) {
                    System.err.println("[Client-" + clientNumber + "] Error: " + e.getMessage());
                }
            } finally {
                if (channel != null && channel.isOpen()) {
                    try {
                        channel.close();
                    } catch (IOException e) {
                        // Ignore
                    }
                }
            }
        }
    }
}
