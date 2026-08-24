from __future__ import annotations

import io
import json
import math
import os
import stat
import tempfile
from contextlib import contextmanager
from pathlib import Path
from pathlib import PurePosixPath, PureWindowsPath
from types import MappingProxyType
from typing import Mapping

from PIL import Image, ImageDraw


MODEL_DIRECTORY = "src/main/resources/assets/usless_mobs/models/item"
TEXTURE_DIRECTORY = "src/main/resources/assets/usless_mobs/textures/item"
CONTACT_PATH = "Modelle/Exports/armor_crowns/review/crown_forms_contact.png"

CROWNS = {
    "void_reaper_king": ("void", "combat"),
    "god_king": ("celestial", "combat"),
    "living_king": ("living", "combat"),
    "true_crown": ("balance", "combat"),
    "royal_void_crown": ("void", "royal"),
    "royal_celestial_crown": ("celestial", "royal"),
    "royal_living_crown": ("living", "royal"),
    "royal_balance_crown": ("balance", "royal"),
}

FORMS = {
    "combat": {
        "peaks": (3.0, 4.0, 3.0),
        "peak_x": (3.1, 8.0, 12.9),
        "ring_height": 2.0,
        "minimum_elements": 7,
    },
    "royal": {
        "peaks": (3.5, 5.0, 6.5, 5.0, 3.5),
        "peak_x": (2.2, 5.1, 8.0, 10.9, 13.8),
        "ring_height": 2.5,
        "minimum_elements": 11,
    },
}

FAMILY_SPECS = {
    "void": {
        "palette": {
            "shadow": (12, 7, 22, 255),
            "base": (35, 23, 52, 255),
            "mid": (67, 40, 91, 255),
            "edge": (130, 74, 170, 255),
            "seam": (21, 12, 31, 255),
            "gem": (190, 42, 226, 255),
            "core": (255, 173, 255, 255),
            "accent": (111, 34, 146, 255),
        },
        "peak_bias": (0.15, 0.0, -0.15),
        "gem_width": 2.25,
    },
    "celestial": {
        "palette": {
            "shadow": (67, 56, 48, 255),
            "base": (174, 161, 137, 255),
            "mid": (226, 215, 184, 255),
            "edge": (255, 248, 218, 255),
            "seam": (105, 84, 58, 255),
            "gem": (44, 189, 219, 255),
            "core": (205, 255, 255, 255),
            "accent": (218, 167, 47, 255),
        },
        "peak_bias": (-0.1, 0.2, -0.1),
        "gem_width": 2.5,
    },
    "living": {
        "palette": {
            "shadow": (30, 23, 13, 255),
            "base": (68, 49, 25, 255),
            "mid": (91, 78, 34, 255),
            "edge": (137, 153, 55, 255),
            "seam": (40, 32, 17, 255),
            "gem": (99, 178, 44, 255),
            "core": (213, 255, 112, 255),
            "accent": (54, 111, 42, 255),
        },
        "peak_bias": (0.3, -0.15, 0.3),
        "gem_width": 2.1,
    },
    "balance": {
        "palette": {
            "shadow": (17, 13, 25, 255),
            "base": (62, 52, 67, 255),
            "mid": (54, 111, 42, 255),
            "edge": (218, 167, 47, 255),
            "seam": (38, 29, 43, 255),
            "gem": (162, 83, 213, 255),
            "core": (213, 255, 112, 255),
            "accent": (44, 189, 219, 255),
        },
        "peak_bias": (0.0, 0.35, 0.0),
        "gem_width": 2.7,
    },
}


def _deep_freeze(value):
    if isinstance(value, dict):
        return MappingProxyType({key: _deep_freeze(item) for key, item in value.items()})
    if isinstance(value, list):
        return tuple(_deep_freeze(item) for item in value)
    if isinstance(value, tuple):
        return tuple(_deep_freeze(item) for item in value)
    return value


FORMS = _deep_freeze(FORMS)
FAMILY_SPECS = _deep_freeze(FAMILY_SPECS)

