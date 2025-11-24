package com.partywave.backend.service;

import com.partywave.backend.domain.AppUser;
import com.partywave.backend.domain.AppUserStats;
import com.partywave.backend.exception.InvalidRequestException;
import com.partywave.backend.exception.ResourceNotFoundException;
import com.partywave.backend.repository.AppUserRepository;
import com.partywave.backend.repository.AppUserStatsRepository;
import com.partywave.backend.service.dto.LikeDislikeResponseDTO;
import com.partywave.backend.service.dto.PlaylistItemStatsEventDTO;
import com.partywave.backend.service.redis.LikeDislikeRedisService;
import com.partywave.backend.service.redis.PlaylistRedisService;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing like/dislike operations on playlist items.
 * Implements compensation pattern for race condition handling between Redis and PostgreSQL.
 *
 * Based on:
 * - PROJECT_OVERVIEW.md section 2.10 - Like / Dislike Tracks
 * - REDIS_ARCHITECTURE.md section 2.2 - Race Condition & Atomicity Problem
 *
 * Race Condition Handling (Compensation Pattern):
 * 1. Update PostgreSQL first (transactional)
 * 2. If PostgreSQL fails, return error (no Redis update)
 * 3. If PostgreSQL succeeds, update Redis atomically
 * 4. If Redis fails, compensate by reverting PostgreSQL update
 * 5. Log all failures for monitoring
 *
 * This ensures PostgreSQL (persistent state) remains consistent even if Redis (runtime state) fails.
 */
@Service
@Transactional
public class LikeDislikeService {

    private static final Logger log = LoggerFactory.getLogger(LikeDislikeService.class);

    private final PlaylistRedisService playlistRedisService;
    private final LikeDislikeRedisService likeDislikeRedisService;
    private final AppUserStatsRepository appUserStatsRepository;
    private final AppUserRepository appUserRepository;

    public LikeDislikeService(
        PlaylistRedisService playlistRedisService,
        LikeDislikeRedisService likeDislikeRedisService,
        AppUserStatsRepository appUserStatsRepository,
        AppUserRepository appUserRepository
    ) {
        this.playlistRedisService = playlistRedisService;
        this.likeDislikeRedisService = likeDislikeRedisService;
        this.appUserStatsRepository = appUserStatsRepository;
        this.appUserRepository = appUserRepository;
    }

