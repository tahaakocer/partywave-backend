package com.partywave.backend.config;

import static com.partywave.backend.security.SecurityUtils.JWT_ALGORITHM;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.util.Base64;
import com.partywave.backend.management.SecurityMetersService;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class SecurityJwtConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityJwtConfiguration.class);

    @Value("${jhipster.security.authentication.jwt.base64-secret}")
    private String jwtKey;

    @Bean
    public JwtDecoder jwtDecoder(SecurityMetersService metersService) {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(getSecretKey()).macAlgorithm(JWT_ALGORITHM).build();
        return token -> {
            try {
                return jwtDecoder.decode(token);
            } catch (Exception e) {
                if (e.getMessage().contains("Invalid signature")) {
                    metersService.trackTokenInvalidSignature();
                } else if (e.getMessage().contains("Jwt expired at")) {
                    metersService.trackTokenExpired();
                } else if (
                    e.getMessage().contains("Invalid JWT serialization") ||
                    e.getMessage().contains("Malformed token") ||
                    e.getMessage().contains("Invalid unsecured/JWS/JWE")
                ) {
                    metersService.trackTokenMalformed();
                } else {
                    LOG.error("Unknown JWT error {}", e.getMessage());
                }
                throw e;
            }
        };
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        SecretKey secretKey = getSecretKey();
        LOG.debug("Creating JwtEncoder with algorithm: {}", JWT_ALGORITHM.getName());
        LOG.debug("Secret key algorithm: {}", secretKey.getAlgorithm());
        LOG.debug("Secret key length: {} bytes", secretKey.getEncoded().length);

        try {
            // Use ImmutableSecret directly - this is the simplest and most reliable approach
            // for symmetric keys (HMAC). It automatically handles JWK creation and selection.
            ImmutableSecret<com.nimbusds.jose.proc.SecurityContext> jwkSource = new ImmutableSecret<>(secretKey.getEncoded());
            NimbusJwtEncoder encoder = new NimbusJwtEncoder(jwkSource);
            LOG.debug("JwtEncoder created successfully using ImmutableSecret");
            return encoder;
        } catch (Exception e) {
            LOG.error("Failed to create JwtEncoder: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to create JwtEncoder", e);
        }
    }

    private SecretKey getSecretKey() {
        if (jwtKey == null || jwtKey.trim().isEmpty()) {
            throw new IllegalStateException(
                "JWT secret key is not configured. Please set jhipster.security.authentication.jwt.base64-secret property."
            );
        }

        try {
            byte[] keyBytes = Base64.from(jwtKey).decode();
            if (keyBytes.length < 32) {
                throw new IllegalStateException(
                    "JWT secret key must be at least 256 bits (32 bytes) long. Current length: " + keyBytes.length * 8 + " bits."
                );
            }
            return new SecretKeySpec(keyBytes, 0, keyBytes.length, JWT_ALGORITHM.getName());
        } catch (Exception e) {
            LOG.error("Failed to decode JWT secret key: {}", e.getMessage());
            throw new IllegalStateException("Failed to decode JWT secret key. Please ensure it is a valid Base64-encoded string.", e);
        }
    }
}
