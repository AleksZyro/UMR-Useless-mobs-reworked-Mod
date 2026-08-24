from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java/com/Momik/usless_mobs"


def test_registered_boss_sizes_match_their_verified_runtime_forms():
    entities = (JAVA / "registry/ModEntities.java").read_text(encoding="utf-8")

    assert 'ENTITY_TYPES.register("king_schleim"' in entities
    assert ".sized(2.04F, 2.04F)" in entities
    assert 'ENTITY_TYPES.register("living_boss"' in entities
    assert ".sized(3.70F, 2.95F)" in entities
    assert 'ENTITY_TYPES.register("witch_boss"' in entities
    assert ".sized(1.15F, 1.95F)" in entities


def test_each_registered_boss_has_boss_grade_health_damage_and_bossbar():
    king = (JAVA / "entity/KingSlimeEntity.java").read_text(encoding="utf-8")
    living = (JAVA / "entity/LivingBossEntity.java").read_text(encoding="utf-8")
    witch = (JAVA / "entity/WitchBossEntity.java").read_text(encoding="utf-8")

    assert "private static final int KING_SIZE = 8" in king
    assert "private static final double BASE_HEALTH = 320.0D" in king
    assert "ServerBossEvent bossEvent" in king
    assert ".add(Attributes.MAX_HEALTH, 220.0D)" in living
    assert ".add(Attributes.ATTACK_DAMAGE, 15.0D)" in living
    assert "ServerBossEvent bossEvent" in living
    assert ".add(Attributes.MAX_HEALTH, 155.0D)" in witch
    assert ".add(Attributes.ATTACK_DAMAGE, 6.0D)" in witch
    assert "ServerBossEvent bossEvent" in witch
