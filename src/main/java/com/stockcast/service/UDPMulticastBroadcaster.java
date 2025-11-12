package com.stockcast.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * UDP Multicast Broadcaster
 * Demonstrates UDP protocol for market-wide announcements
 * 
 * Clients can listen to multicast group without explicit subscription
 * Used for: market open/close, breaking news, system announcements
 */
@Service
@Slf4j
public class UDPMulticastBroadcaster {

    @Value("${stockcast.multicast.address:230.0.0.1}")
    private String multicastAddress;

    @Value("${stockcast.multicast.port:4446}")
    private int multicastPort;

    private MulticastSocket socket;
    private InetAddress group;
    private Thread broadcasterThread;
    private volatile boolean running = false;
    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();

    @PostConstruct
    public void init() {
        try {
            socket = new MulticastSocket();
            group = InetAddress.getByName(multicastAddress);
            log.info("UDP Multicast initialized on {}:{}", multicastAddress, multicastPort);
        } catch (IOException e) {
            log.error("Failed to initialize UDP multicast", e);
        }
    }

    public void start() {
        if (running) {
            log.warn("UDP Multicast broadcaster already running");
            return;
        }

        running = true;
        broadcasterThread = new Thread(this::processBroadcasts, "UDPBroadcaster");
        broadcasterThread.setDaemon(true);
        broadcasterThread.start();

        log.info("UDP Multicast broadcaster started on {}:{}", multicastAddress, multicastPort);

        // Send startup announcement
        sendAnnouncement("SYSTEM", "Market data streaming active");
    }

    @PreDestroy
    public void stop() {
        running = false;

        if (broadcasterThread != null) {
            broadcasterThread.interrupt();
            try {
                broadcasterThread.join(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }

        log.info("UDP Multicast broadcaster stopped");
    }

    /**
     * Send market-wide announcement via UDP multicast
     * 
     * @param type Type of announcement (SYSTEM, MARKET, NEWS, ALERT)
     * @param message The message content
     */
    public void sendAnnouncement(String type, String message) {
        String formattedMessage = String.format("ANNOUNCEMENT|%s|%s|%d", 
            type, message, System.currentTimeMillis());
        
        try {
            messageQueue.put(formattedMessage);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Failed to queue announcement", e);
        }
    }

    /**
     * Broadcast market summary
     */
    public void broadcastMarketSummary(String summary) {
        sendAnnouncement("MARKET", summary);
    }

    /**
     * Broadcast system alert
     */
    public void broadcastAlert(String alert) {
        sendAnnouncement("ALERT", alert);
    }

    /**
     * Process and send queued broadcasts
     */
    private void processBroadcasts() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                String message = messageQueue.take();
                sendMulticastMessage(message);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error processing UDP broadcast", e);
            }
        }
    }

    /**
     * Send message via UDP multicast
     */
    private void sendMulticastMessage(String message) {
        try {
            byte[] data = message.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(
                data, 
                data.length, 
                group, 
                multicastPort
            );
            
            socket.send(packet);
            log.debug("UDP multicast sent: {}", message);
            
        } catch (IOException e) {
            log.error("Failed to send UDP multicast: {}", e.getMessage());
        }
    }

    /**
     * Get multicast group address (for clients to join)
     */
    public String getMulticastAddress() {
        return multicastAddress;
    }

    /**
     * Get multicast port
     */
    public int getMulticastPort() {
        return multicastPort;
    }

    /**
     * Check if broadcaster is running
     */
    public boolean isRunning() {
        return running;
    }
}
