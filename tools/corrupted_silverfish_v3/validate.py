"""Independently validate the committed Corrupted Silverfish v3 candidate.

This module deliberately treats the generated artifacts as untrusted input.  It
does not use any model generator or in-memory specification as an oracle.

Manifest writes are serialized by a cooperative, process-level sidecar lock.
That lock protects validator instances using this module; unrelated processes
that write the manifest without taking the lock remain outside its guarantees.
"""

from __future__ import annotations

import argparse
import base64
import binascii
from contextlib import contextmanager
import hashlib
from io import BytesIO
import json
import math
import os
from pathlib import Path
import struct
import sys
import tempfile
import warnings
from typing import Any, Dict, List, Mapping, Optional, Sequence, Tuple
from uuid import UUID

from PIL import Image, UnidentifiedImageError


TEXTURE_SIZE = 256
MANIFEST_LOCK_NAME = ".candidate-sha256.lock"
FACE_NAMES = ("north", "east", "south", "west", "up", "down")
CHANNEL_NAMES = {"rotation", "position", "scale"}
EXPECTED_ANIMATIONS = {
    "animation.corrupted_silverfish.idle": (1.6, True),
    "animation.corrupted_silverfish.walk": (0.8, True),
    "animation.corrupted_silverfish.attack": (0.45, False),
    "animation.corrupted_silverfish.hurt": (0.3, False),
    "animation.corrupted_silverfish.death": (1.1, False),
}
GLOW_COLORS = {(86, 190, 255, 255), (234, 55, 112, 255)}
MAIN_PALETTE = {
    (22, 18, 28, 255), (38, 34, 46, 255), (45, 10, 43, 255),
    (76, 82, 92, 255), (82, 37, 104, 255), (86, 190, 255, 255),
    (91, 15, 58, 255), (126, 136, 147, 255), (181, 31, 82, 255),
    (188, 198, 207, 255), (224, 230, 235, 255), (234, 55, 112, 255),
}
MODEL_BOUNDS = ((-7.0, 6.7), (0.0, 11.9), (-15.4, 21.0))
EXPECTED_PARENTS = dict((line.split(":", 1)[0], line.split(":", 1)[1] or None) for line in """root:
body:root
head:body
thorax:body
shell_front:thorax
shell_mid:shell_front
shell_rear:shell_mid
abdomen:shell_rear
tail_base:abdomen
tail_tip:tail_base
leg_left_front_upper:body
leg_left_front_lower:leg_left_front_upper
leg_left_mid_upper:body
leg_left_mid_lower:leg_left_mid_upper
leg_left_rear_upper:body
leg_left_rear_lower:leg_left_rear_upper
leg_right_front_upper:body
leg_right_front_lower:leg_right_front_upper
leg_right_mid_upper:body
leg_right_mid_lower:leg_right_mid_upper
leg_right_rear_upper:body
leg_right_rear_lower:leg_right_rear_upper
mandible_left:head
mandible_right:head
mouth_core:head
crystal_cluster_1:thorax
crystal_cluster_2:shell_front
crystal_cluster_3:shell_mid
crystal_cluster_4:shell_rear
crystal_cluster_5:shell_mid
crystal_cluster_6:abdomen
crystal_cluster_7:head""".splitlines())
EXPECTED_CUBE_NAMES = frozenset("""head_core head_brow head_cheek_core_left head_cheek_core_right eye_left eye_right forehead_left forehead_right forehead_crown forehead_nose cheek_plate_1_left cheek_plate_1_right cheek_plate_2_left cheek_plate_2_right cheek_plate_3_left cheek_plate_3_right thorax_core underside_segment_1 side_plate_1_left side_plate_1_right dorsal_thorax_1 dorsal_thorax_2 dorsal_thorax_3 front_core underside_segment_2 side_plate_2_left side_plate_2_right side_plate_3_left side_plate_3_right dorsal_front_1 dorsal_front_2 dorsal_front_3 middle_core underside_segment_3 side_plate_4_left side_plate_4_right dorsal_mid_1 dorsal_mid_2 dorsal_mid_3 rear_core underside_segment_4 side_plate_5_left side_plate_5_right dorsal_rear_1 dorsal_rear_2 dorsal_rear_3 abdomen_core underside_segment_5 dorsal_abdomen_1 dorsal_abdomen_2 dorsal_abdomen_3 tail_root_core underside_segment_6 tail_taper_1 tail_taper_2 tail_taper_6 dorsal_tail_1 dorsal_tail_2 dorsal_tail_3 tail_taper_3 tail_taper_4 tail_taper_5 tail_taper_7 tail_taper_8 tail_taper_9 leg_left_front_upper leg_left_front_lower foot_left_front toe_left_front leg_left_mid_upper leg_left_mid_lower foot_left_mid toe_left_mid leg_left_rear_upper leg_left_rear_lower foot_left_rear toe_left_rear leg_right_front_upper leg_right_front_lower foot_right_front toe_right_front leg_right_mid_upper leg_right_mid_lower foot_right_mid toe_right_mid leg_right_rear_upper leg_right_rear_lower foot_right_rear toe_right_rear mandible_left_cube mandible_right_cube mouth_core_cube mouth_sensor_cube crystal_1_1 crystal_1_2 crystal_1_3 crystal_2_1 crystal_2_2 crystal_2_3 crystal_3_1 crystal_3_2 crystal_3_3 crystal_3_4 crystal_4_1 crystal_4_2 crystal_4_3 crystal_5_1 crystal_5_2 crystal_6_1 crystal_6_2 crystal_7_1 crystal_7_2""".split())
EXPECTED_ANIMATION_CHANNELS = {
    "animation.corrupted_silverfish.idle": {
        **{"body": {"position"}},
        **{name: {"rotation"} for name in ("head", "thorax", "shell_front", "shell_mid", "shell_rear", "abdomen", "tail_base", "tail_tip")},
        **{f"crystal_cluster_{number}": {"scale"} for number in range(1, 8)},
    },
    "animation.corrupted_silverfish.walk": {
        **{name: {"rotation"} for name in ("head", "thorax", "shell_front", "shell_mid", "shell_rear", "abdomen", "tail_base", "tail_tip")},
        **{f"leg_{side}_{position}_{part}": {"rotation"} for side in ("left", "right") for position in ("front", "mid", "rear") for part in ("upper", "lower")},
    },
    "animation.corrupted_silverfish.attack": {"head": {"position"}, "mandible_left": {"rotation"}, "mandible_right": {"rotation"}, "leg_left_front_upper": {"rotation"}, "leg_right_front_upper": {"rotation"}},
    "animation.corrupted_silverfish.hurt": {"body": {"rotation"}, **{name: {"scale"} for name in ("shell_front", "shell_mid", "shell_rear")}, **{f"crystal_cluster_{number}": {"scale"} for number in range(1, 8)}},
    "animation.corrupted_silverfish.death": {"body": {"position"}, "tail_base": {"rotation"}, "tail_tip": {"rotation"}, **{f"leg_{side}_{position}_upper": {"rotation"} for side in ("left", "right") for position in ("front", "mid", "rear")}, **{f"crystal_cluster_{number}": {"scale"} for number in range(1, 8)}},
}
FILE_LIMITS = (4 * 1024 * 1024, 2 * 1024 * 1024, 1024 * 1024, 1024 * 1024, 1024 * 1024)
GEOMETRY_CONTRACT_SHA256 = "877ED79F9B260D2038F738E37565CA9F7DEA574AF2779E83B09840759F61D2BD"
ANIMATION_CONTRACT_SHA256 = "B08162F066BC885A6E363201B7A4DBC03059D72E3C580CD880442F1A91629DA6"
MAIN_RGBA_SHA256 = "2F60AC9D874A4350423EF681FD48CB22B082D48AC6AE0AED5D8D3BE1E51D9E5C"
GLOW_RGBA_SHA256 = "A270884344131F272B02C9BF0E084C2F0BB2039BAFCD5454FBADCD274171F065"
BBMODEL_CONTRACT_SHA256 = "4113ACC380B3470AFB12EA0DCB60EA42E4E08BEB7F680A75329AF9908C4C6AA3"
RELATIVE_PATHS = (
    Path("Modelle/Editierbar/Corrupted Silverfish v3.bbmodel"),
    Path("Modelle/Exports/corrupted_silverfish_v3/geo/corrupted_silverfish.geo.json"),
    Path("Modelle/Exports/corrupted_silverfish_v3/textures/entity/corrupted_silverfish.png"),
    Path("Modelle/Exports/corrupted_silverfish_v3/textures/entity/corrupted_silverfish_glowmask.png"),
    Path("Modelle/Exports/corrupted_silverfish_v3/animations/corrupted_silverfish.animation.json"),
)
MANIFEST_RELATIVE = Path("Modelle/Exports/corrupted_silverfish_v3/review/candidate-sha256.json")


