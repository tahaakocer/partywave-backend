package com.partywave.backend.service.dto;

import java.io.Serializable;

/**
 * Response DTO for Spotify authentication.
 * Contains access token, refresh token, and user profile information.
 */
public class SpotifyAuthResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String error;
    private String accessToken;
    private String refreshToken;
    private Integer expiresIn;
    private String spotifyId;
    private String email;
    private String displayName;
    private String imageUrl;

    public SpotifyAuthResponseDTO() {}

    public SpotifyAuthResponseDTO(String error, String accessToken, String refreshToken, Integer expiresIn) {
        this.error = error;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
    }

    public SpotifyAuthResponseDTO(
        String error,
        String accessToken,
        String refreshToken,
        Integer expiresIn,
        String spotifyId,
        String email,
        String displayName,
        String imageUrl
    ) {
        this.error = error;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.spotifyId = spotifyId;
        this.email = email;
        this.displayName = displayName;
        this.imageUrl = imageUrl;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
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

    public Integer getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Integer expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getSpotifyId() {
        return spotifyId;
    }

    public void setSpotifyId(String spotifyId) {
        this.spotifyId = spotifyId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @Override
    public String toString() {
        return (
            "SpotifyAuthResponseDTO{" +
            "error='" +
            error +
            '\'' +
            ", accessToken='" +
            (accessToken != null ? "[PROTECTED]" : "null") +
            '\'' +
            ", refreshToken='" +
            (refreshToken != null ? "[PROTECTED]" : "null") +
            '\'' +
            ", expiresIn=" +
            expiresIn +
            ", spotifyId='" +
            spotifyId +
            '\'' +
            ", email='" +
            email +
            '\'' +
            ", displayName='" +
            displayName +
            '\'' +
            ", imageUrl='" +
            imageUrl +
            '\'' +
            '}'
        );
    }
}
