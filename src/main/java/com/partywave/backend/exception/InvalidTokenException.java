package com.partywave.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a JWT token is invalid, expired, or malformed.
 */
@ResponseStatus(value = HttpStatus.UNAUTHORIZED)
public class InvalidTokenException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String tokenType;

    public InvalidTokenException(String message, String tokenType) {
        super(message);
        this.tokenType = tokenType;
    }

    public InvalidTokenException(String message, String tokenType, Throwable cause) {
        super(message, cause);
        this.tokenType = tokenType;
    }

    public String getTokenType() {
        return tokenType;
    }
}
