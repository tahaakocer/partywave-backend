package com.partywave.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Service for handling Spotify OAuth2 authentication flow.
 * Implements Authorization Code Flow as described in SPOTIFY_AUTH_ENDPOINTS.md
 */
@Service
public class SpotifyAuthService {

    private static final Logger LOG = LoggerFactory.getLogger(SpotifyAuthService.class);

    private static final String SPOTIFY_AUTH_URL = "https://accounts.spotify.com/authorize";
    private static final String SPOTIFY_TOKEN_URL = "https://accounts.spotify.com/api/token";
    private static final String SPOTIFY_USER_PROFILE_URL = "https://api.spotify.com/v1/me";

    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.client-secret}")
    private String clientSecret;

    @Value("${spotify.redirect-uri}")
    private String redirectUri;

    @Value("${spotify.scope:user-read-email user-read-private}")
    private String scope;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public SpotifyAuthService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Generates the Spotify authorization URL for redirecting users to login.
     *
     * @param state Optional CSRF protection state parameter
     * @return Complete authorization URL
     */
    public String getAuthorizationUrl(String state) {
        LOG.debug("Generating Spotify authorization URL with state: {}", state);

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(SPOTIFY_AUTH_URL)
            .queryParam("client_id", clientId)
            .queryParam("response_type", "code")
            .queryParam("redirect_uri", redirectUri)
            .queryParam("scope", scope);

        if (state != null && !state.isEmpty()) {
            builder.queryParam("state", state);
        }

        String authUrl = builder.toUriString();
        LOG.debug("Generated authorization URL: {}", authUrl);
        return authUrl;
    }

    /**
     * Exchanges authorization code for access and refresh tokens.
     *
     * @param code Authorization code received from Spotify callback
     * @return JsonNode containing access_token, refresh_token, expires_in, etc.
     * @throws Exception if token exchange fails
     */
    public JsonNode exchangeCodeForTokens(String code) throws Exception {
        LOG.debug("Exchanging authorization code for tokens");

        // Prepare headers with Basic authentication
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Basic " + getBase64EncodedCredentials());

        // Prepare request body
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(SPOTIFY_TOKEN_URL, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode tokenResponse = objectMapper.readTree(response.getBody());
                LOG.debug("Successfully exchanged code for tokens");
                return tokenResponse;
            } else {
                LOG.error("Failed to exchange code for tokens. Status: {}", response.getStatusCode());
                throw new Exception("Failed to exchange authorization code for tokens");
            }
        } catch (Exception e) {
            LOG.error("Error exchanging code for tokens: {}", e.getMessage(), e);
            throw new Exception("Error during token exchange: " + e.getMessage(), e);
        }
    }

    /**
     * Fetches the user's Spotify profile using the access token.
     *
     * @param accessToken Spotify access token
     * @return JsonNode containing user profile data (id, email, display_name, etc.)
     * @throws Exception if profile fetch fails
     */
    public JsonNode fetchUserProfile(String accessToken) throws Exception {
        LOG.debug("Fetching user profile from Spotify");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(SPOTIFY_USER_PROFILE_URL, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode userProfile = objectMapper.readTree(response.getBody());
                LOG.debug("Successfully fetched user profile for Spotify ID: {}", userProfile.get("id").asText());
                return userProfile;
            } else {
                LOG.error("Failed to fetch user profile. Status: {}", response.getStatusCode());
                throw new Exception("Failed to fetch user profile from Spotify");
            }
        } catch (Exception e) {
            LOG.error("Error fetching user profile: {}", e.getMessage(), e);
            throw new Exception("Error fetching user profile: " + e.getMessage(), e);
        }
    }

    /**
     * Refreshes an expired access token using the refresh token.
     *
     * @param refreshToken Spotify refresh token
     * @return JsonNode containing new access_token and expires_in
     * @throws Exception if token refresh fails
     */
    public JsonNode refreshAccessToken(String refreshToken) throws Exception {
        LOG.debug("Refreshing access token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Basic " + getBase64EncodedCredentials());

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(SPOTIFY_TOKEN_URL, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode tokenResponse = objectMapper.readTree(response.getBody());
                LOG.debug("Successfully refreshed access token");
                return tokenResponse;
            } else {
                LOG.error("Failed to refresh access token. Status: {}", response.getStatusCode());
                throw new Exception("Failed to refresh access token");
            }
        } catch (Exception e) {
            LOG.error("Error refreshing access token: {}", e.getMessage(), e);
            throw new Exception("Error refreshing access token: " + e.getMessage(), e);
        }
    }

    /**
     * Encodes client credentials for Basic authentication.
     *
     * @return Base64 encoded string of "client_id:client_secret"
     */
    private String getBase64EncodedCredentials() {
        String credentials = clientId + ":" + clientSecret;
        return Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
