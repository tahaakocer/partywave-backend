package com.partywave.backend.service.dto;

import java.io.Serializable;

/**
 * DTO representing a playlist item with track metadata and feedback counts.
 * Based on PROJECT_OVERVIEW.md section 2.3 - Room State Response.
 */
public class PlaylistItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String roomId;
    private String spotifyTrackId;
    private String trackName;
    private String trackArtist;
    private String trackAlbum;
    private String trackImageUrl;
    private Long durationMs;
    private String addedById;
    private String addedByDisplayName;
    private Long addedAtMs;
    private Long sequenceNumber;
    private String status; // QUEUED, PLAYING, PLAYED, SKIPPED
    private Long likeCount;
    private Long dislikeCount;

    // Constructors
    public PlaylistItemDTO() {}

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getSpotifyTrackId() {
        return spotifyTrackId;
    }

    public void setSpotifyTrackId(String spotifyTrackId) {
        this.spotifyTrackId = spotifyTrackId;
    }

    public String getTrackName() {
        return trackName;
    }

    public void setTrackName(String trackName) {
        this.trackName = trackName;
    }

    public String getTrackArtist() {
        return trackArtist;
    }

    public void setTrackArtist(String trackArtist) {
        this.trackArtist = trackArtist;
    }

    public String getTrackAlbum() {
        return trackAlbum;
    }

    public void setTrackAlbum(String trackAlbum) {
        this.trackAlbum = trackAlbum;
    }

    public String getTrackImageUrl() {
        return trackImageUrl;
    }

    public void setTrackImageUrl(String trackImageUrl) {
        this.trackImageUrl = trackImageUrl;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
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

    @Override
    public String toString() {
        return (
            "PlaylistItemDTO{" +
            "id='" +
            id +
            '\'' +
            ", spotifyTrackId='" +
            spotifyTrackId +
            '\'' +
            ", trackName='" +
            trackName +
            '\'' +
            ", status='" +
            status +
            '\'' +
            ", sequenceNumber=" +
            sequenceNumber +
            '}'
        );
    }
}
