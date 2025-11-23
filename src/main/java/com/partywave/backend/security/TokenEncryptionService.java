package com.partywave.backend.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for encrypting and decrypting Spotify OAuth tokens.
 * Uses AES-256-GCM encryption with the JWT secret as the encryption key.
 */
@Service
public class TokenEncryptionService {

    private static final Logger LOG = LoggerFactory.getLogger(TokenEncryptionService.class);
    private static final String ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    @Value("${jhipster.security.authentication.jwt.base64-secret}")
    private String jwtSecret;

    /**
     * Encrypts a token using AES-256-GCM.
     *
     * @param plainToken The token to encrypt
     * @return Base64-encoded encrypted token
     * @throws Exception if encryption fails
     */
    public String encrypt(String plainToken) {
        if (plainToken == null || plainToken.isEmpty()) {
            return plainToken;
        }

        try {
            // Generate a 256-bit key from the JWT secret
            SecretKeySpec keySpec = getSecretKey();

            // Generate a random IV (Initialization Vector)
            byte[] iv = new byte[GCM_IV_LENGTH];
            new java.security.SecureRandom().nextBytes(iv);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            // Encrypt the token
            byte[] encryptedBytes = cipher.doFinal(plainToken.getBytes(StandardCharsets.UTF_8));

            // Concatenate IV + encrypted data
            byte[] encryptedWithIv = new byte[GCM_IV_LENGTH + encryptedBytes.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedBytes, 0, encryptedWithIv, GCM_IV_LENGTH, encryptedBytes.length);

            // Return Base64-encoded result
            return Base64.getEncoder().encodeToString(encryptedWithIv);
        } catch (Exception e) {
            LOG.error("Failed to encrypt token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to encrypt token", e);
        }
    }

    /**
     * Decrypts a token using AES-256-GCM.
     *
     * @param encryptedToken Base64-encoded encrypted token
     * @return Decrypted plain token
     * @throws Exception if decryption fails
     */
    public String decrypt(String encryptedToken) {
        if (encryptedToken == null || encryptedToken.isEmpty()) {
            return encryptedToken;
        }

        try {
            // Decode Base64
            byte[] encryptedWithIv = Base64.getDecoder().decode(encryptedToken);

            // Extract IV and encrypted data
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encryptedBytes = new byte[encryptedWithIv.length - GCM_IV_LENGTH];
            System.arraycopy(encryptedWithIv, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedWithIv, GCM_IV_LENGTH, encryptedBytes, 0, encryptedBytes.length);

            // Generate the same key from JWT secret
            SecretKeySpec keySpec = getSecretKey();

            // Initialize cipher for decryption
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            // Decrypt
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOG.error("Failed to decrypt token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to decrypt token", e);
        }
    }

    /**
     * Generates a 256-bit AES key from the JWT secret using SHA-256.
     *
     * @return SecretKeySpec for AES encryption
     * @throws Exception if key generation fails
     */
    private SecretKeySpec getSecretKey() throws Exception {
        // Use SHA-256 to derive a 256-bit key from the JWT secret
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = digest.digest(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(keyBytes, "AES");
    }
}
