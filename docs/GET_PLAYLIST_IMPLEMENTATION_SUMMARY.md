# Get Playlist Endpoint Implementation Summary

## Overview

Implemented the `GET /api/rooms/{roomId}/playlist` endpoint that retrieves the complete playlist for a room, including all tracks (active + history) with like/dislike counts, sorted by sequence number.

**Date:** November 24, 2025

---

## Implementation Details

### 1. New DTO

#### `GetPlaylistResponseDTO.java`

**Location:** `src/main/java/com/partywave/backend/service/dto/`

**Purpose:** Response wrapper for the playlist endpoint containing:

- `roomId`: Room UUID (String)
- `items`: List of `PlaylistItemDTO` objects
- `totalCount`: Total number of items in the playlist

**Key Features:**

- Auto-calculates total count when items are set
- Initializes with empty list to prevent null issues

---

### 2. Service Layer

#### `PlaylistService.getPlaylist()`

**Location:** `src/main/java/com/partywave/backend/service/PlaylistService.java`

**Method Signature:**

```java
public GetPlaylistResponseDTO getPlaylist(UUID roomId, UUID userId)
```

**Workflow:**

1. **Validation:**

   - Validates room exists in PostgreSQL
   - Validates user is an active member of the room
   - Throws `ResourceNotFoundException` if room not found
   - Throws `UnauthorizedRoomAccessException` if user not a member

2. **Data Retrieval:**

   - Gets all playlist items from Redis using `PlaylistRedisService.getAllPlaylistItems()`
   - Uses `LRANGE` to get all item IDs
   - Uses `HGETALL` for each item to get complete metadata

3. **User Information:**

   - Extracts unique user IDs from playlist items
   - Batch fetches user display names from PostgreSQL
   - Maps user IDs to display names for efficient lookup

4. **Like/Dislike Counts:**

   - For each playlist item, queries `LikeDislikeRedisService`
   - Gets like count using `SCARD` on likes set
   - Gets dislike count using `SCARD` on dislikes set

5. **Sorting:**

   - Sorts items by `sequence_number` in ascending order
   - Ensures chronological order (oldest to newest)

6. **Response:**
   - Returns `GetPlaylistResponseDTO` with all items and metadata

**Helper Methods:**

```java
private PlaylistItemDTO convertToPlaylistItemDTO(
    Map<Object, Object> item,
    String roomId,
    Map<String, String> userDisplayNames
)
```

- Converts Redis hash data to `PlaylistItemDTO`
- Enriches with like/dislike counts
- Handles missing or malformed data gracefully

```java
private String getStringValue(Map<Object, Object> map, String key)
```

- Safely extracts string values from Redis hash

```java
private Long getLongValue(Map<Object, Object> map, String key)
```

- Safely extracts and parses long values from Redis hash

**Dependencies Added:**

- `LikeDislikeRedisService` (injected via constructor)

---

### 3. Controller Endpoint

#### `PlaylistController.getPlaylist()`

**Location:** `src/main/java/com/partywave/backend/web/rest/PlaylistController.java`

**Endpoint Details:**

- **Method:** GET
- **Path:** `/api/rooms/{roomId}/playlist`
- **Authentication:** Required (JWT token)
- **Authorization:** User must be an active room member

**Request Parameters:**

- `roomId`: Room UUID (path variable)

**Response:**

- **Status:** 200 OK
- **Body:** `GetPlaylistResponseDTO`

**Response Structure:**

```json
{
  "roomId": "uuid-string",
  "items": [
    {
      "id": "playlist-item-uuid",
      "roomId": "room-uuid",
      "spotifyTrackId": "spotify-track-id",
      "trackName": "Track Name",
      "trackArtist": "Artist Name",
      "trackAlbum": "Album Name",
      "trackImageUrl": "https://...",
      "durationMs": 180000,
      "addedById": "user-uuid",
      "addedByDisplayName": "User Display Name",
      "addedAtMs": 1700000000000,
      "sequenceNumber": 1,
      "status": "QUEUED|PLAYING|PLAYED|SKIPPED",
      "likeCount": 5,
      "dislikeCount": 2
    }
  ],
  "totalCount": 1
}
```

**Error Responses:**

- `404 NOT_FOUND`: Room doesn't exist
- `403 FORBIDDEN`: User is not a room member
- `401 UNAUTHORIZED`: No valid JWT token

---

## Redis Operations

### Keys Used

1. **Playlist List:**

   - Key: `partywave:room:{roomId}:playlist`
   - Type: LIST
   - Operation: `LRANGE 0 -1` (get all items)

2. **Playlist Item Hash:**

   - Key: `partywave:room:{roomId}:playlist:item:{playlistItemId}`
   - Type: HASH
   - Operation: `HGETALL` (get all fields)

3. **Like Set:**

   - Key: `partywave:room:{roomId}:playlist:item:{playlistItemId}:likes`
   - Type: SET
   - Operation: `SCARD` (count members)

4. **Dislike Set:**
   - Key: `partywave:room:{roomId}:playlist:item:{playlistItemId}:dislikes`
   - Type: SET
   - Operation: `SCARD` (count members)

