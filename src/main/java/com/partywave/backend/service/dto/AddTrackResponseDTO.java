package com.partywave.backend.service.dto;

import java.io.Serializable;

/**
 * DTO for response after adding a track to playlist.
 * Based on PROJECT_OVERVIEW.md section 2.6 - Adding Tracks to Playlist.
 *
 * Returns the created playlist item with full metadata.
 */
public class AddTrackResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String playlistItemId; // UUID of the created playlist item
    private String roomId; // Room UUID
    private String sourceId; // Spotify track ID
    private String sourceUri; // Spotify URI
    private String name; // Track name
    private String artist; // Artist name
    private String album; // Album name
    private Long durationMs; // Track duration in milliseconds
    private String albumImageUrl; // Album image URL
    private String addedById; // User UUID who added the track
    private String addedByDisplayName; // Display name of user who added the track
    private Long addedAtMs; // UTC epoch milliseconds when added
    private Long sequenceNumber; // Chronological order number
    private String status; // QUEUED (always for newly added tracks)
    private Long likeCount; // Like count (0 for new tracks)
    private Long dislikeCount; // Dislike count (0 for new tracks)
    private boolean autoStarted; // Whether this track was auto-started

    // Constructors
    public AddTrackResponseDTO() {}

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

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getSourceUri() {
        return sourceUri;
    }

    public void setSourceUri(String sourceUri) {
        this.sourceUri = sourceUri;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getAlbumImageUrl() {
        return albumImageUrl;
    }

    public void setAlbumImageUrl(String albumImageUrl) {
        this.albumImageUrl = albumImageUrl;
    }

    public String getAddedById() {
        return addedById;
    }

    public void setAddedById(String addedById) {
        this.addedById = addedById;
    }

    public String getAddedByDisplayName() {
        return addedByDisplayName;
    }

    public void setAddedByDisplayName(String addedByDisplayName) {
        this.addedByDisplayName = addedByDisplayName;
    }

    public Long getAddedAtMs() {
        return addedAtMs;
    }

    public void setAddedAtMs(Long addedAtMs) {
        this.addedAtMs = addedAtMs;
    }

    public Long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public boolean isAutoStarted() {
        return autoStarted;
    }

    public void setAutoStarted(boolean autoStarted) {
        this.autoStarted = autoStarted;
    }

    @Override
    public String toString() {
        return (
            "AddTrackResponseDTO{" +
            "playlistItemId='" +
            playlistItemId +
            '\'' +
            ", name='" +
            name +
            '\'' +
            ", artist='" +
            artist +
            '\'' +
            ", status='" +
            status +
            '\'' +
            ", sequenceNumber=" +
            sequenceNumber +
            ", autoStarted=" +
            autoStarted +
            '}'
        );
    }
}
