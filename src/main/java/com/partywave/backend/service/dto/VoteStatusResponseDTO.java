package com.partywave.backend.service.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for vote status response (GET /api/rooms/{roomId}/votes).
 * Based on PROJECT_OVERVIEW.md sections 2.8 and 2.9.
 *
 * Returns current vote counts for skip track and kick users.
 */
public class VoteStatusResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private SkipTrackVoteStatus skipTrackVote; // Current skip track vote status
    private List<KickUserVoteStatus> kickUserVotes; // Kick votes for each user

    // Constructors
    public VoteStatusResponseDTO() {
        this.kickUserVotes = new ArrayList<>();
    }

    public VoteStatusResponseDTO(SkipTrackVoteStatus skipTrackVote, List<KickUserVoteStatus> kickUserVotes) {
        this.skipTrackVote = skipTrackVote;
        this.kickUserVotes = kickUserVotes != null ? kickUserVotes : new ArrayList<>();
    }

    // Getters and Setters

    public SkipTrackVoteStatus getSkipTrackVote() {
        return skipTrackVote;
    }

    public void setSkipTrackVote(SkipTrackVoteStatus skipTrackVote) {
        this.skipTrackVote = skipTrackVote;
    }

    public List<KickUserVoteStatus> getKickUserVotes() {
        return kickUserVotes;
    }

    public void setKickUserVotes(List<KickUserVoteStatus> kickUserVotes) {
        this.kickUserVotes = kickUserVotes;
    }

    /**
     * Nested DTO for skip track vote status.
     */
    public static class SkipTrackVoteStatus implements Serializable {

        private static final long serialVersionUID = 1L;

        private String playlistItemId; // Current playing track ID
        private String trackName; // Track name
        private String artistName; // Artist name
        private long currentVoteCount; // Current votes to skip
        private long requiredVoteCount; // Votes required
        private long onlineMemberCount; // Total online members

        // Constructors
        public SkipTrackVoteStatus() {}

        public SkipTrackVoteStatus(
            String playlistItemId,
            String trackName,
            String artistName,
            long currentVoteCount,
            long requiredVoteCount,
            long onlineMemberCount
        ) {
            this.playlistItemId = playlistItemId;
            this.trackName = trackName;
            this.artistName = artistName;
            this.currentVoteCount = currentVoteCount;
            this.requiredVoteCount = requiredVoteCount;
            this.onlineMemberCount = onlineMemberCount;
        }

        // Getters and Setters

        public String getPlaylistItemId() {
            return playlistItemId;
        }

        public void setPlaylistItemId(String playlistItemId) {
            this.playlistItemId = playlistItemId;
        }

        public String getTrackName() {
            return trackName;
        }

        public void setTrackName(String trackName) {
            this.trackName = trackName;
        }

        public String getArtistName() {
            return artistName;
        }

        public void setArtistName(String artistName) {
            this.artistName = artistName;
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
    }

    /**
     * Nested DTO for kick user vote status.
     */
    public static class KickUserVoteStatus implements Serializable {

        private static final long serialVersionUID = 1L;

        private String targetUserId; // User being voted to kick
        private String targetUserDisplayName; // User's display name
        private long currentVoteCount; // Current votes to kick this user
        private long requiredVoteCount; // Votes required
        private long onlineMemberCount; // Total online members

        // Constructors
        public KickUserVoteStatus() {}

        public KickUserVoteStatus(
            String targetUserId,
            String targetUserDisplayName,
            long currentVoteCount,
            long requiredVoteCount,
            long onlineMemberCount
        ) {
            this.targetUserId = targetUserId;
            this.targetUserDisplayName = targetUserDisplayName;
            this.currentVoteCount = currentVoteCount;
            this.requiredVoteCount = requiredVoteCount;
            this.onlineMemberCount = onlineMemberCount;
        }

        // Getters and Setters

        public String getTargetUserId() {
            return targetUserId;
        }

        public void setTargetUserId(String targetUserId) {
            this.targetUserId = targetUserId;
        }

        public String getTargetUserDisplayName() {
            return targetUserDisplayName;
        }

        public void setTargetUserDisplayName(String targetUserDisplayName) {
            this.targetUserDisplayName = targetUserDisplayName;
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
    }
}
