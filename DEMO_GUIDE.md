# StockCast - Demonstration Guide for Network Module

This guide provides step-by-step instructions for demonstrating StockCast in a network module presentation.

## 📋 Pre-Demo Checklist

- [ ] Java 17+ installed: `java -version`
- [ ] Maven 3.6+ installed: `mvn -version`
- [ ] Project built: `mvn clean package` (in backend folder)
- [ ] No process listening on port 9090: `netstat -ano | findstr 9090` (Windows)
- [ ] Terminal windows available: 3-4 for best experience
- [ ] Network concepts document printed or open: [NETWORK_CONCEPTS.md](NETWORK_CONCEPTS.md)

## 🎬 Demo Scenario (15-20 minutes)

### Phase 1: Architecture Overview (3 minutes)

**Show & Explain**:
1. Open the project structure:
   ```
   backend/
   ├── ConnectionManager.java    (Member 1: Connection handling)
   ├── StockPriceGenerator.java   (Member 2: Price simulation)
   ├── SubscriptionManager.java   (Member 3: Subscription tracking)
   ├── BroadcastModule.java       (Member 4: NIO broadcasting)
   └── client/StockCastClient.java (Member 5: Console client)
   ```

2. Highlight key learning points:
   - **NIO vs Blocking I/O**: BroadcastModule uses Selector for non-blocking writes
   - **Threading Model**: Multiple threads (acceptor, handler, generator, broadcaster)
   - **Concurrency**: ConcurrentHashMap for thread-safe subscription tracking
   - **Protocol**: Simple pipe-delimited text protocol

3. Show the data flow diagram from NETWORK_CONCEPTS.md

---

### Phase 2: Start the Server (2 minutes)

**In Terminal 1 (Server)**:
```powershell
cd c:\Users\Anuradha\Downloads\Moratuwa Academic\Projects\StockCast\backend
mvn spring-boot:run
```

**Expected Output**:
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_|\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::               (v3.2.0)

com.stockcast.StockCastApplication : Starting StockCastApplication
...
StockCast server started on port 9090
======================================================================
StockCast Server is ready to accept connections
Available stock tickers: AAPL, GOOG, MSFT, AMZN, TSLA
======================================================================
```

**Points to Discuss**:
- Server listening on port 9090 (TCP/IP port)
- Spring Boot initialization
- StockPriceGenerator running in background
- Ready to accept connections

---

### Phase 3: Connect First Client (3 minutes)

**In Terminal 2 (Client 1)**:
```powershell
cd c:\Users\Anuradha\Downloads\Moratuwa Academic\Projects\StockCast\backend
mvn compile
java -cp target/classes com.stockcast.client.StockCastClient
```

**Expected Output**:
```
Connected to StockCast server at localhost:9090
======================================================================
Client ID: a3f7d21e
Available tickers: AAPL,GOOG,MSFT,AMZN,TSLA
======================================================================

Available Commands:
  subscribe <tickers>    - Subscribe to stock updates (e.g., subscribe AAPL,GOOG)
  unsubscribe <tickers>  - Unsubscribe from stock updates
  list                   - Show your current subscriptions
  ping                   - Check server connection
  clear                  - Clear screen
  help                   - Show this help message
  quit                   - Exit the client
======================================================================

>
```

**Points to Discuss**:
- TCP connection established successfully
- Server sent WELCOME message with unique client ID
- Client ready for commands
- This is application-level protocol on top of TCP/IP

---

### Phase 4: Subscribe to Stocks (2 minutes)

**In Terminal 2 (Client 1)**, type:
```
subscribe AAPL,TSLA
```

**Expected Output**:
```
> subscribe AAPL,TSLA
✓ Subscribed to: AAPL,TSLA

