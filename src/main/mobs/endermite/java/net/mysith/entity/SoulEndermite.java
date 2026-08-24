package net.mysith.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.mysith.registry.ModItems;
import net.mysith.registry.ModSounds;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class SoulEndermite extends Endermite implements GeoEntity {

    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> ELDER =
            net.minecraft.network.syncher.SynchedEntityData.defineId(
                    SoulEndermite.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    private static final DustParticleOptions CRIMSON_DUST =
            new DustParticleOptions(new Vector3f(0.85F, 0.05F, 0.1F), 1.0F);
    private static final DustParticleOptions DARK_DUST =
            new DustParticleOptions(new Vector3f(0.4F, 0.0F, 0.05F), 1.2F);

    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("animation");
    private static final double VOID_RESCUE_Y = -40.0D;
    private static final int VOID_RESCUE_RADIUS = 192;
    private boolean loggedVoidRescueFailure;
    private int activeTeleportCooldown = 80 + this.random.nextInt(40);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SoulEndermite(EntityType<? extends Endermite> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ELDER, false);
    }

    public boolean isElder() { return this.entityData.get(ELDER); }
    public void setElder(boolean v) { this.entityData.set(ELDER, v); }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Elder", isElder());
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setElder(tag.getBoolean("Elder"));
    }

    @Override
    @SuppressWarnings("deprecation")
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(
            net.minecraft.world.level.ServerLevelAccessor world,
            net.minecraft.world.DifficultyInstance difficulty,
            net.minecraft.world.entity.MobSpawnType reason,
            net.minecraft.world.entity.SpawnGroupData data,
            net.minecraft.nbt.CompoundTag tag) {
        // 10% chance to spawn as Elder variant
        if (this.random.nextFloat() < 0.10F) {
            setElder(true);
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(80.0);
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(11.0);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.40);
            this.setHealth(this.getMaxHealth());
        }
        return super.finalizeSpawn(world, difficulty, reason, data, tag);
    }

    /** Bigger if Elder, normal otherwise. */
    @Override
    public net.minecraft.world.entity.EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose) {
        net.minecraft.world.entity.EntityDimensions base = super.getDimensions(pose);
        return isElder() ? base.scale(1.6F) : base;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Endermite.createAttributes()
                .add(Attributes.MAX_HEALTH, 36.0)        // was 24
                .add(Attributes.ATTACK_DAMAGE, 7.0)      // was 4
                .add(Attributes.MOVEMENT_SPEED, 0.45)    // was 0.35
                .add(Attributes.FOLLOW_RANGE, 48.0);     // was 32
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof Player player) {
            player.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
        }
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_FALL)) {
            this.fallDistance = 0.0F;
            return false;
        }

        // Projektil-Treffer: teleportiere hinter den Schützen und negiere den Schaden
        // (der Schütze hat versucht aus Distanz zu kämpfen — die Milbe schließt die Lücke).
        if (!this.level().isClientSide() && source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)) {
            net.minecraft.world.entity.Entity shooter = source.getEntity();
            if (shooter != null && shooter != this) {
                net.minecraft.world.phys.Vec3 behind = shooter.position()
                        .subtract(shooter.getLookAngle().scale(1.4))
                        .add(0, 0, 0);
                spawnTeleportBurst(this.getX(), this.getY(), this.getZ());
                this.teleportTo(behind.x, shooter.getY(), behind.z);
                spawnTeleportBurst(this.getX(), this.getY(), this.getZ());
                // Setze den Schützen als Ziel damit die Milbe sofort angreift.
                if (shooter instanceof net.minecraft.world.entity.LivingEntity le) {
                    this.setTarget(le);
                }
                return false; // Schaden abgewehrt (dodge)
            }
        }

        // Restliche Treffer: 15% Chance auf zufälligen Kurzteleport (existierendes Verhalten).
        if (!this.level().isClientSide() && this.getRandom().nextFloat() < 0.15F) {
            spawnTeleportBurst(this.getX(), this.getY(), this.getZ());
            this.randomTeleport(this.getX() + (this.getRandom().nextDouble() - 0.5) * 6,
                    this.getY(),
                    this.getZ() + (this.getRandom().nextDouble() - 0.5) * 6,
                    false);
            spawnTeleportBurst(this.getX(), this.getY(), this.getZ());
        }
        return super.hurt(source, amount);
    }

    @Override
    public void tick() {
        tryRescueFromVoid();
        super.tick();
        tryRescueFromVoid();
    }

    @Override
    public void checkBelowWorld() {
        if (tryRescueFromVoid()) return;
        super.checkBelowWorld();
    }

    @Override
    protected void onBelowWorld() {
        if (tryRescueFromVoid()) return;
        super.onBelowWorld();
    }

    private boolean tryRescueFromVoid() {
        if (!(this.level() instanceof net.minecraft.server.level.ServerLevel)) return false;
        if (!this.level().dimension().equals(net.minecraft.world.level.Level.END)) return false;
        if (this.getY() >= VOID_RESCUE_Y) {
            this.loggedVoidRescueFailure = false;
            return false;
        }

        net.minecraft.core.BlockPos landing = findNearestSafeLanding();
        if (landing == null) {
            if (!this.loggedVoidRescueFailure) {
                net.mysith.MySithMod.LOGGER.warn("[SoulEndermite] Void rescue failed at ({}, {}, {}): no safe block within {} blocks",
                        this.getX(), this.getY(), this.getZ(), VOID_RESCUE_RADIUS);
                this.loggedVoidRescueFailure = true;
            }
            return false;
        }

        this.fallDistance = 0.0F;
        this.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        this.loggedVoidRescueFailure = false;
        spawnTeleportBurst(this.getX(), this.getY(), this.getZ());
        net.mysith.MySithMod.LOGGER.debug("[SoulEndermite] Void rescue from ({}, {}, {}) to ({}, {}, {})",
                (int) this.getX(), (int) this.getY(), (int) this.getZ(),
                landing.getX(), landing.getY() + 1, landing.getZ());
        this.teleportToWithTicket(landing.getX() + 0.5, landing.getY() + 1.0, landing.getZ() + 0.5);
        spawnTeleportBurst(this.getX(), this.getY(), this.getZ());
        return true;
    }

    private net.minecraft.core.BlockPos findNearestSafeLanding() {
        Level level = this.level();
        net.minecraft.core.BlockPos origin = this.blockPosition();
        net.minecraft.core.BlockPos best = null;
        double bestDistSqr = Double.MAX_VALUE;
        net.minecraft.core.BlockPos fallback = null;
        double fallbackDistSqr = Double.MAX_VALUE;

        for (int radius = 0; radius <= VOID_RESCUE_RADIUS; radius += 8) {
            for (int dx = -radius; dx <= radius; dx += 4) {
                for (int dz = -radius; dz <= radius; dz += 4) {
                    if (radius > 0 && Math.abs(dx) < radius && Math.abs(dz) < radius) continue;

                    net.minecraft.core.BlockPos sample = level.getHeightmapPos(
                            Heightmap.Types.MOTION_BLOCKING,
                            new net.minecraft.core.BlockPos(origin.getX() + dx, 0, origin.getZ() + dz));
                    net.minecraft.core.BlockPos ground = sample.below();

                    if (!isValidLanding(level, ground)) continue;

                    double distSqr = ground.distToCenterSqr(this.position());
                    if (hasStableFooting(level, ground)) {
                        if (distSqr < bestDistSqr) {
                            best = ground;
                            bestDistSqr = distSqr;
                        }
                    } else if (distSqr < fallbackDistSqr) {
                        fallback = ground;
                        fallbackDistSqr = distSqr;
                    }
                }
            }
        }

        return best != null ? best : fallback;
    }

    private boolean hasStableFooting(Level level, net.minecraft.core.BlockPos ground) {
        return isFullCollision(level, ground.north())
                && isFullCollision(level, ground.south())
                && isFullCollision(level, ground.east())
                && isFullCollision(level, ground.west());
    }

    private boolean isFullCollision(Level level, net.minecraft.core.BlockPos pos) {
        return level.getBlockState(pos).isCollisionShapeFullBlock(level, pos);
    }

    private boolean isValidLanding(Level level, net.minecraft.core.BlockPos ground) {
        if (ground.getY() <= (int) VOID_RESCUE_Y) return false;

        BlockState groundState = level.getBlockState(ground);
        if (!groundState.isCollisionShapeFullBlock(level, ground)) return false;
        if (groundState.is(Blocks.BEDROCK)) return false;

        net.minecraft.core.BlockPos above = ground.above();
        net.minecraft.core.BlockPos above2 = ground.above(2);
        return !level.getBlockState(above).isCollisionShapeFullBlock(level, above)
                && !level.getBlockState(above2).isCollisionShapeFullBlock(level, above2);
    }

    private void spawnTeleportBurst(double x, double y, double z) {
        if (!(this.level() instanceof net.minecraft.server.level.ServerLevel sl)) return;
        sl.sendParticles(CRIMSON_DUST, x, y + 0.3, z, 20, 0.4, 0.4, 0.4, 0.0);
        sl.sendParticles(DARK_DUST, x, y + 0.3, z, 10, 0.3, 0.3, 0.3, 0.0);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.level().isClientSide()) {
            if (this.random.nextFloat() < 0.5F) {
                this.level().addParticle(CRIMSON_DUST,
                        this.getX() + (this.random.nextDouble() - 0.5) * 0.3,
                        this.getY() - 0.05,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 0.3,
                        -this.getDeltaMovement().x * 0.5,
                        0.02,
                        -this.getDeltaMovement().z * 0.5);
            }
            if (this.random.nextFloat() < 0.2F) {
                this.level().addParticle(DARK_DUST,
                        this.getX() + (this.random.nextDouble() - 0.5) * 0.4,
                        this.getY() + 0.05,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 0.4,
                    0, 0.0, 0);
            }
        } else {
            tickActiveVoidRift();
        }
    }

    private void tickActiveVoidRift() {
        if (this.activeTeleportCooldown > 0) {
            this.activeTeleportCooldown--;
            return;
        }

        net.minecraft.world.entity.LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || this.distanceToSqr(target) < 4.0D * 4.0D || this.distanceToSqr(target) > 16.0D * 16.0D) {
            this.activeTeleportCooldown = 45 + this.random.nextInt(35);
            return;
        }

        net.minecraft.world.phys.Vec3 side = target.position().subtract(this.position()).normalize();
        if (side.lengthSqr() < 0.01D) {
            side = new net.minecraft.world.phys.Vec3(1.0D, 0.0D, 0.0D);
        }
        net.minecraft.world.phys.Vec3 offset = side.scale(2.0D + this.random.nextDouble() * 2.0D)
                .yRot((this.random.nextFloat() - 0.5F) * 1.6F);
        spawnTeleportBurst(this.getX(), this.getY(), this.getZ());
        if (this.randomTeleport(this.getX() + offset.x, this.getY(), this.getZ() + offset.z, true)) {
            spawnTeleportBurst(this.getX(), this.getY(), this.getZ());
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 35, 0));
            this.activeTeleportCooldown = 75 + this.random.nextInt(55);
        } else {
            this.activeTeleportCooldown = 35;
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        // Base 20% + 10% pro Looting-Level → Looting III = 50%
        float chance = 0.20F + (looting * 0.10F);
        if (this.getRandom().nextFloat() < chance) {
            this.spawnAtLocation(new ItemStack(ModItems.SOUL_FRAGMENT.get()));
        }
    }

    // ===== Sounds: Custom screech (stumm bis .ogg vorhanden), Vanilla-Fallback =====
    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.SOUL_ENDERMITE_SCREECH.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDERMITE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDERMITE_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.6F + this.random.nextFloat() * 0.2F;
    }

    // ===== GeckoLib =====
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 2, state -> {
            state.setAnimation(WALK_ANIM);
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
