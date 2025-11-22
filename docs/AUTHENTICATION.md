# PartyWave – Authentication & Security

This document provides detailed specifications for **JWT-based application authentication** and **WebSocket authentication** in PartyWave. It is written for AI agents and backend developers implementing authentication and authorization mechanisms.

---

## 1. Authentication Architecture Overview

PartyWave uses a **two-layer authentication system**:

1. **Spotify OAuth2**: External authentication provider (see `SPOTIFY_AUTH_ENDPOINTS.md`)
2. **JWT-based Application Authentication**: Internal authentication for PartyWave API and WebSocket connections

**Key Principle**: Spotify OAuth2 authenticates users with Spotify, but **all PartyWave API requests and WebSocket connections must be authenticated using application-level JWT tokens**.

---

## 2. JWT-Based Application Authentication

### 2.1 JWT Token Generation

After successful Spotify OAuth2 authentication, the backend generates a **PartyWave JWT token** for the user.

**When**: Generated immediately after Spotify OAuth callback completes (step 10 in `PROJECT_OVERVIEW.md` section 2.1).

**Token Structure**:

```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "app_user_id (UUID)",
    "spotify_user_id": "spotify_user_id (String)",
    "email": "user@example.com",
    "display_name": "User Name",
    "iat": 1234567890,
    "exp": 1234571490,
    "jti": "jwt_id (UUID, unique per token)"
  }
}
```

**Token Claims**:

- `sub` (subject): `app_user.id` (UUID) - Primary identifier for PartyWave user
- `spotify_user_id`: Spotify user ID (for reference)
- `email`: User email (from Spotify profile)
- `display_name`: User display name (from Spotify profile)
- `iat` (issued at): Unix timestamp when token was issued
- `exp` (expiration): Unix timestamp when token expires
- `jti` (JWT ID): Unique identifier for this token (UUID)

**Token Expiration**:

- **Access Token**: 15 minutes (900 seconds) - Short-lived for security
- **Refresh Token**: 7 days (604800 seconds) - Long-lived for user convenience

**Token Secret**:

- JWT signing secret must be stored securely (environment variable, secret manager)
- Use strong, randomly generated secret (minimum 256 bits)
- **Never commit secrets to version control**

### 2.2 JWT Token Storage (Client-Side)

**Frontend Storage**:

- Store JWT access token in **memory** (preferred) or **httpOnly cookie** (alternative)
- **Do NOT store in localStorage** (XSS vulnerability)
- Store refresh token in **httpOnly cookie** (more secure than memory)

**Token Transmission**:

- Include JWT token in **Authorization header** for all API requests:
  ```
  Authorization: Bearer <JWT_ACCESS_TOKEN>
  ```

### 2.3 Refresh Token Mechanism

**Purpose**: Allow users to maintain authentication without re-authenticating with Spotify.

**Refresh Token Storage** (PostgreSQL):

Consider adding a `refresh_tokens` table (optional, if token blacklisting is needed):

```sql
CREATE TABLE refresh_tokens (
  id UUID PRIMARY KEY,
  app_user_id UUID NOT NULL REFERENCES app_user(id),
  token_hash VARCHAR(255) NOT NULL UNIQUE,  -- Hashed refresh token
  expires_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  revoked_at TIMESTAMP NULL,
  device_info TEXT,  -- Optional: device/browser info for security
  ip_address VARCHAR(45)  -- Optional: IP address for audit
);
```

**Refresh Flow**:

1. Client detects access token expiration (401 Unauthorized response or client-side expiration check).
2. Client sends refresh request to backend:
   ```
   POST /auth/refresh
   Headers:
     Cookie: refresh_token=<REFRESH_TOKEN>
   ```
3. Backend validates refresh token:
   - Check token signature
   - Check expiration (`exp` claim)
   - Check if token is blacklisted/revoked (if blacklist implemented)
   - Verify user still exists and is not banned (`app_user.status != BANNED`)
4. Backend generates new access token (and optionally new refresh token).
5. Backend returns new tokens to client.
6. Client updates stored tokens.

**Token Rotation** (Recommended):

- On each refresh, generate **new refresh token** and invalidate old one
- Prevents token reuse attacks
- Requires refresh token storage in database

### 2.4 API Request Authentication

**All API endpoints** (except public endpoints like `/auth/spotify/login` and `/auth/spotify/callback`) **require JWT authentication**.

