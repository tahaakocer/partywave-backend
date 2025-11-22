package com.partywave.backend.domain;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class RoomAccessTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static RoomAccess getRoomAccessSample1() {
        return new RoomAccess().id(1L);
    }

    public static RoomAccess getRoomAccessSample2() {
        return new RoomAccess().id(2L);
    }

    public static RoomAccess getRoomAccessRandomSampleGenerator() {
        return new RoomAccess().id(longCount.incrementAndGet());
    }
}
