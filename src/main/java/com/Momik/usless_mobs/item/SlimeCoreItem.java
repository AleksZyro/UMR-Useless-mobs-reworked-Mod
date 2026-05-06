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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class SlimeCoreItem extends Item {

    private static final int COOLDOWN_TICKS = 160;

    public SlimeCoreItem(Properties properties) {
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
        boolean verticalJump = player.isShiftKeyDown();
        double horizontalBoost = verticalJump ? 0.35D : 1.15D;
        double verticalBoost = verticalJump ? 0.95D : 0.55D;

        player.push(lookDirection.x * horizontalBoost, verticalBoost, lookDirection.z * horizontalBoost);
        player.hasImpulse = true;
        player.fallDistance = 0.0F;
        player.addEffect(new MobEffectInstance(Usless_mobs.ELASTICITY.get(), 220, verticalJump ? 1 : 0));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, 180, 1));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0));
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);

        level.playSound(null, player.blockPosition(), SoundEvents.SLIME_BLOCK_PLACE, SoundSource.PLAYERS, 1.0F, verticalJump ? 0.9F : 1.1F);

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Usless_mobs.BLAUER_SCHLEIMBALL.get())),
                    player.getX(), player.getY(0.8D), player.getZ(), 16, 0.45D, 0.25D, 0.45D, 0.04D);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
