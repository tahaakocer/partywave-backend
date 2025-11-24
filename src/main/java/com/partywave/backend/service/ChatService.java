package com.partywave.backend.service;

import com.partywave.backend.config.CacheConfiguration;
import com.partywave.backend.domain.AppUser;
import com.partywave.backend.domain.ChatMessage;
import com.partywave.backend.domain.Room;
import com.partywave.backend.exception.ForbiddenException;
import com.partywave.backend.exception.InvalidRequestException;
import com.partywave.backend.exception.ResourceNotFoundException;
import com.partywave.backend.repository.AppUserRepository;
import com.partywave.backend.repository.ChatMessageRepository;
import com.partywave.backend.repository.RoomMemberRepository;
import com.partywave.backend.repository.RoomRepository;
import com.partywave.backend.service.dto.ChatMessageDTO;
import com.partywave.backend.service.dto.ChatMessageEventDTO;
import com.partywave.backend.service.dto.SendChatMessageRequestDTO;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing chat messages in rooms.
 * Based on PROJECT_OVERVIEW.md section 2.12 - Chat Messaging.
 *
 * Business rules:
 * - User must be a member of the room
 * - Message content must not be empty (max 1000 characters)
 * - Rate limiting: max 10 messages per minute per user per room
 * - All messages are persisted in PostgreSQL
 * - WebSocket CHAT_MESSAGE event is emitted to all room members
 */
