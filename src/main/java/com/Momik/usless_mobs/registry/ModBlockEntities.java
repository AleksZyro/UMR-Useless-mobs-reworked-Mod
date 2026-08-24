package com.Momik.usless_mobs.registry;

import com.Momik.usless_mobs.block.KingSlimeTrophyBlockEntity;
import com.Momik.usless_mobs.Usless_mobs;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    private ModBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Usless_mobs.MODID);

    public static final RegistryObject<BlockEntityType<KingSlimeTrophyBlockEntity>> KING_SLIME_TROPHY_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("king_slime_trophy",
            () -> BlockEntityType.Builder.of(KingSlimeTrophyBlockEntity::new, ModBlocks.KING_SLIME_TROPHY_BLOCK.get()).build(null));
}