class ValidationFailure(Exception):
    """A candidate contract violation suitable for a one-line CLI message."""


def fail(message: str) -> None:
    raise ValidationFailure(message)


def _is_number(value: Any) -> bool:
    return isinstance(value, (int, float)) and not isinstance(value, bool) and math.isfinite(value)


def _number(value: Any, context: str, positive: bool = False) -> float:
    if not _is_number(value):
        fail(f"{context} must be a finite number")
    if positive and value <= 0:
        fail(f"{context} has nonpositive dimension {value}")
    return float(value)


def _numeric_string(value: Any, context: str) -> float:
    """Parse Blockbench's intentionally string-encoded data-point numbers."""
    if not isinstance(value, str):
        fail(f"{context} must be a numeric string")
    try:
        number = float(value)
    except ValueError:
        fail(f"{context} must be a numeric string")
    if not math.isfinite(number):
        fail(f"{context} must be finite")
    return number


def _vec(value: Any, length: int, context: str, positive: bool = False) -> List[float]:
    if not isinstance(value, list) or len(value) != length:
        fail(f"{context} must be a Vec{length}")
    return [_number(item, f"{context}[{index}]", positive) for index, item in enumerate(value)]


def _reject_duplicate_pairs(pairs: List[Tuple[str, Any]]) -> Dict[str, Any]:
    result: Dict[str, Any] = {}
    for key, value in pairs:
        if key in result:
            fail(f"duplicate key {key!r}")
        result[key] = value
    return result


def _reject_constant(value: str) -> None:
    fail(f"non-finite JSON constant {value}")


def _load_json(raw: bytes, path: Path, label: str) -> Mapping[str, Any]:
    try:
        value = json.loads(raw.decode("utf-8"), object_pairs_hook=_reject_duplicate_pairs, parse_constant=_reject_constant)
    except RecursionError as exc:
        fail(f"{label} malformed or too deeply nested JSON ({path}): {exc}")
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        fail(f"{label} JSON is malformed ({path}): {exc}")
    if not isinstance(value, dict):
        fail(f"{label} JSON root must be an object")
    return value


def _exact_keys(value: Mapping[str, Any], required: set, optional: set, context: str) -> None:
    keys = set(value)
    missing = required - keys
    unexpected = keys - required - optional
    if missing:
        fail(f"{context} missing required keys {sorted(missing)}")
    if unexpected:
        fail(f"{context} has unexpected keys {sorted(unexpected)}")


def _read_snapshot(path: Path, limit: int, label: str) -> bytes:
    try:
        size = path.stat().st_size
    except OSError as exc:
        fail(f"{label} stat failed ({path}): {exc}")
    if size > limit:
        fail(f"{label} exceeds size limit {limit} bytes: {size}")
    try:
        raw = path.read_bytes()
    except OSError as exc:
        fail(f"{label} read failed ({path}): {exc}")
    if len(raw) > limit:
        fail(f"{label} exceeds size limit {limit} bytes after read: {len(raw)}")
    return raw


def _validate_uuid(value: Any, context: str, seen: set) -> str:
    if not isinstance(value, str):
        fail(f"{context} UUID must be a string")
    try:
        parsed = UUID(value)
    except (ValueError, AttributeError):
        fail(f"{context} has invalid UUID {value!r}")
    if str(parsed) != value or parsed.version != 5:
        fail(f"{context} UUID must be canonical stable version 5")
    if value in seen:
        fail(f"duplicate UUID {value} at {context}")
    seen.add(value)
    return value


def _validate_hierarchy(parents: Mapping[str, Optional[str]]) -> str:
    roots = [name for name, parent in parents.items() if parent is None]
    if len(roots) != 1:
        fail(f"geometry hierarchy must contain exactly one root, found {len(roots)}")
    for name, parent in parents.items():
        if parent is not None and parent not in parents:
            fail(f"bone {name} references unknown parent {parent}")
        visiting = set()
        current: Optional[str] = name
        while current is not None:
            if current in visiting:
                fail(f"bone hierarchy cycle detected at {current}")
            visiting.add(current)
            current = parents.get(current)
    return roots[0]


