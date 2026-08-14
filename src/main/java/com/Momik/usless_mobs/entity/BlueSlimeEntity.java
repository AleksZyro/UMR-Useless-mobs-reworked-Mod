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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;

public class BlueSlimeEntity extends Slime {

    private static final EntityDataAccessor<Boolean> GOLDEN = SynchedEntityData.defineId(BlueSlimeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SHOOTS_SPIKES = SynchedEntityData.defineId(BlueSlimeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final ResourceKey<DamageType> SLIMED_DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.tryBuild(Usless_mobs.MODID, "slimed"));
    private static final String GOLDEN_TAG = "Golden";
    private static final String SPIKES_TAG = "ShootsSpikes";
    private static final int SPIKE_COOLDOWN_MIN = 40;
    private static final int SPIKE_COOLDOWN_VAR = 40;
    private static final double SPIKE_RANGE_SQR = 256.0D;
    private int spikeCooldown = SPIKE_COOLDOWN_MIN;
    private boolean wasAirborne = false;
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
        this.entityData.define(SHOOTS_SPIKES, false);
    }

    @Override
    public void setSize(int size, boolean heal) {
        int clampedSize = Mth.clamp(size, MIN_SIZE, MAX_SIZE);
        super.setSize(clampedSize, heal);
        this.applyScaledStats(clampedSize, heal);
    }

