package com.Momik.usless_mobs.entity;

import com.Momik.usless_mobs.Usless_mobs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;

public class BlueSlimeEntity extends Slime {

    private static final EntityDataAccessor<Boolean> GOLDEN = SynchedEntityData.defineId(BlueSlimeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final ResourceKey<DamageType> SLIMED_DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.tryBuild(Usless_mobs.MODID, "slimed"));
    private static final String GOLDEN_TAG = "Golden";
    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 6;
    private static final double BASE_HEALTH = 6.0D;
    private static final double HEALTH_PER_SIZE = 4.0D;
    private static final double BASE_ATTACK_DAMAGE = 0.5D;
    private static final double ATTACK_DAMAGE_PER_SIZE = 0.75D;
    private static final double BASE_MOVEMENT_SPEED = 0.18D;
    private static final double MOVEMENT_SPEED_PER_SIZE = 0.02D;
    private static final int BASE_XP_REWARD = 1;
    private static final float GOLDEN_VARIANT_CHANCE = 0.18F;

    public BlueSlimeEntity(EntityType<? extends Slime> entityType, Level level) {
        super(entityType, level);
        this.applyScaledStats(this.getSize(), true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, BASE_HEALTH + HEALTH_PER_SIZE)
                .add(Attributes.MOVEMENT_SPEED, BASE_MOVEMENT_SPEED + MOVEMENT_SPEED_PER_SIZE)
                .add(Attributes.ATTACK_DAMAGE, BASE_ATTACK_DAMAGE + ATTACK_DAMAGE_PER_SIZE);
    }

    @SuppressWarnings("unchecked")
    public static boolean checkBlueSlimeSpawnRules(EntityType<BlueSlimeEntity> entityType, LevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (!(level instanceof ServerLevelAccessor serverLevel)) {
            return false;
        }

        return Monster.checkMonsterSpawnRules((EntityType<? extends Monster>) (EntityType<?>) entityType, serverLevel, spawnType, pos, random);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(GOLDEN, false);
    }

    @Override
    public void setSize(int size, boolean heal) {
        int clampedSize = Mth.clamp(size, MIN_SIZE, MAX_SIZE);
        super.setSize(clampedSize, heal);
        this.applyScaledStats(clampedSize, heal);
    }

