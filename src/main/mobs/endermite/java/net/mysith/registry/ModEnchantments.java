package net.mysith.registry;

import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.mysith.enchantment.BeheadingEnchantment;
import net.mysith.enchantment.CrimsonEdgeEnchantment;
import net.mysith.enchantment.DeathMarkEnchantment;
import net.mysith.enchantment.ReapingEnchantment;
import net.mysith.enchantment.SoulDrainEnchantment;
import net.mysith.enchantment.WhirlwindMasterEnchantment;
import net.mysith.MySithMod;

public class ModEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, MySithMod.MODID);

    public static final RegistryObject<Enchantment> BEHEADING =
            ENCHANTMENTS.register("beheading", BeheadingEnchantment::new);

    public static final RegistryObject<Enchantment> SOUL_DRAIN =
            ENCHANTMENTS.register("soul_drain", SoulDrainEnchantment::new);

    public static final RegistryObject<Enchantment> REAPING =
            ENCHANTMENTS.register("reaping", ReapingEnchantment::new);

    public static final RegistryObject<Enchantment> CRIMSON_EDGE =
            ENCHANTMENTS.register("crimson_edge", CrimsonEdgeEnchantment::new);

    public static final RegistryObject<Enchantment> WHIRLWIND_MASTER =
            ENCHANTMENTS.register("whirlwind_master", WhirlwindMasterEnchantment::new);

    public static final RegistryObject<Enchantment> DEATH_MARK =
            ENCHANTMENTS.register("death_mark", DeathMarkEnchantment::new);
}
