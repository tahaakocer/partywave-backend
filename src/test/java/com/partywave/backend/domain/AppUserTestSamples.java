package com.partywave.backend.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class AppUserTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static AppUser getAppUserSample1() {
        return new AppUser()
            .id(1L)
            .spotifyUserId("spotifyUserId1")
            .displayName("displayName1")
            .email("email1")
            .country("country1")
            .href("href1")
            .url("url1")
            .type("type1")
            .ipAddress("ipAddress1");
    }

    public static AppUser getAppUserSample2() {
        return new AppUser()
            .id(2L)
            .spotifyUserId("spotifyUserId2")
            .displayName("displayName2")
            .email("email2")
            .country("country2")
            .href("href2")
            .url("url2")
            .type("type2")
            .ipAddress("ipAddress2");
    }

    public static AppUser getAppUserRandomSampleGenerator() {
        return new AppUser()
            .id(longCount.incrementAndGet())
            .spotifyUserId(UUID.randomUUID().toString())
            .displayName(UUID.randomUUID().toString())
            .email(UUID.randomUUID().toString())
            .country(UUID.randomUUID().toString())
            .href(UUID.randomUUID().toString())
            .url(UUID.randomUUID().toString())
            .type(UUID.randomUUID().toString())
            .ipAddress(UUID.randomUUID().toString());
    }
}
