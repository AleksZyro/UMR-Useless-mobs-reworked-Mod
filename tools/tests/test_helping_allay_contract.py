import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


class HelpingAllayContractTests(unittest.TestCase):
    def test_entity_has_persistent_bond_and_synced_action(self):
        source = (
            ROOT / "src/main/java/com/Momik/usless_mobs/entity/HelpingAllayEntity.java"
        ).read_text(encoding="utf-8")
        self.assertIn("extends Allay", source)
        self.assertIn("OWNER", source)
        self.assertIn("SUPPORT_UNTIL", source)
        self.assertIn("ACTION", source)
        self.assertIn("addAdditionalSaveData", source)
        self.assertIn("readAdditionalSaveData", source)

    def test_registry_and_attributes_include_helping_allay(self):
        entities = (
            ROOT / "src/main/java/com/Momik/usless_mobs/registry/ModEntities.java"
        ).read_text(encoding="utf-8")
        setup = (
            ROOT / "src/main/java/com/Momik/usless_mobs/Usless_mobs.java"
        ).read_text(encoding="utf-8")
        self.assertIn('register("helping_allay"', entities)
        self.assertIn("ModEntities.HELPING_ALLAY.get()", setup)
        self.assertIn("HelpingAllayEntity.createAttributes().build()", setup)

    def test_handler_converts_and_migrates_vanilla_allays(self):
        source = (
            ROOT / "src/main/java/com/Momik/usless_mobs/event/HelpingSoulHandler.java"
        ).read_text(encoding="utf-8")
        self.assertIn("convertToHelpingAllay", source)
        self.assertIn("ModEntities.HELPING_ALLAY.get().create", source)
        self.assertIn("copyAllayState", source)
        self.assertIn("allay.discard()", source)


if __name__ == "__main__":
    unittest.main()
