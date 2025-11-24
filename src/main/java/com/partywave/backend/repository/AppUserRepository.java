package com.partywave.backend.repository;

import com.partywave.backend.domain.AppUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the AppUser entity.
 */
@SuppressWarnings("unused")
@Repository
public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    /**
     * Find AppUser by Spotify user ID.
     *
     * @param spotifyUserId Spotify user ID
     * @return Optional of AppUser
     */
    Optional<AppUser> findBySpotifyUserId(String spotifyUserId);

    /**
     * Find AppUser by ID with images eagerly fetched.
     * Used when images are needed immediately (e.g., token generation).
     *
     * @param id User ID
     * @return Optional of AppUser with images loaded
     */
    @EntityGraph(attributePaths = { "images" })
    @Query("SELECT u FROM AppUser u WHERE u.id = :id")
    Optional<AppUser> findByIdWithImages(UUID id);

    /**
     * Find AppUser by ID with stats and images eagerly fetched.
     * Used when full user profile is needed (e.g., /api/users/me endpoint).
     *
     * @param id User ID
     * @return Optional of AppUser with stats and images loaded
     */
    @EntityGraph(attributePaths = { "stats", "images" })
    @Query("SELECT u FROM AppUser u WHERE u.id = :id")
    Optional<AppUser> findByIdWithStatsAndImages(UUID id);
}
