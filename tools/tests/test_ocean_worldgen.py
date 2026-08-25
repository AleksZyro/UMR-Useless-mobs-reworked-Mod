import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java/com/Momik/usless_mobs"
DATA = ROOT / "src/main/resources/data/usless_mobs/worldgen"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def load_json(path: Path):
    return json.loads(read(path))


def test_terrablender_is_a_pinned_required_forge_dependency():
    properties = read(ROOT / "gradle.properties")
    build = read(ROOT / "build.gradle")
    mods = read(ROOT / "src/main/resources/META-INF/mods.toml")

    assert "terrablender_version=3.0.1.10" in properties
    assert "com.github.glitchfiend:TerraBlender-forge:${minecraft_version}-${terrablender_version}" in build
    assert 'modId = "terrablender"' in mods
    dependency = mods.split('modId = "terrablender"', 1)[1].split("[[", 1)[0]
    assert "mandatory = true" in dependency
    assert 'versionRange = "[3.0.1.10,3.1)"' in dependency
    assert 'side = "BOTH"' in dependency


def test_ocean_region_maps_only_deep_ocean_surface_and_underground_points():
    biomes = read(JAVA / "worldgen/ModBiomes.java")
    region = read(JAVA / "worldgen/UmrOceanRegion.java")
    main = read(JAVA / "Usless_mobs.java")

    assert 'register("deep_ocean")' in biomes
    assert 'register("big_underwater_cave")' in biomes
    assert "extends Region" in region
    assert "RegionType.OVERWORLD" in region
    assert "Continentalness.DEEP_OCEAN" in region
    assert "Depth.SURFACE" in region
    assert "Depth.UNDERGROUND" in region
    assert "builder.add(point, ModBiomes.DEEP_OCEAN)" in region
    assert "builder.add(point, ModBiomes.BIG_UNDERWATER_CAVE)" in region
    assert "Regions.register(new UmrOceanRegion" in main
    assert 'ResourceLocation.tryBuild(MODID, "ocean_region")' in main
    assert "Config.oceanBiomeRegionWeight > 0" in main
    assert "Config.oceanBiomeRegionWeight));" in main


def test_ocean_region_weight_is_common_config_backed_and_can_disable_generation():
    config = read(JAVA / "Config.java")
    assert 'defineInRange("worldgen.oceanBiomeRegionWeight", 1, 0, 10)' in config
    assert "public static int oceanBiomeRegionWeight = 1;" in config
    assert "oceanBiomeRegionWeight = OCEAN_BIOME_REGION_WEIGHT.get();" in config


def test_custom_biomes_are_valid_full_generation_definitions():
    deep = load_json(DATA / "biome/deep_ocean.json")
    cave = load_json(DATA / "biome/big_underwater_cave.json")

    for biome in (deep, cave):
        assert biome["has_precipitation"] is True
        assert len(biome["features"]) == 11
        assert isinstance(biome["carvers"]["air"], list)
        assert set(biome["spawners"]) == {
            "ambient", "axolotls", "creature", "misc", "monster",
            "underground_water_creature", "water_ambient", "water_creature",
        }
        assert biome["effects"]["water_color"] != 4159204
        assert biome["effects"]["water_fog_color"] != 329011

    assert "usless_mobs:big_underwater_cave" in cave["features"][9]


def test_big_underwater_cave_feature_is_bounded_flooded_and_depth_safe():
    registry = read(JAVA / "registry/ModFeatures.java")
    feature = read(JAVA / "world/BigUnderwaterCaveFeature.java")
    configured = load_json(DATA / "configured_feature/big_underwater_cave.json")
    placed = load_json(DATA / "placed_feature/big_underwater_cave.json")

    assert 'FEATURES.register("big_underwater_cave"' in registry
    assert "MIN_HORIZONTAL_RADIUS = 10" in feature
    assert "MAX_HORIZONTAL_RADIUS = 18" in feature
    assert "MIN_VERTICAL_RADIUS = 6" in feature
    assert "MAX_VERTICAL_RADIUS = 10" in feature
    assert "level.getSeaLevel() - 14" in feature
    assert "Blocks.BEDROCK" in feature
    assert "Blocks.WATER.defaultBlockState()" in feature
    assert "for (int dx = -radiusX; dx <= radiusX; dx++)" in feature
    assert "for (int dy = -radiusY; dy <= radiusY; dy++)" in feature
    assert "for (int dz = -radiusZ; dz <= radiusZ; dz++)" in feature
    assert configured == {"type": "usless_mobs:big_underwater_cave", "config": {}}

    modifier_types = [modifier["type"] for modifier in placed["placement"]]
    assert "minecraft:rarity_filter" in modifier_types
    assert "minecraft:in_square" in modifier_types
    assert "minecraft:height_range" in modifier_types
    assert modifier_types[-1] == "minecraft:biome"
