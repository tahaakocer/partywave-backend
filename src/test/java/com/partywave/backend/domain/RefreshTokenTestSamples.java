package com.partywave.backend.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class RefreshTokenTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static RefreshToken getRefreshTokenSample1() {
        return new RefreshToken().id(1L).tokenHash("tokenHash1").deviceInfo("deviceInfo1").ipAddress("ipAddress1");
    }

    public static RefreshToken getRefreshTokenSample2() {
        return new RefreshToken().id(2L).tokenHash("tokenHash2").deviceInfo("deviceInfo2").ipAddress("ipAddress2");
    }

    public static RefreshToken getRefreshTokenRandomSampleGenerator() {
        return new RefreshToken()
            .id(longCount.incrementAndGet())
            .tokenHash(UUID.randomUUID().toString())
            .deviceInfo(UUID.randomUUID().toString())
            .ipAddress(UUID.randomUUID().toString());
    }
}
