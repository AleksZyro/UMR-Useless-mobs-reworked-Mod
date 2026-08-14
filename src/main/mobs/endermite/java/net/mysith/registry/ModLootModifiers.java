package net.mysith.registry;

import com.mojang.serialization.Codec;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.mysith.loot.AncientCityLootModifier;
import net.mysith.loot.CodexLootModifier;
import net.mysith.loot.EndShipLootModifier;
import net.mysith.loot.LibraryLootModifier;
import net.mysith.MySithMod;

public class ModLootModifiers {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MySithMod.MODID);

    public static final RegistryObject<Codec<? extends IGlobalLootModifier>> END_SHIP_BOOKS =
            LOOT_MODIFIERS.register("end_ship_books", EndShipLootModifier.CODEC);

    public static final RegistryObject<Codec<? extends IGlobalLootModifier>> LIBRARY_BOOKS =
            LOOT_MODIFIERS.register("library_books", LibraryLootModifier.CODEC);

    public static final RegistryObject<Codec<? extends IGlobalLootModifier>> ANCIENT_CITY_BOOKS =
            LOOT_MODIFIERS.register("ancient_city_books", AncientCityLootModifier.CODEC);

    public static final RegistryObject<Codec<? extends IGlobalLootModifier>> CODEX_BOOKS =
            LOOT_MODIFIERS.register("codex_books", CodexLootModifier.CODEC);
}