    @Override
    protected ParticleOptions getParticleType() {
        return new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(this.isGolden() ? Usless_mobs.GOLDENER_SCHLEIMBALL.get() : Usless_mobs.BLAUER_SCHLEIMBALL.get()));
    }

    @Override
    public Component getTypeName() {
        return Component.translatable(this.isGolden() ? "entity.usless_mobs.goldener_schleim" : "entity.usless_mobs.blauer_schleim");
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setGolden(tag.getBoolean(GOLDEN_TAG));
        this.applyScaledStats(this.getSize(), false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(GOLDEN_TAG, this.isGolden());
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, SpawnGroupData spawnGroupData, CompoundTag dataTag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData, dataTag);
        boolean canRollGolden = spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION || spawnType == MobSpawnType.REINFORCEMENT;
        this.setGolden(canRollGolden && level.getRandom().nextFloat() < GOLDEN_VARIANT_CHANCE);
        this.setSize(MIN_SIZE + level.getRandom().nextInt(MAX_SIZE), true);
        return data;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource damageSource, int looting, boolean recentlyHit) {
        if (this.getSize() == MIN_SIZE) {
            ItemStack dropStack = new ItemStack(this.isGolden() ? Usless_mobs.GOLDENER_SCHLEIMBALL.get() : Usless_mobs.BLAUER_SCHLEIMBALL.get(),
                    1 + this.random.nextInt(this.isGolden() ? 3 : 2) + this.random.nextInt(looting + 1));
            this.spawnAtLocation(dropStack);

            if (this.isGolden() && this.random.nextFloat() < 0.35F + (0.1F * looting)) {
                this.spawnAtLocation(new ItemStack(Items.GOLD_NUGGET, 1 + this.random.nextInt(2)));
            }

            if (this.isGolden() && this.random.nextFloat() < 0.06F + (0.02F * looting)) {
                this.spawnAtLocation(new ItemStack(Usless_mobs.SCHLEIMREAKTOR_SCHMIEDEVORLAGE.get()));
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level().isClientSide && this.isGolden() && this.tickCount % 80 == 0) {
            this.emitGoldenAura();
        }
    }

    @Override
    protected void dealDamage(LivingEntity target) {
        if (!this.isAlive() || !this.isDealsDamage()) {
            return;
        }

        int size = this.getSize();
        float attackReach = 0.6F * size;

        if (this.distanceToSqr(target) > (double) (attackReach * attackReach) || !this.hasLineOfSight(target)) {
            return;
        }

        if (!target.hurt(this.createSlimeDamageSource(), (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE))) {
            return;
        }

        this.playSound(net.minecraft.sounds.SoundEvents.SLIME_ATTACK, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
        this.doEnchantDamageEffects(this, target);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40 + size * 20, size >= 4 ? 1 : 0));

        if (this.isGolden()) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80 + size * 15, 0));
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80 + size * 10, 0));
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && this.isDeadOrDying() && !this.isRemoved() && this.getSize() > MIN_SIZE && reason == RemovalReason.KILLED) {
            this.spawnChildren();
            super.setSize(MIN_SIZE, false);
        }

        super.remove(reason);
    }

    public boolean isGolden() {
        return this.entityData.get(GOLDEN);
    }

    private void applyScaledStats(int size, boolean heal) {
        double scaledHealth = BASE_HEALTH + (HEALTH_PER_SIZE * size);
        double scaledMovementSpeed = BASE_MOVEMENT_SPEED + (MOVEMENT_SPEED_PER_SIZE * size);
        double scaledAttackDamage = BASE_ATTACK_DAMAGE + (ATTACK_DAMAGE_PER_SIZE * size);

        if (this.isGolden()) {
            scaledHealth *= 1.45D;
            scaledMovementSpeed += 0.08D;
            scaledAttackDamage += 1.5D;
        }

        if (this.getAttribute(Attributes.MAX_HEALTH) != null) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(scaledHealth);
        }

        if (this.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(scaledMovementSpeed);
        }

        if (this.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(scaledAttackDamage);
        }

        if (heal) {
            this.setHealth(this.getMaxHealth());
        }

        this.xpReward = BASE_XP_REWARD + size;
    }

    private void setGolden(boolean golden) {
        this.entityData.set(GOLDEN, golden);
        this.applyScaledStats(this.getSize(), false);
    }

    private void emitGoldenAura() {
        this.heal(1.0F);

        for (BlueSlimeEntity slime : this.level().getEntitiesOfClass(BlueSlimeEntity.class, this.getBoundingBox().inflate(6.0D), other -> other != this && other.isAlive())) {
            slime.heal(1.0F);
            slime.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 0));
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY(0.5D), this.getZ(), 18, 0.45D, 0.35D, 0.45D, 0.02D);
        }
    }

    private void spawnChildren() {
        int childSize = Math.max(MIN_SIZE, this.getSize() / 2);
        int childCount = 2 + this.random.nextInt(3);
        Component customName = this.getCustomName();
        boolean noAi = this.isNoAi();
        boolean invulnerable = this.isInvulnerable();
        boolean persistent = this.isPersistenceRequired();

        for (int index = 0; index < childCount; index++) {
            float xOffset = ((index % 2) - 0.5F) * this.getSize() / 4.0F;
            float zOffset = ((index / 2) - 0.5F) * this.getSize() / 4.0F;
            BlueSlimeEntity child = Usless_mobs.BLAUER_SCHLEIM.get().create(this.level());

            if (child == null) {
                continue;
            }

            child.setGolden(this.isGolden());
            child.setSize(childSize, true);
            child.moveTo(this.getX() + xOffset, this.getY() + 0.5D, this.getZ() + zOffset, this.random.nextFloat() * 360.0F, 0.0F);

            if (customName != null) {
                child.setCustomName(customName);
            }

            child.setNoAi(noAi);
            child.setInvulnerable(invulnerable);

            if (persistent) {
                child.setPersistenceRequired();
            }

            this.level().addFreshEntity(child);
        }
    }

    private DamageSource createSlimeDamageSource() {
        return new DamageSource(this.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SLIMED_DAMAGE_TYPE), this);
    }
}
