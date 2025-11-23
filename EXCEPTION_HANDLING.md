# Exception Handling in PartyWave Backend

## Overview

PartyWave backend implements a comprehensive exception handling system that provides meaningful error responses to clients. The system follows RFC7807 Problem Details for HTTP APIs standard and uses Spring's `@ControllerAdvice` for global exception handling.

## Architecture

### 1. Custom Exception Package (`com.partywave.backend.exception`)

All custom business exceptions are located in the `exception` package. Each exception is annotated with `@ResponseStatus` to specify the appropriate HTTP status code.

#### Available Exceptions

| Exception                   | HTTP Status               | Use Case                                                         |
| --------------------------- | ------------------------- | ---------------------------------------------------------------- |
| `ResourceNotFoundException` | 404 NOT_FOUND             | When a requested resource (user, room, etc.) is not found        |
| `RoomFullException`         | 400 BAD_REQUEST           | When attempting to join a room that has reached maximum capacity |
| `AlreadyMemberException`    | 400 BAD_REQUEST           | When a user tries to join a room they're already a member of     |
| `RoomNotPublicException`    | 403 FORBIDDEN             | When attempting to join a private room without authorization     |
| `InvalidRequestException`   | 400 BAD_REQUEST           | When request parameters violate business rules                   |
| `SpotifyApiException`       | 502 BAD_GATEWAY           | When Spotify API operations fail                                 |
| `TokenEncryptionException`  | 500 INTERNAL_SERVER_ERROR | When token encryption/decryption fails                           |

### 2. Global Exception Handler (`ExceptionTranslator`)

The `ExceptionTranslator` class (in `web.rest.errors` package) is a `@ControllerAdvice` that intercepts all exceptions thrown by controllers and services. It:

- Catches custom exceptions and maps them to appropriate HTTP status codes
- Generates RFC7807-compliant error responses with detailed information
- Handles Spring framework exceptions (validation, authentication, etc.)
- Provides different error details based on environment (dev vs production)

### 3. Service Layer Exception Handling

Services throw custom exceptions instead of generic ones:

```java
// ❌ BAD - Generic exception
throw new RuntimeException("Room not found");

// ✅ GOOD - Custom exception
throw new ResourceNotFoundException("Room", "id", roomId);
```

### 4. Controller Layer

Controllers let exceptions bubble up to the global exception handler instead of catching them:

```java
// ❌ BAD - Catching and returning generic error
try {
    service.doSomething();
    return ResponseEntity.ok(result);
} catch (Exception e) {
    log.error("Error", e);
    return ResponseEntity.status(500).build();
}

// ✅ GOOD - Let global handler handle it
public ResponseEntity<DTO> endpoint() {
    DTO result = service.doSomething(); // Exceptions handled globally
    return ResponseEntity.ok(result);
}
```

## Error Response Format

All error responses follow RFC7807 Problem Details format:

```json
{
  "type": "about:blank",
  "title": "Room Full",
  "status": 400,
  "detail": "Room is full. Current members: 10, Maximum capacity: 10",
  "instance": "/api/rooms/123e4567-e89b-12d3-a456-426614174000/join",
  "message": "error.http.400",
  "path": "/api/rooms/123e4567-e89b-12d3-a456-426614174000/join"
}
```

## Implementation Examples

### Example 1: RoomService - Join Room

**Before:**

```java
if (isAlreadyMember) {
    throw new IllegalArgumentException("User is already a member of this room");
}
```

**After:**

```java
if (isAlreadyMember) {
    log.error("Illegal argument: [{}, {}] in joinRoom()", roomId, userId);
    throw new AlreadyMemberException(userId, roomId);
}
```

**Client receives:**

```json
{
  "type": "about:blank",
  "title": "Already a Member",
  "status": 400,
  "detail": "User is already a member of this room",
  "path": "/api/rooms/123/join"
}
```

### Example 2: SpotifyAuthService - Token Exchange

**Before:**

```java
catch (Exception e) {
    LOG.error("Error exchanging code for tokens: {}", e.getMessage(), e);
    throw new Exception("Error during token exchange: " + e.getMessage(), e);
}
```

**After:**

