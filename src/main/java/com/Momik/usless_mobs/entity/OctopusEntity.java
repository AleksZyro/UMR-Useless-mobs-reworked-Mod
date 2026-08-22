package com.Momik.usless_mobs.entity;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class OctopusEntity extends Squid {
    public static final byte ACTION_IDLE = 0;
    public static final byte ACTION_SWIM = 1;
    public static final byte ACTION_AMBUSH = 2;
    public static final byte ACTION_GRAB = 3;
    public static final byte ACTION_INK = 4;
    public static final byte ACTION_CAMOUFLAGE = 5;
    public static final byte ACTION_OBJECT = 6;
    private static final byte MAX_ACTION_STATE = ACTION_OBJECT;
    private static final EntityDataAccessor<Byte> ACTION_STATE =
            SynchedEntityData.defineId(OctopusEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> SQUEEZING =
            SynchedEntityData.defineId(OctopusEntity.class, EntityDataSerializers.BOOLEAN);

    private int biteCooldown = 40;
    private int inkCooldown = 80;
    private int camouflageCooldown = 180;
    private int tentacleGrabCooldown = 100;
    private int tentacleGrabWarmup = 0;
    private UUID tentacleGrabTargetId = null;

    public OctopusEntity(EntityType<? extends Squid> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return net.minecraft.world.entity.Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 18.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.9D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.15D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ACTION_STATE, ACTION_IDLE);
        this.entityData.define(SQUEEZING, false);
    }

    public byte getActionState() {
        return this.entityData.get(ACTION_STATE);
    }

    public void setActionState(byte action) {
        this.entityData.set(ACTION_STATE,
                action >= ACTION_IDLE && action <= MAX_ACTION_STATE ? action : ACTION_IDLE);
    }

    public boolean isSqueezing() {
        return this.entityData.get(SQUEEZING);
    }

    private void setSqueezing(boolean squeezing) {
        this.entityData.set(SQUEEZING, squeezing);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("OctopusAction", getActionState());
        tag.putBoolean("OctopusSqueezing", isSqueezing());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setActionState(tag.getByte("OctopusAction"));
        setSqueezing(tag.getBoolean("OctopusSqueezing"));
        refreshDimensions();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }

        if (biteCooldown > 0) {
            biteCooldown--;
        }
        if (inkCooldown > 0) {
            inkCooldown--;
        }
        if (camouflageCooldown > 0) {
            camouflageCooldown--;
        }
        if (tentacleGrabCooldown > 0) {
            tentacleGrabCooldown--;
        }

        if (tentacleGrabWarmup > 0) {
            tickTentacleGrab();
            return;
        }

        if (camouflageCooldown <= 0 && this.getHealth() <= this.getMaxHealth() * 0.35F) {
            this.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 110, 0));
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 110, 1));
            camouflageCooldown = 260;
            inkCloud();
        }

        Player player = this.level().getNearestPlayer(this, 3.2D);
        if (player != null && player.isAlive() && !player.getAbilities().instabuild && biteCooldown <= 0) {
            player.hurt(this.damageSources().mobAttack(this), this.level().getDifficulty().getId() >= 2 ? 4.0F : 2.5F);
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 1));
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 90, 0));
            biteCooldown = 45;
            inkCloud();
        } else if (inkCooldown <= 0 && this.level().random.nextFloat() < 0.05F) {
            inkCloud();
        }

        Player grabTarget = this.level().getNearestPlayer(this, 7.5D);
        if (grabTarget != null && grabTarget.isAlive() && grabTarget.isInWater()
                && !grabTarget.getAbilities().instabuild && tentacleGrabCooldown <= 0
                && this.distanceToSqr(grabTarget) > 3.4D * 3.4D && this.hasLineOfSight(grabTarget)) {
            startTentacleGrab(grabTarget);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide && inkCooldown <= 20) {
            inkCloud();
        }
        return hurt;
    }

    private void inkCloud() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        AABB area = this.getBoundingBox().inflate(4.5D);
        for (LivingEntity living : this.level().getEntitiesOfClass(LivingEntity.class, area,
                living -> living.isAlive() && living != this && !(living instanceof Squid))) {
            living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
            if (living instanceof Mob mob && mob.getTarget() == this) {
                mob.setTarget(null);
            }
        }
        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SQUID_INK,
                this.getX(), this.getY(0.5D), this.getZ(),
                44, 0.75D, 0.45D, 0.75D, 0.08D);
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.SQUID_SQUIRT, SoundSource.HOSTILE, 1.0F, 0.75F);
        inkCooldown = 120;
    }

    private void startTentacleGrab(Player target) {
        this.tentacleGrabTargetId = target.getUUID();
        this.tentacleGrabWarmup = 22;
        this.tentacleGrabCooldown = this.level().getDifficulty() == net.minecraft.world.Difficulty.HARD ? 125 : 165;
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.HOSTILE, 0.8F, 0.55F);
        }
    }

    private void tickTentacleGrab() {
        if (!(this.level() instanceof ServerLevel serverLevel) || this.tentacleGrabTargetId == null) {
            this.tentacleGrabWarmup = 0;
            this.tentacleGrabTargetId = null;
            return;
        }

        Player target = serverLevel.getPlayerByUUID(this.tentacleGrabTargetId);
        if (target == null || !target.isAlive() || !target.isInWater() || this.distanceToSqr(target) > 10.0D * 10.0D) {
            this.tentacleGrabWarmup = 0;
            this.tentacleGrabTargetId = null;
            return;
        }

        if (this.tentacleGrabWarmup % 4 == 0) {
            Vec3 start = this.position().add(0.0D, this.getBbHeight() * 0.45D, 0.0D);
            Vec3 end = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            Vec3 step = end.subtract(start).scale(1.0D / 8.0D);
            for (int i = 1; i <= 8; i++) {
                Vec3 point = start.add(step.scale(i));
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.BUBBLE,
                        point.x, point.y, point.z, 2, 0.04D, 0.04D, 0.04D, 0.01D);
            }
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SQUID_INK,
                    target.getX(), target.getY(0.55D), target.getZ(), 4, 0.18D, 0.18D, 0.18D, 0.02D);
        }

        this.tentacleGrabWarmup--;
        if (this.tentacleGrabWarmup <= 0) {
            releaseTentacleGrab(target, serverLevel);
            this.tentacleGrabTargetId = null;
        }
    }

    private void releaseTentacleGrab(Player target, ServerLevel serverLevel) {
        Vec3 pull = this.position().subtract(target.position());
        if (pull.lengthSqr() > 0.01D) {
            Vec3 direction = pull.normalize();
            target.setDeltaMovement(target.getDeltaMovement().add(direction.x * 1.05D, 0.18D, direction.z * 1.05D));
            target.hurtMarked = true;
        }
        target.hurt(this.damageSources().mobAttack(this), this.level().getDifficulty() == net.minecraft.world.Difficulty.HARD ? 5.0F : 3.5F);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 95, 2));
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 110, 0));
        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SQUID_INK,
                target.getX(), target.getY(0.6D), target.getZ(), 26, 0.45D, 0.35D, 0.45D, 0.05D);
        serverLevel.playSound(null, target.blockPosition(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.HOSTILE, 1.0F, 0.65F);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource damageSource, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(damageSource, looting, recentlyHit);
        int safeLooting = Math.min(5, Math.max(0, looting));
        this.spawnAtLocation(new ItemStack(com.Momik.usless_mobs.registry.ModItems.TENTACLE.get(), 1 + this.random.nextInt(2 + safeLooting)));
    }
}
