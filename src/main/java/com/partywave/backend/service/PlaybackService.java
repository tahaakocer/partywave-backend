package com.partywave.backend.service;

import com.partywave.backend.exception.ForbiddenException;
import com.partywave.backend.exception.ResourceNotFoundException;
import com.partywave.backend.repository.RoomMemberRepository;
import com.partywave.backend.service.dto.TrackStartEventDTO;
import com.partywave.backend.service.redis.PlaybackRedisService;
import com.partywave.backend.service.redis.PlaylistRedisService;
import com.partywave.backend.service.redis.TrackOperationResult;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for managing playback operations with WebSocket event emission.
 * Based on PROJECT_OVERVIEW.md section 2.7.
 *
 * This service wraps PlaybackRedisService and adds:
 * - WebSocket event emission for playback state changes (TODO: requires WebSocket setup)
 * - High-level playback control methods (startNextTrack, etc.)
 *
 * Business rules:
 * - Tracks cannot be paused (no pause state)
 * - Only QUEUED tracks can transition to PLAYING
 * - PLAYED and SKIPPED are final states
 *
 * Note: WebSocket messaging is currently marked as TODO until Spring WebSocket is configured.
 */
@Service
public class PlaybackService {

    private static final Logger log = LoggerFactory.getLogger(PlaybackService.class);

    private final PlaybackRedisService playbackRedisService;
    private final PlaylistRedisService playlistRedisService;
    private final RoomMemberRepository roomMemberRepository;

    public PlaybackService(
        PlaybackRedisService playbackRedisService,
        PlaylistRedisService playlistRedisService,
        RoomMemberRepository roomMemberRepository
    ) {
        this.playbackRedisService = playbackRedisService;
        this.playlistRedisService = playlistRedisService;
        this.roomMemberRepository = roomMemberRepository;
    }

    /**
     * Start playing the next track in the room.
     * Implements PROJECT_OVERVIEW.md section 2.7, step 1.
     *
     * This method:
     * 1. Finds the first QUEUED track from Redis playlist
     * 2. Updates its status to PLAYING
     * 3. Updates the playback hash (current_playlist_item_id, started_at_ms, track_duration_ms, updated_at_ms)
     * 4. Emits TRACK_START WebSocket event to all room members
     *
     * @param roomId Room UUID
     * @return TrackOperationResult with success status and details
     */
    public TrackOperationResult startNextTrack(String roomId) {
        try {
            log.debug("Starting next track in room {}", roomId);

            // Step 1: Find first QUEUED track from Redis playlist
            String nextPlaylistItemId = playlistRedisService.getFirstQueuedItemId(roomId);

            if (nextPlaylistItemId == null) {
                String msg = "No queued tracks found in playlist";
                log.info("Cannot start next track in room {}: {}", roomId, msg);
                return new TrackOperationResult(false, msg, null);
            }

            log.debug("Found next queued track {} in room {}", nextPlaylistItemId, roomId);

            // Step 2-4: Start the track (updates status to PLAYING, updates playback hash)
            TrackOperationResult startResult = playbackRedisService.startTrack(roomId, nextPlaylistItemId);

            if (!startResult.isSuccess()) {
                log.error("Failed to start track {} in room {}: {}", nextPlaylistItemId, roomId, startResult.getMessage());
                return startResult;
            }

            log.info("Successfully started track {} in room {}", nextPlaylistItemId, roomId);

            // Step 5: Fetch track metadata from Redis for WebSocket event
            Map<Object, Object> playlistItem = playlistRedisService.getPlaylistItem(roomId, nextPlaylistItemId);

            if (playlistItem.isEmpty()) {
                log.warn("Track started but metadata not found for playlist item {} in room {}", nextPlaylistItemId, roomId);
                return startResult; // Track started successfully, but can't emit event
            }

            // Step 6: Get playback state for WebSocket event
            Map<Object, Object> playbackState = playbackRedisService.getPlaybackState(roomId);
            Long startedAtMs = playbackState.get("started_at_ms") != null
                ? Long.parseLong(playbackState.get("started_at_ms").toString())
                : System.currentTimeMillis();
            Long trackDurationMs = playbackState.get("track_duration_ms") != null
                ? Long.parseLong(playbackState.get("track_duration_ms").toString())
                : null;

            // Step 7: Emit TRACK_START WebSocket event to all room members
            emitTrackStartEvent(roomId, nextPlaylistItemId, playlistItem, startedAtMs, trackDurationMs);

            return startResult;
        } catch (Exception e) {
            String msg = "Exception occurred: " + e.getMessage();
            log.error("Failed to start next track in room {}", roomId, e);
            return new TrackOperationResult(false, msg, null);
        }
    }

