package com.Momik.usless_mobs.worldgen;

import com.Momik.usless_mobs.Usless_mobs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, Usless_mobs.MODID);
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, Usless_mobs.MODID);

    public static final RegistryObject<StructureType<AncientWhaleRuinStructure>> ANCIENT_WHALE_RUIN =
            STRUCTURE_TYPES.register("ancient_whale_ruin", () -> () -> AncientWhaleRuinStructure.CODEC);
    public static final RegistryObject<StructurePieceType> ANCIENT_WHALE_RUIN_PIECE =
            STRUCTURE_PIECE_TYPES.register("ancient_whale_ruin_piece",
                    () -> (StructurePieceType.ContextlessType) AncientWhaleRuinPiece::new);

    public static final ResourceKey<Structure> ANCIENT_WHALE_RUIN_KEY = ResourceKey.create(
            Registries.STRUCTURE,
            ResourceLocation.tryBuild(Usless_mobs.MODID, "ancient_whale_ruin"));

    private ModStructures() {
    }
}
