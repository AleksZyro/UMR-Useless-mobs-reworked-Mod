from __future__ import annotations

import importlib.util
import json
from pathlib import Path
import sys
import unittest

import numpy as np
from PIL import Image


ROOT = Path(__file__).resolve().parents[3]
MODULE_PATH = ROOT / "tools" / "armor_graphics" / "tripo_to_blockbench.py"
EXPORT_ROOT = ROOT / "Modelle" / "Exports" / "armor_graphics_tripo"


def load_module():
    spec = importlib.util.spec_from_file_location("armor_tripo_to_blockbench", MODULE_PATH)
    if spec is None or spec.loader is None:
        raise RuntimeError("Could not load armor Tripo converter")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


class TripoToBlockbenchContract(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.converter = load_module()
        cls.family = "true_void"
        cls.piece = "chestplate"
        cls.glb = EXPORT_ROOT / "models" / cls.family / cls.piece / "tripo_highpoly.glb"
        cls.views = EXPORT_ROOT / cls.family / cls.piece

    def test_compressed_geometry_without_uvs_decodes(self):
        mesh = self.converter.load_geometry(self.glb)
        self.assertEqual(mesh.positions.ndim, 2)
        self.assertEqual(mesh.positions.shape[1], 3)
        self.assertEqual(mesh.triangles.ndim, 2)
        self.assertEqual(mesh.triangles.shape[1], 3)
        self.assertGreater(len(mesh.positions), 100_000)
        self.assertGreater(len(mesh.triangles), 100_000)
        self.assertTrue(np.isfinite(mesh.positions).all())
        self.assertLess(int(mesh.triangles.max()), len(mesh.positions))

    def test_external_multiview_build_is_deterministic(self):
        first = self.converter.build_candidate(self.glb, self.views, resolution=24)
        second = self.converter.build_candidate(self.glb, self.views, resolution=24)
        self.assertGreater(first.occupied_voxel_count, 300)
        self.assertLess(first.cuboid_count, 5_000)
        self.assertEqual(first, second)
        model_a, texture_a = self.converter.candidate_bytes(first, self.family, self.piece)
        model_b, texture_b = self.converter.candidate_bytes(second, self.family, self.piece)
        self.assertEqual(model_a, model_b)
        self.assertEqual(texture_a, texture_b)

        document = json.loads(model_a)
        self.assertEqual(document["meta"]["model_format"], "free")
        self.assertEqual(document["name"], "true_void_chestplate_tripo_cubes")
        self.assertEqual(len(document["elements"]), first.cuboid_count)
        self.assertEqual(len({element["uuid"] for element in document["elements"]}), first.cuboid_count)
        self.assertTrue(all(element["type"] == "cube" for element in document["elements"]))

        from io import BytesIO

        with Image.open(BytesIO(texture_a)) as image:
            self.assertEqual(image.mode, "RGBA")
            self.assertEqual(image.size, (16, 16))

    def test_chestplate_uses_blockbench_width_height_depth_axes(self):
        candidate = self.converter.build_candidate(self.glb, self.views, resolution=24)
        lower = [min(cube.lower[axis] for cube in candidate.cuboids) for axis in range(3)]
        upper = [max(cube.upper[axis] for cube in candidate.cuboids) for axis in range(3)]
        width, height, depth = [upper[axis] - lower[axis] for axis in range(3)]
        self.assertGreater(width, height)
        self.assertGreater(height, depth)

    def test_missing_required_view_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "required multiview image"):
            self.converter.load_views(self.views, names=("front", "missing"))

    def test_mesh_decoder_dependency_is_reproducibly_declared(self):
        requirements = ROOT / "tools" / "armor_graphics" / "requirements-tripo.txt"
        self.assertEqual(requirements.read_text(encoding="utf-8").splitlines(), [
            "meshoptimizer==0.2.30a0",
        ])

    def test_preview_is_a_visible_rgba_png(self):
        candidate = self.converter.build_candidate(self.glb, self.views, resolution=24)
        payload = self.converter.render_preview(candidate, size=512)
        from io import BytesIO

        with Image.open(BytesIO(payload)) as image:
            self.assertEqual(image.mode, "RGBA")
            self.assertEqual(image.size, (512, 512))
            alpha = np.asarray(image)[:, :, 3]
            self.assertGreater(int((alpha > 0).sum()), 5_000)


if __name__ == "__main__":
    unittest.main()
