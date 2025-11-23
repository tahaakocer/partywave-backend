package com.partywave.backend.domain;

import java.util.UUID;

public class RefreshTokenTestSamples {

    public static RefreshToken getRefreshTokenSample1() {
        return new RefreshToken()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .tokenHash("tokenHash1")
            .deviceInfo("deviceInfo1")
            .ipAddress("ipAddress1");
    }

    public static RefreshToken getRefreshTokenSample2() {
        return new RefreshToken()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .tokenHash("tokenHash2")
            .deviceInfo("deviceInfo2")
            .ipAddress("ipAddress2");
    }

    public static RefreshToken getRefreshTokenRandomSampleGenerator() {
        return new RefreshToken()
            .id(UUID.randomUUID())
            .tokenHash(UUID.randomUUID().toString())
            .deviceInfo(UUID.randomUUID().toString())
            .ipAddress(UUID.randomUUID().toString());
    }
}
