# Add Track to Playlist Implementation Summary

## Overview

Successfully implemented the "Add Track to Playlist" feature based on PROJECT_OVERVIEW.md section 2.6.

## Implementation Date

November 24, 2025

## Components Created

### 1. DTOs (Data Transfer Objects)

#### `AddTrackRequestDTO.java`

- **Location**: `src/main/java/com/partywave/backend/service/dto/`
- **Purpose**: Input validation for track metadata when adding a track
- **Fields**:
  - `sourceId` - Spotify track ID (required)
  - `sourceUri` - Spotify URI (required)
  - `name` - Track name (required)
  - `artist` - Primary artist name (required)
  - `album` - Album name (required)
  - `durationMs` - Track duration in milliseconds (required)
  - `albumImageUrl` - Album image URL (optional)
- **Validation**: Uses Jakarta validation annotations (`@NotBlank`, `@NotNull`, `@Positive`)

#### `AddTrackResponseDTO.java`

- **Location**: `src/main/java/com/partywave/backend/service/dto/`
- **Purpose**: Response containing created playlist item details
- **Fields**:
  - `playlistItemId` - UUID of the created playlist item
  - `roomId` - Room UUID
  - Track metadata (sourceId, sourceUri, name, artist, album, durationMs, albumImageUrl)
  - User info (addedById, addedByDisplayName)
  - Playlist info (addedAtMs, sequenceNumber, status)
  - Statistics (likeCount, dislikeCount - always 0 for new tracks)
  - `autoStarted` - Boolean flag indicating if track was auto-started

### 2. Service Layer

#### `PlaylistService.java`

- **Location**: `src/main/java/com/partywave/backend/service/`
- **Purpose**: Business logic for playlist operations
- **Main Method**: `addTrack(UUID roomId, UUID userId, AddTrackRequestDTO request)`

**Workflow** (as per PROJECT_OVERVIEW.md section 2.6):

1. ✅ Validates room exists
2. ✅ Validates user is an active member of the room
3. ✅ Generates UUID for playlist item
4. ✅ Gets next sequence number (Redis INCR counter)
5. ✅ Creates playlist item hash in Redis (status=QUEUED)
6. ✅ RPUSH to playlist list (appends to end)
7. ✅ Auto-starts track if playlist is empty and no track is playing
8. ⏳ TODO: Emit WebSocket event `PLAYLIST_ITEM_ADDED`

**Dependencies**:

- `RoomRepository` - Room validation
- `RoomMemberRepository` - Membership validation
- `AppUserRepository` - User details (display name)
- `PlaylistRedisService` - Low-level Redis operations
- `PlaybackRedisService` - Playback state management

### 3. Controller Endpoint

#### `PlaylistController.java` (Updated)

- **Location**: `src/main/java/com/partywave/backend/web/rest/`
- **New Endpoint**: `POST /api/rooms/{roomId}/playlist`
- **Request Mapping**: Changed from `/api/rooms/{roomId}/tracks` to `/api/rooms/{roomId}`
- **Authentication**: Requires JWT token (user ID extracted from SecurityContext)
- **Request Body**: `AddTrackRequestDTO` with `@Valid` annotation for validation
- **Response**: `201 CREATED` with `AddTrackResponseDTO` body

**Endpoint Details**:

- Method: `addTrackToPlaylist(@PathVariable UUID roomId, @Valid @RequestBody AddTrackRequestDTO request)`
- Returns: `ResponseEntity<AddTrackResponseDTO>`
- HTTP Status: `201 CREATED` on success
- Error Handling: Delegated to global exception handler
  - `404 NOT_FOUND` if room doesn't exist
  - `403 FORBIDDEN` if user is not a room member
  - `400 BAD_REQUEST` if validation fails

### 4. Supporting Classes

#### `TrackOperationResult.java`

- **Location**: `src/main/java/com/partywave/backend/service/redis/`
- **Purpose**: Result object for track operations (extracted from PlaybackRedisService)
- **Fields**: `success`, `message`, `playlistItemId`
- **Used by**: `PlaybackRedisService.startTrack()` method

## Key Features Implemented

### ✅ Validation

- Room existence check
- User membership validation (active members only)
- Input validation via Jakarta Validation annotations

### ✅ Redis Operations

- UUID generation for playlist item
- Sequence number generation using Redis INCR counter
- Playlist item hash creation with all metadata
- RPUSH to playlist list (FIFO queue)

### ✅ Auto-Start Logic

- Checks if playlist is empty and no track is currently playing
- Automatically starts the first track if conditions are met
- Updates playlist item status from `QUEUED` to `PLAYING`
- Updates Redis playback hash with timing information

### ✅ Response

- Returns complete playlist item details
- Includes `autoStarted` flag for client notification
- Contains user display name for UI rendering

## Redis Data Structure

When a track is added, the following Redis operations are performed:

1. **Sequence Counter Increment**:

   ```
   INCR partywave:room:{roomId}:playlist:sequence_counter
   ```

2. **Playlist Item Hash Creation**:

   ```
   HSET partywave:room:{roomId}:playlist:item:{playlistItemId}
     id {playlistItemId}
     room_id {roomId}
     added_by_id {userId}
     sequence_number {sequenceNumber}
     status QUEUED
     added_at_ms {currentTimeMs}
     source_id {spotifyTrackId}
     source_uri {spotifyUri}
     name {trackName}
     artist {artistName}
     album {albumName}
     duration_ms {durationMs}
     album_image_url {imageUrl} (if provided)
   ```

