package com.partywave.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.partywave.backend.domain.AppUser;
import com.partywave.backend.domain.AppUserImage;
import com.partywave.backend.domain.AppUserStats;
import com.partywave.backend.domain.UserToken;
import com.partywave.backend.domain.enumeration.AppUserStatus;
import com.partywave.backend.repository.AppUserImageRepository;
import com.partywave.backend.repository.AppUserRepository;
import com.partywave.backend.repository.AppUserStatsRepository;
import com.partywave.backend.repository.UserTokenRepository;
import com.partywave.backend.security.TokenEncryptionService;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing AppUser entities.
 * Handles user creation, updates, and Spotify profile synchronization.
 */
@Service
@Transactional
public class AppUserService {

    private static final Logger LOG = LoggerFactory.getLogger(AppUserService.class);

    private final AppUserRepository appUserRepository;
    private final UserTokenRepository userTokenRepository;
    private final AppUserStatsRepository appUserStatsRepository;
    private final AppUserImageRepository appUserImageRepository;
    private final TokenEncryptionService tokenEncryptionService;

    public AppUserService(
        AppUserRepository appUserRepository,
        UserTokenRepository userTokenRepository,
        AppUserStatsRepository appUserStatsRepository,
        AppUserImageRepository appUserImageRepository,
        TokenEncryptionService tokenEncryptionService
    ) {
        this.appUserRepository = appUserRepository;
        this.userTokenRepository = userTokenRepository;
        this.appUserStatsRepository = appUserStatsRepository;
        this.appUserImageRepository = appUserImageRepository;
        this.tokenEncryptionService = tokenEncryptionService;
    }

    /**
     * Create or update AppUser from Spotify profile.
     * If user exists, updates their profile information.
     * If user doesn't exist, creates a new user.
     *
     * @param spotifyProfile Spotify user profile JSON
     * @param spotifyAccessToken Spotify access token
     * @param spotifyRefreshToken Spotify refresh token
     * @param spotifyTokenExpiresIn Spotify token expiration time (seconds)
     * @param scope OAuth scopes granted
     * @param ipAddress User's IP address
     * @return AppUser entity
     */
    public AppUser createOrUpdateFromSpotifyProfile(
        JsonNode spotifyProfile,
        String spotifyAccessToken,
        String spotifyRefreshToken,
        int spotifyTokenExpiresIn,
        String scope,
        String ipAddress
    ) {
        String spotifyUserId = spotifyProfile.get("id").asText();
        LOG.debug("Creating or updating AppUser for Spotify user: {}", spotifyUserId);

        // Find existing user or create new one
        Optional<AppUser> existingUserOpt = appUserRepository.findBySpotifyUserId(spotifyUserId);

        AppUser appUser;
        boolean isNewUser = false;
        if (existingUserOpt.isPresent()) {
            // Update existing user
            appUser = existingUserOpt.get();
            LOG.debug("Updating existing AppUser: {}", appUser.getId());
        } else {
            // Create new user
            appUser = new AppUser();
            appUser.setSpotifyUserId(spotifyUserId);
            appUser.setStatus(AppUserStatus.ONLINE);
            isNewUser = true;
            LOG.debug("Creating new AppUser for Spotify user: {}", spotifyUserId);
        }

        // Update user profile from Spotify
        updateUserProfileFromSpotify(appUser, spotifyProfile, ipAddress);

        // Save user first to get ID
        appUser = appUserRepository.save(appUser);

        // For new users, initialize stats and images
        if (isNewUser) {
            // Initialize AppUserStats
            createAppUserStats(appUser);

            // Create AppUserImages from Spotify profile
            createAppUserImages(appUser, spotifyProfile);
        }

        // Update or create UserToken for Spotify tokens
        updateUserToken(appUser, spotifyAccessToken, spotifyRefreshToken, spotifyTokenExpiresIn, scope);

        LOG.debug("Successfully created/updated AppUser: {}", appUser.getId());
        return appUser;
    }

    /**
     * Update user profile fields from Spotify profile data.
     *
     * @param appUser AppUser to update
     * @param spotifyProfile Spotify profile JSON
     * @param ipAddress User's IP address
     */
    private void updateUserProfileFromSpotify(AppUser appUser, JsonNode spotifyProfile, String ipAddress) {
        // Required fields
        if (spotifyProfile.has("display_name") && !spotifyProfile.get("display_name").isNull()) {
            appUser.setDisplayName(spotifyProfile.get("display_name").asText());
        }

        if (spotifyProfile.has("email") && !spotifyProfile.get("email").isNull()) {
            appUser.setEmail(spotifyProfile.get("email").asText());
        }

        // Optional fields
        if (spotifyProfile.has("country") && !spotifyProfile.get("country").isNull()) {
            appUser.setCountry(spotifyProfile.get("country").asText());
        }

        if (spotifyProfile.has("href") && !spotifyProfile.get("href").isNull()) {
            appUser.setHref(spotifyProfile.get("href").asText());
        }

        if (spotifyProfile.has("uri") && !spotifyProfile.get("uri").isNull()) {
            appUser.setUrl(spotifyProfile.get("uri").asText());
        }

        if (spotifyProfile.has("type") && !spotifyProfile.get("type").isNull()) {
            appUser.setType(spotifyProfile.get("type").asText());
        }

        // Update IP address and last active timestamp
        appUser.setIpAddress(ipAddress);
        appUser.setLastActiveAt(Instant.now());
    }

