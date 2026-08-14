"""Black-box contract tests for the independent v3 artifact validator."""

from __future__ import annotations

import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
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
            self.assertEqual([manifest], list(manifest.parent.iterdir()))

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
