package com.Momik.usless_mobs.entity;

import com.Momik.usless_mobs.Config;
import com.Momik.usless_mobs.event.KingSlimeAdvancements;
import com.Momik.usless_mobs.Usless_mobs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class KingSlimeEntity extends Slime implements GeoEntity {

    private static final ResourceKey<DamageType> SLIMED_DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.tryBuild(Usless_mobs.MODID, "slimed"));
    private static final int KING_SIZE = 8;
    private static final double BASE_HEALTH = 320.0D;
    private static final double BASE_ATTACK_DAMAGE = 9.0D;
    private static final double BASE_MOVEMENT_SPEED = 0.30D;
    private static final double KNOCKBACK_RESISTANCE = 0.85D;
    private static final double FOLLOW_RANGE = 64.0D;
    private static final int MINION_SUMMON_INTERVAL = 220;
    private static final int MAX_NEARBY_MINIONS = 6;
    private static final int BASE_XP_REWARD = 120;
    private static final int MIN_KING_SIZE = 5;
    private static final double TELEPORT_DISTANCE_SQR = 144.0D;
    private static final double TELEPORT_AHEAD_DISTANCE = 5.0D;
    private static final int TELEPORT_SEARCH_RADIUS = 2;
    private static final int DEATH_MINION_COUNT = 2;
    private static final double DEATH_MINION_SIDE_OFFSET = 1.6D;

    private static final int SLAM_COOLDOWN = 200;
    private static final int SLAM_TELEGRAPH_TICKS = 25;
    private static final double SLAM_RADIUS = 4.0D;
    private static final float SLAM_DAMAGE = 7.0F;
    private static final int SHOCKWAVE_COOLDOWN = 240;
    private static final double SHOCKWAVE_RADIUS = 5.0D;
    private static final float ENRAGE_HP_THRESHOLD = 0.5F;
    private static final float PHASE2_HP_THRESHOLD = 0.25F;
    private static final int PHASE2_DURATION_TICKS = 200;
    private static final int PHASE2_GOLDEN_COUNT = 3;
    private static final int PUDDLE_DURATION_TICKS = 160;
    private static final float CROWN_RAGE_HP_THRESHOLD = 0.10F;
    private static final int CROWN_RAGE_DURATION_TICKS = 320;
    private static final int SPIKE_VOLLEY_COOLDOWN = 150;
    private static final int ROYAL_SNARE_COOLDOWN = 220;
    private static final int ABSORB_COOLDOWN = 320;  // ~16s between absorbs (was 6s, way too OP)
    private static final float ABSORB_MAX_HEAL_FRACTION = 0.55F;  // King can never heal above 55% via absorption
    private static final double ABSORB_RADIUS = 11.0D;
    private static final float LOW_HP_PROJECTILE_GUARD_THRESHOLD = 0.18F;
    private static final int LOW_HP_PROJECTILE_GUARD_TICKS = 18;
    private static final int ROYAL_GUARD_BOSSBAR_TICKS = 30;
    private static final int ANTI_CAMP_WARNING_TICKS = 80;
    private static final int ANTI_CAMP_PRESSURE_TICKS = 140;
    private static final int ANTI_CAMP_COOLDOWN_TICKS = 120;
    private static final float FINAL_SHIELD_HP_THRESHOLD = 0.10F;
    private static final int FINAL_SHIELD_COOLDOWN_TICKS = 600;
    private static final int FINAL_SHIELD_MINION_COUNT = 3;
    private static final int FINAL_SHIELD_MAX_TRIGGERS = 2;
    private static final int SPIKE_VOLLEY_TELEGRAPH_TICKS = 24;
    private static final int ROYAL_SNARE_TELEGRAPH_TICKS = 32;
    private static final int ABSORB_TELEGRAPH_TICKS = 30;

    private int slamCooldown = SLAM_COOLDOWN;
    private int slamPhase = 0;
    private int slamWarmupTicks = 0;
    private int slamLeapStartTick = 0;
    private int shockwaveCooldown = SHOCKWAVE_COOLDOWN;
    private int teleportCooldown = 200;
    private int spikeVolleyCooldown = 90;
    private int royalSnareCooldown = 140;
    private int absorbCooldown = ABSORB_COOLDOWN;
    private int lowHpProjectileGuardTicks = 0;
    private int royalGuardBossbarTicks = 0;
    private int antiCampTicks = 0;
    private int antiCampCooldown = 0;
    private int finalShieldCooldown = FINAL_SHIELD_COOLDOWN_TICKS;
    private int finalShieldTriggerCount = 0;
    private int spikeVolleyTelegraphTicks = 0;
    private int royalSnareTelegraphTicks = 0;
    private int absorbTelegraphTicks = 0;
    private java.util.UUID absorbTargetId = null;
    private boolean enraged = false;
    private boolean wasAirborne = false;
    private boolean phase2Triggered = false;
    private boolean phase2Active = false;
    private int phase2RemainingTicks = 0;
    private float phase2EnterHp = 0F;
    private java.util.UUID[] phase2GoldenIds = new java.util.UUID[0];
    private boolean crownRageTriggered = false;
    private boolean crownRageActive = false;
    private int crownRageRemainingTicks = 0;
    private boolean finalShieldActive = false;
    private boolean finalShieldTriggeredOnce = false;
    private java.util.UUID[] finalShieldGoldenIds = new java.util.UUID[0];

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("animation.king_slime.idle");
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            Component.translatable("entity.usless_mobs.king_schleim"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.NOTCHED_10
    ).setDarkenScreen(true).setPlayBossMusic(true);

    public KingSlimeEntity(EntityType<? extends Slime> entityType, Level level) {
        super(entityType, level);
        this.xpReward = BASE_XP_REWARD;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, BASE_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, BASE_ATTACK_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, BASE_MOVEMENT_SPEED)
                .add(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE);
    }

    private Difficulty currentDifficulty() {
        Difficulty difficulty = this.level().getDifficulty();
        return difficulty == Difficulty.PEACEFUL ? Difficulty.EASY : difficulty;
    }

    private double healthMultiplier() {
        switch (this.currentDifficulty()) {
            case EASY:
                return Config.easyHealthMultiplier;
            case HARD:
                return Config.hardHealthMultiplier;
            default:
                return Config.normalHealthMultiplier;
        }
    }

    private double attackMultiplier() {
        switch (this.currentDifficulty()) {
            case EASY:
                return Config.easyAttackMultiplier;
            case HARD:
                return Config.hardAttackMultiplier;
            default:
                return Config.normalAttackMultiplier;
        }
    }

    private double speedMultiplier() {
        switch (this.currentDifficulty()) {
            case EASY:
                return Config.easySpeedMultiplier;
            case HARD:
                return Config.hardSpeedMultiplier;
            default:
                return Config.normalSpeedMultiplier;
        }
    }

    private int scaleCooldown(int baseTicks) {
        switch (this.currentDifficulty()) {
            case EASY:
                return Math.max(20, Math.round(baseTicks * 1.25F));
            case HARD:
                return Math.max(20, Math.round(baseTicks * 0.62F));
            default:
                return baseTicks;
        }
    }

    private int maxNearbyMinions() {
        switch (this.currentDifficulty()) {
            case EASY:
                return 4;
            case HARD:
                return 10;
            default:
                return MAX_NEARBY_MINIONS;
        }
    }

    private int phase2GoldenCount() {
        switch (this.currentDifficulty()) {
            case EASY:
                return 2;
            case HARD:
                return 4;
            default:
                return PHASE2_GOLDEN_COUNT;
        }
    }

    private int phase2DurationTicks() {
        switch (this.currentDifficulty()) {
            case EASY:
                return 160;
            case HARD:
                return 240;
            default:
                return PHASE2_DURATION_TICKS;
        }
    }

    private float phase2DamagePerGolden() {
        switch (this.currentDifficulty()) {
            case EASY:
                return 0.38F;
            case HARD:
                return 0.24F;
            default:
                return 0.33F;
        }
    }

    private int slamTelegraphTicks() {
        switch (this.currentDifficulty()) {
            case EASY:
                return SLAM_TELEGRAPH_TICKS + 10;
            case HARD:
                return Math.max(12, SLAM_TELEGRAPH_TICKS - 7);
            default:
                return SLAM_TELEGRAPH_TICKS;
        }
    }

    private double slamRadius() {
        switch (this.currentDifficulty()) {
            case EASY:
                return SLAM_RADIUS - 0.5D;
            case HARD:
                return SLAM_RADIUS + 1.25D;
            default:
                return SLAM_RADIUS;
        }
    }

    private float slamDamage() {
        float damage = (float) (SLAM_DAMAGE * this.attackMultiplier());
        return this.enraged ? damage * 1.35F : damage;
    }

    private double shockwaveRadius() {
        switch (this.currentDifficulty()) {
            case EASY:
                return SHOCKWAVE_RADIUS - 0.75D;
            case HARD:
                return SHOCKWAVE_RADIUS + 1.5D;
            default:
                return SHOCKWAVE_RADIUS;
        }
    }

    private float maxDamagePerHit() {
        switch (this.currentDifficulty()) {
            case EASY:
                return 18.0F;
            case HARD:
                return 8.0F;
            default:
                return 12.0F;
        }
    }

    @Override
    protected ParticleOptions getParticleType() {
        // Override vanilla Slime's green ITEM_SLIME particle with our blue slimeball item particle.
        // Affects squish, hop, and split particles.
        return new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(com.Momik.usless_mobs.registry.ModItems.BLAUER_SCHLEIMBALL.get()));
    }

    public static boolean checkKingSlimeSpawnRules(EntityType<KingSlimeEntity> entityType, LevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return spawnType != MobSpawnType.NATURAL && spawnType != MobSpawnType.CHUNK_GENERATION;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, SpawnGroupData spawnGroupData, CompoundTag dataTag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData, dataTag);
        this.setSize(KING_SIZE, true);
        this.setPersistenceRequired();
        return data;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public void checkDespawn() {
        this.noActionTime = 0;
    }

    @Override
    public void setSize(int size, boolean heal) {
        int clamped = Mth.clamp(size, MIN_KING_SIZE, KING_SIZE);
        float prevHealthFraction = this.getMaxHealth() > 0 ? this.getHealth() / this.getMaxHealth() : 1.0F;

        super.setSize(clamped, false);

        double targetHp = BASE_HEALTH * this.healthMultiplier();
        double targetAtk = BASE_ATTACK_DAMAGE * this.attackMultiplier();
        double targetSpd = BASE_MOVEMENT_SPEED * this.speedMultiplier();

        if (this.enraged) {
            targetAtk *= 1.5D;
            targetSpd *= 1.25D;
        }
        if (this.crownRageActive) {
            targetAtk *= 1.35D;
            targetSpd *= 1.30D;
        }

        if (this.getAttribute(Attributes.MAX_HEALTH) != null) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(targetHp);
        }
        if (this.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(targetAtk);
        }
        if (this.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(targetSpd);
        }
        if (this.getAttribute(Attributes.KNOCKBACK_RESISTANCE) != null) {
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(KNOCKBACK_RESISTANCE);
        }
        if (this.getAttribute(Attributes.FOLLOW_RANGE) != null) {
            this.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(FOLLOW_RANGE);
        }

        if (heal) {
            this.setHealth(this.getMaxHealth());
        } else {
            this.setHealth(prevHealthFraction * (float) targetHp);
        }
        this.xpReward = BASE_XP_REWARD;
    }

    private int computeSizeFromHp(float hpRatio) {
        if (hpRatio > 0.75F) return KING_SIZE;
        if (hpRatio > 0.50F) return 7;
        if (hpRatio > 0.25F) return 6;
        return MIN_KING_SIZE;
    }

    private void tickShrink(float hpRatio) {
        int desired = computeSizeFromHp(hpRatio);
        if (this.getSize() != desired) {
            this.setSize(desired, false);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(this.getParticleType(), this.getX(), this.getY(0.5D), this.getZ(), 30, 1.0D, 0.6D, 1.0D, 0.05D);
            }
            this.playSound(SoundEvents.SLIME_SQUISH_SMALL, 1.5F, 0.8F);
        }
    }

    private int computeTeleportCooldown(float hpRatio) {
        if (hpRatio > 0.75F) return this.scaleCooldown(200);
        if (hpRatio > 0.50F) return this.scaleCooldown(140);
        if (hpRatio > 0.25F) return this.scaleCooldown(100);
        return this.scaleCooldown(60);
    }

    private void tickTeleport(float hpRatio) {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            this.teleportCooldown = Math.max(this.teleportCooldown, 40);
            this.teleportCooldown--;
            return;
        }

        this.teleportCooldown--;
        if (this.teleportCooldown > 0) {
            return;
        }

        double distSqr = this.distanceToSqr(target);
        if (distSqr < TELEPORT_DISTANCE_SQR) {
            this.teleportCooldown = 40;
            return;
        }

        if (this.tryTeleportInFrontOf(target)) {
            this.teleportCooldown = computeTeleportCooldown(hpRatio);
        } else {
            this.teleportCooldown = 40;
        }
    }

    private boolean tryTeleportInFrontOf(LivingEntity target) {
        Vec3 forward = this.resolveTargetForward(target);
        Vec3 desiredPos = target.position().add(forward.scale(TELEPORT_AHEAD_DISTANCE));
        BlockPos candidate = this.findOpenGroundPosition(desiredPos);
        if (candidate == null) {
            return false;
        }
        if (!this.hasClearTeleportPath(target, candidate)) {
            return false;
        }

        Vec3 oldPos = this.position();
        this.teleportTo(candidate.getX() + 0.5D, candidate.getY(), candidate.getZ() + 0.5D);
        if (this.isInWall() || !this.level().noCollision(this, this.getBoundingBox())) {
            this.teleportTo(oldPos.x, oldPos.y, oldPos.z);
            return false;
        }
        this.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, target.getEyePosition());

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.PORTAL, oldPos.x, oldPos.y + 1.0D, oldPos.z, 40, 0.6D, 1.0D, 0.6D, 0.5D);
            serverLevel.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 1.0D, this.getZ(), 40, 0.6D, 1.0D, 0.6D, 0.5D);
            serverLevel.sendParticles(this.getParticleType(), this.getX(), this.getY() + 0.5D, this.getZ(), 20, 0.8D, 0.6D, 0.8D, 0.05D);
        }
        this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.6F, 0.5F);
        return true;
    }

    private Vec3 resolveTargetForward(LivingEntity target) {
        Vec3 forward = target.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        if (forward.lengthSqr() >= 0.01D) {
            return forward.normalize();
        }

        Vec3 fallback = new Vec3(target.getX() - this.getX(), 0.0D, target.getZ() - this.getZ());
        if (fallback.lengthSqr() >= 0.01D) {
            return fallback.normalize();
        }
        return new Vec3(0.0D, 0.0D, 1.0D);
    }

    private BlockPos findOpenGroundPosition(Vec3 desiredPos) {
        BlockPos basePos = BlockPos.containing(desiredPos.x, desiredPos.y, desiredPos.z);
        BlockPos exact = this.findOpenGroundAt(basePos);
        if (exact != null) {
            return exact;
        }

        for (int radius = 1; radius <= TELEPORT_SEARCH_RADIUS; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.abs(x) != radius && Math.abs(z) != radius) {
                        continue;
                    }

                    BlockPos candidate = this.findOpenGroundAt(basePos.offset(x, 0, z));
                    if (candidate != null) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private BlockPos findOpenGroundAt(BlockPos basePos) {
        for (int dy = 1; dy >= -3; dy--) {
            BlockPos candidate = basePos.offset(0, dy, 0);
            if (this.canTeleportTo(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean canTeleportTo(BlockPos candidate) {
        if (!hasSolidFooting(candidate.below())) {
            return false;
        }
        if (!this.level().getBlockState(candidate).getCollisionShape(this.level(), candidate).isEmpty()) {
            return false;
        }
        if (!this.level().getBlockState(candidate.above()).getCollisionShape(this.level(), candidate.above()).isEmpty()) {
            return false;
        }

        double x = candidate.getX() + 0.5D;
        double y = candidate.getY();
        double z = candidate.getZ() + 0.5D;
        AABB movedBox = this.getBoundingBox().move(x - this.getX(), y - this.getY(), z - this.getZ());
        return this.hasBossBodyClearance(candidate) && this.level().noCollision(this, movedBox);
    }

    private boolean hasBossBodyClearance(BlockPos candidate) {
        int horizontalRadius = Math.max(1, Mth.ceil(this.getBbWidth() / 2.0F));
        int verticalClearance = Math.max(3, Mth.ceil(this.getBbHeight() + 0.25F));

        for (int y = 0; y < verticalClearance; y++) {
            for (int x = -horizontalRadius; x <= horizontalRadius; x++) {
                for (int z = -horizontalRadius; z <= horizontalRadius; z++) {
                    BlockPos checked = candidate.offset(x, y, z);
                    if (!this.level().getBlockState(checked).getCollisionShape(this.level(), checked).isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean hasSolidFooting(BlockPos pos) {
        return this.level().getBlockState(pos).isFaceSturdy(this.level(), pos, Direction.UP);
    }

    private boolean hasClearTeleportPath(LivingEntity target, BlockPos candidate) {
        Vec3 start = target.getEyePosition();
        Vec3 end = new Vec3(candidate.getX() + 0.5D, candidate.getY() + 1.0D, candidate.getZ() + 0.5D);
        HitResult hit = this.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hit.getType() == HitResult.Type.MISS;
    }

    private void tickAntiCampPressure() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            this.antiCampTicks = 0;
            return;
        }

        if (this.antiCampCooldown > 0) {
            this.antiCampCooldown--;
        }

        if (!this.isTargetCamping(target)) {
            this.antiCampTicks = Math.max(0, this.antiCampTicks - 3);
            return;
        }

        this.antiCampTicks++;
        if (this.antiCampTicks >= ANTI_CAMP_WARNING_TICKS && this.tickCount % 20 == 0) {
            this.telegraphAntiCampPressure(target);
        }

        if (this.antiCampTicks >= ANTI_CAMP_PRESSURE_TICKS && this.antiCampCooldown <= 0) {
            this.triggerAntiCampPressure(target);
            this.antiCampCooldown = ANTI_CAMP_COOLDOWN_TICKS;
            this.antiCampTicks = ANTI_CAMP_WARNING_TICKS;
        }
    }

    private boolean isTargetCamping(LivingEntity target) {
        boolean noLineOfSight = !this.hasLineOfSight(target);
        boolean farOrBelow = this.distanceToSqr(target) > 64.0D || target.getY() < this.getY() - 5.0D;
        return noLineOfSight && (farOrBelow || this.isTargetSheltered(target));
    }

    private boolean isTargetSheltered(LivingEntity target) {
        BlockPos pos = target.blockPosition();
        int blockedSides = 0;
        if (this.hasBlockingCollision(pos.north()) || this.hasBlockingCollision(pos.north().above())) blockedSides++;
        if (this.hasBlockingCollision(pos.south()) || this.hasBlockingCollision(pos.south().above())) blockedSides++;
        if (this.hasBlockingCollision(pos.east()) || this.hasBlockingCollision(pos.east().above())) blockedSides++;
        if (this.hasBlockingCollision(pos.west()) || this.hasBlockingCollision(pos.west().above())) blockedSides++;

        boolean lowCeiling = this.hasBlockingCollision(pos.above(2));
        return blockedSides >= 3 || (blockedSides >= 2 && lowCeiling);
    }

    private boolean hasBlockingCollision(BlockPos pos) {
        return !this.level().getBlockState(pos).getCollisionShape(this.level(), pos).isEmpty();
    }

    private void telegraphAntiCampPressure(LivingEntity target) {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ITEM_SLIME, target.getX(), target.getY(0.65D), target.getZ(),
                    14, 0.75D, 0.35D, 0.75D, 0.04D);
            this.sendParticleRing(serverLevel, target.getX(), target.getY() + 0.08D, target.getZ(), 2.2D, this.purpleWarningParticle(), 28);
        }
        this.playSound(SoundEvents.SLIME_SQUISH, 0.8F, 0.5F);
    }

    private void triggerAntiCampPressure(LivingEntity target) {
        AreaEffectCloud pressure = new AreaEffectCloud(this.level(), target.getX(), target.getY(), target.getZ());
        pressure.setOwner(this);
        pressure.setRadius(this.currentDifficulty() == Difficulty.HARD ? 2.6F : 2.2F);
        pressure.setDuration(this.currentDifficulty() == Difficulty.EASY ? 70 : 95);
        pressure.setWaitTime(8);
        pressure.setRadiusPerTick(-0.01F);
        pressure.setParticle(ParticleTypes.ITEM_SLIME);
        pressure.setFixedColor(0x4BA3FF);
        pressure.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90, this.currentDifficulty() == Difficulty.HARD ? 2 : 1));
        pressure.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
        if (this.currentDifficulty() == Difficulty.HARD) {
            pressure.addEffect(new MobEffectInstance(MobEffects.POISON, 55, 0));
        }
        this.level().addFreshEntity(pressure);

        this.spawnAntiCampMinions(target);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SQUID_INK, target.getX(), target.getY(0.35D), target.getZ(),
                    20, 0.75D, 0.15D, 0.75D, 0.02D);
        }
        this.playSound(SoundEvents.WARDEN_SONIC_BOOM, 1.0F, 1.8F);
    }

    private void spawnAntiCampMinions(LivingEntity target) {
        int count = this.currentDifficulty() == Difficulty.HARD ? 3 : this.currentDifficulty() == Difficulty.EASY ? 1 : 2;
        for (int i = 0; i < count; i++) {
            BlueSlimeEntity minion = com.Momik.usless_mobs.registry.ModEntities.BLAUER_SCHLEIM.get().create(this.level());
            if (minion == null) {
                continue;
            }

            double angle = (Math.PI * 2.0D * i) / count + this.random.nextDouble() * 0.45D;
            double radius = 2.2D + this.random.nextDouble() * 1.8D;
            BlockPos candidate = this.findSmallOpenGroundPosition(new Vec3(
                    target.getX() + Math.cos(angle) * radius,
                    target.getY(),
                    target.getZ() + Math.sin(angle) * radius));
            if (candidate == null) {
                continue;
            }

            minion.moveTo(candidate.getX() + 0.5D, candidate.getY(), candidate.getZ() + 0.5D,
                    this.random.nextFloat() * 360.0F, 0.0F);
            minion.setSize(this.currentDifficulty() == Difficulty.HARD ? 3 : 2, true);
            minion.setShootsSpikes(this.currentDifficulty() == Difficulty.HARD);
            minion.setTarget(target);
            this.level().addFreshEntity(minion);
        }
    }

    private BlockPos findSmallOpenGroundPosition(Vec3 desiredPos) {
        BlockPos basePos = BlockPos.containing(desiredPos.x, desiredPos.y, desiredPos.z);
        for (int radius = 0; radius <= 3; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (radius > 0 && Math.abs(x) != radius && Math.abs(z) != radius) {
                        continue;
                    }
                    BlockPos candidate = this.findSmallOpenGroundAt(basePos.offset(x, 0, z));
                    if (candidate != null) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private BlockPos findSmallOpenGroundAt(BlockPos basePos) {
        for (int dy = 1; dy >= -3; dy--) {
            BlockPos candidate = basePos.offset(0, dy, 0);
            if (hasSolidFooting(candidate.below())
                    && !this.hasBlockingCollision(candidate)
                    && !this.hasBlockingCollision(candidate.above())) {
                return candidate;
            }
        }
        return null;
    }

    @Override
    public boolean isDealsDamage() {
        return this.isEffectiveAi();
    }

    @Override
    protected int getJumpDelay() {
        switch (this.currentDifficulty()) {
            case EASY:
                return this.random.nextInt(26) + 12;
            case HARD:
                return this.random.nextInt(12) + 6;
            default:
                return this.random.nextInt(18) + 8;
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level().isClientSide) {
            float hpRatio = this.getHealth() / this.getMaxHealth();
            this.bossEvent.setProgress(hpRatio);
            if (this.lowHpProjectileGuardTicks > 0) {
                this.lowHpProjectileGuardTicks--;
            }
            if (this.royalGuardBossbarTicks > 0) {
                this.royalGuardBossbarTicks--;
            }

            this.updatePhaseShift(hpRatio);
            this.maybeEnrage(hpRatio);
            this.tickPhase2(hpRatio);
            this.tickCrownRage(hpRatio);
            this.tickFinalShield(hpRatio);

            if (!this.phase2Active) {
                this.tickAntiCampPressure();
                int minionInterval = this.enraged ? this.scaleCooldown(MINION_SUMMON_INTERVAL / 2) : this.scaleCooldown(MINION_SUMMON_INTERVAL);
                if (this.tickCount % minionInterval == 0 && this.getTarget() != null) {
                    this.summonMinions();
                }

                this.tickSlamAttack();
                this.tickShockwave();
                this.tickSpikeVolley();
                this.tickRoyalSnare();
                this.tickSlimeAbsorption();
                this.tickTeleport(hpRatio);
                this.tickShrink(hpRatio);
            } else {
                this.tickPhase2Visuals();
            }

            if (this.tickCount % 60 == 0 && this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY(0.6D), this.getZ(), 24, 0.8D, 0.4D, 0.8D, 0.04D);
            }

            this.tickLandTrail();
        }
    }

    private void tickLandTrail() {
        boolean onGroundNow = this.onGround();
        if (this.wasAirborne && onGroundNow && this.level() instanceof ServerLevel serverLevel) {
            int count = 12 + this.getSize() * 2;
            double spread = 0.6D + this.getSize() * 0.1D;
            // Dark blue slime particles (using BLAUER_SCHLEIMBALL as the item particle source).
            net.minecraft.core.particles.ItemParticleOption blueSlimeParticle =
                    new net.minecraft.core.particles.ItemParticleOption(
                            ParticleTypes.ITEM,
                            new ItemStack(com.Momik.usless_mobs.registry.ModItems.BLAUER_SCHLEIMBALL.get()));
            serverLevel.sendParticles(blueSlimeParticle,
                    this.getX(), this.getY() + 0.05D, this.getZ(),
                    count, spread, 0.1D, spread, 0.1D);
            serverLevel.sendParticles(ParticleTypes.POOF,
                    this.getX(), this.getY() + 0.05D, this.getZ(),
                    6, spread * 0.5D, 0.0D, spread * 0.5D, 0.02D);
        }
        this.wasAirborne = !onGroundNow;
    }

    private void updatePhaseShift(float hpRatio) {
        BossEvent.BossBarColor desired;
        if (this.finalShieldActive || this.royalGuardBossbarTicks > 0) {
            desired = BossEvent.BossBarColor.YELLOW;
        } else if (hpRatio > 0.5F) {
            desired = BossEvent.BossBarColor.PURPLE;
        } else if (hpRatio > 0.25F) {
            desired = BossEvent.BossBarColor.PINK;
        } else {
            desired = BossEvent.BossBarColor.RED;
        }
        if (this.bossEvent.getColor() != desired) {
            this.bossEvent.setColor(desired);
        }
        this.bossEvent.setName(this.currentBossName());
    }

    private Component currentBossName() {
        if (this.finalShieldActive) {
            return Component.translatable("entity.usless_mobs.king_schleim.phase.final_shield");
        }
        if (this.royalGuardBossbarTicks > 0) {
            return Component.translatable("entity.usless_mobs.king_schleim.phase.royal_guard");
        }
        if (this.absorbTelegraphTicks > 0) {
            return Component.translatable("entity.usless_mobs.king_schleim.phase.absorb");
        }
        if (this.crownRageActive) {
            return Component.translatable("entity.usless_mobs.king_schleim.phase.crown_rage");
        }
        if (this.phase2Active) {
            return Component.translatable("entity.usless_mobs.king_schleim.phase.split");
        }
        if (this.enraged) {
            return Component.translatable("entity.usless_mobs.king_schleim.phase.enraged");
        }
        return Component.translatable("entity.usless_mobs.king_schleim");
    }

    private void maybeEnrage(float hpRatio) {
        if (this.enraged || hpRatio > ENRAGE_HP_THRESHOLD) {
            return;
        }
        this.enraged = true;
        this.setSize(this.getSize(), false);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY(0.5D), this.getZ(), 40, 1.5D, 1.0D, 1.5D, 0.08D);
            serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER, this.getX(), this.getY(1.5D), this.getZ(), 6, 0.5D, 0.5D, 0.5D, 0.0D);
        }
        this.playSound(SoundEvents.RAVAGER_ROAR, 2.0F, 0.6F);
    }

    private void tickPhase2(float hpRatio) {
        if (!this.phase2Triggered && hpRatio <= PHASE2_HP_THRESHOLD && hpRatio > 0F) {
            this.triggerPhase2();
            return;
        }
        if (!this.phase2Active) {
            return;
        }
        this.phase2RemainingTicks--;
        if (this.phase2RemainingTicks <= 0) {
            this.resolvePhase2();
        }
    }

    private void tickCrownRage(float hpRatio) {
        if (this.currentDifficulty() != Difficulty.HARD) {
            return;
        }

        if (!this.crownRageTriggered && !this.phase2Active && hpRatio <= CROWN_RAGE_HP_THRESHOLD && hpRatio > 0F) {
            this.triggerCrownRage();
            return;
        }

        if (!this.crownRageActive) {
            return;
        }

        this.crownRageRemainingTicks--;
        if (this.crownRageRemainingTicks <= 0) {
            this.crownRageActive = false;
            this.setSize(this.getSize(), false);
            return;
        }

        if (this.tickCount % 35 == 0) {
            this.spawnCrownRageMinions();
        }
        if (this.tickCount % 45 == 0) {
            this.spawnAcidPuddles(4, 5.5D);
        }
        if (this.tickCount % 30 == 0) {
            this.crownPulse();
        }
        if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 3 == 0) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT, this.getX(), this.getY(1.4D), this.getZ(),
                    18, 1.1D, 0.7D, 1.1D, 0.35D);
            serverLevel.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY(0.8D), this.getZ(),
                    8, 0.8D, 0.4D, 0.8D, 0.03D);
        }
    }

    private void triggerCrownRage() {
        this.crownRageTriggered = true;
        this.crownRageActive = true;
        this.crownRageRemainingTicks = CROWN_RAGE_DURATION_TICKS;
        this.setHealth(Math.max(this.getHealth(), this.getMaxHealth() * 0.18F));
        this.setSize(this.getSize(), false);
        this.slamCooldown = 20;
        this.shockwaveCooldown = 20;
        this.teleportCooldown = 20;
        this.spawnCrownRageMinions();
        this.spawnAcidPuddles(6, 6.0D);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY(1.0D), this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, this.getX(), this.getY(1.0D), this.getZ(), 90, 1.4D, 1.0D, 1.4D, 0.2D);
        }
        this.playSound(SoundEvents.WITHER_SPAWN, 1.5F, 1.35F);
        this.playSound(SoundEvents.RAVAGER_ROAR, 2.0F, 0.5F);
    }

    private void spawnCrownRageMinions() {
        LivingEntity target = this.getTarget();
        for (int i = 0; i < 2; i++) {
            BlueSlimeEntity minion = com.Momik.usless_mobs.registry.ModEntities.BLAUER_SCHLEIM.get().create(this.level());
            if (minion == null) {
                continue;
            }

            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            double offsetX = Math.cos(angle) * 4.0D;
            double offsetZ = Math.sin(angle) * 4.0D;
            minion.moveTo(this.getX() + offsetX, this.getY() + 1.0D, this.getZ() + offsetZ,
                    this.random.nextFloat() * 360.0F, 0.0F);

            CompoundTag tag = new CompoundTag();
            minion.addAdditionalSaveData(tag);
            tag.putBoolean("Golden", true);
            minion.readAdditionalSaveData(tag);
            minion.setSize(4, true);
            minion.setShootsSpikes(true);
            if (target != null && target.isAlive()) {
                minion.setTarget(target);
            }
            this.level().addFreshEntity(minion);
        }
    }

    private void crownPulse() {
        AABB hitBox = this.getBoundingBox().inflate(7.0D);
        for (Player player : this.level().getEntitiesOfClass(Player.class, hitBox)) {
            Vec3 push = player.position().subtract(this.position());
            if (push.lengthSqr() < 1.0E-4D) {
                push = new Vec3(0.0D, 0.0D, 1.0D);
            }
            push = push.normalize();
            player.push(push.x * 1.2D, 0.35D, push.z * 1.2D);
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 2));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 70, 1));
            player.hurtMarked = true;
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY(0.5D), this.getZ(), 48, 2.5D, 0.2D, 2.5D, 0.05D);
        }
        this.playSound(SoundEvents.WARDEN_SONIC_BOOM, 1.2F, 1.7F);
    }

    private void tickFinalShield(float hpRatio) {
        if (this.currentDifficulty() != Difficulty.HARD || this.phase2Active) {
            this.finalShieldActive = false;
            this.finalShieldGoldenIds = new java.util.UUID[0];
            return;
        }

        if (this.finalShieldActive) {
            int aliveGoldens = this.countAliveFinalShieldGoldens();
            if (aliveGoldens <= 0) {
                this.breakFinalShield();
                return;
            }
            this.tickFinalShieldVisuals();
            return;
        }

        if (hpRatio > FINAL_SHIELD_HP_THRESHOLD || this.getHealth() <= 0.0F) {
            return;
        }

        // HP under threshold. Counter is the single source of truth for "may trigger again".
        if (this.finalShieldTriggerCount >= FINAL_SHIELD_MAX_TRIGGERS) {
            return;
        }

        this.finalShieldCooldown--;
        if (this.finalShieldCooldown <= 0) {
            this.triggerFinalShield();
        }
    }

    private void triggerFinalShield() {
        this.finalShieldGoldenIds = this.spawnFinalShieldGoldens();
        if (this.finalShieldGoldenIds.length == 0) {
            this.finalShieldCooldown = FINAL_SHIELD_COOLDOWN_TICKS / 2;
            return;
        }

        this.finalShieldActive = true;
        this.finalShieldCooldown = FINAL_SHIELD_COOLDOWN_TICKS;
        this.finalShieldTriggerCount++;
        this.bossEvent.setName(this.currentBossName());
        this.bossEvent.setColor(BossEvent.BossBarColor.YELLOW);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, this.getX(), this.getY(0.9D), this.getZ(),
                    70, 1.4D, 0.8D, 1.4D, 0.18D);
            serverLevel.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY(0.9D), this.getZ(),
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        this.playSound(SoundEvents.BEACON_ACTIVATE, 1.5F, 0.6F);
        this.playSound(SoundEvents.WITHER_AMBIENT, 1.2F, 1.35F);
    }

    private java.util.UUID[] spawnFinalShieldGoldens() {
        java.util.UUID[] ids = new java.util.UUID[FINAL_SHIELD_MINION_COUNT];
        LivingEntity target = this.getTarget();
        int spawned = 0;

        for (int i = 0; i < FINAL_SHIELD_MINION_COUNT; i++) {
            BlueSlimeEntity golden = this.createGoldenShieldSlime(target);
            if (golden == null) {
                continue;
            }

            double angle = (Math.PI * 2.0D * i) / FINAL_SHIELD_MINION_COUNT;
            double offsetX = Math.cos(angle) * 4.2D;
            double offsetZ = Math.sin(angle) * 4.2D;
            BlockPos candidate = this.findSmallOpenGroundPosition(new Vec3(this.getX() + offsetX, this.getY(), this.getZ() + offsetZ));
            if (candidate != null) {
                golden.moveTo(candidate.getX() + 0.5D, candidate.getY(), candidate.getZ() + 0.5D,
                        this.random.nextFloat() * 360.0F, 0.0F);
                golden.setDeltaMovement(0.0D, 0.25D, 0.0D);
            } else {
                golden.moveTo(this.getX() + offsetX, this.getY() + 1.0D, this.getZ() + offsetZ,
                        this.random.nextFloat() * 360.0F, 0.0F);
                golden.setDeltaMovement(offsetX * 0.18D, 0.45D, offsetZ * 0.18D);
            }

            this.level().addFreshEntity(golden);
            ids[spawned] = golden.getUUID();
            spawned++;
        }

        if (spawned == ids.length) {
            return ids;
        }

        java.util.UUID[] compact = new java.util.UUID[spawned];
        System.arraycopy(ids, 0, compact, 0, spawned);
        return compact;
    }

    private BlueSlimeEntity createGoldenShieldSlime(LivingEntity target) {
        BlueSlimeEntity golden = com.Momik.usless_mobs.registry.ModEntities.BLAUER_SCHLEIM.get().create(this.level());
        if (golden == null) {
            return null;
        }

        CompoundTag tag = new CompoundTag();
        golden.addAdditionalSaveData(tag);
        tag.putBoolean("Golden", true);
        golden.readAdditionalSaveData(tag);
        golden.setSize(3 + this.random.nextInt(2), true);
        golden.setShootsSpikes(true);
        golden.getPersistentData().putBoolean(BlueSlimeEntity.FINAL_SHIELD_MINION_TAG, true);
        golden.setPersistenceRequired();
        if (target != null && target.isAlive()) {
            golden.setTarget(target);
        }
        return golden;
    }

    private int countAliveFinalShieldGoldens() {
        int alive = 0;
        if (this.level() instanceof ServerLevel serverLevel) {
            for (java.util.UUID id : this.finalShieldGoldenIds) {
                if (id == null) {
                    continue;
                }
                net.minecraft.world.entity.Entity entity = serverLevel.getEntity(id);
                if (entity != null && entity.isAlive()) {
                    alive++;
                }
            }
        }
        return alive;
    }

    private void tickFinalShieldVisuals() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (this.tickCount % 3 == 0) {
            serverLevel.sendParticles(this.goldWarningParticle(), this.getX(), this.getY(0.9D), this.getZ(),
                    12, 1.25D, 0.6D, 1.25D, 0.0D);
        }
        if (this.tickCount % 10 == 0) {
            Vec3 kingCenter = new Vec3(this.getX(), this.getY(0.75D), this.getZ());
            for (java.util.UUID id : this.finalShieldGoldenIds) {
                if (id == null) {
                    continue;
                }
                net.minecraft.world.entity.Entity entity = serverLevel.getEntity(id);
                if (entity != null && entity.isAlive()) {
                    this.sendParticleLine(serverLevel, entity.position().add(0.0D, 0.55D, 0.0D), kingCenter, this.goldWarningParticle(), 8);
                }
            }
        }
        if (this.tickCount % 50 == 0) {
            this.playSound(SoundEvents.BEACON_AMBIENT, 0.8F, 0.7F);
        }
    }

    private void breakFinalShield() {
        this.finalShieldActive = false;
        this.finalShieldGoldenIds = new java.util.UUID[0];
        this.finalShieldCooldown = FINAL_SHIELD_COOLDOWN_TICKS;
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY(0.8D), this.getZ(),
                    3, 0.8D, 0.4D, 0.8D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY(0.8D), this.getZ(),
                    34, 1.1D, 0.55D, 1.1D, 0.15D);
        }
        this.playSound(SoundEvents.GLASS_BREAK, 1.6F, 0.55F);
    }

    private void triggerPhase2() {
        this.phase2Triggered = true;
        this.phase2Active = true;
        this.phase2RemainingTicks = this.phase2DurationTicks();
        this.phase2EnterHp = this.getHealth();
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
        this.phase2GoldenIds = this.spawnPhase2Goldens();

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY(0.5D), this.getZ(),
                    120, 1.6D, 1.6D, 1.6D, 0.6D);
            serverLevel.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY(0.5D), this.getZ(),
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
            serverLevel.sendParticles(this.getParticleType(), this.getX(), this.getY(0.5D), this.getZ(),
                    80, 1.4D, 1.0D, 1.4D, 0.15D);
        }
        this.playSound(SoundEvents.WITHER_SPAWN, 1.4F, 1.6F);
        this.playSound(SoundEvents.SLIME_SQUISH, 2.0F, 0.4F);
    }

    private java.util.UUID[] spawnPhase2Goldens() {
        int count = this.phase2GoldenCount();
        java.util.UUID[] ids = new java.util.UUID[count];
        LivingEntity target = this.getTarget();
        for (int i = 0; i < count; i++) {
            BlueSlimeEntity golden = com.Momik.usless_mobs.registry.ModEntities.BLAUER_SCHLEIM.get().create(this.level());
            if (golden == null) {
                continue;
            }
            double angle = (Math.PI * 2.0D * i) / count;
            double offsetX = Math.cos(angle) * 3.0D;
            double offsetZ = Math.sin(angle) * 3.0D;
            golden.moveTo(this.getX() + offsetX, this.getY() + 2.0D, this.getZ() + offsetZ,
                    this.random.nextFloat() * 360.0F, 0.0F);

            CompoundTag tag = new CompoundTag();
            golden.addAdditionalSaveData(tag);
            tag.putBoolean("Golden", true);
            golden.readAdditionalSaveData(tag);
            golden.setSize(this.currentDifficulty() == Difficulty.HARD ? 4 : 3, true);
            golden.setShootsSpikes(this.currentDifficulty() != Difficulty.EASY);
            golden.getPersistentData().putBoolean(BlueSlimeEntity.KING_SPLIT_MINION_TAG, true);
            golden.setDeltaMovement(offsetX * 0.35D, 0.6D, offsetZ * 0.35D);

            if (target != null && target.isAlive()) {
                golden.setTarget(target);
            }

            this.level().addFreshEntity(golden);
            ids[i] = golden.getUUID();
        }
        return ids;
    }

    private void tickPhase2Visuals() {
        if (this.tickCount % 4 == 0 && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT, this.getX(), this.getY(1.2D), this.getZ(),
                    8, 0.8D, 0.6D, 0.8D, 0.5D);
        }
    }

    private void resolvePhase2() {
        this.phase2Active = false;
        int aliveCount = 0;
        if (this.level() instanceof ServerLevel serverLevel) {
            for (java.util.UUID id : this.phase2GoldenIds) {
                if (id == null) continue;
                net.minecraft.world.entity.Entity entity = serverLevel.getEntity(id);
                if (entity != null && entity.isAlive()) {
                    aliveCount++;
                    entity.remove(RemovalReason.DISCARDED);
                }
            }
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY(0.5D), this.getZ(),
                    4, 1.0D, 0.5D, 1.0D, 0.0D);
            serverLevel.sendParticles(this.getParticleType(), this.getX(), this.getY(0.5D), this.getZ(),
                    60, 1.2D, 0.8D, 1.2D, 0.2D);
        }

        int totalCount = this.phase2GoldenIds.length;
        int killedCount = totalCount - aliveCount;
        if (aliveCount == totalCount) {
            // All goldens survived → King recombines at preserved HP.
            this.setHealth(this.phase2EnterHp);
            this.playSound(SoundEvents.WITHER_DEATH, 1.4F, 0.7F);
        } else {
            float damage = killedCount * (this.getMaxHealth() * this.phase2DamagePerGolden());
            this.setHealth(Math.max(0.5F, this.getHealth() - damage));
            this.playSound(SoundEvents.SLIME_DEATH, 1.6F, 0.5F);
        }
        this.phase2GoldenIds = new java.util.UUID[0];
    }

    private void tickSlamAttack() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            this.slamPhase = 0;
            return;
        }

        if (this.slamPhase == 0) {
            this.slamCooldown--;
            if (this.slamCooldown <= 0 && this.distanceToSqr(target) < 144.0D && this.onGround()) {
                this.startSlam();
            }
            return;
        }

        if (this.slamPhase == 1) {
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY(0.5D), this.getZ(), 12, 0.8D, 0.35D, 0.8D, 0.06D);
                if (this.slamWarmupTicks % 5 == 0) {
                    serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER, this.getX(), this.getY(1.0D), this.getZ(), 2, 0.5D, 0.2D, 0.5D, 0.0D);
                }
            }
            this.slamWarmupTicks--;
            if (this.slamWarmupTicks <= 0) {
                this.launchSlam();
            }
            return;
        }

        if (this.slamPhase == 2) {
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY(0.5D), this.getZ(), 8, 0.6D, 0.3D, 0.6D, 0.05D);
            }
            if (this.onGround() && this.getDeltaMovement().y <= 0.05D && this.tickCount > this.slamLeapStartTick + 4) {
                this.executeSlamImpact();
                this.slamPhase = 0;
                this.slamCooldown = this.scaleCooldown(SLAM_COOLDOWN);
            }
        }
    }

    private void startSlam() {
        this.slamPhase = 1;
        this.slamWarmupTicks = this.slamTelegraphTicks();
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
        this.playSound(SoundEvents.RAVAGER_STEP, 1.6F, 0.6F);
    }

    private void launchSlam() {
        this.slamPhase = 2;
        this.slamLeapStartTick = this.tickCount;
        LivingEntity target = this.getTarget();
        Vec3 toTarget = target != null
            ? target.position().subtract(this.position()).normalize()
            : Vec3.ZERO;
        double leapSpeed = this.currentDifficulty() == Difficulty.HARD ? 0.95D : this.currentDifficulty() == Difficulty.EASY ? 0.48D : 0.68D;
        double leapHeight = this.currentDifficulty() == Difficulty.HARD ? 1.65D : this.currentDifficulty() == Difficulty.EASY ? 1.25D : 1.45D;
        this.setDeltaMovement(toTarget.x * leapSpeed, leapHeight, toTarget.z * leapSpeed);
        this.hasImpulse = true;
        this.playSound(SoundEvents.SLIME_JUMP, 2.0F, 0.5F);
    }

    private void executeSlamImpact() {
        Level level = this.level();
        double cx = this.getX();
        double cy = this.getY();
        double cz = this.getZ();
        double radius = this.slamRadius();
        float damage = this.slamDamage();

        AABB hitBox = new AABB(cx - radius, cy - 1.0D, cz - radius, cx + radius, cy + 2.0D, cz + radius);
        for (Player player : level.getEntitiesOfClass(Player.class, hitBox)) {
            double dist = player.distanceTo(this);
            if (dist > radius) continue;
            player.hurt(this.createSlimeDamageSource(), damage);
            Vec3 push = player.position().subtract(this.position()).normalize();
            double pushPower = this.currentDifficulty() == Difficulty.HARD ? 2.2D : this.currentDifficulty() == Difficulty.EASY ? 1.2D : 1.6D;
            player.push(push.x * pushPower, 0.7D, push.z * pushPower);
            player.hurtMarked = true;
        }

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, cx, cy + 0.3D, cz, 4, 0.5D, 0.0D, 0.5D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.POOF, cx, cy + 0.1D, cz, 60, radius, 0.2D, radius, 0.1D);
        }
        this.playSound(SoundEvents.GENERIC_EXPLODE, 1.4F, 1.2F);

        this.spawnSlimePuddles();
    }

    private void spawnSlimePuddles() {
        int puddleCount = this.enraged ? 4 : 2;
        if (this.currentDifficulty() == Difficulty.EASY) {
            puddleCount = Math.max(1, puddleCount - 1);
        } else if (this.currentDifficulty() == Difficulty.HARD) {
            puddleCount += 2;
        }
        double radius = this.slamRadius();
        for (int i = 0; i < puddleCount; i++) {
            double angle = (Math.PI * 2.0D * i) / puddleCount + this.random.nextDouble() * 0.5D;
            double dx = Math.cos(angle) * (radius * 0.6D);
            double dz = Math.sin(angle) * (radius * 0.6D);

            AreaEffectCloud puddle = new AreaEffectCloud(this.level(), this.getX() + dx, this.getY(), this.getZ() + dz);
            puddle.setOwner(this);
            puddle.setRadius(2.2F);
            puddle.setDuration(PUDDLE_DURATION_TICKS);
            puddle.setRadiusOnUse(-0.05F);
            puddle.setWaitTime(10);
            puddle.setParticle(this.getParticleType());
            puddle.setFixedColor(0x8A4DC9);
            puddle.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
            puddle.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
            this.level().addFreshEntity(puddle);
        }

        if (this.currentDifficulty() == Difficulty.HARD) {
            this.spawnAcidPuddles(2, radius + 1.0D);
        }
    }

    private void spawnAcidPuddles(int puddleCount, double radius) {
        for (int i = 0; i < puddleCount; i++) {
            double angle = (Math.PI * 2.0D * i) / puddleCount + this.random.nextDouble() * 0.7D;
            double dx = Math.cos(angle) * (radius * (0.45D + this.random.nextDouble() * 0.35D));
            double dz = Math.sin(angle) * (radius * (0.45D + this.random.nextDouble() * 0.35D));

            AreaEffectCloud puddle = new AreaEffectCloud(this.level(), this.getX() + dx, this.getY(), this.getZ() + dz);
            puddle.setOwner(this);
            puddle.setRadius(1.8F);
            puddle.setDuration(180);
            puddle.setRadiusPerTick(-0.004F);
            puddle.setWaitTime(5);
            puddle.setParticle(ParticleTypes.ITEM_SLIME);
            puddle.setFixedColor(0x74C850);
            puddle.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0));
            puddle.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
            this.level().addFreshEntity(puddle);
        }
    }

    private int spikeVolleyCount() {
        int count;
        switch (this.currentDifficulty()) {
            case EASY:
                count = 3;
                break;
            case HARD:
                count = 8;
                break;
            default:
                count = 5;
                break;
        }
        return this.crownRageActive ? count + 3 : count;
    }

    private void tickSpikeVolley() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            this.spikeVolleyTelegraphTicks = 0;
            this.spikeVolleyCooldown = Math.min(this.spikeVolleyCooldown, 60);
            return;
        }

        if (this.distanceToSqr(target) > 900.0D || !this.hasLineOfSight(target)) {
            this.spikeVolleyTelegraphTicks = 0;
            this.spikeVolleyCooldown = Math.min(this.spikeVolleyCooldown, 45);
            return;
        }

        if (this.spikeVolleyTelegraphTicks > 0) {
            this.telegraphSpikeVolley(target);
            this.spikeVolleyTelegraphTicks--;
            if (this.spikeVolleyTelegraphTicks <= 0) {
                this.shootSpikeVolley(target);
                this.resetSpikeVolleyCooldown();
            }
            return;
        }

        this.spikeVolleyCooldown--;
        if (this.spikeVolleyCooldown <= 0) {
            this.spikeVolleyTelegraphTicks = SPIKE_VOLLEY_TELEGRAPH_TICKS;
            this.telegraphSpikeVolley(target);
            this.playSound(SoundEvents.SLIME_ATTACK, 1.2F, 0.55F);
        }
    }

    private void resetSpikeVolleyCooldown() {
        int nextCooldown = this.crownRageActive ? SPIKE_VOLLEY_COOLDOWN / 2 : SPIKE_VOLLEY_COOLDOWN;
        this.spikeVolleyCooldown = this.enraged ? this.scaleCooldown(nextCooldown / 2) : this.scaleCooldown(nextCooldown);
    }

    private void telegraphSpikeVolley(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        ParticleOptions blueSpark = this.blueWarningParticle();
        serverLevel.sendParticles(blueSpark, this.getX(), this.getY(0.82D), this.getZ(),
                8, 0.75D, 0.28D, 0.75D, 0.0D);

        if (this.spikeVolleyTelegraphTicks % 4 == 0) {
            Vec3 from = new Vec3(this.getX(), this.getY(0.82D), this.getZ());
            Vec3 to = new Vec3(target.getX(), target.getY(0.55D), target.getZ());
            this.sendParticleLine(serverLevel, from, to, blueSpark, 9);
        }
    }

    private void shootSpikeVolley(LivingEntity target) {
        int count = this.spikeVolleyCount();
        double startY = this.getY(0.7D);

        for (int i = 0; i < count; i++) {
            SlimeSpikeProjectile spike = new SlimeSpikeProjectile(this.level(), this);
            double spread = (i - ((count - 1) / 2.0D)) * 0.18D;
            double randomSpreadX = (this.random.nextDouble() - 0.5D) * 0.35D;
            double randomSpreadZ = (this.random.nextDouble() - 0.5D) * 0.35D;
            double dx = target.getX() - this.getX() + randomSpreadX + spread;
            double dy = target.getY(0.55D) - startY;
            double dz = target.getZ() - this.getZ() + randomSpreadZ - spread;
            double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

            spike.setPos(this.getX(), startY, this.getZ());
            spike.shoot(dx, dy + horizontalDistance * 0.10D, dz, this.currentDifficulty() == Difficulty.HARD ? 1.65F : 1.35F, 5.0F);
            this.level().addFreshEntity(spike);
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY(0.8D), this.getZ(), 24, 1.0D, 0.45D, 1.0D, 0.08D);
        }
        this.playSound(SoundEvents.SLIME_ATTACK, 1.6F, 0.65F);
    }

    private void tickRoyalSnare() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            this.royalSnareTelegraphTicks = 0;
            this.royalSnareCooldown = Math.min(this.royalSnareCooldown, 80);
            return;
        }

        if (this.distanceToSqr(target) > 1024.0D || !this.hasLineOfSight(target)) {
            this.royalSnareTelegraphTicks = 0;
            this.royalSnareCooldown = Math.min(this.royalSnareCooldown, 60);
            return;
        }

        if (this.royalSnareTelegraphTicks > 0) {
            this.telegraphRoyalSnare(target);
            this.royalSnareTelegraphTicks--;
            if (this.royalSnareTelegraphTicks <= 0) {
                this.spawnRoyalSnare(target);
                this.resetRoyalSnareCooldown();
            }
            return;
        }

        this.royalSnareCooldown--;
        if (this.royalSnareCooldown <= 0) {
            this.royalSnareTelegraphTicks = ROYAL_SNARE_TELEGRAPH_TICKS;
            this.telegraphRoyalSnare(target);
            this.playSound(SoundEvents.SLIME_SQUISH, 1.35F, 0.38F);
        }
    }

    private void resetRoyalSnareCooldown() {
        int nextCooldown = this.crownRageActive ? ROYAL_SNARE_COOLDOWN / 2 : ROYAL_SNARE_COOLDOWN;
        this.royalSnareCooldown = this.scaleCooldown(nextCooldown);
    }

    private void telegraphRoyalSnare(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        boolean hard = this.currentDifficulty() == Difficulty.HARD;
        double radius = hard ? 2.4D : 2.0D;
        ParticleOptions snareWarning = this.purpleWarningParticle();
        this.sendParticleRing(serverLevel, target.getX(), target.getY() + 0.08D, target.getZ(), radius, snareWarning, 32);
        if (this.crownRageActive) {
            this.sendParticleRing(serverLevel, target.getX(), target.getY() + 0.08D, target.getZ(), radius + 2.1D, snareWarning, 42);
        }

        if (this.royalSnareTelegraphTicks % 8 == 0) {
            serverLevel.sendParticles(ParticleTypes.SQUID_INK, target.getX(), target.getY(0.15D), target.getZ(),
                    6, 0.35D, 0.03D, 0.35D, 0.01D);
        }
    }

    private void spawnRoyalSnare(LivingEntity target) {
        boolean hard = this.currentDifficulty() == Difficulty.HARD;
        int puddles = this.crownRageActive ? 3 : hard ? 2 : 1;

        for (int i = 0; i < puddles; i++) {
            double angle = (Math.PI * 2.0D * i) / Math.max(1, puddles);
            double offset = i == 0 ? 0.0D : 2.1D;
            AreaEffectCloud snare = new AreaEffectCloud(this.level(),
                    target.getX() + Math.cos(angle) * offset,
                    target.getY(),
                    target.getZ() + Math.sin(angle) * offset);
            snare.setOwner(this);
            snare.setRadius(hard ? 2.4F : 2.0F);
            snare.setDuration(hard ? 180 : 140);
            snare.setRadiusOnUse(-0.03F);
            snare.setWaitTime(0);
            snare.setParticle(this.getParticleType());
            snare.setFixedColor(0x8A4DC9);
            snare.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90, hard ? 3 : 2));
            snare.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 90, hard ? 1 : 0));
            if (hard || this.crownRageActive) {
                snare.addEffect(new MobEffectInstance(MobEffects.POISON, 70, 0));
            }
            this.level().addFreshEntity(snare);
        }

        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 55, hard ? 2 : 1));
        target.hurt(this.createSlimeDamageSource(), hard ? 5.0F : 3.0F);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SQUID_INK, target.getX(), target.getY(0.2D), target.getZ(), 18, 0.8D, 0.05D, 0.8D, 0.02D);
        }
        this.playSound(SoundEvents.SLIME_SQUISH, 1.4F, 0.45F);
    }

    private void tickSlimeAbsorption() {
        if (this.getHealth() >= this.getMaxHealth() * 0.98F) {
            this.absorbTelegraphTicks = 0;
            this.absorbTargetId = null;
            this.absorbCooldown = Math.min(this.absorbCooldown, 60);
            return;
        }

        if (this.absorbTelegraphTicks > 0) {
            Slime slime = this.resolveAbsorbTarget();
            if (slime == null) {
                this.absorbTelegraphTicks = 0;
                this.absorbTargetId = null;
                this.absorbCooldown = 35;
                return;
            }

            this.telegraphSlimeAbsorption(slime);
            this.absorbTelegraphTicks--;
            if (this.absorbTelegraphTicks <= 0) {
                this.absorbSlime(slime);
                this.absorbTargetId = null;
                this.absorbCooldown = this.scaleCooldown(this.crownRageActive ? ABSORB_COOLDOWN / 2 : ABSORB_COOLDOWN);
            }
            return;
        }

        this.absorbCooldown--;
        if (this.absorbCooldown > 0) {
            return;
        }

        Slime absorbed = this.findAbsorbCandidate();
        if (absorbed == null) {
            this.absorbCooldown = 50;
            return;
        }

        this.absorbTargetId = absorbed.getUUID();
        this.absorbTelegraphTicks = ABSORB_TELEGRAPH_TICKS;
        this.telegraphSlimeAbsorption(absorbed);
        this.playSound(SoundEvents.SLIME_SQUISH, 1.5F, 0.3F);
    }

    private Slime resolveAbsorbTarget() {
        if (this.absorbTargetId != null && this.level() instanceof ServerLevel serverLevel) {
            net.minecraft.world.entity.Entity entity = serverLevel.getEntity(this.absorbTargetId);
            if (entity instanceof Slime slime && this.canAbsorb(slime)) {
                return slime;
            }
        }
        return this.findAbsorbCandidate();
    }

    private Slime findAbsorbCandidate() {
        AABB absorbArea = this.getBoundingBox().inflate(ABSORB_RADIUS);
        java.util.List<Slime> slimes = this.level().getEntitiesOfClass(Slime.class, absorbArea,
                this::canAbsorb);
        if (slimes.isEmpty()) {
            return null;
        }

        Slime absorbed = slimes.get(0);
        double bestDistance = this.distanceToSqr(absorbed);
        for (Slime slime : slimes) {
            double distance = this.distanceToSqr(slime);
            if (distance < bestDistance) {
                absorbed = slime;
                bestDistance = distance;
            }
        }
        return absorbed;
    }

    private boolean canAbsorb(Slime slime) {
        return slime != this && !(slime instanceof KingSlimeEntity) && slime.isAlive()
                && this.distanceToSqr(slime) <= ABSORB_RADIUS * ABSORB_RADIUS;
    }

    private void telegraphSlimeAbsorption(Slime slime) {
        this.bossEvent.setName(Component.translatable("entity.usless_mobs.king_schleim.phase.absorb"));
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        ParticleOptions goldSpark = this.goldWarningParticle();
        Vec3 from = new Vec3(slime.getX(), slime.getY(0.5D), slime.getZ());
        Vec3 to = new Vec3(this.getX(), this.getY(0.75D), this.getZ());
        this.sendParticleLine(serverLevel, from, to, goldSpark, 12);
        serverLevel.sendParticles(ParticleTypes.ITEM_SLIME, slime.getX(), slime.getY(0.5D), slime.getZ(),
                3, 0.25D, 0.25D, 0.25D, 0.03D);

        if (this.absorbTelegraphTicks % 6 == 0) {
            serverLevel.sendParticles(ParticleTypes.HEART, this.getX(), this.getY(0.85D), this.getZ(),
                    1, 0.45D, 0.25D, 0.45D, 0.02D);
            this.playSound(SoundEvents.SLIME_SQUISH, 0.55F, 1.7F);
        }
    }

    private ParticleOptions blueWarningParticle() {
        return new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.12F, 0.42F, 1.0F), 1.2F);
    }

    private ParticleOptions purpleWarningParticle() {
        return new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.55F, 0.18F, 0.95F), 1.15F);
    }

    private ParticleOptions goldWarningParticle() {
        return new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(1.0F, 0.78F, 0.12F), 1.2F);
    }

    private void sendParticleLine(ServerLevel serverLevel, Vec3 from, Vec3 to, ParticleOptions particle, int steps) {
        Vec3 delta = to.subtract(from);
        for (int i = 0; i <= steps; i++) {
            double progress = (double) i / (double) steps;
            Vec3 position = from.add(delta.scale(progress));
            serverLevel.sendParticles(particle, position.x, position.y, position.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private void sendParticleRing(ServerLevel serverLevel, double x, double y, double z, double radius, ParticleOptions particle, int points) {
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0D * i) / points;
            serverLevel.sendParticles(particle, x + Math.cos(angle) * radius, y, z + Math.sin(angle) * radius,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private void absorbSlime(Slime slime) {
        int size = Math.max(1, slime.getSize());
        // Reduced base heal (was 10 + size*7 → up to 66 HP); now caps at ~36 HP per absorb at king-size 6.
        float healAmount = 4.0F + size * 4.0F;
        if (slime instanceof BlueSlimeEntity blueSlime && blueSlime.isGolden()) {
            healAmount += 8.0F;
        }
        if (this.currentDifficulty() == Difficulty.HARD) {
            healAmount *= 1.35F;
        } else if (this.currentDifficulty() == Difficulty.EASY) {
            healAmount *= 0.6F;
        }

        // HARD CAP: King can never heal above ABSORB_MAX_HEAL_FRACTION of max HP via absorption.
        // This stops the boss from infinitely regenerating when many minions are around — at
        // some point the player's damage must stick.
        float maxAllowed = this.getMaxHealth() * ABSORB_MAX_HEAL_FRACTION;
        if (this.getHealth() >= maxAllowed) {
            // Already above cap — give a tiny token heal so the absorb still feels rewarding.
            healAmount = Math.min(healAmount, 1.5F);
        } else {
            // Clamp so we don't overshoot the cap.
            float roomLeft = maxAllowed - this.getHealth();
            healAmount = Math.min(healAmount, roomLeft);
        }

        this.heal(healAmount);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(this.getParticleType(), slime.getX(), slime.getY(0.5D), slime.getZ(), 24, 0.45D, 0.35D, 0.45D, 0.08D);
            serverLevel.sendParticles(ParticleTypes.HEART, this.getX(), this.getY(0.8D), this.getZ(), 6, 0.8D, 0.4D, 0.8D, 0.05D);
        }
        this.playSound(SoundEvents.SLIME_SQUISH, 1.8F, 0.35F);
        slime.discard();
    }

    private void tickShockwave() {
        this.shockwaveCooldown--;
        if (this.shockwaveCooldown > 0) return;

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            this.shockwaveCooldown = 40;
            return;
        }
        double radius = this.shockwaveRadius();
        if (this.distanceToSqr(target) > (radius + 1.5D) * (radius + 1.5D)) {
            this.shockwaveCooldown = 40;
            return;
        }

        AABB hitBox = this.getBoundingBox().inflate(radius);
        for (Player player : this.level().getEntitiesOfClass(Player.class, hitBox)) {
            Vec3 push = player.position().subtract(this.position()).normalize();
            double force = this.currentDifficulty() == Difficulty.HARD ? 2.0D : this.currentDifficulty() == Difficulty.EASY ? 1.0D : 1.4D;
            player.push(push.x * force, 0.5D, push.z * force);
            if (this.currentDifficulty() == Difficulty.HARD) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            }
            player.hurtMarked = true;
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 32; i++) {
                double a = (Math.PI * 2.0D * i) / 32;
                double r = radius;
                serverLevel.sendParticles(ParticleTypes.CLOUD, this.getX() + Math.cos(a) * r, this.getY(0.2D), this.getZ() + Math.sin(a) * r, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
        this.playSound(SoundEvents.WARDEN_SONIC_BOOM, 1.5F, 1.4F);
        this.shockwaveCooldown = this.enraged ? this.scaleCooldown(SHOCKWAVE_COOLDOWN / 2) : this.scaleCooldown(SHOCKWAVE_COOLDOWN);
    }

    @Override
    protected void dealDamage(LivingEntity target) {
        if (!this.isAlive() || !this.isDealsDamage()) {
            return;
        }

        float attackReach = 0.8F * KING_SIZE;
        if (this.distanceToSqr(target) > (double) (attackReach * attackReach) || !this.hasLineOfSight(target)) {
            return;
        }

        if (!target.hurt(this.createSlimeDamageSource(), (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE))) {
            return;
        }

        this.playSound(SoundEvents.SLIME_ATTACK, 1.4F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 0.85F);
        this.doEnchantDamageEffects(this, target);
        int slowAmplifier = this.currentDifficulty() == Difficulty.HARD ? 3 : this.currentDifficulty() == Difficulty.EASY ? 1 : 2;
        int weaknessAmplifier = this.currentDifficulty() == Difficulty.HARD ? 2 : this.currentDifficulty() == Difficulty.EASY ? 0 : 1;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 140, slowAmplifier));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 140, weaknessAmplifier));
        target.knockback(this.currentDifficulty() == Difficulty.HARD ? 1.9D : 1.4D, Mth.sin(this.getYRot() * ((float) Math.PI / 180F)), -Mth.cos(this.getYRot() * ((float) Math.PI / 180F)));
    }

    private boolean isLikelyCriticalHit(Player player) {
        return player.fallDistance > 0.0F
                && !player.onGround()
                && !player.isInWater()
                && !player.isPassenger();
    }

    private float adjustIncomingPlayerDamage(Player player, float amount) {
        float adjusted = Math.min(amount, this.maxDamagePerHit());

        if (player.getMainHandItem().isEmpty()) {
            adjusted = Math.min(adjusted * 0.15F, 0.5F);
        }

        if (this.isLikelyCriticalHit(player)) {
            switch (this.currentDifficulty()) {
                case EASY:
                    adjusted *= 0.75F;
                    break;
                case HARD:
                    adjusted *= 0.35F;
                    break;
                default:
                    adjusted *= 0.55F;
                    break;
            }
            this.punishCloseCritical(player);
        }

        return adjusted;
    }

    private boolean isLowHpProjectileFromPlayer(DamageSource source) {
        return this.getHealth() / this.getMaxHealth() <= LOW_HP_PROJECTILE_GUARD_THRESHOLD
                && source.getEntity() instanceof Player
                && source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile;
    }

    private float adjustLowHpProjectileDamage(DamageSource source, float amount) {
        if (!this.isLowHpProjectileFromPlayer(source)) {
            return amount;
        }

        if (this.lowHpProjectileGuardTicks > 0) {
            this.showProjectileGuard(false);
            return 0.0F;
        }

        this.lowHpProjectileGuardTicks = this.scaleProjectileGuardTicks();
        this.showProjectileGuard(true);

        float multiplier;
        switch (this.currentDifficulty()) {
            case EASY:
                multiplier = 0.55F;
                break;
            case HARD:
                multiplier = this.crownRageActive ? 0.12F : 0.20F;
                break;
            default:
                multiplier = 0.35F;
                break;
        }
        return Math.min(amount * multiplier, this.currentDifficulty() == Difficulty.HARD ? 2.0F : 3.5F);
    }

    private int scaleProjectileGuardTicks() {
        switch (this.currentDifficulty()) {
            case EASY:
                return 12;
            case HARD:
                return this.crownRageActive ? 30 : 24;
            default:
                return LOW_HP_PROJECTILE_GUARD_TICKS;
        }
    }

    private void showProjectileGuard(boolean partialHit) {
        this.royalGuardBossbarTicks = ROYAL_GUARD_BOSSBAR_TICKS;
        this.bossEvent.setName(this.currentBossName());
        this.bossEvent.setColor(BossEvent.BossBarColor.YELLOW);
        if (this.level() instanceof ServerLevel serverLevel) {
            ParticleOptions particle = partialHit ? this.goldWarningParticle() : this.blueWarningParticle();
            serverLevel.sendParticles(particle, this.getX(), this.getY(0.72D), this.getZ(),
                    partialHit ? 18 : 10, 1.15D, 0.55D, 1.15D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.ITEM_SLIME, this.getX(), this.getY(0.55D), this.getZ(),
                    partialHit ? 8 : 4, 0.8D, 0.35D, 0.8D, 0.04D);
        }
        this.playSound(SoundEvents.SHIELD_BLOCK, partialHit ? 1.0F : 0.8F, partialHit ? 0.75F : 1.25F);
        this.playSound(SoundEvents.SLIME_SQUISH, partialHit ? 1.2F : 0.8F, partialHit ? 0.55F : 1.7F);
    }

    private void showFinalShieldBlock() {
        if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 3 == 0) {
            serverLevel.sendParticles(this.goldWarningParticle(), this.getX(), this.getY(0.8D), this.getZ(),
                    12, 1.1D, 0.45D, 1.1D, 0.0D);
        }
        if (this.tickCount % 8 == 0) {
            this.playSound(SoundEvents.SHIELD_BLOCK, 1.0F, 0.55F);
        }
    }

    private boolean shouldTriggerFinalShieldFromHit(float amount) {
        if (this.currentDifficulty() != Difficulty.HARD || this.phase2Active || this.finalShieldActive) {
            return false;
        }
        if (this.finalShieldTriggerCount >= FINAL_SHIELD_MAX_TRIGGERS) {
            return false;
        }
        float thresholdHealth = this.getMaxHealth() * FINAL_SHIELD_HP_THRESHOLD;
        return this.getHealth() > thresholdHealth && this.getHealth() - amount <= thresholdHealth;
    }

    private void punishCloseCritical(Player player) {
        if (this.distanceToSqr(player) > 25.0D) {
            return;
        }

        Vec3 push = player.position().subtract(this.position());
        if (push.lengthSqr() < 1.0E-4D) {
            push = new Vec3(0.0D, 0.0D, 1.0D);
        }
        push = push.normalize();

        double force = this.currentDifficulty() == Difficulty.HARD ? 1.65D : this.currentDifficulty() == Difficulty.EASY ? 0.8D : 1.15D;
        player.push(push.x * force, 0.45D, push.z * force);
        player.hurtMarked = true;
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, this.currentDifficulty() == Difficulty.HARD ? 70 : 40, this.currentDifficulty() == Difficulty.HARD ? 2 : 0));

        if (this.currentDifficulty() == Difficulty.HARD) {
            player.hurt(this.createSlimeDamageSource(), 3.0F);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Slime) {
            return false;
        }
        if (this.phase2Active) {
            // King is split mid-fight — damage him via the goldens instead.
            return false;
        }
        if (this.finalShieldActive) {
            this.showFinalShieldBlock();
            return false;
        }
        if (this.shouldTriggerFinalShieldFromHit(amount)) {
            this.finalShieldTriggeredOnce = true;
            this.triggerFinalShield();
            this.showFinalShieldBlock();
            return false;
        }
        amount = this.adjustLowHpProjectileDamage(source, amount);
        if (amount <= 0.0F) {
            return false;
        }
        if (source.getEntity() instanceof Player player) {
            amount = this.adjustIncomingPlayerDamage(player, amount);
            if (amount <= 0.0F) {
                return false;
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.phase2Triggered = tag.getBoolean("Phase2Triggered");
        this.phase2Active = tag.getBoolean("Phase2Active");
        this.phase2RemainingTicks = tag.getInt("Phase2RemainingTicks");
        this.phase2EnterHp = tag.getFloat("Phase2EnterHp");
        int goldenCount = tag.getInt("Phase2GoldenCount");
        this.phase2GoldenIds = new java.util.UUID[Math.max(0, goldenCount)];
        for (int i = 0; i < this.phase2GoldenIds.length; i++) {
            String key = "Phase2Golden" + i;
            if (tag.hasUUID(key)) {
                this.phase2GoldenIds[i] = tag.getUUID(key);
            }
        }
        this.enraged = tag.getBoolean("Enraged");
        this.crownRageTriggered = tag.getBoolean("CrownRageTriggered");
        this.crownRageActive = tag.getBoolean("CrownRageActive");
        this.crownRageRemainingTicks = tag.getInt("CrownRageRemainingTicks");
        this.finalShieldActive = tag.getBoolean("FinalShieldActive");
        this.finalShieldTriggeredOnce = tag.getBoolean("FinalShieldTriggeredOnce");
        this.finalShieldCooldown = tag.contains("FinalShieldCooldown") ? tag.getInt("FinalShieldCooldown") : FINAL_SHIELD_COOLDOWN_TICKS;
        this.finalShieldTriggerCount = tag.getInt("FinalShieldTriggerCount");
        int finalGoldenCount = tag.getInt("FinalShieldGoldenCount");
        this.finalShieldGoldenIds = new java.util.UUID[Math.max(0, finalGoldenCount)];
        for (int i = 0; i < this.finalShieldGoldenIds.length; i++) {
            String key = "FinalShieldGolden" + i;
            if (tag.hasUUID(key)) {
                this.finalShieldGoldenIds[i] = tag.getUUID(key);
            }
        }
        this.setSize(this.getSize(), false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Phase2Triggered", this.phase2Triggered);
        tag.putBoolean("Phase2Active", this.phase2Active);
        tag.putInt("Phase2RemainingTicks", this.phase2RemainingTicks);
        tag.putFloat("Phase2EnterHp", this.phase2EnterHp);
        tag.putInt("Phase2GoldenCount", this.phase2GoldenIds.length);
        for (int i = 0; i < this.phase2GoldenIds.length; i++) {
            if (this.phase2GoldenIds[i] != null) {
                tag.putUUID("Phase2Golden" + i, this.phase2GoldenIds[i]);
            }
        }
        tag.putBoolean("Enraged", this.enraged);
        tag.putBoolean("CrownRageTriggered", this.crownRageTriggered);
        tag.putBoolean("CrownRageActive", this.crownRageActive);
        tag.putInt("CrownRageRemainingTicks", this.crownRageRemainingTicks);
        tag.putBoolean("FinalShieldActive", this.finalShieldActive);
        tag.putBoolean("FinalShieldTriggeredOnce", this.finalShieldTriggeredOnce);
        tag.putInt("FinalShieldCooldown", this.finalShieldCooldown);
        tag.putInt("FinalShieldTriggerCount", this.finalShieldTriggerCount);
        tag.putInt("FinalShieldGoldenCount", this.finalShieldGoldenIds.length);
        for (int i = 0; i < this.finalShieldGoldenIds.length; i++) {
            if (this.finalShieldGoldenIds[i] != null) {
                tag.putUUID("FinalShieldGolden" + i, this.finalShieldGoldenIds[i]);
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource damageSource, int looting, boolean recentlyHit) {
        Difficulty difficulty = this.currentDifficulty();
        int safeLooting = Math.min(5, Math.max(0, looting));
        this.spawnAtLocation(new ItemStack(com.Momik.usless_mobs.registry.ModItems.SCHLEIMREAKTOR_SCHMIEDEVORLAGE.get()));
        this.spawnAtLocation(new ItemStack(com.Momik.usless_mobs.registry.ModItems.GOLDENER_SCHLEIMBALL.get(), 6 + this.random.nextInt(6 + safeLooting * 2)));
        this.spawnAtLocation(new ItemStack(com.Momik.usless_mobs.registry.ModItems.BLAUER_SCHLEIMBALL.get(), 10 + this.random.nextInt(8 + safeLooting * 2)));
        this.spawnAtLocation(new ItemStack(com.Momik.usless_mobs.registry.ModItems.NETHERITE_SCHLEIMKERN.get()));

        if (this.random.nextFloat() < Math.min(0.55F, 0.25F + (0.05F * safeLooting))) {
            this.spawnAtLocation(new ItemStack(Items.GOLD_BLOCK, 1 + this.random.nextInt(3)));
        }

        boolean shouldDropCrown = difficulty == Difficulty.EASY
                ? Config.easyDropsCrown
                : difficulty == Difficulty.HARD ? Config.hardDropsCrown : Config.normalDropsCrown;
        if (shouldDropCrown) {
            this.spawnAtLocation(new ItemStack(com.Momik.usless_mobs.registry.ModItems.KING_SLIME_KRONE.get()));
        }

        if (difficulty == Difficulty.HARD && Config.hardDropsTrophy) {
            ItemStack trophy = new ItemStack(com.Momik.usless_mobs.registry.ModItems.KING_SLIME_TROPHY.get());
            CompoundTag trophyTag = trophy.getOrCreateTag();
            trophyTag.putString("Difficulty", difficulty.name());
            trophyTag.putLong("DefeatedAtUnix", System.currentTimeMillis());
            trophyTag.putInt("Victories", 1);
            if (damageSource.getEntity() instanceof Player player) {
                trophyTag.putString("DefeatedBy", player.getName().getString());
            }

            CompoundTag blockEntityTag = new CompoundTag();
            if (trophyTag.contains("DefeatedBy")) {
                blockEntityTag.putString("DefeatedBy", trophyTag.getString("DefeatedBy"));
            }
            blockEntityTag.putString("Difficulty", trophyTag.getString("Difficulty"));
            blockEntityTag.putLong("DefeatedAtUnix", trophyTag.getLong("DefeatedAtUnix"));
            blockEntityTag.putInt("Victories", trophyTag.getInt("Victories"));
            trophyTag.put("BlockEntityTag", blockEntityTag);

            this.spawnAtLocation(trophy);
            this.spawnAtLocation(new ItemStack(com.Momik.usless_mobs.registry.ModItems.BALANCE_UPGRADE_TEMPLATE.get()));
            // Rare: one random True-Path template
            int templateRoll = this.random.nextInt(3);
            net.minecraft.world.item.Item trueTemplate = switch (templateRoll) {
                case 0 -> com.Momik.usless_mobs.registry.ModItems.TRUE_VOID_TEMPLATE.get();
                case 1 -> com.Momik.usless_mobs.registry.ModItems.TRUE_CELESTIAL_TEMPLATE.get();
                default -> com.Momik.usless_mobs.registry.ModItems.TRUE_LIVING_TEMPLATE.get();
            };
            this.spawnAtLocation(new ItemStack(trueTemplate));
            if (damageSource.getEntity() instanceof ServerPlayer serverPlayer) {
                KingSlimeAdvancements.grant(serverPlayer, KingSlimeAdvancements.HARD_KING_SLIME);
            }
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && reason == RemovalReason.KILLED && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(0.5D), this.getZ(), 6, 1.5D, 1.0D, 1.5D, 0.0D);
            this.playSound(SoundEvents.GENERIC_EXPLODE, 2.0F, 0.6F);
            this.shatterCrownIntoGoldenSlimes();
            // Bypass vanilla Slime.remove() child-spawning: it checks size > 1 and spawns
            // child Slimes of the same EntityType. For King Slime that would spawn new Kings.
            // super.setSize() skips our MIN_KING_SIZE clamp.
            super.setSize(1, false);
        }
        super.remove(reason);
    }

    private void shatterCrownIntoGoldenSlimes() {
        int count = DEATH_MINION_COUNT;
        if (this.currentDifficulty() == Difficulty.EASY) {
            count = 1;
        } else if (this.currentDifficulty() == Difficulty.HARD) {
            count = 4;
        }
        LivingEntity target = this.getKillCredit();
        if (target == null || !target.isAlive()) {
            target = this.getTarget();
        }

        Vec3 forward = target != null && target.isAlive()
                ? this.resolveTargetForward(target)
                : new Vec3(0.0D, 0.0D, 1.0D);
        Vec3 sideways = new Vec3(-forward.z, 0.0D, forward.x);

        for (int i = 0; i < count; i++) {
            BlueSlimeEntity goldenSlime = com.Momik.usless_mobs.registry.ModEntities.BLAUER_SCHLEIM.get().create(this.level());
            if (goldenSlime == null) {
                continue;
            }

            CompoundTag tag = new CompoundTag();
            goldenSlime.addAdditionalSaveData(tag);
            tag.putBoolean("Golden", true);
            goldenSlime.readAdditionalSaveData(tag);
            goldenSlime.setSize(3, true);

            double sideOffset = i == 0 ? -DEATH_MINION_SIDE_OFFSET : DEATH_MINION_SIDE_OFFSET;
            boolean placedInFront = false;
            if (target != null && target.isAlive()) {
                Vec3 desiredPos = target.position()
                        .add(forward.scale(TELEPORT_AHEAD_DISTANCE))
                        .add(sideways.scale(sideOffset));
                BlockPos candidate = this.findOpenGroundPosition(desiredPos);
                if (candidate != null) {
                    goldenSlime.moveTo(candidate.getX() + 0.5D, candidate.getY(), candidate.getZ() + 0.5D,
                            this.random.nextFloat() * 360.0F, 0.0F);
                    goldenSlime.setDeltaMovement(0.0D, 0.25D, 0.0D);
                    placedInFront = true;
                }
            }

            if (!placedInFront) {
                double angle = (Math.PI * 2.0D * i) / count;
                double offsetX = Math.cos(angle) * 1.8D;
                double offsetZ = Math.sin(angle) * 1.8D;
                goldenSlime.moveTo(this.getX() + offsetX, this.getY() + 2.0D, this.getZ() + offsetZ,
                        this.random.nextFloat() * 360.0F, 0.0F);
                goldenSlime.setDeltaMovement(offsetX * 0.35D, 0.5D, offsetZ * 0.35D);
            }

            if (target != null && target.isAlive()) {
                goldenSlime.setTarget(target);
            }

            this.level().addFreshEntity(goldenSlime);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.PORTAL,
                        goldenSlime.getX(), goldenSlime.getY() + 0.7D, goldenSlime.getZ(),
                        28, 0.35D, 0.5D, 0.35D, 0.15D);
                serverLevel.sendParticles(this.getParticleType(),
                        goldenSlime.getX(), goldenSlime.getY() + 0.3D, goldenSlime.getZ(),
                        14, 0.35D, 0.2D, 0.35D, 0.05D);
            }
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY(0.8D), this.getZ(), 32, 1.2D, 0.6D, 1.2D, 0.15D);
        }
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
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    private void summonMinions() {
        int existing = this.level().getEntitiesOfClass(BlueSlimeEntity.class, this.getBoundingBox().inflate(16.0D)).size();
        if (existing >= this.maxNearbyMinions()) {
            return;
        }

        int spawnCount;
        switch (this.currentDifficulty()) {
            case EASY:
                spawnCount = 1 + this.random.nextInt(2);
                break;
            case HARD:
                spawnCount = 3 + this.random.nextInt(3);
                break;
            default:
                spawnCount = 2 + this.random.nextInt(2);
                break;
        }
        float spikeChance = this.enraged ? 0.6F : 0.25F;
        if (this.currentDifficulty() == Difficulty.HARD) {
            spikeChance += 0.25F;
        } else if (this.currentDifficulty() == Difficulty.EASY) {
            spikeChance *= 0.35F;
        }
        LivingEntity target = this.getTarget();

        for (int i = 0; i < spawnCount; i++) {
            BlueSlimeEntity minion = com.Momik.usless_mobs.registry.ModEntities.BLAUER_SCHLEIM.get().create(this.level());
            if (minion == null) {
                continue;
            }

            double offsetX = (this.random.nextDouble() - 0.5D) * 4.0D;
            double offsetZ = (this.random.nextDouble() - 0.5D) * 4.0D;
            minion.moveTo(this.getX() + offsetX, this.getY() + 1.0D, this.getZ() + offsetZ, this.random.nextFloat() * 360.0F, 0.0F);
            int minionSize = this.currentDifficulty() == Difficulty.HARD ? 3 + this.random.nextInt(2) : 2 + this.random.nextInt(2);
            minion.setSize(minionSize, true);

            if (this.random.nextFloat() < spikeChance) {
                minion.setShootsSpikes(true);
            }

            if (target != null && target.isAlive()) {
                minion.setTarget(target);
            }

            this.level().addFreshEntity(minion);
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF, this.getX(), this.getY(0.5D), this.getZ(), 16, 0.6D, 0.3D, 0.6D, 0.02D);
        }
    }

    private DamageSource createSlimeDamageSource() {
        return new DamageSource(this.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SLIMED_DAMAGE_TYPE), this);
    }

    // === GeckoLib ===

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
