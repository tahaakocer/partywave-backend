package com.partywave.backend.service.redis;

import com.partywave.backend.config.CacheConfiguration;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis service for managing like/dislike statistics for playlist items.
 * Based on REDIS_ARCHITECTURE.md section 2 specifications.
 *
 * Key structure:
 * - Like set: partywave:room:{roomId}:playlist:item:{playlistItemId}:likes
 * - Dislike set: partywave:room:{roomId}:playlist:item:{playlistItemId}:dislikes
 *
 * Business rules:
 * - Each playlist item has two sets: one for likes, one for dislikes
 * - Each set contains user IDs who liked/disliked that item
 * - Like and dislike are mutually exclusive (user can only be in one set at a time)
 * - Statistics are runtime-only and cleaned up when rooms close
 *
 * Note: PostgreSQL app_user_stats updates should be handled by a higher-level service
 * that orchestrates both Redis and database operations. This service only handles Redis.
 */
@Service
public class LikeDislikeRedisService {

    private static final Logger log = LoggerFactory.getLogger(LikeDislikeRedisService.class);

    private final RedisTemplate<String, Object> redisTemplate;

    public LikeDislikeRedisService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ========================================
    // Key Building Methods
    // ========================================

    private String buildLikesKey(String roomId, String playlistItemId) {
        return CacheConfiguration.KEY_PREFIX + "room:" + roomId + ":playlist:item:" + playlistItemId + ":likes";
    }

    private String buildDislikesKey(String roomId, String playlistItemId) {
        return CacheConfiguration.KEY_PREFIX + "room:" + roomId + ":playlist:item:" + playlistItemId + ":dislikes";
    }

    // ========================================
    // Add Like/Dislike Operations
    // ========================================

    /**
     * Add a like for a playlist item from a user.
     * Ensures mutual exclusivity: removes user from dislikes before adding to likes.
     *
     * REDIS_ARCHITECTURE.md Section 2.1 - Write flow:
     * 1. Remove user from dislikes set (if present)
     * 2. Add user to likes set
     *
     * @param roomId Room UUID
     * @param playlistItemId Playlist item UUID
     * @param userId User UUID
     * @return true if like was added (false if user already liked)
     */
    public boolean addLike(String roomId, String playlistItemId, String userId) {
        try {
            String likesKey = buildLikesKey(roomId, playlistItemId);
            String dislikesKey = buildDislikesKey(roomId, playlistItemId);

            // Step 1: Remove from dislikes (ensure mutual exclusivity)
            redisTemplate.opsForSet().remove(dislikesKey, userId);

            // Step 2: Add to likes
            Long result = redisTemplate.opsForSet().add(likesKey, userId);

            boolean wasAdded = result != null && result > 0;
            if (wasAdded) {
                log.debug("User {} liked playlist item {} in room {}", userId, playlistItemId, roomId);
            } else {
                log.debug("User {} already liked playlist item {} in room {}", userId, playlistItemId, roomId);
            }

            return wasAdded;
        } catch (Exception e) {
            log.error("Failed to add like for playlist item {} in room {} by user {}", playlistItemId, roomId, userId, e);
            return false;
        }
    }

    /**
     * Add a dislike for a playlist item from a user.
     * Ensures mutual exclusivity: removes user from likes before adding to dislikes.
     *
     * REDIS_ARCHITECTURE.md Section 2.1 - Write flow:
     * 1. Remove user from likes set (if present)
     * 2. Add user to dislikes set
     *
     * @param roomId Room UUID
     * @param playlistItemId Playlist item UUID
     * @param userId User UUID
     * @return true if dislike was added (false if user already disliked)
     */
    public boolean addDislike(String roomId, String playlistItemId, String userId) {
        try {
            String likesKey = buildLikesKey(roomId, playlistItemId);
            String dislikesKey = buildDislikesKey(roomId, playlistItemId);

            // Step 1: Remove from likes (ensure mutual exclusivity)
            redisTemplate.opsForSet().remove(likesKey, userId);

            // Step 2: Add to dislikes
            Long result = redisTemplate.opsForSet().add(dislikesKey, userId);

            boolean wasAdded = result != null && result > 0;
            if (wasAdded) {
                log.debug("User {} disliked playlist item {} in room {}", userId, playlistItemId, roomId);
            } else {
                log.debug("User {} already disliked playlist item {} in room {}", userId, playlistItemId, roomId);
            }

            return wasAdded;
        } catch (Exception e) {
            log.error("Failed to add dislike for playlist item {} in room {} by user {}", playlistItemId, roomId, userId, e);
            return false;
        }
    }

    // ========================================
    // Remove Like/Dislike Operations
    // ========================================

