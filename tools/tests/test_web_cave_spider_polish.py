import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


def test_web_cave_spider_owns_its_sound_identity_and_cast_sound():
    registry = (ROOT / "src/main/java/com/Momik/usless_mobs/registry/ModSounds.java").read_text()
    entity = (ROOT / "src/main/java/com/Momik/usless_mobs/entity/WebCaveSpiderEntity.java").read_text()
    sounds = json.loads(
        (ROOT / "src/main/resources/assets/usless_mobs/sounds.json").read_text(encoding="utf-8")
    )

    for suffix in ("AMBIENT", "HURT", "DEATH", "CAST"):
        assert f"WEB_CAVE_SPIDER_{suffix}" in registry
    assert "ModSounds.WEB_CAVE_SPIDER_AMBIENT.get()" in entity
    assert "ModSounds.WEB_CAVE_SPIDER_HURT.get()" in entity
    assert "ModSounds.WEB_CAVE_SPIDER_DEATH.get()" in entity
    assert "ModSounds.WEB_CAVE_SPIDER_CAST.get()" in entity
    assert "SoundEvents.SLIME_BLOCK_PLACE" not in entity

    for name in (
        "web_cave_spider_ambient",
        "web_cave_spider_hurt",
        "web_cave_spider_death",
        "web_cave_spider_cast",
    ):
        assert name in sounds
        assert len(sounds[name]["sounds"]) >= 2
