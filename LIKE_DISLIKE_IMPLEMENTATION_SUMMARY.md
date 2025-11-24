# Like/Dislike System Implementation Summary

This document describes the implementation of the Like/Dislike system for PartyWave playlist items, including race condition handling with the compensation pattern.

**Implementation Date:** November 24, 2025

**Based on:**

- `PROJECT_OVERVIEW.md` section 2.10 - Like / Dislike Tracks
- `REDIS_ARCHITECTURE.md` section 2.2 - Race Condition & Atomicity Problem

---

## Overview

The Like/Dislike system allows room members to like or dislike tracks in a room's playlist. Likes and dislikes affect the track adder's user statistics (`AppUserStats.total_like` and `AppUserStats.total_dislike`).

**Key Features:**

- Users can like or dislike any playlist item in rooms they are members of
- Like and dislike are mutually exclusive (switching from like to dislike updates both)
- Statistics are stored in both Redis (runtime) and PostgreSQL (persistent)
- Race condition handling with compensation pattern ensures data consistency
- WebSocket events notify all room members when stats change (TODO: WebSocket implementation)

---

## Architecture

### Dual-Storage System

The system uses both Redis and PostgreSQL:

1. **Redis (Runtime State):**

   - Key: `partywave:room:{roomId}:playlist:item:{playlistItemId}:likes` (SET)
   - Key: `partywave:room:{roomId}:playlist:item:{playlistItemId}:dislikes` (SET)
   - Contains user IDs who liked/disliked the item
   - Cleaned up when rooms close

2. **PostgreSQL (Persistent State):**
   - Table: `app_user_stats`
   - Fields: `total_like`, `total_dislike`
   - Aggregated statistics per user (track adder)
   - Persists even after rooms close

### Race Condition Handling - Compensation Pattern

The system implements the compensation pattern to handle race conditions between Redis and PostgreSQL:

**Problem:**

- Redis and PostgreSQL updates are not atomic
- If Redis succeeds but PostgreSQL fails (or vice versa), data becomes inconsistent

**Solution (Compensation Pattern):**

```
1. Update PostgreSQL first (transactional)
2. If PostgreSQL fails → Return error (no Redis update)
3. If PostgreSQL succeeds → Update Redis atomically
4. If Redis fails → Compensate by reverting PostgreSQL update
5. Log all failures for monitoring
```

**Implementation Details:**

```java
try {
    // Step 1: Update PostgreSQL (transactional)
    updateStatsForLike(addedById, previouslyDisliked);
} catch (Exception e) {
    // PostgreSQL failed - return error
    throw new InvalidRequestException("Failed to update user statistics");
}

// Step 2: Update Redis
boolean redisSuccess = likeDislikeRedisService.addLike(roomIdStr, playlistItemIdStr, userIdStr);

if (!redisSuccess) {
    // Step 3: Compensate - revert PostgreSQL update
    try {
        compensateStatsForLike(addedById, previouslyDisliked);
        log.info("Successfully compensated PostgreSQL update");
    } catch (Exception compensationError) {
        log.error("CRITICAL: Failed to compensate - data inconsistent");
    }

    throw new InvalidRequestException("Failed to update like in Redis. Operation rolled back.");
}
```

**Benefits:**

- PostgreSQL (persistent state) remains consistent
- Failures are logged for monitoring
- Compensation ensures atomicity at application level
- Users receive clear error messages

---

## Components

### 1. DTOs

#### `LikeDislikeResponseDTO`

Response DTO for all like/dislike operations.

**Location:** `src/main/java/com/partywave/backend/service/dto/LikeDislikeResponseDTO.java`

**Fields:**

```json
{
  "playlist_item_id": "UUID",
  "room_id": "UUID",
  "like_count": 42,
  "dislike_count": 5,
  "user_liked": true,
  "user_disliked": false,
  "message": "Track liked successfully"
}
```

#### `PlaylistItemStatsEventDTO`

WebSocket event DTO for statistics updates.

**Location:** `src/main/java/com/partywave/backend/service/dto/PlaylistItemStatsEventDTO.java`

