package com.partywave.backend.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.partywave.backend.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RoomAccessDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(RoomAccessDTO.class);
        RoomAccessDTO roomAccessDTO1 = new RoomAccessDTO();
        roomAccessDTO1.setId(UUID.randomUUID());
        RoomAccessDTO roomAccessDTO2 = new RoomAccessDTO();
        assertThat(roomAccessDTO1).isNotEqualTo(roomAccessDTO2);
        roomAccessDTO2.setId(roomAccessDTO1.getId());
        assertThat(roomAccessDTO1).isEqualTo(roomAccessDTO2);
        roomAccessDTO2.setId(UUID.randomUUID());
        assertThat(roomAccessDTO1).isNotEqualTo(roomAccessDTO2);
        roomAccessDTO1.setId(null);
        assertThat(roomAccessDTO1).isNotEqualTo(roomAccessDTO2);
    }
}
