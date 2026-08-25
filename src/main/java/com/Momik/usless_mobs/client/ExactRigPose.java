package com.Momik.usless_mobs.client;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/** Reusable primitive scratch state shared by exact-mesh pose calculations. */
final class ExactRigPose {
    private float walkCos;
    private float rootSin;
    private boolean inWater;
    private boolean onGround;
    private boolean sprinting;

    void updateFor(CustomMob3DModel.Variant variant, LivingEntity entity,
                   float limbSwing, float limbSwingAmount, float ageInTicks,
                   float netHeadYaw, float headPitch) {
        if (variant == null || entity == null) {
            throw new IllegalArgumentException("variant and entity are required");
        }
        float walk = limbSwing * 0.6662F;
        float rootSpeed = switch (variant) {
            case WEB_CAVE_SPIDER -> 0.20F;
            case OCTOPUS -> 0.08F;
            case SQUID -> 0.12F;
            case GLOW_SQUID -> 0.14F;
            case WITCH_BOSS, CORAL_DROWNED -> 0.10F;
            case LIVING_BAT, AXOLOTL -> 0.16F;
            case ROOTED_HUSK -> 0.08F;
            case LIVING_BOSS, FROST_STRAY, POLAR_BEAR, OCELOT -> 0.0F;
        };
        this.walkCos = Mth.cos(walk);
        this.rootSin = Mth.sin(ageInTicks * rootSpeed);
        this.inWater = entity.isInWater();
        this.onGround = entity.onGround();
        this.sprinting = entity.isSprinting();
    }

    float walkCos() {
        return this.walkCos;
    }

    float rootSin() {
        return this.rootSin;
    }

    boolean inWater() {
        return this.inWater;
    }

    boolean onGround() {
        return this.onGround;
    }

    boolean sprinting() {
        return this.sprinting;
    }
}
