package com.stockcast.model;

import lombok.Data;

/**
 * Represents an authenticated client session
 */
@Data
public class AuthenticatedSession {
    private String token;
    private String username;
    private String clientId;
    private long authenticatedAt;
    private boolean authenticated;

    public AuthenticatedSession() {
        this.authenticated = false;
        this.authenticatedAt = 0;
    }

    public void authenticate(String token, String username, String clientId) {
        this.token = token;
        this.username = username;
        this.clientId = clientId;
        this.authenticatedAt = System.currentTimeMillis();
        this.authenticated = true;
    }

    public void invalidate() {
        this.authenticated = false;
        this.token = null;
    }
}
