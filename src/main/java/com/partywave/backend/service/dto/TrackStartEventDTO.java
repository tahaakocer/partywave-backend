package com.partywave.backend.service.dto;

/**
 * DTO for TRACK_START WebSocket event.
 * Sent to all room members when a track starts playing.
 *
 * Based on PROJECT_OVERVIEW.md section 2.7:
 * - playlist_item_id: UUID of the playlist item
 * - track metadata: name, artist, album, duration, source_uri
 * - started_at_ms: UTC epoch milliseconds when playback started
 * - track_duration_ms: Track duration in milliseconds
 */
public class TrackStartEventDTO {

    private String type = "TRACK_START";
    private String roomId;
    private String playlistItemId;
    private TrackMetadata track;
    private Long startedAtMs;
    private Long trackDurationMs;

    public TrackStartEventDTO() {}

    public TrackStartEventDTO(String roomId, String playlistItemId, TrackMetadata track, Long startedAtMs, Long trackDurationMs) {
        this.roomId = roomId;
        this.playlistItemId = playlistItemId;
        this.track = track;
        this.startedAtMs = startedAtMs;
        this.trackDurationMs = trackDurationMs;
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

    public TrackMetadata getTrack() {
        return track;
    }

    public void setTrack(TrackMetadata track) {
        this.track = track;
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

    /**
     * Nested class for track metadata.
     */
    public static class TrackMetadata {

        private String sourceId;
        private String sourceUri;
        private String name;
        private String artist;
        private String album;
        private Long durationMs;
        private String albumImageUrl;

        public TrackMetadata() {}

        public TrackMetadata(
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
    }
}
