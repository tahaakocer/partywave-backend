package com.partywave.backend.service;

import com.partywave.backend.domain.AppUser;
import com.partywave.backend.domain.ChatMessage;
import com.partywave.backend.domain.Room;
import com.partywave.backend.domain.RoomInvitation;
import com.partywave.backend.domain.RoomMember;
import com.partywave.backend.domain.Tag;
import com.partywave.backend.domain.enumeration.RoomMemberRole;
import com.partywave.backend.exception.AlreadyMemberException;
import com.partywave.backend.exception.InvalidInvitationException;
import com.partywave.backend.exception.InvalidRequestException;
import com.partywave.backend.exception.ResourceNotFoundException;
import com.partywave.backend.exception.RoomFullException;
import com.partywave.backend.exception.UnauthorizedRoomAccessException;
import com.partywave.backend.repository.AppUserRepository;
import com.partywave.backend.repository.ChatMessageRepository;
import com.partywave.backend.repository.RoomAccessRepository;
import com.partywave.backend.repository.RoomInvitationRepository;
import com.partywave.backend.repository.RoomMemberRepository;
import com.partywave.backend.repository.RoomRepository;
import com.partywave.backend.repository.TagRepository;
import com.partywave.backend.repository.specification.RoomSpecifications;
import com.partywave.backend.service.dto.*;
import com.partywave.backend.service.mapper.RoomMapper;
import com.partywave.backend.service.mapper.TagMapper;
import com.partywave.backend.service.redis.LikeDislikeRedisService;
import com.partywave.backend.service.redis.OnlineMembersRedisService;
import com.partywave.backend.service.redis.PlaybackRedisService;
import com.partywave.backend.service.redis.PlaylistRedisService;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing PartyWave rooms.
 * Handles room creation, management, and coordination between PostgreSQL and Redis.
 *
 * Based on PROJECT_OVERVIEW.md section 2.2 - Room Creation.
 */
@Service
@Transactional
public class RoomService {

    private static final Logger log = LoggerFactory.getLogger(RoomService.class);

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final RoomAccessRepository roomAccessRepository;
    private final RoomInvitationRepository roomInvitationRepository;
    private final TagRepository tagRepository;
    private final AppUserRepository appUserRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final RoomMapper roomMapper;
    private final TagMapper tagMapper;
    private final OnlineMembersRedisService onlineMembersRedisService;
    private final PlaylistRedisService playlistRedisService;
    private final PlaybackRedisService playbackRedisService;
    private final LikeDislikeRedisService likeDislikeRedisService;
    private final RedisTemplate<String, Object> redisTemplate;

