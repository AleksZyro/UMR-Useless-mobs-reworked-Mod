package com.Momik.usless_mobs.item;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;

public class GlowbaitFishingRodItem extends FishingRodItem {

    public GlowbaitFishingRodItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!selected || level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        if (isNearWater(level, player.blockPosition())) {
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, 60, 0, true, false, true));
            if (level instanceof ServerLevel serverLevel && player.tickCount % 30 == 0) {
                serverLevel.sendParticles(ParticleTypes.GLOW,
                        player.getX(), player.getY(0.7D), player.getZ(),
                        6, 0.35D, 0.25D, 0.35D, 0.01D);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.usless_mobs.glowbait_fishing_rod.tooltip"));
    }

    private static boolean isNearWater(Level level, BlockPos origin) {
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-3, -2, -3), origin.offset(3, 2, 3))) {
            if (level.getFluidState(pos).is(Fluids.WATER)) {
                return true;
            }
        }
        return false;
    }
}
