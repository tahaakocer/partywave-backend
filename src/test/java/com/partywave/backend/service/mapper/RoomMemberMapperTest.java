package com.partywave.backend.service.mapper;

import static com.partywave.backend.domain.RoomMemberAsserts.*;
import static com.partywave.backend.domain.RoomMemberTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RoomMemberMapperTest {

    private RoomMemberMapper roomMemberMapper;

    @BeforeEach
    void setUp() {
        roomMemberMapper = new RoomMemberMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getRoomMemberSample1();
        var actual = roomMemberMapper.toEntity(roomMemberMapper.toDto(expected));
        assertRoomMemberAllPropertiesEquals(expected, actual);
    }
}