    /**
     * Add a like to a playlist item.
     * Implements compensation pattern to handle Redis-PostgreSQL race conditions.
     *
     * Workflow:
     * 1. Validate playlist item exists in Redis
     * 2. Get track adder's user ID from playlist item
     * 3. Check if user previously liked/disliked
     * 4. Update PostgreSQL app_user_stats (transactional)
     * 5. Update Redis like/dislike sets
     * 6. If Redis fails, compensate by reverting PostgreSQL
     * 7. Return updated counts
     * 8. TODO: Emit WebSocket event PLAYLIST_ITEM_STATS_UPDATED
     *
     * @param roomId Room UUID
     * @param playlistItemId Playlist item UUID
     * @param userId User UUID (from JWT)
     * @return LikeDislikeResponseDTO with updated counts
     * @throws ResourceNotFoundException if playlist item or track adder not found
     * @throws InvalidRequestException if operation fails
     */
    public LikeDislikeResponseDTO likeTrack(UUID roomId, UUID playlistItemId, UUID userId) {
        String roomIdStr = roomId.toString();
        String playlistItemIdStr = playlistItemId.toString();
        String userIdStr = userId.toString();

        log.debug("User {} liking playlist item {} in room {}", userId, playlistItemId, roomId);

        // Step 1: Validate playlist item exists
        Map<Object, Object> playlistItem = playlistRedisService.getPlaylistItem(roomIdStr, playlistItemIdStr);
        if (playlistItem.isEmpty()) {
            throw new ResourceNotFoundException("Playlist item not found: " + playlistItemId);
        }

        // Step 2: Get track adder's user ID
        String addedByIdStr = getStringValue(playlistItem, "added_by_id");
        if (addedByIdStr == null) {
            throw new InvalidRequestException("Playlist item has no added_by_id field");
        }

        UUID addedById = UUID.fromString(addedByIdStr);
        log.debug("Track was added by user {}", addedById);

        // Step 3: Check if user previously liked/disliked
        boolean previouslyLiked = likeDislikeRedisService.getLikedByUser(roomIdStr, playlistItemIdStr, userIdStr);
        boolean previouslyDisliked = likeDislikeRedisService.getDislikedByUser(roomIdStr, playlistItemIdStr, userIdStr);

        // If user already liked, return current state
        if (previouslyLiked) {
            log.debug("User {} already liked playlist item {} in room {}", userId, playlistItemId, roomId);
            return buildResponse(roomIdStr, playlistItemIdStr, userIdStr, "Already liked");
        }

        // Step 4: Update PostgreSQL app_user_stats (transactional)
        try {
            updateStatsForLike(addedById, previouslyDisliked);
        } catch (Exception e) {
            log.error("Failed to update PostgreSQL stats for user {}: {}", addedById, e.getMessage(), e);
            throw new InvalidRequestException("Failed to update user statistics: " + e.getMessage());
        }

        // Step 5: Update Redis like/dislike sets
        boolean redisSuccess = likeDislikeRedisService.addLike(roomIdStr, playlistItemIdStr, userIdStr);

        if (!redisSuccess) {
            // Step 6: Compensate - revert PostgreSQL update
            log.error(
                "Redis addLike failed for playlist item {} in room {} by user {}. Compensating by reverting PostgreSQL update",
                playlistItemId,
                roomId,
                userId
            );

            try {
                compensateStatsForLike(addedById, previouslyDisliked);
                log.info("Successfully compensated PostgreSQL update for user {}", addedById);
            } catch (Exception compensationError) {
                log.error(
                    "CRITICAL: Failed to compensate PostgreSQL update for user {}: {}",
                    addedById,
                    compensationError.getMessage(),
                    compensationError
                );
                // Data is now inconsistent - log for manual intervention
            }

            throw new InvalidRequestException("Failed to update like in Redis. Operation rolled back.");
        }

        log.info("User {} liked playlist item {} in room {}", userId, playlistItemId, roomId);

        // Step 7: Return updated counts
        LikeDislikeResponseDTO response = buildResponse(roomIdStr, playlistItemIdStr, userIdStr, "Track liked successfully");

        // Step 8: Emit WebSocket event
        emitStatsUpdatedEvent(roomIdStr, playlistItemIdStr, response.getLikeCount(), response.getDislikeCount());

        return response;
    }

