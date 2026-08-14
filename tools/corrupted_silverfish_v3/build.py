"""Deterministically generate Corrupted Silverfish v3 geometry artifacts."""

from __future__ import annotations

import argparse
import base64
import json
import math
import os
from pathlib import Path
import tempfile
from typing import Dict, Iterable, Mapping, Sequence, Tuple
from uuid import UUID, uuid5

from .spec import ANIMATION_SPECS, BONES, CUBES, Cube, cube_pivot


PROJECT_ROOT = Path(__file__).resolve().parents[2]
EXPORT_ROOT = PROJECT_ROOT / "Modelle" / "Exports" / "corrupted_silverfish_v3"
GEOMETRY_PATH = EXPORT_ROOT / "geo" / "corrupted_silverfish.geo.json"
ANIMATION_PATH = EXPORT_ROOT / "animations" / "corrupted_silverfish.animation.json"
BBMODEL_PATH = PROJECT_ROOT / "Modelle" / "Editierbar" / "Corrupted Silverfish v3.bbmodel"

TEXTURE_SIZE = 256
GUTTER = 2
FACE_ORDER = ("north", "east", "south", "west", "up", "down")
UUID_NAMESPACE = UUID("b2e12292-b0e2-5f48-91a5-f9385a89e5a3")

FaceKey = Tuple[str, str]
UvRect = Tuple[int, int, int, int]
Island = Tuple[str, str, int, int]


def stable_uuid(kind: str, name: str) -> str:
    """Return a stable UUID for a named generated object."""

    return str(uuid5(UUID_NAMESPACE, f"{kind}:{name}"))


def _face_dimensions(cube: Cube, face: str) -> Tuple[int, int]:
    width, height, depth = cube.size
    dimensions = {
        "north": (width, height),
        "east": (depth, height),
        "south": (width, height),
        "west": (depth, height),
        "up": (width, depth),
        "down": (width, depth),
    }[face]
    if not all(math.isfinite(value) and value > 0 for value in dimensions):
        raise ValueError(f"Face {cube.name}/{face} has invalid dimensions {dimensions}")
    return tuple(max(1, math.ceil(abs(value) * 2)) for value in dimensions)  # type: ignore[return-value]


def _pack_uvs(cubes: Iterable[Cube] = CUBES) -> Dict[FaceKey, UvRect]:
    """Pack every face into deterministic shelves with a two-pixel gutter."""

    faces = []
    for cube in cubes:
        for face in FACE_ORDER:
            width, height = _face_dimensions(cube, face)
            faces.append((cube.name, face, width, height))
    return _pack_islands(faces)


def _pack_islands(islands: Iterable[Island]) -> Dict[FaceKey, UvRect]:
    """Pack already-sized face islands for deterministic boundary testing."""

    faces = list(islands)
    faces.sort(key=lambda item: (-item[3], item[0], item[1]))

    packed: Dict[FaceKey, UvRect] = {}
    x = 0
    y = 0
    shelf_height = 0
    for cube_name, face, width, height in faces:
        if not all(
            isinstance(value, int) and not isinstance(value, bool) and value > 0
            for value in (width, height)
        ):
            raise ValueError(
                f"Face {cube_name}/{face} has invalid packed dimensions {(width, height)}"
            )
        if width > TEXTURE_SIZE or height > TEXTURE_SIZE:
            raise ValueError(f"Face {cube_name}/{face} is larger than {TEXTURE_SIZE}x{TEXTURE_SIZE}")
        if x and x + width > TEXTURE_SIZE:
            y += shelf_height + GUTTER
            x = 0
            shelf_height = 0
        if y + height > TEXTURE_SIZE:
            raise ValueError(f"Face {cube_name}/{face} overflows the {TEXTURE_SIZE}x{TEXTURE_SIZE} atlas")
        packed[(cube_name, face)] = (x, y, width, height)
        x += width + GUTTER
        shelf_height = max(shelf_height, height)
    return packed


