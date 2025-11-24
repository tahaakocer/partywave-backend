package com.partywave.backend.web.rest;

import com.partywave.backend.domain.AppUser;
import com.partywave.backend.exception.ResourceNotFoundException;
import com.partywave.backend.security.SecurityUtils;
import com.partywave.backend.service.AppUserService;
import com.partywave.backend.service.dto.AppUserDTO;
import com.partywave.backend.service.dto.UpdateUserProfileRequestDTO;
import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing user profiles.
 * Provides endpoints for retrieving and updating user information.
 */

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger LOG = LoggerFactory.getLogger(UserController.class);

    private final AppUserService appUserService;

    public UserController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    /**
     * GET /api/users/{userId} : Get user profile by ID
     *
     * Returns detailed information about a user, including profile data, statistics, and images.
     * Based on PROJECT_OVERVIEW.md section 2.13 - User Profile Viewing & Statistics.
     *
     * @param userId User ID
     * @return ResponseEntity with user profile
     */
    @GetMapping("/{userId}")
    public ResponseEntity<AppUserDTO> getUserProfile(@PathVariable UUID userId) {
        LOG.debug("REST request to get user profile: {}", userId);

        // Get user profile using service
        AppUser appUser = appUserService.getUserProfile(userId);

        // Convert to DTO
        AppUserDTO userDTO = convertToDTO(appUser);

        LOG.debug("Successfully retrieved user profile: {}", userId);
        return ResponseEntity.ok(userDTO);
    }

    /**
     * GET /api/users/me : Get current authenticated user's profile
     *
     * Returns detailed information about the currently authenticated user,
     * including profile data, statistics, and images.
     *
     * @return ResponseEntity with current user's profile
     */
    @GetMapping("/me")
    public ResponseEntity<AppUserDTO> getCurrentUser() {
        LOG.debug("REST request to get current user profile");

        // Extract user ID from JWT token
        UUID userId = SecurityUtils.getCurrentUserId()
            .orElseThrow(() -> new ResourceNotFoundException("User", "authentication", "No authenticated user found"));

        // Get user profile using service
        AppUser appUser = appUserService.getUserProfile(userId);

        // Convert to DTO
        AppUserDTO userDTO = convertToDTO(appUser);

        LOG.debug("Successfully retrieved current user profile: {}", userId);
        return ResponseEntity.ok(userDTO);
    }

    /**
     * PUT /api/users/me : Update current authenticated user's profile
     *
     * Updates the display_name and optionally syncs images from Spotify.
     *
     * @param request Update request DTO
     * @return ResponseEntity with updated user profile
     */
    @PutMapping("/me")
    public ResponseEntity<AppUserDTO> updateCurrentUser(@Valid @RequestBody UpdateUserProfileRequestDTO request) {
        LOG.debug("REST request to update current user profile");

        // Extract user ID from JWT token
        UUID userId = SecurityUtils.getCurrentUserId()
            .orElseThrow(() -> new ResourceNotFoundException("User", "authentication", "No authenticated user found"));

        // Update profile using service
        AppUser appUser = appUserService.updateProfile(userId, request);

        // Reload with stats and images to ensure we have the latest data
        appUser = appUserService.getUserProfile(userId);

        // Convert to DTO
        AppUserDTO userDTO = convertToDTO(appUser);

        LOG.debug("Successfully updated current user profile: {}", userId);
        return ResponseEntity.ok(userDTO);
    }

    /**
     * Converts AppUser entity to AppUserDTO.
     *
     * @param appUser The AppUser entity
     * @return AppUserDTO with user information
     */
    private AppUserDTO convertToDTO(AppUser appUser) {
        AppUserDTO dto = new AppUserDTO();
        dto.setId(appUser.getId().toString());
        dto.setSpotifyUserId(appUser.getSpotifyUserId());
        dto.setDisplayName(appUser.getDisplayName());
        dto.setEmail(appUser.getEmail());
        dto.setCountry(appUser.getCountry());
        dto.setHref(appUser.getHref());
        dto.setUrl(appUser.getUrl());
        dto.setType(appUser.getType());
        dto.setIpAddress(appUser.getIpAddress());
        dto.setLastActiveAt(appUser.getLastActiveAt());
        dto.setStatus(appUser.getStatus() != null ? appUser.getStatus().toString() : null);

        // Add stats if available
        if (appUser.getStats() != null) {
            AppUserDTO.StatsDTO stats = new AppUserDTO.StatsDTO();
            stats.setTotalLike(appUser.getStats().getTotalLike());
            stats.setTotalDislike(appUser.getStats().getTotalDislike());
            dto.setStats(stats);
        }

        // Add images
        if (appUser.getImages() != null && !appUser.getImages().isEmpty()) {
            java.util.List<AppUserDTO.ImageDTO> images = appUser
                .getImages()
                .stream()
                .map(img -> {
                    AppUserDTO.ImageDTO imageDTO = new AppUserDTO.ImageDTO();
                    imageDTO.setUrl(img.getUrl());
                    imageDTO.setHeight(img.getHeight());
                    imageDTO.setWidth(img.getWidth());
                    return imageDTO;
                })
                .collect(java.util.stream.Collectors.toList());
            dto.setImages(images);
        }

        return dto;
    }
}