**Fields:**

```json
{
  "type": "PLAYLIST_ITEM_STATS_UPDATED",
  "room_id": "UUID",
  "playlist_item_id": "UUID",
  "like_count": 42,
  "dislike_count": 5,
  "timestamp_ms": 1700000000000
}
```

**TODO:** WebSocket implementation to emit these events.

### 2. Service Layer

#### `LikeDislikeService`

Main business logic for like/dislike operations.

**Location:** `src/main/java/com/partywave/backend/service/LikeDislikeService.java`

**Dependencies:**

- `PlaylistRedisService` - Access playlist item data
- `LikeDislikeRedisService` - Redis like/dislike operations
- `AppUserStatsRepository` - PostgreSQL statistics updates
- `AppUserRepository` - Fetch track adder user

**Public Methods:**

1. **`likeTrack(UUID roomId, UUID playlistItemId, UUID userId)`**

   - Add like to playlist item
   - Remove dislike if user previously disliked
   - Update PostgreSQL and Redis with compensation
   - Return updated counts

2. **`dislikeTrack(UUID roomId, UUID playlistItemId, UUID userId)`**

   - Add dislike to playlist item
   - Remove like if user previously liked
   - Update PostgreSQL and Redis with compensation
   - Return updated counts

3. **`unlikeTrack(UUID roomId, UUID playlistItemId, UUID userId)`**

   - Remove like from playlist item
   - Update PostgreSQL and Redis with compensation
   - Return updated counts

4. **`undislikeTrack(UUID roomId, UUID playlistItemId, UUID userId)`**
   - Remove dislike from playlist item
   - Update PostgreSQL and Redis with compensation
   - Return updated counts

**Private Helper Methods:**

- `updateStatsForLike()` - Increment total_like, decrement total_dislike if needed
- `compensateStatsForLike()` - Revert like statistics update
- `updateStatsForDislike()` - Increment total_dislike, decrement total_like if needed
- `compensateStatsForDislike()` - Revert dislike statistics update
- `buildResponse()` - Build response DTO with current counts
- `emitStatsUpdatedEvent()` - Emit WebSocket event (TODO)

### 3. Controller Layer

#### `LikeDislikeController`

REST API endpoints for like/dislike operations.

**Location:** `src/main/java/com/partywave/backend/web/rest/LikeDislikeController.java`

**Base Path:** `/api/rooms/{roomId}/playlist/{playlistItemId}`

**Authentication:** All endpoints require JWT authentication and active room membership.

**Endpoints:**

1. **`POST /like`** - Like a track
2. **`POST /dislike`** - Dislike a track
3. **`DELETE /like`** - Remove like
4. **`DELETE /dislike`** - Remove dislike

---

## API Endpoints

### 1. Like Track

**Endpoint:** `POST /api/rooms/{roomId}/playlist/{playlistItemId}/like`

**Authentication:** Required (JWT)

**Path Parameters:**

- `roomId` (UUID) - Room ID
- `playlistItemId` (UUID) - Playlist item ID

**Request Body:** None

**Response:** `200 OK`

```json
{
  "playlist_item_id": "123e4567-e89b-12d3-a456-426614174000",
  "room_id": "123e4567-e89b-12d3-a456-426614174001",
  "like_count": 42,
  "dislike_count": 5,
  "user_liked": true,
  "user_disliked": false,
  "message": "Track liked successfully"
}
```

**Errors:**

- `401 UNAUTHORIZED` - No JWT token or invalid token
- `403 FORBIDDEN` - User is not a room member
- `404 NOT FOUND` - Room or playlist item not found
- `400 BAD REQUEST` - Invalid request or operation failed

**Workflow:**

1. Extract user ID from JWT token
2. Validate user is active room member
3. Validate playlist item exists in Redis
4. Get track adder's user ID from playlist item
5. Check if user previously liked/disliked
6. Update PostgreSQL `app_user_stats` (transactional):
   - Increment `total_like`
   - Decrement `total_dislike` if user previously disliked
7. Update Redis like/dislike sets:
   - Add user to `likes` set
   - Remove user from `dislikes` set
