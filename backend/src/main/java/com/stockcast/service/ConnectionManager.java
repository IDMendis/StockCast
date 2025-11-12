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

/**
 * Member 1: Server Setup & Connection Handling
 * Implements ServerSocket, accepts multiple clients, and manages connections
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
    @Value("${stockcast.server.port:9092}")
    private int port;

    private ServerSocketChannel serverSocketChannel;
    private ExecutorService clientHandlerPool;
    private Thread acceptorThread;
    private volatile boolean running = false;

    public void start() throws IOException {
        if (running) {
            log.warn("Connection manager is already running");
            return;
        }

        // Start dependencies
        broadcastModule.start();
        stockPriceGenerator.start();

        // Setup stock price listener
        stockPriceGenerator.addListener(this::onStockPriceUpdate);

        // Create thread pool for handling clients
        clientHandlerPool = Executors.newCachedThreadPool();

        // Create server socket channel
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));
        serverSocketChannel.configureBlocking(true); // Blocking mode for accept

        running = true;

        // Start acceptor thread
        acceptorThread = new Thread(this::acceptConnections, "ClientAcceptor");
        acceptorThread.start();

        log.info("StockCast server started on port {}", port);
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

        log.info("StockCast server stopped");
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
                    log.info("New client connected: {}", clientId);

                    // Configure channel
                    clientChannel.configureBlocking(false);

                    // Register client
                    subscriptionManager.registerClient(clientId, clientChannel);

                    // Register with broadcast module
                    broadcastModule.registerChannel(clientChannel);

                    // Send welcome message
                    String welcome = "WELCOME|" + clientId + "|Available tickers: " +
                            String.join(",", stockPriceGenerator.getAvailableTickers()) + "\n";
                    sendMessage(clientChannel, welcome);

                    // Handle client in separate thread
                    clientHandlerPool.submit(() -> handleClient(clientId, clientChannel));
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
    private void handleClient(String clientId, SocketChannel channel) {
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
                    buffer.flip();
                    String data = StandardCharsets.UTF_8.decode(buffer).toString();
                    messageBuilder.append(data);

                    // Process complete messages (delimited by newline)
                    String messages = messageBuilder.toString();
                    String[] lines = messages.split("\n");

                    // Process all complete messages
                    for (int i = 0; i < lines.length - 1; i++) {
                        processClientMessage(clientId, lines[i].trim());
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
            log.error("Error handling client {}: {}", clientId, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            disconnectClient(clientId, channel);
        }
    }

    /**
     * Process client messages (SUBSCRIBE, UNSUBSCRIBE, etc.)
     */
    private void processClientMessage(String clientId, String message) {
        if (message.isEmpty()) {
            return;
        }

        log.debug("Message from {}: {}", clientId, message);

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
                    log.warn("Unknown command from {}: {}", clientId, command);
            }
        } catch (Exception e) {
            log.error("Error processing message from {}: {}", clientId, e.getMessage());
        }
    }

    /**
     * Disconnect client and cleanup
     */
    private void disconnectClient(String clientId, SocketChannel channel) {
        log.info("Client disconnected: {}", clientId);

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
        }
    }

    /**
     * Send a direct message to a channel
     */
    private void sendMessage(SocketChannel channel, String message) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8));
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
        } catch (IOException e) {
            log.error("Error sending message", e);
        }
    }

    public boolean isRunning() {
        return running;
    }
}
