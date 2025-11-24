package com.partywave.backend.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * DTO for exchanging an authorization code for JWT tokens using PKCE.
 * The client provides the authorization code and code verifier.
 */
public class TokenExchangeRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Authorization code is required")
    @JsonProperty("authorization_code")
    private String authorizationCode;

    @NotBlank(message = "Code verifier is required")
    @JsonProperty("code_verifier")
    private String codeVerifier;

    public TokenExchangeRequestDTO() {}

    public TokenExchangeRequestDTO(String authorizationCode, String codeVerifier) {
        this.authorizationCode = authorizationCode;
        this.codeVerifier = codeVerifier;
    }

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public void setAuthorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
    }

    public String getCodeVerifier() {
        return codeVerifier;
    }

    public void setCodeVerifier(String codeVerifier) {
        this.codeVerifier = codeVerifier;
    }

    @Override
    public String toString() {
        return ("TokenExchangeRequestDTO{" + "authorizationCode='[PROTECTED]'" + ", codeVerifier='[PROTECTED]'" + '}');
    }
}
