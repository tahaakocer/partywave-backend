package com.partywave.backend.exception;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a user attempts to join a room they are already a member of.
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class AlreadyMemberException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final UUID userId;
    private final UUID roomId;

    public AlreadyMemberException(UUID userId, UUID roomId) {
        super(String.format("User is already a member of this room"));
        this.userId = userId;
        this.roomId = roomId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getRoomId() {
        return roomId;
    }
}
