package com.stockcast.service;

import com.stockcast.model.ClientInfo;
import com.stockcast.model.StockPrice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Member 1: Server Setup & Connection Handling
 * Implements ServerSocket, accepts multiple clients, and manages connections
 * 
 * Network Metrics Tracked:
 * - Total connections accepted
 * - Current active connections
 * - Messages processed
 * - Bytes sent/received
 * - Connection timing statistics
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ConnectionManager {

    private final SubscriptionManager subscriptionManager;
    private final BroadcastModule broadcastModule;
    private final StockPriceGenerator stockPriceGenerator;

    // Use a dedicated property for the TCP server port so we don't collide
    // with the embedded HTTP server (Tomcat) which also uses `server.port`.
    @Value("${stockcast.server.port:9090}")
    private int port;

    // Network metrics for monitoring and debugging
    private final AtomicInteger totalConnectionsAccepted = new AtomicInteger(0);
    private final AtomicInteger currentActiveConnections = new AtomicInteger(0);
    private final AtomicLong totalMessagesProcessed = new AtomicLong(0);
    private final AtomicLong totalBytesReceived = new AtomicLong(0);
    private final AtomicLong totalBytesSent = new AtomicLong(0);
    private long serverStartTime;

    private ServerSocketChannel serverSocketChannel;
    private ExecutorService clientHandlerPool;
    private Thread acceptorThread;
    private volatile boolean running = false;

    public void start() throws IOException {
        if (running) {
            log.warn("Connection manager is already running");
            return;
        }

        serverStartTime = System.currentTimeMillis();

        // Start dependencies
        broadcastModule.start();
        stockPriceGenerator.start();

        // Setup stock price listener
        stockPriceGenerator.addListener(this::onStockPriceUpdate);

        // Create thread pool for handling clients
        clientHandlerPool = Executors.newCachedThreadPool();

        // Create server socket channel
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress("0.0.0.0", port));
        serverSocketChannel.configureBlocking(true); // Blocking mode for accept

        running = true;

        // Start acceptor thread
        acceptorThread = new Thread(this::acceptConnections, "ClientAcceptor");
        acceptorThread.start();

        logServerStartup();
    }

    private void logServerStartup() {
        log.info("======================================================================");
        log.info("StockCast Server started on port {}", port);
        log.info("======================================================================");
        log.info("Server Configuration:");
        log.info("  - Host: 0.0.0.0");
        log.info("  - Port: {} (TCP/IP)", port);
        log.info("  - Available tickers: {}", String.join(", ", stockPriceGenerator.getAvailableTickers()));
        log.info("  - Price update interval: 1000ms");
        log.info("======================================================================");
        log.info("Network Concepts Demonstrated:");
        log.info("  - TCP/IP Socket Programming (ServerSocketChannel)");
        log.info("  - Non-Blocking I/O (NIO) with Selector");
        log.info("  - Multi-threaded connection handling");
        log.info("  - Custom protocol with message framing");
        log.info("======================================================================");
        log.info("StockCast Server is ready to accept connections");
        log.info("======================================================================");
    }

    @PreDestroy
    public void stop() {
        running = false;

        // Close server socket
        if (serverSocketChannel != null && serverSocketChannel.isOpen()) {
            try {
                serverSocketChannel.close();
            } catch (IOException e) {
                log.error("Error closing server socket", e);
            }
        }

        // Shutdown thread pool
        if (clientHandlerPool != null) {
            clientHandlerPool.shutdown();
            try {
                if (!clientHandlerPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    clientHandlerPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                clientHandlerPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Wait for acceptor thread
        if (acceptorThread != null) {
            acceptorThread.interrupt();
            try {
                acceptorThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        logServerShutdown();
    }

    private void logServerShutdown() {
        long uptime = System.currentTimeMillis() - serverStartTime;
        log.info("======================================================================");
        log.info("StockCast Server stopped");
        log.info("======================================================================");
        log.info("Server Metrics:");
        log.info("  - Uptime: {}s", uptime / 1000);
        log.info("  - Total connections: {}", totalConnectionsAccepted.get());
        log.info("  - Active connections: {}", currentActiveConnections.get());
        log.info("  - Messages processed: {}", totalMessagesProcessed.get());
        log.info("  - Bytes received: {} KB", totalBytesReceived.get() / 1024);
        log.info("  - Bytes sent: {} KB", totalBytesSent.get() / 1024);
        log.info("======================================================================");
    }

    /**
     * Accept incoming client connections
     */
    private void acceptConnections() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                SocketChannel clientChannel = serverSocketChannel.accept();

                if (clientChannel != null) {
                    String clientId = UUID.randomUUID().toString().substring(0, 8);
                    long connectionTime = System.currentTimeMillis();
                    
                    totalConnectionsAccepted.incrementAndGet();
                    currentActiveConnections.incrementAndGet();
                    
                    log.info("[CONNECT] Client {} connected from {}", 
                        clientId, clientChannel.getRemoteAddress());
                    log.debug("Active connections: {}", currentActiveConnections.get());

                    // Configure channel
                    clientChannel.configureBlocking(false);

                    // Register client
                    subscriptionManager.registerClient(clientId, clientChannel);

                    // Register with broadcast module
                    broadcastModule.registerChannel(clientChannel);

                    // Send welcome message
                    String welcome = "WELCOME|" + clientId + "|Available tickers: " +
                            String.join(",", stockPriceGenerator.getAvailableTickers()) + "\n";
                    sendMessage(clientChannel, welcome, clientId);

                    // Handle client in separate thread
                    clientHandlerPool.submit(() -> handleClient(clientId, clientChannel, connectionTime));
                }
            } catch (IOException e) {
                if (running) {
                    log.error("Error accepting client connection", e);
                }
            }
        }
    }

    /**
     * Handle client communication
     */
    private void handleClient(String clientId, SocketChannel channel, long connectionTime) {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        StringBuilder messageBuilder = new StringBuilder();

        try {
            while (running && channel.isOpen() && channel.isConnected()) {
                buffer.clear();
                int bytesRead = channel.read(buffer);

                if (bytesRead == -1) {
                    // Client disconnected
                    break;
                }

                if (bytesRead > 0) {
                    totalBytesReceived.addAndGet(bytesRead);
                    buffer.flip();
                    String data = StandardCharsets.UTF_8.decode(buffer).toString();
                    messageBuilder.append(data);

                    // Process complete messages (delimited by newline)
                    String messages = messageBuilder.toString();
                    String[] lines = messages.split("\n");

                    // Process all complete messages
                    for (int i = 0; i < lines.length - 1; i++) {
                        String command = lines[i].trim();
                        if (!command.isEmpty()) {
                            totalMessagesProcessed.incrementAndGet();
                            processClientMessage(clientId, command);
                        }
                    }

                    // Keep incomplete message in buffer
                    if (messages.endsWith("\n")) {
                        messageBuilder.setLength(0);
                    } else {
                        messageBuilder.setLength(0);
                        messageBuilder.append(lines[lines.length - 1]);
                    }
                }

                // Small delay to prevent tight loop
                Thread.sleep(10);
            }
        } catch (IOException e) {
            log.debug("[IO_ERROR] Client {}: {}", clientId, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            long connectionDuration = System.currentTimeMillis() - connectionTime;
            disconnectClient(clientId, channel, connectionDuration);
        }
    }

    /**
     * Process client messages (SUBSCRIBE, UNSUBSCRIBE, etc.)
     */
    private void processClientMessage(String clientId, String message) {
        if (message.isEmpty()) {
            return;
        }

        log.debug("[MSG] Client {}: {}", clientId, message);

        String[] parts = message.split("\\|");
        String command = parts[0].toUpperCase();

        try {
            switch (command) {
                case "SUBSCRIBE":
                    if (parts.length > 1) {
                        String[] tickers = parts[1].split(",");
                        subscriptionManager.subscribe(clientId, tickers);

                        ClientInfo client = subscriptionManager.getClient(clientId);
                        if (client != null) {
                            broadcastModule.sendToClient(client,
                                    "ACK|Subscribed to: " + String.join(",", tickers));
                            log.debug("[SUBSCRIBE] Client {} subscribed to {}", clientId, String.join(",", tickers));
                        }
                    }
                    break;

                case "UNSUBSCRIBE":
                    if (parts.length > 1) {
                        String[] tickers = parts[1].split(",");
                        subscriptionManager.unsubscribe(clientId, tickers);

                        ClientInfo client = subscriptionManager.getClient(clientId);
                        if (client != null) {
                            broadcastModule.sendToClient(client,
                                    "ACK|Unsubscribed from: " + String.join(",", tickers));
                            log.debug("[UNSUBSCRIBE] Client {} unsubscribed from {}", clientId, String.join(",", tickers));
                        }
                    }
                    break;

                case "LIST":
                    ClientInfo client = subscriptionManager.getClient(clientId);
                    if (client != null) {
                        String subs = String.join(",", client.getSubscriptions());
                        broadcastModule.sendToClient(client,
                                "SUBSCRIPTIONS|" + (subs.isEmpty() ? "None" : subs));
                    }
                    break;

                case "PING":
                    ClientInfo pingClient = subscriptionManager.getClient(clientId);
                    if (pingClient != null) {
                        broadcastModule.sendToClient(pingClient, "PONG");
                    }
                    break;

                default:
                    log.warn("[UNKNOWN] Unknown command from {}: {}", clientId, command);
            }
        } catch (Exception e) {
            log.error("Error processing message from {}: {}", clientId, e.getMessage());
        }
    }

    /**
     * Disconnect client and cleanup
     */
    private void disconnectClient(String clientId, SocketChannel channel, long connectionDuration) {
        currentActiveConnections.decrementAndGet();
        log.info("[DISCONNECT] Client {} (connected for {}s)", clientId, connectionDuration / 1000);

        // Unregister from broadcast module
        broadcastModule.unregisterChannel(channel);

        // Unregister from subscription manager
        subscriptionManager.unregisterClient(clientId);

        // Close channel
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
        } catch (IOException e) {
            log.error("Error closing client channel", e);
        }
        
        log.debug("Active connections: {}", currentActiveConnections.get());
    }

    /**
     * Handle stock price updates
     */
    private void onStockPriceUpdate(StockPrice stockPrice) {
        // Get all clients subscribed to this ticker
        var subscribers = subscriptionManager.getSubscribersForTicker(stockPrice.getTicker());

        if (!subscribers.isEmpty()) {
            // Broadcast to subscribed clients
            broadcastModule.broadcast(stockPrice, subscribers);
            totalBytesSent.addAndGet(stockPrice.toMessage().length() * subscribers.size());
        }
    }

    /**
     * Send a direct message to a channel
     */
    private void sendMessage(SocketChannel channel, String message, String clientId) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8));
            int bytesWritten = 0;
            while (buffer.hasRemaining()) {
                bytesWritten += channel.write(buffer);
            }
            totalBytesSent.addAndGet(bytesWritten);
        } catch (IOException e) {
            log.error("Error sending message to {}", clientId, e);
        }
    }

    // ============ Metrics Access Methods ============
    
    public int getTotalConnectionsAccepted() {
        return totalConnectionsAccepted.get();
    }

    public int getCurrentActiveConnections() {
        return currentActiveConnections.get();
    }

    public long getTotalMessagesProcessed() {
        return totalMessagesProcessed.get();
    }

    public long getTotalBytesReceived() {
        return totalBytesReceived.get();
    }

    public long getTotalBytesSent() {
        return totalBytesSent.get();
    }

    public long getServerUptimeSeconds() {
        return (System.currentTimeMillis() - serverStartTime) / 1000;
    }

    public boolean isRunning() {
        return running;
    }
}