    /**
     * Start a specific track by playlist item ID.
     * Use this when you already know which track to start (e.g., manual selection).
     *
     * @param roomId Room UUID
     * @param playlistItemId Playlist item UUID to start
     * @return TrackOperationResult with success status and details
     */
    public TrackOperationResult startTrack(String roomId, String playlistItemId) {
        try {
            log.debug("Starting specific track {} in room {}", playlistItemId, roomId);

            // Start the track using PlaybackRedisService
            TrackOperationResult startResult = playbackRedisService.startTrack(roomId, playlistItemId);

            if (!startResult.isSuccess()) {
                log.error("Failed to start track {} in room {}: {}", playlistItemId, roomId, startResult.getMessage());
                return startResult;
            }

            log.info("Successfully started track {} in room {}", playlistItemId, roomId);

            // Fetch track metadata and playback state for WebSocket event
            Map<Object, Object> playlistItem = playlistRedisService.getPlaylistItem(roomId, playlistItemId);

            if (playlistItem.isEmpty()) {
                log.warn("Track started but metadata not found for playlist item {} in room {}", playlistItemId, roomId);
                return startResult;
            }

            Map<Object, Object> playbackState = playbackRedisService.getPlaybackState(roomId);
            Long startedAtMs = playbackState.get("started_at_ms") != null
                ? Long.parseLong(playbackState.get("started_at_ms").toString())
                : System.currentTimeMillis();
            Long trackDurationMs = playbackState.get("track_duration_ms") != null
                ? Long.parseLong(playbackState.get("track_duration_ms").toString())
                : null;

            // Emit TRACK_START WebSocket event
            emitTrackStartEvent(roomId, playlistItemId, playlistItem, startedAtMs, trackDurationMs);

            return startResult;
        } catch (Exception e) {
            String msg = "Exception occurred: " + e.getMessage();
            log.error("Failed to start track {} in room {}", playlistItemId, roomId, e);
            return new TrackOperationResult(false, msg, playlistItemId);
        }
    }

    /**
     * Emit TRACK_START WebSocket event to all room members.
     * Based on PROJECT_OVERVIEW.md section 2.7 and 3.1.
     *
     * Event payload includes:
     * - playlist_item_id: UUID of the playlist item
     * - track metadata: name, artist, album, duration, source_uri
     * - started_at_ms: UTC epoch milliseconds when playback started
     * - track_duration_ms: Track duration in milliseconds
     *
     * TODO: Implement WebSocket event emission once Spring WebSocket is configured.
     * For now, this method logs the event that should be emitted.
     *
     * @param roomId Room UUID
     * @param playlistItemId Playlist item UUID
     * @param playlistItem Playlist item data from Redis
     * @param startedAtMs UTC epoch milliseconds when playback started
     * @param trackDurationMs Track duration in milliseconds
     */
    private void emitTrackStartEvent(
        String roomId,
        String playlistItemId,
        Map<Object, Object> playlistItem,
        Long startedAtMs,
        Long trackDurationMs
    ) {
        try {
            // Build track metadata from playlist item
            TrackStartEventDTO.TrackMetadata trackMetadata = new TrackStartEventDTO.TrackMetadata(
                getStringValue(playlistItem, "source_id"),
                getStringValue(playlistItem, "source_uri"),
                getStringValue(playlistItem, "name"),
                getStringValue(playlistItem, "artist"),
                getStringValue(playlistItem, "album"),
                getLongValue(playlistItem, "duration_ms"),
                getStringValue(playlistItem, "album_image_url")
            );

            // Build TRACK_START event DTO
            TrackStartEventDTO event = new TrackStartEventDTO(roomId, playlistItemId, trackMetadata, startedAtMs, trackDurationMs);

            // TODO: Emit WebSocket event once Spring WebSocket (SimpMessagingTemplate) is configured
            // String destination = "/topic/room/" + roomId;
            // messagingTemplate.convertAndSend(destination, event);

            log.info(
                "TODO: Emit TRACK_START WebSocket event for track {} in room {}. Event data: type={}, started_at_ms={}, track_duration_ms={}, track={}",
                playlistItemId,
                roomId,
                event.getType(),
                event.getStartedAtMs(),
                event.getTrackDurationMs(),
                event.getTrack() != null ? event.getTrack().getName() : "null"
            );
        } catch (Exception e) {
            log.error("Failed to prepare TRACK_START event for track {} in room {}", playlistItemId, roomId, e);
        }
    }