def _geometry_cube(cube: Cube, uvs: Mapping[FaceKey, UvRect]) -> dict:
    faces = {}
    for face in FACE_ORDER:
        u, v, width, height = uvs[(cube.name, face)]
        faces[face] = {"uv": [u, v], "uv_size": [width, height]}
    document = {
        "name": cube.name,
        "origin": list(cube.origin),
        "size": list(cube.size),
        "uv": faces,
    }
    if cube.rotation != (0.0, 0.0, 0.0):
        document["rotation"] = list(cube.rotation)
        document["pivot"] = list(cube_pivot(cube))
    return document


def geometry_document() -> dict:
    """Build the GeckoLib geometry document without writing it."""

    uvs = _pack_uvs()
    cubes_by_bone = {bone.name: [] for bone in BONES}
    for cube in CUBES:
        cubes_by_bone[cube.bone].append(_geometry_cube(cube, uvs))

    bones = []
    for bone in BONES:
        item = {"name": bone.name, "pivot": list(bone.pivot)}
        if bone.parent is not None:
            item["parent"] = bone.parent
        if cubes_by_bone[bone.name]:
            item["cubes"] = cubes_by_bone[bone.name]
        bones.append(item)

    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [
            {
                "description": {
                    "identifier": "geometry.corrupted_silverfish",
                    "texture_width": TEXTURE_SIZE,
                    "texture_height": TEXTURE_SIZE,
                    "visible_bounds_width": 2.6,
                    "visible_bounds_height": 1.7,
                    "visible_bounds_offset": [0, 0.55, 0],
                },
                "bones": bones,
            }
        ],
    }


def animation_document() -> dict:
    """Build GeckoLib animations from the canonical animation specification."""

    return {
        "format_version": "1.8.0",
        "animations": _plain_animation_value(ANIMATION_SPECS),
    }


def _plain_animation_value(value):
    """Thaw immutable canonical animation values into JSON-compatible values."""

    if isinstance(value, Mapping):
        return {key: _plain_animation_value(item) for key, item in value.items()}
    if isinstance(value, tuple):
        return [_plain_animation_value(item) for item in value]
    return value


def _bbmodel_animations() -> list:
    animations = []
    for name, animation in ANIMATION_SPECS.items():
        animators = {}
        for bone_name, channels in animation["bones"].items():
            keyframes = []
            for channel_name, channel_keyframes in channels.items():
                for encoded_time, keyframe in channel_keyframes.items():
                    vector = keyframe["post"]
                    keyframes.append(
                        {
                            "channel": channel_name,
                            "data_points": [
                                {
                                    "x": f"{vector[0]:g}",
                                    "y": f"{vector[1]:g}",
                                    "z": f"{vector[2]:g}",
                                }
                            ],
                            "uuid": stable_uuid(
                                "keyframe",
                                f"{name}:{bone_name}:{channel_name}:{encoded_time}",
                            ),
                            "time": float(encoded_time),
                            "color": -1,
                            "interpolation": keyframe["lerp_mode"],
                        }
                    )
            animators[stable_uuid("group", bone_name)] = {
                "name": bone_name,
                "type": "bone",
                "rotation_global": False,
                "quaternion_interpolation": False,
                "keyframes": keyframes,
            }
        animations.append(
            {
                "uuid": stable_uuid("animation", name),
                "name": name,
                "path": "corrupted_silverfish.animation.json",
                "loop": "loop" if animation["loop"] else "once",
                "override": False,
                "snapping": 20,
                "length": animation["animation_length"],
                "selected_item": None,
                "anim_time_update": "",
                "blend_weight": "",
                "start_delay": "",
                "loop_delay": "",
                "animators": animators,
            }
        )
    return animations


def _bbmodel_element(
    cube: Cube,
    uvs: Mapping[FaceKey, UvRect],
    texture_id: int | None = None,
) -> dict:
    faces = {}
    for face in FACE_ORDER:
        u, v, width, height = uvs[(cube.name, face)]
        faces[face] = {
            "uv": [u, v, u + width, v + height],
            "texture": texture_id,
        }
    return {
        "name": cube.name,
        "box_uv": False,
        "from": list(cube.origin),
        "to": [cube.origin[axis] + cube.size[axis] for axis in range(3)],
        "origin": list(cube_pivot(cube)),
        "rotation": list(cube.rotation),
        "faces": faces,
        "type": "cube",
        "uuid": stable_uuid("element", cube.name),
        "bone": cube.bone,
    }


