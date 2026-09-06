package com.Momik.usless_mobs;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = Usless_mobs.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.IntValue SLIME_KILL_THRESHOLD = BUILDER
            .comment("Slime kills needed before the King Slime can appear naturally.")
            .defineInRange("kingSlime.slimeKillThreshold", 100, 1, 10000);

    private static final ForgeConfigSpec.DoubleValue DAILY_SPAWN_CHANCE = BUILDER
            .comment("Chance per qualified player/day that the King Slime appears naturally.")
            .defineInRange("kingSlime.dailySpawnChance", 0.30D, 0.0D, 1.0D);

    private static final ForgeConfigSpec.IntValue MIN_SPAWN_DISTANCE = BUILDER
            .comment("Minimum natural King Slime spawn distance from the target player, in blocks.")
            .defineInRange("kingSlime.minSpawnDistance", 80, 16, 10000);

    private static final ForgeConfigSpec.IntValue MAX_SPAWN_DISTANCE = BUILDER
            .comment("Maximum natural King Slime spawn distance from the target player, in blocks.")
            .defineInRange("kingSlime.maxSpawnDistance", 200, 16, 20000);

    private static final ForgeConfigSpec.IntValue MAX_SPAWN_ATTEMPTS = BUILDER
            .comment("Attempts used to find a valid natural King Slime spawn position.")
            .defineInRange("kingSlime.maxSpawnAttempts", 12, 1, 128);

    private static final ForgeConfigSpec.DoubleValue EASY_HEALTH_MULTIPLIER = BUILDER
            .comment("King Slime health multiplier on Easy.")
            .defineInRange("kingSlime.easyHealthMultiplier", 0.85D, 0.1D, 20.0D);

    private static final ForgeConfigSpec.DoubleValue NORMAL_HEALTH_MULTIPLIER = BUILDER
            .comment("King Slime health multiplier on Normal.")
            .defineInRange("kingSlime.normalHealthMultiplier", 1.15D, 0.1D, 20.0D);

    private static final ForgeConfigSpec.DoubleValue HARD_HEALTH_MULTIPLIER = BUILDER
            .comment("King Slime health multiplier on Hard.")
            .defineInRange("kingSlime.hardHealthMultiplier", 2.0D, 0.1D, 20.0D);

    private static final ForgeConfigSpec.DoubleValue EASY_ATTACK_MULTIPLIER = BUILDER
            .comment("King Slime attack multiplier on Easy.")
            .defineInRange("kingSlime.easyAttackMultiplier", 0.65D, 0.1D, 20.0D);

    private static final ForgeConfigSpec.DoubleValue NORMAL_ATTACK_MULTIPLIER = BUILDER
            .comment("King Slime attack multiplier on Normal.")
            .defineInRange("kingSlime.normalAttackMultiplier", 1.0D, 0.1D, 20.0D);

    private static final ForgeConfigSpec.DoubleValue HARD_ATTACK_MULTIPLIER = BUILDER
            .comment("King Slime attack multiplier on Hard.")
            .defineInRange("kingSlime.hardAttackMultiplier", 1.85D, 0.1D, 20.0D);

    private static final ForgeConfigSpec.DoubleValue EASY_SPEED_MULTIPLIER = BUILDER
            .comment("King Slime movement speed multiplier on Easy.")
            .defineInRange("kingSlime.easySpeedMultiplier", 1.18D, 0.1D, 10.0D);

    private static final ForgeConfigSpec.DoubleValue NORMAL_SPEED_MULTIPLIER = BUILDER
            .comment("King Slime movement speed multiplier on Normal.")
            .defineInRange("kingSlime.normalSpeedMultiplier", 1.35D, 0.1D, 10.0D);

    private static final ForgeConfigSpec.DoubleValue HARD_SPEED_MULTIPLIER = BUILDER
            .comment("King Slime movement speed multiplier on Hard.")
            .defineInRange("kingSlime.hardSpeedMultiplier", 1.65D, 0.1D, 10.0D);

    private static final ForgeConfigSpec.BooleanValue EASY_DROPS_CROWN = BUILDER
            .comment("Whether the King Slime drops the crown on Easy.")
            .define("kingSlime.easyDropsCrown", false);

    private static final ForgeConfigSpec.BooleanValue NORMAL_DROPS_CROWN = BUILDER
            .comment("Whether the King Slime drops the crown on Normal.")
            .define("kingSlime.normalDropsCrown", true);

    private static final ForgeConfigSpec.BooleanValue HARD_DROPS_CROWN = BUILDER
            .comment("Whether the King Slime drops the crown on Hard.")
            .define("kingSlime.hardDropsCrown", true);

    private static final ForgeConfigSpec.BooleanValue HARD_DROPS_TROPHY = BUILDER
            .comment("Whether the King Slime drops the trophy on Hard.")
            .define("kingSlime.hardDropsTrophy", true);

    private static final ForgeConfigSpec.IntValue OCEAN_BIOME_REGION_WEIGHT = BUILDER
            .comment("TerraBlender weight for UMR Deep Ocean biomes. Set to 0 to disable new UMR ocean-biome placement.")
            .defineInRange("worldgen.oceanBiomeRegionWeight", 1, 0, 10);

    private static final ForgeConfigSpec.IntValue ANCIENT_WHALE_RUIN_SPACING = BUILDER
            .comment("Approximate Ancient Whale Ruin spacing in chunks. Values above 72 make the ruin rarer; restart required.")
            .defineInRange("worldgen.ancientWhaleRuinSpacing", 72, 32, 512);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static int slimeKillThreshold = 100;
    public static double dailySpawnChance = 0.30D;
    public static int minSpawnDistance = 80;
    public static int maxSpawnDistance = 200;
    public static int maxSpawnAttempts = 12;
    public static double easyHealthMultiplier = 0.85D;
    public static double normalHealthMultiplier = 1.15D;
    public static double hardHealthMultiplier = 2.0D;
    public static double easyAttackMultiplier = 0.65D;
    public static double normalAttackMultiplier = 1.0D;
    public static double hardAttackMultiplier = 1.85D;
    public static double easySpeedMultiplier = 1.18D;
    public static double normalSpeedMultiplier = 1.35D;
    public static double hardSpeedMultiplier = 1.65D;
    public static boolean easyDropsCrown = false;
    public static boolean normalDropsCrown = true;
    public static boolean hardDropsCrown = true;
    public static boolean hardDropsTrophy = true;
    public static int oceanBiomeRegionWeight = 1;
    public static int ancientWhaleRuinSpacing = 72;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        slimeKillThreshold = SLIME_KILL_THRESHOLD.get();
        dailySpawnChance = DAILY_SPAWN_CHANCE.get();
        minSpawnDistance = MIN_SPAWN_DISTANCE.get();
        maxSpawnDistance = MAX_SPAWN_DISTANCE.get();
        maxSpawnAttempts = MAX_SPAWN_ATTEMPTS.get();
        easyHealthMultiplier = EASY_HEALTH_MULTIPLIER.get();
        normalHealthMultiplier = NORMAL_HEALTH_MULTIPLIER.get();
        hardHealthMultiplier = HARD_HEALTH_MULTIPLIER.get();
        easyAttackMultiplier = EASY_ATTACK_MULTIPLIER.get();
        normalAttackMultiplier = NORMAL_ATTACK_MULTIPLIER.get();
        hardAttackMultiplier = HARD_ATTACK_MULTIPLIER.get();
        easySpeedMultiplier = EASY_SPEED_MULTIPLIER.get();
        normalSpeedMultiplier = NORMAL_SPEED_MULTIPLIER.get();
        hardSpeedMultiplier = HARD_SPEED_MULTIPLIER.get();
        easyDropsCrown = EASY_DROPS_CROWN.get();
        normalDropsCrown = NORMAL_DROPS_CROWN.get();
        hardDropsCrown = HARD_DROPS_CROWN.get();
        hardDropsTrophy = HARD_DROPS_TROPHY.get();
        oceanBiomeRegionWeight = OCEAN_BIOME_REGION_WEIGHT.get();
        ancientWhaleRuinSpacing = ANCIENT_WHALE_RUIN_SPACING.get();
    }
}
