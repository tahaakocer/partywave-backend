package com.partywave.backend.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * Request DTO for sending a chat message in a room.
 * Based on PROJECT_OVERVIEW.md section 2.12 - Chat Messaging.
 */
public class SendChatMessageRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Message content cannot be empty")
    @Size(max = 1000, message = "Message content cannot exceed 1000 characters")
    private String content;

    public SendChatMessageRequestDTO() {}

    public SendChatMessageRequestDTO(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return (
            "SendChatMessageRequestDTO{" +
            "content='" +
            (content != null ? content.substring(0, Math.min(50, content.length())) + "..." : "null") +
            '\'' +
            '}'
        );
    }
}
