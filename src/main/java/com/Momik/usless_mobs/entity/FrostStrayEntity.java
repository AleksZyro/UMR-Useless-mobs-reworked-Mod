package com.Momik.usless_mobs.entity;

import com.Momik.usless_mobs.ability.FrostStrayAbilityTimeline;
import com.Momik.usless_mobs.network.FrostStrayAbilityPacket;
import com.Momik.usless_mobs.network.FrostStrayImpactPacket;
import com.Momik.usless_mobs.network.ModNetwork;
import com.Momik.usless_mobs.registry.ModSounds;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
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
    private static final EntityDataAccessor<Boolean> ICE_VOLLEY_ACTIVE = SynchedEntityData.defineId(
            FrostStrayEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Long> ICE_VOLLEY_START_TIME = SynchedEntityData.defineId(
            FrostStrayEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> ICE_VOLLEY_DURATION = SynchedEntityData.defineId(
            FrostStrayEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> ICE_VOLLEY_INSTANCE_ID = SynchedEntityData.defineId(
            FrostStrayEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private int iceVolleyCooldown = 110;
    private UUID iceVolleyTargetId = null;
    private FrostStrayAbilityTimeline iceVolleyTimeline;

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
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ICE_VOLLEY_ACTIVE, false);
        this.entityData.define(ICE_VOLLEY_START_TIME, 0L);
        this.entityData.define(ICE_VOLLEY_DURATION, 0);
        this.entityData.define(ICE_VOLLEY_INSTANCE_ID, Optional.empty());
    }

    public boolean isIceVolleyActive() {
        return this.entityData.get(ICE_VOLLEY_ACTIVE);
    }

    public long iceVolleyStartTime() {
        return this.entityData.get(ICE_VOLLEY_START_TIME);
    }

    public int iceVolleyDurationTicks() {
        return this.entityData.get(ICE_VOLLEY_DURATION);
    }

    /**
     * Server-synced fallback for non-render callers. Client render code uses
     * FrostStrayAbilityClient.progressFor so it shares the packet clock with VFX.
     */
    public float iceVolleyProgress(float partialTick) {
        if (!this.isIceVolleyActive() || this.level() == null) {
            return 0.0F;
        }
        int duration = this.iceVolleyDurationTicks();
        if (duration <= 0) {
            return 0.0F;
        }
        float elapsed = this.level().getGameTime() - this.iceVolleyStartTime() + partialTick;
        return Math.max(0.0F, Math.min(1.0F, elapsed / duration));
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

        if (this.iceVolleyCooldown > 0) {
            this.iceVolleyCooldown--;
        }
        if (this.iceVolleyTimeline != null) {
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
        this.iceVolleyTimeline = new FrostStrayAbilityTimeline(UUID.randomUUID(), this.getUUID(),
                serverLevel.getGameTime(), ICE_VOLLEY_WARMUP_TICKS);
        this.entityData.set(ICE_VOLLEY_ACTIVE, true);
        this.entityData.set(ICE_VOLLEY_START_TIME, this.iceVolleyTimeline.startGameTime());
        this.entityData.set(ICE_VOLLEY_DURATION, this.iceVolleyTimeline.durationTicks());
        this.entityData.set(ICE_VOLLEY_INSTANCE_ID, Optional.of(this.iceVolleyTimeline.instanceId()));
        this.iceVolleyCooldown = this.level().getDifficulty() == net.minecraft.world.Difficulty.HARD ? 150 : 195;
        broadcastTimeline(FrostStrayAbilityPacket.Type.START, serverLevel);
    }

    private void tickIceVolley(ServerLevel serverLevel) {
        if (this.iceVolleyTimeline == null || this.iceVolleyTargetId == null) {
            cancelIceVolley(serverLevel);
            return;
        }

        net.minecraft.world.entity.Entity entity = serverLevel.getEntity(this.iceVolleyTargetId);
        if (!(entity instanceof LivingEntity target) || !target.isAlive() || this.distanceToSqr(target) > 28.0D * 28.0D) {
            cancelIceVolley(serverLevel);
            return;
        }
        if (!this.iceVolleyTimeline.isActiveAt(serverLevel.getGameTime())) {
            shootIceVolley(target, serverLevel);
            broadcastTimeline(FrostStrayAbilityPacket.Type.RELEASE, serverLevel);
            clearIceVolley();
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
            FrostStrayVolleyArrow arrow = new FrostStrayVolleyArrow(this.level(), this,
                    this.iceVolleyTimeline.instanceId());
            arrow.setPos(origin.x, origin.y, origin.z);
            arrow.setBaseDamage(arrow.getBaseDamage() + 1.0D);
            arrow.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 180, 2));
            arrow.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
            Vec3 direction = aim.add(side.scale(offset)).normalize();
            arrow.shoot(direction.x, direction.y + 0.03D, direction.z, 1.75F, 0.75F);
            this.level().addFreshEntity(arrow);
        }
    }

    public void syncActiveVolleyTo(ServerPlayer player) {
        if (this.iceVolleyTimeline != null && this.level() instanceof ServerLevel serverLevel
                && this.iceVolleyTimeline.isActiveAt(serverLevel.getGameTime())) {
            ModNetwork.sendToPlayer(player, new FrostStrayAbilityPacket(
                    FrostStrayAbilityPacket.Type.START, this.getId(), this.iceVolleyTimeline.instanceId(),
                    this.iceVolleyTimeline.startGameTime(), serverLevel.getGameTime(),
                    this.iceVolleyTimeline.durationTicks()));
        }
    }

    void onVolleyProjectileImpact(UUID abilityInstanceId, Vec3 impactPosition) {
        if (this.level() instanceof ServerLevel serverLevel) {
            ModNetwork.sendToTrackingAndSelf(this, new FrostStrayImpactPacket(this.getId(), abilityInstanceId,
                    UUID.randomUUID(), impactPosition.x, impactPosition.y, impactPosition.z,
                    serverLevel.getGameTime()));
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && this.level() instanceof ServerLevel serverLevel && this.iceVolleyTimeline != null) {
            cancelIceVolley(serverLevel);
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        if (this.level() instanceof ServerLevel serverLevel) {
            cancelIceVolley(serverLevel);
        }
        super.die(source);
    }

    private void cancelIceVolley(ServerLevel serverLevel) {
        if (this.iceVolleyTimeline != null) {
            broadcastTimeline(FrostStrayAbilityPacket.Type.CANCEL, serverLevel);
        }
        clearIceVolley();
        this.iceVolleyTargetId = null;
    }

    private void clearIceVolley() {
        this.iceVolleyTimeline = null;
        this.entityData.set(ICE_VOLLEY_ACTIVE, false);
        this.entityData.set(ICE_VOLLEY_START_TIME, 0L);
        this.entityData.set(ICE_VOLLEY_DURATION, 0);
        this.entityData.set(ICE_VOLLEY_INSTANCE_ID, Optional.empty());
    }

    private void broadcastTimeline(FrostStrayAbilityPacket.Type type, ServerLevel serverLevel) {
        if (this.iceVolleyTimeline != null) {
            ModNetwork.sendToTrackingAndSelf(this, new FrostStrayAbilityPacket(type, this.getId(),
                    this.iceVolleyTimeline.instanceId(), this.iceVolleyTimeline.startGameTime(),
                    serverLevel.getGameTime(), this.iceVolleyTimeline.durationTicks()));
        }
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
    public SoundEvent getAmbientSound() {
        return ModSounds.FROST_STRAY_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.FROST_STRAY_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.FROST_STRAY_DEATH.get();
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
