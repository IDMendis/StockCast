package com.stockcast.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * UDP Multicast Listener Client
 * 
 * Demonstrates UDP multicast reception
 * Listens for market-wide announcements without explicit subscription
 * 
 * Usage: java com.stockcast.client.UDPMulticastListener [multicast-address] [port]
 */
public class UDPMulticastListener {

    private static final DateTimeFormatter TIME_FORMAT = 
        DateTimeFormatter.ofPattern("HH:mm:ss");

    private String multicastAddress;
    private int multicastPort;
    private MulticastSocket socket;
    private InetAddress group;
    private volatile boolean running = false;

    public UDPMulticastListener(String multicastAddress, int multicastPort) {
        this.multicastAddress = multicastAddress;
        this.multicastPort = multicastPort;
    }

    public static void main(String[] args) {
        String address = args.length > 0 ? args[0] : "230.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 4446;

        UDPMulticastListener listener = new UDPMulticastListener(address, port);
        
        try {
            listener.start();
            listener.listen();
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            listener.stop();
        }
    }

    public void start() throws IOException {
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("  StockCast UDP Multicast Listener");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("Joining multicast group: " + multicastAddress + ":" + multicastPort);
        
        socket = new MulticastSocket(multicastPort);
        group = InetAddress.getByName(multicastAddress);
        
        // Join the multicast group
        NetworkInterface netIf = NetworkInterface.getByInetAddress(
            InetAddress.getLocalHost()
        );
        socket.joinGroup(group, netIf);
        
        running = true;
        System.out.println("✓ Connected! Listening for announcements...");
        System.out.println("Press Ctrl+C to exit");
        System.out.println("═══════════════════════════════════════════════════════════\n");
    }

    public void listen() throws IOException {
        byte[] buffer = new byte[1024];

        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String message = new String(
                    packet.getData(), 
                    0, 
                    packet.getLength(), 
                    StandardCharsets.UTF_8
                );

                processMessage(message);
                
            } catch (IOException e) {
                if (running) {
                    System.err.println("Error receiving: " + e.getMessage());
                }
            }
        }
    }

    private void processMessage(String message) {
        String[] parts = message.split("\\|");
        
        if (parts.length >= 3 && "ANNOUNCEMENT".equals(parts[0])) {
            String type = parts[1];
            String content = parts[2];
            String timestamp = LocalDateTime.now().format(TIME_FORMAT);
            
            String icon = getIconForType(type);
            String color = getColorForType(type);
            
            System.out.printf("[%s] %s %s: %s%n", 
                timestamp, icon, color, content);
        } else {
            System.out.println("Received: " + message);
        }
    }

    private String getIconForType(String type) {
        return switch (type) {
            case "SYSTEM" -> "ℹ️";
            case "MARKET" -> "📊";
            case "NEWS" -> "📰";
            case "ALERT" -> "⚠️";
            default -> "📢";
        };
    }

    private String getColorForType(String type) {
        // ANSI color codes (works in most terminals)
        return switch (type) {
            case "SYSTEM" -> "\u001B[36m" + type + "\u001B[0m"; // Cyan
            case "MARKET" -> "\u001B[32m" + type + "\u001B[0m"; // Green
            case "NEWS" -> "\u001B[33m" + type + "\u001B[0m";   // Yellow
            case "ALERT" -> "\u001B[31m" + type + "\u001B[0m";  // Red
            default -> type;
        };
    }

    public void stop() {
        running = false;
        
        if (socket != null && !socket.isClosed()) {
            try {
                NetworkInterface netIf = NetworkInterface.getByInetAddress(
                    InetAddress.getLocalHost()
                );
                socket.leaveGroup(group, netIf);
            } catch (IOException e) {
                System.err.println("Error leaving group: " + e.getMessage());
            }
            socket.close();
        }
        
        System.out.println("\nDisconnected from multicast group");
    }
}
