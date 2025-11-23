package com.partywave.backend.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class RoomTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static Room getRoomSample1() {
        return new Room()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .name("name1")
            .description("description1")
            .maxParticipants(1);
    }

    public static Room getRoomSample2() {
        return new Room()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .name("name2")
            .description("description2")
            .maxParticipants(2);
    }

    public static Room getRoomRandomSampleGenerator() {
        return new Room()
            .id(UUID.randomUUID())
            .name(UUID.randomUUID().toString())
            .description(UUID.randomUUID().toString())
            .maxParticipants(intCount.incrementAndGet());
    }
}
