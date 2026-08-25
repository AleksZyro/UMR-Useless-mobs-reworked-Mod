package com.Momik.usless_mobs.worldgen;

import com.Momik.usless_mobs.Config;
import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

public final class AncientWhaleRuinStructure extends Structure {
    public static final Codec<AncientWhaleRuinStructure> CODEC = simpleCodec(AncientWhaleRuinStructure::new);

    public AncientWhaleRuinStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int centerX = context.chunkPos().getMiddleBlockX();
        int centerZ = context.chunkPos().getMiddleBlockZ();
        int floorY = context.chunkGenerator().getFirstOccupiedHeight(
                centerX,
                centerZ,
                Heightmap.Types.OCEAN_FLOOR_WG,
                context.heightAccessor(),
                context.randomState());
        if (floorY > context.chunkGenerator().getSeaLevel() - 14
                || floorY < context.heightAccessor().getMinBuildHeight() + 8
                || !passesConfiguredSpacing(context)) {
            return Optional.empty();
        }

        BlockPos center = new BlockPos(centerX, floorY, centerZ);
        return Optional.of(new GenerationStub(center,
                builder -> builder.addPiece(new AncientWhaleRuinPiece(center))));
    }

    private static boolean passesConfiguredSpacing(GenerationContext context) {
        int configured = Math.max(32, Config.ancientWhaleRuinSpacing);
        if (configured <= 72) {
            return true;
        }
        long mixed = (long) context.chunkPos().x * 341873128712L
                ^ (long) context.chunkPos().z * 132897987541L
                ^ context.seed();
        int keepOneIn = Math.max(1, Math.round((float) configured / 72.0F));
        return Math.floorMod(mixed, keepOneIn) == 0;
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.ANCIENT_WHALE_RUIN.get();
    }
}
