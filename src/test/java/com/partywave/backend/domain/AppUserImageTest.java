package com.partywave.backend.domain;

import static com.partywave.backend.domain.AppUserImageTestSamples.*;
import static com.partywave.backend.domain.AppUserTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.partywave.backend.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class AppUserImageTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(AppUserImage.class);
        AppUserImage appUserImage1 = getAppUserImageSample1();
        AppUserImage appUserImage2 = new AppUserImage();
        assertThat(appUserImage1).isNotEqualTo(appUserImage2);

        appUserImage2.setId(appUserImage1.getId());
        assertThat(appUserImage1).isEqualTo(appUserImage2);

        appUserImage2 = getAppUserImageSample2();
        assertThat(appUserImage1).isNotEqualTo(appUserImage2);
    }

    @Test
    void appUserTest() {
        AppUserImage appUserImage = getAppUserImageRandomSampleGenerator();
        AppUser appUserBack = getAppUserRandomSampleGenerator();

        appUserImage.setAppUser(appUserBack);
        assertThat(appUserImage.getAppUser()).isEqualTo(appUserBack);

        appUserImage.appUser(null);
        assertThat(appUserImage.getAppUser()).isNull();
    }
}