```java
catch (Exception e) {
    LOG.error("Error exchanging code for tokens: {}", e.getMessage(), e);
    throw new SpotifyApiException("Error during token exchange: " + e.getMessage(), "token_exchange", e);
}
```

**Client receives:**

```json
{
  "type": "about:blank",
  "title": "Spotify API Error",
  "status": 502,
  "detail": "Error during token exchange: Connection timeout",
  "path": "/api/auth/spotify/callback"
}
```

## Benefits

1. **Consistent Error Format**: All errors follow the same RFC7807 structure
2. **Meaningful Error Messages**: Clients receive detailed, actionable error information
3. **Proper HTTP Status Codes**: Each error type maps to the appropriate HTTP status
4. **Centralized Error Handling**: All exception handling logic is in one place
5. **Better Logging**: Services log errors with context before throwing custom exceptions
6. **Type Safety**: Custom exceptions carry typed information (e.g., `roomId`, `userId`)
7. **Easier Debugging**: Stack traces and error details are preserved in logs

## Testing Exception Handling

### Test Room Full Exception

```bash
# 1. Create a room with max_participants=1
curl -X POST http://localhost:8080/api/rooms \
  -H "Authorization: Bearer YOUR_JWT" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","max_participants":1,"is_public":true}'

# 2. Join with first user (should succeed)
curl -X POST http://localhost:8080/api/rooms/ROOM_ID/join \
  -H "Authorization: Bearer USER1_JWT"

# 3. Try to join with second user (should fail with RoomFullException)
curl -X POST http://localhost:8080/api/rooms/ROOM_ID/join \
  -H "Authorization: Bearer USER2_JWT"

# Expected response: 400 Bad Request
# {
#   "title": "Room Full",
#   "status": 400,
#   "detail": "Room is full. Current members: 1, Maximum capacity: 1"
# }
```

### Test Already Member Exception

```bash
# Try to join the same room twice with the same user
curl -X POST http://localhost:8080/api/rooms/ROOM_ID/join \
  -H "Authorization: Bearer USER_JWT"

# Expected response: 400 Bad Request
# {
#   "title": "Already a Member",
#   "status": 400,
#   "detail": "User is already a member of this room"
# }
```

## Migration Guide for Existing Code

To add exception handling to a new service or controller:

1. **Identify error conditions** in your service methods
2. **Choose or create appropriate custom exception** from `exception` package
3. **Throw the custom exception** with detailed context
4. **Add logging** before throwing the exception
5. **Remove try-catch blocks** from controllers (let global handler catch it)
6. **Update ExceptionTranslator** if you created a new exception type:
   - Add to `getCustomizedTitle()`
   - Add to `getCustomizedErrorDetails()`
   - Add to `getMappedStatus()`

## Best Practices

1. ✅ **Always log before throwing custom exceptions**
2. ✅ **Include relevant context** (IDs, values) in exception constructors
3. ✅ **Use specific exceptions** instead of generic ones
4. ✅ **Let exceptions bubble up** from controllers to global handler
5. ✅ **Document exception types** in controller method JavaDocs
6. ❌ **Don't catch and return generic error responses** in controllers
7. ❌ **Don't throw generic `RuntimeException` or `Exception`**
8. ❌ **Don't expose sensitive information** in error messages

## Production Considerations

The `ExceptionTranslator` automatically sanitizes error messages in production:

- Package names in stack traces are hidden
- Detailed technical errors are replaced with generic messages
- Original errors are still logged for debugging

## Future Enhancements

Potential improvements to the exception handling system:

1. Add more specific exceptions (e.g., `PlaylistNotFoundException`, `InvalidSpotifyTokenException`)
2. Implement i18n for error messages
3. Add error codes for client-side error handling
4. Create exception metrics and monitoring
5. Add retry logic for transient errors (network timeouts, etc.)

## Related Files

- `src/main/java/com/partywave/backend/exception/` - Custom exceptions
- `src/main/java/com/partywave/backend/web/rest/errors/ExceptionTranslator.java` - Global handler
- `src/main/java/com/partywave/backend/service/RoomService.java` - Example service with exceptions
- `src/main/java/com/partywave/backend/web/rest/RoomController.java` - Example controller
