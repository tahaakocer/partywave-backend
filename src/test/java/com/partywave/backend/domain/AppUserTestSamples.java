package com.partywave.backend.domain;

import java.util.UUID;

public class AppUserTestSamples {

    public static AppUser getAppUserSample1() {
        return new AppUser()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
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
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
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
            .id(UUID.randomUUID())
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
