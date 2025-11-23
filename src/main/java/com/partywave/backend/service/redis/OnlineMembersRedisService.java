package com.partywave.backend.service.redis;

import com.partywave.backend.config.CacheConfiguration;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis service for managing online room members.
 * Based on REDIS_ARCHITECTURE.md specifications.
 *
 * Key structure:
 * - Online members set: partywave:room:{roomId}:members:online
 *
 * Business rules:
 * - Tracks users currently online in a room (active WebSocket connections)
 * - Used for displaying online counts and vote thresholds
 * - Purely runtime state (rebuilt as users reconnect)
 */
@Service
public class OnlineMembersRedisService {

    private static final Logger log = LoggerFactory.getLogger(OnlineMembersRedisService.class);

    private final RedisTemplate<String, Object> redisTemplate;

    public OnlineMembersRedisService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ========================================
    // Key Building Methods
    // ========================================

    private String buildOnlineMembersKey(String roomId) {
        return CacheConfiguration.KEY_PREFIX + "room:" + roomId + ":members:online";
    }

    // ========================================
    // Online Members Operations
    // ========================================

    /**
     * Add a user to the online members set for a room.
     * Called when user joins a room (WebSocket connection established).
     *
     * @param roomId Room UUID
     * @param userId User UUID
     * @return true if user was added (false if already present)
     */
    public boolean addOnlineMember(String roomId, String userId) {
        try {
            String onlineMembersKey = buildOnlineMembersKey(roomId);
            Long result = redisTemplate.opsForSet().add(onlineMembersKey, userId);

            boolean wasAdded = result != null && result > 0;
            if (wasAdded) {
                log.debug("User {} joined room {} (now online)", userId, roomId);
            } else {
                log.debug("User {} was already online in room {}", userId, roomId);
            }

            return wasAdded;
        } catch (Exception e) {
            log.error("Failed to add online member {} to room {}", userId, roomId, e);
            return false;
        }
    }

    /**
     * Remove a user from the online members set for a room.
     * Called when user leaves a room or WebSocket disconnects.
     *
     * @param roomId Room UUID
     * @param userId User UUID
     * @return true if user was removed (false if not present)
     */
    public boolean removeOnlineMember(String roomId, String userId) {
        try {
            String onlineMembersKey = buildOnlineMembersKey(roomId);
            Long result = redisTemplate.opsForSet().remove(onlineMembersKey, userId);

            boolean wasRemoved = result != null && result > 0;
            if (wasRemoved) {
                log.debug("User {} left room {} (now offline)", userId, roomId);
            } else {
                log.debug("User {} was not online in room {}", userId, roomId);
            }

            return wasRemoved;
        } catch (Exception e) {
            log.error("Failed to remove online member {} from room {}", userId, roomId, e);
            return false;
        }
    }

    /**
     * Check if a user is online in a room.
     *
     * @param roomId Room UUID
     * @param userId User UUID
     * @return true if user is online
     */
    public boolean isUserOnline(String roomId, String userId) {
        String onlineMembersKey = buildOnlineMembersKey(roomId);
        Boolean isOnline = redisTemplate.opsForSet().isMember(onlineMembersKey, userId);
        return Boolean.TRUE.equals(isOnline);
    }

    /**
     * Get the count of online members in a room.
     *
     * @param roomId Room UUID
     * @return Number of online members
     */
    public long getOnlineMemberCount(String roomId) {
        String onlineMembersKey = buildOnlineMembersKey(roomId);
        Long count = redisTemplate.opsForSet().size(onlineMembersKey);
        return count != null ? count : 0L;
    }

    /**
     * Get all online member IDs for a room.
     *
     * @param roomId Room UUID
     * @return Set of user UUIDs (as strings)
     */
    public Set<String> getOnlineMembers(String roomId) {
        try {
            String onlineMembersKey = buildOnlineMembersKey(roomId);
            Set<Object> members = redisTemplate.opsForSet().members(onlineMembersKey);

            if (members == null || members.isEmpty()) {
                return Collections.emptySet();
            }

            return members.stream().map(Object::toString).collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("Failed to get online members for room {}", roomId, e);
            return Collections.emptySet();
        }
    }

