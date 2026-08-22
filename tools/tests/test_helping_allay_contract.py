import json
import struct
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

    def test_helping_amethyst_is_the_only_conversion_item(self):
        items = (
            ROOT / "src/main/java/com/Momik/usless_mobs/registry/ModItems.java"
        ).read_text(encoding="utf-8")
        handler = (
            ROOT / "src/main/java/com/Momik/usless_mobs/event/HelpingSoulHandler.java"
        ).read_text(encoding="utf-8")
        self.assertIn(
            'HELPING_AMETHYST = ITEMS.register("helping_amethyst"', items
        )
        self.assertIn("ModItems.HELPING_AMETHYST.get()", handler)
        self.assertNotIn("Items.AMETHYST_SHARD", handler)

        item_model = (
            ROOT
            / "src/main/resources/assets/usless_mobs/models/item/helping_amethyst.json"
        )
        item_texture = (
            ROOT
            / "src/main/resources/assets/usless_mobs/textures/item/helping_amethyst.png"
        )
        recipe_path = (
            ROOT / "src/main/resources/data/usless_mobs/recipes/helping_amethyst.json"
        )
        self.assertTrue(item_model.is_file())
        self.assertTrue(item_texture.is_file())
        self.assertTrue(recipe_path.is_file())

        png = item_texture.read_bytes()
        self.assertEqual(b"\x89PNG\r\n\x1a\n", png[:8])
        width, height, bit_depth, color_type = struct.unpack(">IIBB", png[16:26])
        self.assertEqual((64, 64), (width, height))
        self.assertEqual(8, bit_depth)
        self.assertEqual(6, color_type, "texture must use RGBA, not a baked background")

        model = json.loads(item_model.read_text(encoding="utf-8"))
        self.assertEqual("minecraft:item/generated", model["parent"])
        self.assertEqual(
            "usless_mobs:item/helping_amethyst", model["textures"]["layer0"]
        )

        recipe = json.loads(recipe_path.read_text(encoding="utf-8"))
        self.assertEqual("minecraft:crafting_shapeless", recipe["type"])
        self.assertEqual(
            {
                "minecraft:amethyst_shard",
                "minecraft:glow_ink_sac",
                "minecraft:gold_nugget",
            },
            {ingredient["item"] for ingredient in recipe["ingredients"]},
        )
        self.assertEqual("usless_mobs:helping_amethyst", recipe["result"]["item"])

        de_de = json.loads(
            (ROOT / "src/main/resources/assets/usless_mobs/lang/de_de.json").read_text(
                encoding="utf-8"
            )
        )
        en_us = json.loads(
            (ROOT / "src/main/resources/assets/usless_mobs/lang/en_us.json").read_text(
                encoding="utf-8"
            )
        )
        self.assertEqual("Seelen-Amethyst", de_de["item.usless_mobs.helping_amethyst"])
        self.assertEqual("Helping Amethyst", en_us["item.usless_mobs.helping_amethyst"])

    def test_renderer_uses_only_exact_mesh_and_original_albedo(self):
        renderer = (
            ROOT / "src/main/java/com/Momik/usless_mobs/client/HelpingAllayRenderer.java"
        ).read_text(encoding="utf-8")
        layer = (
            ROOT / "src/main/java/com/Momik/usless_mobs/client/HelpingAllayExactLayer.java"
        ).read_text(encoding="utf-8")
        self.assertIn("TRANSPARENT_BASE_TEXTURE", renderer)
        self.assertIn('ExactMobMesh.load(resourceManager, "helping_allay"', layer)
        self.assertIn("HELPING_ALLAY_EXACT_TEXTURE", layer)
        self.assertIn("ACTION_SHIELD", layer)
        self.assertIn("ACTION_HEAL", layer)
        self.assertNotIn("CustomMob3DLayer", renderer + layer)

    def test_renderer_is_registered_for_the_dedicated_entity(self):
        setup = (
            ROOT / "src/main/java/com/Momik/usless_mobs/Usless_mobs.java"
        ).read_text(encoding="utf-8")
        self.assertIn("HelpingAllayRenderer::new", setup)

    def test_helping_allay_has_bond_reveal_shield_heal_and_return_sounds(self):
        sounds = (
            ROOT / "src/main/resources/assets/usless_mobs/sounds.json"
        ).read_text(encoding="utf-8")
        for event in (
            "helping_allay_bond",
            "helping_allay_reveal",
            "helping_allay_shield",
            "helping_allay_heal",
            "helping_allay_return",
        ):
            self.assertIn(f'"{event}"', sounds)

        registry = (
            ROOT / "src/main/java/com/Momik/usless_mobs/registry/ModSounds.java"
        ).read_text(encoding="utf-8")
        self.assertIn("DeferredRegister<SoundEvent>", registry)
        self.assertIn("HELPING_ALLAY_BOND", registry)


if __name__ == "__main__":
    unittest.main()