    /**
     * Create AppUserStats for a new user.
     * Initializes statistics with default values (total_like = 0, total_dislike = 0).
     *
     * @param appUser AppUser entity
     */
    private void createAppUserStats(AppUser appUser) {
        LOG.debug("Creating AppUserStats for user: {}", appUser.getId());

        AppUserStats stats = new AppUserStats();
        stats.setTotalLike(0);
        stats.setTotalDislike(0);
        stats.setAppUser(appUser);

        appUserStatsRepository.save(stats);
        appUser.setStats(stats);

        LOG.debug("Successfully created AppUserStats for user: {}", appUser.getId());
    }

    /**
     * Create AppUserImages from Spotify profile images.
     *
     * @param appUser AppUser entity
     * @param spotifyProfile Spotify profile JSON containing images array
     */
    private void createAppUserImages(AppUser appUser, JsonNode spotifyProfile) {
        LOG.debug("Creating AppUserImages for user: {}", appUser.getId());

        if (spotifyProfile.has("images") && spotifyProfile.get("images").isArray()) {
            JsonNode imagesNode = spotifyProfile.get("images");

            for (JsonNode imageNode : imagesNode) {
                if (imageNode.has("url")) {
                    AppUserImage image = new AppUserImage();
                    image.setUrl(imageNode.get("url").asText());

                    if (imageNode.has("height") && !imageNode.get("height").isNull()) {
                        image.setHeight(imageNode.get("height").asInt());
                    }

                    if (imageNode.has("width") && !imageNode.get("width").isNull()) {
                        image.setWidth(imageNode.get("width").asInt());
                    }

                    image.setAppUser(appUser);
                    appUserImageRepository.save(image);
                    LOG.debug("Created AppUserImage: {} for user: {}", image.getUrl(), appUser.getId());
                }
            }
        }

        LOG.debug("Successfully created AppUserImages for user: {}", appUser.getId());
    }

    /**
     * Update or create UserToken for storing Spotify tokens.
     * Tokens are encrypted at rest using AES-256-GCM encryption.
     *
     * @param appUser AppUser entity
     * @param spotifyAccessToken Spotify access token
     * @param spotifyRefreshToken Spotify refresh token (can be null)
     * @param expiresIn Token expiration time in seconds
     * @param scope OAuth scopes granted (can be null)
     */
    private void updateUserToken(AppUser appUser, String spotifyAccessToken, String spotifyRefreshToken, int expiresIn, String scope) {
        LOG.debug("Updating UserToken for user: {}", appUser.getId());

        UserToken userToken = appUser.getUserToken();

        if (userToken == null) {
            // Create new token
            userToken = new UserToken();
            userToken.setAppUser(appUser);
        }

        // Encrypt and update token values
        try {
            userToken.setAccessToken(tokenEncryptionService.encrypt(spotifyAccessToken));
            if (spotifyRefreshToken != null) {
                userToken.setRefreshToken(tokenEncryptionService.encrypt(spotifyRefreshToken));
            }
            userToken.setTokenType("Bearer");
            userToken.setExpiresAt(Instant.now().plusSeconds(expiresIn));
            userToken.setScope(scope != null ? scope : "user-read-email user-read-private");

            // Save token
            userTokenRepository.save(userToken);
            LOG.debug("Successfully updated UserToken for user: {}", appUser.getId());
        } catch (Exception e) {
            LOG.error("Failed to encrypt and save UserToken for user {}: {}", appUser.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to save user tokens", e);
        }
    }

    /**
     * Update user's last active timestamp.
     *
     * @param userId User ID
     */
    public void updateLastActive(String userId) {
        try {
            Optional<AppUser> userOpt = appUserRepository.findById(java.util.UUID.fromString(userId));
            if (userOpt.isPresent()) {
                AppUser user = userOpt.get();
                user.setLastActiveAt(Instant.now());
                appUserRepository.save(user);
            }
        } catch (Exception e) {
            LOG.warn("Failed to update last active for user {}: {}", userId, e.getMessage());
        }
    }
}
