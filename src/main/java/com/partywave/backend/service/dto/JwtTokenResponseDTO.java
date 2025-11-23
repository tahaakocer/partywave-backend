package com.partywave.backend.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * DTO for returning JWT access and refresh tokens to the client.
 * Used after successful authentication.
 */
public class JwtTokenResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    @JsonProperty("access_token")
    private String accessToken;

    @NotNull
    @JsonProperty("refresh_token")
    private String refreshToken;

    @NotNull
    @JsonProperty("token_type")
    private String tokenType;

    @NotNull
    @JsonProperty("expires_in")
    private Long expiresIn;

    @JsonProperty("user")
    private UserInfoDTO user;

    public JwtTokenResponseDTO() {
        this.tokenType = "Bearer";
    }

    public JwtTokenResponseDTO(String accessToken, String refreshToken, Long expiresIn, UserInfoDTO user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = "Bearer";
        this.expiresIn = expiresIn;
        this.user = user;
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

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public UserInfoDTO getUser() {
        return user;
    }

    public void setUser(UserInfoDTO user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return (
            "JwtTokenResponseDTO{" +
            "accessToken='[PROTECTED]'" +
            ", refreshToken='[PROTECTED]'" +
            ", tokenType='" +
            tokenType +
            '\'' +
            ", expiresIn=" +
            expiresIn +
            ", user=" +
            user +
            '}'
        );
    }

    /**
     * Nested DTO for user information in token response.
     */
    public static class UserInfoDTO implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("id")
        private String id;

        @JsonProperty("spotify_user_id")
        private String spotifyUserId;

        @JsonProperty("email")
        private String email;

        @JsonProperty("display_name")
        private String displayName;

        public UserInfoDTO() {}

        public UserInfoDTO(String id, String spotifyUserId, String email, String displayName) {
            this.id = id;
            this.spotifyUserId = spotifyUserId;
            this.email = email;
            this.displayName = displayName;
        }

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

        @Override
        public String toString() {
            return (
                "UserInfoDTO{" +
                "id='" +
                id +
                '\'' +
                ", spotifyUserId='" +
                spotifyUserId +
                '\'' +
                ", email='" +
                email +
                '\'' +
                ", displayName='" +
                displayName +
                '\'' +
                '}'
            );
        }
    }
}
