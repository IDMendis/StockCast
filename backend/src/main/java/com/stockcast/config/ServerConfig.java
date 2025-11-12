package com.stockcast.config;

import com.stockcast.service.ConnectionManager;
import com.stockcast.service.UDPMulticastBroadcaster;
import com.stockcast.service.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Configuration class to start the server when Spring Boot application is ready
 */
@Component
@Slf4j
@RequiredArgsConstructor
@EnableScheduling
public class ServerConfig {

    private final ConnectionManager connectionManager;
    private final UDPMulticastBroadcaster udpBroadcaster;
    private final MetricsService metricsService;

    @EventListener(ApplicationReadyEvent.class)
    public void startServer() {
        try {
            // Start TCP/WebSocket server
            connectionManager.start();

            // Start UDP multicast broadcaster
            udpBroadcaster.start();

            log.info("=".repeat(70));
            log.info("StockCast Server is ready to accept connections");
            log.info("Available stock tickers: AAPL, GOOG, MSFT, AMZN, TSLA");
            log.info("TCP Server: {}", connectionManager.isRunning() ? "Running" : "Stopped");
            log.info("UDP Multicast: {}:{}",
                    udpBroadcaster.getMulticastAddress(),
                    udpBroadcaster.getMulticastPort());
            log.info("Metrics API: http://localhost:9091/api/metrics");
            log.info("=".repeat(70));
        } catch (IOException e) {
            log.error("Failed to start server", e);
            System.exit(1);
        }
    }

    /**
     * Print metrics summary every 60 seconds
     */
    @Scheduled(fixedRate = 60000, initialDelay = 60000)
    public void printMetrics() {
        try {
            metricsService.printMetricsSummary();
        } catch (Exception e) {
            log.error("Error printing metrics summary", e);
        }
    }

    /**
     * Send periodic UDP announcements
     */
    @Scheduled(fixedRate = 300000, initialDelay = 10000) // Every 5 minutes
    public void sendPeriodicAnnouncements() {
        udpBroadcaster.broadcastMarketSummary(
                String.format("Active clients: %d | Total subscriptions: %d",
                        metricsService.getTotalActiveClients(),
                        metricsService.getTotalSubscriptions()));
    }
}