    /**
     * Remove a like from a playlist item for a user.
     *
     * @param roomId Room UUID
     * @param playlistItemId Playlist item UUID
     * @param userId User UUID
     * @return true if like was removed (false if user hadn't liked)
     */
    public boolean removeLike(String roomId, String playlistItemId, String userId) {
        try {
            String likesKey = buildLikesKey(roomId, playlistItemId);
            Long result = redisTemplate.opsForSet().remove(likesKey, userId);

            boolean wasRemoved = result != null && result > 0;
            if (wasRemoved) {
                log.debug("Removed like from user {} for playlist item {} in room {}", userId, playlistItemId, roomId);
            } else {
                log.debug("User {} had not liked playlist item {} in room {}", userId, playlistItemId, roomId);
            }

            return wasRemoved;
        } catch (Exception e) {
            log.error("Failed to remove like for playlist item {} in room {} by user {}", playlistItemId, roomId, userId, e);
            return false;
        }
    }

    /**
     * Remove a dislike from a playlist item for a user.
     *
     * @param roomId Room UUID
     * @param playlistItemId Playlist item UUID
     * @param userId User UUID
     * @return true if dislike was removed (false if user hadn't disliked)
     */
    public boolean removeDislike(String roomId, String playlistItemId, String userId) {
        try {
            String dislikesKey = buildDislikesKey(roomId, playlistItemId);
            Long result = redisTemplate.opsForSet().remove(dislikesKey, userId);

            boolean wasRemoved = result != null && result > 0;
            if (wasRemoved) {
                log.debug("Removed dislike from user {} for playlist item {} in room {}", userId, playlistItemId, roomId);
            } else {
                log.debug("User {} had not disliked playlist item {} in room {}", userId, playlistItemId, roomId);
            }

            return wasRemoved;
        } catch (Exception e) {
            log.error("Failed to remove dislike for playlist item {} in room {} by user {}", playlistItemId, roomId, userId, e);
            return false;
        }
    }

    /**
     * Remove both like and dislike from a playlist item for a user.
     * Useful for clearing all user feedback on an item.
     *
     * REDIS_ARCHITECTURE.md Section 2.1 - Write flow (remove):
     * 1. Remove user from both sets
     *
     * @param roomId Room UUID
     * @param playlistItemId Playlist item UUID
     * @param userId User UUID
     * @return true if any feedback was removed
     */
    public boolean removeLikeAndDislike(String roomId, String playlistItemId, String userId) {
        try {
            String likesKey = buildLikesKey(roomId, playlistItemId);
            String dislikesKey = buildDislikesKey(roomId, playlistItemId);

            Long likesRemoved = redisTemplate.opsForSet().remove(likesKey, userId);
            Long dislikesRemoved = redisTemplate.opsForSet().remove(dislikesKey, userId);

            boolean anyRemoved = (likesRemoved != null && likesRemoved > 0) || (dislikesRemoved != null && dislikesRemoved > 0);

            if (anyRemoved) {
                log.debug("Removed all feedback from user {} for playlist item {} in room {}", userId, playlistItemId, roomId);
            } else {
                log.debug("User {} had no feedback on playlist item {} in room {}", userId, playlistItemId, roomId);
            }

            return anyRemoved;
        } catch (Exception e) {
            log.error("Failed to remove feedback for playlist item {} in room {} by user {}", playlistItemId, roomId, userId, e);
            return false;
        }
    }

    // ========================================
    // Count Operations
    // ========================================

    /**
     * Get the number of likes for a playlist item.
     *
     * REDIS_ARCHITECTURE.md Section 2.1 - Read flow:
     * SCARD partywave:room:{roomId}:playlist:item:{playlistItemId}:likes
     *
     * @param roomId Room UUID
     * @param playlistItemId Playlist item UUID
     * @return Number of likes (0 if set doesn't exist)
     */
    public long getLikeCount(String roomId, String playlistItemId) {
        try {
            String likesKey = buildLikesKey(roomId, playlistItemId);
            Long count = redisTemplate.opsForSet().size(likesKey);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.error("Failed to get like count for playlist item {} in room {}", playlistItemId, roomId, e);
            return 0L;
        }
    }

    /**
     * Get the number of dislikes for a playlist item.
     *
     * REDIS_ARCHITECTURE.md Section 2.1 - Read flow:
     * SCARD partywave:room:{roomId}:playlist:item:{playlistItemId}:dislikes
     *
     * @param roomId Room UUID
     * @param playlistItemId Playlist item UUID
     * @return Number of dislikes (0 if set doesn't exist)
     */
    public long getDislikeCount(String roomId, String playlistItemId) {
        try {
            String dislikesKey = buildDislikesKey(roomId, playlistItemId);
            Long count = redisTemplate.opsForSet().size(dislikesKey);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.error("Failed to get dislike count for playlist item {} in room {}", playlistItemId, roomId, e);
            return 0L;
        }
    }

    // ========================================
    // User Check Operations
    // ========================================

    /**
     * Check if a user has liked a playlist item.
     *
     * REDIS_ARCHITECTURE.md Section 2.1 - Read flow:
     * SISMEMBER partywave:room:{roomId}:playlist:item:{playlistItemId}:likes {userId}
     *
     * @param roomId Room UUID
     * @param playlistItemId Playlist item UUID
     * @param userId User UUID
     * @return true if user has liked the item
     */
    public boolean getLikedByUser(String roomId, String playlistItemId, String userId) {
        try {
            String likesKey = buildLikesKey(roomId, playlistItemId);
            Boolean isMember = redisTemplate.opsForSet().isMember(likesKey, userId);
            return isMember != null && isMember;
        } catch (Exception e) {
            log.error("Failed to check if user {} liked playlist item {} in room {}", userId, playlistItemId, roomId, e);
            return false;
        }
    }

