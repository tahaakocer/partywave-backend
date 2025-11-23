package com.partywave.backend.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class AppUserStatsTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static AppUserStats getAppUserStatsSample1() {
        return new AppUserStats().id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa")).totalLike(1).totalDislike(1);
    }

    public static AppUserStats getAppUserStatsSample2() {
        return new AppUserStats().id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367")).totalLike(2).totalDislike(2);
    }

    public static AppUserStats getAppUserStatsRandomSampleGenerator() {
        return new AppUserStats().id(UUID.randomUUID()).totalLike(intCount.incrementAndGet()).totalDislike(intCount.incrementAndGet());
    }
}
