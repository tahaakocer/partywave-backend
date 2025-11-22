package com.partywave.backend.repository;

import com.partywave.backend.domain.RoomInvitation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the RoomInvitation entity.
 */
@Repository
public interface RoomInvitationRepository extends JpaRepository<RoomInvitation, Long> {
    default Optional<RoomInvitation> findOneWithEagerRelationships(Long id) {
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
    Optional<RoomInvitation> findOneWithToOneRelationships(@Param("id") Long id);
}
