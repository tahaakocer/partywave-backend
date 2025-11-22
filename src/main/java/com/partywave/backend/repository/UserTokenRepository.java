package com.partywave.backend.repository;

import com.partywave.backend.domain.UserToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the UserToken entity.
 */
@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, Long> {
    default Optional<UserToken> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<UserToken> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<UserToken> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select userToken from UserToken userToken left join fetch userToken.appUser",
        countQuery = "select count(userToken) from UserToken userToken"
    )
    Page<UserToken> findAllWithToOneRelationships(Pageable pageable);

    @Query("select userToken from UserToken userToken left join fetch userToken.appUser")
    List<UserToken> findAllWithToOneRelationships();

    @Query("select userToken from UserToken userToken left join fetch userToken.appUser where userToken.id =:id")
    Optional<UserToken> findOneWithToOneRelationships(@Param("id") Long id);
}
