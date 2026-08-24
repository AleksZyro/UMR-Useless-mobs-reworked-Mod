import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


def test_frost_stray_and_coral_drowned_own_their_sound_families():
    registry = (ROOT / "src/main/java/com/Momik/usless_mobs/registry/ModSounds.java").read_text()
    frost = (ROOT / "src/main/java/com/Momik/usless_mobs/entity/FrostStrayEntity.java").read_text()
    coral = (ROOT / "src/main/java/com/Momik/usless_mobs/entity/CoralDrownedEntity.java").read_text()
    sounds = json.loads(
        (ROOT / "src/main/resources/assets/usless_mobs/sounds.json").read_text(encoding="utf-8")
    )

    contracts = {
        "FROST_STRAY": (frost, ("AMBIENT", "HURT", "DEATH", "VOLLEY")),
        "CORAL_DROWNED": (coral, ("AMBIENT", "HURT", "DEATH", "SURGE")),
    }
    for prefix, (source, suffixes) in contracts.items():
        for suffix in suffixes:
            constant = f"{prefix}_{suffix}"
            sound_id = constant.lower()
            assert constant in registry
            assert f"ModSounds.{constant}.get()" in source
            assert sound_id in sounds
            assert len(sounds[sound_id]["sounds"]) >= 2
