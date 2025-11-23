package com.partywave.backend.repository;

import com.partywave.backend.domain.AppUserStats;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the AppUserStats entity.
 */
@SuppressWarnings("unused")
@Repository
public interface AppUserStatsRepository extends JpaRepository<AppUserStats, UUID> {}
