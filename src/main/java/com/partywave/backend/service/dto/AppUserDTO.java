package com.partywave.backend.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * DTO for AppUser entity with full details.
 * Used for returning detailed user information.
 */
public class AppUserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id")
    private String id;

    @JsonProperty("spotify_user_id")
    private String spotifyUserId;

    @JsonProperty("display_name")
    private String displayName;

    @JsonProperty("email")
    private String email;

    @JsonProperty("country")
    private String country;

    @JsonProperty("href")
    private String href;

    @JsonProperty("url")
    private String url;

    @JsonProperty("type")
    private String type;

    @JsonProperty("ip_address")
    private String ipAddress;

    @JsonProperty("last_active_at")
    private Instant lastActiveAt;

    @JsonProperty("status")
    private String status;

    @JsonProperty("stats")
    private StatsDTO stats;

    @JsonProperty("images")
    private List<ImageDTO> images;

    public AppUserDTO() {}

    // Getters and setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSpotifyUserId() {
        return spotifyUserId;
    }

    public void setSpotifyUserId(String spotifyUserId) {
        this.spotifyUserId = spotifyUserId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Instant getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(Instant lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public StatsDTO getStats() {
        return stats;
    }

    public void setStats(StatsDTO stats) {
        this.stats = stats;
    }

    public List<ImageDTO> getImages() {
        return images;
    }

    public void setImages(List<ImageDTO> images) {
        this.images = images;
    }

    /**
     * Nested DTO for user statistics.
     */
    public static class StatsDTO implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("total_like")
        private Integer totalLike;

        @JsonProperty("total_dislike")
        private Integer totalDislike;

        public StatsDTO() {}

        public Integer getTotalLike() {
            return totalLike;
        }

        public void setTotalLike(Integer totalLike) {
            this.totalLike = totalLike;
        }

        public Integer getTotalDislike() {
            return totalDislike;
        }

        public void setTotalDislike(Integer totalDislike) {
            this.totalDislike = totalDislike;
        }
    }

    /**
     * Nested DTO for user images.
     */
    public static class ImageDTO implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("url")
        private String url;

        @JsonProperty("height")
        private Integer height;

        @JsonProperty("width")
        private Integer width;

        public ImageDTO() {}

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
}
