package com.partywave.backend.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class RoomInvitationTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static RoomInvitation getRoomInvitationSample1() {
        return new RoomInvitation().id(1L).token("token1").maxUses(1).usedCount(1);
    }

    public static RoomInvitation getRoomInvitationSample2() {
        return new RoomInvitation().id(2L).token("token2").maxUses(2).usedCount(2);
    }

    public static RoomInvitation getRoomInvitationRandomSampleGenerator() {
        return new RoomInvitation()
            .id(longCount.incrementAndGet())
            .token(UUID.randomUUID().toString())
            .maxUses(intCount.incrementAndGet())
            .usedCount(intCount.incrementAndGet());
    }
}
