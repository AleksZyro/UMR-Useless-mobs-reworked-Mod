package com.Momik.usless_mobs.item;

import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class KingSlimeTrophyItem extends BlockItem {
    private static final int COOLDOWN_TICKS = 20 * 60 * 5;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    public KingSlimeTrophyItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        player.addEffect(new MobEffectInstance(com.Momik.usless_mobs.registry.ModEffects.GOLDEN_FLOW.get(), 20 * 30, 1));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 30, 1));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 30, 1));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 45, 2));
        player.addEffect(new MobEffectInstance(MobEffects.LUCK, 20 * 60, 1));
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING,
                    player.getX(), player.getY(0.8D), player.getZ(), 32, 0.8D, 0.5D, 0.8D, 0.08D);
        }

        level.playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 0.8F);
        player.displayClientMessage(Component.translatable("item.usless_mobs.king_slime_trophy.used").withStyle(ChatFormatting.GOLD), false);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.usless_mobs.king_slime_trophy.tooltip").withStyle(ChatFormatting.DARK_PURPLE));
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return;
        }

        if (tag.contains("DefeatedBy")) {
            tooltip.add(Component.translatable("item.usless_mobs.king_slime_trophy.defeated_by", tag.getString("DefeatedBy")).withStyle(ChatFormatting.GOLD));
        }
        if (tag.contains("Difficulty")) {
            tooltip.add(Component.translatable("item.usless_mobs.king_slime_trophy.difficulty", tag.getString("Difficulty")).withStyle(ChatFormatting.YELLOW));
        }
        if (tag.contains("DefeatedAtUnix")) {
            String defeatedAt = TIME_FORMAT.format(Instant.ofEpochMilli(tag.getLong("DefeatedAtUnix")));
            tooltip.add(Component.translatable("item.usless_mobs.king_slime_trophy.defeated_at", defeatedAt).withStyle(ChatFormatting.GRAY));
        }
        if (tag.contains("Victories")) {
            tooltip.add(Component.translatable("item.usless_mobs.king_slime_trophy.victories", tag.getInt("Victories")).withStyle(ChatFormatting.AQUA));
        }
    }
}
