package com.Momik.usless_mobs.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class PotionOfLifeItem extends Item {
    public PotionOfLifeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            player.heal(8.0F);
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 12 * 20, 1));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 45 * 20, 1));
            ((ServerLevel) level).sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,
                    player.getX(), player.getY(0.8D), player.getZ(),
                    12, 0.35D, 0.35D, 0.35D, 0.03D);
            level.playSound(null, player.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 0.8F, 1.35F);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.usless_mobs.potion_of_life.tooltip.use").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("item.usless_mobs.potion_of_life.tooltip.craft").withStyle(ChatFormatting.GREEN));
    }
}
