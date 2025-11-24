package com.partywave.backend.service.redis;

import com.partywave.backend.config.CacheConfiguration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis service for managing room playback state.
 * Based on REDIS_ARCHITECTURE.md specifications.
 *
 * Key structure:
 * - Playback hash: partywave:room:{roomId}:playback
 *
 * Business rules:
 * - Tracks can be started or skipped
 * - Tracks CANNOT be paused (no pause state)
 * - Once started, a track plays until completion or is skipped
 *
 * Hash fields:
 * - current_playlist_item_id: UUID of currently playing track
 * - started_at_ms: UTC epoch milliseconds when playback started
 * - track_duration_ms: Track duration in milliseconds
 * - updated_at_ms: UTC epoch milliseconds of last state change
 */
@Service
public class PlaybackRedisService {

    private static final Logger log = LoggerFactory.getLogger(PlaybackRedisService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final PlaylistRedisService playlistRedisService;

    public PlaybackRedisService(RedisTemplate<String, Object> redisTemplate, PlaylistRedisService playlistRedisService) {
        this.redisTemplate = redisTemplate;
        this.playlistRedisService = playlistRedisService;
    }

    // ========================================
    // Key Building Methods
    // ========================================

    private String buildPlaybackKey(String roomId) {
        return CacheConfiguration.KEY_PREFIX + "room:" + roomId + ":playback";
    }

    // ========================================
    // Playback State Operations
    // ========================================

    /**
     * Start playback for a track in a room.
     * Sets the playback state with current playlist item and timing information.
     *
     * @param roomId Room UUID
     * @param playlistItemId Playlist item UUID to start playing
     * @param startedAtMs UTC epoch milliseconds when playback started
     * @param trackDurationMs Track duration in milliseconds
     * @return true if successful
     */
    public boolean startPlayback(String roomId, String playlistItemId, long startedAtMs, long trackDurationMs) {
        try {
            String playbackKey = buildPlaybackKey(roomId);
            long nowMs = System.currentTimeMillis();

            Map<String, String> playbackData = Map.of(
                "current_playlist_item_id",
                playlistItemId,
                "started_at_ms",
                String.valueOf(startedAtMs),
                "track_duration_ms",
                String.valueOf(trackDurationMs),
                "updated_at_ms",
                String.valueOf(nowMs)
            );

            redisTemplate.opsForHash().putAll(playbackKey, playbackData);

            log.debug("Started playback for playlist item {} in room {}", playlistItemId, roomId);
            return true;
        } catch (Exception e) {
            log.error("Failed to start playback for playlist item {} in room {}", playlistItemId, roomId, e);
            return false;
        }
    }

    /**
     * Update playback state.
     * Allows updating specific fields without replacing the entire hash.
     *
     * @param roomId Room UUID
     * @param playbackData Map of fields to update
     * @return true if successful
     */
    public boolean updatePlayback(String roomId, Map<String, String> playbackData) {
        try {
            String playbackKey = buildPlaybackKey(roomId);

            // Always update the updated_at_ms field
            playbackData.put("updated_at_ms", String.valueOf(System.currentTimeMillis()));

            redisTemplate.opsForHash().putAll(playbackKey, playbackData);

            log.debug("Updated playback state for room {}", roomId);
            return true;
        } catch (Exception e) {
            log.error("Failed to update playback state for room {}", roomId, e);
            return false;
        }
    }

    /**
     * Get playback state for a room.
     *
     * @param roomId Room UUID
     * @return Map of playback fields, or empty map if no playback state exists
     */
    public Map<Object, Object> getPlaybackState(String roomId) {
        try {
            String playbackKey = buildPlaybackKey(roomId);
            Map<Object, Object> playbackData = redisTemplate.opsForHash().entries(playbackKey);
            return playbackData != null ? playbackData : Collections.emptyMap();
        } catch (Exception e) {
            log.error("Failed to get playback state for room {}", roomId, e);
            return Collections.emptyMap();
        }
    }

    /**
     * Get current playlist item ID being played.
     *
     * @param roomId Room UUID
     * @return Playlist item UUID, or null if no track is playing
     */
    public String getCurrentPlaylistItemId(String roomId) {
        String playbackKey = buildPlaybackKey(roomId);
        Object itemId = redisTemplate.opsForHash().get(playbackKey, "current_playlist_item_id");
        return itemId != null ? itemId.toString() : null;
    }

    /**
     * Get when playback started (UTC epoch milliseconds).
     *
     * @param roomId Room UUID
     * @return Started at timestamp in milliseconds, or null if not available
     */
    public Long getStartedAtMs(String roomId) {
        String playbackKey = buildPlaybackKey(roomId);
        Object startedAt = redisTemplate.opsForHash().get(playbackKey, "started_at_ms");
        return startedAt != null ? Long.parseLong(startedAt.toString()) : null;
    }

    /**
     * Get track duration (milliseconds).
     *
     * @param roomId Room UUID
     * @return Track duration in milliseconds, or null if not available
     */
    public Long getTrackDurationMs(String roomId) {
        String playbackKey = buildPlaybackKey(roomId);
        Object duration = redisTemplate.opsForHash().get(playbackKey, "track_duration_ms");
        return duration != null ? Long.parseLong(duration.toString()) : null;
    }

    /**
     * Get elapsed time since playback started (milliseconds).
     * Calculated as: current_time - started_at_ms
     * Clients use this to seek to the correct position in their Spotify player.
     *
     * @param roomId Room UUID
     * @return Elapsed time in milliseconds, or null if no playback state exists
     */
    public Long getElapsedMs(String roomId) {
        Long startedAtMs = getStartedAtMs(roomId);
        if (startedAtMs == null) {
            return null;
        }

        long nowMs = System.currentTimeMillis();
        return nowMs - startedAtMs;
    }

    /**
     * Check if a track is currently playing in the room.
     *
     * @param roomId Room UUID
     * @return true if playback state exists
     */
    public boolean isPlaying(String roomId) {
        String playbackKey = buildPlaybackKey(roomId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(playbackKey));
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
        try {
            String playbackKey = buildPlaybackKey(roomId);
            redisTemplate.delete(playbackKey);

            log.debug("Stopped playback for room {}", roomId);
            return true;
        } catch (Exception e) {
            log.error("Failed to stop playback for room {}", roomId, e);
            return false;
        }
    }

    // ========================================
    // Track Control Operations (Section 3.2, 3.3)
    // ========================================

    /**
     * Start playing a track in a room.
     * Implements the "Start Track Flow" from REDIS_ARCHITECTURE.md Section 3.2.
     *
     * This method:
     * 1. Validates the playlist item status is QUEUED
     * 2. Marks any previously PLAYING track as PLAYED
     * 3. Updates the playlist item status from QUEUED to PLAYING
     * 4. Updates the playback hash with current track info
     *
     * @param roomId Room UUID
     * @param playlistItemId Playlist item UUID to start playing
     * @return TrackOperationResult with success status and details
     */
    public TrackOperationResult startTrack(String roomId, String playlistItemId) {
        try {
            // Step 1: Validate playlist item exists and status is QUEUED
            Map<Object, Object> playlistItem = playlistRedisService.getPlaylistItem(roomId, playlistItemId);
            if (playlistItem.isEmpty()) {
                String msg = "Playlist item not found: " + playlistItemId;
                log.warn("Failed to start track in room {}: {}", roomId, msg);
                return new TrackOperationResult(false, msg, playlistItemId);
            }

            Object statusObj = playlistItem.get("status");
            if (statusObj == null) {
                String msg = "Playlist item has no status field";
                log.warn("Failed to start track {} in room {}: {}", playlistItemId, roomId, msg);
                return new TrackOperationResult(false, msg, playlistItemId);
            }

            String currentStatus = statusObj.toString();

            // Only QUEUED tracks can be started
            if (!"QUEUED".equals(currentStatus)) {
                String msg = String.format("Invalid status transition: %s cannot transition to PLAYING", currentStatus);
                log.warn("Failed to start track {} in room {}: {}", playlistItemId, roomId, msg);
                return new TrackOperationResult(false, msg, playlistItemId);
            }

            // Step 2: Mark any previously PLAYING track as PLAYED
            String previousPlayingItemId = playlistRedisService.getCurrentPlayingItemId(roomId);
            if (previousPlayingItemId != null && !previousPlayingItemId.equals(playlistItemId)) {
                boolean updated = playlistRedisService.updatePlaylistItemStatus(roomId, previousPlayingItemId, "PLAYED");
                if (updated) {
                    log.debug("Marked previous track {} as PLAYED in room {}", previousPlayingItemId, roomId);
                } else {
                    log.warn("Failed to mark previous track {} as PLAYED in room {}", previousPlayingItemId, roomId);
                }
            }

            // Step 3: Mark the selected playlist item as PLAYING
            boolean statusUpdated = playlistRedisService.updatePlaylistItemStatus(roomId, playlistItemId, "PLAYING");
            if (!statusUpdated) {
                String msg = "Failed to update playlist item status to PLAYING";
                log.error("Failed to start track {} in room {}: {}", playlistItemId, roomId, msg);
                return new TrackOperationResult(false, msg, playlistItemId);
            }

            // Step 4: Update the playback hash
            Object durationObj = playlistItem.get("duration_ms");
            long trackDurationMs = durationObj != null ? Long.parseLong(durationObj.toString()) : 0L;
            long startedAtMs = System.currentTimeMillis();

            Map<String, String> playbackData = Map.of(
                "current_playlist_item_id",
                playlistItemId,
                "started_at_ms",
                String.valueOf(startedAtMs),
                "track_duration_ms",
                String.valueOf(trackDurationMs),
                "updated_at_ms",
                String.valueOf(startedAtMs)
            );

            String playbackKey = buildPlaybackKey(roomId);
            redisTemplate.opsForHash().putAll(playbackKey, playbackData);

            log.info("Started track {} in room {} (duration: {}ms)", playlistItemId, roomId, trackDurationMs);
            return new TrackOperationResult(true, "Track started successfully", playlistItemId);
        } catch (Exception e) {
            String msg = "Exception occurred: " + e.getMessage();
            log.error("Failed to start track {} in room {}", playlistItemId, roomId, e);
            return new TrackOperationResult(false, msg, playlistItemId);
        }
    }

    /**
     * Skip the currently playing track in a room.
     * Implements the "Skip Track Flow" from REDIS_ARCHITECTURE.md Section 3.3.
     *
     * This method:
     * 1. Gets the current playing track from playback hash
     * 2. Validates the track status is PLAYING
     * 3. Marks the current track as SKIPPED
     * 4. Finds the next QUEUED track
     * 5. Starts the next track automatically (or stops playback if no more tracks)
     *
     * @param roomId Room UUID
     * @return TrackOperationResult with success status and details
     */
    public TrackOperationResult skipTrack(String roomId) {
        try {
            // Step 1: Get current playing track from playback hash
            String currentPlaylistItemId = getCurrentPlaylistItemId(roomId);
            if (currentPlaylistItemId == null) {
                String msg = "No track is currently playing";
                log.warn("Failed to skip track in room {}: {}", roomId, msg);
                return new TrackOperationResult(false, msg, null);
            }

            // Step 2: Validate current track status is PLAYING
            Map<Object, Object> currentItem = playlistRedisService.getPlaylistItem(roomId, currentPlaylistItemId);
            if (currentItem.isEmpty()) {
                String msg = "Current playlist item not found: " + currentPlaylistItemId;
                log.warn("Failed to skip track in room {}: {}", roomId, msg);
                return new TrackOperationResult(false, msg, currentPlaylistItemId);
            }

            Object statusObj = currentItem.get("status");
            String currentStatus = statusObj != null ? statusObj.toString() : null;

            if (!"PLAYING".equals(currentStatus)) {
                String msg = String.format("Cannot skip track with status: %s (must be PLAYING)", currentStatus);
                log.warn("Failed to skip track {} in room {}: {}", currentPlaylistItemId, roomId, msg);
                return new TrackOperationResult(false, msg, currentPlaylistItemId);
            }

            // Step 3: Mark current track as SKIPPED
            boolean statusUpdated = playlistRedisService.updatePlaylistItemStatus(roomId, currentPlaylistItemId, "SKIPPED");
            if (!statusUpdated) {
                String msg = "Failed to update playlist item status to SKIPPED";
                log.error("Failed to skip track {} in room {}: {}", currentPlaylistItemId, roomId, msg);
                return new TrackOperationResult(false, msg, currentPlaylistItemId);
            }

            log.info("Skipped track {} in room {}", currentPlaylistItemId, roomId);

            // Step 4: Find the next QUEUED track
            String nextPlaylistItemId = playlistRedisService.getFirstQueuedItemId(roomId);

            // Step 5: Start the next track or stop playback
            if (nextPlaylistItemId != null) {
                // Start the next track
                TrackOperationResult startResult = startTrack(roomId, nextPlaylistItemId);
                if (startResult.isSuccess()) {
                    log.info("Auto-started next track {} in room {} after skip", nextPlaylistItemId, roomId);
                    return new TrackOperationResult(
                        true,
                        "Track skipped and next track started: " + nextPlaylistItemId,
                        nextPlaylistItemId
                    );
                } else {
                    log.warn(
                        "Failed to auto-start next track {} in room {} after skip: {}",
                        nextPlaylistItemId,
                        roomId,
                        startResult.getMessage()
                    );
                    return new TrackOperationResult(
                        false,
                        "Track skipped but failed to start next track: " + startResult.getMessage(),
                        null
                    );
                }
            } else {
                // No more tracks to play - stop playback
                stopPlayback(roomId);
                log.info("No more tracks to play in room {} after skip - playback stopped", roomId);
                return new TrackOperationResult(true, "Track skipped and playback stopped (no more tracks)", null);
            }
        } catch (Exception e) {
            String msg = "Exception occurred: " + e.getMessage();
            log.error("Failed to skip track in room {}", roomId, e);
            return new TrackOperationResult(false, msg, null);
        }
    }

    // ========================================
    // Cleanup Operations
    // ========================================

    /**
     * Delete playback state for a room.
     * Used during room cleanup/deletion.
     *
     * @param roomId Room UUID
     */
    public void deletePlaybackState(String roomId) {
        try {
            String playbackKey = buildPlaybackKey(roomId);
            redisTemplate.delete(playbackKey);

            log.info("Deleted playback state for room {}", roomId);
        } catch (Exception e) {
            log.error("Failed to delete playback state for room {}", roomId, e);
        }
    }

    /**
     * Set TTL for playback state.
     * Used when room becomes inactive.
     *
     * @param roomId Room UUID
     * @param ttlSeconds TTL in seconds
     */
    public void setPlaybackTTL(String roomId, long ttlSeconds) {
        try {
            String playbackKey = buildPlaybackKey(roomId);
            redisTemplate.expire(playbackKey, ttlSeconds, TimeUnit.SECONDS);

            log.debug("Set TTL of {} seconds for playback state in room {}", ttlSeconds, roomId);
        } catch (Exception e) {
            log.error("Failed to set TTL for playback state in room {}", roomId, e);
        }
    }
}
