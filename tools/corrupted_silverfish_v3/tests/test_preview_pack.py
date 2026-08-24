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
        run.assert_called_once()
        project_root, snapshot_root = run.call_args.args
        self.assertEqual(project_root, self.root)
        self.assertTrue(snapshot_root.name.startswith(".corrupted_silverfish_v3_validation."))
        self.assertFalse(snapshot_root.exists())
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
        actual_directories = {
            path.relative_to(destination)
            for path in destination.rglob("*")
            if path.is_dir()
        }
        self.assertEqual(actual_directories, preview_pack.EXPECTED_PACK_DIRECTORIES)
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
                preview_pack._run_validator(ROOT, self.root)
        command = run.call_args.args[0]
        self.assertEqual(
            command[:2],
            [preview_pack.sys.executable, str(ROOT / "tools/corrupted_silverfish_v3/validate.py")],
        )
        false_success = subprocess.CompletedProcess([], 0, "not the validator\n", "")
        with mock.patch.object(preview_pack.subprocess, "run", return_value=false_success):
            with self.assertRaisesRegex(preview_pack.PreviewPackFailure, "validator failed"):
                preview_pack._run_validator(ROOT, self.root)

    def test_source_swap_after_snapshot_validation_cannot_reach_published_pack(self):
        validator = self.root / "tools/corrupted_silverfish_v3/validate.py"
        validator.parent.mkdir(parents=True)
        shutil.copyfile(ROOT / "tools/corrupted_silverfish_v3/validate.py", validator)
        old_assets = {
            relative: (self.root / relative).read_bytes()
            for relative in validate.RELATIVE_PATHS
        }
        old_pack_assets = {
            relative: old_assets[relative]
            for relative in preview_pack.PACK_ASSETS
        }
        real_validator = preview_pack._run_validator

        def validate_then_replace_sources(project_root, snapshot_root):
            real_validator(project_root, snapshot_root)
            replacements = {}
            for index, relative in enumerate(validate.RELATIVE_PATHS):
                replacement = f"unchecked-replacement-{index}".encode("ascii")
                (self.root / relative).write_bytes(replacement)
                replacements[relative.as_posix()] = hashlib.sha256(replacement).hexdigest().upper()
            (self.root / validate.MANIFEST_RELATIVE).write_text(
                json.dumps(replacements, indent=2) + "\n", encoding="utf-8"
            )

        with mock.patch.object(preview_pack, "_run_validator", side_effect=validate_then_replace_sources):
            destination = preview_pack.build_preview_pack(self.root)
        for relative, packed in preview_pack.PACK_ASSETS.items():
            self.assertEqual((destination / packed).read_bytes(), old_pack_assets[relative])
            self.assertNotEqual((destination / packed).read_bytes(), (self.root / relative).read_bytes())

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
            with self.assertRaisesRegex(preview_pack.PreviewPackFailure, "unexpected entries.*stale.txt"):
                preview_pack.build_preview_pack(self.root)
        self.assertEqual(extra.read_text("utf-8"), "keep")

    def test_empty_extra_directories_at_top_and_deep_are_rejected_unchanged(self):
        destination = self.build()
        top = destination / "unexpected-empty"
        deep = destination / "assets/usless_mobs/textures/entity/deep-empty"
        top.mkdir()
        deep.mkdir()
        before = sorted(path.relative_to(destination).as_posix() for path in destination.rglob("*"))
        with mock.patch.object(preview_pack, "_run_validator"):
            with self.assertRaisesRegex(preview_pack.PreviewPackFailure, "unexpected entries.*directories.*deep-empty"):
                preview_pack.build_preview_pack(self.root)
        after = sorted(path.relative_to(destination).as_posix() for path in destination.rglob("*"))
        self.assertEqual(after, before)
        self.assertTrue(top.is_dir())
        self.assertTrue(deep.is_dir())

    def test_symlink_entry_is_rejected_without_touching_destination_when_supported(self):
        destination = self.build()
        link = destination / "linked"
        try:
            link.symlink_to(self.root, target_is_directory=True)
        except OSError as exc:
            self.skipTest(f"symlink creation unavailable: {exc}")
        try:
            with mock.patch.object(preview_pack, "_run_validator"):
                with self.assertRaisesRegex(preview_pack.PreviewPackFailure, "symlink/reparse"):
                    preview_pack.build_preview_pack(self.root)
            self.assertTrue(link.is_symlink())
        finally:
            link.unlink(missing_ok=True)

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

        with mock.patch.object(preview_pack, "_validate_snapshots"), mock.patch.object(preview_pack, "_write_stage_file", side_effect=fail_fifth):
            with self.assertRaisesRegex(preview_pack.PreviewPackFailure, "injected stage failure"):
                preview_pack.build_preview_pack(self.root)
        self.assertEqual(old, {p.relative_to(destination): p.read_bytes() for p in destination.rglob("*") if p.is_file()})

        real_replace = os.replace

        def fail_stage_publish(source, target):
            if ".stage." in Path(source).name and Path(target) == destination:
                raise OSError("injected publish failure")
            return real_replace(source, target)

        with mock.patch.object(preview_pack, "_run_validator"), mock.patch.object(preview_pack.os, "replace", side_effect=fail_stage_publish):
            with self.assertRaisesRegex(preview_pack.PreviewPackFailure, "injected publish failure"):
                preview_pack.build_preview_pack(self.root)
        self.assertEqual(old, {p.relative_to(destination): p.read_bytes() for p in destination.rglob("*") if p.is_file()})

    def test_stage_cleanup_failure_preserves_primary_and_reports_retained_path(self):
        with mock.patch.object(preview_pack, "_validate_snapshots"), \
                mock.patch.object(preview_pack, "_write_stage_file", side_effect=OSError("primary stage write")), \
                mock.patch.object(preview_pack, "_remove_tree", side_effect=OSError("stage locked")):
            with self.assertRaises(preview_pack.PreviewPackFailure) as raised:
                preview_pack.build_preview_pack(self.root)
        message = str(raised.exception)
        self.assertIn("primary stage write", message)
        self.assertIn("stage cleanup failed", message)
        self.assertIn("stage locked", message)
        retained = list((self.root / "run/resourcepacks").glob(".corrupted_silverfish_v3_preview.stage.*"))
        self.assertEqual(len(retained), 1)
        shutil.rmtree(retained[0])

    def test_temporary_file_cleanup_does_not_mask_write_primary(self):
        target = self.root / "stage" / "file.bin"
        target.parent.mkdir()
        with mock.patch.object(preview_pack.os, "replace", side_effect=OSError("replace primary")), \
                mock.patch.object(Path, "unlink", side_effect=OSError("temp locked")):
            with self.assertRaises(OSError) as raised:
                preview_pack._write_stage_file(target, b"candidate")
        message = str(raised.exception)
        self.assertIn("replace primary", message)
        self.assertIn("temporary cleanup failed", message)
        self.assertIn("temp locked", message)
        for temporary in target.parent.glob(".*.tmp"):
            temporary.unlink()

    def test_prepublish_verification_failure_cleans_stage_and_preserves_old_pack(self):
        destination = self.build()
        old = {p.relative_to(destination): p.read_bytes() for p in destination.rglob("*") if p.is_file()}
        with mock.patch.object(preview_pack, "_run_validator"), \
                mock.patch.object(preview_pack, "_verify_pack", side_effect=preview_pack.PreviewPackFailure("prepublish verify")):
            with self.assertRaisesRegex(preview_pack.PreviewPackFailure, "prepublish verify"):
                preview_pack.build_preview_pack(self.root)
        self.assertEqual(old, {p.relative_to(destination): p.read_bytes() for p in destination.rglob("*") if p.is_file()})
        self.assertEqual(list(destination.parent.glob(".corrupted_silverfish_v3_preview.stage.*")), [])

    def test_publish_failure_restores_old_and_reports_failed_cleanup_path(self):
        destination = self.build()
        old_meta = destination.joinpath("pack.mcmeta").read_bytes()
        verify_calls = 0
        real_verify = preview_pack._verify_pack

        def fail_published(path, snapshots):
            nonlocal verify_calls
            verify_calls += 1
            if verify_calls == 2:
                raise preview_pack.PreviewPackFailure("published verify failed")
            return real_verify(path, snapshots)

        with mock.patch.object(preview_pack, "_validate_snapshots"), \
                mock.patch.object(preview_pack, "_verify_pack", side_effect=fail_published), \
                mock.patch.object(preview_pack, "_remove_tree", side_effect=OSError("failed dir locked")):
            with self.assertRaises(preview_pack.PreviewPackFailure) as raised:
                preview_pack.build_preview_pack(self.root)
        self.assertIn("published verify failed", str(raised.exception))
        self.assertIn("failed publication cleanup failed", str(raised.exception))
        self.assertEqual(destination.joinpath("pack.mcmeta").read_bytes(), old_meta)
        failed = list(destination.parent.glob(".corrupted_silverfish_v3_preview.failed.*"))
        self.assertEqual(len(failed), 1)
        self.assertEqual(list(destination.parent.glob(".corrupted_silverfish_v3_preview.backup.*")), [])
        shutil.rmtree(failed[0])

    def test_rollback_failure_preserves_backup_and_failed_pack_with_paths(self):
        destination = self.build()
        verify_calls = 0
        real_verify = preview_pack._verify_pack
        real_replace = os.replace

        def fail_published(path, snapshots):
            nonlocal verify_calls
            verify_calls += 1
            if verify_calls == 2:
                raise preview_pack.PreviewPackFailure("published verify failed")
            return real_verify(path, snapshots)

        def fail_restore(source, target):
            if ".backup." in str(source) and Path(target) == destination:
                raise OSError("restore locked")
            return real_replace(source, target)

        with mock.patch.object(preview_pack, "_run_validator"), \
                mock.patch.object(preview_pack, "_verify_pack", side_effect=fail_published), \
                mock.patch.object(preview_pack.os, "replace", side_effect=fail_restore):
            with self.assertRaises(preview_pack.PreviewPackFailure) as raised:
                preview_pack.build_preview_pack(self.root)
        message = str(raised.exception)
        self.assertIn("rollback failed restoring backup", message)
        self.assertIn("restore locked", message)
        self.assertFalse(destination.exists())
        backups = list(destination.parent.glob(".corrupted_silverfish_v3_preview.backup.*"))
        failed = list(destination.parent.glob(".corrupted_silverfish_v3_preview.failed.*"))
        self.assertEqual((len(backups), len(failed)), (1, 1))
        os.replace(backups[0], destination)
        shutil.rmtree(failed[0])

    def test_successful_publish_with_backup_cleanup_failure_keeps_new_pack(self):
        destination = self.build()
        old_meta = destination.joinpath("pack.mcmeta").read_bytes()
        replacement_meta = preview_pack.PACK_META.replace(b"preview", b"preview-new")
        with mock.patch.object(preview_pack, "_validate_snapshots"), \
                mock.patch.object(preview_pack, "PACK_META", replacement_meta), \
                mock.patch.object(preview_pack, "_remove_tree", side_effect=OSError("backup locked")):
            with self.assertRaises(preview_pack.PreviewPackFailure) as raised:
                preview_pack.build_preview_pack(self.root)
        message = str(raised.exception)
        self.assertIn("pack published, cleanup incomplete", message)
        self.assertIn("backup locked", message)
        self.assertEqual(destination.joinpath("pack.mcmeta").read_bytes(), replacement_meta)
        self.assertNotEqual(destination.joinpath("pack.mcmeta").read_bytes(), old_meta)
        backups = list(destination.parent.glob(".corrupted_silverfish_v3_preview.backup.*"))
        self.assertEqual(len(backups), 1)
        shutil.rmtree(backups[0])

    def test_normal_publish_leaves_no_transaction_directories(self):
        destination = self.build()
        leftovers = [
            path for path in destination.parent.iterdir()
            if path.name.startswith(".corrupted_silverfish_v3_preview.")
        ]
        self.assertEqual(leftovers, [])

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
