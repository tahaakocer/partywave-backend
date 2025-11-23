package com.partywave.backend.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * DTO for JWT refresh token requests.
 * Client sends this when their access token expires.
 */
public class JwtRefreshRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Refresh token is required")
    @JsonProperty("refresh_token")
    private String refreshToken;

    public JwtRefreshRequestDTO() {}

    public JwtRefreshRequestDTO(String refreshToken) {
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
        return "JwtRefreshRequestDTO{" + "refreshToken='[PROTECTED]'" + '}';
    }
}
