# 🎯 StockCast Project Demonstration Guide

## 📋 Pre-Demonstration Checklist

### Before the Presentation:

- [ ] Backend server is NOT running
- [ ] Frontend is NOT running
- [ ] TCP client is NOT running
- [ ] UDP listener is NOT running
- [ ] Browser is ready at blank tab
- [ ] 4 terminals prepared and labeled

---

## 🎬 DEMONSTRATION FLOW (30-40 minutes)

---

## **PART 1: Project Overview (5 minutes)**

### **Introduction**

> "Good [morning/afternoon]. Today I'm presenting **StockCast** - a distributed real-time stock price broadcasting system built with Java Spring Boot and React."

### **Project Architecture Overview**

#### **Key Components:**

```
┌─────────────────────────────────────────────────────┐
│                  StockCast System                    │
├─────────────────────────────────────────────────────┤
│                                                      │
│  ┌──────────────┐         ┌──────────────┐         │
│  │   Backend    │────────▶│   Frontend   │         │
│  │ (Spring Boot)│         │   (React)    │         │
│  │  Port 9091   │         │  Port 3000   │         │
│  │  Port 9092   │         │              │         │
│  └──────────────┘         └──────────────┘         │
│         │                                            │
│         ├─── TCP Server (Port 9092)                 │
│         ├─── WebSocket Server (Port 9091/ws/stock)  │
│         └─── UDP Multicast (230.0.0.1:4446)         │
│                                                      │
│  Clients:                                            │
│  • Console TCP Client (Java NIO)                    │
│  • Web Browser (React + WebSocket)                  │
│  • UDP Multicast Listener (Java)                    │
└─────────────────────────────────────────────────────┘
```

#### **Technologies Used:**

- **Backend**: Java 17, Spring Boot 3.2, NIO SocketChannel, WebSockets
- **Frontend**: React 18, WebSocket API, Chart.js
- **Protocols**: TCP, WebSocket, UDP Multicast
- **Build Tools**: Maven, npm

#### **Core Features:**

1. ✅ Real-time stock price generation and broadcasting
2. ✅ Multiple client types (TCP, WebSocket, UDP)
3. ✅ Publish-Subscribe pattern for selective updates
4. ✅ Live metrics and performance monitoring
5. ✅ Multi-threaded concurrent client handling

---

## **PART 2: System Architecture Deep Dive (5 minutes)**

### **Explain the 5-Member Architecture**

> "This project demonstrates distributed systems concepts by dividing responsibilities across 5 key modules:"

```
┌────────────────────────────────────────────────────────┐
│  Member 1: Stock Price Generator & Broadcast Module   │
│  • Generates realistic stock prices (5 tickers)        │
│  • Broadcasts to all subscribed clients                │
│  • File: StockPriceGenerator.java, BroadcastModule.java│
└────────────────────────────────────────────────────────┘
                          ↓
┌────────────────────────────────────────────────────────┐
│  Member 2: Connection Manager (TCP/WebSocket)         │
│  • Accepts TCP connections on port 9092                │
│  • Manages WebSocket connections on port 9091          │
│  • File: ConnectionManager.java                        │
└────────────────────────────────────────────────────────┘
                          ↓
┌────────────────────────────────────────────────────────┐
│  Member 3: Subscription Manager                        │
│  • Tracks client subscriptions (who wants what)        │
│  • Uses thread-safe ConcurrentHashMap                  │
│  • File: SubscriptionManager.java                      │
└────────────────────────────────────────────────────────┘
                          ↓
┌────────────────────────────────────────────────────────┐
│  Member 4: UDP Multicast Broadcaster                   │
│  • Broadcasts announcements to 230.0.0.1:4446          │
│  • One-to-many communication (no subscription needed)  │
│  • File: UDPMulticastBroadcaster.java                  │
└────────────────────────────────────────────────────────┘
                          ↓
┌────────────────────────────────────────────────────────┐
│  Member 5: Client Applications                         │
│  • TCP Console Client (StockCastClient.java)           │
│  • Web Browser Client (React App)                      │
│  • UDP Listener (UDPMulticastListener.java)            │
└────────────────────────────────────────────────────────┘
```

