# StockCast - Quick Start Guide

## Prerequisites Check

Ensure you have Java 17+ and Maven installed:

```powershell
java -version    # Should show Java 17 or higher
mvn -version     # Should show Maven 3.6 or higher
```

## Step 1: Build the Project

```powershell
cd D:\StockCast
mvn clean package
```

This will:
- Download all dependencies
- Compile the code
- Run any tests
- Package the application

## Step 2: Start the Server

**Option A: Using PowerShell script**
```powershell
.\run-server.ps1
```

**Option B: Using Maven directly**
```powershell
mvn spring-boot:run
```

**Option C: Using the JAR**
```powershell
java -jar target/stockcast-1.0.0.jar
```

You should see:
```
StockCast Server started on port 9090
======================================================================
StockCast Server is ready to accept connections
Available stock tickers: AAPL, GOOG, MSFT, AMZN, TSLA
======================================================================
```

## Step 3: Start the Client

Open a **new PowerShell window** (keep server running in the first one):

**Option A: Using PowerShell script**
```powershell
cd D:\StockCast
.\run-client.ps1
```

**Option B: Using Maven**
```powershell
mvn compile
java -cp target/classes com.stockcast.client.StockCastClient
```

## Step 4: Test the System

In the client window, try these commands:

1. **Subscribe to stocks**
   ```
   subscribe AAPL,GOOG
   ```

2. **Watch real-time updates**
   You'll see price updates streaming in:
   ```
   [12:34:56] AAPL   $150.25    ↑ +0.17%
   [12:34:56] GOOG   $2800.45   ↓ -0.12%
   ```

3. **List your subscriptions**
   ```
   list
   ```

4. **Unsubscribe**
   ```
   unsubscribe AAPL
   ```

5. **Test connection**
   ```
   ping
   ```

## Step 5: Test Multiple Clients

Open **another new PowerShell window** and repeat Step 3.

Now you have:
- 1 server
- 2+ clients connected simultaneously

Each client can:
- Subscribe to different stocks
- Receive updates independently
- Connect/disconnect without affecting others

## Common Issues

### "Port 9090 already in use"
Change the port in `src/main/resources/application.properties`:
```properties
server.port=9091
```

### "Connection refused"
- Make sure the server is running
- Check Windows Firewall settings
- Verify you're using the correct port

### "java: command not found"
Add Java to your PATH or use full path:
```powershell
"C:\Program Files\Java\jdk-17\bin\java.exe" -version
```

### "mvn: command not found"
Install Maven or use full path:
```powershell
"C:\Program Files\Apache\maven\bin\mvn.cmd" -version
```

## Stopping the Application

**Stop the Server:**
Press `Ctrl+C` in the server window

**Stop a Client:**
Type `quit` or press `Ctrl+C`

## Next Steps

- Read the full [README.md](README.md) for detailed documentation
- Explore the code in `src/main/java/com/stockcast/`
- Modify stock tickers in `application.properties`
- Adjust price update interval for faster/slower updates

## Architecture Overview

```
Member 1 (ConnectionManager)    → Handles client connections
Member 2 (StockPriceGenerator)  → Generates mock stock prices
Member 3 (SubscriptionManager)  → Manages client subscriptions
Member 4 (BroadcastModule)      → Broadcasts using Java NIO
Member 5 (StockCastClient)      → Console client interface
```

## Support

If you encounter issues:
1. Check logs in the server console
2. Verify Java and Maven versions
3. Ensure no firewall is blocking port 9090
4. Try restarting both server and client

Happy testing! 🚀📈
