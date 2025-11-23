package com.partywave.backend.repository;

import com.partywave.backend.domain.RoomInvitation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the RoomInvitation entity.
 */
@Repository
public interface RoomInvitationRepository extends JpaRepository<RoomInvitation, UUID> {
    default Optional<RoomInvitation> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<RoomInvitation> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<RoomInvitation> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select roomInvitation from RoomInvitation roomInvitation left join fetch roomInvitation.room left join fetch roomInvitation.createdBy",
        countQuery = "select count(roomInvitation) from RoomInvitation roomInvitation"
    )
    Page<RoomInvitation> findAllWithToOneRelationships(Pageable pageable);

    @Query(
        "select roomInvitation from RoomInvitation roomInvitation left join fetch roomInvitation.room left join fetch roomInvitation.createdBy"
    )
    List<RoomInvitation> findAllWithToOneRelationships();

    @Query(
        "select roomInvitation from RoomInvitation roomInvitation left join fetch roomInvitation.room left join fetch roomInvitation.createdBy where roomInvitation.id =:id"
    )
    Optional<RoomInvitation> findOneWithToOneRelationships(@Param("id") UUID id);

    /**
     * Find an active invitation by token.
     *
     * @param token Invitation token
     * @return Optional of RoomInvitation
     */
    @Query("select ri from RoomInvitation ri left join fetch ri.room where ri.token = :token and ri.isActive = true")
    Optional<RoomInvitation> findByTokenAndIsActiveTrue(@Param("token") String token);

    /**
     * Increment the used count for an invitation token.
     * This is an atomic operation to prevent race conditions.
     *
     * @param invitationId Invitation UUID
     * @return number of rows updated
     */
    @Modifying
    @Query("update RoomInvitation ri set ri.usedCount = ri.usedCount + 1 where ri.id = :invitationId")
    int incrementUsedCount(@Param("invitationId") UUID invitationId);
}
