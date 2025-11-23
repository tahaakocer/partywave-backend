package com.partywave.backend.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.partywave.backend.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChatMessageDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ChatMessageDTO.class);
        ChatMessageDTO chatMessageDTO1 = new ChatMessageDTO();
        chatMessageDTO1.setId(UUID.randomUUID());
        ChatMessageDTO chatMessageDTO2 = new ChatMessageDTO();
        assertThat(chatMessageDTO1).isNotEqualTo(chatMessageDTO2);
        chatMessageDTO2.setId(chatMessageDTO1.getId());
        assertThat(chatMessageDTO1).isEqualTo(chatMessageDTO2);
        chatMessageDTO2.setId(UUID.randomUUID());
        assertThat(chatMessageDTO1).isNotEqualTo(chatMessageDTO2);
        chatMessageDTO1.setId(null);
        assertThat(chatMessageDTO1).isNotEqualTo(chatMessageDTO2);
    }
}
