package com.partywave.backend.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * DTO for Spotify track search results from /v1/search endpoint.
 */
public class SpotifyTrackSearchResultDTO {

    private TracksWrapper tracks;

    public static class TracksWrapper {

        private String href;
        private Integer limit;
        private String next;
        private Integer offset;
        private String previous;
        private Integer total;
        private List<TrackDTO> items;

        // Getters and Setters
        public String getHref() {
            return href;
        }

        public void setHref(String href) {
            this.href = href;
        }

        public Integer getLimit() {
            return limit;
        }

        public void setLimit(Integer limit) {
            this.limit = limit;
        }

        public String getNext() {
            return next;
        }

        public void setNext(String next) {
            this.next = next;
        }

        public Integer getOffset() {
            return offset;
        }

        public void setOffset(Integer offset) {
            this.offset = offset;
        }

        public String getPrevious() {
            return previous;
        }

        public void setPrevious(String previous) {
            this.previous = previous;
        }

        public Integer getTotal() {
            return total;
        }

        public void setTotal(Integer total) {
            this.total = total;
        }

        public List<TrackDTO> getItems() {
            return items;
        }

        public void setItems(List<TrackDTO> items) {
            this.items = items;
        }
    }

    public static class TrackDTO {

        private String id;
        private String name;
        private String uri;

        @JsonProperty("duration_ms")
        private Integer durationMs;

        private Boolean explicit;

        @JsonProperty("preview_url")
        private String previewUrl;

        private Integer popularity;

        @JsonProperty("track_number")
        private Integer trackNumber;

        private List<ArtistDTO> artists;
        private AlbumDTO album;

        // Getters and Setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public Integer getDurationMs() {
            return durationMs;
        }

        public void setDurationMs(Integer durationMs) {
            this.durationMs = durationMs;
        }

        public Boolean getExplicit() {
            return explicit;
        }

        public void setExplicit(Boolean explicit) {
            this.explicit = explicit;
        }

        public String getPreviewUrl() {
            return previewUrl;
        }

        public void setPreviewUrl(String previewUrl) {
            this.previewUrl = previewUrl;
        }

        public Integer getPopularity() {
            return popularity;
        }

        public void setPopularity(Integer popularity) {
            this.popularity = popularity;
        }

        public Integer getTrackNumber() {
            return trackNumber;
        }

        public void setTrackNumber(Integer trackNumber) {
            this.trackNumber = trackNumber;
        }

        public List<ArtistDTO> getArtists() {
            return artists;
        }

        public void setArtists(List<ArtistDTO> artists) {
            this.artists = artists;
        }

        public AlbumDTO getAlbum() {
            return album;
        }

        public void setAlbum(AlbumDTO album) {
            this.album = album;
        }
    }

    public static class ArtistDTO {

        private String id;
        private String name;
        private String uri;

        // Getters and Setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }
    }

    public static class AlbumDTO {

        private String id;
        private String name;
        private String uri;

        @JsonProperty("album_type")
        private String albumType;

        @JsonProperty("release_date")
        private String releaseDate;

        private List<ImageDTO> images;

        // Getters and Setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getAlbumType() {
            return albumType;
        }

        public void setAlbumType(String albumType) {
            this.albumType = albumType;
        }

        public String getReleaseDate() {
            return releaseDate;
        }

        public void setReleaseDate(String releaseDate) {
            this.releaseDate = releaseDate;
        }

        public List<ImageDTO> getImages() {
            return images;
        }

        public void setImages(List<ImageDTO> images) {
            this.images = images;
        }
    }

    public static class ImageDTO {

        private String url;
        private Integer height;
        private Integer width;

        // Getters and Setters
        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public Integer getHeight() {
            return height;
        }

        public void setHeight(Integer height) {
            this.height = height;
        }

        public Integer getWidth() {
            return width;
        }

        public void setWidth(Integer width) {
            this.width = width;
        }
    }

    // Main DTO Getters and Setters
    public TracksWrapper getTracks() {
        return tracks;
    }

    public void setTracks(TracksWrapper tracks) {
        this.tracks = tracks;
    }
}
