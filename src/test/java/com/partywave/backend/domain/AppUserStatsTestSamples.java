package com.partywave.backend.domain;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class AppUserStatsTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static AppUserStats getAppUserStatsSample1() {
        return new AppUserStats().id(1L).totalLike(1).totalDislike(1);
    }

    public static AppUserStats getAppUserStatsSample2() {
        return new AppUserStats().id(2L).totalLike(2).totalDislike(2);
    }

    public static AppUserStats getAppUserStatsRandomSampleGenerator() {
        return new AppUserStats()
            .id(longCount.incrementAndGet())
            .totalLike(intCount.incrementAndGet())
            .totalDislike(intCount.incrementAndGet());
    }
}
