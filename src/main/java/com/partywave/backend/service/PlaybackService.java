package com.partywave.backend.service;

import com.partywave.backend.service.dto.TrackStartEventDTO;
import com.partywave.backend.service.redis.PlaybackRedisService;
import com.partywave.backend.service.redis.PlaylistRedisService;
import com.partywave.backend.service.redis.TrackOperationResult;
import java.util.Map;
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

    public PlaybackService(PlaybackRedisService playbackRedisService, PlaylistRedisService playlistRedisService) {
        this.playbackRedisService = playbackRedisService;
        this.playlistRedisService = playlistRedisService;
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
}
