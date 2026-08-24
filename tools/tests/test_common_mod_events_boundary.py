from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java/com/Momik/usless_mobs"
MAIN = JAVA / "Usless_mobs.java"
COMMON_EVENTS = JAVA / "event/CommonModEvents.java"


def test_entrypoint_does_not_own_entity_attribute_or_spawn_contracts():
    source = MAIN.read_text(encoding="utf-8")

    assert "com.Momik.usless_mobs.entity" not in source
    assert "EntityAttributeCreationEvent" not in source
    assert "SpawnPlacementRegisterEvent" not in source
    assert "SpawnPlacements" not in source


def test_common_mod_events_owns_attributes_and_spawn_placements():
    source = COMMON_EVENTS.read_text(encoding="utf-8")

    assert "bus = Mod.EventBusSubscriber.Bus.MOD" in source
    assert "registerAttributes(EntityAttributeCreationEvent event)" in source
    assert "registerSpawnPlacements(SpawnPlacementRegisterEvent event)" in source
    assert "ModEntities.CORRUPTED_SILVERFISH" in source
    assert "ModEntities.CORAL_DROWNED" in source