EXPECTED_TARGETS = frozenset(
    path
    for item_id in CROWNS
    for path in (
        f"{MODEL_DIRECTORY}/{item_id}.json",
        f"{TEXTURE_DIRECTORY}/{item_id}.png",
    )
)
EXPECTED_DIRECTORIES = (MODEL_DIRECTORY, TEXTURE_DIRECTORY)
PUBLICATION_LOCK = ".crown-assets-publication.lock"


class InjectedFailure(RuntimeError):
    pass


class PublicationError(RuntimeError):
    def __init__(self, message: str, errors: list[BaseException]):
        self.errors = errors
        details = "; ".join(str(error) for error in errors)
        super().__init__(f"{message}: {details}")


class PublicationBusyError(RuntimeError):
    pass


def png_bytes(image: Image.Image) -> bytes:
    stream = io.BytesIO()
    image.save(stream, format="PNG", optimize=False, compress_level=9)
    return stream.getvalue()


def _round(number: float) -> float:
    return round(number, 4)


def _faces(uv: tuple[float, float, float, float]) -> dict:
    return {
        direction: {"uv": list(uv), "texture": "#main"}
        for direction in ("down", "up", "north", "south", "west", "east")
    }


def _cube(
    name: str,
    lower: tuple[float, float, float],
    upper: tuple[float, float, float],
    uv: tuple[float, float, float, float],
    *,
    rotation: tuple[str, float] | None = None,
) -> dict:
    element = {
        "name": name,
        "from": [_round(value) for value in lower],
        "to": [_round(value) for value in upper],
        "faces": _faces(uv),
    }
    if rotation is not None:
        axis, angle = rotation
        element["rotation"] = {
            "origin": [_round((lower[i] + upper[i]) / 2.0) for i in range(3)],
            "axis": axis,
            "angle": angle,
            "rescale": False,
        }
    return element


def _peak_biases(family: str, count: int) -> tuple[float, ...]:
    base = FAMILY_SPECS[family]["peak_bias"]
    if count == 3:
        return base
    return (0.0,) * count


