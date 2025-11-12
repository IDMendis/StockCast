# Client Authentication

## Overview

StockCast now includes **token-based authentication** for secure client access. This demonstrates security concepts in network programming.

## Authentication Flow

```
1. Client connects
2. Server requests authentication
3. Client sends: AUTH|username|password
4. Server validates credentials
5. Server returns: AUTH_OK|token|username
6. Client includes token in subsequent requests
7. Token expires after 30 minutes
```

## Protocol Messages

### Client → Server

**Authenticate:**
```
AUTH|<username>|<password>
```

**With Token:**
```
SUBSCRIBE|<token>|<tickers>
UNSUBSCRIBE|<token>|<tickers>
LIST|<token>
```

### Server → Client

**Success:**
```
AUTH_OK|<token>|<username>
```

**Failure:**
```
AUTH_FAIL|<error_message>
```

**Unauthorized:**
```
ERROR|Authentication required
ERROR|Invalid or expired token
```

## Demo Users

| Username | Password | Purpose |
|----------|----------|---------|
| demo | password123 | General demo |
| user1 | pass1 | Test user |
| user2 | pass2 | Test user |
| admin | admin123 | Admin demo |
| guest | guest | Guest access |

## Security Features

✅ **Password Hashing** - SHA-256 hashing  
✅ **Secure Tokens** - Cryptographically random  
✅ **Token Expiry** - 30-minute sessions  
✅ **Session Management** - Active session tracking  
✅ **Automatic Cleanup** - Expired sessions removed  

## Implementation

### Server Side

**AuthenticationService.java**
- Password hashing with SHA-256
- Secure token generation
- Session management
- Token validation

### Client Side

Clients must authenticate before subscribing:

```java
// 1. Connect
socket.connect(server, port);

// 2. Authenticate
socket.send("AUTH|demo|password123\n");

// 3. Receive token
String response = socket.receive(); // "AUTH_OK|abc123xyz|demo"

// 4. Use token
socket.send("SUBSCRIBE|abc123xyz|AAPL,GOOG\n");
```

## Testing

### Using TCP Client

```powershell
cd D:\StockCast\backend
.\run-client.ps1
```

When connected:
```
> auth demo password123
✓ Authenticated as: demo
Token: abc123xyz...

> subscribe AAPL
✓ Subscribed to: AAPL
```

### Using WebSocket (React)

Login form appears on connection. Enter:
- Username: `demo`
- Password: `password123`

### Manual Testing

```powershell
# Using telnet or netcat
echo "AUTH|demo|password123" | nc localhost 9092
```

## Configuration

Edit `application.properties`:

```properties
# Authentication
stockcast.auth.enabled=true
stockcast.auth.token.expiry.minutes=30
stockcast.auth.require.subscription=true
```

## Network Security Concepts Demonstrated

✅ **Authentication** - Verifying client identity  
✅ **Authorization** - Controlling resource access  
✅ **Session Management** - Tracking authenticated clients  
✅ **Cryptography** - Password hashing, secure tokens  
✅ **Token-based Auth** - Stateless authentication  
✅ **Security Best Practices** - Password storage, session expiry  

## Advantages

1. **Security** - Only authenticated users can access
2. **Tracking** - Know who is connected
3. **Rate Limiting** - Can limit per-user
4. **Accountability** - Audit trail of actions

## Limitations (Demo)

⚠️ **This is educational** - Not production-grade:
- Passwords transmitted in plaintext (would use TLS in production)
- Simple user storage (would use database)
- No password complexity requirements
- No rate limiting on auth attempts

**For Production:**
- Add TLS/SSL encryption
- Use bcrypt instead of SHA-256
- Store users in database
- Add password policies
- Implement rate limiting
- Add 2FA support

## Integration with Existing Features

### With TCP Clients
Clients must auth before subscribing

### With WebSocket
Login form on frontend

### With Metrics
Track auth attempts, active users

### With UDP
UDP multicast remains open (broadcast nature)

## Error Handling

| Error | Cause | Solution |
|-------|-------|----------|
| AUTH_FAIL | Wrong credentials | Check username/password |
| Token expired | Session timeout | Re-authenticate |
| Invalid token | Token not found | Login again |
| Already authenticated | Duplicate auth | Continue with existing token |

## Future Enhancements

- Add OAuth2 support
- Implement JWT tokens
- Add role-based access control (RBAC)
- Support API keys
- Add password reset
- Implement MFA

## Academic Value

This demonstrates:

1. **Network Security** - Authentication in distributed systems
2. **Cryptography** - Hash functions, secure random
3. **State Management** - Session tracking
4. **Protocol Design** - Secure message formats
5. **Best Practices** - Security considerations

Perfect for showing security awareness in network programming!
