package com.Momik.usless_mobs.ability;

import java.util.UUID;

/**
 * Server-authoritative description of one Frost Stray volley. The UUID identifies one
 * execution, while the entity UUID prevents an entity-id reuse from becoming ambiguous.
 */
public record FrostStrayAbilityTimeline(UUID instanceId, UUID entityId,
                                        long startGameTime, int durationTicks) {
    public FrostStrayAbilityTimeline {
        if (instanceId == null || entityId == null) {
            throw new IllegalArgumentException("instanceId and entityId are required");
        }
        if (startGameTime < 0L || durationTicks <= 0) {
            throw new IllegalArgumentException("startGameTime must be non-negative and durationTicks positive");
        }
    }

    public int elapsedAt(long serverGameTime) {
        if (serverGameTime <= this.startGameTime) {
            return 0;
        }
        long elapsed = serverGameTime - this.startGameTime;
        return elapsed >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) elapsed;
    }

    public boolean isActiveAt(long serverGameTime) {
        return this.elapsedAt(serverGameTime) < this.durationTicks;
    }
}
