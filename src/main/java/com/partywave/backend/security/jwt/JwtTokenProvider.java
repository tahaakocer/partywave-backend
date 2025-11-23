package com.partywave.backend.security.jwt;

import static com.partywave.backend.security.SecurityUtils.JWT_ALGORITHM;

import com.partywave.backend.domain.AppUser;
import com.partywave.backend.domain.RefreshToken;
import com.partywave.backend.domain.enumeration.AppUserStatus;
import com.partywave.backend.repository.AppUserRepository;
import com.partywave.backend.repository.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/**
 * JWT Token Provider for PartyWave application.
 * Handles generation and validation of JWT access tokens and refresh tokens.
 *
 * Token Structure:
 * - Access Token: 15 minutes expiry
 * - Refresh Token: 7 days expiry
 *
 * Claims:
 * - sub: app_user_id (UUID)
 * - spotify_user_id: Spotify user ID
 * - email: User email
 * - display_name: User display name
 * - iat: Issued at timestamp
 * - exp: Expiration timestamp
 * - jti: JWT ID (unique per token)
 */
@Component
public class JwtTokenProvider {

    private static final Logger LOG = LoggerFactory.getLogger(JwtTokenProvider.class);

    // Token expiration times (in seconds)
    private static final long ACCESS_TOKEN_VALIDITY = 900L; // 15 minutes
    private static final long REFRESH_TOKEN_VALIDITY = 604800L; // 7 days