def build_model(item_id: str, family: str, form: str) -> dict:
    form_spec = FORMS[form]
    ring_top = 8.0 + form_spec["ring_height"]
    elements = [
        _cube("ring_front", (3.0, 8.0, 1.0), (13.0, ring_top, 3.0), (1, 1, 7, 7)),
        _cube("ring_back", (3.0, 8.0, 13.0), (13.0, ring_top, 15.0), (1, 1, 7, 7)),
        _cube("ring_left", (1.0, 8.0, 3.0), (3.0, ring_top, 13.0), (1, 1, 7, 7)),
        _cube("ring_right", (13.0, 8.0, 3.0), (15.0, ring_top, 13.0), (1, 1, 7, 7)),
        _cube("ring_corner_front_left", (1.35, 8.0, 1.35), (3.65, ring_top, 3.65), (1, 1, 7, 7), rotation=("y", 45)),
        _cube("ring_corner_front_right", (12.35, 8.0, 1.35), (14.65, ring_top, 3.65), (1, 1, 7, 7), rotation=("y", 45)),
        _cube("ring_corner_back_left", (1.35, 8.0, 12.35), (3.65, ring_top, 14.65), (1, 1, 7, 7), rotation=("y", 45)),
        _cube("ring_corner_back_right", (12.35, 8.0, 12.35), (14.65, ring_top, 14.65), (1, 1, 7, 7), rotation=("y", 45)),
        _cube("trim_front_lower", (2.55, 8.25, 0.6), (13.45, 8.7, 1.25), (9, 9, 15, 15)),
        _cube("trim_front_upper", (2.55, ring_top - 0.45, 0.62), (13.45, ring_top + 0.1, 1.28), (9, 9, 15, 15)),
    ]

    peak_width = 1.65 if form == "combat" else 1.45
    biases = _peak_biases(family, len(form_spec["peaks"]))
    for index, (x, height, bias) in enumerate(
        zip(form_spec["peak_x"], form_spec["peaks"], biases), start=1
    ):
        top = ring_top + height + bias
        angle = -22.5 if index < (len(form_spec["peaks"]) + 1) / 2 else 22.5
        if index == (len(form_spec["peaks"]) + 1) // 2:
            angle = 22.5 if family in {"void", "balance"} else -22.5
        elements.append(
            _cube(
                f"peak_{index:02d}",
                (x - peak_width / 2, ring_top - 0.25, 1.25),
                (x + peak_width / 2, top, 2.75),
                (1, 9, 7, 15),
                rotation=("z", angle),
            )
        )
        elements.append(
            _cube(
                f"finial_{index:02d}",
                (x - peak_width * 0.27, top - 0.12, 1.4),
                (x + peak_width * 0.27, top + 0.62, 2.6),
                (9, 1, 13, 5),
                rotation=("z", angle),
            )
        )

        if index == 1 or index == len(form_spec["peaks"]) or form == "royal":
            jewel_x = max(3.15, min(12.85, x))
            elements.append(
                _cube(
                    f"jewel_setting_{index:02d}",
                    (jewel_x - 0.42, 8.35, 0.45),
                    (jewel_x + 0.42, 9.35, 1.3),
                    (9, 1, 13, 5),
                    rotation=("z", 45),
                )
            )

    gem_width = FAMILY_SPECS[family]["gem_width"] + (0.45 if form == "royal" else 0.0)
    gem_top = min(15.6, ring_top + (3.0 if form == "combat" else 4.4))
    elements.append(
        _cube(
            "gem_mount",
            (8.0 - gem_width / 2 - 0.45, ring_top - 0.6, 0.62),
            (8.0 + gem_width / 2 + 0.45, gem_top - 0.15, 1.4),
            (9, 9, 15, 15),
            rotation=("z", 45),
        )
    )
    elements.append(
        _cube(
            "gem_central",
            (8.0 - gem_width / 2, ring_top - 0.45, 0.35),
            (8.0 + gem_width / 2, gem_top, 1.55),
            (9, 1, 13, 5),
            rotation=("z", 45),
        )
    )

    ornament_height = ring_top + (1.6 if form == "combat" else 2.5)
    ornament_width = 2.5 if family in {"void", "living"} else 2.9
    for side, x0, x1, angle in (
        ("left", 1.45, 1.45 + ornament_width, -22.5),
        ("right", 14.55 - ornament_width, 14.55, 22.5),
    ):
        elements.append(
            _cube(
                f"ornament_{family}_{side}",
                (x0, ring_top - 0.35, 1.15),
                (x1, ornament_height, 2.85),
                (9, 9, 15, 15),
                rotation=("z", angle),
            )
        )
    if form == "royal":
        elements.extend(
            (
                _cube(
                    f"ornament_{family}_left_bridge",
                    (3.35, ring_top - 0.3, 1.4),
                    (6.5, ring_top + 1.0, 2.6),
                    (9, 9, 15, 15),
                    rotation=("z", -22.5),
                ),
                _cube(
                    f"ornament_{family}_right_bridge",
                    (9.5, ring_top - 0.3, 1.4),
                    (12.65, ring_top + 1.0, 2.6),
                    (9, 9, 15, 15),
                    rotation=("z", 22.5),
                ),
            )
        )

    signature_top = ring_top + (1.75 if form == "combat" else 2.35)
    if family == "void":
        signature = (
            ("left_horn", (1.15, ring_top - 0.2, 1.25), (2.25, signature_top, 2.55), -22.5),
            ("right_horn", (13.75, ring_top - 0.2, 1.25), (14.85, signature_top, 2.55), 22.5),
        )
    elif family == "celestial":
        signature = (
            ("left_ray", (3.55, ring_top - 0.2, 1.2), (4.45, signature_top + 0.45, 2.45), -22.5),
            ("right_ray", (11.55, ring_top - 0.2, 1.2), (12.45, signature_top + 0.45, 2.45), 22.5),
        )
    elif family == "living":
        signature = (
            ("left_branch", (2.1, ring_top - 0.2, 1.15), (6.15, ring_top + 0.9, 2.35), -22.5),
            ("right_branch", (9.85, ring_top - 0.2, 1.15), (13.9, ring_top + 0.9, 2.35), 22.5),
        )
    else:
        signature = (
            ("left_prism", (3.55, ring_top - 0.2, 1.15), (5.35, signature_top, 2.4), -22.5),
            ("right_prism", (10.65, ring_top - 0.2, 1.15), (12.45, signature_top, 2.4), 22.5),
        )
    for suffix, lower, upper, angle in signature:
        elements.append(
            _cube(
                f"ornament_{family}_{suffix}",
                lower,
                upper,
                (9, 9, 15, 15),
                rotation=("z", angle),
            )
        )

    return {
        "credit": "Deterministically generated by build_curio_crowns.py",
        "gui_light": "front",
        "textures": {
            "main": f"usless_mobs:item/{item_id}",
            "particle": f"usless_mobs:item/{item_id}",
        },
        "elements": elements,
        "display": {
            "gui": {"rotation": [28, 224, 0], "translation": [0, -1, 0], "scale": [0.82, 0.82, 0.82]},
            "ground": {"translation": [0, 2, 0], "scale": [0.38, 0.38, 0.38]},
            "fixed": {"rotation": [0, 180, 0], "scale": [0.72, 0.72, 0.72]},
            "head": {"translation": [0, 13, 0], "scale": [0.78, 0.78, 0.78]},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.48, 0.48, 0.48]},
            "thirdperson_lefthand": {"rotation": [75, -45, 0], "translation": [0, 2.5, 0], "scale": [0.48, 0.48, 0.48]},
            "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 1.5, 0], "scale": [0.58, 0.58, 0.58]},
            "firstperson_lefthand": {"rotation": [0, -45, 0], "translation": [0, 1.5, 0], "scale": [0.58, 0.58, 0.58]},
        },
    }


