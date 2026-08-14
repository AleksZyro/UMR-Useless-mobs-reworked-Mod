package net.mysith.silverfish;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class CorruptedSilverfishEntity extends Silverfish implements GeoEntity {
    private static final DustParticleOptions CORRUPTION_DUST =
            new DustParticleOptions(new Vector3f(0.45F, 0.10F, 0.75F), 1.0F);
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("animation.corrupted_silverfish.idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("animation.corrupted_silverfish.walk");
    private static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("animation.corrupted_silverfish.attack");
    private static final RawAnimation HURT_ANIM = RawAnimation.begin().thenPlay("animation.corrupted_silverfish.hurt");
    private static final RawAnimation DEATH_ANIM = RawAnimation.begin().thenPlay("animation.corrupted_silverfish.death");
    private boolean panicBurstUsed = false;
    private int swarmCallCooldown = 80;
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public CorruptedSilverfishEntity(EntityType<? extends Silverfish> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 14.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @SuppressWarnings("unchecked")
    public static boolean checkCorruptedSilverfishSpawnRules(EntityType<CorruptedSilverfishEntity> entityType, LevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (!(level instanceof ServerLevelAccessor serverLevel)) {
            return false;
        }

        return Monster.checkMonsterSpawnRules((EntityType<? extends Monster>) (EntityType<?>) entityType, serverLevel, spawnType, pos, random);
    }

    @Override
    @SuppressWarnings("deprecation")
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, SpawnGroupData spawnGroupData, net.minecraft.nbt.CompoundTag dataTag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData, dataTag);
        this.setPersistenceRequired();
        return data;
    }

    @Override
    public Component getTypeName() {
        return Component.translatable("entity.usless_mobs.corrupted_silverfish");
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt && !this.level().isClientSide) {
            this.triggerAnim("action", "attack");
        }
        if (hurt && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 70, 0));
            if (this.level().getMaxLocalRawBrightness(this.blockPosition()) <= 4) {
                living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 45, 0));
            }
        }
        return hurt;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.level().isClientSide) {
            return;
        }

        if (this.tickCount % 16 == 0 && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(CORRUPTION_DUST, this.getX(), this.getY(0.45D), this.getZ(),
                    4, 0.16D, 0.10D, 0.16D, 0.0D);
        }
        if (this.swarmCallCooldown > 0) {
            this.swarmCallCooldown--;
        }

        int light = this.level().getMaxLocalRawBrightness(this.blockPosition());
        if (light >= 12 && this.tickCount % 60 == 0) {
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0));
            this.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
        }

        if (!this.panicBurstUsed && this.getHealth() <= this.getMaxHealth() * 0.35F) {
            this.panicBurstUsed = true;
            this.panicBurst();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide && !this.isDeadOrDying()) {
            this.triggerAnim("action", "hurt");
        }
        if (hurt && !this.level().isClientSide && this.swarmCallCooldown <= 0 && source.getEntity() instanceof LivingEntity attacker) {
            callSwarm(attacker);
            this.swarmCallCooldown = 150 + this.random.nextInt(60);
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        boolean wasDead = this.dead;
        super.die(source);
        if (!this.level().isClientSide && !wasDead && this.dead) {
            this.triggerAnim("action", "death");
        }
    }

    private void panicBurst() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        serverLevel.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY(0.5D), this.getZ(),
                18, 0.45D, 0.25D, 0.45D, 0.03D);
        this.playSound(SoundEvents.SILVERFISH_HURT, 1.2F, 0.55F);

        callSwarm(this.getTarget());
    }

    private void callSwarm(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        int helpers = 1 + (this.random.nextFloat() < 0.45F ? 1 : 0);
        for (int index = 0; index < helpers; index++) {
            Silverfish helper = EntityType.SILVERFISH.create(this.level());
            if (helper == null) {
                continue;
            }

            helper.moveTo(this.getX() + (this.random.nextDouble() - 0.5D), this.getY(), this.getZ() + (this.random.nextDouble() - 0.5D),
                    this.random.nextFloat() * 360.0F, 0.0F);
            if (target != null) {
                helper.setTarget(target);
            }
            this.level().addFreshEntity(helper);
        }
        serverLevel.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY(0.35D), this.getZ(),
                10 + helpers * 6, 0.35D, 0.18D, 0.35D, 0.03D);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource damageSource, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(damageSource, looting, recentlyHit);

        int safeLooting = Math.max(0, looting);
        int chitin = 1 + this.random.nextInt(2) + (safeLooting > 0 && this.random.nextFloat() < Math.min(0.85F, 0.35F * safeLooting) ? 1 : 0);
        this.spawnAtLocation(new ItemStack(com.Momik.usless_mobs.registry.ModItems.CORRUPTED_CHITIN.get(), chitin));

        if (this.random.nextFloat() < cappedDropChance(0.58F, 0.08F, safeLooting, 0.90F)) {
            this.spawnAtLocation(new ItemStack(com.Momik.usless_mobs.registry.ModItems.SILVER_DUST.get(), 1 + this.random.nextInt(2)));
        }

        if (this.random.nextFloat() < cappedDropChance(0.30F, 0.05F, safeLooting, 0.70F)) {
            this.spawnAtLocation(new ItemStack(com.Momik.usless_mobs.registry.ModItems.INFESTED_STONE_FRAGMENT.get()));
        }

        if (this.random.nextFloat() < cappedDropChance(0.10F, 0.03F, safeLooting, 0.35F)) {
            this.spawnAtLocation(new ItemStack(this.random.nextBoolean() ? Items.RAW_IRON : Items.RAW_COPPER));
        }

        float shardChance = cappedDropChance(0.05F, 0.01F, safeLooting, 0.12F);
        if (this.random.nextFloat() < shardChance) {
            this.spawnAtLocation(new ItemStack(com.Momik.usless_mobs.registry.ModItems.CORRUPTED_SHARD.get()));
        }
    }

    private static float cappedDropChance(float baseChance, float perLootingLevel, int looting, float cap) {
        return Math.min(cap, baseChance + perLootingLevel * looting);
    }

    @Override
    public boolean isSensitiveToWater() {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 3,
                state -> state.setAndContinue(state.isMoving() ? WALK_ANIM : IDLE_ANIM)));
        controllers.add(new AnimationController<>(this, "action", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK_ANIM)
                .triggerableAnim("hurt", HURT_ANIM)
                .triggerableAnim("death", DEATH_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
