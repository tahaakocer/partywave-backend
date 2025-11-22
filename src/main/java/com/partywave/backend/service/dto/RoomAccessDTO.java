package com.partywave.backend.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.partywave.backend.domain.RoomAccess} entity.
 */
@Schema(description = "Explicit access grants for private rooms.")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RoomAccessDTO implements Serializable {

    private Long id;

    private Instant grantedAt;

    private RoomDTO room;

    private AppUserDTO appUser;

    private AppUserDTO grantedBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(Instant grantedAt) {
        this.grantedAt = grantedAt;
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

    public AppUserDTO getGrantedBy() {
        return grantedBy;
    }

    public void setGrantedBy(AppUserDTO grantedBy) {
        this.grantedBy = grantedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RoomAccessDTO)) {
            return false;
        }

        RoomAccessDTO roomAccessDTO = (RoomAccessDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, roomAccessDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RoomAccessDTO{" +
            "id=" + getId() +
            ", grantedAt='" + getGrantedAt() + "'" +
            ", room=" + getRoom() +
            ", appUser=" + getAppUser() +
            ", grantedBy=" + getGrantedBy() +
            "}";
    }
}
