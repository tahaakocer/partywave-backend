package com.partywave.backend.domain;

import static com.partywave.backend.domain.AppUserTestSamples.*;
import static com.partywave.backend.domain.RoomAccessTestSamples.*;
import static com.partywave.backend.domain.RoomTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.partywave.backend.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class RoomAccessTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(RoomAccess.class);
        RoomAccess roomAccess1 = getRoomAccessSample1();
        RoomAccess roomAccess2 = new RoomAccess();
        assertThat(roomAccess1).isNotEqualTo(roomAccess2);

        roomAccess2.setId(roomAccess1.getId());
        assertThat(roomAccess1).isEqualTo(roomAccess2);

        roomAccess2 = getRoomAccessSample2();
        assertThat(roomAccess1).isNotEqualTo(roomAccess2);
    }

    @Test
    void roomTest() {
        RoomAccess roomAccess = getRoomAccessRandomSampleGenerator();
        Room roomBack = getRoomRandomSampleGenerator();

        roomAccess.setRoom(roomBack);
        assertThat(roomAccess.getRoom()).isEqualTo(roomBack);

        roomAccess.room(null);
        assertThat(roomAccess.getRoom()).isNull();
    }

    @Test
    void appUserTest() {
        RoomAccess roomAccess = getRoomAccessRandomSampleGenerator();
        AppUser appUserBack = getAppUserRandomSampleGenerator();

        roomAccess.setAppUser(appUserBack);
        assertThat(roomAccess.getAppUser()).isEqualTo(appUserBack);

        roomAccess.appUser(null);
        assertThat(roomAccess.getAppUser()).isNull();
    }

    @Test
    void grantedByTest() {
        RoomAccess roomAccess = getRoomAccessRandomSampleGenerator();
        AppUser appUserBack = getAppUserRandomSampleGenerator();

        roomAccess.setGrantedBy(appUserBack);
        assertThat(roomAccess.getGrantedBy()).isEqualTo(appUserBack);

        roomAccess.grantedBy(null);
        assertThat(roomAccess.getGrantedBy()).isNull();
    }
}