8. If Redis fails, compensate by reverting PostgreSQL update
9. Return updated like/dislike counts
10. TODO: Emit `PLAYLIST_ITEM_STATS_UPDATED` WebSocket event

**Example:**

```bash
curl -X POST \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  http://localhost:8080/api/rooms/123e4567-e89b-12d3-a456-426614174001/playlist/123e4567-e89b-12d3-a456-426614174000/like
```

---

### 2. Dislike Track

**Endpoint:** `POST /api/rooms/{roomId}/playlist/{playlistItemId}/dislike`

**Authentication:** Required (JWT)

**Path Parameters:**

- `roomId` (UUID) - Room ID
- `playlistItemId` (UUID) - Playlist item ID

**Request Body:** None

**Response:** `200 OK`

```json
{
  "playlist_item_id": "123e4567-e89b-12d3-a456-426614174000",
  "room_id": "123e4567-e89b-12d3-a456-426614174001",
  "like_count": 41,
  "dislike_count": 6,
  "user_liked": false,
  "user_disliked": true,
  "message": "Track disliked successfully"
}
```

**Errors:**

- `401 UNAUTHORIZED` - No JWT token or invalid token
- `403 FORBIDDEN` - User is not a room member
- `404 NOT FOUND` - Room or playlist item not found
- `400 BAD REQUEST` - Invalid request or operation failed

**Workflow:**

1. Extract user ID from JWT token
2. Validate user is active room member
3. Validate playlist item exists in Redis
4. Get track adder's user ID from playlist item
5. Check if user previously liked/disliked
6. Update PostgreSQL `app_user_stats` (transactional):
   - Increment `total_dislike`
   - Decrement `total_like` if user previously liked
7. Update Redis like/dislike sets:
   - Add user to `dislikes` set
   - Remove user from `likes` set
8. If Redis fails, compensate by reverting PostgreSQL update
9. Return updated like/dislike counts
10. TODO: Emit `PLAYLIST_ITEM_STATS_UPDATED` WebSocket event

**Example:**

```bash
curl -X POST \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  http://localhost:8080/api/rooms/123e4567-e89b-12d3-a456-426614174001/playlist/123e4567-e89b-12d3-a456-426614174000/dislike
```

---

### 3. Unlike Track

**Endpoint:** `DELETE /api/rooms/{roomId}/playlist/{playlistItemId}/like`

**Authentication:** Required (JWT)

**Path Parameters:**

- `roomId` (UUID) - Room ID
- `playlistItemId` (UUID) - Playlist item ID

**Request Body:** None

**Response:** `200 OK`

```json
{
  "playlist_item_id": "123e4567-e89b-12d3-a456-426614174000",
  "room_id": "123e4567-e89b-12d3-a456-426614174001",
  "like_count": 40,
  "dislike_count": 6,
  "user_liked": false,
  "user_disliked": false,
  "message": "Like removed successfully"
}
```

**Errors:**

- `401 UNAUTHORIZED` - No JWT token or invalid token
- `403 FORBIDDEN` - User is not a room member
- `404 NOT FOUND` - Room or playlist item not found
- `400 BAD REQUEST` - User has not liked this track

**Workflow:**

1. Extract user ID from JWT token
2. Validate user is active room member
3. Validate playlist item exists in Redis
4. Get track adder's user ID from playlist item
5. Check if user has liked the item
6. Update PostgreSQL `app_user_stats` (transactional):
   - Decrement `total_like`
7. Update Redis like set:
   - Remove user from `likes` set
8. If Redis fails, compensate by reverting PostgreSQL update
9. Return updated like/dislike counts
10. TODO: Emit `PLAYLIST_ITEM_STATS_UPDATED` WebSocket event

**Example:**

```bash
curl -X DELETE \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  http://localhost:8080/api/rooms/123e4567-e89b-12d3-a456-426614174001/playlist/123e4567-e89b-12d3-a456-426614174000/like
```

---

### 4. Undislike Track

**Endpoint:** `DELETE /api/rooms/{roomId}/playlist/{playlistItemId}/dislike`

