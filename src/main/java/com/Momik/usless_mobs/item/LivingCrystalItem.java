package com.Momik.usless_mobs.item;

import com.Momik.usless_mobs.event.LivingRaidHandler;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class LivingCrystalItem extends Item {

    public LivingCrystalItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null) {
            return InteractionResult.PASS;
        }

        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        if (!state.is(Blocks.MOSS_BLOCK) && !state.is(Blocks.ROOTED_DIRT)) {
            return InteractionResult.PASS;
        }

        if (context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (context.getLevel() instanceof ServerLevel serverLevel && context.getPlayer() instanceof ServerPlayer player) {
            if (LivingRaidHandler.startRaid(serverLevel, player, context.getClickedPos().above())) {
                if (!player.getAbilities().instabuild) {
                    context.getItemInHand().shrink(1);
                }
                return InteractionResult.CONSUME;
            }
            player.displayClientMessage(Component.translatable("item.usless_mobs.living_crystal.raid_active")
                    .withStyle(ChatFormatting.AQUA), true);
        }
        return InteractionResult.FAIL;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.usless_mobs.living_crystal.tooltip.raid").withStyle(ChatFormatting.DARK_GREEN));
        tooltip.add(Component.translatable("item.usless_mobs.living_crystal.tooltip.reward").withStyle(ChatFormatting.AQUA));
    }
}