def build_texture(family: str, form: str) -> Image.Image:
    palette = FAMILY_SPECS[family]["palette"]
    image = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)

    draw.rectangle((4, 4, 27, 27), fill=palette["shadow"])
    draw.rectangle((5, 5, 26, 26), fill=palette["base"])
    draw.rectangle((6, 6, 25, 13), fill=palette["edge"])
    draw.rectangle((6, 14, 25, 25), fill=palette["mid"])
    for offset in (9, 17, 23):
        draw.line((offset, 7, offset, 25), fill=palette["seam"], width=1)
    for y in range(15, 25, 4):
        draw.line((6, y, 25, y), fill=palette["base"], width=1)
    draw.rectangle((21, 20, 24, 23), fill=palette["accent"])

    draw.rectangle((36, 4, 51, 19), fill=palette["seam"])
    draw.polygon(((44, 5), (50, 11), (44, 18), (37, 11)), fill=palette["gem"])
    draw.line((44, 6, 44, 16), fill=palette["edge"], width=1)
    core = palette["core"]
    for point in ((43, 10), (44, 9), (44, 10), (45, 10), (44, 11)):
        draw.point(point, fill=core)

    draw.rectangle((4, 36, 27, 59), fill=palette["shadow"])
    draw.rectangle((5, 37, 26, 58), fill=palette["base"])
    step = 4 if form == "combat" else 3
    for x in range(6, 26, step):
        draw.rectangle((x, 38, min(25, x + 1), 56), fill=palette["mid"])
    draw.line((5, 37, 26, 37), fill=palette["edge"], width=2)
    draw.line((5, 57, 26, 57), fill=palette["seam"], width=2)

    draw.rectangle((36, 36, 59, 59), fill=palette["seam"])
    draw.rectangle((37, 37, 58, 58), fill=palette["gem"])
    for index in range(38, 58, 4 if form == "combat" else 3):
        draw.line((index, 38, index, 57), fill=palette["edge"], width=1)
    draw.line((37, 57, 58, 57), fill=palette["shadow"], width=2)
    return image


def _json_bytes(document: dict) -> bytes:
    return (json.dumps(document, indent=2, ensure_ascii=False) + "\n").encode("utf-8")


def build_payloads() -> dict[str, bytes]:
    payloads: dict[str, bytes] = {}
    for item_id, (family, form) in CROWNS.items():
        payloads[f"{MODEL_DIRECTORY}/{item_id}.json"] = _json_bytes(
            build_model(item_id, family, form)
        )
        payloads[f"{TEXTURE_DIRECTORY}/{item_id}.png"] = png_bytes(
            build_texture(family, form)
        )
    return payloads


