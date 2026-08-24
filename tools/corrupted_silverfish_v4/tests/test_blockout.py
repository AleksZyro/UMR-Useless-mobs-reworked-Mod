import json
from pathlib import Path
import unittest

from PIL import Image

from tools.corrupted_silverfish_v4 import blockout


class BlockoutContractTest(unittest.TestCase):
    def test_reference_first_proportions(self):
        summary = blockout.model_summary()
        self.assertLessEqual(summary["length"] / summary["width"], 2.25)
        self.assertGreaterEqual(summary["head_width"] / summary["armor_width"], 0.85)
        self.assertLessEqual(summary["tail_length"] / summary["length"], 0.28)
        self.assertEqual(summary["legs"], 6)
        self.assertEqual(summary["main_armor_plates"], 3)

    def test_named_visual_features_exist(self):
        names = {cube["name"] for cube in blockout.cubes()}
        for required in {
            "head_core", "mandible_left", "mandible_right", "eye_left", "eye_right",
            "armor_front", "armor_mid", "armor_rear", "tail_tip",
        }:
            self.assertIn(required, names)
        for side in ("left", "right"):
            for index in range(1, 4):
                self.assertTrue(any(name.startswith(f"leg_{side}_{index}_") for name in names))

    def test_build_outputs_blockbench_texture_and_views(self):
        paths = blockout.build()
        self.assertTrue(paths["bbmodel"].is_file())
        model = json.loads(paths["bbmodel"].read_text(encoding="utf-8"))
        self.assertEqual(model["meta"]["model_format"], "geckolib_model")
        self.assertEqual(model["resolution"], {"width": 256, "height": 256})
        self.assertEqual(len(model["textures"]), 1)
        with Image.open(paths["texture"]) as image:
            self.assertEqual((image.mode, image.size), ("RGBA", (256, 256)))
        for name in ("front", "right", "top", "perspective", "contact_sheet"):
            with Image.open(paths[name]) as image:
                self.assertEqual(image.mode, "RGBA")
                self.assertIsNotNone(image.getbbox())


if __name__ == "__main__":
    unittest.main()
