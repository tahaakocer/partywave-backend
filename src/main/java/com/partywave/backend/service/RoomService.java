package com.partywave.backend.service;

import com.partywave.backend.domain.AppUser;
import com.partywave.backend.domain.Room;
import com.partywave.backend.domain.RoomMember;
import com.partywave.backend.domain.Tag;
import com.partywave.backend.domain.enumeration.RoomMemberRole;
import com.partywave.backend.repository.AppUserRepository;
import com.partywave.backend.repository.RoomMemberRepository;
import com.partywave.backend.repository.RoomRepository;
import com.partywave.backend.repository.TagRepository;
import com.partywave.backend.service.dto.CreateRoomRequestDTO;
import com.partywave.backend.service.dto.RoomResponseDTO;
import com.partywave.backend.service.dto.TagDTO;
import com.partywave.backend.service.mapper.RoomMapper;
import com.partywave.backend.service.mapper.TagMapper;
import com.partywave.backend.service.redis.OnlineMembersRedisService;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final TagRepository tagRepository;
    private final AppUserRepository appUserRepository;
    private final RoomMapper roomMapper;
    private final TagMapper tagMapper;
    private final OnlineMembersRedisService onlineMembersRedisService;
    private final RedisTemplate<String, Object> redisTemplate;

    public RoomService(
        RoomRepository roomRepository,
        RoomMemberRepository roomMemberRepository,
        TagRepository tagRepository,
        AppUserRepository appUserRepository,
        RoomMapper roomMapper,
        TagMapper tagMapper,
        OnlineMembersRedisService onlineMembersRedisService,
        RedisTemplate<String, Object> redisTemplate
    ) {
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.tagRepository = tagRepository;
        this.appUserRepository = appUserRepository;
        this.roomMapper = roomMapper;
        this.tagMapper = tagMapper;
        this.onlineMembersRedisService = onlineMembersRedisService;
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
     * @throws IllegalArgumentException if validation fails
     * @throws RuntimeException if user not found
     */
    public RoomResponseDTO createRoom(CreateRoomRequestDTO request, UUID creatorUserId) {
        log.debug("Creating room: {} by user: {}", request.getName(), creatorUserId);

        // Step 1: Validate input
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Room name must not be empty");
        }

        if (request.getMaxParticipants() == null || request.getMaxParticipants() <= 0) {
            throw new IllegalArgumentException("Max participants must be greater than 0");
        }

        // Step 2: Validate creator user ID and get creator user
        if (creatorUserId == null) {
            throw new IllegalArgumentException("Creator user ID must not be null");
        }

        AppUser creator = appUserRepository
            .findById(creatorUserId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + creatorUserId));

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

        // Set member count
        long memberCount = roomMemberRepository.countByRoom(room);
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
}
