package com.partywave.backend.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class AppUserImageTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static AppUserImage getAppUserImageSample1() {
        return new AppUserImage().id(1L).url("url1").height("height1").width("width1");
    }

    public static AppUserImage getAppUserImageSample2() {
        return new AppUserImage().id(2L).url("url2").height("height2").width("width2");
    }

    public static AppUserImage getAppUserImageRandomSampleGenerator() {
        return new AppUserImage()
            .id(longCount.incrementAndGet())
            .url(UUID.randomUUID().toString())
            .height(UUID.randomUUID().toString())
            .width(UUID.randomUUID().toString());
    }
}
