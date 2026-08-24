package com.Momik.usless_mobs.entity;

import com.Momik.usless_mobs.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

/** Dedicated UMR rooted husk; vanilla husks remain visually unchanged. */
public final class RootedHuskEntity extends Husk {
    public RootedHuskEntity(EntityType<? extends Husk> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 32.0D)
                .add(Attributes.ARMOR, 5.0D);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (!hurt || !(target instanceof LivingEntity living)) {
            return hurt;
        }

        living.addEffect(new MobEffectInstance(MobEffects.HUNGER, 180, 1), this);
        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 0), this);
        if (this.getHealth() < this.getMaxHealth()) {
            this.heal(1.0F);
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.SPORE_BLOSSOM_AIR,
                    living.getX(), living.getY(0.65D), living.getZ(),
                    10, 0.3D, 0.3D, 0.3D, 0.01D);
            serverLevel.playSound(
                    null,
                    living.blockPosition(),
                    SoundEvents.ROOTED_DIRT_BREAK,
                    SoundSource.HOSTILE,
                    0.8F,
                    0.75F);
        }
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ROOTED_HUSK_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ROOTED_HUSK_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ROOTED_HUSK_DEATH.get();
    }
}
