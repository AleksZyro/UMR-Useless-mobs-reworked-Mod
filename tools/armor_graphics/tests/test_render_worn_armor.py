import hashlib
import unittest
from pathlib import Path

from PIL import Image

from tools.armor_graphics import render_worn_armor


ROOT = Path(__file__).resolve().parents[3]


class WornArmorOfflineRenderContract(unittest.TestCase):
    def test_all_path_pieces_assemble_real_bone_owned_geometry(self):
        expected_minimums = {
            "helmet": 11,
            "chestplate": 11,
            "leggings": 11,
            "boots": 10,
        }
        for family in render_worn_armor.FAMILIES:
            for slot, minimum in expected_minimums.items():
                with self.subTest(family=family, slot=slot):
                    cubes = render_worn_armor.assemble_piece(ROOT, family, slot)
                    self.assertGreaterEqual(len(cubes), minimum)
                    self.assertTrue({cube["bone"] for cube in cubes} <= render_worn_armor.BONE_OFFSETS.keys())
                    self.assertEqual(len(cubes), len({cube["name"] for cube in cubes}))

    def test_java_box_uvs_expand_to_six_bounded_faces(self):
        faces = render_worn_armor.box_uv_faces((10.0, 12.0), (4.0, 6.0, 2.0))
        self.assertEqual({"north", "east", "south", "west", "up", "down"}, set(faces))
        for face in faces.values():
            u, v = face["uv"]
            width, height = face["uv_size"]
            self.assertGreater(width, 0)
            self.assertGreater(height, 0)
            self.assertGreaterEqual(u, 0)
            self.assertGreaterEqual(v, 0)
            self.assertLessEqual(u + width, 128)
            self.assertLessEqual(v + height, 64)

    def test_piece_render_is_textured_nonempty_and_deterministic(self):
        first = render_worn_armor.render_piece(ROOT, "void", "chestplate", "three_quarter")
        second = render_worn_armor.render_piece(ROOT, "void", "chestplate", "three_quarter")
        self.assertEqual((512, 512), first.size)
        self.assertEqual("RGBA", first.mode)
        self.assertGreater(first.getbbox()[2] - first.getbbox()[0], 100)
        self.assertGreater(first.getbbox()[3] - first.getbbox()[1], 100)
        self.assertEqual(hashlib.sha256(first.tobytes()).digest(), hashlib.sha256(second.tobytes()).digest())
        colours = {pixel for pixel in first.getdata() if pixel[3]}
        self.assertGreater(len(colours), 8, "the preview must use the real detailed atlas, not one flat colour")

    def test_contact_sheet_contains_every_family_slot_and_view(self):
        for family in render_worn_armor.FAMILIES:
            for view in render_worn_armor.VIEWS:
                with self.subTest(family=family, view=view):
                    preview = render_worn_armor.render_set(ROOT, family, view)
                    self.assertEqual((512, 512), preview.size)
                    self.assertIsNotNone(preview.getbbox())
        sheet = render_worn_armor.build_contact_sheet(ROOT)
        self.assertEqual((2048, 2048), sheet.size)
        self.assertEqual("RGBA", sheet.mode)
        self.assertIsNotNone(sheet.getbbox())


if __name__ == "__main__":
    unittest.main()
