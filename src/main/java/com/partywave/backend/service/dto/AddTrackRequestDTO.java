package com.partywave.backend.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.io.Serializable;

/**
 * DTO for adding a track to a room's playlist.
 * Based on PROJECT_OVERVIEW.md section 2.6 - Adding Tracks to Playlist.
 *
 * Input: track metadata from Spotify search results.
 * Required fields: source_id, source_uri, name, artist, album, duration_ms
 */
public class AddTrackRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "source_id is required")
    private String sourceId; // Spotify track ID

    @NotBlank(message = "source_uri is required")
    private String sourceUri; // Spotify URI (e.g., spotify:track:...)

    @NotBlank(message = "name is required")
    private String name; // Track name

    @NotBlank(message = "artist is required")
    private String artist; // Primary artist name

    @NotBlank(message = "album is required")
    private String album; // Album name

    @NotNull(message = "duration_ms is required")
    @Positive(message = "duration_ms must be positive")
    private Long durationMs; // Track duration in milliseconds

    private String albumImageUrl; // Optional album image URL

    // Constructors
    public AddTrackRequestDTO() {}

    public AddTrackRequestDTO(
        String sourceId,
        String sourceUri,
        String name,
        String artist,
        String album,
        Long durationMs,
        String albumImageUrl
    ) {
        this.sourceId = sourceId;
        this.sourceUri = sourceUri;
        this.name = name;
        this.artist = artist;
        this.album = album;
        this.durationMs = durationMs;
        this.albumImageUrl = albumImageUrl;
    }

    // Getters and Setters

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

    @Override
    public String toString() {
        return (
            "AddTrackRequestDTO{" +
            "sourceId='" +
            sourceId +
            '\'' +
            ", sourceUri='" +
            sourceUri +
            '\'' +
            ", name='" +
            name +
            '\'' +
            ", artist='" +
            artist +
            '\'' +
            ", album='" +
            album +
            '\'' +
            ", durationMs=" +
            durationMs +
            '}'
        );
    }
}
