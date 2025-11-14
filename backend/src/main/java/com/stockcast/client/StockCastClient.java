package com.stockcast.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Member 5: Client Interface & Update Display
 * Console application to subscribe, receive updates, and display stock prices
 * in real-time
 */
public class StockCastClient {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private SocketChannel socketChannel;
    private volatile boolean running = false;
    private String clientId;
    private Thread receiverThread;

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 9092;

        StockCastClient client = new StockCastClient();
        try {
            client.connect(host, port);
            client.run();
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            client.disconnect();
        }
    }

    public void connect(String host, int port) throws IOException {
        socketChannel = SocketChannel.open();
        socketChannel.connect(new InetSocketAddress(host, port));

        // ✅ USE BLOCKING MODE for reliable write operations
        // Non-blocking mode requires a Selector which is complex
        socketChannel.configureBlocking(true);

        System.out.println("DEBUG: Socket connected in BLOCKING mode");
        System.out.println("DEBUG: Socket connected: " + socketChannel.isConnected());

        running = true;

        // Start receiver thread
        receiverThread = new Thread(this::receiveMessages, "MessageReceiver");
        receiverThread.start();

        System.out.println("Connected to StockCast server at " + host + ":" + port);
        System.out.println("=".repeat(70));
    }

    public void disconnect() {
        running = false;

        if (receiverThread != null) {
            receiverThread.interrupt();
            try {
                receiverThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (socketChannel != null && socketChannel.isOpen()) {
            try {
                socketChannel.close();
            } catch (IOException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }

        System.out.println("\nDisconnected from server");
    }

    public void run() throws IOException {
        printHelp();

        // Always use BufferedReader with blocking I/O for reliable command input
        BufferedReader consoleReader = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8));

        System.out.println("\nREADY for commands. Type 'help' for options.");
        System.out.println("=".repeat(70));

        while (running) {
            try {
                // Show prompt
                System.out.print("\nstockcast> ");
                System.out.flush();

                // BLOCKING read - wait for user input
                String input = consoleReader.readLine();

                if (input == null) {
                    // EOF or stream closed
                    break;
                }

                // Strip control characters
                input = stripControlCharacters(input).trim();

                System.out.println("DEBUG: Raw input received: [" + input + "] length=" + input.length());

                if (input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("exit")) {
                    break;
                }

                if (!input.isEmpty()) {
                    processUserInput(input);
                } else {
                    System.out.println("DEBUG: Empty input ignored");
                }

            } catch (IOException e) {
                if (running) {
                    System.err.println("Input error: " + e.getMessage());
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    /**
     * Strip control characters that Windows PowerShell sometimes injects
     */
    private String stripControlCharacters(String input) {
        if (input == null)
            return "";

        // Remove common control characters (0x00-0x1F except tab, newline, carriage
        // return)
        // Also remove Unicode BOM and other invisible characters
        return input.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "")
                .replaceAll("\\p{C}", "") // Remove other control characters
                .trim();
    }

    private void receiveMessages() {
        ByteBuffer buffer = ByteBuffer.allocate(4096);
        StringBuilder messageBuilder = new StringBuilder();

        while (running && socketChannel.isOpen()) {
            try {
                buffer.clear();
                int bytesRead = socketChannel.read(buffer);

                if (bytesRead == -1) {
                    System.out.println("\nServer closed connection");
                    running = false;
                    break;
                }

                if (bytesRead > 0) {
                    buffer.flip();
                    String data = StandardCharsets.UTF_8.decode(buffer).toString();
                    messageBuilder.append(data);

                    // Process complete messages
                    String messages = messageBuilder.toString();
                    String[] lines = messages.split("\n");

                    for (int i = 0; i < lines.length - 1; i++) {
                        processServerMessage(lines[i].trim());
                    }

                    // Keep incomplete message
                    if (messages.endsWith("\n")) {
                        messageBuilder.setLength(0);
                    } else {
                        messageBuilder.setLength(0);
                        messageBuilder.append(lines[lines.length - 1]);
                    }
                }

                Thread.sleep(10);
            } catch (IOException e) {
                if (running) {
                    System.err.println("Connection error: " + e.getMessage());
                    running = false;
                }
                break;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void processServerMessage(String message) {
        if (message.isEmpty()) {
            return;
        }

        String[] parts = message.split("\\|");
        String messageType = parts[0];

        switch (messageType) {
            case "WELCOME":
                if (parts.length >= 3) {
                    clientId = parts[1];
                    System.out.println("Client ID: " + clientId);
                    System.out.println(parts[2]);
                    System.out.println("=".repeat(70));
                }
                break;

            case "PRICE":
                displayStockPrice(parts);
                break;

            case "ACK":
                if (parts.length > 1) {
                    System.out.println("✓ " + parts[1]);
                }
                break;

            case "SUBSCRIPTIONS":
                if (parts.length > 1) {
                    System.out.println("Your subscriptions: " + parts[1]);
                }
                break;

            case "PONG":
                System.out.println("Server is alive (PONG)");
                break;

            default:
                System.out.println("Server: " + message);
        }
    }

    private void displayStockPrice(String[] parts) {
        if (parts.length >= 4) {
            String ticker = parts[1];
            String price = parts[2];
            String change = parts.length > 4 ? parts[4] : "0.00%";

            // Color coding for price changes
            String changeDisplay = formatPriceChange(change);
            String time = LocalDateTime.now().format(TIME_FORMAT);

            System.out.printf("[%s] %-6s $%-10s %s%n",
                    time, ticker, price, changeDisplay);
        }
    }

    private String formatPriceChange(String change) {
        try {
            double changeValue = Double.parseDouble(change.replace("%", ""));
            if (changeValue > 0) {
                return "↑ +" + change;
            } else if (changeValue < 0) {
                return "↓ " + change;
            } else {
                return "→ " + change;
            }
        } catch (NumberFormatException e) {
            return change;
        }
    }

    private void processUserInput(String input) throws IOException {
        if (input.isEmpty()) {
            return;
        }

        // DEBUG: Show what command we captured
        System.out.println("DEBUG: Captured command: [" + input + "]");

        String[] parts = input.split("\\s+", 2);
        String command = parts[0].toLowerCase();

        switch (command) {
            case "help":
            case "?":
                printHelp();
                break;

            case "subscribe":
            case "sub":
                if (parts.length > 1) {
                    sendMessage("SUBSCRIBE|" + parts[1].toUpperCase());
                } else {
                    System.out.println("Usage: subscribe <ticker1,ticker2,...>");
                }
                break;

            case "unsubscribe":
            case "unsub":
                if (parts.length > 1) {
                    sendMessage("UNSUBSCRIBE|" + parts[1].toUpperCase());
                } else {
                    System.out.println("Usage: unsubscribe <ticker1,ticker2,...>");
                }
                break;

            case "list":
                sendMessage("LIST");
                break;

            case "ping":
                sendMessage("PING");
                break;

            case "clear":
                clearScreen();
                break;

            default:
                System.out.println("Unknown command. Type 'help' for available commands.");
        }
    }

    private void sendMessage(String message) throws IOException {
        System.out.println("DEBUG: Sending to server: [" + message + "]");

        // Ensure the channel is writable
        if (!socketChannel.isConnected()) {
            System.err.println("ERROR: Socket is not connected!");
            return;
        }

        // Create buffer with message + newline delimiter
        byte[] messageBytes = (message + "\n").getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.wrap(messageBytes);

        System.out.println("DEBUG: Buffer size: " + messageBytes.length + " bytes");
        System.out.println("DEBUG: Message with newline: [" + (message + "\n").replace("\n", "\\n") + "]");
        System.out.println("DEBUG: Hex dump: " + bytesToHex(messageBytes));

        // In BLOCKING mode, write() will block until ALL data is written
        int totalWritten = 0;
        while (buffer.hasRemaining()) {
            int written = socketChannel.write(buffer);
            totalWritten += written;
            System.out.println("DEBUG: Wrote " + written + " bytes (total: " + totalWritten + ")");
        }

        System.out.println("DEBUG: ✅ Successfully sent " + totalWritten + " bytes to server");

        // Verify socket is still connected after write
        System.out.println("DEBUG: Socket still connected: " + socketChannel.isConnected());
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    private void printHelp() {
        System.out.println("\nAvailable Commands:");
        System.out.println("  subscribe <tickers>    - Subscribe to stock updates (e.g., subscribe AAPL,GOOG)");
        System.out.println("  unsubscribe <tickers>  - Unsubscribe from stock updates");
        System.out.println("  list                   - Show your current subscriptions");
        System.out.println("  ping                   - Check server connection");
        System.out.println("  clear                  - Clear screen");
        System.out.println("  help                   - Show this help message");
        System.out.println("  quit                   - Exit the client");
        System.out.println("=".repeat(70));
    }

    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}
