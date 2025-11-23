# Playback State Management Implementation

## Overview

This document describes the implementation of playback state management in `PlaybackRedisService` according to **REDIS_ARCHITECTURE.md Section 3**.

## Implementation Date

November 23, 2025

## What Was Implemented

### 1. TrackOperationResult Class

A new result class to provide detailed feedback from track operations:

```java
class TrackOperationResult {

  private final boolean success;
  private final String message;
  private final String playlistItemId;
}

```

**Purpose**: Returns operation status, descriptive messages, and the affected playlist item ID for better error handling and logging.

### 2. startTrack() Method

**Signature**: `public TrackOperationResult startTrack(String roomId, String playlistItemId)`

**Implementation based on REDIS_ARCHITECTURE.md Section 3.2 - Start Track Flow**

**Steps**:

1. **Validates playlist item status is QUEUED**

   - Checks if playlist item exists
   - Verifies status is QUEUED (only QUEUED tracks can transition to PLAYING)
   - Rejects if status is PLAYED or SKIPPED (final states)

2. **Marks previous PLAYING track as PLAYED**

   - Gets current playing track using `PlaylistRedisService.getCurrentPlayingItemId()`
   - Updates its status from PLAYING to PLAYED
   - Track remains in playlist list with updated status

3. **Updates playlist item status from QUEUED to PLAYING**

   - Uses `PlaylistRedisService.updatePlaylistItemStatus()`
   - Validates status transition rules

4. **Updates playback hash**
   - Sets `current_playlist_item_id`
   - Sets `started_at_ms` (UTC epoch milliseconds)
   - Sets `track_duration_ms` (from playlist item)
   - Sets `updated_at_ms` (UTC epoch milliseconds)

**Key Format**:

```
partywave:room:{roomId}:playback
```

**Hash Fields**:

```
current_playlist_item_id: UUID
started_at_ms: Long
track_duration_ms: Long
updated_at_ms: Long
```

**Return Values**:

- Success: `TrackOperationResult(true, "Track started successfully", playlistItemId)`
- Failure: `TrackOperationResult(false, errorMessage, playlistItemId)`

**Example Usage**:

```java
TrackOperationResult result = playbackRedisService.startTrack(roomId, playlistItemId);
if (result.isSuccess()) {
    // Emit TRACK_START WebSocket event
    // Clients compute elapsedMs = now - started_at_ms
    log.info("Track started: {}", result.getPlaylistItemId());
} else {
    log.error("Failed to start track: {}", result.getMessage());
}
```

### 3. skipTrack() Method

**Signature**: `public TrackOperationResult skipTrack(String roomId)`

**Implementation based on REDIS_ARCHITECTURE.md Section 3.3 - Skip Track Flow**

**Steps**:

1. **Gets current playing track from playback hash**

   - Uses `getCurrentPlaylistItemId()` to get current track
   - Returns error if no track is playing

2. **Validates current track status is PLAYING**

   - Retrieves playlist item from Redis
   - Verifies status is PLAYING
   - Rejects if status is not PLAYING

3. **Marks current track as SKIPPED**

   - Uses `PlaylistRedisService.updatePlaylistItemStatus()`
   - Updates status from PLAYING to SKIPPED (final state)
   - Track remains in playlist list with updated status

4. **Finds next QUEUED track**

   - Uses `PlaylistRedisService.getFirstQueuedItemId()`
   - Searches playlist list for first item with status QUEUED

5. **Starts next track or stops playback**
   - **If next track exists**: Calls `startTrack()` to automatically start it
   - **If no next track**: Calls `stopPlayback()` to clear playback hash

**Return Values**:

- Success (next track started): `TrackOperationResult(true, "Track skipped and next track started: {nextId}", nextPlaylistItemId)`
- Success (no more tracks): `TrackOperationResult(true, "Track skipped and playback stopped (no more tracks)", null)`
- Failure: `TrackOperationResult(false, errorMessage, affectedPlaylistItemId)`

**Example Usage**:

```java
TrackOperationResult result = playbackRedisService.skipTrack(roomId);
if (result.isSuccess()) {
    if (result.getPlaylistItemId() != null) {
        // Next track started - emit TRACK_START event
        log.info("Skipped and started next track: {}", result.getPlaylistItemId());
    } else {
        // No more tracks - emit PLAYBACK_STOPPED event
        log.info("Playback stopped: {}", result.getMessage());
    }
} else {
    log.error("Failed to skip track: {}", result.getMessage());
}
```

## Integration with PlaylistRedisService

The implementation integrates with `PlaylistRedisService` for:

1. **Status validation and transitions**:

   - `getPlaylistItem()` - Get playlist item data
   - `updatePlaylistItemStatus()` - Update status with validation
   - `getCurrentPlayingItemId()` - Find currently playing track
   - `getFirstQueuedItemId()` - Find next queued track

2. **Business rule enforcement**:
   - QUEUED → PLAYING (valid)
   - PLAYING → PLAYED (valid)
   - PLAYING → SKIPPED (valid)
   - PLAYED → \* (invalid - final state)
   - SKIPPED → \* (invalid - final state)

## Dependencies

```
PlaybackRedisService
  └── PlaylistRedisService
        └── LikeDislikeRedisService
```

