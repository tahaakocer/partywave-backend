package com.partywave.backend.repository;

import com.partywave.backend.domain.RoomMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the RoomMember entity.
 */
@Repository
public interface RoomMemberRepository extends JpaRepository<RoomMember, UUID> {
    default Optional<RoomMember> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<RoomMember> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<RoomMember> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select roomMember from RoomMember roomMember left join fetch roomMember.room left join fetch roomMember.appUser",
        countQuery = "select count(roomMember) from RoomMember roomMember"
    )
    Page<RoomMember> findAllWithToOneRelationships(Pageable pageable);

    @Query("select roomMember from RoomMember roomMember left join fetch roomMember.room left join fetch roomMember.appUser")
    List<RoomMember> findAllWithToOneRelationships();

    @Query(
        "select roomMember from RoomMember roomMember left join fetch roomMember.room left join fetch roomMember.appUser where roomMember.id =:id"
    )
    Optional<RoomMember> findOneWithToOneRelationships(@Param("id") UUID id);
}
