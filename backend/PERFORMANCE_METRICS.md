# Performance Metrics Dashboard

## Overview

StockCast includes a comprehensive **Performance Metrics Service** that tracks system performance in real-time. This demonstrates production-ready monitoring and scalability awareness.

## What's Tracked

### Connection Metrics
- Active TCP clients
- Active WebSocket clients  
- Total connections served
- Connection history

### Message Metrics
- Total messages sent
- Bytes transferred
- Price updates generated
- UDP multicasts sent
- Messages per second (throughput)

### Subscription Metrics
- Total active subscriptions
- Per-ticker subscription counts
- Most popular ticker

### Performance Metrics
- Average latency
- Error count
- System uptime

## Accessing Metrics

### 1. HTTP REST API

Metrics are exposed via REST endpoints:

**Base URL:** `http://localhost:9091/api/metrics`

#### All Metrics
```bash
curl http://localhost:9091/api/metrics
```

#### Specific Categories
```bash
# Connection stats
curl http://localhost:9091/api/metrics/connections

# Message stats
curl http://localhost:9091/api/metrics/messages

# Subscription stats
curl http://localhost:9091/api/metrics/subscriptions

# System stats
curl http://localhost:9091/api/metrics/system
```

### 2. Console Logs

Metrics are printed to console every 60 seconds:

```
═══════════════════════════════════════════════════════════
                  PERFORMANCE METRICS                      
═══════════════════════════════════════════════════════════
Uptime:               00:15:32
Active Clients:       5 (TCP: 2, WebSocket: 3)
Total Connections:    12
Messages Sent:        3456
Throughput:           3.71 msg/sec
Data Transferred:     0.85 MB
Price Updates:        930
UDP Multicasts:       3
Total Subscriptions:  15
Most Popular:         AAPL
Average Latency:      2.34 ms
Errors:               0
═══════════════════════════════════════════════════════════
```

### 3. Web Browser

Open in browser:
```
http://localhost:9091/api/metrics
```

Returns JSON:
```json
{
  "activeTcpClients": 2,
  "activeWebSocketClients": 3,
  "totalActiveClients": 5,
  "totalConnectionsServed": 12,
  "totalMessagesSent": 3456,
  "messagesPerSecond": "3.71",
  "priceUpdatesGenerated": 930,
  "totalSubscriptions": 15,
  "mostPopularTicker": "AAPL",
  "uptime": "00:15:32",
  "errorCount": 0
}