No circular dependencies exist.

## Important Business Rules Enforced

1. **No pause state**: Tracks can only be started or skipped (no pause/resume)
2. **Final states**: PLAYED and SKIPPED are final states that cannot transition back to PLAYING
3. **Status validation**: All status transitions are validated before execution
4. **Automatic progression**: Skipping automatically starts the next queued track
5. **Track history**: All tracks remain in playlist list regardless of status

## Testing Considerations

### Unit Tests Should Cover:

1. **startTrack()**:

   - ✅ Start a QUEUED track successfully
   - ✅ Mark previous PLAYING track as PLAYED
   - ❌ Reject starting a PLAYED track
   - ❌ Reject starting a SKIPPED track
   - ❌ Reject starting a non-existent track
   - ✅ Update playback hash correctly

2. **skipTrack()**:

   - ✅ Skip PLAYING track successfully
   - ✅ Mark skipped track as SKIPPED
   - ✅ Auto-start next QUEUED track
   - ✅ Stop playback when no more tracks
   - ❌ Reject skip when no track is playing
   - ❌ Handle error when next track fails to start

3. **Integration**:
   - ✅ Complete flow: add tracks → start → skip → next track starts
   - ✅ Status transitions are atomic
   - ✅ Playback hash stays in sync with playlist item status

## WebSocket Event Integration (Future Work)

The controller/resource layer should emit WebSocket events after successful operations:

1. **After startTrack() success**:

   - Event: `TRACK_START`
   - Payload: `{ playlistItemId, startedAtMs, trackDurationMs, trackMetadata }`
   - Clients: Seek to `(now - startedAtMs)` in Spotify player

2. **After skipTrack() success**:
   - If next track started: Emit `TRACK_START` for next track
   - If no more tracks: Emit `PLAYBACK_STOPPED`

## Remaining Work

The following are **NOT** implemented in this service (should be handled by controller/resource layer):

1. **WebSocket event emission**: Controllers should emit events after successful operations
2. **Authorization**: Verify user has permission to control playback in the room
3. **Vote threshold logic**: Decision to skip based on vote count (handled elsewhere)
4. **Natural track completion**: Client-side or scheduler should call `startTrack()` for next track when current track finishes naturally

## Key Differences from Previous Implementation

### Before:

- `startPlayback()` only updated playback hash
- No playlist item status transitions
- No integration with PlaylistRedisService
- No automatic next track on skip

### After:

- `startTrack()` updates both playback hash and playlist item status
- Enforces status transition rules
- Integrates with PlaylistRedisService for status management
- `skipTrack()` automatically starts next track or stops playback

### Backward Compatibility:

- `startPlayback()` method retained for backward compatibility
- New code should use `startTrack()` and `skipTrack()`
- Consider deprecating `startPlayback()` in future release

## Example Scenarios

### Scenario 1: Start First Track in Room

```java
// User adds first track to playlist
playlistRedisService.addPlaylistItem(roomId, playlistItemId1, trackData);
// Status: QUEUED

// Start playback
TrackOperationResult result = playbackRedisService.startTrack(roomId, playlistItemId1);
// Status: PLAYING
// Playback hash updated with current track info
```

### Scenario 2: Skip Current Track and Auto-Start Next

```java
// Current state: Track 1 is PLAYING
// Track 2 is QUEUED

// Skip current track
TrackOperationResult result = playbackRedisService.skipTrack(roomId);
// Track 1 status: SKIPPED
// Track 2 status: PLAYING
// Playback hash updated with Track 2 info
// result.getPlaylistItemId() returns Track 2 ID

```

### Scenario 3: Skip Last Track in Playlist

```java
// Current state: Track 3 is PLAYING (last track)

// Skip current track
TrackOperationResult result = playbackRedisService.skipTrack(roomId);
// Track 3 status: SKIPPED
// Playback hash deleted (stopPlayback called)
// result.getPlaylistItemId() returns null
// result.getMessage() indicates playback stopped

```

## Compliance with REDIS_ARCHITECTURE.md

✅ **Section 3.2 - Start Track Flow**: Fully implemented

- ✅ Validate playlist item status (QUEUED)
- ✅ Mark previous PLAYING track as PLAYED
- ✅ Update playlist item status to PLAYING
- ✅ Update playback hash (current_playlist_item_id, started_at_ms, track_duration_ms, updated_at_ms)

✅ **Section 3.3 - Skip Track Flow**: Fully implemented

- ✅ Get current playing track from playback hash
- ✅ Validate track status is PLAYING
- ✅ Mark current track as SKIPPED
- ✅ Find next QUEUED track
- ✅ Start next track or stop playback

✅ **Section 1.2 - Status State Machine**: Enforced

- ✅ QUEUED → PLAYING (valid)
- ✅ PLAYING → PLAYED (valid)
- ✅ PLAYING → SKIPPED (valid)
- ✅ PLAYED → \* (invalid - rejected)
- ✅ SKIPPED → \* (invalid - rejected)

## Conclusion

The playback state management implementation is **complete and compliant** with REDIS_ARCHITECTURE.md Section 3. The service provides robust track control with proper status validation, automatic track progression, and detailed operation feedback through `TrackOperationResult`.
