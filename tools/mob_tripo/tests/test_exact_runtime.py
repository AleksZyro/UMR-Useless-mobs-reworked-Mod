from __future__ import annotations

import io
import unittest
from pathlib import Path

import numpy as np
from PIL import Image

from tools.corrupted_silverfish_v5.tripo_voxel import MeshData, load_glb
from tools.mob_tripo.exact_runtime import (
    MOB_SPECS,
    build_runtime_assets,
    decode_mesh,
    transform_positions,
)


class ExactMobRuntimeTests(unittest.TestCase):
    def setUp(self):
        self.mesh = MeshData(
            positions=np.asarray(
                [
                    [-2.0, 0.0, -1.0],
                    [2.0, 0.0, -1.0],
                    [2.0, 3.0, 1.0],
                    [-2.0, 3.0, 1.0],
                ],
                dtype=np.float64,
            ),
            uvs=np.asarray([[0.0, 0.0], [1.0, 0.0], [1.0, 1.0], [0.0, 1.0]], dtype=np.float64),
            triangles=np.asarray([[0, 1, 2], [0, 2, 3]], dtype=np.int64),
            base_colour=Image.new("RGBA", (4, 4), (40, 90, 130, 255)),
        )

    def test_batch_contains_the_exact_runtime_models(self):
        self.assertEqual(
            {
                "living_boss",
                "web_cave_spider",
                "octopus",
                "witch_boss",
                "living_bat",
                "rooted_husk",
                "helping_allay",
            },
            set(MOB_SPECS),
        )
        self.assertNotIn("frost_stray", MOB_SPECS)
        self.assertNotIn("coral_drowned", MOB_SPECS)

    def test_transform_preserves_shape_and_places_feet_at_model_y_24(self):
        actual = transform_positions(self.mesh.positions, longest_span=24.0)
        source_distances = np.linalg.norm(self.mesh.positions[1:] - self.mesh.positions[0], axis=1)
        actual_distances = np.linalg.norm(actual[1:] - actual[0], axis=1)
        ratios = actual_distances / source_distances
        np.testing.assert_allclose(ratios, np.repeat(ratios[0], len(ratios)), atol=1e-10)
        self.assertAlmostEqual(float(actual[:, 1].max()), 24.0)
        self.assertAlmostEqual(float(actual[:, 0].min() + actual[:, 0].max()), 0.0)
        self.assertAlmostEqual(float(actual[:, 2].min() + actual[:, 2].max()), 0.0)

    def test_binary_round_trip_preserves_every_triangle_and_uv(self):
        mesh_bytes, texture_bytes, report = build_runtime_assets("rooted_husk", self.mesh)
        decoded = decode_mesh(mesh_bytes)
        self.assertEqual(2, report["source_triangles"])
        self.assertEqual(2, report["output_triangles"])
        self.assertEqual(2, sum(len(part["faces"]) for part in decoded.values()))
        self.assertTrue(texture_bytes.startswith(b"\x89PNG\r\n\x1a\n"))
        with Image.open(io.BytesIO(texture_bytes)) as texture:
            self.assertEqual((4, 4), texture.size)
        emitted_uvs = {
            tuple(round(value, 6) for value in corner[1])
            for part in decoded.values()
            for face in part["faces"]
            for corner in face
        }
        self.assertEqual({(0.0, 0.0), (1.0, 0.0), (1.0, 1.0), (0.0, 1.0)}, emitted_uvs)

    def test_runtime_keeps_asymmetric_gltf_uv_coordinates_unflipped(self):
        self.mesh.uvs[0] = [0.2, 0.1]
        mesh_bytes, _, _ = build_runtime_assets("rooted_husk", self.mesh)
        decoded = decode_mesh(mesh_bytes)
        emitted_uvs = {
            tuple(round(value, 6) for value in corner[1])
            for part in decoded.values()
            for face in part["faces"]
            for corner in face
        }
        self.assertIn((0.2, 0.1), emitted_uvs)
        self.assertNotIn((0.2, 0.9), emitted_uvs)

    def test_runtime_keeps_original_tripo_albedo_pixels_for_dark_models(self):
        for name in ("living_boss", "witch_boss", "living_bat"):
            with self.subTest(name=name):
                _, texture_bytes, _ = build_runtime_assets(name, self.mesh)
                with Image.open(io.BytesIO(texture_bytes)) as texture:
                    np.testing.assert_array_equal(
                        np.asarray(self.mesh.base_colour.convert("RGBA")),
                        np.asarray(texture.convert("RGBA")),
                    )

    def test_every_spec_has_real_movable_bones(self):
        for name, spec in MOB_SPECS.items():
            with self.subTest(name=name):
                self.assertEqual("body", spec.bones[0])
                self.assertGreaterEqual(len(spec.bones), 3)
                self.assertEqual(len(spec.bones), len(set(spec.bones)))

    def test_each_mob_uses_its_gameplay_dimension_instead_of_one_global_size(self):
        expected = {
            "living_boss": (0, 31.2),
            "web_cave_spider": (0, 11.2),
            "octopus": (0, 14.4),
            "witch_boss": (1, 31.2),
            "living_bat": (0, 8.0),
            "rooted_husk": (1, 31.2),
            "helping_allay": (1, 10.4),
        }
        for name, (axis, span) in expected.items():
            with self.subTest(name=name):
                spec = MOB_SPECS[name]
                self.assertTrue(hasattr(spec, "fit_axis"), "MobSpec needs an explicit fit axis")
                self.assertTrue(hasattr(spec, "fit_span"), "MobSpec needs an explicit gameplay span")
                actual = transform_positions(
                    self.mesh.positions,
                    fit_axis=spec.fit_axis,
                    fit_span=spec.fit_span,
                )
                self.assertEqual(axis, spec.fit_axis)
                self.assertAlmostEqual(span, spec.fit_span)
                self.assertAlmostEqual(span, float(np.ptp(actual[:, axis])), places=5)

    def test_helping_allay_spec_uses_nine_visible_motion_regions(self):
        spec = MOB_SPECS["helping_allay"]
        self.assertEqual(1, spec.fit_axis)
        self.assertAlmostEqual(10.4, spec.fit_span)
        self.assertEqual(
            (
                "body",
                "head",
                "right_arm",
                "left_arm",
                "right_wing",
                "right_wing_tip",
                "left_wing",
                "left_wing_tip",
                "soul_core",
            ),
            spec.bones,
        )

    def test_helping_allay_export_preserves_every_source_triangle(self):
        mesh_bytes, texture_bytes, report = build_runtime_assets("helping_allay", self.mesh)
        decoded = decode_mesh(mesh_bytes)
        self.assertEqual(report["source_triangles"], report["output_triangles"])
        self.assertEqual(
            report["source_triangles"],
            sum(len(part["faces"]) for part in decoded.values()),
        )
        self.assertEqual(set(MOB_SPECS["helping_allay"].bones), set(decoded))
        self.assertTrue(texture_bytes.startswith(b"\x89PNG\r\n\x1a\n"))

    def test_octopus_preserves_all_triangles_in_nine_nonempty_regions(self):
        root = Path(__file__).resolve().parents[3]
        source = load_glb(
            root
            / "Modelle/Exports/octopus_v1/tripo_export/octopus_tripo_textured_4k_20260821.glb"
        )

        payload, _, report = build_runtime_assets("octopus", source)
        decoded = decode_mesh(payload)

        self.assertEqual(source.triangles.shape[0], report["source_triangles"])
        self.assertEqual(report["source_triangles"], report["output_triangles"])
        self.assertEqual({"body", *(f"tentacle{i}" for i in range(8))}, set(decoded))
        self.assertTrue(all(len(part["faces"]) > 0 for part in decoded.values()))
        self.assertEqual((4096, 4096), (report["texture_width"], report["texture_height"]))
        self.assertEqual(0, report["cubes"])
        self.assertEqual(14.4, report["fit_span"])


if __name__ == "__main__":
    unittest.main()