### **Key Concepts Demonstrated:**

1. **TCP vs UDP vs WebSocket:**

   - **TCP**: Reliable, connection-oriented, bidirectional (console client)
   - **WebSocket**: Full-duplex over HTTP, browser-friendly (web client)
   - **UDP Multicast**: Connectionless, broadcast to group (announcements)

2. **Concurrency:**

   - Thread pools for handling multiple clients
   - Thread-safe collections (ConcurrentHashMap)
   - Non-blocking I/O (NIO) and Selectors

3. **Design Patterns:**
   - **Publish-Subscribe**: Clients subscribe to specific stock tickers
   - **Observer Pattern**: Clients receive updates when prices change
   - **Singleton**: WebSocket service, Broadcast module

---

## **PART 3: Live Demonstration (20 minutes)**

### **Setup: Arrange 4 Terminal Windows**

```
┌─────────────────┬─────────────────┐
│   Terminal 1    │   Terminal 2    │
│   (Backend)     │   (Frontend)    │
│                 │                 │
├─────────────────┼─────────────────┤
│   Terminal 3    │   Terminal 4    │
│  (TCP Client)   │ (UDP Listener)  │
│                 │                 │
└─────────────────┴─────────────────┘
```

---

### **STEP 1: Start the Backend Server**

**Terminal 1 (Backend):**

```powershell
cd D:\StockCast
.\run-server.ps1
```

#### **Explain What Happens:**

> "The backend server is starting up. Let me explain what you're seeing:"

**Watch for these logs:**

```
✓ Spring Boot application starting
✓ UDP Multicast initialized on 230.0.0.1:4446
✓ Tomcat started on port 9091 (HTTP + WebSocket)
✓ Stock price generator started with 1000ms interval
✓ TCP Server running on port 9092
✓ Available tickers: AAPL, GOOG, MSFT, AMZN, TSLA
```

#### **Key Points to Highlight:**

- ✅ **Multi-protocol support**: TCP (9092), WebSocket (9091), UDP (4446)
- ✅ **Spring Boot auto-configuration**: Automatic server startup
- ✅ **5 stock tickers**: AAPL, GOOG, MSFT, AMZN, TSLA
- ✅ **1-second update interval**: Realistic price updates

**Wait for:**

```
StockCast Server is ready to accept connections
```

---

### **STEP 2: Start the Web Frontend**

**Terminal 2 (Frontend):**

```powershell
cd D:\StockCast
.\run-frontend.ps1
```

#### **Explain What Happens:**

> "This is starting our React development server, which hosts the web-based client."

**Watch for:**

```
Compiled successfully!
webpack compiled successfully
Local: http://localhost:3000
```

**Open Browser:**

- Navigate to `http://localhost:3000`
- **Share screen to show the web interface**

#### **Explain the Web Interface:**

**Point out each component:**

1. **Connection Status (Top)**

   ```
   🟢 Connected | Client ID: xxxxxxxx
   ```

   - Shows real-time connection status
   - Displays unique client ID assigned by server

2. **Subscription Panel (Left)**

   ```
   Available Tickers:
   [+] AAPL  [+] GOOG  [+] MSFT  [+] AMZN  [+] TSLA

   Your Subscriptions:
   (empty initially)
   ```

   - Click to subscribe to stock updates
   - Demonstrates publish-subscribe pattern

3. **Stock Cards (Center)**

   ```
   ┌──────────────────┐
   │  AAPL  $150.00   │
   │  ↑ +0.50 (0.33%) │
   │  [Price Chart]   │
   └──────────────────┘
   ```

   - Real-time price updates
   - Color-coded changes (green/red)
   - Live sparkline chart