    /**
     * Add a dislike to a playlist item.
     * Implements compensation pattern to handle Redis-PostgreSQL race conditions.
     *
     * @param roomId Room UUID
     * @param playlistItemId Playlist item UUID
     * @param userId User UUID (from JWT)
     * @return LikeDislikeResponseDTO with updated counts
     * @throws ResourceNotFoundException if playlist item or track adder not found
     * @throws InvalidRequestException if operation fails
     */
    public LikeDislikeResponseDTO dislikeTrack(UUID roomId, UUID playlistItemId, UUID userId) {
        String roomIdStr = roomId.toString();
        String playlistItemIdStr = playlistItemId.toString();
        String userIdStr = userId.toString();

        log.debug("User {} disliking playlist item {} in room {}", userId, playlistItemId, roomId);

        // Step 1: Validate playlist item exists
        Map<Object, Object> playlistItem = playlistRedisService.getPlaylistItem(roomIdStr, playlistItemIdStr);
        if (playlistItem.isEmpty()) {
            throw new ResourceNotFoundException("Playlist item not found: " + playlistItemId);
        }

        // Step 2: Get track adder's user ID
        String addedByIdStr = getStringValue(playlistItem, "added_by_id");
        if (addedByIdStr == null) {
            throw new InvalidRequestException("Playlist item has no added_by_id field");
        }

        UUID addedById = UUID.fromString(addedByIdStr);
        log.debug("Track was added by user {}", addedById);

        // Step 3: Check if user previously liked/disliked
        boolean previouslyLiked = likeDislikeRedisService.getLikedByUser(roomIdStr, playlistItemIdStr, userIdStr);
        boolean previouslyDisliked = likeDislikeRedisService.getDislikedByUser(roomIdStr, playlistItemIdStr, userIdStr);

        // If user already disliked, return current state
        if (previouslyDisliked) {
            log.debug("User {} already disliked playlist item {} in room {}", userId, playlistItemId, roomId);
            return buildResponse(roomIdStr, playlistItemIdStr, userIdStr, "Already disliked");
        }

        // Step 4: Update PostgreSQL app_user_stats (transactional)
        try {
            updateStatsForDislike(addedById, previouslyLiked);
        } catch (Exception e) {
            log.error("Failed to update PostgreSQL stats for user {}: {}", addedById, e.getMessage(), e);
            throw new InvalidRequestException("Failed to update user statistics: " + e.getMessage());
        }

        // Step 5: Update Redis like/dislike sets
        boolean redisSuccess = likeDislikeRedisService.addDislike(roomIdStr, playlistItemIdStr, userIdStr);

        if (!redisSuccess) {
            // Step 6: Compensate - revert PostgreSQL update
            log.error(
                "Redis addDislike failed for playlist item {} in room {} by user {}. Compensating by reverting PostgreSQL update",
                playlistItemId,
                roomId,
                userId
            );

            try {
                compensateStatsForDislike(addedById, previouslyLiked);
                log.info("Successfully compensated PostgreSQL update for user {}", addedById);
            } catch (Exception compensationError) {
                log.error(
                    "CRITICAL: Failed to compensate PostgreSQL update for user {}: {}",
                    addedById,
                    compensationError.getMessage(),
                    compensationError
                );
                // Data is now inconsistent - log for manual intervention
            }

            throw new InvalidRequestException("Failed to update dislike in Redis. Operation rolled back.");
        }

        log.info("User {} disliked playlist item {} in room {}", userId, playlistItemId, roomId);

        // Step 7: Return updated counts
        LikeDislikeResponseDTO response = buildResponse(roomIdStr, playlistItemIdStr, userIdStr, "Track disliked successfully");

        // Step 8: Emit WebSocket event
        emitStatsUpdatedEvent(roomIdStr, playlistItemIdStr, response.getLikeCount(), response.getDislikeCount());

        return response;
    }

