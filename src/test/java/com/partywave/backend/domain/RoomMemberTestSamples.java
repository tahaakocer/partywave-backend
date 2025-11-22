package com.partywave.backend.domain;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class RoomMemberTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static RoomMember getRoomMemberSample1() {
        return new RoomMember().id(1L);
    }

    public static RoomMember getRoomMemberSample2() {
        return new RoomMember().id(2L);
    }

    public static RoomMember getRoomMemberRandomSampleGenerator() {
        return new RoomMember().id(longCount.incrementAndGet());
    }
}
