package com.partywave.backend.web.rest;

import com.partywave.backend.security.jwt.JwtTokenProvider;
import com.partywave.backend.service.RoomService;
import com.partywave.backend.service.dto.CreateRoomRequestDTO;
import com.partywave.backend.service.dto.RoomResponseDTO;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
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
 * REST controller for managing PartyWave rooms.
 *
 * All endpoints require JWT authentication (configured in SecurityConfiguration).
 *
 * Based on PROJECT_OVERVIEW.md section 2.2 - Room Creation.
 */
@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private static final Logger log = LoggerFactory.getLogger(RoomController.class);

    private final RoomService roomService;
    private final JwtTokenProvider jwtTokenProvider;

    public RoomController(RoomService roomService, JwtTokenProvider jwtTokenProvider) {
        this.roomService = roomService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * POST /api/rooms : Create a new room.
     *
     * Creates a new room with the authenticated user as OWNER.
     * Room creation includes:
     * - Creating Room entity in PostgreSQL
     * - Creating RoomMember entity with OWNER role
     * - Handling tags (create if missing, normalize to lowercase)
     * - Initializing Redis state (empty playlist, playback hash, online members set)
     * - Adding creator to online members
     *
     * @param request CreateRoomRequestDTO containing room details (name, description, tags, max_participants, is_public)
     * @return ResponseEntity with status 201 (Created) and RoomResponseDTO body, or 400 (Bad Request) if validation fails, or 401 (Unauthorized) if not authenticated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping
    public ResponseEntity<RoomResponseDTO> createRoom(@Valid @RequestBody CreateRoomRequestDTO request) throws URISyntaxException {
        log.debug("REST request to create Room: {}", request);

        // Extract authenticated user ID from JWT token
        UUID userId = extractUserIdFromAuthentication();

        if (userId == null) {
            log.warn("Unauthorized room creation attempt - no valid JWT token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // Create room via service
            RoomResponseDTO result = roomService.createRoom(request, userId);

            log.info("Room created successfully: {} (id: {}) by user: {}", result.getName(), result.getId(), userId);

            // Return 201 Created with Location header pointing to the new room
            return ResponseEntity.created(new URI("/api/rooms/" + result.getId())).body(result);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid room creation request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.error("Failed to create room", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/rooms/:id : Get a room by ID.
     *
     * Returns room details including tags, member count, and online member count.
     *
     * @param id Room UUID
     * @return ResponseEntity with status 200 (OK) and RoomResponseDTO body, or 404 (Not Found) if room doesn't exist
     */
    @GetMapping("/{id}")
    public ResponseEntity<RoomResponseDTO> getRoom(@PathVariable UUID id) {
        log.debug("REST request to get Room: {}", id);

        return roomService.findRoomResponseById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
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