    /**
     * Remove a like from a playlist item.
     *
     * @param roomId Room UUID
     * @param playlistItemId Playlist item UUID
     * @param userId User UUID (from JWT)
     * @return LikeDislikeResponseDTO with updated counts
     * @throws ResourceNotFoundException if playlist item or track adder not found
     * @throws InvalidRequestException if operation fails
     */
    public LikeDislikeResponseDTO unlikeTrack(UUID roomId, UUID playlistItemId, UUID userId) {
        String roomIdStr = roomId.toString();
        String playlistItemIdStr = playlistItemId.toString();
        String userIdStr = userId.toString();

        log.debug("User {} removing like from playlist item {} in room {}", userId, playlistItemId, roomId);

        // Step 1: Validate playlist item exists
        Map<Object, Object> playlistItem = playlistRedisService.getPlaylistItem(roomIdStr, playlistItemIdStr);
        if (playlistItem.isEmpty()) {
            throw new ResourceNotFoundException("Playlist item not found: " + playlistItemId);
        }

        // Step 2: Get track adder's user ID
        String addedByIdStr = getStringValue(playlistItem, "added_by_id");
        if (addedByIdStr == null) {
            throw new InvalidRequestException("Playlist item has no added_by_id field");
        }

        UUID addedById = UUID.fromString(addedByIdStr);

        // Step 3: Check if user has liked
        boolean previouslyLiked = likeDislikeRedisService.getLikedByUser(roomIdStr, playlistItemIdStr, userIdStr);

        if (!previouslyLiked) {
            log.debug("User {} has not liked playlist item {} in room {}", userId, playlistItemId, roomId);
            return buildResponse(roomIdStr, playlistItemIdStr, userIdStr, "Not liked");
        }

        // Step 4: Update PostgreSQL app_user_stats
        try {
            AppUser trackAdder = appUserRepository
                .findById(addedById)
                .orElseThrow(() -> new ResourceNotFoundException("Track adder not found: " + addedById));

            AppUserStats stats = trackAdder.getStats();
            if (stats == null) {
                throw new InvalidRequestException("Track adder has no stats record");
            }

            // Decrement total_like
            int currentLikes = stats.getTotalLike() != null ? stats.getTotalLike() : 0;
            stats.setTotalLike(Math.max(0, currentLikes - 1));
            appUserStatsRepository.save(stats);

            log.debug("Decremented total_like for user {} (new value: {})", addedById, stats.getTotalLike());
        } catch (Exception e) {
            log.error("Failed to update PostgreSQL stats for user {}: {}", addedById, e.getMessage(), e);
            throw new InvalidRequestException("Failed to update user statistics: " + e.getMessage());
        }

        // Step 5: Update Redis
        boolean redisSuccess = likeDislikeRedisService.removeLike(roomIdStr, playlistItemIdStr, userIdStr);

        if (!redisSuccess) {
            // Compensate
            log.error("Redis removeLike failed. Compensating by reverting PostgreSQL update");

            try {
                AppUser trackAdder = appUserRepository.findById(addedById).orElseThrow();
                AppUserStats stats = trackAdder.getStats();
                int currentLikes = stats.getTotalLike() != null ? stats.getTotalLike() : 0;
                stats.setTotalLike(currentLikes + 1);
                appUserStatsRepository.save(stats);
                log.info("Successfully compensated PostgreSQL update for user {}", addedById);
            } catch (Exception compensationError) {
                log.error(
                    "CRITICAL: Failed to compensate PostgreSQL update for user {}: {}",
                    addedById,
                    compensationError.getMessage(),
                    compensationError
                );
            }

            throw new InvalidRequestException("Failed to remove like in Redis. Operation rolled back.");
        }

        log.info("User {} removed like from playlist item {} in room {}", userId, playlistItemId, roomId);

        // Return updated counts
        LikeDislikeResponseDTO response = buildResponse(roomIdStr, playlistItemIdStr, userIdStr, "Like removed successfully");

        // Emit WebSocket event
        emitStatsUpdatedEvent(roomIdStr, playlistItemIdStr, response.getLikeCount(), response.getDislikeCount());

        return response;
    }

