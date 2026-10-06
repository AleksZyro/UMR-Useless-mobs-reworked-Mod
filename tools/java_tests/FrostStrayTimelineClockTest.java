package com.Momik.usless_mobs.ability;

/** Plain-Java regression test for the production clock; no Forge runtime is needed. */
public final class FrostStrayTimelineClockTest {
    private FrostStrayTimelineClockTest() {
    }

    public static void main(String[] args) {
        FrostStrayTimelineClock clock = new FrostStrayTimelineClock();
        clock.observe(100L, 100L);
        require(clock.serverTime(106L) == 106L, "initial server time");

        // The original bug rewrote the offset to -6 here and moved time back to 100.
        clock.observe(100L, 106L);
        require(clock.serverTime(106L) == 106L, "duplicate packet must not move time backwards");
        require(clock.elapsed(100L, 106L) == 6L, "duplicate packet must preserve elapsed charge");

        clock.observe(106L, 106L);
        require(clock.serverTime(108L) == 108L, "newer packet may advance the estimate");
        clock.observe(104L, 110L);
        require(clock.serverTime(110L) == 110L, "late older packet must be ignored");

        clock.reset();
        clock.observe(112L, 110L);
        require(clock.elapsed(100L, 110L) == 12L, "late first packet must use its server start tick");
        System.out.println("FrostStrayTimelineClockTest PASS");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
