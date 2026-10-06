package com.Momik.usless_mobs.ability;

import java.util.UUID;

/** Plain-Java packet-order regression test for the productive cue-state component. */
public final class FrostStrayTimelineStateTest {
    private FrostStrayTimelineStateTest() {
    }

    public static void main(String[] args) {
        FrostStrayTimelineState state = new FrostStrayTimelineState();
        UUID first = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID second = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID impact = UUID.fromString("00000000-0000-0000-0000-000000000003");

        require(state.start(7, first, 100L, 18, 100L) == FrostStrayTimelineState.StartResult.STARTED,
                "first start must activate");
        require(state.start(7, first, 100L, 18, 106L)
                        == FrostStrayTimelineState.StartResult.IGNORED_STALE,
                "duplicate start must not restart the cue");
        require(state.activeFor(7).startGameTime() == 100L, "duplicate start must preserve start tick");

        require(state.cancel(7, first, 105L), "active cancellation must remove the active timeline");
        require(state.start(7, first, 100L, 18, 106L)
                        == FrostStrayTimelineState.StartResult.IGNORED_FINISHED,
                "delayed start must not reactivate a cancelled instance");

        require(state.start(7, second, 130L, 18, 130L) == FrostStrayTimelineState.StartResult.STARTED,
                "new instance must activate");
        require(!state.release(7, first, 140L), "old release must not complete a new instance");
        require(state.activeFor(7).instanceId().equals(second), "new instance must remain active");
        require(state.release(7, second, 148L), "matching release must complete active instance");
        require(state.activeFor(7) == null, "release must remove active instance");

        require(state.recordImpact(impact, 148L), "first impact must be emitted");
        require(!state.recordImpact(impact, 149L), "duplicate impact must be ignored");
        require(state.start(9, UUID.randomUUID(), 100L, 18, 118L)
                        == FrostStrayTimelineState.StartResult.IGNORED_EXPIRED,
                "late snapshot after duration must not create a cue");
        System.out.println("FrostStrayTimelineStateTest PASS");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
