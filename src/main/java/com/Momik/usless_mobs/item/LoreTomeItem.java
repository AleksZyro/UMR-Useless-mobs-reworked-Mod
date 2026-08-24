package com.Momik.usless_mobs.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Die Chronik aller Pfade: ein Buch, das die gesamte Lore des Mods buendelt -
 * die sieben alten Lore-Buecher, die neuen Kapitel (King Slime, Ozean-Hof,
 * lebendige Wildnis, Altar-Wahl) und den kompletten Kodex.
 * Gleiches Muster wie {@link net.mysith.item.SoulCodexItem}.
 */
public class LoreTomeItem extends Item {

    public LoreTomeItem(Properties properties) {
        super(properties);
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return Rarity.EPIC;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            com.Momik.usless_mobs.client.LoreTomeClient.openBook();
        }
        float openPitch = 0.8F + level.getRandom().nextFloat() * 0.1F;
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, openPitch);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.3F, 0.5F);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.usless_mobs.lore_tome.tooltip1")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.usless_mobs.lore_tome.tooltip2")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
