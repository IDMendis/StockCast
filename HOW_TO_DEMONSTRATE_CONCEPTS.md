# StockCast - How to Demonstrate Each Networking Concept

## Overview

Your project implements 7 key networking concepts. Here's exactly how to demonstrate each one:

---

## ✅ 1. TCP Sockets

### What it is:

Reliable, connection-oriented communication using Java ServerSocket/SocketChannel

### Where in your code:

**File:** `backend/src/main/java/com/stockcast/service/ConnectionManager.java`

```java
// Lines 61-65: TCP Server Setup
serverSocketChannel = ServerSocketChannel.open();
serverSocketChannel.bind(new InetSocketAddress(port));
serverSocketChannel.configureBlocking(true);
```

### How to Demonstrate:

**Step 1:** Start the backend

```powershell
cd D:\StockCast\backend
mvn spring-boot:run
```

**Look for this log:**

```
StockCast server started on port 9092
```

**Step 2:** Run the TCP client

```powershell
cd D:\StockCast\backend
java -cp target/classes com.stockcast.client.StockCastClient
```

**Step 3:** Show the interaction

```
# Client sends:
SUBSCRIBE|AAPL,GOOGL,MSFT

# Server responds with:
ACK|Subscribed to: AAPL,GOOGL,MSFT
PRICE|AAPL|150.25|+1.5%
PRICE|GOOGL|2800.50|-0.3%
```

**Talking Points:**

- TCP guarantees delivery and order
- Bidirectional communication
- Connection-oriented (handshake required)
- Uses port 9092

**Code to Show:**

```java
// ConnectionManager.java - acceptConnections()
SocketChannel clientChannel = serverSocketChannel.accept();
clientChannel.configureBlocking(false);
```

---

## ✅ 2. UDP/Multicast

### What it is:

Connectionless broadcast protocol - one message reaches multiple subscribers simultaneously

### Where in your code:

**File:** `backend/src/main/java/com/stockcast/service/UDPMulticastBroadcaster.java`

```java
// Lines 27-31: UDP Multicast Setup
multicastSocket = new MulticastSocket();
InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS); // 230.0.0.1
multicastSocket.joinGroup(group);
```

### How to Demonstrate:

**Step 1:** Backend is already broadcasting (automatic on startup)
Look for this log:

```
UDP Multicast broadcaster started on 230.0.0.1:4446
```

**Step 2:** Run UDP listener to receive broadcasts

```powershell
cd D:\StockCast\backend
java -cp target/classes com.stockcast.client.UDPMulticastListener
```

**Step 3:** Open MULTIPLE listeners simultaneously

```powershell
# Terminal 1
java -cp target/classes com.stockcast.client.UDPMulticastListener

# Terminal 2
java -cp target/classes com.stockcast.client.UDPMulticastListener

# Terminal 3
java -cp target/classes com.stockcast.client.UDPMulticastListener
```

**What to show:**

- All 3 listeners receive the SAME message at the SAME time
- No connection setup needed
- One broadcast reaches everyone

**Example Output (all listeners see this):**

```
Listening for UDP multicast on 230.0.0.1:4446
STOCK|AAPL|150.25|+1.5%|1699876543210
STOCK|GOOGL|2800.50|-0.3%|1699876543210
STOCK|MSFT|380.75|+2.1%|1699876543210
```

**Talking Points:**

- UDP = no connection, no handshake
- Multicast = one-to-many broadcasting
- Efficient for real-time data distribution
- Used in financial systems, video streaming
- Multicast group: 230.0.0.1

**Code to Show:**

```java
// UDPMulticastBroadcaster.java
byte[] data = message.getBytes();
DatagramPacket packet = new DatagramPacket(data, data.length, group, PORT);
multicastSocket.send(packet);
```

---

## ✅ 3. Java NIO (Non-blocking I/O)

### What it is:

Scalable I/O using Selectors and Channels - one thread handles thousands of connections

### Where in your code:

**File:** `backend/src/main/java/com/stockcast/service/BroadcastModule.java`

