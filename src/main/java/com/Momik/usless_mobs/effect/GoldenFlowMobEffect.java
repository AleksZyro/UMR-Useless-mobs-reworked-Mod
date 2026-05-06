package com.Momik.usless_mobs.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class GoldenFlowMobEffect extends MobEffect {

    public GoldenFlowMobEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xE0BE45);
        this.addAttributeModifier(Attributes.LUCK, "f272d7aa-c2f0-4b4c-b7d1-4025958ba53d", 1.0D, AttributeModifier.Operation.ADDITION);
        this.addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, "f477a8c2-91fe-4c34-8717-9ce65c25ce19", 0.15D, AttributeModifier.Operation.ADDITION);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, "a8a20c9d-9916-4a50-b8f6-2332d20bcb52", 0.04D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public void applyEffectTick(LivingEntity livingEntity, int amplifier) {
        int healInterval = Math.max(12, 40 - (amplifier * 10));

        if (livingEntity.tickCount % healInterval == 0 && livingEntity.getHealth() < livingEntity.getMaxHealth()) {
            livingEntity.heal(1.0F + (0.5F * amplifier));
        }

        if (livingEntity.isOnFire()) {
            livingEntity.clearFire();
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}
