package com.partywave.backend.domain;

import static com.partywave.backend.domain.AppUserTestSamples.*;
import static com.partywave.backend.domain.RoomMemberTestSamples.*;
import static com.partywave.backend.domain.RoomTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.partywave.backend.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class RoomMemberTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(RoomMember.class);
        RoomMember roomMember1 = getRoomMemberSample1();
        RoomMember roomMember2 = new RoomMember();
        assertThat(roomMember1).isNotEqualTo(roomMember2);

        roomMember2.setId(roomMember1.getId());
        assertThat(roomMember1).isEqualTo(roomMember2);

        roomMember2 = getRoomMemberSample2();
        assertThat(roomMember1).isNotEqualTo(roomMember2);
    }

    @Test
    void roomTest() {
        RoomMember roomMember = getRoomMemberRandomSampleGenerator();
        Room roomBack = getRoomRandomSampleGenerator();

        roomMember.setRoom(roomBack);
        assertThat(roomMember.getRoom()).isEqualTo(roomBack);

        roomMember.room(null);
        assertThat(roomMember.getRoom()).isNull();
    }

    @Test
    void appUserTest() {
        RoomMember roomMember = getRoomMemberRandomSampleGenerator();
        AppUser appUserBack = getAppUserRandomSampleGenerator();

        roomMember.setAppUser(appUserBack);
        assertThat(roomMember.getAppUser()).isEqualTo(appUserBack);

        roomMember.appUser(null);
        assertThat(roomMember.getAppUser()).isNull();
    }
}
