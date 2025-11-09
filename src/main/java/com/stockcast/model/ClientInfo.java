package com.stockcast.model;

import lombok.Data;

import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class ClientInfo {
    private String clientId;
    private SocketChannel socketChannel;
    private Set<String> subscriptions;
    private long connectedAt;

    public ClientInfo(String clientId, SocketChannel socketChannel) {
        this.clientId = clientId;
        this.socketChannel = socketChannel;
        this.subscriptions = ConcurrentHashMap.newKeySet();
        this.connectedAt = System.currentTimeMillis();
    }

    public void subscribe(String ticker) {
        subscriptions.add(ticker.toUpperCase());
    }

    public void unsubscribe(String ticker) {
        subscriptions.remove(ticker.toUpperCase());
    }

    public boolean isSubscribedTo(String ticker) {
        return subscriptions.contains(ticker.toUpperCase());
    }
}
