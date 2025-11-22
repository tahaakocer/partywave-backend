package com.partywave.backend.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.partywave.backend.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class RoomMemberDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(RoomMemberDTO.class);
        RoomMemberDTO roomMemberDTO1 = new RoomMemberDTO();
        roomMemberDTO1.setId(1L);
        RoomMemberDTO roomMemberDTO2 = new RoomMemberDTO();
        assertThat(roomMemberDTO1).isNotEqualTo(roomMemberDTO2);
        roomMemberDTO2.setId(roomMemberDTO1.getId());
        assertThat(roomMemberDTO1).isEqualTo(roomMemberDTO2);
        roomMemberDTO2.setId(2L);
        assertThat(roomMemberDTO1).isNotEqualTo(roomMemberDTO2);
        roomMemberDTO1.setId(null);
        assertThat(roomMemberDTO1).isNotEqualTo(roomMemberDTO2);
    }
}
