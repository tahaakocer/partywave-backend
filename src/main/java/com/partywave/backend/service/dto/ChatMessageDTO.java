package com.partywave.backend.service.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing a chat message in a room.
 * Based on PROJECT_OVERVIEW.md section 2.3 - Room State Response.
 */
public class ChatMessageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;
    private UUID roomId;
    private UUID senderId;
    private String senderDisplayName;
    private String content;
    private Instant timestamp;

    // Constructors
    public ChatMessageDTO() {}

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRoomId() {
        return roomId;
    }

    public void setRoomId(UUID roomId) {
        this.roomId = roomId;
    }

    public UUID getSenderId() {
        return senderId;
    }

    public void setSenderId(UUID senderId) {
        this.senderId = senderId;
    }

    public String getSenderDisplayName() {
        return senderDisplayName;
    }

    public void setSenderDisplayName(String senderDisplayName) {
        this.senderDisplayName = senderDisplayName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return (
            "ChatMessageDTO{" +
            "id=" +
            id +
            ", roomId=" +
            roomId +
            ", senderId=" +
            senderId +
            ", senderDisplayName='" +
            senderDisplayName +
            '\'' +
            ", timestamp=" +
            timestamp +
            '}'
        );
    }
}
