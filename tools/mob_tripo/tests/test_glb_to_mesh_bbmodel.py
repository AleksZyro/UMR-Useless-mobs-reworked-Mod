from __future__ import annotations

import base64
import json
import tempfile
import unittest
from pathlib import Path

import numpy as np
from PIL import Image

from tools.armor_graphics.tripo_to_blockbench import Geometry
from tools.mob_tripo.glb_to_mesh_bbmodel import (
    build_document,
    build_texture_atlas,
    transformed_positions,
    write_document,
)


class ExactMeshConversionTests(unittest.TestCase):
    def setUp(self):
        self.geometry = Geometry(
            positions=np.asarray(
                [
                    [-2.0, 0.0, -1.0],
                    [2.0, 0.0, -1.0],
                    [2.0, 3.0, 1.0],
                    [-2.0, 3.0, 1.0],
                ],
                dtype=np.float64,
            ),
            triangles=np.asarray([[0, 1, 2], [0, 2, 3]], dtype=np.int64),
        )

    def test_transform_is_only_uniform_scale_and_translation(self):
        actual = transformed_positions(self.geometry.positions, longest_span=24.0)
        source_distances = np.linalg.norm(self.geometry.positions[1:] - self.geometry.positions[0], axis=1)
        actual_distances = np.linalg.norm(actual[1:] - actual[0], axis=1)
        ratios = actual_distances / source_distances
        np.testing.assert_allclose(ratios, np.repeat(ratios[0], len(ratios)), atol=1e-10)
        self.assertAlmostEqual(float(actual[:, 1].min()), 0.0)
        self.assertAlmostEqual(float(actual[:, 0].min() + actual[:, 0].max()), 0.0)
        self.assertAlmostEqual(float(actual[:, 2].min() + actual[:, 2].max()), 0.0)

    def test_every_source_triangle_becomes_one_mesh_face_and_no_cube(self):
        atlas = Image.new("RGBA", (32, 32), (96, 96, 96, 255))
        document = build_document("test_mesh", self.geometry, atlas)
        self.assertEqual(["mesh"], [element["type"] for element in document["elements"]])
        mesh = document["elements"][0]
        self.assertEqual(len(self.geometry.positions), len(mesh["vertices"]))
        self.assertEqual(len(self.geometry.triangles), len(mesh["faces"]))
        source = [tuple(map(int, triangle)) for triangle in self.geometry.triangles]
        vertex_order = {vertex_id: index for index, vertex_id in enumerate(mesh["vertices"])}
        converted = [
            tuple(vertex_order[vertex_id] for vertex_id in face["vertices"])
            for face in mesh["faces"].values()
        ]
        self.assertEqual(source, converted)
        self.assertTrue(all(face["texture"] == 0 for face in mesh["faces"].values()))

    def test_atlas_removes_green_background_without_repainting_subject(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            colours = {
                "front": (210, 30, 40, 255),
                "back": (30, 210, 40, 255),
                "left": (30, 40, 210, 255),
                "right": (210, 180, 30, 255),
            }
            for name, colour in colours.items():
                image = Image.new("RGBA", (8, 8), (0, 255, 0, 255))
                image.putpixel((4, 4), colour)
                image.save(root / f"{name}.png")
            atlas = build_texture_atlas(root, cell_size=8)
            self.assertEqual((16, 16), atlas.size)
            self.assertEqual(colours["front"], atlas.getpixel((4, 4)))
            self.assertEqual(0, atlas.getpixel((0, 0))[3])

    def test_writer_is_atomic_and_byte_deterministic(self):
        atlas = Image.new("RGBA", (32, 32), (96, 96, 96, 255))
        document = build_document("test_mesh", self.geometry, atlas)
        with tempfile.TemporaryDirectory() as temp:
            first = Path(temp) / "first.bbmodel"
            second = Path(temp) / "second.bbmodel"
            write_document(first, document)
            write_document(second, document)
            self.assertEqual(first.read_bytes(), second.read_bytes())
            parsed = json.loads(first.read_text(encoding="utf-8"))
            source = parsed["textures"][0]["source"]
            self.assertTrue(source.startswith("data:image/png;base64,"))
            self.assertGreater(len(base64.b64decode(source.split(",", 1)[1])), 32)
            self.assertFalse(list(Path(temp).glob("*.tmp")))


if __name__ == "__main__":
    unittest.main()
