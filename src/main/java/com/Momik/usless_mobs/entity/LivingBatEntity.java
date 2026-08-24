package com.Momik.usless_mobs.entity;

import com.Momik.usless_mobs.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;

/** Dedicated UMR living bat; it no longer replaces every vanilla bat. */
public final class LivingBatEntity extends Bat {
    private static final int ECHO_ATTACK_COOLDOWN_TICKS = 80;

    private int echoAttackCooldown;

    public LivingBatEntity(EntityType<? extends Bat> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Bat.createAttributes()
                .add(Attributes.MAX_HEALTH, 12.0D);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (this.echoAttackCooldown > 0) {
            this.echoAttackCooldown--;
            return;
        }

        Player player = serverLevel.getNearestPlayer(this, 2.4D);
        if (player == null || player.getAbilities().instabuild) {
            return;
        }

        this.echoAttackCooldown = ECHO_ATTACK_COOLDOWN_TICKS;
        boolean night = isNight(serverLevel);
        player.hurt(this.damageSources().mobAttack(this), night ? 3.0F : 2.0F);
        player.addEffect(new MobEffectInstance(MobEffects.POISON, 45, 0));
        if (night) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 45, 0));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
        }
        serverLevel.sendParticles(
                ParticleTypes.SCULK_SOUL,
                player.getX(), player.getY(0.55D), player.getZ(),
                night ? 16 : 8, 0.25D, 0.25D, 0.25D, 0.02D);
        serverLevel.playSound(
                null,
                player.blockPosition(),
                SoundEvents.BAT_TAKEOFF,
                SoundSource.HOSTILE,
                0.8F,
                0.65F);
    }

    private static boolean isNight(ServerLevel level) {
        long time = level.getDayTime() % 24000L;
        return time >= 13000L && time <= 23000L;
    }

    @Override
    public SoundEvent getAmbientSound() {
        return ModSounds.LIVING_BAT_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.LIVING_BAT_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.LIVING_BAT_DEATH.get();
    }
}
