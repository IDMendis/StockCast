package com.stockcast.service;

import com.stockcast.model.ClientInfo;
import com.stockcast.model.StockPrice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Member 4: Efficient Broadcasting & NIO
 * Broadcasts price updates to clients using Java NIO for scalable, non-blocking communication
 */
@Service
@Slf4j
public class BroadcastModule {
    
    private Selector selector;
    private final Queue<BroadcastTask> broadcastQueue = new ConcurrentLinkedQueue<>();
    private Thread broadcastThread;
    private volatile boolean running = false;

    public void start() throws IOException {
        if (running) {
            log.warn("Broadcast module is already running");
            return;
        }

        selector = Selector.open();
        running = true;
        
        broadcastThread = new Thread(this::processBroadcasts, "BroadcastThread");
        broadcastThread.setDaemon(true);
        broadcastThread.start();
        
        log.info("Broadcast module started");
    }

    @PreDestroy
    public void stop() {
        running = false;
        
        if (selector != null && selector.isOpen()) {
            try {
                selector.wakeup();
                selector.close();
            } catch (IOException e) {
                log.error("Error closing selector", e);
            }
        }
        
        if (broadcastThread != null) {
            broadcastThread.interrupt();
            try {
                broadcastThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        log.info("Broadcast module stopped");
    }

    /**
     * Broadcast a stock price update to specific clients
     */
    public void broadcast(StockPrice stockPrice, List<ClientInfo> clients) {
        if (clients == null || clients.isEmpty()) {
            return;
        }

        String message = stockPrice.toMessage() + "\n";
        broadcastQueue.offer(new BroadcastTask(message, clients));
        
        // Wake up selector to process the new broadcast
        if (selector != null) {
            selector.wakeup();
        }
    }

    /**
     * Broadcast a message to a single client
     */
    public void sendToClient(ClientInfo client, String message) {
        if (client == null || message == null) {
            return;
        }
        
        broadcastQueue.offer(new BroadcastTask(message + "\n", Collections.singletonList(client)));
        
        if (selector != null) {
            selector.wakeup();
        }
    }

    /**
     * Process broadcasts in a non-blocking manner
     */
    private void processBroadcasts() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                // Process queued broadcasts
                BroadcastTask task;
                while ((task = broadcastQueue.poll()) != null) {
                    sendMessage(task.message, task.clients);
                }

                // Wait for a short time before checking queue again
                Thread.sleep(10);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error processing broadcasts", e);
            }
        }
    }

    /**
     * Send message to multiple clients using NIO
     */
    private void sendMessage(String message, List<ClientInfo> clients) {
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8));
        
        for (ClientInfo client : clients) {
            try {
                SocketChannel channel = client.getSocketChannel();
                
                if (channel == null || !channel.isOpen() || !channel.isConnected()) {
                    log.warn("Cannot send to client {}: channel not available", client.getClientId());
                    continue;
                }

                // Prepare buffer for reading
                buffer.rewind();
                
                // Non-blocking write
                int bytesWritten = 0;
                
                while (buffer.hasRemaining()) {
                    int written = channel.write(buffer);
                    bytesWritten += written;
                    
                    if (written == 0) {
                        // Channel buffer is full, try again after a short delay
                        Thread.sleep(1);
                    }
                }
                
                if (bytesWritten > 0) {
                    log.debug("Sent {} bytes to client {}", bytesWritten, client.getClientId());
                }
                
            } catch (IOException e) {
                log.error("Error sending message to client {}: {}", 
                    client.getClientId(), e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Unexpected error sending to client {}", client.getClientId(), e);
            }
        }
    }

    /**
     * Register a socket channel with the selector for write operations
     */
    public void registerChannel(SocketChannel channel) throws IOException {
        if (selector != null && channel != null && channel.isOpen()) {
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_WRITE);
            log.debug("Channel registered with selector");
        }
    }

    /**
     * Unregister a socket channel from the selector
     */
    public void unregisterChannel(SocketChannel channel) {
        if (channel != null && channel.isOpen()) {
            SelectionKey key = channel.keyFor(selector);
            if (key != null) {
                key.cancel();
            }
        }
    }

    /**
     * Internal class to hold broadcast tasks
     */
    private static class BroadcastTask {
        final String message;
        final List<ClientInfo> clients;

        BroadcastTask(String message, List<ClientInfo> clients) {
            this.message = message;
            this.clients = clients;
        }
    }
}
