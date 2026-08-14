from __future__ import annotations

import hashlib
import json
from pathlib import Path
import tempfile
import unittest
from io import BytesIO

from PIL import Image

from tools.corrupted_silverfish_v3 import render


def _png_digest(image: Image.Image) -> bytes:
    buffer = BytesIO()
    image.save(buffer, format="PNG")
    return hashlib.sha256(buffer.getvalue()).digest()


class AnimationSamplingContract(unittest.TestCase):
    def test_linear_keyframes_and_loop_boundary_are_sampled(self):
        channel = {
            "0": {"post": [0, 0, 0], "lerp_mode": "linear"},
            "1": {"post": [10, 20, 30], "lerp_mode": "linear"},
        }
        self.assertEqual(render.sample_channel(channel, 0.25, 1.0, False), (2.5, 5.0, 7.5))
        self.assertEqual(render.sample_channel(channel, 1.25, 1.0, True), (2.5, 5.0, 7.5))

    def test_invalid_keyframes_fail_with_context(self):
        with self.assertRaisesRegex(render.RenderFailure, "rotation.*finite Vec3"):
            render.sample_channel({"0": {"post": [0, "bad", 0]}}, 0, 1, False, "rotation")


class HierarchyContract(unittest.TestCase):
    def test_parent_position_rotation_and_scale_transform_child(self):
        bones = [
            {"name": "root", "pivot": [0, 0, 0]},
            {"name": "child", "parent": "root", "pivot": [1, 0, 0]},
        ]
        pose = {
            "root": {"position": (2, 0, 0), "rotation": (0, 0, 90), "scale": (2, 2, 2)},
            "child": {"position": (0, 0, 0), "rotation": (0, 0, 0), "scale": (1, 1, 1)},
        }
        transforms = render.build_bone_transforms(bones, pose)
        point = render.transform_point(transforms["child"], (2, 0, 0))
        self.assertAlmostEqual(point[0], 2.0, places=6)
        self.assertAlmostEqual(point[1], 4.0, places=6)

    def test_unknown_parent_cycle_and_negative_scale_fail_clearly(self):
        with self.assertRaisesRegex(render.RenderFailure, "unknown parent.*missing"):
            render.build_bone_transforms([{"name": "a", "parent": "missing"}], {})
        with self.assertRaisesRegex(render.RenderFailure, "cycle.*a"):
            render.build_bone_transforms(
                [{"name": "a", "parent": "b"}, {"name": "b", "parent": "a"}], {}
            )
        with self.assertRaisesRegex(render.RenderFailure, "negative scale.*a"):
            render.build_bone_transforms(
                [{"name": "a"}], {"a": {"scale": (-1, 1, 1)}}
            )


class RasterContract(unittest.TestCase):
    def test_nearer_textured_cube_occludes_far_cube(self):
        texture = Image.new("RGBA", (4, 2), (0, 0, 0, 0))
        for y in range(2):
            for x in range(2):
                texture.putpixel((x, y), (210, 30, 40, 255))
            for x in range(2, 4):
                texture.putpixel((x, y), (30, 70, 220, 255))
        uv_red = {name: {"uv": [0, 0], "uv_size": [2, 2]} for name in render.FACE_NAMES}
        uv_blue = {name: {"uv": [2, 0], "uv_size": [2, 2]} for name in render.FACE_NAMES}
        far = {"name": "far", "origin": [-1, -1, 1], "size": [2, 2, 1], "uv": uv_blue}
        near = {"name": "near", "origin": [-1, -1, -1], "size": [2, 2, 1], "uv": uv_red}
        image = render.render_cubes(
            [(far, render.identity_matrix()), (near, render.identity_matrix())],
            texture,
            render.camera_for("front"),
            canvas_size=(96, 96),
            pixels_per_unit=24,
            center_world=(0, 0, 0),
        )
        self.assertEqual(image.getpixel((48, 48)), (210, 30, 40, 255))
        self.assertNotIn((0, 255, 0, 255), set(image.getdata()))


