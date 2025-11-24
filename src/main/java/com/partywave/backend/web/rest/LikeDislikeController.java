package com.partywave.backend.web.rest;

import com.partywave.backend.exception.ForbiddenException;
import com.partywave.backend.exception.ResourceNotFoundException;
import com.partywave.backend.repository.RoomMemberRepository;
import com.partywave.backend.service.LikeDislikeService;
import com.partywave.backend.service.dto.LikeDislikeResponseDTO;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing like/dislike operations on playlist items.
 * Provides endpoints for liking, disliking, and removing likes/dislikes.
 *
 * Based on PROJECT_OVERVIEW.md section 2.10 - Like / Dislike Tracks.
 *
 * All endpoints require:
 * - JWT authentication
 * - Active room membership
 * - Valid playlist item in Redis
 */
@RestController
@RequestMapping("/api/rooms/{roomId}/playlist/{playlistItemId}")
public class LikeDislikeController {

    private static final Logger LOG = LoggerFactory.getLogger(LikeDislikeController.class);

    private final LikeDislikeService likeDislikeService;
    private final RoomMemberRepository roomMemberRepository;

    public LikeDislikeController(LikeDislikeService likeDislikeService, RoomMemberRepository roomMemberRepository) {
        this.likeDislikeService = likeDislikeService;
        this.roomMemberRepository = roomMemberRepository;
    }

