package com.partywave.backend.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.partywave.backend.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class AppUserImageDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(AppUserImageDTO.class);
        AppUserImageDTO appUserImageDTO1 = new AppUserImageDTO();
        appUserImageDTO1.setId(1L);
        AppUserImageDTO appUserImageDTO2 = new AppUserImageDTO();
        assertThat(appUserImageDTO1).isNotEqualTo(appUserImageDTO2);
        appUserImageDTO2.setId(appUserImageDTO1.getId());
        assertThat(appUserImageDTO1).isEqualTo(appUserImageDTO2);
        appUserImageDTO2.setId(2L);
        assertThat(appUserImageDTO1).isNotEqualTo(appUserImageDTO2);
        appUserImageDTO1.setId(null);
        assertThat(appUserImageDTO1).isNotEqualTo(appUserImageDTO2);
    }
}
