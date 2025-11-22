package com.partywave.backend.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.partywave.backend.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class UserTokenDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(UserTokenDTO.class);
        UserTokenDTO userTokenDTO1 = new UserTokenDTO();
        userTokenDTO1.setId(1L);
        UserTokenDTO userTokenDTO2 = new UserTokenDTO();
        assertThat(userTokenDTO1).isNotEqualTo(userTokenDTO2);
        userTokenDTO2.setId(userTokenDTO1.getId());
        assertThat(userTokenDTO1).isEqualTo(userTokenDTO2);
        userTokenDTO2.setId(2L);
        assertThat(userTokenDTO1).isNotEqualTo(userTokenDTO2);
        userTokenDTO1.setId(null);
        assertThat(userTokenDTO1).isNotEqualTo(userTokenDTO2);
    }
}