4. **Metrics Panel (Right)**
   ```
   📊 System Metrics
   Active Clients: 1
   Messages/sec: 15
   Uptime: 00:02:30
   ```
   - Live server metrics
   - Updated every second

#### **Live Demo - Subscribe to Stocks:**

**Action 1: Subscribe to AAPL**

- Click the "+ AAPL" button
- **Watch what happens:**
  - Button becomes "- AAPL" (unsubscribe option)
  - AAPL card appears and starts updating
  - Chart begins to populate
  - Server logs show: `WebSocket session subscribed to AAPL`

**Action 2: Subscribe to GOOG and MSFT**

- Click "+ GOOG" and "+ MSFT"
- **Observe:**
  - Multiple cards update simultaneously
  - Each has independent price movements
  - Charts update in real-time

**Action 3: Unsubscribe from AAPL**

- Click "- AAPL"
- **Observe:**
  - Updates stop for AAPL
  - Other stocks continue updating
  - Card remains visible with last price

---

### **STEP 3: Start TCP Console Client**

**Terminal 3 (TCP Client):**

```powershell
cd D:\StockCast
.\run-client.ps1
```

#### **Explain What Happens:**

> "Now we're connecting a command-line TCP client. This demonstrates direct socket communication without a browser."

**Client Output:**

```
🔧 Configuring terminal for clean input...
✓ Terminal configured

Connecting to StockCast Server at localhost:9092...

Connected to StockCast server at localhost:9092
======================================================================
Available Commands:
  subscribe <tickers>    - Subscribe to stock updates
  unsubscribe <tickers>  - Unsubscribe from stock updates
  list                   - Show current subscriptions
  ping                   - Check server connection
  clear                  - Clear screen
  help                   - Show help
  quit                   - Exit
======================================================================

READY for commands. Type 'help' for options.
======================================================================

stockcast>
```

**Server logs (Terminal 1) show:**

```
New client connected: xxxxxxxx
Client registered: xxxxxxxx
```

#### **Live Demo - TCP Client Commands:**

**Command 1: Test Connection**

```
stockcast> ping
```

**Output:**

```
Server is alive (PONG)
```

> "This demonstrates request-response over TCP."

**Command 2: Subscribe to Stocks**

```
stockcast> subscribe AAPL,TSLA
```

**Output:**

```
✓ Subscribed to AAPL,TSLA
[12:34:56] AAPL   $150.25    ↑ +0.75
[12:34:57] TSLA   $245.80    ↓ -1.20
[12:34:58] AAPL   $150.30    ↑ +0.05
[12:34:59] TSLA   $245.75    ↓ -0.05
```

> "Notice how we're receiving real-time price updates in the console. The arrow indicators show price movements."

**Command 3: Check Subscriptions**

```
stockcast> list
```

**Output:**

```
Your subscriptions: AAPL, TSLA
```

**Command 4: Unsubscribe**

```
stockcast> unsubscribe TSLA
```

**Output:**

```
✓ Unsubscribed from TSLA
```

> "Updates for TSLA stop, but AAPL continues."

---

### **STEP 4: Start UDP Multicast Listener**

**Terminal 4 (UDP Listener):**

```powershell
cd D:\StockCast
.\run-udp-listener.ps1
```

#### **Explain What Happens:**

> "This is a UDP multicast listener. Unlike TCP, it doesn't establish a connection - it just joins a multicast group and listens for announcements."

**Client Output:**

```
═══════════════════════════════════════════════════════════
  StockCast UDP Multicast Listener
═══════════════════════════════════════════════════════════
Joining multicast group: 230.0.0.1:4446
✓ Connected! Listening for announcements...
Press Ctrl+C to exit
═══════════════════════════════════════════════════════════
```

#### **Trigger UDP Broadcast from Browser:**

**Go to browser metrics panel** and look for broadcast announcements (or trigger manually from server).

**UDP Listener Output:**

