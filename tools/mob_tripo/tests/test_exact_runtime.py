from __future__ import annotations

import io
import json
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
                "squid",
                "glow_squid",
                "witch_boss",
                "living_bat",
                "rooted_husk",
                "helping_allay",
                "polar_bear",
                "frost_stray",
                "coral_drowned",
                "axolotl",
                "ocelot",
            },
            set(MOB_SPECS),
        )

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
            "squid": (2, 22.4),
            "glow_squid": (1, 24.0),
            "witch_boss": (1, 31.2),
            "living_bat": (0, 8.0),
            "rooted_husk": (1, 31.2),
            "helping_allay": (1, 10.4),
            "polar_bear": (2, 30.4),
            "frost_stray": (1, 31.2),
            "coral_drowned": (1, 31.2),
            "axolotl": (2, 20.8),
            "ocelot": (2, 23.2),
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

    def test_ocelot_spec_is_a_seven_region_quadruped(self):
        spec = MOB_SPECS["ocelot"]
        self.assertEqual(
            (
                "body",
                "head",
                "tail",
                "leg_front_left",
                "leg_front_right",
                "leg_rear_left",
                "leg_rear_right",
            ),
            spec.bones,
        )
        self.assertEqual("ocelot", spec.classifier)
        self.assertEqual(2, spec.fit_axis)
        self.assertEqual(23.2, spec.fit_span)

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

    def test_regenerated_helping_allay_is_the_active_exact_4k_source(self):
        root = Path(__file__).resolve().parents[3]
        source = load_glb(
            root
            / "Modelle/Exports/helping_allay_v1/source/helping_allay_runtime_optimized_4k.glb"
        )
        report = json.loads(
            (
                root
                / "src/main/resources/assets/usless_mobs/meshes/entity/custom3d/helping_allay.report.json"
            ).read_text(encoding="utf-8")
        )

        self.assertEqual(99241, source.triangles.shape[0])
        self.assertEqual(source.triangles.shape[0], report["source_triangles"])
        self.assertEqual(report["source_triangles"], report["output_triangles"])
        self.assertEqual((4096, 4096), (report["texture_width"], report["texture_height"]))
        self.assertEqual(0, report["cubes"])

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

    def test_squid_preserves_4k_texture_and_all_triangles_in_eleven_regions(self):
        root = Path(__file__).resolve().parents[3]
        source = load_glb(root / "Modelle/Exports/squid_v1/source/squid_runtime_optimized_4k.glb")

        payload, _, report = build_runtime_assets("squid", source)
        decoded = decode_mesh(payload)

        self.assertEqual(source.triangles.shape[0], report["source_triangles"])
        self.assertEqual(report["source_triangles"], report["output_triangles"])
        self.assertEqual(set(MOB_SPECS["squid"].bones), set(decoded))
        self.assertTrue(all(len(part["faces"]) > 0 for part in decoded.values()))
        self.assertEqual((4096, 4096), (report["texture_width"], report["texture_height"]))
        self.assertEqual(0, report["cubes"])
        self.assertEqual(22.4, report["fit_span"])

    def test_glow_squid_preserves_4k_texture_and_all_triangles_in_eleven_regions(self):
        root = Path(__file__).resolve().parents[3]
        source = load_glb(root / "Modelle/Exports/glow_squid_v1/source/glow_squid_runtime_optimized_4k.glb")
        asset_root = root / "src/main/resources/assets/usless_mobs"
        payload = (asset_root / "meshes/entity/custom3d/glow_squid.mesh").read_bytes()
        report = json.loads((asset_root / "meshes/entity/custom3d/glow_squid.report.json").read_text())
        self.assertEqual(100030, source.triangles.shape[0])
        self.assertEqual(source.triangles.shape[0], report["source_triangles"])
        self.assertEqual(report["source_triangles"], report["output_triangles"])
        self.assertTrue(payload.startswith(b"UMMESH1\0"))
        self.assertEqual(set(MOB_SPECS["glow_squid"].bones), set(report["bones"]))
        self.assertTrue(all(face_count > 0 for face_count in report["bones"].values()))
        self.assertEqual((4096, 4096), (report["texture_width"], report["texture_height"]))
        self.assertEqual(0, report["cubes"])
        self.assertEqual(24.0, report["fit_span"])

    def test_polar_bear_discards_the_verified_floating_shell_and_keeps_4k_uvs(self):
        root = Path(__file__).resolve().parents[3]
        source = load_glb(root / "Modelle/Exports/polar_bear_v1/source/polar_bear_runtime_optimized_4k.glb")

        payload, _, report = build_runtime_assets("polar_bear", source)
        decoded = decode_mesh(payload)

        self.assertEqual(95888, source.triangles.shape[0])
        self.assertEqual(source.triangles.shape[0], report["source_triangles"])
        self.assertEqual(84682, report["output_triangles"])
        self.assertEqual(11206, report["discarded_detached_triangles"])
        self.assertEqual(set(MOB_SPECS["polar_bear"].bones), set(decoded))
        self.assertTrue(all(len(part["faces"]) > 0 for part in decoded.values()))
        self.assertEqual((4096, 4096), (report["texture_width"], report["texture_height"]))
        self.assertEqual(0, report["cubes"])
        self.assertEqual(30.4, report["fit_span"])

    def test_frost_stray_preserves_4k_texture_and_all_triangles_in_six_regions(self):
        root = Path(__file__).resolve().parents[3]
        source = load_glb(
            root
            / "Modelle/Exports/frost_stray_v1/source/frost_stray_runtime_optimized_4k.glb"
        )

        payload, _, report = build_runtime_assets("frost_stray", source)
        decoded = decode_mesh(payload)

        self.assertEqual(98103, source.triangles.shape[0])
        self.assertEqual(source.triangles.shape[0], report["source_triangles"])
        self.assertEqual(report["source_triangles"], report["output_triangles"])
        self.assertEqual(set(MOB_SPECS["frost_stray"].bones), set(decoded))
        self.assertTrue(all(len(part["faces"]) > 0 for part in decoded.values()))
        self.assertEqual((4096, 4096), (report["texture_width"], report["texture_height"]))
        self.assertEqual(0, report["cubes"])
        self.assertEqual(31.2, report["fit_span"])
        active = json.loads(
            (
                root
                / "src/main/resources/assets/usless_mobs/meshes/entity/custom3d/frost_stray.report.json"
            ).read_text(encoding="utf-8")
        )
        self.assertEqual(source.triangles.shape[0], active["source_triangles"])

    def test_coral_drowned_preserves_oriented_4k_source_and_every_triangle(self):
        root = Path(__file__).resolve().parents[3]
        source = load_glb(
            root
            / "Modelle/Exports/coral_drowned_v1/source/coral_drowned_runtime_optimized_4k.glb"
        )

        payload, _, report = build_runtime_assets("coral_drowned", source)
        decoded = decode_mesh(payload)

        self.assertEqual(102563, source.triangles.shape[0])
        self.assertGreater(float(np.ptp(source.positions[:, 1])), float(np.ptp(source.positions[:, 0])))
        self.assertEqual(source.triangles.shape[0], report["source_triangles"])
        self.assertEqual(report["source_triangles"], report["output_triangles"])
        self.assertEqual(set(MOB_SPECS["coral_drowned"].bones), set(decoded))
        self.assertTrue(all(len(part["faces"]) > 0 for part in decoded.values()))
        self.assertEqual((4096, 4096), (report["texture_width"], report["texture_height"]))
        self.assertEqual(0, report["cubes"])
        self.assertEqual(31.2, report["fit_span"])
        active = json.loads(
            (
                root
                / "src/main/resources/assets/usless_mobs/meshes/entity/custom3d/coral_drowned.report.json"
            ).read_text(encoding="utf-8")
        )
        self.assertEqual(source.triangles.shape[0], active["source_triangles"])

    def test_axolotl_preserves_4k_texture_and_every_source_triangle(self):
        root = Path(__file__).resolve().parents[3]
        source = load_glb(
            root
            / "Modelle/Exports/axolotl_v1/source/axolotl_runtime_optimized_4k.glb"
        )

        payload, _, report = build_runtime_assets("axolotl", source)
        decoded = decode_mesh(payload)

        self.assertEqual(98535, source.triangles.shape[0])
        self.assertEqual(source.triangles.shape[0], report["source_triangles"])
        self.assertEqual(report["source_triangles"], report["output_triangles"])
        self.assertEqual(set(MOB_SPECS["axolotl"].bones), set(decoded))
        self.assertTrue(all(len(part["faces"]) > 0 for part in decoded.values()))
        self.assertEqual((4096, 4096), (report["texture_width"], report["texture_height"]))
        self.assertEqual(0, report["cubes"])
        self.assertEqual(20.8, report["fit_span"])
        active = json.loads(
            (
                root
                / "src/main/resources/assets/usless_mobs/meshes/entity/custom3d/axolotl.report.json"
            ).read_text(encoding="utf-8")
        )
        self.assertEqual(source.triangles.shape[0], active["source_triangles"])

    def test_ocelot_preserves_4k_texture_and_every_source_triangle(self):
        root = Path(__file__).resolve().parents[3]
        source = load_glb(root / "Modelle/Exports/ocelot_v1/source/ocelot_runtime_optimized_4k.glb")

        payload, _, report = build_runtime_assets("ocelot", source)
        decoded = decode_mesh(payload)

        self.assertEqual(101058, source.triangles.shape[0])
        self.assertEqual(source.triangles.shape[0], report["source_triangles"])
        self.assertEqual(report["source_triangles"], report["output_triangles"])
        self.assertEqual(set(MOB_SPECS["ocelot"].bones), set(decoded))
        self.assertTrue(all(len(part["faces"]) > 0 for part in decoded.values()))
        self.assertEqual((4096, 4096), (report["texture_width"], report["texture_height"]))
        self.assertEqual(0, report["cubes"])
        self.assertEqual(23.2, report["fit_span"])


if __name__ == "__main__":
    unittest.main()
