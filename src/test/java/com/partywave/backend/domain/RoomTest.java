package com.partywave.backend.domain;

import static com.partywave.backend.domain.ChatMessageTestSamples.*;
import static com.partywave.backend.domain.RoomAccessTestSamples.*;
import static com.partywave.backend.domain.RoomInvitationTestSamples.*;
import static com.partywave.backend.domain.RoomMemberTestSamples.*;
import static com.partywave.backend.domain.RoomTestSamples.*;
import static com.partywave.backend.domain.TagTestSamples.*;
import static com.partywave.backend.domain.VoteTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.partywave.backend.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RoomTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Room.class);
        Room room1 = getRoomSample1();
        Room room2 = new Room();
        assertThat(room1).isNotEqualTo(room2);

        room2.setId(room1.getId());
        assertThat(room1).isEqualTo(room2);

        room2 = getRoomSample2();
        assertThat(room1).isNotEqualTo(room2);
    }

    @Test
    void membersTest() {
        Room room = getRoomRandomSampleGenerator();
        RoomMember roomMemberBack = getRoomMemberRandomSampleGenerator();

        room.addMembers(roomMemberBack);
        assertThat(room.getMembers()).containsOnly(roomMemberBack);
        assertThat(roomMemberBack.getRoom()).isEqualTo(room);

        room.removeMembers(roomMemberBack);
        assertThat(room.getMembers()).doesNotContain(roomMemberBack);
        assertThat(roomMemberBack.getRoom()).isNull();

        room.members(new HashSet<>(Set.of(roomMemberBack)));
        assertThat(room.getMembers()).containsOnly(roomMemberBack);
        assertThat(roomMemberBack.getRoom()).isEqualTo(room);

        room.setMembers(new HashSet<>());
        assertThat(room.getMembers()).doesNotContain(roomMemberBack);
        assertThat(roomMemberBack.getRoom()).isNull();
    }

    @Test
    void accessesTest() {
        Room room = getRoomRandomSampleGenerator();
        RoomAccess roomAccessBack = getRoomAccessRandomSampleGenerator();

        room.addAccesses(roomAccessBack);
        assertThat(room.getAccesses()).containsOnly(roomAccessBack);
        assertThat(roomAccessBack.getRoom()).isEqualTo(room);

        room.removeAccesses(roomAccessBack);
        assertThat(room.getAccesses()).doesNotContain(roomAccessBack);
        assertThat(roomAccessBack.getRoom()).isNull();

        room.accesses(new HashSet<>(Set.of(roomAccessBack)));
        assertThat(room.getAccesses()).containsOnly(roomAccessBack);
        assertThat(roomAccessBack.getRoom()).isEqualTo(room);

        room.setAccesses(new HashSet<>());
        assertThat(room.getAccesses()).doesNotContain(roomAccessBack);
        assertThat(roomAccessBack.getRoom()).isNull();
    }

    @Test
    void invitationsTest() {
        Room room = getRoomRandomSampleGenerator();
        RoomInvitation roomInvitationBack = getRoomInvitationRandomSampleGenerator();

        room.addInvitations(roomInvitationBack);
        assertThat(room.getInvitations()).containsOnly(roomInvitationBack);
        assertThat(roomInvitationBack.getRoom()).isEqualTo(room);

        room.removeInvitations(roomInvitationBack);
        assertThat(room.getInvitations()).doesNotContain(roomInvitationBack);
        assertThat(roomInvitationBack.getRoom()).isNull();

        room.invitations(new HashSet<>(Set.of(roomInvitationBack)));
        assertThat(room.getInvitations()).containsOnly(roomInvitationBack);
        assertThat(roomInvitationBack.getRoom()).isEqualTo(room);

        room.setInvitations(new HashSet<>());
        assertThat(room.getInvitations()).doesNotContain(roomInvitationBack);
        assertThat(roomInvitationBack.getRoom()).isNull();
    }

    @Test
    void messagesTest() {
        Room room = getRoomRandomSampleGenerator();
        ChatMessage chatMessageBack = getChatMessageRandomSampleGenerator();

        room.addMessages(chatMessageBack);
        assertThat(room.getMessages()).containsOnly(chatMessageBack);
        assertThat(chatMessageBack.getRoom()).isEqualTo(room);

        room.removeMessages(chatMessageBack);
        assertThat(room.getMessages()).doesNotContain(chatMessageBack);
        assertThat(chatMessageBack.getRoom()).isNull();

        room.messages(new HashSet<>(Set.of(chatMessageBack)));
        assertThat(room.getMessages()).containsOnly(chatMessageBack);
        assertThat(chatMessageBack.getRoom()).isEqualTo(room);

        room.setMessages(new HashSet<>());
        assertThat(room.getMessages()).doesNotContain(chatMessageBack);
        assertThat(chatMessageBack.getRoom()).isNull();
    }

    @Test
    void votesTest() {
        Room room = getRoomRandomSampleGenerator();
        Vote voteBack = getVoteRandomSampleGenerator();

        room.addVotes(voteBack);
        assertThat(room.getVotes()).containsOnly(voteBack);
        assertThat(voteBack.getRoom()).isEqualTo(room);

        room.removeVotes(voteBack);
        assertThat(room.getVotes()).doesNotContain(voteBack);
        assertThat(voteBack.getRoom()).isNull();

        room.votes(new HashSet<>(Set.of(voteBack)));
        assertThat(room.getVotes()).containsOnly(voteBack);
        assertThat(voteBack.getRoom()).isEqualTo(room);

        room.setVotes(new HashSet<>());
        assertThat(room.getVotes()).doesNotContain(voteBack);
        assertThat(voteBack.getRoom()).isNull();
    }

    @Test
    void tagsTest() {
        Room room = getRoomRandomSampleGenerator();
        Tag tagBack = getTagRandomSampleGenerator();

        room.addTags(tagBack);
        assertThat(room.getTags()).containsOnly(tagBack);

        room.removeTags(tagBack);
        assertThat(room.getTags()).doesNotContain(tagBack);

        room.tags(new HashSet<>(Set.of(tagBack)));
        assertThat(room.getTags()).containsOnly(tagBack);

        room.setTags(new HashSet<>());
        assertThat(room.getTags()).doesNotContain(tagBack);
    }
}
