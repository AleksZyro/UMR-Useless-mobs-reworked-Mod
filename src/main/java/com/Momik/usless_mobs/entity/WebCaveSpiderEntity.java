package com.Momik.usless_mobs.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class WebCaveSpiderEntity extends CaveSpider {
    private int webShotCooldown = 50;
    private int webTrapCooldown = 95;

    public WebCaveSpiderEntity(EntityType<? extends CaveSpider> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 9;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return CaveSpider.createAttributes()
                .add(Attributes.MAX_HEALTH, 22.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }
        if (this.horizontalCollision && this.getTarget() != null && this.tickCount % 20 == 0) {
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 35, 1));
        }
        if (this.webShotCooldown > 0) {
            this.webShotCooldown--;
        }
        if (this.webTrapCooldown > 0) {
            this.webTrapCooldown--;
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || this.distanceToSqr(target) > 12.0D * 12.0D || !this.hasLineOfSight(target)) {
            return;
        }

        if (this.webTrapCooldown <= 0 && this.distanceToSqr(target) >= 3.0D * 3.0D) {
            createWebTrap(target);
            this.webTrapCooldown = this.level().getDifficulty() == net.minecraft.world.Difficulty.HARD ? 145 : 185;
            return;
        }

        if (this.webShotCooldown > 0) {
            return;
        }

        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 3));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 90, 0));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 1));
        if (target instanceof Player player) {
            player.setSprinting(false);
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ITEM_SLIME,
                    target.getX(), target.getY(0.6D), target.getZ(),
                    18, 0.35D, 0.35D, 0.35D, 0.03D);
            serverLevel.playSound(null, target.blockPosition(), SoundEvents.SLIME_BLOCK_PLACE, SoundSource.HOSTILE, 1.0F, 0.8F);
        }
        this.webShotCooldown = this.level().getDifficulty() == net.minecraft.world.Difficulty.HARD ? 90 : 120;
    }

    private void createWebTrap(LivingEntity target) {
        AreaEffectCloud cloud = new AreaEffectCloud(this.level(), target.getX(), target.getY(), target.getZ());
        cloud.setOwner(this);
        cloud.setRadius(2.45F);
        cloud.setRadiusPerTick(-0.012F);
        cloud.setWaitTime(14);
        cloud.setDuration(125);
        cloud.setParticle(ParticleTypes.ITEM_SLIME);
        cloud.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 130, 3));
        cloud.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 115, 1));
        cloud.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
        this.level().addFreshEntity(cloud);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ITEM_SLIME,
                    target.getX(), target.getY(0.15D), target.getZ(),
                    30, 1.05D, 0.08D, 1.05D, 0.02D);
            serverLevel.playSound(null, target.blockPosition(), SoundEvents.SLIME_BLOCK_PLACE, SoundSource.HOSTILE, 1.0F, 0.9F);
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource damageSource, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(damageSource, looting, recentlyHit);
        int safeLooting = Math.max(0, looting);
        this.spawnAtLocation(new ItemStack(com.Momik.usless_mobs.registry.ModItems.LIVING_TISSUE.get(), 1 + this.random.nextInt(1 + Math.max(1, safeLooting + 1))));
    }
}
