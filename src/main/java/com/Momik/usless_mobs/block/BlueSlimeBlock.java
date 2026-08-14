package com.Momik.usless_mobs.block;

import com.Momik.usless_mobs.effect.SlimePowerToggle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class BlueSlimeBlock extends SlimeBlock {

    public BlueSlimeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        if (entity.isSuppressingBounce()) {
            super.fallOn(level, state, pos, entity, fallDistance);
            return;
        }

        entity.causeFallDamage(fallDistance, 0.0F, level.damageSources().fall());

        if (!level.isClientSide && entity instanceof Player player) {
            SlimePowerToggle.applyFiltered(player, com.Momik.usless_mobs.registry.ModEffects.ELASTICITY.get(), 120, 0);
        }
    }

    @Override
    public void updateEntityAfterFallOn(BlockGetter level, Entity entity) {
        if (entity.isSuppressingBounce()) {
            super.updateEntityAfterFallOn(level, entity);
            return;
        }

        Vec3 movement = entity.getDeltaMovement();

        if (movement.y < 0.0D) {
            double verticalBoost = entity instanceof LivingEntity ? 1.18D : 1.06D;
            entity.setDeltaMovement(movement.x, -movement.y * verticalBoost, movement.z);
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && entity instanceof Player player) {
            SlimePowerToggle.applyFiltered(player, com.Momik.usless_mobs.registry.ModEffects.ELASTICITY.get(), 80, 0);
        }

        super.stepOn(level, pos, state, entity);
    }

    // Forge stick-to-pistons hook — vanilla checks Blocks.SLIME_BLOCK directly,
    // so without this override custom slime blocks don't stick to pistons.
    @Override
    public boolean isStickyBlock(BlockState state) {
        return true;
    }

    @Override
    public boolean isSlimeBlock(BlockState state) {
        return true;
    }

    @Override
    public boolean canStickTo(BlockState state, BlockState other) {
        // Same logic as vanilla — slime + honey don't stick to each other, everything else sticks.
        if (other.is(net.minecraft.world.level.block.Blocks.HONEY_BLOCK)) {
            return false;
        }
        return true;
    }
}
