package com.partywave.backend.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link com.partywave.backend.domain.RefreshToken} entity.
 */
@Schema(description = "Stores PartyWave JWT refresh tokens (optional but recommended for security).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RefreshTokenDTO implements Serializable {

    private UUID id;

    @NotNull
    private String tokenHash;

    @NotNull
    private Instant expiresAt;

    @NotNull
    private Instant createdAt;

    private Instant revokedAt;

    private String deviceInfo;

    private String ipAddress;

    private AppUserDTO appUser;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public AppUserDTO getAppUser() {
        return appUser;
    }

    public void setAppUser(AppUserDTO appUser) {
        this.appUser = appUser;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RefreshTokenDTO)) {
            return false;
        }

        RefreshTokenDTO refreshTokenDTO = (RefreshTokenDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, refreshTokenDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RefreshTokenDTO{" +
            "id='" + getId() + "'" +
            ", tokenHash='" + getTokenHash() + "'" +
            ", expiresAt='" + getExpiresAt() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            ", revokedAt='" + getRevokedAt() + "'" +
            ", deviceInfo='" + getDeviceInfo() + "'" +
            ", ipAddress='" + getIpAddress() + "'" +
            ", appUser=" + getAppUser() +
            "}";
    }
}