def validate_geometry(document: Mapping[str, Any]) -> Dict[str, Any]:
    _exact_keys(document, {"format_version", "minecraft:geometry"}, set(), "geometry root")
    if document.get("format_version") != "1.12.0":
        fail("geometry format_version must be 1.12.0")
    geometries = document.get("minecraft:geometry")
    if not isinstance(geometries, list) or len(geometries) != 1 or not isinstance(geometries[0], dict):
        fail("geometry must contain exactly one minecraft:geometry object")
    geometry = geometries[0]
    _exact_keys(geometry, {"description", "bones"}, set(), "geometry object")
    description = geometry.get("description")
    if not isinstance(description, dict):
        fail("geometry description must be an object")
    _exact_keys(description, {"identifier", "texture_width", "texture_height", "visible_bounds_width", "visible_bounds_height", "visible_bounds_offset"}, set(), "geometry description")
    if description.get("identifier") != "geometry.corrupted_silverfish":
        fail("geometry description identifier must be geometry.corrupted_silverfish")
    if description.get("texture_width") != 256 or description.get("texture_height") != 256:
        fail("geometry texture resolution must be 256x256")
    if _number(description.get("visible_bounds_width"), "geometry visible_bounds_width") != 2.6 or _number(description.get("visible_bounds_height"), "geometry visible_bounds_height") != 1.7:
        fail("geometry visible bounds dimensions mismatch fixed v3 contract")
    if _vec(description.get("visible_bounds_offset"), 3, "geometry visible_bounds_offset") != [0.0, 0.55, 0.0]:
        fail("geometry visible bounds offset mismatch fixed v3 contract")
    bones = geometry.get("bones")
    if not isinstance(bones, list) or len(bones) != 32:
        fail(f"geometry must contain exactly 32 bones, found {len(bones) if isinstance(bones, list) else 'invalid'}")
    names: List[str] = []
    parents: Dict[str, Optional[str]] = {}
    pivots: Dict[str, List[float]] = {}
    bone_rotations: Dict[str, List[float]] = {}
    cubes: Dict[str, Dict[str, Any]] = {}
    rectangles: List[Tuple[str, str, float, float, float, float]] = []
    uv_dimension_errors: List[str] = []
    cube_bones: Dict[str, str] = {}
    for index, bone in enumerate(bones):
        if not isinstance(bone, dict) or not isinstance(bone.get("name"), str) or not bone["name"]:
            fail(f"geometry bone {index} has invalid name")
        _exact_keys(bone, {"name", "pivot"}, {"parent", "rotation", "cubes"}, f"geometry bone {bone['name']}")
        name = bone["name"]
        if name in parents:
            fail(f"duplicate bone name {name}")
        names.append(name)
        parent = bone.get("parent")
        if parent is not None and not isinstance(parent, str):
            fail(f"bone {name} parent must be a string")
        parents[name] = parent
        pivots[name] = _vec(bone.get("pivot"), 3, f"bone {name} pivot")
        rotation = _vec(bone.get("rotation", [0, 0, 0]), 3, f"bone {name} rotation")
        bone_rotations[name] = rotation
        bone_cubes = bone.get("cubes", [])
        if not isinstance(bone_cubes, list):
            fail(f"bone {name} cubes must be an array")
        for cube_index, cube in enumerate(bone_cubes):
            if not isinstance(cube, dict) or not isinstance(cube.get("name"), str) or not cube["name"]:
                fail(f"bone {name} cube {cube_index} has invalid name")
            cube_name = cube["name"]
            _exact_keys(cube, {"name", "origin", "size", "uv"}, {"rotation", "pivot"}, f"geometry cube {cube_name}")
            if cube_name in cubes:
                fail(f"duplicate cube name {cube_name}")
            origin = _vec(cube.get("origin"), 3, f"cube {cube_name} origin")
            size = _vec(cube.get("size"), 3, f"cube {cube_name} size", positive=True)
            rotation_present = "rotation" in cube
            pivot_present = "pivot" in cube
            if rotation_present != pivot_present:
                fail(f"cube {cube_name} rotation and pivot must either both be present or both absent")
            rotation = _vec(cube.get("rotation", [0, 0, 0]), 3, f"cube {cube_name} rotation")
            pivot = _vec(cube.get("pivot", [origin[i] + size[i] / 2 for i in range(3)]), 3, f"cube {cube_name} pivot")
            expected_pivot = [origin[i] + size[i] / 2 for i in range(3)]
            if pivot != expected_pivot:
                fail(f"cube {cube_name} pivot must equal its geometric center")
            uv = cube.get("uv")
            if not isinstance(uv, dict) or set(uv) != set(FACE_NAMES):
                fail(f"cube {cube_name} must define exactly six UV faces")
            normalized_faces: Dict[str, List[float]] = {}
            for face in FACE_NAMES:
                face_value = uv[face]
                if not isinstance(face_value, dict):
                    fail(f"cube {cube_name} face {face} must be an object")
                _exact_keys(face_value, {"uv", "uv_size"}, set(), f"geometry cube {cube_name} face {face}")
                start = _vec(face_value.get("uv"), 2, f"cube {cube_name} face {face} UV")
                extent = _vec(face_value.get("uv_size"), 2, f"cube {cube_name} face {face} UV extent", positive=True)
                if not all(value.is_integer() for value in (*start, *extent)):
                    uv_dimension_errors.append(f"cube {cube_name} face {face} UV dimensions must be integers")
                face_units = {
                    "north": (size[0], size[1]), "south": (size[0], size[1]),
                    "east": (size[2], size[1]), "west": (size[2], size[1]),
                    "up": (size[0], size[2]), "down": (size[0], size[2]),
                }[face]
                expected_extent = [float(math.ceil(value * 2)) for value in face_units]
                if extent != expected_extent:
                    uv_dimension_errors.append(f"cube {cube_name} face {face} UV dimensions {extent} do not match 2px/unit {expected_extent}")
                x1, y1 = start
                x2, y2 = x1 + extent[0], y1 + extent[1]
                if x1 < 0 or y1 < 0 or x2 > TEXTURE_SIZE or y2 > TEXTURE_SIZE:
                    fail(f"cube {cube_name} face {face} UV out of bounds: {[x1, y1, x2, y2]}")
                rectangles.append((cube_name, face, x1, y1, x2, y2))
                normalized_faces[face] = [x1, y1, x2, y2]
            cubes[cube_name] = {"from": origin, "to": [origin[i] + size[i] for i in range(3)], "origin": pivot, "rotation": rotation, "faces": normalized_faces}
            cube_bones[cube_name] = name
    _validate_hierarchy(parents)
    if parents != EXPECTED_PARENTS:
        fail("geometry bone names or parent map mismatch fixed v3 contract")
    if len(cubes) != 112:
        fail(f"geometry must contain exactly 112 cubes, found {len(cubes)}")
    if len(rectangles) != 672:
        fail(f"geometry must contain exactly 672 UV rectangles, found {len(rectangles)}")
    for index, left in enumerate(rectangles):
        for right in rectangles[index + 1:]:
            overlap_x = min(left[4], right[4]) - max(left[2], right[2])
            overlap_y = min(left[5], right[5]) - max(left[3], right[3])
            if overlap_x > 0 and overlap_y > 0:
                fail(f"UV overlap between {left[0]}/{left[1]} and {right[0]}/{right[1]}")
            if overlap_y > 0:
                gap = max(left[2], right[2]) - min(left[4], right[4])
                if 0 <= gap < 2:
                    fail(f"UV gutter below 2px between {left[0]}/{left[1]} and {right[0]}/{right[1]}")
            if overlap_x > 0:
                gap = max(left[3], right[3]) - min(left[5], right[5])
                if 0 <= gap < 2:
                    fail(f"UV gutter below 2px between {left[0]}/{left[1]} and {right[0]}/{right[1]}")
    if uv_dimension_errors:
        fail(uv_dimension_errors[0])
    if set(cubes) != EXPECTED_CUBE_NAMES:
        fail("geometry cube names mismatch fixed v3 contract")
    actual_bounds = tuple(
        (min(cube["from"][axis] for cube in cubes.values()), max(cube["to"][axis] for cube in cubes.values()))
        for axis in range(3)
    )
    if actual_bounds != MODEL_BOUNDS:
        fail(f"geometry bounds {actual_bounds} mismatch fixed v3 bounds {MODEL_BOUNDS}")
    return {"bones": names, "parents": parents, "pivots": pivots, "bone_rotations": bone_rotations, "cubes": cubes, "cube_bones": cube_bones, "uv_rectangles": rectangles}


def _load_rgba(raw: bytes, path: Path, label: str) -> Image.Image:
    if len(raw) < 24 or raw[:8] != b"\x89PNG\r\n\x1a\n" or raw[12:16] != b"IHDR":
        fail(f"{label} PNG is malformed ({path}): invalid PNG signature/IHDR")
    width, height = struct.unpack(">II", raw[16:24])
    if (width, height) != (256, 256):
        fail(f"{label} must declare 256x256 dimensions before decode")
    try:
        with warnings.catch_warnings():
            warnings.simplefilter("error", Image.DecompressionBombWarning)
            with Image.open(BytesIO(raw)) as opened:
                opened.verify()
            image = Image.open(BytesIO(raw))
            image.load()
    except (OSError, ValueError, Image.DecompressionBombError, Image.DecompressionBombWarning, UnidentifiedImageError) as exc:
        fail(f"{label} PNG is malformed ({path}): {exc}")
    if image.format != "PNG" or image.mode != "RGBA" or image.size != (256, 256):
        fail(f"{label} must be a 256x256 RGBA PNG")
    return image