### Data Flow

```
1. GET /api/rooms/{roomId}/playlist
   ↓
2. Controller validates JWT and extracts userId
   ↓
3. Service validates room exists (PostgreSQL)
   ↓
4. Service validates user membership (PostgreSQL)
   ↓
5. PlaylistRedisService.getAllPlaylistItems(roomId)
   → LRANGE partywave:room:{roomId}:playlist 0 -1
   → For each item: HGETALL partywave:room:{roomId}:playlist:item:{itemId}
   ↓
6. Fetch user display names (PostgreSQL batch query)
   ↓
7. For each item: LikeDislikeRedisService.getLikeCount/getDislikeCount
   → SCARD partywave:room:{roomId}:playlist:item:{itemId}:likes
   → SCARD partywave:room:{roomId}:playlist:item:{itemId}:dislikes
   ↓
8. Convert to DTOs and sort by sequence_number
   ↓
9. Return GetPlaylistResponseDTO
```

---

## Playlist Item Statuses

The endpoint returns all tracks regardless of status:

- **QUEUED**: Tracks waiting to be played (active playlist)
- **PLAYING**: Currently playing track (active playlist)
- **PLAYED**: Tracks that finished playing (history)
- **SKIPPED**: Tracks that were skipped (history)

Items are sorted by `sequence_number` which represents chronological order.

---

## Performance Considerations

1. **Batch Operations:**

   - Uses `LRANGE` to get all item IDs in one call
   - Batch fetches user display names with single database query
   - Individual `HGETALL` calls for each playlist item (Redis is very fast)

2. **Caching:**

   - All playlist data comes from Redis (in-memory)
   - Like/dislike counts are real-time from Redis sets
   - Only user display names require PostgreSQL access

3. **Sorting:**
   - In-memory sorting by sequence number (very fast)
   - No pagination needed as playlists are typically small (<100 items)

---

## Testing Recommendations

### Manual Testing

1. **Empty Playlist:**

   ```bash
   curl -H "Authorization: Bearer $TOKEN" \
        http://localhost:8080/api/rooms/{roomId}/playlist
   ```

   Expected: `{ "roomId": "...", "items": [], "totalCount": 0 }`

2. **Playlist with Items:**

   - Add tracks using `POST /api/rooms/{roomId}/playlist`
   - Get playlist using `GET /api/rooms/{roomId}/playlist`
   - Verify items are sorted by sequence_number
   - Verify like/dislike counts are 0 initially

3. **After Likes/Dislikes:**

   - Add likes/dislikes using respective endpoints (when implemented)
   - Get playlist again
   - Verify counts are updated

4. **Authentication Tests:**
   - Try without token → 401 Unauthorized
   - Try with invalid room ID → 404 Not Found
   - Try as non-member → 403 Forbidden

### Integration Tests

Consider adding tests for:

- Empty playlist scenario
- Playlist with multiple items of different statuses
- Correct sorting by sequence number
- Like/dislike count accuracy
- User display name resolution
- Authorization checks

---

## Related Endpoints

- **POST /api/rooms/{roomId}/playlist** - Add track to playlist
- **GET /api/rooms/{roomId}/tracks/search** - Search Spotify tracks
- **GET /api/rooms/{roomId}/state** - Get complete room state (includes playlist)

---

## Future Enhancements

1. **Pagination:**

   - Add optional `limit` and `offset` parameters
   - Useful for rooms with very large playlists (100+ tracks)

2. **Status Filtering:**

   - Add optional `status` query parameter
   - Example: `?status=QUEUED,PLAYING` for active tracks only
   - Example: `?status=PLAYED,SKIPPED` for history only

3. **User Feedback:**

   - Include `likedByCurrentUser` and `dislikedByCurrentUser` boolean fields
   - Requires additional Redis queries per item

4. **Performance Optimization:**
   - Consider Redis pipelining for fetching multiple items
   - Cache user display names in Redis with TTL

---

## Files Modified

1. **New Files:**

   - `src/main/java/com/partywave/backend/service/dto/GetPlaylistResponseDTO.java`

2. **Modified Files:**

   - `src/main/java/com/partywave/backend/service/PlaylistService.java`
     - Added `LikeDislikeRedisService` dependency
     - Added `getPlaylist()` method
     - Added helper methods for DTO conversion
   - `src/main/java/com/partywave/backend/web/rest/PlaylistController.java`
     - Added `getPlaylist()` endpoint

3. **Compilation Status:**
   - ✅ All files compile successfully
   - ✅ No errors, only minor warnings about null type safety (consistent with codebase)
   - ✅ Maven build successful

---

## Conclusion

The GET playlist endpoint is fully implemented and ready for testing. It provides:

✅ Complete playlist retrieval (active + history)  
✅ Real-time like/dislike counts from Redis  
✅ Sorted by chronological order (sequence_number)  
✅ Proper authentication and authorization  
✅ Efficient data fetching with minimal database queries  
✅ Graceful error handling

The implementation follows the existing codebase patterns and architecture guidelines from PROJECT_OVERVIEW.md and REDIS_ARCHITECTURE.md.