def _stage_payload(target: Path, payload: bytes, suffix: str) -> Path:
    target.parent.mkdir(parents=True, exist_ok=True)
    descriptor, name = tempfile.mkstemp(
        prefix=f".{target.name}.{os.getpid()}.", suffix=suffix, dir=target.parent
    )
    temporary = Path(name)
    try:
        with os.fdopen(descriptor, "wb") as stream:
            stream.write(payload)
            stream.flush()
            os.fsync(stream.fileno())
    except BaseException:
        try:
            temporary.unlink(missing_ok=True)
        finally:
            raise
    return temporary


def _failure_indexes(failure: str | None, phase: str) -> set[int]:
    indexes = set()
    for token in (failure or "").split(","):
        token = token.strip()
        if token.startswith(f"{phase}:"):
            indexes.add(int(token.split(":", 1)[1]))
    return indexes


def _is_reparse_point(path: Path) -> bool:
    try:
        metadata = path.lstat()
    except FileNotFoundError:
        return False
    attributes = getattr(metadata, "st_file_attributes", 0)
    reparse_flag = getattr(stat, "FILE_ATTRIBUTE_REPARSE_POINT", 0)
    return path.is_symlink() or bool(attributes & reparse_flag)


def _is_within(path: Path, root: Path) -> bool:
    try:
        path.relative_to(root)
    except ValueError:
        return False
    return True


def _validated_targets(repo_root: Path, payloads: Mapping[str, bytes]) -> tuple[Path, list[tuple[str, bytes]]]:
    if not repo_root.exists() or not repo_root.is_dir():
        raise ValueError("repository root must be an existing directory")
    if _is_reparse_point(repo_root):
        raise ValueError("repository root cannot be a symlink or reparse point")
    root = repo_root.resolve(strict=True)

    keys: list[str] = []
    folded: set[str] = set()
    for relative in payloads:
        if type(relative) is not str or not relative:
            raise ValueError("crown target keys must be non-empty strings")
        posix = PurePosixPath(relative)
        windows = PureWindowsPath(relative)
        if (
            "\\" in relative
            or posix.is_absolute()
            or windows.is_absolute()
            or windows.drive
            or any(part in {"", ".", ".."} for part in posix.parts)
            or posix.as_posix() != relative
        ):
            raise ValueError(f"unsafe crown target: {relative!r}")
        case_key = relative.casefold()
        if case_key in folded:
            raise ValueError(f"duplicate or case-colliding crown target: {relative!r}")
        folded.add(case_key)
        keys.append(relative)

    if len(keys) != 16 or frozenset(keys) != EXPECTED_TARGETS:
        raise ValueError("crown publication requires the canonical sixteen targets")

    allowed_roots = {
        directory: (root / PurePosixPath(directory)).resolve(strict=False)
        for directory in EXPECTED_DIRECTORIES
    }
    for relative in keys:
        relative_path = PurePosixPath(relative)
        target = root.joinpath(*relative_path.parts)
        expected_directory = next(
            directory
            for directory in EXPECTED_DIRECTORIES
            if relative == directory or relative.startswith(directory + "/")
        )
        allowed_root = allowed_roots[expected_directory]
        resolved_target = target.resolve(strict=False)
        if not _is_within(resolved_target, root) or not _is_within(resolved_target, allowed_root):
            raise ValueError(f"crown target escapes its asset directory: {relative!r}")
        cursor = root
        for part in relative_path.parts:
            cursor = cursor / part
            if _is_reparse_point(cursor):
                raise ValueError(f"crown target crosses a symlink or reparse point: {relative!r}")
    return root, sorted(payloads.items())


@contextmanager
def _publication_lock(repo_root: Path):
    lock_path = repo_root / PUBLICATION_LOCK
    flags = os.O_CREAT | os.O_EXCL | os.O_WRONLY
    if hasattr(os, "O_BINARY"):
        flags |= os.O_BINARY
    try:
        descriptor = os.open(lock_path, flags, 0o600)
    except FileExistsError as error:
        raise PublicationBusyError(
            f"another crown publication is active ({PUBLICATION_LOCK})"
        ) from error
    try:
        with os.fdopen(descriptor, "wb") as stream:
            stream.write(f"pid={os.getpid()}\n".encode("ascii"))
            stream.flush()
            os.fsync(stream.fileno())
        yield
    finally:
        lock_path.unlink(missing_ok=True)


