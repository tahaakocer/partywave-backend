package com.partywave.backend.service;

import com.partywave.backend.domain.AppUser;
import com.partywave.backend.exception.ResourceNotFoundException;
import com.partywave.backend.exception.UnauthorizedRoomAccessException;
import com.partywave.backend.repository.AppUserRepository;
import com.partywave.backend.repository.RoomMemberRepository;
import com.partywave.backend.repository.RoomRepository;
import com.partywave.backend.service.dto.AddTrackRequestDTO;
import com.partywave.backend.service.dto.AddTrackResponseDTO;
import com.partywave.backend.service.redis.PlaybackRedisService;
import com.partywave.backend.service.redis.PlaylistRedisService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing playlist operations.
 * Handles adding tracks to room playlists and coordinating with Redis and playback services.
 *
 * Based on PROJECT_OVERVIEW.md section 2.6 - Adding Tracks to Playlist.
 */
@Service
@Transactional
public class PlaylistService {

    private static final Logger log = LoggerFactory.getLogger(PlaylistService.class);

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final AppUserRepository appUserRepository;
    private final PlaylistRedisService playlistRedisService;
    private final PlaybackRedisService playbackRedisService;

    public PlaylistService(
        RoomRepository roomRepository,
        RoomMemberRepository roomMemberRepository,
        AppUserRepository appUserRepository,
        PlaylistRedisService playlistRedisService,
        PlaybackRedisService playbackRedisService
    ) {
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.appUserRepository = appUserRepository;
        this.playlistRedisService = playlistRedisService;
        this.playbackRedisService = playbackRedisService;
    }

