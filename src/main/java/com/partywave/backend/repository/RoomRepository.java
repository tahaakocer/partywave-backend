package com.partywave.backend.repository;

import com.partywave.backend.domain.Room;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Room entity.
 *
 * When extending this class, extend RoomRepositoryWithBagRelationships too.
 * For more information refer to https://github.com/jhipster/generator-jhipster/issues/17990.
 */
@Repository
public interface RoomRepository extends RoomRepositoryWithBagRelationships, JpaRepository<Room, UUID>, JpaSpecificationExecutor<Room> {
    default Optional<Room> findOneWithEagerRelationships(UUID id) {
        return this.fetchBagRelationships(this.findById(id));
    }

    default List<Room> findAllWithEagerRelationships() {
        return this.fetchBagRelationships(this.findAll());
    }

    default Page<Room> findAllWithEagerRelationships(Pageable pageable) {
        return this.fetchBagRelationships(this.findAll(pageable));
    }

    /**
     * Batch query to get member counts for multiple rooms in a single query.
     *
     * Replaces N individual COUNT queries with a single GROUP BY query.
     * Returns array of [roomId, memberCount] for each room.
     *
     * Used in room discovery to avoid N+1 query problem when loading member counts.
     *
     * @param roomIds List of room UUIDs to get member counts for
     * @return List of Object arrays where arr[0] = UUID roomId, arr[1] = Long memberCount
     */
    @Query("SELECT rm.room.id, COUNT(rm) FROM RoomMember rm WHERE rm.room.id IN :roomIds GROUP BY rm.room.id")
    List<Object[]> countMembersByRoomIds(@Param("roomIds") List<UUID> roomIds);
}
