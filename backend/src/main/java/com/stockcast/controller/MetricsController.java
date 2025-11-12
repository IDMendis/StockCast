package com.stockcast.controller;

import com.stockcast.service.MetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST Controller for Performance Metrics
 * 
 * Exposes metrics via HTTP endpoints for monitoring and dashboards
 * Access at: http://localhost:9090/api/metrics
 */
@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allow frontend to access
public class MetricsController {

    private final MetricsService metricsService;

    /**
     * Get complete metrics snapshot
     * GET /api/metrics
     */
    @GetMapping
    public Map<String, Object> getMetrics() {
        return metricsService.getMetricsSnapshot();
    }

    /**
     * Get connection statistics
     * GET /api/metrics/connections
     */
    @GetMapping("/connections")
    public Map<String, Object> getConnectionMetrics() {
        return Map.of(
            "activeTcpClients", metricsService.getActiveTcpClients(),
            "activeWebSocketClients", metricsService.getActiveWebSocketClients(),
            "totalActiveClients", metricsService.getTotalActiveClients(),
            "totalConnectionsServed", metricsService.getTotalConnectionsServed()
        );
    }

    /**
     * Get message statistics
     * GET /api/metrics/messages
     */
    @GetMapping("/messages")
    public Map<String, Object> getMessageMetrics() {
        return Map.of(
            "totalMessagesSent", metricsService.getTotalMessagesSent(),
            "totalBytesTransferred", metricsService.getTotalBytesTransferred(),
            "priceUpdatesGenerated", metricsService.getPriceUpdatesGenerated(),
            "udpMulticastsSent", metricsService.getUdpMulticastsSent(),
            "messagesPerSecond", metricsService.getMessagesPerSecond()
        );
    }

    /**
     * Get subscription statistics
     * GET /api/metrics/subscriptions
     */
    @GetMapping("/subscriptions")
    public Map<String, Object> getSubscriptionMetrics() {
        return Map.of(
            "totalSubscriptions", metricsService.getTotalSubscriptions(),
            "tickerSubscriptions", metricsService.getTickerSubscriptionCounts(),
            "mostPopularTicker", metricsService.getMostPopularTicker()
        );
    }

    /**
     * Get system statistics
     * GET /api/metrics/system
     */
    @GetMapping("/system")
    public Map<String, Object> getSystemMetrics() {
        return Map.of(
            "uptime", metricsService.getUptimeFormatted(),
            "uptimeSeconds", metricsService.getUptimeSeconds(),
            "startTime", metricsService.getStartTime().toString(),
            "errorCount", metricsService.getErrorCount(),
            "averageLatencyMs", metricsService.getAverageLatencyMs()
        );
    }

    /**
     * Reset all metrics (useful for testing)
     * POST /api/metrics/reset
     */
    @GetMapping("/reset")
    public Map<String, String> resetMetrics() {
        metricsService.reset();
        return Map.of("status", "Metrics reset successfully");
    }
}
