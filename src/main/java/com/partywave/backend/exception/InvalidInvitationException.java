package com.partywave.backend.exception;

/**
 * Exception thrown when an invitation token is invalid, expired, or exhausted.
 */
public class InvalidInvitationException extends RuntimeException {

    private final String token;
    private final String reason;

    public InvalidInvitationException(String token, String reason) {
        super(String.format("Invalid invitation token: %s. Reason: %s", token, reason));
        this.token = token;
        this.reason = reason;
    }

    public InvalidInvitationException(String reason) {
        super(String.format("Invalid invitation: %s", reason));
        this.token = null;
        this.reason = reason;
    }

    public String getToken() {
        return token;
    }

    public String getReason() {
        return reason;
    }
}