    /**
     * Check if any users are online in a room.
     *
     * @param roomId Room UUID
     * @return true if at least one user is online
     */
    public boolean hasOnlineMembers(String roomId) {
        return getOnlineMemberCount(roomId) > 0;
    }

    /**
     * Remove all online members from a room.
     * Used when room is being closed or reset.
     *
     * @param roomId Room UUID
     * @return Number of members removed
     */
    public long clearOnlineMembers(String roomId) {
        try {
            String onlineMembersKey = buildOnlineMembersKey(roomId);
            long count = getOnlineMemberCount(roomId);
            redisTemplate.delete(onlineMembersKey);

            log.debug("Cleared {} online members from room {}", count, roomId);
            return count;
        } catch (Exception e) {
            log.error("Failed to clear online members for room {}", roomId, e);
            return 0;
        }
    }

    // ========================================
    // Cleanup Operations
    // ========================================

    /**
     * Delete online members set for a room.
     * Used during room cleanup/deletion.
     *
     * @param roomId Room UUID
     */
    public void deleteOnlineMembers(String roomId) {
        try {
            String onlineMembersKey = buildOnlineMembersKey(roomId);
            redisTemplate.delete(onlineMembersKey);

            log.info("Deleted online members for room {}", roomId);
        } catch (Exception e) {
            log.error("Failed to delete online members for room {}", roomId, e);
        }
    }

    /**
     * Set TTL for online members set.
     * Used when room becomes inactive.
     *
     * @param roomId Room UUID
     * @param ttlSeconds TTL in seconds
     */
    public void setOnlineMembersTTL(String roomId, long ttlSeconds) {
        try {
            String onlineMembersKey = buildOnlineMembersKey(roomId);
            redisTemplate.expire(onlineMembersKey, ttlSeconds, TimeUnit.SECONDS);

            log.debug("Set TTL of {} seconds for online members in room {}", ttlSeconds, roomId);
        } catch (Exception e) {
            log.error("Failed to set TTL for online members in room {}", roomId, e);
        }
    }

    // ========================================
    // Batch Operations
    // ========================================

    /**
     * Add multiple users to the online members set.
     * Useful for bulk operations.
     *
     * @param roomId Room UUID
     * @param userIds Set of user UUIDs
     * @return Number of users added
     */
    public long addOnlineMembers(String roomId, Set<String> userIds) {
        try {
            if (userIds == null || userIds.isEmpty()) {
                return 0;
            }

            String onlineMembersKey = buildOnlineMembersKey(roomId);
            Long result = redisTemplate.opsForSet().add(onlineMembersKey, userIds.toArray());

            long added = result != null ? result : 0;
            log.debug("Added {} users to online members in room {}", added, roomId);
            return added;
        } catch (Exception e) {
            log.error("Failed to add multiple online members to room {}", roomId, e);
            return 0;
        }
    }

    /**
     * Remove multiple users from the online members set.
     * Useful for bulk operations.
     *
     * @param roomId Room UUID
     * @param userIds Set of user UUIDs
     * @return Number of users removed
     */
    public long removeOnlineMembers(String roomId, Set<String> userIds) {
        try {
            if (userIds == null || userIds.isEmpty()) {
                return 0;
            }

            String onlineMembersKey = buildOnlineMembersKey(roomId);
            Long result = redisTemplate.opsForSet().remove(onlineMembersKey, userIds.toArray());

            long removed = result != null ? result : 0;
            log.debug("Removed {} users from online members in room {}", removed, roomId);
            return removed;
        } catch (Exception e) {
            log.error("Failed to remove multiple online members from room {}", roomId, e);
            return 0;
        }
    }
}