**Authentication:** Required (JWT)

**Path Parameters:**

- `roomId` (UUID) - Room ID
- `playlistItemId` (UUID) - Playlist item ID

**Request Body:** None

**Response:** `200 OK`

```json
{
  "playlist_item_id": "123e4567-e89b-12d3-a456-426614174000",
  "room_id": "123e4567-e89b-12d3-a456-426614174001",
  "like_count": 40,
  "dislike_count": 5,
  "user_liked": false,
  "user_disliked": false,
  "message": "Dislike removed successfully"
}
```

**Errors:**

- `401 UNAUTHORIZED` - No JWT token or invalid token
- `403 FORBIDDEN` - User is not a room member
- `404 NOT FOUND` - Room or playlist item not found
- `400 BAD REQUEST` - User has not disliked this track

**Workflow:**

1. Extract user ID from JWT token
2. Validate user is active room member
3. Validate playlist item exists in Redis
4. Get track adder's user ID from playlist item
5. Check if user has disliked the item
6. Update PostgreSQL `app_user_stats` (transactional):
   - Decrement `total_dislike`
7. Update Redis dislike set:
   - Remove user from `dislikes` set
8. If Redis fails, compensate by reverting PostgreSQL update
9. Return updated like/dislike counts
10. TODO: Emit `PLAYLIST_ITEM_STATS_UPDATED` WebSocket event

**Example:**

```bash
curl -X DELETE \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  http://localhost:8080/api/rooms/123e4567-e89b-12d3-a456-426614174001/playlist/123e4567-e89b-12d3-a456-426614174000/dislike
```

---

## Data Flow

### Like Track Flow

```
1. Client sends POST /api/rooms/{roomId}/playlist/{playlistItemId}/like
   ↓
2. LikeDislikeController extracts user ID from JWT
   ↓
3. Controller validates room membership (PostgreSQL)
   ↓
4. LikeDislikeService validates playlist item exists (Redis)
   ↓
5. Service gets track adder ID from playlist item (Redis)
   ↓
6. Service checks if user previously liked/disliked (Redis)
   ↓
7. Service updates PostgreSQL app_user_stats (TRANSACTIONAL):
   - Increment total_like
   - Decrement total_dislike (if previously disliked)
   ↓
8. Service updates Redis like/dislike sets:
   - SADD partywave:room:{roomId}:playlist:item:{itemId}:likes {userId}
   - SREM partywave:room:{roomId}:playlist:item:{itemId}:dislikes {userId}
   ↓
9. If Redis fails:
   - Compensate by reverting PostgreSQL update
   - Return error to client
   ↓
10. Service gets updated counts:
   - SCARD partywave:room:{roomId}:playlist:item:{itemId}:likes
   - SCARD partywave:room:{roomId}:playlist:item:{itemId}:dislikes
   ↓
11. TODO: Service emits PLAYLIST_ITEM_STATS_UPDATED WebSocket event
   ↓
12. Service returns LikeDislikeResponseDTO to controller
   ↓
13. Controller returns 200 OK with response
```

---

## Testing

### Manual Testing with cURL

**Prerequisites:**

1. Backend server running on `http://localhost:8080`
2. Valid JWT token (obtain via `/api/auth/spotify/callback`)
3. Existing room with active membership
4. Playlist item added to room

**Test Scenario: Like a Track**

