from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
PROFILE = ROOT / "src/main/java/com/Momik/usless_mobs/entity/boss/BossDifficultyProfile.java"
LIVING = ROOT / "src/main/java/com/Momik/usless_mobs/entity/LivingBossEntity.java"
WITCH = ROOT / "src/main/java/com/Momik/usless_mobs/entity/WitchBossEntity.java"


def test_profile_defines_three_distinct_complete_difficulty_tiers():
    assert PROFILE.is_file(), f"missing shared boss profile: {PROFILE}"
    source = PROFILE.read_text(encoding="utf-8")

    assert "record BossDifficultyProfile(" in source
    for field in (
        "damageMultiplier",
        "cooldownMultiplier",
        "livingSummonCap",
        "witchSpiritCount",
        "huntHoundCount",
        "rewardTier",
    ):
        assert field in source

    assert "case PEACEFUL, EASY -> new BossDifficultyProfile(0.72F, 1.25F, 2, 1, 2, 0)" in source
    assert "case HARD -> new BossDifficultyProfile(1.30F, 0.78F, 6, 3, 4, 2)" in source
    assert "default -> new BossDifficultyProfile(1.00F, 1.00F, 4, 2, 3, 1)" in source
    assert "float damage(float baseDamage)" in source
    assert "int cooldown(int baseTicks)" in source


def test_living_boss_routes_combat_balance_through_shared_profile():
    source = LIVING.read_text(encoding="utf-8")

    assert "BossDifficultyProfile difficultyProfile()" in source
    assert source.count("difficultyProfile().cooldown(") >= 4
    assert source.count("difficultyProfile().damage(") >= 5
    assert "difficultyProfile().livingSummonCap()" in source
    assert "difficultyProfile().rewardTier()" in source
    assert "15.0D * difficultyProfile().damageMultiplier()" in source


def test_witch_boss_routes_combat_balance_through_shared_profile():
    source = WITCH.read_text(encoding="utf-8")

    assert "BossDifficultyProfile difficultyProfile()" in source
    assert source.count("difficultyProfile().cooldown(") >= 6
    assert source.count("difficultyProfile().damage(") >= 2
    assert "difficultyProfile().witchSpiritCount()" in source
    assert "profile.huntHoundCount()" in source
    assert "difficultyProfile().rewardTier()" in source
    assert "6.0D * difficultyProfile().damageMultiplier()" in source
