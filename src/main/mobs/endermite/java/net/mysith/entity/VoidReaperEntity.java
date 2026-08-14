package net.mysith.entity;

import com.Momik.usless_mobs.allegiance.AllegiancePath;
import com.Momik.usless_mobs.allegiance.AllegianceUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.mysith.registry.ModItems;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class VoidReaperEntity extends Zombie implements GeoEntity {
    private static final EntityDataAccessor<Integer> REAPER_ACTION =
            SynchedEntityData.defineId(VoidReaperEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> REAPER_ACTION_TICKS =
            SynchedEntityData.defineId(VoidReaperEntity.class, EntityDataSerializers.INT);
    private static final int ACTION_IDLE = 0;
    private static final int ACTION_CLEAVE = 1;
    private static final int ACTION_RIFT = 2;
    private static final int ACTION_DEATH_MARK = 3;
    private static final int ACTION_SNARE = 4;
    private static final int ACTION_SIPHON = 5;
    private static final int ACTION_SHADOW_STEP = 6;
    private static final DustParticleOptions VOID_RIFT_DUST =
            new DustParticleOptions(new Vector3f(0.18F, 0.02F, 0.32F), 1.5F);
    private static final double MAX_HEALTH = 180.0D;
    private static final double ATTACK_DAMAGE = 14.0D;
    private static final double MOVEMENT_SPEED = 0.32D;
    private static final double KNOCKBACK_RESISTANCE = 0.90D;
    private static final double FOLLOW_RANGE = 42.0D;
    private static final int SHADOW_STEP_COOLDOWN = 150;
    private static final int VOID_CLEAVE_COOLDOWN = 95;
    private static final int SOUL_SNARE_COOLDOWN = 130;
    private static final int SOUL_SIPHON_COOLDOWN = 220;
    private static final int VOID_RIFT_COOLDOWN = 175;
    private static final int VOID_RIFT_WARMUP_TICKS = 24;
    private static final int SCYTHE_RETURN_DELAY_TICKS = 12;
    private static final int DEATH_MARK_COOLDOWN = 260;
    private static final int DEATH_MARK_WARMUP_TICKS = 34;
    private static final double VOID_CLEAVE_RADIUS = 4.6D;
    private static final double SOUL_SNARE_RANGE_SQR = 16.0D * 16.0D;
    private static final double VOID_RIFT_LENGTH = 8.5D;
    private static final double VOID_RIFT_HALF_WIDTH = 1.15D;
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("animation.void_reaper.idle");
    private static final RawAnimation FLOAT_WALK_ANIM = RawAnimation.begin().thenLoop("animation.void_reaper.float_walk");
    private static final RawAnimation CLEAVE_ANIM = RawAnimation.begin().thenPlay("animation.void_reaper.cleave");
    private static final RawAnimation RIFT_ANIM = RawAnimation.begin().thenPlay("animation.void_reaper.rift_cast");
    private static final RawAnimation DEATH_MARK_ANIM = RawAnimation.begin().thenPlay("animation.void_reaper.death_mark");
    private static final RawAnimation SNARE_ANIM = RawAnimation.begin().thenPlay("animation.void_reaper.snare");
    private static final RawAnimation SIPHON_ANIM = RawAnimation.begin().thenPlay("animation.void_reaper.siphon");
    private static final RawAnimation SHADOW_STEP_ANIM = RawAnimation.begin().thenPlay("animation.void_reaper.shadow_step");

    private int shadowStepCooldown = 45;
    private int voidCleaveCooldown = 35;
    private int soulSnareCooldown = 70;
    private int soulSiphonCooldown = 140;
    private int voidRiftCooldown = 95;
    private int voidRiftWarmup = 0;
    private int scytheReturnTicks = 0;
    private int deathMarkCooldown = 170;
    private int deathMarkWarmup = 0;
    private java.util.UUID deathMarkTargetId = null;
    private Vec3 voidRiftOrigin = Vec3.ZERO;
    private Vec3 voidRiftDirection = Vec3.ZERO;
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public VoidReaperEntity(EntityType<? extends Zombie> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 80;
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.VOIDBOUND_SCYTHE.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        this.applyBaselineAttributes(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
                .add(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(REAPER_ACTION, ACTION_IDLE);
        this.entityData.define(REAPER_ACTION_TICKS, 0);
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        this.tickReaperAction();
        this.tickCooldowns();
        this.tickPendingCustomAttacks();
        this.tickCustomAttacks();

        if (!this.level().isClientSide && this.tickCount % 40 == 0 && this.getHealth() < this.getMaxHealth()) {
            this.heal(1.0F);
        }

        if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 8 == 0) {
            serverLevel.sendParticles(ParticleTypes.SCULK_SOUL,
                    this.getX(), this.getY(0.8D), this.getZ(),
                    4, 0.35D, 0.35D, 0.35D, 0.015D);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (target instanceof Player player && AllegianceUtil.hasPath(player, AllegiancePath.VOID)) {
            return false;
        }
        boolean hurt = super.doHurtTarget(target);
        if (hurt && target instanceof LivingEntity living) {
            this.setReaperAction(ACTION_CLEAVE, 16);
            living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
            living.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
            this.playSound(SoundEvents.WITHER_HURT, 0.8F, 0.65F);
        }
        return hurt;
    }

    @Override
    public void tick() {
        super.tick();
        this.applyBaselineAttributes(true);
    }

    private void tickCooldowns() {
        if (this.level().isClientSide) {
            return;
        }
        if (this.shadowStepCooldown > 0) {
            this.shadowStepCooldown--;
        }
        if (this.voidCleaveCooldown > 0) {
            this.voidCleaveCooldown--;
        }
        if (this.soulSnareCooldown > 0) {
            this.soulSnareCooldown--;
        }
        if (this.soulSiphonCooldown > 0) {
            this.soulSiphonCooldown--;
        }
        if (this.voidRiftCooldown > 0) {
            this.voidRiftCooldown--;
        }
        if (this.deathMarkCooldown > 0) {
            this.deathMarkCooldown--;
        }
    }

    private void tickPendingCustomAttacks() {
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (this.voidRiftWarmup > 0) {
            this.telegraphVoidRift(serverLevel);
            this.voidRiftWarmup--;
            if (this.voidRiftWarmup <= 0) {
                this.releaseVoidRift(serverLevel);
            }
        }
        if (this.scytheReturnTicks > 0) {
            this.scytheReturnTicks--;
            this.telegraphScytheReturn(serverLevel);
            if (this.scytheReturnTicks <= 0) {
                this.scytheReturn(serverLevel);
            }
        }
        if (this.deathMarkWarmup > 0) {
            this.deathMarkWarmup--;
            this.telegraphDeathMark(serverLevel);
            if (this.deathMarkWarmup <= 0) {
                this.releaseDeathMark(serverLevel);
            }
        }
    }

    private void tickCustomAttacks() {
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || !this.hasLineOfSight(target)) {
            return;
        }
        if (this.voidRiftWarmup > 0 || this.deathMarkWarmup > 0) {
            return;
        }
        if (target instanceof Player player && AllegianceUtil.hasPath(player, AllegiancePath.VOID)) {
            this.setTarget(null);
            serverLevel.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY(1.0D), this.getZ(),
                    8, 0.35D, 0.45D, 0.35D, 0.015D);
            return;
        }

        double distanceSqr = this.distanceToSqr(target);
        if (target instanceof Player player && this.deathMarkCooldown <= 0
                && player.getHealth() <= player.getMaxHealth() * 0.35F
                && distanceSqr <= 18.0D * 18.0D) {
            this.startDeathMark(player, serverLevel);
            this.deathMarkCooldown = this.scaledCooldown(DEATH_MARK_COOLDOWN);
            return;
        }

        if (distanceSqr > VOID_CLEAVE_RADIUS * VOID_CLEAVE_RADIUS
                && distanceSqr <= VOID_RIFT_LENGTH * VOID_RIFT_LENGTH
                && this.voidRiftCooldown <= 0) {
            this.startVoidRift(target, serverLevel);
            this.voidRiftCooldown = this.scaledCooldown(VOID_RIFT_COOLDOWN);
            return;
        }

        if (distanceSqr > 42.0D && this.shadowStepCooldown <= 0 && this.shadowStepNear(target, serverLevel)) {
            this.shadowStepCooldown = this.scaledCooldown(SHADOW_STEP_COOLDOWN);
            return;
        }

        if (distanceSqr <= VOID_CLEAVE_RADIUS * VOID_CLEAVE_RADIUS && this.voidCleaveCooldown <= 0) {
            this.voidCleave(serverLevel);
            this.voidCleaveCooldown = this.scaledCooldown(VOID_CLEAVE_COOLDOWN);
            return;
        }

        if (distanceSqr > 12.0D && distanceSqr <= SOUL_SNARE_RANGE_SQR && this.soulSnareCooldown <= 0) {
            this.soulSnare(target, serverLevel);
            this.soulSnareCooldown = this.scaledCooldown(SOUL_SNARE_COOLDOWN);
            return;
        }

        if (this.getHealth() <= this.getMaxHealth() * 0.55F && this.soulSiphonCooldown <= 0) {
            this.soulSiphon(serverLevel);
            this.soulSiphonCooldown = this.scaledCooldown(SOUL_SIPHON_COOLDOWN);
        }
    }

    private int scaledCooldown(int baseTicks) {
        Difficulty difficulty = this.level().getDifficulty();
        if (difficulty == Difficulty.HARD) {
            return Math.max(35, Math.round(baseTicks * 0.78F));
        }
        if (difficulty == Difficulty.EASY) {
            return Math.round(baseTicks * 1.25F);
        }
        return baseTicks;
    }

    private void tickReaperAction() {
        if (this.level().isClientSide) {
            return;
        }
        int ticks = this.entityData.get(REAPER_ACTION_TICKS);
        if (ticks <= 0) {
            if (this.entityData.get(REAPER_ACTION) != ACTION_IDLE) {
                this.entityData.set(REAPER_ACTION, ACTION_IDLE);
            }
            return;
        }
        this.entityData.set(REAPER_ACTION_TICKS, ticks - 1);
        if (ticks - 1 <= 0) {
            this.entityData.set(REAPER_ACTION, ACTION_IDLE);
        }
    }

    private void setReaperAction(int action, int ticks) {
        if (this.level().isClientSide) {
            return;
        }
        this.entityData.set(REAPER_ACTION, action);
        this.entityData.set(REAPER_ACTION_TICKS, Math.max(1, ticks));
    }

    private boolean shadowStepNear(LivingEntity target, ServerLevel serverLevel) {
        Vec3 look = target.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        if (look.lengthSqr() < 0.01D) {
            look = target.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
        }
        if (look.lengthSqr() < 0.01D) {
            look = new Vec3(0.0D, 0.0D, 1.0D);
        }
        look = look.normalize();

        Vec3 behind = target.position().subtract(look.scale(2.4D));
        Vec3 oldPos = this.position();
        boolean teleported = false;
        for (int attempt = 0; attempt < 8 && !teleported; attempt++) {
            double spread = attempt == 0 ? 0.0D : 1.2D + attempt * 0.35D;
            double x = behind.x + (this.random.nextDouble() - 0.5D) * spread;
            double y = target.getY() + this.random.nextInt(3) - 1;
            double z = behind.z + (this.random.nextDouble() - 0.5D) * spread;
            teleported = this.randomTeleport(x, y, z, true);
        }

        if (!teleported) {
            return false;
        }

        this.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
        serverLevel.sendParticles(ParticleTypes.PORTAL, oldPos.x, oldPos.y + 0.6D, oldPos.z, 36, 0.45D, 0.75D, 0.45D, 0.35D);
        serverLevel.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY(0.7D), this.getZ(), 28, 0.35D, 0.55D, 0.35D, 0.04D);
        this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.25F, 0.45F);
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 55, 0));
        this.setReaperAction(ACTION_SHADOW_STEP, 18);
        return true;
    }

    private void voidCleave(ServerLevel serverLevel) {
        this.setReaperAction(ACTION_CLEAVE, 28);
        AABB area = this.getBoundingBox().inflate(VOID_CLEAVE_RADIUS, 1.0D, VOID_CLEAVE_RADIUS);
        Vec3 forward = this.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        if (forward.lengthSqr() < 0.01D) {
            forward = new Vec3(0.0D, 0.0D, 1.0D);
        }
        forward = forward.normalize();

        int hits = 0;
        for (LivingEntity living : this.level().getEntitiesOfClass(LivingEntity.class, area, this::isValidReaperTarget)) {
            Vec3 offset = living.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
            double distanceSqr = offset.lengthSqr();
            if (distanceSqr > VOID_CLEAVE_RADIUS * VOID_CLEAVE_RADIUS) {
                continue;
            }
            boolean inFront = distanceSqr < 2.25D || offset.normalize().dot(forward) > 0.05D;
            if (!inFront) {
                continue;
            }

            float damage = this.level().getDifficulty() == Difficulty.HARD ? 13.0F : 10.0F;
            if (living.hurt(this.damageSources().mobAttack(this), damage)) {
                living.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0));
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
                living.knockback(0.65D, this.getX() - living.getX(), this.getZ() - living.getZ());
                hits++;
            }
        }

        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, this.getX(), this.getY(0.7D), this.getZ(), 4, 1.5D, 0.25D, 1.5D, 0.0D);
        serverLevel.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY(0.55D), this.getZ(), 26, 1.4D, 0.35D, 1.4D, 0.04D);
        this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.35F, 0.35F);
        this.scytheReturnTicks = SCYTHE_RETURN_DELAY_TICKS;
        if (hits > 0) {
            this.heal(Math.min(6.0F, hits * 2.0F));
        }
    }

    private void startVoidRift(LivingEntity target, ServerLevel serverLevel) {
        this.setReaperAction(ACTION_RIFT, VOID_RIFT_WARMUP_TICKS + 10);
        Vec3 direction = target.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
        if (direction.lengthSqr() < 0.01D) {
            direction = this.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        }
        if (direction.lengthSqr() < 0.01D) {
            direction = new Vec3(0.0D, 0.0D, 1.0D);
        }
        this.voidRiftDirection = direction.normalize();
        this.voidRiftOrigin = this.position().add(this.voidRiftDirection.scale(0.9D));
        this.voidRiftWarmup = VOID_RIFT_WARMUP_TICKS;
        this.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
        this.playSound(SoundEvents.SCULK_SHRIEKER_SHRIEK, 0.9F, 0.48F);
        this.telegraphVoidRift(serverLevel);
    }

    private void telegraphVoidRift(ServerLevel serverLevel) {
        if (this.voidRiftDirection.lengthSqr() < 0.01D) {
            return;
        }
        double pulse = this.voidRiftWarmup % 6 == 0 ? 0.12D : 0.0D;
        for (double step = 0.8D; step <= VOID_RIFT_LENGTH; step += 0.75D) {
            Vec3 pos = this.voidRiftOrigin.add(this.voidRiftDirection.scale(step));
            serverLevel.sendParticles(VOID_RIFT_DUST, pos.x, this.getY() + 0.08D, pos.z,
                    2, VOID_RIFT_HALF_WIDTH * 0.28D + pulse, 0.03D, VOID_RIFT_HALF_WIDTH * 0.28D + pulse, 0.0D);
        }
    }

    private void releaseVoidRift(ServerLevel serverLevel) {
        if (this.voidRiftDirection.lengthSqr() < 0.01D) {
            return;
        }
        AABB area = new AABB(this.voidRiftOrigin, this.voidRiftOrigin.add(this.voidRiftDirection.scale(VOID_RIFT_LENGTH)))
                .inflate(VOID_RIFT_HALF_WIDTH + 0.8D, 1.3D, VOID_RIFT_HALF_WIDTH + 0.8D);
        Vec3 side = new Vec3(-this.voidRiftDirection.z, 0.0D, this.voidRiftDirection.x);
        for (LivingEntity living : this.level().getEntitiesOfClass(LivingEntity.class, area, this::isValidReaperTarget)) {
            Vec3 offset = living.position().subtract(this.voidRiftOrigin).multiply(1.0D, 0.0D, 1.0D);
            double along = offset.dot(this.voidRiftDirection);
            double sideDistance = Math.abs(offset.dot(side));
            if (along < 0.0D || along > VOID_RIFT_LENGTH || sideDistance > VOID_RIFT_HALF_WIDTH) {
                continue;
            }
            float damage = this.level().getDifficulty() == Difficulty.HARD ? 11.0F : 8.0F;
            if (living.hurt(this.damageSources().magic(), damage)) {
                living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 95, 0));
                living.addEffect(new MobEffectInstance(MobEffects.WITHER, 75, 0));
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90, 2));
            }
        }
        for (double step = 0.5D; step <= VOID_RIFT_LENGTH; step += 0.45D) {
            Vec3 pos = this.voidRiftOrigin.add(this.voidRiftDirection.scale(step));
            serverLevel.sendParticles(ParticleTypes.SCULK_SOUL, pos.x, this.getY() + 0.15D, pos.z,
                    5, VOID_RIFT_HALF_WIDTH * 0.35D, 0.12D, VOID_RIFT_HALF_WIDTH * 0.35D, 0.035D);
        }
        this.playSound(SoundEvents.WARDEN_SONIC_BOOM, 0.75F, 0.55F);
    }

    private void telegraphScytheReturn(ServerLevel serverLevel) {
        if (this.scytheReturnTicks % 3 != 0) {
            return;
        }
        serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY(0.75D), this.getZ(),
                12, 1.2D, 0.25D, 1.2D, 0.08D);
    }

    private void scytheReturn(ServerLevel serverLevel) {
        this.setReaperAction(ACTION_CLEAVE, 16);
        AABB area = this.getBoundingBox().inflate(VOID_CLEAVE_RADIUS + 0.7D, 1.0D, VOID_CLEAVE_RADIUS + 0.7D);
        int hits = 0;
        for (LivingEntity living : this.level().getEntitiesOfClass(LivingEntity.class, area, this::isValidReaperTarget)) {
            if (living.distanceToSqr(this) > (VOID_CLEAVE_RADIUS + 0.7D) * (VOID_CLEAVE_RADIUS + 0.7D)) {
                continue;
            }
            Vec3 pull = this.position().subtract(living.position()).multiply(1.0D, 0.0D, 1.0D);
            if (pull.lengthSqr() > 0.01D) {
                pull = pull.normalize().scale(0.72D);
                living.push(pull.x, 0.1D, pull.z);
                living.hurtMarked = true;
            }
            if (living.hurt(this.damageSources().mobAttack(this), this.level().getDifficulty() == Difficulty.HARD ? 8.0F : 6.0F)) {
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
                hits++;
            }
        }
        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, this.getX(), this.getY(0.65D), this.getZ(), 5, 1.7D, 0.25D, 1.7D, 0.0D);
        serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY(0.8D), this.getZ(), 28, 1.25D, 0.35D, 1.25D, 0.05D);
        this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.1F, 0.62F);
        if (hits > 0) {
            this.heal(Math.min(5.0F, hits * 1.5F));
        }
    }

    private void startDeathMark(Player player, ServerLevel serverLevel) {
        this.setReaperAction(ACTION_DEATH_MARK, DEATH_MARK_WARMUP_TICKS + 12);
        this.deathMarkTargetId = player.getUUID();
        this.deathMarkWarmup = DEATH_MARK_WARMUP_TICKS;
        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, DEATH_MARK_WARMUP_TICKS + 35, 0, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, DEATH_MARK_WARMUP_TICKS + 20, 0));
        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY(1.0D), player.getZ(),
                16, 0.45D, 0.55D, 0.45D, 0.035D);
        this.playSound(SoundEvents.WITHER_SPAWN, 0.65F, 0.85F);
    }

    private void telegraphDeathMark(ServerLevel serverLevel) {
        if (this.deathMarkTargetId == null) {
            this.deathMarkWarmup = 0;
            return;
        }
        Player player = serverLevel.getPlayerByUUID(this.deathMarkTargetId);
        if (player == null || !player.isAlive() || AllegianceUtil.hasPath(player, AllegiancePath.VOID)) {
            this.deathMarkWarmup = 0;
            this.deathMarkTargetId = null;
            return;
        }
        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY(1.15D), player.getZ(),
                5, 0.28D, 0.35D, 0.28D, 0.02D);
        if (this.deathMarkWarmup % 8 == 0) {
            this.playSound(SoundEvents.SOUL_ESCAPE, 0.55F, 0.35F);
        }
    }

    private void releaseDeathMark(ServerLevel serverLevel) {
        if (this.deathMarkTargetId == null) {
            return;
        }
        Player player = serverLevel.getPlayerByUUID(this.deathMarkTargetId);
        this.deathMarkTargetId = null;
        if (player == null || !player.isAlive() || AllegianceUtil.hasPath(player, AllegiancePath.VOID)) {
            return;
        }
        float damage = this.level().getDifficulty() == Difficulty.HARD ? 9.0F : 6.5F;
        player.hurt(this.damageSources().magic(), damage);
        player.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90, 1));
        serverLevel.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY(0.8D), player.getZ(),
                34, 0.55D, 0.55D, 0.55D, 0.05D);
        this.playSound(SoundEvents.SOUL_ESCAPE, 1.0F, 0.42F);
    }

    private void soulSnare(LivingEntity target, ServerLevel serverLevel) {
        this.setReaperAction(ACTION_SNARE, 26);
        Vec3 pull = this.position().subtract(target.position()).multiply(1.0D, 0.0D, 1.0D);
        if (pull.lengthSqr() > 0.01D) {
            pull = pull.normalize().scale(1.15D);
            target.push(pull.x, 0.18D, pull.z);
            if (target instanceof Player player) {
                player.hurtMarked = true;
            }
        }

        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 95, 2));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 85, 0));
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 70, 0));
        target.hurt(this.damageSources().mobAttack(this), this.level().getDifficulty() == Difficulty.HARD ? 7.0F : 5.0F);

        serverLevel.sendParticles(ParticleTypes.SCULK_SOUL, target.getX(), target.getY(0.45D), target.getZ(), 22, 0.65D, 0.12D, 0.65D, 0.02D);
        serverLevel.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY(0.85D), this.getZ(), 24, 0.35D, 0.35D, 0.35D, 0.22D);
        this.playSound(SoundEvents.SCULK_SHRIEKER_SHRIEK, 1.0F, 0.65F);
    }

    private void soulSiphon(ServerLevel serverLevel) {
        this.setReaperAction(ACTION_SIPHON, 42);
        AABB area = this.getBoundingBox().inflate(7.0D, 2.0D, 7.0D);
        int drained = 0;
        for (LivingEntity living : this.level().getEntitiesOfClass(LivingEntity.class, area, this::isValidReaperTarget)) {
            if (!this.hasLineOfSight(living)) {
                continue;
            }
            float damage = this.level().getDifficulty() == Difficulty.HARD ? 5.0F : 3.5F;
            if (living.hurt(this.damageSources().magic(), damage)) {
                living.addEffect(new MobEffectInstance(MobEffects.WITHER, 70, 0));
                living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 45, 0));
                drained++;
                serverLevel.sendParticles(ParticleTypes.SOUL, living.getX(), living.getY(0.7D), living.getZ(), 9, 0.25D, 0.25D, 0.25D, 0.04D);
            }
        }

        if (drained <= 0) {
            return;
        }

        this.heal(Math.min(18.0F, drained * 5.0F));
        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 65, 0));
        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY(0.85D), this.getZ(), 42, 0.6D, 0.75D, 0.6D, 0.04D);
        this.playSound(SoundEvents.SOUL_ESCAPE, 1.0F, 0.55F);
    }

    private boolean isValidReaperTarget(LivingEntity living) {
        if (living == this || !living.isAlive() || living instanceof VoidReaperEntity) {
            return false;
        }
        if (living instanceof Player player && AllegianceUtil.hasPath(player, AllegiancePath.VOID)) {
            return false;
        }
        return living == this.getTarget() || living instanceof Player;
    }

    private void applyBaselineAttributes(boolean preserveHealth) {
        float healthPercent = this.getMaxHealth() > 0.0F ? this.getHealth() / this.getMaxHealth() : 1.0F;
        if (this.getAttribute(Attributes.MAX_HEALTH) != null && this.getAttributeValue(Attributes.MAX_HEALTH) < MAX_HEALTH) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(MAX_HEALTH);
            this.setHealth(preserveHealth ? Math.max(1.0F, healthPercent * (float) MAX_HEALTH) : (float) MAX_HEALTH);
        }
        if (this.getAttribute(Attributes.ATTACK_DAMAGE) != null && this.getAttributeValue(Attributes.ATTACK_DAMAGE) < ATTACK_DAMAGE) {
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(ATTACK_DAMAGE);
        }
        if (this.getAttribute(Attributes.MOVEMENT_SPEED) != null && this.getAttributeValue(Attributes.MOVEMENT_SPEED) < MOVEMENT_SPEED) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(MOVEMENT_SPEED);
        }
        if (this.getAttribute(Attributes.KNOCKBACK_RESISTANCE) != null && this.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) < KNOCKBACK_RESISTANCE) {
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(KNOCKBACK_RESISTANCE);
        }
        if (this.getAttribute(Attributes.FOLLOW_RANGE) != null && this.getAttributeValue(Attributes.FOLLOW_RANGE) < FOLLOW_RANGE) {
            this.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(FOLLOW_RANGE);
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource damageSource, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(damageSource, looting, recentlyHit);
        int safeLooting = Math.min(5, Math.max(0, looting));
        this.spawnAtLocation(new ItemStack(ModItems.VOID_CRYSTAL.get()));
        this.spawnAtLocation(new ItemStack(ModItems.VOID_CORE.get()));
        this.spawnAtLocation(new ItemStack(com.Momik.usless_mobs.registry.ModItems.VOID_VITALITY_TEMPLATE.get()));
        this.spawnAtLocation(new ItemStack(ModItems.SOUL_FRAGMENT.get(), 4 + this.random.nextInt(3 + safeLooting)));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            int action = this.entityData.get(REAPER_ACTION);
            if (action == ACTION_CLEAVE) {
                state.setAnimation(CLEAVE_ANIM);
            } else if (action == ACTION_RIFT) {
                state.setAnimation(RIFT_ANIM);
            } else if (action == ACTION_DEATH_MARK) {
                state.setAnimation(DEATH_MARK_ANIM);
            } else if (action == ACTION_SNARE) {
                state.setAnimation(SNARE_ANIM);
            } else if (action == ACTION_SIPHON) {
                state.setAnimation(SIPHON_ANIM);
            } else if (action == ACTION_SHADOW_STEP) {
                state.setAnimation(SHADOW_STEP_ANIM);
            } else if (this.getDeltaMovement().horizontalDistanceSqr() > 0.0025D) {
                state.setAnimation(FLOAT_WALK_ANIM);
            } else {
                state.setAnimation(IDLE_ANIM);
            }
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
