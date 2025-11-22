package com.partywave.backend.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class UserTokenTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static UserToken getUserTokenSample1() {
        return new UserToken().id(1L).accessToken("accessToken1").refreshToken("refreshToken1").tokenType("tokenType1").scope("scope1");
    }

    public static UserToken getUserTokenSample2() {
        return new UserToken().id(2L).accessToken("accessToken2").refreshToken("refreshToken2").tokenType("tokenType2").scope("scope2");
    }

    public static UserToken getUserTokenRandomSampleGenerator() {
        return new UserToken()
            .id(longCount.incrementAndGet())
            .accessToken(UUID.randomUUID().toString())
            .refreshToken(UUID.randomUUID().toString())
            .tokenType(UUID.randomUUID().toString())
            .scope(UUID.randomUUID().toString());
    }
}
