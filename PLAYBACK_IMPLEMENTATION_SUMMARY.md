# Playback Implementation Summary

This document summarizes the playback features implemented based on PROJECT_OVERVIEW.md sections 6.2, 6.3, and 6.4.

## Implemented Features

### 1. Track Completion Logic (Section 6.2)

**File:** `PlaybackService.java`  
**Method:** `completeTrack(String roomId)`

**Functionality:**

- Marks the current track status as `PLAYED` (final state)
- Finds the next `QUEUED` track
- If next track exists, calls `startNextTrack()`
- If no more tracks, clears the playback hash (stops playback)

**Business Rules:**

- Only tracks with status `PLAYING` can be marked as `PLAYED`
- `PLAYED` is a final state - tracks cannot transition back to `PLAYING`
- Tracks remain in the playlist list for history with their like/dislike statistics

**Usage:**

```java
TrackOperationResult result = playbackService.completeTrack(roomId);
if (result.isSuccess()) {
    // Track completed and next track started (if exists)
}
```

---

### 2. Manual Skip Track (Section 6.3)

**Files:**

- `PlaybackService.java` - `skipTrack(UUID roomId, UUID userId)`
- `PlaybackController.java` - `POST /api/rooms/{roomId}/playback/skip`
- `RoomMemberRepository.java` - `hasModeratorPermissions(UUID roomId, UUID userId)`
- `ForbiddenException.java` (new exception class)

**Functionality:**

- Validates user has OWNER or MODERATOR role
- Marks current track status as `SKIPPED` (final state)
- Starts next `QUEUED` track automatically
- If no more tracks, clears playback hash

**Authorization:**

- Only OWNER and MODERATOR roles can manually skip tracks
- Throws `ForbiddenException` (403 Forbidden) if user doesn't have permission
- Throws `ResourceNotFoundException` (404 Not Found) if user is not an active member

**API Endpoint:**

```
POST /api/rooms/{roomId}/playback/skip
Authorization: Bearer {JWT_TOKEN}
```

**Response:**

```json
{
  "success": true,
  "message": "Track skipped and next track started: {nextPlaylistItemId}",
  "nextPlaylistItemId": "uuid-of-next-track"
}
```

**Error Responses:**

- 401 Unauthorized - Invalid or missing JWT token
- 403 Forbidden - User doesn't have OWNER or MODERATOR role
- 404 Not Found - User is not an active member of the room
- 400 Bad Request - No track is currently playing or operation failed

---

### 3. Get Current Playback State (Section 6.4)

**Files:**

- `PlaybackService.java` - `getPlaybackStateWithMetadata(String roomId)`
- `PlaybackController.java` - `GET /api/rooms/{roomId}/playback`

**Functionality:**

- Returns current playback state from Redis playback hash
- Includes full track metadata from playlist item
- Calculates elapsed time for client synchronization

**API Endpoint:**

```
GET /api/rooms/{roomId}/playback
Authorization: Bearer {JWT_TOKEN}
```

**Response:**

```json
{
  "currentPlaylistItemId": "uuid-of-current-track",
  "startedAtMs": 1732441234567,
  "trackDurationMs": 180000,
  "elapsedMs": 45000,
  "updatedAtMs": 1732441234567,
  "track": {
    "sourceId": "spotify-track-id",
    "sourceUri": "spotify:track:...",
    "name": "Track Name",
    "artist": "Artist Name",
    "album": "Album Name",
    "durationMs": 180000,
    "albumImageUrl": "https://...",
    "status": "PLAYING"
  }
}
```

**Response Codes:**

- 200 OK - Playback is active, returns state and track metadata
- 204 No Content - No playback is active in the room
- 401 Unauthorized - Invalid or missing JWT token

**Client Usage:**
Clients can use this endpoint to:

- Synchronize playback on join or reconnection
- Calculate elapsed time: `elapsedMs = now - startedAtMs`
- Seek to correct position in Spotify player: `player.seek(elapsedMs)`

---

