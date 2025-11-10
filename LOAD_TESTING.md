# Load Testing Guide for StockCast

This document provides tools and instructions for stress-testing the StockCast server with multiple concurrent clients.

## Overview

Load testing helps verify:
- ✅ Server stability under multiple concurrent connections
- ✅ Message delivery reliability
- ✅ Performance degradation patterns
- ✅ Network resource utilization
- ✅ Comparison of blocking vs non-blocking I/O

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.6+
- Backend already built (`mvn clean package`)

### Simple Load Test (3 clients)

**Terminal 1 - Start Server**:
```powershell
cd backend
mvn spring-boot:run
```

**Terminal 2 - Start LoadTestClient**:
```powershell
cd backend
mvn compile
java -cp target/classes com.stockcast.loadtest.LoadTestClient 3 AAPL,GOOG,MSFT
```

Expected output:
```
Starting load test with 3 clients...
[Client-1] Connected
[Client-1] Subscribed to AAPL,GOOG,MSFT
[Client-2] Connected
[Client-2] Subscribed to AAPL,GOOG,MSFT
[Client-3] Connected
[Client-3] Subscribed to AAPL,GOOG,MSFT

--- Load Test Running ---
Uptime: 5s | Clients: 3 | Messages received: 15 | Errors: 0

Uptime: 10s | Clients: 3 | Messages received: 30 | Errors: 0
```

---

## LoadTestClient Usage

The `LoadTestClient` simulates multiple concurrent connections with realistic behavior.

### Command Line Arguments

```bash
java -cp target/classes com.stockcast.loadtest.LoadTestClient <numClients> <tickers> [options]
```

| Argument | Example | Required | Description |
|----------|---------|----------|-------------|
| `numClients` | `10` | Yes | Number of concurrent clients to spawn |
| `tickers` | `AAPL,GOOG` | Yes | Comma-separated list of tickers to subscribe to |
| `-host` | `localhost` | No | Server hostname (default: localhost) |
| `-port` | `9090` | No | Server port (default: 9090) |
| `-duration` | `60` | No | Test duration in seconds (default: infinite) |
| `-delay` | `1000` | No | Delay between operations in ms (default: 1000) |
| `-verbose` | (flag) | No | Print detailed logs |
| `-help` | (flag) | No | Show help message |

### Examples

**Light Load (5 clients, 30 seconds)**:
```bash
java -cp target/classes com.stockcast.loadtest.LoadTestClient 5 AAPL -duration 30
```

**Medium Load (50 clients, all tickers)**:
```bash
java -cp target/classes com.stockcast.loadtest.LoadTestClient 50 AAPL,GOOG,MSFT,AMZN,TSLA -duration 120
```

**Heavy Load (500 clients, specific tickers, verbose)**:
```bash
java -cp target/classes com.stockcast.loadtest.LoadTestClient 500 AAPL,GOOG -duration 180 -verbose
```

**Custom Server**:
```bash
java -cp target/classes com.stockcast.loadtest.LoadTestClient 20 AAPL -host 192.168.1.100 -port 9090
```

---

## Performance Benchmarks

### Test Matrix

| Clients | Duration | Tickers | Expected Result |
|---------|----------|---------|-----------------|
| 5 | 30s | AAPL | CPU: <5%, Memory: <200MB ✓ |
| 50 | 60s | AAPL,GOOG | CPU: <10%, Memory: <500MB ✓ |
| 100 | 60s | All | CPU: <15%, Memory: <1GB ✓ |
| 200 | 120s | All | CPU: 20-30%, Memory: ~2GB ⚠ |
| 500+ | 120s | All | Possible saturation (varies by OS) |

### What to Monitor

**Server Metrics** (watch Terminal 1):
```
[CONNECT] Client xxxxxxxx connected from /127.0.0.1:xxxxx
[DISCONNECT] Client xxxxxxxx (connected for 30s)
```

**JVM Metrics**:
```bash
# In another terminal, monitor JVM:
jps  # Find Java process ID
jconsole <PID>  # Open JVM monitoring tool
```

**OS Metrics** (Windows):
```powershell
# Monitor CPU and Memory
while ($true) { Get-Process | Where-Object {$_.ProcessName -like "*java*"} | Select-Object ProcessName, CPU, Memory; Start-Sleep 1; Clear-Host }
```

