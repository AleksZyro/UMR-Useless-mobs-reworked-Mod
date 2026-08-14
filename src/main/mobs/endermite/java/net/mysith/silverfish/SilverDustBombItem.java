package net.mysith.silverfish;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SilverDustBombItem extends Item {
    public SilverDustBombItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        CorruptedSilverfishTracker.SearchResult result = CorruptedSilverfishTracker.findNearest(serverLevel, player, 12);
        if (result == null) {
            player.displayClientMessage(Component.translatable("item.usless_mobs.silver_dust_bomb.none"), true);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 0.8F, 0.7F);
        } else if (result.hidden()) {
            CorruptedSilverfishTracker.spawnFromHost(serverLevel, result.pos(), player);
            player.displayClientMessage(Component.translatable("item.usless_mobs.silver_dust_bomb.flushed"), true);
        } else {
            CorruptedSilverfishTracker.reveal(serverLevel, result.pos(), true);
            player.displayClientMessage(Component.translatable("item.usless_mobs.silver_dust_bomb.revealed"), true);
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        player.getCooldowns().addCooldown(this, 40);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }
}
