package com.partywave.backend.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.partywave.backend.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RoomInvitationDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(RoomInvitationDTO.class);
        RoomInvitationDTO roomInvitationDTO1 = new RoomInvitationDTO();
        roomInvitationDTO1.setId(UUID.randomUUID());
        RoomInvitationDTO roomInvitationDTO2 = new RoomInvitationDTO();
        assertThat(roomInvitationDTO1).isNotEqualTo(roomInvitationDTO2);
        roomInvitationDTO2.setId(roomInvitationDTO1.getId());
        assertThat(roomInvitationDTO1).isEqualTo(roomInvitationDTO2);
        roomInvitationDTO2.setId(UUID.randomUUID());
        assertThat(roomInvitationDTO1).isNotEqualTo(roomInvitationDTO2);
        roomInvitationDTO1.setId(null);
        assertThat(roomInvitationDTO1).isNotEqualTo(roomInvitationDTO2);
    }
}