```bash
# 1. Like a track
curl -X POST \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  http://localhost:8080/api/rooms/ROOM_UUID/playlist/ITEM_UUID/like

# Expected: 200 OK
# {
#   "playlist_item_id": "ITEM_UUID",
#   "room_id": "ROOM_UUID",
#   "like_count": 1,
#   "dislike_count": 0,
#   "user_liked": true,
#   "user_disliked": false,
#   "message": "Track liked successfully"
# }

# 2. Verify idempotency - like again
curl -X POST \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  http://localhost:8080/api/rooms/ROOM_UUID/playlist/ITEM_UUID/like

# Expected: 200 OK with "Already liked" message, same counts

# 3. Switch to dislike
curl -X POST \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  http://localhost:8080/api/rooms/ROOM_UUID/playlist/ITEM_UUID/dislike

# Expected: 200 OK
# {
#   "like_count": 0,
#   "dislike_count": 1,
#   "user_liked": false,
#   "user_disliked": true,
#   "message": "Track disliked successfully"
# }

# 4. Remove dislike
curl -X DELETE \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  http://localhost:8080/api/rooms/ROOM_UUID/playlist/ITEM_UUID/dislike

# Expected: 200 OK
# {
#   "like_count": 0,
#   "dislike_count": 0,
#   "user_liked": false,
#   "user_disliked": false,
#   "message": "Dislike removed successfully"
# }
```

**Test Scenario: Error Cases**

```bash
# 1. Test without authentication
curl -X POST http://localhost:8080/api/rooms/ROOM_UUID/playlist/ITEM_UUID/like
# Expected: 401 UNAUTHORIZED

# 2. Test with non-member user
curl -X POST \
  -H "Authorization: Bearer NON_MEMBER_JWT_TOKEN" \
  http://localhost:8080/api/rooms/ROOM_UUID/playlist/ITEM_UUID/like
# Expected: 403 FORBIDDEN

# 3. Test with non-existent playlist item
curl -X POST \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  http://localhost:8080/api/rooms/ROOM_UUID/playlist/00000000-0000-0000-0000-000000000000/like
# Expected: 404 NOT FOUND
```

### Database Verification

**Verify PostgreSQL Updates:**

```sql
-- Check app_user_stats for track adder
SELECT au.display_name, aus.total_like, aus.total_dislike
FROM app_user au
JOIN app_user_stats aus ON au.stats_id = aus.id
WHERE au.id = 'TRACK_ADDER_UUID';

-- Expected: total_like increments when tracks are liked
-- Expected: total_dislike increments when tracks are disliked
```

**Verify Redis State:**

```bash
# Check likes set
redis-cli SMEMBERS partywave:room:ROOM_UUID:playlist:item:ITEM_UUID:likes
# Expected: List of user UUIDs who liked

# Check dislikes set
redis-cli SMEMBERS partywave:room:ROOM_UUID:playlist:item:ITEM_UUID:dislikes
# Expected: List of user UUIDs who disliked

# Check counts
redis-cli SCARD partywave:room:ROOM_UUID:playlist:item:ITEM_UUID:likes
redis-cli SCARD partywave:room:ROOM_UUID:playlist:item:ITEM_UUID:dislikes
```

### Race Condition Testing

**Simulate PostgreSQL Failure:**

To test compensation pattern, temporarily modify the service to throw an exception after PostgreSQL update:

```java
// In LikeDislikeService.likeTrack()
updateStatsForLike(addedById, previouslyDisliked);

// Simulate PostgreSQL success but Redis failure
throw new RuntimeException("Simulated Redis failure");
```

**Expected Behavior:**

1. PostgreSQL update succeeds
2. Redis update fails (simulated)
3. Compensation logic triggers
4. PostgreSQL update is reverted
5. Error is returned to client
6. No data inconsistency

**Verification:**

- Check logs for "Compensating by reverting PostgreSQL update"
- Verify PostgreSQL stats are unchanged
- Verify Redis sets are unchanged

---

## Performance Considerations

### Redis Operations

**Per Like/Dislike Request:**

- 1x `HGETALL` (get playlist item)
- 2x `SISMEMBER` (check if user liked/disliked)
- 1x `SREM` (remove from opposite set)
- 1x `SADD` (add to target set)
- 2x `SCARD` (get like/dislike counts)

**Total: ~7 Redis operations per request**

**Optimization Opportunities:**

- Use Redis pipelines for multiple operations
- Cache playlist item data in service layer
- Use Lua scripts for atomic multi-operation commands

### PostgreSQL Operations

**Per Like/Dislike Request:**

- 1x `SELECT` (room membership validation)
- 1x `SELECT` (get track adder user)
- 1x `SELECT` (get user stats)
- 1x `UPDATE` (update stats)

