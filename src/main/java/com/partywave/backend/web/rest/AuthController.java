package com.partywave.backend.web.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.partywave.backend.domain.AppUser;
import com.partywave.backend.service.AppUserService;
import com.partywave.backend.service.JwtAuthenticationService;
import com.partywave.backend.service.SpotifyAuthService;
import com.partywave.backend.service.dto.JwtRefreshRequestDTO;
import com.partywave.backend.service.dto.JwtTokenResponseDTO;
import com.partywave.backend.service.dto.RefreshTokenRequestDTO;
import com.partywave.backend.service.dto.RefreshTokenResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
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
 * API documentation is maintained in openapi.yml file.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger LOG = LoggerFactory.getLogger(AuthController.class);

    private final SpotifyAuthService spotifyAuthService;
    private final AppUserService appUserService;
    private final JwtAuthenticationService jwtAuthenticationService;

    // In-memory state storage for CSRF protection
    // TODO: Replace with Redis or database storage in production
    private final Map<String, String> stateStore = new HashMap<>();

    public AuthController(
        SpotifyAuthService spotifyAuthService,
        AppUserService appUserService,
        JwtAuthenticationService jwtAuthenticationService
    ) {
        this.spotifyAuthService = spotifyAuthService;
        this.appUserService = appUserService;
        this.jwtAuthenticationService = jwtAuthenticationService;
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
     * fetches user profile, creates/updates AppUser, and generates JWT tokens.
     *
     * @param code Authorization code from Spotify
     * @param state CSRF protection state parameter
     * @param request HTTP request
     * @return ResponseEntity with JWT tokens and user info
     */
    @GetMapping("/spotify/callback")
    public ResponseEntity<JwtTokenResponseDTO> spotifyCallback(
        @RequestParam("code") String code,
        @RequestParam(value = "state", required = false) String state,
        HttpServletRequest request
    ) {
        LOG.debug("REST request to handle Spotify callback with code");

        try {
            // Validate state parameter for CSRF protection
            if (state != null && !stateStore.containsKey(state)) {
                LOG.warn("Invalid state parameter received in callback");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            // Remove state from store after validation
            if (state != null) {
                stateStore.remove(state);
            }

            // Exchange code for Spotify tokens
            JsonNode tokenResponse = spotifyAuthService.exchangeCodeForTokens(code);
            String spotifyAccessToken = tokenResponse.get("access_token").asText();
            String spotifyRefreshToken = tokenResponse.has("refresh_token") ? tokenResponse.get("refresh_token").asText() : null;
            int spotifyExpiresIn = tokenResponse.get("expires_in").asInt();
            String scope = tokenResponse.has("scope") ? tokenResponse.get("scope").asText() : null;

            // Fetch Spotify user profile
            JsonNode userProfile = spotifyAuthService.fetchUserProfile(spotifyAccessToken);

            // Extract client IP address
            String ipAddress = extractIpAddress(request);

            // Create or update AppUser from Spotify profile
            AppUser appUser = appUserService.createOrUpdateFromSpotifyProfile(
                userProfile,
                spotifyAccessToken,
                spotifyRefreshToken,
                spotifyExpiresIn,
                scope,
                ipAddress
            );

            // Generate PartyWave JWT tokens
            JwtTokenResponseDTO jwtResponse = jwtAuthenticationService.generateTokens(appUser, request);

            LOG.debug("Successfully authenticated user and generated JWT tokens: {}", appUser.getId());
            return ResponseEntity.ok(jwtResponse);
        } catch (Exception e) {
            LOG.error("Error handling Spotify callback: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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

    /**
     * POST /api/auth/refresh : Refreshes PartyWave JWT tokens
     *
     * Client uses this endpoint when their JWT access token expires.
     * Requires a valid refresh token to generate new access and refresh tokens.
     *
     * @param refreshRequest Request containing JWT refresh token
     * @param request HTTP request
     * @return ResponseEntity with new JWT tokens
     */
    @PostMapping("/refresh")
    public ResponseEntity<JwtTokenResponseDTO> refreshJwtToken(
        @Valid @RequestBody JwtRefreshRequestDTO refreshRequest,
        HttpServletRequest request
    ) {
        LOG.debug("REST request to refresh JWT tokens");

        try {
            JwtTokenResponseDTO response = jwtAuthenticationService.refreshTokens(refreshRequest, request);
            LOG.debug("Successfully refreshed JWT tokens");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LOG.error("Error refreshing JWT tokens: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * POST /api/auth/logout : Logout user by revoking refresh token
     *
     * @param refreshRequest Request containing JWT refresh token to revoke
     * @return ResponseEntity with no content
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody JwtRefreshRequestDTO refreshRequest) {
        LOG.debug("REST request to logout (revoke refresh token)");

        try {
            jwtAuthenticationService.revokeToken(refreshRequest.getRefreshToken());
            LOG.debug("Successfully revoked refresh token");
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            LOG.error("Error revoking refresh token: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Extract client IP address from HTTP request.
     *
     * @param request HTTP request
     * @return IP address
     */
    private String extractIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("X-Real-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }

        // If multiple IPs (proxy chain), take the first one
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }

        return ipAddress != null ? ipAddress : "unknown";
    }
}
