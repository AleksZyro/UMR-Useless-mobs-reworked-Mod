package com.Momik.usless_mobs.entity;

import com.Momik.usless_mobs.allegiance.AllegiancePath;
import com.Momik.usless_mobs.allegiance.AllegianceUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.Difficulty;
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
    private static final int MAX_ROOT_SPIRITS = 4;

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

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.level().isClientSide) {
            return;
        }

        this.bossEvent.setProgress(Math.max(0.0F, this.getHealth() / this.getMaxHealth()));
        tickCooldowns();
        tickRootCageWarmup();
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
        if (target != null && target.isAlive() && this.rootCageCooldown <= 0 && this.rootCageWarmup <= 0
                && this.distanceToSqr(target) <= 14.0D * 14.0D) {
            this.startRootCage(target);
            this.rootCageCooldown = this.level().getDifficulty() == Difficulty.HARD ? 115 : ROOT_CAGE_COOLDOWN;
        }
        if (this.heartPulseCooldown <= 0 && this.getHealth() < this.getMaxHealth()) {
            this.heartPulse();
            this.heartPulseCooldown = this.awakenedRoots ? 95 : HEART_PULSE_COOLDOWN;
        }
        if (this.rootSpiritCooldown <= 0 && this.getHealth() <= this.getMaxHealth() * 0.65F) {
            if (this.awakenedRoots) {
                this.callRootSpirits();
            } else {
                this.callLivingSwarm();
            }
            this.rootSpiritCooldown = this.level().getDifficulty() == Difficulty.HARD ? 145 : ROOT_SPIRIT_COOLDOWN;
        }
        if (this.tickCount % 60 == 0 && this.getHealth() < this.getMaxHealth()) {
            this.heal(this.level().getDifficulty() == Difficulty.HARD ? 3.0F : 2.0F);
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
        target.hurt(this.damageSources().mobAttack(this), this.level().getDifficulty() == Difficulty.HARD ? 8.0F : 5.5F);
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
            if (living.hurt(this.damageSources().mobAttack(this), this.level().getDifficulty() == Difficulty.HARD ? 7.0F : 5.0F)) {
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
        for (int i = 0; i < 2; i++) {
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
        int toSpawn = Math.max(0, Math.min(2, MAX_ROOT_SPIRITS - existing));
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
        this.spawnAtLocation(new ItemStack(com.Momik.usless_mobs.registry.ModItems.LIVING_CORE.get()));
        this.spawnAtLocation(new ItemStack(com.Momik.usless_mobs.registry.ModItems.LIVING_TISSUE.get(), 4 + this.random.nextInt(3 + safeLooting)));
        if (this.random.nextFloat() < Math.min(0.65F, 0.25F + safeLooting * 0.08F)) {
            this.spawnAtLocation(new ItemStack(com.Momik.usless_mobs.registry.ModItems.NATURE_CRYSTAL.get()));
        }
        if (this.random.nextFloat() < Math.min(0.20F, 0.08F + (0.03F * safeLooting))) {
            this.spawnAtLocation(new ItemStack(com.Momik.usless_mobs.registry.ModItems.LIVING_TALISMAN.get()));
        }
        if (this.random.nextFloat() < Math.min(0.12F, 0.04F + (0.01F * safeLooting))) {
            this.spawnAtLocation(new ItemStack(com.Momik.usless_mobs.registry.ModItems.LIVING_CRYSTAL_BLOCK_ITEM.get(),
                    1 + this.random.nextInt(2)));
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide && this.thornCounterCooldown <= 0 && source.getEntity() instanceof LivingEntity attacker
                && attacker.isAlive() && attacker.distanceToSqr(this) <= 7.0D * 7.0D) {
            this.thornCounterCooldown = THORN_COUNTER_COOLDOWN;
            attacker.hurt(this.damageSources().thorns(this), this.level().getDifficulty() == Difficulty.HARD ? 5.0F : 3.0F);
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
