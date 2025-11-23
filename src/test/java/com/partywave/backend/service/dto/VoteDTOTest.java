package com.partywave.backend.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.partywave.backend.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VoteDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(VoteDTO.class);
        VoteDTO voteDTO1 = new VoteDTO();
        voteDTO1.setId(UUID.randomUUID());
        VoteDTO voteDTO2 = new VoteDTO();
        assertThat(voteDTO1).isNotEqualTo(voteDTO2);
        voteDTO2.setId(voteDTO1.getId());
        assertThat(voteDTO1).isEqualTo(voteDTO2);
        voteDTO2.setId(UUID.randomUUID());
        assertThat(voteDTO1).isNotEqualTo(voteDTO2);
        voteDTO1.setId(null);
        assertThat(voteDTO1).isNotEqualTo(voteDTO2);
    }
}
