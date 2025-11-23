package com.partywave.backend.service.mapper;

import static com.partywave.backend.domain.UserTokenAsserts.*;
import static com.partywave.backend.domain.UserTokenTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserTokenMapperTest {

    private UserTokenMapper userTokenMapper;

    @BeforeEach
    void setUp() {
        userTokenMapper = new UserTokenMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getUserTokenSample1();
        var actual = userTokenMapper.toEntity(userTokenMapper.toDto(expected));
        assertUserTokenAllPropertiesEquals(expected, actual);
    }
}
