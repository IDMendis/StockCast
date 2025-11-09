package com.stockcast.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages WebSocket client subscriptions
 */
@Service
@Slf4j
public class WebSocketSubscriptionManager {
    
    // Map of sessionId -> WebSocketSession
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    // Map of sessionId -> Set of tickers
    private final Map<String, Set<String>> sessionSubscriptions = new ConcurrentHashMap<>();
    
    // Map of ticker -> Set of sessionIds
    private final Map<String, Set<String>> tickerSubscriptions = new ConcurrentHashMap<>();

    public void registerSession(WebSocketSession session) {
        sessions.put(session.getId(), session);
        sessionSubscriptions.put(session.getId(), ConcurrentHashMap.newKeySet());
        log.info("WebSocket session registered: {}", session.getId());
    }

    public void unregisterSession(String sessionId) {
        WebSocketSession session = sessions.remove(sessionId);
        if (session != null) {
            // Remove all subscriptions for this session
            Set<String> tickers = sessionSubscriptions.remove(sessionId);
            if (tickers != null) {
                for (String ticker : tickers) {
                    Set<String> subscribers = tickerSubscriptions.get(ticker);
                    if (subscribers != null) {
                        subscribers.remove(sessionId);
                        if (subscribers.isEmpty()) {
                            tickerSubscriptions.remove(ticker);
                        }
                    }
                }
            }
            log.info("WebSocket session unregistered: {}", sessionId);
        }
    }

    public void subscribe(String sessionId, String... tickers) {
        Set<String> subscriptions = sessionSubscriptions.get(sessionId);
        if (subscriptions == null) {
            log.warn("Cannot subscribe: session {} not found", sessionId);
            return;
        }

        for (String ticker : tickers) {
            String normalizedTicker = ticker.toUpperCase().trim();
            if (normalizedTicker.isEmpty()) {
                continue;
            }

            // Add to session's subscriptions
            subscriptions.add(normalizedTicker);

            // Add to ticker's subscribers
            tickerSubscriptions
                .computeIfAbsent(normalizedTicker, k -> ConcurrentHashMap.newKeySet())
                .add(sessionId);

            log.info("WebSocket session {} subscribed to {}", sessionId, normalizedTicker);
        }
    }

    public void unsubscribe(String sessionId, String... tickers) {
        Set<String> subscriptions = sessionSubscriptions.get(sessionId);
        if (subscriptions == null) {
            log.warn("Cannot unsubscribe: session {} not found", sessionId);
            return;
        }

        for (String ticker : tickers) {
            String normalizedTicker = ticker.toUpperCase().trim();
            
            // Remove from session's subscriptions
            subscriptions.remove(normalizedTicker);

            // Remove from ticker's subscribers
            Set<String> subscribers = tickerSubscriptions.get(normalizedTicker);
            if (subscribers != null) {
                subscribers.remove(sessionId);
                if (subscribers.isEmpty()) {
                    tickerSubscriptions.remove(normalizedTicker);
                }
            }

            log.info("WebSocket session {} unsubscribed from {}", sessionId, normalizedTicker);
        }
    }

    public List<WebSocketSession> getSubscribersForTicker(String ticker) {
        String normalizedTicker = ticker.toUpperCase().trim();
        Set<String> subscriberIds = tickerSubscriptions.get(normalizedTicker);
        
        if (subscriberIds == null || subscriberIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<WebSocketSession> result = new ArrayList<>();
        for (String sessionId : subscriberIds) {
            WebSocketSession session = sessions.get(sessionId);
            if (session != null && session.isOpen()) {
                result.add(session);
            }
        }
        return result;
    }

    public Set<String> getSubscriptions(String sessionId) {
        Set<String> subscriptions = sessionSubscriptions.get(sessionId);
        return subscriptions != null ? new HashSet<>(subscriptions) : Collections.emptySet();
    }

    public int getSessionCount() {
        return sessions.size();
    }
}
