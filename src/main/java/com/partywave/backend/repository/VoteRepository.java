package com.partywave.backend.repository;

import com.partywave.backend.domain.Vote;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Vote entity.
 */
@Repository
public interface VoteRepository extends JpaRepository<Vote, UUID> {
    default Optional<Vote> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Vote> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Vote> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select vote from Vote vote left join fetch vote.room left join fetch vote.voter left join fetch vote.targetUser",
        countQuery = "select count(vote) from Vote vote"
    )
    Page<Vote> findAllWithToOneRelationships(Pageable pageable);

    @Query("select vote from Vote vote left join fetch vote.room left join fetch vote.voter left join fetch vote.targetUser")
    List<Vote> findAllWithToOneRelationships();

    @Query(
        "select vote from Vote vote left join fetch vote.room left join fetch vote.voter left join fetch vote.targetUser where vote.id =:id"
    )
    Optional<Vote> findOneWithToOneRelationships(@Param("id") UUID id);

    /**
     * Check if a user has already voted to skip a specific track.
     * Based on PROJECT_OVERVIEW.md section 2.8 - Skipping Tracks (Vote-Based).
     *
     * @param roomId Room UUID
     * @param voterId Voter user UUID
     * @param playlistItemId Playlist item ID (string UUID from Redis)
     * @return true if vote exists
     */
    @Query(
        "select count(v) > 0 from Vote v where v.room.id = :roomId and v.voter.id = :voterId and v.voteType = 'SKIPTRACK' and v.playlistItemId = :playlistItemId"
    )
    boolean existsByRoomIdAndVoterIdAndPlaylistItemId(
        @Param("roomId") UUID roomId,
        @Param("voterId") UUID voterId,
        @Param("playlistItemId") String playlistItemId
    );

    /**
     * Check if a user has already voted to kick another user.
     * Based on PROJECT_OVERVIEW.md section 2.9 - Kicking Users (Vote-Based).
     *
     * @param roomId Room UUID
     * @param voterId Voter user UUID
     * @param targetUserId Target user UUID to kick
     * @return true if vote exists
     */
    @Query(
        "select count(v) > 0 from Vote v where v.room.id = :roomId and v.voter.id = :voterId and v.voteType = 'KICKUSER' and v.targetUser.id = :targetUserId"
    )
    boolean existsByRoomIdAndVoterIdAndTargetUserId(
        @Param("roomId") UUID roomId,
        @Param("voterId") UUID voterId,
        @Param("targetUserId") UUID targetUserId
    );

    /**
     * Count votes to skip a specific track.
     *
     * @param roomId Room UUID
     * @param playlistItemId Playlist item ID (string UUID from Redis)
     * @return Vote count
     */
    @Query("select count(v) from Vote v where v.room.id = :roomId and v.voteType = 'SKIPTRACK' and v.playlistItemId = :playlistItemId")
    long countByRoomIdAndPlaylistItemId(@Param("roomId") UUID roomId, @Param("playlistItemId") String playlistItemId);

    /**
     * Count votes to kick a specific user.
     *
     * @param roomId Room UUID
     * @param targetUserId Target user UUID to kick
     * @return Vote count
     */
    @Query("select count(v) from Vote v where v.room.id = :roomId and v.voteType = 'KICKUSER' and v.targetUser.id = :targetUserId")
    long countByRoomIdAndTargetUserId(@Param("roomId") UUID roomId, @Param("targetUserId") UUID targetUserId);

    /**
     * Find all KICKUSER votes for a room with eager loading of target user.
     *
     * @param roomId Room UUID
     * @return List of KICKUSER votes with target user loaded
     */
    @Query(
        "select v from Vote v left join fetch v.targetUser where v.room.id = :roomId and v.voteType = 'KICKUSER' order by v.targetUser.id"
    )
    List<Vote> findAllKickUserVotesByRoomId(@Param("roomId") UUID roomId);

    /**
     * Delete all skip track votes for a specific playlist item.
     * Used when track is skipped or finished to clean up old votes.
     *
     * @param roomId Room UUID
     * @param playlistItemId Playlist item ID
     */
    @Modifying
    @Query("delete from Vote v where v.room.id = :roomId and v.voteType = 'SKIPTRACK' and v.playlistItemId = :playlistItemId")
    void deleteSkipTrackVotesByPlaylistItemId(@Param("roomId") UUID roomId, @Param("playlistItemId") String playlistItemId);

    /**
     * Delete all kick user votes for a specific target user.
     * Used when user is kicked or leaves the room.
     *
     * @param roomId Room UUID
     * @param targetUserId Target user UUID
     */
    @Modifying
    @Query("delete from Vote v where v.room.id = :roomId and v.voteType = 'KICKUSER' and v.targetUser.id = :targetUserId")
    void deleteKickUserVotesByTargetUserId(@Param("roomId") UUID roomId, @Param("targetUserId") UUID targetUserId);

    /**
     * Delete a specific user's skip track vote for a playlist item.
     * Used when user withdraws their skip vote.
     *
     * @param roomId Room UUID
     * @param voterId Voter user UUID
     * @param playlistItemId Playlist item ID
     * @return Number of votes deleted (0 or 1)
     */
    @Modifying
    @Query(
        "delete from Vote v where v.room.id = :roomId and v.voter.id = :voterId and v.voteType = 'SKIPTRACK' and v.playlistItemId = :playlistItemId"
    )
    int deleteSkipTrackVoteByVoter(
        @Param("roomId") UUID roomId,
        @Param("voterId") UUID voterId,
        @Param("playlistItemId") String playlistItemId
    );

    /**
     * Delete a specific user's kick vote for a target user.
     * Used when user withdraws their kick vote.
     *
     * @param roomId Room UUID
     * @param voterId Voter user UUID
     * @param targetUserId Target user UUID
     * @return Number of votes deleted (0 or 1)
     */
    @Modifying
    @Query(
        "delete from Vote v where v.room.id = :roomId and v.voter.id = :voterId and v.voteType = 'KICKUSER' and v.targetUser.id = :targetUserId"
    )
    int deleteKickUserVoteByVoter(@Param("roomId") UUID roomId, @Param("voterId") UUID voterId, @Param("targetUserId") UUID targetUserId);
}
