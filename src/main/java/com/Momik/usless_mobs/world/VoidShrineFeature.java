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

public class VoidShrineFeature extends Feature<NoneFeatureConfiguration> {

    private static final ResourceLocation LOOT =
            ResourceLocation.tryBuild("usless_mobs", "chests/void_shrine");

    public VoidShrineFeature(Codec<NoneFeatureConfiguration> codec) {
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

        // 5x5 obsidian floor
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.setBlock(origin.offset(dx, -1, dz), Blocks.OBSIDIAN.defaultBlockState(), 2);
                level.setBlock(origin.offset(dx, 0, dz), Blocks.AIR.defaultBlockState(), 2);
                level.setBlock(origin.offset(dx, 1, dz), Blocks.AIR.defaultBlockState(), 2);
                level.setBlock(origin.offset(dx, 2, dz), Blocks.AIR.defaultBlockState(), 2);
            }
        }

        // Sculk accents on floor edges
        for (int dx = -2; dx <= 2; dx++) {
            if (rand.nextFloat() < 0.4f) level.setBlock(origin.offset(dx, -1, -2), Blocks.SCULK.defaultBlockState(), 2);
            if (rand.nextFloat() < 0.4f) level.setBlock(origin.offset(dx, -1,  2), Blocks.SCULK.defaultBlockState(), 2);
            if (rand.nextFloat() < 0.4f) level.setBlock(origin.offset(-2, -1, dx), Blocks.SCULK.defaultBlockState(), 2);
            if (rand.nextFloat() < 0.4f) level.setBlock(origin.offset( 2, -1, dx), Blocks.SCULK.defaultBlockState(), 2);
        }

        // 4 obsidian corner pillars (height 3)
        for (int[] corner : new int[][]{{-2, -2}, {-2, 2}, {2, -2}, {2, 2}}) {
            for (int dy = 0; dy <= 2; dy++) {
                level.setBlock(origin.offset(corner[0], dy, corner[1]),
                        Blocks.OBSIDIAN.defaultBlockState(), 2);
            }
            // Crying obsidian cap
            level.setBlock(origin.offset(corner[0], 3, corner[1]),
                    Blocks.CRYING_OBSIDIAN.defaultBlockState(), 2);
        }

        // Central void fragment column
        level.setBlock(origin, ModBlocks.VOID_FRAGMENT_BLOCK.get().defaultBlockState(), 2);
        level.setBlock(origin.above(), ModBlocks.VOID_FRAGMENT_BLOCK.get().defaultBlockState(), 2);
        level.setBlock(origin.above(2), Blocks.AMETHYST_CLUSTER.defaultBlockState(), 2);

        // Sculk sensor ring
        for (int[] pos : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            if (rand.nextFloat() < 0.6f) {
                level.setBlock(origin.offset(pos[0], 0, pos[1]),
                        Blocks.SCULK_SENSOR.defaultBlockState(), 2);
            }
        }

        // Ender Slime spawner inside shrine
        BlockPos spawnerPos = origin.offset(-1, 0, -1);
        level.setBlock(spawnerPos, Blocks.SPAWNER.defaultBlockState(), 2);
        if (level.getBlockEntity(spawnerPos) instanceof SpawnerBlockEntity spawner) {
            spawner.getSpawner().setEntityId(ModEntities.ENDER_SCHLEIM.get(), null, rand, spawnerPos);
        }

        // Chest with void loot
        BlockPos chestPos = origin.offset(0, 0, 2).above();
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
        RandomizableContainerBlockEntity.setLootTable(level, rand, chestPos, LOOT);

        return true;
    }
}
