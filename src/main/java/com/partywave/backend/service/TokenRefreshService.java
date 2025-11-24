package com.partywave.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.partywave.backend.domain.AppUser;
import com.partywave.backend.domain.UserToken;
import com.partywave.backend.exception.SpotifyApiException;
import com.partywave.backend.repository.UserTokenRepository;
import com.partywave.backend.security.TokenEncryptionService;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for automatically refreshing expired Spotify access tokens.
 * Checks token expiration and refreshes when necessary before API calls.
 */
@Service
public class TokenRefreshService {

    private static final Logger LOG = LoggerFactory.getLogger(TokenRefreshService.class);

    private final UserTokenRepository userTokenRepository;
    private final SpotifyAuthService spotifyAuthService;
    private final TokenEncryptionService tokenEncryptionService;

    public TokenRefreshService(
        UserTokenRepository userTokenRepository,
        SpotifyAuthService spotifyAuthService,
        TokenEncryptionService tokenEncryptionService
    ) {
        this.userTokenRepository = userTokenRepository;
        this.spotifyAuthService = spotifyAuthService;
        this.tokenEncryptionService = tokenEncryptionService;
    }

    /**
     * Gets a valid access token for the user, refreshing if necessary.
     * This method checks if the access token is expired and automatically
     * refreshes it using the refresh token if needed.
     *
     * @param userId User ID (UUID)
     * @return Valid decrypted access token
     * @throws SpotifyApiException if token refresh fails or user has no tokens
     */
    @Transactional
    public String getValidAccessToken(UUID userId) {
        LOG.debug("Getting valid access token for user: {}", userId);

        // Find user token
        UserToken userToken = findUserTokenByUserId(userId);

        // Check if token is expired or about to expire (within 60 seconds)
        if (isTokenExpired(userToken)) {
            LOG.debug("Access token expired for user: {}, refreshing...", userId);
            refreshAndUpdateToken(userToken);
        } else {
            LOG.debug("Access token is still valid for user: {}", userId);
        }

        // Decrypt and return access token
        try {
            return tokenEncryptionService.decrypt(userToken.getAccessToken());
        } catch (Exception e) {
            LOG.error("Failed to decrypt access token for user {}: {}", userId, e.getMessage(), e);
            throw new SpotifyApiException("Failed to decrypt access token", "token_decryption", e);
        }
    }

    /**
     * Gets a valid access token for the user by AppUser entity.
     *
     * @param appUser AppUser entity
     * @return Valid decrypted access token
     * @throws SpotifyApiException if token refresh fails or user has no tokens
     */
    @Transactional
    public String getValidAccessToken(AppUser appUser) {
        return getValidAccessToken(appUser.getId());
    }

    /**
     * Finds UserToken by user ID.
     *
     * @param userId User ID
     * @return UserToken entity
     * @throws SpotifyApiException if user has no tokens stored
     */
    private UserToken findUserTokenByUserId(UUID userId) {
        return userTokenRepository
            .findByAppUserId(userId)
            .orElseThrow(() -> {
                LOG.error("No Spotify tokens found for user: {}", userId);
                return new SpotifyApiException("User has no Spotify tokens stored", "no_tokens");
            });
    }

    /**
     * Checks if the access token is expired or about to expire.
     * Considers token expired if it expires within the next 60 seconds.
     *
     * @param userToken UserToken entity
     * @return true if token is expired or about to expire
     */
    private boolean isTokenExpired(UserToken userToken) {
        if (userToken.getExpiresAt() == null) {
            LOG.warn("Token expiration time is null, treating as expired");
            return true;
        }

        // Consider expired if expires within next 60 seconds (buffer for API call time)
        Instant expirationThreshold = Instant.now().plusSeconds(60);
        boolean expired = userToken.getExpiresAt().isBefore(expirationThreshold);

        if (expired) {
            LOG.debug("Token expired or expiring soon. Expires at: {}, Current time: {}", userToken.getExpiresAt(), Instant.now());
        }

        return expired;
    }

    /**
     * Refreshes the access token using the refresh token and updates the database.
     *
     * @param userToken UserToken entity to refresh
     * @throws SpotifyApiException if refresh fails
     */
    private void refreshAndUpdateToken(UserToken userToken) {
        try {
            // Decrypt refresh token
            String decryptedRefreshToken = tokenEncryptionService.decrypt(userToken.getRefreshToken());

            // Call Spotify API to refresh access token
            JsonNode tokenResponse = spotifyAuthService.refreshAccessToken(decryptedRefreshToken);

            // Extract new access token and expiration
            String newAccessToken = tokenResponse.get("access_token").asText();
            int expiresIn = tokenResponse.get("expires_in").asInt();

            // Update refresh token if provided (Spotify may rotate refresh tokens)
            if (tokenResponse.has("refresh_token")) {
                String newRefreshToken = tokenResponse.get("refresh_token").asText();
                userToken.setRefreshToken(tokenEncryptionService.encrypt(newRefreshToken));
                LOG.debug("Refresh token was rotated for user: {}", userToken.getAppUser().getId());
            }

            // Encrypt and update access token
            userToken.setAccessToken(tokenEncryptionService.encrypt(newAccessToken));
            userToken.setExpiresAt(Instant.now().plusSeconds(expiresIn));

            // Save updated token
            userTokenRepository.save(userToken);

            LOG.info("Successfully refreshed access token for user: {}", userToken.getAppUser().getId());
        } catch (SpotifyApiException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Failed to refresh access token for user {}: {}", userToken.getAppUser().getId(), e.getMessage(), e);
            throw new SpotifyApiException("Failed to refresh access token: " + e.getMessage(), "token_refresh", e);
        }
    }

    /**
     * Forces a token refresh for a user, even if the token is not expired.
     * Useful for testing or when token is known to be invalid.
     *
     * @param userId User ID
     * @return New valid access token
     * @throws SpotifyApiException if refresh fails
     */
    @Transactional
    public String forceRefreshToken(UUID userId) {
        LOG.debug("Forcing token refresh for user: {}", userId);
        UserToken userToken = findUserTokenByUserId(userId);
        refreshAndUpdateToken(userToken);

        try {
            return tokenEncryptionService.decrypt(userToken.getAccessToken());
        } catch (Exception e) {
            LOG.error("Failed to decrypt access token after refresh for user {}: {}", userId, e.getMessage(), e);
            throw new SpotifyApiException("Failed to decrypt access token after refresh", "token_decryption", e);
        }
    }
}
