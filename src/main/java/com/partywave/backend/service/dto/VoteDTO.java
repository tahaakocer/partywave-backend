package com.partywave.backend.service.dto;

import com.partywave.backend.domain.enumeration.VoteType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link com.partywave.backend.domain.Vote} entity.
 */
@Schema(description = "Votes to skip tracks or kick users.")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class VoteDTO implements Serializable {

    private UUID id;

    @NotNull
    private VoteType voteType;

    private String playlistItemId;

    @NotNull
    private Instant createdAt;

    private RoomDTO room;

    private AppUserDTO voter;

    private AppUserDTO targetUser;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public VoteType getVoteType() {
        return voteType;
    }

    public void setVoteType(VoteType voteType) {
        this.voteType = voteType;
    }

    public String getPlaylistItemId() {
        return playlistItemId;
    }

    public void setPlaylistItemId(String playlistItemId) {
        this.playlistItemId = playlistItemId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public RoomDTO getRoom() {
        return room;
    }

    public void setRoom(RoomDTO room) {
        this.room = room;
    }

    public AppUserDTO getVoter() {
        return voter;
    }

    public void setVoter(AppUserDTO voter) {
        this.voter = voter;
    }

    public AppUserDTO getTargetUser() {
        return targetUser;
    }

    public void setTargetUser(AppUserDTO targetUser) {
        this.targetUser = targetUser;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VoteDTO)) {
            return false;
        }

        VoteDTO voteDTO = (VoteDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, voteDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "VoteDTO{" +
            "id='" + getId() + "'" +
            ", voteType='" + getVoteType() + "'" +
            ", playlistItemId='" + getPlaylistItemId() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            ", room=" + getRoom() +
            ", voter=" + getVoter() +
            ", targetUser=" + getTargetUser() +
            "}";
    }
}