    /**
     * Helper method to safely get String value from Redis hash map.
     */
    private String getStringValue(Map<Object, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Helper method to safely get Long value from Redis hash map.
     */
    private Long getLongValue(Map<Object, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Failed to parse Long value for key {}: {}", key, value);
            return null;
        }
    }

    /**
     * Get current playback state for a room.
     * Delegates to PlaybackRedisService.
     *
     * @param roomId Room UUID
     * @return Map of playback fields, or empty map if no playback state exists
     */
    public Map<Object, Object> getPlaybackState(String roomId) {
        return playbackRedisService.getPlaybackState(roomId);
    }

    /**
     * Get current playback state with full track metadata.
     * Implements PROJECT_OVERVIEW.md section 6.4 - Get Current Playback State.
     *
     * Returns:
     * - Current playback state (started_at_ms, track_duration_ms, elapsed_ms)
     * - Full track metadata from playlist item
     *
     * @param roomId Room UUID
     * @return Map containing playback state and track metadata, or null if no playback
     */
    public Map<String, Object> getPlaybackStateWithMetadata(String roomId) {
        try {
            // Get current playback state from Redis
            Map<Object, Object> playbackState = playbackRedisService.getPlaybackState(roomId);

            if (playbackState.isEmpty()) {
                log.debug("No playback state found for room {}", roomId);
                return null;
            }

            // Get current playlist item ID
            Object currentPlaylistItemIdObj = playbackState.get("current_playlist_item_id");
            if (currentPlaylistItemIdObj == null) {
                log.warn("Playback state exists but current_playlist_item_id is null for room {}", roomId);
                return null;
            }

            String currentPlaylistItemId = currentPlaylistItemIdObj.toString();

            // Get track metadata from playlist item
            Map<Object, Object> playlistItem = playlistRedisService.getPlaylistItem(roomId, currentPlaylistItemId);

            if (playlistItem.isEmpty()) {
                log.warn("Current playlist item {} not found in room {}", currentPlaylistItemId, roomId);
                return null;
            }

            // Calculate elapsed time
            Long startedAtMs = getLongValue(playbackState, "started_at_ms");
            Long elapsedMs = null;
            if (startedAtMs != null) {
                elapsedMs = System.currentTimeMillis() - startedAtMs;
            }

            // Build response with playback state and track metadata
            return Map.of(
                "currentPlaylistItemId",
                currentPlaylistItemId,
                "startedAtMs",
                startedAtMs != null ? startedAtMs : 0L,
                "trackDurationMs",
                getLongValue(playbackState, "track_duration_ms") != null ? getLongValue(playbackState, "track_duration_ms") : 0L,
                "elapsedMs",
                elapsedMs != null ? elapsedMs : 0L,
                "updatedAtMs",
                getLongValue(playbackState, "updated_at_ms") != null ? getLongValue(playbackState, "updated_at_ms") : 0L,
                "track",
                buildTrackMetadata(playlistItem)
            );
        } catch (Exception e) {
            log.error("Failed to get playback state with metadata for room {}", roomId, e);
            return null;
        }
    }

