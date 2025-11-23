package com.partywave.backend.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.partywave.backend.domain.enumeration.VoteType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Votes to skip tracks or kick users.
 */
@Entity
@Table(name = "vote")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Vote implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "vote_type", nullable = false)
    private VoteType voteType;

    @Column(name = "playlist_item_id")
    private String playlistItemId;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "members", "accesses", "invitations", "messages", "votes", "tags" }, allowSetters = true)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(
        value = {
            "stats",
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
    private AppUser voter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(
        value = {
            "stats",
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
    private AppUser targetUser;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Vote id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public VoteType getVoteType() {
        return this.voteType;
    }

    public Vote voteType(VoteType voteType) {
        this.setVoteType(voteType);
        return this;
    }

    public void setVoteType(VoteType voteType) {
        this.voteType = voteType;
    }

    public String getPlaylistItemId() {
        return this.playlistItemId;
    }

    public Vote playlistItemId(String playlistItemId) {
        this.setPlaylistItemId(playlistItemId);
        return this;
    }

    public void setPlaylistItemId(String playlistItemId) {
        this.playlistItemId = playlistItemId;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Vote createdAt(Instant createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Room getRoom() {
        return this.room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public Vote room(Room room) {
        this.setRoom(room);
        return this;
    }

    public AppUser getVoter() {
        return this.voter;
    }

    public void setVoter(AppUser appUser) {
        this.voter = appUser;
    }

    public Vote voter(AppUser appUser) {
        this.setVoter(appUser);
        return this;
    }

    public AppUser getTargetUser() {
        return this.targetUser;
    }

    public void setTargetUser(AppUser appUser) {
        this.targetUser = appUser;
    }

    public Vote targetUser(AppUser appUser) {
        this.setTargetUser(appUser);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Vote)) {
            return false;
        }
        return getId() != null && getId().equals(((Vote) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Vote{" +
            "id=" + getId() +
            ", voteType='" + getVoteType() + "'" +
            ", playlistItemId='" + getPlaylistItemId() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            "}";
    }
}
