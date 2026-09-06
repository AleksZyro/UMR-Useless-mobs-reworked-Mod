package com.Momik.usless_mobs.world;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class BigUnderwaterCaveFeature extends Feature<NoneFeatureConfiguration> {
    static final int MIN_HORIZONTAL_RADIUS = 10;
    static final int MAX_HORIZONTAL_RADIUS = 18;
    static final int MIN_VERTICAL_RADIUS = 6;
    static final int MAX_VERTICAL_RADIUS = 10;

    public BigUnderwaterCaveFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        if (origin.getY() > level.getSeaLevel() - 14
                || origin.getY() < level.getMinBuildHeight() + MAX_VERTICAL_RADIUS + 3
                || !level.getBlockState(origin).is(BlockTags.BASE_STONE_OVERWORLD)) {
            return false;
        }

        int radiusX = random.nextIntBetweenInclusive(MIN_HORIZONTAL_RADIUS, MAX_HORIZONTAL_RADIUS);
        int radiusZ = random.nextIntBetweenInclusive(MIN_HORIZONTAL_RADIUS, MAX_HORIZONTAL_RADIUS);
        int radiusY = random.nextIntBetweenInclusive(MIN_VERTICAL_RADIUS, MAX_VERTICAL_RADIUS);
        int changed = 0;

        for (int dx = -radiusX; dx <= radiusX; dx++) {
            for (int dy = -radiusY; dy <= radiusY; dy++) {
                for (int dz = -radiusZ; dz <= radiusZ; dz++) {
                    double distance = square(dx / (double) radiusX)
                            + square(dy / (double) radiusY)
                            + square(dz / (double) radiusZ);
                    if (distance > 1.0D) {
                        continue;
                    }

                    BlockPos target = origin.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(target);
                    if (state.is(Blocks.BEDROCK) || level.getBlockEntity(target) != null) {
                        continue;
                    }

                    if (distance > 0.82D && state.is(BlockTags.BASE_STONE_OVERWORLD)
                            && random.nextFloat() < 0.035F) {
                        level.setBlock(target, landmark(random), 2);
                        changed++;
                    } else if (isFloodable(state)) {
                        level.setBlock(target, Blocks.WATER.defaultBlockState(), 2);
                        changed++;
                    }
                }
            }
        }

        return changed > 0;
    }

    private static double square(double value) {
        return value * value;
    }

    private static boolean isFloodable(BlockState state) {
        return state.isAir()
                || state.is(Blocks.WATER)
                || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY);
    }

    private static BlockState landmark(RandomSource random) {
        return switch (random.nextInt(12)) {
            case 0 -> Blocks.SEA_LANTERN.defaultBlockState();
            case 1, 2 -> Blocks.DARK_PRISMARINE.defaultBlockState();
            case 3, 4, 5 -> Blocks.CALCITE.defaultBlockState();
            default -> Blocks.PRISMARINE.defaultBlockState();
        };
    }
}
