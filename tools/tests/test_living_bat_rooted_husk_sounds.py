import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


def test_living_bat_and_rooted_husk_register_complete_sound_families():
    registry = (ROOT / "src/main/java/com/Momik/usless_mobs/registry/ModSounds.java").read_text()
    sounds = json.loads(
        (ROOT / "src/main/resources/assets/usless_mobs/sounds.json").read_text(encoding="utf-8")
    )

    for family in ("living_bat", "rooted_husk"):
        for state in ("ambient", "hurt", "death"):
            sound_id = f"{family}_{state}"
            assert f'register("{sound_id}")' in registry
            assert sound_id in sounds
            assert len(sounds[sound_id]["sounds"]) >= 2


def test_entities_use_their_registered_sound_families():
    contracts = {
        "LivingBatEntity.java": ("LIVING_BAT", "public"),
        "RootedHuskEntity.java": ("ROOTED_HUSK", "protected"),
    }
    entity_root = ROOT / "src/main/java/com/Momik/usless_mobs/entity"

    for filename, (prefix, ambient_visibility) in contracts.items():
        source = (entity_root / filename).read_text()
        assert f"{ambient_visibility} SoundEvent getAmbientSound()" in source
        assert "protected SoundEvent getHurtSound(DamageSource source)" in source
        assert "protected SoundEvent getDeathSound()" in source
        assert f"ModSounds.{prefix}_AMBIENT.get()" in source
        assert f"ModSounds.{prefix}_HURT.get()" in source
        assert f"ModSounds.{prefix}_DEATH.get()" in source
