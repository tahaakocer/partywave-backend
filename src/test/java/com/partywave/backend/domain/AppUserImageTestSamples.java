package com.partywave.backend.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class AppUserImageTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static AppUserImage getAppUserImageSample1() {
        return new AppUserImage().id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa")).url("url1").height(1).width(1);
    }

    public static AppUserImage getAppUserImageSample2() {
        return new AppUserImage().id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367")).url("url2").height(2).width(2);
    }

    public static AppUserImage getAppUserImageRandomSampleGenerator() {
        return new AppUserImage()
            .id(UUID.randomUUID())
            .url(UUID.randomUUID().toString())
            .height(intCount.incrementAndGet())
            .width(intCount.incrementAndGet());
    }
}
