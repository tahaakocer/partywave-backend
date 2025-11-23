package com.partywave.backend.exception;

import java.util.UUID;

/**
 * Exception thrown when a user attempts to access a private room without proper authorization.
 */
public class UnauthorizedRoomAccessException extends RuntimeException {

    private final UUID roomId;
    private final UUID userId;

    public UnauthorizedRoomAccessException(UUID roomId, UUID userId) {
        super(
            String.format(
                "User %s is not authorized to access private room %s. " +
                "Access requires either explicit room access grant or a valid invitation token.",
                userId,
                roomId
            )
        );
        this.roomId = roomId;
        this.userId = userId;
    }

    public UUID getRoomId() {
        return roomId;
    }

    public UUID getUserId() {
        return userId;
    }
}
