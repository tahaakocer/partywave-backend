package com.partywave.backend.domain;

import static com.partywave.backend.domain.AppUserTestSamples.*;
import static com.partywave.backend.domain.ChatMessageTestSamples.*;
import static com.partywave.backend.domain.RoomTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.partywave.backend.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ChatMessageTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ChatMessage.class);
        ChatMessage chatMessage1 = getChatMessageSample1();
        ChatMessage chatMessage2 = new ChatMessage();
        assertThat(chatMessage1).isNotEqualTo(chatMessage2);

        chatMessage2.setId(chatMessage1.getId());
        assertThat(chatMessage1).isEqualTo(chatMessage2);

        chatMessage2 = getChatMessageSample2();
        assertThat(chatMessage1).isNotEqualTo(chatMessage2);
    }

    @Test
    void roomTest() {
        ChatMessage chatMessage = getChatMessageRandomSampleGenerator();
        Room roomBack = getRoomRandomSampleGenerator();

        chatMessage.setRoom(roomBack);
        assertThat(chatMessage.getRoom()).isEqualTo(roomBack);

        chatMessage.room(null);
        assertThat(chatMessage.getRoom()).isNull();
    }

    @Test
    void senderTest() {
        ChatMessage chatMessage = getChatMessageRandomSampleGenerator();
        AppUser appUserBack = getAppUserRandomSampleGenerator();

        chatMessage.setSender(appUserBack);
        assertThat(chatMessage.getSender()).isEqualTo(appUserBack);

        chatMessage.sender(null);
        assertThat(chatMessage.getSender()).isNull();
    }
}
