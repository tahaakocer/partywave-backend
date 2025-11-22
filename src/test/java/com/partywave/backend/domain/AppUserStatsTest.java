package com.partywave.backend.domain;

import static com.partywave.backend.domain.AppUserStatsTestSamples.*;
import static com.partywave.backend.domain.AppUserTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.partywave.backend.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class AppUserStatsTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(AppUserStats.class);
        AppUserStats appUserStats1 = getAppUserStatsSample1();
        AppUserStats appUserStats2 = new AppUserStats();
        assertThat(appUserStats1).isNotEqualTo(appUserStats2);

        appUserStats2.setId(appUserStats1.getId());
        assertThat(appUserStats1).isEqualTo(appUserStats2);

        appUserStats2 = getAppUserStatsSample2();
        assertThat(appUserStats1).isNotEqualTo(appUserStats2);
    }

    @Test
    void appUserTest() {
        AppUserStats appUserStats = getAppUserStatsRandomSampleGenerator();
        AppUser appUserBack = getAppUserRandomSampleGenerator();

        appUserStats.setAppUser(appUserBack);
        assertThat(appUserStats.getAppUser()).isEqualTo(appUserBack);
        assertThat(appUserBack.getAppUserStats()).isEqualTo(appUserStats);

        appUserStats.appUser(null);
        assertThat(appUserStats.getAppUser()).isNull();
        assertThat(appUserBack.getAppUserStats()).isNull();
    }
}
