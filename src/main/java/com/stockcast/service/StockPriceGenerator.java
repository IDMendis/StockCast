package com.stockcast.service;

import com.stockcast.model.StockPrice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Member 2: Stock Price Simulation Module
 * Continuously generates mock stock price updates in a separate thread
 */
@Service
@Slf4j
public class StockPriceGenerator {
    
    @Value("${stockcast.price.update.interval:1000}")
    private long updateInterval;
    
    private final Map<String, Double> currentPrices = new ConcurrentHashMap<>();
    private final Map<String, Double> previousPrices = new ConcurrentHashMap<>();
    private final List<Consumer<StockPrice>> listeners = new CopyOnWriteArrayList<>();
    private Thread generatorThread;
    private volatile boolean running = false;
    private final Random random = new Random();

    @PostConstruct
    public void init() {
        // Initialize stock prices
        currentPrices.put("AAPL", 150.0);
        currentPrices.put("GOOG", 2800.0);
        currentPrices.put("MSFT", 380.0);
        currentPrices.put("AMZN", 3400.0);
        currentPrices.put("TSLA", 250.0);
        
        previousPrices.putAll(currentPrices);
    }

    public void start() {
        if (running) {
            log.warn("Stock price generator is already running");
            return;
        }
        
        running = true;
        generatorThread = new Thread(this::generatePrices, "StockPriceGenerator");
        generatorThread.setDaemon(true);
        generatorThread.start();
        log.info("Stock price generator started with update interval: {}ms", updateInterval);
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (generatorThread != null) {
            generatorThread.interrupt();
            try {
                generatorThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        log.info("Stock price generator stopped");
    }

    private void generatePrices() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                for (Map.Entry<String, Double> entry : currentPrices.entrySet()) {
                    String ticker = entry.getKey();
                    double currentPrice = entry.getValue();
                    
                    // Generate random price change between -2% and +2%
                    double changePercent = (random.nextDouble() * 4.0) - 2.0;
                    double newPrice = currentPrice * (1 + changePercent / 100.0);
                    
                    // Ensure price stays positive and reasonable
                    newPrice = Math.max(newPrice, currentPrice * 0.5);
                    newPrice = Math.min(newPrice, currentPrice * 1.5);
                    
                    previousPrices.put(ticker, currentPrice);
                    currentPrices.put(ticker, newPrice);
                    
                    // Create stock price update
                    StockPrice stockPrice = new StockPrice(
                        ticker,
                        newPrice,
                        LocalDateTime.now(),
                        changePercent
                    );
                    
                    // Notify all listeners
                    notifyListeners(stockPrice);
                }
                
                Thread.sleep(updateInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error generating stock prices", e);
            }
        }
    }

    private void notifyListeners(StockPrice stockPrice) {
        for (Consumer<StockPrice> listener : listeners) {
            try {
                listener.accept(stockPrice);
            } catch (Exception e) {
                log.error("Error notifying listener", e);
            }
        }
    }

    public void addListener(Consumer<StockPrice> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<StockPrice> listener) {
        listeners.remove(listener);
    }

    public Map<String, Double> getCurrentPrices() {
        return new HashMap<>(currentPrices);
    }

    public Set<String> getAvailableTickers() {
        return currentPrices.keySet();
    }
}
