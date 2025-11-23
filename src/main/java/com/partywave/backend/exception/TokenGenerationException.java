package com.partywave.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when JWT token generation fails.
 */
@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class TokenGenerationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TokenGenerationException(String message) {
        super(message);
    }

    public TokenGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
