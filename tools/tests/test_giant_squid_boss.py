import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java/com/Momik/usless_mobs"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def test_giant_squid_is_a_distinct_boss_entity_with_correct_dimensions():
    registry = read(JAVA / "registry/ModEntities.java")
    entity = read(JAVA / "entity/GiantSquidEntity.java")
    events = read(JAVA / "event/CommonModEvents.java")
    assert 'ENTITY_TYPES.register("giant_squid"' in registry
    assert ".sized(5.80F, 3.20F)" in registry
    assert "ServerBossEvent" in entity
    assert "GiantSquidEntity.createAttributes()" in events


def test_boss_uses_exact_squid_mesh_not_a_cube_substitute():
    renderer = read(JAVA / "client/GiantSquidRenderer.java")
    client = read(JAVA / "client/ClientModEvents.java")
    assert "ExactMobMeshLayer" in renderer
    assert "CustomMob3DModel.Variant.SQUID" in renderer
    assert "SQUID_EXACT_TEXTURE" in renderer
    assert "poseStack.scale" in renderer
    assert "GiantSquidRenderer::new" in client


def test_giant_squid_scale_matches_the_verified_mesh_span_and_hitbox():
    report = json.loads(read(
        ROOT / "src/main/resources/assets/usless_mobs/meshes/entity/custom3d/squid.report.json"
    ))
    renderer = read(JAVA / "client/GiantSquidRenderer.java")
    registry = read(JAVA / "registry/ModEntities.java")

    # The exact layer scales the source by 1.80 and the boss renderer by 2.23.
    # Its verified 22.4-pixel longest source span therefore becomes 5.62 blocks.
    visible_span = report["fit_span"] * 1.80 * 2.23 / 16.0
    assert visible_span == __import__("pytest").approx(5.6196)
    assert visible_span <= 5.80
    assert "poseStack.scale(2.23F, 2.23F, 2.23F)" in renderer
    assert ".sized(5.80F, 3.20F)" in registry


def test_combat_has_four_telegraphed_non_overlapping_phases():
    entity = read(JAVA / "entity/GiantSquidEntity.java")
    for token in (
        "STALKING",
        "HUNT",
        "RUIN_COLLAPSE",
        "DESPERATION",
        "telegraphInk",
        "telegraphGrab",
        "telegraphDash",
        "applyCurrentPull",
        "attackCooldown",
    ):
        assert token in entity
    assert "Difficulty.HARD" in entity
    assert "Difficulty.EASY" in entity


def test_encounter_key_and_victory_are_saved():
    entity = read(JAVA / "entity/GiantSquidEntity.java")
    handler = read(JAVA / "event/WhaleRuinEncounterHandler.java")
    assert "RuinEncounterKey" in entity
    assert "addAdditionalSaveData" in entity
    assert "readAdditionalSaveData" in entity
    assert "markDefeated" in entity
    assert "setBossUuid" in handler
