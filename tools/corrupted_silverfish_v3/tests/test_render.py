from __future__ import annotations

import hashlib
import json
import os
from pathlib import Path
import tempfile
import unittest
from contextlib import redirect_stderr
from io import BytesIO
from io import StringIO
from unittest import mock

from PIL import Image, ImageDraw

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
    MARKERS = ((240, 30, 40, 255), (30, 220, 70, 255), (40, 80, 240, 255), (240, 210, 30, 255))

    def test_all_six_faces_keep_non_square_uv_axes_and_marker_orientation(self):
        sizes = {
            "north": (8, 6), "south": (8, 6),
            "east": (4, 6), "west": (4, 6),
            "up": (8, 4), "down": (8, 4),
        }
        cameras = {
            "north": render.camera_for("front"),
            "south": render.camera_for("back"),
            "east": render.camera_for("right"),
            "west": ((-1, 0, 0), (0, 0, -1), (0, 1, 0)),
            "up": render.camera_for("top"),
            "down": ((0, -1, 0), (1, 0, 0), (0, 0, 1)),
        }
        texture = Image.new("RGBA", (48, 8), (0, 0, 0, 0))
        faces = {}
        offset = 0
        for face in render.FACE_NAMES:
            width, height = sizes[face]
            faces[face] = {"uv": [offset, 0], "uv_size": [width, height]}
            draw = ImageDraw.Draw(texture)
            draw.rectangle((offset, 0, offset + width - 1, height - 1), fill=(90, 90, 90, 255))
            draw.rectangle((offset, 0, offset + width // 2 - 1, height // 2 - 1), fill=self.MARKERS[0])
            draw.rectangle((offset + width // 2, 0, offset + width - 1, height // 2 - 1), fill=self.MARKERS[1])
            draw.rectangle((offset + width // 2, height // 2, offset + width - 1, height - 1), fill=self.MARKERS[2])
            draw.rectangle((offset, height // 2, offset + width // 2 - 1, height - 1), fill=self.MARKERS[3])
            offset += width
        cube = {"name": "marker", "origin": [-2, -1.5, -1], "size": [4, 3, 2], "uv": faces}
        expected_bounds = {
            "north": (80, 60), "south": (80, 60),
            "east": (40, 60), "west": (40, 60),
            "up": (80, 40), "down": (80, 40),
        }
        for face in render.FACE_NAMES:
            with self.subTest(face=face):
                image = render.render_cubes(
                    [(cube, render.identity_matrix())], texture, cameras[face],
                    canvas_size=(120, 120), pixels_per_unit=20, center_world=(0, 0, 0),
                )
                bounds = image.getchannel("A").getbbox()
                self.assertIsNotNone(bounds)
                width, height = bounds[2] - bounds[0], bounds[3] - bounds[1]
                self.assertEqual((width, height), expected_bounds[face])
                self.assertEqual(sum(1 for alpha in image.getchannel("A").getdata() if alpha), width * height)
                points = (
                    (bounds[0] + width // 4, bounds[1] + height // 4),
                    (bounds[0] + 3 * width // 4, bounds[1] + height // 4),
                    (bounds[0] + 3 * width // 4, bounds[1] + 3 * height // 4),
                    (bounds[0] + width // 4, bounds[1] + 3 * height // 4),
                )
                self.assertEqual(tuple(image.getpixel(point) for point in points), self.MARKERS)

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

    def test_translucent_near_texel_alpha_composites_over_opaque_far_face(self):
        translucent_red, opaque_blue = (255, 0, 0, 128), (0, 0, 255, 255)
        texture = Image.new("RGBA", (2, 1))
        texture.putdata((translucent_red, opaque_blue))
        near_uv = {name: {"uv": [0, 0], "uv_size": [1, 1]} for name in render.FACE_NAMES}
        far_uv = {name: {"uv": [1, 0], "uv_size": [1, 1]} for name in render.FACE_NAMES}
        far = {"name": "far", "origin": [-1, -1, 1], "size": [2, 2, 1], "uv": far_uv}
        near = {"name": "near", "origin": [-1, -1, -1], "size": [2, 2, 1], "uv": near_uv}
        image = render.render_cubes(
            [(near, render.identity_matrix()), (far, render.identity_matrix())],
            texture, render.camera_for("front"), canvas_size=(64, 64),
            pixels_per_unit=20, center_world=(0, 0, 0),
        )
        self.assertEqual(image.getpixel((32, 32)), (128, 0, 127, 255))

    def test_crossing_rotated_cuboids_use_local_per_pixel_occlusion(self):
        red, blue = (220, 30, 40, 255), (35, 75, 225, 255)
        texture = Image.new("RGBA", (2, 1))
        texture.putdata((red, blue))
        red_uv = {name: {"uv": [0, 0], "uv_size": [1, 1]} for name in render.FACE_NAMES}
        blue_uv = {name: {"uv": [1, 0], "uv_size": [1, 1]} for name in render.FACE_NAMES}
        red_cube = {"name": "red", "origin": [-3, -3, -0.5], "size": [6, 6, 1], "pivot": [0, 0, 0], "rotation": [0, 35, 0], "uv": red_uv}
        blue_cube = {"name": "blue", "origin": [-3, -3, -0.5], "size": [6, 6, 1], "pivot": [0, 0, 0], "rotation": [0, -35, 0], "uv": blue_uv}
        image = render.render_cubes(
            [(red_cube, render.identity_matrix()), (blue_cube, render.identity_matrix())],
            texture, render.camera_for("front"), canvas_size=(160, 160),
            pixels_per_unit=18, center_world=(0, 0, 0),
        )
        self.assertEqual({image.getpixel((50, 80)), image.getpixel((110, 80))}, {red, blue})


class ReviewTransactionContract(unittest.TestCase):
    def _fixture(self, directory):
        targets = tuple(Path(directory) / f"candidate-{index}.png" for index in range(8))
        originals = tuple(f"old-{index}".encode() for index in range(8))
        for target, contents in zip(targets, originals):
            target.write_bytes(contents)
        payloads = tuple((target, f"new-{index}".encode()) for index, target in enumerate(targets))
        return targets, originals, payloads

    def _assert_restored_and_clean(self, targets, originals):
        self.assertEqual(tuple(path.read_bytes() for path in targets), originals)
        extras = [path for path in targets[0].parent.iterdir() if path not in targets]
        self.assertEqual(extras, [])

    def test_fourth_and_eighth_stage_failure_preserve_all_outputs_and_cleanup(self):
        for failure_index in (4, 8):
            with self.subTest(failure_index=failure_index), tempfile.TemporaryDirectory() as directory:
                targets, originals, payloads = self._fixture(directory)
                real_stage = render._stage_bytes
                calls = 0

                def fail_stage(path, contents, role):
                    nonlocal calls
                    calls += 1
                    if calls == failure_index:
                        raise OSError(f"stage {failure_index}")
                    return real_stage(path, contents, role)

                with mock.patch.object(render, "_stage_bytes", side_effect=fail_stage), self.assertRaisesRegex(OSError, f"stage {failure_index}"):
                    render._publish_transaction(payloads)
                self._assert_restored_and_clean(targets, originals)

    def test_fourth_and_eighth_publish_failure_restore_all_outputs_and_cleanup(self):
        for failure_index in (4, 8):
            with self.subTest(failure_index=failure_index), tempfile.TemporaryDirectory() as directory:
                targets, originals, payloads = self._fixture(directory)
                real_replace = os.replace
                publish_calls = 0

                def fail_publish(source, target):
                    nonlocal publish_calls
                    if ".candidate." in Path(source).name:
                        publish_calls += 1
                        if publish_calls == failure_index:
                            raise OSError(f"publish {failure_index}")
                    return real_replace(source, target)

                with mock.patch("os.replace", side_effect=fail_publish), self.assertRaisesRegex(OSError, f"publish {failure_index}"):
                    render._publish_transaction(payloads)
                self._assert_restored_and_clean(targets, originals)

    def test_fourth_and_eighth_publish_with_rollback_failure_retain_closed_backup_and_chain(self):
        for failure_index in (4, 8):
            with self.subTest(failure_index=failure_index), tempfile.TemporaryDirectory() as directory:
                targets, originals, payloads = self._fixture(directory)
                real_replace = os.replace
                publish_calls = 0
                rollback_failed = False
                retained_index = failure_index - 2

                def fail_publish_and_rollback(source, target):
                    nonlocal publish_calls, rollback_failed
                    source = Path(source)
                    if ".candidate." in source.name:
                        publish_calls += 1
                        if publish_calls == failure_index:
                            raise OSError(f"publish {failure_index} exploded")
                    if ".backup." in source.name and Path(target) == targets[retained_index] and not rollback_failed:
                        rollback_failed = True
                        raise OSError(f"rollback {failure_index} exploded")
                    return real_replace(source, target)

                with mock.patch("os.replace", side_effect=fail_publish_and_rollback), self.assertRaisesRegex(RuntimeError, "review transaction rollback failed") as raised:
                    render._publish_transaction(payloads)
                self.assertIn(f"publish {failure_index} exploded", str(raised.exception))
                self.assertIn(f"rollback {failure_index} exploded", str(raised.exception))
                backups = [path for path in targets[0].parent.iterdir() if ".backup." in path.name]
                self.assertEqual(len(backups), 1)
                self.assertEqual(backups[0].read_bytes(), originals[retained_index])
                probe = backups[0].with_suffix(".handle-check")
                os.replace(backups[0], probe)
                os.replace(probe, backups[0])

    def test_cleanup_failure_is_reported_without_hiding_stage_failure(self):
        with tempfile.TemporaryDirectory() as directory:
            targets, originals, payloads = self._fixture(directory)
            real_stage = render._stage_bytes
            real_unlink = Path.unlink
            calls = 0

            def fail_stage(path, contents, role):
                nonlocal calls
                calls += 1
                if calls == 4:
                    raise OSError("stage exploded")
                return real_stage(path, contents, role)

            def fail_one_cleanup(path, *args, **kwargs):
                if ".candidate." in path.name and path.exists():
                    raise OSError("cleanup exploded")
                return real_unlink(path, *args, **kwargs)

            with mock.patch.object(render, "_stage_bytes", side_effect=fail_stage), mock.patch.object(Path, "unlink", autospec=True, side_effect=fail_one_cleanup), self.assertRaisesRegex(OSError, "stage exploded.*cleanup incomplete.*cleanup exploded"):
                render._publish_transaction(payloads)
            self.assertEqual(tuple(path.read_bytes() for path in targets), originals)

    def test_fourth_and_eighth_cleanup_failure_after_publish_reports_new_outputs(self):
        for failure_index in (4, 8):
            with self.subTest(failure_index=failure_index), tempfile.TemporaryDirectory() as directory:
                targets, _originals, payloads = self._fixture(directory)
                real_unlink = Path.unlink
                cleanup_calls = 0

                def fail_cleanup(path, *args, **kwargs):
                    nonlocal cleanup_calls
                    cleanup_calls += 1
                    if cleanup_calls == failure_index:
                        raise OSError(f"cleanup {failure_index} exploded")
                    return real_unlink(path, *args, **kwargs)

                with mock.patch.object(Path, "unlink", autospec=True, side_effect=fail_cleanup), self.assertRaisesRegex(RuntimeError, f"published, cleanup incomplete.*cleanup {failure_index} exploded"):
                    render._publish_transaction(payloads)
                self.assertEqual(
                    tuple(path.read_bytes() for path in targets),
                    tuple(contents for _target, contents in payloads),
                )

    def test_publish_verification_failure_rolls_back_all_outputs(self):
        with tempfile.TemporaryDirectory() as directory:
            targets, originals, payloads = self._fixture(directory)
            real_replace = os.replace
            publish_calls = 0

            def corrupt_fourth_publish(source, target):
                nonlocal publish_calls
                result = real_replace(source, target)
                if ".candidate." in Path(source).name:
                    publish_calls += 1
                    if publish_calls == 4:
                        Path(target).write_bytes(b"corrupt")
                return result

            with mock.patch("os.replace", side_effect=corrupt_fourth_publish), self.assertRaisesRegex(OSError, "publish verification failed"):
                render._publish_transaction(payloads)
            self._assert_restored_and_clean(targets, originals)


class CommittedRenderContract(unittest.TestCase):
    ROOT = Path(__file__).resolve().parents[3]

    def test_default_concept_is_a_portable_repo_relative_asset(self):
        self.assertEqual(
            render.DEFAULT_CONCEPT,
            Path("Modelle/Exports/corrupted_silverfish_v2/concept/concept_sheet_raw.png"),
        )
        self.assertFalse(render.DEFAULT_CONCEPT.is_absolute())
        self.assertTrue((self.ROOT / render.DEFAULT_CONCEPT).is_file())

    def test_renderer_reads_disk_assets_and_produces_deterministic_review_set(self):
        with tempfile.TemporaryDirectory() as first, tempfile.TemporaryDirectory() as second:
            first_paths = render.render_review_set(self.ROOT, Path(first))
            second_paths = render.render_review_set(self.ROOT, Path(second))
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

    def test_candidate_layout_crops_alpha_and_uses_one_scale_for_all_views(self):
        images = {}
        for name, size in (("wide", (40, 20)), ("tall", (20, 40)), ("small", (10, 10))):
            image = Image.new("RGBA", (100, 100), (0, 0, 0, 0))
            ImageDraw.Draw(image).rectangle((10, 10, 9 + size[0], 9 + size[1]), fill=(255, 255, 255, 255))
            images[name] = image
        fitted, scale = render._candidate_layout(images, (160, 160))
        self.assertEqual(scale, 4.0)
        self.assertEqual(fitted["wide"].size, (160, 80))
        self.assertEqual(fitted["tall"].size, (80, 160))
        self.assertEqual(fitted["small"].size, (40, 40))
        self.assertTrue(all(image.getchannel("A").getbbox() == (0, 0, *image.size) for image in fitted.values()))

    def test_actual_candidate_layout_is_materially_larger_than_full_canvas_thumbnails(self):
        assets = render.load_assets(self.ROOT)
        candidates = {
            "front": render.render_model(assets, "front"),
            "right": render.render_model(assets, "right"),
            "back": render.render_model(assets, "back"),
            "top": render.render_model(assets, "top"),
            "idle": render.render_model(assets, "three_quarter", "animation.corrupted_silverfish.idle", 0.8),
            "walk": render.render_model(assets, "three_quarter", "animation.corrupted_silverfish.walk", 0.1),
            "attack": render.render_model(assets, "three_quarter", "animation.corrupted_silverfish.attack", 0.225),
        }
        _, scale = render._candidate_layout(candidates, (244, 218))
        self.assertGreater(scale, 0.3)

    def test_missing_and_invalid_concept_fail_instead_of_rendering_placeholder(self):
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "review"
            stderr = StringIO()
            with redirect_stderr(stderr):
                result = render.main([
                    "--root", str(self.ROOT),
                    "--output", str(output),
                    "--concept", "missing-concept.png",
                ])
            self.assertEqual(result, 1)
            self.assertIn("concept image is missing", stderr.getvalue())
            self.assertFalse(output.exists())

            invalid = Path(directory) / "invalid.png"
            invalid.write_bytes(b"not a png")
            stderr = StringIO()
            with redirect_stderr(stderr):
                result = render.main([
                    "--root", str(self.ROOT),
                    "--output", str(output),
                    "--concept", str(invalid),
                ])
            self.assertEqual(result, 1)
            self.assertIn("concept image is not a valid image", stderr.getvalue())
            self.assertFalse(output.exists())

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

    def test_walk_review_uses_a_visible_tripod_pose_instead_of_neutral_crossing(self):
        assets = render.load_assets(self.ROOT)
        rest = render.render_model(assets, "three_quarter")
        walk = render.render_model(assets, "three_quarter", "animation.corrupted_silverfish.walk", 0.1)
        self.assertNotEqual(_png_digest(rest), _png_digest(walk))
        pose = render._pose_for(assets, "animation.corrupted_silverfish.walk", 0.1)
        expected = {
            "leg_left_front_upper": 8.0, "leg_left_front_lower": -5.0,
            "leg_left_mid_upper": -8.0, "leg_left_mid_lower": 5.0,
            "leg_left_rear_upper": 8.0, "leg_left_rear_lower": -5.0,
            "leg_right_front_upper": -8.0, "leg_right_front_lower": 5.0,
            "leg_right_mid_upper": 8.0, "leg_right_mid_lower": -5.0,
            "leg_right_rear_upper": -8.0, "leg_right_rear_lower": 5.0,
        }
        self.assertEqual(
            {bone: channels["rotation"][0] for bone, channels in pose.items() if bone.startswith("leg_")},
            expected,
        )

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
