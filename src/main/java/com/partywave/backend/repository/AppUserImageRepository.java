package com.partywave.backend.repository;

import com.partywave.backend.domain.AppUser;
import com.partywave.backend.domain.AppUserImage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the AppUserImage entity.
 */
@Repository
public interface AppUserImageRepository extends JpaRepository<AppUserImage, UUID> {
    default Optional<AppUserImage> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<AppUserImage> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<AppUserImage> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select appUserImage from AppUserImage appUserImage left join fetch appUserImage.appUser",
        countQuery = "select count(appUserImage) from AppUserImage appUserImage"
    )
    Page<AppUserImage> findAllWithToOneRelationships(Pageable pageable);

    @Query("select appUserImage from AppUserImage appUserImage left join fetch appUserImage.appUser")
    List<AppUserImage> findAllWithToOneRelationships();

    @Query("select appUserImage from AppUserImage appUserImage left join fetch appUserImage.appUser where appUserImage.id =:id")
    Optional<AppUserImage> findOneWithToOneRelationships(@Param("id") UUID id);

    /**
     * Find all AppUserImages for a specific AppUser.
     *
     * @param appUser AppUser entity
     * @return List of AppUserImage entities
     */
    List<AppUserImage> findByAppUser(AppUser appUser);
}