def _bbmodel_group(name: str, pivot: Sequence[float]) -> dict:
    return {
        "name": name,
        "uuid": stable_uuid("group", name),
        "export": True,
        "locked": False,
        "origin": list(pivot),
        "rotation": [0, 0, 0],
        "color": 0,
        "children": [],
        "reset": False,
        "shade": True,
        "mirror_uv": False,
        "visibility": True,
        "autouv": 0,
        "isOpen": True,
    }


def _outliner() -> list:
    child_bones = {bone.name: [] for bone in BONES}
    for bone in BONES:
        if bone.parent is not None:
            child_bones[bone.parent].append(bone.name)
    element_ids = {bone.name: [] for bone in BONES}
    for cube in CUBES:
        element_ids[cube.bone].append(stable_uuid("element", cube.name))

    def node(name: str) -> dict:
        children = list(element_ids[name])
        children.extend(node(child_name) for child_name in child_bones[name])
        return {
            "uuid": stable_uuid("group", name),
            "isOpen": True,
            "children": children,
        }

    return [node("root")]


def bbmodel_document(texture_source: str | None = None) -> dict:
    """Build an editable structural GeckoLib Blockbench project."""

    uvs = _pack_uvs()
    cubes_in_hierarchy_order = [
        cube for bone in BONES for cube in CUBES if cube.bone == bone.name
    ]
    return {
        "meta": {
            "format_version": "5.0",
            "model_format": "geckolib_model",
            "box_uv": False,
        },
        "name": "Corrupted Silverfish v3",
        "model_identifier": "geometry.corrupted_silverfish",
        "visible_box": [2.6, 1.7, 0],
        "variable_placeholders": "",
        "timeline_setups": [],
        "unhandled_root_fields": {},
        "geckolib_modid": "usless_mobs",
        "geckolib_filepath_cache": "",
        "resolution": {"width": TEXTURE_SIZE, "height": TEXTURE_SIZE},
        "elements": [
            _bbmodel_element(cube, uvs, 0 if texture_source is not None else None)
            for cube in cubes_in_hierarchy_order
        ],
        "groups": [_bbmodel_group(bone.name, bone.pivot) for bone in BONES],
        "outliner": _outliner(),
        "textures": (
            []
            if texture_source is None
            else [
                {
                    "path": "",
                    "name": "corrupted_silverfish.png",
                    "folder": "entity",
                    "namespace": "usless_mobs",
                    "id": "0",
                    "particle": False,
                    "render_mode": "default",
                    "visible": True,
                    "mode": "bitmap",
                    "saved": True,
                    "uuid": stable_uuid("texture", "corrupted_silverfish.png"),
                    "source": texture_source,
                }
            ]
        ),
        "animations": _bbmodel_animations(),
        "geckolib_model_type": "Entity",
    }


