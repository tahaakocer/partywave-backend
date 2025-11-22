package com.partywave.backend.domain;

import static com.partywave.backend.domain.AppUserTestSamples.*;
import static com.partywave.backend.domain.RefreshTokenTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.partywave.backend.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(RefreshToken.class);
        RefreshToken refreshToken1 = getRefreshTokenSample1();
        RefreshToken refreshToken2 = new RefreshToken();
        assertThat(refreshToken1).isNotEqualTo(refreshToken2);

        refreshToken2.setId(refreshToken1.getId());
        assertThat(refreshToken1).isEqualTo(refreshToken2);

        refreshToken2 = getRefreshTokenSample2();
        assertThat(refreshToken1).isNotEqualTo(refreshToken2);
    }

    @Test
    void appUserTest() {
        RefreshToken refreshToken = getRefreshTokenRandomSampleGenerator();
        AppUser appUserBack = getAppUserRandomSampleGenerator();

        refreshToken.setAppUser(appUserBack);
        assertThat(refreshToken.getAppUser()).isEqualTo(appUserBack);

        refreshToken.appUser(null);
        assertThat(refreshToken.getAppUser()).isNull();
    }
}
