package net.mysith.item;

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

public class SoulCodexItem extends Item {

    public SoulCodexItem(Properties properties) {
        super(properties);
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return Rarity.RARE;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            // Öffnet das Vanilla-Buch-GUI mit unseren Seiten (nur Client-Side)
            net.mysith.client.SoulCodexClient.openBook();
        }
        // Custom Codex-Sounds (stumm bis .ogg vorhanden) + Vanilla-Fallback mit Pitch-Layern
        float openPitch = 0.85F + level.getRandom().nextFloat() * 0.1F;
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                net.mysith.registry.ModSounds.CODEX_OPEN.get(), SoundSource.PLAYERS, 1.0F, openPitch);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                net.mysith.registry.ModSounds.CODEX_PAGE.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, openPitch);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.3F, 0.5F);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.usless_mobs.soul_codex.tooltip1")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.usless_mobs.soul_codex.tooltip2")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
