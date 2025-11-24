package com.partywave.backend.web.rest;

import com.partywave.backend.exception.ResourceNotFoundException;
import com.partywave.backend.exception.UnauthorizedRoomAccessException;
import com.partywave.backend.repository.RoomMemberRepository;
import com.partywave.backend.repository.RoomRepository;
import com.partywave.backend.service.PlaylistService;
import com.partywave.backend.service.SpotifyApiClient;
import com.partywave.backend.service.dto.AddTrackRequestDTO;
import com.partywave.backend.service.dto.AddTrackResponseDTO;
import com.partywave.backend.service.dto.SpotifyTrackSearchResultDTO;
import com.partywave.backend.service.dto.TrackSearchResponseDTO;
import jakarta.validation.Valid;
import java.util.ArrayList;
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
 * REST controller for managing room playlists.
 * Provides endpoints for track search and playlist operations.
 *
 * Based on PROJECT_OVERVIEW.md section 2.6 - Adding Tracks to Playlist.
 */
@RestController
@RequestMapping("/api/rooms/{roomId}")
public class PlaylistController {

    private static final Logger LOG = LoggerFactory.getLogger(PlaylistController.class);

    private final SpotifyApiClient spotifyApiClient;
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final PlaylistService playlistService;

    public PlaylistController(
        SpotifyApiClient spotifyApiClient,
        RoomRepository roomRepository,
        RoomMemberRepository roomMemberRepository,
        PlaylistService playlistService
    ) {
        this.spotifyApiClient = spotifyApiClient;
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.playlistService = playlistService;
    }

