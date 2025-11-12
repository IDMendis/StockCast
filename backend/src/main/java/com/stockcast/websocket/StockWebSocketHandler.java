package com.stockcast.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockcast.model.StockPrice;
import com.stockcast.service.MetricsService;
import com.stockcast.service.StockPriceGenerator;
import com.stockcast.service.WebSocketSubscriptionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket handler for browser-based clients
 * Bridges the gap between the existing NIO socket system and WebSocket
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class StockWebSocketHandler extends TextWebSocketHandler {

    private final WebSocketSubscriptionManager subscriptionManager;
    private final StockPriceGenerator stockPriceGenerator;
    private final MetricsService metricsService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        // Listen to stock price updates and broadcast to WebSocket clients
        stockPriceGenerator.addListener(this::broadcastStockPrice);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket client connected: {}", session.getId());
        subscriptionManager.registerSession(session);
        metricsService.incrementWebSocketClients();

        // Send welcome message
        Map<String, Object> welcome = new HashMap<>();
        welcome.put("type", "WELCOME");
        welcome.put("clientId", session.getId());
        welcome.put("availableTickers", stockPriceGenerator.getAvailableTickers());
        welcome.put("currentPrices", stockPriceGenerator.getCurrentPrices());

        sendMessage(session, welcome);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("Received message from {}: {}", session.getId(), payload);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> msg = objectMapper.readValue(payload, Map.class);
            String command = (String) msg.get("command");

            switch (command) {
                case "SUBSCRIBE":
                    handleSubscribe(session, msg);
                    break;
                case "UNSUBSCRIBE":
                    handleUnsubscribe(session, msg);
                    break;
                case "LIST":
                    handleList(session);
                    break;
                case "PING":
                    handlePing(session);
                    break;
                default:
                    sendError(session, "Unknown command: " + command);
            }
        } catch (Exception e) {
            log.error("Error processing message", e);
            sendError(session, "Invalid message format");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket client disconnected: {}", session.getId());
        subscriptionManager.unregisterSession(session.getId());
        metricsService.decrementWebSocketClients();
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket error for session {}", session.getId(), exception);
        subscriptionManager.unregisterSession(session.getId());
        metricsService.decrementWebSocketClients();
    }

    private void handleSubscribe(WebSocketSession session, Map<String, Object> msg) throws IOException {
        @SuppressWarnings("unchecked")
        java.util.List<String> tickers = (java.util.List<String>) msg.get("tickers");

        if (tickers != null && !tickers.isEmpty()) {
            subscriptionManager.subscribe(session.getId(), tickers.toArray(new String[0]));

            // Record subscription metrics
            for (String ticker : tickers) {
                metricsService.recordSubscription(ticker);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("type", "ACK");
            response.put("message", "Subscribed to: " + String.join(", ", tickers));
            sendMessage(session, response);
        }
    }

    private void handleUnsubscribe(WebSocketSession session, Map<String, Object> msg) throws IOException {
        @SuppressWarnings("unchecked")
        java.util.List<String> tickers = (java.util.List<String>) msg.get("tickers");

        if (tickers != null && !tickers.isEmpty()) {
            subscriptionManager.unsubscribe(session.getId(), tickers.toArray(new String[0]));
            
            // Record unsubscription metrics
            for (String ticker : tickers) {
                metricsService.recordUnsubscription(ticker);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("type", "ACK");
            response.put("message", "Unsubscribed from: " + String.join(", ", tickers));
            sendMessage(session, response);
        }
    }

    private void handleList(WebSocketSession session) throws IOException {
        var subscriptions = subscriptionManager.getSubscriptions(session.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("type", "SUBSCRIPTIONS");
        response.put("subscriptions", subscriptions);
        sendMessage(session, response);
    }

    private void handlePing(WebSocketSession session) throws IOException {
        Map<String, Object> response = new HashMap<>();
        response.put("type", "PONG");
        sendMessage(session, response);
    }

    private void broadcastStockPrice(StockPrice stockPrice) {
        var subscribers = subscriptionManager.getSubscribersForTicker(stockPrice.getTicker());

        Map<String, Object> priceMsg = new HashMap<>();
        priceMsg.put("type", "PRICE");
        priceMsg.put("ticker", stockPrice.getTicker());
        priceMsg.put("price", stockPrice.getPrice());
        priceMsg.put("timestamp", stockPrice.getTimestamp().toString());
        priceMsg.put("changePercent", stockPrice.getChangePercent());

        for (WebSocketSession session : subscribers) {
            try {
                sendMessage(session, priceMsg);
            } catch (IOException e) {
                log.error("Error sending to session {}", session.getId(), e);
            }
        }
    }

    private void sendMessage(WebSocketSession session, Map<String, Object> message) throws IOException {
        if (session.isOpen()) {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        }
    }

    private void sendError(WebSocketSession session, String error) throws IOException {
        Map<String, Object> errorMsg = new HashMap<>();
        errorMsg.put("type", "ERROR");
        errorMsg.put("message", error);
        sendMessage(session, errorMsg);
    }
}