class CommittedRenderContract(unittest.TestCase):
    ROOT = Path(__file__).resolve().parents[3]

    def test_renderer_reads_disk_assets_and_produces_deterministic_review_set(self):
        with tempfile.TemporaryDirectory() as first, tempfile.TemporaryDirectory() as second:
            concept = render.DEFAULT_CONCEPT if render.DEFAULT_CONCEPT.is_file() else None
            first_paths = render.render_review_set(self.ROOT, Path(first), concept_path=concept)
            second_paths = render.render_review_set(self.ROOT, Path(second), concept_path=concept)
            self.assertEqual(set(first_paths), set(render.OUTPUT_NAMES))
            digests = {}
            for name in render.OUTPUT_NAMES:
                one = Path(first_paths[name]).read_bytes()
                two = Path(second_paths[name]).read_bytes()
                self.assertEqual(hashlib.sha256(one).digest(), hashlib.sha256(two).digest(), name)
                digests[name] = hashlib.sha256(one).digest()
                committed = self.ROOT / render.REVIEW_RELATIVE / name
                if committed.is_file():
                    self.assertEqual(one, committed.read_bytes(), f"fresh render differs from committed {name}")
                with Image.open(first_paths[name]) as image:
                    self.assertEqual(image.size, (768, 768))
                    self.assertEqual(image.mode, "RGBA")
                    self.assertIsNotNone(image.getbbox())
                    if name != "candidate_contact_sheet.png":
                        alpha = image.getchannel("A")
                        self.assertEqual(alpha.getpixel((0, 0)), 0)
                        bounds = alpha.getbbox()
                        self.assertGreaterEqual(bounds[0], 48)
                        self.assertGreaterEqual(bounds[1], 48)
                        self.assertLessEqual(bounds[2], 720)
                        self.assertLessEqual(bounds[3], 720)
            self.assertEqual(len(set(digests[name] for name in render.OUTPUT_NAMES[:7])), 7)

    def test_requested_key_poses_project_to_distinct_frames(self):
        assets = render.load_assets(self.ROOT)
        rest = render.render_model(assets, "three_quarter")
        idle = render.render_model(assets, "three_quarter", "animation.corrupted_silverfish.idle", 0.8)
        walk = render.render_model(assets, "three_quarter", "animation.corrupted_silverfish.walk", 0.2)
        attack = render.render_model(assets, "three_quarter", "animation.corrupted_silverfish.attack", 0.225)
        self.assertEqual(render._pose_for(assets, "animation.corrupted_silverfish.idle", 0.8)["body"]["position"], (0, 0.12, 0))
        self.assertIn("leg_left_front_upper", render._pose_for(assets, "animation.corrupted_silverfish.walk", 0.2))
        self.assertIn("mandible_left", render._pose_for(assets, "animation.corrupted_silverfish.attack", 0.225))
        # The committed walk cycle crosses its exact neutral pose at 0.2 s;
        # still verify the requested animated outputs are mutually distinct.
        self.assertEqual(len({_png_digest(image) for image in (idle, walk, attack)}), 3)
        self.assertNotEqual(_png_digest(rest), _png_digest(idle))

    def test_missing_and_malformed_assets_have_actionable_errors(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            with self.assertRaisesRegex(render.RenderFailure, "geometry asset is missing"):
                render.load_assets(root)
            geometry = root / render.GEOMETRY_RELATIVE
            geometry.parent.mkdir(parents=True)
            geometry.write_text("not json", encoding="utf-8")
            with self.assertRaisesRegex(render.RenderFailure, "geometry asset.*valid JSON"):
                render.load_assets(root)

    def test_concept_copy_uses_one_of_the_permitted_resamplers(self):
        self.assertIn(render.CONCEPT_RESAMPLING, {Image.Resampling.NEAREST, Image.Resampling.BICUBIC})


if __name__ == "__main__":
    unittest.main()
