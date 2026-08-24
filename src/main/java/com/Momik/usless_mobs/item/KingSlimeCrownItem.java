package com.Momik.usless_mobs.item;

import com.Momik.usless_mobs.effect.KingSlimeCrownEffects;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;

public class KingSlimeCrownItem extends ArmorItem {

    public KingSlimeCrownItem(Properties properties) {
        super(ArmorMaterials.GOLD, Type.HELMET, properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!(entity instanceof Player player) || player.getItemBySlot(getType().getSlot()) != stack) {
            return;
        }
        KingSlimeCrownEffects.apply(player);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.usless_mobs.king_slime_krone.tooltip.helmet").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.usless_mobs.king_slime_krone.tooltip.power").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("item.usless_mobs.king_slime_krone.tooltip.guard").withStyle(ChatFormatting.DARK_PURPLE));
        if (ModList.get().isLoaded("curios")) {
            tooltip.add(Component.translatable("item.usless_mobs.king_slime_krone.tooltip.curio").withStyle(ChatFormatting.AQUA));
        }
    }
}
