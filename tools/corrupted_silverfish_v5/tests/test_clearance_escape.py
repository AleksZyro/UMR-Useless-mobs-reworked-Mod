from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[3]
ENTITY = ROOT / "src/main/mobs/endermite/java/net/mysith/silverfish/CorruptedSilverfishEntity.java"
CLEARANCE = ROOT / "src/main/mobs/endermite/java/net/mysith/silverfish/CorruptedSilverfishClearance.java"


class ClearanceEscapeContractTests(unittest.TestCase):
    def test_clearance_checks_the_whole_oriented_body(self):
        self.assertTrue(CLEARANCE.is_file(), f"missing clearance helper: {CLEARANCE}")
        source = CLEARANCE.read_text(encoding="utf-8")

        self.assertIn("AABB fullBodyBounds", source)
        self.assertIn("hasFullBodyClearance", source)
        self.assertIn("hasSafeBodySupport", source)
        self.assertIn("this.parent.level().noCollision(this.parent, bounds)", source)

    def test_escape_requires_sustained_stuck_state_and_last_safe_position(self):
        source = CLEARANCE.read_text(encoding="utf-8")

        self.assertIn("STUCK_TICKS_BEFORE_ESCAPE = 40", source)
        self.assertIn("MAX_SAFE_POSITION_AGE = 20 * 12", source)
        self.assertIn("lastSafePosition", source)
        self.assertIn("lastSafePositionTick", source)
        self.assertIn("parent.getNavigation().stop()", source)
        self.assertIn("parent.teleportTo", source)

    def test_entity_ticks_clearance_and_emits_escape_feedback(self):
        source = ENTITY.read_text(encoding="utf-8")

        self.assertIn("CorruptedSilverfishClearance clearance", source)
        self.assertIn("this.clearance.tick();", source)
        self.assertIn("onCorruptionEscape", source)
        self.assertIn("ParticleTypes.SCULK_SOUL", source)


if __name__ == "__main__":
    unittest.main()
