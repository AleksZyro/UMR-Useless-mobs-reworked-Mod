package com.Momik.usless_mobs.entity.boss;

import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;

/**
 * Shared combat tuning for UMR's Living and Witch bosses.
 * Model scale and collision dimensions deliberately do not belong here:
 * those values come from measured runtime meshes, not game difficulty.
 */
public record BossDifficultyProfile(
        float damageMultiplier,
        float cooldownMultiplier,
        int livingSummonCap,
        int witchSpiritCount,
        int huntHoundCount,
        int rewardTier) {

    public static BossDifficultyProfile forDifficulty(Difficulty difficulty) {
        return switch (difficulty) {
            case PEACEFUL, EASY -> new BossDifficultyProfile(0.72F, 1.25F, 2, 1, 2, 0);
            case HARD -> new BossDifficultyProfile(1.30F, 0.78F, 6, 3, 4, 2);
            default -> new BossDifficultyProfile(1.00F, 1.00F, 4, 2, 3, 1);
        };
    }

    public float damage(float baseDamage) {
        return Math.max(0.0F, baseDamage) * this.damageMultiplier;
    }

    public int cooldown(int baseTicks) {
        return Math.max(20, Mth.floor(Math.max(0, baseTicks) * this.cooldownMultiplier));
    }
}
