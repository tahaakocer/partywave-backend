# PartyWave – Authentication & Security

This document provides detailed specifications for **JWT-based application authentication** and **WebSocket authentication** in PartyWave. It is written for AI agents and backend developers implementing authentication and authorization mechanisms.

---

## 1. Authentication Architecture Overview

PartyWave uses a **three-phase authentication system**:

1. **Spotify OAuth2 with PKCE**: External authentication provider using PKCE (Proof Key for Code Exchange) for enhanced security (see `SPOTIFY_AUTH_ENDPOINTS.md`)
2. **Temporary Authorization Code Exchange**: PKCE-based token exchange phase
3. **JWT-based Application Authentication**: Internal authentication for PartyWave API and WebSocket connections

**Key Principle**: Spotify OAuth2 authenticates users with Spotify using PKCE, then backend generates a temporary authorization code. Frontend exchanges this code for JWT tokens using PKCE code verifier. **All PartyWave API requests and WebSocket connections must be authenticated using application-level JWT tokens**.

**PKCE Benefits**:

- Prevents authorization code interception attacks
- No client secret required in frontend (public client security)
- Enhanced security for OAuth flows

---

## 2. JWT-Based Application Authentication

### 2.1 JWT Token Generation

After successful PKCE token exchange, the backend generates **PartyWave JWT tokens** for the user.

**When**: Generated during token exchange phase (step 15 in `PROJECT_OVERVIEW.md` section 2.1) after PKCE validation succeeds.

**PKCE Flow**:

1. Frontend initiates login with `code_challenge` parameter
2. Backend stores challenge in Redis, redirects to Spotify
3. Spotify callback returns authorization code
4. Backend creates temporary authorization code, redirects to frontend
5. Frontend exchanges authorization code + code verifier for JWT tokens
6. Backend validates PKCE and generates JWT tokens

**Legacy Flow** (backward compatibility, PKCE not used):

- JWT tokens generated immediately after Spotify OAuth callback (step 12 in legacy flow)

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

### 4.1 PKCE OAuth Flow

**PKCE Flow** (from `PROJECT_OVERVIEW.md` section 2.1):

#### Phase 1: Authorization Request

1. **Frontend generates PKCE parameters**:

   - Generate random `code_verifier` (43-128 characters, URL-safe)
   - Compute `code_challenge` = Base64URL(SHA256(code_verifier))
   - Store `code_verifier` securely in memory

2. **Frontend initiates login**:

   ```
   GET /api/auth/spotify/login?code_challenge={CODE_CHALLENGE}&code_challenge_method=S256
   ```

3. **Backend processes login**:
   - Validates `code_challenge_method` (must be "S256")
   - Generates random `state` for CSRF protection
   - Stores `code_challenge` in Redis: `partywave:pkce:state:{state}` (TTL: 10 minutes)
   - Redirects to Spotify authorization URL

#### Phase 2: OAuth Callback & Authorization Code Generation

4. **Spotify callback**:

   ```
   GET /api/auth/spotify/callback?code={SPOTIFY_CODE}&state={STATE}
   ```

5. **Backend processes callback**:

   - Validates `state` parameter
   - Retrieves `code_challenge` from Redis
   - Exchanges code for Spotify tokens
   - Fetches user profile
   - Creates/updates user in database
   - Stores Spotify tokens in `user_tokens` table

6. **Backend generates temporary authorization code**:

   - Generates cryptographically secure authorization code (32 bytes, Base64URL)
   - Stores in Redis: `partywave:pkce:authcode:{authCode}` with:
     - `userId`: User UUID
     - `codeChallenge`: Original code challenge
   - Sets TTL: 5 minutes
   - Cleans up code challenge from Redis

7. **Backend redirects to frontend**:
   ```
   {frontend-url}/pkce-test.html#authorization_code={AUTH_CODE}&expires_in=300
   ```

#### Phase 3: Token Exchange

8. **Frontend exchanges authorization code for JWT tokens**:

   ```
   POST /api/auth/token
   Content-Type: application/json

   {
     "authorization_code": "{AUTH_CODE}",
     "code_verifier": "{CODE_VERIFIER}"
   }
   ```

9. **Backend validates PKCE and generates JWT tokens**:

   - Retrieves authorization code data from Redis
   - Validates PKCE: Computes SHA256(code_verifier) and compares with stored code_challenge
   - If validation fails: Deletes authorization code, returns error
   - If validation succeeds:
     - Loads user from database
     - Generates JWT access token (15 min expiration) with claims:
       - `sub`: `app_user.id`
       - `spotify_user_id`: `app_user.spotify_user_id`
       - `email`: `app_user.email`
       - `display_name`: `app_user.display_name`
       - `iat`: current timestamp
       - `exp`: current timestamp + 900 seconds
       - `jti`: new UUID
     - Generates JWT refresh token (7 days expiration) with same claims but longer expiration
     - Stores refresh token hash in database (if refresh token storage implemented)
     - Deletes authorization code from Redis (single-use)

10. **Backend returns JWT tokens**:
    ```json
    {
      "user": {
        "id": "app_user_id",
        "display_name": "User Name",
        "email": "user@example.com"
      },
      "access_token": "JWT_ACCESS_TOKEN",
      "refresh_token": "JWT_REFRESH_TOKEN",
      "token_type": "Bearer",
      "expires_in": 900
    }
    ```

**Legacy Flow** (backward compatibility, PKCE not used):