## New Files Created

1. **ForbiddenException.java**

   - Custom exception for authorization failures
   - HTTP Status: 403 Forbidden
   - Used when users lack required permissions (e.g., non-moderator trying to skip)

2. **PlaybackController.java**
   - REST controller for playback operations
   - Endpoints:
     - `POST /api/rooms/{roomId}/playback/skip` - Manual skip (OWNER/MODERATOR only)
     - `GET /api/rooms/{roomId}/playback` - Get current playback state

---

## Modified Files

1. **PlaybackService.java**

   - Added `completeTrack()` method
   - Added `skipTrack()` method with role checking
   - Added `getPlaybackStateWithMetadata()` method
   - Added helper method `buildTrackMetadata()`
   - Injected `RoomMemberRepository` for role checks

2. **RoomMemberRepository.java**

   - Added `hasModeratorPermissions(UUID roomId, UUID userId)` query method
   - Returns true if user is active member with OWNER or MODERATOR role

3. **ExceptionTranslator.java**
   - Added mapping for `ForbiddenException` → HTTP 403

---

## Testing Checklist

### Manual Testing

#### 1. Complete Track

- [ ] Track completes naturally and status changes to PLAYED
- [ ] Next QUEUED track starts automatically
- [ ] When no more tracks, playback stops and hash is cleared
- [ ] Track remains in playlist with PLAYED status

#### 2. Manual Skip Track

- [ ] OWNER can skip current track
- [ ] MODERATOR can skip current track
- [ ] PARTICIPANT receives 403 Forbidden
- [ ] DJ role receives 403 Forbidden
- [ ] Non-member receives 404 Not Found
- [ ] Unauthenticated request receives 401 Unauthorized
- [ ] Track status changes to SKIPPED
- [ ] Next track starts automatically
- [ ] WebSocket TRACK_SKIPPED event emitted (TODO: requires WebSocket setup)

#### 3. Get Playback State

- [ ] Returns state with track metadata when playback is active
- [ ] Returns 204 No Content when no playback is active
- [ ] Elapsed time calculation is accurate
- [ ] All track metadata fields are populated correctly
- [ ] Unauthorized requests receive 401

### Integration Testing

- [ ] Track completion → start next track → complete → no more tracks → stop playback
- [ ] Manual skip → start next track → manual skip → no more tracks → stop playback
- [ ] Get playback state during various playback stages (QUEUED, PLAYING, PLAYED, SKIPPED)
- [ ] Role changes (promote to MODERATOR) → manual skip permission granted
- [ ] User leaves room → rejoins → can/cannot skip based on role

---

## WebSocket Events (TODO)

The following WebSocket events should be emitted when WebSocket is configured:

1. **TRACK_SKIPPED** - When OWNER/MODERATOR manually skips a track

   - Payload: `{ type: "TRACK_SKIPPED", roomId, skippedPlaylistItemId, nextPlaylistItemId, skippedBy }`

2. **TRACK_FINISHED** - When track completes naturally (optional)
   - Payload: `{ type: "TRACK_FINISHED", roomId, completedPlaylistItemId, nextPlaylistItemId }`

---

## Notes

- All endpoints require JWT authentication
- Role checking is performed at the service layer for reusability
- Track status transitions follow strict state machine (see PROJECT_OVERVIEW.md section 2.7)
- Tracks are never removed from playlist - only status changes
- Like/dislike statistics are preserved for history

---

## Related Documentation

- `PROJECT_OVERVIEW.md` - Section 2.7 (Synchronized Playback)
- `PROJECT_OVERVIEW.md` - Section 6.2 (Track Completion Logic)
- `PROJECT_OVERVIEW.md` - Section 6.3 (Manual Skip Track)
- `PROJECT_OVERVIEW.md` - Section 6.4 (Get Current Playback State)
- `REDIS_ARCHITECTURE.md` - Section 3.2, 3.3 (Track Control Operations)
- `EXCEPTION_HANDLING.md` - Custom exceptions and error responses
