# StockCast - 15-Minute Live Demonstration Guide

## Pre-Demo Setup (2 minutes before demo starts)
Ensure all terminals are ready:
- Terminal 1: Backend server (ready to launch)
- Terminal 2: Frontend (ready to launch)
- Terminal 3: TCP client (ready to launch)
- Terminal 4: UDP listener (ready to launch)

All code is built and ready. Just execute the scripts.

---

## Demo Timeline: 15 Minutes

### **00:00-00:30 | Introduction (30 seconds)**
**What to say:**
"StockCast is a real-time distributed stock market data system. It demonstrates a modern multi-protocol architecture using Java Spring Boot with concurrent client handling, publish-subscribe patterns, and UDP multicast broadcasting. We have three different client types that can connect simultaneously."

### **00:30-02:00 | Architecture Overview (90 seconds)**
**What to say:**
"The system has four core components:

1. **Stock Price Generator** - Creates random stock prices every second for AAPL, GOOG, MSFT, TSLA
2. **Connection Manager** - Manages all client connections (TCP, WebSocket)
3. **Subscription Manager** - Handles selective stock updates based on subscriptions
4. **UDP Multicast Broadcaster** - Sends announcements to everyone listening

The beauty is that clients can connect via:
- **TCP Console Client** - Direct socket connection, command-driven (ping, subscribe)
- **WebSocket Browser Client** - React frontend with real-time updates
- **UDP Multicast Listener** - Monitoring all system-wide broadcasts

Let me show you all three in action..."

### **02:00-03:00 | Backend Startup (60 seconds)**

**Terminal 1 - Backend:**
```powershell
.\run-server.ps1
```

**What to say while launching:**
"Starting the Spring Boot backend server. It will initialize:
- TCP server on port 9092
- WebSocket server on port 9091
- Stock Price Generator
- UDP Multicast Broadcaster

Watch for the startup message..."

**Expected output:**
```
[INFO] StockCast server started on port 9092
[INFO] Stock Price Generator started, generating prices every 1 second
[INFO] UDP Multicast broadcaster started on 230.0.0.1:4446
```

**Wait for confirmation**, then say:
"Server is running. Now let's launch the frontend browser client..."

### **03:00-04:00 | Frontend Launch (60 seconds)**

**Terminal 2 - Frontend:**
```powershell
cd .\frontend
.\run-frontend.ps1
```

**What to say:**
"Launching React frontend on port 3000. This uses WebSocket to connect to our backend. We'll see real-time stock price updates in the browser."

**Wait ~10 seconds for Webpack to compile**, then:
- Open browser to `http://localhost:3000`
- Show the dashboard with:
  - Connection status indicator (should show "Connected")
  - Metrics panel showing active clients
  - Stock cards updating in real-time

**Say:**
"Notice the connection status shows 'Connected' - that's the WebSocket connection to our backend. The metrics panel shows we have active clients. And look - the stock prices are updating every second! This is the WebSocket client."

### **04:00-06:00 | TCP Console Client (120 seconds)**

**Terminal 3 - TCP Client:**
```powershell
.\run-client.ps1
```

**What to say:**
"Now let's add a TCP console client. This connects directly via socket to our TCP server. Unlike the browser, it's command-driven."

**Wait for connection message**, then type these commands:

**Command 1: PING**
```
> ping
Response: [PONG] 
```
**Say:** "Ping-pong works - the client sends a message to the server and gets a response. Simple connectivity check."

**Command 2: SUBSCRIBE**
```
> subscribe AAPL
Response: [SUBSCRIBED] AAPL
[12:45:15] AAPL: 175.45
[12:45:16] AAPL: 174.89
```
**Say:** "Now we're subscribed to AAPL stock updates. Notice it's selective - we only get AAPL prices, not GOOG or MSFT. This is the publish-subscribe pattern in action."

**Command 3: SUBSCRIBE another ticker**
```
> subscribe GOOG
Response: [SUBSCRIBED] GOOG
[12:45:17] AAPL: 175.23
[12:45:17] GOOG: 1350.67
```
**Say:** "Multiple subscriptions work. Now we're getting both AAPL and GOOG updates. Look at the frontend browser - the Metrics panel now shows 2 TCP clients connected!"

### **06:00-08:00 | UDP Multicast Listener (120 seconds)**

**Terminal 4 - UDP Listener:**
```powershell
.\run-udp-listener.ps1
```

**What to say:**
"Finally, our UDP multicast listener. This connects to the multicast group 230.0.0.1:4446 and receives ALL system broadcasts - not selective like the TCP clients."

**Expected output:**
```
Listening on 230.0.0.1:4446...
[SYSTEM] Market data streaming active
[12:45:18] AAPL: 175.45
[12:45:19] GOOG: 1350.12
[12:45:20] MSFT: 380.05
[12:45:21] TSLA: 245.78
```

