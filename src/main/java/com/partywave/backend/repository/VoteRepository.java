package com.partywave.backend.repository;

import com.partywave.backend.domain.Vote;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Vote entity.
 */
@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {
    default Optional<Vote> findOneWithEagerRelationships(Long id) {
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
    Optional<Vote> findOneWithToOneRelationships(@Param("id") Long id);
}
