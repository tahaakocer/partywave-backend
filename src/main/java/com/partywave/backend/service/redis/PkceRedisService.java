package com.partywave.backend.service.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.partywave.backend.config.CacheConfiguration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis service for managing PKCE (Proof Key for Code Exchange) data.
 * Stores code challenges and authorization codes with TTL for security.
 *
 * Key structures:
 * - Code challenge: partywave:pkce:state:{state} → code_challenge (TTL: 10 minutes)
 * - Authorization code: partywave:pkce:authcode:{authCode} → JSON {userId, codeChallenge} (TTL: 5 minutes)
 */
@Service
public class PkceRedisService {

    private static final Logger LOG = LoggerFactory.getLogger(PkceRedisService.class);

    private static final long CODE_CHALLENGE_TTL_MINUTES = 10;
    private static final long AUTHORIZATION_CODE_TTL_MINUTES = 5;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public PkceRedisService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    // ========================================
    // Key Building Methods
    // ========================================

    private String buildCodeChallengeKey(String state) {
        return CacheConfiguration.KEY_PREFIX + "pkce:state:" + state;
    }

    private String buildAuthorizationCodeKey(String authCode) {
        return CacheConfiguration.KEY_PREFIX + "pkce:authcode:" + authCode;
    }

    // ========================================
    // Code Challenge Storage Methods
    // ========================================

    /**
     * Stores a code challenge for a given OAuth state parameter.
     * The challenge will expire after 10 minutes.
     *
     * @param state The OAuth state parameter
     * @param codeChallenge The PKCE code challenge
     */
    public void storeChallengeForState(String state, String codeChallenge) {
        String key = buildCodeChallengeKey(state);
        try {
            redisTemplate.opsForValue().set(key, codeChallenge, CODE_CHALLENGE_TTL_MINUTES, TimeUnit.MINUTES);
            LOG.debug("Stored code challenge for state: {} (TTL: {} minutes)", state, CODE_CHALLENGE_TTL_MINUTES);
        } catch (Exception e) {
            LOG.error("Failed to store code challenge for state {}: {}", state, e.getMessage(), e);
            throw new RuntimeException("Failed to store code challenge", e);
        }
    }

    /**
     * Retrieves the code challenge for a given OAuth state parameter.
     *
     * @param state The OAuth state parameter
     * @return The code challenge, or null if not found or expired
     */
    public String getChallengeByState(String state) {
        String key = buildCodeChallengeKey(state);
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                LOG.debug("Retrieved code challenge for state: {}", state);
                return value.toString();
            } else {
                LOG.debug("No code challenge found for state: {}", state);
                return null;
            }
        } catch (Exception e) {
            LOG.error("Failed to retrieve code challenge for state {}: {}", state, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Deletes the code challenge for a given OAuth state parameter.
     *
     * @param state The OAuth state parameter
     */
    public void deleteChallengeForState(String state) {
        String key = buildCodeChallengeKey(state);
        try {
            redisTemplate.delete(key);
            LOG.debug("Deleted code challenge for state: {}", state);
        } catch (Exception e) {
            LOG.warn("Failed to delete code challenge for state {}: {}", state, e.getMessage());
        }
    }

    // ========================================
    // Authorization Code Storage Methods
    // ========================================

    /**
     * Stores an authorization code with associated user ID and code challenge.
     * The authorization code will expire after 5 minutes.
     *
     * @param authorizationCode The temporary authorization code
     * @param userId The user's UUID
     * @param codeChallenge The PKCE code challenge
     */
    public void storeAuthorizationCode(String authorizationCode, UUID userId, String codeChallenge) {
        String key = buildAuthorizationCodeKey(authorizationCode);
        try {
            AuthorizationCodeData data = new AuthorizationCodeData(userId.toString(), codeChallenge);

            // Store the object directly - RedisTemplate's GenericJackson2JsonRedisSerializer will handle serialization
            redisTemplate.opsForValue().set(key, data, AUTHORIZATION_CODE_TTL_MINUTES, TimeUnit.MINUTES);
            LOG.debug("Stored authorization code for user: {} (TTL: {} minutes)", userId, AUTHORIZATION_CODE_TTL_MINUTES);
        } catch (Exception e) {
            LOG.error("Failed to store authorization code: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to store authorization code", e);
        }
    }

    /**
     * Retrieves the authorization code data (user ID and code challenge).
     *
     * @param authorizationCode The authorization code
     * @return The authorization code data, or null if not found or expired
     */
    public AuthorizationCodeData getAuthorizationCodeData(String authorizationCode) {
        String key = buildAuthorizationCodeKey(authorizationCode);
        Object value = null;
        try {
            value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                // RedisTemplate's GenericJackson2JsonRedisSerializer will deserialize automatically
                if (value instanceof AuthorizationCodeData) {
                    AuthorizationCodeData data = (AuthorizationCodeData) value;
                    LOG.debug("Retrieved authorization code data for user: {}", data.getUserId());
                    return data;
                } else {
                    // Fallback: try to deserialize from string (for backward compatibility)
                    AuthorizationCodeData data = objectMapper.readValue(value.toString(), AuthorizationCodeData.class);
                    LOG.debug("Retrieved authorization code data for user: {} (from string)", data.getUserId());
                    return data;
                }
            } else {
                LOG.debug("No authorization code data found for key: {}", key);
                return null;
            }
        } catch (Exception e) {
            LOG.error("Failed to retrieve authorization code data: {}", e.getMessage(), e);
            LOG.error("Value type: {}, Value: {}", value != null ? value.getClass().getName() : "null", value);
            return null;
        }
    }

    /**
     * Deletes an authorization code.
     * Should be called after successful token exchange to prevent reuse.
     *
     * @param authorizationCode The authorization code to delete
     */
    public void deleteAuthorizationCode(String authorizationCode) {
        String key = buildAuthorizationCodeKey(authorizationCode);
        try {
            redisTemplate.delete(key);
            LOG.debug("Deleted authorization code");
        } catch (Exception e) {
            LOG.warn("Failed to delete authorization code: {}", e.getMessage());
        }
    }

    // ========================================
    // Data Classes
    // ========================================

    /**
     * Data class for storing authorization code information in Redis.
     */
    public static class AuthorizationCodeData {

        private String userId;
        private String codeChallenge;

        public AuthorizationCodeData() {}

        public AuthorizationCodeData(String userId, String codeChallenge) {
            this.userId = userId;
            this.codeChallenge = codeChallenge;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getCodeChallenge() {
            return codeChallenge;
        }

        public void setCodeChallenge(String codeChallenge) {
            this.codeChallenge = codeChallenge;
        }
    }
}
