# StockCast - Real-Time Stock Price Broadcasting System

A Spring Boot-based server-client system that simulates a stock market by generating mock stock prices and broadcasting them to subscribed clients in real-time using Java NIO.

## System Overview

StockCast demonstrates:
- **Real-time stock price generation** with continuous updates
- **Client subscription management** per stock ticker
- **Efficient broadcasting** using Java NIO (SocketChannel + Selector)
- **Multi-client support** with thread-safe operations
- **Non-blocking I/O** for scalable communication

## Team Member Contributions

### Member 1: Server Setup & Connection Handling
**File:** `ConnectionManager.java`
- Implements ServerSocket to accept multiple client connections
- Manages client connections and lifecycle
- Handles client message processing (SUBSCRIBE, UNSUBSCRIBE, LIST, PING)
- Thread pool for handling multiple clients concurrently

### Member 2: Stock Price Simulation Module
**File:** `StockPriceGenerator.java`
- Continuously generates mock stock price updates in a separate thread
- Simulates realistic price changes (-2% to +2% per update)
- Notifies listeners when prices update
- Default tickers: AAPL, GOOG, MSFT, AMZN, TSLA

### Member 3: Client Subscription Management
**File:** `SubscriptionManager.java`
- Tracks which clients subscribe to which stock tickers
- Uses thread-safe collections (ConcurrentHashMap)
- Manages registration/unregistration of clients
- Provides efficient lookup of subscribers per ticker

### Member 4: Efficient Broadcasting & NIO
**File:** `BroadcastModule.java`
- Broadcasts price updates using Java NIO (SocketChannel + Selector)
- Non-blocking communication for scalability
- Queue-based broadcasting to handle multiple simultaneous updates
- Efficient message delivery to subscribed clients only

### Member 5: Client Interface & Update Display
**File:** `StockCastClient.java`
- Console-based client application
- Real-time display of stock price updates
- Interactive command interface for subscriptions
- Visual indicators for price changes (↑↓→)

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                  StockCast Server                        │
├─────────────────────────────────────────────────────────┤
│  ConnectionManager (Member 1)                            │
│    - Accept client connections                           │
│    - Process client commands                             │
│                                                           │
│  StockPriceGenerator (Member 2)                          │
│    - Generate mock prices                                │
│    - Notify listeners                                    │
│                                                           │
│  SubscriptionManager (Member 3)                          │
│    - Track subscriptions                                 │
│    - Thread-safe operations                              │
│                                                           │
│  BroadcastModule (Member 4)                              │
│    - NIO-based broadcasting                              │
│    - Non-blocking I/O                                    │
└─────────────────────────────────────────────────────────┘
                          │
                          │ TCP/IP (Java NIO)
                          │
┌─────────────────────────────────────────────────────────┐
│              StockCastClient (Member 5)                  │
├─────────────────────────────────────────────────────────┤
│  - Subscribe to tickers                                  │
│  - Receive real-time updates                             │
│  - Display prices in console                             │
└─────────────────────────────────────────────────────────┘
```

## Technologies Used

- **Spring Boot 3.2.0** - Application framework
- **Java 17** - Programming language
- **Java NIO** - Non-blocking I/O for efficient communication
- **ConcurrentHashMap** - Thread-safe collections
- **Maven** - Build tool
- **Lombok** - Reduce boilerplate code

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

## Building the Project

```powershell
# Navigate to project directory
cd D:\StockCast

# Build the project
mvn clean package
```

## Running the Server

```powershell
# Using Maven
mvn spring-boot:run

# Or using JAR
java -jar target/stockcast-1.0.0.jar
```

The server will start on port **9090** by default.

## Running the Client

Open a new terminal/PowerShell window:

```powershell
# Using Maven
mvn exec:java -Dexec.mainClass="com.stockcast.client.StockCastClient"

# Or compile and run directly
mvn compile
java -cp target/classes com.stockcast.client.StockCastClient

# Connect to custom host/port
java -cp target/classes com.stockcast.client.StockCastClient localhost 9090
```

## Client Commands

Once connected, use these commands:

| Command | Description | Example |
|---------|-------------|---------|
| `subscribe <tickers>` | Subscribe to stock updates | `subscribe AAPL,GOOG` |
| `unsubscribe <tickers>` | Unsubscribe from updates | `unsubscribe AAPL` |
| `list` | Show current subscriptions | `list` |
| `ping` | Check server connection | `ping` |
| `clear` | Clear the screen | `clear` |
| `help` | Show help message | `help` |
| `quit` | Exit the client | `quit` |

## Protocol

The system uses a simple text-based protocol with pipe-delimited messages:

### Client → Server
- `SUBSCRIBE|AAPL,GOOG` - Subscribe to tickers
- `UNSUBSCRIBE|MSFT` - Unsubscribe from tickers
- `LIST` - Get current subscriptions
- `PING` - Ping server

### Server → Client
- `WELCOME|<clientId>|Available tickers: ...` - Welcome message
- `PRICE|<ticker>|<price>|<timestamp>|<change%>` - Price update
- `ACK|<message>` - Acknowledgment
- `SUBSCRIPTIONS|<ticker1,ticker2>` - Subscription list
- `PONG` - Ping response

## Configuration

Edit `src/main/resources/application.properties`:

```properties
# Server port
server.port=9090

# Price update interval (milliseconds)
stockcast.price.update.interval=1000

# Initial stock prices
stockcast.price.initial.aapl=150.0
stockcast.price.initial.goog=2800.0
stockcast.price.initial.msft=380.0
stockcast.price.initial.amzn=3400.0
stockcast.price.initial.tsla=250.0
```

## Example Session

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

> subscribe AAPL,TSLA
✓ Subscribed to: AAPL,TSLA

[12:34:56] AAPL   $150.25    ↑ +0.17%
[12:34:56] TSLA   $249.87    ↓ -0.05%
[12:34:57] AAPL   $150.48    ↑ +0.15%
[12:34:57] TSLA   $250.12    ↑ +0.10%
```

## Testing Multiple Clients

Open multiple terminal windows and run the client in each. Each client can:
- Subscribe to different tickers
- Receive updates independently
- Connect/disconnect without affecting others

## Key Concepts Demonstrated

### 1. TCP Sockets
- ServerSocketChannel for accepting connections
- SocketChannel for client-server communication

### 2. Multithreading
- Separate thread for stock price generation
- Thread pool for handling multiple clients
- Daemon threads for background tasks

### 3. Java NIO (Non-blocking I/O)
- SocketChannel + Selector for efficient I/O
- Non-blocking read/write operations
- Scalable to hundreds of clients

### 4. Concurrency Control
- ConcurrentHashMap for thread-safe storage
- CopyOnWriteArrayList for listener management
- Volatile flags for thread coordination
- Proper synchronization and cleanup

### 5. Client-Server Communication
- Text-based protocol
- Message framing with newlines
- Command-response pattern
- Push-based updates

## Stopping the Server

Press `Ctrl+C` in the server terminal. The server will:
- Stop accepting new connections
- Close all client connections
- Shut down thread pools
- Release all resources

## Troubleshooting

**Port already in use:**
```properties
# Change port in application.properties
server.port=9091
```

**Connection refused:**
- Ensure server is running
- Check firewall settings
- Verify correct host and port

**No price updates:**
- Check if you've subscribed to tickers
- Use `list` command to verify subscriptions
- Ensure tickers are uppercase (AAPL, not aapl)

## License

This is an educational project demonstrating server-client architecture with Java NIO.
