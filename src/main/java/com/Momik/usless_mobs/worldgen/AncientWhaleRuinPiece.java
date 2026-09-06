package com.Momik.usless_mobs.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public final class AncientWhaleRuinPiece extends StructurePiece {
    public static final int WIDTH = 49;
    public static final int HEIGHT = 17;
    public static final int DEPTH = 45;
    public static final int TREASURE_GOLD_BLOCKS = 7;

    public AncientWhaleRuinPiece(BlockPos center) {
        super(ModStructures.ANCIENT_WHALE_RUIN_PIECE.get(), 0, new BoundingBox(
                center.getX() - WIDTH / 2,
                center.getY() - 1,
                center.getZ() - DEPTH / 2,
                center.getX() + WIDTH / 2,
                center.getY() + HEIGHT - 2,
                center.getZ() + DEPTH / 2));
    }

    public AncientWhaleRuinPiece(CompoundTag tag) {
        super(ModStructures.ANCIENT_WHALE_RUIN_PIECE.get(), tag);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager,
                            ChunkGenerator chunkGenerator, RandomSource random,
                            BoundingBox chunkBounds, ChunkPos chunkPos, BlockPos pivot) {
        floodNavigationVolume(level, chunkBounds);
        buildFoundation(level, chunkBounds);
        buildTreasury(level, chunkBounds);
        buildWhaleSkeleton(level, chunkBounds, 5, 7, true);
        buildWhaleSkeleton(level, chunkBounds, 43, 36, false);
        buildBrokenArches(level, chunkBounds);
    }

    private void floodNavigationVolume(WorldGenLevel level, BoundingBox chunkBounds) {
        for (int x = 2; x < WIDTH - 2; x++) {
            for (int z = 2; z < DEPTH - 2; z++) {
                double nx = (x - WIDTH / 2.0D) / 23.0D;
                double nz = (z - DEPTH / 2.0D) / 20.0D;
                if (nx * nx + nz * nz > 1.0D) {
                    continue;
                }
                for (int y = 2; y <= 12; y++) {
                    BlockPos pos = worldPos(x, y, z);
                    if (chunkBounds.isInside(pos) && level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, Blocks.WATER.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    private void buildFoundation(WorldGenLevel level, BoundingBox chunkBounds) {
        for (int x = 1; x < WIDTH - 1; x++) {
            for (int z = 1; z < DEPTH - 1; z++) {
                double nx = (x - WIDTH / 2.0D) / 24.0D;
                double nz = (z - DEPTH / 2.0D) / 22.0D;
                if (nx * nx + nz * nz <= 1.0D && ((x + z) & 3) != 0) {
                    BlockState state = ((x * 31 + z * 17) & 7) == 0
                            ? Blocks.DARK_PRISMARINE.defaultBlockState()
                            : Blocks.PRISMARINE_BRICKS.defaultBlockState();
                    setBlock(level, chunkBounds, x, 0, z, state);
                }
            }
        }
    }

    private void buildTreasury(WorldGenLevel level, BoundingBox chunkBounds) {
        for (int x = 17; x <= 31; x++) {
            for (int z = 16; z <= 28; z++) {
                boolean edge = x == 17 || x == 31 || z == 16 || z == 28;
                setBlock(level, chunkBounds, x, 1, z, Blocks.DARK_PRISMARINE.defaultBlockState());
                if (edge && !isDoorway(x, z)) {
                    for (int y = 2; y <= 6; y++) {
                        if (((x + z + y) % 11) != 0) {
                            setBlock(level, chunkBounds, x, y, z, Blocks.PRISMARINE_BRICKS.defaultBlockState());
                        }
                    }
                }
            }
        }
        for (int x = 18; x <= 30; x++) {
            setBlock(level, chunkBounds, x, 7, 16, Blocks.PRISMARINE_BRICKS.defaultBlockState());
            setBlock(level, chunkBounds, x, 7, 28, Blocks.PRISMARINE_BRICKS.defaultBlockState());
        }
        int[][] treasure = {{24, 2, 22}, {23, 2, 22}, {25, 2, 22}, {24, 2, 21},
                {24, 2, 23}, {23, 2, 21}, {25, 2, 23}};
        for (int[] p : treasure) {
            setBlock(level, chunkBounds, p[0], p[1], p[2], Blocks.GOLD_BLOCK.defaultBlockState());
        }
        setBlock(level, chunkBounds, 19, 5, 17, Blocks.SEA_LANTERN.defaultBlockState());
        setBlock(level, chunkBounds, 29, 5, 27, Blocks.SEA_LANTERN.defaultBlockState());
    }

    private static boolean isDoorway(int x, int z) {
        return (z == 16 || z == 28) && x >= 22 && x <= 26;
    }

    private void buildWhaleSkeleton(WorldGenLevel level, BoundingBox chunkBounds,
                                    int headX, int centerZ, boolean eastward) {
        int direction = eastward ? 1 : -1;
        BlockState boneX = Blocks.BONE_BLOCK.defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        BlockState boneY = Blocks.BONE_BLOCK.defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState boneZ = Blocks.BONE_BLOCK.defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
        for (int i = 0; i <= 19; i++) {
            int x = headX + direction * i;
            int y = 4 + (i > 14 ? (i - 14) / 3 : 0);
            setBlock(level, chunkBounds, x, y, centerZ, boneX);
            if (i >= 4 && i <= 15 && (i & 1) == 0) {
                for (int rib = 1; rib <= 4; rib++) {
                    int ribY = y - Math.max(0, rib - 2);
                    setBlock(level, chunkBounds, x, ribY, centerZ - rib, boneZ);
                    setBlock(level, chunkBounds, x, ribY, centerZ + rib, boneZ);
                }
            }
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) + Math.abs(dz) <= 3) {
                    setBlock(level, chunkBounds, headX + dx, 4 + (Math.abs(dx) == 2 ? 0 : 1), centerZ + dz, boneY);
                }
            }
        }
    }

    private void buildBrokenArches(WorldGenLevel level, BoundingBox chunkBounds) {
        int[] archX = {9, 39};
        for (int x : archX) {
            for (int z = 15; z <= 29; z += 7) {
                for (int y = 1; y <= 8; y++) {
                    if (!(x == 39 && y > 5 && z == 29)) {
                        setBlock(level, chunkBounds, x, y, z, Blocks.PRISMARINE_BRICKS.defaultBlockState());
                    }
                }
                for (int dx = -3; dx <= 3; dx++) {
                    int top = 8 + Math.max(0, 2 - Math.abs(dx));
                    setBlock(level, chunkBounds, x + dx, top, z, Blocks.PRISMARINE_BRICKS.defaultBlockState());
                }
            }
        }
    }

    private BlockPos worldPos(int x, int y, int z) {
        return new BlockPos(this.boundingBox.minX() + x, this.boundingBox.minY() + y,
                this.boundingBox.minZ() + z);
    }

    private void setBlock(WorldGenLevel level, BoundingBox chunkBounds, int x, int y, int z, BlockState state) {
        BlockPos pos = worldPos(x, y, z);
        if (chunkBounds.isInside(pos) && !level.getBlockState(pos).is(Blocks.BEDROCK)) {
            level.setBlock(pos, state, 2);
        }
    }
}
