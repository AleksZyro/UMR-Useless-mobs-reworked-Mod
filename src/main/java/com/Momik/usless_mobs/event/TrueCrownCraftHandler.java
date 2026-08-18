package com.Momik.usless_mobs.event;

import com.Momik.usless_mobs.Usless_mobs;
import com.Momik.usless_mobs.world.TrueCrownTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Usless_mobs.MODID)
public class TrueCrownCraftHandler {
    private TrueCrownCraftHandler() {
    }

    @SubscribeEvent
    public static void onCraft(PlayerEvent.ItemCraftedEvent event) {
        ItemStack crafted = event.getCrafting();
        if (!crafted.is(com.Momik.usless_mobs.registry.ModItems.TRUE_CROWN.get())) {
            return;
        }
        if (isBalanceCrownConversion(crafted, event.getInventory())) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        TrueCrownTracker tracker = TrueCrownTracker.get(server);
        int existingCrownCount = countTrueCrowns(server);
        if (tracker.claimedFinalPathCount() >= 3 || existingCrownCount >= 3) {
            crafted.shrink(crafted.getCount());
            player.displayClientMessage(Component.translatable("item.usless_mobs.true_crown.only_one")
                    .withStyle(ChatFormatting.DARK_RED), false);
            return;
        }

        tracker.markCrafted(player.getUUID(), player.getGameProfile().getName());
        spawnAllegianceAltars(player);
    }

    static boolean isBalanceCrownConversion(ItemStack crafted, Container inventory) {
        if (!crafted.is(com.Momik.usless_mobs.registry.ModItems.TRUE_CROWN.get())) {
            return false;
        }
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(com.Momik.usless_mobs.registry.ModItems.ROYAL_BALANCE_CROWN.get())) {
                return true;
            }
        }
        return false;
    }

    private static void spawnAllegianceAltars(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();
        placeAltar(level, center.offset(3, 0, 0), com.Momik.usless_mobs.registry.ModBlocks.VOID_ALTAR.get());
        placeAltar(level, center.offset(-3, 0, 0), com.Momik.usless_mobs.registry.ModBlocks.CELESTIAL_ALTAR.get());
        placeAltar(level, center.offset(0, 0, 3), com.Momik.usless_mobs.registry.ModBlocks.LIVING_ALTAR.get());
        level.playSound(null, center, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.PLAYERS, 1.0F, 0.7F);
        level.sendParticles(ParticleTypes.WAX_ON, player.getX(), player.getY(1.0D), player.getZ(),
                45, 1.3D, 0.75D, 1.3D, 0.04D);
        player.displayClientMessage(Component.translatable("item.usless_mobs.true_crown.altars_spawned")
                .withStyle(ChatFormatting.GOLD), false);
        player.displayClientMessage(Component.translatable("item.usless_mobs.true_crown.codex_unlocked")
                .withStyle(ChatFormatting.DARK_PURPLE), false);
    }

    private static void placeAltar(ServerLevel level, BlockPos desired, Block altar) {
        BlockPos target = findAltarSpot(level, desired);
        if (target == null) {
            return;
        }
        level.setBlock(target, altar.defaultBlockState(), 3);
        level.sendParticles(ParticleTypes.ENCHANT, target.getX() + 0.5D, target.getY() + 1.0D, target.getZ() + 0.5D,
                24, 0.45D, 0.35D, 0.45D, 0.08D);
    }

    private static BlockPos findAltarSpot(ServerLevel level, BlockPos desired) {
        for (int dy = 0; dy <= 3; dy++) {
            BlockPos candidate = desired.above(dy);
            if (canPlaceAltar(level, candidate)) {
                return candidate;
            }
        }
        for (int dy = -1; dy >= -3; dy--) {
            BlockPos candidate = desired.above(dy);
            if (canPlaceAltar(level, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean canPlaceAltar(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir() && !level.getBlockState(pos.below()).isAir();
    }

    private static int countTrueCrowns(MinecraftServer server) {
        int total = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            total += countCrowns(player.getInventory());
            total += countCrowns(player.getEnderChestInventory());
        }
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof ItemEntity itemEntity && isCrown(itemEntity.getItem())) {
                    total += itemEntity.getItem().getCount();
                }
            }
        }
        return total;
    }

    private static int countCrowns(Container container) {
        int total = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (isCrown(stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static boolean isCrown(ItemStack stack) {
        return stack.is(com.Momik.usless_mobs.registry.ModItems.TRUE_CROWN.get())
                || stack.is(com.Momik.usless_mobs.registry.ModItems.ROYAL_BALANCE_CROWN.get());
    }
}
