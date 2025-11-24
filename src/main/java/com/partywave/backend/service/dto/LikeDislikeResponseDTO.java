package com.partywave.backend.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Response DTO for like/dislike operations.
 * Returns updated like/dislike counts for a playlist item.
 *
 * Based on PROJECT_OVERVIEW.md section 2.10 - Like / Dislike Tracks.
 */
public class LikeDislikeResponseDTO implements Serializable {

    @NotNull
    @JsonProperty("playlist_item_id")
    private String playlistItemId;

    @NotNull
    @JsonProperty("room_id")
    private String roomId;

    @NotNull
    @JsonProperty("like_count")
    private Long likeCount;

    @NotNull
    @JsonProperty("dislike_count")
    private Long dislikeCount;

    @JsonProperty("user_liked")
    private boolean userLiked;

    @JsonProperty("user_disliked")
    private boolean userDisliked;

    @JsonProperty("message")
    private String message;

    public LikeDislikeResponseDTO() {}

    public LikeDislikeResponseDTO(
        String playlistItemId,
        String roomId,
        Long likeCount,
        Long dislikeCount,
        boolean userLiked,
        boolean userDisliked,
        String message
    ) {
        this.playlistItemId = playlistItemId;
        this.roomId = roomId;
        this.likeCount = likeCount;
        this.dislikeCount = dislikeCount;
        this.userLiked = userLiked;
        this.userDisliked = userDisliked;
        this.message = message;
    }

    // Getters and Setters

    public String getPlaylistItemId() {
        return playlistItemId;
    }

    public void setPlaylistItemId(String playlistItemId) {
        this.playlistItemId = playlistItemId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public Long getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Long likeCount) {
        this.likeCount = likeCount;
    }

    public Long getDislikeCount() {
        return dislikeCount;
    }

    public void setDislikeCount(Long dislikeCount) {
        this.dislikeCount = dislikeCount;
    }

    public boolean isUserLiked() {
        return userLiked;
    }

    public void setUserLiked(boolean userLiked) {
        this.userLiked = userLiked;
    }

    public boolean isUserDisliked() {
        return userDisliked;
    }

    public void setUserDisliked(boolean userDisliked) {
        this.userDisliked = userDisliked;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return (
            "LikeDislikeResponseDTO{" +
            "playlistItemId='" +
            playlistItemId +
            '\'' +
            ", roomId='" +
            roomId +
            '\'' +
            ", likeCount=" +
            likeCount +
            ", dislikeCount=" +
            dislikeCount +
            ", userLiked=" +
            userLiked +
            ", userDisliked=" +
            userDisliked +
            ", message='" +
            message +
            '\'' +
            '}'
        );
    }
}
