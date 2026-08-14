"""Black-box contract tests for the independent v3 artifact validator."""

from __future__ import annotations

import json
import base64
import hashlib
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import time
import unittest
from unittest import mock

from PIL import Image

from tools.corrupted_silverfish_v3 import validate


ROOT = Path(__file__).resolve().parents[3]
VALIDATOR = ROOT / "tools" / "corrupted_silverfish_v3" / "validate.py"
RELATIVE_PATHS = (
    Path("Modelle/Editierbar/Corrupted Silverfish v3.bbmodel"),
    Path("Modelle/Exports/corrupted_silverfish_v3/geo/corrupted_silverfish.geo.json"),
    Path("Modelle/Exports/corrupted_silverfish_v3/textures/entity/corrupted_silverfish.png"),
    Path("Modelle/Exports/corrupted_silverfish_v3/textures/entity/corrupted_silverfish_glowmask.png"),
    Path("Modelle/Exports/corrupted_silverfish_v3/animations/corrupted_silverfish.animation.json"),
)
LOCK_NAME = ".candidate-sha256.lock"


class ValidatorContract(unittest.TestCase):
    def copy_candidate(self, directory: str) -> Path:
        root = Path(directory)
        for relative in RELATIVE_PATHS:
            target = root / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(ROOT / relative, target)
        return root

    def run_validator(self, root: Path, *extra: str) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            [sys.executable, str(VALIDATOR), "--root", str(root), *extra],
            cwd=ROOT,
            text=True,
            capture_output=True,
            check=False,
        )

    def assert_bbmodel_mutation_fails(self, mutator, expected: str) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = self.copy_candidate(directory)
            manifest = root / validate.MANIFEST_RELATIVE
            manifest.parent.mkdir(parents=True)
            manifest.write_bytes(b"sentinel manifest\n")
            path = root / RELATIVE_PATHS[0]
            document = json.loads(path.read_text(encoding="utf-8"))
            mutator(document)
            path.write_text(json.dumps(document), encoding="utf-8")
            result = self.run_validator(root)
            self.assertEqual(1, result.returncode, result.stderr)
            self.assertEqual("", result.stdout)
            self.assertIn("ASSET_CHECK_FAILED:", result.stderr)
            self.assertIn(expected, result.stderr)
            self.assertEqual(b"sentinel manifest\n", manifest.read_bytes())

    def assert_json_mutation_fails(self, relative: Path, mutator, expected: str) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = self.copy_candidate(directory)
            manifest = root / validate.MANIFEST_RELATIVE
            manifest.parent.mkdir(parents=True)
            manifest.write_bytes(b"sentinel manifest\n")
            path = root / relative
            document = json.loads(path.read_text(encoding="utf-8"))
            mutator(document)
            path.write_text(json.dumps(document), encoding="utf-8")
            result = self.run_validator(root)
            self.assertEqual(1, result.returncode, result.stderr)
            self.assertEqual("", result.stdout)
            self.assertIn(expected, result.stderr)
            self.assertEqual(b"sentinel manifest\n", manifest.read_bytes())

    @staticmethod
    def update_embedded_main(root: Path) -> None:
        model_path = root / RELATIVE_PATHS[0]
        model = json.loads(model_path.read_text(encoding="utf-8"))
        encoded = base64.b64encode((root / RELATIVE_PATHS[2]).read_bytes()).decode("ascii")
        model["textures"][0]["source"] = "data:image/png;base64," + encoded
        model_path.write_text(json.dumps(model), encoding="utf-8")

    @staticmethod
    def geometry_rectangles(root: Path):
        geometry = json.loads((root / RELATIVE_PATHS[1]).read_text(encoding="utf-8"))
        for bone in geometry["minecraft:geometry"][0]["bones"]:
            for cube in bone.get("cubes", []):
                for face in cube["uv"].values():
                    x, y = face["uv"]
                    width, height = face["uv_size"]
                    yield cube["name"], range(x, x + width), range(y, y + height)

    @staticmethod
    def outliner_element_slots(items):
        slots = []
        for index, item in enumerate(items):
            if isinstance(item, str):
                slots.append((items, index))
            else:
                slots.extend(ValidatorContract.outliner_element_slots(item["children"]))
        return slots

    def test_valid_candidate_passes_and_writes_exact_manifest(self):
        with tempfile.TemporaryDirectory() as directory:
            root = self.copy_candidate(directory)
            result = self.run_validator(root)
            self.assertEqual(0, result.returncode, result.stderr)
            self.assertRegex(result.stdout, r"^ASSET_CHECK=PASS;TARGET=V3;BONES=32;CUBES=112;ANIMATIONS=5;TEXTURE=256x256;GLOW_PIXELS=\d+\n$")
            self.assertEqual("", result.stderr)
            manifest = root / "Modelle/Exports/corrupted_silverfish_v3/review/candidate-sha256.json"
            payload = json.loads(manifest.read_text(encoding="utf-8"))
            self.assertEqual([path.as_posix() for path in RELATIVE_PATHS], list(payload))
            self.assertTrue(all(len(value) == 64 and value == value.upper() for value in payload.values()))
            first = manifest.read_bytes()
            self.assertEqual(0, self.run_validator(root).returncode)
            self.assertEqual(first, manifest.read_bytes())

    def test_death_timeline_cannot_outlive_vanilla_removal_window(self):
        with tempfile.TemporaryDirectory() as directory:
            root = self.copy_candidate(directory)
            path = root / RELATIVE_PATHS[4]
            document = json.loads(path.read_text(encoding="utf-8"))
            death = document["animations"]["animation.corrupted_silverfish.death"]
            death["animation_length"] = 1.1
            for channels in death["bones"].values():
                for keyframes in channels.values():
                    final = max(keyframes, key=float)
                    keyframes["1.1"] = keyframes.pop(final)
            path.write_text(json.dumps(document), encoding="utf-8")
            result = self.run_validator(root)
            self.assertEqual(1, result.returncode)
            self.assertIn("death", result.stderr)

    def test_zero_cube_dimension_fails_without_touching_manifest(self):
        with tempfile.TemporaryDirectory() as directory:
            root = self.copy_candidate(directory)
            manifest = root / "Modelle/Exports/corrupted_silverfish_v3/review/candidate-sha256.json"
            manifest.parent.mkdir(parents=True)
            manifest.write_bytes(b"sentinel\n")
            geometry_path = root / RELATIVE_PATHS[1]
            geometry = json.loads(geometry_path.read_text(encoding="utf-8"))
            cube = next(bone["cubes"][0] for bone in geometry["minecraft:geometry"][0]["bones"] if bone.get("cubes"))
            cube["size"][1] = 0
            geometry_path.write_text(json.dumps(geometry), encoding="utf-8")
            result = self.run_validator(root)
            self.assertEqual(1, result.returncode)
            self.assertEqual("", result.stdout)
            self.assertRegex(result.stderr, rf"^ASSET_CHECK_FAILED: .*{cube['name']}.*nonpositive dimension.*\n$")
            self.assertEqual(b"sentinel\n", manifest.read_bytes())

    def test_malformed_json_and_unknown_animation_bone_have_context(self):
        with tempfile.TemporaryDirectory() as directory:
            root = self.copy_candidate(directory)
            (root / RELATIVE_PATHS[1]).write_text("{", encoding="utf-8")
            result = self.run_validator(root)
            self.assertEqual(1, result.returncode)
            self.assertIn("ASSET_CHECK_FAILED: geometry JSON", result.stderr)
        with tempfile.TemporaryDirectory() as directory:
            root = self.copy_candidate(directory)
            path = root / RELATIVE_PATHS[4]
            data = json.loads(path.read_text(encoding="utf-8"))
            data["animations"]["animation.corrupted_silverfish.idle"]["bones"]["not_a_bone"] = {}
            path.write_text(json.dumps(data), encoding="utf-8")
            result = self.run_validator(root)
            self.assertEqual(1, result.returncode)
            self.assertIn("unknown bone not_a_bone", result.stderr)

    def test_uv_out_of_bounds_and_bbmodel_mismatch_fail(self):
        with tempfile.TemporaryDirectory() as directory:
            root = self.copy_candidate(directory)
            path = root / RELATIVE_PATHS[1]
            data = json.loads(path.read_text(encoding="utf-8"))
            cube = next(bone["cubes"][0] for bone in data["minecraft:geometry"][0]["bones"] if bone.get("cubes"))
            cube["uv"]["north"]["uv"][0] = 255
            path.write_text(json.dumps(data), encoding="utf-8")
            result = self.run_validator(root)
            self.assertEqual(1, result.returncode)
            self.assertIn("UV out of bounds", result.stderr)
        with tempfile.TemporaryDirectory() as directory:
            root = self.copy_candidate(directory)
            path = root / RELATIVE_PATHS[0]
            data = json.loads(path.read_text(encoding="utf-8"))
            data["elements"][0]["to"][0] += 1
            path.write_text(json.dumps(data), encoding="utf-8")
            result = self.run_validator(root)
            self.assertEqual(1, result.returncode)
            self.assertIn("bbmodel cube head_core", result.stderr)

    def test_uv_overlap_and_malformed_png_fail(self):
        with tempfile.TemporaryDirectory() as directory:
            root = self.copy_candidate(directory)
            path = root / RELATIVE_PATHS[1]
            data = json.loads(path.read_text(encoding="utf-8"))
            cube = next(bone["cubes"][0] for bone in data["minecraft:geometry"][0]["bones"] if bone.get("cubes"))
            cube["uv"]["east"] = dict(cube["uv"]["north"])
            path.write_text(json.dumps(data), encoding="utf-8")
            result = self.run_validator(root)
            self.assertEqual(1, result.returncode)
            self.assertIn("UV overlap", result.stderr)
        with tempfile.TemporaryDirectory() as directory:
            root = self.copy_candidate(directory)
            (root / RELATIVE_PATHS[2]).write_bytes(b"not a png")
            result = self.run_validator(root)
            self.assertEqual(1, result.returncode)
            self.assertIn("main texture PNG is malformed", result.stderr)

    def test_glow_leak_fails(self):
        with tempfile.TemporaryDirectory() as directory:
            root = self.copy_candidate(directory)
            glow_path = root / RELATIVE_PATHS[3]
            with Image.open(glow_path) as opened:
                glow = opened.copy()
            with Image.open(root / RELATIVE_PATHS[2]) as opened:
                main = opened.copy()
            transparent_index = next(index for index, pixel in enumerate(main.getdata()) if pixel[3] == 0)
            glow.putpixel((transparent_index % 256, transparent_index // 256), (86, 190, 255, 255))
            glow.save(glow_path)
            result = self.run_validator(root)
            self.assertEqual(1, result.returncode)
            self.assertIn("glow leak", result.stderr)

    def test_manifest_publish_failure_preserves_target_and_cleans_temp(self):
        with tempfile.TemporaryDirectory() as directory:
            root = self.copy_candidate(directory)
            paths = tuple(root / relative for relative in RELATIVE_PATHS)
            manifest = root / validate.MANIFEST_RELATIVE
            manifest.parent.mkdir(parents=True)
            manifest.write_bytes(b"old manifest\n")
            with mock.patch("os.replace", side_effect=OSError("simulated publish failure")):
                with self.assertRaisesRegex(validate.ValidationFailure, "manifest write failed"):
                    validate.validate(paths, manifest)
            self.assertEqual(b"old manifest\n", manifest.read_bytes())
            self.assertEqual({manifest, manifest.parent / LOCK_NAME}, set(manifest.parent.iterdir()))

    def test_bbmodel_group_rotation_mismatch_fails_without_manifest_write(self):
        def mutate(document):
            document["groups"][0]["rotation"][1] = 7

        self.assert_bbmodel_mutation_fails(mutate, "group root rotation mismatches geometry")

    def test_swapped_outliner_element_owners_fail_without_manifest_write(self):
        def mutate(document):
            bone_by_uuid = {element["uuid"]: element["bone"] for element in document["elements"]}
            slots = self.outliner_element_slots(document["outliner"])
            first = slots[0]
            second = next(slot for slot in slots[1:] if bone_by_uuid[slot[0][slot[1]]] != bone_by_uuid[first[0][first[1]]])
            first[0][first[1]], second[0][second[1]] = second[0][second[1]], first[0][first[1]]

        self.assert_bbmodel_mutation_fails(mutate, "outliner element")

    def test_bbmodel_mesh_element_fails_without_manifest_write(self):
        def mutate(document):
            document["elements"][0]["type"] = "mesh"

        self.assert_bbmodel_mutation_fails(mutate, "element head_core type must be cube")

    def test_bbmodel_face_texture_index_fails_without_manifest_write(self):
        def mutate(document):
            document["elements"][0]["faces"]["north"]["texture"] = 7

        self.assert_bbmodel_mutation_fails(mutate, "cube head_core face north texture must be 0")

    def test_duplicate_and_orphan_outliner_elements_fail_without_manifest_write(self):
        def duplicate(document):
            slots = self.outliner_element_slots(document["outliner"])
            slots[1][0][slots[1][1]] = slots[0][0][slots[0][1]]

        self.assert_bbmodel_mutation_fails(duplicate, "duplicate element UUID")

        def orphan(document):
            slots = self.outliner_element_slots(document["outliner"])
            del slots[0][0][slots[0][1]]

        self.assert_bbmodel_mutation_fails(orphan, "outliner hierarchy mismatches geometry")

    def test_geometry_normalizes_default_and_explicit_bone_rotations(self):
        path = ROOT / RELATIVE_PATHS[1]
        document = json.loads(path.read_text(encoding="utf-8"))
        bones = document["minecraft:geometry"][0]["bones"]
        bones[0].pop("rotation", None)
        bones[1]["rotation"] = [1, 2.5, -3]
        geometry = validate.validate_geometry(document)
        self.assertEqual([0.0, 0.0, 0.0], geometry["bone_rotations"][bones[0]["name"]])
        self.assertEqual([1.0, 2.5, -3.0], geometry["bone_rotations"][bones[1]["name"]])

    def test_manifest_cannot_alias_an_input(self):
        with tempfile.TemporaryDirectory() as directory:
            root = self.copy_candidate(directory)
            main = root / RELATIVE_PATHS[2]
            before = main.read_bytes()
            result = self.run_validator(root, "--manifest", str(main))
            self.assertEqual(1, result.returncode)
            self.assertEqual("", result.stdout)
            self.assertIn("ASSET_CHECK_FAILED:", result.stderr)
            self.assertIn("manifest", result.stderr.lower())
            self.assertEqual(before, main.read_bytes())

    def test_input_override_cannot_escape_root(self):
        with tempfile.TemporaryDirectory() as directory, tempfile.TemporaryDirectory() as outside:
            root = self.copy_candidate(directory)
            external = Path(outside) / "geometry.json"
            shutil.copy2(root / RELATIVE_PATHS[1], external)
            result = self.run_validator(root, "--geometry", str(external))
            self.assertEqual(1, result.returncode)
            self.assertEqual("", result.stdout)
            self.assertIn("outside root", result.stderr)

    def test_symlink_input_is_rejected_when_supported(self):
        with tempfile.TemporaryDirectory() as directory, tempfile.TemporaryDirectory() as outside:
            root = self.copy_candidate(directory)
            geometry = root / RELATIVE_PATHS[1]
            external = Path(outside) / "geometry.json"
            shutil.copy2(geometry, external)
            geometry.unlink()
            try:
                geometry.symlink_to(external)
            except OSError as exc:
                self.skipTest(f"symlink creation unavailable: {exc}")
            result = self.run_validator(root)
            self.assertEqual(1, result.returncode)
            self.assertIn("reparse", result.stderr.lower())

    def test_coherent_huge_cube_shift_is_rejected_by_fixed_contract(self):
        with tempfile.TemporaryDirectory() as directory:
            root = self.copy_candidate(directory)
            geo_path = root / RELATIVE_PATHS[1]
            geo = json.loads(geo_path.read_text(encoding="utf-8"))
            cube = next(b["cubes"][0] for b in geo["minecraft:geometry"][0]["bones"] if b.get("cubes"))
            cube["origin"][0] += 10000
            if "pivot" in cube:
                cube["pivot"][0] += 10000
            geo_path.write_text(json.dumps(geo), encoding="utf-8")
            bb_path = root / RELATIVE_PATHS[0]
            bb = json.loads(bb_path.read_text(encoding="utf-8"))
            element = next(item for item in bb["elements"] if item["name"] == cube["name"])
            for key in ("from", "to", "origin"):
                element[key][0] += 10000
            bb_path.write_text(json.dumps(bb), encoding="utf-8")
            result = self.run_validator(root)
            self.assertEqual(1, result.returncode)
            self.assertIn("bounds", result.stderr.lower())

    def test_fractional_coherent_uv_extent_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = self.copy_candidate(directory)
            geo_path = root / RELATIVE_PATHS[1]
            geo = json.loads(geo_path.read_text(encoding="utf-8"))
            cube = next(b["cubes"][0] for b in geo["minecraft:geometry"][0]["bones"] if b.get("cubes"))
            face = cube["uv"]["north"]
            face["uv_size"][0] -= 0.5
            geo_path.write_text(json.dumps(geo), encoding="utf-8")
            bb_path = root / RELATIVE_PATHS[0]
            bb = json.loads(bb_path.read_text(encoding="utf-8"))
            element = next(item for item in bb["elements"] if item["name"] == cube["name"])
            element["faces"]["north"]["uv"][2] -= 0.5
            bb_path.write_text(json.dumps(bb), encoding="utf-8")
            result = self.run_validator(root)
            self.assertEqual(1, result.returncode)
            self.assertIn("UV dimensions", result.stderr)

    def test_empty_attack_is_rejected_even_when_bbmodel_matches(self):
        with tempfile.TemporaryDirectory() as directory:
            root = self.copy_candidate(directory)
            animation_path = root / RELATIVE_PATHS[4]
            animations = json.loads(animation_path.read_text(encoding="utf-8"))
            animations["animations"]["animation.corrupted_silverfish.attack"]["bones"] = {}
            animation_path.write_text(json.dumps(animations), encoding="utf-8")
            bb_path = root / RELATIVE_PATHS[0]
            bb = json.loads(bb_path.read_text(encoding="utf-8"))
            next(a for a in bb["animations"] if a["name"].endswith(".attack"))["animators"] = {}
            bb_path.write_text(json.dumps(bb), encoding="utf-8")
            result = self.run_validator(root)
            self.assertEqual(1, result.returncode)
            self.assertIn("attack", result.stderr)
            self.assertIn("required", result.stderr)

    def test_foreign_main_palette_color_is_rejected_with_matching_embed(self):
        with tempfile.TemporaryDirectory() as directory:
            root = self.copy_candidate(directory)
            path = root / RELATIVE_PATHS[2]
            with Image.open(path) as opened:
                image = opened.copy()
            point = next((index % 256, index // 256) for index, pixel in enumerate(image.getdata()) if pixel[3] == 255)
            image.putpixel(point, (1, 2, 3, 255))
            image.save(path)
            self.update_embedded_main(root)
            result = self.run_validator(root)
            self.assertEqual(1, result.returncode)
            self.assertIn("palette", result.stderr.lower())

    def test_moved_glow_and_painted_gutter_are_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = self.copy_candidate(directory)
            main = Image.open(root / RELATIVE_PATHS[2]).copy()
            glow_path = root / RELATIVE_PATHS[3]
            glow = Image.open(glow_path).copy()
            allowed = set()
            all_islands = set()
            for name, xs, ys in self.geometry_rectangles(root):
                points = {(x, y) for x in xs for y in ys}
                all_islands.update(points)
                if name.startswith("eye_") or name.startswith("crystal_"):
                    allowed.update(points)
            source = next((index % 256, index // 256) for index, pixel in enumerate(glow.getdata()) if pixel[3] == 255)
            target = next(point for point in all_islands - allowed if main.getpixel(point)[3] == 255 and glow.getpixel(point)[3] == 0)
            color = glow.getpixel(source)
            glow.putpixel(source, (0, 0, 0, 0))
            glow.putpixel(target, color)
            glow.save(glow_path)
            result = self.run_validator(root)
            self.assertEqual(1, result.returncode)
            self.assertIn("allowed", result.stderr.lower())
        with tempfile.TemporaryDirectory() as directory:
            root = self.copy_candidate(directory)
            main_path = root / RELATIVE_PATHS[2]
            main = Image.open(main_path).copy()
            all_islands = set()
            for _name, xs, ys in self.geometry_rectangles(root):
                all_islands.update((x, y) for x in xs for y in ys)
            gutter = next((x, y) for y in range(256) for x in range(256) if (x, y) not in all_islands)
            main.putpixel(gutter, (22, 18, 28, 255))
            main.save(main_path)
            self.update_embedded_main(root)
            result = self.run_validator(root)
            self.assertEqual(1, result.returncode)
            self.assertIn("outside UV islands", result.stderr)

    def test_snapshot_bytes_are_hashed_without_rereading_changed_disk(self):
        with tempfile.TemporaryDirectory() as directory:
            root = self.copy_candidate(directory)
            paths = tuple(root / relative for relative in RELATIVE_PATHS)
            manifest = root / validate.MANIFEST_RELATIVE
            geometry_path = paths[1]
            original = geometry_path.read_bytes()
            real_read = Path.read_bytes
            counts = {path: 0 for path in paths}

            def mutate_after_snapshot(path):
                if path in counts:
                    counts[path] += 1
                    data = real_read(path)
                    if path == geometry_path and counts[path] == 1:
                        path.write_bytes(b"corrupt after snapshot")
                    return data
                return real_read(path)

            with mock.patch.object(Path, "read_bytes", autospec=True, side_effect=mutate_after_snapshot):
                validate.validate(paths, manifest, root=root)
            payload = json.loads(manifest.read_text(encoding="utf-8"))
            self.assertEqual(hashlib.sha256(original).hexdigest().upper(), payload[RELATIVE_PATHS[1].as_posix()])
            self.assertEqual({path: 1 for path in paths}, counts)

    def test_duplicate_keys_nonfinite_and_oversized_inputs_are_rejected(self):
        mutations = (
            (RELATIVE_PATHS[1], b'{"format_version":"1.12.0","format_version":"1.12.0","minecraft:geometry":[]} ', "duplicate key"),
            (RELATIVE_PATHS[1], b'{"format_version":"1.12.0","unused":NaN,"minecraft:geometry":[]} ', "non-finite"),
            (RELATIVE_PATHS[1], b'{"format_version":"1.12.0","unused":Infinity,"minecraft:geometry":[]} ', "non-finite"),
            (RELATIVE_PATHS[1], b'{}' + b' ' * (3 * 1024 * 1024), "size limit"),
            (RELATIVE_PATHS[2], (ROOT / RELATIVE_PATHS[2]).read_bytes() + b'x' * (2 * 1024 * 1024), "size limit"),
        )
        for relative, contents, expected in mutations:
            with self.subTest(relative=relative, expected=expected), tempfile.TemporaryDirectory() as directory:
                root = self.copy_candidate(directory)
                (root / relative).write_bytes(contents)
                result = self.run_validator(root)
                self.assertEqual(1, result.returncode)
                self.assertEqual("", result.stdout)
                self.assertIn(expected, result.stderr.lower())

    def test_manifest_cleanup_failure_reports_temp_and_preserves_target(self):
        with tempfile.TemporaryDirectory() as directory:
            target = Path(directory) / "manifest.json"
            target.write_bytes(b"old\n")
            real_unlink = Path.unlink

            def locked_temp(path, *args, **kwargs):
                if path != target:
                    raise OSError("simulated cleanup lock")
                return real_unlink(path, *args, **kwargs)

            with mock.patch("os.replace", side_effect=OSError("publish failed")), mock.patch.object(Path, "unlink", autospec=True, side_effect=locked_temp):
                with self.assertRaisesRegex(validate.ValidationFailure, "cleanup.*simulated cleanup lock") as raised:
                    validate._atomic_write(target, b"new\n")
            self.assertIn(str(target.parent), str(raised.exception))
            self.assertEqual(b"old\n", target.read_bytes())

    def test_manifest_publish_is_verified_before_success(self):
        with tempfile.TemporaryDirectory() as directory:
            target = Path(directory) / "manifest.json"
            target.write_bytes(b"old\n")
            with mock.patch("os.replace", return_value=None):
                with self.assertRaisesRegex(validate.ValidationFailure, "verification"):
                    validate._atomic_write(target, b"new\n")
            self.assertEqual(b"old\n", target.read_bytes())
            self.assertEqual({target, target.parent / LOCK_NAME}, set(target.parent.iterdir()))

    def test_strict_runtime_schemas_reject_unknown_or_wrong_fields(self):
        def bad_identifier(document):
            document["minecraft:geometry"][0]["description"]["identifier"] = "geometry.other"

        self.assert_json_mutation_fails(RELATIVE_PATHS[1], bad_identifier, "identifier")

        def geo_inflate(document):
            cube = next(b["cubes"][0] for b in document["minecraft:geometry"][0]["bones"] if b.get("cubes"))
            cube["inflate"] = 1

        self.assert_json_mutation_fails(RELATIVE_PATHS[1], geo_inflate, "unexpected keys")

        def element_visibility(document):
            document["elements"][0]["visibility"] = False

        self.assert_bbmodel_mutation_fails(element_visibility, "unexpected keys")

        def keyframe_pre(document):
            animation = document["animations"]["animation.corrupted_silverfish.idle"]
            keyframe = animation["bones"]["body"]["position"]["0"]
            keyframe["pre"] = [0, 0, 0]

        self.assert_json_mutation_fails(RELATIVE_PATHS[4], keyframe_pre, "unexpected keys")

    def test_exact_rgba_contract_rejects_uniform_seam_and_allowed_glow_move(self):
        with tempfile.TemporaryDirectory() as directory:
            root = self.copy_candidate(directory)
            main_path = root / RELATIVE_PATHS[2]
            main = Image.open(main_path).copy()
            point = next((i % 256, i // 256) for i, pixel in enumerate(main.getdata()) if pixel == (76, 82, 92, 255))
            main.putpixel(point, (126, 136, 147, 255))
            main.save(main_path)
            self.update_embedded_main(root)
            result = self.run_validator(root)
            self.assertEqual(1, result.returncode)
            self.assertIn("RGBA pixel hash", result.stderr)
        with tempfile.TemporaryDirectory() as directory:
            root = self.copy_candidate(directory)
            main = Image.open(root / RELATIVE_PATHS[2]).copy()
            glow_path = root / RELATIVE_PATHS[3]
            glow = Image.open(glow_path).copy()
            allowed = set()
            for name, xs, ys in self.geometry_rectangles(root):
                if name.startswith("eye_") or name.startswith("crystal_") or name == "mouth_sensor_cube":
                    allowed.update((x, y) for x in xs for y in ys)
            source = next((i % 256, i // 256) for i, pixel in enumerate(glow.getdata()) if pixel[3] == 255)
            target = next(point for point in allowed if main.getpixel(point)[3] == 255 and glow.getpixel(point)[3] == 0)
            glow.putpixel(target, glow.getpixel(source))
            glow.putpixel(source, (0, 0, 0, 0))
            glow.save(glow_path)
            result = self.run_validator(root)
            self.assertEqual(1, result.returncode)
            self.assertIn("RGBA pixel hash", result.stderr)

    def test_rgba_contract_allows_lossless_png_recompression(self):
        with tempfile.TemporaryDirectory() as directory:
            root = self.copy_candidate(directory)
            main_path = root / RELATIVE_PATHS[2]
            original_pixels = Image.open(main_path).convert("RGBA").tobytes()
            image = Image.open(main_path).copy()
            image.save(main_path, optimize=True, compress_level=9)
            self.assertEqual(original_pixels, Image.open(main_path).convert("RGBA").tobytes())
            self.update_embedded_main(root)
            result = self.run_validator(root)
            self.assertEqual(0, result.returncode, result.stderr)

    def test_manifest_verification_failure_restores_existing_or_removes_new_target(self):
        for initially_present in (True, False):
            with self.subTest(initially_present=initially_present), tempfile.TemporaryDirectory() as directory:
                target = Path(directory) / "manifest.json"
                if initially_present:
                    target.write_bytes(b"old manifest\n")
                real_read = Path.read_bytes
                reads = 0

                def fail_first_target_read(path):
                    nonlocal reads
                    if path == target:
                        reads += 1
                        verification_read = 2 if initially_present else 1
                        if reads == verification_read:
                            raise OSError("simulated post-publish verification failure")
                    return real_read(path)

                with mock.patch.object(Path, "read_bytes", autospec=True, side_effect=fail_first_target_read):
                    with self.assertRaisesRegex(validate.ValidationFailure, "verification.*rollback"):
                        validate._atomic_write(target, b"new manifest\n")
                if initially_present:
                    self.assertEqual(b"old manifest\n", target.read_bytes())
                else:
                    self.assertFalse(target.exists())
                expected = {target.parent / LOCK_NAME}
                if initially_present:
                    expected.add(target)
                self.assertEqual(expected, set(target.parent.iterdir()))

    def test_manifest_rollback_failure_retains_backup_with_chain(self):
        with tempfile.TemporaryDirectory() as directory:
            target = Path(directory) / "manifest.json"
            target.write_bytes(b"old manifest\n")
            real_replace = os.replace
            replace_calls = 0

            def fail_rollback(source, destination):
                nonlocal replace_calls
                replace_calls += 1
                if replace_calls == 2:
                    raise OSError("simulated rollback failure")
                return real_replace(source, destination)

            real_read = Path.read_bytes
            target_reads = 0

            def fail_target_verification(path):
                nonlocal target_reads
                if path == target:
                    target_reads += 1
                    if target_reads == 2:
                        raise OSError("verification read failure")
                return real_read(path)

            with mock.patch("os.replace", side_effect=fail_rollback), mock.patch.object(Path, "read_bytes", autospec=True, side_effect=fail_target_verification):
                with self.assertRaisesRegex(validate.ValidationFailure, "verification.*rollback failed") as raised:
                    validate._atomic_write(target, b"new manifest\n")
            backups = [path for path in target.parent.iterdir() if ".backup." in path.name]
            self.assertEqual(1, len(backups))
            self.assertEqual(b"old manifest\n", backups[0].read_bytes())
            self.assertIn(str(backups[0]), str(raised.exception))

    def test_crash_after_backup_or_immediately_before_publish_keeps_canonical_target(self):
        script = r'''
import os
from pathlib import Path
import sys
from tools.corrupted_silverfish_v3 import validate
target = Path(sys.argv[1])
mode = sys.argv[2]
if mode == "after_backup":
    real_read = Path.read_bytes
    def crash_on_backup_read(path):
        data = real_read(path)
        if ".backup." in path.name:
            os._exit(71)
        return data
    Path.read_bytes = crash_on_backup_read
else:
    os.replace = lambda _source, _destination: os._exit(72)
validate._atomic_write(target, b"new manifest\n")
'''
        for mode, expected_exit in (("after_backup", 71), ("before_publish", 72)):
            with self.subTest(mode=mode), tempfile.TemporaryDirectory() as directory:
                target = Path(directory) / "manifest.json"
                target.write_bytes(b"old manifest\n")
                result = subprocess.run([sys.executable, "-c", script, str(target), mode], cwd=ROOT, check=False)
                self.assertEqual(expected_exit, result.returncode)
                self.assertEqual(b"old manifest\n", target.read_bytes())
                backups = [path for path in target.parent.iterdir() if ".backup." in path.name]
                self.assertEqual(1, len(backups))
                self.assertEqual(b"old manifest\n", backups[0].read_bytes())

    def test_concurrent_target_recreation_is_not_overwritten_or_misreported_as_rollback(self):
        for initially_present in (True, False):
            with self.subTest(initially_present=initially_present), tempfile.TemporaryDirectory() as directory:
                target = Path(directory) / "manifest.json"
                if initially_present:
                    target.write_bytes(b"old manifest\n")
                real_read = Path.read_bytes
                target_reads = 0

                def recreate_during_verification(path):
                    nonlocal target_reads
                    if path == target:
                        target_reads += 1
                        verification_read = target_reads == (2 if initially_present else 1)
                        if verification_read:
                            target.write_bytes(b"concurrent manifest\n")
                            raise OSError("simulated verification race")
                    return real_read(path)

                with mock.patch.object(Path, "read_bytes", autospec=True, side_effect=recreate_during_verification):
                    with self.assertRaisesRegex(validate.ValidationFailure, "concurrent target") as raised:
                        validate._atomic_write(target, b"new manifest\n")
                self.assertEqual(b"concurrent manifest\n", target.read_bytes())
                self.assertNotIn("rollback completed", str(raised.exception))
                backups = [path for path in target.parent.iterdir() if ".backup." in path.name]
                if initially_present:
                    self.assertEqual(1, len(backups))
                    self.assertEqual(b"old manifest\n", backups[0].read_bytes())
                else:
                    self.assertEqual([], backups)

    def test_backup_verification_read_lock_keeps_target_and_has_no_false_rollback(self):
        with tempfile.TemporaryDirectory() as directory:
            target = Path(directory) / "manifest.json"
            target.write_bytes(b"old manifest\n")
            real_read = Path.read_bytes

            def lock_backup(path):
                if ".backup." in path.name:
                    raise OSError("simulated backup read lock")
                return real_read(path)

            with mock.patch.object(Path, "read_bytes", autospec=True, side_effect=lock_backup), mock.patch("os.replace", wraps=os.replace) as replace:
                with self.assertRaisesRegex(validate.ValidationFailure, "backup verification.*read lock") as raised:
                    validate._atomic_write(target, b"new manifest\n")
            replace.assert_not_called()
            self.assertEqual(b"old manifest\n", target.read_bytes())
            self.assertNotIn("rollback", str(raised.exception))

    def test_manifest_success_removes_closed_backup_and_temp(self):
        with tempfile.TemporaryDirectory() as directory:
            target = Path(directory) / "manifest.json"
            target.write_bytes(b"old manifest\n")
            validate._atomic_write(target, b"new manifest\n")
            self.assertEqual(b"new manifest\n", target.read_bytes())
            self.assertEqual({target, target.parent / LOCK_NAME}, set(target.parent.iterdir()))

    def test_manifest_writer_lock_serializes_existing_and_missing_targets(self):
        worker = r'''
import os
from pathlib import Path
import sys
import time
from tools.corrupted_silverfish_v3 import validate
target, ready, release = map(Path, sys.argv[1:4])
real_replace = os.replace
def pause_before_publish(source, destination):
    source = Path(source)
    if Path(destination) == target and ".backup." not in source.name:
        ready.write_text("ready", encoding="ascii")
        deadline = time.monotonic() + 15
        while not release.exists():
            if time.monotonic() >= deadline:
                os._exit(74)
            time.sleep(0.01)
    return real_replace(source, destination)
os.replace = pause_before_publish
validate._atomic_write(target, b"first writer\n")
'''
        for initially_present in (True, False):
            with self.subTest(initially_present=initially_present), tempfile.TemporaryDirectory() as directory:
                parent = Path(directory)
                target = parent / "manifest.json"
                ready = parent / "ready"
                release = parent / "release"
                if initially_present:
                    target.write_bytes(b"old manifest\n")
                process = subprocess.Popen([sys.executable, "-c", worker, str(target), str(ready), str(release)], cwd=ROOT)
                try:
                    deadline = time.monotonic() + 10
                    marker = ""
                    while marker != "ready" and process.poll() is None and time.monotonic() < deadline:
                        if ready.exists():
                            marker = ready.read_text(encoding="ascii")
                        time.sleep(0.01)
                    self.assertEqual("ready", marker, f"writer exited early with {process.poll()}")
                    if initially_present:
                        self.assertEqual(b"old manifest\n", target.read_bytes())
                    else:
                        self.assertFalse(target.exists())
                    before_contention = {
                        path.name: (path.stat().st_size, None if path.name == LOCK_NAME else path.read_bytes())
                        for path in parent.iterdir() if path.is_file()
                    }
                    with self.assertRaisesRegex(validate.ValidationFailure, "writer lock contention"):
                        validate._atomic_write(target, b"second writer\n")
                    after_contention = {
                        path.name: (path.stat().st_size, None if path.name == LOCK_NAME else path.read_bytes())
                        for path in parent.iterdir() if path.is_file()
                    }
                    self.assertEqual(before_contention, after_contention)
                    if initially_present:
                        self.assertEqual(b"old manifest\n", target.read_bytes())
                    else:
                        self.assertFalse(target.exists())
                finally:
                    release.touch()
                    process.wait(timeout=10)
                self.assertEqual(0, process.returncode)
                self.assertEqual(b"first writer\n", target.read_bytes())
                validate._atomic_write(target, b"third writer\n")
                self.assertEqual(b"third writer\n", target.read_bytes())

    def test_writer_crash_releases_os_lock_for_next_writer(self):
        worker = r'''
import os
from pathlib import Path
import sys
from tools.corrupted_silverfish_v3 import validate
target, ready = map(Path, sys.argv[1:3])
with validate._manifest_lock(target):
    ready.write_text("ready", encoding="ascii")
    os._exit(73)
'''
        with tempfile.TemporaryDirectory() as directory:
            parent = Path(directory)
            target = parent / "manifest.json"
            ready = parent / "ready"
            target.write_bytes(b"old manifest\n")
            process = subprocess.run([sys.executable, "-c", worker, str(target), str(ready)], cwd=ROOT, check=False)
            self.assertEqual(73, process.returncode)
            self.assertTrue(ready.exists())
            validate._atomic_write(target, b"after crash\n")
            self.assertEqual(b"after crash\n", target.read_bytes())

    def test_manifest_lockfile_is_exactly_scoped_in_gitignore(self):
        lines = (ROOT / ".gitignore").read_text(encoding="utf-8").splitlines()
        expected = "/Modelle/Exports/corrupted_silverfish_v3/review/.candidate-sha256.lock"
        self.assertIn(expected, lines)

    def test_manifest_lockfile_cannot_hardlink_an_input(self):
        with tempfile.TemporaryDirectory() as directory:
            root = self.copy_candidate(directory)
            main = root / RELATIVE_PATHS[2]
            original = main.read_bytes()
            lock_path = root / validate.MANIFEST_RELATIVE.parent / LOCK_NAME
            lock_path.parent.mkdir(parents=True, exist_ok=True)
            os.link(main, lock_path)
            result = self.run_validator(root)
            self.assertEqual(1, result.returncode)
            self.assertEqual("", result.stdout)
            self.assertIn("ASSET_CHECK_FAILED:", result.stderr)
            self.assertIn("lock path collides with candidate input", result.stderr)
            self.assertEqual(original, main.read_bytes())

    def test_missing_candidate_after_sidecar_reaches_contextual_snapshot_failure(self):
        with tempfile.TemporaryDirectory() as directory:
            root = self.copy_candidate(directory)
            first = self.run_validator(root)
            self.assertEqual(0, first.returncode, first.stderr)
            manifest = root / validate.MANIFEST_RELATIVE
            before = manifest.read_bytes()
            lock_path = manifest.parent / LOCK_NAME
            self.assertTrue(lock_path.exists())
            missing = root / RELATIVE_PATHS[1]
            missing.unlink()

            result = self.run_validator(root)

            self.assertEqual(1, result.returncode)
            self.assertEqual("", result.stdout)
            self.assertIn("ASSET_CHECK_FAILED: geometry missing/not found", result.stderr)
            self.assertIn(str(missing), result.stderr)
            self.assertNotIn("unexpected validator error", result.stderr)
            self.assertNotIn("Traceback", result.stderr)
            self.assertEqual(before, manifest.read_bytes())

    def test_lock_samefile_oserror_is_contextualized(self):
        with tempfile.TemporaryDirectory() as directory:
            root = self.copy_candidate(directory)
            first = self.run_validator(root)
            self.assertEqual(0, first.returncode, first.stderr)
            paths = tuple(root / relative for relative in RELATIVE_PATHS)
            manifest = root / validate.MANIFEST_RELATIVE
            before = manifest.read_bytes()
            with mock.patch("os.path.samefile", side_effect=OSError("injected samefile failure")):
                with self.assertRaisesRegex(validate.ValidationFailure, "lock identity check failed.*injected samefile failure"):
                    validate.validate(paths, manifest, root=root)
            self.assertEqual(before, manifest.read_bytes())

    def test_deeply_nested_json_is_contextualized_without_unexpected_error(self):
        with tempfile.TemporaryDirectory() as directory:
            root = self.copy_candidate(directory)
            nested = b'{"format_version":"1.12.0","x":' + b"[" * 1500 + b"0" + b"]" * 1500 + b',"minecraft:geometry":[]}'
            (root / RELATIVE_PATHS[1]).write_bytes(nested)
            result = self.run_validator(root)
            self.assertEqual(1, result.returncode)
            self.assertEqual("", result.stdout)
            self.assertIn("malformed or too deeply nested JSON", result.stderr)
            self.assertNotIn("unexpected validator error", result.stderr)

    def test_cli_usage_is_exit_two_without_traceback(self):
        result = subprocess.run(
            [sys.executable, str(VALIDATOR), "--not-an-option"],
            cwd=ROOT, text=True, capture_output=True, check=False,
        )
        self.assertEqual(2, result.returncode)
        self.assertNotIn("Traceback", result.stderr)

    def test_validator_never_imports_generators(self):
        source = VALIDATOR.read_text(encoding="utf-8")
        for module in ("spec", "build", "paint"):
            self.assertNotRegex(source, rf"(?:from|import)\s+.*\b{module}\b")


if __name__ == "__main__":
    unittest.main()
