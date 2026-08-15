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


def _run_validator(root: Path) -> None:
    command = [
        sys.executable,
        "-m",
        "tools.corrupted_silverfish_v3.validate",
        "--root",
        str(root),
    ]
    try:
        result = subprocess.run(
            command,
            cwd=str(root),
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


def _verified_candidate(root: Path) -> Dict[Path, bytes]:
    manifest_path = root / validate.MANIFEST_RELATIVE
    _secure_existing(root, manifest_path, "candidate manifest")
    try:
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    except (OSError, UnicodeError, json.JSONDecodeError) as exc:
        raise PreviewPackFailure(f"candidate manifest is unreadable: {exc}") from exc
    expected_keys = {path.as_posix() for path in validate.RELATIVE_PATHS}
    if not isinstance(manifest, dict) or set(manifest) != expected_keys:
        raise PreviewPackFailure("candidate manifest must contain exactly the 5 candidate paths")

    snapshots: Dict[Path, bytes] = {}
    for relative in validate.RELATIVE_PATHS:
        expected = manifest.get(relative.as_posix())
        if not isinstance(expected, str) or len(expected) != 64 or expected != expected.upper():
            raise PreviewPackFailure(f"invalid manifest hash for {relative.as_posix()}")
        path = root / relative
        _secure_existing(root, path, "candidate file")
        try:
            contents = path.read_bytes()
        except OSError as exc:
            raise PreviewPackFailure(f"candidate file is unreadable ({relative.as_posix()}): {exc}") from exc
        actual = hashlib.sha256(contents).hexdigest().upper()
        if actual != expected:
            raise PreviewPackFailure(
                f"manifest hash mismatch for {relative.as_posix()}: expected {expected}, got {actual}"
            )
        snapshots[relative] = contents
    return snapshots


def _expected_pack_files() -> set[Path]:
    return {Path("pack.mcmeta"), *PACK_ASSETS.values()}


def _pack_files(directory: Path) -> set[Path]:
    files: set[Path] = set()
    for path in directory.rglob("*"):
        if _is_reparse_or_link(path):
            raise PreviewPackFailure(f"preview pack contains a symlink/reparse point: {path}")
        if path.is_file():
            files.add(path.relative_to(directory))
        elif not path.is_dir():
            raise PreviewPackFailure(f"preview pack contains an unsupported entry: {path}")
    return files


def _reject_stale_destination(root: Path, destination: Path) -> None:
    if not _lexists(destination):
        return
    _secure_existing(root, destination, "preview destination")
    if not destination.is_dir():
        raise PreviewPackFailure(f"preview destination is not a directory: {destination}")
    extras = _pack_files(destination) - _expected_pack_files()
    if extras:
        names = ", ".join(sorted(path.as_posix() for path in extras))
        raise PreviewPackFailure(f"preview destination has unexpected files; remove them manually: {names}")


def _write_stage_file(path: Path, contents: bytes) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    descriptor, temporary_name = tempfile.mkstemp(prefix=f".{path.name}.", suffix=".tmp", dir=str(path.parent))
    temporary = Path(temporary_name)
    try:
        with os.fdopen(descriptor, "wb") as handle:
            handle.write(contents)
            handle.flush()
            os.fsync(handle.fileno())
        os.replace(temporary, path)
    finally:
        try:
            temporary.unlink(missing_ok=True)
        except OSError:
            pass


def _verify_pack(destination: Path, snapshots: Mapping[Path, bytes]) -> None:
    actual = _pack_files(destination)
    if actual != _expected_pack_files():
        raise PreviewPackFailure("published preview pack file listing is not exact")
    if (destination / "pack.mcmeta").read_bytes() != PACK_META:
        raise PreviewPackFailure("published pack.mcmeta byte verification failed")
    for source, packed in PACK_ASSETS.items():
        if (destination / packed).read_bytes() != snapshots[source]:
            raise PreviewPackFailure(f"published asset byte verification failed: {packed.as_posix()}")


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
        rollback_error: Optional[BaseException] = None
        try:
            if published and _lexists(destination):
                failed = destination.parent / f".{destination.name}.failed.{uuid4().hex}"
                os.replace(destination, failed)
                try:
                    if moved_old:
                        os.replace(backup, destination)
                        moved_old = False
                finally:
                    shutil.rmtree(failed, ignore_errors=True)
            elif moved_old and _lexists(backup):
                os.replace(backup, destination)
                moved_old = False
        except BaseException as rollback_exc:
            rollback_error = rollback_exc
        detail = f"preview pack transaction failed: {exc}"
        if rollback_error is not None:
            detail += f"; rollback failed: {rollback_error}"
        raise PreviewPackFailure(detail) from exc
    finally:
        if _lexists(stage):
            shutil.rmtree(stage, ignore_errors=True)
        if _lexists(backup) and not moved_old:
            shutil.rmtree(backup, ignore_errors=True)
    if _lexists(backup):
        shutil.rmtree(backup)


def build_preview_pack(root: Path) -> Path:
    root = Path(os.path.abspath(str(root)))
    if not root.is_dir():
        raise PreviewPackFailure(f"project root is missing or not a directory: {root}")
    if _is_reparse_or_link(root):
        raise PreviewPackFailure(f"project root is a symlink/reparse point: {root}")

    # Validation intentionally happens in an independent interpreter before any
    # destination inspection, directory creation, staging or publication.
    _run_validator(root)
    snapshots = _verified_candidate(root)

    destination = root / PREVIEW_RELATIVE
    _reject_stale_destination(root, destination)
    _secure_parent(root, destination.parent)
    try:
        stage = Path(tempfile.mkdtemp(prefix=f".{destination.name}.stage.", dir=str(destination.parent)))
        _write_stage_file(stage / "pack.mcmeta", PACK_META)
        for source, packed in PACK_ASSETS.items():
            _write_stage_file(stage / packed, snapshots[source])
        _verify_pack(stage, snapshots)
        _publish(stage, destination, snapshots)
    except PreviewPackFailure:
        raise
    except BaseException as exc:
        if "stage" in locals() and _lexists(stage):
            shutil.rmtree(stage, ignore_errors=True)
        raise PreviewPackFailure(f"preview pack transaction failed: {exc}") from exc
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
