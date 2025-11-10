# StockCast - Quick Start Guide (Updated)

## 📋 Prerequisites Check

Ensure you have Java 17+ and Maven installed:

```powershell
java -version    # Should show Java 17 or higher
mvn -version     # Should show Maven 3.6 or higher
```

## 🚀 Quick Start (5 minutes)

### Step 1: Navigate to Project

```powershell
cd c:\Users\Anuradha\Downloads\Moratuwa Academic\Projects\StockCast\backend
```

### Step 2: Build the Project

```powershell
mvn clean compile
```

This will download dependencies and compile the code.

### Step 3: Start the Server

```powershell
mvn spring-boot:run
```

**Expected Output:**
```
======================================================================
StockCast Server started on port 9090
======================================================================
Server Configuration:
  - Host: 0.0.0.0
  - Port: 9090 (TCP/IP)
  - Available tickers: AAPL, GOOG, MSFT, AMZN, TSLA
======================================================================
StockCast Server is ready to accept connections
======================================================================
```

### Step 4: Start a Client (in a NEW terminal)

```powershell
cd c:\Users\Anuradha\Downloads\Moratuwa Academic\Projects\StockCast\backend
java -cp target/classes com.stockcast.client.StockCastClient
```

### Step 5: Test the System

Subscribe to stocks:
```
subscribe AAPL,GOOG
```

You'll see real-time price updates! Type `quit` to exit.

---

## 📊 Available Tickers

- **AAPL** - Apple
- **GOOG** - Google
- **MSFT** - Microsoft
- **AMZN** - Amazon
- **TSLA** - Tesla

---

## 📚 Learn More

- **[NETWORK_CONCEPTS.md](NETWORK_CONCEPTS.md)** - Network theory and concepts
- **[DEMO_GUIDE.md](DEMO_GUIDE.md)** - Full demonstration walkthrough
- **[LOAD_TESTING.md](LOAD_TESTING.md)** - Stress testing
- **[README.md](README.md)** - Complete documentation

---

## 🧪 Test Multiple Clients

1. **Terminal 1**: `mvn spring-boot:run` (server)
2. **Terminal 2**: `java -cp target/classes com.stockcast.client.StockCastClient` (client 1)
3. **Terminal 3**: `java -cp target/classes com.stockcast.client.StockCastClient` (client 2)

Each client can subscribe to different tickers and will receive updates independently!

---

Enjoy! 🚀📈
