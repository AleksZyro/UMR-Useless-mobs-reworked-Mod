package com.Momik.usless_mobs.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class AwakenedBearclawNecklaceItem extends BearclawNecklaceItem {
    public AwakenedBearclawNecklaceItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.usless_mobs.awakened_bearclaw_necklace.tooltip.slot").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.usless_mobs.awakened_bearclaw_necklace.tooltip.berserk").withStyle(ChatFormatting.RED));
    }
}