    /**
     * Build track metadata map from playlist item.
     */
    private Map<String, Object> buildTrackMetadata(Map<Object, Object> playlistItem) {
        return Map.of(
            "sourceId",
            getStringValue(playlistItem, "source_id") != null ? getStringValue(playlistItem, "source_id") : "",
            "sourceUri",
            getStringValue(playlistItem, "source_uri") != null ? getStringValue(playlistItem, "source_uri") : "",
            "name",
            getStringValue(playlistItem, "name") != null ? getStringValue(playlistItem, "name") : "",
            "artist",
            getStringValue(playlistItem, "artist") != null ? getStringValue(playlistItem, "artist") : "",
            "album",
            getStringValue(playlistItem, "album") != null ? getStringValue(playlistItem, "album") : "",
            "durationMs",
            getLongValue(playlistItem, "duration_ms") != null ? getLongValue(playlistItem, "duration_ms") : 0L,
            "albumImageUrl",
            getStringValue(playlistItem, "album_image_url") != null ? getStringValue(playlistItem, "album_image_url") : "",
            "status",
            getStringValue(playlistItem, "status") != null ? getStringValue(playlistItem, "status") : "UNKNOWN"
        );
    }

    /**
     * Check if a track is currently playing in the room.
     * Delegates to PlaybackRedisService.
     *
     * @param roomId Room UUID
     * @return true if playback state exists
     */
    public boolean isPlaying(String roomId) {
        return playbackRedisService.isPlaying(roomId);
    }

    /**
     * Stop playback in a room.
     * Clears the playback state (deletes the hash).
     * Used when there are no more tracks to play.
     *
     * @param roomId Room UUID
     * @return true if successful
     */
    public boolean stopPlayback(String roomId) {
        return playbackRedisService.stopPlayback(roomId);
    }

    /**
     * Complete the currently playing track in a room.
     * Implements PROJECT_OVERVIEW.md section 2.7, step 2 (Track Completion).
     *
     * This method:
     * 1. Marks the current track status as PLAYED
     * 2. Finds the next QUEUED track
     * 3. If exists, calls startNextTrack()
     * 4. If not exists, clears the playback hash
     *
     * @param roomId Room UUID
     * @return TrackOperationResult with success status and details
     */
    public TrackOperationResult completeTrack(String roomId) {
        try {
            log.debug("Completing current track in room {}", roomId);

            // Step 1: Get current playing track from playback hash
            String currentPlaylistItemId = playbackRedisService.getCurrentPlaylistItemId(roomId);
            if (currentPlaylistItemId == null) {
                String msg = "No track is currently playing";
                log.warn("Cannot complete track in room {}: {}", roomId, msg);
                return new TrackOperationResult(false, msg, null);
            }

            // Step 2: Validate current track status is PLAYING before marking as PLAYED
            Map<Object, Object> currentItem = playlistRedisService.getPlaylistItem(roomId, currentPlaylistItemId);
            if (currentItem.isEmpty()) {
                String msg = "Current playlist item not found: " + currentPlaylistItemId;
                log.warn("Cannot complete track in room {}: {}", roomId, msg);
                return new TrackOperationResult(false, msg, currentPlaylistItemId);
            }

            Object statusObj = currentItem.get("status");
            String currentStatus = statusObj != null ? statusObj.toString() : null;

            if (!"PLAYING".equals(currentStatus)) {
                String msg = String.format("Cannot complete track with status: %s (must be PLAYING)", currentStatus);
                log.warn("Cannot complete track {} in room {}: {}", currentPlaylistItemId, roomId, msg);
                return new TrackOperationResult(false, msg, currentPlaylistItemId);
            }

            // Step 3: Mark current track as PLAYED (final state)
            boolean statusUpdated = playlistRedisService.updatePlaylistItemStatus(roomId, currentPlaylistItemId, "PLAYED");
            if (!statusUpdated) {
                String msg = "Failed to update playlist item status to PLAYED";
                log.error("Cannot complete track {} in room {}: {}", currentPlaylistItemId, roomId, msg);
                return new TrackOperationResult(false, msg, currentPlaylistItemId);
            }

            log.info("Marked track {} as PLAYED in room {}", currentPlaylistItemId, roomId);

            // Step 4: Find the next QUEUED track
            String nextPlaylistItemId = playlistRedisService.getFirstQueuedItemId(roomId);

            // Step 5: Start next track or stop playback
            if (nextPlaylistItemId != null) {
                // Start the next track
                log.debug("Starting next track {} after completion in room {}", nextPlaylistItemId, roomId);
                TrackOperationResult startResult = startNextTrack(roomId);
                if (startResult.isSuccess()) {
                    log.info("Track completed and next track started in room {}", roomId);
                    return new TrackOperationResult(
                        true,
                        "Track completed and next track started: " + nextPlaylistItemId,
                        nextPlaylistItemId
                    );
                } else {
                    log.warn(
                        "Failed to start next track {} after completion in room {}: {}",
                        nextPlaylistItemId,
                        roomId,
                        startResult.getMessage()
                    );
                    return new TrackOperationResult(
                        false,
                        "Track completed but failed to start next track: " + startResult.getMessage(),
                        null
                    );
                }
            } else {
                // No more tracks to play - stop playback and clear playback hash
                stopPlayback(roomId);
                log.info("Track completed and no more tracks to play in room {} - playback stopped", roomId);
                return new TrackOperationResult(true, "Track completed and playback stopped (no more tracks)", null);
            }
        } catch (Exception e) {
            String msg = "Exception occurred: " + e.getMessage();
            log.error("Failed to complete track in room {}", roomId, e);
            return new TrackOperationResult(false, msg, null);
        }
    }

