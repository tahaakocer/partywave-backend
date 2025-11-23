package com.partywave.backend.domain;

import java.util.UUID;

public class UserTokenTestSamples {

    public static UserToken getUserTokenSample1() {
        return new UserToken()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .accessToken("accessToken1")
            .refreshToken("refreshToken1")
            .tokenType("tokenType1")
            .scope("scope1");
    }

    public static UserToken getUserTokenSample2() {
        return new UserToken()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .accessToken("accessToken2")
            .refreshToken("refreshToken2")
            .tokenType("tokenType2")
            .scope("scope2");
    }

    public static UserToken getUserTokenRandomSampleGenerator() {
        return new UserToken()
            .id(UUID.randomUUID())
            .accessToken(UUID.randomUUID().toString())
            .refreshToken(UUID.randomUUID().toString())
            .tokenType(UUID.randomUUID().toString())
            .scope(UUID.randomUUID().toString());
    }
}
