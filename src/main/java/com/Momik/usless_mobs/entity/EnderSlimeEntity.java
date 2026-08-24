package com.Momik.usless_mobs.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.mysith.item.DarkCrystalItem;
import net.mysith.registry.ModItems;

public class EnderSlimeEntity extends Slime {

    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 4;
    private static final double BASE_HEALTH = 8.0D;
    private static final double HEALTH_PER_SIZE = 3.0D;
    private static final double BASE_ATTACK_DAMAGE = 2.0D;
    private static final double ATTACK_DAMAGE_PER_SIZE = 1.0D;
    private static final double KNOCKBACK_RESISTANCE = 0.6D;
    private static final double TELEPORT_RANGE = 12.0D;
    private static final float HURT_TELEPORT_CHANCE = 0.55F;

    public EnderSlimeEntity(EntityType<? extends Slime> entityType, Level level) {
        super(entityType, level);
        this.applyScaledStats(this.getSize());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, BASE_HEALTH + HEALTH_PER_SIZE)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, BASE_ATTACK_DAMAGE + ATTACK_DAMAGE_PER_SIZE)
                .add(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE);
    }

    @SuppressWarnings("unchecked")
    public static boolean checkEnderSlimeSpawnRules(EntityType<EnderSlimeEntity> entityType, LevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (!(level instanceof ServerLevelAccessor serverLevel)) {
            return false;
        }
        return Monster.checkMonsterSpawnRules((EntityType<? extends Monster>) (EntityType<?>) entityType, serverLevel, spawnType, pos, random);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, SpawnGroupData spawnGroupData, CompoundTag dataTag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData, dataTag);
        this.setSize(MIN_SIZE + level.getRandom().nextInt(MAX_SIZE), true);
        return data;
    }

    @Override
    public void setSize(int size, boolean heal) {
        int clamped = Mth.clamp(size, MIN_SIZE, MAX_SIZE);
        super.setSize(clamped, heal);
        this.applyScaledStats(clamped);
        if (heal) {
            this.setHealth(this.getMaxHealth());
        }
    }

    private void applyScaledStats(int size) {
        double hp = BASE_HEALTH + (HEALTH_PER_SIZE * size);
        double atk = BASE_ATTACK_DAMAGE + (ATTACK_DAMAGE_PER_SIZE * size);
        if (this.getAttribute(Attributes.MAX_HEALTH) != null) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
        }
        if (this.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(atk);
        }
        this.xpReward = 4 + size * 2;
    }

    @Override
    protected ParticleOptions getParticleType() {
        return ParticleTypes.PORTAL;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(ModItems.DARK_CRYSTAL.get())) {
            return DarkCrystalItem.awakenCelestialSlime(stack, player, hand, this);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && this.isAlive() && this.random.nextFloat() < HURT_TELEPORT_CHANCE) {
            if (this.tryRandomTeleport()) {
                this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
                return false;
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level().isClientSide && this.tickCount % 80 == 0 && this.getTarget() == null && this.random.nextFloat() < 0.15F) {
            this.tryRandomTeleport();
        }

        if (this.level().isClientSide && this.tickCount % 4 == 0) {
            this.level().addParticle(ParticleTypes.PORTAL,
                    this.getX() + (this.random.nextDouble() - 0.5D) * this.getBbWidth(),
                    this.getY() + this.random.nextDouble() * this.getBbHeight(),
                    this.getZ() + (this.random.nextDouble() - 0.5D) * this.getBbWidth(),
                    (this.random.nextDouble() - 0.5D) * 0.2D,
                    -this.random.nextDouble(),
                    (this.random.nextDouble() - 0.5D) * 0.2D);
        }
    }

    private boolean tryRandomTeleport() {
        double tx = this.getX() + (this.random.nextDouble() - 0.5D) * TELEPORT_RANGE * 2.0D;
        double ty = this.getY() + (this.random.nextInt(16) - 8);
        double tz = this.getZ() + (this.random.nextDouble() - 0.5D) * TELEPORT_RANGE * 2.0D;
        if (this.randomTeleport(tx, ty, tz, true)) {
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 0.5D, this.getZ(),
                        24, 0.4D, 0.6D, 0.4D, 0.4D);
            }
            return true;
        }
        return false;
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
        if (!target.hurt(this.damageSources().mobAttack(this), (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE))) {
            return;
        }
        this.playSound(SoundEvents.SLIME_ATTACK, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100 + size * 20, 0));
        target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20 + size * 5, 0));
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource damageSource, int looting, boolean recentlyHit) {
        if (this.getSize() == MIN_SIZE) {
            int safeLooting = Math.min(5, Math.max(0, looting));
            this.spawnAtLocation(new ItemStack(com.Momik.usless_mobs.registry.ModItems.VOID_SCHLEIMBALL.get(),
                    1 + this.random.nextInt(2) + this.random.nextInt(safeLooting + 1)));

            if (this.random.nextFloat() < Math.min(0.55F, 0.20F + (0.05F * safeLooting))) {
                this.spawnAtLocation(new ItemStack(Items.ENDER_PEARL));
            }
            if (this.random.nextFloat() < Math.min(0.18F, 0.04F + (0.02F * safeLooting))) {
                this.spawnAtLocation(new ItemStack(Items.ENDER_EYE));
            }
            if (this.random.nextFloat() < Math.min(0.20F, 0.08F + (0.03F * safeLooting))) {
                this.spawnAtLocation(new ItemStack(com.Momik.usless_mobs.registry.ModItems.VOID_TALISMAN.get()));
            }
            if (this.random.nextFloat() < Math.min(0.12F, 0.04F + (0.01F * safeLooting))) {
                this.spawnAtLocation(new ItemStack(com.Momik.usless_mobs.registry.ModItems.VOID_FRAGMENT_BLOCK_ITEM.get(),
                        1 + this.random.nextInt(2)));
            }
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (this.shouldSplitOnDeath() && !this.level().isClientSide && this.isDeadOrDying() && !this.isRemoved() && this.getSize() > MIN_SIZE && reason == RemovalReason.KILLED) {
            this.spawnChildren();
            super.setSize(MIN_SIZE, false);
        }
        super.remove(reason);
    }

    protected boolean shouldSplitOnDeath() {
        return true;
    }

    private void spawnChildren() {
        int childSize = Math.max(MIN_SIZE, this.getSize() / 2);
        int childCount = 2 + this.random.nextInt(2);
        for (int i = 0; i < childCount; i++) {
            float xOff = ((i % 2) - 0.5F) * this.getSize() / 4.0F;
            float zOff = ((i / 2) - 0.5F) * this.getSize() / 4.0F;
            EnderSlimeEntity child = com.Momik.usless_mobs.registry.ModEntities.ENDER_SCHLEIM.get().create(this.level());
            if (child == null) continue;
            child.setSize(childSize, true);
            child.moveTo(this.getX() + xOff, this.getY() + 0.5D, this.getZ() + zOff, this.random.nextFloat() * 360.0F, 0.0F);
            this.level().addFreshEntity(child);
        }
    }
}
