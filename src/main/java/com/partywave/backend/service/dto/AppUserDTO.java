package com.partywave.backend.service.dto;

import com.partywave.backend.domain.enumeration.AppUserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link com.partywave.backend.domain.AppUser} entity.
 */
@Schema(description = "Represents a PartyWave user linked to a Spotify account.")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AppUserDTO implements Serializable {

    private UUID id;

    @NotNull
    private String spotifyUserId;

    @NotNull
    private String displayName;

    @NotNull
    private String email;

    private String country;

    private String href;

    private String url;

    private String type;

    private String ipAddress;

    private Instant lastActiveAt;

    private AppUserStatus status;

    private AppUserStatsDTO stats;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
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

    public AppUserStatus getStatus() {
        return status;
    }

    public void setStatus(AppUserStatus status) {
        this.status = status;
    }

    public AppUserStatsDTO getStats() {
        return stats;
    }

    public void setStats(AppUserStatsDTO stats) {
        this.stats = stats;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AppUserDTO)) {
            return false;
        }

        AppUserDTO appUserDTO = (AppUserDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, appUserDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AppUserDTO{" +
            "id='" + getId() + "'" +
            ", spotifyUserId='" + getSpotifyUserId() + "'" +
            ", displayName='" + getDisplayName() + "'" +
            ", email='" + getEmail() + "'" +
            ", country='" + getCountry() + "'" +
            ", href='" + getHref() + "'" +
            ", url='" + getUrl() + "'" +
            ", type='" + getType() + "'" +
            ", ipAddress='" + getIpAddress() + "'" +
            ", lastActiveAt='" + getLastActiveAt() + "'" +
            ", status='" + getStatus() + "'" +
            ", stats=" + getStats() +
            "}";
    }
}
