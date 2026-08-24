package net.mysith.registry;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.mysith.item.AwakenedCrystalItem;
import net.mysith.item.BalanceScytheItem;
import net.mysith.item.CelestialScytheItem;
import net.mysith.item.DarkCrystalItem;
import net.mysith.item.LivingScytheItem;
import net.mysith.item.ScytheItem;
import net.mysith.item.SoulCodexItem;
import net.mysith.item.SoulCompassItem;
import net.mysith.item.SoulContainerItem;
import net.mysith.item.VoidboundScytheItem;
import net.mysith.item.VoidCoreItem;
import net.mysith.item.VoidSummonerItem;
import net.mysith.MySithMod;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MySithMod.MODID);

    public static final RegistryObject<Item> SITH_SCYTHE = ITEMS.register(
            "sith_scythe",
            () -> new ScytheItem(Tiers.NETHERITE, 13, -2.4F, new Item.Properties().stacksTo(1).fireResistant())
    );

    public static final RegistryObject<Item> BALANCE_SCYTHE = ITEMS.register(
            "balance_scythe",
            () -> new BalanceScytheItem(Tiers.NETHERITE, 15, -2.4F, new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))
    );

    public static final RegistryObject<Item> VOIDBOUND_SCYTHE = ITEMS.register(
            "voidbound_scythe",
            () -> new VoidboundScytheItem(Tiers.NETHERITE, 20, -2.35F, new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))
    );

    public static final RegistryObject<Item> CELESTIAL_SCYTHE = ITEMS.register(
            "celestial_scythe",
            () -> new CelestialScytheItem(Tiers.NETHERITE, 17, -2.2F, new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))
    );

    public static final RegistryObject<Item> LIVING_SCYTHE = ITEMS.register(
            "living_scythe",
            () -> new LivingScytheItem(Tiers.NETHERITE, 17, -2.3F, new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))
    );

    public static final RegistryObject<Item> SOUL_FRAGMENT = ITEMS.register(
            "soul_fragment",
            () -> new Item(new Item.Properties())
    );

    public static final RegistryObject<Item> SOUL_CRYSTAL = ITEMS.register(
            "soul_crystal",
            () -> new Item(new Item.Properties())
    );

    public static final RegistryObject<Item> DARK_CRYSTAL = ITEMS.register(
            "dark_crystal",
            () -> new DarkCrystalItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))
    );

    public static final RegistryObject<Item> CELESTIAL_CRYSTAL = ITEMS.register(
            "celestial_crystal",
            () -> new Item(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)) {
                @Override
                public boolean isFoil(net.minecraft.world.item.ItemStack stack) {
                    return true;
                }
            }
    );

    public static final RegistryObject<Item> AWAKENED_CELESTIAL_CRYSTAL = ITEMS.register(
            "awakened_celestial_crystal",
            () -> new AwakenedCrystalItem("item.usless_mobs.awakened_celestial_crystal.tooltip",
                    new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))
    );

    public static final RegistryObject<Item> VOID_SUMMONER = ITEMS.register(
            "void_summoner",
            () -> new VoidSummonerItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))
    );

    public static final RegistryObject<Item> VOID_CRYSTAL = ITEMS.register(
            "void_crystal",
            () -> new Item(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)) {
                @Override
                public boolean isFoil(net.minecraft.world.item.ItemStack stack) {
                    return true;
                }
            }
    );

    public static final RegistryObject<Item> AWAKENED_VOID_CRYSTAL = ITEMS.register(
            "awakened_void_crystal",
            () -> new AwakenedCrystalItem("item.usless_mobs.awakened_void_crystal.tooltip",
                    new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))
    );

    public static final RegistryObject<Item> VOID_CORE = ITEMS.register(
            "void_core",
            () -> new VoidCoreItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))
    );

    public static final RegistryObject<Item> SOUL_CONTAINER = ITEMS.register(
            "soul_container",
            () -> new SoulContainerItem(new Item.Properties().stacksTo(1))
    );

    public static final RegistryObject<Item> SOUL_CODEX = ITEMS.register(
            "soul_codex",
            () -> new SoulCodexItem(new Item.Properties().stacksTo(1))
    );

    public static final RegistryObject<Item> SOUL_COMPASS = ITEMS.register(
            "soul_compass",
            () -> new SoulCompassItem(new Item.Properties().stacksTo(1))
    );

    // Spawn Egg für Soul Endermite (rot innen, lila aussen)
    public static final RegistryObject<Item> SOUL_ENDERMITE_SPAWN_EGG = ITEMS.register(
            "soul_endermite_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.SOUL_ENDERMITE,
                    0xC8141E,  // primary color: crimson red
                    0x4B0082,  // secondary color: deep purple
                    new Item.Properties())
    );

    public static boolean isReaperScythe(ItemStack stack) {
        return stack.is(SITH_SCYTHE.get()) || stack.is(VOIDBOUND_SCYTHE.get())
                || stack.is(CELESTIAL_SCYTHE.get()) || stack.is(BALANCE_SCYTHE.get())
                || stack.is(LIVING_SCYTHE.get());
    }
}
