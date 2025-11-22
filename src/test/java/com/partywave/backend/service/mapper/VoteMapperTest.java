package com.partywave.backend.service.mapper;

import static com.partywave.backend.domain.VoteAsserts.*;
import static com.partywave.backend.domain.VoteTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VoteMapperTest {

    private VoteMapper voteMapper;

    @BeforeEach
    void setUp() {
        voteMapper = new VoteMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getVoteSample1();
        var actual = voteMapper.toEntity(voteMapper.toDto(expected));
        assertVoteAllPropertiesEquals(expected, actual);
    }
}
