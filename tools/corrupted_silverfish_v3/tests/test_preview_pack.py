from __future__ import annotations

import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest
from unittest import mock
from contextlib import redirect_stderr, redirect_stdout
from io import StringIO

from PIL import Image

from tools.corrupted_silverfish_v3 import preview_pack
from tools.corrupted_silverfish_v3 import validate


ROOT = Path(__file__).resolve().parents[3]


class PreviewPackContract(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.root = Path(self.temporary.name)
        for relative in validate.RELATIVE_PATHS:
            source = ROOT / relative
            target = self.root / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source, target)
        manifest = self.root / validate.MANIFEST_RELATIVE
        manifest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(ROOT / validate.MANIFEST_RELATIVE, manifest)

    def tearDown(self):
        self.temporary.cleanup()

    def build(self):
        with mock.patch.object(preview_pack, "_run_validator") as run:
            run.return_value = None
            result = preview_pack.build_preview_pack(self.root)
        run.assert_called_once_with(self.root)
        return result

    def test_pack_metadata_assets_hashes_and_no_extras_are_exact(self):
        destination = self.build()
        expected_files = {Path("pack.mcmeta"), *preview_pack.PACK_ASSETS.values()}
        actual_files = {
            path.relative_to(destination)
            for path in destination.rglob("*")
            if path.is_file()
        }
        self.assertEqual(actual_files, expected_files)
        self.assertEqual(
            json.loads((destination / "pack.mcmeta").read_text(encoding="utf-8")),
            {
                "pack": {
                    "pack_format": 15,
                    "description": "Corrupted Silverfish v3 – concept-faithful preview",
                }
            },
        )
        manifest = json.loads((self.root / validate.MANIFEST_RELATIVE).read_text("utf-8"))
        for source_relative, pack_relative in preview_pack.PACK_ASSETS.items():
            source = self.root / source_relative
            packed = destination / pack_relative
            self.assertEqual(packed.read_bytes(), source.read_bytes())
            self.assertEqual(hashlib.sha256(packed.read_bytes()).hexdigest().upper(), manifest[source_relative.as_posix()])

    def test_rerun_is_deterministic(self):
        destination = self.build()
        first = {p.relative_to(destination): p.read_bytes() for p in destination.rglob("*") if p.is_file()}
        self.build()
        second = {p.relative_to(destination): p.read_bytes() for p in destination.rglob("*") if p.is_file()}
        self.assertEqual(first, second)

    def test_validator_failure_leaves_existing_pack_unchanged(self):
        destination = self.root / preview_pack.PREVIEW_RELATIVE
        destination.mkdir(parents=True)
        marker = destination / "pack.mcmeta"
        marker.write_bytes(b"old")
        before = marker.read_bytes()
        with mock.patch.object(preview_pack, "_run_validator", side_effect=preview_pack.PreviewPackFailure("validator failed")):
            with self.assertRaisesRegex(preview_pack.PreviewPackFailure, "validator failed"):
                preview_pack.build_preview_pack(self.root)
        self.assertEqual(marker.read_bytes(), before)

    def test_validator_is_an_independent_process_and_requires_pass_marker(self):
        failed = subprocess.CompletedProcess([], 1, "", "ASSET_CHECK_FAILED: broken\n")
        with mock.patch.object(preview_pack.subprocess, "run", return_value=failed) as run:
            with self.assertRaisesRegex(preview_pack.PreviewPackFailure, "validator failed.*broken"):
                preview_pack._run_validator(self.root)
        command = run.call_args.args[0]
        self.assertEqual(command[:3], [preview_pack.sys.executable, "-m", "tools.corrupted_silverfish_v3.validate"])
        false_success = subprocess.CompletedProcess([], 0, "not the validator\n", "")
        with mock.patch.object(preview_pack.subprocess, "run", return_value=false_success):
            with self.assertRaisesRegex(preview_pack.PreviewPackFailure, "validator failed"):
                preview_pack._run_validator(self.root)

    def test_cli_has_exact_pass_and_failure_prefixes(self):
        expected = self.root / preview_pack.PREVIEW_RELATIVE
        stdout = StringIO()
        with mock.patch.object(preview_pack, "build_preview_pack", return_value=expected), redirect_stdout(stdout):
            self.assertEqual(preview_pack.main(["--root", str(self.root)]), 0)
        self.assertEqual(stdout.getvalue().strip(), f"PREVIEW_PACK=PASS;TARGET=V3;FILES=5;PATH={expected}")
        stderr = StringIO()
        with mock.patch.object(preview_pack, "build_preview_pack", side_effect=preview_pack.PreviewPackFailure("bad")), redirect_stderr(stderr):
            self.assertEqual(preview_pack.main(["--root", str(self.root)]), 1)
        self.assertEqual(stderr.getvalue().strip(), "PREVIEW_PACK_FAILED: bad")

    def test_root_symlink_is_rejected_before_validator_or_destination_write(self):
        link = self.root.parent / f"{self.root.name}-link"
        try:
            link.symlink_to(self.root, target_is_directory=True)
        except OSError as exc:
            self.skipTest(f"symlink creation unavailable: {exc}")
        try:
            with mock.patch.object(preview_pack, "_run_validator") as validator_run:
                with self.assertRaisesRegex(preview_pack.PreviewPackFailure, "root.*symlink/reparse"):
                    preview_pack.build_preview_pack(link)
            validator_run.assert_not_called()
            self.assertFalse((self.root / preview_pack.PREVIEW_RELATIVE).exists())
        finally:
            link.unlink(missing_ok=True)

    def test_manifest_hash_mismatch_leaves_existing_pack_unchanged(self):
        destination = self.build()
        before = {p.relative_to(destination): p.read_bytes() for p in destination.rglob("*") if p.is_file()}
        manifest_path = self.root / validate.MANIFEST_RELATIVE
        manifest = json.loads(manifest_path.read_text("utf-8"))
        manifest[next(iter(manifest))] = "0" * 64
        manifest_path.write_text(json.dumps(manifest), encoding="utf-8")
        with mock.patch.object(preview_pack, "_run_validator"):
            with self.assertRaisesRegex(preview_pack.PreviewPackFailure, "manifest hash mismatch"):
                preview_pack.build_preview_pack(self.root)
        after = {p.relative_to(destination): p.read_bytes() for p in destination.rglob("*") if p.is_file()}
        self.assertEqual(after, before)

    def test_stale_extra_file_is_rejected_without_deletion(self):
        destination = self.build()
        extra = destination / "stale.txt"
        extra.write_text("keep", encoding="utf-8")
        with mock.patch.object(preview_pack, "_run_validator"):
            with self.assertRaisesRegex(preview_pack.PreviewPackFailure, "unexpected files"):
                preview_pack.build_preview_pack(self.root)
        self.assertEqual(extra.read_text("utf-8"), "keep")

    def test_stage_failure_and_publish_failure_roll_back(self):
        destination = self.build()
        old = {p.relative_to(destination): p.read_bytes() for p in destination.rglob("*") if p.is_file()}
        original_write = preview_pack._write_stage_file
        calls = 0

        def fail_fifth(path, contents):
            nonlocal calls
            calls += 1
            if calls == 5:
                raise OSError("injected stage failure")
            return original_write(path, contents)

        with mock.patch.object(preview_pack, "_run_validator"), mock.patch.object(preview_pack, "_write_stage_file", side_effect=fail_fifth):
            with self.assertRaisesRegex(preview_pack.PreviewPackFailure, "injected stage failure"):
                preview_pack.build_preview_pack(self.root)
        self.assertEqual(old, {p.relative_to(destination): p.read_bytes() for p in destination.rglob("*") if p.is_file()})

        real_replace = os.replace
        replace_calls = 0

        def fail_second_replace(source, target):
            nonlocal replace_calls
            replace_calls += 1
            if replace_calls == 2:
                raise OSError("injected publish failure")
            return real_replace(source, target)

        with mock.patch.object(preview_pack, "_run_validator"), mock.patch.object(preview_pack.os, "replace", side_effect=fail_second_replace):
            with self.assertRaisesRegex(preview_pack.PreviewPackFailure, "injected publish failure"):
                preview_pack.build_preview_pack(self.root)
        self.assertEqual(old, {p.relative_to(destination): p.read_bytes() for p in destination.rglob("*") if p.is_file()})

    def test_preview_closes_java_resource_and_animation_gate(self):
        destination = self.build()
        geo = json.loads((destination / preview_pack.PACK_ASSETS[validate.RELATIVE_PATHS[1]]).read_text("utf-8"))
        animation = json.loads((destination / preview_pack.PACK_ASSETS[validate.RELATIVE_PATHS[4]]).read_text("utf-8"))
        self.assertEqual(len(geo["minecraft:geometry"][0]["bones"]), 32)
        self.assertEqual(set(animation["animations"]), set(validate.EXPECTED_ANIMATIONS))
        for relative in (validate.RELATIVE_PATHS[2], validate.RELATIVE_PATHS[3]):
            with Image.open(destination / preview_pack.PACK_ASSETS[relative]) as image:
                self.assertEqual(image.size, (256, 256))

        model_java = (self.root / "src/main/mobs/endermite/java/net/mysith/client/CorruptedSilverfishModel.java")
        entity_java = (self.root / "src/main/mobs/endermite/java/net/mysith/silverfish/CorruptedSilverfishEntity.java")
        glow_java = (self.root / "src/main/mobs/endermite/java/net/mysith/client/CorruptedSilverfishGlowLayer.java")
        for source in (model_java, entity_java, glow_java):
            real = ROOT / source.relative_to(self.root)
            source.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(real, source)
        model_text = model_java.read_text("utf-8")
        entity_text = entity_java.read_text("utf-8")
        glow_text = glow_java.read_text("utf-8")
        for resource in (
            "geo/corrupted_silverfish.geo.json",
            "textures/entity/corrupted_silverfish.png",
            "animations/corrupted_silverfish.animation.json",
        ):
            self.assertIn(resource, model_text)
        for animation_id in validate.EXPECTED_ANIMATIONS:
            self.assertIn(animation_id, entity_text)
        self.assertIn("AutoGlowingGeoLayer", glow_text)
        self.assertIn("corrupted_silverfish_glowmask.png", (ROOT / "src/main/mobs/endermite/java/net/mysith/client/CorruptedSilverfishRenderer.java").read_text("utf-8"))


if __name__ == "__main__":
    unittest.main()