    // Claim names
    private static final String CLAIM_SPOTIFY_USER_ID = "spotify_user_id";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_DISPLAY_NAME = "display_name";

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final AppUserRepository appUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public JwtTokenProvider(
        JwtEncoder jwtEncoder,
        JwtDecoder jwtDecoder,
        AppUserRepository appUserRepository,
        RefreshTokenRepository refreshTokenRepository
    ) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.appUserRepository = appUserRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Generate access token for a user.
     *
     * @param appUser The application user
     * @return JWT access token string
     */
    public String generateAccessToken(AppUser appUser) {
        LOG.debug("Generating access token for user: {}", appUser.getId());

        Instant now = Instant.now();
        Instant expiration = now.plus(ACCESS_TOKEN_VALIDITY, ChronoUnit.SECONDS);
        String jti = UUID.randomUUID().toString();

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("partywave")
            .issuedAt(now)
            .expiresAt(expiration)
            .subject(appUser.getId().toString())
            .claim("jti", jti)
            .claim(CLAIM_SPOTIFY_USER_ID, appUser.getSpotifyUserId())
            .claim(CLAIM_EMAIL, appUser.getEmail())
            .claim(CLAIM_DISPLAY_NAME, appUser.getDisplayName())
            .build();

        // Specify the algorithm in the header to help the encoder select the correct key
        JwsHeader header = JwsHeader.with(JWT_ALGORITHM).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /**
     * Generate refresh token for a user and store it in database.
     *
     * @param appUser The application user
     * @param ipAddress Client IP address (optional)
     * @param deviceInfo Client device info (optional)
     * @return JWT refresh token string
     */
    public String generateRefreshToken(AppUser appUser, String ipAddress, String deviceInfo) {
        LOG.debug("Generating refresh token for user: {}", appUser.getId());

        Instant now = Instant.now();
        Instant expiration = now.plus(REFRESH_TOKEN_VALIDITY, ChronoUnit.SECONDS);
        String jti = UUID.randomUUID().toString();

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("partywave")
            .issuedAt(now)
            .expiresAt(expiration)
            .subject(appUser.getId().toString())
            .claim("jti", jti)
            .claim(CLAIM_SPOTIFY_USER_ID, appUser.getSpotifyUserId())
            .claim(CLAIM_EMAIL, appUser.getEmail())
            .claim(CLAIM_DISPLAY_NAME, appUser.getDisplayName())
            .build();

        // Specify the algorithm in the header to help the encoder select the correct key
        JwsHeader header = JwsHeader.with(JWT_ALGORITHM).build();
        String tokenValue = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        // Store refresh token hash in database
        try {
            String tokenHash = hashToken(tokenValue);
            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setTokenHash(tokenHash);
            refreshToken.setAppUser(appUser);
            refreshToken.setExpiresAt(expiration);
            refreshToken.setCreatedAt(now);
            refreshToken.setIpAddress(ipAddress);
            refreshToken.setDeviceInfo(deviceInfo);

            refreshTokenRepository.save(refreshToken);
            LOG.debug("Refresh token stored in database for user: {}", appUser.getId());
        } catch (Exception e) {
            LOG.error("Failed to store refresh token in database: {}", e.getMessage(), e);
            // Continue anyway - token is still valid
        }

        return tokenValue;
    }

    /**
     * Validate JWT token and extract claims.
     *
     * @param token JWT token string
     * @return Jwt object if valid
     * @throws Exception if token is invalid or expired
     */
    public Jwt validateToken(String token) throws Exception {
        LOG.debug("Validating JWT token");

        try {
            Jwt jwt = jwtDecoder.decode(token);

            // Additional validation: check user status
            String userId = jwt.getSubject();
            if (userId == null || userId.isEmpty()) {
                LOG.warn("Token validation failed: Missing subject claim");
                throw new Exception("Invalid token: missing subject");
            }
            Optional<AppUser> userOpt = appUserRepository.findById(UUID.fromString(userId));

            if (userOpt.isEmpty()) {
                LOG.warn("Token validation failed: User not found (id: {})", userId);
                throw new Exception("User not found");
            }

            AppUser user = userOpt.get();
            if (user.getStatus() == AppUserStatus.BANNED) {
                LOG.warn("Token validation failed: User is banned (id: {})", userId);
                throw new Exception("User is banned");
            }

            LOG.debug("Token validated successfully for user: {}", userId);
            return jwt;
        } catch (Exception e) {
            LOG.debug("Token validation failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Validate refresh token and check if it's not revoked.
     *
     * @param token Refresh token string
     * @return Jwt object if valid
     * @throws Exception if token is invalid, expired, or revoked
     */
    public Jwt validateRefreshToken(String token) throws Exception {
        LOG.debug("Validating refresh token");

        // First validate JWT structure and signature
        Jwt jwt = validateToken(token);

        // Check if refresh token is revoked in database
        try {
            String tokenHash = hashToken(token);
            Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository
                .findAllWithToOneRelationships()
                .stream()
                .filter(rt -> rt.getTokenHash().equals(tokenHash))
                .findFirst();

            if (refreshTokenOpt.isEmpty()) {
                LOG.warn("Refresh token not found in database");
                throw new Exception("Refresh token not found");
            }

            RefreshToken refreshToken = refreshTokenOpt.get();

            if (refreshToken.getRevokedAt() != null) {
                LOG.warn("Refresh token has been revoked");
                throw new Exception("Refresh token revoked");
            }

            if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
                LOG.warn("Refresh token has expired");
                throw new Exception("Refresh token expired");
            }

            LOG.debug("Refresh token validated successfully");
            return jwt;
        } catch (Exception e) {
            LOG.debug("Refresh token validation failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Revoke refresh token (marks it as revoked in database).
     *
     * @param token Refresh token string
     * @throws Exception if token cannot be revoked
     */
    public void revokeRefreshToken(String token) throws Exception {
        LOG.debug("Revoking refresh token");

        try {
            String tokenHash = hashToken(token);
            Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository
                .findAllWithToOneRelationships()
                .stream()
                .filter(rt -> rt.getTokenHash().equals(tokenHash))
                .findFirst();

            if (refreshTokenOpt.isPresent()) {
                RefreshToken refreshToken = refreshTokenOpt.get();
                refreshToken.setRevokedAt(Instant.now());
                refreshTokenRepository.save(refreshToken);
                LOG.debug("Refresh token revoked successfully");
            } else {
                LOG.warn("Refresh token not found for revocation");
            }
        } catch (Exception e) {
            LOG.error("Failed to revoke refresh token: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Extract user ID from JWT token.
     *
     * @param jwt JWT object
     * @return User ID (UUID)
     */
    public UUID getUserIdFromToken(Jwt jwt) {
        String subject = jwt.getSubject();
        return UUID.fromString(subject);
    }

    /**
     * Extract Spotify user ID from JWT token.
     *
     * @param jwt JWT object
     * @return Spotify user ID
     */
    public String getSpotifyUserIdFromToken(Jwt jwt) {
        return jwt.getClaimAsString(CLAIM_SPOTIFY_USER_ID);
    }

    /**
     * Extract email from JWT token.
     *
     * @param jwt JWT object
     * @return Email
     */
    public String getEmailFromToken(Jwt jwt) {
        return jwt.getClaimAsString(CLAIM_EMAIL);
    }

    /**
     * Extract display name from JWT token.
     *
     * @param jwt JWT object
     * @return Display name
     */
    public String getDisplayNameFromToken(Jwt jwt) {
        return jwt.getClaimAsString(CLAIM_DISPLAY_NAME);
    }

    /**
     * Get access token validity in seconds.
     *
     * @return Access token validity
     */
    public long getAccessTokenValidity() {
        return ACCESS_TOKEN_VALIDITY;
    }

    /**
     * Get refresh token validity in seconds.
     *
     * @return Refresh token validity
     */
    public long getRefreshTokenValidity() {
        return REFRESH_TOKEN_VALIDITY;
    }

    /**
     * Hash token using SHA-256 for secure storage.
     *
     * @param token Token string
     * @return Hashed token (Base64 encoded)
     * @throws NoSuchAlgorithmException if SHA-256 is not available
     */
    private String hashToken(String token) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}