def validate_textures(main_bytes: bytes, glow_bytes: bytes, main_path: Path, glow_path: Path, geometry: Mapping[str, Any]) -> int:
    main = _load_rgba(main_bytes, main_path, "main texture")
    glow = _load_rgba(glow_bytes, glow_path, "glow texture")
    main_pixels = list(main.getdata())
    glow_pixels = list(glow.getdata())
    if any(pixel[3] not in (0, 255) for pixel in main_pixels):
        fail("main texture alpha must contain only 0 or 255")
    opaque_main = [pixel for pixel in main_pixels if pixel[3] == 255]
    if not opaque_main:
        fail("main texture is empty")
    if any(pixel[:3] == (0, 255, 0) for pixel in opaque_main):
        fail("main texture contains forbidden pure green")
    if any(pixel not in MAIN_PALETTE for pixel in opaque_main):
        fail("main texture contains a color outside the fixed 12-color palette")
    if any(pixel[3] not in (0, 255) for pixel in glow_pixels):
        fail("glow texture alpha must contain only 0 or 255")
    glow_count = sum(pixel[3] == 255 for pixel in glow_pixels)
    if not 20 <= glow_count <= 800:
        fail(f"glow texture must contain 20..800 opaque pixels, found {glow_count}")
    island_pixels: set = set()
    glow_allowed_pixels: set = set()
    for cube_name, _face, x1, y1, x2, y2 in geometry["uv_rectangles"]:
        points = {y * 256 + x for y in range(int(y1), int(y2)) for x in range(int(x1), int(x2))}
        island_pixels.update(points)
        if cube_name.startswith("eye_") or cube_name.startswith("crystal_") or cube_name == "mouth_sensor_cube":
            glow_allowed_pixels.update(points)
    for index, pixel in enumerate(main_pixels):
        if index in island_pixels and pixel[3] != 255:
            fail(f"main texture UV island is not fully opaque at ({index % 256},{index // 256})")
        if index not in island_pixels and pixel[3] != 0:
            fail(f"main texture has opaque pixel outside UV islands at ({index % 256},{index // 256})")
    for index, pixel in enumerate(glow_pixels):
        if pixel[3] == 255:
            if main_pixels[index][3] != 255:
                fail(f"glow leak at pixel ({index % 256},{index // 256}): main texture is transparent")
            if pixel not in GLOW_COLORS:
                fail(f"glow pixel ({index % 256},{index // 256}) has disallowed color {pixel}")
            if index not in glow_allowed_pixels:
                fail(f"glow pixel ({index % 256},{index // 256}) is outside allowed eye/crystal UV islands")
    if hashlib.sha256(main.tobytes()).hexdigest().upper() != MAIN_RGBA_SHA256:
        fail("main texture RGBA pixel hash mismatches approved Task4 visual contract")
    if hashlib.sha256(glow.tobytes()).hexdigest().upper() != GLOW_RGBA_SHA256:
        fail("glow texture RGBA pixel hash mismatches approved Task4 visual contract")
    return glow_count


def validate_animations(document: Mapping[str, Any], bone_names: set) -> Dict[str, Any]:
    _exact_keys(document, {"format_version", "animations"}, set(), "animation root")
    if document.get("format_version") != "1.8.0":
        fail("animation format_version must be 1.8.0")
    animations = document.get("animations")
    if not isinstance(animations, dict) or set(animations) != set(EXPECTED_ANIMATIONS):
        fail("animations file must contain exactly the five required animation IDs")
    normalized: Dict[str, Any] = {}
    for animation_name, (expected_length, expected_loop) in EXPECTED_ANIMATIONS.items():
        animation = animations[animation_name]
        if not isinstance(animation, dict):
            fail(f"animation {animation_name} must be an object")
        _exact_keys(animation, {"loop", "animation_length", "bones"}, set(), f"animation {animation_name}")
        length = _number(animation.get("animation_length"), f"animation {animation_name} length", positive=True)
        if length != expected_length:
            fail(f"animation {animation_name} length must be {expected_length}")
        loop = animation.get("loop")
        if not isinstance(loop, bool) or loop != expected_loop:
            fail(f"animation {animation_name} loop flag is invalid")
        bones = animation.get("bones")
        if not isinstance(bones, dict):
            fail(f"animation {animation_name} bones must be an object")
        expected_channels = EXPECTED_ANIMATION_CHANNELS[animation_name]
        for bone_name in bones:
            if bone_name not in bone_names:
                fail(f"animation {animation_name} references unknown bone {bone_name}")
        if set(bones) != set(expected_channels):
            fail(f"animation {animation_name} required bone set mismatches fixed v3 motion contract")
        normalized_bones: Dict[str, Any] = {}
        for bone_name, channels in bones.items():
            if bone_name not in bone_names:
                fail(f"animation {animation_name} references unknown bone {bone_name}")
            if not isinstance(channels, dict) or not channels or not set(channels).issubset(CHANNEL_NAMES):
                fail(f"animation {animation_name} bone {bone_name} has invalid channels")
            if set(channels) != expected_channels[bone_name]:
                fail(f"animation {animation_name} bone {bone_name} required channels mismatch fixed v3 motion contract")
            normalized_channels: Dict[str, Any] = {}
            for channel_name, keyframes in channels.items():
                if not isinstance(keyframes, dict) or not keyframes:
                    fail(f"animation {animation_name} {bone_name}/{channel_name} has no keyframes")
                normalized_keyframes: Dict[float, List[float]] = {}
                for time_text, keyframe in keyframes.items():
                    try:
                        time = float(time_text)
                    except (TypeError, ValueError):
                        fail(f"animation {animation_name} has invalid keyframe time {time_text!r}")
                    if not math.isfinite(time) or time < 0 or time > length:
                        fail(f"animation {animation_name} keyframe time {time_text!r} is outside 0..{length}")
                    if time in normalized_keyframes:
                        fail(f"animation {animation_name} has duplicate numeric keyframe time {time_text!r}")
                    if not isinstance(keyframe, dict) or keyframe.get("lerp_mode") != "linear":
                        fail(f"animation {animation_name} {bone_name}/{channel_name} keyframe {time_text} must be linear")
                    _exact_keys(keyframe, {"post", "lerp_mode"}, set(), f"animation {animation_name} {bone_name}/{channel_name} keyframe {time_text}")
                    normalized_keyframes[time] = _vec(keyframe.get("post"), 3, f"animation {animation_name} {bone_name}/{channel_name} keyframe {time_text} post")
                ordered = sorted(normalized_keyframes.items())
                if loop and (ordered[0][0] != 0 or ordered[-1][0] != length or ordered[0][1] != ordered[-1][1]):
                    fail(f"loop animation {animation_name} {bone_name}/{channel_name} start/end vectors must match")
                normalized_channels[channel_name] = normalized_keyframes
            normalized_bones[bone_name] = normalized_channels
        normalized[animation_name] = {"length": length, "loop": loop, "bones": normalized_bones}
    expected_times = {
        "animation.corrupted_silverfish.idle": {0.0, 0.8, 1.6},
        "animation.corrupted_silverfish.walk": {0.0, 0.2, 0.4, 0.6, 0.8},
        "animation.corrupted_silverfish.attack": {0.0, 0.225, 0.45},
        "animation.corrupted_silverfish.hurt": {0.0, 0.1, 0.2, 0.3},
        "animation.corrupted_silverfish.death": {0.0, 0.55, 1.1},
    }
    for animation_name, animation in normalized.items():
        for bone_name, channels in animation["bones"].items():
            for channel_name, frames in channels.items():
                if set(frames) != expected_times[animation_name]:
                    fail(f"animation {animation_name} {bone_name}/{channel_name} times mismatch fixed v3 motion contract")

    def vectors(animation_name: str, bone: str, channel: str) -> List[List[float]]:
        frames = normalized[animation_name]["bones"][bone][channel]
        return [frames[time] for time in sorted(frames)]

    idle = "animation.corrupted_silverfish.idle"
    if [value[1] for value in vectors(idle, "body", "position")] != [0.0, 0.12, 0.0]:
        fail("animation idle body position amplitude mismatches fixed v3 motion contract")
    for bone in ("head", "thorax", "shell_front", "shell_mid", "shell_rear", "abdomen", "tail_base", "tail_tip"):
        if max(abs(value[1]) for value in vectors(idle, bone, "rotation")) != 1.5:
            fail(f"animation idle {bone} rotation amplitude mismatches fixed v3 motion contract")
    for number in range(1, 8):
        if max(value[0] for value in vectors(idle, f"crystal_cluster_{number}", "scale")) != 1.025:
            fail(f"animation idle crystal_cluster_{number} scale amplitude mismatches fixed v3 motion contract")
    walk = "animation.corrupted_silverfish.walk"
    for bone in ("head", "thorax", "shell_front", "shell_mid", "shell_rear", "abdomen", "tail_base", "tail_tip"):
        if max(abs(value[1]) for value in vectors(walk, bone, "rotation")) != 3.0:
            fail(f"animation walk {bone} amplitude mismatches fixed v3 motion contract")
    for side in ("left", "right"):
        for position in ("front", "mid", "rear"):
            for part, amplitude in (("upper", 16.0), ("lower", 10.0)):
                bone = f"leg_{side}_{position}_{part}"
                if max(abs(value[0]) for value in vectors(walk, bone, "rotation")) != amplitude:
                    fail(f"animation walk {bone} amplitude mismatches fixed v3 motion contract")
    attack = "animation.corrupted_silverfish.attack"
    if min(value[2] for value in vectors(attack, "head", "position")) != -1.2:
        fail("animation attack head amplitude mismatches fixed v3 motion contract")
    if max(value[1] for value in vectors(attack, "mandible_left", "rotation")) != 24.0 or min(value[1] for value in vectors(attack, "mandible_right", "rotation")) != -24.0:
        fail("animation attack mandible amplitudes mismatch fixed v3 motion contract")
    for bone in ("leg_left_front_upper", "leg_right_front_upper"):
        if max(abs(value[0]) for value in vectors(attack, bone, "rotation")) != 8.0:
            fail(f"animation attack {bone} amplitude mismatches fixed v3 motion contract")
    hurt = "animation.corrupted_silverfish.hurt"
    if [value[2] for value in vectors(hurt, "body", "rotation")] != [0.0, 6.0, -2.0, 0.0]:
        fail("animation hurt body amplitude mismatches fixed v3 motion contract")
    for bone in ("shell_front", "shell_mid", "shell_rear"):
        if min(value[0] for value in vectors(hurt, bone, "scale")) != 0.96:
            fail(f"animation hurt {bone} amplitude mismatches fixed v3 motion contract")
    death = "animation.corrupted_silverfish.death"
    if vectors(death, "body", "position")[-1][1] != -1.4 or abs(vectors(death, "tail_tip", "rotation")[-1][0]) != 12.0:
        fail("animation death body/tail amplitudes mismatch fixed v3 motion contract")
    for side in ("left", "right"):
        for position in ("front", "mid", "rear"):
            bone = f"leg_{side}_{position}_upper"
            if abs(vectors(death, bone, "rotation")[-1][2]) != 48.0:
                fail(f"animation death {bone} amplitude mismatches fixed v3 motion contract")
    for number in range(1, 8):
        if vectors(death, f"crystal_cluster_{number}", "scale")[-1] != [0.82, 0.82, 0.82]:
            fail(f"animation death crystal_cluster_{number} amplitude mismatches fixed v3 motion contract")
    return normalized


