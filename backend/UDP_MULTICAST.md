# UDP Multicast Broadcasting

## Overview

StockCast now supports **UDP Multicast** for broadcasting market-wide announcements. This demonstrates understanding of both **TCP** (reliable, connection-oriented) and **UDP** (fast, connectionless) protocols.

## Why UDP Multicast?

### TCP vs UDP Comparison

| Feature | TCP (Current Stock Updates) | UDP Multicast (Announcements) |
|---------|----------------------------|------------------------------|
| **Reliability** | Guaranteed delivery | Best-effort delivery |
| **Connection** | Connection-oriented | Connectionless |
| **Speed** | Slower (overhead) | Faster (no overhead) |
| **Use Case** | Stock price updates | Market-wide announcements |
| **Subscription** | Explicit subscribe | Join multicast group |

### When to Use UDP Multicast

✅ Market open/close announcements  
✅ Breaking news alerts  
✅ System status updates  
✅ One-to-many broadcasts  
✅ Time-sensitive, non-critical data  

## How It Works

### Multicast Group

- **Address:** `230.0.0.1` (Class D multicast address)
- **Port:** `4446`
- **Protocol:** UDP

### Message Format

```
ANNOUNCEMENT|<TYPE>|<MESSAGE>|<TIMESTAMP>
```

**Types:**
- `SYSTEM` - System announcements
- `MARKET` - Market summaries
- `NEWS` - Breaking news
- `ALERT` - Important alerts

### Example Messages

```
ANNOUNCEMENT|SYSTEM|Market data streaming active|1699876543210
ANNOUNCEMENT|MARKET|Active clients: 5 | Total subscriptions: 12|1699876543210
ANNOUNCEMENT|ALERT|High volatility detected|1699876543210
```

## Running the UDP Listener

### Option 1: PowerShell Script

```powershell
cd D:\StockCast\backend
.\run-udp-listener.ps1
```

### Option 2: Direct Java

```powershell
mvn compile
java -cp target/classes com.stockcast.client.UDPMulticastListener
```

### Option 3: Custom Address/Port

```powershell
java -cp target/classes com.stockcast.client.UDPMulticastListener 230.0.0.1 4446
```

## Configuration

Edit `application.properties`:

```properties
# UDP Multicast Configuration
stockcast.multicast.address=230.0.0.1
stockcast.multicast.port=4446
```

## Testing

### 1. Start the Backend Server

```powershell
cd D:\StockCast\backend
mvn spring-boot:run
```

Server automatically starts UDP broadcaster.

### 2. Start UDP Listener(s)

Open new terminal windows:

```powershell
# Terminal 2
.\run-udp-listener.ps1

# Terminal 3 (another listener)
.\run-udp-listener.ps1
```

### 3. Observe Announcements

You'll see:

```
[12:34:56] ℹ️ SYSTEM: Market data streaming active
[12:39:56] 📊 MARKET: Active clients: 3 | Total subscriptions: 8
```

## Implementation Details

### Server Side

**UDPMulticastBroadcaster.java**
- Uses `MulticastSocket` for sending
- Queues messages for non-blocking send
- Dedicated thread for broadcasting

```java
// Send announcement
udpBroadcaster.sendAnnouncement("MARKET", "Trading day started");
```

### Client Side

**UDPMulticastListener.java**
- Joins multicast group using `MulticastSocket`
- Receives datagrams without subscribing
- Non-blocking reception

```java
// Join multicast group
socket.joinGroup(group, networkInterface);
```

## Network Concepts Demonstrated

✅ **UDP Protocol** - Connectionless, fast delivery  
✅ **Multicast** - One-to-many communication  
✅ **Datagram Sockets** - Packet-based transmission  
✅ **Network Interfaces** - Joining multicast groups  
✅ **Port Management** - Multiple protocols on different ports  

## Advantages

1. **Scalability** - Send to many clients with one packet
2. **Speed** - No connection overhead
3. **Efficiency** - Network-level multicast routing
4. **Independence** - No subscription management needed

## Limitations

1. **Unreliable** - Packets may be lost
2. **No ACK** - Can't confirm delivery
3. **Firewall** - May block multicast traffic
4. **Order** - Packets may arrive out of order

## Real-World Applications

- **Stock Exchanges** - Market status updates
- **Video Streaming** - IPTV, live broadcasts
- **Gaming** - Real-time game state updates
- **IoT** - Sensor broadcasts, device discovery

## Comparison with TCP Stock Updates

| Aspect | TCP Stock Updates | UDP Announcements |
|--------|------------------|-------------------|
| Data Volume | High (continuous) | Low (periodic) |
| Importance | Critical | Informational |
| Clients | Subscribed only | All listeners |
| Latency | Moderate | Very low |
| Loss Tolerance | Zero | Acceptable |

## Troubleshooting

### "Cannot join multicast group"

- Check firewall allows multicast (224.0.0.0/4)
- Verify network interface supports multicast
- Try different multicast address

### "No messages received"

- Ensure server is running and sending
- Check multicast address matches
- Verify port number is correct
- Check Windows Firewall rules

### "Connection refused"

This error doesn't apply to UDP - it's connectionless!

## Future Enhancements

- Add multicast TTL configuration
- Support multiple multicast groups
- Add message filtering on client
- Implement rate limiting

## Academic Value

This demonstrates:

1. **Protocol Selection** - Choosing right protocol for task
2. **Trade-offs** - Reliability vs Speed
3. **Scalability** - One-to-many vs One-to-one
4. **Real-world** - How financial systems work

Perfect for explaining network programming concepts!
