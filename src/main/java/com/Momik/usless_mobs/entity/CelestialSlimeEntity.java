package com.Momik.usless_mobs.entity;

import com.Momik.usless_mobs.allegiance.AllegiancePath;
import com.Momik.usless_mobs.allegiance.AllegianceUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.mysith.registry.ModItems;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class CelestialSlimeEntity extends EnderSlimeEntity implements GeoEntity {
    private static final double BASE_HEALTH = 24.0D;
    private static final double HEALTH_PER_SIZE = 7.0D;
    private static final double BASE_ATTACK_DAMAGE = 5.0D;
    private static final double ATTACK_DAMAGE_PER_SIZE = 1.75D;
    public static final String STAR_MINION_TAG = "UslessMobs_CelestialStarMinion";
    private static final String STAR_SLIMES_SUMMONED_TAG = "StarSlimesSummoned";
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("animation.celestial_slime.idle");
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private boolean wasAirborne = false;
    private boolean starSlimesSummoned = false;
    private int lightLeapCooldown = 60;
    private int holyShineCooldown = 85;
    private int starfallCooldown = 145;
    private int starfallWarmup = 0;
    private Vec3 starfallPos = Vec3.ZERO;

    public CelestialSlimeEntity(EntityType<? extends Slime> entityType, Level level) {
        super(entityType, level);
        this.setSize(Math.max(2, this.getSize()), true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, BASE_HEALTH + HEALTH_PER_SIZE * 2.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D)
                .add(Attributes.ATTACK_DAMAGE, BASE_ATTACK_DAMAGE + ATTACK_DAMAGE_PER_SIZE * 2.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.85D);
    }

    @SuppressWarnings("unchecked")
    public static boolean checkCelestialSlimeSpawnRules(EntityType<CelestialSlimeEntity> entityType, LevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (!(level instanceof ServerLevelAccessor serverLevel)) {
            return false;
        }
        return Monster.checkMonsterSpawnRules((EntityType<? extends Monster>) (EntityType<?>) entityType, serverLevel, spawnType, pos, random);
    }

    @Override
    public void setSize(int size, boolean heal) {
        super.setSize(Math.max(2, size), heal);
        applyCelestialStats(this.getSize());
        if (heal) {
            this.setHealth(this.getMaxHealth());
        }
    }

    private void applyCelestialStats(int size) {
        if (this.getAttribute(Attributes.MAX_HEALTH) != null) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(BASE_HEALTH + HEALTH_PER_SIZE * size);
        }
        if (this.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(BASE_ATTACK_DAMAGE + ATTACK_DAMAGE_PER_SIZE * size);
        }
        if (this.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.32D + size * 0.01D);
        }
        this.xpReward = 12 + size * 5;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(STAR_SLIMES_SUMMONED_TAG, this.starSlimesSummoned);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.starSlimesSummoned = tag.getBoolean(STAR_SLIMES_SUMMONED_TAG);
    }

    @Override
    protected ParticleOptions getParticleType() {
        return ParticleTypes.END_ROD;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level().isClientSide) {
            tickCelestialCooldowns();
            tickStarfallWarmup();
            tickLightLeap();
            tickLandingWave();
            tickStarSlimeSummon();
            tickHolyShine();
        }
        if (!this.level().isClientSide && this.tickCount % 60 == 0 && this.getHealth() < this.getMaxHealth()) {
            this.heal(1.0F);
        }
        if (!this.level().isClientSide && this.getTarget() instanceof Player player && AllegianceUtil.hasPath(player, AllegiancePath.CELESTIAL)) {
            this.setTarget(null);
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 80, 0, true, false, true));
        }
        if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 20 == 0) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    this.getX(), this.getY(0.85D), this.getZ(),
                    10, 0.55D, 0.55D, 0.55D, 0.025D);
            if (this.tickCount % 60 == 0) {
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        this.getX(), this.getY(1.0D), this.getZ(),
                        4, 0.35D, 0.35D, 0.35D, 0.01D);
            }
        }

        if (this.level().isClientSide && this.tickCount % 3 == 0) {
            this.level().addParticle(ParticleTypes.END_ROD,
                    this.getX() + (this.random.nextDouble() - 0.5D) * this.getBbWidth(),
                    this.getY() + this.random.nextDouble() * this.getBbHeight(),
                    this.getZ() + (this.random.nextDouble() - 0.5D) * this.getBbWidth(),
                    0.0D,
                    0.02D + this.random.nextDouble() * 0.03D,
                    0.0D);
        }
    }

    private void tickCelestialCooldowns() {
        if (this.holyShineCooldown > 0) {
            this.holyShineCooldown--;
        }
        if (this.starfallCooldown > 0) {
            this.starfallCooldown--;
        }
    }

    private void tickLightLeap() {
        if (this.lightLeapCooldown > 0) {
            this.lightLeapCooldown--;
        }
        LivingEntity target = this.getTarget();
        if (this.lightLeapCooldown > 0 || target == null || !target.isAlive() || !this.onGround()
                || this.distanceToSqr(target) < 4.0D || this.distanceToSqr(target) > 14.0D * 14.0D) {
            return;
        }

        Vec3 leap = target.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D).normalize();
        this.setDeltaMovement(this.getDeltaMovement().add(leap.x * 0.58D, 0.45D + this.getSize() * 0.04D, leap.z * 0.58D));
        this.hurtMarked = true;
        this.lightLeapCooldown = 70;
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    this.getX(), this.getY(0.45D), this.getZ(),
                    22, 0.45D, 0.18D, 0.45D, 0.04D);
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 0.9F, 1.55F);
        }
    }

    private void tickLandingWave() {
        boolean onGroundNow = this.onGround();
        if (this.wasAirborne && onGroundNow && this.level() instanceof ServerLevel serverLevel) {
            double radius = 3.6D + this.getSize() * 0.55D;
            AABB area = this.getBoundingBox().inflate(radius, 0.8D, radius);
            for (LivingEntity living : this.level().getEntitiesOfClass(LivingEntity.class, area,
                    living -> living != this && living.isAlive() && !(living instanceof CelestialSlimeEntity))) {
                if (living instanceof Player player && AllegianceUtil.hasPath(player, AllegiancePath.CELESTIAL)) {
                    player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 80, 0, true, false, true));
                    continue;
                }
                living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 90, 0));
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
                living.hurt(this.damageSources().mobAttack(this), 3.0F + this.getSize());
            }
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    this.getX(), this.getY(0.12D), this.getZ(),
                    54, radius * 0.28D, 0.05D, radius * 0.28D, 0.025D);
            serverLevel.sendParticles(ParticleTypes.FLASH,
                    this.getX(), this.getY(0.35D), this.getZ(),
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.45F, 1.8F);
        }
        this.wasAirborne = !onGroundNow;
    }

    private void tickHolyShine() {
        LivingEntity target = this.getTarget();
        if (this.holyShineCooldown > 0 || target == null || !target.isAlive() || this.distanceToSqr(target) > 10.0D * 10.0D) {
            return;
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            AABB area = this.getBoundingBox().inflate(5.5D, 1.4D, 5.5D);
            for (LivingEntity living : this.level().getEntitiesOfClass(LivingEntity.class, area,
                    living -> living != this && living.isAlive() && !(living instanceof CelestialSlimeEntity))) {
                if (living instanceof Player player && AllegianceUtil.hasPath(player, AllegiancePath.CELESTIAL)) {
                    player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 0, true, false, true));
                    continue;
                }
                living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0));
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 90, 0));
                living.hurt(this.damageSources().magic(), this.level().getDifficulty() == net.minecraft.world.Difficulty.HARD ? 5.0F : 3.0F);
            }
            this.heal(2.0F);
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    this.getX(), this.getY(0.95D), this.getZ(),
                    48, 0.85D, 0.65D, 0.85D, 0.04D);
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 0.75F, 1.45F);
        }
        this.holyShineCooldown = this.level().getDifficulty() == net.minecraft.world.Difficulty.HARD ? 95 : 125;
    }

    private void tickStarfallWarmup() {
        LivingEntity target = this.getTarget();
        if (this.starfallWarmup <= 0 && this.starfallCooldown <= 0 && target != null && target.isAlive()
                && this.distanceToSqr(target) <= 18.0D * 18.0D) {
            this.starfallWarmup = 28;
            this.starfallPos = target.position();
            this.starfallCooldown = this.level().getDifficulty() == net.minecraft.world.Difficulty.HARD ? 145 : 190;
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.0F, 0.9F);
            }
        }
        if (this.starfallWarmup <= 0 || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        this.starfallWarmup--;
        serverLevel.sendParticles(ParticleTypes.END_ROD,
                this.starfallPos.x, this.starfallPos.y + 2.4D, this.starfallPos.z,
                8, 0.45D, 0.12D, 0.45D, -0.02D);
        serverLevel.sendParticles(ParticleTypes.GLOW,
                this.starfallPos.x, this.starfallPos.y + 0.1D, this.starfallPos.z,
                6, 1.3D, 0.02D, 1.3D, 0.0D);
        if (this.starfallWarmup <= 0) {
            releaseStarfall(serverLevel);
        }
    }

    private void releaseStarfall(ServerLevel serverLevel) {
        AABB area = new AABB(this.starfallPos, this.starfallPos).inflate(3.0D, 1.2D, 3.0D);
        for (LivingEntity living : this.level().getEntitiesOfClass(LivingEntity.class, area,
                living -> living != this && living.isAlive() && !(living instanceof CelestialSlimeEntity))) {
            if (living instanceof Player player && AllegianceUtil.hasPath(player, AllegiancePath.CELESTIAL)) {
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 0, true, false, true));
                continue;
            }
            living.hurt(this.damageSources().magic(), this.level().getDifficulty() == net.minecraft.world.Difficulty.HARD ? 9.0F : 6.0F);
            living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0));
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90, 1));
        }
        serverLevel.sendParticles(ParticleTypes.FIREWORK,
                this.starfallPos.x, this.starfallPos.y + 0.8D, this.starfallPos.z,
                38, 1.0D, 0.55D, 1.0D, 0.08D);
        serverLevel.sendParticles(ParticleTypes.FLASH,
                this.starfallPos.x, this.starfallPos.y + 0.2D, this.starfallPos.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        serverLevel.playSound(null, BlockPos.containing(this.starfallPos), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.65F, 1.65F);
    }

    private void tickStarSlimeSummon() {
        if (this.starSlimesSummoned || this.getHealth() > this.getMaxHealth() * 0.5F || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        this.starSlimesSummoned = true;
        int count = this.level().getDifficulty() == net.minecraft.world.Difficulty.HARD ? 3 : 2;
        for (int index = 0; index < count; index++) {
            BlueSlimeEntity star = com.Momik.usless_mobs.registry.ModEntities.BLAUER_SCHLEIM.get().create(this.level());
            if (star == null) {
                continue;
            }
            double angle = (Math.PI * 2.0D / count) * index;
            star.setSize(1, true);
            star.setShootsSpikes(true);
            star.moveTo(this.getX() + Math.cos(angle) * 1.8D, this.getY() + 0.3D, this.getZ() + Math.sin(angle) * 1.8D,
                    this.random.nextFloat() * 360.0F, 0.0F);
            star.setCustomName(Component.translatable("entity.usless_mobs.star_slime"));
            star.getPersistentData().putBoolean(STAR_MINION_TAG, true);
            star.getPersistentData().putBoolean(BlueSlimeEntity.KING_SPLIT_MINION_TAG, true);
            star.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 20, 0));
            star.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 20, 0));
            if (this.getTarget() != null) {
                star.setTarget(this.getTarget());
            }
            serverLevel.addFreshEntity(star);
        }
        serverLevel.sendParticles(ParticleTypes.FIREWORK,
                this.getX(), this.getY(1.0D), this.getZ(),
                24, 0.65D, 0.55D, 0.65D, 0.04D);
    }

    @Override
    protected void dealDamage(LivingEntity target) {
        if (target instanceof Player player && AllegianceUtil.hasPath(player, AllegiancePath.CELESTIAL)) {
            return;
        }
        super.dealDamage(target);
        if (this.distanceToSqr(target) <= this.getSize() * this.getSize()) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
        }
    }

    @Override
    protected boolean shouldSplitOnDeath() {
        return false;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && this.isDeadOrDying() && !this.isRemoved() && reason == RemovalReason.KILLED) {
            // Vanilla Slime.remove() still splits if size > 1, so shrink internally before it runs.
            super.setSize(1, false);
        }
        super.remove(reason);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource damageSource, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(damageSource, looting, recentlyHit);
        int safeLooting = Math.min(5, Math.max(0, looting));
        this.spawnAtLocation(new ItemStack(ModItems.CELESTIAL_CRYSTAL.get()));
        this.spawnAtLocation(new ItemStack(com.Momik.usless_mobs.registry.ModItems.CELESTIAL_VITALITY_TEMPLATE.get()));
        this.spawnAtLocation(new ItemStack(com.Momik.usless_mobs.registry.ModItems.VOID_SCHLEIMBALL.get(), 1 + this.random.nextInt(2 + safeLooting)));
        this.spawnAtLocation(new ItemStack(ModItems.SOUL_FRAGMENT.get(), 1 + this.random.nextInt(2 + safeLooting)));
        if (this.random.nextFloat() < Math.min(0.20F, 0.08F + (0.03F * safeLooting))) {
            this.spawnAtLocation(new ItemStack(com.Momik.usless_mobs.registry.ModItems.CELESTIAL_TALISMAN.get()));
        }
        if (this.random.nextFloat() < Math.min(0.12F, 0.04F + (0.01F * safeLooting))) {
            this.spawnAtLocation(new ItemStack(com.Momik.usless_mobs.registry.ModItems.CELESTIAL_AETHER_BLOCK_ITEM.get(),
                    1 + this.random.nextInt(2)));
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            state.setAnimation(IDLE_ANIM);
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
