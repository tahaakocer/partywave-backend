package com.partywave.backend.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link com.partywave.backend.domain.RoomInvitation} entity.
 */
@Schema(description = "Invitation tokens for private rooms.")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RoomInvitationDTO implements Serializable {

    private UUID id;

    @NotNull
    private String token;

    @NotNull
    private Instant createdAt;

    private Instant expiresAt;

    private Integer maxUses;

    @NotNull
    private Integer usedCount;

    @NotNull
    private Boolean isActive;

    private RoomDTO room;

    private AppUserDTO createdBy;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Integer getMaxUses() {
        return maxUses;
    }

    public void setMaxUses(Integer maxUses) {
        this.maxUses = maxUses;
    }

    public Integer getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(Integer usedCount) {
        this.usedCount = usedCount;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public RoomDTO getRoom() {
        return room;
    }

    public void setRoom(RoomDTO room) {
        this.room = room;
    }

    public AppUserDTO getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(AppUserDTO createdBy) {
        this.createdBy = createdBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RoomInvitationDTO)) {
            return false;
        }

        RoomInvitationDTO roomInvitationDTO = (RoomInvitationDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, roomInvitationDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RoomInvitationDTO{" +
            "id='" + getId() + "'" +
            ", token='" + getToken() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            ", expiresAt='" + getExpiresAt() + "'" +
            ", maxUses=" + getMaxUses() +
            ", usedCount=" + getUsedCount() +
            ", isActive='" + getIsActive() + "'" +
            ", room=" + getRoom() +
            ", createdBy=" + getCreatedBy() +
            "}";
    }
}
