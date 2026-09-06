import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java/com/Momik/usless_mobs"
DATA = ROOT / "src/main/resources/data/usless_mobs"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def load(path: Path):
    return json.loads(read(path))


def test_structure_and_piece_are_registered_on_the_mod_bus():
    registry = read(JAVA / "worldgen/ModStructures.java")
    main = read(JAVA / "Usless_mobs.java")
    assert "Registries.STRUCTURE_TYPE" in registry
    assert "Registries.STRUCTURE_PIECE" in registry
    assert "AncientWhaleRuinStructure.CODEC" in registry
    assert "AncientWhaleRuinPiece::new" in registry
    assert "ModStructures.STRUCTURE_TYPES.register(modEventBus)" in main
    assert "ModStructures.STRUCTURE_PIECE_TYPES.register(modEventBus)" in main


def test_ruin_targets_only_umr_deep_ocean_and_is_locatable():
    structure = load(DATA / "worldgen/structure/ancient_whale_ruin.json")
    structure_set = load(DATA / "worldgen/structure_set/ancient_whale_ruins.json")
    biome_tag = load(DATA / "tags/worldgen/biome/has_structure/ancient_whale_ruin.json")
    assert structure["type"] == "usless_mobs:ancient_whale_ruin"
    assert structure["biomes"] == "#usless_mobs:has_structure/ancient_whale_ruin"
    assert structure["step"] == "surface_structures"
    assert biome_tag["values"] == ["usless_mobs:deep_ocean"]
    assert structure_set["structures"] == [
        {"structure": "usless_mobs:ancient_whale_ruin", "weight": 1}
    ]
    placement = structure_set["placement"]
    assert placement["type"] == "minecraft:random_spread"
    assert placement["spacing"] > placement["separation"] >= 8


def test_piece_is_large_flooded_and_contains_whale_bones_and_treasure():
    piece = read(JAVA / "worldgen/AncientWhaleRuinPiece.java")
    structure = read(JAVA / "worldgen/AncientWhaleRuinStructure.java")
    assert "WIDTH = 49" in piece
    assert "HEIGHT = 17" in piece
    assert "DEPTH = 45" in piece
    assert "Blocks.BONE_BLOCK" in piece
    assert "buildWhaleSkeleton" in piece
    assert "Blocks.GOLD_BLOCK" in piece
    assert "TREASURE_GOLD_BLOCKS = 7" in piece
    assert "Blocks.WATER" in piece
    assert "chunkBounds.isInside" in piece
    assert "Heightmap.Types.OCEAN_FLOOR_WG" in structure
    assert "getSeaLevel() - 14" in structure


def test_encounter_state_is_persistent_and_duplicate_safe():
    data = read(JAVA / "worldgen/WhaleRuinEncounterData.java")
    handler = read(JAVA / "event/WhaleRuinEncounterHandler.java")
    assert "extends SavedData" in data
    assert "CompoundTag save" in data
    assert "static WhaleRuinEncounterData load" in data
    assert "activateIfInactive" in data
    assert "setBossUuid" in data
    assert "markDefeated" in data
    assert "BlockEvent.BreakEvent" in handler
    assert "Blocks.GOLD_BLOCK" in handler
    assert "getStructureWithPieceAt" in handler
    assert "ANCIENT_WHALE_RUIN_KEY" in handler
    assert "event.setCanceled(true)" in handler


def test_worldgen_has_configurable_ruin_spacing():
    config = read(JAVA / "Config.java")
    assert 'worldgen.ancientWhaleRuinSpacing' in config
    assert "ancientWhaleRuinSpacing" in config
