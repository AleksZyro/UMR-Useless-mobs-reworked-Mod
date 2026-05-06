package com.Momik.usless_mobs.item;

import com.Momik.usless_mobs.Usless_mobs;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class NetheriteSlimeCoreItem extends Item {

    private static final int COOLDOWN_TICKS = 120;

    public NetheriteSlimeCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        Vec3 lookDirection = player.getLookAngle();
        boolean verticalBurst = player.isShiftKeyDown();
        double horizontalBoost = verticalBurst ? 0.55D : 1.7D;
        double verticalBoost = verticalBurst ? 1.35D : 0.82D;

        player.push(lookDirection.x * horizontalBoost, verticalBoost, lookDirection.z * horizontalBoost);
        player.hasImpulse = true;
        player.fallDistance = 0.0F;
        player.addEffect(new MobEffectInstance(Usless_mobs.ELASTICITY.get(), 360, 1));
        player.addEffect(new MobEffectInstance(Usless_mobs.GOLDEN_FLOW.get(), 240, 0));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 140, 0));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0));
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);

        level.playSound(null, player.blockPosition(), SoundEvents.SLIME_BLOCK_PLACE, SoundSource.PLAYERS, 1.15F, verticalBurst ? 0.75F : 0.95F);

        if (level instanceof ServerLevel serverLevel) {
            this.triggerShockwave(serverLevel, player, verticalBurst);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private void triggerShockwave(ServerLevel level, Player player, boolean verticalBurst) {
        double radius = verticalBurst ? 5.0D : 4.0D;
        AABB area = player.getBoundingBox().inflate(radius);

        for (LivingEntity livingEntity : level.getEntitiesOfClass(LivingEntity.class, area, target -> target != player && target.isAlive())) {
            Vec3 pushDirection = livingEntity.position().subtract(player.position());

            if (pushDirection.lengthSqr() < 1.0E-4D) {
                pushDirection = new Vec3(0.0D, 0.0D, 1.0D);
            }

            pushDirection = pushDirection.normalize();
            double horizontalForce = verticalBurst ? 0.55D : 0.85D;
            double verticalForce = verticalBurst ? 0.8D : 0.45D;

            livingEntity.push(pushDirection.x * horizontalForce, verticalForce, pushDirection.z * horizontalForce);
            livingEntity.hurt(player.damageSources().playerAttack(player), verticalBurst ? 8.0F : 6.0F);
            livingEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        }

        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Usless_mobs.GOLDENER_SCHLEIMBALL.get())),
                player.getX(), player.getY(0.8D), player.getZ(), verticalBurst ? 48 : 32, 1.2D, 0.5D, 1.2D, 0.08D);
        level.sendParticles(ParticleTypes.CRIT,
                player.getX(), player.getY(0.8D), player.getZ(), verticalBurst ? 30 : 18, 0.8D, 0.35D, 0.8D, 0.08D);
    }
}
