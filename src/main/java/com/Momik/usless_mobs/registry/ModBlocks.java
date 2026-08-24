package com.Momik.usless_mobs.registry;

import com.Momik.usless_mobs.allegiance.AllegiancePath;
import com.Momik.usless_mobs.block.AllegianceAltarBlock;
import com.Momik.usless_mobs.block.BlueSlimeBlock;
import com.Momik.usless_mobs.block.CelestialAetherBlock;
import com.Momik.usless_mobs.block.GoldenSlimeBlock;
import com.Momik.usless_mobs.block.KingSlimeTrophyBlock;
import com.Momik.usless_mobs.block.LivingCrystalBlock;
import com.Momik.usless_mobs.block.VoidFragmentBlock;
import com.Momik.usless_mobs.Usless_mobs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Usless_mobs.MODID);

    public static final RegistryObject<Block> BLAUER_SCHLEIMBLOCK = BLOCKS.register("blauer_schleimblock",
            () -> new BlueSlimeBlock(BlockBehaviour.Properties.copy(Blocks.SLIME_BLOCK).strength(0.2F).sound(SoundType.SLIME_BLOCK)));

    public static final RegistryObject<Block> GOLDENER_SCHLEIMBLOCK = BLOCKS.register("goldener_schleimblock",
            () -> new GoldenSlimeBlock(BlockBehaviour.Properties.copy(Blocks.SLIME_BLOCK).strength(0.35F).sound(SoundType.SLIME_BLOCK).lightLevel(state -> 7)));

    public static final RegistryObject<Block> KING_SLIME_TROPHY_BLOCK = BLOCKS.register("king_slime_trophy",
            () -> new KingSlimeTrophyBlock(BlockBehaviour.Properties.copy(Blocks.GOLD_BLOCK).strength(3.0F, 6.0F).sound(SoundType.METAL).lightLevel(state -> 5).noOcclusion()));

    public static final RegistryObject<Block> VOID_ALTAR = BLOCKS.register("void_altar",
            () -> new AllegianceAltarBlock(AllegiancePath.VOID,
                    BlockBehaviour.Properties.copy(Blocks.CRYING_OBSIDIAN).strength(8.0F, 18.0F).sound(SoundType.SCULK).lightLevel(state -> 5)));

    public static final RegistryObject<Block> CELESTIAL_ALTAR = BLOCKS.register("celestial_altar",
            () -> new AllegianceAltarBlock(AllegiancePath.CELESTIAL,
                    BlockBehaviour.Properties.copy(Blocks.AMETHYST_BLOCK).strength(6.0F, 12.0F).sound(SoundType.AMETHYST).lightLevel(state -> 10)));

    public static final RegistryObject<Block> LIVING_ALTAR = BLOCKS.register("living_altar",
            () -> new AllegianceAltarBlock(AllegiancePath.LIVING,
                    BlockBehaviour.Properties.copy(Blocks.ROOTED_DIRT).strength(4.0F, 9.0F).sound(SoundType.ROOTED_DIRT).lightLevel(state -> 4)));

    // Dekorative Kristall-Blöcke (thematisch passend zu den Pfad-Varianten)
    public static final RegistryObject<Block> LIVING_CRYSTAL_BLOCK = BLOCKS.register("living_crystal_block",
            () -> new LivingCrystalBlock(BlockBehaviour.Properties.copy(Blocks.AMETHYST_BLOCK)
                    .strength(1.5F, 3.0F).sound(SoundType.AMETHYST).lightLevel(state -> 8).noOcclusion()));

    public static final RegistryObject<Block> VOID_FRAGMENT_BLOCK = BLOCKS.register("void_fragment_block",
            () -> new VoidFragmentBlock(BlockBehaviour.Properties.copy(Blocks.OBSIDIAN)
                    .strength(3.5F, 7.0F).sound(SoundType.SCULK).lightLevel(state -> 5)));

    public static final RegistryObject<Block> CELESTIAL_AETHER_BLOCK = BLOCKS.register("celestial_aether_block",
            () -> new CelestialAetherBlock(BlockBehaviour.Properties.copy(Blocks.AMETHYST_BLOCK)
                    .strength(2.0F, 4.0F).sound(SoundType.AMETHYST).lightLevel(state -> 12).noOcclusion()));
}