**Request Format**:

```
GET /api/rooms
Headers:
  Authorization: Bearer <JWT_ACCESS_TOKEN>
```

**Backend Validation**:

1. Extract token from `Authorization` header.
2. Verify token signature using JWT secret.
3. Check token expiration (`exp` claim).
4. Check if token is blacklisted (if blacklist implemented).
5. Verify user exists in database (`app_user.id = sub`).
6. Verify user is not banned (`app_user.status != BANNED`).
7. If all checks pass, extract `app_user_id` from `sub` claim and proceed with request.

**Error Responses**:

- `401 Unauthorized`: Missing or invalid token
  ```json
  {
    "error": "UNAUTHORIZED",
    "message": "Invalid or expired token"
  }
  ```
- `403 Forbidden`: Valid token but user is banned
  ```json
  {
    "error": "FORBIDDEN",
    "message": "User account is banned"
  }
  ```

### 2.5 Token Blacklist (Optional but Recommended)

**Purpose**: Allow immediate token invalidation on logout or security incidents.

**Implementation Options**:

1. **Redis-based blacklist** (Recommended for performance):
   - Store blacklisted token IDs (`jti`) in Redis with TTL matching token expiration
   - Key: `partywave:jwt:blacklist:{jti}`
   - Value: `exp` timestamp
   - TTL: Token expiration time

2. **Database-based blacklist**:
   - Store blacklisted tokens in `refresh_tokens` table (set `revoked_at`)
   - Query database on each request (slower but persistent)

**Blacklist Check**:

Before processing authenticated request:
```bash
# Redis check
EXISTS partywave:jwt:blacklist:{jti}
```

**Logout Flow**:

1. Client sends logout request with JWT token.
2. Backend adds token `jti` to blacklist (Redis or database).
3. Backend invalidates refresh token (if stored).
4. Backend returns success response.

### 2.6 Token Security Best Practices

1. **HTTPS Only**: All token transmission must be over HTTPS.
2. **Short Expiration**: Access tokens expire in 15 minutes.
3. **Secure Storage**: Refresh tokens in httpOnly cookies (not localStorage).
4. **Token Rotation**: Rotate refresh tokens on each use.
5. **Blacklist Support**: Implement token blacklist for logout/revocation.
6. **Rate Limiting**: Limit authentication endpoints (login, refresh) to prevent brute force.
7. **Audit Logging**: Log authentication events (login, logout, token refresh, failures).

---

## 3. WebSocket Authentication

### 3.1 WebSocket Connection Authentication

**All WebSocket connections must be authenticated** before allowing access to room events.

**Connection Flow**:

1. Client initiates WebSocket connection:
   ```
   ws://api.partywave.com/ws?token=<JWT_ACCESS_TOKEN>
   ```
   Or via subprotocol:
   ```
   Sec-WebSocket-Protocol: bearer, <JWT_ACCESS_TOKEN>
   ```

2. Backend validates JWT token (same validation as API requests):
   - Verify signature
   - Check expiration
   - Check blacklist (if implemented)
   - Verify user exists and is not banned

3. **If authentication fails**: Close WebSocket connection with code `1008` (Policy Violation).

4. **If authentication succeeds**: 
   - Extract `app_user_id` from token `sub` claim
   - Store user ID in WebSocket session
   - Send `AUTH_SUCCESS` event to client
   - Connection is now authenticated

**Alternative: Token in Initial Message**:

If token cannot be sent in connection URL (e.g., due to proxy limitations), client can send authentication as first message:

```json
{
  "type": "AUTHENTICATE",
  "token": "<JWT_ACCESS_TOKEN>"
}
```

Backend responds:
```json
{
  "type": "AUTH_SUCCESS",
  "user_id": "app_user_id"
}
```

Or on failure:
```json
{
  "type": "AUTH_ERROR",
  "error": "Invalid or expired token"
}
```

### 3.2 Room Subscription & Authorization

After WebSocket authentication, user must **subscribe to a room** to receive events.

**Subscribe Flow**:

1. Client sends subscription message:
   ```json
   {
     "type": "SUBSCRIBE_ROOM",
     "room_id": "room_uuid"
   }
   ```

2. Backend validates:
   - User is authenticated (from WebSocket session)
   - Room exists (check `room` table)
   - User is a member of the room (check `room_member` table):
     ```sql
     SELECT * FROM room_member 
     WHERE room_id = ? AND app_user_id = ?
     ```
   - For private rooms: Verify access (see `PROJECT_OVERVIEW.md` section 2.3)

