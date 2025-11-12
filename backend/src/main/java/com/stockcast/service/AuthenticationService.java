
package com.stockcast.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple token-based authentication service for clients.
 */
@Service
public class AuthenticationService {
	// In-memory map: token -> username (or clientId)
	private final Map<String, String> tokenStore = new ConcurrentHashMap<>();

	/**
	 * Authenticate a user and generate a token.
	 * For demo: accepts any username/password, returns a token.
	 */
	public String authenticate(String username, String password) {
		// In production, check username/password!
		String token = UUID.randomUUID().toString();
		tokenStore.put(token, username);
		return token;
	}

	/**
	 * Validate a token.
	 */
	public boolean isValidToken(String token) {
		return token != null && tokenStore.containsKey(token);
	}

	/**
	 * Invalidate a token (logout).
	 */
	public void invalidateToken(String token) {
		if (token != null) {
			tokenStore.remove(token);
		}
	}

	/**
	 * Get username by token.
	 */
	public String getUsernameForToken(String token) {
		return tokenStore.get(token);
	}
}
