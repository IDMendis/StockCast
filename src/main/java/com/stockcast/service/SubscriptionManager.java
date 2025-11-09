package com.stockcast.service;

import com.stockcast.model.ClientInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Member 3: Client Subscription Management
 * Tracks which clients subscribe to which stock tickers using thread-safe collections
 */
@Service
@Slf4j
public class SubscriptionManager {
    
    // Map of clientId -> ClientInfo
    private final Map<String, ClientInfo> clients = new ConcurrentHashMap<>();
    
    // Map of ticker -> Set of clientIds subscribed to that ticker
    private final Map<String, Set<String>> tickerSubscriptions = new ConcurrentHashMap<>();

    /**
     * Register a new client
     */
    public void registerClient(String clientId, SocketChannel socketChannel) {
        ClientInfo clientInfo = new ClientInfo(clientId, socketChannel);
        clients.put(clientId, clientInfo);
        log.info("Client registered: {}", clientId);
    }

    /**
     * Unregister a client and clean up all subscriptions
     */
    public void unregisterClient(String clientId) {
        ClientInfo clientInfo = clients.remove(clientId);
        if (clientInfo != null) {
            // Remove client from all ticker subscriptions
            for (String ticker : clientInfo.getSubscriptions()) {
                Set<String> subscribers = tickerSubscriptions.get(ticker);
                if (subscribers != null) {
                    subscribers.remove(clientId);
                    if (subscribers.isEmpty()) {
                        tickerSubscriptions.remove(ticker);
                    }
                }
            }
            log.info("Client unregistered: {}", clientId);
        }
    }

    /**
     * Subscribe a client to one or more tickers
     */
    public void subscribe(String clientId, String... tickers) {
        ClientInfo clientInfo = clients.get(clientId);
        if (clientInfo == null) {
            log.warn("Cannot subscribe: client {} not found", clientId);
            return;
        }

        for (String ticker : tickers) {
            String normalizedTicker = ticker.toUpperCase().trim();
            if (normalizedTicker.isEmpty()) {
                continue;
            }

            // Add to client's subscription list
            clientInfo.subscribe(normalizedTicker);

            // Add to ticker's subscriber list
            tickerSubscriptions
                .computeIfAbsent(normalizedTicker, k -> ConcurrentHashMap.newKeySet())
                .add(clientId);

            log.info("Client {} subscribed to {}", clientId, normalizedTicker);
        }
    }

    /**
     * Unsubscribe a client from one or more tickers
     */
    public void unsubscribe(String clientId, String... tickers) {
        ClientInfo clientInfo = clients.get(clientId);
        if (clientInfo == null) {
            log.warn("Cannot unsubscribe: client {} not found", clientId);
            return;
        }

        for (String ticker : tickers) {
            String normalizedTicker = ticker.toUpperCase().trim();
            
            // Remove from client's subscription list
            clientInfo.unsubscribe(normalizedTicker);

            // Remove from ticker's subscriber list
            Set<String> subscribers = tickerSubscriptions.get(normalizedTicker);
            if (subscribers != null) {
                subscribers.remove(clientId);
                if (subscribers.isEmpty()) {
                    tickerSubscriptions.remove(normalizedTicker);
                }
            }

            log.info("Client {} unsubscribed from {}", clientId, normalizedTicker);
        }
    }

    /**
     * Get all clients subscribed to a specific ticker
     */
    public List<ClientInfo> getSubscribersForTicker(String ticker) {
        String normalizedTicker = ticker.toUpperCase().trim();
        Set<String> subscriberIds = tickerSubscriptions.get(normalizedTicker);
        
        if (subscriberIds == null || subscriberIds.isEmpty()) {
            return Collections.emptyList();
        }

        return subscriberIds.stream()
            .map(clients::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * Get client info by ID
     */
    public ClientInfo getClient(String clientId) {
        return clients.get(clientId);
    }

    /**
     * Get all registered clients
     */
    public List<ClientInfo> getAllClients() {
        return new ArrayList<>(clients.values());
    }

    /**
     * Get subscriptions for a specific client
     */
    public Set<String> getClientSubscriptions(String clientId) {
        ClientInfo clientInfo = clients.get(clientId);
        return clientInfo != null ? 
            new HashSet<>(clientInfo.getSubscriptions()) : 
            Collections.emptySet();
    }

    /**
     * Check if a client is subscribed to a ticker
     */
    public boolean isClientSubscribed(String clientId, String ticker) {
        ClientInfo clientInfo = clients.get(clientId);
        return clientInfo != null && clientInfo.isSubscribedTo(ticker);
    }

    /**
     * Get total number of clients
     */
    public int getClientCount() {
        return clients.size();
    }

    /**
     * Get total number of subscriptions across all clients
     */
    public int getTotalSubscriptionCount() {
        return clients.values().stream()
            .mapToInt(client -> client.getSubscriptions().size())
            .sum();
    }

    /**
     * Get statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalClients", getClientCount());
        stats.put("totalSubscriptions", getTotalSubscriptionCount());
        stats.put("activeTickers", tickerSubscriptions.keySet());
        return stats;
    }
}
