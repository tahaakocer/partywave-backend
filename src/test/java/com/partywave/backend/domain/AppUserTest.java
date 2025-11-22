package com.partywave.backend.domain;

import static com.partywave.backend.domain.AppUserImageTestSamples.*;
import static com.partywave.backend.domain.AppUserStatsTestSamples.*;
import static com.partywave.backend.domain.AppUserTestSamples.*;
import static com.partywave.backend.domain.ChatMessageTestSamples.*;
import static com.partywave.backend.domain.RefreshTokenTestSamples.*;
import static com.partywave.backend.domain.RoomAccessTestSamples.*;
import static com.partywave.backend.domain.RoomInvitationTestSamples.*;
import static com.partywave.backend.domain.RoomMemberTestSamples.*;
import static com.partywave.backend.domain.UserTokenTestSamples.*;
import static com.partywave.backend.domain.VoteTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.partywave.backend.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AppUserTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(AppUser.class);
        AppUser appUser1 = getAppUserSample1();
        AppUser appUser2 = new AppUser();
        assertThat(appUser1).isNotEqualTo(appUser2);

        appUser2.setId(appUser1.getId());
        assertThat(appUser1).isEqualTo(appUser2);

        appUser2 = getAppUserSample2();
        assertThat(appUser1).isNotEqualTo(appUser2);
    }

    @Test
    void appUserStatsTest() {
        AppUser appUser = getAppUserRandomSampleGenerator();
        AppUserStats appUserStatsBack = getAppUserStatsRandomSampleGenerator();

        appUser.setAppUserStats(appUserStatsBack);
        assertThat(appUser.getAppUserStats()).isEqualTo(appUserStatsBack);

        appUser.appUserStats(null);
        assertThat(appUser.getAppUserStats()).isNull();
    }

    @Test
    void imagesTest() {
        AppUser appUser = getAppUserRandomSampleGenerator();
        AppUserImage appUserImageBack = getAppUserImageRandomSampleGenerator();

        appUser.addImages(appUserImageBack);
        assertThat(appUser.getImages()).containsOnly(appUserImageBack);
        assertThat(appUserImageBack.getAppUser()).isEqualTo(appUser);

        appUser.removeImages(appUserImageBack);
        assertThat(appUser.getImages()).doesNotContain(appUserImageBack);
        assertThat(appUserImageBack.getAppUser()).isNull();

        appUser.images(new HashSet<>(Set.of(appUserImageBack)));
        assertThat(appUser.getImages()).containsOnly(appUserImageBack);
        assertThat(appUserImageBack.getAppUser()).isEqualTo(appUser);

        appUser.setImages(new HashSet<>());
        assertThat(appUser.getImages()).doesNotContain(appUserImageBack);
        assertThat(appUserImageBack.getAppUser()).isNull();
    }

    @Test
    void refreshTokensTest() {
        AppUser appUser = getAppUserRandomSampleGenerator();
        RefreshToken refreshTokenBack = getRefreshTokenRandomSampleGenerator();

        appUser.addRefreshTokens(refreshTokenBack);
        assertThat(appUser.getRefreshTokens()).containsOnly(refreshTokenBack);
        assertThat(refreshTokenBack.getAppUser()).isEqualTo(appUser);

        appUser.removeRefreshTokens(refreshTokenBack);
        assertThat(appUser.getRefreshTokens()).doesNotContain(refreshTokenBack);
        assertThat(refreshTokenBack.getAppUser()).isNull();

        appUser.refreshTokens(new HashSet<>(Set.of(refreshTokenBack)));
        assertThat(appUser.getRefreshTokens()).containsOnly(refreshTokenBack);
        assertThat(refreshTokenBack.getAppUser()).isEqualTo(appUser);

        appUser.setRefreshTokens(new HashSet<>());
        assertThat(appUser.getRefreshTokens()).doesNotContain(refreshTokenBack);
        assertThat(refreshTokenBack.getAppUser()).isNull();
    }

    @Test
    void membershipsTest() {
        AppUser appUser = getAppUserRandomSampleGenerator();
        RoomMember roomMemberBack = getRoomMemberRandomSampleGenerator();

        appUser.addMemberships(roomMemberBack);
        assertThat(appUser.getMemberships()).containsOnly(roomMemberBack);
        assertThat(roomMemberBack.getAppUser()).isEqualTo(appUser);

        appUser.removeMemberships(roomMemberBack);
        assertThat(appUser.getMemberships()).doesNotContain(roomMemberBack);
        assertThat(roomMemberBack.getAppUser()).isNull();

        appUser.memberships(new HashSet<>(Set.of(roomMemberBack)));
        assertThat(appUser.getMemberships()).containsOnly(roomMemberBack);
        assertThat(roomMemberBack.getAppUser()).isEqualTo(appUser);

        appUser.setMemberships(new HashSet<>());
        assertThat(appUser.getMemberships()).doesNotContain(roomMemberBack);
        assertThat(roomMemberBack.getAppUser()).isNull();
    }

    @Test
    void receivedAccessesTest() {
        AppUser appUser = getAppUserRandomSampleGenerator();
        RoomAccess roomAccessBack = getRoomAccessRandomSampleGenerator();

        appUser.addReceivedAccesses(roomAccessBack);
        assertThat(appUser.getReceivedAccesses()).containsOnly(roomAccessBack);
        assertThat(roomAccessBack.getAppUser()).isEqualTo(appUser);

        appUser.removeReceivedAccesses(roomAccessBack);
        assertThat(appUser.getReceivedAccesses()).doesNotContain(roomAccessBack);
        assertThat(roomAccessBack.getAppUser()).isNull();

        appUser.receivedAccesses(new HashSet<>(Set.of(roomAccessBack)));
        assertThat(appUser.getReceivedAccesses()).containsOnly(roomAccessBack);
        assertThat(roomAccessBack.getAppUser()).isEqualTo(appUser);

        appUser.setReceivedAccesses(new HashSet<>());
        assertThat(appUser.getReceivedAccesses()).doesNotContain(roomAccessBack);
        assertThat(roomAccessBack.getAppUser()).isNull();
    }

    @Test
    void grantedAccessesTest() {
        AppUser appUser = getAppUserRandomSampleGenerator();
        RoomAccess roomAccessBack = getRoomAccessRandomSampleGenerator();

        appUser.addGrantedAccesses(roomAccessBack);
        assertThat(appUser.getGrantedAccesses()).containsOnly(roomAccessBack);
        assertThat(roomAccessBack.getGrantedBy()).isEqualTo(appUser);

        appUser.removeGrantedAccesses(roomAccessBack);
        assertThat(appUser.getGrantedAccesses()).doesNotContain(roomAccessBack);
        assertThat(roomAccessBack.getGrantedBy()).isNull();

        appUser.grantedAccesses(new HashSet<>(Set.of(roomAccessBack)));
        assertThat(appUser.getGrantedAccesses()).containsOnly(roomAccessBack);
        assertThat(roomAccessBack.getGrantedBy()).isEqualTo(appUser);

        appUser.setGrantedAccesses(new HashSet<>());
        assertThat(appUser.getGrantedAccesses()).doesNotContain(roomAccessBack);
        assertThat(roomAccessBack.getGrantedBy()).isNull();
    }

    @Test
    void createdInvitationsTest() {
        AppUser appUser = getAppUserRandomSampleGenerator();
        RoomInvitation roomInvitationBack = getRoomInvitationRandomSampleGenerator();

        appUser.addCreatedInvitations(roomInvitationBack);
        assertThat(appUser.getCreatedInvitations()).containsOnly(roomInvitationBack);
        assertThat(roomInvitationBack.getCreatedBy()).isEqualTo(appUser);

        appUser.removeCreatedInvitations(roomInvitationBack);
        assertThat(appUser.getCreatedInvitations()).doesNotContain(roomInvitationBack);
        assertThat(roomInvitationBack.getCreatedBy()).isNull();

        appUser.createdInvitations(new HashSet<>(Set.of(roomInvitationBack)));
        assertThat(appUser.getCreatedInvitations()).containsOnly(roomInvitationBack);
        assertThat(roomInvitationBack.getCreatedBy()).isEqualTo(appUser);

        appUser.setCreatedInvitations(new HashSet<>());
        assertThat(appUser.getCreatedInvitations()).doesNotContain(roomInvitationBack);
        assertThat(roomInvitationBack.getCreatedBy()).isNull();
    }

    @Test
    void messagesTest() {
        AppUser appUser = getAppUserRandomSampleGenerator();
        ChatMessage chatMessageBack = getChatMessageRandomSampleGenerator();

        appUser.addMessages(chatMessageBack);
        assertThat(appUser.getMessages()).containsOnly(chatMessageBack);
        assertThat(chatMessageBack.getSender()).isEqualTo(appUser);

        appUser.removeMessages(chatMessageBack);
        assertThat(appUser.getMessages()).doesNotContain(chatMessageBack);
        assertThat(chatMessageBack.getSender()).isNull();

        appUser.messages(new HashSet<>(Set.of(chatMessageBack)));
        assertThat(appUser.getMessages()).containsOnly(chatMessageBack);
        assertThat(chatMessageBack.getSender()).isEqualTo(appUser);

        appUser.setMessages(new HashSet<>());
        assertThat(appUser.getMessages()).doesNotContain(chatMessageBack);
        assertThat(chatMessageBack.getSender()).isNull();
    }

    @Test
    void castVotesTest() {
        AppUser appUser = getAppUserRandomSampleGenerator();
        Vote voteBack = getVoteRandomSampleGenerator();

        appUser.addCastVotes(voteBack);
        assertThat(appUser.getCastVotes()).containsOnly(voteBack);
        assertThat(voteBack.getVoter()).isEqualTo(appUser);

        appUser.removeCastVotes(voteBack);
        assertThat(appUser.getCastVotes()).doesNotContain(voteBack);
        assertThat(voteBack.getVoter()).isNull();

        appUser.castVotes(new HashSet<>(Set.of(voteBack)));
        assertThat(appUser.getCastVotes()).containsOnly(voteBack);
        assertThat(voteBack.getVoter()).isEqualTo(appUser);

        appUser.setCastVotes(new HashSet<>());
        assertThat(appUser.getCastVotes()).doesNotContain(voteBack);
        assertThat(voteBack.getVoter()).isNull();
    }

    @Test
    void receivedVotesTest() {
        AppUser appUser = getAppUserRandomSampleGenerator();
        Vote voteBack = getVoteRandomSampleGenerator();

        appUser.addReceivedVotes(voteBack);
        assertThat(appUser.getReceivedVotes()).containsOnly(voteBack);
        assertThat(voteBack.getTargetUser()).isEqualTo(appUser);

        appUser.removeReceivedVotes(voteBack);
        assertThat(appUser.getReceivedVotes()).doesNotContain(voteBack);
        assertThat(voteBack.getTargetUser()).isNull();

        appUser.receivedVotes(new HashSet<>(Set.of(voteBack)));
        assertThat(appUser.getReceivedVotes()).containsOnly(voteBack);
        assertThat(voteBack.getTargetUser()).isEqualTo(appUser);

        appUser.setReceivedVotes(new HashSet<>());
        assertThat(appUser.getReceivedVotes()).doesNotContain(voteBack);
        assertThat(voteBack.getTargetUser()).isNull();
    }

    @Test
    void userTokenTest() {
        AppUser appUser = getAppUserRandomSampleGenerator();
        UserToken userTokenBack = getUserTokenRandomSampleGenerator();

        appUser.setUserToken(userTokenBack);
        assertThat(appUser.getUserToken()).isEqualTo(userTokenBack);
        assertThat(userTokenBack.getAppUser()).isEqualTo(appUser);

        appUser.userToken(null);
        assertThat(appUser.getUserToken()).isNull();
        assertThat(userTokenBack.getAppUser()).isNull();
    }
}
