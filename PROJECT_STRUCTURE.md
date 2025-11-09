# StockCast Project Structure

```
D:\StockCast
│
├── pom.xml                          # Maven configuration and dependencies
├── README.md                        # Comprehensive project documentation
├── QUICKSTART.md                    # Quick start guide
├── PROJECT_STRUCTURE.md             # This file
├── .gitignore                       # Git ignore patterns
│
├── run-server.ps1                   # PowerShell script to start server
├── run-client.ps1                   # PowerShell script to start client
│
└── src
    └── main
        ├── java
        │   └── com
        │       └── stockcast
        │           │
        │           ├── StockCastApplication.java    # Main Spring Boot application
        │           │
        │           ├── model/                       # Data models
        │           │   ├── StockPrice.java         # Stock price data structure
        │           │   └── ClientInfo.java         # Client information holder
        │           │
        │           ├── service/                     # Business logic (Server-side)
        │           │   ├── ConnectionManager.java      # Member 1: Connection handling
        │           │   ├── StockPriceGenerator.java    # Member 2: Price simulation
        │           │   ├── SubscriptionManager.java    # Member 3: Subscription tracking
        │           │   └── BroadcastModule.java        # Member 4: NIO broadcasting
        │           │
        │           ├── config/                      # Spring configuration
        │           │   └── ServerConfig.java       # Server startup configuration
        │           │
        │           └── client/                      # Client-side application
        │               └── StockCastClient.java    # Member 5: Console client
        │
        └── resources
            └── application.properties              # Application configuration
```

## File Descriptions

### Root Files

**pom.xml**
- Maven build configuration
- Dependencies: Spring Boot, Lombok
- Java 17 target
- Build plugins configuration

**README.md**
- Comprehensive documentation
- Architecture overview
- Team member contributions
- Usage instructions
- Troubleshooting guide

**QUICKSTART.md**
- Quick start guide
- Step-by-step setup
- Common issues and solutions

**run-server.ps1 / run-client.ps1**
- Convenience scripts for Windows PowerShell
- Launch server and client easily

### Source Code Structure

#### Main Application
**StockCastApplication.java**
- Spring Boot entry point
- `@SpringBootApplication` annotation
- Starts the Spring context

#### Model Package (`com.stockcast.model`)

**StockPrice.java**
- Represents a stock price update
- Fields: ticker, price, timestamp, changePercent
- Methods for serialization/deserialization

**ClientInfo.java**
- Holds client connection information
- Manages client's subscriptions
- Thread-safe subscription set

#### Service Package (`com.stockcast.service`)

**ConnectionManager.java** *(Member 1)*
- ServerSocketChannel for accepting connections
- ExecutorService for handling multiple clients
- Message protocol implementation (SUBSCRIBE, UNSUBSCRIBE, LIST, PING)
- Client lifecycle management

**StockPriceGenerator.java** *(Member 2)*
- Separate thread for price generation
- Mock stock price simulation
- Observable pattern for price updates
- Configurable update interval

**SubscriptionManager.java** *(Member 3)*
- ConcurrentHashMap for thread-safe storage
- Maps clients to subscriptions
- Maps tickers to subscribers
- Registration/unregistration logic

**BroadcastModule.java** *(Member 4)*
- Java NIO implementation (Selector + SocketChannel)
- Non-blocking I/O operations
- Queue-based message broadcasting
- Efficient multi-client updates

#### Config Package (`com.stockcast.config`)

**ServerConfig.java**
- Spring Boot configuration
- Auto-starts server on application ready
- Wires all components together

#### Client Package (`com.stockcast.client`)

**StockCastClient.java** *(Member 5)*
- Standalone client application
- SocketChannel for server connection
- Interactive console interface
- Real-time price display
- Command processing

### Resources

**application.properties**
- Server port configuration
- Price update interval
- Initial stock prices
- Logging configuration

## Component Interactions

