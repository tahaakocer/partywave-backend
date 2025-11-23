package com.partywave.backend.domain;

import static com.partywave.backend.domain.AppUserTestSamples.*;
import static com.partywave.backend.domain.RoomTestSamples.*;
import static com.partywave.backend.domain.VoteTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.partywave.backend.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class VoteTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Vote.class);
        Vote vote1 = getVoteSample1();
        Vote vote2 = new Vote();
        assertThat(vote1).isNotEqualTo(vote2);

        vote2.setId(vote1.getId());
        assertThat(vote1).isEqualTo(vote2);

        vote2 = getVoteSample2();
        assertThat(vote1).isNotEqualTo(vote2);
    }

    @Test
    void roomTest() {
        Vote vote = getVoteRandomSampleGenerator();
        Room roomBack = getRoomRandomSampleGenerator();

        vote.setRoom(roomBack);
        assertThat(vote.getRoom()).isEqualTo(roomBack);

        vote.room(null);
        assertThat(vote.getRoom()).isNull();
    }

    @Test
    void voterTest() {
        Vote vote = getVoteRandomSampleGenerator();
        AppUser appUserBack = getAppUserRandomSampleGenerator();

        vote.setVoter(appUserBack);
        assertThat(vote.getVoter()).isEqualTo(appUserBack);

        vote.voter(null);
        assertThat(vote.getVoter()).isNull();
    }

    @Test
    void targetUserTest() {
        Vote vote = getVoteRandomSampleGenerator();
        AppUser appUserBack = getAppUserRandomSampleGenerator();

        vote.setTargetUser(appUserBack);
        assertThat(vote.getTargetUser()).isEqualTo(appUserBack);

        vote.targetUser(null);
        assertThat(vote.getTargetUser()).isNull();
    }
}
