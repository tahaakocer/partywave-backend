package com.partywave.backend.service.dto;

import java.util.List;

/**
 * DTO for track search results returned to frontend.
 * Contains simplified track metadata for playlist addition.
 */
public class TrackSearchResponseDTO {

    private List<TrackDTO> tracks;
    private Integer total;
    private Integer limit;
    private Integer offset;

    public TrackSearchResponseDTO() {}

    public TrackSearchResponseDTO(List<TrackDTO> tracks, Integer total, Integer limit, Integer offset) {
        this.tracks = tracks;
        this.total = total;
        this.limit = limit;
        this.offset = offset;
    }

    /**
     * Individual track metadata for search results.
     */
    public static class TrackDTO {

        private String sourceId; // Spotify track ID
        private String sourceUri; // Spotify URI (spotify:track:...)
        private String name; // Track name
        private String artist; // Primary artist name
        private String album; // Album name
        private Integer durationMs; // Track duration in milliseconds
        private String albumImageUrl; // Album cover image URL (medium size)
        private Boolean explicit; // Explicit content flag

        public TrackDTO() {}

        public TrackDTO(
            String sourceId,
            String sourceUri,
            String name,
            String artist,
            String album,
            Integer durationMs,
            String albumImageUrl,
            Boolean explicit
        ) {
            this.sourceId = sourceId;
            this.sourceUri = sourceUri;
            this.name = name;
            this.artist = artist;
            this.album = album;
            this.durationMs = durationMs;
            this.albumImageUrl = albumImageUrl;
            this.explicit = explicit;
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

        public Integer getDurationMs() {
            return durationMs;
        }

        public void setDurationMs(Integer durationMs) {
            this.durationMs = durationMs;
        }

        public String getAlbumImageUrl() {
            return albumImageUrl;
        }

        public void setAlbumImageUrl(String albumImageUrl) {
            this.albumImageUrl = albumImageUrl;
        }

        public Boolean getExplicit() {
            return explicit;
        }

        public void setExplicit(Boolean explicit) {
            this.explicit = explicit;
        }
    }

    // Main DTO Getters and Setters
    public List<TrackDTO> getTracks() {
        return tracks;
    }

    public void setTracks(List<TrackDTO> tracks) {
        this.tracks = tracks;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }
}
