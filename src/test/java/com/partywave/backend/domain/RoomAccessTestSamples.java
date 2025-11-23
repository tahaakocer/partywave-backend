package com.partywave.backend.domain;

import java.util.UUID;

public class RoomAccessTestSamples {

    public static RoomAccess getRoomAccessSample1() {
        return new RoomAccess().id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static RoomAccess getRoomAccessSample2() {
        return new RoomAccess().id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static RoomAccess getRoomAccessRandomSampleGenerator() {
        return new RoomAccess().id(UUID.randomUUID());
    }
}
