package net.mysith.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.mysith.entity.VoidReaperEntity;
import net.mysith.registry.ModEntities;
import org.jetbrains.annotations.Nullable;

public class VoidSummonerItem extends Item {
    public VoidSummonerItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }

        Player player = context.getPlayer();
        BlockPos altarPos = context.getClickedPos();
        if (context.getClickedFace() != Direction.UP || !level.getBlockState(altarPos).is(Blocks.CRYING_OBSIDIAN)) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("item.usless_mobs.void_summoner.needs_altar"), true);
            }
            serverLevel.playSound(null, altarPos, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 0.85F, 0.65F);
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    altarPos.getX() + 0.5D, altarPos.getY() + 1.0D, altarPos.getZ() + 0.5D,
                    18, 0.35D, 0.25D, 0.35D, 0.02D);
            return InteractionResult.FAIL;
        }

        BlockPos spawnPos = altarPos.above();
        VoidReaperEntity reaper = ModEntities.VOID_REAPER.get().create(serverLevel);
        if (reaper == null) {
            return InteractionResult.PASS;
        }

        reaper.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                level.random.nextFloat() * 360.0F, 0.0F);
        reaper.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(spawnPos), MobSpawnType.MOB_SUMMONED, null, null);
        reaper.setPersistenceRequired();
        if (player != null) {
            reaper.setTarget(player);
        }

        if (!serverLevel.addFreshEntity(reaper)) {
            return InteractionResult.FAIL;
        }
        serverLevel.setBlock(altarPos, Blocks.OBSIDIAN.defaultBlockState(), 3);
        serverLevel.sendParticles(ParticleTypes.SCULK_SOUL,
                spawnPos.getX() + 0.5D, spawnPos.getY() + 1.0D, spawnPos.getZ() + 0.5D,
                80, 1.1D, 0.75D, 1.1D, 0.08D);
        serverLevel.sendParticles(ParticleTypes.PORTAL,
                spawnPos.getX() + 0.5D, spawnPos.getY() + 0.7D, spawnPos.getZ() + 0.5D,
                80, 0.8D, 0.8D, 0.8D, 0.35D);
        serverLevel.playSound(null, spawnPos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 0.65F);

        ItemStack stack = context.getItemInHand();
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        if (player != null) {
            player.displayClientMessage(Component.translatable("item.usless_mobs.void_summoner.used"), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.usless_mobs.void_summoner.tooltip1").withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.translatable("item.usless_mobs.void_summoner.tooltip2").withStyle(ChatFormatting.GRAY));
    }
}
