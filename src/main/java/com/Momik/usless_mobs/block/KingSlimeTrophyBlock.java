package com.Momik.usless_mobs.block;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class KingSlimeTrophyBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(
            box(2.0D, 0.0D, 2.0D, 14.0D, 3.0D, 14.0D),
            box(5.5D, 3.0D, 6.75D, 10.5D, 4.25D, 9.25D),
            box(6.0D, 4.2D, 6.6D, 10.0D, 5.25D, 9.4D),
            box(1.0D, 4.5D, 6.0D, 15.0D, 16.0D, 10.0D)
    );

    public KingSlimeTrophyBlock(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KingSlimeTrophyBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof KingSlimeTrophyBlockEntity trophy) {
            player.displayClientMessage(trophy.infoComponent(), false);
            level.playSound(null, pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 0.65F, 1.4F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