[12:45:30] AAPL   $150.25    ↑ +0.17%
[12:45:30] TSLA   $249.87    ↓ -0.05%
[12:45:31] AAPL   $150.48    ↑ +0.15%
[12:45:31] TSLA   $250.12    ↑ +0.10%
[12:45:32] AAPL   $150.12    ↓ -0.24%
```

**Points to Discuss**:
- Subscription message sent to server
- Server validated and registered subscription
- Prices streaming in real-time (every ~1 second)
- Non-blocking communication - client can receive while idle
- This is a "push" model (server initiates)

---

### Phase 5: Connect Multiple Clients (4 minutes)

**In Terminal 3 (Client 2)**:
```powershell
cd c:\Users\Anuradha\Downloads\Moratuwa Academic\Projects\StockCast\backend
java -cp target/classes com.stockcast.client.StockCastClient
```

Wait for welcome message, then:
```
subscribe GOOG,MSFT
```

**In Terminal 4 (Client 3)** (optional for larger demo):
```powershell
cd c:\Users\Anuradha\Downloads\Moratuwa Academic\Projects\StockCast\backend
java -cp target/classes com.stockcast.client.StockCastClient
```

Wait for welcome message, then:
```
subscribe AAPL
```

**Points to Discuss**:
- Three clients connected to single server
- Each receives different subset of updates (selective broadcast)
- This is the NIO advantage: one thread, many clients
- In Thread-per-Client model, we'd need 3 threads now
- With NIO Selector, still using single broadcast thread

---

### Phase 6: Show Multi-Client Behavior (3 minutes)

**In all terminals**, type:
```
list
```

**Expected Output**:
```
> list
SUBSCRIPTIONS|AAPL,TSLA        (Terminal 2)
SUBSCRIPTIONS|GOOG,MSFT        (Terminal 3)
SUBSCRIPTIONS|AAPL             (Terminal 4)
```

**Points to Discuss**:
- Server tracks subscriptions per client
- SubscriptionManager maintains bidirectional mapping:
  - Client → Subscriptions (what they want)
  - Ticker → Clients (who wants this)
- Demonstrates ConcurrentHashMap usage
- Thread-safe without locks (fine-grained locking)

---

### Phase 7: Show Unsubscription (1 minute)

**In Terminal 2 (Client 1)**, type:
```
unsubscribe TSLA
```

**Expected Output**:
```
> unsubscribe TSLA
✓ Unsubscribed from: TSLA

