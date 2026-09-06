package com.Momik.usless_mobs.entity;

import com.Momik.usless_mobs.Config;
import net.minecraft.world.Difficulty;

/**
 * Difficulty-dependent King Slime values.
 *
 * <p>Keeping these values together makes balancing changes easier to review and
 * prevents the entity's combat flow from being mixed with configuration rules.</p>
 */
final class KingSlimeDifficultySettings {
    private KingSlimeDifficultySettings() {
    }

    static Difficulty normalize(Difficulty difficulty) {
        return difficulty == Difficulty.PEACEFUL ? Difficulty.EASY : difficulty;
    }

    static double healthMultiplier(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> Config.easyHealthMultiplier;
            case HARD -> Config.hardHealthMultiplier;
            default -> Config.normalHealthMultiplier;
        };
    }

    static double attackMultiplier(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> Config.easyAttackMultiplier;
            case HARD -> Config.hardAttackMultiplier;
            default -> Config.normalAttackMultiplier;
        };
    }

    static double speedMultiplier(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> Config.easySpeedMultiplier;
            case HARD -> Config.hardSpeedMultiplier;
            default -> Config.normalSpeedMultiplier;
        };
    }

    static int cooldown(int baseTicks, Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> Math.max(20, Math.round(baseTicks * 1.25F));
            case HARD -> Math.max(20, Math.round(baseTicks * 0.62F));
            default -> baseTicks;
        };
    }

    static int maxNearbyMinions(Difficulty difficulty, int normalValue) {
        return switch (difficulty) {
            case EASY -> 4;
            case HARD -> 10;
            default -> normalValue;
        };
    }

    static int phase2GoldenCount(Difficulty difficulty, int normalValue) {
        return switch (difficulty) {
            case EASY -> 2;
            case HARD -> 4;
            default -> normalValue;
        };
    }

    static int phase2DurationTicks(Difficulty difficulty, int normalValue) {
        return switch (difficulty) {
            case EASY -> 160;
            case HARD -> 240;
            default -> normalValue;
        };
    }

    static float phase2DamagePerGolden(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> 0.38F;
            case HARD -> 0.24F;
            default -> 0.33F;
        };
    }

    static int slamTelegraphTicks(Difficulty difficulty, int normalValue) {
        return switch (difficulty) {
            case EASY -> normalValue + 10;
            case HARD -> Math.max(12, normalValue - 7);
            default -> normalValue;
        };
    }

    static double slamRadius(Difficulty difficulty, double normalValue) {
        return switch (difficulty) {
            case EASY -> normalValue - 0.5D;
            case HARD -> normalValue + 1.25D;
            default -> normalValue;
        };
    }

    static double shockwaveRadius(Difficulty difficulty, double normalValue) {
        return switch (difficulty) {
            case EASY -> normalValue - 0.75D;
            case HARD -> normalValue + 1.5D;
            default -> normalValue;
        };
    }

    static float maxDamagePerHit(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> 18.0F;
            case HARD -> 8.0F;
            default -> 12.0F;
        };
    }
}
