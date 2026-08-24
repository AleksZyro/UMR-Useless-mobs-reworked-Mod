import json
from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[3]
SOUNDS_CLASS = ROOT / "src/main/mobs/endermite/java/net/mysith/registry/ModSounds.java"
SOUNDS_JSON = ROOT / "src/main/resources/assets/usless_mobs/sounds.json"
ENTITY = ROOT / "src/main/mobs/endermite/java/net/mysith/silverfish/CorruptedSilverfishEntity.java"


class CorruptedSilverfishSoundIdentityTests(unittest.TestCase):
    SOUND_IDS = {
        "corrupted_silverfish_ambient",
        "corrupted_silverfish_hurt",
        "corrupted_silverfish_attack",
        "corrupted_silverfish_escape",
        "corrupted_silverfish_death",
    }

    def test_every_sound_is_registered_and_has_a_resource_definition(self):
        registry = SOUNDS_CLASS.read_text(encoding="utf-8")
        definitions = json.loads(SOUNDS_JSON.read_text(encoding="utf-8"))

        for sound_id in self.SOUND_IDS:
            field = sound_id.upper()
            self.assertIn(field, registry)
            self.assertIn(f'register("{sound_id}")', registry)
            self.assertIn(sound_id, definitions)
            self.assertTrue(definitions[sound_id]["sounds"])

    def test_entity_uses_registered_events_for_its_lifecycle(self):
        source = ENTITY.read_text(encoding="utf-8")

        self.assertIn("protected SoundEvent getAmbientSound()", source)
        self.assertIn("protected SoundEvent getHurtSound(DamageSource source)", source)
        self.assertIn("protected SoundEvent getDeathSound()", source)
        self.assertIn("ModSounds.CORRUPTED_SILVERFISH_ATTACK.get()", source)
        self.assertIn("ModSounds.CORRUPTED_SILVERFISH_ESCAPE.get()", source)


if __name__ == "__main__":
    unittest.main()
