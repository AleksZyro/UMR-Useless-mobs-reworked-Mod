from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[2]
LIVING = ROOT / "src/main/java/com/Momik/usless_mobs/entity/LivingBossEntity.java"
WITCH = ROOT / "src/main/java/com/Momik/usless_mobs/entity/WitchBossEntity.java"


class BossExpansionContractTests(unittest.TestCase):
    def test_living_boss_has_two_telegraphed_new_attacks(self):
        source = LIVING.read_text(encoding="utf-8")

        self.assertIn("startRootWave", source)
        self.assertIn("tickRootWaveWarmup", source)
        self.assertIn("releaseRootWave", source)
        self.assertIn("startGroundRupture", source)
        self.assertIn("tickGroundRuptureWarmup", source)
        self.assertIn("releaseGroundRupture", source)
        self.assertIn("ROOT_WAVE_WARMUP_TICKS", source)
        self.assertIn("GROUND_RUPTURE_WARMUP_TICKS", source)

    def test_root_wave_preserves_a_visible_safe_corridor(self):
        source = LIVING.read_text(encoding="utf-8")

        self.assertIn("rootWaveSafeAngle", source)
        self.assertIn("SAFE_CORRIDOR_HALF_ANGLE", source)
        self.assertIn("isInsideSafeCorridor", source)
        self.assertIn("ParticleTypes.HAPPY_VILLAGER", source)

    def test_boss_rewards_are_explicitly_difficulty_tiered(self):
        living = LIVING.read_text(encoding="utf-8")
        witch = WITCH.read_text(encoding="utf-8")

        for source in (living, witch):
            self.assertIn("BossDifficultyProfile", source)
            self.assertIn("difficultyProfile().rewardTier()", source)


if __name__ == "__main__":
    unittest.main()
