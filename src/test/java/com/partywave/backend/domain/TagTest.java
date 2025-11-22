package com.partywave.backend.domain;

import static com.partywave.backend.domain.RoomTestSamples.*;
import static com.partywave.backend.domain.TagTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.partywave.backend.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TagTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Tag.class);
        Tag tag1 = getTagSample1();
        Tag tag2 = new Tag();
        assertThat(tag1).isNotEqualTo(tag2);

        tag2.setId(tag1.getId());
        assertThat(tag1).isEqualTo(tag2);

        tag2 = getTagSample2();
        assertThat(tag1).isNotEqualTo(tag2);
    }

    @Test
    void roomsTest() {
        Tag tag = getTagRandomSampleGenerator();
        Room roomBack = getRoomRandomSampleGenerator();

        tag.addRooms(roomBack);
        assertThat(tag.getRooms()).containsOnly(roomBack);
        assertThat(roomBack.getTags()).containsOnly(tag);

        tag.removeRooms(roomBack);
        assertThat(tag.getRooms()).doesNotContain(roomBack);
        assertThat(roomBack.getTags()).doesNotContain(tag);

        tag.rooms(new HashSet<>(Set.of(roomBack)));
        assertThat(tag.getRooms()).containsOnly(roomBack);
        assertThat(roomBack.getTags()).containsOnly(tag);

        tag.setRooms(new HashSet<>());
        assertThat(tag.getRooms()).doesNotContain(roomBack);
        assertThat(roomBack.getTags()).doesNotContain(tag);
    }
}
