package com.Momik.usless_mobs.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeMod;

public class TentacleItem extends Item {
    public TentacleItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide && entity instanceof Player player && level.random.nextFloat() < 0.22F) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 8 * 20, 0));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 4 * 20, 1));
            if (player.canDrownInFluidType(ForgeMod.WATER_TYPE.get())) {
                player.hurt(player.damageSources().drown(), 2.0F);
            }
            ((ServerLevel) level).sendParticles(net.minecraft.core.particles.ParticleTypes.SQUID_INK,
                    player.getX(), player.getY(0.65D), player.getZ(),
                    18, 0.3D, 0.3D, 0.3D, 0.04D);
            level.playSound(null, player.blockPosition(), SoundEvents.SQUID_SQUIRT, SoundSource.PLAYERS, 0.7F, 0.75F);
        }
        return result;
    }
}
