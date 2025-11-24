package com.partywave.backend.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * DTO for returning an authorization code to the client after successful OAuth callback.
 * The client will exchange this code for JWT tokens using PKCE.
 */
public class AuthorizationCodeResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    @JsonProperty("authorization_code")
    private String authorizationCode;

    @NotNull
    @JsonProperty("expires_in")
    private Integer expiresIn;

    public AuthorizationCodeResponseDTO() {}

    public AuthorizationCodeResponseDTO(String authorizationCode, Integer expiresIn) {
        this.authorizationCode = authorizationCode;
        this.expiresIn = expiresIn;
    }

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public void setAuthorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
    }

    public Integer getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Integer expiresIn) {
        this.expiresIn = expiresIn;
    }

    @Override
    public String toString() {
        return ("AuthorizationCodeResponseDTO{" + "authorizationCode='[PROTECTED]'" + ", expiresIn=" + expiresIn + '}');
    }
}