    @Override
    protected ParticleOptions getParticleType() {
        return new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(this.isGolden() ? com.Momik.usless_mobs.registry.ModItems.GOLDENER_SCHLEIMBALL.get() : com.Momik.usless_mobs.registry.ModItems.BLAUER_SCHLEIMBALL.get()));
    }

    @Override
    public Component getTypeName() {
        return Component.translatable(this.isGolden() ? "entity.usless_mobs.goldener_schleim" : "entity.usless_mobs.blauer_schleim");
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setGolden(tag.getBoolean(GOLDEN_TAG));
        this.setShootsSpikes(tag.getBoolean(SPIKES_TAG));
        this.applyScaledStats(this.getSize(), false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(GOLDEN_TAG, this.isGolden());
        tag.putBoolean(SPIKES_TAG, this.isShootsSpikes());
    }

    public boolean isShootsSpikes() {
        return this.entityData.get(SHOOTS_SPIKES);
    }

    public void setShootsSpikes(boolean value) {
        this.entityData.set(SHOOTS_SPIKES, value);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, SpawnGroupData spawnGroupData, CompoundTag dataTag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData, dataTag);
        boolean canRollGolden = spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION || spawnType == MobSpawnType.REINFORCEMENT;
        this.setGolden(canRollGolden && level.getRandom().nextFloat() < GOLDEN_VARIANT_CHANCE);
        this.setSize(MIN_SIZE + level.getRandom().nextInt(MAX_SIZE), true);
        this.rollEquipment(level.getRandom(), difficulty);
        return data;
    }

    private void rollEquipment(RandomSource random, DifficultyInstance difficulty) {
        if (this.getSize() < 3) {
            return;
        }

        float difficultyMult = difficulty.getSpecialMultiplier();
        boolean golden = this.isGolden();
        int size = this.getSize();
        float weaponChance = golden ? 0.30F : 0.20F;
        float armorBaseChance = (golden ? 0.35F : 0.22F) * difficultyMult;

        // Drop chances bumped from 0.06-0.08 to 0.40-0.50 so kills actually yield gear.
        float weaponDropChance = 0.50F;
        float armorDropChance = 0.40F;

        // Enchantment chance: 30% for golden+large, 15% otherwise.
        float enchantChance = (golden && size >= 5) ? 0.30F : 0.15F;

        if (random.nextFloat() < weaponChance) {
            ItemStack weapon = pickWeapon(random, golden);
            if (random.nextFloat() < enchantChance) {
                weapon = EnchantmentHelper.enchantItem(random, weapon, 10 + random.nextInt(15), false);
            }
            this.setItemSlot(EquipmentSlot.MAINHAND, weapon);
            this.setDropChance(EquipmentSlot.MAINHAND, weaponDropChance);
        }

        // Each armor slot rolled independently with tapered chance,
        // but no longer ridiculously low for legs/feet so all 4 tiers get tested.
        rollArmorPiece(random, golden, size, armorBaseChance,        EquipmentSlot.HEAD,  armorDropChance, enchantChance);
        rollArmorPiece(random, golden, size, armorBaseChance * 0.7F, EquipmentSlot.CHEST, armorDropChance, enchantChance);
        rollArmorPiece(random, golden, size, armorBaseChance * 0.5F, EquipmentSlot.LEGS,  armorDropChance, enchantChance);
        rollArmorPiece(random, golden, size, armorBaseChance * 0.3F, EquipmentSlot.FEET,  armorDropChance, enchantChance);
    }

    private void rollArmorPiece(RandomSource random, boolean golden, int size, float chance,
                                EquipmentSlot slot, float dropChance, float enchantChance) {
        if (random.nextFloat() >= chance) {
            return;
        }
        ItemStack armor = pickArmor(random, golden, size, slot);
        if (armor.isEmpty()) {
            return;
        }
        if (random.nextFloat() < enchantChance) {
            armor = EnchantmentHelper.enchantItem(random, armor, 5 + random.nextInt(15), false);
        }
        this.setItemSlot(slot, armor);
        this.setDropChance(slot, dropChance);
    }

    private static ItemStack pickWeapon(RandomSource random, boolean golden) {
        if (golden) {
            // Golden slimes only carry diamond + gold gear.
            float roll = random.nextFloat();
            if (roll < 0.30F) return new ItemStack(Items.DIAMOND_SWORD);
            if (roll < 0.65F) return new ItemStack(Items.GOLDEN_SWORD);
            return new ItemStack(Items.GOLDEN_AXE);
        }
        float roll = random.nextFloat();
        if (roll < 0.55F) return new ItemStack(Items.IRON_SWORD);
        if (roll < 0.80F) return new ItemStack(Items.IRON_AXE);
        return new ItemStack(Items.STONE_SWORD);
    }

    /**
     * Tiered armor pick — smaller slimes get weaker tiers, large + golden may get netherite.
     * Non-golden: chainmail → iron (size 3-6).
     * Golden:     gold → diamond → netherite (size 3-6).
     */
    private static ItemStack pickArmor(RandomSource random, boolean golden, int size, EquipmentSlot slot) {
        if (golden) {
            // Golden slimes only wear diamond + gold armor. Larger ones get diamond more often.
            float roll = random.nextFloat();
            float diamondChance = size >= 5 ? 0.35F : size >= 4 ? 0.25F : 0.15F;
            if (roll < diamondChance) {
                return diamondArmor(slot);
            }
            return goldenArmor(slot);
        }
        float roll = random.nextFloat();
        if (size <= 3 && roll < 0.40F) {
            return chainmailArmor(slot);
        }
        return ironArmor(slot);
    }

    private static ItemStack chainmailArmor(EquipmentSlot slot) {
        switch (slot) {
            case HEAD:  return new ItemStack(Items.CHAINMAIL_HELMET);
            case CHEST: return new ItemStack(Items.CHAINMAIL_CHESTPLATE);
            case LEGS:  return new ItemStack(Items.CHAINMAIL_LEGGINGS);
            case FEET:  return new ItemStack(Items.CHAINMAIL_BOOTS);
            default:    return ItemStack.EMPTY;
        }
    }

    private static ItemStack ironArmor(EquipmentSlot slot) {
        switch (slot) {
            case HEAD:  return new ItemStack(Items.IRON_HELMET);
            case CHEST: return new ItemStack(Items.IRON_CHESTPLATE);
            case LEGS:  return new ItemStack(Items.IRON_LEGGINGS);
            case FEET:  return new ItemStack(Items.IRON_BOOTS);
            default:    return ItemStack.EMPTY;
        }
    }

    private static ItemStack goldenArmor(EquipmentSlot slot) {
        switch (slot) {
            case HEAD:  return new ItemStack(Items.GOLDEN_HELMET);
            case CHEST: return new ItemStack(Items.GOLDEN_CHESTPLATE);
            case LEGS:  return new ItemStack(Items.GOLDEN_LEGGINGS);
            case FEET:  return new ItemStack(Items.GOLDEN_BOOTS);
            default:    return ItemStack.EMPTY;
        }
    }

    private static ItemStack diamondArmor(EquipmentSlot slot) {
        switch (slot) {
            case HEAD:  return new ItemStack(Items.DIAMOND_HELMET);
            case CHEST: return new ItemStack(Items.DIAMOND_CHESTPLATE);
            case LEGS:  return new ItemStack(Items.DIAMOND_LEGGINGS);
            case FEET:  return new ItemStack(Items.DIAMOND_BOOTS);
            default:    return ItemStack.EMPTY;
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource damageSource, int looting, boolean recentlyHit) {
        if (this.getSize() == MIN_SIZE) {
            int safeLooting = Math.min(5, Math.max(0, looting));
            ItemStack dropStack = new ItemStack(this.isGolden() ? com.Momik.usless_mobs.registry.ModItems.GOLDENER_SCHLEIMBALL.get() : com.Momik.usless_mobs.registry.ModItems.BLAUER_SCHLEIMBALL.get(),
                    1 + this.random.nextInt(this.isGolden() ? 3 : 2) + this.random.nextInt(safeLooting + 1));
            this.spawnAtLocation(dropStack);

            if (this.isGolden() && this.random.nextFloat() < Math.min(0.80F, 0.35F + (0.1F * safeLooting))) {
                this.spawnAtLocation(new ItemStack(Items.GOLD_NUGGET, 1 + this.random.nextInt(2)));
            }

            if (this.isGolden() && this.random.nextFloat() < 0.05F) {
                this.spawnAtLocation(new ItemStack(com.Momik.usless_mobs.registry.ModItems.SCHLEIMKERN.get()));
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level().isClientSide && this.isGolden() && this.tickCount % 80 == 0) {
            this.emitGoldenAura();
        }
        if (!this.level().isClientSide && this.isGolden() && this.getPersistentData().getBoolean(KING_SPLIT_MINION_TAG)) {
            this.tickGoldenSplitRole();
        }

        if (this.isShootsSpikes()) {
            this.tickSpikeShooting();
        }

        this.tickLandTrail();
    }

    private void tickLandTrail() {
        boolean onGroundNow = this.onGround();
        if (this.wasAirborne && onGroundNow && this.level() instanceof ServerLevel serverLevel) {
            int count = 3 + this.getSize();
            double spread = 0.2D + this.getSize() * 0.05D;
            serverLevel.sendParticles(this.getParticleType(),
                    this.getX(), this.getY() + 0.05D, this.getZ(),
                    count, spread, 0.05D, spread, 0.05D);
        }
        this.wasAirborne = !onGroundNow;
    }

    private void tickSpikeShooting() {
        if (this.level().isClientSide) {
            if (this.tickCount % 6 == 0) {
                this.level().addParticle(ParticleTypes.CRIT,
                    this.getX() + (this.random.nextDouble() - 0.5D) * 0.6D,
                    this.getY(0.6D),
                    this.getZ() + (this.random.nextDouble() - 0.5D) * 0.6D,
                    0.0D, 0.05D, 0.0D);
            }
            return;
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        if (this.distanceToSqr(target) > SPIKE_RANGE_SQR || !this.hasLineOfSight(target)) {
            return;
        }

        this.spikeCooldown--;
        if (this.spikeCooldown <= 0) {
            this.shootSpike(target);
            this.spikeCooldown = SPIKE_COOLDOWN_MIN + this.random.nextInt(SPIKE_COOLDOWN_VAR);
        }
    }

    private void shootSpike(LivingEntity target) {
        SlimeSpikeProjectile spike = new SlimeSpikeProjectile(this.level(), this);
        double startY = this.getY(0.6D);
        double targetX = target.getX();
        double targetY = target.getY(0.5D);
        double targetZ = target.getZ();
        double dx = targetX - this.getX();
        double dy = targetY - startY;
        double dz = targetZ - this.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        spike.setPos(this.getX(), startY, this.getZ());
        spike.shoot(dx, dy + dist * 0.12D, dz, 1.3F, 6.0F);
        this.level().addFreshEntity(spike);
        this.playSound(net.minecraft.sounds.SoundEvents.SLIME_SQUISH, 0.9F, 1.4F);
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

    public static final String FINAL_SHIELD_MINION_TAG = "UslessMobs_FinalShieldMinion";
    public static final String KING_SPLIT_MINION_TAG = "UslessMobs_KingSplitMinion";
    private static final String GOLDEN_SPLIT_ROLE_TAG = "UslessMobs_GoldenSplitRole";

    @Override
    public void remove(RemovalReason reason) {
        boolean shieldMinion = this.getPersistentData().getBoolean(FINAL_SHIELD_MINION_TAG);
        boolean kingSplitMinion = this.getPersistentData().getBoolean(KING_SPLIT_MINION_TAG);
        boolean killedLargeSlime = !this.level().isClientSide && this.isDeadOrDying() && !this.isRemoved() && this.getSize() > MIN_SIZE && reason == RemovalReason.KILLED;
        if (killedLargeSlime) {
            if (!shieldMinion && !kingSplitMinion) {
                this.spawnChildren();
            }
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
            // Golden aura — explicit gold-RGB dust + nectar droplets + golden slimeball sparkles.
            net.minecraft.core.particles.DustParticleOptions goldDust =
                    new net.minecraft.core.particles.DustParticleOptions(
                            new org.joml.Vector3f(1.0F, 0.84F, 0.05F), 1.6F);
            serverLevel.sendParticles(goldDust,
                    this.getX(), this.getY(0.6D), this.getZ(), 18, 0.55D, 0.4D, 0.55D, 0.05D);
            serverLevel.sendParticles(ParticleTypes.FALLING_NECTAR,
                    this.getX(), this.getY(0.6D), this.getZ(), 8, 0.5D, 0.4D, 0.5D, 0.02D);
            serverLevel.sendParticles(
                    new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(com.Momik.usless_mobs.registry.ModItems.GOLDENER_SCHLEIMBALL.get())),
                    this.getX(), this.getY(0.5D), this.getZ(), 6, 0.45D, 0.35D, 0.45D, 0.05D);
        }
    }

    private void tickGoldenSplitRole() {
        if (this.tickCount % 35 != 0) {
            return;
        }
        int role = this.getPersistentData().getInt(GOLDEN_SPLIT_ROLE_TAG);
        switch (role) {
            case 0 -> this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1));
            case 1 -> {
                this.heal(1.0F);
                for (BlueSlimeEntity slime : this.level().getEntitiesOfClass(BlueSlimeEntity.class, this.getBoundingBox().inflate(4.0D),
                        other -> other != this && other.isAlive())) {
                    slime.heal(1.0F);
                }
            }
            case 2 -> this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0));
            default -> {
                LivingEntity target = this.getTarget();
                if (target != null && target.isAlive() && this.distanceToSqr(target) < 3.2D * 3.2D) {
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 70, 0));
                    target.knockback(0.55D, this.getX() - target.getX(), this.getZ() - target.getZ());
                }
            }
        }
    }

    private void spawnChildren() {
        int childSize = this.isGolden() ? MIN_SIZE : Math.max(MIN_SIZE, this.getSize() / 2);
        int childCount = this.isGolden() ? Math.min(4, Math.max(2, this.getSize())) : 1 + this.random.nextInt(2);
        Component customName = this.getCustomName();
        boolean noAi = this.isNoAi();
        boolean invulnerable = this.isInvulnerable();
        boolean persistent = this.isPersistenceRequired();

        for (int index = 0; index < childCount; index++) {
            float xOffset = ((index % 2) - 0.5F) * this.getSize() / 4.0F;
            float zOffset = ((index / 2) - 0.5F) * this.getSize() / 4.0F;
            BlueSlimeEntity child = com.Momik.usless_mobs.registry.ModEntities.BLAUER_SCHLEIM.get().create(this.level());

            if (child == null) {
                continue;
            }

            child.setGolden(this.isGolden());
            child.setSize(childSize, true);
            if (this.isGolden()) {
                child.getPersistentData().putBoolean(KING_SPLIT_MINION_TAG, true);
                child.getPersistentData().putInt(GOLDEN_SPLIT_ROLE_TAG, index % 4);
            }
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