def _flatten_outliner(
    items: Any,
    parent: Optional[str],
    groups_by_uuid: Mapping[str, str],
    elements_by_uuid: Mapping[str, Tuple[str, str]],
    found_parents: Dict[str, Optional[str]],
    found_element_owners: Dict[str, Optional[str]],
) -> None:
    if not isinstance(items, list):
        fail("bbmodel outliner children must be arrays")
    for item in items:
        if isinstance(item, str):
            if item not in elements_by_uuid or item in found_element_owners:
                fail(f"bbmodel outliner has invalid or duplicate element UUID {item}")
            element_name, expected_owner = elements_by_uuid[item]
            found_element_owners[item] = parent
            if parent != expected_owner:
                fail(f"bbmodel outliner element {element_name} belongs under {parent!r}, expected group {expected_owner}")
        elif isinstance(item, dict):
            _exact_keys(item, {"uuid", "isOpen", "children"}, set(), "bbmodel outliner group")
            uuid = item.get("uuid")
            if uuid not in groups_by_uuid or uuid in found_parents:
                fail(f"bbmodel outliner has invalid or duplicate group UUID {uuid}")
            name = groups_by_uuid[uuid]
            found_parents[name] = parent
            _flatten_outliner(item.get("children"), name, groups_by_uuid, elements_by_uuid, found_parents, found_element_owners)
        else:
            fail("bbmodel outliner contains an invalid child")