```
[12:35:01] 📰 NEWS: Market volatility increased
[12:35:15] ⚠️ ALERT: Trading volume surge detected
[12:36:00] 📊 MARKET: Session closing in 30 minutes
[12:36:30] ℹ️ SYSTEM: Server health check passed
```

#### **Key Concepts to Explain:**

> "Notice these important differences:"

1. **No subscription needed**: UDP listener receives ALL broadcasts
2. **One-to-many**: Server sends once, all listeners receive
3. **No acknowledgment**: Fire-and-forget protocol
4. **Different use case**: System-wide announcements, not individual updates

---

### **STEP 5: Show Concurrent Operation**

**Now all 4 terminals are running. Demonstrate:**

#### **Scenario 1: Multiple Clients, Same Stock**

**In Browser:**

- Subscribe to MSFT

**In TCP Client:**

```
stockcast> subscribe MSFT
```

**Observe:**

- Both clients receive MSFT updates simultaneously
- Server is broadcasting to multiple clients efficiently
- Check metrics in browser: "Active Clients: 2"

#### **Scenario 2: Different Subscriptions**

**In Browser:**

- Subscribed to: AAPL, GOOG, MSFT

**In TCP Client:**

- Subscribed to: AAPL, TSLA

**Observe:**

- Each client receives only subscribed tickers
- AAPL updates go to both
- GOOG/MSFT only to browser
- TSLA only to console
- UDP listener receives all announcements

> "This demonstrates the publish-subscribe pattern: clients only receive data they're interested in, reducing network traffic."

---

### **STEP 6: Show Real-Time Metrics**

**Focus on Browser Metrics Panel:**

```
📊 System Metrics
━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Active Clients:       2
Messages/sec:         ~45
Total Messages:       2,847
Uptime:              00:04:30
Subscriptions:        5
Most Popular:         AAPL (2 clients)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

**Explain Each Metric:**

- **Active Clients**: Real-time count (WebSocket + TCP)
- **Messages/sec**: Current throughput
- **Total Messages**: Cumulative since server start
- **Uptime**: Server running time
- **Subscriptions**: Total across all clients
- **Most Popular**: Which stock has most subscribers

**Server Logs (Terminal 1) - Periodic Metrics:**

```
═══════════════════════════════════════════════════════════
              PERFORMANCE METRICS
═══════════════════════════════════════════════════════════
Uptime:               00:04:00
Active Clients:       2 (TCP: 1, WebSocket: 1)
Total Connections:    2
Messages Sent:        2,400
Throughput:           10.00 msg/sec
Price Updates:        240
UDP Multicasts:       4
Total Subscriptions:  5
Most Popular:         AAPL
═══════════════════════════════════════════════════════════
```

---

## **PART 4: Technical Deep Dive (5 minutes)**

### **Code Walkthrough (Show Key Files)**

#### **1. Stock Price Generator**

**File**: `backend/src/main/java/com/stockcast/service/StockPriceGenerator.java`

**Explain:**

```java
@Scheduled(fixedRate = 1000)  // Runs every 1 second
public void generateAndBroadcast() {
    for (String ticker : tickers) {
        StockPrice price = generateStockPrice(ticker);
        broadcastModule.broadcastStockPrice(price);
    }
}
```

> "Spring's @Scheduled annotation runs this every second, generating realistic price changes using random walk algorithm."

#### **2. Connection Manager - TCP Handler**

**File**: `backend/src/main/java/com/stockcast/service/ConnectionManager.java`

**Explain:**

```java
private void handleClient(String clientId, SocketChannel channel) {
    channel.configureBlocking(true);  // Blocking mode for reliability
    ByteBuffer buffer = ByteBuffer.allocate(1024);

    while (running && channel.isOpen()) {
        int bytesRead = channel.read(buffer);
        if (bytesRead > 0) {
            String command = parseCommand(buffer);
            processClientMessage(clientId, command);
        }
    }
}
```

> "Each client runs in its own thread. We use blocking I/O for simplicity, which is perfect for moderate client loads."

#### **3. Subscription Manager**

**File**: `backend/src/main/java/com/stockcast/service/SubscriptionManager.java`

**Explain:**

```java
// Thread-safe collections for concurrent access
private final Map<String, ClientInfo> clients = new ConcurrentHashMap<>();
private final Map<String, Set<String>> tickerSubscriptions = new ConcurrentHashMap<>();

