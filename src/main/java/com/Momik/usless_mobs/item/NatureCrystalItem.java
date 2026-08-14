package com.Momik.usless_mobs.item;

import com.Momik.usless_mobs.entity.LivingBossEntity;
import com.Momik.usless_mobs.entity.WitchBossEntity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class NatureCrystalItem extends Item {

    public NatureCrystalItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (player == null) {
            return InteractionResult.PASS;
        }
        if (isWitchAltar(state)) {
            return summonWitchBoss(context, level, player, pos);
        }
        if (!isLivingAltar(state)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        if (!hasRequiredMaterial(player, com.Momik.usless_mobs.registry.ModItems.LIVING_TISSUE.get()) || !hasRequiredMaterial(player, com.Momik.usless_mobs.registry.ModItems.FROST_CORE.get())) {
            player.displayClientMessage(Component.translatable("item.usless_mobs.nature_crystal.needs_altar_materials")
                    .withStyle(ChatFormatting.GREEN), true);
            return InteractionResult.FAIL;
        }

        LivingBossEntity boss = com.Momik.usless_mobs.registry.ModEntities.LIVING_BOSS.get().create(serverLevel);
        if (boss == null) {
            return InteractionResult.FAIL;
        }

        boss.moveTo(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, player.getYRot(), 0.0F);
        boss.setTarget(player);
        if (!serverLevel.addFreshEntity(boss)) {
            return InteractionResult.FAIL;
        }

        if (!player.getAbilities().instabuild) {
            consumeRequiredMaterial(player, com.Momik.usless_mobs.registry.ModItems.LIVING_TISSUE.get());
            consumeRequiredMaterial(player, com.Momik.usless_mobs.registry.ModItems.FROST_CORE.get());
            context.getItemInHand().shrink(1);
        }

        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.COMPOSTER,
                pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                80, 0.9D, 0.6D, 0.9D, 0.08D);
        serverLevel.playSound(null, pos, SoundEvents.AZALEA_PLACE, SoundSource.BLOCKS, 1.5F, 0.65F);
        serverPlayer.displayClientMessage(Component.translatable("item.usless_mobs.nature_crystal.boss_summoned")
                .withStyle(ChatFormatting.DARK_GREEN), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Animal animal)) {
            return InteractionResult.PASS;
        }

        if (!player.level().isClientSide) {
            boolean didSomething = false;
            if (animal.getHealth() < animal.getMaxHealth()) {
                animal.heal(6.0F);
                didSomething = true;
            }
            if (!animal.isBaby() && animal.canFallInLove()) {
                animal.setInLove(player);
                didSomething = true;
            }

            if (didSomething && !player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            if (didSomething) {
                ((ServerLevel) player.level()).sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                        target.getX(), target.getY(0.8D), target.getZ(),
                        8, 0.35D, 0.25D, 0.35D, 0.02D);
                return InteractionResult.CONSUME;
            }
        }

        return InteractionResult.SUCCESS;
    }

    private static boolean isLivingAltar(BlockState state) {
        return state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.ROOTED_DIRT);
    }

    private static boolean isWitchAltar(BlockState state) {
        return state.is(Blocks.BREWING_STAND);
    }

    private static InteractionResult summonWitchBoss(UseOnContext context, Level level, Player player, BlockPos pos) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (!hasRequiredMaterial(player, com.Momik.usless_mobs.registry.ModItems.SHADOWTOOTH.get())
                || !hasRequiredMaterial(player, com.Momik.usless_mobs.registry.ModItems.GLOW_FLARE.get())
                || !hasRequiredMaterial(player, com.Momik.usless_mobs.registry.ModItems.AXOLOTL_GILLS.get())) {
            player.displayClientMessage(Component.translatable("item.usless_mobs.nature_crystal.needs_witch_materials")
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return InteractionResult.FAIL;
        }

        WitchBossEntity boss = com.Momik.usless_mobs.registry.ModEntities.WITCH_BOSS.get().create(serverLevel);
        if (boss == null) {
            return InteractionResult.FAIL;
        }
        boss.moveTo(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, player.getYRot(), 0.0F);
        boss.setTarget(player);
        if (!serverLevel.addFreshEntity(boss)) {
            return InteractionResult.FAIL;
        }
        if (!player.getAbilities().instabuild) {
            consumeRequiredMaterial(player, com.Momik.usless_mobs.registry.ModItems.SHADOWTOOTH.get());
            consumeRequiredMaterial(player, com.Momik.usless_mobs.registry.ModItems.GLOW_FLARE.get());
            consumeRequiredMaterial(player, com.Momik.usless_mobs.registry.ModItems.AXOLOTL_GILLS.get());
            context.getItemInHand().shrink(1);
        }
        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH,
                pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                90, 0.75D, 0.65D, 0.75D, 0.08D);
        serverLevel.playSound(null, pos, SoundEvents.WITCH_CELEBRATE, SoundSource.BLOCKS, 1.5F, 0.7F);
        serverPlayer.displayClientMessage(Component.translatable("item.usless_mobs.nature_crystal.witch_boss_summoned")
                .withStyle(ChatFormatting.DARK_PURPLE), true);
        return InteractionResult.CONSUME;
    }

    private static boolean hasRequiredMaterial(Player player, Item item) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        return player.getInventory().contains(new ItemStack(item));
    }

    private static void consumeRequiredMaterial(Player player, Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                stack.shrink(1);
                return;
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.usless_mobs.nature_crystal.tooltip.heal").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("item.usless_mobs.nature_crystal.tooltip.altar").withStyle(ChatFormatting.DARK_GREEN));
        tooltip.add(Component.translatable("item.usless_mobs.nature_crystal.tooltip.witch").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
