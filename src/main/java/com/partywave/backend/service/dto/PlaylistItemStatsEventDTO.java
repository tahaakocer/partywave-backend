package com.partywave.backend.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * WebSocket event DTO for playlist item statistics updates.
 * Emitted when like/dislike counts change for a playlist item.
 *
 * Event type: PLAYLIST_ITEM_STATS_UPDATED
 * Based on PROJECT_OVERVIEW.md section 3.3 - Social Events.
 */
public class PlaylistItemStatsEventDTO implements Serializable {

    @NotNull
    @JsonProperty("type")
    private String type = "PLAYLIST_ITEM_STATS_UPDATED";

    @NotNull
    @JsonProperty("room_id")
    private String roomId;

    @NotNull
    @JsonProperty("playlist_item_id")
    private String playlistItemId;

    @NotNull
    @JsonProperty("like_count")
    private Long likeCount;

    @NotNull
    @JsonProperty("dislike_count")
    private Long dislikeCount;

    @JsonProperty("timestamp_ms")
    private Long timestampMs;

    public PlaylistItemStatsEventDTO() {
        this.timestampMs = System.currentTimeMillis();
    }

    public PlaylistItemStatsEventDTO(String roomId, String playlistItemId, Long likeCount, Long dislikeCount) {
        this.roomId = roomId;
        this.playlistItemId = playlistItemId;
        this.likeCount = likeCount;
        this.dislikeCount = dislikeCount;
        this.timestampMs = System.currentTimeMillis();
    }

    // Getters and Setters

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getPlaylistItemId() {
        return playlistItemId;
    }

    public void setPlaylistItemId(String playlistItemId) {
        this.playlistItemId = playlistItemId;
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

    public Long getTimestampMs() {
        return timestampMs;
    }

    public void setTimestampMs(Long timestampMs) {
        this.timestampMs = timestampMs;
    }

    @Override
    public String toString() {
        return (
            "PlaylistItemStatsEventDTO{" +
            "type='" +
            type +
            '\'' +
            ", roomId='" +
            roomId +
            '\'' +
            ", playlistItemId='" +
            playlistItemId +
            '\'' +
            ", likeCount=" +
            likeCount +
            ", dislikeCount=" +
            dislikeCount +
            ", timestampMs=" +
            timestampMs +
            '}'
        );
    }
}
