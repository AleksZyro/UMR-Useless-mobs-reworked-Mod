package com.Momik.usless_mobs.ability;

/**
 * Monotonic server-time estimate for client-side Frost Stray cues.
 *
 * <p>A packet may arrive late or be duplicated. Equal or older server ticks
 * therefore must never rewrite the offset, otherwise a duplicate START can
 * move the estimated clock backwards and replay already emitted cues.</p>
 */
public final class FrostStrayTimelineClock {
    private long offset;
    private long highestServerGameTime = Long.MIN_VALUE;
    private boolean initialized;

    public void observe(long packetServerGameTime, long clientGameTime) {
        if (!this.initialized || packetServerGameTime > this.highestServerGameTime) {
            this.initialized = true;
            this.highestServerGameTime = packetServerGameTime;
            this.offset = packetServerGameTime - clientGameTime;
        }
    }

    public long serverTime(long clientGameTime) {
        return clientGameTime + this.offset;
    }

    public double serverTime(double clientGameTime) {
        return clientGameTime + this.offset;
    }

    public long elapsed(long startGameTime, long clientGameTime) {
        return Math.max(0L, this.serverTime(clientGameTime) - startGameTime);
    }

    public double elapsed(double startGameTime, double clientGameTime) {
        return Math.max(0.0D, this.serverTime(clientGameTime) - startGameTime);
    }

    public long offset() {
        return this.offset;
    }

    public long highestServerGameTime() {
        return this.highestServerGameTime;
    }

    public void reset() {
        this.offset = 0L;
        this.highestServerGameTime = Long.MIN_VALUE;
        this.initialized = false;
    }
}