[12:46:00] AAPL   $150.56    ↑ +0.26%
[12:46:01] AAPL   $150.43    ↓ -0.09%
```

**Points to Discuss**:
- Now only receiving AAPL updates
- TSLA prices no longer broadcast to this client
- SubscriptionManager updated bidirectional mappings
- Server still broadcasting TSLA to other clients

---

### Phase 8: Test Connectivity (1 minute)

**In any client**, type:
```
ping
```

**Expected Output**:
```
> ping
PONG
```

**Points to Discuss**:
- Ping/Pong demonstrates connection is alive
- Useful for detecting broken connections
- In real systems, heartbeat important for long-lived connections

---

### Phase 9: Disconnect Client Gracefully (1 minute)

**In Terminal 2 (Client 1)**, type:
```
quit
```

**Expected Output**:
```
> quit
Disconnected from server
```

**Server terminal (Terminal 1)** shows:
```
Client disconnected: a3f7d21e
```

**Points to Discuss**:
- Graceful shutdown (TCP FIN handshake)
- Server freed the client's resources
- Other clients unaffected
- ConnectionManager unregisters client from SubscriptionManager

---

### Phase 10: Kill Server Abruptly (1 minute, if showing error handling)

**In Terminal 1**, press `Ctrl+C`:

**Expected Output**:
```
^CStockCast server stopped
```

**In remaining clients** (Terminals 3 & 4):
```
[Error] Server connection lost
Reconnecting in 5 seconds...
```

**Points to Discuss**:
- Abrupt disconnect (no FIN handshake)
- Clients detect this through read() failure or timeout
- OS closes sockets after cleanup
- In production, would attempt automatic reconnection

---

## 📊 Demo Metrics to Highlight

Show these statistics during/after demo:

```
Metric                          Value           Significance
────────────────────────────────────────────────────────────
Connection time                 <100ms          TCP handshake + auth
Time to first price update       <50ms           Low latency
Messages/sec throughput          100+            At 1-sec generation
Server threads                   ~5              Not per-client
Server memory                    ~50MB           For 3 clients
CPU usage                        <1%             NIO efficiency
```

## 🎯 Key Teaching Points During Demo

### 1. **Protocol Layer** (Application vs Network)
- **Show**: Send raw message "SUBSCRIBE|AAPL"
- **Explain**: This text is wrapped in TCP segment
  - TCP handles ordering, reliability, retransmission
  - Application layer handles meaning
  - This is the OSI model: Layer 7 (app) on Layer 4 (transport)

### 2. **Non-Blocking I/O Advantage**
- **Show**: 3 clients, 1 server
- **Compare**: Thread-per-Client would need 3+ threads
- **Explain**: NIO Selector = 1 thread monitoring 3 channels
  - Selector.select() returns when any channel ready
  - Process that channel, go back to select()
  - See BroadcastModule.java for implementation

### 3. **Subscription Management** (distributed state)
- **Show**: Different clients → different subscriptions
- **Ask**: "How does server know who to send AAPL to?"
- **Explain**: SubscriptionManager maintains mappings
  - Ticker → List<ClientInfo> (who wants this ticker)
  - ClientInfo → Set<String> (what tickers they want)
  - Both must be kept in sync (concurrency challenge)

### 4. **Real-time Broadcasting**
- **Show**: Price updates appear simultaneously for all subscribers
- **Ask**: "How does server send to multiple clients?"
- **Explain**: BroadcastModule queue
  - StockPriceGenerator notifies listeners (push)
  - Listener queries SubscriptionManager for subscribers
  - Broadcasts to all via non-blocking NIO writes

### 5. **Thread Safety** (the hard part)
- **Show**: Open SubscriptionManager.java
- **Ask**: "Why use ConcurrentHashMap?"
- **Explain**:
  - Multiple threads modify subscriptions concurrently
    - Client handler threads (from client messages)
    - Broadcast thread (from disconnect handlers)
  - Without synchronization = race conditions
  - ConcurrentHashMap uses fine-grained locking

---

## 🐛 Troubleshooting Common Issues

### Port Already in Use
```powershell
# Find process using port 9090
netstat -ano | findstr 9090

# Kill the process
taskkill /PID <PID> /F

# Or change port in application.properties
```

### "Connection refused" Error
- Ensure server is running
- Check port is 9090 (not 9092 or 9091)
- Check firewall isn't blocking

### No Price Updates Appearing
- Ensure you've subscribed: type `subscribe AAPL`
- Check ticker names are uppercase
- Verify StockPriceGenerator thread started (check logs)

### Client Appears Frozen
- It's waiting for user input (blocking on stdin)
- Type something and press Enter
- Or press Ctrl+C to exit

---

## 🧪 Extended Experiments (if time permits)

### Experiment 1: Latency Measurement
```
Add timestamps to price updates:
- Observe time from generation to display
- Should be <100ms on local machine
```

### Experiment 2: Stress with Multiple Connections
```powershell
# Terminal: Start load generator
# See LOAD_TESTING.md for tool

# This would show:
# - Server remains responsive
# - CPU stays low
# - All clients receive updates
```

### Experiment 3: Network Sniffing
```powershell
# On Windows with Wireshark:
# Filter: tcp.port == 9090