public void subscribe(String clientId, String ticker) {
    // Add client to ticker's subscriber set
    tickerSubscriptions
        .computeIfAbsent(ticker, k -> ConcurrentHashMap.newKeySet())
        .add(clientId);
}
```

> "ConcurrentHashMap allows safe access from multiple threads without explicit locking."

#### **4. React WebSocket Integration**

**File**: `frontend/src/services/WebSocketService.js`

**Explain:**

```javascript
connect(url) {
  this.ws = new WebSocket(url);

  this.ws.onmessage = (event) => {
    const data = JSON.parse(event.data);
    this.listeners.onMessage.forEach(cb => cb(data));
  };
}
```

> "Simple WebSocket API usage. The service acts as an event emitter, notifying React components of updates."

---

## **PART 5: Testing Edge Cases (3 minutes)**

### **Test 1: Client Disconnect Handling**

**Action:** Close TCP client (Ctrl+C in Terminal 3)

**Server Logs:**

```
Client disconnected: xxxxxxxx
Client unregistered: xxxxxxxx
```

**Browser Metrics:**

```
Active Clients: 1 (was 2)
```

> "The server detects disconnection and cleans up resources automatically."

### **Test 2: Rapid Subscriptions**

**In Browser:** Quickly click subscribe on all 5 stocks

**Observe:**

- All 5 cards populate immediately
- No duplicate messages
- Smooth performance

> "The system handles rapid state changes efficiently."

### **Test 3: Server Restart Resilience**

**Action:** Stop server (Ctrl+C in Terminal 1)

**Browser Shows:**

```
🔴 Disconnected. Reconnecting...
```

**WebSocket Service:**

```javascript
onclose = () => {
  // Automatic reconnection every 3 seconds
  setInterval(() => this.connect(url), 3000);
};
```

**Restart Server:** `.\run-server.ps1`

**Browser:**

```
🟢 Connected
```

> "Clients automatically reconnect when the server comes back online."

---

## **PART 6: Wrap-Up & Q&A (5 minutes)**

### **Key Achievements Summary**

```
✅ Multi-Protocol Server
   • TCP, WebSocket, UDP Multicast

✅ Real-Time Broadcasting
   • Sub-second latency
   • Concurrent client handling

✅ Publish-Subscribe Pattern
   • Efficient data distribution
   • Client-side filtering

✅ Scalable Architecture
   • Thread-safe collections
   • Non-blocking I/O ready

✅ Modern Web Interface
   • React with live charts
   • Real-time metrics

✅ Production-Ready Features
   • Auto-reconnection
   • Error handling
   • Clean shutdown
