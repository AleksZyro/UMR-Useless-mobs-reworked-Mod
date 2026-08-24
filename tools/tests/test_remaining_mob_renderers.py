import unittest
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[2]
CLIENT = ROOT / "src/main/java/com/Momik/usless_mobs/client"
EXACT_TRIPO_RENDERERS = (
    "LivingBossRenderer.java",
    "WebCaveSpiderRenderer.java",
    "OctopusRenderer.java",
    "WitchBossRenderer.java",
    "LivingBatRenderer.java",
    "RootedHuskRenderer.java",
    "LivingSquidRenderer.java",
    "LivingGlowSquidRenderer.java",
    "LivingPolarBearRenderer.java",
    "LivingAxolotlRenderer.java",
    "LivingOcelotRenderer.java",
    "FrostStrayRenderer.java",
    "CoralDrownedRenderer.java",
)
SPECIAL_CASE_RENDERERS = ()


class RemainingMobRendererContract(unittest.TestCase):
    def test_exact_tripo_layer_uses_stable_full_bright_material_lighting(self):
        source = (CLIENT / "ExactMobMeshLayer.java").read_text(encoding="utf-8")

        self.assertIn("LightTexture.FULL_BRIGHT", source)
        self.assertIn("this.mesh.renderBone(bone, poseStack, buffer, materialLight, overlay);", source)

    def test_approved_tripo_mobs_use_the_exact_mesh_layer(self):
        for filename in EXACT_TRIPO_RENDERERS:
            with self.subTest(renderer=filename):
                source = (CLIENT / filename).read_text(encoding="utf-8")
                clear = source.index("this.layers.clear();")
                exact = source.index("this.addLayer(new ExactMobMeshLayer<>")
                self.assertLess(clear, exact)
                self.assertEqual(1, source.count("new ExactMobMeshLayer<>("))
                self.assertNotIn("new CustomMob3DLayer<>(", source)
                self.assertIn("return CustomMobModelLayers.TRANSPARENT_BASE_TEXTURE;", source)

    def test_helping_allay_uses_its_dedicated_exact_mesh_layer(self):
        source = (CLIENT / "HelpingAllayRenderer.java").read_text(encoding="utf-8")

        clear = source.index("this.layers.clear();")
        exact = source.index("this.addLayer(new HelpingAllayExactLayer(")
        self.assertLess(clear, exact)
        self.assertNotIn("new CustomMob3DLayer<>(", source)
        self.assertIn("return CustomMobModelLayers.TRANSPARENT_BASE_TEXTURE;", source)

    def test_two_special_cases_remain_untouched_until_regenerated(self):
        for filename in SPECIAL_CASE_RENDERERS:
            with self.subTest(renderer=filename):
                source = (CLIENT / filename).read_text(encoding="utf-8")
                self.assertEqual(1, source.count("new CustomMob3DLayer<>("))
                self.assertNotIn("new ExactMobMeshLayer<>(", source)

    def test_frost_stray_keeps_its_visible_bow_after_replacing_vanilla_layers(self):
        source = (CLIENT / "FrostStrayRenderer.java").read_text(encoding="utf-8")
        clear = source.index("this.layers.clear();")
        held_item = source.index("new ItemInHandLayer<>(")
        exact_mesh = source.index("new ExactMobMeshLayer<>(")

        self.assertLess(clear, held_item)
        self.assertLess(held_item, exact_mesh)

    def test_coral_drowned_keeps_held_items_after_replacing_vanilla_layers(self):
        source = (CLIENT / "CoralDrownedRenderer.java").read_text(encoding="utf-8")
        clear = source.index("this.layers.clear();")
        held_item = source.index("new ItemInHandLayer<>(")
        exact_mesh = source.index("new ExactMobMeshLayer<>(")

        self.assertLess(clear, held_item)
        self.assertLess(held_item, exact_mesh)

    def test_base_model_suppression_uses_a_real_fully_transparent_png(self):
        layers = (CLIENT / "CustomMobModelLayers.java").read_text(encoding="utf-8")
        self.assertIn('texture("textures/entity/custom3d/transparent_base.png")', layers)
        texture = ROOT / "src/main/resources/assets/usless_mobs/textures/entity/custom3d/transparent_base.png"
        with Image.open(texture) as source:
            image = source.convert("RGBA")
            image.load()
        self.assertGreaterEqual(image.width, 1)
        self.assertGreaterEqual(image.height, 1)
        self.assertIsNone(image.getchannel("A").getbbox())

    def test_excluded_families_are_not_part_of_the_batch(self):
        names = " ".join(EXACT_TRIPO_RENDERERS + SPECIAL_CASE_RENDERERS).lower()
        self.assertNotIn("slime", names)
        self.assertNotIn("silverfish", names)
        self.assertNotIn("endermite", names)


if __name__ == "__main__":
    unittest.main()
