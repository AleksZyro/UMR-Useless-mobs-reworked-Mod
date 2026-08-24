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

public class NetheriteCrownItem extends ArmorItem {

    public NetheriteCrownItem(Properties properties) {
        // Netherite material → fire-resistant + better armor than gold base crown.
        super(ArmorMaterials.NETHERITE, Type.HELMET, properties);
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
        KingSlimeCrownEffects.apply(player, KingSlimeCrownEffects.NETHERITE);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.usless_mobs.netherite_kings_krone.tooltip.helmet").withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.translatable("item.usless_mobs.netherite_kings_krone.tooltip.power").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("item.usless_mobs.netherite_kings_krone.tooltip.guard").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("item.usless_mobs.netherite_kings_krone.tooltip.immunity").withStyle(ChatFormatting.GOLD));
        if (ModList.get().isLoaded("curios")) {
            tooltip.add(Component.translatable("item.usless_mobs.king_slime_krone.tooltip.curio").withStyle(ChatFormatting.AQUA));
        }
    }
}
