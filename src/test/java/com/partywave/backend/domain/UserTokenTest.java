package com.partywave.backend.domain;

import static com.partywave.backend.domain.AppUserTestSamples.*;
import static com.partywave.backend.domain.UserTokenTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.partywave.backend.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class UserTokenTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(UserToken.class);
        UserToken userToken1 = getUserTokenSample1();
        UserToken userToken2 = new UserToken();
        assertThat(userToken1).isNotEqualTo(userToken2);

        userToken2.setId(userToken1.getId());
        assertThat(userToken1).isEqualTo(userToken2);

        userToken2 = getUserTokenSample2();
        assertThat(userToken1).isNotEqualTo(userToken2);
    }

    @Test
    void appUserTest() {
        UserToken userToken = getUserTokenRandomSampleGenerator();
        AppUser appUserBack = getAppUserRandomSampleGenerator();

        userToken.setAppUser(appUserBack);
        assertThat(userToken.getAppUser()).isEqualTo(appUserBack);

        userToken.appUser(null);
        assertThat(userToken.getAppUser()).isNull();
    }
}
