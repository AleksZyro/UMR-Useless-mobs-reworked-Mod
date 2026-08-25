import json
import re
from pathlib import Path
from typing import Optional


ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java/com/Momik/usless_mobs"
MOB_JAVA = ROOT / "src/main/mobs/endermite/java/net/mysith"
ASSETS = ROOT / "src/main/resources/assets/usless_mobs"
MOB_ASSETS = ROOT / "src/main/mobs/endermite/resources/assets/usless_mobs"
SITH_JAVA = ROOT / "src/main/mobs/endermite/java/net/mysith"


def _entity_registry():
    source = (JAVA / "registry/ModEntities.java").read_text(encoding="utf-8")
    pattern = re.compile(
        r"RegistryObject<EntityType<(?P<class>\w+)>>\s+(?P<constant>\w+)\s*=\s*"
        r'ENTITY_TYPES\.register\("(?P<id>[^"]+)"'
    )
    return [match.groupdict() for match in pattern.finditer(source)]


def _asset(relative: str) -> Optional[Path]:
    for root in (ASSETS, MOB_ASSETS):
        candidate = root / relative
        if candidate.is_file():
            return candidate
    return None


def test_every_registered_entity_has_renderer_attributes_and_localized_name():
    entities = _entity_registry()
    renderers = (JAVA / "client/ClientModEvents.java").read_text(encoding="utf-8")
    attributes = (JAVA / "event/CommonModEvents.java").read_text(encoding="utf-8")
    de_de = json.loads((ASSETS / "lang/de_de.json").read_text(encoding="utf-8"))
    en_us = json.loads((ASSETS / "lang/en_us.json").read_text(encoding="utf-8"))

    assert len(entities) == 21
    for entity in entities:
        constant = entity["constant"]
        entity_id = entity["id"]
        assert f"ModEntities.{constant}.get()" in renderers, entity_id
        assert f"entity.usless_mobs.{entity_id}" in de_de, entity_id
        assert f"entity.usless_mobs.{entity_id}" in en_us, entity_id
        if constant != "SLIME_SPIKE":
            assert f"ModEntities.{constant}.get()" in attributes, entity_id


def test_every_creative_spawnable_mob_has_spawn_egg_model_and_localization():
    items = (JAVA / "registry/ModItems.java").read_text(encoding="utf-8")
    creative_tabs = (JAVA / "registry/ModCreativeTabs.java").read_text(encoding="utf-8")
    de_de = json.loads((ASSETS / "lang/de_de.json").read_text(encoding="utf-8"))
    en_us = json.loads((ASSETS / "lang/en_us.json").read_text(encoding="utf-8"))

    for entity in _entity_registry():
        if entity["constant"] == "SLIME_SPIKE":
            continue
        entity_id = entity["id"]
        constant = f'{entity["constant"]}_SPAWN_EGG'
        item_id = f"{entity_id}_spawn_egg"
        assert constant in items, entity_id
        assert f"ModItems.{constant}" in creative_tabs, entity_id
        assert f"item.usless_mobs.{item_id}" in de_de, entity_id
        assert f"item.usless_mobs.{item_id}" in en_us, entity_id
        model = _asset(f"models/item/{item_id}.json")
        assert model is not None, entity_id
        parent = json.loads(model.read_text(encoding="utf-8"))["parent"]
        assert parent in {"item/template_spawn_egg", "minecraft:item/template_spawn_egg"}


def test_hostile_and_boss_entities_have_explicit_combat_implementation():
    combat_sources = {
        "BLAUER_SCHLEIM": JAVA / "entity/BlueSlimeEntity.java",
        "KING_SCHLEIM": JAVA / "entity/KingSlimeEntity.java",
        "ENDER_SCHLEIM": JAVA / "entity/EnderSlimeEntity.java",
        "CELESTIAL_SLIME": JAVA / "entity/CelestialSlimeEntity.java",
        "CORRUPTED_SILVERFISH": MOB_JAVA / "silverfish/CorruptedSilverfishEntity.java",
        "LIVING_BOSS": JAVA / "entity/LivingBossEntity.java",
        "FROST_STRAY": JAVA / "entity/FrostStrayEntity.java",
        "WEB_CAVE_SPIDER": JAVA / "entity/WebCaveSpiderEntity.java",
        "CORAL_DROWNED": JAVA / "entity/CoralDrownedEntity.java",
        "OCTOPUS": JAVA / "entity/OctopusEntity.java",
        "WITCH_BOSS": JAVA / "entity/WitchBossEntity.java",
        "GIANT_SQUID": JAVA / "entity/GiantSquidEntity.java",
        "LIVING_BAT": JAVA / "entity/LivingBatEntity.java",
        "ROOTED_HUSK": JAVA / "entity/RootedHuskEntity.java",
    }
    passive_or_utility = {
        "HELPING_ALLAY",
        "LIVING_SQUID",
        "LIVING_GLOW_SQUID",
        "LIVING_POLAR_BEAR",
        "LIVING_AXOLOTL",
        "LIVING_OCELOT",
    }

    registered = {entity["constant"] for entity in _entity_registry() if entity["constant"] != "SLIME_SPIKE"}
    assert registered == set(combat_sources) | passive_or_utility
    for constant, path in combat_sources.items():
        source = path.read_text(encoding="utf-8")
        assert any(token in source for token in (
            "doHurtTarget(",
            ".hurt(",
            "target.hurt(",
            "performRangedAttack(",
            "customServerAiStep(",
            "target.addEffect(",
        )), constant