# You'll see:
# - TCP SYN/ACK handshake
# - SUBSCRIBE command in plaintext
# - PRICE updates in stream
# - TCP FIN on disconnect
```

---

## 📝 Post-Demo Discussion Questions

1. **Scalability**: How would you support 10,000 concurrent clients?
   - Answer: Current architecture scales with NIO
   - Beyond that: distributed across multiple servers, queue-based (Kafka), etc.

2. **Security**: What security measures are missing?
   - Answer: Authentication, encryption (TLS), rate limiting
   - Production would add these

3. **Reliability**: What if server crashes mid-broadcast?
   - Answer: Clients need heartbeat detection + auto-reconnect
   - Or use message queue (Kafka) for persistence

4. **Performance**: How could we make it faster?
   - Answer: Binary protocol, compression, caching, batching updates
   - Current text protocol good for teaching, not production

5. **Real-world Examples**: Where is this pattern used?
   - Answer: WebSockets (browsers), Slack, WhatsApp, financial systems

---

## 📚 Demo Handout (Print & Distribute)

### Quick Reference: StockCast Commands

| Command | Example | Purpose |
|---------|---------|---------|
| `subscribe` | `subscribe AAPL,GOOG` | Subscribe to price updates |
| `unsubscribe` | `unsubscribe AAPL` | Stop receiving updates |
| `list` | `list` | Show current subscriptions |
| `ping` | `ping` | Test server connection |
| `clear` | `clear` | Clear terminal screen |
| `help` | `help` | Show available commands |
| `quit` | `quit` | Disconnect from server |

### Available Stock Tickers
- **AAPL** - Apple Inc.
- **GOOG** - Google (Alphabet)
- **MSFT** - Microsoft
- **AMZN** - Amazon
- **TSLA** - Tesla

### Network Concepts Demonstrated

- ✅ **TCP/IP Sockets**: Reliable client-server communication
- ✅ **Protocol Design**: Custom text-based protocol with message framing
- ✅ **Non-Blocking I/O**: NIO Selector for handling multiple clients
- ✅ **Multithreading**: Thread pool + daemon threads + synchronized access
- ✅ **Concurrency**: ConcurrentHashMap + listener patterns
- ✅ **Scalability**: Single thread handles many clients

### Further Learning
- Read [NETWORK_CONCEPTS.md](NETWORK_CONCEPTS.md) for detailed explanations
- Explore source code in `backend/src/main/java/com/stockcast/`
- Try experiments in [LOAD_TESTING.md](LOAD_TESTING.md)

---

## 🎓 Learning Outcomes

After this demonstration, students should understand:

1. ✅ How TCP sockets enable client-server communication
2. ✅ The difference between blocking and non-blocking I/O
3. ✅ How Java NIO's Selector pattern scales to many clients
4. ✅ The importance of thread safety in concurrent systems
5. ✅ How to design simple, effective network protocols
6. ✅ Real-world patterns used in modern applications (websockets, message queues)

---

## ⏱️ Timing Summary

| Phase | Duration | Content |
|-------|----------|---------|
| 1. Architecture | 3 min | Overview of 5 components |
| 2. Start Server | 2 min | Show startup logs |
| 3. First Client | 3 min | Show welcome + explain connection |
| 4. Subscribe | 2 min | Show price stream |
| 5. Multi-Client | 4 min | Connect 2-3 more clients |
| 6. List Subscriptions | 3 min | Show server state |
| 7. Unsubscribe | 1 min | Change subscriptions |
| 8. Ping | 1 min | Test connectivity |
| 9. Disconnect | 1 min | Graceful shutdown |
| 10. Error Handling | 1 min | Kill server, show recovery |
| **Q&A** | **varies** | Answer questions |
| **Total** | **~20 min** | Flexible based on questions |

---

## 💾 Demo Files Checklist

Before presenting, ensure these files exist:

- ✅ Backend source code compiled: `backend/target/classes/`
- ✅ Documentation: `NETWORK_CONCEPTS.md` (detailed explanations)
- ✅ This file: `DEMO_GUIDE.md` (step-by-step walkthrough)
- ✅ Configuration: `backend/src/main/resources/application.properties` (port 9090)
- ✅ PowerShell scripts: `run-server.ps1`, `run-client.ps1` (optional)

---

Good luck with your demo! 🚀📈
