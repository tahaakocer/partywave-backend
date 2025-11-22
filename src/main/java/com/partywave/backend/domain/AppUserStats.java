package com.partywave.backend.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.io.Serializable;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Aggregated statistics for a user.
 */
@Entity
@Table(name = "app_user_stats")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AppUserStats implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @Column(name = "total_like")
    private Integer totalLike;

    @Column(name = "total_dislike")
    private Integer totalDislike;

    @JsonIgnoreProperties(
        value = {
            "appUserStats",
            "images",
            "refreshTokens",
            "memberships",
            "receivedAccesses",
            "grantedAccesses",
            "createdInvitations",
            "messages",
            "castVotes",
            "receivedVotes",
            "userToken",
        },
        allowSetters = true
    )
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "appUserStats")
    private AppUser appUser;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public AppUserStats id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getTotalLike() {
        return this.totalLike;
    }

    public AppUserStats totalLike(Integer totalLike) {
        this.setTotalLike(totalLike);
        return this;
    }

    public void setTotalLike(Integer totalLike) {
        this.totalLike = totalLike;
    }

    public Integer getTotalDislike() {
        return this.totalDislike;
    }

    public AppUserStats totalDislike(Integer totalDislike) {
        this.setTotalDislike(totalDislike);
        return this;
    }

    public void setTotalDislike(Integer totalDislike) {
        this.totalDislike = totalDislike;
    }

    public AppUser getAppUser() {
        return this.appUser;
    }

    public void setAppUser(AppUser appUser) {
        if (this.appUser != null) {
            this.appUser.setAppUserStats(null);
        }
        if (appUser != null) {
            appUser.setAppUserStats(this);
        }
        this.appUser = appUser;
    }

    public AppUserStats appUser(AppUser appUser) {
        this.setAppUser(appUser);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AppUserStats)) {
            return false;
        }
        return getId() != null && getId().equals(((AppUserStats) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AppUserStats{" +
            "id=" + getId() +
            ", totalLike=" + getTotalLike() +
            ", totalDislike=" + getTotalDislike() +
            "}";
    }
}
