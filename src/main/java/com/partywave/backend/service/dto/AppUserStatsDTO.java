package com.partywave.backend.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link com.partywave.backend.domain.AppUserStats} entity.
 */
@Schema(description = "Aggregated statistics for a user.")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AppUserStatsDTO implements Serializable {

    private UUID id;

    private Integer totalLike;

    private Integer totalDislike;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Integer getTotalLike() {
        return totalLike;
    }

    public void setTotalLike(Integer totalLike) {
        this.totalLike = totalLike;
    }

    public Integer getTotalDislike() {
        return totalDislike;
    }

    public void setTotalDislike(Integer totalDislike) {
        this.totalDislike = totalDislike;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AppUserStatsDTO)) {
            return false;
        }

        AppUserStatsDTO appUserStatsDTO = (AppUserStatsDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, appUserStatsDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AppUserStatsDTO{" +
            "id='" + getId() + "'" +
            ", totalLike=" + getTotalLike() +
            ", totalDislike=" + getTotalDislike() +
            "}";
    }
}
