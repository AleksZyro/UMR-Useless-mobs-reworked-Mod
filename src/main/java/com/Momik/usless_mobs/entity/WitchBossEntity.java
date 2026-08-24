package com.Momik.usless_mobs.entity;

import com.Momik.usless_mobs.entity.boss.BossDifficultyProfile;
import com.Momik.usless_mobs.event.RabbitTransformationHandler;
import com.Momik.usless_mobs.registry.ModEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class WitchBossEntity extends Witch {
    public static final String DECOY_KEY = "UmrWitchBossDecoy";
    public static final String DECOY_TICKS_KEY = "UmrWitchBossDecoyTicks";
    public static final String ROOT_SPIRIT_KEY = "UmrWitchBossRootSpirit";
    public static final String ROOT_SPIRIT_TICKS_KEY = "UmrWitchBossRootSpiritTicks";
    public static final String HUNT_HOUND_KEY = "UmrWitchBossHuntHound";
    public static final String HUNT_OWNER_KEY = "UmrWitchBossHuntOwner";
    public static final String HUNT_HOUND_TICKS_KEY = "UmrWitchBossHuntTicks";

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.usless_mobs.witch_boss"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS);

    private int scareCooldown = 80;
    private int dodgeCooldown = 120;
    private int brewCooldown = 60;
    private int rootSpiritCooldown = 180;
    private int rottenBrewCooldown = 105;
    private int lifeBrewCooldown = 170;
    private int huntCooldown = 200;
    private int huntTicks;
    private UUID huntTargetId;

    public WitchBossEntity(EntityType<? extends Witch> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 80;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 155.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.31D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 42.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.45D);
    }

    private BossDifficultyProfile difficultyProfile() {
        return BossDifficultyProfile.forDifficulty(this.level().getDifficulty());
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }

        if (this.tickCount % 20 == 1) {
            syncDifficultyAttackDamage();
        }

        bossEvent.setProgress(Math.max(0.0F, this.getHealth() / this.getMaxHealth()));
        tickCooldowns();
        tickRabbitHunt();
        if (this.tickCount % 16 == 0 && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH,
                    this.getX(), this.getY(1.0D), this.getZ(),
                    8, 0.35D, 0.55D, 0.35D, 0.02D);
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        if (scareCooldown <= 0 && this.distanceToSqr(target) < 20.0D * 20.0D) {
            scarePulse(target);
            scareCooldown = difficultyProfile().cooldown(130);
        }

        if (brewCooldown <= 0 && this.distanceToSqr(target) < 12.0D * 12.0D) {
            curseBrew(target);
            brewCooldown = difficultyProfile().cooldown(70);
        }

        if (rottenBrewCooldown <= 0 && this.distanceToSqr(target) < 16.0D * 16.0D) {
            rottenBrew(target);
            rottenBrewCooldown = difficultyProfile().cooldown(150);
        }

        if (lifeBrewCooldown <= 0 && this.getHealth() < this.getMaxHealth() * 0.82F) {
            lifeBrew();
            lifeBrewCooldown = difficultyProfile().cooldown(185);
        }

        if (rootSpiritCooldown <= 0 && this.getHealth() < this.getMaxHealth() * 0.5F && this.distanceToSqr(target) < 24.0D * 24.0D) {
            summonRootSpirits(target);
            rootSpiritCooldown = difficultyProfile().cooldown(190);
        }

        if (dodgeCooldown <= 0 && (this.getHealth() < this.getMaxHealth() * 0.55F || this.random.nextFloat() < 0.08F)) {
            rabbitDodge(target);
            dodgeCooldown = difficultyProfile().cooldown(145);
        }

        if (huntCooldown <= 0 && huntTicks <= 0 && target instanceof ServerPlayer player
                && !RabbitTransformationHandler.isTransformed(player)
                && this.distanceToSqr(player) < 20.0D * 20.0D) {
            startRabbitHunt(player);
            huntCooldown = difficultyProfile().cooldown(340);
        }
    }

    private void tickCooldowns() {
        if (scareCooldown > 0) {
            scareCooldown--;
        }
        if (dodgeCooldown > 0) {
            dodgeCooldown--;
        }
        if (brewCooldown > 0) {
            brewCooldown--;
        }
        if (rootSpiritCooldown > 0) {
            rootSpiritCooldown--;
        }
        if (rottenBrewCooldown > 0) {
            rottenBrewCooldown--;
        }
        if (lifeBrewCooldown > 0) {
            lifeBrewCooldown--;
        }
        if (huntCooldown > 0) {
            huntCooldown--;
        }
    }

    private void syncDifficultyAttackDamage() {
        var attackDamage = this.getAttribute(Attributes.ATTACK_DAMAGE);
        double scaledDamage = 6.0D * difficultyProfile().damageMultiplier();
        if (attackDamage != null && Math.abs(attackDamage.getBaseValue() - scaledDamage) > 0.001D) {
            attackDamage.setBaseValue(scaledDamage);
        }
    }

    private void startRabbitHunt(ServerPlayer player) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        BossDifficultyProfile profile = difficultyProfile();
        int duration = (7 + profile.rewardTier() * 2) * 20;
        int houndCount = profile.huntHoundCount();
        this.huntTicks = duration;
        this.huntTargetId = player.getUUID();

        player.addEffect(new MobEffectInstance(ModEffects.RABBIT_FORM.get(), duration, 0, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 1, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, duration, 1, false, false, true));
        player.refreshDimensions();

        for (int index = 0; index < houndCount; index++) {
            Wolf wolf = EntityType.WOLF.create(serverLevel);
            if (wolf == null) {
                continue;
            }
            double angle = Math.PI * 2.0D * index / houndCount;
            wolf.moveTo(this.getX() + Math.cos(angle) * 4.5D, this.getY(), this.getZ() + Math.sin(angle) * 4.5D,
                    this.random.nextFloat() * 360.0F, 0.0F);
            wolf.getPersistentData().putBoolean(HUNT_HOUND_KEY, true);
            wolf.getPersistentData().putUUID(HUNT_OWNER_KEY, this.getUUID());
            wolf.getPersistentData().putInt(HUNT_HOUND_TICKS_KEY, duration + 40);
            wolf.setCustomName(Component.translatable("entity.usless_mobs.witch_hound"));
            wolf.setTarget(player);
            wolf.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, profile.rewardTier()));
            serverLevel.addFreshEntity(wolf);
        }

        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,
                player.getX(), player.getY(0.5D), player.getZ(), 42, 0.5D, 0.45D, 0.5D, 0.05D);
        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH,
                player.getX(), player.getY(0.5D), player.getZ(), 32, 0.6D, 0.5D, 0.6D, 0.04D);
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON,
                SoundSource.HOSTILE, 1.4F, 1.25F);
    }

    private void tickRabbitHunt() {
        if (this.huntTicks <= 0 || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        this.huntTicks--;
        Player player = this.huntTargetId == null ? null : serverLevel.getPlayerByUUID(this.huntTargetId);
        if (player == null || !player.isAlive() || !RabbitTransformationHandler.isTransformed(player)) {
            finishRabbitHunt(player);
            return;
        }
        for (Wolf wolf : serverLevel.getEntitiesOfClass(Wolf.class, this.getBoundingBox().inflate(40.0D),
                candidate -> candidate.getPersistentData().getBoolean(HUNT_HOUND_KEY)
                        && candidate.getPersistentData().hasUUID(HUNT_OWNER_KEY)
                        && this.getUUID().equals(candidate.getPersistentData().getUUID(HUNT_OWNER_KEY)))) {
            wolf.setTarget(player);
        }
        if (this.huntTicks == 0) {
            finishRabbitHunt(player);
        }
    }

    private void finishRabbitHunt(Player player) {
        if (player != null) {
            RabbitTransformationHandler.clear(player);
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            for (Wolf wolf : serverLevel.getEntitiesOfClass(Wolf.class, this.getBoundingBox().inflate(48.0D),
                    candidate -> candidate.getPersistentData().getBoolean(HUNT_HOUND_KEY)
                            && candidate.getPersistentData().hasUUID(HUNT_OWNER_KEY)
                            && this.getUUID().equals(candidate.getPersistentData().getUUID(HUNT_OWNER_KEY)))) {
                wolf.discard();
            }
        }
        this.huntTicks = 0;
        this.huntTargetId = null;
    }

    private void scarePulse(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        AABB area = this.getBoundingBox().inflate(8.0D);
        for (LivingEntity living : this.level().getEntitiesOfClass(LivingEntity.class, area,
                living -> living instanceof Player || living == target)) {
            living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 90, 0));
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0));
            living.hurt(this.damageSources().magic(), difficultyProfile().damage(4.5F));
        }
        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SONIC_BOOM,
                target.getX(), target.getY(0.8D), target.getZ(),
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        serverLevel.playSound(null, target.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.0F, 0.85F);
    }

    private void curseBrew(LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.POISON, 7 * 20, 1));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5 * 20, 1));
        target.hurt(this.damageSources().magic(), difficultyProfile().damage(3.0F));
        this.heal(this.getHealth() < this.getMaxHealth() * 0.5F ? 5.0F : 3.0F);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH,
                    target.getX(), target.getY(0.8D), target.getZ(),
                    24, 0.35D, 0.35D, 0.35D, 0.05D);
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,
                    this.getX(), this.getY(1.0D), this.getZ(),
                    5, 0.25D, 0.35D, 0.25D, 0.02D);
        }
        this.level().playSound(null, this.blockPosition(), SoundEvents.WITCH_THROW, SoundSource.HOSTILE, 0.9F, 0.95F);
    }

    private void rottenBrew(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        AreaEffectCloud cloud = new AreaEffectCloud(this.level(), target.getX(), target.getY(), target.getZ());
        cloud.setOwner(this);
        cloud.setRadius(3.0F);
        cloud.setRadiusPerTick(-0.015F);
        cloud.setDuration(90 + difficultyProfile().rewardTier() * 30);
        cloud.setWaitTime(18);
        cloud.setParticle(net.minecraft.core.particles.ParticleTypes.WITCH);
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1));
        cloud.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 1));
        cloud.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
        this.level().addFreshEntity(cloud);

        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH,
                target.getX(), target.getY(0.15D), target.getZ(),
                42, 1.2D, 0.08D, 1.2D, 0.04D);
        serverLevel.playSound(null, target.blockPosition(), SoundEvents.SPLASH_POTION_BREAK, SoundSource.HOSTILE, 1.0F, 0.65F);
    }

    private void lifeBrew() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        this.heal(6.0F + difficultyProfile().rewardTier() * 3.0F);
        this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 110, 1));
        this.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 140, 1));
        AABB area = this.getBoundingBox().inflate(5.0D);
        for (Vex spirit : this.level().getEntitiesOfClass(Vex.class, area,
                spirit -> spirit.isAlive() && spirit.getPersistentData().getBoolean(ROOT_SPIRIT_KEY))) {
            spirit.heal(4.0F);
            spirit.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 0));
        }
        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,
                this.getX(), this.getY(1.0D), this.getZ(),
                16, 0.6D, 0.45D, 0.6D, 0.035D);
        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH,
                this.getX(), this.getY(0.45D), this.getZ(),
                36, 1.0D, 0.25D, 1.0D, 0.035D);
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.HOSTILE, 1.1F, 0.8F);
    }

    private void summonRootSpirits(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        int count = difficultyProfile().witchSpiritCount();
        for (int index = 0; index < count; index++) {
            Vex spirit = EntityType.VEX.create(serverLevel);
            if (spirit == null) {
                continue;
            }
            double angle = (Math.PI * 2.0D / count) * index + this.random.nextDouble() * 0.4D;
            double x = this.getX() + Math.cos(angle) * 2.2D;
            double z = this.getZ() + Math.sin(angle) * 2.2D;
            spirit.moveTo(x, this.getY() + 0.35D, z, this.random.nextFloat() * 360.0F, 0.0F);
            spirit.setCustomName(Component.translatable("entity.usless_mobs.root_spirit"));
            spirit.getPersistentData().putBoolean(ROOT_SPIRIT_KEY, true);
            spirit.getPersistentData().putInt(ROOT_SPIRIT_TICKS_KEY, 20 * 18);
            spirit.setTarget(target);
            spirit.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 18, 0));
            serverLevel.addFreshEntity(spirit);
        }
        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SPORE_BLOSSOM_AIR,
                this.getX(), this.getY(0.6D), this.getZ(),
                48, 0.85D, 0.25D, 0.85D, 0.04D);
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 1.0F, 0.65F);
    }

    private void rabbitDodge(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 away = this.position().subtract(target.position()).normalize();
        if (away.lengthSqr() < 0.01D) {
            away = new Vec3(this.random.nextDouble() - 0.5D, 0.0D, this.random.nextDouble() - 0.5D).normalize();
        }
        double x = this.getX() + away.x * 4.0D;
        double z = this.getZ() + away.z * 4.0D;
        this.teleportTo(x, this.getY(), z);
        this.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 55, 0));
        this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 70, 2));

        Rabbit decoy = EntityType.RABBIT.create(serverLevel);
        if (decoy != null) {
            decoy.moveTo(target.getX(), target.getY(), target.getZ(), this.random.nextFloat() * 360.0F, 0.0F);
            decoy.setCustomName(Component.translatable("entity.usless_mobs.witch_boss.decoy"));
            decoy.getPersistentData().putBoolean(DECOY_KEY, true);
            decoy.getPersistentData().putInt(DECOY_TICKS_KEY, 80);
            serverLevel.addFreshEntity(decoy);
        }
        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,
                this.getX(), this.getY(0.5D), this.getZ(),
                28, 0.45D, 0.45D, 0.45D, 0.04D);
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.RABBIT_AMBIENT, SoundSource.HOSTILE, 1.0F, 0.75F);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide && dodgeCooldown <= 25 && source.getEntity() instanceof LivingEntity attacker) {
            rabbitDodge(attacker);
            dodgeCooldown = difficultyProfile().cooldown(115);
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        finishRabbitHunt(this.huntTargetId != null && this.level() instanceof ServerLevel serverLevel
                ? serverLevel.getPlayerByUUID(this.huntTargetId)
                : null);
        super.die(source);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && this.huntTargetId != null && this.level() instanceof ServerLevel serverLevel) {
            finishRabbitHunt(serverLevel.getPlayerByUUID(this.huntTargetId));
        }
        super.remove(reason);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource damageSource, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(damageSource, looting, recentlyHit);
        int safeLooting = Math.min(5, Math.max(0, looting));
        int rewardTier = difficultyProfile().rewardTier();
        this.spawnAtLocation(new ItemStack(com.Momik.usless_mobs.registry.ModItems.POTION_OF_LIFE.get(), 1 + rewardTier));
        if (this.random.nextFloat() < Math.min(0.95F, 0.35F + rewardTier * 0.18F + safeLooting * 0.08F)) {
            this.spawnAtLocation(new ItemStack(com.Momik.usless_mobs.registry.ModItems.NATURE_CRYSTAL.get(), 1 + rewardTier / 2));
        }
    }

}