**Say:**
"Notice it receives ALL stock prices from all companies, not just subscribed ones. This is the power of UDP multicast - a one-to-many broadcast that's efficient for system-wide announcements and monitoring. Perfect for dashboards, alerts, or logging systems that need to see everything."

### **08:00-10:00 | Multi-Client Demonstration (120 seconds)**

**Now all 4 components are running. Demonstrate the architecture:**

**In TCP Client Terminal:**
```
> subscribe MSFT
```

**What to say:**
"Watch what happens when we add a new subscription. Looking at the frontend, the Metrics panel updates to show the new subscription. The UDP listener continues receiving ALL prices. This is a distributed system - all clients coexisting, different protocols, different subscription models."

**Show three information sources updating:**
1. **Browser** - WebSocket client seeing selective updates
2. **TCP Client** - Console client with multiple subscriptions
3. **UDP Listener** - Monitoring everything

**Say:**
"This demonstrates concurrent client handling, selective subscriptions, and system-wide broadcasting all working together. The backend is managing connections, subscriptions, and broadcasts across three different protocols simultaneously."

### **10:00-12:00 | Architecture Deep-Dive (120 seconds)**

**Use the browser Metrics panel and show:**

**Say:**
"Here's what's happening behind the scenes:

1. **Stock Price Generator** generates a price every second (e.g., AAPL: 175.45)

2. **ConnectionManager receives the update** and checks subscriptions. If any TCP client subscribed, it sends to them.

3. **Same update goes to UDP Multicast Broadcaster** for system-wide announcement. Both TCP and UDP listeners get it simultaneously.

4. **WebSocket clients** subscribed to AAPL get it in real-time through their persistent WebSocket connection.

5. **Metrics Service** tracks all connections and subscriptions for the dashboard.

The elegance: The SAME price update flows through multiple channels - TCP selective, UDP broadcast, WebSocket real-time - without duplication or conflicts. That's multi-protocol architecture done right."

### **12:00-13:30 | Key Technical Highlights (90 seconds)**

**Say:**
"Let me highlight the key technical achievements:

**1. Concurrent Connection Management**
- We're handling 3 different client types simultaneously
- Thread pools manage hundreds of potential connections
- Non-blocking I/O for TCP, separate thread pool for WebSocket
- ConcurrentHashMap for thread-safe subscription tracking

**2. Multi-Protocol Support**
- TCP (low-level socket, direct connection)
- WebSocket (persistent connection for real-time browser updates)
- UDP Multicast (efficient one-to-many broadcasting)
- Each has different use cases, all managed by a single backend

**3. Publish-Subscribe Pattern**
- Clients can selectively subscribe to specific stocks
- Server only sends relevant data to each client
- Reduces network overhead - no unnecessary broadcasts to clients who don't care
- Perfect for selective real-time updates

**4. UDP Multicast for System-wide Monitoring**
- Unlike TCP (one-to-one), multicast goes to everyone listening
- No central registry needed - clients just join the group
- Efficient for announcements, alerts, and monitoring
- Used in real-world systems like financial data feeds"

### **13:30-14:30 | Code Highlights (60 seconds)**

**Optional: Show key code snippets on screen (if time allows):**

1. **StockPriceGenerator.java**
   - Generates prices every 1 second
   - Notifies all listeners automatically

2. **ConnectionManager.java**
   - Manages TCP connections
   - Integrates with UDP broadcaster
   - Handles subscription forwarding

3. **SubscriptionManager.java**
   - ConcurrentHashMap for thread-safe subscriptions
   - Fast lookup: client → subscribed stocks

4. **UDPMulticastBroadcaster.java**
   - Sends to multicast group 230.0.0.1:4446
   - Non-blocking queue for async broadcasting

### **14:30-15:00 | Summary & Key Takeaways (30 seconds)**

**Say:**
"In 15 minutes, we've demonstrated:

✅ Real-time distributed system handling 3 protocol types
✅ Concurrent client management (TCP, WebSocket, UDP multicast)
✅ Selective data delivery via publish-subscribe
✅ System-wide broadcasting via multicast
✅ Live metrics and monitoring
✅ Professional Spring Boot architecture with proper concurrency patterns

This is the foundation for production systems like financial data platforms, IoT monitoring dashboards, and real-time notification systems.

Questions?"

---

## Troubleshooting Quick Fixes

| Issue | Fix |
|-------|-----|
| Backend won't start | Check port 9092 is free: `netstat -ano \| grep 9092` |
| Frontend won't connect | Check WebSocket URL in browser console (F12) |
| TCP client can't connect | Verify backend started, check port 9092 |
| UDP listener shows nothing | Ensure backend is running UDP broadcaster |
| Slow price updates | Normal - generated every 1 second on purpose |
| Multiple connections in metrics | Normal in dev - React StrictMode can cause 2x connections |

---

## Post-Demo

**To save the demo data:**
```powershell
# All terminals will keep running
# Press Ctrl+C in any terminal to stop that component
```

**To reset and restart:**
```powershell
# Stop all terminals and rebuild
mvn clean package -DskipTests
```
