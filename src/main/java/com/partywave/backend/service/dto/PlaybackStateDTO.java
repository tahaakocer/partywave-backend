package com.partywave.backend.service.dto;

import java.io.Serializable;

/**
 * DTO representing the current playback state of a room.
 * Based on PROJECT_OVERVIEW.md section 2.3 - Room State Response.
 */
public class PlaybackStateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String currentPlaylistItemId;
    private Long startedAtMs;
    private Long trackDurationMs;
    private Long elapsedMs;
    private Long updatedAtMs;

    // Constructors
    public PlaybackStateDTO() {}

    // Getters and Setters

    public String getCurrentPlaylistItemId() {
        return currentPlaylistItemId;
    }

    public void setCurrentPlaylistItemId(String currentPlaylistItemId) {
        this.currentPlaylistItemId = currentPlaylistItemId;
    }

    public Long getStartedAtMs() {
        return startedAtMs;
    }

    public void setStartedAtMs(Long startedAtMs) {
        this.startedAtMs = startedAtMs;
    }

    public Long getTrackDurationMs() {
        return trackDurationMs;
    }

    public void setTrackDurationMs(Long trackDurationMs) {
        this.trackDurationMs = trackDurationMs;
    }

    public Long getElapsedMs() {
        return elapsedMs;
    }

    public void setElapsedMs(Long elapsedMs) {
        this.elapsedMs = elapsedMs;
    }

    public Long getUpdatedAtMs() {
        return updatedAtMs;
    }

    public void setUpdatedAtMs(Long updatedAtMs) {
        this.updatedAtMs = updatedAtMs;
    }

    @Override
    public String toString() {
        return (
            "PlaybackStateDTO{" +
            "currentPlaylistItemId='" +
            currentPlaylistItemId +
            '\'' +
            ", startedAtMs=" +
            startedAtMs +
            ", trackDurationMs=" +
            trackDurationMs +
            ", elapsedMs=" +
            elapsedMs +
            '}'
        );
    }
}
