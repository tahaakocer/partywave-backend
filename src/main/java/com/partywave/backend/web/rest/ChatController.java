package com.partywave.backend.web.rest;

import com.partywave.backend.service.ChatService;
import com.partywave.backend.service.dto.ChatMessageDTO;
import com.partywave.backend.service.dto.SendChatMessageRequestDTO;
import jakarta.validation.Valid;
import java.util.List;
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
 * REST controller for managing chat messages in rooms.
 *
 * All endpoints require JWT authentication (configured in SecurityConfiguration).
 *
 * Based on PROJECT_OVERVIEW.md section 2.12 - Chat Messaging.
 */
@RestController
@RequestMapping("/api/rooms")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * POST /api/rooms/:roomId/chat : Send a chat message in a room.
     *
     * Allows room members to send chat messages in real-time.
     *
     * Requirements:
     * - User must be authenticated
     * - User must be an active member of the room
     * - Message content must not be empty (max 1000 characters)
     * - Rate limiting: max 10 messages per minute per user per room
     *
     * Workflow (based on PROJECT_OVERVIEW.md section 2.12):
     * 1. Validates user is a room member
     * 2. Validates content is not empty (max 1000 characters)
     * 3. Checks rate limiting (max 10 messages per minute)
     * 4. Creates ChatMessage record in PostgreSQL
     * 5. Emits WebSocket CHAT_MESSAGE event to all room members
     *
     * On success:
     * - ChatMessage is created and persisted
     * - CHAT_MESSAGE event is emitted to all room members (TODO: WebSocket)
     * - Returns ChatMessageDTO with created message data
     *
     * @param roomId UUID of the room
     * @param request SendChatMessageRequestDTO with message content
     * @return ResponseEntity with status:
     *         - 200 (OK) with ChatMessageDTO body
     *         - 400 (Bad Request) if content is empty or rate limit exceeded
     *         - 401 (Unauthorized) if not authenticated
     *         - 403 (Forbidden) if user is not a room member
     *         - 404 (Not Found) if room doesn't exist
     */
    @PostMapping("/{roomId}/chat")
    public ResponseEntity<ChatMessageDTO> sendMessage(@PathVariable UUID roomId, @Valid @RequestBody SendChatMessageRequestDTO request) {
        log.debug("REST request to send chat message in room: {}", roomId);

        // Extract authenticated user ID from JWT token
        UUID userId = extractUserIdFromAuthentication();

        if (userId == null) {
            log.warn("Unauthorized send message attempt - no valid JWT token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Send message via service
        // BadRequestException, ForbiddenException, and ResourceNotFoundException will be handled by global exception handler
        ChatMessageDTO response = chatService.sendMessage(roomId, userId, request);

        log.info("User {} sent chat message in room {}: messageId={}", userId, roomId, response.getId());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/rooms/:roomId/chat : Get chat history for a room.
     *
     * Returns paginated chat messages ordered by sentAt descending (newest first).
     * Frontend can reverse the order to show oldest first.
     *
     * Requirements:
     * - User must be authenticated
     * - User must be an active member of the room
     *
     * Query parameters:
     * - page: Page number (0-indexed, default: 0)
     * - size: Page size (default: 50)
     *
     * Based on PROJECT_OVERVIEW.md section 2.12 - Chat Messaging.
     *
     * @param roomId UUID of the room
     * @param page Page number (0-indexed, default: 0)
     * @param size Page size (default: 50)
     * @return ResponseEntity with status:
     *         - 200 (OK) with List<ChatMessageDTO> body (newest first)
     *         - 401 (Unauthorized) if not authenticated
     *         - 403 (Forbidden) if user is not a room member
     *         - 404 (Not Found) if room doesn't exist
     */
    @GetMapping("/{roomId}/chat")
    public ResponseEntity<List<ChatMessageDTO>> getChatHistory(
        @PathVariable UUID roomId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        log.debug("REST request to get chat history for room: {}, page={}, size={}", roomId, page, size);

        // Extract authenticated user ID from JWT token
        UUID userId = extractUserIdFromAuthentication();

        if (userId == null) {
            log.warn("Unauthorized get chat history attempt - no valid JWT token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Validate pagination parameters
        if (page < 0) {
            page = 0;
        }
        if (size < 1) {
            size = 50;
        }
        if (size > 100) {
            size = 100; // Max page size
        }

        // Get chat history via service
        // ForbiddenException and ResourceNotFoundException will be handled by global exception handler
        List<ChatMessageDTO> chatHistory = chatService.getChatHistory(roomId, userId, page, size);

        log.debug("User {} retrieved {} chat messages for room {}", userId, chatHistory.size(), roomId);

        return ResponseEntity.ok(chatHistory);
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
