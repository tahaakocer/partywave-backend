package com.partywave.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.partywave.backend.domain.AppUser;
import com.partywave.backend.exception.SpotifyApiException;
import com.partywave.backend.service.dto.SpotifyTrackSearchResultDTO;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Service for making Spotify API calls with automatic token refresh.
 * Provides methods for search, track retrieval, and playback control.
 * Based on SPOTIFY_SEARCH_ENDPOINTS.md and SPOTIFY_PLAYER_API_ENDPOINTS.md
 */
@Service
public class SpotifyApiClient {

    private static final Logger LOG = LoggerFactory.getLogger(SpotifyApiClient.class);

    private static final String SPOTIFY_API_BASE_URL = "https://api.spotify.com/v1";

    private final TokenRefreshService tokenRefreshService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public SpotifyApiClient(TokenRefreshService tokenRefreshService) {
        this.tokenRefreshService = tokenRefreshService;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Search for tracks on Spotify.
     * GET /v1/search?q=&type=track
     *
     * @param userId User ID performing the search
     * @param query Search query (song name, artist, album)
     * @param limit Number of results to return (default 20, max 50)
     * @param offset Offset for pagination (default 0)
     * @param market Optional market/region code (e.g., "TR", "US")
     * @return Track search results
     * @throws SpotifyApiException if search fails
     */
    public SpotifyTrackSearchResultDTO searchTracks(UUID userId, String query, Integer limit, Integer offset, String market) {
        LOG.debug("Searching tracks for user: {}, query: {}", userId, query);

        // Validate query
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Search query cannot be empty");
        }

        // Get valid access token (with automatic refresh)
        String accessToken = tokenRefreshService.getValidAccessToken(userId);

        // Build URL with query parameters
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(SPOTIFY_API_BASE_URL + "/search")
            .queryParam("q", query)
            .queryParam("type", "track");

        if (limit != null && limit > 0 && limit <= 50) {
            builder.queryParam("limit", limit);
        }

        if (offset != null && offset >= 0) {
            builder.queryParam("offset", offset);
        }

        if (market != null && !market.trim().isEmpty()) {
            builder.queryParam("market", market);
        }

        String url = builder.toUriString();

        // Prepare headers
        HttpHeaders headers = createAuthHeaders(accessToken);

        // Make request
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<SpotifyTrackSearchResultDTO> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                SpotifyTrackSearchResultDTO.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                LOG.debug("Successfully searched tracks for user: {}", userId);
                return response.getBody();
            } else {
                LOG.error("Failed to search tracks. Status: {}", response.getStatusCode());
                throw new SpotifyApiException("Failed to search tracks", "search_tracks");
            }
        } catch (HttpClientErrorException e) {
            handleHttpError(e, "search tracks");
            throw new SpotifyApiException("Failed to search tracks: " + e.getMessage(), "search_tracks", e);
        } catch (Exception e) {
            LOG.error("Error searching tracks: {}", e.getMessage(), e);
            throw new SpotifyApiException("Error searching tracks: " + e.getMessage(), "search_tracks", e);
        }
    }

    /**
     * Search for tracks with default parameters.
     *
     * @param userId User ID
     * @param query Search query
     * @return Track search results
     */
    public SpotifyTrackSearchResultDTO searchTracks(UUID userId, String query) {
        return searchTracks(userId, query, 20, 0, null);
    }

    /**
     * Get a single track by its Spotify ID.
     * GET /v1/tracks/{id}
     *
     * @param userId User ID
     * @param trackId Spotify track ID
     * @return Track details as JsonNode
     * @throws SpotifyApiException if retrieval fails
     */
    public JsonNode getTrack(UUID userId, String trackId) {
        LOG.debug("Getting track {} for user: {}", trackId, userId);

        if (trackId == null || trackId.trim().isEmpty()) {
            throw new IllegalArgumentException("Track ID cannot be empty");
        }

        // Get valid access token
        String accessToken = tokenRefreshService.getValidAccessToken(userId);

        // Build URL
        String url = SPOTIFY_API_BASE_URL + "/tracks/" + trackId;

        // Prepare headers
        HttpHeaders headers = createAuthHeaders(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode track = objectMapper.readTree(response.getBody());
                LOG.debug("Successfully retrieved track: {}", trackId);
                return track;
            } else {
                LOG.error("Failed to get track. Status: {}", response.getStatusCode());
                throw new SpotifyApiException("Failed to get track", "get_track");
            }
        } catch (HttpClientErrorException e) {
            handleHttpError(e, "get track");
            throw new SpotifyApiException("Failed to get track: " + e.getMessage(), "get_track", e);
        } catch (Exception e) {
            LOG.error("Error getting track: {}", e.getMessage(), e);
            throw new SpotifyApiException("Error getting track: " + e.getMessage(), "get_track", e);
        }
    }

    /**
     * Start or resume playback of a track.
     * PUT /v1/me/player/play
     *
     * @param userId User ID
     * @param trackUris List of Spotify track URIs to play (e.g., ["spotify:track:..."])
     * @param deviceId Optional device ID to play on
     * @param positionMs Optional starting position in milliseconds
     * @throws SpotifyApiException if playback fails
     */
    public void playTrack(UUID userId, List<String> trackUris, String deviceId, Integer positionMs) {
        LOG.debug("Playing track(s) for user: {}", userId);

        if (trackUris == null || trackUris.isEmpty()) {
            throw new IllegalArgumentException("Track URIs cannot be empty");
        }

        // Get valid access token
        String accessToken = tokenRefreshService.getValidAccessToken(userId);

        // Build URL
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(SPOTIFY_API_BASE_URL + "/me/player/play");

        if (deviceId != null && !deviceId.trim().isEmpty()) {
            builder.queryParam("device_id", deviceId);
        }

        String url = builder.toUriString();

        // Prepare request body
        Map<String, Object> body = new HashMap<>();
        body.put("uris", trackUris);

        if (positionMs != null && positionMs >= 0) {
            body.put("position_ms", positionMs);
        }

        // Prepare headers
        HttpHeaders headers = createAuthHeaders(accessToken);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);

            if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                LOG.debug("Successfully started playback for user: {}", userId);
            } else {
                LOG.error("Failed to start playback. Status: {}", response.getStatusCode());
                throw new SpotifyApiException("Failed to start playback", "play_track");
            }
        } catch (HttpClientErrorException e) {
            handleHttpError(e, "play track");
            throw new SpotifyApiException("Failed to play track: " + e.getMessage(), "play_track", e);
        } catch (Exception e) {
            LOG.error("Error playing track: {}", e.getMessage(), e);
            throw new SpotifyApiException("Error playing track: " + e.getMessage(), "play_track", e);
        }
    }

    /**
     * Play a single track by URI.
     *
     * @param userId User ID
     * @param trackUri Spotify track URI
     * @param deviceId Optional device ID
     */
    public void playTrack(UUID userId, String trackUri, String deviceId) {
        playTrack(userId, List.of(trackUri), deviceId, 0);
    }

    /**
     * Play a single track by URI without device ID.
     *
     * @param userId User ID
     * @param trackUri Spotify track URI
     */
    public void playTrack(UUID userId, String trackUri) {
        playTrack(userId, List.of(trackUri), null, 0);
    }

    /**
     * Pause playback.
     * PUT /v1/me/player/pause
     *
     * @param userId User ID
     * @param deviceId Optional device ID
     * @throws SpotifyApiException if pause fails
     */
    public void pauseTrack(UUID userId, String deviceId) {
        LOG.debug("Pausing playback for user: {}", userId);

        // Get valid access token
        String accessToken = tokenRefreshService.getValidAccessToken(userId);

        // Build URL
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(SPOTIFY_API_BASE_URL + "/me/player/pause");

        if (deviceId != null && !deviceId.trim().isEmpty()) {
            builder.queryParam("device_id", deviceId);
        }

        String url = builder.toUriString();

        // Prepare headers
        HttpHeaders headers = createAuthHeaders(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);

            if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                LOG.debug("Successfully paused playback for user: {}", userId);
            } else {
                LOG.error("Failed to pause playback. Status: {}", response.getStatusCode());
                throw new SpotifyApiException("Failed to pause playback", "pause_track");
            }
        } catch (HttpClientErrorException e) {
            handleHttpError(e, "pause track");
            throw new SpotifyApiException("Failed to pause track: " + e.getMessage(), "pause_track", e);
        } catch (Exception e) {
            LOG.error("Error pausing track: {}", e.getMessage(), e);
            throw new SpotifyApiException("Error pausing track: " + e.getMessage(), "pause_track", e);
        }
    }

    /**
     * Pause playback without device ID.
     *
     * @param userId User ID
     */
    public void pauseTrack(UUID userId) {
        pauseTrack(userId, null);
    }

    /**
     * Skip to the next track.
     * POST /v1/me/player/next
     *
     * @param userId User ID
     * @param deviceId Optional device ID
     * @throws SpotifyApiException if skip fails
     */
    public void skipTrack(UUID userId, String deviceId) {
        LOG.debug("Skipping to next track for user: {}", userId);

        // Get valid access token
        String accessToken = tokenRefreshService.getValidAccessToken(userId);

        // Build URL
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(SPOTIFY_API_BASE_URL + "/me/player/next");

        if (deviceId != null && !deviceId.trim().isEmpty()) {
            builder.queryParam("device_id", deviceId);
        }

        String url = builder.toUriString();

        // Prepare headers
        HttpHeaders headers = createAuthHeaders(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);

            if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                LOG.debug("Successfully skipped track for user: {}", userId);
            } else {
                LOG.error("Failed to skip track. Status: {}", response.getStatusCode());
                throw new SpotifyApiException("Failed to skip track", "skip_track");
            }
        } catch (HttpClientErrorException e) {
            handleHttpError(e, "skip track");
            throw new SpotifyApiException("Failed to skip track: " + e.getMessage(), "skip_track", e);
        } catch (Exception e) {
            LOG.error("Error skipping track: {}", e.getMessage(), e);
            throw new SpotifyApiException("Error skipping track: " + e.getMessage(), "skip_track", e);
        }
    }

    /**
     * Skip to next track without device ID.
     *
     * @param userId User ID
     */
    public void skipTrack(UUID userId) {
        skipTrack(userId, null);
    }

    /**
     * Skip to the previous track.
     * POST /v1/me/player/previous
     *
     * @param userId User ID
     * @param deviceId Optional device ID
     * @throws SpotifyApiException if skip fails
     */
    public void previousTrack(UUID userId, String deviceId) {
        LOG.debug("Skipping to previous track for user: {}", userId);

        // Get valid access token
        String accessToken = tokenRefreshService.getValidAccessToken(userId);

        // Build URL
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(SPOTIFY_API_BASE_URL + "/me/player/previous");

        if (deviceId != null && !deviceId.trim().isEmpty()) {
            builder.queryParam("device_id", deviceId);
        }

        String url = builder.toUriString();

        // Prepare headers
        HttpHeaders headers = createAuthHeaders(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);

            if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                LOG.debug("Successfully skipped to previous track for user: {}", userId);
            } else {
                LOG.error("Failed to skip to previous track. Status: {}", response.getStatusCode());
                throw new SpotifyApiException("Failed to skip to previous track", "previous_track");
            }
        } catch (HttpClientErrorException e) {
            handleHttpError(e, "skip to previous track");
            throw new SpotifyApiException("Failed to skip to previous track: " + e.getMessage(), "previous_track", e);
        } catch (Exception e) {
            LOG.error("Error skipping to previous track: {}", e.getMessage(), e);
            throw new SpotifyApiException("Error skipping to previous track: " + e.getMessage(), "previous_track", e);
        }
    }

    /**
     * Skip to previous track without device ID.
     *
     * @param userId User ID
     */
    public void previousTrack(UUID userId) {
        previousTrack(userId, null);
    }

    /**
     * Seek to a specific position in the currently playing track.
     * PUT /v1/me/player/seek
     *
     * @param userId User ID
     * @param positionMs Target position in milliseconds
     * @param deviceId Optional device ID
     * @throws SpotifyApiException if seek fails
     */
    public void seekTrack(UUID userId, int positionMs, String deviceId) {
        LOG.debug("Seeking to position {}ms for user: {}", positionMs, userId);

        if (positionMs < 0) {
            throw new IllegalArgumentException("Position must be >= 0");
        }

        // Get valid access token
        String accessToken = tokenRefreshService.getValidAccessToken(userId);

        // Build URL with required position_ms parameter
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(SPOTIFY_API_BASE_URL + "/me/player/seek").queryParam(
            "position_ms",
            positionMs
        );

        if (deviceId != null && !deviceId.trim().isEmpty()) {
            builder.queryParam("device_id", deviceId);
        }

        String url = builder.toUriString();

        // Prepare headers
        HttpHeaders headers = createAuthHeaders(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);

            if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                LOG.debug("Successfully seeked to position {}ms for user: {}", positionMs, userId);
            } else {
                LOG.error("Failed to seek track. Status: {}", response.getStatusCode());
                throw new SpotifyApiException("Failed to seek track", "seek_track");
            }
        } catch (HttpClientErrorException e) {
            handleHttpError(e, "seek track");
            throw new SpotifyApiException("Failed to seek track: " + e.getMessage(), "seek_track", e);
        } catch (Exception e) {
            LOG.error("Error seeking track: {}", e.getMessage(), e);
            throw new SpotifyApiException("Error seeking track: " + e.getMessage(), "seek_track", e);
        }
    }

    /**
     * Seek to position without device ID.
     *
     * @param userId User ID
     * @param positionMs Target position in milliseconds
     */
    public void seekTrack(UUID userId, int positionMs) {
        seekTrack(userId, positionMs, null);
    }

    /**
     * Get the current playback state.
     * GET /v1/me/player
     *
     * @param userId User ID
     * @return Current playback state as JsonNode
     * @throws SpotifyApiException if retrieval fails
     */
    public JsonNode getPlaybackState(UUID userId) {
        LOG.debug("Getting playback state for user: {}", userId);

        // Get valid access token
        String accessToken = tokenRefreshService.getValidAccessToken(userId);

        // Build URL
        String url = SPOTIFY_API_BASE_URL + "/me/player";

        // Prepare headers
        HttpHeaders headers = createAuthHeaders(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode playbackState = objectMapper.readTree(response.getBody());
                LOG.debug("Successfully retrieved playback state for user: {}", userId);
                return playbackState;
            } else if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                // No active playback
                LOG.debug("No active playback for user: {}", userId);
                return null;
            } else {
                LOG.error("Failed to get playback state. Status: {}", response.getStatusCode());
                throw new SpotifyApiException("Failed to get playback state", "get_playback_state");
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NO_CONTENT) {
                return null;
            }
            handleHttpError(e, "get playback state");
            throw new SpotifyApiException("Failed to get playback state: " + e.getMessage(), "get_playback_state", e);
        } catch (Exception e) {
            LOG.error("Error getting playback state: {}", e.getMessage(), e);
            throw new SpotifyApiException("Error getting playback state: " + e.getMessage(), "get_playback_state", e);
        }
    }

    /**
     * Get currently playing track.
     * GET /v1/me/player/currently-playing
     *
     * @param userId User ID
     * @return Currently playing track as JsonNode
     * @throws SpotifyApiException if retrieval fails
     */
    public JsonNode getCurrentlyPlaying(UUID userId) {
        LOG.debug("Getting currently playing track for user: {}", userId);

        // Get valid access token
        String accessToken = tokenRefreshService.getValidAccessToken(userId);

        // Build URL
        String url = SPOTIFY_API_BASE_URL + "/me/player/currently-playing";

        // Prepare headers
        HttpHeaders headers = createAuthHeaders(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode currentTrack = objectMapper.readTree(response.getBody());
                LOG.debug("Successfully retrieved currently playing track for user: {}", userId);
                return currentTrack;
            } else if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                LOG.debug("No track currently playing for user: {}", userId);
                return null;
            } else {
                LOG.error("Failed to get currently playing track. Status: {}", response.getStatusCode());
                throw new SpotifyApiException("Failed to get currently playing track", "get_currently_playing");
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NO_CONTENT) {
                return null;
            }
            handleHttpError(e, "get currently playing track");
            throw new SpotifyApiException("Failed to get currently playing track: " + e.getMessage(), "get_currently_playing", e);
        } catch (Exception e) {
            LOG.error("Error getting currently playing track: {}", e.getMessage(), e);
            throw new SpotifyApiException("Error getting currently playing track: " + e.getMessage(), "get_currently_playing", e);
        }
    }

    /**
     * Add a track to the playback queue.
     * POST /v1/me/player/queue
     *
     * @param userId User ID
     * @param trackUri Spotify track URI to add to queue
     * @param deviceId Optional device ID
     * @throws SpotifyApiException if adding to queue fails
     */
    public void addToQueue(UUID userId, String trackUri, String deviceId) {
        LOG.debug("Adding track to queue for user: {}", userId);

        if (trackUri == null || trackUri.trim().isEmpty()) {
            throw new IllegalArgumentException("Track URI cannot be empty");
        }

        // Get valid access token
        String accessToken = tokenRefreshService.getValidAccessToken(userId);

        // Build URL with required uri parameter
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(SPOTIFY_API_BASE_URL + "/me/player/queue").queryParam(
            "uri",
            trackUri
        );

        if (deviceId != null && !deviceId.trim().isEmpty()) {
            builder.queryParam("device_id", deviceId);
        }

        String url = builder.toUriString();

        // Prepare headers
        HttpHeaders headers = createAuthHeaders(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);

            if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                LOG.debug("Successfully added track to queue for user: {}", userId);
            } else {
                LOG.error("Failed to add track to queue. Status: {}", response.getStatusCode());
                throw new SpotifyApiException("Failed to add track to queue", "add_to_queue");
            }
        } catch (HttpClientErrorException e) {
            handleHttpError(e, "add to queue");
            throw new SpotifyApiException("Failed to add track to queue: " + e.getMessage(), "add_to_queue", e);
        } catch (Exception e) {
            LOG.error("Error adding track to queue: {}", e.getMessage(), e);
            throw new SpotifyApiException("Error adding track to queue: " + e.getMessage(), "add_to_queue", e);
        }
    }

    /**
     * Add to queue without device ID.
     *
     * @param userId User ID
     * @param trackUri Track URI to add
     */
    public void addToQueue(UUID userId, String trackUri) {
        addToQueue(userId, trackUri, null);
    }

    /**
     * Creates HTTP headers with authorization token.
     *
     * @param accessToken Spotify access token
     * @return HttpHeaders with authorization and content-type
     */
    private HttpHeaders createAuthHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * Handles HTTP client errors with detailed logging.
     *
     * @param e HttpClientErrorException
     * @param operation Operation name for logging
     */
    private void handleHttpError(HttpClientErrorException e, String operation) {
        LOG.error("HTTP error during {}: Status={}, Body={}", operation, e.getStatusCode(), e.getResponseBodyAsString());

        if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            LOG.error("Unauthorized access - token may be invalid or expired");
        } else if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
            LOG.error("Forbidden - insufficient permissions or premium required");
        } else if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
            LOG.error("Resource not found - track or device may not exist");
        } else if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
            LOG.error("Rate limit exceeded");
        }
    }

    // Overloaded methods that accept AppUser instead of UUID

    public SpotifyTrackSearchResultDTO searchTracks(AppUser appUser, String query) {
        return searchTracks(appUser.getId(), query);
    }

    public JsonNode getTrack(AppUser appUser, String trackId) {
        return getTrack(appUser.getId(), trackId);
    }

    public void playTrack(AppUser appUser, String trackUri) {
        playTrack(appUser.getId(), trackUri);
    }

    public void pauseTrack(AppUser appUser) {
        pauseTrack(appUser.getId());
    }

    public void skipTrack(AppUser appUser) {
        skipTrack(appUser.getId());
    }

    public void seekTrack(AppUser appUser, int positionMs) {
        seekTrack(appUser.getId(), positionMs);
    }

    public JsonNode getPlaybackState(AppUser appUser) {
        return getPlaybackState(appUser.getId());
    }

    public void addToQueue(AppUser appUser, String trackUri) {
        addToQueue(appUser.getId(), trackUri);
    }
}
