package net.mysith.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class SoulContainerItem extends Item {

    public static final int MAX_SOULS = 50;
    private static final String SOULS_KEY = "StoredSouls";

    public SoulContainerItem(Properties properties) {
        super(properties);
    }

    public static int getSouls(ItemStack stack) {
        return stack.getOrCreateTag().getInt(SOULS_KEY);
    }

    public static void setSouls(ItemStack stack, int souls) {
        stack.getOrCreateTag().putInt(SOULS_KEY, Math.min(MAX_SOULS, souls));
    }

    public static boolean addSoul(ItemStack stack) {
        int current = getSouls(stack);
        if (current >= MAX_SOULS) return false;
        setSouls(stack, current + 1);
        return true;
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return getSouls(stack) >= MAX_SOULS ? Rarity.EPIC : Rarity.UNCOMMON;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return getSouls(stack) >= MAX_SOULS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int souls = getSouls(stack);

        if (souls < MAX_SOULS) {
            player.displayClientMessage(
                    Component.translatable("item.usless_mobs.soul_container.status", souls, MAX_SOULS)
                            .withStyle(ChatFormatting.GRAY),
                    true);
            return InteractionResultHolder.fail(stack);
        }

        // Consume + Strength III for 30 sec
        if (!level.isClientSide()) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 2));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            setSouls(stack, 0);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.6F, 1.5F);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        int souls = getSouls(stack);
        tooltip.add(Component.translatable("item.usless_mobs.soul_container.tooltip.stored", souls, MAX_SOULS)
                .withStyle(souls >= MAX_SOULS ? ChatFormatting.DARK_PURPLE : ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.usless_mobs.soul_container.tooltip.use")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.usless_mobs.soul_container.tooltip.fill")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
