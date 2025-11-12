package com.stockcast.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Performance Metrics Service
 * 
 * Tracks and reports system performance metrics:
 * - Message throughput (messages/second)
 * - Active connections (TCP, WebSocket, UDP listeners)
 * - Subscription statistics
 * - Latency measurements
 * - Uptime
 */
@Service
@Slf4j
public class MetricsService {

    // Connection metrics
    private final AtomicInteger activeTcpClients = new AtomicInteger(0);
    private final AtomicInteger activeWebSocketClients = new AtomicInteger(0);
    private final AtomicInteger totalConnectionsServed = new AtomicInteger(0);

    // Message metrics
    private final AtomicLong totalMessagesSent = new AtomicLong(0);
    private final AtomicLong totalBytesTransferred = new AtomicLong(0);
    private final AtomicLong priceUpdatesGenerated = new AtomicLong(0);
    private final AtomicLong udpMulticastsSent = new AtomicLong(0);

    // Subscription metrics
    private final AtomicInteger totalSubscriptions = new AtomicInteger(0);
    private final Map<String, AtomicInteger> tickerSubscriptionCounts = new ConcurrentHashMap<>();

    // Performance metrics
    private final AtomicLong totalLatencyMs = new AtomicLong(0);
    private final AtomicLong latencyMeasurements = new AtomicLong(0);

    // System metrics
    @Getter
    private final LocalDateTime startTime = LocalDateTime.now();
    private final AtomicLong errorCount = new AtomicLong(0);

    // ========== Connection Metrics ==========

    public void incrementTcpClients() {
        activeTcpClients.incrementAndGet();
        totalConnectionsServed.incrementAndGet();
        log.debug("TCP clients: {}", activeTcpClients.get());
    }

    public void decrementTcpClients() {
        activeTcpClients.decrementAndGet();
        log.debug("TCP clients: {}", activeTcpClients.get());
    }

    public void incrementWebSocketClients() {
        activeWebSocketClients.incrementAndGet();
        totalConnectionsServed.incrementAndGet();
        log.debug("WebSocket clients: {}", activeWebSocketClients.get());
    }

    public void decrementWebSocketClients() {
        activeWebSocketClients.decrementAndGet();
        log.debug("WebSocket clients: {}", activeWebSocketClients.get());
    }

    public int getActiveTcpClients() {
        return activeTcpClients.get();
    }

    public int getActiveWebSocketClients() {
        return activeWebSocketClients.get();
    }

    public int getTotalActiveClients() {
        return activeTcpClients.get() + activeWebSocketClients.get();
    }

    public int getTotalConnectionsServed() {
        return totalConnectionsServed.get();
    }

    // ========== Message Metrics ==========

    public void recordMessageSent(int bytes) {
        totalMessagesSent.incrementAndGet();
        totalBytesTransferred.addAndGet(bytes);
    }

    public void recordPriceUpdate() {
        priceUpdatesGenerated.incrementAndGet();
    }

    public void recordUdpMulticast() {
        udpMulticastsSent.incrementAndGet();
    }

    public long getTotalMessagesSent() {
        return totalMessagesSent.get();
    }

    public long getTotalBytesTransferred() {
        return totalBytesTransferred.get();
    }

    public long getPriceUpdatesGenerated() {
        return priceUpdatesGenerated.get();
    }

    public long getUdpMulticastsSent() {
        return udpMulticastsSent.get();
    }

    // Calculate messages per second
    public double getMessagesPerSecond() {
        long uptimeSeconds = getUptimeSeconds();
        if (uptimeSeconds == 0) return 0;
        return (double) totalMessagesSent.get() / uptimeSeconds;
    }

    // ========== Subscription Metrics ==========

    public void recordSubscription(String ticker) {
        totalSubscriptions.incrementAndGet();
        tickerSubscriptionCounts
            .computeIfAbsent(ticker, k -> new AtomicInteger(0))
            .incrementAndGet();
    }

    public void recordUnsubscription(String ticker) {
        totalSubscriptions.decrementAndGet();
        AtomicInteger count = tickerSubscriptionCounts.get(ticker);
        if (count != null) {
            count.decrementAndGet();
        }
    }

    public int getTotalSubscriptions() {
        return totalSubscriptions.get();
    }

    public Map<String, Integer> getTickerSubscriptionCounts() {
        Map<String, Integer> result = new ConcurrentHashMap<>();
        tickerSubscriptionCounts.forEach((ticker, count) -> 
            result.put(ticker, count.get())
        );
        return result;
    }