    /**
     * Remove a dislike from a playlist item.
     *
     * @param roomId Room UUID
     * @param playlistItemId Playlist item UUID
     * @param userId User UUID (from JWT)
     * @return LikeDislikeResponseDTO with updated counts
     * @throws ResourceNotFoundException if playlist item or track adder not found
     * @throws InvalidRequestException if operation fails
     */
    public LikeDislikeResponseDTO undislikeTrack(UUID roomId, UUID playlistItemId, UUID userId) {
        String roomIdStr = roomId.toString();
        String playlistItemIdStr = playlistItemId.toString();
        String userIdStr = userId.toString();

        log.debug("User {} removing dislike from playlist item {} in room {}", userId, playlistItemId, roomId);

        // Step 1: Validate playlist item exists
        Map<Object, Object> playlistItem = playlistRedisService.getPlaylistItem(roomIdStr, playlistItemIdStr);
        if (playlistItem.isEmpty()) {
            throw new ResourceNotFoundException("Playlist item not found: " + playlistItemId);
        }

        // Step 2: Get track adder's user ID
        String addedByIdStr = getStringValue(playlistItem, "added_by_id");
        if (addedByIdStr == null) {
            throw new InvalidRequestException("Playlist item has no added_by_id field");
        }

        UUID addedById = UUID.fromString(addedByIdStr);

        // Step 3: Check if user has disliked
        boolean previouslyDisliked = likeDislikeRedisService.getDislikedByUser(roomIdStr, playlistItemIdStr, userIdStr);

        if (!previouslyDisliked) {
            log.debug("User {} has not disliked playlist item {} in room {}", userId, playlistItemId, roomId);
            return buildResponse(roomIdStr, playlistItemIdStr, userIdStr, "Not disliked");
        }

        // Step 4: Update PostgreSQL app_user_stats
        try {
            AppUser trackAdder = appUserRepository
                .findById(addedById)
                .orElseThrow(() -> new ResourceNotFoundException("Track adder not found: " + addedById));

            AppUserStats stats = trackAdder.getStats();
            if (stats == null) {
                throw new InvalidRequestException("Track adder has no stats record");
            }

            // Decrement total_dislike
            int currentDislikes = stats.getTotalDislike() != null ? stats.getTotalDislike() : 0;
            stats.setTotalDislike(Math.max(0, currentDislikes - 1));
            appUserStatsRepository.save(stats);

            log.debug("Decremented total_dislike for user {} (new value: {})", addedById, stats.getTotalDislike());
        } catch (Exception e) {
            log.error("Failed to update PostgreSQL stats for user {}: {}", addedById, e.getMessage(), e);
            throw new InvalidRequestException("Failed to update user statistics: " + e.getMessage());
        }

        // Step 5: Update Redis
        boolean redisSuccess = likeDislikeRedisService.removeDislike(roomIdStr, playlistItemIdStr, userIdStr);

        if (!redisSuccess) {
            // Compensate
            log.error("Redis removeDislike failed. Compensating by reverting PostgreSQL update");

            try {
                AppUser trackAdder = appUserRepository.findById(addedById).orElseThrow();
                AppUserStats stats = trackAdder.getStats();
                int currentDislikes = stats.getTotalDislike() != null ? stats.getTotalDislike() : 0;
                stats.setTotalDislike(currentDislikes + 1);
                appUserStatsRepository.save(stats);
                log.info("Successfully compensated PostgreSQL update for user {}", addedById);
            } catch (Exception compensationError) {
                log.error(
                    "CRITICAL: Failed to compensate PostgreSQL update for user {}: {}",
                    addedById,
                    compensationError.getMessage(),
                    compensationError
                );
            }

            throw new InvalidRequestException("Failed to remove dislike in Redis. Operation rolled back.");
        }

        log.info("User {} removed dislike from playlist item {} in room {}", userId, playlistItemId, roomId);

        // Return updated counts
        LikeDislikeResponseDTO response = buildResponse(roomIdStr, playlistItemIdStr, userIdStr, "Dislike removed successfully");

        // Emit WebSocket event
        emitStatsUpdatedEvent(roomIdStr, playlistItemIdStr, response.getLikeCount(), response.getDislikeCount());

        return response;
    }

    // ========================================
    // Helper Methods
    // ========================================

    /**
     * Update PostgreSQL stats for a like operation.
     * Increments total_like, decrements total_dislike if user previously disliked.
     */
    private void updateStatsForLike(UUID addedById, boolean previouslyDisliked) {
        AppUser trackAdder = appUserRepository
            .findById(addedById)
            .orElseThrow(() -> new ResourceNotFoundException("Track adder not found: " + addedById));

        AppUserStats stats = trackAdder.getStats();
        if (stats == null) {
            throw new InvalidRequestException("Track adder has no stats record");
        }

        // Increment total_like
        int currentLikes = stats.getTotalLike() != null ? stats.getTotalLike() : 0;
        stats.setTotalLike(currentLikes + 1);

        // If user previously disliked, decrement total_dislike
        if (previouslyDisliked) {
            int currentDislikes = stats.getTotalDislike() != null ? stats.getTotalDislike() : 0;
            stats.setTotalDislike(Math.max(0, currentDislikes - 1));
            log.debug("User switched from dislike to like. Incrementing total_like and decrementing total_dislike for user {}", addedById);
        } else {
            log.debug("Incrementing total_like for user {}", addedById);
        }

        appUserStatsRepository.save(stats);
    }

    /**
     * Compensate PostgreSQL stats for a failed like operation.
     * Reverts the changes made by updateStatsForLike.
     */
    private void compensateStatsForLike(UUID addedById, boolean previouslyDisliked) {
        AppUser trackAdder = appUserRepository.findById(addedById).orElseThrow();
        AppUserStats stats = trackAdder.getStats();

        // Decrement total_like
        int currentLikes = stats.getTotalLike() != null ? stats.getTotalLike() : 0;
        stats.setTotalLike(Math.max(0, currentLikes - 1));

        // If user previously disliked, increment total_dislike back
        if (previouslyDisliked) {
            int currentDislikes = stats.getTotalDislike() != null ? stats.getTotalDislike() : 0;
            stats.setTotalDislike(currentDislikes + 1);
        }

        appUserStatsRepository.save(stats);
    }

