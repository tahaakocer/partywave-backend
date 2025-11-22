package com.partywave.backend.service.mapper;

import static com.partywave.backend.domain.RoomInvitationAsserts.*;
import static com.partywave.backend.domain.RoomInvitationTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RoomInvitationMapperTest {

    private RoomInvitationMapper roomInvitationMapper;

    @BeforeEach
    void setUp() {
        roomInvitationMapper = new RoomInvitationMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getRoomInvitationSample1();
        var actual = roomInvitationMapper.toEntity(roomInvitationMapper.toDto(expected));
        assertRoomInvitationAllPropertiesEquals(expected, actual);
    }
}
