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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    private final com.partywave.backend.service.redis.PkceRedisService pkceRedisService;
    private final com.partywave.backend.repository.AppUserRepository appUserRepository;

    // In-memory state storage for CSRF protection
    // TODO: Replace with Redis or database storage in production
    private final Map<String, String> stateStore = new HashMap<>();

    public AuthController(
        SpotifyAuthService spotifyAuthService,
        AppUserService appUserService,
        JwtAuthenticationService jwtAuthenticationService,
        com.partywave.backend.service.redis.PkceRedisService pkceRedisService,
        com.partywave.backend.repository.AppUserRepository appUserRepository
    ) {
        this.spotifyAuthService = spotifyAuthService;
        this.appUserService = appUserService;
        this.jwtAuthenticationService = jwtAuthenticationService;
        this.pkceRedisService = pkceRedisService;
        this.appUserRepository = appUserRepository;
    }

    /**
     * GET /api/auth/spotify/login : Initiates Spotify OAuth2 login flow
     *
     * Redirects the user to Spotify's authorization page.
     * Generates a random state parameter for CSRF protection.
     * Optionally supports PKCE (Proof Key for Code Exchange) for enhanced security.
     *
     * @param codeChallenge Optional PKCE code challenge (base64url-encoded SHA-256 hash of code verifier)
     * @param codeChallengeMethod Optional PKCE challenge method (must be "S256" if provided)
     * @return 302 redirect to Spotify authorization URL
     */
    @GetMapping("/spotify/login")
    public ResponseEntity<Void> spotifyLogin(
        @RequestParam(required = false) String codeChallenge,
        @RequestParam(required = false) String codeChallengeMethod
    ) {
        LOG.debug("REST request to initiate Spotify login (PKCE enabled: {})", codeChallenge != null);

        // Validate PKCE parameters if provided
        if (codeChallenge != null) {
            if (codeChallengeMethod != null && !"S256".equals(codeChallengeMethod)) {
                LOG.warn("Invalid code challenge method: {}. Only S256 is supported.", codeChallengeMethod);
                throw new com.partywave.backend.exception.InvalidPkceException("Invalid code challenge method. Only S256 is supported.");
            }
        }

        // Generate random state for CSRF protection
        String state = UUID.randomUUID().toString();
        stateStore.put(state, state); // Store state for validation

        // Store code challenge in Redis if PKCE is enabled
        if (codeChallenge != null) {
            pkceRedisService.storeChallengeForState(state, codeChallenge);
            LOG.debug("Stored PKCE code challenge for state: {}", state);
        }

        // Get authorization URL from service
        String authUrl = spotifyAuthService.getAuthorizationUrl(state);

        LOG.debug("Redirecting to Spotify authorization URL");
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(authUrl)).build();
    }

    /**
     * GET /api/auth/spotify/callback : Handles Spotify OAuth2 callback
     *
     * Receives authorization code from Spotify, exchanges it for tokens,
     * fetches user profile, creates/updates AppUser.
     *
     * If PKCE is enabled (code challenge was provided during login):
     * - Returns a temporary authorization code that must be exchanged for JWT tokens
     * - Client must call POST /api/auth/token with code verifier to get JWT tokens
     *
     * If PKCE is not enabled (backward compatibility):
     * - Generates JWT tokens immediately
     * - Redirects to frontend with tokens in URL hash fragment
     *
     * @param code Authorization code from Spotify
     * @param state CSRF protection state parameter
     * @param request HTTP request
     * @return 302 redirect to frontend with authorization code or JWT tokens
     */
    @GetMapping("/spotify/callback")
    public ResponseEntity<Void> spotifyCallback(
        @RequestParam("code") String code,
        @RequestParam(value = "state", required = false) String state,
        HttpServletRequest request
    ) {
        LOG.debug("REST request to handle Spotify callback with code");

        // Validate state parameter for CSRF protection
        if (state != null && !stateStore.containsKey(state)) {
            LOG.warn("Invalid state parameter received in callback");
            // Redirect to frontend with error
            return buildErrorRedirect();
        }

        // Check if PKCE was used by retrieving code challenge from Redis
        String codeChallenge = null;
        if (state != null) {
            codeChallenge = pkceRedisService.getChallengeByState(state);
        }

        boolean pkceEnabled = codeChallenge != null;
        LOG.debug("PKCE enabled for this callback: {}", pkceEnabled);

        // Remove state from store after validation
        if (state != null) {
            stateStore.remove(state);
        }

        try {
            // Exchange code for Spotify tokens - exceptions will be handled by global exception handler
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

            LOG.debug("Successfully authenticated user with Spotify: {}", appUser.getId());

            if (pkceEnabled) {
                // PKCE flow: Generate authorization code and store it in Redis
                com.partywave.backend.service.PkceService pkceService = new com.partywave.backend.service.PkceService();
                String authorizationCode = pkceService.generateAuthorizationCode();

                // Store authorization code with user ID and code challenge
                pkceRedisService.storeAuthorizationCode(authorizationCode, appUser.getId(), codeChallenge);

                // Clean up code challenge from Redis
                if (state != null) {
                    pkceRedisService.deleteChallengeForState(state);
                }

                LOG.debug("Generated authorization code for PKCE flow");

                // Build redirect URL with authorization code in hash fragment
                String redirectUrl = buildRedirectUrlWithAuthorizationCode(authorizationCode);
                return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
            } else {
                // Legacy flow: Generate JWT tokens immediately
                JwtTokenResponseDTO jwtResponse = jwtAuthenticationService.generateTokens(appUser, request);

                LOG.debug("Successfully generated JWT tokens: {}", appUser.getId());

                // Build redirect URL with tokens in hash fragment
                String redirectUrl = buildRedirectUrlWithTokens(jwtResponse);
                return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
            }
        } catch (Exception e) {
            LOG.error("Error during Spotify callback: {}", e.getMessage(), e);
            // Clean up code challenge if it exists
            if (state != null && codeChallenge != null) {
                pkceRedisService.deleteChallengeForState(state);
            }
            // Redirect to frontend with error
            return buildErrorRedirect();
        }
    }

    /**
     * Builds redirect URL to frontend with JWT tokens in hash fragment.
     * Hash fragment is not sent to server and provides secure token transmission.
     *
     * @param jwtResponse JWT token response containing access and refresh tokens
     * @return Redirect URL with tokens in hash fragment
     */
    private String buildRedirectUrlWithTokens(JwtTokenResponseDTO jwtResponse) {
        String frontendUrl = spotifyAuthService.getFrontendUrl();

        // Build hash fragment with encoded tokens
        StringBuilder hashFragment = new StringBuilder();
        hashFragment.append("access_token=").append(URLEncoder.encode(jwtResponse.getAccessToken(), StandardCharsets.UTF_8));
        hashFragment.append("&refresh_token=").append(URLEncoder.encode(jwtResponse.getRefreshToken(), StandardCharsets.UTF_8));
        hashFragment.append("&token_type=").append(URLEncoder.encode(jwtResponse.getTokenType(), StandardCharsets.UTF_8));
        hashFragment.append("&expires_in=").append(jwtResponse.getExpiresIn());

        if (jwtResponse.getUser() != null) {
            hashFragment.append("&user_id=").append(URLEncoder.encode(jwtResponse.getUser().getId(), StandardCharsets.UTF_8));
        }

        // Construct redirect URL: frontend-url/pkce-test.html#tokens
        String redirectUrl = frontendUrl.endsWith("/") ? frontendUrl : frontendUrl + "/";
        redirectUrl += "pkce-test.html#" + hashFragment.toString();

        LOG.debug("Built redirect URL to frontend (length: {})", redirectUrl.length());
        return redirectUrl;
    }

    /**
     * Builds redirect URL to frontend with authorization code in hash fragment.
     * Used for PKCE flow - client must exchange this code for JWT tokens.
     *
     * @param authorizationCode The temporary authorization code
     * @return Redirect URL with authorization code in hash fragment
     */
    private String buildRedirectUrlWithAuthorizationCode(String authorizationCode) {
        String frontendUrl = spotifyAuthService.getFrontendUrl();

        // Build hash fragment with authorization code
        StringBuilder hashFragment = new StringBuilder();
        hashFragment.append("authorization_code=").append(URLEncoder.encode(authorizationCode, StandardCharsets.UTF_8));
        hashFragment.append("&expires_in=").append(300); // 5 minutes

        // Construct redirect URL: frontend-url/pkce-test.html#authorization_code
        String redirectUrl = frontendUrl.endsWith("/") ? frontendUrl : frontendUrl + "/";
        redirectUrl += "pkce-test.html#" + hashFragment.toString();

        LOG.debug("Built redirect URL to frontend with authorization code");
        return redirectUrl;
    }

    /**
     * Builds redirect URL to frontend with error indication in hash fragment.
     *
     * @return Redirect response with error URL
     */
    private ResponseEntity<Void> buildErrorRedirect() {
        String frontendUrl = spotifyAuthService.getFrontendUrl();
        String redirectUrl = (frontendUrl.endsWith("/") ? frontendUrl : frontendUrl + "/") + "auth/callback#error=authentication_failed";
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
    }

    /**
     * POST /api/auth/token : Exchanges authorization code for JWT tokens using PKCE
     *
     * This endpoint is used in the PKCE flow after the client receives an authorization code
     * from the Spotify callback. The client must provide the code verifier to prove they
     * initiated the original authorization request.
     *
     * @param tokenRequest Request containing authorization code and code verifier
     * @param request HTTP request (for IP and user agent)
     * @return ResponseEntity with JWT access and refresh tokens
     */
    @PostMapping("/token")
    public ResponseEntity<JwtTokenResponseDTO> exchangeToken(
        @Valid @RequestBody com.partywave.backend.service.dto.TokenExchangeRequestDTO tokenRequest,
        HttpServletRequest request
    ) {
        LOG.debug("REST request to exchange authorization code for JWT tokens");

        try {
            // Retrieve authorization code data from Redis
            com.partywave.backend.service.redis.PkceRedisService.AuthorizationCodeData authData = pkceRedisService.getAuthorizationCodeData(
                tokenRequest.getAuthorizationCode()
            );

            if (authData == null) {
                LOG.warn("Authorization code not found or expired: {}", tokenRequest.getAuthorizationCode());
                LOG.warn("This could mean: 1) Code expired (5 min TTL), 2) Redis not connected, 3) Code was already used");
                throw new com.partywave.backend.exception.InvalidAuthorizationCodeException(
                    "Authorization code is invalid or has expired. Please try logging in again."
                );
            }

            // Validate PKCE: code verifier must match code challenge
            com.partywave.backend.service.PkceService pkceService = new com.partywave.backend.service.PkceService();
            boolean isValid = pkceService.validateCodeVerifier(tokenRequest.getCodeVerifier(), authData.getCodeChallenge());

            if (!isValid) {
                LOG.warn("PKCE validation failed: code verifier does not match code challenge");
                // Delete authorization code to prevent retry attacks
                pkceRedisService.deleteAuthorizationCode(tokenRequest.getAuthorizationCode());
                throw new com.partywave.backend.exception.InvalidPkceException("Code verifier validation failed");
            }

            LOG.debug("PKCE validation successful");

            // Load user from database with images eagerly fetched
            UUID userId = UUID.fromString(authData.getUserId());
            AppUser appUser = appUserRepository
                .findByIdWithImages(userId)
                .orElseThrow(() -> new com.partywave.backend.exception.ResourceNotFoundException("User", "id", userId));

            // Generate JWT tokens
            JwtTokenResponseDTO jwtResponse = jwtAuthenticationService.generateTokens(appUser, request);

            // Delete authorization code after successful exchange (single use)
            pkceRedisService.deleteAuthorizationCode(tokenRequest.getAuthorizationCode());

            LOG.debug("Successfully exchanged authorization code for JWT tokens: {}", userId);
            return ResponseEntity.ok(jwtResponse);
        } catch (
            com.partywave.backend.exception.InvalidAuthorizationCodeException
            | com.partywave.backend.exception.InvalidPkceException
            | com.partywave.backend.exception.ResourceNotFoundException e
        ) {
            LOG.warn("Token exchange failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            LOG.error("Error exchanging authorization code for JWT tokens: {}", e.getMessage(), e);
            LOG.error("Exception type: {}, Cause: {}", e.getClass().getName(), e.getCause());
            throw new com.partywave.backend.exception.InvalidAuthorizationCodeException(
                "Failed to exchange authorization code: " + e.getMessage(),
                e
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

        // Refresh token - exceptions will be handled by global exception handler
        JsonNode tokenResponse = spotifyAuthService.refreshAccessToken(refreshTokenRequest.getRefreshToken());
        String accessToken = tokenResponse.get("access_token").asText();
        int expiresIn = tokenResponse.get("expires_in").asInt();

        RefreshTokenResponseDTO response = new RefreshTokenResponseDTO(accessToken, expiresIn);
        LOG.debug("Successfully refreshed Spotify access token");
        return ResponseEntity.ok(response);
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

        // Refresh tokens - exceptions will be handled by global exception handler
        JwtTokenResponseDTO response = jwtAuthenticationService.refreshTokens(refreshRequest, request);
        LOG.debug("Successfully refreshed JWT tokens");
        return ResponseEntity.ok(response);
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

        // Revoke token - exceptions will be handled by global exception handler
        jwtAuthenticationService.revokeToken(refreshRequest.getRefreshToken());
        LOG.debug("Successfully revoked refresh token");
        return ResponseEntity.noContent().build();
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