def publish_payloads(
    repo_root: Path, payloads: Mapping[str, bytes], *, failure: str | None = None
) -> None:
    if not repo_root.exists() or not repo_root.is_dir() or _is_reparse_point(repo_root):
        raise ValueError("repository root must be an existing non-reparse directory")
    root = repo_root.resolve(strict=True)
    with _publication_lock(root):
        _publish_payloads_locked(root, payloads, failure=failure)


def _publish_payloads_locked(
    repo_root: Path, payloads: Mapping[str, bytes], *, failure: str | None = None
) -> None:
    repo_root, ordered = _validated_targets(repo_root, payloads)
    candidates: dict[str, Path] = {}
    backups: dict[str, Path | None] = {}
    published: list[str] = []
    cleanup_errors: list[BaseException] = []
    primary_error: BaseException | None = None
    rollback_errors: list[BaseException] = []
    protected_backups: set[Path] = set()

    try:
        for index, (relative, payload) in enumerate(ordered, start=1):
            if index in _failure_indexes(failure, "candidate"):
                raise InjectedFailure(f"candidate:{index}")
            candidates[relative] = _stage_payload(
                repo_root / relative, payload, ".candidate"
            )

        for index, (relative, _payload) in enumerate(ordered, start=1):
            target = repo_root / relative
            backup = None
            if target.exists():
                backup = _stage_payload(target, target.read_bytes(), ".backup")
            backups[relative] = backup
            os.replace(candidates[relative], target)
            published.append(relative)
            if index in _failure_indexes(failure, "publish"):
                raise InjectedFailure(f"publish:{index}")
        for index, (relative, expected) in enumerate(ordered, start=1):
            if index in _failure_indexes(failure, "verify"):
                raise InjectedFailure(f"verify:{index}")
            actual = (repo_root / relative).read_bytes()
            if actual != expected:
                raise OSError(f"published payload verification failed: {relative}")
    except BaseException as error:
        primary_error = error
        for relative in reversed(published):
            target = repo_root / relative
            backup = backups.get(relative)
            try:
                if backup is None:
                    target.unlink(missing_ok=True)
                elif backup.exists():
                    os.replace(backup, target)
            except BaseException as rollback_error:
                rollback_errors.append(rollback_error)
                if backup is not None:
                    protected_backups.add(backup)
    finally:
        for path in candidates.values():
            if path.exists():
                try:
                    path.unlink()
                except BaseException as error:
                    cleanup_errors.append(error)
        for path in backups.values():
            if path is not None and path.exists() and path not in protected_backups:
                try:
                    path.unlink()
                except BaseException as error:
                    cleanup_errors.append(error)

    if primary_error is None:
        for index, (_relative, _payload) in enumerate(ordered, start=1):
            if index in _failure_indexes(failure, "cleanup"):
                cleanup_errors.append(InjectedFailure(f"cleanup:{index}"))
    if primary_error is not None:
        if rollback_errors or cleanup_errors:
            raise PublicationError(
                "publication, rollback, or cleanup failed",
                [primary_error, *rollback_errors, *cleanup_errors],
            )
        raise primary_error
    if cleanup_errors:
        raise PublicationError("publication cleanup failed", cleanup_errors)


def _rotate(point: tuple[float, float, float], rotation: dict | None):
    if rotation is None:
        return point
    origin = rotation["origin"]
    axis = rotation["axis"]
    radians = math.radians(rotation["angle"])
    values = [point[i] - origin[i] for i in range(3)]
    first, second = {"x": (1, 2), "y": (0, 2), "z": (0, 1)}[axis]
    a, b = values[first], values[second]
    values[first] = a * math.cos(radians) - b * math.sin(radians)
    values[second] = a * math.sin(radians) + b * math.cos(radians)
    return tuple(values[i] + origin[i] for i in range(3))


