package com.partywave.backend.exception;

/**
 * Exception thrown when an authorization code is invalid, expired, or not found.
 * Authorization codes are temporary and should be exchanged for JWT tokens promptly.
 */
public class InvalidAuthorizationCodeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String errorCode;

    public InvalidAuthorizationCodeException(String message) {
        super(message);
        this.errorCode = "INVALID_AUTHORIZATION_CODE";
    }

    public InvalidAuthorizationCodeException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public InvalidAuthorizationCodeException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "INVALID_AUTHORIZATION_CODE";
    }

    public InvalidAuthorizationCodeException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