```
1. Application Startup Flow:
   StockCastApplication
   └─> ServerConfig (ApplicationReadyEvent)
       └─> ConnectionManager.start()
           ├─> BroadcastModule.start()
           ├─> StockPriceGenerator.start()
           └─> ServerSocketChannel.accept() [loop]

2. Client Connection Flow:
   Client connects
   └─> ConnectionManager.acceptConnections()
       ├─> SubscriptionManager.registerClient()
       ├─> BroadcastModule.registerChannel()
       └─> ClientHandler thread (handleClient)

3. Subscription Flow:
   Client sends "SUBSCRIBE|AAPL,GOOG"
   └─> ConnectionManager.processClientMessage()
       └─> SubscriptionManager.subscribe()
           └─> Updates ticker subscription maps

4. Price Update Flow:
   StockPriceGenerator generates price
   └─> Notifies listeners (ConnectionManager)
       └─> ConnectionManager.onStockPriceUpdate()
           └─> SubscriptionManager.getSubscribersForTicker()
               └─> BroadcastModule.broadcast()
                   └─> Sends to all subscribed clients via NIO
```

## Threading Model

```
Main Thread
├─> Spring Boot Application Context

ClientAcceptor Thread (Member 1)
├─> Accepts new connections
└─> Spawns ClientHandler threads

ClientHandler Threads (Member 1)
├─> One per connected client
└─> Reads client messages

StockPriceGenerator Thread (Member 2)
├─> Generates prices every N milliseconds
└─> Notifies listeners

BroadcastThread (Member 4)
├─> Processes broadcast queue
└─> Writes to client channels via NIO
```

## Data Flow

```
StockPriceGenerator
    ↓ (StockPrice objects)
ConnectionManager (listener)
    ↓ (query subscribers)
SubscriptionManager
    ↓ (list of subscribed clients)
BroadcastModule
    ↓ (NIO write to channels)
Clients (receive updates)
```

## Technology Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| Framework | Spring Boot 3.2.0 | Application framework, DI |
| Language | Java 17 | Programming language |
| I/O | Java NIO | Non-blocking socket I/O |
| Concurrency | ConcurrentHashMap | Thread-safe collections |
| Build | Maven | Dependency management, build |
| Code Quality | Lombok | Reduce boilerplate |

## Member Responsibilities Mapping

| Member | Component | File | Lines | Key Features |
|--------|-----------|------|-------|--------------|
| Member 1 | Connection Manager | ConnectionManager.java | ~318 | ServerSocket, Accept, Handle clients |
| Member 2 | Price Generator | StockPriceGenerator.java | ~138 | Thread, Mock prices, Listeners |
| Member 3 | Subscription Mgr | SubscriptionManager.java | ~187 | ConcurrentHashMap, Thread-safe |
| Member 4 | Broadcast Module | BroadcastModule.java | ~211 | NIO, Selector, Non-blocking |
| Member 5 | Client Interface | StockCastClient.java | ~309 | Console UI, Display updates |

## Extension Points

Want to extend the project? Consider:

1. **Add more stocks**: Modify `StockPriceGenerator.init()`
2. **Change protocol**: Update message format in model classes
3. **Add persistence**: Save subscriptions to database
4. **Add authentication**: Require login before subscribing
5. **Add REST API**: Expose statistics via HTTP endpoints
6. **Add WebSocket**: Alternative to raw socket communication
7. **Add GUI**: JavaFX or Swing client instead of console
8. **Add metrics**: Track client count, message throughput

## Testing Strategy

1. **Unit Tests**: Test individual components in isolation
2. **Integration Tests**: Test component interactions
3. **Load Tests**: Multiple clients, stress broadcasting
4. **Concurrency Tests**: Race conditions, thread safety

## Build & Deployment

**Development:**
```powershell
mvn spring-boot:run
```

**Production JAR:**
```powershell
mvn clean package
java -jar target/stockcast-1.0.0.jar
```

**With custom properties:**
```powershell
java -jar target/stockcast-1.0.0.jar --server.port=8080
```

## Maintenance Notes

- All threads use proper cleanup in `@PreDestroy` methods
- NIO channels are properly closed on disconnect
- Thread-safe collections prevent race conditions
- Daemon threads ensure proper JVM shutdown
- Logging at INFO level for production, DEBUG for development
