package com.Momik.usless_mobs.entity;

import com.Momik.usless_mobs.registry.ModSounds;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class HelpingAllayEntity extends Allay {
    public static final byte ACTION_IDLE = 0;
    public static final byte ACTION_REVEAL = 1;
    public static final byte ACTION_SHIELD = 2;
    public static final byte ACTION_HEAL = 3;
    public static final byte ACTION_BOND = 4;
    public static final byte ACTION_TELEPORT = 5;

    private static final String OWNER_TAG = "HelpingAllayOwner";
    private static final String SUPPORT_UNTIL_TAG = "HelpingAllaySupportUntil";
    private static final String ACTION_TAG = "HelpingAllayAction";

    private static final EntityDataAccessor<Optional<UUID>> OWNER =
            SynchedEntityData.defineId(HelpingAllayEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Long> SUPPORT_UNTIL =
            SynchedEntityData.defineId(HelpingAllayEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Byte> ACTION =
            SynchedEntityData.defineId(HelpingAllayEntity.class, EntityDataSerializers.BYTE);

    private int actionTicks;

    public HelpingAllayEntity(EntityType<? extends Allay> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Allay.createAttributes();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER, Optional.empty());
        this.entityData.define(SUPPORT_UNTIL, 0L);
        this.entityData.define(ACTION, ACTION_IDLE);
    }

    public void bind(UUID owner, long supportUntil) {
        this.entityData.set(OWNER, Optional.of(owner));
        this.entityData.set(SUPPORT_UNTIL, supportUntil);
        playAction(ACTION_BOND, 24);
    }

    public void extendSupport(UUID owner, long now, long duration) {
        long current = Math.max(now, this.entityData.get(SUPPORT_UNTIL));
        this.entityData.set(OWNER, Optional.of(owner));
        this.entityData.set(SUPPORT_UNTIL, current + duration);
        playAction(ACTION_BOND, 18);
    }

    public Optional<UUID> ownerId() {
        return this.entityData.get(OWNER);
    }

    public long supportUntil() {
        return this.entityData.get(SUPPORT_UNTIL);
    }

    public byte action() {
        return this.entityData.get(ACTION);
    }

    public void playAction(byte action, int ticks) {
        this.entityData.set(ACTION, action);
        this.actionTicks = Math.max(1, ticks);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (this.actionTicks > 0 && --this.actionTicks == 0) {
            this.entityData.set(ACTION, ACTION_IDLE);
        }

        long now = serverLevel.getGameTime();
        if (this.supportUntil() <= now) {
            return;
        }
        Player owner = this.ownerId().map(serverLevel::getPlayerByUUID).orElse(null);
        if (owner == null || !owner.isAlive()) {
            return;
        }

        double distance = this.distanceToSqr(owner);
        if (distance > 24.0D * 24.0D) {
            this.teleportTo(owner.getX(), owner.getY() + 0.5D, owner.getZ());
            playAction(ACTION_TELEPORT, 10);
            serverLevel.playSound(null, this.blockPosition(), ModSounds.HELPING_ALLAY_RETURN.get(), SoundSource.NEUTRAL, 0.75F, 1.35F);
        } else if (distance > 7.0D * 7.0D) {
            this.getNavigation().moveTo(owner, 1.15D);
        }

        if (this.tickCount % 40 == 0) {
            revealThreats(serverLevel, owner);
        }
        if (this.tickCount % 120 == 0
                && owner.distanceToSqr(this) < 10.0D * 10.0D
                && owner.getHealth() < owner.getMaxHealth()) {
            owner.heal(1.0F);
            owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 45, 0, true, false, true));
            playAction(ACTION_HEAL, 26);
            serverLevel.playSound(null, this.blockPosition(), ModSounds.HELPING_ALLAY_HEAL.get(), SoundSource.NEUTRAL, 0.70F, 1.25F);
        }
    }

    private void revealThreats(ServerLevel serverLevel, Player owner) {
        int pressuredMonsters = 0;
        for (Monster monster : serverLevel.getEntitiesOfClass(
                Monster.class,
                owner.getBoundingBox().inflate(8.0D),
                monster -> monster.isAlive() && monster.hasLineOfSight(owner))) {
            pressuredMonsters++;
            monster.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0));
            monster.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
        }
        if (pressuredMonsters > 0) {
            playAction(ACTION_REVEAL, 18);
            serverLevel.playSound(null, this.blockPosition(), ModSounds.HELPING_ALLAY_REVEAL.get(), SoundSource.NEUTRAL, 0.58F, 1.35F);
        }
        if (pressuredMonsters > 0
                && this.tickCount % 80 == 0
                && owner.distanceToSqr(this) < 10.0D * 10.0D) {
            owner.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 0, true, false, true));
            owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 65, 0, true, false, true));
            serverLevel.sendParticles(
                    ParticleTypes.GLOW,
                    owner.getX(), owner.getY(0.9D), owner.getZ(),
                    9, 0.35D, 0.45D, 0.35D, 0.01D);
            serverLevel.playSound(
                    null,
                    owner.blockPosition(),
                    SoundEvents.ALLAY_AMBIENT_WITH_ITEM,
                    SoundSource.NEUTRAL,
                    0.55F,
                    1.45F);
            playAction(ACTION_SHIELD, 28);
            serverLevel.playSound(null, this.blockPosition(), ModSounds.HELPING_ALLAY_SHIELD.get(), SoundSource.NEUTRAL, 0.78F, 1.15F);
        }
        serverLevel.sendParticles(
                ParticleTypes.NOTE,
                this.getX(), this.getY(0.8D), this.getZ(),
                5, 0.18D, 0.18D, 0.18D, 0.0D);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        this.ownerId().ifPresent(owner -> tag.putUUID(OWNER_TAG, owner));
        tag.putLong(SUPPORT_UNTIL_TAG, this.supportUntil());
        tag.putByte(ACTION_TAG, this.action());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(OWNER, tag.hasUUID(OWNER_TAG) ? Optional.of(tag.getUUID(OWNER_TAG)) : Optional.empty());
        this.entityData.set(SUPPORT_UNTIL, tag.getLong(SUPPORT_UNTIL_TAG));
        this.entityData.set(ACTION, tag.getByte(ACTION_TAG));
        this.actionTicks = this.action() == ACTION_IDLE ? 0 : 1;
    }
}
