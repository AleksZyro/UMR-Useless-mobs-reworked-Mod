package com.Momik.usless_mobs.world;

import com.Momik.usless_mobs.registry.ModBlocks;
import com.Momik.usless_mobs.registry.ModEntities;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class CelestialSpireFeature extends Feature<NoneFeatureConfiguration> {

    private static final ResourceLocation LOOT =
            ResourceLocation.tryBuild("usless_mobs", "chests/celestial_spire");
    private static final int HEIGHT = 10;

    public CelestialSpireFeature(Codec<NoneFeatureConfiguration> codec) {
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

        // 3x3 packed ice base
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlock(origin.offset(dx, -1, dz), Blocks.PACKED_ICE.defaultBlockState(), 2);
            }
        }
        // Blue ice centre of base
        level.setBlock(origin.offset(0, -1, 0), Blocks.BLUE_ICE.defaultBlockState(), 2);

        // Central aether tower
        for (int dy = 0; dy < HEIGHT; dy++) {
            level.setBlock(origin.above(dy), ModBlocks.CELESTIAL_AETHER_BLOCK.get().defaultBlockState(), 2);
        }

        // Calcite cross at top
        BlockPos top = origin.above(HEIGHT);
        level.setBlock(top, Blocks.CALCITE.defaultBlockState(), 2);
        for (int[] dir : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
            level.setBlock(top.offset(dir[0], 0, dir[1]), Blocks.CALCITE.defaultBlockState(), 2);
        }

        // Amethyst cluster tip
        level.setBlock(top.above(), Blocks.AMETHYST_CLUSTER.defaultBlockState(), 2);

        // Small packed ice outriggers midway
        int mid = HEIGHT / 2;
        for (int[] dir : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
            if (rand.nextFloat() < 0.7f) {
                BlockPos out = origin.above(mid).offset(dir[0], 0, dir[1]);
                level.setBlock(out, Blocks.PACKED_ICE.defaultBlockState(), 2);
                level.setBlock(out.above(), ModBlocks.CELESTIAL_AETHER_BLOCK.get().defaultBlockState(), 2);
            }
        }

        // Celestial Slime spawner at base
        BlockPos spawnerPos = origin.offset(-1, 0, -1);
        level.setBlock(spawnerPos, Blocks.SPAWNER.defaultBlockState(), 2);
        if (level.getBlockEntity(spawnerPos) instanceof SpawnerBlockEntity spawner) {
            spawner.getSpawner().setEntityId(ModEntities.CELESTIAL_SLIME.get(), null, rand, spawnerPos);
        }

        // Chest at base, offset one block
        BlockPos chestPos = origin.offset(1, 0, 1);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
        RandomizableContainerBlockEntity.setLootTable(level, rand, chestPos, LOOT);

        return true;
    }
}