    /**
     * Update PostgreSQL stats for a dislike operation.
     * Increments total_dislike, decrements total_like if user previously liked.
     */
    private void updateStatsForDislike(UUID addedById, boolean previouslyLiked) {
        AppUser trackAdder = appUserRepository
            .findById(addedById)
            .orElseThrow(() -> new ResourceNotFoundException("Track adder not found: " + addedById));

        AppUserStats stats = trackAdder.getStats();
        if (stats == null) {
            throw new InvalidRequestException("Track adder has no stats record");
        }

        // Increment total_dislike
        int currentDislikes = stats.getTotalDislike() != null ? stats.getTotalDislike() : 0;
        stats.setTotalDislike(currentDislikes + 1);

        // If user previously liked, decrement total_like
        if (previouslyLiked) {
            int currentLikes = stats.getTotalLike() != null ? stats.getTotalLike() : 0;
            stats.setTotalLike(Math.max(0, currentLikes - 1));
            log.debug("User switched from like to dislike. Incrementing total_dislike and decrementing total_like for user {}", addedById);
        } else {
            log.debug("Incrementing total_dislike for user {}", addedById);
        }

        appUserStatsRepository.save(stats);
    }

    /**
     * Compensate PostgreSQL stats for a failed dislike operation.
     * Reverts the changes made by updateStatsForDislike.
     */
    private void compensateStatsForDislike(UUID addedById, boolean previouslyLiked) {
        AppUser trackAdder = appUserRepository.findById(addedById).orElseThrow();
        AppUserStats stats = trackAdder.getStats();

        // Decrement total_dislike
        int currentDislikes = stats.getTotalDislike() != null ? stats.getTotalDislike() : 0;
        stats.setTotalDislike(Math.max(0, currentDislikes - 1));

        // If user previously liked, increment total_like back
        if (previouslyLiked) {
            int currentLikes = stats.getTotalLike() != null ? stats.getTotalLike() : 0;
            stats.setTotalLike(currentLikes + 1);
        }

        appUserStatsRepository.save(stats);
    }

    /**
     * Build response DTO with current like/dislike counts.
     */
    private LikeDislikeResponseDTO buildResponse(String roomId, String playlistItemId, String userId, String message) {
        long likeCount = likeDislikeRedisService.getLikeCount(roomId, playlistItemId);
        long dislikeCount = likeDislikeRedisService.getDislikeCount(roomId, playlistItemId);
        boolean userLiked = likeDislikeRedisService.getLikedByUser(roomId, playlistItemId, userId);
        boolean userDisliked = likeDislikeRedisService.getDislikedByUser(roomId, playlistItemId, userId);

        return new LikeDislikeResponseDTO(roomId, playlistItemId, likeCount, dislikeCount, userLiked, userDisliked, message);
    }

    /**
     * Emit WebSocket event for stats update.
     * TODO: Implement WebSocket event emission once Spring WebSocket is configured.
     */
    private void emitStatsUpdatedEvent(String roomId, String playlistItemId, Long likeCount, Long dislikeCount) {
        try {
            PlaylistItemStatsEventDTO event = new PlaylistItemStatsEventDTO(roomId, playlistItemId, likeCount, dislikeCount);

            // TODO: Emit WebSocket event once Spring WebSocket (SimpMessagingTemplate) is configured
            // String destination = "/topic/room/" + roomId;
            // messagingTemplate.convertAndSend(destination, event);

            log.info(
                "TODO: Emit PLAYLIST_ITEM_STATS_UPDATED WebSocket event for item {} in room {}. Event data: like_count={}, dislike_count={}",
                playlistItemId,
                roomId,
                likeCount,
                dislikeCount
            );
        } catch (Exception e) {
            log.error("Failed to prepare PLAYLIST_ITEM_STATS_UPDATED event for item {} in room {}", playlistItemId, roomId, e);
        }
    }

    /**
     * Helper method to safely get String value from Redis hash map.
     */
    private String getStringValue(Map<Object, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}
