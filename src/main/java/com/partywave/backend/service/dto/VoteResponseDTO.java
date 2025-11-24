package com.partywave.backend.service.dto;

import java.io.Serializable;

/**
 * DTO for vote operation response.
 * Based on PROJECT_OVERVIEW.md sections 2.8 and 2.9 - Vote-Based Skip/Kick.
 *
 * Returns vote status and whether threshold was reached.
 */
public class VoteResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean voteRecorded; // Whether vote was successfully recorded
    private long currentVoteCount; // Current votes for this action
    private long requiredVoteCount; // Votes required to reach threshold
    private long onlineMemberCount; // Total online members
    private boolean thresholdReached; // Whether action was executed
    private String message; // User-friendly message
    private String targetPlaylistItemId; // For skip votes: the track being voted on
    private String targetUserId; // For kick votes: the user being voted on

    // Constructors
    public VoteResponseDTO() {}

    public VoteResponseDTO(
        boolean voteRecorded,
        long currentVoteCount,
        long requiredVoteCount,
        long onlineMemberCount,
        boolean thresholdReached,
        String message
    ) {
        this.voteRecorded = voteRecorded;
        this.currentVoteCount = currentVoteCount;
        this.requiredVoteCount = requiredVoteCount;
        this.onlineMemberCount = onlineMemberCount;
        this.thresholdReached = thresholdReached;
        this.message = message;
    }

    // Getters and Setters

    public boolean isVoteRecorded() {
        return voteRecorded;
    }

    public void setVoteRecorded(boolean voteRecorded) {
        this.voteRecorded = voteRecorded;
    }

    public long getCurrentVoteCount() {
        return currentVoteCount;
    }

    public void setCurrentVoteCount(long currentVoteCount) {
        this.currentVoteCount = currentVoteCount;
    }

    public long getRequiredVoteCount() {
        return requiredVoteCount;
    }

    public void setRequiredVoteCount(long requiredVoteCount) {
        this.requiredVoteCount = requiredVoteCount;
    }

    public long getOnlineMemberCount() {
        return onlineMemberCount;
    }

    public void setOnlineMemberCount(long onlineMemberCount) {
        this.onlineMemberCount = onlineMemberCount;
    }

    public boolean isThresholdReached() {
        return thresholdReached;
    }

    public void setThresholdReached(boolean thresholdReached) {
        this.thresholdReached = thresholdReached;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTargetPlaylistItemId() {
        return targetPlaylistItemId;
    }

    public void setTargetPlaylistItemId(String targetPlaylistItemId) {
        this.targetPlaylistItemId = targetPlaylistItemId;
    }

    public String getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(String targetUserId) {
        this.targetUserId = targetUserId;
    }

    @Override
    public String toString() {
        return (
            "VoteResponseDTO{" +
            "voteRecorded=" +
            voteRecorded +
            ", currentVoteCount=" +
            currentVoteCount +
            ", requiredVoteCount=" +
            requiredVoteCount +
            ", thresholdReached=" +
            thresholdReached +
            ", message='" +
            message +
            '\'' +
            '}'
        );
    }
}
