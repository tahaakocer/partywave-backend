package com.partywave.backend.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.partywave.backend.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RefreshTokenDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(RefreshTokenDTO.class);
        RefreshTokenDTO refreshTokenDTO1 = new RefreshTokenDTO();
        refreshTokenDTO1.setId(UUID.randomUUID());
        RefreshTokenDTO refreshTokenDTO2 = new RefreshTokenDTO();
        assertThat(refreshTokenDTO1).isNotEqualTo(refreshTokenDTO2);
        refreshTokenDTO2.setId(refreshTokenDTO1.getId());
        assertThat(refreshTokenDTO1).isEqualTo(refreshTokenDTO2);
        refreshTokenDTO2.setId(UUID.randomUUID());
        assertThat(refreshTokenDTO1).isNotEqualTo(refreshTokenDTO2);
        refreshTokenDTO1.setId(null);
        assertThat(refreshTokenDTO1).isNotEqualTo(refreshTokenDTO2);
    }
}
