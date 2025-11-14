# 🐛 TCP Client Input Debugging Guide

## ✅ Fixes Applied

### 1. **Enhanced Input Reading**

- Changed from non-blocking `ready()` check to **BLOCKING** `readLine()`
- Added clear prompt: `stockcast>`
- Shows "READY for commands" message

### 2. **Debug Output Added**

You will now see these debug messages:

```
DEBUG: Raw input received: [ping] length=4
DEBUG: Captured command: [ping]
DEBUG: Sending to server: [PING]
DEBUG: Sent 5 bytes to server
```

### 3. **Fixed sendMessage() Method**

- Added connection check before sending
- Tracks bytes actually written
- Handles non-blocking channel properly (retry logic)
- Forces flush with empty buffer write

---

## 🧪 Testing the Fixes

### Step 1: Start the Server

```powershell
.\run-server.ps1
```

Wait for: `StockCast Server is ready to accept connections`

### Step 2: Start the Client (Fresh Terminal!)

**Open a NEW PowerShell window**, then:

```powershell
.\run-client.ps1
```

### Step 3: Look for Debug Output

You should see:

```
Connected to StockCast server at localhost:9092
======================================================================
Available Commands:
  ...
======================================================================

READY for commands. Type 'help' for options.
======================================================================

stockcast>
```

### Step 4: Type a Command

Type: `ping` and press Enter

**You should see:**

```
stockcast> ping
DEBUG: Raw input received: [ping] length=4
DEBUG: Captured command: [ping]
DEBUG: Sending to server: [PING]
DEBUG: Sent 5 bytes to server
Server is alive (PONG)
```

---

## 🔍 Diagnosing Issues

### Issue #1: No DEBUG output when typing

**Symptoms:**

- You type `ping` but see no "DEBUG: Raw input received" message
- Input is completely ignored

**Cause:** Terminal input stream is broken

**Fix:**

1. Close terminal completely
2. Open fresh PowerShell
3. Run client again

### Issue #2: DEBUG shows input but "Sent 0 bytes"

**Symptoms:**

```
DEBUG: Captured command: [ping]
DEBUG: Sending to server: [PING]
DEBUG: Sent 0 bytes to server    ← PROBLEM!
```

**Cause:** Socket channel not ready for writing (non-blocking mode issue)

**Check Server Logs:** Look for "Client disconnected" - connection may have dropped

### Issue #3: Bytes sent but server doesn't respond

**Symptoms:**

```
DEBUG: Sent 5 bytes to server
(nothing happens)
```

**Cause:** Server not receiving or not processing the message

**Check:**

1. Server logs - does it show the PING command?
2. Client connection - is it still active?
3. Network issues - firewall blocking localhost?

### Issue #4: Control characters still appearing

**Symptoms:**

```
DEBUG: Raw input received: [^Bping] length=5    ← Control chars present
```

**Cause:** `stripControlCharacters()` not working properly

**Check:** The function should remove all characters in range 0x00-0x1F

---

## 🔧 Manual Testing Commands

### Test Input Capture

```java
// This will show what you're typing is being received
stockcast> test
DEBUG: Raw input received: [test] length=4
DEBUG: Captured command: [test]
Unknown command. Type 'help' for available commands.
```

### Test Network Send

```java
stockcast> ping
DEBUG: Sending to server: [PING]
DEBUG: Sent 5 bytes to server    ← Must be > 0!
```

### Test Server Response

```java
// After ping, you should see:
Server is alive (PONG)
```

---

## 📊 Expected vs Actual Behavior

| Scenario        | Expected                            | If Different          |
| --------------- | ----------------------------------- | --------------------- |
| Type command    | `DEBUG: Raw input received` appears | Terminal input broken |
| Process command | `DEBUG: Captured command` appears   | Input parsing broken  |
| Send to server  | `DEBUG: Sent X bytes` (X > 0)       | Socket write issue    |
| Server response | `Server is alive (PONG)` for ping   | Server not receiving  |

---

## 🚨 Common Errors and Solutions

### Error: "Socket is not connected!"

```
DEBUG: Sending to server: [PING]
ERROR: Socket is not connected!
```

**Solution:** Connection dropped. Restart client.

### Error: ByteBuffer hangs

```
DEBUG: Sending to server: [PING]
(client freezes)
```

**Solution:** Non-blocking channel issue. Already fixed with retry loop.

### Error: Empty input

```
stockcast> ping
DEBUG: Raw input received: [] length=0
DEBUG: Empty input ignored
```

**Solution:** Input being stripped entirely. Check `stripControlCharacters()`.

---

## 🎯 Success Criteria

✅ **Input is captured:**

```
DEBUG: Raw input received: [ping] length=4
```

✅ **Command is processed:**

```
DEBUG: Captured command: [ping]
```

✅ **Data is sent:**

```
DEBUG: Sent 5 bytes to server
```

✅ **Server responds:**

```
Server is alive (PONG)
```

---

## 🧹 Removing Debug Output (Later)

Once everything works, remove these lines from `StockCastClient.java`:

```java
// Remove these DEBUG System.out.println() lines:
System.out.println("DEBUG: Raw input received: [" + input + "] length=" + input.length());
System.out.println("DEBUG: Captured command: [" + input + "]");
System.out.println("DEBUG: Sending to server: [" + message + "]");
System.out.println("DEBUG: Sent " + totalWritten + " bytes to server");
System.out.println("DEBUG: Empty input ignored");
```

---

## 📝 Next Steps

1. **Run the client** with the new debug build
2. **Watch for DEBUG messages** as you type
3. **Report back** which debug line is the LAST one you see
4. This will pinpoint exactly where the failure occurs

The debug output will tell us:

- ✅ Is input being captured?
- ✅ Is the command being parsed?
- ✅ Is data being written to the socket?
- ✅ How many bytes were actually sent?

---

## 🆘 Still Stuck?

If the client still doesn't work, provide:

1. **Client output** (all DEBUG lines)
2. **Server logs** (does it show the ping?)
3. **Your PowerShell version:** `$PSVersionTable.PSVersion`
4. **Java version:** `java -version`

This will help identify if it's:

- A terminal issue (no DEBUG output at all)
- An input issue (DEBUG shows but data is empty)
- A network issue (DEBUG shows data sent but server doesn't receive)
- A server issue (server receives but doesn't respond)
