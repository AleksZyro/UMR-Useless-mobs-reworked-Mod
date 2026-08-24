from __future__ import annotations

import io
import json
import unittest

from PIL import Image

from tools.living_boss_tripo.cuboid_model import (
    BONES,
    CUBES,
    FACE_NAMES,
    TEXTURE_SIZE,
    build_payloads,
)


EXPECTED_BONES = {
    "root",
    "body",
    "head",
    "leg_front_left",
    "leg_front_right",
    "leg_rear_left",
    "leg_rear_right",
    "roots_body",
}


class LivingBossCuboidContract(unittest.TestCase):
    def test_uses_curated_cuboids_instead_of_a_triangle_mesh(self):
        self.assertEqual(EXPECTED_BONES, {bone.name for bone in BONES})
        self.assertGreaterEqual(len(CUBES), 100)
        self.assertLessEqual(len(CUBES), 220)
        self.assertTrue(all(cube.bone in EXPECTED_BONES for cube in CUBES))
        self.assertEqual(len(CUBES), len({cube.name for cube in CUBES}))

    def test_four_leg_bones_are_separate_and_reach_the_floor(self):
        for station in ("front", "rear"):
            for side in ("left", "right"):
                bone = f"leg_{station}_{side}"
                cubes = [cube for cube in CUBES if cube.bone == bone]
                self.assertGreaterEqual(len(cubes), 8, bone)
                self.assertEqual(0.0, min(cube.origin[1] for cube in cubes), bone)

    def test_model_has_large_forms_and_small_surface_details(self):
        volumes = sorted(cube.size[0] * cube.size[1] * cube.size[2] for cube in CUBES)
        self.assertGreater(volumes[-1], 900.0)
        self.assertLessEqual(volumes[0], 1.0)
        categories = {cube.category for cube in CUBES}
        self.assertTrue({"mass", "plate", "root", "crystal", "face"} <= categories)

    def test_bbmodel_is_cube_only_with_bounded_nonoverlapping_uvs(self):
        payloads = build_payloads()
        document = json.loads(payloads.bbmodel)
        self.assertEqual(len(CUBES), len(document["elements"]))
        self.assertTrue(all(element["type"] == "cube" for element in document["elements"]))
        self.assertFalse(any(element.get("type") == "mesh" for element in document["elements"]))
        rectangles = []
        for element in document["elements"]:
            self.assertEqual(set(FACE_NAMES), set(element["faces"]))
            for face in element["faces"].values():
                left, top, right, bottom = face["uv"]
                self.assertTrue(0 <= left < right <= TEXTURE_SIZE)
                self.assertTrue(0 <= top < bottom <= TEXTURE_SIZE)
                rectangles.append((left, top, right, bottom))
        for index, first in enumerate(rectangles):
            for second in rectangles[index + 1 :]:
                overlap = first[0] < second[2] and second[0] < first[2] and first[1] < second[3] and second[1] < first[3]
                self.assertFalse(overlap, (first, second))

    def test_texture_and_glowmask_are_detailed_rgba_atlases(self):
        payloads = build_payloads()
        with Image.open(io.BytesIO(payloads.texture)) as texture:
            self.assertEqual((TEXTURE_SIZE, TEXTURE_SIZE), texture.size)
            self.assertEqual("RGBA", texture.mode)
            opaque = [pixel for pixel in texture.getdata() if pixel[3]]
            self.assertGreater(len(set(opaque)), 20)
        with Image.open(io.BytesIO(payloads.glowmask)) as glow:
            self.assertEqual((TEXTURE_SIZE, TEXTURE_SIZE), glow.size)
            glowing = sum(pixel[3] > 0 for pixel in glow.getdata())
            self.assertGreater(glowing, 30)
            self.assertLess(glowing, 1600)

    def test_generation_is_byte_deterministic(self):
        first = build_payloads()
        second = build_payloads()
        self.assertEqual(first, second)


if __name__ == "__main__":
    unittest.main()
