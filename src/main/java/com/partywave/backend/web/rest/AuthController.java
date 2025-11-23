package com.partywave.backend.web.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.partywave.backend.service.SpotifyAuthService;
import com.partywave.backend.service.dto.RefreshTokenRequestDTO;
import com.partywave.backend.service.dto.RefreshTokenResponseDTO;
import com.partywave.backend.service.dto.SpotifyAuthResponseDTO;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for handling Spotify OAuth2 authentication.
 * Provides endpoints for initiating login and handling callback.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger LOG = LoggerFactory.getLogger(AuthController.class);

    private final SpotifyAuthService spotifyAuthService;

    // In-memory state storage for CSRF protection
    // TODO: Replace with Redis or database storage in production
    private final Map<String, String> stateStore = new HashMap<>();

    public AuthController(SpotifyAuthService spotifyAuthService) {
        this.spotifyAuthService = spotifyAuthService;
    }

    /**
     * GET /api/auth/spotify/login : Initiates Spotify OAuth2 login flow
     *
     * Redirects the user to Spotify's authorization page.
     * Generates a random state parameter for CSRF protection.
     *
     * @return 302 redirect to Spotify authorization URL
     */
    @GetMapping("/spotify/login")
    public ResponseEntity<Void> spotifyLogin() {
        LOG.debug("REST request to initiate Spotify login");

        try {
            // Generate random state for CSRF protection
            String state = UUID.randomUUID().toString();
            stateStore.put(state, state); // Store state for validation

            // Get authorization URL from service
            String authUrl = spotifyAuthService.getAuthorizationUrl(state);

            LOG.debug("Redirecting to Spotify authorization URL");
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(authUrl)).build();
        } catch (Exception e) {
            LOG.error("Error initiating Spotify login: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/auth/spotify/callback : Handles Spotify OAuth2 callback
     *
     * Receives authorization code from Spotify, exchanges it for tokens,
     * and fetches user profile information.
     *
     * @param code Authorization code from Spotify
     * @param state CSRF protection state parameter
     * @return ResponseEntity with user profile and tokens, or error status
     */
    @GetMapping("/spotify/callback")
    public ResponseEntity<SpotifyAuthResponseDTO> spotifyCallback(
        @RequestParam("code") String code,
        @RequestParam(value = "state", required = false) String state
    ) {
        LOG.debug("REST request to handle Spotify callback with code");

        try {
            // Validate state parameter for CSRF protection
            if (state != null && !stateStore.containsKey(state)) {
                LOG.warn("Invalid state parameter received in callback");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new SpotifyAuthResponseDTO("Invalid state parameter", null, null, null)
                );
            }

            // Remove state from store after validation
            if (state != null) {
                stateStore.remove(state);
            }

            // Exchange code for tokens
            JsonNode tokenResponse = spotifyAuthService.exchangeCodeForTokens(code);
            String accessToken = tokenResponse.get("access_token").asText();
            String refreshToken = tokenResponse.has("refresh_token") ? tokenResponse.get("refresh_token").asText() : null;
            int expiresIn = tokenResponse.get("expires_in").asInt();

            // Fetch user profile
            JsonNode userProfile = spotifyAuthService.fetchUserProfile(accessToken);

            // Build response
            SpotifyAuthResponseDTO response = new SpotifyAuthResponseDTO(
                null,
                accessToken,
                refreshToken,
                expiresIn,
                userProfile.get("id").asText(),
                userProfile.has("email") ? userProfile.get("email").asText() : null,
                userProfile.has("display_name") ? userProfile.get("display_name").asText() : null,
                userProfile.has("images") && userProfile.get("images").size() > 0
                    ? userProfile.get("images").get(0).get("url").asText()
                    : null
            );

            LOG.debug("Successfully authenticated Spotify user: {}", response.getSpotifyId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LOG.error("Error handling Spotify callback: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new SpotifyAuthResponseDTO("Error during authentication: " + e.getMessage(), null, null, null)
            );
        }
    }

    /**
     * POST /api/auth/spotify/refresh : Refreshes Spotify access token
     *
     * @param refreshTokenRequest Request containing refresh token
     * @return ResponseEntity with new access token
     */
    @PostMapping("/spotify/refresh")
    public ResponseEntity<RefreshTokenResponseDTO> refreshSpotifyToken(@Valid @RequestBody RefreshTokenRequestDTO refreshTokenRequest) {
        LOG.debug("REST request to refresh Spotify access token");

        try {
            JsonNode tokenResponse = spotifyAuthService.refreshAccessToken(refreshTokenRequest.getRefreshToken());
            String accessToken = tokenResponse.get("access_token").asText();
            int expiresIn = tokenResponse.get("expires_in").asInt();

            RefreshTokenResponseDTO response = new RefreshTokenResponseDTO(accessToken, expiresIn);
            LOG.debug("Successfully refreshed Spotify access token");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LOG.error("Error refreshing Spotify token: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