def _atomic_json_write(path: Path, document: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    file_descriptor, temporary_name = tempfile.mkstemp(
        prefix=f".{path.name}.",
        suffix=".tmp",
        dir=str(path.parent),
    )
    temporary = Path(temporary_name)
    try:
        handle = os.fdopen(
            file_descriptor,
            "w",
            encoding="utf-8",
            newline="\n",
        )
        file_descriptor = -1
        with handle:
            handle.write(json.dumps(document, ensure_ascii=False, indent=2) + "\n")
        os.replace(temporary, path)
    finally:
        if file_descriptor != -1:
            os.close(file_descriptor)
        temporary.unlink(missing_ok=True)


def _json_bytes(document: dict) -> bytes:
    return (json.dumps(document, ensure_ascii=False, indent=2) + "\n").encode("utf-8")


def _stage_bytes(path: Path, contents: bytes, role: str) -> Path:
    path.parent.mkdir(parents=True, exist_ok=True)
    file_descriptor, temporary_name = tempfile.mkstemp(
        prefix=f".{path.name}.{role}.", suffix=".tmp", dir=str(path.parent)
    )
    temporary = Path(temporary_name)
    try:
        with os.fdopen(file_descriptor, "wb") as handle:
            file_descriptor = -1
            handle.write(contents)
        return temporary
    except BaseException:
        if file_descriptor != -1:
            os.close(file_descriptor)
        temporary.unlink(missing_ok=True)
        raise


def _publish_transaction(payloads: Sequence[tuple[Path, bytes]]) -> None:
    """Publish related artifacts together, restoring every old byte on failure."""

    targets = [target for target, _contents in payloads]
    if len(targets) != len(set(targets)):
        raise ValueError("transaction targets must be unique")
    candidates: dict[Path, Path] = {}
    backups: dict[Path, Path | None] = {}
    published: list[Path] = []
    retained_backups: set[Path] = set()
    try:
        for target, contents in payloads:
            candidates[target] = _stage_bytes(target, contents, "candidate")
        for target in targets:
            backups[target] = (
                _stage_bytes(target, target.read_bytes(), "backup")
                if target.is_file()
                else None
            )
        for target in targets:
            os.replace(candidates[target], target)
            published.append(target)
    except BaseException as publish_error:
        rollback_failures = []
        for target in reversed(published):
            backup = backups[target]
            try:
                if backup is None:
                    target.unlink(missing_ok=True)
                else:
                    os.replace(backup, target)
            except BaseException as rollback_error:
                rollback_failures.append((target, backup, rollback_error))
                if backup is not None:
                    retained_backups.add(backup)
            else:
                backups.pop(target)
        if rollback_failures:
            details = "; ".join(
                f"target={target}, retained_backup={backup or '<none>'}, "
                f"rollback_error={type(error).__name__}: {error}"
                for target, backup, error in rollback_failures
            )
            error_chain = publish_error
            for _target, _backup, error in rollback_failures:
                error.__cause__ = error_chain
                error_chain = error
            raise RuntimeError(
                "animation transaction rollback failed after "
                f"publish_error={type(publish_error).__name__}: {publish_error}; {details}"
            ) from error_chain
        raise
    finally:
        for temporary in (*candidates.values(), *backups.values()):
            if temporary is not None and temporary not in retained_backups:
                temporary.unlink(missing_ok=True)


def _embedded_texture_source() -> str:
    """Reuse the editable project's texture bytes, falling back to the atlas."""

    if BBMODEL_PATH.is_file():
        document = json.loads(BBMODEL_PATH.read_text(encoding="utf-8"))
        textures = document.get("textures", [])
        if len(textures) == 1 and isinstance(textures[0].get("source"), str):
            return textures[0]["source"]
    main_texture = EXPORT_ROOT / "textures" / "entity" / "corrupted_silverfish.png"
    return "data:image/png;base64," + base64.b64encode(main_texture.read_bytes()).decode("ascii")


def build_geometry() -> Path:
    """Write only the geometry artifact, leaving the editable project untouched."""

    _atomic_json_write(GEOMETRY_PATH, geometry_document())
    return GEOMETRY_PATH


def write_textured_bbmodel(texture_source: str) -> Path:
    """Write the editable project with one caller-supplied embedded texture."""

    _atomic_json_write(BBMODEL_PATH, bbmodel_document(texture_source))
    return BBMODEL_PATH


def build_all() -> tuple[Path, Path, Path]:
    """Build geometry and publish matching animation outputs transactionally."""

    texture_source = _embedded_texture_source()
    _publish_transaction(
        (
            (GEOMETRY_PATH, _json_bytes(geometry_document())),
            (ANIMATION_PATH, _json_bytes(animation_document())),
            (BBMODEL_PATH, _json_bytes(bbmodel_document(texture_source))),
        )
    )
    return GEOMETRY_PATH, ANIMATION_PATH, BBMODEL_PATH


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--geometry-only",
        action="store_true",
        help="write only the geometry JSON",
    )
    arguments = parser.parse_args(argv)
    if arguments.geometry_only:
        geometry_path = build_geometry()
        print(f"Geometry: {geometry_path} (32 bones, 112 cubes)")
        return 0
    geometry_path, animation_path, bbmodel_path = build_all()
    print(f"Geometry: {geometry_path} (32 bones, 112 cubes)")
    print(f"Animations: {animation_path} (5 animations)")
    print(f"Blockbench: {bbmodel_path} (one embedded texture, 5 animations)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