    public String getMostPopularTicker() {
        return tickerSubscriptionCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue((a, b) -> Integer.compare(a.get(), b.get())))
            .map(Map.Entry::getKey)
            .orElse("N/A");
    }

    // ========== Latency Metrics ==========

    public void recordLatency(long latencyMs) {
        totalLatencyMs.addAndGet(latencyMs);
        latencyMeasurements.incrementAndGet();
    }

    public double getAverageLatencyMs() {
        long measurements = latencyMeasurements.get();
        if (measurements == 0) return 0;
        return (double) totalLatencyMs.get() / measurements;
    }

    // ========== Error Metrics ==========

    public void recordError() {
        errorCount.incrementAndGet();
    }

    public long getErrorCount() {
        return errorCount.get();
    }

    // ========== System Metrics ==========

    public long getUptimeSeconds() {
        return java.time.Duration.between(startTime, LocalDateTime.now()).getSeconds();
    }

    public String getUptimeFormatted() {
        long seconds = getUptimeSeconds();
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    // ========== Comprehensive Report ==========

    public Map<String, Object> getMetricsSnapshot() {
        Map<String, Object> metrics = new ConcurrentHashMap<>();
        
        // Connection metrics
        metrics.put("activeTcpClients", getActiveTcpClients());
        metrics.put("activeWebSocketClients", getActiveWebSocketClients());
        metrics.put("totalActiveClients", getTotalActiveClients());
        metrics.put("totalConnectionsServed", getTotalConnectionsServed());
        
        // Message metrics
        metrics.put("totalMessagesSent", getTotalMessagesSent());
        metrics.put("totalBytesTransferred", getTotalBytesTransferred());
        metrics.put("priceUpdatesGenerated", getPriceUpdatesGenerated());
        metrics.put("udpMulticastsSent", getUdpMulticastsSent());
        metrics.put("messagesPerSecond", String.format("%.2f", getMessagesPerSecond()));
        
        // Subscription metrics
        metrics.put("totalSubscriptions", getTotalSubscriptions());
        metrics.put("tickerSubscriptions", getTickerSubscriptionCounts());
        metrics.put("mostPopularTicker", getMostPopularTicker());
        
        // Performance metrics
        metrics.put("averageLatencyMs", String.format("%.2f", getAverageLatencyMs()));
        
        // System metrics
        metrics.put("uptime", getUptimeFormatted());
        metrics.put("uptimeSeconds", getUptimeSeconds());
        metrics.put("errorCount", getErrorCount());
        metrics.put("startTime", startTime.toString());
        
        return metrics;
    }

    /**
     * Print metrics summary to console
     */
    public void printMetricsSummary() {
        log.info("═══════════════════════════════════════════════════════════");
        log.info("                  PERFORMANCE METRICS                      ");
        log.info("═══════════════════════════════════════════════════════════");
        log.info("Uptime:               {}", getUptimeFormatted());
        log.info("Active Clients:       {} (TCP: {}, WebSocket: {})", 
            getTotalActiveClients(), getActiveTcpClients(), getActiveWebSocketClients());
        log.info("Total Connections:    {}", getTotalConnectionsServed());
        log.info("Messages Sent:        {}", getTotalMessagesSent());
        log.info("Throughput:           {:.2f} msg/sec", getMessagesPerSecond());
        log.info("Data Transferred:     {:.2f} MB", getTotalBytesTransferred() / 1024.0 / 1024.0);
        log.info("Price Updates:        {}", getPriceUpdatesGenerated());
        log.info("UDP Multicasts:       {}", getUdpMulticastsSent());
        log.info("Total Subscriptions:  {}", getTotalSubscriptions());
        log.info("Most Popular:         {}", getMostPopularTicker());
        log.info("Average Latency:      {:.2f} ms", getAverageLatencyMs());
        log.info("Errors:               {}", getErrorCount());
        log.info("═══════════════════════════════════════════════════════════");
    }

    /**
     * Reset all metrics (useful for testing)
     */
    public void reset() {
        activeTcpClients.set(0);
        activeWebSocketClients.set(0);
        totalConnectionsServed.set(0);
        totalMessagesSent.set(0);
        totalBytesTransferred.set(0);
        priceUpdatesGenerated.set(0);
        udpMulticastsSent.set(0);
        totalSubscriptions.set(0);
        tickerSubscriptionCounts.clear();
        totalLatencyMs.set(0);
        latencyMeasurements.set(0);
        errorCount.set(0);
        log.info("Metrics reset");
    }
}