    /**
     * Add a track to a room's playlist.
     *
     * Workflow (based on PROJECT_OVERVIEW.md section 2.6):
     * 1. Validate user is a member of the room
     * 2. Generate UUID for playlist item
     * 3. Get next sequence number (Redis INCR counter)
     * 4. Create playlist item hash in Redis (status=QUEUED)
     * 5. RPUSH to playlist list
     * 6. If playlist is empty and no track is playing, auto-start the first track
     * 7. TODO: Emit WebSocket event PLAYLIST_ITEM_ADDED
     *
     * @param roomId Room UUID
     * @param userId User UUID (authenticated user)
     * @param request AddTrackRequestDTO containing track metadata
     * @return AddTrackResponseDTO with created playlist item details
     * @throws ResourceNotFoundException if room doesn't exist
     * @throws UnauthorizedRoomAccessException if user is not a room member
     */
    public AddTrackResponseDTO addTrack(UUID roomId, UUID userId, AddTrackRequestDTO request) {
        log.debug("Adding track to room {}: {}", roomId, request);

        // Step 1: Validate room exists
        if (!roomRepository.existsById(roomId)) {
            log.error("Room not found: {}", roomId);
            throw new ResourceNotFoundException("Room", "id", roomId);
        }

        // Step 2: Validate user is an active member of the room
        boolean isMember = roomMemberRepository.existsByRoomIdAndUserIdAndIsActiveTrue(roomId, userId);
        if (!isMember) {
            log.error("User {} is not an active member of room {}", userId, roomId);
            throw new UnauthorizedRoomAccessException(roomId, userId);
        }

        // Step 3: Get user details for display name
        Optional<AppUser> userOpt = appUserRepository.findById(userId);
        String userDisplayName = userOpt.map(AppUser::getDisplayName).orElse("Unknown User");

        // Step 4: Generate UUID for playlist item
        String playlistItemId = UUID.randomUUID().toString();
        String roomIdStr = roomId.toString();
        String userIdStr = userId.toString();

        // Step 5: Get next sequence number (Redis INCR counter)
        Long sequenceNumber = playlistRedisService.getNextSequenceNumber(roomIdStr);
        if (sequenceNumber == null) {
            log.error("Failed to generate sequence number for room {} - Redis INCR returned null", roomId);
            throw new RuntimeException("Failed to generate sequence number for playlist item");
        }
        log.debug("Generated sequence number {} for playlist item {}", sequenceNumber, playlistItemId);

        // Step 6: Get current timestamp
        long addedAtMs = System.currentTimeMillis();

        // Step 7: Create playlist item hash data
        Map<String, String> playlistItemData = new HashMap<>();
        playlistItemData.put("id", playlistItemId);
        playlistItemData.put("room_id", roomIdStr);
        playlistItemData.put("added_by_id", userIdStr);
        playlistItemData.put("sequence_number", sequenceNumber.toString());
        playlistItemData.put("status", "QUEUED");
        playlistItemData.put("added_at_ms", String.valueOf(addedAtMs));
        playlistItemData.put("source_id", request.getSourceId());
        playlistItemData.put("source_uri", request.getSourceUri());
        playlistItemData.put("name", request.getName());
        playlistItemData.put("artist", request.getArtist());
        playlistItemData.put("album", request.getAlbum());
        playlistItemData.put("duration_ms", request.getDurationMs().toString());

        // Add optional album image URL if provided
        if (request.getAlbumImageUrl() != null && !request.getAlbumImageUrl().isEmpty()) {
            playlistItemData.put("album_image_url", request.getAlbumImageUrl());
        }

        // Step 8: Add playlist item to Redis (creates hash and appends to list)
        boolean added = playlistRedisService.addPlaylistItem(roomIdStr, playlistItemId, playlistItemData);
        if (!added) {
            log.error("Failed to add playlist item {} to room {}", playlistItemId, roomId);
            throw new RuntimeException("Failed to add track to playlist");
        }

        log.info(
            "Added track '{}' by {} to room {} (playlist item: {}, sequence: {})",
            request.getName(),
            request.getArtist(),
            roomId,
            playlistItemId,
            sequenceNumber
        );

        // Step 9: Check if this is the first track and auto-start if needed
        boolean autoStarted = false;
        boolean isPlaying = playbackRedisService.isPlaying(roomIdStr);

        if (!isPlaying) {
            // No track is currently playing - check if this is the first queued track
            String firstQueuedItemId = playlistRedisService.getFirstQueuedItemId(roomIdStr);

            if (firstQueuedItemId != null && firstQueuedItemId.equals(playlistItemId)) {
                // This is the first track in an empty playlist - auto-start it
                log.info("Auto-starting first track {} in room {}", playlistItemId, roomId);

                var startResult = playbackRedisService.startTrack(roomIdStr, playlistItemId);
                if (startResult.isSuccess()) {
                    autoStarted = true;
                    log.info("Successfully auto-started track {} in room {}", playlistItemId, roomId);
                    // TODO: Emit WebSocket event TRACK_START
                } else {
                    log.warn("Failed to auto-start track {} in room {}: {}", playlistItemId, roomId, startResult.getMessage());
                }
            }
        }

        // Step 10: Prepare response DTO
        AddTrackResponseDTO response = new AddTrackResponseDTO();
        response.setPlaylistItemId(playlistItemId);
        response.setRoomId(roomIdStr);
        response.setSourceId(request.getSourceId());
        response.setSourceUri(request.getSourceUri());
        response.setName(request.getName());
        response.setArtist(request.getArtist());
        response.setAlbum(request.getAlbum());
        response.setDurationMs(request.getDurationMs());
        response.setAlbumImageUrl(request.getAlbumImageUrl());
        response.setAddedById(userIdStr);
        response.setAddedByDisplayName(userDisplayName);
        response.setAddedAtMs(addedAtMs);
        response.setSequenceNumber(sequenceNumber);
        response.setStatus(autoStarted ? "PLAYING" : "QUEUED");
        response.setLikeCount(0L);
        response.setDislikeCount(0L);
        response.setAutoStarted(autoStarted);

        // TODO: Emit WebSocket event PLAYLIST_ITEM_ADDED to all room members
        // This should include the full playlist item data so clients can update their UI
        log.debug("TODO: Emit WebSocket event PLAYLIST_ITEM_ADDED for room {}", roomId);

        return response;
    }
}
