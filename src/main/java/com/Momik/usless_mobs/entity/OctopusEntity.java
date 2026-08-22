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
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
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
    private int ambushCooldown = 100;
    private int ambushWarmup = 0;
    private int objectCooldown = 80;
    private int carriedObjectTicks = 0;
    private int actionTicks = 0;
    private UUID tentacleGrabTargetId = null;
    private UUID ambushTargetId = null;

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
    public EntityDimensions getDimensions(Pose pose) {
        return isSqueezing() ? EntityDimensions.scalable(0.62F, 0.48F) : super.getDimensions(pose);
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
        tag.putInt("OctopusCarriedTicks", carriedObjectTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setActionState(tag.getByte("OctopusAction"));
        setSqueezing(tag.getBoolean("OctopusSqueezing"));
        carriedObjectTicks = tag.getInt("OctopusCarriedTicks");
        carriedObjectTicks = Math.max(0, Math.min(160, carriedObjectTicks));
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
        if (ambushCooldown > 0) {
            ambushCooldown--;
        }
        if (objectCooldown > 0) {
            objectCooldown--;
        }

        updateSqueezing();
        tickCarriedObject();
        tickTimedAction();

        if (tentacleGrabWarmup > 0) {
            tickTentacleGrab();
            return;
        }

        if (ambushWarmup > 0) {
            tickAmbush();
            return;
        }

        if (camouflageCooldown <= 0 && this.getHealth() <= this.getMaxHealth() * 0.35F) {
            beginCamouflage();
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
        } else if (grabTarget != null && grabTarget.isAlive() && grabTarget.isInWater()
                && !grabTarget.getAbilities().instabuild && ambushCooldown <= 0
                && this.distanceToSqr(grabTarget) > 4.0D * 4.0D && this.hasLineOfSight(grabTarget)
                && this.random.nextFloat() < 0.05F) {
            beginAmbush(grabTarget);
        } else if (objectCooldown <= 0) {
            interactWithNearbyObject();
        }

        if (actionTicks == 0 && tentacleGrabWarmup == 0 && ambushWarmup == 0) {
            setActionState(this.getDeltaMovement().horizontalDistanceSqr() > 0.002D
                    ? ACTION_SWIM : ACTION_IDLE);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide) {
            dropCarriedObject();
            if (inkCooldown <= 20) {
                inkCloud();
            }
        }
        return hurt;
    }

    private void beginCamouflage() {
        inkCloud();
        this.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 110, 0));
        this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 110, 1));
        camouflageCooldown = 260;
        startTimedAction(ACTION_CAMOUFLAGE, 110);
    }

    private void beginAmbush(Player target) {
        this.ambushTargetId = target.getUUID();
        this.ambushWarmup = 36 + this.random.nextInt(10);
        this.ambushCooldown = 180;
        this.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, this.ambushWarmup, 0));
        setActionState(ACTION_AMBUSH);
    }

    private void tickAmbush() {
        if (!(this.level() instanceof ServerLevel serverLevel) || this.ambushTargetId == null) {
            cancelAmbush();
            return;
        }
        Player target = serverLevel.getPlayerByUUID(this.ambushTargetId);
        if (target == null || !target.isAlive() || !target.isInWater()
                || this.distanceToSqr(target) > 10.0D * 10.0D || !this.hasLineOfSight(target)) {
            cancelAmbush();
            return;
        }
        this.ambushWarmup--;
        if (this.ambushWarmup <= 0) {
            this.removeEffect(MobEffects.INVISIBILITY);
            Vec3 direction = target.position().add(0.0D, target.getBbHeight() * 0.45D, 0.0D)
                    .subtract(this.position()).normalize();
            this.setDeltaMovement(this.getDeltaMovement().scale(0.2D).add(direction.scale(0.92D)));
            this.ambushTargetId = null;
            startTimedAction(ACTION_SWIM, 18);
        }
    }

    private void cancelAmbush() {
        this.ambushWarmup = 0;
        this.ambushTargetId = null;
        this.removeEffect(MobEffects.INVISIBILITY);
        setActionState(ACTION_IDLE);
    }

    private void updateSqueezing() {
        Vec3 look = this.getLookAngle();
        Vec3 probeCenter = this.position().add(look.x * 0.45D, 0.24D, look.z * 0.45D);
        AABB squeezedProbe = AABB.ofSize(probeCenter, 0.62D, 0.48D, 0.62D);
        AABB normalProbe = AABB.ofSize(this.position().add(0.0D, 0.60D, 0.0D),
                1.15D, 1.20D, 1.15D);
        boolean squeeze = isSqueezing()
                ? !this.level().noCollision(this, normalProbe)
                : this.isInWater() && this.horizontalCollision
                        && this.level().noCollision(this, squeezedProbe);
        if (squeeze != isSqueezing()) {
            setSqueezing(squeeze);
            refreshDimensions();
        }
    }

    private void interactWithNearbyObject() {
        objectCooldown = 200;
        if (!this.getMainHandItem().isEmpty()) {
            return;
        }
        ItemEntity item = this.level().getEntitiesOfClass(ItemEntity.class,
                        this.getBoundingBox().inflate(3.0D),
                        candidate -> candidate.isAlive() && !candidate.getItem().isEmpty())
                .stream().findFirst().orElse(null);
        if (item == null) {
            return;
        }
        ItemStack carried = item.getItem().split(1);
        this.setItemSlot(EquipmentSlot.MAINHAND, carried);
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        if (item.getItem().isEmpty()) {
            item.discard();
        }
        carriedObjectTicks = 160;
        startTimedAction(ACTION_OBJECT, 30);
    }

    private void tickCarriedObject() {
        if (this.getMainHandItem().isEmpty()) {
            carriedObjectTicks = 0;
            return;
        }
        if (carriedObjectTicks > 0 && --carriedObjectTicks == 0) {
            dropCarriedObject();
            objectCooldown = 120;
        }
    }

    private void dropCarriedObject() {
        ItemStack carried = this.getMainHandItem();
        if (carried.isEmpty()) {
            return;
        }
        this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        this.spawnAtLocation(carried.copy());
        carriedObjectTicks = 0;
    }

    private void startTimedAction(byte action, int ticks) {
        setActionState(action);
        actionTicks = Math.max(1, ticks);
    }

    private void tickTimedAction() {
        if (actionTicks > 0) {
            actionTicks--;
        }
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
        startTimedAction(ACTION_INK, 18);
    }

    private void startTentacleGrab(Player target) {
        this.tentacleGrabTargetId = target.getUUID();
        this.tentacleGrabWarmup = 22;
        this.tentacleGrabCooldown = this.level().getDifficulty() == net.minecraft.world.Difficulty.HARD ? 125 : 165;
        setActionState(ACTION_GRAB);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.HOSTILE, 0.8F, 0.55F);
        }
    }

    private void tickTentacleGrab() {
        if (!(this.level() instanceof ServerLevel serverLevel) || this.tentacleGrabTargetId == null) {
            this.tentacleGrabWarmup = 0;
            this.tentacleGrabTargetId = null;
            setActionState(ACTION_IDLE);
            return;
        }

        Player target = serverLevel.getPlayerByUUID(this.tentacleGrabTargetId);
        if (target == null || !target.isAlive() || !target.isInWater() || this.distanceToSqr(target) > 10.0D * 10.0D) {
            this.tentacleGrabWarmup = 0;
            this.tentacleGrabTargetId = null;
            setActionState(ACTION_IDLE);
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
            startTimedAction(ACTION_SWIM, 12);
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
        dropCarriedObject();
        super.dropCustomDeathLoot(damageSource, looting, recentlyHit);
        int safeLooting = Math.min(5, Math.max(0, looting));
        this.spawnAtLocation(new ItemStack(com.Momik.usless_mobs.registry.ModItems.TENTACLE.get(), 1 + this.random.nextInt(2 + safeLooting)));
    }
}
