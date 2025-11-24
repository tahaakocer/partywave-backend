package com.partywave.backend.service;

import com.partywave.backend.domain.AppUser;
import com.partywave.backend.exception.InvalidTokenException;
import com.partywave.backend.exception.ResourceNotFoundException;
import com.partywave.backend.exception.TokenGenerationException;
import com.partywave.backend.repository.AppUserRepository;
import com.partywave.backend.security.jwt.JwtTokenProvider;
import com.partywave.backend.service.dto.JwtRefreshRequestDTO;
import com.partywave.backend.service.dto.JwtTokenResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for JWT authentication operations.
 * Handles token generation, refresh, and revocation.
 */
@Service
@Transactional
public class JwtAuthenticationService {

    private static final Logger LOG = LoggerFactory.getLogger(JwtAuthenticationService.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final AppUserRepository appUserRepository;

    public JwtAuthenticationService(JwtTokenProvider jwtTokenProvider, AppUserRepository appUserRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.appUserRepository = appUserRepository;
    }

    /**
     * Generate JWT tokens (access and refresh) for a user.
     *
     * @param appUser The application user
     * @param request HTTP request (for IP and user agent)
     * @return JWT token response DTO
     * @throws TokenGenerationException if token generation fails
     */
    public JwtTokenResponseDTO generateTokens(AppUser appUser, HttpServletRequest request) {
        LOG.debug("Generating JWT tokens for user: {}", appUser.getId());

        try {
            // Extract request information
            String ipAddress = extractIpAddress(request);
            String deviceInfo = extractDeviceInfo(request);

            // Generate tokens
            String accessToken = jwtTokenProvider.generateAccessToken(appUser);
            String refreshToken = jwtTokenProvider.generateRefreshToken(appUser, ipAddress, deviceInfo);

            // Create user info DTO with full details
            // Handle images safely - they might be lazy loaded
            java.util.List<JwtTokenResponseDTO.ImageDTO> imageDTOs = new java.util.ArrayList<>();
            try {
                if (appUser.getImages() != null) {
                    imageDTOs = appUser
                        .getImages()
                        .stream()
                        .map(img ->
                            new JwtTokenResponseDTO.ImageDTO(
                                img.getUrl(),
                                img.getHeight() != null ? img.getHeight().toString() : null,
                                img.getWidth() != null ? img.getWidth().toString() : null
                            )
                        )
                        .collect(java.util.stream.Collectors.toList());
                }
            } catch (org.hibernate.LazyInitializationException e) {
                LOG.warn("Images are lazy loaded and session is closed. Skipping images in token response.");
                // Images will be empty list - user can fetch them via /users/me endpoint
            } catch (Exception e) {
                LOG.warn("Failed to load user images: {}", e.getMessage());
                // Images will be empty list
            }

            JwtTokenResponseDTO.UserInfoDTO userInfo = new JwtTokenResponseDTO.UserInfoDTO(
                appUser.getId().toString(),
                appUser.getSpotifyUserId(),
                appUser.getEmail(),
                appUser.getDisplayName(),
                appUser.getCountry(),
                appUser.getStatus() != null ? appUser.getStatus().toString() : null,
                imageDTOs
            );

            // Create response
            JwtTokenResponseDTO response = new JwtTokenResponseDTO(
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenValidity(),
                userInfo
            );

            LOG.debug("Successfully generated JWT tokens for user: {}", appUser.getId());
            return response;
        } catch (Exception e) {
            LOG.error("Failed to generate JWT tokens for user: {}", appUser.getId(), e);
            throw new TokenGenerationException("Failed to generate tokens", e);
        }
    }

    /**
     * Refresh JWT tokens using a valid refresh token.
     *
     * @param refreshRequest Refresh token request DTO
     * @param request HTTP request (for IP and user agent)
     * @return New JWT token response DTO
     * @throws InvalidTokenException if refresh token is invalid or expired
     * @throws ResourceNotFoundException if user is not found
     */
    public JwtTokenResponseDTO refreshTokens(JwtRefreshRequestDTO refreshRequest, HttpServletRequest request) {
        LOG.debug("Refreshing JWT tokens");

        try {
            // Validate refresh token
            Jwt jwt = jwtTokenProvider.validateRefreshToken(refreshRequest.getRefreshToken());

            // Get user from token
            UUID userId = jwtTokenProvider.getUserIdFromToken(jwt);
            Optional<AppUser> userOpt = appUserRepository.findById(userId);

            if (userOpt.isEmpty()) {
                LOG.error("User not found for refresh token: {}", userId);
                throw new ResourceNotFoundException("User", "id", userId);
            }

            AppUser appUser = userOpt.get();

            // Revoke old refresh token (token rotation)
            try {
                jwtTokenProvider.revokeRefreshToken(refreshRequest.getRefreshToken());
            } catch (Exception e) {
                LOG.warn("Failed to revoke old refresh token: {}", e.getMessage());
                // Continue anyway
            }

            // Generate new tokens
            JwtTokenResponseDTO response = generateTokens(appUser, request);

            LOG.debug("Successfully refreshed JWT tokens for user: {}", userId);
            return response;
        } catch (InvalidTokenException | ResourceNotFoundException | TokenGenerationException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Failed to refresh JWT tokens: {}", e.getMessage(), e);
            throw new InvalidTokenException("Failed to refresh tokens: " + e.getMessage(), "refresh_token", e);
        }
    }

    /**
     * Revoke a refresh token (logout).
     *
     * @param refreshToken Refresh token to revoke
     * @throws InvalidTokenException if token cannot be revoked
     */
    public void revokeToken(String refreshToken) {
        LOG.debug("Revoking refresh token");

        try {
            jwtTokenProvider.revokeRefreshToken(refreshToken);
            LOG.debug("Successfully revoked refresh token");
        } catch (Exception e) {
            LOG.error("Failed to revoke refresh token: {}", e.getMessage(), e);
            throw new InvalidTokenException("Failed to revoke token: " + e.getMessage(), "refresh_token", e);
        }
    }

    /**
     * Extract client IP address from HTTP request.
     *
     * @param request HTTP request
     * @return IP address
     */
    private String extractIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

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

    /**
     * Extract device/user agent information from HTTP request.
     *
     * @param request HTTP request
     * @return Device info
     */
    private String extractDeviceInfo(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String userAgent = request.getHeader("User-Agent");
        return userAgent != null ? userAgent : "unknown";
    }
}