    public RoomService(
        RoomRepository roomRepository,
        RoomMemberRepository roomMemberRepository,
        RoomAccessRepository roomAccessRepository,
        RoomInvitationRepository roomInvitationRepository,
        TagRepository tagRepository,
        AppUserRepository appUserRepository,
        ChatMessageRepository chatMessageRepository,
        RoomMapper roomMapper,
        TagMapper tagMapper,
        OnlineMembersRedisService onlineMembersRedisService,
        PlaylistRedisService playlistRedisService,
        PlaybackRedisService playbackRedisService,
        LikeDislikeRedisService likeDislikeRedisService,
        RedisTemplate<String, Object> redisTemplate
    ) {
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.roomAccessRepository = roomAccessRepository;
        this.roomInvitationRepository = roomInvitationRepository;
        this.tagRepository = tagRepository;
        this.appUserRepository = appUserRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.roomMapper = roomMapper;
        this.tagMapper = tagMapper;
        this.onlineMembersRedisService = onlineMembersRedisService;
        this.playlistRedisService = playlistRedisService;
        this.playbackRedisService = playbackRedisService;
        this.likeDislikeRedisService = likeDislikeRedisService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Create a new room with the authenticated user as OWNER.
     *
     * Workflow (based on PROJECT_OVERVIEW.md section 2.2):
     * 1. Validate input (name, max_participants > 0)
     * 2. Create Room entity
     * 3. Create RoomMember entity with OWNER role
     * 4. Handle tags (create if missing, normalize to lowercase)
     * 5. Initialize Redis state:
     *    - Create empty playlist list
     *    - Create empty playback hash
     *    - Create empty online members set
     *    - Add creator to online members
     * 6. Return RoomResponseDTO
     *
     * @param request CreateRoomRequestDTO containing room details
     * @param creatorUserId UUID of the authenticated user creating the room
     * @return RoomResponseDTO with created room data
     * @throws InvalidRequestException if validation fails
     * @throws ResourceNotFoundException if user not found
     */
    public RoomResponseDTO createRoom(CreateRoomRequestDTO request, UUID creatorUserId) {
        log.debug("Creating room: {} by user: {}", request.getName(), creatorUserId);

        // Step 1: Validate input
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            log.error("Invalid room creation request: room name is empty");
            throw new InvalidRequestException("Room name must not be empty", "name", request.getName());
        }

        if (request.getMaxParticipants() == null || request.getMaxParticipants() <= 0) {
            log.error("Invalid room creation request: max participants must be greater than 0");
            throw new InvalidRequestException("Max participants must be greater than 0", "maxParticipants", request.getMaxParticipants());
        }

        // Step 2: Validate creator user ID and get creator user
        if (creatorUserId == null) {
            log.error("Invalid room creation request: creator user ID is null");
            throw new InvalidRequestException("Creator user ID must not be null", "creatorUserId", null);
        }

        AppUser creator = appUserRepository
            .findById(creatorUserId)
            .orElseThrow(() -> {
                log.error("User not found with id: {}", creatorUserId);
                return new ResourceNotFoundException("User", "id", creatorUserId);
            });

        // Step 3: Create Room entity
        Room room = new Room();
        room.setName(request.getName());
        room.setDescription(request.getDescription());
        room.setMaxParticipants(request.getMaxParticipants());
        room.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : false);

        Instant now = Instant.now();
        room.setCreatedAt(now);
        room.setUpdatedAt(now);

