# User Registration & Token Storage Implementation Summary

**Date:** 2025-11-23  
**Status:** ✅ Completed  
**Reference:** PROJECT_OVERVIEW.md Section 2.1

## Overview

This document summarizes the implementation of user registration and token storage logic for the PartyWave application, following the specifications in PROJECT_OVERVIEW.md section 2.1.

## Implementation Details

### 1. Token Encryption Service ✅

**File:** `src/main/java/com/partywave/backend/security/TokenEncryptionService.java`

Created a new service to handle encryption and decryption of Spotify OAuth tokens:

- **Algorithm:** AES-256-GCM (Galois/Counter Mode)
- **Key Derivation:** SHA-256 hash of JWT secret
- **IV Generation:** 12-byte random Initialization Vector per encryption
- **Tag Length:** 128 bits for authentication
- **Methods:**
  - `encrypt(String plainToken)` - Encrypts tokens using AES-GCM
  - `decrypt(String encryptedToken)` - Decrypts tokens

**Security Features:**

- Uses authenticated encryption (GCM mode) to prevent tampering
- Random IV for each encryption operation
- Base64 encoding for storage compatibility
- Reuses existing JWT secret from configuration

### 2. AppUserService Enhancements ✅

**File:** `src/main/java/com/partywave/backend/service/AppUserService.java`

#### 2.1 Updated Dependencies

Added new repository and service dependencies:

- `AppUserStatsRepository` - For user statistics
- `AppUserImageRepository` - For user profile images
- `TokenEncryptionService` - For token encryption

#### 2.2 New Method: `createAppUserStats(AppUser appUser)`

Creates and initializes statistics for new users:

```java
AppUserStats stats = new AppUserStats();
stats.setTotalLike(0);
stats.setTotalDislike(0);
stats.setAppUser(appUser);
```

**Database Impact:** INSERT into `app_user_stats` table

#### 2.3 New Method: `createAppUserImages(AppUser appUser, JsonNode spotifyProfile)`

Extracts and stores profile images from Spotify profile:

- Parses `images` array from Spotify profile JSON
- Creates `AppUserImage` entities with URL, height, and width
- Links images to the user

**Database Impact:** INSERT into `app_user_image` table (one row per image)

#### 2.4 Updated Method: `updateUserToken(...)`

Enhanced to support encryption and additional fields:

- **Encryption:** Encrypts both access token and refresh token using `TokenEncryptionService`
- **token_type:** Now set to "Bearer"
- **scope:** Stores OAuth scopes from Spotify response (or defaults to "user-read-email user-read-private")
- **Error Handling:** Throws RuntimeException if encryption fails

**Signature Change:**

```java
// Before
private void updateUserToken(AppUser appUser, String spotifyAccessToken,
    String spotifyRefreshToken, int expiresIn)

// After
private void updateUserToken(AppUser appUser, String spotifyAccessToken,
    String spotifyRefreshToken, int expiresIn, String scope)
```

#### 2.5 Updated Method: `createOrUpdateFromSpotifyProfile(...)`

Enhanced user registration flow:

**For New Users:**

1. Create `AppUser` entity with profile data
2. Save user to database (to generate ID)
3. **NEW:** Call `createAppUserStats()` to initialize statistics
4. **NEW:** Call `createAppUserImages()` to store profile images
5. Call `updateUserToken()` to store encrypted Spotify tokens

**For Existing Users:**

1. Update profile data from Spotify
2. Update Spotify tokens (encrypted)

**Signature Change:**

```java
// Before
public AppUser createOrUpdateFromSpotifyProfile(JsonNode spotifyProfile,
    String spotifyAccessToken, String spotifyRefreshToken,
    int spotifyTokenExpiresIn, String ipAddress)

// After
public AppUser createOrUpdateFromSpotifyProfile(JsonNode spotifyProfile,
    String spotifyAccessToken, String spotifyRefreshToken,
    int spotifyTokenExpiresIn, String scope, String ipAddress)
```

### 3. AuthController Updates ✅

**File:** `src/main/java/com/partywave/backend/web/rest/AuthController.java`

**Method:** `spotifyCallback()`

Updated to extract and pass the OAuth scope from Spotify token response:

```java
// Extract scope from Spotify token response
String scope = tokenResponse.has("scope") ? tokenResponse.get("scope").asText() : null;

// Pass scope to service
AppUser appUser = appUserService.createOrUpdateFromSpotifyProfile(
  userProfile,
  spotifyAccessToken,
  spotifyRefreshToken,
  spotifyExpiresIn,
  scope,
  ipAddress
);

```

## Database Impact Summary

### For New Users (First Login)

1. **app_user** table: INSERT

   - All profile fields from Spotify (spotify_user_id, display_name, email, etc.)
   - status = ONLINE
   - last_active_at = current timestamp

2. **app_user_stats** table: INSERT

   - total_like = 0
   - total_dislike = 0
   - app_user_id = user's ID

