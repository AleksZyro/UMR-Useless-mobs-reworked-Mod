package com.Momik.usless_mobs.block;

import com.Momik.usless_mobs.allegiance.AllegiancePath;
import com.Momik.usless_mobs.allegiance.AllegianceUtil;
import com.Momik.usless_mobs.world.TrueCrownTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public class AllegianceAltarBlock extends Block {
    private final AllegiancePath path;

    public AllegianceAltarBlock(AllegiancePath path, Properties properties) {
        super(properties);
        this.path = path;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }

        if (AllegianceUtil.hasAnyPath(serverPlayer)) {
            serverPlayer.displayClientMessage(Component.translatable("block.usless_mobs.allegiance_altar.already_chosen")
                    .withStyle(ChatFormatting.DARK_RED), false);
            return InteractionResult.CONSUME;
        }
        MinecraftServer server = serverPlayer.getServer();
        if (server == null) {
            return InteractionResult.PASS;
        }
        TrueCrownTracker tracker = TrueCrownTracker.get(server);
        if (!tracker.canClaimFinalPath(path, serverPlayer.getUUID())) {
            serverPlayer.displayClientMessage(Component.translatable("block.usless_mobs.allegiance_altar.path_taken")
                    .withStyle(ChatFormatting.DARK_RED), false);
            return InteractionResult.CONSUME;
        }
        if (!consumeBalancedCrown(serverPlayer)) {
            serverPlayer.displayClientMessage(Component.translatable("block.usless_mobs.allegiance_altar.needs_crown")
                    .withStyle(ChatFormatting.GOLD), false);
            return InteractionResult.CONSUME;
        }

        AllegianceUtil.setIfUnset(serverPlayer, path);
        AllegianceUtil.setStage(serverPlayer, com.Momik.usless_mobs.allegiance.AllegianceStage.PATH_CHOSEN);
        com.Momik.usless_mobs.event.KingSlimeAdvancements.grant(serverPlayer, "allegiance/path_chosen");
        tracker.claimFinalPath(path, serverPlayer.getUUID(), serverPlayer.getGameProfile().getName());
        grantTrueTemplate(serverPlayer);
        serverPlayer.displayClientMessage(Component.translatable("block.usless_mobs.allegiance_altar.chosen." + path.name().toLowerCase())
                .withStyle(pathColor()), false);
        serverLevel.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.35F, path == AllegiancePath.VOID ? 0.55F : 1.2F);
        sendChoiceParticles(serverLevel, pos);
        clearNearbyAltars(serverLevel, pos);
        return InteractionResult.CONSUME;
    }

    private Item trueTemplateItem() {
        if (path == AllegiancePath.VOID) {
            return com.Momik.usless_mobs.registry.ModItems.TRUE_VOID_TEMPLATE.get();
        }
        if (path == AllegiancePath.CELESTIAL) {
            return com.Momik.usless_mobs.registry.ModItems.TRUE_CELESTIAL_TEMPLATE.get();
        }
        return com.Momik.usless_mobs.registry.ModItems.TRUE_LIVING_TEMPLATE.get();
    }

    private ChatFormatting pathColor() {
        if (path == AllegiancePath.VOID) {
            return ChatFormatting.DARK_PURPLE;
        }
        if (path == AllegiancePath.CELESTIAL) {
            return ChatFormatting.AQUA;
        }
        return ChatFormatting.GREEN;
    }

    private static boolean consumeBalancedCrown(ServerPlayer player) {
        if (consumeFromList(player.getInventory().items)) {
            player.getInventory().setChanged();
            return true;
        }
        if (consumeFromList(player.getInventory().armor)) {
            player.getInventory().setChanged();
            return true;
        }
        if (consumeFromList(player.getInventory().offhand)) {
            player.getInventory().setChanged();
            return true;
        }
        return false;
    }

    private void grantTrueTemplate(ServerPlayer player) {
        ItemStack templates = new ItemStack(trueTemplateItem(), 4);
        if (!player.getInventory().add(templates)) {
            player.drop(templates, false);
        }
        player.displayClientMessage(Component.translatable("block.usless_mobs.allegiance_altar.true_template")
                .withStyle(pathColor()), false);
    }

    private static boolean consumeFromList(NonNullList<ItemStack> stacks) {
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (!stack.is(com.Momik.usless_mobs.registry.ModItems.TRUE_CROWN.get())) {
                continue;
            }
            stack.shrink(1);
            if (stack.isEmpty()) {
                stacks.set(i, ItemStack.EMPTY);
            }
            return true;
        }
        return false;
    }

    private void sendChoiceParticles(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 1.1D;
        double z = pos.getZ() + 0.5D;
        if (path == AllegiancePath.VOID) {
            level.sendParticles(ParticleTypes.SCULK_SOUL, x, y, z, 70, 0.7D, 0.7D, 0.7D, 0.05D);
            level.sendParticles(ParticleTypes.PORTAL, x, y, z, 45, 0.8D, 0.7D, 0.8D, 0.25D);
        } else if (path == AllegiancePath.CELESTIAL) {
            level.sendParticles(ParticleTypes.END_ROD, x, y, z, 80, 0.75D, 0.9D, 0.75D, 0.04D);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 30, 0.55D, 0.45D, 0.55D, 0.02D);
        } else {
            level.sendParticles(ParticleTypes.HEART, x, y, z, 16, 0.5D, 0.6D, 0.5D, 0.02D);
            level.sendParticles(ParticleTypes.COMPOSTER, x, y, z, 80, 0.8D, 0.5D, 0.8D, 0.06D);
        }
    }

    private static void clearNearbyAltars(ServerLevel level, BlockPos center) {
        for (BlockPos current : BlockPos.betweenClosed(center.offset(-5, -2, -5), center.offset(5, 2, 5))) {
            BlockState state = level.getBlockState(current);
            if (state.is(com.Momik.usless_mobs.registry.ModBlocks.VOID_ALTAR.get()) || state.is(com.Momik.usless_mobs.registry.ModBlocks.CELESTIAL_ALTAR.get()) || state.is(com.Momik.usless_mobs.registry.ModBlocks.LIVING_ALTAR.get())) {
                level.removeBlock(current, false);
            }
        }
    }
}
