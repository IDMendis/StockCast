# StockCast - Real-Time Stock Price Broadcasting System

**StockCast** is a real-time stock market simulation system that sends continuously updated stock prices to multiple clients at once. It combines several networking technologies — **TCP, UDP Multicast, WebSocket, and REST APIs** — to show how data can move across different network layers efficiently.

---

## System Overview

StockCast is built with **Java and Spring Boot** to demonstrate how real-time communication works between servers and multiple clients.

The system includes:

- A **server** that generates and broadcasts stock prices.
- Different **communication modules** (TCP, UDP, WebSocket).
- A **client interface** to receive updates.
- A **dashboard** to monitor performance and metrics.

### Key Features

- Real-time stock price generation and broadcasting.
- Multiple clients can connect using TCP or WebSocket.
- UDP multicast allows one-to-many message delivery.
- REST API for system monitoring.
- Uses threads for handling multiple clients efficiently.
- Live performance tracking with metrics display.

---

## Networking Concepts Used

### 🧩 1. TCP/IP Socket Programming

**Files:** `ConnectionManager.java`, `StockCastClient.java`

- Creates a **reliable connection** between the server and clients using TCP.
- Handles sending and receiving stock updates in real-time.
- Manages the full connection process: connect → send/receive → close.
- Runs on **port 9092** for TCP communication.

---

### 📬 2. UDP Multicast

**Files:** `UDPMulticastBroadcaster.java`, `UDPMulticastListener.java`

- Sends messages to **multiple clients at once** without a direct connection.
- Uses **multicast groups** (`230.0.0.1:4446`) to broadcast data.
- Good for fast, light updates where reliability is less critical.
- Works with `MulticastSocket` and `DatagramPacket`.

---

### ⚡ 3. Java NIO (Non-Blocking I/O)

**File:** `BroadcastModule.java`

- Allows the server to handle **many clients at the same time** without waiting.
- Uses `Selector` and `SocketChannel` for non-blocking data transfer.
- Improves performance by using fewer threads.
- Stores and processes data efficiently using `ByteBuffer`.

---

### 🌐 4. WebSocket Protocol

**Files:** `StockWebSocketHandler.java`, `WebSocketConfig.java`, `WebSocketService.js`

- Keeps a **constant connection** between browser clients and the server.
- Sends real-time updates using the `ws://` protocol.
- Supports **two-way communication** — the client and server can both send messages anytime.
- Built with **Spring WebSocket** for backend and **JavaScript WebSocket API** for frontend.

---

### 🔗 5. HTTP/REST API

**File:** `MetricsController.java`

- Exposes data through **HTTP endpoints** (like `/api/metrics`).
- Allows viewing of performance statistics such as connection count and uptime.
- Uses **JSON format** for communication between client and server.
- Runs on **port 9091**.

---

### 🧵 6. Multi-Threading & Concurrency

**Files:** `ConnectionManager.java`, `StockPriceGenerator.java`, `BroadcastModule.java`, `UDPMulticastBroadcaster.java`

- Uses multiple threads to handle many users and processes at once.
- Thread pools (`ExecutorService`) prevent blocking.
- Thread-safe collections (`ConcurrentHashMap`) keep data safe from conflicts.
- Ensures smooth updates without slowing down the system.

---

### 🖧 7. Client-Server Architecture

- Follows the **publish-subscribe** model — clients subscribe to specific stock tickers.
- The server acts as a **broadcaster**, sending updates to all subscribed clients.
- Uses a simple command-based protocol like `SUBSCRIBE`, `LIST`, `PING`.

---

### 🔐 8. Application Layer Protocol

**Files:** `ConnectionManager.java`, `StockWebSocketHandler.java`, `AUTHENTICATION.md`

- Custom message format for sending commands and data.
- Includes token-based authentication for secure sessions.
- Handles errors and responses in a consistent way.

---

### 📈 9. Real-Time Data Streaming

**Files:** `StockPriceGenerator.java`, `BroadcastModule.java`

- Continuously generates stock price changes every few seconds.
- Sends updates automatically to all active clients.
- Supports **push-based communication** (server sends updates instantly).

---

### 📊 10. Performance Monitoring

**Files:** `MetricsService.java`, `MetricsPanel.js`

- Tracks live metrics: active connections, system load, and uptime.
- Displays data on a **real-time dashboard**.
- Helps visualize how the system performs during operation.

---

## Technologies Used

- **Java 17** – Core programming language
- **Spring Boot 3.2.0** – Backend framework
- **Java NIO** – For non-blocking I/O
- **WebSocket API** – Real-time browser communication
- **UDP Multicast** – Fast group broadcasting
- **RESTful API** – For monitoring and data access
- **Concurrent Collections** – Thread-safe data handling
- **Maven & Lombok** – Build management and reduced boilerplate

---

## How to Run

### Run the Server

```bash
cd backend
mvn spring-boot:run
```

**Ports used:**

- REST + WebSocket → 9091
- TCP Server → 9092
- UDP Multicast → 4446

### Run the Client

```bash
java -cp target/classes com.stockcast.client.StockCastClient
```

---

## Architecture Overview

```
┌───────────────────────────────────────────────┐
│                StockCast Server               │
├───────────────────────────────────────────────┤
│ TCP Module (ConnectionManager)                │
│ UDP Multicast (UDPMulticastBroadcaster)       │
│ WebSocket (StockWebSocketHandler)             │
│ Stock Generator (StockPriceGenerator)         │
│ REST Metrics (MetricsController)              │
└───────────────────────────────────────────────┘
                 │         │         │
                 │         │         │
                 ▼         ▼         ▼
        TCP Clients   Web Clients   Multicast Listeners
```

---

## Educational Purpose

This project is designed to help understand **real-time communication, concurrency, and different networking models** using Java and Spring Boot.  
It shows how to build a system that can broadcast, stream, and monitor live data efficiently.
