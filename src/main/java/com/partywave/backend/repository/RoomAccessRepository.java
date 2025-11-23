package com.partywave.backend.repository;

import com.partywave.backend.domain.RoomAccess;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the RoomAccess entity.
 */
@Repository
public interface RoomAccessRepository extends JpaRepository<RoomAccess, UUID> {
    default Optional<RoomAccess> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<RoomAccess> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<RoomAccess> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select roomAccess from RoomAccess roomAccess left join fetch roomAccess.room left join fetch roomAccess.appUser left join fetch roomAccess.grantedBy",
        countQuery = "select count(roomAccess) from RoomAccess roomAccess"
    )
    Page<RoomAccess> findAllWithToOneRelationships(Pageable pageable);

    @Query(
        "select roomAccess from RoomAccess roomAccess left join fetch roomAccess.room left join fetch roomAccess.appUser left join fetch roomAccess.grantedBy"
    )
    List<RoomAccess> findAllWithToOneRelationships();

    @Query(
        "select roomAccess from RoomAccess roomAccess left join fetch roomAccess.room left join fetch roomAccess.appUser left join fetch roomAccess.grantedBy where roomAccess.id =:id"
    )
    Optional<RoomAccess> findOneWithToOneRelationships(@Param("id") UUID id);

    /**
     * Check if a user has explicit access to a room.
     *
     * @param roomId Room UUID
     * @param userId User UUID
     * @return true if user has explicit access
     */
    @Query("select count(ra) > 0 from RoomAccess ra where ra.room.id = :roomId and ra.appUser.id = :userId")
    boolean existsByRoomIdAndAppUserId(@Param("roomId") UUID roomId, @Param("userId") UUID userId);
}
