package com.partywave.backend.service.dto;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * Request DTO for Spotify token refresh.
 */
public class RefreshTokenRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank
    private String refreshToken;

    public RefreshTokenRequestDTO() {}

    public RefreshTokenRequestDTO(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    @Override
    public String toString() {
        return "RefreshTokenRequestDTO{" + "refreshToken='" + (refreshToken != null ? "[PROTECTED]" : "null") + '\'' + '}';
    }
}
