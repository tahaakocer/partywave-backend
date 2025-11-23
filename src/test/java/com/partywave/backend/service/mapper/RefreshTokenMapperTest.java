package com.partywave.backend.service.mapper;

import static com.partywave.backend.domain.RefreshTokenAsserts.*;
import static com.partywave.backend.domain.RefreshTokenTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RefreshTokenMapperTest {

    private RefreshTokenMapper refreshTokenMapper;

    @BeforeEach
    void setUp() {
        refreshTokenMapper = new RefreshTokenMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getRefreshTokenSample1();
        var actual = refreshTokenMapper.toEntity(refreshTokenMapper.toDto(expected));
        assertRefreshTokenAllPropertiesEquals(expected, actual);
    }
}