**Total: ~4 PostgreSQL operations per request**

**Optimization Opportunities:**

- Cache room membership status
- Batch stats updates (update periodically instead of real-time)
- Use PostgreSQL triggers for stats updates

---

## Known Limitations

1. **WebSocket Events Not Implemented:**

   - `PLAYLIST_ITEM_STATS_UPDATED` events are logged but not emitted
   - Clients must poll for updated counts or refresh on action
   - **TODO:** Implement WebSocket event emission with Spring WebSocket

2. **No Retry Logic:**

   - Removed `@Retryable` annotations (Spring Retry not configured)
   - Operations fail immediately on error
   - **TODO:** Configure Spring Retry and re-enable retries

3. **No Rate Limiting:**

   - Users can spam like/dislike requests
   - **TODO:** Implement rate limiting (e.g., max 10 requests per minute)

4. **No Batch Operations:**

   - Users must like/dislike tracks individually
   - **TODO:** Consider adding bulk like/dislike endpoints

5. **Compensation May Fail:**
   - If compensation fails, data becomes inconsistent
   - Manual intervention required (check logs for "CRITICAL" errors)
   - **TODO:** Implement reconciliation job to detect and fix inconsistencies

---

## Future Enhancements

1. **WebSocket Integration:**

   - Emit `PLAYLIST_ITEM_STATS_UPDATED` events in real-time
   - Subscribe clients to room-specific channels
   - Push updates to all connected clients

2. **Analytics:**

   - Track like/dislike patterns per user
   - Generate "most liked tracks" reports
   - User recommendations based on like history

3. **Advanced Statistics:**

   - Like/dislike ratio per user
   - Like/dislike trends over time
   - Top contributors (users with most liked tracks)

4. **Notifications:**

   - Notify track adder when their track is liked
   - Weekly summary emails of stats

5. **Admin Features:**
   - View all likes/dislikes for a room
   - Reset statistics
   - Ban users from liking/disliking

---

## Troubleshooting

### Issue: "Failed to update user statistics"

**Cause:** PostgreSQL update failed (track adder not found or no stats record)

**Solution:**

1. Verify track adder user exists: `SELECT * FROM app_user WHERE id = 'TRACK_ADDER_UUID'`
2. Verify stats record exists: `SELECT * FROM app_user_stats WHERE id = (SELECT stats_id FROM app_user WHERE id = 'TRACK_ADDER_UUID')`
3. If missing, create stats record manually or re-run user registration

### Issue: "CRITICAL: Failed to compensate PostgreSQL update"

**Cause:** Compensation logic failed after Redis failure

**Solution:**

1. Check logs for exact error
2. Manually verify PostgreSQL stats vs Redis counts
3. If inconsistent, manually correct:
   ```sql
   -- Recalculate stats from Redis (when room is still active)
   UPDATE app_user_stats SET total_like = CORRECT_VALUE WHERE id = 'STATS_UUID';
   ```
4. Consider implementing reconciliation job

### Issue: "User is not a member of this room"

**Cause:** User is not an active room member

**Solution:**

1. Verify room membership: `SELECT * FROM room_member WHERE room_id = 'ROOM_UUID' AND app_user_id = 'USER_UUID' AND is_active = true`
2. If membership exists but inactive, reactivate: `UPDATE room_member SET is_active = true WHERE ...`
3. If no membership, user must join room first

---

## Conclusion

The Like/Dislike system is now fully implemented with:

- ✅ Four REST API endpoints (like, dislike, unlike, undislike)
- ✅ Dual-storage architecture (Redis + PostgreSQL)
- ✅ Race condition handling with compensation pattern
- ✅ Room membership validation
- ✅ Comprehensive error handling
- ✅ Detailed logging for monitoring

**Remaining TODOs:**

- WebSocket event emission
- Spring Retry configuration
- Rate limiting
- Reconciliation job for data consistency

**Next Steps:**

1. Test all endpoints with real data
2. Monitor logs for compensation failures
3. Implement WebSocket events when WebSocket layer is ready
4. Add rate limiting to prevent abuse
