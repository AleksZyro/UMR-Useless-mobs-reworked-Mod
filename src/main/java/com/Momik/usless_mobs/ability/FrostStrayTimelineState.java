package com.Momik.usless_mobs.ability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Client-side identity and completion history for Frost Stray ability cues.
 *
 * <p>This class deliberately has no Minecraft or Forge dependency so the same
 * packet-order rules used by {@code FrostStrayAbilityClient} can be exercised
 * directly. Time conversion remains the responsibility of
 * {@link FrostStrayTimelineClock}; callers pass already estimated server time.</p>
 */
public final class FrostStrayTimelineState {
    private final Map<Integer, ActiveTimeline> active = new HashMap<>();
    private final Map<UUID, Long> finished = new HashMap<>();
    private final Map<UUID, Long> impacts = new HashMap<>();

    public StartResult start(int entityId, UUID instanceId, long startGameTime, int durationTicks,
                             long estimatedServerTime) {
        if (this.finished.containsKey(instanceId)) {
            return StartResult.IGNORED_FINISHED;
        }
        if (estimatedServerTime - startGameTime >= durationTicks) {
            finish(instanceId, estimatedServerTime);
            return StartResult.IGNORED_EXPIRED;
        }

        ActiveTimeline existing = this.active.get(entityId);
        if (existing != null) {
            if (existing.instanceId.equals(instanceId) || existing.startGameTime > startGameTime) {
                return StartResult.IGNORED_STALE;
            }
            complete(entityId, existing.instanceId, estimatedServerTime);
        }
        this.active.put(entityId, new ActiveTimeline(entityId, instanceId, startGameTime, durationTicks));
        return StartResult.STARTED;
    }

    /** Completes a release only when it belongs to the currently active entity timeline. */
    public boolean release(int entityId, UUID instanceId, long serverGameTime) {
        ActiveTimeline current = this.active.get(entityId);
        if (current == null || !current.instanceId.equals(instanceId)) {
            finish(instanceId, serverGameTime);
            return false;
        }
        complete(entityId, instanceId, serverGameTime);
        return true;
    }

    /** A cancellation always closes the instance, including a delayed cancellation. */
    public boolean cancel(int entityId, UUID instanceId, long serverGameTime) {
        ActiveTimeline current = this.active.get(entityId);
        if (current != null && current.instanceId.equals(instanceId)) {
            complete(entityId, instanceId, serverGameTime);
            return true;
        }
        finish(instanceId, serverGameTime);
        return false;
    }

    /** Returns true only for the first confirmed impact identity. */
    public boolean recordImpact(UUID impactId, long serverGameTime) {
        if (this.impacts.containsKey(impactId)) {
            return false;
        }
        this.impacts.put(impactId, serverGameTime);
        return true;
    }

    public ActiveTimeline activeFor(int entityId) {
        return this.active.get(entityId);
    }

    public List<ActiveTimeline> activeTimelines() {
        return new ArrayList<>(this.active.values());
    }

    public void complete(int entityId, UUID instanceId, long serverGameTime) {
        ActiveTimeline current = this.active.get(entityId);
        if (current != null && current.instanceId.equals(instanceId)) {
            this.active.remove(entityId);
        }
        finish(instanceId, serverGameTime);
    }

    public void finish(UUID instanceId, long serverGameTime) {
        this.finished.put(instanceId, serverGameTime);
    }

    public void prune(long serverGameTime, long historyTicks) {
        this.finished.entrySet().removeIf(entry -> entry.getValue() < serverGameTime - historyTicks);
        this.impacts.entrySet().removeIf(entry -> entry.getValue() < serverGameTime - historyTicks);
    }

    public void clear() {
        this.active.clear();
        this.finished.clear();
        this.impacts.clear();
    }

    public enum StartResult {
        STARTED,
        IGNORED_FINISHED,
        IGNORED_EXPIRED,
        IGNORED_STALE
    }

    public static final class ActiveTimeline {
        private final int entityId;
        private final UUID instanceId;
        private final long startGameTime;
        private final int durationTicks;
        private long lastEmissionTick = Long.MIN_VALUE;

        private ActiveTimeline(int entityId, UUID instanceId, long startGameTime, int durationTicks) {
            this.entityId = entityId;
            this.instanceId = instanceId;
            this.startGameTime = startGameTime;
            this.durationTicks = durationTicks;
        }

        public int entityId() {
            return this.entityId;
        }

        public UUID instanceId() {
            return this.instanceId;
        }

        public long startGameTime() {
            return this.startGameTime;
        }

        public int durationTicks() {
            return this.durationTicks;
        }

        public long lastEmissionTick() {
            return this.lastEmissionTick;
        }

        public void markEmission(long elapsed) {
            this.lastEmissionTick = elapsed;
        }
    }
}
