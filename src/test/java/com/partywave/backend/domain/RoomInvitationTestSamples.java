package com.partywave.backend.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class RoomInvitationTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static RoomInvitation getRoomInvitationSample1() {
        return new RoomInvitation().id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa")).token("token1").maxUses(1).usedCount(1);
    }

    public static RoomInvitation getRoomInvitationSample2() {
        return new RoomInvitation().id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367")).token("token2").maxUses(2).usedCount(2);
    }

    public static RoomInvitation getRoomInvitationRandomSampleGenerator() {
        return new RoomInvitation()
            .id(UUID.randomUUID())
            .token(UUID.randomUUID().toString())
            .maxUses(intCount.incrementAndGet())
            .usedCount(intCount.incrementAndGet());
    }
}