3. **If validation fails**: Send error event:
   ```json
   {
     "type": "SUBSCRIBE_ERROR",
     "error": "Not a member of this room"
   }
   ```

4. **If validation succeeds**:
   - Add user to Redis online members: `SADD partywave:room:{roomId}:members:online {userId}`
   - Send `SUBSCRIBE_SUCCESS` event
   - Send current room state (playlist, playback state, chat history)
   - User now receives all room events

**Unsubscribe Flow**:

1. Client sends unsubscribe message:
   ```json
   {
     "type": "UNSUBSCRIBE_ROOM",
     "room_id": "room_uuid"
   }
   ```

2. Backend removes user from Redis online members: `SREM partywave:room:{roomId}:members:online {userId}`

3. Backend sends `UNSUBSCRIBE_SUCCESS` event

### 3.3 WebSocket Message Authorization

**All WebSocket messages** (except `AUTHENTICATE`, `SUBSCRIBE_ROOM`, `UNSUBSCRIBE_ROOM`) **require room subscription**.

**Message Validation**:

Before processing any room-related message:

1. Verify user is authenticated (from WebSocket session).
2. Verify user has subscribed to the room (check Redis: `SISMEMBER partywave:room:{roomId}:members:online {userId}`).
3. Verify user is a member of the room (check `room_member` table).
4. If any check fails, send error event and ignore message.

**Example: Chat Message Authorization**:

```json
{
  "type": "SEND_CHAT_MESSAGE",
  "room_id": "room_uuid",
  "content": "Hello!"
}
```

Backend checks:
1. User authenticated? ✅
2. User subscribed to room? ✅
3. User is room member? ✅
4. Process message and broadcast to room ✅

### 3.4 WebSocket Reconnection & Token Refresh

**Reconnection Handling**:

1. Client detects WebSocket disconnection.
2. Client checks if access token is expired.
3. If expired, refresh token first (see section 2.3).
4. Reconnect WebSocket with new token.
5. Re-authenticate and re-subscribe to rooms.

**Token Expiration During Connection**:

- Backend should monitor token expiration for active WebSocket connections.
- When token expires, send `TOKEN_EXPIRED` event:
  ```json
  {
    "type": "TOKEN_EXPIRED",
    "message": "Please refresh your token and reconnect"
  }
  ```
- Close WebSocket connection with code `1008` (Policy Violation).
- Client must refresh token and reconnect.

### 3.5 WebSocket Security Best Practices

1. **Authentication Required**: All connections must authenticate before accessing any features.
2. **Room Authorization**: Verify room membership before allowing subscription.
3. **Message Validation**: Validate all incoming messages (type, format, authorization).
4. **Rate Limiting**: Limit message frequency per connection (prevent spam).
5. **Input Sanitization**: Sanitize all user input (chat messages, etc.).
6. **Connection Limits**: Limit concurrent WebSocket connections per user.
7. **Audit Logging**: Log WebSocket events (connect, disconnect, subscribe, message send).

---

## 4. Integration with Existing Authentication Flow

### 4.1 Updated OAuth Callback Flow

**Step 10 Enhancement** (from `PROJECT_OVERVIEW.md` section 2.1):

After storing Spotify tokens in `user_tokens` table:

10. **Backend generates PartyWave JWT tokens**:
    - Generate JWT access token (15 min expiration) with claims:
      - `sub`: `app_user.id`
      - `spotify_user_id`: `app_user.spotify_user_id`
      - `email`: `app_user.email`
      - `display_name`: `app_user.display_name`
      - `iat`: current timestamp
      - `exp`: current timestamp + 900 seconds
      - `jti`: new UUID
    - Generate JWT refresh token (7 days expiration) with same claims but longer expiration.
    - Store refresh token hash in database (if refresh token storage implemented).