def validate_bbmodel(document: Mapping[str, Any], geometry: Mapping[str, Any], animations: Mapping[str, Any], main_bytes: bytes) -> None:
    _exact_keys(document, {"meta", "name", "model_identifier", "visible_box", "variable_placeholders", "timeline_setups", "unhandled_root_fields", "geckolib_modid", "geckolib_filepath_cache", "resolution", "elements", "groups", "outliner", "textures", "animations", "geckolib_model_type"}, set(), "bbmodel root")
    meta = document.get("meta")
    if isinstance(meta, dict):
        _exact_keys(meta, {"format_version", "model_format", "box_uv"}, set(), "bbmodel meta")
    if not isinstance(meta, dict) or meta.get("format_version") != "5.0" or meta.get("model_format") != "geckolib_model" or meta.get("box_uv") is not False:
        fail("bbmodel meta format must be GeckoLib 5.0 with per-face UV")
    if document.get("resolution") != {"width": 256, "height": 256}:
        fail("bbmodel resolution must be 256x256")
    groups = document.get("groups")
    elements = document.get("elements")
    if not isinstance(groups, list) or len(groups) != 32:
        fail("bbmodel must contain exactly 32 groups")
    if not isinstance(elements, list) or len(elements) != 112:
        fail("bbmodel must contain exactly 112 elements")
    seen_uuids: set = set()
    group_by_name: Dict[str, Mapping[str, Any]] = {}
    groups_by_uuid: Dict[str, str] = {}
    for group in groups:
        if not isinstance(group, dict) or not isinstance(group.get("name"), str):
            fail("bbmodel group has invalid name")
        name = group["name"]
        _exact_keys(group, {"name", "uuid", "export", "locked", "origin", "rotation", "color", "children", "reset", "shade", "mirror_uv", "visibility", "autouv", "isOpen"}, set(), f"bbmodel group {name}")
        if name in group_by_name:
            fail(f"bbmodel duplicate group name {name}")
        uuid = _validate_uuid(group.get("uuid"), f"bbmodel group {name}", seen_uuids)
        if name not in geometry["pivots"] or _vec(group.get("origin"), 3, f"bbmodel group {name} origin") != geometry["pivots"][name]:
            fail(f"bbmodel group {name} origin mismatches geometry")
        if _vec(group.get("rotation"), 3, f"bbmodel group {name} rotation") != geometry["bone_rotations"][name]:
            fail(f"bbmodel group {name} rotation mismatches geometry")
        group_by_name[name] = group
        groups_by_uuid[uuid] = name
    if set(group_by_name) != set(geometry["bones"]):
        fail("bbmodel group names mismatch geometry bones")
    elements_by_name: Dict[str, Mapping[str, Any]] = {}
    element_uuids: set = set()
    elements_by_uuid: Dict[str, Tuple[str, str]] = {}
    for element in elements:
        if not isinstance(element, dict) or not isinstance(element.get("name"), str):
            fail("bbmodel element has invalid name")
        name = element["name"]
        _exact_keys(element, {"name", "box_uv", "from", "to", "origin", "rotation", "faces", "type", "uuid", "bone"}, set(), f"bbmodel element {name}")
        if name in elements_by_name:
            fail(f"bbmodel duplicate element name {name}")
        uuid = _validate_uuid(element.get("uuid"), f"bbmodel element {name}", seen_uuids)
        element_uuids.add(uuid)
        if element.get("type") != "cube":
            fail(f"bbmodel element {name} type must be cube")
        if element.get("box_uv") is not False:
            fail(f"bbmodel element {name} box_uv must be false")
        expected = geometry["cubes"].get(name)
        if expected is None:
            fail(f"bbmodel cube {name} is absent from geometry")
        for key in ("from", "to", "origin", "rotation"):
            actual = _vec(element.get(key), 3, f"bbmodel cube {name} {key}")
            if actual != expected[key]:
                fail(f"bbmodel cube {name} {key} mismatches geometry")
        if element.get("bone") != geometry["cube_bones"][name]:
            fail(f"bbmodel cube {name} bone mismatches geometry")
        elements_by_uuid[uuid] = (name, element["bone"])
        faces = element.get("faces")
        if not isinstance(faces, dict) or set(faces) != set(FACE_NAMES):
            fail(f"bbmodel cube {name} faces mismatch geometry")
        for face in FACE_NAMES:
            if not isinstance(faces[face], dict):
                fail(f"bbmodel cube {name} face {face} must be an object")
            _exact_keys(faces[face], {"uv", "texture"}, set(), f"bbmodel cube {name} face {face}")
            if faces[face].get("texture") != 0 or isinstance(faces[face].get("texture"), bool):
                fail(f"bbmodel cube {name} face {face} texture must be 0")
            if _vec(faces[face].get("uv"), 4, f"bbmodel cube {name} face {face} UV") != expected["faces"][face]:
                fail(f"bbmodel cube {name} face {face} UV mismatches geometry")
        elements_by_name[name] = element
    if set(elements_by_name) != set(geometry["cubes"]):
        fail("bbmodel element names mismatch geometry cubes")
    found_parents: Dict[str, Optional[str]] = {}
    found_element_owners: Dict[str, Optional[str]] = {}
    _flatten_outliner(document.get("outliner"), None, groups_by_uuid, elements_by_uuid, found_parents, found_element_owners)
    if found_parents != geometry["parents"] or set(found_element_owners) != element_uuids:
        fail("bbmodel outliner hierarchy mismatches geometry")
    textures = document.get("textures")
    if not isinstance(textures, list) or len(textures) != 1 or not isinstance(textures[0], dict):
        fail("bbmodel must contain exactly one embedded texture")
    source = textures[0].get("source")
    _exact_keys(textures[0], {"path", "name", "folder", "namespace", "id", "particle", "render_mode", "visible", "mode", "saved", "uuid", "source"}, set(), "bbmodel texture")
    _validate_uuid(textures[0].get("uuid"), "bbmodel texture", seen_uuids)
    prefix = "data:image/png;base64,"
    if not isinstance(source, str) or not source.startswith(prefix):
        fail("bbmodel texture must be an embedded PNG data URL")
    try:
        embedded = base64.b64decode(source[len(prefix):], validate=True)
    except (ValueError, binascii.Error):
        fail("bbmodel embedded texture base64 is malformed")
    if embedded != main_bytes:
        fail("bbmodel embedded texture bytes mismatch main texture")
    bb_animations = document.get("animations")
    if not isinstance(bb_animations, list) or len(bb_animations) != 5:
        fail("bbmodel must contain exactly five animations")
    by_name: Dict[str, Mapping[str, Any]] = {}
    for animation in bb_animations:
        if not isinstance(animation, dict) or not isinstance(animation.get("name"), str):
            fail("bbmodel animation has invalid name")
        name = animation["name"]
        _exact_keys(animation, {"uuid", "name", "path", "loop", "override", "snapping", "length", "selected_item", "anim_time_update", "blend_weight", "start_delay", "loop_delay", "animators"}, set(), f"bbmodel animation {name}")
        if name in by_name:
            fail(f"bbmodel duplicate animation {name}")
        _validate_uuid(animation.get("uuid"), f"bbmodel animation {name}", seen_uuids)
        by_name[name] = animation
    if set(by_name) != set(animations):
        fail("bbmodel animation names mismatch animations file")
    for name, expected_animation in animations.items():
        actual = by_name[name]
        if _number(actual.get("length"), f"bbmodel animation {name} length") != expected_animation["length"]:
            fail(f"bbmodel animation {name} length mismatches animations file")
        if actual.get("loop") != ("loop" if expected_animation["loop"] else "once"):
            fail(f"bbmodel animation {name} loop mismatches animations file")
        animators = actual.get("animators")
        if not isinstance(animators, dict):
            fail(f"bbmodel animation {name} animators must be an object")
        projected: Dict[str, Any] = {}
        for group_uuid, animator in animators.items():
            if group_uuid not in groups_by_uuid or not isinstance(animator, dict) or animator.get("name") != groups_by_uuid[group_uuid]:
                fail(f"bbmodel animation {name} has invalid animator {group_uuid}")
            bone_name = groups_by_uuid[group_uuid]
            _exact_keys(animator, {"name", "type", "rotation_global", "quaternion_interpolation", "keyframes"}, set(), f"bbmodel animation {name} animator {bone_name}")
            channels: Dict[str, Dict[float, List[float]]] = {}
            keyframes = animator.get("keyframes")
            if not isinstance(keyframes, list):
                fail(f"bbmodel animation {name} animator {bone_name} keyframes must be an array")
            for keyframe in keyframes:
                if not isinstance(keyframe, dict) or keyframe.get("channel") not in CHANNEL_NAMES or keyframe.get("interpolation") != "linear":
                    fail(f"bbmodel animation {name} animator {bone_name} has invalid keyframe")
                _exact_keys(keyframe, {"channel", "data_points", "uuid", "time", "color", "interpolation"}, set(), f"bbmodel animation {name} animator {bone_name} keyframe")
                _validate_uuid(keyframe.get("uuid"), f"bbmodel animation {name} keyframe", seen_uuids)
                time = _number(keyframe.get("time"), f"bbmodel animation {name} keyframe time")
                points = keyframe.get("data_points")
                if not isinstance(points, list) or len(points) != 1 or not isinstance(points[0], dict):
                    fail(f"bbmodel animation {name} keyframe must have one data point")
                _exact_keys(points[0], {"x", "y", "z"}, set(), f"bbmodel animation {name} keyframe data point")
                vector = [_numeric_string(points[0].get(axis), f"bbmodel animation {name} keyframe {axis}") for axis in "xyz"]
                channel = channels.setdefault(keyframe["channel"], {})
                if time in channel:
                    fail(f"bbmodel animation {name} animator {bone_name} has duplicate keyframe time {time}")
                channel[time] = vector
            projected[bone_name] = channels
        if projected != expected_animation["bones"]:
            fail(f"bbmodel animation {name} channels mismatch animations file")
    contract_document = json.loads(json.dumps(document))
    contract_document["textures"][0]["source"] = "<validated-main-texture>"
    contract_digest = hashlib.sha256(json.dumps(contract_document, sort_keys=True, separators=(",", ":")).encode("utf-8")).hexdigest().upper()
    if contract_digest != BBMODEL_CONTRACT_SHA256:
        fail("bbmodel runtime/editor values mismatch immutable v3 contract")


