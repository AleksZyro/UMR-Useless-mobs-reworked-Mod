package com.Momik.usless_mobs.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class ElasticityMobEffect extends MobEffect {

    public ElasticityMobEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x3D7DFF);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, "b4c1df64-a0e7-4d9f-bd46-3b71ccf0a188", 0.08D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public void applyEffectTick(LivingEntity livingEntity, int amplifier) {
        livingEntity.fallDistance = Math.max(0.0F, livingEntity.fallDistance - (1.5F + amplifier));

        if (livingEntity.onGround() && livingEntity.tickCount % 10 == 0) {
            Vec3 movement = livingEntity.getDeltaMovement();

            if (movement.horizontalDistanceSqr() > 0.01D) {
                double horizontalBoost = 1.03D + (0.02D * amplifier);
                livingEntity.setDeltaMovement(movement.x * horizontalBoost, movement.y, movement.z * horizontalBoost);
            }
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}
