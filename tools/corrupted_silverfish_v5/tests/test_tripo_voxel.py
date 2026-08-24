from pathlib import Path
import json
import unittest

import numpy as np

from tools.corrupted_silverfish_v5.tripo_voxel import (
    build_candidate,
    candidate_bytes,
    load_glb,
    normalise_positions,
)


ROOT = Path(__file__).resolve().parents[3]
GLB = (
    ROOT
    / "Modelle"
    / "Exports"
    / "corrupted_silverfish_v5"
    / "tripo_export"
    / "corrupted_silverfish_tripo_multiview_v5.glb"
)


class TripoGlbTests(unittest.TestCase):
    def test_load_glb_extracts_mesh_uv_and_base_colour(self):
        model = load_glb(GLB)

        self.assertEqual(model.positions.ndim, 2)
        self.assertEqual(model.positions.shape[1], 3)
        self.assertEqual(model.uvs.shape, (len(model.positions), 2))
        self.assertEqual(model.triangles.shape[1], 3)
        self.assertEqual(model.base_colour.mode, "RGBA")
        self.assertGreater(model.base_colour.width, 1)
        self.assertGreater(model.base_colour.height, 1)
        self.assertTrue(np.isfinite(model.positions).all())
        self.assertTrue(np.isfinite(model.uvs).all())

    def test_normalise_positions_sets_length_and_floor(self):
        model = load_glb(GLB)
        positions = normalise_positions(model.positions, target_length=32.0)
        extent = positions.max(axis=0) - positions.min(axis=0)

        self.assertAlmostEqual(float(extent.max()), 32.0, places=5)
        self.assertAlmostEqual(float(positions[:, 1].min()), 0.0, places=5)
        self.assertAlmostEqual(float((positions[:, 0].min() + positions[:, 0].max()) / 2), 0.0, places=5)
        self.assertAlmostEqual(float((positions[:, 2].min() + positions[:, 2].max()) / 2), 0.0, places=5)

    def test_rejects_non_glb(self):
        with self.subTest("short file"):
            bad = Path(__file__).with_name("not-a-model.glb")
            bad.write_bytes(b"no")
            try:
                with self.assertRaisesRegex(ValueError, "GLB"):
                    load_glb(bad)
            finally:
                bad.unlink(missing_ok=True)


class CandidateTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.candidate = build_candidate(GLB)

    def test_candidate_is_bounded_merged_and_textured(self):
        candidate = self.candidate
        self.assertGreater(candidate.occupied_voxel_count, 500)
        self.assertLess(candidate.cuboid_count, candidate.occupied_voxel_count)
        self.assertLess(candidate.cuboid_count, 5000)
        self.assertEqual(candidate.texture_size, (16, 16))
        self.assertTrue(candidate.all_uvs_in_bounds)
        self.assertGreaterEqual(max(colour[0] for colour in candidate.palette), 200)
        self.assertGreaterEqual(
            max(max(colour[:3]) - min(colour[:3]) for colour in candidate.palette),
            120,
        )

    def test_candidate_bytes_are_deterministic_and_structural(self):
        first_model, first_texture = candidate_bytes(self.candidate)
        second_model, second_texture = candidate_bytes(build_candidate(GLB))
        self.assertEqual(first_model, second_model)
        self.assertEqual(first_texture, second_texture)
        document = json.loads(first_model)
        self.assertEqual(document["meta"]["model_format"], "geckolib_model")
        self.assertEqual(len(document["elements"]), self.candidate.cuboid_count)
        self.assertEqual(len(document["textures"]), 1)


if __name__ == "__main__":
    unittest.main()
