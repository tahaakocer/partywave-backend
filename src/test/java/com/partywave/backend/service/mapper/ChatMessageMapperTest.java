package com.partywave.backend.service.mapper;

import static com.partywave.backend.domain.ChatMessageAsserts.*;
import static com.partywave.backend.domain.ChatMessageTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChatMessageMapperTest {

    private ChatMessageMapper chatMessageMapper;

    @BeforeEach
    void setUp() {
        chatMessageMapper = new ChatMessageMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getChatMessageSample1();
        var actual = chatMessageMapper.toEntity(chatMessageMapper.toDto(expected));
        assertChatMessageAllPropertiesEquals(expected, actual);
    }
}
