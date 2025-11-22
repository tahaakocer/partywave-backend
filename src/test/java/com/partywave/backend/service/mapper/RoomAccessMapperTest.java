package com.partywave.backend.service.mapper;

import static com.partywave.backend.domain.RoomAccessAsserts.*;
import static com.partywave.backend.domain.RoomAccessTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RoomAccessMapperTest {

    private RoomAccessMapper roomAccessMapper;

    @BeforeEach
    void setUp() {
        roomAccessMapper = new RoomAccessMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getRoomAccessSample1();
        var actual = roomAccessMapper.toEntity(roomAccessMapper.toDto(expected));
        assertRoomAccessAllPropertiesEquals(expected, actual);
    }
}
