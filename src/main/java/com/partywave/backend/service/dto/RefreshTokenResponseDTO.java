package com.partywave.backend.service.dto;

import java.io.Serializable;

/**
 * Response DTO for Spotify token refresh.
 */
public class RefreshTokenResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String accessToken;
    private Integer expiresIn;

    public RefreshTokenResponseDTO() {}

    public RefreshTokenResponseDTO(String accessToken, Integer expiresIn) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public Integer getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Integer expiresIn) {
        this.expiresIn = expiresIn;
    }

    @Override
    public String toString() {
        return (
            "RefreshTokenResponseDTO{" +
            "accessToken='" +
            (accessToken != null ? "[PROTECTED]" : "null") +
            '\'' +
            ", expiresIn=" +
            expiresIn +
            '}'
        );
    }
}