@Service
@Transactional
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    // Rate limiting: max 10 messages per minute per user per room
    private static final int RATE_LIMIT_MAX_MESSAGES = 10;
    private static final int RATE_LIMIT_WINDOW_SECONDS = 60;

    private final ChatMessageRepository chatMessageRepository;
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final AppUserRepository appUserRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public ChatService(
        ChatMessageRepository chatMessageRepository,
        RoomRepository roomRepository,
        RoomMemberRepository roomMemberRepository,
        AppUserRepository appUserRepository,
        RedisTemplate<String, Object> redisTemplate
    ) {
        this.chatMessageRepository = chatMessageRepository;
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.appUserRepository = appUserRepository;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Send a chat message in a room.
     * Based on PROJECT_OVERVIEW.md section 2.12 - Chat Messaging.
     *
     * Workflow:
     * 1. Validate user is a room member
     * 2. Validate content is not empty (max 1000 characters)
     * 3. Check rate limiting (max 10 messages per minute)
     * 4. Create ChatMessage record in PostgreSQL
     * 5. Emit WebSocket CHAT_MESSAGE event to all room members
     *
     * @param roomId Room UUID
     * @param userId User UUID (from JWT)
     * @param request SendChatMessageRequestDTO with message content
     * @return ChatMessageDTO with created message data
     * @throws ResourceNotFoundException if room or user doesn't exist
     * @throws ForbiddenException if user is not a room member
     * @throws InvalidRequestException if content is empty or rate limit exceeded
     */
    public ChatMessageDTO sendMessage(UUID roomId, UUID userId, SendChatMessageRequestDTO request) {
        log.debug("User {} sending chat message in room {}", userId, roomId);

        // Step 1: Validate room exists
        Room room = roomRepository
            .findById(roomId)
            .orElseThrow(() -> {
                log.error("Room not found with id: {}", roomId);
                return new ResourceNotFoundException("Room", "id", roomId);
            });

        // Step 2: Validate user is a room member
        boolean isMember = roomMemberRepository.existsByRoomIdAndUserIdAndIsActiveTrue(roomId, userId);
        if (!isMember) {
            log.error("User {} is not a member of room {}", userId, roomId);
            throw new ForbiddenException("User is not a member of this room");
        }

        // Step 3: Validate content (already validated by DTO @NotBlank and @Size, but double-check)
        String content = request.getContent();
        if (content == null || content.trim().isEmpty()) {
            throw new InvalidRequestException("Message content cannot be empty");
        }
        if (content.length() > 1000) {
            throw new InvalidRequestException("Message content cannot exceed 1000 characters");
        }

        // Step 4: Check rate limiting
        checkRateLimit(roomId, userId);

        // Step 5: Get user entity
        AppUser sender = appUserRepository
            .findById(userId)
            .orElseThrow(() -> {
                log.error("User not found with id: {}", userId);
                return new ResourceNotFoundException("User", "id", userId);
            });

        // Step 6: Create ChatMessage record
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setRoom(room);
        chatMessage.setSender(sender);
        chatMessage.setContent(content.trim());
        chatMessage.setSentAt(Instant.now());

        chatMessage = chatMessageRepository.save(chatMessage);
        log.info("Chat message created: id={}, room={}, sender={}", chatMessage.getId(), roomId, userId);

        // Step 7: Increment rate limit counter
        incrementRateLimitCounter(roomId, userId);

        // Step 8: Emit WebSocket CHAT_MESSAGE event
        emitChatMessageEvent(roomId, chatMessage, sender);

        // Step 9: Build and return DTO
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setId(chatMessage.getId());
        dto.setRoomId(roomId);
        dto.setSenderId(userId);
        dto.setSenderDisplayName(sender.getDisplayName());
        dto.setContent(chatMessage.getContent());
        dto.setTimestamp(chatMessage.getSentAt());

        return dto;
    }

    /**
     * Get chat history for a room with pagination.
     * Based on PROJECT_OVERVIEW.md section 2.12 - Chat Messaging.
     *
     * Returns messages ordered by sentAt descending (newest first).
     * Frontend can reverse the order to show oldest first.
     *
     * @param roomId Room UUID
     * @param userId User UUID (from JWT) - for membership validation
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return List of ChatMessageDTO (newest first)
     * @throws ResourceNotFoundException if room doesn't exist
     * @throws ForbiddenException if user is not a room member
     */
    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getChatHistory(UUID roomId, UUID userId, int page, int size) {
        log.debug("User {} requesting chat history for room {}: page={}, size={}", userId, roomId, page, size);

        // Validate room exists
        roomRepository
            .findById(roomId)
            .orElseThrow(() -> {
                log.error("Room not found with id: {}", roomId);
                return new ResourceNotFoundException("Room", "id", roomId);
            });

        // Validate user is a room member
        boolean isMember = roomMemberRepository.existsByRoomIdAndUserIdAndIsActiveTrue(roomId, userId);
        if (!isMember) {
            log.error("User {} is not a member of room {}", userId, roomId);
            throw new ForbiddenException("User is not a member of this room");
        }

        // Fetch messages with pagination (newest first)
        Pageable pageable = PageRequest.of(page, size);
        Page<ChatMessage> messagesPage = chatMessageRepository.findByRoomIdOrderBySentAtDesc(roomId, pageable);

        // Convert to DTOs
        List<ChatMessageDTO> chatHistory = messagesPage
            .getContent()
            .stream()
            .map(message -> {
                ChatMessageDTO dto = new ChatMessageDTO();
                dto.setId(message.getId());
                dto.setRoomId(message.getRoom().getId());
                dto.setSenderId(message.getSender().getId());
                dto.setSenderDisplayName(message.getSender().getDisplayName());
                dto.setContent(message.getContent());
                dto.setTimestamp(message.getSentAt());
                return dto;
            })
            .collect(Collectors.toList());

        log.debug("Retrieved {} chat messages for room {}", chatHistory.size(), roomId);

        return chatHistory;
    }

    /**
     * Check rate limiting for chat messages.
     * Uses Redis to track message count per user per room.
     * Rate limit: max 10 messages per minute per user per room.
     *
     * @param roomId Room UUID
     * @param userId User UUID
     * @throws InvalidRequestException if rate limit exceeded
     */
    private void checkRateLimit(UUID roomId, UUID userId) {
        String rateLimitKey = buildRateLimitKey(roomId, userId);

        try {
            // Get current count
            Object countObj = redisTemplate.opsForValue().get(rateLimitKey);
            int currentCount = countObj != null ? Integer.parseInt(countObj.toString()) : 0;

            if (currentCount >= RATE_LIMIT_MAX_MESSAGES) {
                log.warn(
                    "Rate limit exceeded for user {} in room {}: {} messages in last {} seconds",
                    userId,
                    roomId,
                    currentCount,
                    RATE_LIMIT_WINDOW_SECONDS
                );
                throw new InvalidRequestException(
                    "Rate limit exceeded. Maximum " + RATE_LIMIT_MAX_MESSAGES + " messages per " + RATE_LIMIT_WINDOW_SECONDS + " seconds."
                );
            }
        } catch (Exception e) {
            if (e instanceof InvalidRequestException) {
                throw e;
            }
            log.error("Failed to check rate limit for user {} in room {}", userId, roomId, e);
            // On Redis error, allow the request (fail open)
        }
    }

    /**
     * Increment rate limit counter after sending a message.
     * Sets TTL on the key to implement sliding window.
     *
     * @param roomId Room UUID
     * @param userId User UUID
     */
    private void incrementRateLimitCounter(UUID roomId, UUID userId) {
        String rateLimitKey = buildRateLimitKey(roomId, userId);

        try {
            // Increment counter
            Long count = redisTemplate.opsForValue().increment(rateLimitKey);

            // Set TTL if this is the first message in the window
            if (count != null && count == 1) {
                redisTemplate.expire(rateLimitKey, RATE_LIMIT_WINDOW_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.error("Failed to increment rate limit counter for user {} in room {}", userId, roomId, e);
            // On Redis error, continue (fail open)
        }
    }

    /**
     * Build Redis key for rate limiting.
     * Format: partywave:chat:ratelimit:{roomId}:{userId}
     *
     * @param roomId Room UUID
     * @param userId User UUID
     * @return Redis key string
     */
    private String buildRateLimitKey(UUID roomId, UUID userId) {
        return CacheConfiguration.KEY_PREFIX + "chat:ratelimit:" + roomId + ":" + userId;
    }

    /**
     * Emit CHAT_MESSAGE WebSocket event to all room members.
     * Based on PROJECT_OVERVIEW.md section 2.12 and 3.3.
     *
     * Event payload includes:
     * - messageId: UUID of the chat message
     * - senderId: UUID of the sender
     * - senderDisplayName: Display name of the sender
     * - content: Message content
     * - sentAt: Timestamp when message was sent
     *
     * TODO: Implement WebSocket event emission once Spring WebSocket (SimpMessagingTemplate) is configured.
     * For now, this method logs the event that should be emitted.
     *
     * @param roomId Room UUID
     * @param chatMessage ChatMessage entity
     * @param sender AppUser entity
     */
    private void emitChatMessageEvent(UUID roomId, ChatMessage chatMessage, AppUser sender) {
        try {
            // Build CHAT_MESSAGE event DTO
            ChatMessageEventDTO event = new ChatMessageEventDTO(
                roomId.toString(),
                chatMessage.getId(),
                sender.getId(),
                sender.getDisplayName(),
                chatMessage.getContent(),
                chatMessage.getSentAt()
            );

            // TODO: Emit WebSocket event once Spring WebSocket (SimpMessagingTemplate) is configured
            // String destination = "/topic/room/" + roomId;
            // messagingTemplate.convertAndSend(destination, event);

            log.info(
                "TODO: Emit CHAT_MESSAGE WebSocket event for message {} in room {}. Event data: type={}, sender={}, content={}",
                chatMessage.getId(),
                roomId,
                event.getType(),
                event.getSenderDisplayName(),
                event.getContent() != null ? event.getContent().substring(0, Math.min(50, event.getContent().length())) + "..." : "null"
            );
        } catch (Exception e) {
            log.error("Failed to prepare CHAT_MESSAGE event for message {} in room {}", chatMessage.getId(), roomId, e);
        }
    }
}