```

### **Technologies Demonstrated**

| Layer           | Technology        | Purpose               |
| --------------- | ----------------- | --------------------- |
| **Backend**     | Spring Boot       | Server framework      |
|                 | Java NIO          | Non-blocking TCP      |
|                 | WebSocket         | Browser communication |
|                 | UDP Multicast     | Broadcast messaging   |
| **Frontend**    | React 18          | UI framework          |
|                 | WebSocket API     | Real-time updates     |
|                 | Chart.js          | Data visualization    |
| **Concurrency** | Thread Pools      | Multi-client handling |
|                 | ConcurrentHashMap | Thread-safe storage   |
| **Build**       | Maven             | Backend build         |
|                 | npm/webpack       | Frontend build        |

### **Potential Improvements (Future Work)**

1. **Add Authentication**: JWT tokens for client verification
2. **Database Integration**: Persist historical price data
3. **Load Balancing**: Multiple server instances
4. **Message Queue**: Apache Kafka for scaling
5. **Monitoring**: Prometheus + Grafana
6. **Docker**: Containerization for deployment

---

## **Q&A Preparation**

### **Common Questions & Answers:**

#### **Q1: "Why use TCP instead of just WebSockets?"**

> "TCP demonstrates lower-level socket programming. WebSockets are HTTP-based and browser-friendly, while TCP shows raw network communication fundamentals."

#### **Q2: "How does the system handle 1000 clients?"**

> "Current architecture supports ~100-200 concurrent clients. For 1000+, we'd need:
>
> - Non-blocking I/O with Selectors
> - Connection pooling
> - Horizontal scaling with load balancers"

#### **Q3: "What happens if a client is slow to process messages?"**

> "Each client has a send buffer. If full, the server either:
>
> - Blocks (current implementation)
> - Drops messages (UDP style)
> - Queues with backpressure (production approach)"

#### **Q4: "Why UDP for announcements?"**

> "UDP multicast is perfect for:
>
> - System-wide broadcasts
> - When delivery guarantee isn't critical
> - Reducing server load (send once, many receive)"

#### **Q5: "How do you ensure thread safety?"**

> "We use:
>
> - ConcurrentHashMap for shared state
> - Thread pools for client handlers
> - Immutable message objects
> - Synchronized blocks only where necessary"

---

## **🎬 Demonstration Checklist**

### **Before Demo:**

- [ ] All code committed to Git
- [ ] Maven dependencies downloaded
- [ ] npm packages installed
- [ ] PowerShell scripts tested
- [ ] Browser cache cleared
- [ ] Terminal font size readable

### **During Demo:**

- [ ] Explain before showing
- [ ] Show logs while explaining
- [ ] Point out key output lines
- [ ] Pause for questions
- [ ] Show metrics panel frequently

### **After Demo:**

- [ ] Offer to show specific code
- [ ] Provide GitHub repository link
- [ ] Share documentation files
- [ ] Thank the instructor

---

## **📁 Files to Have Ready**

### **Documentation to Share:**

1. `README.md` - Project overview
2. `PROJECT_STRUCTURE.md` - Architecture details
3. `QUICKSTART.md` - Setup instructions
4. `UDP_MULTICAST.md` - UDP explanation
5. This demo guide

### **Code to Highlight:**

1. `StockPriceGenerator.java` - Price generation logic
2. `ConnectionManager.java` - TCP handling
3. `SubscriptionManager.java` - Pub-sub pattern
4. `WebSocketService.js` - Frontend WebSocket
5. `App.js` - React component structure

---

## **🎯 Time Management**

| Section    | Time        | Content                             |
| ---------- | ----------- | ----------------------------------- |
| Intro      | 5 min       | Overview + Architecture             |
| Deep Dive  | 5 min       | Explain 5-member design             |
| Live Demo  | 20 min      | Start all components, show features |
| Code Walk  | 5 min       | Key code snippets                   |
| Edge Cases | 3 min       | Testing scenarios                   |
| Wrap-up    | 5 min       | Summary + Q&A                       |
| **Total**  | **~40 min** |                                     |

---

## **💡 Pro Tips**

1. **Start with working system**: Don't live-code, show working demo
2. **Keep terminals visible**: Arrange so logs are always visible
3. **Explain THEN show**: Concept first, then demonstration
4. **Use metrics panel**: It's impressive and shows real-time nature
5. **Have backup plan**: Screenshots if live demo fails
6. **Enthusiasm matters**: Show excitement about the project
7. **Connect to theory**: Mention course concepts (concurrency, protocols, patterns)

---

## **🚀 Closing Statement**

> "In summary, StockCast demonstrates:
>
> - Multi-protocol server architecture
> - Real-time data distribution at scale
> - Modern web development practices
> - Concurrent programming patterns
> - Production-ready error handling
>
> The system is fully functional, well-documented, and ready for extension. Thank you for your time. Are there any questions?"

---

**Good luck with your demonstration! 🎉**
