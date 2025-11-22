package com.partywave.backend.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.partywave.backend.domain.enumeration.RoomMemberRole;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Represents a user's membership in a room.
 */
@Entity
@Table(name = "room_member")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RoomMember implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @Column(name = "joined_at")
    private Instant joinedAt;

    @Column(name = "last_active_at")
    private Instant lastActiveAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private RoomMemberRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "members", "accesses", "invitations", "messages", "votes", "tags" }, allowSetters = true)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
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
    private AppUser appUser;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public RoomMember id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getJoinedAt() {
        return this.joinedAt;
    }

    public RoomMember joinedAt(Instant joinedAt) {
        this.setJoinedAt(joinedAt);
        return this;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }

    public Instant getLastActiveAt() {
        return this.lastActiveAt;
    }

    public RoomMember lastActiveAt(Instant lastActiveAt) {
        this.setLastActiveAt(lastActiveAt);
        return this;
    }

    public void setLastActiveAt(Instant lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }

    public RoomMemberRole getRole() {
        return this.role;
    }

    public RoomMember role(RoomMemberRole role) {
        this.setRole(role);
        return this;
    }

    public void setRole(RoomMemberRole role) {
        this.role = role;
    }

    public Room getRoom() {
        return this.room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public RoomMember room(Room room) {
        this.setRoom(room);
        return this;
    }

    public AppUser getAppUser() {
        return this.appUser;
    }

    public void setAppUser(AppUser appUser) {
        this.appUser = appUser;
    }

    public RoomMember appUser(AppUser appUser) {
        this.setAppUser(appUser);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RoomMember)) {
            return false;
        }
        return getId() != null && getId().equals(((RoomMember) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RoomMember{" +
            "id=" + getId() +
            ", joinedAt='" + getJoinedAt() + "'" +
            ", lastActiveAt='" + getLastActiveAt() + "'" +
            ", role='" + getRole() + "'" +
            "}";
    }
}
