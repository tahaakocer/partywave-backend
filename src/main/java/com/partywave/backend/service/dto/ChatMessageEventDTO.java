package com.partywave.backend.service.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for CHAT_MESSAGE WebSocket event.
 * Sent to all room members when a new chat message is sent.
 *
 * Based on PROJECT_OVERVIEW.md section 2.12 and 3.3:
 * - CHAT_MESSAGE: New chat message sent
 */
public class ChatMessageEventDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String type = "CHAT_MESSAGE";
    private String roomId;
    private UUID messageId;
    private UUID senderId;
    private String senderDisplayName;
    private String content;
    private Instant sentAt;

    public ChatMessageEventDTO() {}

    public ChatMessageEventDTO(String roomId, UUID messageId, UUID senderId, String senderDisplayName, String content, Instant sentAt) {
        this.roomId = roomId;
        this.messageId = messageId;
        this.senderId = senderId;
        this.senderDisplayName = senderDisplayName;
        this.content = content;
        this.sentAt = sentAt;
    }

    // Getters and Setters

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public void setMessageId(UUID messageId) {
        this.messageId = messageId;
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

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    @Override
    public String toString() {
        return (
            "ChatMessageEventDTO{" +
            "type='" +
            type +
            '\'' +
            ", roomId='" +
            roomId +
            '\'' +
            ", messageId=" +
            messageId +
            ", senderId=" +
            senderId +
            ", senderDisplayName='" +
            senderDisplayName +
            '\'' +
            ", content='" +
            (content != null ? content.substring(0, Math.min(50, content.length())) + "..." : "null") +
            '\'' +
            ", sentAt=" +
            sentAt +
            '}'
        );
    }
}
