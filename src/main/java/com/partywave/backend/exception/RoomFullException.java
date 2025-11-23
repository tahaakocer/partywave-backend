package com.partywave.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when attempting to join a room that has reached its maximum capacity.
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class RoomFullException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int currentMemberCount;
    private final int maxParticipants;

    public RoomFullException(int currentMemberCount, int maxParticipants) {
        super(String.format("Room is full. Current members: %d, Maximum capacity: %d", currentMemberCount, maxParticipants));
        this.currentMemberCount = currentMemberCount;
        this.maxParticipants = maxParticipants;
    }

    public int getCurrentMemberCount() {
        return currentMemberCount;
    }

    public int getMaxParticipants() {
        return maxParticipants;
    }
}
