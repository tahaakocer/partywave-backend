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
}
