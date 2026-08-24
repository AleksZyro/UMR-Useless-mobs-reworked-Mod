package com.Momik.usless_mobs.entity;

import com.Momik.usless_mobs.allegiance.AllegiancePath;
import com.Momik.usless_mobs.allegiance.AllegianceUtil;
import com.Momik.usless_mobs.entity.boss.BossDifficultyProfile;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class LivingBossEntity extends Ravager {
    private static final double ROOT_RADIUS = 7.0D;
    private static final double SUMMON_RADIUS = 9.0D;
    private static final int ROOT_CAGE_COOLDOWN = 155;
    private static final int ROOT_CAGE_WARMUP_TICKS = 26;
    private static final int HEART_PULSE_COOLDOWN = 130;
    private static final int ROOT_SPIRIT_COOLDOWN = 190;
    private static final int THORN_COUNTER_COOLDOWN = 75;
    private static final int ROOT_WAVE_WARMUP_TICKS = 32;
    private static final int GROUND_RUPTURE_WARMUP_TICKS = 38;
    private static final double SAFE_CORRIDOR_HALF_ANGLE = Math.toRadians(28.0D);

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.usless_mobs.living_boss"),
            BossEvent.BossBarColor.GREEN,
            BossEvent.BossBarOverlay.PROGRESS);

    private int rootCageCooldown = 90;
    private int rootCageWarmup = 0;
    private java.util.UUID rootCageTargetId = null;
    private int heartPulseCooldown = 80;
    private int rootSpiritCooldown = 150;
    private int thornCounterCooldown = 0;
    private int rootWaveCooldown = 120;
    private int rootWaveWarmup;
    private double rootWaveSafeAngle;
    private int groundRuptureCooldown = 180;
    private int groundRuptureWarmup;
    private Vec3 groundRuptureCenter;
    private double groundRuptureSafeAngle;
    private boolean awakenedRoots = false;

    public LivingBossEntity(EntityType<? extends Ravager> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 90;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Ravager.createAttributes()
                .add(Attributes.MAX_HEALTH, 220.0D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.31D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.85D)
                .add(Attributes.FOLLOW_RANGE, 42.0D);
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

        this.bossEvent.setProgress(Math.max(0.0F, this.getHealth() / this.getMaxHealth()));
        tickCooldowns();
        tickRootCageWarmup();
        tickRootWaveWarmup();
        tickGroundRuptureWarmup();
        if (this.getTarget() instanceof Player player && AllegianceUtil.hasPath(player, AllegiancePath.LIVING)) {
            this.setTarget(null);
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, true, false, true));
        }
        if (!this.awakenedRoots && this.getHealth() <= this.getMaxHealth() * 0.5F) {
            this.awakenedRoots = true;
            this.bossEvent.setColor(BossEvent.BossBarColor.YELLOW);
            this.heartPulse();
        }
        if (this.tickCount % 20 == 0 && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,
                    this.getX(), this.getY(1.4D), this.getZ(),
                    2, 0.25D, 0.25D, 0.25D, 0.01D);
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.COMPOSTER,
                    this.getX(), this.getY(1.0D), this.getZ(),
                    18, 0.55D, 0.45D, 0.55D, 0.035D);
        }

        if (this.tickCount % 70 == 0) {
            this.rootPulse();
        }
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive() && this.rootWaveCooldown <= 0
                && this.rootWaveWarmup <= 0 && this.groundRuptureWarmup <= 0
                && this.distanceToSqr(target) <= 18.0D * 18.0D) {
            startRootWave(target);
            this.rootWaveCooldown = difficultyProfile().cooldown(165);
        }
        if (target != null && target.isAlive() && this.awakenedRoots && this.groundRuptureCooldown <= 0
                && this.groundRuptureWarmup <= 0 && this.rootWaveWarmup <= 0
                && this.distanceToSqr(target) <= 20.0D * 20.0D) {
            startGroundRupture(target);
            this.groundRuptureCooldown = difficultyProfile().cooldown(220);
        }
        if (target != null && target.isAlive() && this.rootCageCooldown <= 0 && this.rootCageWarmup <= 0
                && this.distanceToSqr(target) <= 14.0D * 14.0D) {
            this.startRootCage(target);
            this.rootCageCooldown = difficultyProfile().cooldown(ROOT_CAGE_COOLDOWN);
        }
        if (this.heartPulseCooldown <= 0 && this.getHealth() < this.getMaxHealth()) {
            this.heartPulse();
            this.heartPulseCooldown = difficultyProfile().cooldown(this.awakenedRoots ? 95 : HEART_PULSE_COOLDOWN);
        }
        if (this.rootSpiritCooldown <= 0 && this.getHealth() <= this.getMaxHealth() * 0.65F) {
            if (this.awakenedRoots) {
                this.callRootSpirits();
            } else {
                this.callLivingSwarm();
            }
            this.rootSpiritCooldown = difficultyProfile().cooldown(ROOT_SPIRIT_COOLDOWN);
        }
        if (this.tickCount % 60 == 0 && this.getHealth() < this.getMaxHealth()) {
            this.heal(1.5F + difficultyProfile().rewardTier() * 0.75F);
        }
    }

    private void tickCooldowns() {
        if (this.rootCageCooldown > 0) {
            this.rootCageCooldown--;
        }
        if (this.heartPulseCooldown > 0) {
            this.heartPulseCooldown--;
        }
        if (this.rootSpiritCooldown > 0) {
            this.rootSpiritCooldown--;
        }
        if (this.thornCounterCooldown > 0) {
            this.thornCounterCooldown--;
        }
        if (this.rootWaveCooldown > 0) {
            this.rootWaveCooldown--;
        }
        if (this.groundRuptureCooldown > 0) {
            this.groundRuptureCooldown--;
        }
    }

    private void syncDifficultyAttackDamage() {
        var attackDamage = this.getAttribute(Attributes.ATTACK_DAMAGE);
        double scaledDamage = 15.0D * difficultyProfile().damageMultiplier();
        if (attackDamage != null && Math.abs(attackDamage.getBaseValue() - scaledDamage) > 0.001D) {
            attackDamage.setBaseValue(scaledDamage);
        }
    }

    private void startRootWave(LivingEntity target) {
        this.rootWaveWarmup = ROOT_WAVE_WARMUP_TICKS;
        this.rootWaveSafeAngle = this.random.nextDouble() * Math.PI * 2.0D;
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                    SoundSource.HOSTILE, 1.15F, 0.55F);
        }
    }

    private void tickRootWaveWarmup() {
        if (this.rootWaveWarmup <= 0 || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        this.rootWaveWarmup--;
        double progress = 1.0D - this.rootWaveWarmup / (double) ROOT_WAVE_WARMUP_TICKS;
        double radius = 2.0D + progress * 8.0D;
        for (int index = 0; index < 36; index++) {
            double angle = Math.PI * 2.0D * index / 36.0D;
            boolean safe = isInsideSafeCorridor(angle, this.rootWaveSafeAngle);
            serverLevel.sendParticles(safe
                            ? net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER
                            : net.minecraft.core.particles.ParticleTypes.COMPOSTER,
                    this.getX() + Math.cos(angle) * radius,
                    this.getY() + 0.12D,
                    this.getZ() + Math.sin(angle) * radius,
                    1, 0.02D, 0.02D, 0.02D, 0.0D);
        }
        if (this.rootWaveWarmup == 0) {
            releaseRootWave(serverLevel);
        }
    }

    private void releaseRootWave(ServerLevel serverLevel) {
        double radius = this.awakenedRoots ? 11.0D : 9.0D;
        float damage = difficultyProfile().damage(7.0F);
        for (LivingEntity living : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(radius, 2.0D, radius), this::isLivingBossTarget)) {
            Vec3 offset = living.position().subtract(this.position());
            double angle = Math.atan2(offset.z, offset.x);
            if (isInsideSafeCorridor(angle, this.rootWaveSafeAngle)) {
                continue;
            }
            living.hurt(this.damageSources().mobAttack(this), damage);
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 65, 1));
            Vec3 push = offset.multiply(1.0D, 0.0D, 1.0D).normalize().scale(0.9D);
            living.push(push.x, 0.35D, push.z);
            living.hurtMarked = true;
        }
        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SPORE_BLOSSOM_AIR,
                this.getX(), this.getY(0.2D), this.getZ(), 130, radius * 0.45D, 0.2D, radius * 0.45D, 0.06D);
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.45F, 0.62F);
    }

    private void startGroundRupture(LivingEntity target) {
        this.groundRuptureWarmup = GROUND_RUPTURE_WARMUP_TICKS;
        this.groundRuptureCenter = target.position();
        this.groundRuptureSafeAngle = this.random.nextDouble() * Math.PI * 2.0D;
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, target.blockPosition(), SoundEvents.ROOTED_DIRT_PLACE,
                    SoundSource.HOSTILE, 1.3F, 0.4F);
        }
    }

    private void tickGroundRuptureWarmup() {
        if (this.groundRuptureWarmup <= 0 || this.groundRuptureCenter == null
                || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        this.groundRuptureWarmup--;
        for (int index = 0; index < 32; index++) {
            double angle = Math.PI * 2.0D * index / 32.0D;
            boolean safe = isInsideSafeCorridor(angle, this.groundRuptureSafeAngle);
            double radius = 1.2D + (index % 4) * 1.25D;
            serverLevel.sendParticles(safe
                            ? net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER
                            : net.minecraft.core.particles.ParticleTypes.FALLING_SPORE_BLOSSOM,
                    this.groundRuptureCenter.x + Math.cos(angle) * radius,
                    this.groundRuptureCenter.y + 0.08D,
                    this.groundRuptureCenter.z + Math.sin(angle) * radius,
                    1, 0.03D, 0.02D, 0.03D, 0.0D);
        }
        if (this.groundRuptureWarmup == 0) {
            releaseGroundRupture(serverLevel);
        }
    }

    private void releaseGroundRupture(ServerLevel serverLevel) {
        Vec3 center = this.groundRuptureCenter;
        this.groundRuptureCenter = null;
        float damage = difficultyProfile().damage(8.0F);
        AABB area = new AABB(center, center).inflate(6.2D, 2.0D, 6.2D);
        for (LivingEntity living : serverLevel.getEntitiesOfClass(LivingEntity.class, area, this::isLivingBossTarget)) {
            Vec3 offset = living.position().subtract(center);
            if (isInsideSafeCorridor(Math.atan2(offset.z, offset.x), this.groundRuptureSafeAngle)) {
                continue;
            }
            living.hurt(this.damageSources().mobAttack(this), damage);
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 90, 1));
            living.push(0.0D, 0.7D, 0.0D);
            living.hurtMarked = true;
        }
        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.COMPOSTER,
                center.x, center.y + 0.15D, center.z, 150, 2.6D, 0.25D, 2.6D, 0.08D);
        serverLevel.playSound(null, net.minecraft.core.BlockPos.containing(center),
                SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.1F, 0.55F);
    }

    private static boolean isInsideSafeCorridor(double angle, double safeAngle) {
        double difference = Math.atan2(Math.sin(angle - safeAngle), Math.cos(angle - safeAngle));
        return Math.abs(difference) <= SAFE_CORRIDOR_HALF_ANGLE;
    }

    private void startRootCage(LivingEntity target) {
        this.rootCageTargetId = target.getUUID();
        this.rootCageWarmup = ROOT_CAGE_WARMUP_TICKS;
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.COMPOSTER,
                    target.getX(), target.getY(0.12D), target.getZ(),
                    32, 1.0D, 0.08D, 1.0D, 0.02D);
            serverLevel.playSound(null, target.blockPosition(), SoundEvents.ROOTED_DIRT_PLACE, SoundSource.HOSTILE, 1.1F, 0.55F);
        }
    }

    private void tickRootCageWarmup() {
        if (this.rootCageWarmup <= 0 || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        this.rootCageWarmup--;
        Entity entity = this.rootCageTargetId == null ? null : serverLevel.getEntity(this.rootCageTargetId);
        if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
            this.rootCageWarmup = 0;
            this.rootCageTargetId = null;
            return;
        }
        double radius = 1.4D;
        for (int i = 0; i < 12; i++) {
            double angle = (Math.PI * 2.0D / 12.0D) * i;
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SPORE_BLOSSOM_AIR,
                    target.getX() + Math.cos(angle) * radius,
                    target.getY() + 0.08D,
                    target.getZ() + Math.sin(angle) * radius,
                    1, 0.02D, 0.03D, 0.02D, 0.0D);
        }
        if (this.rootCageWarmup <= 0) {
            this.releaseRootCage(serverLevel, target);
            this.rootCageTargetId = null;
        }
    }

    private void releaseRootCage(ServerLevel serverLevel, LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, this.awakenedRoots ? 150 : 110, this.awakenedRoots ? 3 : 2));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 120, 1));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
        target.hurt(this.damageSources().mobAttack(this), difficultyProfile().damage(5.5F));
        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.COMPOSTER,
                target.getX(), target.getY(0.35D), target.getZ(),
                56, 0.75D, 0.3D, 0.75D, 0.07D);
        serverLevel.playSound(null, target.blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 1.35F, 0.5F);
    }

    private void heartPulse() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        this.heal(this.awakenedRoots ? 10.0F : 6.0F);
        AABB area = this.getBoundingBox().inflate(this.awakenedRoots ? 9.0D : 6.0D, 1.6D, this.awakenedRoots ? 9.0D : 6.0D);
        for (LivingEntity living : this.level().getEntitiesOfClass(LivingEntity.class, area, living -> living.isAlive() && living != this)) {
            if (living instanceof WebCaveSpiderEntity || living instanceof Vex && living.getPersistentData().getBoolean(WitchBossEntity.ROOT_SPIRIT_KEY)) {
                living.heal(3.0F);
                living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 70, 0));
                continue;
            }
            if (this.isLivingBossTarget(living)) {
                Vec3 push = living.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
                if (push.lengthSqr() > 0.01D) {
                    push = push.normalize().scale(0.55D);
                    living.push(push.x, 0.18D, push.z);
                    living.hurtMarked = true;
                }
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 1));
            }
        }
        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,
                this.getX(), this.getY(1.35D), this.getZ(),
                16, 0.85D, 0.45D, 0.85D, 0.04D);
        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SPORE_BLOSSOM_AIR,
                this.getX(), this.getY(0.45D), this.getZ(),
                70, 1.6D, 0.25D, 1.6D, 0.045D);
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 0.85F, 0.65F);
    }

    private void rootPulse() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        int hits = 0;
        AABB area = this.getBoundingBox().inflate(ROOT_RADIUS, 1.2D, ROOT_RADIUS);
        for (LivingEntity living : this.level().getEntitiesOfClass(LivingEntity.class, area, this::isLivingBossTarget)) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 2));
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
            if (living.hurt(this.damageSources().mobAttack(this), difficultyProfile().damage(5.0F))) {
                hits++;
            }
        }

        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.COMPOSTER,
                this.getX(), this.getY(0.2D), this.getZ(),
                60, ROOT_RADIUS * 0.35D, 0.25D, ROOT_RADIUS * 0.35D, 0.05D);
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 1.4F, 0.55F);
        if (hits > 0) {
            this.heal(Math.min(10.0F, hits * 2.0F));
        }
    }

    private void callLivingSwarm() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LivingEntity target = this.getTarget();
        int summonCount = Math.max(1, difficultyProfile().livingSummonCap() / 2);
        for (int i = 0; i < summonCount; i++) {
            WebCaveSpiderEntity spider = com.Momik.usless_mobs.registry.ModEntities.WEB_CAVE_SPIDER.get().create(serverLevel);
            if (spider == null) {
                continue;
            }
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            double distance = 2.5D + this.random.nextDouble() * SUMMON_RADIUS;
            spider.moveTo(this.getX() + Math.cos(angle) * distance, this.getY(), this.getZ() + Math.sin(angle) * distance,
                    this.random.nextFloat() * 360.0F, 0.0F);
            spider.setPersistenceRequired();
            if (target != null) {
                spider.setTarget(target);
            }
            serverLevel.addFreshEntity(spider);
        }
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.SPIDER_AMBIENT, SoundSource.HOSTILE, 1.2F, 0.65F);
    }

    private void callRootSpirits() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        int existing = serverLevel.getEntitiesOfClass(Vex.class, this.getBoundingBox().inflate(18.0D),
                vex -> vex.isAlive() && vex.getPersistentData().getBoolean(WitchBossEntity.ROOT_SPIRIT_KEY)).size();
        int summonCap = difficultyProfile().livingSummonCap();
        int toSpawn = Math.max(0, Math.min(Math.max(1, summonCap / 2), summonCap - existing));
        LivingEntity target = this.getTarget();
        for (int index = 0; index < toSpawn; index++) {
            Vex spirit = EntityType.VEX.create(serverLevel);
            if (spirit == null) {
                continue;
            }
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            double distance = 2.0D + this.random.nextDouble() * 4.0D;
            spirit.moveTo(this.getX() + Math.cos(angle) * distance, this.getY() + 0.4D, this.getZ() + Math.sin(angle) * distance,
                    this.random.nextFloat() * 360.0F, 0.0F);
            spirit.setCustomName(Component.translatable("entity.usless_mobs.root_spirit"));
            spirit.getPersistentData().putBoolean(WitchBossEntity.ROOT_SPIRIT_KEY, true);
            spirit.getPersistentData().putInt(WitchBossEntity.ROOT_SPIRIT_TICKS_KEY, 20 * 20);
            spirit.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 20, 0));
            if (target != null) {
                spirit.setTarget(target);
            }
            serverLevel.addFreshEntity(spirit);
        }
        if (toSpawn > 0) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SPORE_BLOSSOM_AIR,
                    this.getX(), this.getY(0.7D), this.getZ(),
                    42, 0.9D, 0.35D, 0.9D, 0.04D);
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.HOSTILE, 1.1F, 0.55F);
        }
    }

    private boolean isLivingBossTarget(LivingEntity living) {
        return living.isAlive()
                && living != this
                && !(living instanceof LivingBossEntity)
                && !(living instanceof Player player && AllegianceUtil.hasPath(player, AllegiancePath.LIVING))
                && (living instanceof Player || living == this.getTarget());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource damageSource, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(damageSource, looting, recentlyHit);
        int safeLooting = Math.min(5, Math.max(0, looting));
        int rewardTier = difficultyProfile().rewardTier();
        this.spawnAtLocation(new ItemStack(com.Momik.usless_mobs.registry.ModItems.LIVING_CORE.get()));
        this.spawnAtLocation(new ItemStack(com.Momik.usless_mobs.registry.ModItems.LIVING_TISSUE.get(),
                4 + rewardTier * 2 + this.random.nextInt(3 + safeLooting)));
        if (this.random.nextFloat() < Math.min(0.90F, 0.25F + rewardTier * 0.20F + safeLooting * 0.08F)) {
            this.spawnAtLocation(new ItemStack(com.Momik.usless_mobs.registry.ModItems.NATURE_CRYSTAL.get(), 1 + rewardTier / 2));
        }
        if (this.random.nextFloat() < Math.min(0.55F, 0.08F + rewardTier * 0.16F + (0.03F * safeLooting))) {
            this.spawnAtLocation(new ItemStack(com.Momik.usless_mobs.registry.ModItems.LIVING_TALISMAN.get()));
        }
        if (this.random.nextFloat() < Math.min(0.35F, 0.04F + rewardTier * 0.11F + (0.01F * safeLooting))) {
            this.spawnAtLocation(new ItemStack(com.Momik.usless_mobs.registry.ModItems.LIVING_CRYSTAL_BLOCK_ITEM.get(),
                    1 + this.random.nextInt(2)));
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide && this.thornCounterCooldown <= 0 && source.getEntity() instanceof LivingEntity attacker
                && attacker.isAlive() && attacker.distanceToSqr(this) <= 7.0D * 7.0D) {
            this.thornCounterCooldown = difficultyProfile().cooldown(THORN_COUNTER_COOLDOWN);
            attacker.hurt(this.damageSources().thorns(this), difficultyProfile().damage(3.0F));
            attacker.addEffect(new MobEffectInstance(MobEffects.POISON, 70, 0));
            attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 55, 1));
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SPORE_BLOSSOM_AIR,
                        attacker.getX(), attacker.getY(0.7D), attacker.getZ(),
                        16, 0.35D, 0.35D, 0.35D, 0.02D);
                serverLevel.playSound(null, attacker.blockPosition(), SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.HOSTILE, 0.9F, 0.7F);
            }
        }
        return hurt;
    }
}
