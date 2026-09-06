package com.Momik.usless_mobs.entity;

import com.Momik.usless_mobs.worldgen.WhaleRuinEncounterData;
import java.util.Comparator;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class GiantSquidEntity extends Squid {
    private static final String RUIN_ENCOUNTER_KEY = "RuinEncounterKey";
    private static final String RUIN_ORIGIN_KEY = "RuinOrigin";
    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.usless_mobs.giant_squid"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.NOTCHED_10);

    private String ruinEncounterKey = "";
    private BlockPos ruinOrigin = BlockPos.ZERO;
    private int attackCooldown = 50;
    private int telegraphTicks;
    private Attack pendingAttack = Attack.NONE;
    private int attackSequence;

    public GiantSquidEntity(EntityType<? extends Squid> type, Level level) {
        super(type, level);
        this.xpReward = 120;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 360.0D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.ATTACK_DAMAGE, 13.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.75D);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        bossEvent.setProgress(getHealth() / getMaxHealth());
        ServerPlayer target = nearestTarget(serverLevel);
        if (target == null) {
            return;
        }
        moveTowardTarget(target);
        if (telegraphTicks > 0) {
            telegraphTicks--;
            if (telegraphTicks == 0) {
                executePendingAttack(serverLevel, target);
            }
            return;
        }
        if (attackCooldown > 0) {
            attackCooldown--;
            return;
        }
        beginNextAttack(serverLevel, target);
    }

    @Nullable
    private ServerPlayer nearestTarget(ServerLevel level) {
        return level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(48.0D),
                        player -> player.isAlive() && !player.isSpectator() && !player.isCreative())
                .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
    }

    private void moveTowardTarget(LivingEntity target) {
        double desiredDistance = phase() == Phase.STALKING ? 15.0D : 8.0D;
        Vec3 delta = target.position().subtract(position());
        if (delta.lengthSqr() > desiredDistance * desiredDistance) {
            double speed = phase() == Phase.DESPERATION ? 0.075D : 0.045D;
            setDeltaMovement(getDeltaMovement().scale(0.88D).add(delta.normalize().scale(speed)));
            hurtMarked = true;
        }
    }

    private void beginNextAttack(ServerLevel level, ServerPlayer target) {
        Phase phase = phase();
        int selector = attackSequence++ % (phase.ordinal() + 2);
        if (selector == 0) {
            pendingAttack = Attack.INK;
            telegraphInk(level);
        } else if (selector == 1) {
            pendingAttack = Attack.GRAB;
            telegraphGrab(level, target);
        } else if (selector == 2) {
            pendingAttack = Attack.CURRENT;
            telegraphCurrent(level);
        } else {
            pendingAttack = Attack.DASH;
            telegraphDash(level, target);
        }
        telegraphTicks = phase == Phase.DESPERATION ? 16 : 26;
    }

    private void executePendingAttack(ServerLevel level, ServerPlayer target) {
        switch (pendingAttack) {
            case INK -> releaseInk(level);
            case GRAB -> tentacleGrab(level, target);
            case CURRENT -> applyCurrentPull(level);
            case DASH -> dashAt(target);
            default -> {
            }
        }
        pendingAttack = Attack.NONE;
        attackCooldown = attackCooldownForDifficulty();
    }

    private int attackCooldownForDifficulty() {
        Difficulty difficulty = level().getDifficulty();
        if (difficulty == Difficulty.HARD) {
            return phase() == Phase.DESPERATION ? 24 : 38;
        }
        if (difficulty == Difficulty.EASY) {
            return phase() == Phase.DESPERATION ? 48 : 70;
        }
        return phase() == Phase.DESPERATION ? 34 : 52;
    }

    private void telegraphInk(ServerLevel level) {
        level.sendParticles(ParticleTypes.BUBBLE, getX(), getY(0.55D), getZ(),
                70, 2.4D, 1.2D, 2.4D, 0.04D);
        level.playSound(null, blockPosition(), SoundEvents.SQUID_SQUIRT,
                SoundSource.HOSTILE, 2.0F, 0.45F);
    }

    private void telegraphGrab(ServerLevel level, LivingEntity target) {
        level.sendParticles(ParticleTypes.NAUTILUS, target.getX(), target.getY(0.5D), target.getZ(),
                32, 1.2D, 1.2D, 1.2D, 0.02D);
        level.playSound(null, blockPosition(), SoundEvents.ELDER_GUARDIAN_AMBIENT,
                SoundSource.HOSTILE, 1.7F, 0.7F);
    }

    private void telegraphCurrent(ServerLevel level) {
        level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, getX(), getY(), getZ(),
                90, 5.0D, 2.0D, 5.0D, 0.05D);
        level.playSound(null, blockPosition(), SoundEvents.CONDUIT_AMBIENT,
                SoundSource.HOSTILE, 2.0F, 0.55F);
    }

    private void telegraphDash(ServerLevel level, LivingEntity target) {
        Vec3 line = target.position().subtract(position()).normalize();
        for (int i = 1; i <= 8; i++) {
            Vec3 p = position().add(line.scale(i * 1.5D));
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y + 1.0D, p.z,
                    4, 0.25D, 0.25D, 0.25D, 0.01D);
        }
        level.playSound(null, blockPosition(), SoundEvents.DOLPHIN_JUMP,
                SoundSource.HOSTILE, 2.2F, 0.4F);
    }

    private void releaseInk(ServerLevel level) {
        AABB area = getBoundingBox().inflate(9.0D, 5.0D, 9.0D);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, area,
                player -> !player.isCreative() && !player.isSpectator())) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
        }
        level.sendParticles(ParticleTypes.SQUID_INK, getX(), getY(0.5D), getZ(),
                180, 4.5D, 2.2D, 4.5D, 0.06D);
    }

    private void tentacleGrab(ServerLevel level, ServerPlayer target) {
        if (distanceToSqr(target) > 16.0D * 16.0D) {
            return;
        }
        Vec3 pull = position().subtract(target.position()).normalize().scale(0.9D);
        target.setDeltaMovement(target.getDeltaMovement().add(pull));
        target.hurtMarked = true;
        target.hurt(damageSources().mobAttack(this), scaledDamage(9.0F));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 35, 1));
    }

    private void applyCurrentPull(ServerLevel level) {
        AABB area = getBoundingBox().inflate(18.0D, 8.0D, 18.0D);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, area,
                player -> !player.isCreative() && !player.isSpectator())) {
            Vec3 pull = position().subtract(player.position());
            if (pull.lengthSqr() > 0.1D) {
                player.setDeltaMovement(player.getDeltaMovement().add(pull.normalize().scale(0.48D)));
                player.hurtMarked = true;
            }
        }
    }

    private void dashAt(LivingEntity target) {
        Vec3 direction = target.position().subtract(position()).normalize();
        setDeltaMovement(direction.scale(1.35D));
        hurtMarked = true;
        if (distanceToSqr(target) < 8.0D * 8.0D) {
            target.hurt(damageSources().mobAttack(this), scaledDamage(12.0F));
        }
    }

    private float scaledDamage(float base) {
        if (level().getDifficulty() == Difficulty.HARD) {
            return base * 1.25F;
        }
        if (level().getDifficulty() == Difficulty.EASY) {
            return base * 0.72F;
        }
        return base;
    }

    private Phase phase() {
        float health = getHealth() / getMaxHealth();
        if (health > 0.75F) {
            return Phase.STALKING;
        }
        if (health > 0.45F) {
            return Phase.HUNT;
        }
        if (health > 0.20F) {
            return Phase.RUIN_COLLAPSE;
        }
        return Phase.DESPERATION;
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
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!ruinEncounterKey.isEmpty() && level() instanceof ServerLevel serverLevel) {
            WhaleRuinEncounterData.get(serverLevel.getServer()).markDefeated(ruinEncounterKey);
        }
    }

    public void setRuinEncounterKey(String key) {
        this.ruinEncounterKey = key == null ? "" : key;
    }

    public void setRuinOrigin(BlockPos origin) {
        this.ruinOrigin = origin.immutable();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString(RUIN_ENCOUNTER_KEY, ruinEncounterKey);
        tag.putLong(RUIN_ORIGIN_KEY, ruinOrigin.asLong());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ruinEncounterKey = tag.getString(RUIN_ENCOUNTER_KEY);
        if (tag.contains(RUIN_ORIGIN_KEY)) {
            ruinOrigin = BlockPos.of(tag.getLong(RUIN_ORIGIN_KEY));
        }
        if (hasCustomName()) {
            bossEvent.setName(getDisplayName());
        }
    }

    private enum Phase {
        STALKING,
        HUNT,
        RUIN_COLLAPSE,
        DESPERATION
    }

    private enum Attack {
        NONE,
        INK,
        GRAB,
        CURRENT,
        DASH
    }
}
