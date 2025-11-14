# 🐛 SERVER BUG: Non-Blocking Read Race Condition

## ✅ ROOT CAUSE IDENTIFIED

### **The Server Bug**

The server configures client channels as **non-blocking**, but the `handleClient()` method doesn't properly handle non-blocking reads:

```java
// In acceptConnections():
clientChannel.configureBlocking(false);  // ← Non-blocking mode

// In handleClient():
while (running && channel.isOpen() && channel.isConnected()) {
    buffer.clear();
    int bytesRead = channel.read(buffer);  // ← Returns 0 if no data ready!

    if (bytesRead > 0) {
        // Process data...
    }

    Thread.sleep(10);  // ← Only 10ms delay before next read attempt
}
```

**The Problem:**

1. Client sends data
2. Network latency delays arrival (> 10ms)
3. Server's `read()` returns 0 (no data yet)
4. Server loops and sleeps for 10ms
5. By the time server reads again, client may have sent more data or given up

---

## 🔍 Why TCP Shows as 0

The metrics show `TCP: 0` because `ConnectionManager` never calls:

```java
metricsService.clientConnected(false);  // ← MISSING!
```

This should be added after line 131 in `ConnectionManager.java`.

---

## ✅ SERVER-SIDE FIXES NEEDED

### Fix #1: Add Metrics Tracking (Simple)

**File**: `ConnectionManager.java`  
**Location**: After line 131

```java
// Register client
subscriptionManager.registerClient(clientId, clientChannel);

// ADD THIS LINE:
metricsService.clientConnected(false);  // false = TCP client

// Register with broadcast module
broadcastModule.registerChannel(clientChannel);
```

### Fix #2: Improve Non-Blocking Read Logic (Better)

**File**: `ConnectionManager.java`  
**Method**: `handleClient()`

**Option A: Use Selector (Proper non-blocking)**

```java
private void handleClient(String clientId, SocketChannel channel) {
    Selector selector = Selector.open();
    channel.register(selector, SelectionKey.OP_READ);
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    StringBuilder messageBuilder = new StringBuilder();

    while (running && channel.isOpen()) {
        // Wait for data to be available (with timeout)
        if (selector.select(100) > 0) {  // 100ms timeout
            buffer.clear();
            int bytesRead = channel.read(buffer);

            if (bytesRead > 0) {
                // Process data...
            }

            selector.selectedKeys().clear();
        }
    }
}
```

**Option B: Switch to Blocking Mode (Simpler)**

```java
private void handleClient(String clientId, SocketChannel channel) {
    // Switch to blocking mode for this handler thread
    channel.configureBlocking(true);  // ← ADD THIS

    ByteBuffer buffer = ByteBuffer.allocate(1024);
    StringBuilder messageBuilder = new StringBuilder();

    while (running && channel.isOpen()) {
        buffer.clear();
        int bytesRead = channel.read(buffer);  // Now blocks until data arrives!

        if (bytesRead == -1) {
            break;  // Client disconnected
        }

        if (bytesRead > 0) {
            // Process data...
        }

        // NO Thread.sleep() needed - blocking read waits for data
    }
}
```

### Fix #3: Add Debug Logging (Diagnostic)

**File**: `ConnectionManager.java`  
**Method**: `handleClient()`

```java
private void handleClient(String clientId, SocketChannel channel) {
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    StringBuilder messageBuilder = new StringBuilder();

    log.info("TCP handler started for client: {}", clientId);  // ← ADD

    try {
        while (running && channel.isOpen() && channel.isConnected()) {
            buffer.clear();
            int bytesRead = channel.read(buffer);

            log.debug("Client {}: read {} bytes", clientId, bytesRead);  // ← ADD

            if (bytesRead == -1) {
                log.info("Client {} disconnected", clientId);  // ← ADD
                break;
            }

            if (bytesRead > 0) {
                buffer.flip();
                String data = StandardCharsets.UTF_8.decode(buffer).toString();
                log.debug("Client {}: received data [{}]", clientId, data);  // ← ADD
                messageBuilder.append(data);

                // ... rest of processing ...
            }

            Thread.sleep(10);
        }
    } catch (IOException e) {
        log.error("Error handling client {}: {}", clientId, e.getMessage());
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    } finally {
        log.info("TCP handler stopped for client: {}", clientId);  // ← ADD
        disconnectClient(clientId, channel);
    }
}
```

---

## 🎯 RECOMMENDED FIX (Easiest)

**Apply Fix #1 AND Fix #2 Option B:**

1. **Add metrics tracking** so TCP clients show correctly
2. **Switch to blocking mode** in the handler thread

This makes the server behavior match the client (both using blocking mode).

---

## 📊 Before vs After

### Before (Broken)

```
Client connects → Server creates non-blocking channel
Client sends "PING\n" → Takes 15ms to arrive due to network
Server reads (t=5ms) → Returns 0 bytes (data not arrived yet)
Server sleeps 10ms → Wakes up at t=15ms
Server reads (t=15ms) → Returns 5 bytes "PING\n"
Server processes → PONG sent back
```

**Problem**: If client sends quickly, first read might miss it!

### After (Fixed)

```
Client connects → Server creates non-blocking channel
Handler thread → Switches channel to BLOCKING mode
Client sends "PING\n" → Handler thread is blocked on read()
Data arrives → read() returns immediately with 5 bytes
Server processes → PONG sent back instantly
```

**Benefit**: Guaranteed to receive data, no timing issues!

---

## 🚀 Testing the Fix

### Test #1: Verify Metrics

After Fix #1, server should show:

```
Active Clients: X (TCP: 1, WebSocket: Y)
```

### Test #2: Verify Command Processing

After Fix #2, client should see:

```
stockcast> ping
DEBUG: ✅ Successfully sent 5 bytes to server
Server is alive (PONG)
```

Server should log:

```
2025-11-14 XX:XX:XX - Client xxxxxxxx: received data [PING
]
2025-11-14 XX:XX:XX - Processing command: PING from client: xxxxxxxx
```

---

## 📝 Files to Modify

1. **`ConnectionManager.java`**
   - Line 132: Add `metricsService.clientConnected(false);`
   - Line 159: Add `channel.configureBlocking(true);`
   - Lines 162-195: Add debug logging

---

## 🎓 Lesson Learned

**Never mix blocking and non-blocking I/O without understanding the implications!**

- Server accepts with blocking ServerSocketChannel ✅
- Server configures client as non-blocking ❌
- Handler reads in tight loop with small sleep ❌
- **Result**: Race condition with data arrival

**Proper patterns:**

1. **All blocking**: Simple, works great for moderate load
2. **All non-blocking with Selector**: Complex, scales to thousands of connections
3. **Blocking accept + blocking handlers**: Perfect for most applications
4. **Non-blocking accept + Selector for handlers**: Advanced high-performance servers

---

## 📖 References

- [Java NIO Non-Blocking Server](https://jenkov.com/tutorials/java-nio/non-blocking-server.html)
- [When to use blocking vs non-blocking](https://stackoverflow.com/questions/8777066/when-to-use-blocking-or-non-blocking-sockets)

---

**Status**: Waiting for server-side fix  
**Workaround**: Client code is correct, server needs fixing  
**Impact**: TCP clients can connect but commands are not reliably received