    /**
     * Skip the currently playing track (manual skip by OWNER/MODERATOR).
     * Implements PROJECT_OVERVIEW.md section 6.3 - Manual Skip Track.
     *
     * This method:
     * 1. Validates user has OWNER or MODERATOR role
     * 2. Marks the current track status as SKIPPED
     * 3. Finds the next QUEUED track
     * 4. If exists, calls startNextTrack()
     * 5. If not exists, clears the playback hash
     *
     * Role Check: Only OWNER and MODERATOR can manually skip tracks.
     *
     * @param roomId Room UUID
     * @param userId User UUID performing the skip
     * @return TrackOperationResult with success status and details
     * @throws ForbiddenException if user doesn't have OWNER or MODERATOR role
     * @throws ResourceNotFoundException if user is not an active member of the room
     */
    public TrackOperationResult skipTrack(UUID roomId, UUID userId) {
        try {
            log.debug("User {} requesting to skip track in room {}", userId, roomId);

            // Step 1: Validate user is an active member of the room
            if (!roomMemberRepository.existsByRoomIdAndUserIdAndIsActiveTrue(roomId, userId)) {
                throw new ResourceNotFoundException("User is not an active member of this room");
            }

            // Step 2: Validate user has OWNER or MODERATOR role
            if (!roomMemberRepository.hasModeratorPermissions(roomId, userId)) {
                throw new ForbiddenException("Only room OWNER or MODERATOR can manually skip tracks");
            }

            // Step 3: Delegate to PlaybackRedisService to perform the skip
            // (This handles status validation, marking as SKIPPED, and starting next track)
            TrackOperationResult skipResult = playbackRedisService.skipTrack(roomId.toString());

            if (skipResult.isSuccess()) {
                log.info("User {} (OWNER/MODERATOR) manually skipped track in room {}", userId, roomId);
            } else {
                log.warn("User {} (OWNER/MODERATOR) failed to skip track in room {}: {}", userId, roomId, skipResult.getMessage());
            }

            return skipResult;
        } catch (ForbiddenException | ResourceNotFoundException e) {
            // Re-throw authorization exceptions
            throw e;
        } catch (Exception e) {
            String msg = "Exception occurred: " + e.getMessage();
            log.error("Failed to skip track in room {} by user {}", roomId, userId, e);
            return new TrackOperationResult(false, msg, null);
        }
    }
}
