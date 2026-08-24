package com.Momik.usless_mobs.effect;

import com.Momik.usless_mobs.entity.KingSlimeEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class KingSlimeCrownEffects {

    // Per-tick dedup marker so wearing the crown in both helmet AND curio slot
    // doesn't apply the heal twice or stack buffs.
    private static final String LAST_TICK_KEY = "UslessMobs_KingCrown_LastTick";
    private static final String NEXT_GUARD_TICK_KEY = "UslessMobs_KingCrown_NextGuardTick";

    /**
     * Configurable tier — controls effect strength so the netherite-upgrade crown
     * can reuse the same apply() logic with stronger values.
     */
    public static final class Tier {
        public final int amp_resist;
        public final int amp_speed;
        public final int amp_jump;
        public final int healInterval;
        public final float healAmount;
        public final float guardThreshold;
        public final int guardCooldown;
        public final double auraRadius;
        public final double magnetRadius;
        public final double magnetPullStrength;
        public final boolean lavaImmunity;
        public final boolean witherImmunity;

        public Tier(int amp_resist, int amp_speed, int amp_jump,
                    int healInterval, float healAmount,
                    float guardThreshold, int guardCooldown,
                    double auraRadius, double magnetRadius, double magnetPullStrength,
                    boolean lavaImmunity, boolean witherImmunity) {
            this.amp_resist = amp_resist;
            this.amp_speed = amp_speed;
            this.amp_jump = amp_jump;
            this.healInterval = healInterval;
            this.healAmount = healAmount;
            this.guardThreshold = guardThreshold;
            this.guardCooldown = guardCooldown;
            this.auraRadius = auraRadius;
            this.magnetRadius = magnetRadius;
            this.magnetPullStrength = magnetPullStrength;
            this.lavaImmunity = lavaImmunity;
            this.witherImmunity = witherImmunity;
        }
    }

    public static final Tier STANDARD = new Tier(
            -1,  // no permanent Resistance
            0,   // Speed I
            0,   // Jump I
            120, // heal every 6s, only when wounded
            0.5F,
            0.30F,  // guard at 30% HP
            900,    // 45s cooldown
            4.0D,
            3.5D, 0.10D,
            false, false
    );

    public static final Tier NETHERITE = new Tier(
            0,   // Resistance I
            0,   // Speed I
            1,   // Jump II
            100, // heal every 5s, only when wounded
            0.75F,
            0.40F,  // guard at 40% HP
            700,    // 35s cooldown
            5.5D,
            5.5D, 0.14D,
            true, true   // fire safety + periodic wither cleanse
    );

    private KingSlimeCrownEffects() {}

    public static void apply(Player player) {
        apply(player, STANDARD);
    }

    private static void applyFiltered(Player player, net.minecraft.world.effect.MobEffect effect, int duration, int amplifier) {
        SlimePowerToggle.applyFiltered(player, effect, duration, amplifier);
    }

    public static boolean isCrownActive(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(LAST_TICK_KEY)) {
            return false;
        }
        int lastAppliedTick = data.getInt(LAST_TICK_KEY);
        return lastAppliedTick == player.tickCount || lastAppliedTick == player.tickCount - 1;
    }

    public static void apply(Player player, Tier tier) {
        if (SlimePowerToggle.areEffectsDisabled(player)) {
            return;
        }

        CompoundTag data = player.getPersistentData();
        int now = player.tickCount;
        if (data.contains(LAST_TICK_KEY) && data.getInt(LAST_TICK_KEY) == now) {
            return;
        }
        data.putInt(LAST_TICK_KEY, now);

        player.fallDistance = Math.min(player.fallDistance, tier == NETHERITE ? 2.0F : 3.0F);
        if (tier.lavaImmunity && player.isOnFire()) {
            player.clearFire();
        }

        applyFiltered(player, com.Momik.usless_mobs.registry.ModEffects.GOLDEN_FLOW.get(), 60, 0);
        if (tier.amp_jump >= 0) {
            applyFiltered(player, MobEffects.JUMP, 60, tier.amp_jump);
        }
        if (tier.amp_resist >= 0) {
            applyFiltered(player, MobEffects.DAMAGE_RESISTANCE, 60, tier.amp_resist);
        }
        if (tier.amp_speed >= 0) {
            applyFiltered(player, MobEffects.MOVEMENT_SPEED, 60, tier.amp_speed);
        }

        // Netherite tier: fire safety is reactive; wither cleanse has a short rhythm instead of full immunity.
        if (tier.lavaImmunity) {
            if (player.isInLava() || player.isOnFire()) {
                applyFiltered(player, MobEffects.FIRE_RESISTANCE, 100, 0);
            }
        }
        if (tier.witherImmunity && now % 80 == 0 && player.hasEffect(MobEffects.WITHER)) {
            player.removeEffect(MobEffects.WITHER);
        }

        if (!player.onGround() && player.isShiftKeyDown() && player.getDeltaMovement().y < -0.08D) {
            Vec3 movement = player.getDeltaMovement();
            player.setDeltaMovement(movement.x, -0.12D, movement.z);
            player.fallDistance = 0.0F;
            player.hasImpulse = true;
        }

        if (now % tier.healInterval == 0 && player.getHealth() < player.getMaxHealth() * 0.75F) {
            player.heal(tier.healAmount);
        }

        if (player.getHealth() <= player.getMaxHealth() * tier.guardThreshold && data.getInt(NEXT_GUARD_TICK_KEY) <= now) {
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 220, tier == NETHERITE ? 1 : 0, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, tier == NETHERITE ? 110 : 80, 0, true, false, true));
            data.putInt(NEXT_GUARD_TICK_KEY, now + tier.guardCooldown);
        }

        // Royal comfort: trims exhaustion occasionally, instead of deleting all hunger cost every tick.
        if (now % 40 == 0 && player.getFoodData().getExhaustionLevel() > 2.0F) {
            player.getFoodData().setExhaustion(0F);
        }

        tickItemMagnetism(player, now, tier);
        tickRoyalAura(player, now, tier);
        emitCrownParticles(player, now);
    }

    private static void tickItemMagnetism(Player player, int tick, Tier tier) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        // Run every 2 ticks for perf; items still feel snappy.
        if (tick % 2 != 0) {
            return;
        }

        AABB pullArea = player.getBoundingBox().inflate(tier.magnetRadius);
        for (ItemEntity item : serverLevel.getEntitiesOfClass(ItemEntity.class, pullArea,
                ie -> ie.isAlive() && !ie.hasPickUpDelay())) {
            Vec3 toPlayer = player.position().add(0.0D, 0.5D, 0.0D).subtract(item.position());
            double dist = toPlayer.length();
            if (dist < 1.2D || dist > tier.magnetRadius) {
                continue;
            }
            Vec3 pull = toPlayer.normalize().scale(tier.magnetPullStrength);
            item.setDeltaMovement(item.getDeltaMovement().add(pull));
            item.hasImpulse = true;
        }
    }

    private static void tickRoyalAura(Player player, int tick, Tier tier) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        AABB aura = player.getBoundingBox().inflate(tier.auraRadius);

        if (tick % 20 == 0) {
            for (Slime slime : serverLevel.getEntitiesOfClass(Slime.class, aura, slime -> slime.isAlive() && !(slime instanceof KingSlimeEntity))) {
                if (slime.getTarget() == player) {
                    slime.setTarget(null);
                }
            }
        }

        if (tick % 60 != 0) {
            return;
        }

        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, aura, target -> target.isAlive() && target != player)) {
            if (!(target instanceof Monster)) {
                continue;
            }

            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
            Vec3 push = target.position().subtract(player.position());
            if (push.lengthSqr() < 1.0E-4D) {
                push = new Vec3(0.0D, 0.0D, 1.0D);
            }
            push = push.normalize();
            target.push(push.x * 0.45D, 0.15D, push.z * 0.45D);
            target.hurtMarked = true;
        }

        serverLevel.sendParticles(ParticleTypes.FALLING_NECTAR,
                player.getX(), player.getY(0.8D), player.getZ(),
                18, tier.auraRadius * 0.35D, 0.45D, tier.auraRadius * 0.35D, 0.02D);
    }

    private static void emitCrownParticles(Player player, int tick) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (tick % 4 != 0) {
            return;
        }
        double rx = (player.getRandom().nextDouble() - 0.5D) * 0.6D;
        double rz = (player.getRandom().nextDouble() - 0.5D) * 0.6D;
        double y = player.getEyeY() + 0.4D + player.getRandom().nextDouble() * 0.25D;
        serverLevel.sendParticles(ParticleTypes.ENCHANT,
                player.getX() + rx, y, player.getZ() + rz,
                1, 0.0D, 0.0D, 0.0D, 0.05D);

        if (tick % 16 == 0) {
            serverLevel.sendParticles(ParticleTypes.FALLING_NECTAR,
                    player.getX() + rx, y, player.getZ() + rz,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }
}