3. **app_user_image** table: INSERT (one or more rows)

   - url, height, width from Spotify profile images
   - app_user_id = user's ID

4. **user_tokens** table: INSERT
   - access_token = **encrypted** Spotify access token
   - refresh_token = **encrypted** Spotify refresh token
   - token_type = "Bearer"
   - expires_at = calculated expiration timestamp
   - scope = OAuth scopes from Spotify
   - app_user_id = user's ID

### For Existing Users (Subsequent Logins)

1. **app_user** table: UPDATE

   - Profile fields (if changed)
   - last_active_at = current timestamp

2. **user_tokens** table: UPDATE
   - access_token = **encrypted** new Spotify access token
   - refresh_token = **encrypted** new Spotify refresh token (if provided)
   - expires_at = new expiration timestamp
   - scope = updated OAuth scopes

## Security Enhancements

### Token Encryption at Rest

**Before:** Spotify tokens stored in plain text ❌  
**After:** Spotify tokens encrypted with AES-256-GCM ✅

**Encryption Details:**

- Algorithm: AES-256-GCM (authenticated encryption)
- Key: Derived from JWT secret using SHA-256
- IV: Random 12-byte vector per encryption
- Format: Base64(IV + Encrypted Data + Authentication Tag)

**Benefits:**

- Protects tokens if database is compromised
- Prevents token tampering (GCM authentication)
- Reuses existing JWT secret infrastructure
- Transparent to application logic (encrypt on save, decrypt on read)

## Compliance with PROJECT_OVERVIEW.md Section 2.1

| Requirement                               | Status | Implementation                                            |
| ----------------------------------------- | ------ | --------------------------------------------------------- |
| Check if user exists by `spotify_user_id` | ✅     | `appUserRepository.findBySpotifyUserId()`                 |
| Create `app_user` for new users           | ✅     | `AppUser` entity saved with all profile fields            |
| Create `app_user_images`                  | ✅     | `createAppUserImages()` method                            |
| Initialize `app_user_stats`               | ✅     | `createAppUserStats()` with total_like=0, total_dislike=0 |
| Set status = ONLINE                       | ✅     | `appUser.setStatus(AppUserStatus.ONLINE)`                 |
| Store Spotify tokens in `user_tokens`     | ✅     | `updateUserToken()` method                                |
| Encrypt access token                      | ✅     | `tokenEncryptionService.encrypt()`                        |
| Encrypt refresh token                     | ✅     | `tokenEncryptionService.encrypt()`                        |
| Set token_type = "Bearer"                 | ✅     | `userToken.setTokenType("Bearer")`                        |
| Store expires_at                          | ✅     | Calculated from `expires_in`                              |
| Store scope                               | ✅     | Extracted from Spotify response                           |
| Generate PartyWave JWT tokens             | ✅     | `jwtAuthenticationService.generateTokens()`               |
| Return JWT tokens in response             | ✅     | `JwtTokenResponseDTO`                                     |

## Testing Recommendations

### Unit Tests

1. Test `TokenEncryptionService.encrypt()` and `decrypt()`
2. Test `createAppUserStats()` initializes with zeros
3. Test `createAppUserImages()` handles missing images
4. Test `updateUserToken()` encrypts tokens correctly

### Integration Tests

1. Test full Spotify callback flow for new users
2. Test existing user login updates tokens
3. Test encrypted tokens can be decrypted and used
4. Verify all database tables are populated correctly

### Security Tests

1. Verify tokens are encrypted in database (not plain text)
2. Test encryption/decryption with different token lengths
3. Test error handling for encryption failures
4. Verify IV randomness (no repeated IVs)

## Files Modified

1. ✅ `src/main/java/com/partywave/backend/security/TokenEncryptionService.java` (NEW)
2. ✅ `src/main/java/com/partywave/backend/service/AppUserService.java` (MODIFIED)
3. ✅ `src/main/java/com/partywave/backend/web/rest/AuthController.java` (MODIFIED)

## Build Status

✅ **Maven Compilation:** SUCCESS  
✅ **No Compilation Errors**  
⚠️ **Minor Warnings:** Null-safety warnings (non-critical)

## Next Steps

1. **Token Decryption Service:** Create a service method to decrypt tokens when needed for Spotify API calls
2. **Token Refresh Logic:** Update token refresh endpoint to handle encrypted tokens
3. **Database Migration:** If upgrading existing system, migrate existing plain-text tokens to encrypted format
4. **Monitoring:** Add logging/metrics for encryption failures
5. **Testing:** Write comprehensive unit and integration tests

## Notes

- The implementation follows Spring Security best practices
- Token encryption uses industry-standard AES-GCM algorithm
- All database operations are wrapped in `@Transactional` for atomicity
- Error handling includes proper logging and exception propagation
- Code includes comprehensive JavaDoc comments

## References

- PROJECT_OVERVIEW.md Section 2.1: User Authentication & Registration
- AUTHENTICATION.md: JWT-based Application Authentication
- POSTGRES_SCHEMA.md: Database Schema Documentation
