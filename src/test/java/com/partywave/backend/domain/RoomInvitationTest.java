package com.partywave.backend.domain;

import static com.partywave.backend.domain.AppUserTestSamples.*;
import static com.partywave.backend.domain.RoomInvitationTestSamples.*;
import static com.partywave.backend.domain.RoomTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.partywave.backend.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class RoomInvitationTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(RoomInvitation.class);
        RoomInvitation roomInvitation1 = getRoomInvitationSample1();
        RoomInvitation roomInvitation2 = new RoomInvitation();
        assertThat(roomInvitation1).isNotEqualTo(roomInvitation2);

        roomInvitation2.setId(roomInvitation1.getId());
        assertThat(roomInvitation1).isEqualTo(roomInvitation2);

        roomInvitation2 = getRoomInvitationSample2();
        assertThat(roomInvitation1).isNotEqualTo(roomInvitation2);
    }

    @Test
    void roomTest() {
        RoomInvitation roomInvitation = getRoomInvitationRandomSampleGenerator();
        Room roomBack = getRoomRandomSampleGenerator();

        roomInvitation.setRoom(roomBack);
        assertThat(roomInvitation.getRoom()).isEqualTo(roomBack);

        roomInvitation.room(null);
        assertThat(roomInvitation.getRoom()).isNull();
    }

    @Test
    void createdByTest() {
        RoomInvitation roomInvitation = getRoomInvitationRandomSampleGenerator();
        AppUser appUserBack = getAppUserRandomSampleGenerator();

        roomInvitation.setCreatedBy(appUserBack);
        assertThat(roomInvitation.getCreatedBy()).isEqualTo(appUserBack);

        roomInvitation.createdBy(null);
        assertThat(roomInvitation.getCreatedBy()).isNull();
    }
}
