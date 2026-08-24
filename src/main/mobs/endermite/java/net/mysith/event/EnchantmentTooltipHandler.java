package net.mysith.event;

import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.mysith.MySithMod;

@Mod.EventBusSubscriber(modid = com.Momik.usless_mobs.Usless_mobs.MODID)
public class EnchantmentTooltipHandler {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(stack);
        if (enchants.isEmpty()) return;

        List<Component> tooltip = event.getToolTip();

        for (Map.Entry<Enchantment, Integer> e : enchants.entrySet()) {
            ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(e.getKey());
            if (id == null) continue;
            if (!id.getNamespace().equals(MySithMod.MODID)) continue;

            String descKey = "enchantment." + id.getNamespace() + "." + id.getPath() + ".desc";
            tooltip.add(
                    Component.literal("  » ").withStyle(ChatFormatting.DARK_GRAY)
                            .append(Component.translatable(descKey)
                                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC))
            );
        }
    }
}
