package com.partywave.backend.service.redis;

import com.partywave.backend.config.CacheConfiguration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis service for managing playlist items and track metadata.
 * Based on REDIS_ARCHITECTURE.md specifications.
 *
 * Key structure:
 * - Playlist item hash: partywave:room:{roomId}:playlist:item:{playlistItemId}
 * - Room playlist list: partywave:room:{roomId}:playlist
 * - Sequence counter: partywave:room:{roomId}:playlist:sequence_counter
 *
 * Business rules:
 * - Tracks are always appended to the end of the playlist
 * - Tracks are never removed from the playlist list (only status changes)
 * - Status values: QUEUED, PLAYING, PLAYED, SKIPPED
 * - PLAYED and SKIPPED are final states (cannot transition back to PLAYING)
 *
 * Note: Like/dislike operations are handled by LikeDislikeRedisService.
 */
@Service
public class PlaylistRedisService {

    private static final Logger log = LoggerFactory.getLogger(PlaylistRedisService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final LikeDislikeRedisService likeDislikeRedisService;

    public PlaylistRedisService(RedisTemplate<String, Object> redisTemplate, LikeDislikeRedisService likeDislikeRedisService) {
        this.redisTemplate = redisTemplate;
        this.likeDislikeRedisService = likeDislikeRedisService;
    }

    // ========================================
    // Key Building Methods
    // ========================================

    private String buildPlaylistItemKey(String roomId, String playlistItemId) {
        return CacheConfiguration.KEY_PREFIX + "room:" + roomId + ":playlist:item:" + playlistItemId;
    }

    private String buildPlaylistKey(String roomId) {
        return CacheConfiguration.KEY_PREFIX + "room:" + roomId + ":playlist";
    }

    private String buildSequenceCounterKey(String roomId) {
        return CacheConfiguration.KEY_PREFIX + "room:" + roomId + ":playlist:sequence_counter";
    }

    private String buildPlaylistItemPattern(String roomId) {
        return CacheConfiguration.KEY_PREFIX + "room:" + roomId + ":playlist:item:*";
    }

    // ========================================
    // Playlist Item Operations
    // ========================================

    /**
     * Add a new track to the playlist.
     * Track is appended to the end of the playlist list.
     *
     * @param roomId Room UUID
     * @param playlistItemId Playlist item UUID
     * @param playlistItemData Map containing all playlist item fields
     * @return true if successful
     */
    public boolean addPlaylistItem(String roomId, String playlistItemId, Map<String, String> playlistItemData) {
        try {
            String itemKey = buildPlaylistItemKey(roomId, playlistItemId);
            String playlistKey = buildPlaylistKey(roomId);

            // Ensure status is QUEUED for new items
            playlistItemData.put("status", "QUEUED");
            playlistItemData.put("id", playlistItemId);
            playlistItemData.put("room_id", roomId);

            // Store playlist item hash
            redisTemplate.opsForHash().putAll(itemKey, playlistItemData);

            // Append item ID to playlist list (RPUSH - add to tail)
            redisTemplate.opsForList().rightPush(playlistKey, playlistItemId);

            log.debug("Added playlist item {} to room {}", playlistItemId, roomId);
            return true;
        } catch (Exception e) {
            log.error("Failed to add playlist item {} to room {}", playlistItemId, roomId, e);
            return false;
        }
    }

    /**
     * Get the next sequence number for a room.
     * Uses Redis INCR for atomic increment.
     *
     * @param roomId Room UUID
     * @return Next sequence number (starts at 1), or null if Redis operation fails
     */
    public Long getNextSequenceNumber(String roomId) {
        try {
            String counterKey = buildSequenceCounterKey(roomId);
            Long result = redisTemplate.opsForValue().increment(counterKey);
            if (result == null) {
                log.error("Redis INCR returned null for key: {}", counterKey);
            } else {
                log.debug("Generated sequence number {} for room {}", result, roomId);
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to generate sequence number for room {}: {}", roomId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get a specific playlist item data.
     *
     * @param roomId Room UUID
     * @param playlistItemId Playlist item UUID
     * @return Map of playlist item fields, or empty map if not found
     */
    public Map<Object, Object> getPlaylistItem(String roomId, String playlistItemId) {
        String itemKey = buildPlaylistItemKey(roomId, playlistItemId);
        Map<Object, Object> item = redisTemplate.opsForHash().entries(itemKey);
        return item != null ? item : Collections.emptyMap();
    }

    /**
     * Get all playlist item IDs for a room (complete playlist).
     * Returns items in chronological order (oldest first).
     *
     * @param roomId Room UUID
     * @return List of playlist item IDs
     */
    public List<String> getPlaylistItemIds(String roomId) {
        String playlistKey = buildPlaylistKey(roomId);
        List<Object> items = redisTemplate.opsForList().range(playlistKey, 0, -1);
        if (items == null) {
            return Collections.emptyList();
        }
        return items.stream().map(Object::toString).toList();
    }

    /**
     * Get all playlist items with their full data for a room.
     * Uses LRANGE to get all item IDs, then HGETALL for each item.
     * Returns items in chronological order (oldest first).
     * Includes all tracks regardless of status (QUEUED, PLAYING, PLAYED, SKIPPED).
     *
     * @param roomId Room UUID
     * @return List of playlist items with full data
     */
    public List<Map<Object, Object>> getAllPlaylistItems(String roomId) {
        List<String> itemIds = getPlaylistItemIds(roomId);
        List<Map<Object, Object>> result = new ArrayList<>();

        for (String itemId : itemIds) {
            Map<Object, Object> item = getPlaylistItem(roomId, itemId);
            if (!item.isEmpty()) {
                result.add(item);
            }
        }

        return result;
    }

    /**
     * Get playlist items filtered by status.
     * Use this to get only active tracks (QUEUED, PLAYING) or history (PLAYED, SKIPPED).
     *
     * @param roomId Room UUID
     * @param statuses Status values to filter by (e.g., "QUEUED", "PLAYING")
     * @return List of playlist items matching the status filter
     */
    public List<Map<Object, Object>> getPlaylistItemsByStatus(String roomId, String... statuses) {
        List<String> itemIds = getPlaylistItemIds(roomId);
        Set<String> statusSet = Set.of(statuses);
        List<Map<Object, Object>> result = new ArrayList<>();

        for (String itemId : itemIds) {
            Map<Object, Object> item = getPlaylistItem(roomId, itemId);
            if (!item.isEmpty()) {
                Object status = item.get("status");
                if (status != null && statusSet.contains(status.toString())) {
                    result.add(item);
                }
            }
        }

        return result;
    }

    /**
     * Update playlist item status.
     * CRITICAL: Validates status transitions according to business rules.
     *
     * Status transition rules:
     * - QUEUED → PLAYING (valid)
     * - PLAYING → PLAYED (valid)
     * - PLAYING → SKIPPED (valid)
     * - PLAYED → * (INVALID - final state)
     * - SKIPPED → * (INVALID - final state)
     *
     * @param roomId Room UUID
     * @param playlistItemId Playlist item UUID
     * @param newStatus New status value
     * @return true if status was updated, false if transition is invalid
     */
    public boolean updatePlaylistItemStatus(String roomId, String playlistItemId, String newStatus) {
        try {
            String itemKey = buildPlaylistItemKey(roomId, playlistItemId);

            // Get current status
            Object currentStatusObj = redisTemplate.opsForHash().get(itemKey, "status");
            if (currentStatusObj == null) {
                log.warn("Playlist item {} not found in room {}", playlistItemId, roomId);
                return false;
            }

            String currentStatus = currentStatusObj.toString();

            // Validate status transition
            if (!isValidStatusTransition(currentStatus, newStatus)) {
                log.warn(
                    "Invalid status transition for playlist item {} in room {}: {} → {}",
                    playlistItemId,
                    roomId,
                    currentStatus,
                    newStatus
                );
                return false;
            }

            // Update status
            redisTemplate.opsForHash().put(itemKey, "status", newStatus);
            log.debug("Updated playlist item {} status: {} → {}", playlistItemId, currentStatus, newStatus);
            return true;
        } catch (Exception e) {
            log.error("Failed to update playlist item {} status in room {}", playlistItemId, roomId, e);
            return false;
        }
    }

    /**
     * Validate status transition according to business rules.
     */
    private boolean isValidStatusTransition(String currentStatus, String newStatus) {
        // PLAYED and SKIPPED are final states
        if ("PLAYED".equals(currentStatus) || "SKIPPED".equals(currentStatus)) {
            return false;
        }

        // QUEUED can only transition to PLAYING
        if ("QUEUED".equals(currentStatus)) {
            return "PLAYING".equals(newStatus);
        }

        // PLAYING can transition to PLAYED or SKIPPED
        if ("PLAYING".equals(currentStatus)) {
            return "PLAYED".equals(newStatus) || "SKIPPED".equals(newStatus);
        }

        return false;
    }

    /**
     * Get the current playing playlist item ID.
     *
     * @param roomId Room UUID
     * @return Playlist item ID with status PLAYING, or null if none
     */
    public String getCurrentPlayingItemId(String roomId) {
        List<String> itemIds = getPlaylistItemIds(roomId);

        for (String itemId : itemIds) {
            Map<Object, Object> item = getPlaylistItem(roomId, itemId);
            Object status = item.get("status");
            if ("PLAYING".equals(status)) {
                return itemId;
            }
        }

        return null;
    }

    /**
     * Get the first queued playlist item ID.
     *
     * @param roomId Room UUID
     * @return Playlist item ID with status QUEUED, or null if none
     */
    public String getFirstQueuedItemId(String roomId) {
        List<String> itemIds = getPlaylistItemIds(roomId);

        for (String itemId : itemIds) {
            Map<Object, Object> item = getPlaylistItem(roomId, itemId);
            Object status = item.get("status");
            if ("QUEUED".equals(status)) {
                return itemId;
            }
        }

        return null;
    }

    // ========================================
    // Cleanup Operations
    // ========================================

    /**
     * Delete all playlist data for a room.
     * Used during room cleanup/deletion.
     *
     * @param roomId Room UUID
     */
    public void deleteRoomPlaylistData(String roomId) {
        try {
            // Get all playlist item IDs
            List<String> itemIds = getPlaylistItemIds(roomId);

            // Delete like/dislike data for all playlist items using LikeDislikeRedisService
            if (!itemIds.isEmpty()) {
                likeDislikeRedisService.deleteAllRoomFeedback(roomId, new java.util.HashSet<>(itemIds));
            }

            // Delete each playlist item hash
            for (String itemId : itemIds) {
                String itemKey = buildPlaylistItemKey(roomId, itemId);
                redisTemplate.delete(itemKey);
            }

            // Delete playlist list and sequence counter
            String playlistKey = buildPlaylistKey(roomId);
            String sequenceCounterKey = buildSequenceCounterKey(roomId);

            redisTemplate.delete(playlistKey);
            redisTemplate.delete(sequenceCounterKey);

            log.info("Deleted all playlist data for room {}", roomId);
        } catch (Exception e) {
            log.error("Failed to delete playlist data for room {}", roomId, e);
        }
    }

    /**
     * Set TTL for all playlist-related keys in a room.
     * Used when room becomes inactive.
     *
     * Note: This sets TTL for playlist items only. Like/dislike TTL should be managed
     * separately if needed. Consider using a coordinated cleanup approach for all room data.
     *
     * @param roomId Room UUID
     * @param ttlSeconds TTL in seconds
     */
    public void setPlaylistTTL(String roomId, long ttlSeconds) {
        try {
            // Get all playlist item IDs
            List<String> itemIds = getPlaylistItemIds(roomId);

            // Set TTL for each playlist item hash
            for (String itemId : itemIds) {
                String itemKey = buildPlaylistItemKey(roomId, itemId);
                redisTemplate.expire(itemKey, ttlSeconds, TimeUnit.SECONDS);
            }

            // Set TTL for playlist list and sequence counter
            String playlistKey = buildPlaylistKey(roomId);
            String sequenceCounterKey = buildSequenceCounterKey(roomId);

            redisTemplate.expire(playlistKey, ttlSeconds, TimeUnit.SECONDS);
            redisTemplate.expire(sequenceCounterKey, ttlSeconds, TimeUnit.SECONDS);

            log.debug("Set TTL of {} seconds for playlist data in room {}", ttlSeconds, roomId);
        } catch (Exception e) {
            log.error("Failed to set TTL for playlist data in room {}", roomId, e);
        }
    }
}
