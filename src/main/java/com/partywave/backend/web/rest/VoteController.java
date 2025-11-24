package com.partywave.backend.web.rest;

import com.partywave.backend.service.VoteService;
import com.partywave.backend.service.dto.KickUserRequestDTO;
import com.partywave.backend.service.dto.VoteResponseDTO;
import com.partywave.backend.service.dto.VoteStatusResponseDTO;
import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing vote operations in rooms.
 *
 * All endpoints require JWT authentication (configured in SecurityConfiguration).
 *
 * Based on PROJECT_OVERVIEW.md sections 2.8 and 2.9 - Vote-Based Skip/Kick.
 */
@RestController
@RequestMapping("/api/rooms")
public class VoteController {

    private static final Logger log = LoggerFactory.getLogger(VoteController.class);

    private final VoteService voteService;

    public VoteController(VoteService voteService) {
        this.voteService = voteService;
    }

    /**
     * POST /api/rooms/:roomId/votes/skip : Vote to skip current track.
     *
     * Allows room members to vote to skip the currently playing track.
     * When 50% threshold is reached, track is skipped automatically.
     *
     * Requirements:
     * - User must be authenticated
     * - User must be an active member of the room
     * - A track must be currently playing
     * - User must not have already voted for this track
     *
     * Workflow (based on PROJECT_OVERVIEW.md section 2.8):
     * 1. Validates user is a room member
     * 2. Checks there is a track currently playing
     * 3. Checks if user already voted for this track
     * 4. Creates vote record in PostgreSQL
     * 5. Counts votes for this track
     * 6. Gets online member count from Redis
     * 7. Checks threshold (50%)
     * 8. If threshold reached, skips track via PlaybackRedisService
     *
     * On success:
     * - Vote is recorded
     * - If threshold reached: track is skipped, next track starts, TRACK_SKIPPED event emitted (TODO: WebSocket)
     * - If threshold not reached: VOTE_CAST event emitted (TODO: WebSocket)
     *
     * @param roomId UUID of the room
     * @return ResponseEntity with status:
     *         - 200 (OK) with VoteResponseDTO body
     *         - 400 (Bad Request) if no track is playing or user already voted
     *         - 401 (Unauthorized) if not authenticated
     *         - 403 (Forbidden) if user is not a room member
     *         - 404 (Not Found) if room doesn't exist
     */
    @PostMapping("/{roomId}/votes/skip")
    public ResponseEntity<VoteResponseDTO> voteSkipTrack(@PathVariable UUID roomId) {
        log.debug("REST request to vote skip track in room: {}", roomId);

        // Extract authenticated user ID from JWT token
        UUID userId = extractUserIdFromAuthentication();

        if (userId == null) {
            log.warn("Unauthorized vote skip attempt - no valid JWT token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Vote to skip track via service
        // BadRequestException, ForbiddenException, and ResourceNotFoundException will be handled by global exception handler
        VoteResponseDTO response = voteService.voteSkipTrack(roomId, userId);

        log.info("User {} voted to skip track in room {}: threshold reached = {}", userId, roomId, response.isThresholdReached());

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/rooms/:roomId/votes/kick : Vote to kick a user from the room.
     *
     * Allows room members to vote to kick another user from the room.
     * When 50% threshold is reached, user is kicked automatically.
     *
     * Requirements:
     * - User must be authenticated
     * - User must be an active member of the room
     * - Target user must be an active member of the room
     * - User cannot vote to kick themselves
     * - Target user cannot be the room owner
     * - User must not have already voted to kick this target
     *
     * Workflow (based on PROJECT_OVERVIEW.md section 2.9):
     * 1. Validates user is a room member
     * 2. Validates target user is a room member
     * 3. Validates user is not voting to kick themselves
     * 4. Validates target user is not the room owner
     * 5. Checks if user already voted to kick this target
     * 6. Creates vote record
     * 7. Counts votes for this target
     * 8. Gets online member count
     * 9. Checks threshold (50%)
     * 10. If threshold reached, kicks user (soft delete RoomMember, remove from Redis, close WebSocket)
     *
     * On success:
     * - Vote is recorded
     * - If threshold reached: user is kicked, USER_KICKED event emitted (TODO: WebSocket)
     * - If threshold not reached: VOTE_CAST event emitted (TODO: WebSocket)
     *
     * @param roomId UUID of the room
     * @param request KickUserRequestDTO with target user ID
     * @return ResponseEntity with status:
     *         - 200 (OK) with VoteResponseDTO body
     *         - 400 (Bad Request) if validation fails
     *         - 401 (Unauthorized) if not authenticated
     *         - 403 (Forbidden) if user is not a room member
     *         - 404 (Not Found) if room or target user doesn't exist
     */
    @PostMapping("/{roomId}/votes/kick")
    public ResponseEntity<VoteResponseDTO> voteKickUser(@PathVariable UUID roomId, @Valid @RequestBody KickUserRequestDTO request) {
        log.debug("REST request to vote kick user in room: {}", roomId);

        // Extract authenticated user ID from JWT token
        UUID userId = extractUserIdFromAuthentication();

        if (userId == null) {
            log.warn("Unauthorized vote kick attempt - no valid JWT token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Vote to kick user via service
        // BadRequestException, ForbiddenException, and ResourceNotFoundException will be handled by global exception handler
        VoteResponseDTO response = voteService.voteKickUser(roomId, userId, request);

        log.info(
            "User {} voted to kick user {} in room {}: threshold reached = {}",
            userId,
            request.getTargetUserId(),
            roomId,
            response.isThresholdReached()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/rooms/:roomId/votes : Get current vote status for the room.
     *
     * Returns current vote counts for:
     * - Skip track vote (if a track is currently playing)
     * - Kick user votes (for all users being voted to kick)
     *
     * Requirements:
     * - User must be authenticated
     * - User must be an active member of the room
     *
     * Based on PROJECT_OVERVIEW.md sections 2.8 and 2.9.
     *
     * @param roomId UUID of the room
     * @return ResponseEntity with status:
     *         - 200 (OK) with VoteStatusResponseDTO body
     *         - 401 (Unauthorized) if not authenticated
     *         - 403 (Forbidden) if user is not a room member
     *         - 404 (Not Found) if room doesn't exist
     */
    @GetMapping("/{roomId}/votes")
    public ResponseEntity<VoteStatusResponseDTO> getVoteStatus(@PathVariable UUID roomId) {
        log.debug("REST request to get vote status for room: {}", roomId);

        // Extract authenticated user ID from JWT token
        UUID userId = extractUserIdFromAuthentication();

        if (userId == null) {
            log.warn("Unauthorized get vote status attempt - no valid JWT token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Get vote status via service
        // ForbiddenException and ResourceNotFoundException will be handled by global exception handler
        VoteStatusResponseDTO response = voteService.getVoteStatus(roomId, userId);

        log.debug("User {} retrieved vote status for room {}", userId, roomId);

        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/rooms/:roomId/votes/skip : Withdraw skip track vote.
     *
     * Allows room members to withdraw their vote to skip the currently playing track.
     * User can change their mind and remove their vote before threshold is reached.
     *
     * Requirements:
     * - User must be authenticated
     * - User must be an active member of the room
     * - A track must be currently playing
     * - User must have previously voted to skip this track
     *
     * Workflow:
     * 1. Validates user is a room member
     * 2. Checks there is a track currently playing
     * 3. Checks if user has voted for this track
     * 4. Deletes the vote record
     * 5. Returns updated vote counts
     *
     * On success:
     * - Vote is removed
     * - Updated vote count is returned
     * - VOTE_WITHDRAWN event emitted (TODO: WebSocket)
     *
     * @param roomId UUID of the room
     * @return ResponseEntity with status:
     *         - 200 (OK) with VoteResponseDTO body
     *         - 400 (Bad Request) if no track is playing or user hasn't voted
     *         - 401 (Unauthorized) if not authenticated
     *         - 403 (Forbidden) if user is not a room member
     *         - 404 (Not Found) if room doesn't exist
     */
    @DeleteMapping("/{roomId}/votes/skip")
    public ResponseEntity<VoteResponseDTO> withdrawSkipVote(@PathVariable UUID roomId) {
        log.debug("REST request to withdraw skip vote in room: {}", roomId);

        // Extract authenticated user ID from JWT token
        UUID userId = extractUserIdFromAuthentication();

        if (userId == null) {
            log.warn("Unauthorized withdraw skip vote attempt - no valid JWT token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Withdraw skip vote via service
        // InvalidRequestException, ForbiddenException, and ResourceNotFoundException will be handled by global exception handler
        VoteResponseDTO response = voteService.withdrawSkipVote(roomId, userId);

        log.info("User {} withdrew skip vote in room {}", userId, roomId);

        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/rooms/:roomId/votes/kick : Withdraw kick user vote.
     *
     * Allows room members to withdraw their vote to kick a user from the room.
     * User can change their mind and remove their vote before threshold is reached.
     *
     * Requirements:
     * - User must be authenticated
     * - User must be an active member of the room
     * - User must have previously voted to kick the target user
     *
     * Workflow:
     * 1. Validates user is a room member
     * 2. Validates target user exists
     * 3. Checks if user has voted to kick this target
     * 4. Deletes the vote record
     * 5. Returns updated vote counts
     *
     * On success:
     * - Vote is removed
     * - Updated vote count is returned
     * - VOTE_WITHDRAWN event emitted (TODO: WebSocket)
     *
     * @param roomId UUID of the room
     * @param request KickUserRequestDTO with target user ID
     * @return ResponseEntity with status:
     *         - 200 (OK) with VoteResponseDTO body
     *         - 400 (Bad Request) if user hasn't voted to kick this target
     *         - 401 (Unauthorized) if not authenticated
     *         - 403 (Forbidden) if user is not a room member
     *         - 404 (Not Found) if room or target user doesn't exist
     */
    @DeleteMapping("/{roomId}/votes/kick")
    public ResponseEntity<VoteResponseDTO> withdrawKickVote(@PathVariable UUID roomId, @Valid @RequestBody KickUserRequestDTO request) {
        log.debug("REST request to withdraw kick vote in room: {}", roomId);

        // Extract authenticated user ID from JWT token
        UUID userId = extractUserIdFromAuthentication();

        if (userId == null) {
            log.warn("Unauthorized withdraw kick vote attempt - no valid JWT token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Withdraw kick vote via service
        // InvalidRequestException, ForbiddenException, and ResourceNotFoundException will be handled by global exception handler
        VoteResponseDTO response = voteService.withdrawKickVote(roomId, userId, request);

        log.info("User {} withdrew kick vote for user {} in room {}", userId, request.getTargetUserId(), roomId);

        return ResponseEntity.ok(response);
    }

    /**
     * Extract user ID from JWT token in SecurityContext.
     *
     * @return User UUID from JWT 'sub' claim, or null if not authenticated
     */
    private UUID extractUserIdFromAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String subject = jwt.getSubject(); // 'sub' claim contains user ID

            if (subject != null && !subject.isEmpty()) {
                try {
                    return UUID.fromString(subject);
                } catch (IllegalArgumentException e) {
                    log.error("Invalid UUID in JWT subject: {}", subject, e);
                    return null;
                }
            }
        }

        return null;
    }
}
