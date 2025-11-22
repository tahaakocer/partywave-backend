package com.partywave.backend.service.dto;

import com.partywave.backend.domain.enumeration.RoomMemberRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.partywave.backend.domain.RoomMember} entity.
 */
@Schema(description = "Represents a user's membership in a room.")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RoomMemberDTO implements Serializable {

    private Long id;

    private Instant joinedAt;

    private Instant lastActiveAt;

    private RoomMemberRole role;

    private RoomDTO room;

    private AppUserDTO appUser;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }

    public Instant getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(Instant lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }

    public RoomMemberRole getRole() {
        return role;
    }

    public void setRole(RoomMemberRole role) {
        this.role = role;
    }

    public RoomDTO getRoom() {
        return room;
    }

    public void setRoom(RoomDTO room) {
        this.room = room;
    }

    public AppUserDTO getAppUser() {
        return appUser;
    }

    public void setAppUser(AppUserDTO appUser) {
        this.appUser = appUser;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RoomMemberDTO)) {
            return false;
        }

        RoomMemberDTO roomMemberDTO = (RoomMemberDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, roomMemberDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RoomMemberDTO{" +
            "id=" + getId() +
            ", joinedAt='" + getJoinedAt() + "'" +
            ", lastActiveAt='" + getLastActiveAt() + "'" +
            ", role='" + getRole() + "'" +
            ", room=" + getRoom() +
            ", appUser=" + getAppUser() +
            "}";
    }
}
