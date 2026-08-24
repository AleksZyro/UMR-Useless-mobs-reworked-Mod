from pathlib import Path
import re
import unittest


ROOT = Path(__file__).resolve().parents[3]
ENTITY = ROOT / "src/main/mobs/endermite/java/net/mysith/silverfish/CorruptedSilverfishEntity.java"
PART = ROOT / "src/main/mobs/endermite/java/net/mysith/silverfish/CorruptedSilverfishPart.java"


class MultipartHitboxContractTests(unittest.TestCase):
    def test_parent_exposes_front_and_rear_damage_parts(self):
        source = ENTITY.read_text(encoding="utf-8")

        self.assertIn("boolean isMultipartEntity()", source)
        self.assertIn("PartEntity<?>[] getParts()", source)
        self.assertRegex(source, r'CorruptedSilverfishPart\(this,\s*"front"')
        self.assertRegex(source, r'CorruptedSilverfishPart\(this,\s*"rear"')
        self.assertIn("updateDamageParts();", source)

    def test_part_is_pickable_unsaved_and_delegates_damage(self):
        self.assertTrue(PART.is_file(), f"missing multipart hitbox class: {PART}")
        source = PART.read_text(encoding="utf-8")

        self.assertIn("extends PartEntity<CorruptedSilverfishEntity>", source)
        self.assertIn("public boolean isPickable()", source)
        self.assertIn("public boolean shouldBeSaved()", source)
        self.assertIn("return this.getParent().hurtFromPart(this, source, amount);", source)
        self.assertIn("EntityDimensions.scalable", source)

    def test_parts_cover_the_two_block_body_without_square_core(self):
        source = ENTITY.read_text(encoding="utf-8")
        offsets = [float(value) for value in re.findall(r'PART_(?:FRONT|REAR)_OFFSET\s*=\s*([0-9.]+)D', source)]

        self.assertEqual(len(offsets), 2)
        self.assertTrue(all(0.45 <= offset <= 0.65 for offset in offsets))
        self.assertIn("PART_WIDTH = 1.10F", source)
        self.assertIn("PART_HEIGHT = 0.92F", source)

    def test_same_attack_cannot_damage_multiple_parts_in_one_tick(self):
        source = ENTITY.read_text(encoding="utf-8")

        self.assertIn("lastPartDamageTick", source)
        self.assertIn("lastPartDamageAttackerId", source)
        self.assertIn("lastPartDamageAmount", source)
        self.assertIn("this.tickCount == this.lastPartDamageTick", source)
        self.assertIn("attackerId == this.lastPartDamageAttackerId", source)
        self.assertIn("Float.compare(amount, this.lastPartDamageAmount) == 0", source)


if __name__ == "__main__":
    unittest.main()