3. **Append to Playlist List**:

   ```
   RPUSH partywave:room:{roomId}:playlist {playlistItemId}
   ```

4. **Auto-Start (if applicable)**:
   ```
   HSET partywave:room:{roomId}:playlist:item:{playlistItemId} status PLAYING
   HSET partywave:room:{roomId}:playback
     current_playlist_item_id {playlistItemId}
     started_at_ms {startTimeMs}
     track_duration_ms {durationMs}
     updated_at_ms {currentTimeMs}
   ```

## API Usage Example

### Request

```http
POST /api/rooms/123e4567-e89b-12d3-a456-426614174000/playlist
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "sourceId": "3n3Ppam7vgaVa1iaRUc9Lp",
  "sourceUri": "spotify:track:3n3Ppam7vgaVa1iaRUc9Lp",
  "name": "Mr. Brightside",
  "artist": "The Killers",
  "album": "Hot Fuss",
  "durationMs": 222973,
  "albumImageUrl": "https://i.scdn.co/image/ab67616d00001e02..."
}
```

### Response (201 Created)

```json
{
  "playlistItemId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "roomId": "123e4567-e89b-12d3-a456-426614174000",
  "sourceId": "3n3Ppam7vgaVa1iaRUc9Lp",
  "sourceUri": "spotify:track:3n3Ppam7vgaVa1iaRUc9Lp",
  "name": "Mr. Brightside",
  "artist": "The Killers",
  "album": "Hot Fuss",
  "durationMs": 222973,
  "albumImageUrl": "https://i.scdn.co/image/ab67616d00001e02...",
  "addedById": "789e4567-e89b-12d3-a456-426614174001",
  "addedByDisplayName": "John Doe",
  "addedAtMs": 1700000000000,
  "sequenceNumber": 1,
  "status": "PLAYING",
  "likeCount": 0,
  "dislikeCount": 0,
  "autoStarted": true
}
```

## Testing Status

### ✅ Compilation

- Project compiled successfully with `mvn clean compile -DskipTests`
- No compilation errors
- Only minor null safety warnings (common in Spring Boot projects)

### ⏳ Pending Tests

The following tests should be performed:

1. **Integration Tests**:

   - Test adding a track to an empty playlist
   - Test adding a track to an existing playlist
   - Test auto-start behavior
   - Test validation failures (invalid room, non-member user)

2. **Manual Testing**:
   - Use Postman/curl to test the endpoint
   - Verify Redis data structure
   - Check WebSocket events (once implemented)

## Known Limitations / TODO

1. **WebSocket Events**: Not yet implemented

   - Need to emit `PLAYLIST_ITEM_ADDED` event to all room members
   - Need to emit `TRACK_START` event if track is auto-started
   - Requires WebSocket service/messaging template implementation

2. **Error Handling**: Currently relies on global exception handler

   - Consider adding specific error messages for edge cases

3. **Rate Limiting**: Not implemented

   - Consider adding rate limiting to prevent spam

4. **Deduplication**: Not implemented (as per spec)
   - Same track can be added multiple times (creates new playlist items)
   - Consider optional deduplication based on business requirements

## Files Modified/Created

### Created:

1. `src/main/java/com/partywave/backend/service/dto/AddTrackRequestDTO.java`
2. `src/main/java/com/partywave/backend/service/dto/AddTrackResponseDTO.java`
3. `src/main/java/com/partywave/backend/service/PlaylistService.java`
4. `src/main/java/com/partywave/backend/service/redis/TrackOperationResult.java`

### Modified:

1. `src/main/java/com/partywave/backend/web/rest/PlaylistController.java`

   - Changed request mapping from `/api/rooms/{roomId}/tracks` to `/api/rooms/{roomId}`
   - Added `PlaylistService` dependency
   - Added `addTrackToPlaylist()` endpoint method
   - Search endpoint path changed to `/tracks/search`

2. `src/main/java/com/partywave/backend/service/redis/PlaybackRedisService.java`
   - Extracted `TrackOperationResult` to separate file (changed from package-private class to public)

## References

- **PROJECT_OVERVIEW.md** - Section 2.6: Adding Tracks to Playlist
- **REDIS_ARCHITECTURE.md** - Section 1: Playlist Items and Track Metadata
- **AUTHENTICATION.md** - Section 2: JWT Authentication

## Next Steps (Recommendations)

1. **Implement WebSocket Events**:

   - Create WebSocket service or use messaging template
   - Emit `PLAYLIST_ITEM_ADDED` event with full playlist item data
   - Emit `TRACK_START` event when track is auto-started

2. **Add Integration Tests**:

   - Test the complete add track workflow
   - Test membership validation
   - Test auto-start logic

3. **Frontend Integration**:

   - Update frontend to call the new endpoint
   - Handle `autoStarted` flag in response
   - Subscribe to WebSocket events (once implemented)

4. **Performance Considerations**:

   - Monitor Redis INCR performance for sequence numbers
   - Consider batch operations if adding multiple tracks

5. **Security**:
   - Review JWT validation
   - Consider rate limiting
   - Add audit logging for track additions

---

**Implementation Status**: ✅ Core functionality complete, ⏳ WebSocket events pending
**Build Status**: ✅ Successful compilation
**Ready for Testing**: ✅ Yes
