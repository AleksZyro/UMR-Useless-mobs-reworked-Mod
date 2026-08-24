from __future__ import annotations

import io
import json
import unittest

from PIL import Image

from tools.web_cave_spider_v2.cuboid_model import BONES, CUBES, build_payloads


class WebCaveSpiderV2Contract(unittest.TestCase):
    def test_curated_model_has_readable_forms_and_eight_complete_legs(self):
        self.assertGreaterEqual(len(CUBES), 75)
        self.assertLessEqual(len(CUBES), 150)
        self.assertEqual(12, len(BONES))
        self.assertEqual(len(CUBES), len({cube.name for cube in CUBES}))
        for index in range(8):
            owned = [cube for cube in CUBES if cube.bone == f"web_leg_{index}"]
            self.assertGreaterEqual(len(owned), 6, index)

    def test_cube_only_bbmodel_has_detailed_texture_and_motion(self):
        payloads = build_payloads()
        document = json.loads(payloads.bbmodel)
        self.assertEqual(len(CUBES), len(document["elements"]))
        self.assertTrue(all(element["type"] == "cube" for element in document["elements"]))
        self.assertEqual({bone.name for bone in BONES}, self._groups(document["outliner"]))
        self.assertEqual(2, len(document["animations"]))
        self.assertIn("idle", document["animations"][0]["name"])
        self.assertIn("walk", document["animations"][1]["name"])
        with Image.open(io.BytesIO(payloads.texture)) as image:
            self.assertEqual((256, 256), image.size)
            self.assertGreater(len(set(image.getdata())), 35)
        with Image.open(io.BytesIO(payloads.glowmask)) as glow:
            lit = sum(pixel[3] > 0 for pixel in glow.getdata())
            self.assertGreater(lit, 20)
            self.assertLess(lit, 1200)

    @staticmethod
    def _groups(items):
        result = set()
        for item in items:
            if isinstance(item, dict):
                result.add(item["name"])
                result.update(WebCaveSpiderV2Contract._groups(item.get("children", [])))
        return result

    def test_generation_is_deterministic(self):
        self.assertEqual(build_payloads(), build_payloads())


if __name__ == "__main__":
    unittest.main()
