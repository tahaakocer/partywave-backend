package com.partywave.backend.repository;

import com.partywave.backend.domain.Room;
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

    /**
     * Count the number of members in a room.
     *
     * @param room Room entity
     * @return Number of members
     */
    long countByRoom(Room room);

    /**
     * Check if a user is already a member of a room.
     *
     * @param roomId Room UUID
     * @param userId User UUID
     * @return true if user is a member
     */
    @Query("select count(rm) > 0 from RoomMember rm where rm.room.id = :roomId and rm.appUser.id = :userId")
    boolean existsByRoomIdAndUserId(@Param("roomId") UUID roomId, @Param("userId") UUID userId);

    /**
     * Check if a user is an active member of a room.
     *
     * @param roomId Room UUID
     * @param userId User UUID
     * @return true if user is an active member
     */
    @Query("select count(rm) > 0 from RoomMember rm where rm.room.id = :roomId and rm.appUser.id = :userId and rm.isActive = true")
    boolean existsByRoomIdAndUserIdAndIsActiveTrue(@Param("roomId") UUID roomId, @Param("userId") UUID userId);

    /**
     * Find active room member by room ID and user ID.
     *
     * @param roomId Room UUID
     * @param userId User UUID
     * @return Optional of RoomMember
     */
    @Query("select rm from RoomMember rm where rm.room.id = :roomId and rm.appUser.id = :userId and rm.isActive = true")
    Optional<RoomMember> findByRoomIdAndUserIdAndIsActiveTrue(@Param("roomId") UUID roomId, @Param("userId") UUID userId);

    /**
     * Find room member by room ID and user ID (including inactive).
     *
     * @param roomId Room UUID
     * @param userId User UUID
     * @return Optional of RoomMember
     */
    @Query("select rm from RoomMember rm where rm.room.id = :roomId and rm.appUser.id = :userId")
    Optional<RoomMember> findByRoomIdAndUserId(@Param("roomId") UUID roomId, @Param("userId") UUID userId);

    /**
     * Count active members in a room.
     *
     * @param room Room entity
     * @return Number of active members
     */
    long countByRoomAndIsActiveTrue(Room room);
}
