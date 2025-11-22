package com.partywave.backend.service.mapper;

import static com.partywave.backend.domain.AppUserStatsAsserts.*;
import static com.partywave.backend.domain.AppUserStatsTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AppUserStatsMapperTest {

    private AppUserStatsMapper appUserStatsMapper;

    @BeforeEach
    void setUp() {
        appUserStatsMapper = new AppUserStatsMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getAppUserStatsSample1();
        var actual = appUserStatsMapper.toEntity(appUserStatsMapper.toDto(expected));
        assertAppUserStatsAllPropertiesEquals(expected, actual);
    }
}
