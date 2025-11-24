package com.partywave.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for PKCE (Proof Key for Code Exchange) operations.
 * Implements RFC 7636 - Proof Key for Code Exchange by OAuth Public Clients.
 *
 * PKCE adds security to OAuth 2.0 flows by requiring clients to prove
 * they are the same client that initiated the authorization request.
 */
@Service
public class PkceService {

    private static final Logger LOG = LoggerFactory.getLogger(PkceService.class);
    private static final String CODE_CHALLENGE_METHOD = "S256"; // SHA-256
    private static final int AUTHORIZATION_CODE_LENGTH = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Validates that the code verifier matches the code challenge.
     * Uses SHA-256 hashing to compute the challenge from the verifier.
     *
     * @param codeVerifier The code verifier provided by the client
     * @param codeChallenge The code challenge stored during authorization
     * @return true if the verifier is valid, false otherwise
     * @throws NoSuchAlgorithmException if SHA-256 algorithm is not available
     */
    public boolean validateCodeVerifier(String codeVerifier, String codeChallenge) throws NoSuchAlgorithmException {
        if (codeVerifier == null || codeVerifier.isEmpty()) {
            LOG.warn("Code verifier is null or empty");
            return false;
        }

        if (codeChallenge == null || codeChallenge.isEmpty()) {
            LOG.warn("Code challenge is null or empty");
            return false;
        }

        try {
            // Compute the code challenge from the verifier
            String computedChallenge = generateCodeChallenge(codeVerifier);

            // Compare computed challenge with stored challenge
            boolean isValid = computedChallenge.equals(codeChallenge);

            if (isValid) {
                LOG.debug("Code verifier validation successful");
            } else {
                LOG.warn("Code verifier validation failed: computed challenge does not match");
            }

            return isValid;
        } catch (Exception e) {
            LOG.error("Error validating code verifier: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Generates a code challenge from a code verifier using SHA-256.
     * This is used internally for validation.
     *
     * @param codeVerifier The code verifier to hash
     * @return Base64-URL-encoded SHA-256 hash of the verifier
     * @throws NoSuchAlgorithmException if SHA-256 algorithm is not available
     */
    private String generateCodeChallenge(String codeVerifier) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    /**
     * Generates a cryptographically secure random authorization code.
     * This code is temporary and should be exchanged for JWT tokens within minutes.
     *
     * @return A random 32-character authorization code
     */
    public String generateAuthorizationCode() {
        byte[] randomBytes = new byte[AUTHORIZATION_CODE_LENGTH];
        SECURE_RANDOM.nextBytes(randomBytes);
        String authCode = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        LOG.debug("Generated authorization code (length: {})", authCode.length());
        return authCode;
    }

    /**
     * Gets the supported code challenge method.
     *
     * @return "S256" (SHA-256)
     */
    public String getCodeChallengeMethod() {
        return CODE_CHALLENGE_METHOD;
    }
}
