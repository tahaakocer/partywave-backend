package com.partywave.backend.exception;

/**
 * Exception thrown when PKCE (Proof Key for Code Exchange) validation fails.
 * This occurs when the code verifier does not match the code challenge,
 * or when required PKCE parameters are missing or invalid.
 */
public class InvalidPkceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String errorCode;

    public InvalidPkceException(String message) {
        super(message);
        this.errorCode = "INVALID_PKCE";
    }

    public InvalidPkceException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public InvalidPkceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "INVALID_PKCE";
    }

    public InvalidPkceException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
