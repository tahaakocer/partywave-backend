package com.partywave.backend.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.partywave.backend.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class AppUserStatsDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(AppUserStatsDTO.class);
        AppUserStatsDTO appUserStatsDTO1 = new AppUserStatsDTO();
        appUserStatsDTO1.setId(1L);
        AppUserStatsDTO appUserStatsDTO2 = new AppUserStatsDTO();
        assertThat(appUserStatsDTO1).isNotEqualTo(appUserStatsDTO2);
        appUserStatsDTO2.setId(appUserStatsDTO1.getId());
        assertThat(appUserStatsDTO1).isEqualTo(appUserStatsDTO2);
        appUserStatsDTO2.setId(2L);
        assertThat(appUserStatsDTO1).isNotEqualTo(appUserStatsDTO2);
        appUserStatsDTO1.setId(null);
        assertThat(appUserStatsDTO1).isNotEqualTo(appUserStatsDTO2);
    }
}