    /**
     * POST /api/rooms/{roomId}/playlist/{playlistItemId}/like : Like a playlist item
     *
     * Adds a like to a playlist item. If user previously disliked, the dislike is removed.
     * Updates both Redis (runtime state) and PostgreSQL (persistent user statistics).
     *
     * Workflow:
     * 1. Validates user is a room member
     * 2. Validates playlist item exists in Redis
     * 3. Gets track adder's user ID from playlist item
     * 4. Updates PostgreSQL app_user_stats (increment total_like, decrement total_dislike if needed)
     * 5. Updates Redis like/dislike sets (add to likes, remove from dislikes)
     * 6. If Redis fails, compensates by reverting PostgreSQL update
     * 7. Returns updated like/dislike counts
     * 8. TODO: Emits WebSocket event PLAYLIST_ITEM_STATS_UPDATED
     *
     * @param roomId Room ID (UUID)
     * @param playlistItemId Playlist item ID (UUID)
     * @return ResponseEntity with LikeDislikeResponseDTO containing updated counts
     * @throws ResourceNotFoundException if room or playlist item doesn't exist
     * @throws ForbiddenException if user is not a room member
     */
    @PostMapping("/like")
    public ResponseEntity<LikeDislikeResponseDTO> likeTrack(@PathVariable UUID roomId, @PathVariable UUID playlistItemId) {
        UUID userId = getCurrentUserId();
        LOG.debug("REST request to like playlist item {} in room {} by user {}", playlistItemId, roomId, userId);

        // Validate room membership
        validateRoomMembership(roomId, userId);

        // Delegate to service
        LikeDislikeResponseDTO response = likeDislikeService.likeTrack(roomId, playlistItemId, userId);

        LOG.info(
            "User {} liked playlist item {} in room {}. Counts: {} likes, {} dislikes",
            userId,
            playlistItemId,
            roomId,
            response.getLikeCount(),
            response.getDislikeCount()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/rooms/{roomId}/playlist/{playlistItemId}/dislike : Dislike a playlist item
     *
     * Adds a dislike to a playlist item. If user previously liked, the like is removed.
     * Updates both Redis (runtime state) and PostgreSQL (persistent user statistics).
     *
     * Workflow:
     * 1. Validates user is a room member
     * 2. Validates playlist item exists in Redis
     * 3. Gets track adder's user ID from playlist item
     * 4. Updates PostgreSQL app_user_stats (increment total_dislike, decrement total_like if needed)
     * 5. Updates Redis like/dislike sets (add to dislikes, remove from likes)
     * 6. If Redis fails, compensates by reverting PostgreSQL update
     * 7. Returns updated like/dislike counts
     * 8. TODO: Emits WebSocket event PLAYLIST_ITEM_STATS_UPDATED
     *
     * @param roomId Room ID (UUID)
     * @param playlistItemId Playlist item ID (UUID)
     * @return ResponseEntity with LikeDislikeResponseDTO containing updated counts
     * @throws ResourceNotFoundException if room or playlist item doesn't exist
     * @throws ForbiddenException if user is not a room member
     */
    @PostMapping("/dislike")
    public ResponseEntity<LikeDislikeResponseDTO> dislikeTrack(@PathVariable UUID roomId, @PathVariable UUID playlistItemId) {
        UUID userId = getCurrentUserId();
        LOG.debug("REST request to dislike playlist item {} in room {} by user {}", playlistItemId, roomId, userId);

        // Validate room membership
        validateRoomMembership(roomId, userId);

        // Delegate to service
        LikeDislikeResponseDTO response = likeDislikeService.dislikeTrack(roomId, playlistItemId, userId);

        LOG.info(
            "User {} disliked playlist item {} in room {}. Counts: {} likes, {} dislikes",
            userId,
            playlistItemId,
            roomId,
            response.getLikeCount(),
            response.getDislikeCount()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/rooms/{roomId}/playlist/{playlistItemId}/like : Remove like from playlist item
     *
     * Removes user's like from a playlist item.
     * Updates both Redis (runtime state) and PostgreSQL (persistent user statistics).
     *
     * Workflow:
     * 1. Validates user is a room member
     * 2. Validates playlist item exists in Redis
     * 3. Checks if user has liked the item
     * 4. Updates PostgreSQL app_user_stats (decrement total_like)
     * 5. Updates Redis like set (remove user)
     * 6. If Redis fails, compensates by reverting PostgreSQL update
     * 7. Returns updated like/dislike counts
     * 8. TODO: Emits WebSocket event PLAYLIST_ITEM_STATS_UPDATED
     *
     * @param roomId Room ID (UUID)
     * @param playlistItemId Playlist item ID (UUID)
     * @return ResponseEntity with LikeDislikeResponseDTO containing updated counts
     * @throws ResourceNotFoundException if room or playlist item doesn't exist
     * @throws ForbiddenException if user is not a room member
     */
    @DeleteMapping("/like")
    public ResponseEntity<LikeDislikeResponseDTO> unlikeTrack(@PathVariable UUID roomId, @PathVariable UUID playlistItemId) {
        UUID userId = getCurrentUserId();
        LOG.debug("REST request to remove like from playlist item {} in room {} by user {}", playlistItemId, roomId, userId);

        // Validate room membership
        validateRoomMembership(roomId, userId);

        // Delegate to service
        LikeDislikeResponseDTO response = likeDislikeService.unlikeTrack(roomId, playlistItemId, userId);

        LOG.info(
            "User {} removed like from playlist item {} in room {}. Counts: {} likes, {} dislikes",
            userId,
            playlistItemId,
            roomId,
            response.getLikeCount(),
            response.getDislikeCount()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/rooms/{roomId}/playlist/{playlistItemId}/dislike : Remove dislike from playlist item
     *
     * Removes user's dislike from a playlist item.
     * Updates both Redis (runtime state) and PostgreSQL (persistent user statistics).
     *
     * Workflow:
     * 1. Validates user is a room member
     * 2. Validates playlist item exists in Redis
     * 3. Checks if user has disliked the item
     * 4. Updates PostgreSQL app_user_stats (decrement total_dislike)
     * 5. Updates Redis dislike set (remove user)
     * 6. If Redis fails, compensates by reverting PostgreSQL update
     * 7. Returns updated like/dislike counts
     * 8. TODO: Emits WebSocket event PLAYLIST_ITEM_STATS_UPDATED
     *
     * @param roomId Room ID (UUID)
     * @param playlistItemId Playlist item ID (UUID)
     * @return ResponseEntity with LikeDislikeResponseDTO containing updated counts
     * @throws ResourceNotFoundException if room or playlist item doesn't exist
     * @throws ForbiddenException if user is not a room member
     */
    @DeleteMapping("/dislike")
    public ResponseEntity<LikeDislikeResponseDTO> undislikeTrack(@PathVariable UUID roomId, @PathVariable UUID playlistItemId) {
        UUID userId = getCurrentUserId();
        LOG.debug("REST request to remove dislike from playlist item {} in room {} by user {}", playlistItemId, roomId, userId);

        // Validate room membership
        validateRoomMembership(roomId, userId);

        // Delegate to service
        LikeDislikeResponseDTO response = likeDislikeService.undislikeTrack(roomId, playlistItemId, userId);

        LOG.info(
            "User {} removed dislike from playlist item {} in room {}. Counts: {} likes, {} dislikes",
            userId,
            playlistItemId,
            roomId,
            response.getLikeCount(),
            response.getDislikeCount()
        );

        return ResponseEntity.ok(response);
    }

    // ========================================
    // Helper Methods
    // ========================================

    /**
     * Get current authenticated user ID from JWT token.
     * Extracts the 'sub' claim from the JWT which contains the user UUID.
     *
     * @return User UUID
     * @throws IllegalStateException if no authentication or JWT not found
     */
    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            throw new IllegalStateException("No JWT authentication found");
        }

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userIdStr = jwt.getSubject(); // 'sub' claim contains user UUID
        return UUID.fromString(userIdStr);
    }

    /**
     * Validate that user is an active member of the room.
     *
     * @param roomId Room UUID
     * @param userId User UUID
     * @throws ForbiddenException if user is not a room member
     */
    private void validateRoomMembership(UUID roomId, UUID userId) {
        boolean isMember = roomMemberRepository.existsByRoomIdAndUserIdAndIsActiveTrue(roomId, userId);

        if (!isMember) {
            LOG.warn("User {} attempted to like/dislike in room {} but is not a member", userId, roomId);
            throw new ForbiddenException("User is not a member of this room");
        }

        LOG.debug("Validated user {} is a member of room {}", userId, roomId);
    }
}
