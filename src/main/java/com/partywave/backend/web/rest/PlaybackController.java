package com.partywave.backend.web.rest;

import com.partywave.backend.security.jwt.JwtTokenProvider;
import com.partywave.backend.service.PlaybackService;
import com.partywave.backend.service.redis.TrackOperationResult;
import java.util.Map;
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
 * REST controller for managing playback operations in rooms.
 *
 * All endpoints require JWT authentication (configured in SecurityConfiguration).
 *
 * Based on PROJECT_OVERVIEW.md section 2.7 - Synchronized Playback.
 */
@RestController
@RequestMapping("/api/rooms")
public class PlaybackController {

    private static final Logger log = LoggerFactory.getLogger(PlaybackController.class);

    private final PlaybackService playbackService;
    private final JwtTokenProvider jwtTokenProvider;

    public PlaybackController(PlaybackService playbackService, JwtTokenProvider jwtTokenProvider) {
        this.playbackService = playbackService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * POST /api/rooms/:roomId/playback/skip : Manually skip current track.
     *
     * Allows OWNER or MODERATOR to manually skip the currently playing track.
     * This is different from vote-based skip (section 2.8).
     *
     * Requirements:
     * - User must be authenticated
     * - User must be an active member of the room
     * - User must have OWNER or MODERATOR role
     * - A track must be currently playing
     *
     * On success:
     * - Current track status is changed to SKIPPED
     * - Next QUEUED track is automatically started (if exists)
     * - If no more tracks, playback is stopped and playback hash is cleared
     * - WebSocket TRACK_SKIPPED event is emitted to all room members (TODO: implement WebSocket)
     *
     * Based on PROJECT_OVERVIEW.md section 6.3 - Manual Skip Track.
     *
     * @param roomId UUID of the room
     * @return ResponseEntity with status:
     *         - 200 (OK) with TrackOperationResult body if successful
     *         - 400 (Bad Request) if no track is playing or operation fails
     *         - 401 (Unauthorized) if not authenticated
     *         - 403 (Forbidden) if user doesn't have OWNER or MODERATOR role
     *         - 404 (Not Found) if user is not an active member of the room
     */
    @PostMapping("/{roomId}/playback/skip")
    public ResponseEntity<Map<String, Object>> skipTrack(@PathVariable UUID roomId) {
        log.debug("REST request to manually skip track in room: {}", roomId);

        // Extract authenticated user ID from JWT token
        UUID userId = extractUserIdFromAuthentication();

        if (userId == null) {
            log.warn("Unauthorized skip track attempt - no valid JWT token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Skip track via service (includes role check and permission validation)
        // ForbiddenException and ResourceNotFoundException will be handled by global exception handler
        TrackOperationResult result = playbackService.skipTrack(roomId, userId);

        if (result.isSuccess()) {
            log.info("User {} (OWNER/MODERATOR) successfully skipped track in room {}", userId, roomId);

            // Build response
            Map<String, Object> response = Map.of(
                "success",
                true,
                "message",
                result.getMessage(),
                "nextPlaylistItemId",
                result.getPlaylistItemId() != null ? result.getPlaylistItemId() : ""
            );

            return ResponseEntity.ok(response);
        } else {
            log.warn("User {} failed to skip track in room {}: {}", userId, roomId, result.getMessage());

            // Build error response
            Map<String, Object> response = Map.of("success", false, "message", result.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * GET /api/rooms/:roomId/playback : Get current playback state.
     *
     * Returns the current playback state for a room, including:
     * - Current playlist item ID
     * - Playback timing information (started_at_ms, track_duration_ms, elapsed_ms)
     * - Full track metadata (name, artist, album, duration, source_uri, etc.)
     *
     * Clients can use this to:
     * - Synchronize playback on join or reconnection
     * - Calculate elapsed time: elapsed_ms = now - started_at_ms
     * - Seek to correct position in Spotify player: player.seek(elapsed_ms)
     *
     * Based on PROJECT_OVERVIEW.md section 6.4 - Get Current Playback State.
     *
     * @param roomId UUID of the room
     * @return ResponseEntity with status:
     *         - 200 (OK) with playback state and track metadata if playback is active
     *         - 204 (No Content) if no playback is active in the room
     *         - 401 (Unauthorized) if not authenticated
     */
    @GetMapping("/{roomId}/playback")
    public ResponseEntity<Map<String, Object>> getPlaybackState(@PathVariable UUID roomId) {
        log.debug("REST request to get playback state for room: {}", roomId);

        // Extract authenticated user ID from JWT token (for audit/logging)
        UUID userId = extractUserIdFromAuthentication();

        if (userId == null) {
            log.warn("Unauthorized get playback state attempt - no valid JWT token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Get playback state with track metadata
        Map<String, Object> playbackStateWithMetadata = playbackService.getPlaybackStateWithMetadata(roomId.toString());

        if (playbackStateWithMetadata == null || playbackStateWithMetadata.isEmpty()) {
            log.debug("No active playback in room {}", roomId);
            return ResponseEntity.noContent().build();
        }

        log.debug("Retrieved playback state for room {}: {}", roomId, playbackStateWithMetadata);

        return ResponseEntity.ok(playbackStateWithMetadata);
    }

    /**
     * Extract user ID from JWT token in SecurityContext.
     *
     * @return UUID of authenticated user, or null if not authenticated
     */
    private UUID extractUserIdFromAuthentication() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null) {
                log.debug("No authentication found in SecurityContext");
                return null;
            }

            Object principal = authentication.getPrincipal();

            if (principal instanceof Jwt jwt) {
                // Extract user ID from JWT subject claim
                return jwtTokenProvider.getUserIdFromToken(jwt);
            } else {
                log.warn("Principal is not a Jwt object: {}", principal.getClass().getName());
                return null;
            }
        } catch (Exception e) {
            log.error("Failed to extract user ID from authentication", e);
            return null;
        }
    }
}
