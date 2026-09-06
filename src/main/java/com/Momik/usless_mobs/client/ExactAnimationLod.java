package com.Momik.usless_mobs.client;

import net.minecraft.util.Mth;

enum ExactAnimationLod {
    NEAR,
    MID,
    FAR;

    static final double NEAR_DISTANCE_SQUARED = 144.0D;
    static final double MID_DISTANCE_SQUARED = 576.0D;

    static ExactAnimationLod at(double distanceSquared) {
        if (!Double.isFinite(distanceSquared) || distanceSquared < 0.0D) {
            throw new IllegalArgumentException("distanceSquared must be finite and non-negative");
        }
        if (distanceSquared <= NEAR_DISTANCE_SQUARED) {
            return NEAR;
        }
        if (distanceSquared <= MID_DISTANCE_SQUARED) {
            return MID;
        }
        return FAR;
    }

    float quantizedAge(float ageInTicks) {
        return this == MID ? Mth.floor(ageInTicks * 0.5F) * 2.0F : ageInTicks;
    }
}
