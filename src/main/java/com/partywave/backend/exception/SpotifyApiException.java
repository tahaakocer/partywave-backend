package com.partywave.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when Spotify API operations fail.
 */
@ResponseStatus(value = HttpStatus.BAD_GATEWAY)
public class SpotifyApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String operation;

    public SpotifyApiException(String message, String operation) {
        super(message);
        this.operation = operation;
    }

    public SpotifyApiException(String message, String operation, Throwable cause) {
        super(message, cause);
        this.operation = operation;
    }

    public String getOperation() {
        return operation;
    }
}
