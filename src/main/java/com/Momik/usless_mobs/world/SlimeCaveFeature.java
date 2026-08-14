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

/**
 * Procedural Slime-Cave: hohle Kugel aus Schleimblock, zufällige Kristall-Dekorationen,
 * Kiste im Innern mit Schleimkram. Generiert underground in geeigneten Biomen.
 */
public class SlimeCaveFeature extends Feature<NoneFeatureConfiguration> {

    private static final ResourceLocation LOOT =
            ResourceLocation.tryBuild("usless_mobs", "chests/slime_cave");
    private static final int RADIUS = 4;

    public SlimeCaveFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        RandomSource rand = ctx.random();

        if (!level.getBlockState(origin).isSolidRender(level, origin)) {
            return false;
        }

        // Sphere aushöhlen + Shell aus Slime bauen
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dy = -RADIUS; dy <= RADIUS; dy++) {
                for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                    double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
                    BlockPos p = origin.offset(dx, dy, dz);
                    if (dist <= RADIUS - 0.5) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    } else if (dist <= RADIUS + 0.3) {
                        if (rand.nextFloat() < 0.12f) {
                            level.setBlock(p, ModBlocks.GOLDENER_SCHLEIMBLOCK.get().defaultBlockState(), 2);
                        } else {
                            level.setBlock(p, ModBlocks.BLAUER_SCHLEIMBLOCK.get().defaultBlockState(), 2);
                        }
                    }
                }
            }
        }

        // Boden der Höhle mit Stein bedecken
        for (int dx = -(RADIUS-1); dx <= RADIUS-1; dx++) {
            for (int dz = -(RADIUS-1); dz <= RADIUS-1; dz++) {
                if (dx*dx + dz*dz <= (RADIUS-1)*(RADIUS-1)) {
                    level.setBlock(origin.offset(dx, -(RADIUS-1), dz),
                            Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 2);
                }
            }
        }

        // Kristall-Dekorationen an den Wänden (8 Positionen)
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI * 2.0 / 8;
            int dx = (int) Math.round((RADIUS - 1) * Math.cos(angle));
            int dz = (int) Math.round((RADIUS - 1) * Math.sin(angle));
            int dy = rand.nextInt(3) - 1;
            BlockPos p = origin.offset(dx, dy, dz);
            if (level.getBlockState(p).isAir()) {
                switch (rand.nextInt(3)) {
                    case 0 -> level.setBlock(p, ModBlocks.LIVING_CRYSTAL_BLOCK.get().defaultBlockState(), 2);
                    case 1 -> level.setBlock(p, ModBlocks.VOID_FRAGMENT_BLOCK.get().defaultBlockState(), 2);
                    default -> level.setBlock(p, ModBlocks.CELESTIAL_AETHER_BLOCK.get().defaultBlockState(), 2);
                }
            }
        }

        // Kiste mit Beute im Zentrum
        BlockPos chestPos = origin.above();
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
        RandomizableContainerBlockEntity.setLootTable(level, rand, chestPos, LOOT);

        return true;
    }
}
