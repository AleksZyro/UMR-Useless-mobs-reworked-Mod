package com.Momik.usless_mobs.entity;

import com.Momik.usless_mobs.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.level.Level;

/** Dedicated UMR polar bear; vanilla polar bears keep their vanilla model. */
public final class LivingPolarBearEntity extends PolarBear {
    public LivingPolarBearEntity(EntityType<? extends PolarBear> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PolarBear.createAttributes()
                .add(Attributes.MAX_HEALTH, 36.0D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.POLAR_BEAR_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.POLAR_BEAR_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.POLAR_BEAR_DEATH.get();
    }
}