def _project(point: tuple[float, float, float]):
    x, y, z = point[0] - 8.0, point[1] - 8.0, point[2] - 8.0
    yaw = math.radians(24)
    pitch = math.radians(18)
    horizontal = x * math.cos(yaw) - z * math.sin(yaw)
    depth0 = x * math.sin(yaw) + z * math.cos(yaw)
    vertical = y * math.cos(pitch) - depth0 * math.sin(pitch)
    depth = y * math.sin(pitch) + depth0 * math.cos(pitch)
    return horizontal, -vertical, depth


def render_model(model: dict, family: str, form: str, size: int = 330) -> Image.Image:
    image = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    surfaces = []
    face_indices = (
        (0, 1, 3, 2), (4, 6, 7, 5), (0, 4, 5, 1),
        (2, 3, 7, 6), (0, 2, 6, 4), (1, 5, 7, 3),
    )
    palette = FAMILY_SPECS[family]["palette"]
    for element in model["elements"]:
        x0, y0, z0 = element["from"]
        x1, y1, z1 = element["to"]
        corners = [
            (x, y, z)
            for z in (z0, z1)
            for y in (y0, y1)
            for x in (x0, x1)
        ]
        corners = [_rotate(corner, element.get("rotation")) for corner in corners]
        projected = [_project(corner) for corner in corners]
        if element["name"].startswith("gem") or element["name"].startswith("jewel"):
            base = palette["gem"]
        elif element["name"].startswith("finial"):
            base = palette["core"]
        elif element["name"].startswith("ornament"):
            base = palette["accent"]
        elif element["name"].startswith("trim"):
            base = palette["edge"]
        elif element["name"].startswith("peak"):
            base = palette["mid"]
        else:
            base = palette["base"]
        for face_number, indices in enumerate(face_indices):
            polygon = [projected[index] for index in indices]
            area = sum(
                polygon[index][0] * polygon[(index + 1) % 4][1]
                - polygon[(index + 1) % 4][0] * polygon[index][1]
                for index in range(4)
            )
            if area >= 0:
                continue
            shade = (0.7, 1.0, 0.84, 0.64, 0.76, 0.92)[face_number]
            colour = tuple(min(255, round(channel * shade)) for channel in base[:3]) + (255,)
            surfaces.append((sum(point[2] for point in polygon) / 4, polygon, colour))

    scale = 14.0 if form == "combat" else 12.5
    offset_x = size / 2
    offset_y = size * (0.63 if form == "combat" else 0.66)
    for _depth, polygon, colour in sorted(
        surfaces, key=lambda value: value[0], reverse=True
    ):
        points = [(offset_x + point[0] * scale, offset_y + point[1] * scale) for point in polygon]
        draw.polygon(points, fill=colour, outline=palette["seam"])
    return image


def build_contact_sheet() -> Image.Image:
    sheet = Image.new("RGBA", (1600, 800), (12, 11, 17, 255))
    draw = ImageDraw.Draw(sheet)
    for index, (item_id, (family, form)) in enumerate(CROWNS.items()):
        column, row = index % 4, index // 4
        left, top = column * 400, row * 400
        draw.rounded_rectangle(
            (left + 12, top + 12, left + 388, top + 388),
            radius=12,
            fill=(22, 20, 30, 255),
            outline=(72, 64, 86, 255),
            width=2,
        )
        preview = render_model(build_model(item_id, family, form), family, form)
        sheet.alpha_composite(preview, (left + 35, top + 38))
        draw.text((left + 24, top + 22), f"{family.upper()} / {form.upper()}", fill=(239, 232, 246, 255))
        draw.text((left + 24, top + 366), item_id, fill=(163, 151, 178, 255))
    return sheet


def write_assets(repo_root: Path) -> None:
    payloads = build_payloads()
    publish_payloads(repo_root, payloads)
    contact = repo_root / CONTACT_PATH
    candidate = _stage_payload(contact, png_bytes(build_contact_sheet()), ".candidate")
    try:
        os.replace(candidate, contact)
    finally:
        candidate.unlink(missing_ok=True)


if __name__ == "__main__":
    write_assets(Path(__file__).resolve().parents[2])
