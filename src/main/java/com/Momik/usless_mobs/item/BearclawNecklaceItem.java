package com.Momik.usless_mobs.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class BearclawNecklaceItem extends Item {

    public BearclawNecklaceItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.usless_mobs.bearclaw_necklace.tooltip.slot"));
        tooltip.add(Component.translatable("item.usless_mobs.bearclaw_necklace.tooltip.power"));
    }
}
