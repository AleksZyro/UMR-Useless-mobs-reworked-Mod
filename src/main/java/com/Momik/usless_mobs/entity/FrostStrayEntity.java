package com.Momik.usless_mobs.entity;

import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class FrostStrayEntity extends Stray {
    private static final int ICE_VOLLEY_WARMUP_TICKS = 18;
    private int iceVolleyCooldown = 110;
    private int iceVolleyWarmup = 0;
    private UUID iceVolleyTargetId = null;

    public FrostStrayEntity(EntityType<? extends Stray> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 34.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 36.0D);
    }

    @Override
    protected AbstractArrow getArrow(ItemStack arrowStack, float velocity) {
        AbstractArrow arrow = super.getArrow(arrowStack, velocity);
        arrow.setBaseDamage(arrow.getBaseDamage() + 1.5D);
        if (arrow instanceof Arrow tippedArrow) {
            tippedArrow.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 220, 2));
            tippedArrow.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0));
        }
        return arrow;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (this.tickCount % 12 == 0) {
            serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                    this.getX(), this.getY(0.75D), this.getZ(),
                    4, 0.25D, 0.45D, 0.25D, 0.01D);
        }
        if (this.iceVolleyCooldown > 0) {
            this.iceVolleyCooldown--;
        }
        if (this.iceVolleyWarmup > 0) {
            tickIceVolley(serverLevel);
            return;
        }

        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive() && this.iceVolleyCooldown <= 0
                && this.distanceToSqr(target) >= 6.0D * 6.0D && this.distanceToSqr(target) <= 25.0D * 25.0D
                && this.hasLineOfSight(target)) {
            startIceVolley(target, serverLevel);
        }
    }

    private void startIceVolley(LivingEntity target, ServerLevel serverLevel) {
        this.iceVolleyTargetId = target.getUUID();
        this.iceVolleyWarmup = ICE_VOLLEY_WARMUP_TICKS;
        this.iceVolleyCooldown = this.level().getDifficulty() == net.minecraft.world.Difficulty.HARD ? 150 : 195;
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.POWDER_SNOW_STEP, SoundSource.HOSTILE, 1.0F, 0.7F);
    }

    private void tickIceVolley(ServerLevel serverLevel) {
        if (this.iceVolleyTargetId == null) {
            this.iceVolleyWarmup = 0;
            return;
        }

        net.minecraft.world.entity.Entity entity = serverLevel.getEntity(this.iceVolleyTargetId);
        if (!(entity instanceof LivingEntity target) || !target.isAlive() || this.distanceToSqr(target) > 28.0D * 28.0D) {
            this.iceVolleyWarmup = 0;
            this.iceVolleyTargetId = null;
            return;
        }

        if (this.iceVolleyWarmup % 3 == 0) {
            serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                    target.getX(), target.getY(0.9D), target.getZ(),
                    9, 0.45D, 0.6D, 0.45D, 0.02D);
            serverLevel.sendParticles(ParticleTypes.ITEM_SNOWBALL,
                    this.getX(), this.getEyeY(), this.getZ(),
                    5, 0.18D, 0.18D, 0.18D, 0.02D);
        }

        this.iceVolleyWarmup--;
        if (this.iceVolleyWarmup <= 0) {
            shootIceVolley(target, serverLevel);
            this.iceVolleyTargetId = null;
        }
    }

    private void shootIceVolley(LivingEntity target, ServerLevel serverLevel) {
        Vec3 origin = new Vec3(this.getX(), this.getEyeY() - 0.1D, this.getZ());
        Vec3 aim = target.getEyePosition().subtract(origin).normalize();
        Vec3 side = aim.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 0.01D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            side = side.normalize();
        }

        int arrows = this.level().getDifficulty() == net.minecraft.world.Difficulty.HARD ? 5 : 3;
        double center = (arrows - 1) / 2.0D;
        for (int i = 0; i < arrows; i++) {
            double offset = (i - center) * 0.13D;
            Arrow arrow = new Arrow(this.level(), this);
            arrow.setPos(origin.x, origin.y, origin.z);
            arrow.setBaseDamage(arrow.getBaseDamage() + 1.0D);
            arrow.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 180, 2));
            arrow.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
            Vec3 direction = aim.add(side.scale(offset)).normalize();
            arrow.shoot(direction.x, direction.y + 0.03D, direction.z, 1.75F, 0.75F);
            this.level().addFreshEntity(arrow);
        }

        serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                this.getX(), this.getEyeY(), this.getZ(),
                18, 0.35D, 0.25D, 0.35D, 0.04D);
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.SKELETON_SHOOT, SoundSource.HOSTILE, 1.0F, 0.65F);
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt && target instanceof net.minecraft.world.entity.LivingEntity living) {
            living.setTicksFrozen(Math.max(living.getTicksFrozen(), 180));
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
            this.playSound(SoundEvents.PLAYER_HURT_FREEZE, 0.8F, 0.8F);
        }
        return hurt;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource damageSource, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(damageSource, looting, recentlyHit);
        int safeLooting = Math.max(0, looting);
        if (this.random.nextFloat() < Math.min(0.75F, 0.35F + safeLooting * 0.12F)) {
            this.spawnAtLocation(new ItemStack(com.Momik.usless_mobs.registry.ModItems.FROST_CORE.get()));
        }
        if (this.random.nextFloat() < Math.min(0.85F, 0.45F + safeLooting * 0.10F)) {
            this.spawnAtLocation(new ItemStack(com.Momik.usless_mobs.registry.ModItems.ICE_ARROW.get(), 2 + this.random.nextInt(3)));
        }
    }
}