    /**
     * Check if a user has disliked a playlist item.
     *
     * REDIS_ARCHITECTURE.md Section 2.1 - Read flow:
     * SISMEMBER partywave:room:{roomId}:playlist:item:{playlistItemId}:dislikes {userId}
     *
     * @param roomId Room UUID
     * @param playlistItemId Playlist item UUID
     * @param userId User UUID
     * @return true if user has disliked the item
     */
    public boolean getDislikedByUser(String roomId, String playlistItemId, String userId) {
        try {
            String dislikesKey = buildDislikesKey(roomId, playlistItemId);
            Boolean isMember = redisTemplate.opsForSet().isMember(dislikesKey, userId);
            return isMember != null && isMember;
        } catch (Exception e) {
            log.error("Failed to check if user {} disliked playlist item {} in room {}", userId, playlistItemId, roomId, e);
            return false;
        }
    }

    // ========================================
    // Bulk Operations
    // ========================================

    /**
     * Get all user IDs who liked a playlist item.
     *
     * @param roomId Room UUID
     * @param playlistItemId Playlist item UUID
     * @return Set of user IDs (empty set if none)
     */
    public Set<String> getAllLikes(String roomId, String playlistItemId) {
        try {
            String likesKey = buildLikesKey(roomId, playlistItemId);
            Set<Object> members = redisTemplate.opsForSet().members(likesKey);

            if (members == null || members.isEmpty()) {
                return Collections.emptySet();
            }

            return members.stream().map(Object::toString).collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("Failed to get all likes for playlist item {} in room {}", playlistItemId, roomId, e);
            return Collections.emptySet();
        }
    }

    /**
     * Get all user IDs who disliked a playlist item.
     *
     * @param roomId Room UUID
     * @param playlistItemId Playlist item UUID
     * @return Set of user IDs (empty set if none)
     */
    public Set<String> getAllDislikes(String roomId, String playlistItemId) {
        try {
            String dislikesKey = buildDislikesKey(roomId, playlistItemId);
            Set<Object> members = redisTemplate.opsForSet().members(dislikesKey);

            if (members == null || members.isEmpty()) {
                return Collections.emptySet();
            }

            return members.stream().map(Object::toString).collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("Failed to get all dislikes for playlist item {} in room {}", playlistItemId, roomId, e);
            return Collections.emptySet();
        }
    }

    // ========================================
    // Cleanup Operations
    // ========================================

    /**
     * Delete all like/dislike data for a playlist item.
     * Called during room cleanup (REDIS_ARCHITECTURE.md Section 5.3).
     *
     * Important: Ensure app_user_stats in PostgreSQL has been updated with final counts
     * before calling this method.
     *
     * @param roomId Room UUID
     * @param playlistItemId Playlist item UUID
     * @return true if any keys were deleted
     */
    public boolean deletePlaylistItemFeedback(String roomId, String playlistItemId) {
        try {
            String likesKey = buildLikesKey(roomId, playlistItemId);
            String dislikesKey = buildDislikesKey(roomId, playlistItemId);

            Boolean likesDeleted = redisTemplate.delete(likesKey);
            Boolean dislikesDeleted = redisTemplate.delete(dislikesKey);

            boolean anyDeleted = (likesDeleted != null && likesDeleted) || (dislikesDeleted != null && dislikesDeleted);

            if (anyDeleted) {
                log.debug("Deleted feedback data for playlist item {} in room {}", playlistItemId, roomId);
            }

            return anyDeleted;
        } catch (Exception e) {
            log.error("Failed to delete feedback for playlist item {} in room {}", playlistItemId, roomId, e);
            return false;
        }
    }

    /**
     * Delete all feedback data for all playlist items in a room.
     * Called during room deletion/cleanup (REDIS_ARCHITECTURE.md Section 5.3).
     *
     * Note: This requires iterating through all playlist items.
     * Consider calling this method with the list of playlist item IDs.
     *
     * @param roomId Room UUID
     * @param playlistItemIds List of playlist item UUIDs
     * @return Number of playlist items whose feedback was deleted
     */
    public int deleteAllRoomFeedback(String roomId, Set<String> playlistItemIds) {
        if (playlistItemIds == null || playlistItemIds.isEmpty()) {
            log.debug("No playlist items to clean up feedback for in room {}", roomId);
            return 0;
        }

        int deletedCount = 0;
        for (String playlistItemId : playlistItemIds) {
            if (deletePlaylistItemFeedback(roomId, playlistItemId)) {
                deletedCount++;
            }
        }

        log.info("Deleted feedback for {} playlist items in room {}", deletedCount, roomId);
        return deletedCount;
    }
}