```java
// Lines 34-38: NIO Selector Setup
selector = Selector.open();
channel.configureBlocking(false);
channel.register(selector, SelectionKey.OP_WRITE);
```

### How to Demonstrate:

**Step 1:** Show the code structure

```java
// BroadcastModule.java - processBroadcasts()
while (running) {
    selector.select(100); // Non-blocking wait
    Set<SelectionKey> selectedKeys = selector.selectedKeys();
    for (SelectionKey key : selectedKeys) {
        if (key.isWritable()) {
            // Write to channel without blocking
        }
    }
}
```

**Step 2:** Open multiple connections

- Open 5+ browser tabs at `http://localhost:3000`
- Run 3+ TCP clients
- Show metrics: `http://localhost:8080/api/metrics/connections`

**What to show:**

```json
{
  "activeConnections": 8,
  "totalConnectionsHandled": 12,
  "activeWebSocketClients": 5
}
```

**Talking Points:**

- Traditional I/O = one thread per connection (doesn't scale)
- NIO = one thread handles many connections using Selector
- Non-blocking = thread doesn't wait for I/O completion
- Channels replace Streams
- Scalable to 10,000+ connections

**Visual Proof:**

```powershell
# Show thread count doesn't explode with connections
jps -l  # Find Java process ID
jstack <PID> | grep "BroadcastThread"
# You'll see only ONE BroadcastThread handling ALL connections
```

---

## ✅ 4. Multi-threading

### What it is:

Multiple threads executing concurrently for parallel processing

### Where in your code:

**File:** `backend/src/main/java/com/stockcast/service/ConnectionManager.java`

```java
// Lines 58-59: Thread Pool
clientHandlerPool = Executors.newCachedThreadPool();

// Lines 67-68: Dedicated Acceptor Thread
acceptorThread = new Thread(this::acceptConnections, "ClientAcceptor");
acceptorThread.start();

// Lines 142-143: Each client handled in separate thread
clientHandlerPool.submit(() -> handleClient(clientId, clientChannel));
```

### How to Demonstrate:

**Step 1:** Start backend and show thread creation logs

```
INFO: ClientAcceptor thread started
INFO: BroadcastThread started
INFO: StockPriceGenerator thread started
```

**Step 2:** Monitor threads with JConsole

```powershell
jconsole
# Connect to your Java process
# Go to "Threads" tab
# Show: ClientAcceptor, pool-threads, BroadcastThread
```

**Step 3:** Show thread pool in action

```java
// ConnectionManager.java
clientHandlerPool.submit(() -> handleClient(clientId, clientChannel));
// Each new client gets a thread from the pool
```

**Visual Demonstration:**

- Connect 3 TCP clients simultaneously
- Show logs: "Client handler thread started for client-123"
- Show all 3 being handled concurrently

**Talking Points:**

- Acceptor Thread: Accepts new connections
- Thread Pool: Reusable worker threads (efficient)
- Broadcast Thread: Sends updates to all clients
- Generator Thread: Produces stock prices
- Each thread runs independently and concurrently

---

## ✅ 5. Concurrency Control

### What it is:

Thread-safe data structures and synchronization to prevent race conditions

### Where in your code:

**File:** `backend/src/main/java/com/stockcast/service/SubscriptionManager.java`

```java
// Lines 19-23: Thread-safe collections
private final Map<String, ClientInfo> clients = new ConcurrentHashMap<>();
private final Map<String, Set<String>> tickerSubscriptions = new ConcurrentHashMap<>();

// ClientInfo.java - Line 8
private Set<String> subscriptions = ConcurrentHashMap.newKeySet();
```

### How to Demonstrate:

**Step 1:** Show concurrent modifications

```powershell
# Terminal 1: Subscribe from browser
# Terminal 2: Subscribe from TCP client
# Terminal 3: Subscribe from another browser
# All happen at the same time
```

**Step 2:** Explain the problem without concurrency control

```java
// ❌ Without ConcurrentHashMap (would crash):
Map<String, ClientInfo> clients = new HashMap<>();
// Thread 1: clients.put("client1", info1);
// Thread 2: clients.put("client2", info2);
// Result: ConcurrentModificationException or data corruption

// ✅ With ConcurrentHashMap (thread-safe):
Map<String, ClientInfo> clients = new ConcurrentHashMap<>();
// Multiple threads can safely modify simultaneously
```

**Step 3:** Demonstrate thread safety

```java
// SubscriptionManager.java
public void subscribe(String clientId, String... tickers) {
    // Multiple threads calling this simultaneously
    ClientInfo clientInfo = clients.get(clientId); // Thread-safe read
    clientInfo.subscribe(normalizedTicker); // Thread-safe write
    tickerSubscriptions.computeIfAbsent(...); // Atomic operation
}
```

**Stress Test:**

```powershell
# Run this script to test concurrent access
for ($i=1; $i -le 10; $i++) {
    Start-Job -ScriptBlock {
        java -cp target/classes com.stockcast.client.StockCastClient
    }
}
# All 10 clients can subscribe/unsubscribe without errors
```

**Talking Points:**

- ConcurrentHashMap: Thread-safe map (no locking needed)
- newKeySet(): Thread-safe Set implementation
- Atomic operations: computeIfAbsent, putIfAbsent
- No synchronized blocks needed (ConcurrentHashMap handles it)
- Prevents race conditions and data corruption

**Code to Show:**

```java
// Thread-safe subscription
Set<String> subscribers = tickerSubscriptions.computeIfAbsent(
    ticker,
    k -> ConcurrentHashMap.newKeySet()
);
subscribers.add(clientId); // Multiple threads can do this safely
```

---

## ✅ 6. WebSocket

### What it is:

Full-duplex communication over a single TCP connection for real-time web apps

### Where in your code:

**File:** `backend/src/main/java/com/stockcast/websocket/StockWebSocketHandler.java`

```java
// Line 27: WebSocket handler
public class StockWebSocketHandler extends TextWebSocketHandler

// Lines 40-51: Connection handling
public void afterConnectionEstablished(WebSocketSession session)
```

**File:** `backend/src/main/java/com/stockcast/config/WebSocketConfig.java`

```java
// WebSocket endpoint registration
registry.addHandler(stockWebSocketHandler, "/ws/stock")
```

### How to Demonstrate:

**Step 1:** Open browser at `http://localhost:3000`

**Step 2:** Open browser DevTools (F12) → Network tab → WS filter

- You'll see: `ws://localhost:9091/ws/stock`
- Status: 101 Switching Protocols (HTTP upgrade to WebSocket)

**Step 3:** Show WebSocket frames

- In DevTools → WS connection → Messages tab
- See continuous bidirectional messages:

```
⬆️ SENT:
{"command":"SUBSCRIBE","tickers":["AAPL","GOOGL"]}

⬇️ RECEIVED:
{"type":"ACK","message":"Subscribed to: AAPL, GOOGL"}

⬇️ RECEIVED (every 2 seconds):
{"type":"PRICE","ticker":"AAPL","price":150.25,"changePercent":1.5}
{"type":"PRICE","ticker":"GOOGL","price":2800.50,"changePercent":-0.3}
```

**Step 4:** Show persistent connection

```javascript
// Frontend: WebSocketService.js
connect((url = "ws://localhost:9091/ws/stock"));
// Connection stays open - no polling needed!
```

**Comparison with HTTP:**

```
❌ HTTP Polling (old way):
Client → Server: GET /prices (every 2 seconds)
- New connection each time
- High overhead

✅ WebSocket (your way):
Client ⟷ Server: Single persistent connection
- Bidirectional
- Real-time push from server
- Low latency
```

**Talking Points:**

- WebSocket = upgrade from HTTP to persistent connection
- Full-duplex = both send/receive simultaneously
- Low latency (no connection overhead)
- Perfect for real-time apps (chat, trading, gaming)
- Port 9091 in your project

---

## ✅ 7. Performance Monitoring

### What it is:

Real-time metrics collection and REST API for system monitoring

### Where in your code:

**File:** `backend/src/main/java/com/stockcast/service/MetricsService.java`

```java
// Lines 14-20: Atomic counters (thread-safe)
private final AtomicLong totalMessagesReceived = new AtomicLong(0);
private final AtomicLong totalMessagesSent = new AtomicLong(0);
private final AtomicInteger activeConnections = new AtomicInteger(0);
```

**File:** `backend/src/main/java/com/stockcast/controller/MetricsController.java`

```java
// REST endpoints for metrics
@GetMapping("/api/metrics")
@GetMapping("/api/metrics/connections")
@GetMapping("/api/metrics/messages")
```

### How to Demonstrate:

**Step 1:** View metrics in browser

```
Open: http://localhost:8080/api/metrics
```

**Example Response:**

```json
{
  "totalConnections": 15,
  "activeConnections": 5,
  "totalMessagesReceived": 487,
  "totalMessagesSent": 1532,
  "totalSubscriptions": 12,
  "activeTickers": ["AAPL", "GOOGL", "MSFT", "AMZN"],
  "uptime": "00:15:23",
  "systemMetrics": {
    "usedMemory": "250 MB",
    "totalMemory": "512 MB",
    "freeMemory": "262 MB",
    "cpuCores": 8
  }
}
```

**Step 2:** Show metrics updating in real-time

```powershell
# Watch metrics change
while ($true) {
    curl http://localhost:8080/api/metrics/messages
    Start-Sleep -Seconds 2
}
```

**Step 3:** Show frontend metrics panel

- Open frontend at `http://localhost:3000`
- Scroll to "Performance Metrics" section
- Shows live updates every 5 seconds

**Step 4:** Demonstrate metric types

**Connection Metrics:**

```powershell
curl http://localhost:8080/api/metrics/connections
```

```json
{
  "activeConnections": 5,
  "totalConnectionsHandled": 23,
  "activeWebSocketClients": 3,
  "activeTcpClients": 2
}
```

**Message Metrics:**

```powershell
curl http://localhost:8080/api/metrics/messages
```

```json
{
  "totalMessagesReceived": 487,
  "totalMessagesSent": 1532,
  "messagesPerSecond": 25.3
}
```

**Subscription Metrics:**

```powershell
curl http://localhost:8080/api/metrics/subscriptions
```

```json
{
  "totalSubscriptions": 12,
  "activeTickers": ["AAPL", "GOOGL", "MSFT"],
  "subscriptionsPerClient": 2.4
}
```

**Talking Points:**

- AtomicLong/AtomicInteger = thread-safe counters
- No locking needed for incrementing
- REST API exposes metrics for monitoring tools
- Real-time dashboard in frontend
- Can integrate with Grafana, Prometheus

---

## ✅ 8. REST API

### What it is:

HTTP-based API for stateless request/response operations

### Where in your code:

**File:** `backend/src/main/java/com/stockcast/controller/MetricsController.java`

```java
@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    @GetMapping
    @GetMapping("/connections")
    @GetMapping("/messages")
    @GetMapping("/subscriptions")
    @GetMapping("/system")
    @GetMapping("/reset")
}
```

### How to Demonstrate:

**Step 1:** List all API endpoints

```powershell
# Get all metrics
curl http://localhost:8080/api/metrics

# Get connection metrics
curl http://localhost:8080/api/metrics/connections

# Get message metrics
curl http://localhost:8080/api/metrics/messages

# Get subscription metrics
curl http://localhost:8080/api/metrics/subscriptions

# Get system metrics
curl http://localhost:8080/api/metrics/system

# Reset metrics (admin operation)
curl http://localhost:8080/api/metrics/reset
```

**Step 2:** Show in browser

```
http://localhost:8080/api/metrics
```

**Step 3:** Use Postman or browser extensions

- Import all endpoints
- Show GET requests
- Show JSON responses

**Step 4:** Show CORS configuration

```java
// MetricsController.java
@CrossOrigin(origins = "http://localhost:3000")
// Allows frontend to call API from different port
```

**Talking Points:**

- REST = Representational State Transfer
- Stateless (each request independent)
- Uses HTTP methods (GET)
- JSON response format
- Port 8080 (Tomcat embedded server)
- CORS enabled for frontend access

---

## 🎯 Complete Demonstration Sequence

### Full Demo Script (15 minutes)

**1. Start Everything (2 min)**

```powershell
# Terminal 1: Backend
cd D:\StockCast\backend
mvn spring-boot:run

# Terminal 2: Frontend
cd D:\StockCast\frontend
npm start

# Terminal 3: TCP Client
cd D:\StockCast\backend
java -cp target/classes com.stockcast.client.StockCastClient

# Terminal 4: UDP Listener
java -cp target/classes com.stockcast.client.UDPMulticastListener
```

**2. Demonstrate Each Concept (10 min)**

| Concept                    | What to Show                                         | Where                             |
| -------------------------- | ---------------------------------------------------- | --------------------------------- |
| **TCP Sockets**            | TCP client connecting, sending commands              | Terminal 3                        |
| **UDP/Multicast**          | Multiple listeners receiving same broadcast          | Terminal 4 (open 2-3 more)        |
| **Java NIO**               | Single BroadcastThread handling multiple connections | Show code + thread dump           |
| **Multi-threading**        | Thread pool, acceptor thread, generator thread       | JConsole or logs                  |
| **Concurrency**            | 5+ clients subscribing simultaneously without errors | Multiple terminals                |
| **WebSocket**              | Browser DevTools → WS frames                         | Browser F12                       |
| **Performance Monitoring** | Live metrics API                                     | http://localhost:8080/api/metrics |
| **REST API**               | HTTP requests returning JSON                         | Browser/Postman                   |

**3. Q&A (3 min)**

---

## 📊 Visual Aids to Prepare

### Architecture Diagram

```
┌─────────────┐         ┌──────────────────┐
│   Browser   │◄───WS───┤                  │
│   (React)   │         │   Spring Boot    │
└─────────────┘         │     Server       │
                        │   Port 8080/9091 │
┌─────────────┐         │                  │
│ TCP Client  │◄───TCP──┤   • NIO Engine   │
│  (Java)     │         │   • Thread Pool  │
└─────────────┘         │   • Concurrency  │
                        │   • Metrics       │
┌─────────────┐         │                  │
│UDP Listener │◄───UDP──┤                  │
│ (Multicast) │         └──────────────────┘
└─────────────┘               Port 9092
```

### Threading Model

```
Main Thread
  ├─► ClientAcceptor Thread (accepts connections)
  ├─► BroadcastThread (NIO - sends to all clients)
  ├─► StockPriceGenerator Thread (generates prices)
  └─► Thread Pool
       ├─► Worker-1 (handles client A)
       ├─► Worker-2 (handles client B)
       └─► Worker-N (handles client N)
```

---

## 🎓 Key Interview Questions & Answers

**Q: Why use NIO over traditional I/O?**
A: NIO uses Selectors - one thread can manage thousands of connections. Traditional I/O needs one thread per connection, which doesn't scale.

**Q: What's the difference between TCP and UDP?**
A: TCP is reliable, connection-oriented, guarantees order. UDP is connectionless, faster, used for broadcasting (like our multicast).

**Q: How do you prevent race conditions?**
A: Using ConcurrentHashMap and thread-safe collections. They handle synchronization internally without explicit locking.

**Q: Why WebSocket over HTTP?**
A: WebSocket is persistent, bidirectional, lower latency. HTTP requires new connection for each request - inefficient for real-time updates.

**Q: How do you monitor system performance?**
A: REST API exposes metrics (connections, messages, memory). Frontend dashboard displays real-time data using AtomicLong counters.

---

## ✅ Checklist Before Demonstration

- [ ] Backend compiles without errors
- [ ] Frontend runs without errors
- [ ] Can see "StockCast server started on port 9092"
- [ ] Can see "WebSocket endpoint available at ws://localhost:9091/ws/stock"
- [ ] TCP client connects successfully
- [ ] UDP listener receives broadcasts
- [ ] Browser shows "Connected" status
- [ ] Stock prices update every 2 seconds
- [ ] Metrics API returns data
- [ ] Multiple clients can connect simultaneously

---

Good luck! You've built a sophisticated networking application! 🚀
