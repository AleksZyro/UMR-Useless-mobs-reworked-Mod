"""Build the isolated Corrupted Silverfish v3 resource-pack preview."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import stat
import subprocess
import sys
import tempfile
from typing import Dict, Mapping, Optional, Sequence
from uuid import uuid4

from . import validate


DESCRIPTION = "Corrupted Silverfish v3 – concept-faithful preview"
PREVIEW_RELATIVE = Path("run/resourcepacks/corrupted_silverfish_v3_preview")
PACK_ASSETS = {
    validate.RELATIVE_PATHS[1]: Path("assets/usless_mobs/geo/corrupted_silverfish.geo.json"),
    validate.RELATIVE_PATHS[2]: Path("assets/usless_mobs/textures/entity/corrupted_silverfish.png"),
    validate.RELATIVE_PATHS[3]: Path("assets/usless_mobs/textures/entity/corrupted_silverfish_glowmask.png"),
    validate.RELATIVE_PATHS[4]: Path("assets/usless_mobs/animations/corrupted_silverfish.animation.json"),
}
PACK_META = (
    json.dumps(
        {"pack": {"pack_format": 15, "description": DESCRIPTION}},
        ensure_ascii=False,
        indent=2,
    )
    + "\n"
).encode("utf-8")
EXPECTED_PACK_DIRECTORIES = {
    Path("assets"),
    Path("assets/usless_mobs"),
    Path("assets/usless_mobs/geo"),
    Path("assets/usless_mobs/textures"),
    Path("assets/usless_mobs/textures/entity"),
    Path("assets/usless_mobs/animations"),
}


class PreviewPackFailure(Exception):
    """A safe, concise failure suitable for the command-line interface."""


def _lexists(path: Path) -> bool:
    return os.path.lexists(str(path))


def _is_reparse_or_link(path: Path) -> bool:
    try:
        info = path.lstat()
    except OSError as exc:
        raise PreviewPackFailure(f"cannot inspect path {path}: {exc}") from exc
    attributes = getattr(info, "st_file_attributes", 0)
    reparse = getattr(stat, "FILE_ATTRIBUTE_REPARSE_POINT", 0x400)
    return stat.S_ISLNK(info.st_mode) or bool(attributes & reparse)


def _inside(root: Path, path: Path) -> bool:
    try:
        return os.path.commonpath((str(root), str(path))) == str(root)
    except ValueError:
        return False


def _secure_existing(root: Path, path: Path, label: str) -> None:
    if not _inside(root, path):
        raise PreviewPackFailure(f"{label} escapes project root: {path}")
    current = root
    if _is_reparse_or_link(current):
        raise PreviewPackFailure(f"project root is a symlink/reparse point: {root}")
    for part in path.relative_to(root).parts:
        current = current / part
        if not _lexists(current):
            raise PreviewPackFailure(f"{label} is missing: {current}")
        if _is_reparse_or_link(current):
            raise PreviewPackFailure(f"{label} crosses a symlink/reparse point: {current}")


def _secure_parent(root: Path, parent: Path) -> None:
    if not _inside(root, parent):
        raise PreviewPackFailure(f"preview destination escapes project root: {parent}")
    if _is_reparse_or_link(root):
        raise PreviewPackFailure(f"project root is a symlink/reparse point: {root}")
    current = root
    for part in parent.relative_to(root).parts:
        current = current / part
        if _lexists(current):
            if _is_reparse_or_link(current) or not current.is_dir():
                raise PreviewPackFailure(f"unsafe preview destination component: {current}")
        else:
            try:
                current.mkdir()
            except OSError as exc:
                raise PreviewPackFailure(f"cannot create preview destination {current}: {exc}") from exc


def _run_validator(project_root: Path, candidate_root: Path) -> None:
    validator_entry = project_root / "tools/corrupted_silverfish_v3/validate.py"
    _secure_existing(project_root, validator_entry, "validator entry")
    command = [
        sys.executable,
        str(validator_entry),
        "--root",
        str(candidate_root),
    ]
    try:
        result = subprocess.run(
            command,
            cwd=str(project_root),
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
            check=False,
        )
    except OSError as exc:
        raise PreviewPackFailure(f"validator process could not start: {exc}") from exc
    output = result.stdout.strip()
    if result.returncode != 0 or not output.startswith("ASSET_CHECK=PASS;"):
        detail = result.stderr.strip() or output or f"exit code {result.returncode}"
        raise PreviewPackFailure(f"validator failed: {detail}")


def _parse_manifest(contents: bytes, label: str) -> Mapping[str, object]:
    try:
        manifest = json.loads(contents.decode("utf-8"))
    except (UnicodeError, json.JSONDecodeError) as exc:
        raise PreviewPackFailure(f"{label} is unreadable: {exc}") from exc
    expected_keys = {path.as_posix() for path in validate.RELATIVE_PATHS}
    if not isinstance(manifest, dict) or set(manifest) != expected_keys:
        raise PreviewPackFailure(f"{label} must contain exactly the 5 candidate paths")
    return manifest


def _read_bounded_once(path: Path, limit: int, label: str) -> bytes:
    try:
        size = path.stat().st_size
        if size > limit:
            raise PreviewPackFailure(f"{label} exceeds size limit {limit} bytes: {size}")
        contents = path.read_bytes()
    except PreviewPackFailure:
        raise
    except OSError as exc:
        raise PreviewPackFailure(f"{label} is unreadable ({path}): {exc}") from exc
    if len(contents) > limit:
        raise PreviewPackFailure(f"{label} exceeds size limit {limit} bytes after read: {len(contents)}")
    return contents


def _verify_manifest_hashes(
    manifest_contents: bytes,
    snapshots: Mapping[Path, bytes],
    label: str,
) -> None:
    manifest = _parse_manifest(manifest_contents, label)
    for relative in validate.RELATIVE_PATHS:
        expected = manifest.get(relative.as_posix())
        if not isinstance(expected, str) or len(expected) != 64 or expected != expected.upper():
            raise PreviewPackFailure(f"invalid {label} hash for {relative.as_posix()}")
        actual = hashlib.sha256(snapshots[relative]).hexdigest().upper()
        if actual != expected:
            raise PreviewPackFailure(
                f"{label} hash mismatch for {relative.as_posix()}: expected {expected}, got {actual}"
            )


def _read_candidate_snapshots(root: Path) -> tuple[Dict[Path, bytes], bytes]:
    manifest_path = root / validate.MANIFEST_RELATIVE
    _secure_existing(root, manifest_path, "candidate manifest")
    manifest_contents = _read_bounded_once(manifest_path, 64 * 1024, "candidate manifest")
    snapshots: Dict[Path, bytes] = {}
    for relative, limit in zip(validate.RELATIVE_PATHS, validate.FILE_LIMITS):
        path = root / relative
        _secure_existing(root, path, "candidate file")
        snapshots[relative] = _read_bounded_once(path, limit, f"candidate file {relative.as_posix()}")
    _verify_manifest_hashes(manifest_contents, snapshots, "committed candidate manifest")
    return snapshots, manifest_contents


def _validate_snapshots(
    root: Path,
    snapshots: Mapping[Path, bytes],
    committed_manifest: bytes,
) -> None:
    validation_root = Path(tempfile.mkdtemp(prefix=".corrupted_silverfish_v3_validation.", dir=str(root)))
    primary: Optional[BaseException] = None
    cleanup_errors: list[str] = []
    try:
        for relative in validate.RELATIVE_PATHS:
            _write_stage_file(validation_root / relative, snapshots[relative])
        _write_stage_file(validation_root / validate.MANIFEST_RELATIVE, committed_manifest)
        _run_validator(root, validation_root)
        snapshot_manifest = validation_root / validate.MANIFEST_RELATIVE
        _secure_existing(validation_root, snapshot_manifest, "validated snapshot manifest")
        generated_manifest = _read_bounded_once(
            snapshot_manifest, 64 * 1024, "validated snapshot manifest"
        )
        _verify_manifest_hashes(generated_manifest, snapshots, "validated snapshot manifest")
    except BaseException as exc:
        primary = exc
    finally:
        _cleanup_tree(validation_root, "validated snapshot", cleanup_errors)
    if primary is not None or cleanup_errors:
        if primary is None:
            raise PreviewPackFailure(
                "validated snapshots passed, cleanup incomplete; " + "; ".join(cleanup_errors)
            )
        raise PreviewPackFailure(_failure_message(primary, [], cleanup_errors)) from primary


def _expected_pack_files() -> set[Path]:
    return {Path("pack.mcmeta"), *PACK_ASSETS.values()}


def _pack_entries(directory: Path) -> tuple[set[Path], set[Path]]:
    files: set[Path] = set()
    directories: set[Path] = set()
    for path in directory.rglob("*"):
        if _is_reparse_or_link(path):
            raise PreviewPackFailure(f"preview pack contains a symlink/reparse point: {path}")
        if path.is_file():
            files.add(path.relative_to(directory))
        elif path.is_dir():
            directories.add(path.relative_to(directory))
        else:
            raise PreviewPackFailure(f"preview pack contains an unsupported entry: {path}")
    return files, directories


def _reject_stale_destination(root: Path, destination: Path) -> None:
    if not _lexists(destination):
        return
    _secure_existing(root, destination, "preview destination")
    if not destination.is_dir():
        raise PreviewPackFailure(f"preview destination is not a directory: {destination}")
    files, directories = _pack_entries(destination)
    extra_files = files - _expected_pack_files()
    extra_directories = directories - EXPECTED_PACK_DIRECTORIES
    if extra_files or extra_directories:
        details = []
        if extra_files:
            details.append("files: " + ", ".join(sorted(path.as_posix() for path in extra_files)))
        if extra_directories:
            details.append("directories: " + ", ".join(sorted(path.as_posix() for path in extra_directories)))
        raise PreviewPackFailure(
            "preview destination has unexpected entries; remove them manually: " + "; ".join(details)
        )


def _write_stage_file(path: Path, contents: bytes) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    descriptor, temporary_name = tempfile.mkstemp(prefix=f".{path.name}.", suffix=".tmp", dir=str(path.parent))
    temporary = Path(temporary_name)
    primary: Optional[BaseException] = None
    cleanup: list[str] = []
    descriptor_open = True
    try:
        handle = os.fdopen(descriptor, "wb")
        descriptor_open = False  # ownership transferred to the context manager
        with handle:
            handle.write(contents)
            handle.flush()
            os.fsync(handle.fileno())
        os.replace(temporary, path)
    except BaseException as exc:
        primary = exc
    finally:
        if descriptor_open:
            try:
                os.close(descriptor)
            except BaseException as exc:
                cleanup.append(f"descriptor cleanup failed for {temporary}: {exc}")
        try:
            temporary.unlink(missing_ok=True)
        except BaseException as exc:
            cleanup.append(f"temporary cleanup failed for {temporary}: {exc}")
    if primary is not None or cleanup:
        details = []
        if primary is not None:
            details.append(str(primary))
        details.extend(cleanup)
        raise OSError("; ".join(details)) from primary


def _verify_pack(destination: Path, snapshots: Mapping[Path, bytes]) -> None:
    actual_files, actual_directories = _pack_entries(destination)
    if actual_files != _expected_pack_files() or actual_directories != EXPECTED_PACK_DIRECTORIES:
        raise PreviewPackFailure("published preview pack file/directory listing is not exact")
    if (destination / "pack.mcmeta").read_bytes() != PACK_META:
        raise PreviewPackFailure("published pack.mcmeta byte verification failed")
    for source, packed in PACK_ASSETS.items():
        if (destination / packed).read_bytes() != snapshots[source]:
            raise PreviewPackFailure(f"published asset byte verification failed: {packed.as_posix()}")


def _remove_tree(path: Path) -> None:
    shutil.rmtree(path)


def _cleanup_tree(path: Path, purpose: str, errors: list[str]) -> bool:
    if not _lexists(path):
        return True
    try:
        _remove_tree(path)
        return True
    except BaseException as exc:
        errors.append(f"{purpose} cleanup failed for {path}: {exc}")
        return False


def _failure_message(primary: BaseException, rollback: list[str], cleanup: list[str]) -> str:
    details = [f"preview pack transaction failed: {primary}"]
    details.extend(rollback)
    details.extend(cleanup)
    return "; ".join(details)


def _publish(stage: Path, destination: Path, snapshots: Mapping[Path, bytes]) -> None:
    backup = destination.parent / f".{destination.name}.backup.{uuid4().hex}"
    had_destination = _lexists(destination)
    moved_old = False
    published = False
    try:
        if had_destination:
            os.replace(destination, backup)
            moved_old = True
        os.replace(stage, destination)
        published = True
        _verify_pack(destination, snapshots)
    except BaseException as exc:
        rollback_errors: list[str] = []
        cleanup_errors: list[str] = []
        failed: Optional[Path] = None
        new_pack_isolated = False
        if published and _lexists(destination):
            failed = destination.parent / f".{destination.name}.failed.{uuid4().hex}"
            try:
                os.replace(destination, failed)
                new_pack_isolated = True
            except BaseException as rollback_exc:
                rollback_errors.append(
                    f"rollback failed moving published pack from {destination} to {failed}: {rollback_exc}; "
                    f"preserved backup={backup if moved_old else 'none'}"
                )
        if moved_old and (not published or new_pack_isolated) and _lexists(backup):
            try:
                os.replace(backup, destination)
                moved_old = False
            except BaseException as rollback_exc:
                rollback_errors.append(
                    f"rollback failed restoring backup {backup} to {destination}: {rollback_exc}; "
                    f"preserved backup={backup}, failed={failed if failed is not None else 'none'}"
                )
        if _lexists(stage):
            _cleanup_tree(stage, "stage", cleanup_errors)
        rollback_complete = not rollback_errors and not moved_old
        if failed is not None and _lexists(failed) and rollback_complete:
            _cleanup_tree(failed, "failed publication", cleanup_errors)
        raise PreviewPackFailure(_failure_message(exc, rollback_errors, cleanup_errors)) from exc

    cleanup_errors: list[str] = []
    if _lexists(backup):
        _cleanup_tree(backup, "published backup", cleanup_errors)
    if cleanup_errors:
        raise PreviewPackFailure(
            "pack published, cleanup incomplete; new pack retained at "
            f"{destination}; " + "; ".join(cleanup_errors)
        )


def build_preview_pack(root: Path) -> Path:
    root = Path(os.path.abspath(str(root)))
    if not root.is_dir():
        raise PreviewPackFailure(f"project root is missing or not a directory: {root}")
    if _is_reparse_or_link(root):
        raise PreviewPackFailure(f"project root is a symlink/reparse point: {root}")

    # Read each source exactly once, then validate only the immutable staged
    # snapshots. Every later hash check and pack write consumes those bytes.
    snapshots, committed_manifest = _read_candidate_snapshots(root)
    _validate_snapshots(root, snapshots, committed_manifest)

    destination = root / PREVIEW_RELATIVE
    _reject_stale_destination(root, destination)
    _secure_parent(root, destination.parent)
    stage = Path(tempfile.mkdtemp(prefix=f".{destination.name}.stage.", dir=str(destination.parent)))
    try:
        _write_stage_file(stage / "pack.mcmeta", PACK_META)
        for source, packed in PACK_ASSETS.items():
            _write_stage_file(stage / packed, snapshots[source])
        _verify_pack(stage, snapshots)
    except BaseException as exc:
        cleanup_errors: list[str] = []
        _cleanup_tree(stage, "stage", cleanup_errors)
        raise PreviewPackFailure(_failure_message(exc, [], cleanup_errors)) from exc
    _publish(stage, destination, snapshots)
    return destination


def _parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Build the isolated Corrupted Silverfish v3 preview pack")
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[2])
    return parser


def main(argv: Optional[Sequence[str]] = None) -> int:
    args = _parser().parse_args(argv)
    try:
        destination = build_preview_pack(args.root)
    except PreviewPackFailure as exc:
        print(f"PREVIEW_PACK_FAILED: {exc}", file=sys.stderr)
        return 1
    except Exception as exc:
        print(f"PREVIEW_PACK_FAILED: unexpected builder error: {exc}", file=sys.stderr)
        return 1
    print(f"PREVIEW_PACK=PASS;TARGET=V3;FILES=5;PATH={destination}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
