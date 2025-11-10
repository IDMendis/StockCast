package com.stockcast.config;

import com.stockcast.service.ConnectionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Configuration class to start the server when Spring Boot application is ready
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ServerConfig {

    private final ConnectionManager connectionManager;

    @EventListener(ApplicationReadyEvent.class)
    public void startServer() {
        try {
            connectionManager.start();
            log.info("=".repeat(70));
            log.info("StockCast Server is ready to accept connections");
            log.info("Available stock tickers: AAPL, GOOG, MSFT, AMZN, TSLA");
            log.info("=".repeat(70));
            
            // Keep the application running indefinitely
            Thread.currentThread().join();
        } catch (IOException e) {
            log.error("Failed to start server", e);
            System.exit(1);
        } catch (InterruptedException e) {
            log.info("Server interrupted");
            Thread.currentThread().interrupt();
        }
    }
}