11. **Backend returns tokens to frontend**:
    - **Option A**: Redirect to frontend with tokens in URL fragment (one-time, not stored):
      ```
      https://app.partywave.com/auth/callback#access_token=JWT_TOKEN&refresh_token=REFRESH_TOKEN
      ```
    - **Option B**: Return tokens in HTTP-only cookies (more secure):
      ```
      Set-Cookie: access_token=JWT_TOKEN; HttpOnly; Secure; SameSite=Strict; Max-Age=900
      Set-Cookie: refresh_token=REFRESH_TOKEN; HttpOnly; Secure; SameSite=Strict; Max-Age=604800
      ```
    - **Option C**: Return tokens in JSON response (if using API endpoint instead of redirect):
      ```json
      {
        "user": {
          "id": "app_user_id",
          "display_name": "User Name",
          "email": "user@example.com"
        },
        "access_token": "JWT_ACCESS_TOKEN",
        "refresh_token": "JWT_REFRESH_TOKEN",
        "expires_in": 900
      }
      ```

### 4.2 API Endpoint Protection

**All protected endpoints** must include JWT authentication middleware:

```
GET /api/rooms              → Requires JWT
POST /api/rooms              → Requires JWT
GET /api/rooms/{roomId}      → Requires JWT
POST /api/rooms/{roomId}/join → Requires JWT
POST /api/rooms/{roomId}/chat → Requires JWT
... (all room-related endpoints)
```

**Public endpoints** (no JWT required):

```
GET /auth/spotify/login      → Public (OAuth initiation)
GET /auth/spotify/callback   → Public (OAuth callback)
POST /auth/refresh           → Public (but requires refresh token)
GET /health                  → Public (health check)
```

---

## 5. Error Handling

### 5.1 Authentication Errors

**Invalid Token**:
```json
{
  "error": "UNAUTHORIZED",
  "message": "Invalid or expired token",
  "code": "AUTH_INVALID_TOKEN"
}
```

**Token Expired**:
```json
{
  "error": "UNAUTHORIZED",
  "message": "Token has expired. Please refresh your token.",
  "code": "AUTH_TOKEN_EXPIRED"
}
```

**User Banned**:
```json
{
  "error": "FORBIDDEN",
  "message": "Your account has been banned.",
  "code": "AUTH_USER_BANNED"
}
```

### 5.2 WebSocket Errors

**Authentication Failed**:
```json
{
  "type": "AUTH_ERROR",
  "error": "Invalid or expired token",
  "code": "WS_AUTH_FAILED"
}
```

**Room Subscription Failed**:
```json
{
  "type": "SUBSCRIBE_ERROR",
  "error": "Not a member of this room",
  "code": "WS_NOT_MEMBER"
}
```

**Message Authorization Failed**:
```json
{
  "type": "MESSAGE_ERROR",
  "error": "Not authorized to perform this action",
  "code": "WS_UNAUTHORIZED"
}
```

---

## 6. Implementation Notes for AI Agents

When implementing authentication:

1. **JWT Library**: Use a well-tested JWT library (e.g., `jsonwebtoken` for Node.js, `PyJWT` for Python, `java-jwt` for Java).

2. **Secret Management**: Store JWT secret in environment variables or secret manager. Never hardcode.

3. **Token Validation**: Always validate token signature, expiration, and user status before processing requests.

4. **Refresh Token Storage**: Consider storing refresh tokens in database for revocation support.

5. **WebSocket Authentication**: Authenticate WebSocket connections immediately on connect, before allowing any room subscriptions.

6. **Room Authorization**: Always verify room membership (PostgreSQL) and subscription (Redis) before processing room-related messages.

7. **Error Handling**: Return clear, consistent error messages for authentication/authorization failures.

8. **Security Headers**: Set appropriate security headers (CORS, CSP, etc.) for API endpoints.

9. **Rate Limiting**: Implement rate limiting on authentication endpoints to prevent brute force attacks.

10. **Audit Logging**: Log all authentication events (successful logins, failed attempts, token refreshes, WebSocket connections).

---

## 7. Summary

**JWT Authentication**:
- Generated after Spotify OAuth2 success
- Required for all API requests (except public endpoints)
- Short expiration (15 minutes) for access tokens
- Refresh token mechanism for extended sessions
- Optional token blacklist for logout/revocation

**WebSocket Authentication**:
- JWT token required for connection
- Room subscription requires membership verification
- All messages require room subscription
- Token expiration handling and reconnection support

**Security Principles**:
- HTTPS only
- Secure token storage (httpOnly cookies preferred)
- Token rotation on refresh
- Rate limiting
- Audit logging

For Spotify OAuth2 flow details, see `SPOTIFY_AUTH_ENDPOINTS.md`.
For general project overview, see `PROJECT_OVERVIEW.md`.

