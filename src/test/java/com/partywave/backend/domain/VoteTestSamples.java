package com.partywave.backend.domain;

import java.util.UUID;

public class VoteTestSamples {

    public static Vote getVoteSample1() {
        return new Vote().id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa")).playlistItemId("playlistItemId1");
    }

    public static Vote getVoteSample2() {
        return new Vote().id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367")).playlistItemId("playlistItemId2");
    }

    public static Vote getVoteRandomSampleGenerator() {
        return new Vote().id(UUID.randomUUID()).playlistItemId(UUID.randomUUID().toString());
    }
}
