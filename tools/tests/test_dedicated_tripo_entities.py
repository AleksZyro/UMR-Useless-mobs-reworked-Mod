import json
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java/com/Momik/usless_mobs"
ASSETS = ROOT / "src/main/resources/assets/usless_mobs"
CLIENT_EVENTS = JAVA / "client/ClientModEvents.java"


class DedicatedTripoEntityContract(unittest.TestCase):
    def test_squid_is_a_dedicated_large_entity_not_a_vanilla_renderer_override(self):
        entity = (JAVA / "entity/LivingSquidEntity.java").read_text(encoding="utf-8")
        entities = (JAVA / "registry/ModEntities.java").read_text(encoding="utf-8")
        setup = CLIENT_EVENTS.read_text(encoding="utf-8")
        renderer = (JAVA / "client/LivingSquidRenderer.java").read_text(encoding="utf-8")

        self.assertIn("class LivingSquidEntity extends Squid", entity)
        self.assertIn('register("living_squid"', entities)
        self.assertIn(".sized(2.60F, 1.30F)", entities)
        self.assertIn("ModEntities.LIVING_SQUID.get(), LivingSquidRenderer::new", setup)
        self.assertNotIn("registerEntityRenderer(EntityType.SQUID", setup)
        self.assertIn("MobRenderer<LivingSquidEntity", renderer)
        self.assertNotIn("extends SquidRenderer", renderer)

    def test_helping_allay_has_a_larger_body_and_its_own_spawn_egg(self):
        entities = (JAVA / "registry/ModEntities.java").read_text(encoding="utf-8")
        layer = (JAVA / "client/HelpingAllayExactLayer.java").read_text(encoding="utf-8")
        items = (JAVA / "registry/ModItems.java").read_text(encoding="utf-8")

        self.assertIn(".sized(0.95F, 0.90F)", entities)
        self.assertIn("float modelScale = 1.35F;", layer)
        self.assertIn("poseStack.scale(modelScale, modelScale, modelScale);", layer)
        self.assertIn("poseStack.translate(0F, 1.5F / modelScale - 1.5F, 0F);", layer)
        self.assertIn('HELPING_ALLAY_SPAWN_EGG = ITEMS.register("helping_allay_spawn_egg"', items)

    def test_glow_squid_is_a_dedicated_entity_with_its_own_renderer_and_spawn_egg(self):
        entity = (JAVA / "entity/LivingGlowSquidEntity.java").read_text(encoding="utf-8")
        entities = (JAVA / "registry/ModEntities.java").read_text(encoding="utf-8")
        items = (JAVA / "registry/ModItems.java").read_text(encoding="utf-8")
        setup = CLIENT_EVENTS.read_text(encoding="utf-8")
        renderer = (JAVA / "client/LivingGlowSquidRenderer.java").read_text(encoding="utf-8")

        self.assertIn("class LivingGlowSquidEntity extends GlowSquid", entity)
        self.assertIn('register("living_glow_squid"', entities)
        self.assertIn(".sized(1.45F, 1.80F)", entities)
        self.assertIn("ModEntities.LIVING_GLOW_SQUID.get(), LivingGlowSquidRenderer::new", setup)
        self.assertIn("MobRenderer<LivingGlowSquidEntity", renderer)
        self.assertIn('LIVING_GLOW_SQUID_SPAWN_EGG = ITEMS.register("living_glow_squid_spawn_egg"', items)

    def test_polar_bear_is_a_dedicated_entity_not_a_vanilla_renderer_override(self):
        entity = (JAVA / "entity/LivingPolarBearEntity.java").read_text(encoding="utf-8")
        entities = (JAVA / "registry/ModEntities.java").read_text(encoding="utf-8")
        items = (JAVA / "registry/ModItems.java").read_text(encoding="utf-8")
        setup = CLIENT_EVENTS.read_text(encoding="utf-8")
        renderer = (JAVA / "client/LivingPolarBearRenderer.java").read_text(encoding="utf-8")

        self.assertIn("class LivingPolarBearEntity extends PolarBear", entity)
        self.assertIn('register("living_polar_bear"', entities)
        self.assertIn(".sized(1.90F, 1.40F)", entities)
        self.assertIn("ModEntities.LIVING_POLAR_BEAR.get(), LivingPolarBearRenderer::new", setup)
        self.assertNotIn("registerEntityRenderer(EntityType.POLAR_BEAR", setup)
        self.assertIn("MobRenderer<LivingPolarBearEntity", renderer)
        self.assertIn('LIVING_POLAR_BEAR_SPAWN_EGG = ITEMS.register("living_polar_bear_spawn_egg"', items)

        drops = (JAVA / "event/LivingDropsHandler.java").read_text(encoding="utf-8")
        self.assertIn("entity instanceof PolarBear", drops)
        self.assertNotIn("entity.getType() == EntityType.POLAR_BEAR", drops)

        sounds = (JAVA / "registry/ModSounds.java").read_text(encoding="utf-8")
        sounds_json = json.loads((ASSETS / "sounds.json").read_text(encoding="utf-8"))
        for sound in ("polar_bear_ambient", "polar_bear_hurt", "polar_bear_death", "polar_bear_charge"):
            self.assertIn(sound.upper(), sounds)
            self.assertIn(sound, sounds_json)

    def test_axolotl_is_a_dedicated_entity_with_exact_renderer_and_spawn_egg(self):
        entity = (JAVA / "entity/LivingAxolotlEntity.java").read_text(encoding="utf-8")
        entities = (JAVA / "registry/ModEntities.java").read_text(encoding="utf-8")
        items = (JAVA / "registry/ModItems.java").read_text(encoding="utf-8")
        setup = CLIENT_EVENTS.read_text(encoding="utf-8")
        renderer = (JAVA / "client/LivingAxolotlRenderer.java").read_text(encoding="utf-8")

        self.assertIn("class LivingAxolotlEntity extends Axolotl", entity)
        self.assertIn('register("living_axolotl"', entities)
        self.assertIn(".sized(1.35F, 0.65F)", entities)
        self.assertIn("ModEntities.LIVING_AXOLOTL.get(), LivingAxolotlRenderer::new", setup)
        self.assertNotIn("registerEntityRenderer(EntityType.AXOLOTL", setup)
        self.assertIn("MobRenderer<LivingAxolotlEntity", renderer)
        self.assertIn('LIVING_AXOLOTL_SPAWN_EGG = ITEMS.register("living_axolotl_spawn_egg"', items)

    def test_ocelot_is_dedicated_and_does_not_replace_vanilla_renderer(self):
        entity = (JAVA / "entity/LivingOcelotEntity.java").read_text(encoding="utf-8")
        entities = (JAVA / "registry/ModEntities.java").read_text(encoding="utf-8")
        items = (JAVA / "registry/ModItems.java").read_text(encoding="utf-8")
        setup = CLIENT_EVENTS.read_text(encoding="utf-8")
        renderer = (JAVA / "client/LivingOcelotRenderer.java").read_text(encoding="utf-8")

        self.assertIn("class LivingOcelotEntity extends Ocelot", entity)
        self.assertIn('register("living_ocelot"', entities)
        self.assertIn(".sized(1.45F, 0.90F)", entities)
        self.assertIn("ModEntities.LIVING_OCELOT.get(), LivingOcelotRenderer::new", setup)
        self.assertNotIn("registerEntityRenderer(EntityType.OCELOT", setup)
        self.assertIn("MobRenderer<LivingOcelotEntity", renderer)
        self.assertIn('LIVING_OCELOT_SPAWN_EGG = ITEMS.register("living_ocelot_spawn_egg"', items)

        handler = (JAVA / "event/LivingMobReworkHandler.java").read_text(encoding="utf-8")
        drops = (JAVA / "event/LivingDropsHandler.java").read_text(encoding="utf-8")
        self.assertIn("entity instanceof LivingOcelotEntity", handler)
        self.assertIn("event.getTarget() instanceof LivingOcelotEntity", handler)
        self.assertIn("getEntitiesOfClass(LivingOcelotEntity.class", handler)
        self.assertNotIn("entity.getType() == EntityType.OCELOT", drops)

    def test_every_exact_vanilla_subclass_has_its_own_registry_id_and_spawn_egg(self):
        entities = (JAVA / "registry/ModEntities.java").read_text(encoding="utf-8")
        items = (JAVA / "registry/ModItems.java").read_text(encoding="utf-8")
        setup = CLIENT_EVENTS.read_text(encoding="utf-8")

        for java_name, base_name, registry_name, constant, egg in (
            ("LivingBatEntity", "Bat", "living_bat", "LIVING_BAT", "LIVING_BAT_SPAWN_EGG"),
            ("RootedHuskEntity", "Husk", "rooted_husk", "ROOTED_HUSK", "ROOTED_HUSK_SPAWN_EGG"),
        ):
            with self.subTest(entity=java_name):
                source = (JAVA / f"entity/{java_name}.java").read_text(encoding="utf-8")
                self.assertIn(f"class {java_name} extends {base_name}", source)
                self.assertIn(f'register("{registry_name}"', entities)
                self.assertIn(f"ModEntities.{constant}.get()", setup)
                self.assertIn(egg, items)

        self.assertNotIn("registerEntityRenderer(EntityType.BAT", setup)
        self.assertNotIn("registerEntityRenderer(EntityType.HUSK", setup)

    def test_new_spawn_eggs_are_exposed_and_localized(self):
        setup = (JAVA / "Usless_mobs.java").read_text(encoding="utf-8")
        tabs = (JAVA / "registry/ModCreativeTabs.java").read_text(encoding="utf-8")
        de_de = json.loads((ASSETS / "lang/de_de.json").read_text(encoding="utf-8"))
        en_us = json.loads((ASSETS / "lang/en_us.json").read_text(encoding="utf-8"))

        expected = {
            "helping_allay_spawn_egg": ("Spawn-Ei des Hilfsseelen-Allays", "Helping Soul Allay Spawn Egg"),
            "living_squid_spawn_egg": ("Spawn-Ei des lebenden Kalmars", "Living Squid Spawn Egg"),
            "living_glow_squid_spawn_egg": ("Spawn-Ei des Leuchtkalmars", "Living Glow Squid Spawn Egg"),
            "living_bat_spawn_egg": ("Spawn-Ei der lebenden Fledermaus", "Living Bat Spawn Egg"),
            "rooted_husk_spawn_egg": ("Spawn-Ei des verwurzelten Husks", "Rooted Husk Spawn Egg"),
            "living_polar_bear_spawn_egg": ("Spawn-Ei des lebenden Eisbären", "Living Polar Bear Spawn Egg"),
            "living_axolotl_spawn_egg": ("Spawn-Ei des lebenden Axolotls", "Living Axolotl Spawn Egg"),
            "living_ocelot_spawn_egg": ("Spawn-Ei des lebenden Ozelots", "Living Ocelot Spawn Egg"),
        }
        for name, (german, english) in expected.items():
            constant = name.removesuffix("_spawn_egg").upper() + "_SPAWN_EGG"
            with self.subTest(egg=name):
                self.assertIn(f"ModItems.{constant}", setup + tabs)
                self.assertEqual(german, de_de[f"item.usless_mobs.{name}"])
                self.assertEqual(english, en_us[f"item.usless_mobs.{name}"])
                model = json.loads((ASSETS / f"models/item/{name}.json").read_text(encoding="utf-8"))
                self.assertEqual("minecraft:item/template_spawn_egg", model["parent"])


if __name__ == "__main__":
    unittest.main()