        // Step 4: Handle tags (normalize and create if missing)
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            Set<Tag> tags = new HashSet<>();
            for (String tagName : request.getTags()) {
                String normalizedTagName = tagName.toLowerCase().trim();
                if (!normalizedTagName.isEmpty()) {
                    Tag tag = tagRepository
                        .findAll()
                        .stream()
                        .filter(t -> t.getName().equalsIgnoreCase(normalizedTagName))
                        .findFirst()
                        .orElseGet(() -> {
                            Tag newTag = new Tag();
                            newTag.setName(normalizedTagName);
                            return tagRepository.save(newTag);
                        });
                    tags.add(tag);
                }
            }
            room.setTags(tags);
        }

        // Save room
        room = roomRepository.save(room);
        log.debug("Room created with id: {}", room.getId());

        // Step 5: Create RoomMember with OWNER role
        RoomMember roomMember = new RoomMember();
        roomMember.setRoom(room);
        roomMember.setAppUser(creator);
        roomMember.setRole(RoomMemberRole.OWNER);
        roomMember.setJoinedAt(now);
        roomMember.setLastActiveAt(now);

        roomMemberRepository.save(roomMember);
        log.debug("Room member created with OWNER role for user: {}", creatorUserId);

        // Step 6: Initialize Redis state for the room
        String roomIdStr = room.getId().toString();
        String userIdStr = creatorUserId.toString();

        try {
            // 6a. Create empty playlist list (key will be created when first item added)
            // Initialize sequence counter to 0
            String sequenceCounterKey = "partywave:room:" + roomIdStr + ":playlist:sequence_counter";
            redisTemplate.opsForValue().set(sequenceCounterKey, 0);
            log.debug("Initialized playlist sequence counter for room: {}", roomIdStr);

            // 6b. Create empty playback hash (key will be created when playback starts)
            // No need to create empty hash in Redis - it will be created when track starts

            // 6c. Create empty online members set
            // No need to create empty set - adding first member will create it

            // 6d. Add creator to online members
            onlineMembersRedisService.addOnlineMember(roomIdStr, userIdStr);
            log.debug("Added creator to online members for room: {}", roomIdStr);

            log.info("Redis state initialized for room: {}", roomIdStr);
        } catch (Exception e) {
            log.error("Failed to initialize Redis state for room: {}", roomIdStr, e);
            // Continue anyway - Redis state can be rebuilt later
        }

        // Step 7: Build and return response DTO
        RoomResponseDTO response = roomMapper.toDto(room);

        // Set member count (1 = creator)
        response.setMemberCount(1);

        // Set online member count from Redis
        long onlineCount = onlineMembersRedisService.getOnlineMemberCount(roomIdStr);
        response.setOnlineMemberCount(onlineCount);

        // Convert tags to TagDTO
        if (room.getTags() != null && !room.getTags().isEmpty()) {
            List<TagDTO> tagDTOs = room.getTags().stream().map(tagMapper::toDto).collect(Collectors.toList());
            response.setTags(tagDTOs);
        } else {
            response.setTags(Collections.emptyList());
        }

        log.info("Room created successfully: {} (id: {})", room.getName(), room.getId());
        return response;
    }

    /**
     * Get room by ID with eager loading of tags.
     *
     * @param roomId Room UUID
     * @return Optional of Room entity
     */
    public Optional<Room> findRoomById(UUID roomId) {
        return roomRepository.findOneWithEagerRelationships(roomId);
    }

    /**
     * Get room response DTO by ID.
     *
     * @param roomId Room UUID
     * @return Optional of RoomResponseDTO
     */
    public Optional<RoomResponseDTO> findRoomResponseById(UUID roomId) {
        Optional<Room> roomOpt = roomRepository.findOneWithEagerRelationships(roomId);
        if (roomOpt.isEmpty()) {
            return Optional.empty();
        }

        Room room = roomOpt.get();
        RoomResponseDTO response = roomMapper.toDto(room);

        // Set member count (only active members)
        long memberCount = roomMemberRepository.countByRoomAndIsActiveTrue(room);
        response.setMemberCount((int) memberCount);

        // Set online member count from Redis
        String roomIdStr = roomId.toString();
        long onlineCount = onlineMembersRedisService.getOnlineMemberCount(roomIdStr);
        response.setOnlineMemberCount(onlineCount);

        // Convert tags to TagDTO
        if (room.getTags() != null && !room.getTags().isEmpty()) {
            List<TagDTO> tagDTOs = room.getTags().stream().map(tagMapper::toDto).collect(Collectors.toList());
            response.setTags(tagDTOs);
        } else {
            response.setTags(Collections.emptyList());
        }

        return Optional.of(response);
    }

    /**
     * Find public rooms with optional filtering using JPA Specifications.
     *
     * Workflow (based on PROJECT_OVERVIEW.md section 2.3):
     * 1. Build dynamic query using Specifications (type-safe, composable)
     * 2. Filter rooms by is_public = true
     * 3. Optionally filter by tags (case-insensitive, OR logic)
     * 4. Optionally search by name/description (case-insensitive)
     * 5. Fetch tags for paginated results (single batch query)
     * 6. Batch load member counts (single GROUP BY query, no N+1 problem)
     * 7. Load online member counts from Redis
     * 8. Return paginated results with metadata:
     *    - Room details (name, description, tags, max_participants)
     *    - Current member count (from PostgreSQL room_member table)
     *    - Online member count (from Redis)
     *
     * Performance improvements over previous implementation:
     * - Uses Specifications for type-safe, maintainable queries
     * - Batch loads member counts (1 query instead of N queries)
     * - Uses EXISTS subquery for tag filtering (avoids LEFT JOIN issues)
     *
     * @param tags List of tag names to filter by, or null for no tag filtering
     * @param search Search term for name/description, or null for no search
     * @param pageable Pagination parameters (page, size, sort)
     * @return Page of RoomResponseDTO matching the criteria
     */
    @Transactional(readOnly = true)
    public Page<RoomResponseDTO> findPublicRooms(List<String> tags, String search, Pageable pageable) {
        log.debug("Finding public rooms with tags: {}, search: {}, page: {}", tags, search, pageable);

        // Step 1: Normalize tags to lowercase for case-insensitive matching
        List<String> normalizedTags = Collections.emptyList();
        if (tags != null && !tags.isEmpty()) {
            normalizedTags = tags
                .stream()
                .map(String::toLowerCase)
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .collect(Collectors.toList());
        }

        // Step 2: Build specification using composable predicates
        Specification<Room> spec = RoomSpecifications.findPublicRooms(normalizedTags, search);

        // Step 3: Execute query with pagination (tags not eagerly loaded)
        Page<Room> roomPage = roomRepository.findAll(spec, pageable);

        // Early return if no results
        if (roomPage.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        // Step 4: Batch load tags for all rooms in this page (single query)
        List<Room> roomsWithTags = roomRepository.fetchBagRelationships(roomPage.getContent());

        // Step 5: Batch load member counts for all rooms (single GROUP BY query)
        List<UUID> roomIds = roomsWithTags.stream().map(Room::getId).collect(Collectors.toList());
        Map<UUID, Long> memberCountMap = roomRepository
            .countMembersByRoomIds(roomIds)
            .stream()
            .collect(Collectors.toMap(arr -> (UUID) arr[0], arr -> (Long) arr[1]));

        // Step 6: Convert to DTOs with enriched metadata
        List<RoomResponseDTO> roomDTOs = roomsWithTags
            .stream()
            .map(room -> {
                RoomResponseDTO response = roomMapper.toDto(room);

                // Set member count from batch query result (default to 0 if room has no members)
                Long memberCount = memberCountMap.getOrDefault(room.getId(), 0L);
                response.setMemberCount(memberCount.intValue());

                // Set online member count from Redis
                String roomIdStr = room.getId().toString();
                long onlineCount = onlineMembersRedisService.getOnlineMemberCount(roomIdStr);
                response.setOnlineMemberCount(onlineCount);

                // Convert tags to TagDTO
                if (room.getTags() != null && !room.getTags().isEmpty()) {
                    List<TagDTO> tagDTOs = room.getTags().stream().map(tagMapper::toDto).collect(Collectors.toList());
                    response.setTags(tagDTOs);
                } else {
                    response.setTags(Collections.emptyList());
                }

                return response;
            })
            .collect(Collectors.toList());

        return new PageImpl<>(roomDTOs, pageable, roomPage.getTotalElements());
    }

    /**
     * Join a room (public or private with invitation token).
     *
     * Workflow (based on PROJECT_OVERVIEW.md section 2.3):
     * 1. Validate room exists
     * 2. Check if user is already an active member
     * 3. Check if user has inactive membership (reactivate if so)
     * 4. For public rooms: allow join without checks
     * 5. For private rooms: validate access (RoomAccess or invitation token)
     * 6. Validate room is not full (only active members count)
     * 7. Create or reactivate RoomMember entity
     * 8. Add user to Redis online members set
     * 9. Build complete room state response
     *
     * @param roomId UUID of the room to join
     * @param userId UUID of the authenticated user joining the room
     * @param invitationToken Optional invitation token for private rooms
     * @return RoomStateResponseDTO with complete room state
     * @throws ResourceNotFoundException if user or room not found
     * @throws UnauthorizedRoomAccessException if user cannot access private room
     * @throws InvalidInvitationException if invitation token is invalid
     * @throws RoomFullException if room has reached maximum capacity
     * @throws AlreadyMemberException if user is already an active member
     */
    @Transactional
    public RoomStateResponseDTO joinRoom(UUID roomId, UUID userId, String invitationToken) {
        log.debug("User {} attempting to join room {} with invitation token: {}", userId, roomId, invitationToken != null);

        // Step 1: Validate room exists
        Room room = roomRepository
            .findOneWithEagerRelationships(roomId)
            .orElseThrow(() -> {
                log.error("Room not found with id: {}", roomId);
                return new ResourceNotFoundException("Room", "id", roomId);
            });

        // Step 2: Check if user is already an active member
        boolean isActivelyMember = roomMemberRepository.existsByRoomIdAndUserIdAndIsActiveTrue(roomId, userId);
        if (isActivelyMember) {
            log.error("User {} is already an active member of room {}", userId, roomId);
            throw new AlreadyMemberException(userId, roomId);
        }

        // Step 3: Check if user has inactive membership (for reactivation)
        Optional<RoomMember> existingMembershipOpt = roomMemberRepository.findByRoomIdAndUserId(roomId, userId);

        // Step 4: For private rooms, validate access
        if (Boolean.FALSE.equals(room.getIsPublic())) {
            log.debug("Room {} is private, checking access for user {}", roomId, userId);

            // Check if user has explicit access
            boolean hasExplicitAccess = roomAccessRepository.existsByRoomIdAndAppUserId(roomId, userId);

            if (!hasExplicitAccess) {
                // No explicit access, check invitation token
                if (invitationToken == null || invitationToken.trim().isEmpty()) {
                    log.error("User {} attempted to join private room {} without invitation token", userId, roomId);
                    throw new UnauthorizedRoomAccessException(roomId, userId);
                }

                // Validate invitation token
                RoomInvitation invitation = roomInvitationRepository
                    .findByTokenAndIsActiveTrue(invitationToken)
                    .orElseThrow(() -> {
                        log.error("Invalid or inactive invitation token: {}", invitationToken);
                        return new InvalidInvitationException(invitationToken, "Token not found or inactive");
                    });

                // Verify token belongs to this room
                if (!invitation.getRoom().getId().equals(roomId)) {
                    log.error(
                        "Invitation token {} belongs to room {}, but user tried to join room {}",
                        invitationToken,
                        invitation.getRoom().getId(),
                        roomId
                    );
                    throw new InvalidInvitationException(invitationToken, "Token does not belong to this room");
                }

                // Check if token is expired
                if (invitation.getExpiresAt() != null && invitation.getExpiresAt().isBefore(Instant.now())) {
                    log.error("Invitation token {} has expired at {}", invitationToken, invitation.getExpiresAt());
                    throw new InvalidInvitationException(invitationToken, "Token has expired");
                }

                // Check if token has reached max uses
                if (invitation.getMaxUses() != null && invitation.getUsedCount() >= invitation.getMaxUses()) {
                    log.error(
                        "Invitation token {} has reached max uses: {}/{}",
                        invitationToken,
                        invitation.getUsedCount(),
                        invitation.getMaxUses()
                    );
                    throw new InvalidInvitationException(invitationToken, "Token has reached maximum uses");
                }

                // Increment used count atomically
                roomInvitationRepository.incrementUsedCount(invitation.getId());
                log.debug("Incremented used count for invitation token {}", invitationToken);
            } else {
                log.debug("User {} has explicit access to private room {}", userId, roomId);
            }
        }

        // Step 5: Validate room is not full (only count active members)
        long currentMemberCount = roomMemberRepository.countByRoomAndIsActiveTrue(room);
        if (currentMemberCount >= room.getMaxParticipants()) {
            log.error("Room is full: {} - Active members: {}, Max: {}", roomId, currentMemberCount, room.getMaxParticipants());
            throw new RoomFullException((int) currentMemberCount, room.getMaxParticipants());
        }

        // Step 6: Get user entity
        AppUser user = appUserRepository
            .findById(userId)
            .orElseThrow(() -> {
                log.error("User not found with id: {}", userId);
                return new ResourceNotFoundException("User", "id", userId);
            });

        // Step 7: Create or reactivate RoomMember
        RoomMember roomMember;
        Instant now = Instant.now();

        if (existingMembershipOpt.isPresent()) {
            // Reactivate existing membership
            roomMember = existingMembershipOpt.get();
            roomMember.setIsActive(true);
            roomMember.setLastActiveAt(now);
            log.debug("Reactivating membership for user {} in room {}", userId, roomId);
        } else {
            // Create new membership
            roomMember = new RoomMember();
            roomMember.setRoom(room);
            roomMember.setAppUser(user);
            roomMember.setRole(RoomMemberRole.PARTICIPANT);
            roomMember.setJoinedAt(now);
            roomMember.setLastActiveAt(now);
            roomMember.setIsActive(true);
            log.debug("Creating new membership for user {} in room {}", userId, roomId);
        }

        roomMemberRepository.save(roomMember);
        log.debug("Saved room member with PARTICIPANT role for user {} in room {}", userId, roomId);

        // Step 8: Add user to Redis online members
        String roomIdStr = roomId.toString();
        String userIdStr = userId.toString();
        onlineMembersRedisService.addOnlineMember(roomIdStr, userIdStr);
        log.debug("Added user {} to online members for room {}", userId, roomId);

        // Step 9: Build complete room state response
        RoomStateResponseDTO response = new RoomStateResponseDTO();

        // 9a. Set room details
        RoomResponseDTO roomDto = roomMapper.toDto(room);
        roomDto.setMemberCount((int) (currentMemberCount + 1)); // Include the user who just joined
        long onlineCount = onlineMembersRedisService.getOnlineMemberCount(roomIdStr);
        roomDto.setOnlineMemberCount(onlineCount);

        // Convert tags to TagDTO
        if (room.getTags() != null && !room.getTags().isEmpty()) {
            List<TagDTO> tagDTOs = room.getTags().stream().map(tagMapper::toDto).collect(Collectors.toList());
            roomDto.setTags(tagDTOs);
        } else {
            roomDto.setTags(Collections.emptyList());
        }

        response.setRoom(roomDto);

        // 9b. Get complete playlist from Redis with metadata and feedback counts
        List<PlaylistItemDTO> playlist = buildPlaylistResponse(roomIdStr);
        response.setPlaylist(playlist);

        // 9c. Get playback state from Redis (if a track is playing)
        PlaybackStateDTO playbackState = buildPlaybackStateResponse(roomIdStr);
        response.setPlaybackState(playbackState);

        // 9d. Get recent chat history (last 50 messages)
        List<ChatMessageDTO> chatHistory = buildChatHistoryResponse(roomId);
        response.setChatHistory(chatHistory);

        log.info("User {} successfully joined room {} - returning complete room state", userId, roomId);
        return response;
    }

    /**
     * Build playlist response with track metadata and feedback counts from Redis.
     *
     * @param roomId Room UUID as string
     * @return List of PlaylistItemDTO
     */
    private List<PlaylistItemDTO> buildPlaylistResponse(String roomId) {
        try {
            List<Map<Object, Object>> playlistItems = playlistRedisService.getAllPlaylistItems(roomId);
            List<PlaylistItemDTO> playlist = new ArrayList<>();

            for (Map<Object, Object> item : playlistItems) {
                PlaylistItemDTO dto = new PlaylistItemDTO();
                dto.setId(getStringValue(item, "id"));
                dto.setRoomId(getStringValue(item, "room_id"));
                dto.setSpotifyTrackId(getStringValue(item, "spotify_track_id"));
                dto.setTrackName(getStringValue(item, "track_name"));
                dto.setTrackArtist(getStringValue(item, "track_artist"));
                dto.setTrackAlbum(getStringValue(item, "track_album"));
                dto.setTrackImageUrl(getStringValue(item, "track_image_url"));
                dto.setDurationMs(getLongValue(item, "duration_ms"));
                dto.setAddedById(getStringValue(item, "added_by_id"));
                dto.setAddedByDisplayName(getStringValue(item, "added_by_display_name"));
                dto.setAddedAtMs(getLongValue(item, "added_at_ms"));
                dto.setSequenceNumber(getLongValue(item, "sequence_number"));
                dto.setStatus(getStringValue(item, "status"));

                // Get like/dislike counts from Redis
                String playlistItemId = dto.getId();
                long likeCount = likeDislikeRedisService.getLikeCount(roomId, playlistItemId);
                long dislikeCount = likeDislikeRedisService.getDislikeCount(roomId, playlistItemId);
                dto.setLikeCount(likeCount);
                dto.setDislikeCount(dislikeCount);

                playlist.add(dto);
            }

            return playlist;
        } catch (Exception e) {
            log.error("Failed to build playlist response for room {}", roomId, e);
            return Collections.emptyList();
        }
    }

    /**
     * Build playback state response from Redis.
     *
     * @param roomId Room UUID as string
     * @return PlaybackStateDTO or null if no playback state exists
     */
    private PlaybackStateDTO buildPlaybackStateResponse(String roomId) {
        try {
            Map<Object, Object> playbackData = playbackRedisService.getPlaybackState(roomId);
            if (playbackData.isEmpty()) {
                return null;
            }

            PlaybackStateDTO dto = new PlaybackStateDTO();
            dto.setCurrentPlaylistItemId(getStringValue(playbackData, "current_playlist_item_id"));
            dto.setStartedAtMs(getLongValue(playbackData, "started_at_ms"));
            dto.setTrackDurationMs(getLongValue(playbackData, "track_duration_ms"));
            dto.setUpdatedAtMs(getLongValue(playbackData, "updated_at_ms"));

            // Calculate elapsed time
            Long elapsedMs = playbackRedisService.getElapsedMs(roomId);
            dto.setElapsedMs(elapsedMs);

            return dto;
        } catch (Exception e) {
            log.error("Failed to build playback state response for room {}", roomId, e);
            return null;
        }
    }

    /**
     * Build chat history response from PostgreSQL.
     * Fetches last 50 messages ordered by sentAt descending (newest first).
     *
     * @param roomId Room UUID
     * @return List of ChatMessageDTO (reversed to show oldest first)
     */
    private List<ChatMessageDTO> buildChatHistoryResponse(UUID roomId) {
        try {
            // Fetch last 50 messages (newest first)
            PageRequest pageRequest = PageRequest.of(0, 50);
            Page<ChatMessage> messagesPage = chatMessageRepository.findByRoomIdOrderBySentAtDesc(roomId, pageRequest);

            // Convert to DTOs and reverse to show oldest first
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

            // Reverse to show oldest first
            Collections.reverse(chatHistory);

            return chatHistory;
        } catch (Exception e) {
            log.error("Failed to build chat history response for room {}", roomId, e);
            return Collections.emptyList();
        }
    }

    /**
     * Leave a room.
     *
     * Workflow:
     * 1. Find active RoomMember record
     * 2. Soft delete: set is_active = false, update lastActiveAt
     * 3. Remove user from Redis online members
     * 4. If no online members left, set TTL for room Redis keys (1 hour)
     *
     * @param roomId UUID of the room to leave
     * @param userId UUID of the authenticated user leaving the room
     * @throws ResourceNotFoundException if user is not an active member of the room
     */
    @Transactional
    public void leaveRoom(UUID roomId, UUID userId) {
        log.debug("User {} attempting to leave room {}", userId, roomId);

        // Step 1: Find active RoomMember record
        RoomMember roomMember = roomMemberRepository
            .findByRoomIdAndUserIdAndIsActiveTrue(roomId, userId)
            .orElseThrow(() -> {
                log.error("User {} is not an active member of room {}", userId, roomId);
                return new ResourceNotFoundException("RoomMember", "roomId/userId", roomId + "/" + userId);
            });

        // Step 2: Soft delete - set is_active = false, update lastActiveAt
        Instant now = Instant.now();
        roomMember.setIsActive(false);
        roomMember.setLastActiveAt(now);
        roomMemberRepository.save(roomMember);
        log.debug("Soft deleted room member for user {} in room {}", userId, roomId);

        // Step 3: Remove user from Redis online members
        String roomIdStr = roomId.toString();
        String userIdStr = userId.toString();
        onlineMembersRedisService.removeOnlineMember(roomIdStr, userIdStr);
        log.debug("Removed user {} from online members for room {}", userId, roomId);

        // Step 4: Check if room has any online members left
        boolean hasOnlineMembers = onlineMembersRedisService.hasOnlineMembers(roomIdStr);

        if (!hasOnlineMembers) {
            // No online members left, set TTL for room Redis keys (1 hour = 3600 seconds)
            log.info("No online members left in room {}, setting TTL for Redis keys", roomId);

            // Set TTL for online members set
            onlineMembersRedisService.setOnlineMembersTTL(roomIdStr, 3600);

            // Set TTL for playlist keys
            playlistRedisService.setPlaylistTTL(roomIdStr, 3600);

            // Set TTL for playback state
            playbackRedisService.setPlaybackTTL(roomIdStr, 3600);

            log.info("Set 1 hour TTL for all Redis keys in room {}", roomId);
        } else {
            log.debug("Room {} still has online members, not setting TTL", roomId);
        }

        log.info("User {} successfully left room {}", userId, roomId);
    }

    /**
     * Helper method to safely get string value from Redis map.
     */
    private String getStringValue(Map<Object, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Helper method to safely get long value from Redis map.
     */
    private Long getLongValue(Map<Object, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Failed to parse long value for key {}: {}", key, value);
            return null;
        }
    }
}
