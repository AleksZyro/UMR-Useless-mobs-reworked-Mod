package com.Momik.usless_mobs.item;

import com.Momik.usless_mobs.entity.KingSlimeEntity;
import com.Momik.usless_mobs.event.KingSlimeAdvancements;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class KingSlimeSpawnerItem extends Item {

    public KingSlimeSpawnerItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.consume(stack);
        }

        ServerLevel serverLevel = (ServerLevel) level;
        if (player instanceof ServerPlayer serverPlayer && !KingSlimeAdvancements.hasEnteredNether(serverPlayer)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("item.usless_mobs.king_slime_spawner.locked"), true);
            return InteractionResultHolder.fail(stack);
        }

        HitResult ray = player.pick(8.0D, 0.0F, false);
        if (!(ray instanceof BlockHitResult blockHit) || ray.getType() != HitResult.Type.BLOCK) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("item.usless_mobs.king_slime_spawner.ritual"), true);
            return InteractionResultHolder.fail(stack);
        }

        BlockPos ritualPos = blockHit.getBlockPos();
        if (!serverLevel.getBlockState(ritualPos).is(com.Momik.usless_mobs.registry.ModBlocks.GOLDENER_SCHLEIMBLOCK.get())) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("item.usless_mobs.king_slime_spawner.ritual"), true);
            return InteractionResultHolder.fail(stack);
        }
        BlockPos spawnPos = ritualPos.above();

        KingSlimeEntity king = com.Momik.usless_mobs.registry.ModEntities.KING_SCHLEIM.get().create(serverLevel);
        if (king == null) {
            return InteractionResultHolder.fail(stack);
        }
        king.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, level.getRandom().nextFloat() * 360.0F, 0.0F);
        king.finalizeSpawn(serverLevel, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.SPAWN_EGG, null, null);
        if (!serverLevel.addFreshEntity(king)) {
            return InteractionResultHolder.fail(stack);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            KingSlimeAdvancements.grant(serverPlayer, KingSlimeAdvancements.SUMMON_KING_SLIME);
        }

        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, spawnPos.getX() + 0.5D, spawnPos.getY() + 1.0D, spawnPos.getZ() + 0.5D, 4, 1.5D, 1.0D, 1.5D, 0.0D);
        serverLevel.sendParticles(ParticleTypes.PORTAL, spawnPos.getX() + 0.5D, spawnPos.getY() + 1.0D, spawnPos.getZ() + 0.5D, 80, 1.5D, 2.0D, 1.5D, 0.6D);
        serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, ritualPos.getX() + 0.5D, ritualPos.getY() + 0.8D, ritualPos.getZ() + 0.5D, 35, 0.7D, 0.35D, 0.7D, 0.08D);
        level.playSound(null, spawnPos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.5F, 0.7F);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        player.getCooldowns().addCooldown(this, 200);

        return InteractionResultHolder.consume(stack);
    }
}
