package com.partywave.backend.service.mapper;

import static com.partywave.backend.domain.AppUserImageAsserts.*;
import static com.partywave.backend.domain.AppUserImageTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AppUserImageMapperTest {

    private AppUserImageMapper appUserImageMapper;

    @BeforeEach
    void setUp() {
        appUserImageMapper = new AppUserImageMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getAppUserImageSample1();
        var actual = appUserImageMapper.toEntity(appUserImageMapper.toDto(expected));
        assertAppUserImageAllPropertiesEquals(expected, actual);
    }
}
