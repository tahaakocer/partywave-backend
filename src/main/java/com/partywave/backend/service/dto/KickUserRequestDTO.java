package com.partywave.backend.service.dto;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.UUID;

/**
 * DTO for requesting to vote to kick a user from a room.
 * Based on PROJECT_OVERVIEW.md section 2.9 - Kicking Users (Vote-Based).
 *
 * Input: target user ID to kick.
 */
public class KickUserRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "target_user_id is required")
    private UUID targetUserId; // User to kick

    // Constructors
    public KickUserRequestDTO() {}

    public KickUserRequestDTO(UUID targetUserId) {
        this.targetUserId = targetUserId;
    }

    // Getters and Setters

    public UUID getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(UUID targetUserId) {
        this.targetUserId = targetUserId;
    }

    @Override
    public String toString() {
        return "KickUserRequestDTO{" + "targetUserId=" + targetUserId + '}';
    }
}