If `code_challenge` is not provided in login request:

- Steps 1-5 remain the same (without PKCE storage)
- Step 6: Backend generates JWT tokens immediately (no authorization code)
- Step 7: Backend redirects to frontend with JWT tokens in hash fragment:
  ```
  {frontend-url}/pkce-test.html#access_token={JWT_ACCESS_TOKEN}&refresh_token={JWT_REFRESH_TOKEN}&token_type=Bearer&expires_in=900&user_id={USER_ID}
  ```
- Steps 8-10 are skipped

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
GET /api/auth/spotify/login      → Public (OAuth initiation with PKCE)
GET /api/auth/spotify/callback  → Public (OAuth callback)
POST /api/auth/token             → Public (PKCE token exchange, requires authorization code + code verifier)
POST /api/auth/refresh           → Public (but requires refresh token)
POST /api/auth/logout            → Public (but requires refresh token)
GET /health                      → Public (health check)
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

## 6. PKCE Implementation Details

### 6.1 PKCE Code Challenge Generation (Frontend)

**Code Verifier**:

- Length: 43-128 characters
- Character set: URL-safe (A-Z, a-z, 0-9, -, ., \_, ~)
- Generation: Cryptographically secure random (e.g., `crypto.getRandomValues()`)

**Code Challenge**:

- Method: S256 (SHA-256)
- Computation: `Base64URL(SHA256(code_verifier))`
- Example:
  ```javascript
  const codeVerifier = generateRandomString(128);
  const codeChallenge = base64URL(sha256(codeVerifier));
  ```

### 6.2 PKCE Storage (Backend)

**Redis Key Structures**:

- Code challenge: `partywave:pkce:state:{state}` → `code_challenge` (TTL: 10 minutes)
- Authorization code: `partywave:pkce:authcode:{authCode}` → JSON `{userId, codeChallenge}` (TTL: 5 minutes)

**TTL Management**:

- Code challenges expire after 10 minutes (covers OAuth flow duration)
- Authorization codes expire after 5 minutes (short-lived for security)
- Failed validations delete codes immediately (prevents replay attacks)

### 6.3 PKCE Validation

**Validation Process**:

1. Retrieve authorization code data from Redis
2. Extract stored `code_challenge`
3. Compute challenge from verifier: `Base64URL(SHA256(code_verifier))`
4. Compare computed challenge with stored challenge
5. If match: Proceed with token generation
6. If mismatch: Delete authorization code, return error

**Security Considerations**:

- Authorization codes are single-use (deleted after successful exchange)
- Code verifier is never stored (only challenge is stored)
- TTL prevents long-lived codes from being exploited
- Failed validations prevent brute force attacks

## 7. Implementation Notes for AI Agents

When implementing authentication:

1. **PKCE Implementation**: Always use PKCE for OAuth flows. Generate code verifier/challenge on frontend, store challenge in Redis, validate during token exchange.

2. **JWT Library**: Use a well-tested JWT library (e.g., `jsonwebtoken` for Node.js, `PyJWT` for Python, `java-jwt` for Java).

3. **Secret Management**: Store JWT secret in environment variables or secret manager. Never hardcode.

4. **Token Validation**: Always validate token signature, expiration, and user status before processing requests.

5. **PKCE Validation**: Always validate code verifier against stored code challenge. Delete authorization codes after successful exchange or failed validation.

6. **Redis TTL**: Set appropriate TTLs for PKCE data (10 min for challenges, 5 min for authorization codes). Clean up expired data.

7. **Refresh Token Storage**: Consider storing refresh tokens in database for revocation support.

8. **WebSocket Authentication**: Authenticate WebSocket connections immediately on connect, before allowing any room subscriptions.

9. **Room Authorization**: Always verify room membership (PostgreSQL) and subscription (Redis) before processing room-related messages.

10. **Error Handling**: Return clear, consistent error messages for authentication/authorization failures.

11. **Security Headers**: Set appropriate security headers (CORS, CSP, etc.) for API endpoints.

12. **Rate Limiting**: Implement rate limiting on authentication endpoints (login, token exchange, refresh) to prevent brute force attacks.

13. **Audit Logging**: Log all authentication events (successful logins, failed attempts, PKCE validations, token refreshes, WebSocket connections).

14. **Code Verifier Storage**: Frontend must store code verifier securely (in memory, not localStorage). Code verifier is only sent during token exchange.

---

## 8. Summary

**PKCE OAuth Flow**:

- Frontend generates code verifier/challenge
- Backend stores challenge in Redis (TTL: 10 minutes)
- Backend creates temporary authorization code after Spotify OAuth (TTL: 5 minutes)
- Frontend exchanges authorization code + code verifier for JWT tokens
- PKCE validation prevents authorization code interception attacks

**JWT Authentication**:

- Generated after successful PKCE token exchange
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
- PKCE for OAuth security (no client secret in frontend)
- Secure token storage (httpOnly cookies preferred for refresh tokens, memory for access tokens)
- Token rotation on refresh
- Single-use authorization codes
- Rate limiting
- Audit logging

**Redis PKCE Keys**:

- `partywave:pkce:state:{state}` → code challenge (TTL: 10 min)
- `partywave:pkce:authcode:{authCode}` → {userId, codeChallenge} (TTL: 5 min)

For Spotify OAuth2 flow details, see `SPOTIFY_AUTH_ENDPOINTS.md`.
For general project overview, see `PROJECT_OVERVIEW.md`.
