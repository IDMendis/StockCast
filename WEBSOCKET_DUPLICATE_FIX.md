# 🔌 Multiple WebSocket Connections Explained

## 🔍 **Why You See 4 WebSocket Connections**

When you opened **one browser tab**, the server logged **4 WebSocket connections**:

```
2025-11-14 11:46:32 - WebSocket client connected: 87470278-... (1)
2025-11-14 11:46:32 - WebSocket client connected: 99d498f1-... (2)
2025-11-14 11:46:37 - WebSocket client connected: f5ad324b-... (3)
2025-11-14 11:46:37 - WebSocket client connected: ea0beeaf-... (4)
```

Plus 1 TCP client:

```
2025-11-14 11:46:46 - New client connected: bfa39b2a (TCP console client)
```

---

## ✅ **Root Causes**

### **1. React StrictMode (Primary Cause)**

**File**: `frontend/src/index.js` (line 8)

```javascript
<React.StrictMode>
  {" "}
  // ← THIS CAUSES DOUBLE MOUNTING
  <App />
</React.StrictMode>
```

**What happens:**

1. Component mounts → Creates WebSocket #1
2. StrictMode unmounts component (intentionally!)
3. Component remounts → Creates WebSocket #2
4. Cleanup sometimes delayed → Both connections exist

**Purpose**: StrictMode helps find bugs by double-invoking effects in development.  
**Side effect**: Creates duplicate connections.

---

### **2. Hot Module Replacement (HMR)**

When you save a file in development:

1. React hot-reloads the component
2. New WebSocket connection created
3. Old connection might not close immediately
4. Result: Additional connections (connections #3 and #4)

**Timestamps confirm this:**

- 11:46:32 - Initial mount (2 connections from StrictMode)
- 11:46:37 - HMR refresh (2 more connections)

---

### **3. Weak Connection Checking**

**Original code** in `WebSocketService.js`:

```javascript
if (this.ws && this.ws.readyState === WebSocket.OPEN) {
  return; // Only checks OPEN state
}
```

**Problem**: Doesn't check `CONNECTING` state, so rapid calls create multiple connections.

---

## ✅ **Fixes Applied**

### **Fix #1: Disable StrictMode in Development**

**File**: `frontend/src/index.js`

```javascript
// BEFORE:
root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);

// AFTER:
root.render(
  // StrictMode disabled in development to prevent duplicate WebSocket connections
  // Re-enable for production builds
  // <React.StrictMode>
  <App />
  // </React.StrictMode>
);
```

**Result**: Eliminates double-mounting → Reduces connections from 4 to 2

---

### **Fix #2: Improve Connection Guard**

**File**: `frontend/src/services/WebSocketService.js`

```javascript
// BEFORE:
connect(url) {
  if (this.ws && this.ws.readyState === WebSocket.OPEN) {
    return;
  }
  this.ws = new WebSocket(url);
}

// AFTER:
connect(url) {
  // Check both OPEN and CONNECTING states
  if (this.ws && (
    this.ws.readyState === WebSocket.OPEN ||
    this.ws.readyState === WebSocket.CONNECTING
  )) {
    console.log('WebSocket already connected or connecting');
    return;
  }

  // Close existing connection before creating new one
  if (this.ws) {
    console.log('Closing existing WebSocket before reconnecting');
    this.ws.close();
    this.ws = null;
  }

  this.ws = new WebSocket(url);
}
```

**Result**: Prevents duplicate connections during HMR → Reduces from 2 to 1

---

## 📊 **Before vs After**

### **Before Fixes**

```
Browser Tab Opens
  ↓
React StrictMode mounts → WebSocket #1
  ↓
React StrictMode remounts → WebSocket #2
  ↓
HMR triggers (file save) → WebSocket #3
  ↓
Cleanup delayed → WebSocket #4
  ↓
Total: 4 WebSocket connections
```

### **After Fixes**

```
Browser Tab Opens
  ↓
React mounts (no StrictMode) → WebSocket #1
  ↓
Connection guard prevents duplicates
  ↓
HMR triggers → Closes old, creates new (still 1 total)
  ↓
Total: 1 WebSocket connection ✅
```

---

## 🧪 **How to Verify**

### **Test the Fix:**

1. **Save the files** (both `index.js` and `WebSocketService.js`)
2. **Restart the frontend:**
   ```powershell
   cd frontend
   npm start
   ```
3. **Open ONE browser tab** to `http://localhost:3000`
4. **Check server logs** - should show only **1 WebSocket connection** now:

   ```
   2025-11-14 XX:XX:XX - WebSocket client connected: <uuid>
   2025-11-14 XX:XX:XX - WebSocket session registered: <uuid>
   ```

5. **Check metrics:**
   ```
   Active Clients: 1 (TCP: 0, WebSocket: 1)
   ```

---

## 📝 **Additional Notes**

### **TCP Client is Separate**

```
2025-11-14 11:46:46 - New client connected: bfa39b2a
```

This is your **console TCP client** (from `run-client.ps1`), not from the browser.

### **WebSocket States**

```javascript
WebSocket.CONNECTING = 0; // Connection is being established
WebSocket.OPEN = 1; // Connection is open and ready
WebSocket.CLOSING = 2; // Connection is closing
WebSocket.CLOSED = 3; // Connection is closed
```

### **StrictMode in Production**

For production builds, StrictMode has no effect (it only runs in development). You can re-enable it later if needed:

```javascript
root.render(
  process.env.NODE_ENV === "production" ? (
    <App />
  ) : (
    <React.StrictMode>
      <App />
    </React.StrictMode>
  )
);
```

---

## 🎯 **Summary**

| Issue                 | Cause                 | Fix                                     | Result              |
| --------------------- | --------------------- | --------------------------------------- | ------------------- |
| 2 connections on load | React StrictMode      | Disabled in `index.js`                  | -2 connections      |
| 2 more on HMR         | Weak connection guard | Enhanced check in `WebSocketService.js` | -1 connection       |
| **Total**             |                       |                                         | **1 connection** ✅ |

---

**Files Modified:**

1. ✅ `frontend/src/index.js` - Disabled StrictMode
2. ✅ `frontend/src/services/WebSocketService.js` - Improved connection guard

**Expected Result:** Only 1 WebSocket connection per browser tab! 🎉
