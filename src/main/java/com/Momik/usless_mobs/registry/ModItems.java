package com.Momik.usless_mobs.registry;

import com.Momik.usless_mobs.item.ArmorOfBalanceItem;
import com.Momik.usless_mobs.item.AwakenedBearclawNecklaceItem;
import com.Momik.usless_mobs.item.BearclawNecklaceItem;
import com.Momik.usless_mobs.item.CrystalToolTier;
import com.Momik.usless_mobs.item.CrystalUpgradeTemplateItem;
import com.Momik.usless_mobs.item.CrownForm;
import com.Momik.usless_mobs.item.GlowbaitFishingRodItem;
import com.Momik.usless_mobs.item.GlowFlareItem;
import com.Momik.usless_mobs.item.GoldenSlimeSpawnEggItem;
import com.Momik.usless_mobs.item.IceArrowItem;
import com.Momik.usless_mobs.item.KingSlimeCrownItem;
import com.Momik.usless_mobs.item.KingSlimeSpawnerItem;
import com.Momik.usless_mobs.item.KingSlimeTrophyItem;
import com.Momik.usless_mobs.item.LivingCrystalHelmetItem;
import com.Momik.usless_mobs.item.LivingCrystalItem;
import com.Momik.usless_mobs.item.LivingRootBootsItem;
import com.Momik.usless_mobs.item.NatureCrystalItem;
import com.Momik.usless_mobs.item.NatureRelicItem;
import com.Momik.usless_mobs.item.NetheriteCrownItem;
import com.Momik.usless_mobs.item.PathCrownItem;
import com.Momik.usless_mobs.item.PathTalismanItem;
import com.Momik.usless_mobs.item.TruePathSwordItem;
import com.Momik.usless_mobs.item.TrueLivingAxeItem;
import com.Momik.usless_mobs.item.NetheriteSlimeCoreItem;
import com.Momik.usless_mobs.item.NetheriteSlimeCoreSwordItem;
import com.Momik.usless_mobs.item.PotionOfLifeItem;
import com.Momik.usless_mobs.item.SlimeCompassItem;
import com.Momik.usless_mobs.item.SlimeCoreItem;
import com.Momik.usless_mobs.item.SlimeCoreSwordItem;
import com.Momik.usless_mobs.item.SlimeReactorChestplateItem;
import com.Momik.usless_mobs.item.TentacleItem;
import com.Momik.usless_mobs.item.TrueCrownItem;
import com.Momik.usless_mobs.item.TruePathArmorItem;
import com.Momik.usless_mobs.item.UpgradedSlimeCoreSwordItem;
import com.Momik.usless_mobs.item.VoidCrystalHelmetItem;
import com.Momik.usless_mobs.Usless_mobs;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.mysith.silverfish.CorruptedCrystalLeggingsItem;
import net.mysith.silverfish.CorruptionResonatorItem;
import net.mysith.silverfish.InfestedBaitItem;
import net.mysith.silverfish.SilverDustBombItem;
import net.mysith.silverfish.SilverFlareItem;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Usless_mobs.MODID);

    public static final RegistryObject<Item> BLAUER_SCHLEIMBALL = ITEMS.register("blauer_schleimball",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> GOLDENER_SCHLEIMBALL = ITEMS.register("goldener_schleimball", () -> new Item(new Item.Properties()) {
        @Override
        public boolean isFoil(net.minecraft.world.item.ItemStack stack) {
            return true;
        }
    });

    public static final RegistryObject<Item> SCHLEIMKERN = ITEMS.register("schleimkern",
            () -> new SlimeCoreItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> NETHERITE_SCHLEIMKERN = ITEMS.register("netherite_schleimkern",
            () -> new NetheriteSlimeCoreItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> SCHLEIMREAKTOR_SCHMIEDEVORLAGE = ITEMS.register("schleimreaktor_schmiedevorlage",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistryObject<Item> SCHLEIMREAKTOR_BRUSTPANZER = ITEMS.register("schleimreaktor_brustpanzer",
            () -> new SlimeReactorChestplateItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> KING_SLIME_KRONE = ITEMS.register("king_slime_krone",
            () -> new KingSlimeCrownItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> NETHERITE_KINGS_KRONE = ITEMS.register("netherite_kings_krone",
            () -> new NetheriteCrownItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> SCHLEIMKERN_SCHWERT = ITEMS.register("schleimkern_schwert",
            () -> new SlimeCoreSwordItem(new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistryObject<Item> NETHERITE_SLIME_CORE_SWORD = ITEMS.register("netherite_slime_core_sword",
            () -> new NetheriteSlimeCoreSwordItem(new Item.Properties().fireResistant().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> VOID_SLIME_CORE_SWORD = ITEMS.register("void_slime_core_sword",
            () -> new UpgradedSlimeCoreSwordItem(CrystalToolTier.VOID, 6, -1.85F, UpgradedSlimeCoreSwordItem.Path.VOID,
                    new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> CELESTIAL_SLIME_CORE_SWORD = ITEMS.register("celestial_slime_core_sword",
            () -> new UpgradedSlimeCoreSwordItem(CrystalToolTier.CELESTIAL, 7, -1.8F, UpgradedSlimeCoreSwordItem.Path.CELESTIAL,
                    new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> BALANCE_SLIME_CORE_SWORD = ITEMS.register("balance_slime_core_sword",
            () -> new UpgradedSlimeCoreSwordItem(CrystalToolTier.BALANCE, 8, -1.75F, UpgradedSlimeCoreSwordItem.Path.BALANCE,
                    new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> SLIME_KOMPASS = ITEMS.register("slime_kompass",
            () -> new SlimeCompassItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> BEAR_CLAW = ITEMS.register("bear_claw",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> BEARCLAW_NECKLACE = ITEMS.register("bearclaw_necklace",
            () -> new BearclawNecklaceItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> AWAKENED_BEARCLAW_NECKLACE = ITEMS.register("awakened_bearclaw_necklace",
            () -> new AwakenedBearclawNecklaceItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> GLOWBAIT_FISHING_ROD = ITEMS.register("glowbait_fishing_rod",
            () -> new GlowbaitFishingRodItem(new Item.Properties().stacksTo(1).durability(128).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> ICE_ARROW = ITEMS.register("ice_arrow",
            () -> new IceArrowItem(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> AXOLOTL_GILLS = ITEMS.register("axolotl_gills",
            () -> new NatureRelicItem(NatureRelicItem.Relic.AXOLOTL_GILLS, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> BAT_WING = ITEMS.register("bat_wing",
            () -> new NatureRelicItem(NatureRelicItem.Relic.BAT_WING, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> SHADOWTOOTH = ITEMS.register("shadowtooth",
            () -> new NatureRelicItem(NatureRelicItem.Relic.SHADOWTOOTH, new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> TENTACLE = ITEMS.register("tentacle",
            () -> new TentacleItem(new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder()
                    .nutrition(3).saturationMod(0.35F).meat().build()).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> GLOW_FLARE = ITEMS.register("glow_flare",
            () -> new GlowFlareItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> POTION_OF_LIFE = ITEMS.register("potion_of_life",
            () -> new PotionOfLifeItem(new Item.Properties().stacksTo(8).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> CORAL_SCALE = ITEMS.register("coral_scale",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> NATURE_CRYSTAL = ITEMS.register("nature_crystal",
            () -> new NatureCrystalItem(new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistryObject<Item> LIVING_TISSUE = ITEMS.register("living_tissue",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> FROST_CORE = ITEMS.register("frost_core",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)) {
                @Override
                public boolean isFoil(ItemStack stack) {
                    return true;
                }
            });

    public static final RegistryObject<Item> LIVING_CORE = ITEMS.register("living_core",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)) {
                @Override
                public boolean isFoil(ItemStack stack) {
                    return true;
                }
            });

    public static final RegistryObject<Item> LIVING_CRYSTAL = ITEMS.register("living_crystal",
            () -> new LivingCrystalItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> AWAKENED_LIVING_CRYSTAL = ITEMS.register("awakened_living_crystal",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()) {
                @Override
                public boolean isFoil(ItemStack stack) {
                    return true;
                }
            });

    public static final RegistryObject<Item> HELPING_AMETHYST = ITEMS.register("helping_amethyst",
            () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)) {
                @Override
                public boolean isFoil(ItemStack stack) {
                    return true;
                }
            });

    public static final RegistryObject<Item> HELPING_SOUL = ITEMS.register("helping_soul",
            () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)) {
                @Override
                public boolean isFoil(ItemStack stack) {
                    return true;
                }
            });

    public static final RegistryObject<Item> LIVING_CRYSTAL_HELMET = ITEMS.register("living_crystal_helmet",
            () -> new LivingCrystalHelmetItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> LIVING_ROOT_BOOTS = ITEMS.register("living_root_boots",
            () -> new LivingRootBootsItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> TRUE_CROWN = ITEMS.register("true_crown",
            () -> new TrueCrownItem(TrueCrownItem.Path.BALANCED, CrownForm.COMBAT, crownProperties()));

    public static final RegistryObject<Item> VOID_REAPER_KING = ITEMS.register("void_reaper_king",
            () -> new PathCrownItem(PathCrownItem.Path.VOID, CrownForm.COMBAT, crownProperties()));
    public static final RegistryObject<Item> GOD_KING = ITEMS.register("god_king",
            () -> new PathCrownItem(PathCrownItem.Path.CELESTIAL, CrownForm.COMBAT, crownProperties()));
    public static final RegistryObject<Item> LIVING_KING = ITEMS.register("living_king",
            () -> new PathCrownItem(PathCrownItem.Path.LIVING, CrownForm.COMBAT, crownProperties()));
    public static final RegistryObject<Item> ROYAL_VOID_CROWN = ITEMS.register("royal_void_crown",
            () -> new PathCrownItem(PathCrownItem.Path.VOID, CrownForm.ROYAL, crownProperties()));
    public static final RegistryObject<Item> ROYAL_CELESTIAL_CROWN = ITEMS.register("royal_celestial_crown",
            () -> new PathCrownItem(PathCrownItem.Path.CELESTIAL, CrownForm.ROYAL, crownProperties()));
    public static final RegistryObject<Item> ROYAL_LIVING_CROWN = ITEMS.register("royal_living_crown",
            () -> new PathCrownItem(PathCrownItem.Path.LIVING, CrownForm.ROYAL, crownProperties()));
    public static final RegistryObject<Item> ROYAL_BALANCE_CROWN = ITEMS.register("royal_balance_crown",
            () -> new TrueCrownItem(TrueCrownItem.Path.BALANCED, CrownForm.ROYAL, crownProperties()));

    public static final RegistryObject<Item> VOID_VITALITY_TEMPLATE = ITEMS.register("void_vitality_template",
            () -> new CrystalUpgradeTemplateItem("item.usless_mobs.void_vitality_template.tooltip",
                    new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> CELESTIAL_VITALITY_TEMPLATE = ITEMS.register("celestial_vitality_template",
            () -> new CrystalUpgradeTemplateItem("item.usless_mobs.celestial_vitality_template.tooltip",
                    new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> BALANCE_UPGRADE_TEMPLATE = ITEMS.register("balance_upgrade_template",
            () -> new CrystalUpgradeTemplateItem("item.usless_mobs.balance_upgrade_template.tooltip",
                    new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> TRUE_VOID_TEMPLATE = ITEMS.register("true_void_template",
            () -> new CrystalUpgradeTemplateItem("item.usless_mobs.true_void_template.tooltip",
                    new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> TRUE_CELESTIAL_TEMPLATE = ITEMS.register("true_celestial_template",
            () -> new CrystalUpgradeTemplateItem("item.usless_mobs.true_celestial_template.tooltip",
                    new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> TRUE_LIVING_TEMPLATE = ITEMS.register("true_living_template",
            () -> new CrystalUpgradeTemplateItem("item.usless_mobs.true_living_template.tooltip",
                    new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> BALANCE_CATALYST = ITEMS.register("balance_catalyst",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()) {
                @Override
                public boolean isFoil(ItemStack stack) {
                    return true;
                }

                @Override
                public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
                    tooltip.add(Component.translatable("item.usless_mobs.balance_catalyst.tooltip").withStyle(ChatFormatting.GOLD));
                }
            });

    public static final RegistryObject<Item> VOID_CRYSTAL_HELMET = ITEMS.register("void_crystal_helmet",
            () -> new VoidCrystalHelmetItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> ARMOR_OF_BALANCE_HELMET = ITEMS.register("armor_of_balance_helmet",
            () -> new ArmorOfBalanceItem(ArmorItem.Type.HELMET, new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> ARMOR_OF_BALANCE_CHESTPLATE = ITEMS.register("armor_of_balance_chestplate",
            () -> new ArmorOfBalanceItem(ArmorItem.Type.CHESTPLATE, new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> ARMOR_OF_BALANCE_LEGGINGS = ITEMS.register("armor_of_balance_leggings",
            () -> new ArmorOfBalanceItem(ArmorItem.Type.LEGGINGS, new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> ARMOR_OF_BALANCE_BOOTS = ITEMS.register("armor_of_balance_boots",
            () -> new ArmorOfBalanceItem(ArmorItem.Type.BOOTS, new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> TRUE_VOID_HELMET = ITEMS.register("true_void_helmet",
            () -> new TruePathArmorItem(TruePathArmorItem.Path.VOID, ArmorItem.Type.HELMET, new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> TRUE_VOID_CHESTPLATE = ITEMS.register("true_void_chestplate",
            () -> new TruePathArmorItem(TruePathArmorItem.Path.VOID, ArmorItem.Type.CHESTPLATE, new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> TRUE_VOID_LEGGINGS = ITEMS.register("true_void_leggings",
            () -> new TruePathArmorItem(TruePathArmorItem.Path.VOID, ArmorItem.Type.LEGGINGS, new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> TRUE_VOID_BOOTS = ITEMS.register("true_void_boots",
            () -> new TruePathArmorItem(TruePathArmorItem.Path.VOID, ArmorItem.Type.BOOTS, new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> TRUE_CELESTIAL_HELMET = ITEMS.register("true_celestial_helmet",
            () -> new TruePathArmorItem(TruePathArmorItem.Path.CELESTIAL, ArmorItem.Type.HELMET, new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> TRUE_CELESTIAL_CHESTPLATE = ITEMS.register("true_celestial_chestplate",
            () -> new TruePathArmorItem(TruePathArmorItem.Path.CELESTIAL, ArmorItem.Type.CHESTPLATE, new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> TRUE_CELESTIAL_LEGGINGS = ITEMS.register("true_celestial_leggings",
            () -> new TruePathArmorItem(TruePathArmorItem.Path.CELESTIAL, ArmorItem.Type.LEGGINGS, new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> TRUE_CELESTIAL_BOOTS = ITEMS.register("true_celestial_boots",
            () -> new TruePathArmorItem(TruePathArmorItem.Path.CELESTIAL, ArmorItem.Type.BOOTS, new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> TRUE_LIVING_HELMET = ITEMS.register("true_living_helmet",
            () -> new TruePathArmorItem(TruePathArmorItem.Path.LIVING, ArmorItem.Type.HELMET, new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> TRUE_LIVING_CHESTPLATE = ITEMS.register("true_living_chestplate",
            () -> new TruePathArmorItem(TruePathArmorItem.Path.LIVING, ArmorItem.Type.CHESTPLATE, new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> TRUE_LIVING_LEGGINGS = ITEMS.register("true_living_leggings",
            () -> new TruePathArmorItem(TruePathArmorItem.Path.LIVING, ArmorItem.Type.LEGGINGS, new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> TRUE_LIVING_BOOTS = ITEMS.register("true_living_boots",
            () -> new TruePathArmorItem(TruePathArmorItem.Path.LIVING, ArmorItem.Type.BOOTS, new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> VOIDBOUND_AXE = ITEMS.register("voidbound_axe",
            () -> new AxeItem(CrystalToolTier.VOID, 7.5F, -2.9F, crystalToolProperties()) {
                @Override public boolean isFoil(ItemStack stack) { return true; }
                @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) { addCrystalToolTooltip(tooltip, "item.usless_mobs.voidbound_tool.tooltip", ChatFormatting.DARK_PURPLE); }
            });

    public static final RegistryObject<Item> VOIDBOUND_PICKAXE = ITEMS.register("voidbound_pickaxe",
            () -> new PickaxeItem(CrystalToolTier.VOID, 3, -2.7F, crystalToolProperties()) {
                @Override public boolean isFoil(ItemStack stack) { return true; }
                @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) { addCrystalToolTooltip(tooltip, "item.usless_mobs.voidbound_tool.tooltip", ChatFormatting.DARK_PURPLE); }
            });

    public static final RegistryObject<Item> VOIDBOUND_SHOVEL = ITEMS.register("voidbound_shovel",
            () -> new ShovelItem(CrystalToolTier.VOID, 3.5F, -2.9F, crystalToolProperties()) {
                @Override public boolean isFoil(ItemStack stack) { return true; }
                @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) { addCrystalToolTooltip(tooltip, "item.usless_mobs.voidbound_tool.tooltip", ChatFormatting.DARK_PURPLE); }
            });

    public static final RegistryObject<Item> VOIDBOUND_HOE = ITEMS.register("voidbound_hoe",
            () -> new HoeItem(CrystalToolTier.VOID, -3, 0.0F, crystalToolProperties()) {
                @Override public boolean isFoil(ItemStack stack) { return true; }
                @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) { addCrystalToolTooltip(tooltip, "item.usless_mobs.voidbound_tool.tooltip", ChatFormatting.DARK_PURPLE); }
            });

    public static final RegistryObject<Item> VOIDBOUND_SHIELD = ITEMS.register("voidbound_shield",
            () -> new ShieldItem(new Item.Properties().durability(960).fireResistant().rarity(Rarity.EPIC)) {
                @Override public boolean isFoil(ItemStack stack) { return true; }
                @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) { addCrystalToolTooltip(tooltip, "item.usless_mobs.voidbound_shield.tooltip", ChatFormatting.DARK_PURPLE); }
            });

    public static final RegistryObject<Item> CELESTIAL_AXE = ITEMS.register("celestial_axe",
            () -> new AxeItem(CrystalToolTier.CELESTIAL, 7.0F, -2.85F, crystalToolProperties()) {
                @Override public boolean isFoil(ItemStack stack) { return true; }
                @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) { addCrystalToolTooltip(tooltip, "item.usless_mobs.celestial_tool.tooltip", ChatFormatting.AQUA); }
            });

    public static final RegistryObject<Item> CELESTIAL_PICKAXE = ITEMS.register("celestial_pickaxe",
            () -> new PickaxeItem(CrystalToolTier.CELESTIAL, 3, -2.65F, crystalToolProperties()) {
                @Override public boolean isFoil(ItemStack stack) { return true; }
                @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) { addCrystalToolTooltip(tooltip, "item.usless_mobs.celestial_tool.tooltip", ChatFormatting.AQUA); }
            });

    public static final RegistryObject<Item> CELESTIAL_SHOVEL = ITEMS.register("celestial_shovel",
            () -> new ShovelItem(CrystalToolTier.CELESTIAL, 3.0F, -2.85F, crystalToolProperties()) {
                @Override public boolean isFoil(ItemStack stack) { return true; }
                @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) { addCrystalToolTooltip(tooltip, "item.usless_mobs.celestial_tool.tooltip", ChatFormatting.AQUA); }
            });

    public static final RegistryObject<Item> CELESTIAL_HOE = ITEMS.register("celestial_hoe",
            () -> new HoeItem(CrystalToolTier.CELESTIAL, -4, 0.5F, crystalToolProperties()) {
                @Override public boolean isFoil(ItemStack stack) { return true; }
                @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) { addCrystalToolTooltip(tooltip, "item.usless_mobs.celestial_tool.tooltip", ChatFormatting.AQUA); }
            });

    public static final RegistryObject<Item> CELESTIAL_SHIELD = ITEMS.register("celestial_shield",
            () -> new ShieldItem(new Item.Properties().durability(1200).fireResistant().rarity(Rarity.EPIC)) {
                @Override public boolean isFoil(ItemStack stack) { return true; }
                @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) { addCrystalToolTooltip(tooltip, "item.usless_mobs.celestial_shield.tooltip", ChatFormatting.AQUA); }
            });

    public static final RegistryObject<Item> LIVING_AXE = ITEMS.register("living_axe",
            () -> new AxeItem(CrystalToolTier.LIVING, 7.0F, -3.0F, crystalToolProperties()) {
                @Override public boolean isFoil(ItemStack stack) { return true; }
                @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) { addCrystalToolTooltip(tooltip, "item.usless_mobs.living_tool.tooltip", ChatFormatting.GREEN); }
            });

    public static final RegistryObject<Item> LIVING_PICKAXE = ITEMS.register("living_pickaxe",
            () -> new PickaxeItem(CrystalToolTier.LIVING, 3, -2.75F, crystalToolProperties()) {
                @Override public boolean isFoil(ItemStack stack) { return true; }
                @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) { addCrystalToolTooltip(tooltip, "item.usless_mobs.living_tool.tooltip", ChatFormatting.GREEN); }
            });

    public static final RegistryObject<Item> LIVING_SHOVEL = ITEMS.register("living_shovel",
            () -> new ShovelItem(CrystalToolTier.LIVING, 3.0F, -3.0F, crystalToolProperties()) {
                @Override public boolean isFoil(ItemStack stack) { return true; }
                @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) { addCrystalToolTooltip(tooltip, "item.usless_mobs.living_tool.tooltip", ChatFormatting.GREEN); }
            });

    public static final RegistryObject<Item> LIVING_HOE = ITEMS.register("living_hoe",
            () -> new HoeItem(CrystalToolTier.LIVING, -4, 0.5F, crystalToolProperties()) {
                @Override public boolean isFoil(ItemStack stack) { return true; }
                @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) { addCrystalToolTooltip(tooltip, "item.usless_mobs.living_tool.tooltip", ChatFormatting.GREEN); }
            });

    public static final RegistryObject<Item> LIVING_SHIELD = ITEMS.register("living_shield",
            () -> new ShieldItem(new Item.Properties().durability(1800).fireResistant().rarity(Rarity.EPIC)) {
                @Override public boolean isFoil(ItemStack stack) { return true; }
                @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) { addCrystalToolTooltip(tooltip, "item.usless_mobs.living_shield.tooltip", ChatFormatting.GREEN); }
            });

    public static final RegistryObject<Item> BALANCE_AXE = ITEMS.register("balance_axe",
            () -> new AxeItem(CrystalToolTier.BALANCE, 6.0F, -3.0F, crystalToolProperties()) {
                @Override public boolean isFoil(ItemStack stack) { return true; }
                @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) { addCrystalToolTooltip(tooltip, "item.usless_mobs.balance_tool.tooltip", ChatFormatting.GOLD); }
            });

    public static final RegistryObject<Item> BALANCE_PICKAXE = ITEMS.register("balance_pickaxe",
            () -> new PickaxeItem(CrystalToolTier.BALANCE, 3, -2.8F, crystalToolProperties()) {
                @Override public boolean isFoil(ItemStack stack) { return true; }
                @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) { addCrystalToolTooltip(tooltip, "item.usless_mobs.balance_tool.tooltip", ChatFormatting.GOLD); }
            });

    public static final RegistryObject<Item> BALANCE_SHOVEL = ITEMS.register("balance_shovel",
            () -> new ShovelItem(CrystalToolTier.BALANCE, 2.5F, -3.0F, crystalToolProperties()) {
                @Override public boolean isFoil(ItemStack stack) { return true; }
                @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) { addCrystalToolTooltip(tooltip, "item.usless_mobs.balance_tool.tooltip", ChatFormatting.GOLD); }
            });

    public static final RegistryObject<Item> BALANCE_HOE = ITEMS.register("balance_hoe",
            () -> new HoeItem(CrystalToolTier.BALANCE, -5, 0.5F, crystalToolProperties()) {
                @Override public boolean isFoil(ItemStack stack) { return true; }
                @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) { addCrystalToolTooltip(tooltip, "item.usless_mobs.balance_tool.tooltip", ChatFormatting.GOLD); }
            });

    public static final RegistryObject<Item> BALANCE_SHIELD = ITEMS.register("balance_shield",
            () -> new ShieldItem(new Item.Properties().durability(1100).fireResistant().rarity(Rarity.EPIC)) {
                @Override public boolean isFoil(ItemStack stack) { return true; }
                @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) { addCrystalToolTooltip(tooltip, "item.usless_mobs.balance_shield.tooltip", ChatFormatting.GOLD); }
            });

    public static final RegistryObject<Item> KING_SLIME_SPAWNER = ITEMS.register("king_slime_spawner",
            () -> new KingSlimeSpawnerItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> KING_SLIME_TROPHY = ITEMS.register("king_slime_trophy",
            () -> new KingSlimeTrophyItem(ModBlocks.KING_SLIME_TROPHY_BLOCK.get(), new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> BLAUER_SCHLEIMBLOCK_ITEM = registerBlockItem("blauer_schleimblock", ModBlocks.BLAUER_SCHLEIMBLOCK, new Item.Properties().rarity(Rarity.UNCOMMON));

    public static final RegistryObject<Item> GOLDENER_SCHLEIMBLOCK_ITEM = registerBlockItem("goldener_schleimblock", ModBlocks.GOLDENER_SCHLEIMBLOCK, new Item.Properties().rarity(Rarity.RARE));

    public static final RegistryObject<Item> VOID_ALTAR_ITEM = registerBlockItem("void_altar", ModBlocks.VOID_ALTAR, new Item.Properties().rarity(Rarity.EPIC).fireResistant());

    public static final RegistryObject<Item> CELESTIAL_ALTAR_ITEM = registerBlockItem("celestial_altar", ModBlocks.CELESTIAL_ALTAR, new Item.Properties().rarity(Rarity.EPIC).fireResistant());

    public static final RegistryObject<Item> LIVING_ALTAR_ITEM = registerBlockItem("living_altar", ModBlocks.LIVING_ALTAR, new Item.Properties().rarity(Rarity.EPIC).fireResistant());

    // Neue dekorative Kristall-Blöcke
    public static final RegistryObject<Item> LIVING_CRYSTAL_BLOCK_ITEM = registerBlockItem("living_crystal_block", ModBlocks.LIVING_CRYSTAL_BLOCK, new Item.Properties().rarity(Rarity.RARE));
    public static final RegistryObject<Item> VOID_FRAGMENT_BLOCK_ITEM  = registerBlockItem("void_fragment_block",  ModBlocks.VOID_FRAGMENT_BLOCK,  new Item.Properties().rarity(Rarity.RARE).fireResistant());
    public static final RegistryObject<Item> CELESTIAL_AETHER_BLOCK_ITEM = registerBlockItem("celestial_aether_block", ModBlocks.CELESTIAL_AETHER_BLOCK, new Item.Properties().rarity(Rarity.RARE));

    public static final RegistryObject<Item> BLAUER_SCHLEIM_SPAWN_EGG = ITEMS.register("blauer_schleim_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.BLAUER_SCHLEIM, 0x3D7DFF, 0xA7D3FF, new Item.Properties()));

    public static final RegistryObject<Item> GOLDENER_SCHLEIM_SPAWN_EGG = ITEMS.register("goldener_schleim_spawn_egg",
            () -> new GoldenSlimeSpawnEggItem(ModEntities.BLAUER_SCHLEIM, 0xFFD93D, 0xFFF1A8, new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistryObject<Item> KING_SCHLEIM_SPAWN_EGG = ITEMS.register("king_schleim_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.KING_SCHLEIM, 0x6A1FAE, 0xC8A2E0, new Item.Properties().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> VOID_SCHLEIMBALL = ITEMS.register("void_schleimball",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)) {
                @Override
                public boolean isFoil(net.minecraft.world.item.ItemStack stack) {
                    return true;
                }
            });

    // Chronik aller Pfade: buendelt die komplette Lore (7 alte Lore-Buecher,
    // neue Kapitel und den vollstaendigen Kodex) in einem lesbaren Buch.
    public static final RegistryObject<Item> LORE_TOME = ITEMS.register("lore_tome",
            () -> new com.Momik.usless_mobs.item.LoreTomeItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> ENDER_SCHLEIM_SPAWN_EGG = ITEMS.register("ender_schleim_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.ENDER_SCHLEIM, 0x1B0033, 0x6B1FAE, new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistryObject<Item> CELESTIAL_SLIME_SPAWN_EGG = ITEMS.register("celestial_slime_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.CELESTIAL_SLIME, 0xDDEBFF, 0x8A3DFF, new Item.Properties().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> CORRUPTED_CHITIN = ITEMS.register("corrupted_chitin",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> SILVER_DUST = ITEMS.register("silver_dust",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> INFESTED_STONE_FRAGMENT = ITEMS.register("infested_stone_fragment",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CORRUPTED_SHARD = ITEMS.register("corrupted_shard",
            () -> new Item(new Item.Properties().rarity(Rarity.EPIC)) {
                @Override
                public boolean isFoil(ItemStack stack) {
                    return true;
                }
            });

    public static final RegistryObject<Item> CORRUPTED_CRYSTAL = ITEMS.register("corrupted_crystal",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()) {
                @Override
                public boolean isFoil(ItemStack stack) {
                    return true;
                }
            });

    public static final RegistryObject<Item> CORRUPTION_RESONATOR = ITEMS.register("corruption_resonator",
            () -> new CorruptionResonatorItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> SILVER_FLARE = ITEMS.register("silver_flare",
            () -> new SilverFlareItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> SILVER_DUST_BOMB = ITEMS.register("silver_dust_bomb",
            () -> new SilverDustBombItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> INFESTED_BAIT = ITEMS.register("infested_bait",
            () -> new InfestedBaitItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> TRUE_VOID_SWORD = ITEMS.register("true_void_sword",
            () -> new TruePathSwordItem(CrystalToolTier.VOID, 7, -1.8F, TruePathSwordItem.Path.VOID,
                    new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> TRUE_CELESTIAL_SWORD = ITEMS.register("true_celestial_sword",
            () -> new TruePathSwordItem(CrystalToolTier.CELESTIAL, 7, -1.75F, TruePathSwordItem.Path.CELESTIAL,
                    new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> TRUE_LIVING_AXE = ITEMS.register("true_living_axe",
            () -> new TrueLivingAxeItem(CrystalToolTier.LIVING, 10.0F, -2.8F,
                    new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> VOID_TALISMAN = ITEMS.register("void_talisman",
            () -> new PathTalismanItem(PathTalismanItem.Path.VOID,
                    new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> CELESTIAL_TALISMAN = ITEMS.register("celestial_talisman",
            () -> new PathTalismanItem(PathTalismanItem.Path.CELESTIAL,
                    new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> LIVING_TALISMAN = ITEMS.register("living_talisman",
            () -> new PathTalismanItem(PathTalismanItem.Path.LIVING,
                    new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> CORRUPTED_CRYSTAL_LEGGINGS = ITEMS.register("corrupted_crystal_leggings",
            () -> new CorruptedCrystalLeggingsItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> CORRUPTED_SILVERFISH_SPAWN_EGG = ITEMS.register("corrupted_silverfish_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.CORRUPTED_SILVERFISH, 0x2A2033, 0xB66DFF, new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistryObject<Item> LIVING_BOSS_SPAWN_EGG = ITEMS.register("living_boss_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.LIVING_BOSS, 0x355E2B, 0xB8E986, new Item.Properties().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> FROST_STRAY_SPAWN_EGG = ITEMS.register("frost_stray_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.FROST_STRAY, 0xD7F7FF, 0x3D8CFF, new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistryObject<Item> WEB_CAVE_SPIDER_SPAWN_EGG = ITEMS.register("web_cave_spider_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.WEB_CAVE_SPIDER, 0x16332A, 0xBDEBE4, new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistryObject<Item> CORAL_DROWNED_SPAWN_EGG = ITEMS.register("coral_drowned_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.CORAL_DROWNED, 0x204F5E, 0xE5798D, new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistryObject<Item> OCTOPUS_SPAWN_EGG = ITEMS.register("octopus_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.OCTOPUS, 0x2F1B45, 0xD28AA8, new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistryObject<Item> WITCH_BOSS_SPAWN_EGG = ITEMS.register("witch_boss_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.WITCH_BOSS, 0x2B1433, 0xB95CFF, new Item.Properties().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> HELPING_ALLAY_SPAWN_EGG = ITEMS.register("helping_allay_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.HELPING_ALLAY, 0x67DDE8, 0x6E3BA4, new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistryObject<Item> LIVING_SQUID_SPAWN_EGG = ITEMS.register("living_squid_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.LIVING_SQUID, 0x315B70, 0xD89BBD, new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistryObject<Item> GIANT_SQUID_SPAWN_EGG = ITEMS.register("giant_squid_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.GIANT_SQUID, 0x07162E, 0xA13CFF, new Item.Properties().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> LIVING_GLOW_SQUID_SPAWN_EGG = ITEMS.register("living_glow_squid_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.LIVING_GLOW_SQUID, 0x071C38, 0x38F4FF, new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistryObject<Item> LIVING_POLAR_BEAR_SPAWN_EGG = ITEMS.register("living_polar_bear_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.LIVING_POLAR_BEAR, 0xE8EEF0, 0x66767D, new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistryObject<Item> LIVING_AXOLOTL_SPAWN_EGG = ITEMS.register("living_axolotl_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.LIVING_AXOLOTL, 0x102B3B, 0x73DCE8, new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistryObject<Item> LIVING_OCELOT_SPAWN_EGG = ITEMS.register("living_ocelot_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.LIVING_OCELOT, 0xD88919, 0x33200F, new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistryObject<Item> LIVING_BAT_SPAWN_EGG = ITEMS.register("living_bat_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.LIVING_BAT, 0x27452E, 0x91D16E, new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistryObject<Item> ROOTED_HUSK_SPAWN_EGG = ITEMS.register("rooted_husk_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.ROOTED_HUSK, 0x4B3826, 0x77914A, new Item.Properties().rarity(Rarity.RARE)));

    private static RegistryObject<Item> registerBlockItem(String name, RegistryObject<Block> block, Item.Properties properties) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), properties));
    }

    private static Item.Properties crystalToolProperties() {
        return new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC);
    }

    private static Item.Properties crownProperties() {
        return new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC);
    }

    private static void addCrystalToolTooltip(List<Component> tooltip, String key, ChatFormatting color) {
        tooltip.add(Component.translatable(key).withStyle(color));
    }
}