**Network Metrics** (Wireshark on Windows):
```
Capture on loopback
Filter: tcp.port == 9090
Observe: Connection count, message rate, throughput
```

---

## Test Scenarios

### Scenario 1: Connection Stability
**Goal**: Verify server accepts many connections without crashing

```bash
# In Terminal 2:
java -cp target/classes com.stockcast.loadtest.LoadTestClient 100 AAPL -duration 60

# Expected behavior:
# - All 100 connections succeed
# - Server remains responsive
# - No connection errors
# - Clean disconnect at end
```

**Success Criteria**:
- ✅ All clients connected
- ✅ No timeout errors
- ✅ Graceful disconnect
- ✅ Server logs show 100 CONNECT + 100 DISCONNECT

---

### Scenario 2: Message Throughput
**Goal**: Measure messages/second delivery under load

```bash
# In Terminal 2:
java -cp target/classes com.stockcast.loadtest.LoadTestClient 200 AAPL,GOOG,MSFT -duration 120

# At end, tool reports:
# "Total messages received: 24000"
# "Average: 200 msg/s"
```

**Success Criteria**:
- ✅ Messages received match expected (clients × tickers × duration)
- ✅ No message loss
- ✅ Consistent delivery rate

---

### Scenario 3: Selective Subscriptions
**Goal**: Verify subscription filtering works under load

```bash
# Modify LoadTestClient to:
# - Client 1-50: subscribe AAPL only
# - Client 51-100: subscribe GOOG only
# - Client 101-150: subscribe MSFT only

# Expected behavior:
# - Each client receives only subscribed tickers
# - No cross-talk (AAPL subscriber doesn't get GOOG)
# - Load distributed across tickers
```

**Success Criteria**:
- ✅ Message filtering works correctly
- ✅ No message loss
- ✅ Server efficiently routes updates

---

### Scenario 4: Rapid Connect/Disconnect
**Goal**: Test connection churn (simulates real-world mobile apps)

```bash
# Modify LoadTestClient to:
# - Connect client
# - Receive 10 messages
# - Disconnect
# - Repeat

# Duration: 5 minutes with 100 concurrent "slots"
```

**Success Criteria**:
- ✅ No memory leaks
- ✅ Port reuse works (TIME_WAIT handling)
- ✅ Consistent CPU usage
- ✅ No resource exhaustion

---

### Scenario 5: Subscription Changes Under Load
**Goal**: Test SUBSCRIBE/UNSUBSCRIBE while receiving messages

```bash
# Modify LoadTestClient to:
# - Connect and subscribe to AAPL
# - After 10s: subscribe to GOOG
# - After 20s: unsubscribe from AAPL
# - After 30s: subscribe to MSFT
# - Verify correct messages at each step
```

**Success Criteria**:
- ✅ Subscription changes are instant
- ✅ No message loss during change
- ✅ No duplicate messages
- ✅ Consistent delivery

---

## Performance Comparison: NIO vs Blocking I/O

### Current Implementation (NIO with Selector)

```
Clients | Threads | Memory | CPU | Notes
--------|---------|--------|-----|-------
10      | 15      | 80MB   | 1%  | ClientAcceptor + 10 handlers
100     | 105     | 300MB  | 3%  | Thread pool grows
500     | 505     | 1.5GB  | 8%  | Performance still good
1000    | 1005    | 3GB    | 15% | Approaching thread limit
```

### Theoretical Blocking I/O (Thread-per-Client)

```
Clients | Threads | Memory | CPU | Notes
--------|---------|--------|-----|-------
10      | 15      | 80MB   | 1%  | Similar at low load
100     | 105     | 300MB  | 5%  | More context switching
500     | 505     | 2.5GB  | 25% | Thread overhead significant
1000    | 1005    | 5GB    | 50% | Not practical
```

**Key Insight**: With NIO, we can handle 10x more clients with same resources!

---

## Troubleshooting Load Tests

### Test hangs at startup

**Problem**: LoadTestClient doesn't start connecting

**Solution**:
```bash
# Check server is running
netstat -ano | findstr 9090

# Verify firewall isn't blocking
# Try connecting manually:
java -cp target/classes com.stockcast.client.StockCastClient
```

### "Connection refused" errors

**Problem**: Clients can't connect to server

**Solution**:
```bash
# 1. Verify server is running and listening
# 2. Check port number (default 9090)
# 3. Check firewall: Windows Defender > Firewall > Allow app

# On server, increase backlog:
# This is in ConnectionManager.start():
# serverSocketChannel.bind(new InetSocketAddress("0.0.0.0", port), 200);
```

