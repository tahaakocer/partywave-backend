package com.partywave.backend.exception;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when attempting to join a private room without proper authorization.
 */
@ResponseStatus(value = HttpStatus.FORBIDDEN)
public class RoomNotPublicException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final UUID roomId;

    public RoomNotPublicException(UUID roomId) {
        super("Room is not public. Private room access requires invitation or access grant.");
        this.roomId = roomId;
    }

    public UUID getRoomId() {
        return roomId;
    }
}
