package com.Momik.usless_mobs.entity;

import com.Momik.usless_mobs.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class CoralDrownedEntity extends Drowned {
    private int waterGripCooldown = 45;
    private int coralGuardCooldown = 140;
    private int coralSurgeCooldown = 95;
    private int coralSurgeWarmup = 0;
    private Vec3 coralSurgeOrigin = Vec3.ZERO;
    private Vec3 coralSurgeDirection = Vec3.ZERO;

    // Kein setCustomName im Konstruktor: der Anzeigename kommt aus dem EntityType-Lang-Key.
    // Ein Custom Name würde jeden Coral Drowned vom Despawnen ausnehmen (Ozean läuft voll).
    public CoralDrownedEntity(EntityType<? extends Drowned> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 14;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Drowned.createAttributes()
                .add(Attributes.MAX_HEALTH, 34.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.29D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 36.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.35D);
    }

    // Vanilla Zombie.handleAttributes() randomisiert Reinforcement-Chance + Difficulty-Modifier
    // und greift dabei auf SPAWN_REINFORCEMENTS_CHANCE/ARMOR zu. Beim Coral Drowned führte das
    // beim finalizeSpawn (Ei, Naturspawn, OceanMobSpawnHandler) zu einer NPE. Wir nutzen feste,
    // in createAttributes() definierte Werte und überschreiben das null-sicher: kein Zombie-
    // Reinforcement, keine zufälligen Modifier.
    @Override
    protected void handleAttributes(float difficulty) {
        AttributeInstance reinforcements = this.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE);
        if (reinforcements != null) {
            reinforcements.setBaseValue(0.0D);
        }
    }

    @SuppressWarnings("unchecked")
    public static boolean checkCoralDrownedSpawnRules(EntityType<CoralDrownedEntity> entityType, LevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (!(level instanceof ServerLevelAccessor serverLevel)) {
            return false;
        }
        return Monster.checkMonsterSpawnRules((EntityType<? extends Monster>) (EntityType<?>) entityType, serverLevel, spawnType, pos, random);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }

        if (this.waterGripCooldown > 0) {
            this.waterGripCooldown--;
        }
        if (this.coralGuardCooldown > 0) {
            this.coralGuardCooldown--;
        }
        if (this.coralSurgeCooldown > 0) {
            this.coralSurgeCooldown--;
        }
        tickCoralSurgeWarmup();

        if (this.isInWaterOrBubble() && this.tickCount % 70 == 0) {
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 90, 0));
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 90, 0));
        }

        if (this.isInWaterOrBubble() && this.level() instanceof ServerLevel serverLevel && this.tickCount % 14 == 0) {
            serverLevel.sendParticles(ParticleTypes.NAUTILUS,
                    this.getX(), this.getY(0.75D), this.getZ(),
                    4, 0.24D, 0.28D, 0.24D, 0.015D);
        }

        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive() && this.waterGripCooldown <= 0
                && this.isInWaterOrBubble() && this.distanceToSqr(target) <= 13.0D * 13.0D) {
            waterGrip(target);
            this.waterGripCooldown = this.level().getDifficulty() == Difficulty.HARD ? 65 : 90;
        }
        if (target != null && target.isAlive() && this.coralSurgeCooldown <= 0 && this.coralSurgeWarmup <= 0
                && this.isInWaterOrBubble() && this.distanceToSqr(target) > 4.0D * 4.0D
                && this.distanceToSqr(target) <= 15.0D * 15.0D) {
            startCoralSurge(target);
            this.coralSurgeCooldown = this.level().getDifficulty() == Difficulty.HARD ? 110 : 150;
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide && this.coralGuardCooldown <= 0 && this.getHealth() <= this.getMaxHealth() * 0.45F) {
            this.coralGuardCooldown = 220;
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 1));
            this.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.NAUTILUS,
                        this.getX(), this.getY(0.8D), this.getZ(),
                        32, 0.55D, 0.45D, 0.55D, 0.03D);
            }
            this.playSound(SoundEvents.TURTLE_EGG_CRACK, 1.0F, 0.65F);
        }
        return hurt;
    }

    private void waterGrip(LivingEntity target) {
        Vec3 pull = this.position().subtract(target.position()).multiply(1.0D, 0.0D, 1.0D);
        if (pull.lengthSqr() > 0.01D) {
            pull = pull.normalize().scale(0.85D);
            target.setDeltaMovement(target.getDeltaMovement().add(pull.x, 0.15D, pull.z));
            target.hurtMarked = true;
        }
        target.hurt(this.damageSources().mobAttack(this), this.level().getDifficulty() == Difficulty.HARD ? 5.0F : 3.5F);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.NAUTILUS,
                    target.getX(), target.getY(0.55D), target.getZ(),
                    30, 0.45D, 0.22D, 0.45D, 0.055D);
            serverLevel.playSound(null, target.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.HOSTILE, 1.0F, 0.85F);
        }
    }

    private void startCoralSurge(LivingEntity target) {
        Vec3 direction = target.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
        if (direction.lengthSqr() < 0.01D) {
            direction = this.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        }
        this.coralSurgeDirection = direction.normalize();
        this.coralSurgeOrigin = this.position().add(this.coralSurgeDirection.scale(0.8D));
        this.coralSurgeWarmup = 22;
        this.playSound(ModSounds.CORAL_DROWNED_SURGE.get(), 0.9F, 0.72F);
    }

    private void tickCoralSurgeWarmup() {
        if (this.coralSurgeWarmup <= 0 || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        this.coralSurgeWarmup--;
        for (double step = 0.7D; step <= 7.5D; step += 0.8D) {
            Vec3 pos = this.coralSurgeOrigin.add(this.coralSurgeDirection.scale(step));
            serverLevel.sendParticles(ParticleTypes.NAUTILUS, pos.x, this.getY(0.35D), pos.z,
                    2, 0.2D, 0.12D, 0.2D, 0.01D);
        }
        if (this.coralSurgeWarmup <= 0) {
            releaseCoralSurge(serverLevel);
        }
    }

    private void releaseCoralSurge(ServerLevel serverLevel) {
        if (this.coralSurgeDirection.lengthSqr() < 0.01D) {
            return;
        }
        AABB area = new AABB(this.coralSurgeOrigin, this.coralSurgeOrigin.add(this.coralSurgeDirection.scale(7.5D))).inflate(1.2D, 1.0D, 1.2D);
        Vec3 side = new Vec3(-this.coralSurgeDirection.z, 0.0D, this.coralSurgeDirection.x);
        for (LivingEntity living : this.level().getEntitiesOfClass(LivingEntity.class, area, living -> living.isAlive() && living != this)) {
            Vec3 offset = living.position().subtract(this.coralSurgeOrigin).multiply(1.0D, 0.0D, 1.0D);
            double along = offset.dot(this.coralSurgeDirection);
            double sideDistance = Math.abs(offset.dot(side));
            if (along < 0.0D || along > 7.5D || sideDistance > 1.15D) {
                continue;
            }
            living.hurt(this.damageSources().mobAttack(this), this.level().getDifficulty() == Difficulty.HARD ? 6.0F : 4.0F);
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90, 1));
            living.setDeltaMovement(living.getDeltaMovement().add(this.coralSurgeDirection.x * 0.85D, 0.12D, this.coralSurgeDirection.z * 0.85D));
            living.hurtMarked = true;
        }
        for (double step = 0.5D; step <= 7.5D; step += 0.45D) {
            Vec3 pos = this.coralSurgeOrigin.add(this.coralSurgeDirection.scale(step));
            serverLevel.sendParticles(ParticleTypes.SPLASH, pos.x, this.getY(0.35D), pos.z,
                    5, 0.28D, 0.15D, 0.28D, 0.06D);
        }
        serverLevel.playSound(null, this.blockPosition(), ModSounds.CORAL_DROWNED_SURGE.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    @Override
    public SoundEvent getAmbientSound() {
        return ModSounds.CORAL_DROWNED_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.CORAL_DROWNED_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.CORAL_DROWNED_DEATH.get();
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource damageSource, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(damageSource, looting, recentlyHit);
        int safeLooting = Math.min(5, Math.max(0, looting));
        this.spawnAtLocation(new ItemStack(com.Momik.usless_mobs.registry.ModItems.CORAL_SCALE.get(), 2 + this.random.nextInt(2 + safeLooting)));
        if (this.random.nextFloat() < Math.min(0.55F, 0.18F + safeLooting * 0.08F)) {
            this.spawnAtLocation(new ItemStack(com.Momik.usless_mobs.registry.ModItems.NATURE_CRYSTAL.get()));
        }
    }
}