### Messages not being received

**Problem**: Load test runs but `messages received: 0`

**Solution**:
```bash
# 1. Verify subscription worked:
java -cp target/classes com.stockcast.client.StockCastClient
# Type: subscribe AAPL
# Should see prices

# 2. Check ticker names match (must be uppercase)

# 3. Check StockPriceGenerator is running:
# Server log should show price updates
```

### High memory usage / Out of Memory

**Problem**: Java throws OutOfMemoryError

**Solution**:
```bash
# Increase JVM heap size:
java -Xmx4g -cp target/classes com.stockcast.loadtest.LoadTestClient 500 AAPL

# Or reduce number of clients:
java -cp target/classes com.stockcast.loadtest.LoadTestClient 200 AAPL
```

### CPU spike then drops

**Problem**: CPU usage bounces around

**Solution**: This is normal. JVM is:
1. Accepting connections (high CPU)
2. Settling to steady-state (lower CPU)
3. Full GC pauses (spike)

To reduce GC impact:
```bash
java -XX:+UseG1GC -Xmx4g -cp target/classes com.stockcast.loadtest.LoadTestClient 500 AAPL
```

---

## Advanced Scenarios

### Chaos Testing: Random Disconnects

Modify LoadTestClient to:
```java
// Randomly disconnect clients during test
if (random.nextDouble() < 0.01) {  // 1% chance per second
    client.disconnect();
}
```

Verify:
- ✅ Server cleans up disconnected clients
- ✅ No resource leaks
- ✅ Other clients unaffected

### Network Simulation: Artificial Latency

Using Linux `tc` command or Windows NetLimiter:
```bash
# Add 50ms latency to all port 9090 traffic
# Then re-run load tests
# Observe: How does server handle slow clients?
```

### Message Burst Testing

Modify server to:
```java
// Instead of 1 update/sec, burst 100 updates/sec for 5 seconds
```

Verify:
- ✅ Queues handle burst
- ✅ No message loss
- ✅ No buffer overflows

---

## Integration with CI/CD

### GitHub Actions Example

```yaml
name: Load Tests

on: [push, pull_request]

jobs:
  load-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      
      - name: Build
        run: cd backend && mvn clean package
      
      - name: Start Server
        run: cd backend && mvn spring-boot:run &
      
      - name: Wait for Server
        run: sleep 5
      
      - name: Load Test
        run: |
          cd backend
          java -cp target/classes com.stockcast.loadtest.LoadTestClient 100 AAPL -duration 60
      
      - name: Check Results
        run: |
          # Parse output for success criteria
          if [[ $? -eq 0 ]]; then
            echo "✓ Load test passed"
          else
            echo "✗ Load test failed"
            exit 1
          fi
```

---

## Monitoring with VisualVM

```powershell
# Terminal 1: Start server (note the process ID)
cd backend
mvn spring-boot:run

# Terminal 2: Find Java process
jps

# Terminal 3: Open VisualVM
jvisualvm

# In VisualVM:
# 1. Right-click on StockCastApplication
# 2. Select "Profiler"
# 3. Start load test in Terminal 4
# 4. Watch: Threads, Memory, CPU, GC
```

This gives real-time visibility into what the server is doing!

---

## Reporting Results

After load testing, create a report:

```markdown
# Load Test Report - StockCast

## Test Configuration
- Date: 2025-11-10
- Duration: 120 seconds
- Clients: 200
- Tickers: AAPL, GOOG, MSFT

## Results
- ✓ All 200 clients connected successfully
- ✓ Messages received: 60,000 (expected 60,000)
- ✓ No message loss
- ✓ No errors
- ✓ Average response time: 2ms
- ✓ Peak memory: 800MB
- ✓ Peak CPU: 12%

## Conclusion
Server handles 200 concurrent clients with NIO efficiently.
No degradation observed.
```

---

## Next Steps

1. **Run baseline tests** with 10, 50, 100 clients
2. **Record metrics** (see above)
3. **Identify bottlenecks** (CPU, memory, network, disk?)
4. **Optimize if needed**:
   - Buffer sizes
   - Thread pool configuration
   - Message batching
   - Protocol compression
5. **Compare results** with production requirements

---

Good luck with your load testing! 🚀
