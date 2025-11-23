package com.partywave.backend.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link com.partywave.backend.domain.UserToken} entity.
 */
@Schema(description = "Stores Spotify OAuth access and refresh tokens.")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class UserTokenDTO implements Serializable {

    private UUID id;

    @NotNull
    private String accessToken;

    @NotNull
    private String refreshToken;

    private String tokenType;

    private Instant expiresAt;

    private String scope;

    @NotNull
    private AppUserDTO appUser;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
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
        if (!(o instanceof UserTokenDTO)) {
            return false;
        }

        UserTokenDTO userTokenDTO = (UserTokenDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, userTokenDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "UserTokenDTO{" +
            "id='" + getId() + "'" +
            ", accessToken='" + getAccessToken() + "'" +
            ", refreshToken='" + getRefreshToken() + "'" +
            ", tokenType='" + getTokenType() + "'" +
            ", expiresAt='" + getExpiresAt() + "'" +
            ", scope='" + getScope() + "'" +
            ", appUser=" + getAppUser() +
            "}";
    }
}