    /**
     * POST /api/rooms/{roomId}/playlist : Add a track to the room's playlist
     *
     * Adds a track to the room's playlist (always appended to the end).
     * User must be an active member of the room to add tracks.
     *
     * Workflow (based on PROJECT_OVERVIEW.md section 2.6):
     * 1. Validates user is a room member
     * 2. Generates UUID for playlist item
     * 3. Gets next sequence number (Redis INCR counter)
     * 4. Creates playlist item hash in Redis (status=QUEUED)
     * 5. RPUSH to playlist list
     * 6. If playlist is empty and no track is playing, auto-starts the first track
     * 7. TODO: Emits WebSocket event PLAYLIST_ITEM_ADDED
     *
     * @param roomId Room ID (UUID)
     * @param request AddTrackRequestDTO containing track metadata (source_id, source_uri, name, artist, album, duration_ms)
     * @return ResponseEntity with AddTrackResponseDTO containing created playlist item details
     * @throws ResourceNotFoundException if room doesn't exist
     * @throws UnauthorizedRoomAccessException if user is not a room member
     */
    @PostMapping("/playlist")
    public ResponseEntity<AddTrackResponseDTO> addTrackToPlaylist(
        @PathVariable UUID roomId,
        @Valid @RequestBody AddTrackRequestDTO request
    ) {
        LOG.debug("REST request to add track to room {}: {}", roomId, request);

        // Get authenticated user ID from JWT
        UUID userId = getCurrentUserId();
        LOG.debug("User {} adding track to room {}", userId, roomId);

        // Delegate to service layer
        AddTrackResponseDTO response = playlistService.addTrack(roomId, userId, request);

        LOG.info(
            "Track added to room {} playlist: {} (item: {}, sequence: {}, auto-started: {})",
            roomId,
            response.getName(),
            response.getPlaylistItemId(),
            response.getSequenceNumber(),
            response.isAutoStarted()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/rooms/{roomId}/tracks/search : Search for tracks to add to playlist
     *
     * Searches Spotify for tracks based on the query parameter.
     * User must be an active member of the room to search.
     * Uses the user's Spotify access token for the search.
     *
     * Based on PROJECT_OVERVIEW.md section 2.6 steps 1-2:
     * - Backend proxies request to Spotify API with user's access token
     * - Returns track metadata: name, artist, album, duration, source_uri, source_id
     *
     * @param roomId Room ID (UUID)
     * @param query Search query (song name, artist, album)
     * @param limit Number of results (default 20, max 50), optional
     * @param offset Offset for pagination (default 0), optional
     * @return ResponseEntity with TrackSearchResponseDTO containing track results
     * @throws ResourceNotFoundException if room doesn't exist
     * @throws UnauthorizedRoomAccessException if user is not a room member
     */
    @GetMapping("/tracks/search")
    public ResponseEntity<TrackSearchResponseDTO> searchTracks(
        @PathVariable UUID roomId,
        @RequestParam("q") String query,
        @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit,
        @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset
    ) {
        LOG.debug("REST request to search tracks for room: {}, query: {}", roomId, query);

        // Get authenticated user ID from JWT
        UUID userId = getCurrentUserId();
        LOG.debug("User {} searching tracks for room {}", userId, roomId);

        // Validate room exists
        if (!roomRepository.existsById(roomId)) {
            LOG.error("Room not found: {}", roomId);
            throw new ResourceNotFoundException("Room", "id", roomId);
        }

        // Validate user is an active member of the room
        boolean isMember = roomMemberRepository.existsByRoomIdAndUserIdAndIsActiveTrue(roomId, userId);
        if (!isMember) {
            LOG.error("User {} is not a member of room {}", userId, roomId);
            throw new UnauthorizedRoomAccessException(roomId, userId);
        }

        // Validate query parameter
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Search query cannot be empty");
        }

        // Validate and constrain limit
        if (limit != null && limit > 50) {
            limit = 50;
        }
        if (limit != null && limit < 1) {
            limit = 1;
        }

        // Call Spotify API with user's access token (automatic token refresh handled by SpotifyApiClient)
        SpotifyTrackSearchResultDTO spotifyResult = spotifyApiClient.searchTracks(userId, query, limit, offset, null);

        // Transform Spotify results to our DTO format
        TrackSearchResponseDTO response = transformSpotifyResultToDTO(spotifyResult);

        LOG.debug("Successfully retrieved {} tracks for room {}", response.getTracks().size(), roomId);
        return ResponseEntity.ok(response);
    }

    /**
     * Transforms Spotify API search results to our TrackSearchResponseDTO format.
     * Extracts the essential track metadata needed for playlist operations.
     *
     * @param spotifyResult Spotify search result from SpotifyApiClient
     * @return TrackSearchResponseDTO with simplified track data
     */
    private TrackSearchResponseDTO transformSpotifyResultToDTO(SpotifyTrackSearchResultDTO spotifyResult) {
        List<TrackSearchResponseDTO.TrackDTO> tracks = new ArrayList<>();

        if (spotifyResult.getTracks() != null && spotifyResult.getTracks().getItems() != null) {
            for (SpotifyTrackSearchResultDTO.TrackDTO spotifyTrack : spotifyResult.getTracks().getItems()) {
                // Extract primary artist name (first artist)
                String artistName = null;
                if (spotifyTrack.getArtists() != null && !spotifyTrack.getArtists().isEmpty()) {
                    artistName = spotifyTrack.getArtists().get(0).getName();
                }

                // Extract album name
                String albumName = null;
                if (spotifyTrack.getAlbum() != null) {
                    albumName = spotifyTrack.getAlbum().getName();
                }

                // Extract album image URL (prefer medium size, typically 300x300)
                String albumImageUrl = null;
                if (
                    spotifyTrack.getAlbum() != null &&
                    spotifyTrack.getAlbum().getImages() != null &&
                    !spotifyTrack.getAlbum().getImages().isEmpty()
                ) {
                    // Get first image (usually largest) or middle image if multiple
                    List<SpotifyTrackSearchResultDTO.ImageDTO> images = spotifyTrack.getAlbum().getImages();
                    if (images.size() > 1) {
                        // Get middle size image
                        albumImageUrl = images.get(images.size() / 2).getUrl();
                    } else {
                        albumImageUrl = images.get(0).getUrl();
                    }
                }

                TrackSearchResponseDTO.TrackDTO trackDTO = new TrackSearchResponseDTO.TrackDTO(
                    spotifyTrack.getId(), // source_id
                    spotifyTrack.getUri(), // source_uri
                    spotifyTrack.getName(), // name
                    artistName, // artist
                    albumName, // album
                    spotifyTrack.getDurationMs(), // duration_ms
                    albumImageUrl, // album_image_url
                    spotifyTrack.getExplicit() // explicit
                );

                tracks.add(trackDTO);
            }
        }

        // Extract pagination info
        Integer total = spotifyResult.getTracks() != null ? spotifyResult.getTracks().getTotal() : 0;
        Integer limit = spotifyResult.getTracks() != null ? spotifyResult.getTracks().getLimit() : 0;
        Integer offset = spotifyResult.getTracks() != null ? spotifyResult.getTracks().getOffset() : 0;

        return new TrackSearchResponseDTO(tracks, total, limit, offset);
    }

    /**
     * Extracts the current user's UUID from the JWT token in the security context.
     *
     * @return UUID of the authenticated user
     * @throws IllegalStateException if authentication is not available or invalid
     */
    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            throw new IllegalStateException("No valid authentication found");
        }

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String subject = jwt.getSubject();

        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid user ID in JWT token: " + subject, e);
        }
    }
}
