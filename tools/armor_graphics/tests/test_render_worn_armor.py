import hashlib
import math
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

    def test_every_runtime_cube_face_is_opaque_in_its_active_atlas(self):
        transparent_faces = []
        for family in render_worn_armor.FAMILIES:
            for slot in render_worn_armor.SLOTS:
                with Image.open(render_worn_armor._texture_path(ROOT, family, slot)) as source:
                    texture = source.convert("RGBA")
                for cube in render_worn_armor.assemble_piece(ROOT, family, slot):
                    for face, uv in render_worn_armor.box_uv_faces(cube["uv_origin"], cube["source_size"]).items():
                        u, v = uv["uv"]
                        width, height = uv["uv_size"]
                        pixels = [
                            texture.getpixel((x, y))[3]
                            for y in range(int(v), min(texture.height, math.ceil(v + height)))
                            for x in range(int(u), min(texture.width, math.ceil(u + width)))
                        ]
                        if not pixels or any(alpha != 255 for alpha in pixels):
                            transparent_faces.append(f"{family}/{slot}/{cube['name']}/{face}")
        self.assertEqual([], transparent_faces, f"runtime faces with missing texels: {transparent_faces[:12]}")

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

    def test_complete_set_uses_one_depth_sorted_texture_atlas(self):
        cubes, atlas = render_worn_armor._combined_set_inputs(ROOT, "void")
        expected_count = sum(
            len(render_worn_armor._render_inputs(ROOT, "void", slot))
            for slot in render_worn_armor.SLOTS
        )
        self.assertEqual(expected_count, len(cubes))
        self.assertEqual((128, 64 * len(render_worn_armor.SLOTS)), atlas.size)
        for slot_index, slot in enumerate(render_worn_armor.SLOTS):
            names = {cube["name"] for cube, _matrix in render_worn_armor._render_inputs(ROOT, "void", slot)}
            slot_cubes = [cube for cube, _matrix in cubes if cube["name"] in names]
            self.assertTrue(slot_cubes)
            for cube in slot_cubes:
                for face in cube["uv"].values():
                    self.assertGreaterEqual(face["uv"][1], slot_index * 64)
                    self.assertLessEqual(face["uv"][1] + face["uv_size"][1], (slot_index + 1) * 64)


if __name__ == "__main__":
    unittest.main()
