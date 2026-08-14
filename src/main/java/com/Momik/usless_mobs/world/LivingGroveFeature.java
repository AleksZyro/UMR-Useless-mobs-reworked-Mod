package com.Momik.usless_mobs.world;

import com.Momik.usless_mobs.registry.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class LivingGroveFeature extends Feature<NoneFeatureConfiguration> {

    private static final ResourceLocation LOOT =
            ResourceLocation.tryBuild("usless_mobs", "chests/living_grove");

    public LivingGroveFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        RandomSource rand = ctx.random();

        if (!level.getBlockState(origin.below()).isSolidRender(level, origin.below())) {
            return false;
        }

        // Moss + grass clearing (radius 3)
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (dx * dx + dz * dz <= 9) {
                    BlockPos floor = origin.offset(dx, -1, dz);
                    if (rand.nextFloat() < 0.55f) {
                        level.setBlock(floor, Blocks.MOSS_BLOCK.defaultBlockState(), 2);
                    } else {
                        level.setBlock(floor, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                    }
                    level.setBlock(origin.offset(dx, 0, dz), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }

        // 4 oak log "tree" trunks at cardinal positions
        int[][] treeOffsets = {{2, 0}, {-2, 0}, {0, 2}, {0, -2}};
        for (int[] off : treeOffsets) {
            int treeH = 3 + rand.nextInt(2); // 3-4 blocks tall
            for (int dy = 0; dy < treeH; dy++) {
                level.setBlock(origin.offset(off[0], dy, off[1]),
                        Blocks.OAK_LOG.defaultBlockState(), 2);
            }
            // Leaf crown
            BlockPos crown = origin.offset(off[0], treeH, off[1]);
            for (int ldx = -1; ldx <= 1; ldx++) {
                for (int ldz = -1; ldz <= 1; ldz++) {
                    if (rand.nextFloat() < 0.8f) {
                        level.setBlock(crown.offset(ldx, 0, ldz),
                                Blocks.OAK_LEAVES.defaultBlockState(), 2);
                    }
                }
            }
            level.setBlock(crown.above(), Blocks.OAK_LEAVES.defaultBlockState(), 2);

            // Living crystal block at tree base
            level.setBlock(origin.offset(off[0], 0, off[1]).below(),
                    ModBlocks.LIVING_CRYSTAL_BLOCK.get().defaultBlockState(), 2);
        }

        // Fern/grass carpet in clearing
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (rand.nextFloat() < 0.5f) {
                    BlockPos p = origin.offset(dx, 0, dz);
                    if (level.getBlockState(p).isAir()) {
                        level.setBlock(p, Blocks.FERN.defaultBlockState(), 2);
                    }
                }
            }
        }

        // Moss vines hanging from logs
        for (int[] off : treeOffsets) {
            if (rand.nextFloat() < 0.5f) {
                level.setBlock(origin.offset(off[0], 1, off[1]).relative(
                        net.minecraft.core.Direction.NORTH, 1),
                        Blocks.VINE.defaultBlockState(), 2);
            }
        }

        // Central living crystal cluster
        level.setBlock(origin, ModBlocks.LIVING_CRYSTAL_BLOCK.get().defaultBlockState(), 2);
        level.setBlock(origin.above(), Blocks.AMETHYST_CLUSTER.defaultBlockState(), 2);

        // Buried chest in centre
        BlockPos chestPos = origin.offset(1, 0, 0);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
        RandomizableContainerBlockEntity.setLootTable(level, rand, chestPos, LOOT);

        return true;
    }
}