def test_embedded_sith_entities_have_complete_runtime_contracts():
    registry = (SITH_JAVA / "registry/ModEntities.java").read_text(encoding="utf-8")
    renderers = (SITH_JAVA / "client/ClientSetup.java").read_text(encoding="utf-8")
    attributes = (SITH_JAVA / "event/CommonModEvents.java").read_text(encoding="utf-8")
    de_de = json.loads((ASSETS / "lang/de_de.json").read_text(encoding="utf-8"))
    en_us = json.loads((ASSETS / "lang/en_us.json").read_text(encoding="utf-8"))

    contracts = {
        "SOUL_ENDERMITE": ("soul_endermite", "SoulEndermite.java"),
        "VOID_REAPER": ("void_reaper", "VoidReaperEntity.java"),
    }
    assert registry.count("RegistryObject<EntityType<") == 3
    for constant, (entity_id, source_name) in contracts.items():
        assert f'ENTITIES.register("{entity_id}"' in registry
        assert f"ModEntities.{constant}.get()" in renderers
        assert f"ModEntities.{constant}.get()" in attributes
        assert f"entity.usless_mobs.{entity_id}" in de_de
        assert f"entity.usless_mobs.{entity_id}" in en_us
        source = (SITH_JAVA / f"entity/{source_name}").read_text(encoding="utf-8")
        assert "createAttributes()" in source
        assert "new AnimationController<>(" in source
        assert "doHurtTarget(" in source

        model = json.loads((ASSETS / f"geo/{entity_id}.geo.json").read_text(encoding="utf-8"))
        animation = json.loads((ASSETS / f"animations/{entity_id}.animation.json").read_text(encoding="utf-8"))
        assert model["minecraft:geometry"][0]["description"]["identifier"] == f"geometry.{entity_id}"
        assert model["minecraft:geometry"][0]["bones"]
        assert animation["animations"]
        model_bones = {bone["name"] for bone in model["minecraft:geometry"][0]["bones"]}
        animated_bones = {
            bone
            for clip in animation["animations"].values()
            for bone in clip.get("bones", {})
        }
        assert animated_bones <= model_bones, (entity_id, sorted(animated_bones - model_bones))
        requested_clips = set(re.findall(r'then(?:Loop|Play)\("([^"]+)"\)', source))
        assert requested_clips <= set(animation["animations"]), (
            entity_id,
            sorted(requested_clips - set(animation["animations"])),
        )
        assert (ASSETS / f"textures/entity/{entity_id}.png").stat().st_size > 0


def test_embedded_sith_spawn_paths_are_exposed_without_duplicate_boss_egg():
    items = (SITH_JAVA / "registry/ModItems.java").read_text(encoding="utf-8")
    creative_tabs = (SITH_JAVA / "registry/ModCreativeTabs.java").read_text(encoding="utf-8")
    assert "SOUL_ENDERMITE_SPAWN_EGG" in items
    assert "ModItems.SOUL_ENDERMITE_SPAWN_EGG" in creative_tabs
    assert (ASSETS / "models/item/soul_endermite_spawn_egg.json").is_file()

    # The Void Reaper is intentionally ritual-summoned rather than exposed by
    # a second, balance-breaking creative spawn egg.
    assert "VOID_SUMMONER" in items
    assert "ModItems.VOID_SUMMONER" in creative_tabs
    assert (ASSETS / "models/item/void_summoner.json").is_file()
