package com.partywave.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when token encryption or decryption fails.
 */
@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class TokenEncryptionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TokenEncryptionException(String message) {
        super(message);
    }

    public TokenEncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