def _is_reparse(path: Path) -> bool:
    try:
        stat_result = path.lstat()
    except OSError:
        return False
    if path.is_symlink():
        return True
    attributes = getattr(stat_result, "st_file_attributes", 0)
    return bool(attributes & getattr(stat_result, "FILE_ATTRIBUTE_REPARSE_POINT", 0x400))


def _inside(root: Path, path: Path) -> bool:
    try:
        return os.path.normcase(os.path.commonpath((str(root), str(path)))) == os.path.normcase(str(root))
    except ValueError:
        return False


def _secure_candidate_paths(root: Path, paths: Sequence[Path], manifest: Path) -> Tuple[Tuple[Path, ...], Path]:
    root_lexical = Path(os.path.abspath(str(root)))
    if _is_reparse(root_lexical):
        fail(f"root path is a symlink/reparse point: {root_lexical}")
    root_resolved = root_lexical.resolve()
    canonical_manifest = root_resolved / MANIFEST_RELATIVE
    secured: List[Path] = []
    for label, supplied in zip(("bbmodel", "geometry", "main texture", "glow texture", "animation"), paths):
        lexical = Path(os.path.abspath(str(supplied)))
        if not _inside(root_lexical, lexical):
            fail(f"{label} path is lexically outside root: {lexical}")
        try:
            relative = lexical.relative_to(root_lexical)
        except ValueError:
            fail(f"{label} path is lexically outside root: {lexical}")
        current = root_lexical
        for component in relative.parts:
            current = current / component
            if current.exists() or current.is_symlink():
                if _is_reparse(current):
                    fail(f"{label} path contains symlink/reparse component: {current}")
        resolved = lexical.resolve()
        if not _inside(root_resolved, resolved):
            fail(f"{label} resolved path is outside root: {resolved}")
        secured.append(resolved)
    manifest_lexical = Path(os.path.abspath(str(manifest)))
    if manifest_lexical != canonical_manifest or manifest_lexical.resolve() != canonical_manifest:
        fail(f"manifest path must be exactly {canonical_manifest}")
    if not _inside(root_resolved, manifest_lexical):
        fail(f"manifest path is outside root: {manifest_lexical}")
    current = root_lexical
    for component in manifest_lexical.relative_to(root_lexical).parts:
        current = current / component
        if current.exists() or current.is_symlink():
            if _is_reparse(current):
                fail(f"manifest path contains symlink/reparse component: {current}")
    resolved_manifest = manifest_lexical.resolve()
    if resolved_manifest in secured:
        fail(f"manifest path collides with candidate input: {resolved_manifest}")
    if len(set(secured)) != len(secured):
        fail("candidate input paths collide after resolution")
    lock_path = canonical_manifest.parent / MANIFEST_LOCK_NAME
    if lock_path.exists() or lock_path.is_symlink():
        if _is_reparse(lock_path):
            fail(f"manifest writer lock path is a symlink/reparse point: {lock_path}")
        resolved_lock = lock_path.resolve()
        if not _inside(root_resolved, resolved_lock):
            fail(f"manifest writer lock path is outside root: {resolved_lock}")
        for candidate in secured:
            if os.path.samefile(lock_path, candidate):
                fail(f"manifest writer lock path collides with candidate input: {candidate}")
        if resolved_manifest.exists() and os.path.samefile(lock_path, resolved_manifest):
            fail(f"manifest writer lock path collides with manifest: {resolved_manifest}")
    return tuple(secured), resolved_manifest


def _manifest_bytes(snapshots: Sequence[bytes]) -> bytes:
    payload: Dict[str, str] = {}
    for relative, snapshot in zip(RELATIVE_PATHS, snapshots):
        digest = hashlib.sha256(snapshot).hexdigest().upper()
        payload[relative.as_posix()] = digest
    return (json.dumps(payload, ensure_ascii=False, indent=2) + "\n").encode("utf-8")


def _write_synced_unique(path: Path, prefix: str, suffix: str, contents: bytes) -> Path:
    descriptor, name = tempfile.mkstemp(prefix=prefix, suffix=suffix, dir=str(path.parent))
    unique_path = Path(name)
    try:
        with os.fdopen(descriptor, "wb") as handle:
            handle.write(contents)
            handle.flush()
            os.fsync(handle.fileno())
    except BaseException:
        try:
            os.close(descriptor)
        except OSError:
            pass
        try:
            unique_path.unlink(missing_ok=True)
        except OSError:
            pass
        raise
    return unique_path


def _file_identity(path: Path) -> Tuple[int, int, int, int]:
    stat_result = path.stat()
    return (stat_result.st_dev, stat_result.st_ino, stat_result.st_size, stat_result.st_mtime_ns)


@contextmanager
def _manifest_lock(path: Path):
    path.parent.mkdir(parents=True, exist_ok=True)
    lock_path = path.parent / MANIFEST_LOCK_NAME
    if lock_path.exists() or lock_path.is_symlink():
        if _is_reparse(lock_path):
            fail(f"manifest writer lock path is a symlink/reparse point: {lock_path}")
    handle = None
    locked = False
    body_error: Optional[BaseException] = None
    release_errors: List[str] = []
    try:
        try:
            handle = lock_path.open("a+b", buffering=0)
            handle.seek(0, os.SEEK_END)
            if handle.tell() == 0:
                handle.write(b"\0")
                os.fsync(handle.fileno())
            handle.seek(0)
            if os.name == "nt":
                import msvcrt

                msvcrt.locking(handle.fileno(), msvcrt.LK_NBLCK, 1)
            else:
                import fcntl

                fcntl.flock(handle.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
            locked = True
        except OSError as exc:
            close_detail = ""
            if handle is not None:
                try:
                    handle.close()
                except OSError as close_exc:
                    close_detail = f"; lock handle close failed: {close_exc}"
                handle = None
            fail(f"manifest writer lock contention at {lock_path}: {exc}{close_detail}")
        try:
            yield
        except BaseException as exc:
            body_error = exc
    finally:
        if locked and handle is not None:
            try:
                handle.seek(0)
                if os.name == "nt":
                    import msvcrt

                    msvcrt.locking(handle.fileno(), msvcrt.LK_UNLCK, 1)
                else:
                    import fcntl

                    fcntl.flock(handle.fileno(), fcntl.LOCK_UN)
            except OSError as exc:
                release_errors.append(f"manifest writer unlock failed for {lock_path}: {exc}")
        if handle is not None:
            try:
                handle.close()
            except OSError as exc:
                release_errors.append(f"manifest writer lock handle close failed for {lock_path}: {exc}")
    if body_error is not None:
        if release_errors:
            fail(f"{body_error}; {'; '.join(release_errors)}")
        raise body_error.with_traceback(body_error.__traceback__)
    if release_errors:
        fail("; ".join(release_errors))


def _atomic_write_locked(path: Path, contents: bytes) -> None:
    temp_path: Optional[Path] = None
    backup_path: Optional[Path] = None
    old_snapshot: Optional[bytes] = None
    old_identity: Optional[Tuple[int, int, int, int]] = None
    candidate_identity: Optional[Tuple[int, int, int, int]] = None
    backup_ready = False
    target_existed = path.exists()
    commit_returned = False
    primary: Optional[BaseException] = None
    rollback_messages: List[str] = []
    cleanup_errors: List[str] = []
    retain_backup = False
    try:
        path.parent.mkdir(parents=True, exist_ok=True)
        temp_path = _write_synced_unique(path, f".{path.name}.", ".tmp", contents)
        candidate_identity = _file_identity(temp_path)
        if target_existed:
            try:
                old_identity = _file_identity(path)
                old_snapshot = path.read_bytes()
                if _file_identity(path) != old_identity:
                    raise OSError(f"concurrent target change detected while reading old manifest: {path}")
            except OSError as exc:
                raise OSError(f"manifest pre-publish read failed: {exc}") from exc
            backup_path = _write_synced_unique(path, f".{path.name}.backup.", ".tmp", old_snapshot)
            try:
                backup_bytes = backup_path.read_bytes()
            except OSError as exc:
                raise OSError(f"manifest backup verification read failed: {exc}") from exc
            if backup_bytes != old_snapshot or hashlib.sha256(backup_bytes).digest() != hashlib.sha256(old_snapshot).digest():
                raise OSError("manifest backup verification byte/hash mismatch")
            backup_ready = True
            if not path.exists() or _file_identity(path) != old_identity:
                retain_backup = True
                raise OSError(f"concurrent target change detected before publish: {path}")
        os.replace(temp_path, path)
        commit_returned = True
        if not temp_path.exists():
            temp_path = None
        current_identity = _file_identity(path)
        if current_identity != candidate_identity:
            if target_existed and current_identity == old_identity:
                raise OSError("manifest publish verification failed: target was not replaced")
            retain_backup = backup_ready
            raise OSError(f"concurrent target change detected immediately after publish: {path}")
        try:
            published = path.read_bytes()
        except OSError as exc:
            raise OSError(f"manifest verification read failed: {exc}") from exc
        if published != contents:
            raise OSError("manifest verification byte mismatch")
        if hashlib.sha256(published).digest() != hashlib.sha256(contents).digest():
            raise OSError("manifest verification hash mismatch")
    except BaseException as exc:
        primary = exc
        current_identity: Optional[Tuple[int, int, int, int]] = None
        if path.exists():
            try:
                current_identity = _file_identity(path)
            except OSError as identity_exc:
                retain_backup = True
                rollback_messages.append(f"rollback safety check failed for {path}: {identity_exc}")
        replace_consumed_candidate = temp_path is not None and not temp_path.exists()
        safely_published_target = (commit_returned or replace_consumed_candidate) and current_identity == candidate_identity
        if safely_published_target and backup_ready and backup_path is not None:
            try:
                os.replace(backup_path, path)
                restored = path.read_bytes()
                if old_snapshot is None or restored != old_snapshot:
                    raise OSError("restored manifest bytes mismatch backup")
                backup_path = None
                rollback_messages.append(f"rollback completed for {path}")
            except OSError as rollback_exc:
                retain_backup = True
                rollback_messages.append(f"rollback failed from {backup_path} to {path}: {rollback_exc}")
        elif safely_published_target and not target_existed:
            try:
                path.unlink()
                rollback_messages.append(f"rollback completed by removing new target {path}")
            except OSError as rollback_exc:
                rollback_messages.append(f"rollback failed removing new target {path}: {rollback_exc}")
        elif (commit_returned or replace_consumed_candidate) and current_identity != candidate_identity:
            if not (target_existed and current_identity == old_identity):
                retain_backup = backup_ready
                rollback_messages.append(f"rollback skipped because concurrent target must not be overwritten: {path}")
    finally:
        if temp_path is not None:
            try:
                temp_path.unlink(missing_ok=True)
            except OSError as exc:
                cleanup_errors.append(f"cleanup failed for {temp_path}: {exc}")
        if backup_path is not None and not retain_backup:
            try:
                backup_path.unlink(missing_ok=True)
            except OSError as exc:
                cleanup_errors.append(f"cleanup failed for {backup_path}: {exc}")
    if primary is not None or rollback_messages or cleanup_errors:
        details = []
        if primary is not None:
            details.append(f"manifest write failed ({path}): {primary}")
        details.extend(rollback_messages)
        details.extend(cleanup_errors)
        fail("; ".join(details))


def _atomic_write(path: Path, contents: bytes) -> None:
    with _manifest_lock(path):
        _atomic_write_locked(path, contents)


def validate(paths: Sequence[Path], manifest: Path, root: Optional[Path] = None) -> int:
    if root is None:
        root = Path(os.path.abspath(str(paths[0]))).parents[2]
    paths, manifest = _secure_candidate_paths(root, paths, manifest)
    labels = ("bbmodel", "geometry", "main texture", "glow texture", "animation")
    snapshots = tuple(_read_snapshot(path, limit, label) for path, limit, label in zip(paths, FILE_LIMITS, labels))
    bbmodel_path, geometry_path, main_path, glow_path, animation_path = paths
    bbmodel_bytes, geometry_bytes, main_bytes, glow_bytes, animation_bytes = snapshots
    geometry = validate_geometry(_load_json(geometry_bytes, geometry_path, "geometry"))
    geometry_projection = {key: geometry[key] for key in ("parents", "pivots", "bone_rotations", "cubes", "cube_bones")}
    geometry_digest = hashlib.sha256(json.dumps(geometry_projection, sort_keys=True, separators=(",", ":")).encode("utf-8")).hexdigest().upper()
    if geometry_digest != GEOMETRY_CONTRACT_SHA256:
        fail("geometry transforms/UVs mismatch immutable v3 contract")
    glow_count = validate_textures(main_bytes, glow_bytes, main_path, glow_path, geometry)
    animations = validate_animations(_load_json(animation_bytes, animation_path, "animation"), set(geometry["bones"]))
    animation_digest = hashlib.sha256(json.dumps(animations, sort_keys=True, separators=(",", ":")).encode("utf-8")).hexdigest().upper()
    if animation_digest != ANIMATION_CONTRACT_SHA256:
        fail("animation times/vectors mismatch immutable v3 motion contract")
    validate_bbmodel(_load_json(bbmodel_bytes, bbmodel_path, "bbmodel"), geometry, animations, main_bytes)
    _atomic_write(manifest, _manifest_bytes(snapshots))
    return glow_count


def _parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Validate Corrupted Silverfish v3 artifacts")
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument("--bbmodel", type=Path)
    parser.add_argument("--geometry", type=Path)
    parser.add_argument("--main", type=Path)
    parser.add_argument("--glow", type=Path)
    parser.add_argument("--animation", type=Path)
    parser.add_argument("--manifest", type=Path)
    return parser


def main(argv: Optional[Sequence[str]] = None) -> int:
    args = _parser().parse_args(argv)
    root = Path(os.path.abspath(str(args.root)))
    overrides = (args.bbmodel, args.geometry, args.main, args.glow, args.animation)
    paths = tuple((Path(os.path.abspath(str(override))) if override is not None else root / relative) for override, relative in zip(overrides, RELATIVE_PATHS))
    manifest = Path(os.path.abspath(str(args.manifest))) if args.manifest is not None else root / MANIFEST_RELATIVE
    try:
        glow_count = validate(paths, manifest, root=root)
    except ValidationFailure as exc:
        print(f"ASSET_CHECK_FAILED: {exc}", file=sys.stderr)
        return 1
    except Exception as exc:
        print(f"ASSET_CHECK_FAILED: unexpected validator error: {exc}", file=sys.stderr)
        return 1
    print(f"ASSET_CHECK=PASS;TARGET=V3;BONES=32;CUBES=112;ANIMATIONS=5;TEXTURE=256x256;GLOW_PIXELS={glow_count}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
