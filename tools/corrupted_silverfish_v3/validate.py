"""Independently validate the committed Corrupted Silverfish v3 candidate.

This module deliberately treats the generated artifacts as untrusted input.  It
does not use any model generator or in-memory specification as an oracle.
"""

from __future__ import annotations

import argparse
import base64
import binascii
import hashlib
import json
import math
import os
from pathlib import Path
import sys
import tempfile
from typing import Any, Dict, List, Mapping, Optional, Sequence, Tuple
from uuid import UUID

from PIL import Image, UnidentifiedImageError


TEXTURE_SIZE = 256
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


def _load_json(path: Path, label: str) -> Mapping[str, Any]:
    try:
        raw = path.read_bytes()
    except OSError as exc:
        fail(f"{label} read failed ({path}): {exc}")
    try:
        value = json.loads(raw.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        fail(f"{label} JSON is malformed ({path}): {exc}")
    if not isinstance(value, dict):
        fail(f"{label} JSON root must be an object")
    return value


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
    if document.get("format_version") != "1.12.0":
        fail("geometry format_version must be 1.12.0")
    geometries = document.get("minecraft:geometry")
    if not isinstance(geometries, list) or len(geometries) != 1 or not isinstance(geometries[0], dict):
        fail("geometry must contain exactly one minecraft:geometry object")
    geometry = geometries[0]
    description = geometry.get("description")
    if not isinstance(description, dict) or description.get("texture_width") != 256 or description.get("texture_height") != 256:
        fail("geometry texture resolution must be 256x256")
    bones = geometry.get("bones")
    if not isinstance(bones, list) or len(bones) != 32:
        fail(f"geometry must contain exactly 32 bones, found {len(bones) if isinstance(bones, list) else 'invalid'}")
    names: List[str] = []
    parents: Dict[str, Optional[str]] = {}
    pivots: Dict[str, List[float]] = {}
    cubes: Dict[str, Dict[str, Any]] = {}
    rectangles: List[Tuple[str, str, float, float, float, float]] = []
    cube_bones: Dict[str, str] = {}
    for index, bone in enumerate(bones):
        if not isinstance(bone, dict) or not isinstance(bone.get("name"), str) or not bone["name"]:
            fail(f"geometry bone {index} has invalid name")
        name = bone["name"]
        if name in parents:
            fail(f"duplicate bone name {name}")
        names.append(name)
        parent = bone.get("parent")
        if parent is not None and not isinstance(parent, str):
            fail(f"bone {name} parent must be a string")
        parents[name] = parent
        pivots[name] = _vec(bone.get("pivot"), 3, f"bone {name} pivot")
        rotation = bone.get("rotation", [0, 0, 0])
        _vec(rotation, 3, f"bone {name} rotation")
        bone_cubes = bone.get("cubes", [])
        if not isinstance(bone_cubes, list):
            fail(f"bone {name} cubes must be an array")
        for cube_index, cube in enumerate(bone_cubes):
            if not isinstance(cube, dict) or not isinstance(cube.get("name"), str) or not cube["name"]:
                fail(f"bone {name} cube {cube_index} has invalid name")
            cube_name = cube["name"]
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
                start = _vec(face_value.get("uv"), 2, f"cube {cube_name} face {face} UV")
                extent = _vec(face_value.get("uv_size"), 2, f"cube {cube_name} face {face} UV extent", positive=True)
                x1, y1 = start
                x2, y2 = x1 + extent[0], y1 + extent[1]
                if x1 < 0 or y1 < 0 or x2 > TEXTURE_SIZE or y2 > TEXTURE_SIZE:
                    fail(f"cube {cube_name} face {face} UV out of bounds: {[x1, y1, x2, y2]}")
                rectangles.append((cube_name, face, x1, y1, x2, y2))
                normalized_faces[face] = [x1, y1, x2, y2]
            cubes[cube_name] = {"from": origin, "to": [origin[i] + size[i] for i in range(3)], "origin": pivot, "rotation": rotation, "faces": normalized_faces}
            cube_bones[cube_name] = name
    _validate_hierarchy(parents)
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
    return {"bones": names, "parents": parents, "pivots": pivots, "cubes": cubes, "cube_bones": cube_bones}


def _load_rgba(path: Path, label: str) -> Tuple[Image.Image, bytes]:
    try:
        raw = path.read_bytes()
        with Image.open(path) as opened:
            opened.verify()
        image = Image.open(path)
        image.load()
    except (OSError, UnidentifiedImageError) as exc:
        fail(f"{label} PNG is malformed ({path}): {exc}")
    if image.format != "PNG" or image.mode != "RGBA" or image.size != (256, 256):
        fail(f"{label} must be a 256x256 RGBA PNG")
    return image, raw


def validate_textures(main_path: Path, glow_path: Path) -> Tuple[int, bytes]:
    main, main_bytes = _load_rgba(main_path, "main texture")
    glow, _ = _load_rgba(glow_path, "glow texture")
    main_pixels = list(main.getdata())
    glow_pixels = list(glow.getdata())
    if any(pixel[3] not in (0, 255) for pixel in main_pixels):
        fail("main texture alpha must contain only 0 or 255")
    opaque_main = [pixel for pixel in main_pixels if pixel[3] == 255]
    if not opaque_main:
        fail("main texture is empty")
    if any(pixel[:3] == (0, 255, 0) for pixel in opaque_main):
        fail("main texture contains forbidden pure green")
    if any(pixel[3] not in (0, 255) for pixel in glow_pixels):
        fail("glow texture alpha must contain only 0 or 255")
    glow_count = sum(pixel[3] == 255 for pixel in glow_pixels)
    if not 20 <= glow_count <= 800:
        fail(f"glow texture must contain 20..800 opaque pixels, found {glow_count}")
    for index, pixel in enumerate(glow_pixels):
        if pixel[3] == 255:
            if main_pixels[index][3] != 255:
                fail(f"glow leak at pixel ({index % 256},{index // 256}): main texture is transparent")
            if pixel not in GLOW_COLORS:
                fail(f"glow pixel ({index % 256},{index // 256}) has disallowed color {pixel}")
    return glow_count, main_bytes


def validate_animations(document: Mapping[str, Any], bone_names: set) -> Dict[str, Any]:
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
        length = _number(animation.get("animation_length"), f"animation {animation_name} length", positive=True)
        if length != expected_length:
            fail(f"animation {animation_name} length must be {expected_length}")
        loop = animation.get("loop")
        if not isinstance(loop, bool) or loop != expected_loop:
            fail(f"animation {animation_name} loop flag is invalid")
        bones = animation.get("bones")
        if not isinstance(bones, dict):
            fail(f"animation {animation_name} bones must be an object")
        normalized_bones: Dict[str, Any] = {}
        for bone_name, channels in bones.items():
            if bone_name not in bone_names:
                fail(f"animation {animation_name} references unknown bone {bone_name}")
            if not isinstance(channels, dict) or not channels or not set(channels).issubset(CHANNEL_NAMES):
                fail(f"animation {animation_name} bone {bone_name} has invalid channels")
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
                    normalized_keyframes[time] = _vec(keyframe.get("post"), 3, f"animation {animation_name} {bone_name}/{channel_name} keyframe {time_text} post")
                ordered = sorted(normalized_keyframes.items())
                if loop and (ordered[0][0] != 0 or ordered[-1][0] != length or ordered[0][1] != ordered[-1][1]):
                    fail(f"loop animation {animation_name} {bone_name}/{channel_name} start/end vectors must match")
                normalized_channels[channel_name] = normalized_keyframes
            normalized_bones[bone_name] = normalized_channels
        normalized[animation_name] = {"length": length, "loop": loop, "bones": normalized_bones}
    return normalized


def _flatten_outliner(items: Any, parent: Optional[str], groups_by_uuid: Mapping[str, str], element_uuids: set, found_parents: Dict[str, Optional[str]], found_elements: set) -> None:
    if not isinstance(items, list):
        fail("bbmodel outliner children must be arrays")
    for item in items:
        if isinstance(item, str):
            if item not in element_uuids or item in found_elements:
                fail(f"bbmodel outliner has invalid or duplicate element UUID {item}")
            found_elements.add(item)
        elif isinstance(item, dict):
            uuid = item.get("uuid")
            if uuid not in groups_by_uuid or uuid in found_parents:
                fail(f"bbmodel outliner has invalid or duplicate group UUID {uuid}")
            name = groups_by_uuid[uuid]
            found_parents[name] = parent
            _flatten_outliner(item.get("children"), name, groups_by_uuid, element_uuids, found_parents, found_elements)
        else:
            fail("bbmodel outliner contains an invalid child")


def validate_bbmodel(document: Mapping[str, Any], geometry: Mapping[str, Any], animations: Mapping[str, Any], main_bytes: bytes) -> None:
    meta = document.get("meta")
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
        if name in group_by_name:
            fail(f"bbmodel duplicate group name {name}")
        uuid = _validate_uuid(group.get("uuid"), f"bbmodel group {name}", seen_uuids)
        if name not in geometry["pivots"] or _vec(group.get("origin"), 3, f"bbmodel group {name} origin") != geometry["pivots"][name]:
            fail(f"bbmodel group {name} origin mismatches geometry")
        group_by_name[name] = group
        groups_by_uuid[uuid] = name
    if set(group_by_name) != set(geometry["bones"]):
        fail("bbmodel group names mismatch geometry bones")
    elements_by_name: Dict[str, Mapping[str, Any]] = {}
    element_uuids: set = set()
    for element in elements:
        if not isinstance(element, dict) or not isinstance(element.get("name"), str):
            fail("bbmodel element has invalid name")
        name = element["name"]
        if name in elements_by_name:
            fail(f"bbmodel duplicate element name {name}")
        uuid = _validate_uuid(element.get("uuid"), f"bbmodel element {name}", seen_uuids)
        element_uuids.add(uuid)
        expected = geometry["cubes"].get(name)
        if expected is None:
            fail(f"bbmodel cube {name} is absent from geometry")
        for key in ("from", "to", "origin", "rotation"):
            actual = _vec(element.get(key), 3, f"bbmodel cube {name} {key}")
            if actual != expected[key]:
                fail(f"bbmodel cube {name} {key} mismatches geometry")
        if element.get("bone") != geometry["cube_bones"][name]:
            fail(f"bbmodel cube {name} bone mismatches geometry")
        faces = element.get("faces")
        if not isinstance(faces, dict) or set(faces) != set(FACE_NAMES):
            fail(f"bbmodel cube {name} faces mismatch geometry")
        for face in FACE_NAMES:
            if not isinstance(faces[face], dict) or _vec(faces[face].get("uv"), 4, f"bbmodel cube {name} face {face} UV") != expected["faces"][face]:
                fail(f"bbmodel cube {name} face {face} UV mismatches geometry")
        elements_by_name[name] = element
    if set(elements_by_name) != set(geometry["cubes"]):
        fail("bbmodel element names mismatch geometry cubes")
    found_parents: Dict[str, Optional[str]] = {}
    found_elements: set = set()
    _flatten_outliner(document.get("outliner"), None, groups_by_uuid, element_uuids, found_parents, found_elements)
    if found_parents != geometry["parents"] or found_elements != element_uuids:
        fail("bbmodel outliner hierarchy mismatches geometry")
    textures = document.get("textures")
    if not isinstance(textures, list) or len(textures) != 1 or not isinstance(textures[0], dict):
        fail("bbmodel must contain exactly one embedded texture")
    source = textures[0].get("source")
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
            channels: Dict[str, Dict[float, List[float]]] = {}
            keyframes = animator.get("keyframes")
            if not isinstance(keyframes, list):
                fail(f"bbmodel animation {name} animator {bone_name} keyframes must be an array")
            for keyframe in keyframes:
                if not isinstance(keyframe, dict) or keyframe.get("channel") not in CHANNEL_NAMES or keyframe.get("interpolation") != "linear":
                    fail(f"bbmodel animation {name} animator {bone_name} has invalid keyframe")
                _validate_uuid(keyframe.get("uuid"), f"bbmodel animation {name} keyframe", seen_uuids)
                time = _number(keyframe.get("time"), f"bbmodel animation {name} keyframe time")
                points = keyframe.get("data_points")
                if not isinstance(points, list) or len(points) != 1 or not isinstance(points[0], dict):
                    fail(f"bbmodel animation {name} keyframe must have one data point")
                vector = [_numeric_string(points[0].get(axis), f"bbmodel animation {name} keyframe {axis}") for axis in "xyz"]
                channel = channels.setdefault(keyframe["channel"], {})
                if time in channel:
                    fail(f"bbmodel animation {name} animator {bone_name} has duplicate keyframe time {time}")
                channel[time] = vector
            projected[bone_name] = channels
        if projected != expected_animation["bones"]:
            fail(f"bbmodel animation {name} channels mismatch animations file")


def _manifest_bytes(paths: Sequence[Path]) -> bytes:
    payload: Dict[str, str] = {}
    for relative, path in zip(RELATIVE_PATHS, paths):
        try:
            digest = hashlib.sha256(path.read_bytes()).hexdigest().upper()
        except OSError as exc:
            fail(f"manifest hashing failed for {path}: {exc}")
        payload[relative.as_posix()] = digest
    return (json.dumps(payload, ensure_ascii=False, indent=2) + "\n").encode("utf-8")


def _atomic_write(path: Path, contents: bytes) -> None:
    temp_path: Optional[Path] = None
    try:
        path.parent.mkdir(parents=True, exist_ok=True)
        descriptor, name = tempfile.mkstemp(prefix=f".{path.name}.", suffix=".tmp", dir=str(path.parent))
        temp_path = Path(name)
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
            raise
        os.replace(temp_path, path)
        temp_path = None
    except OSError as exc:
        fail(f"manifest write failed ({path}): {exc}")
    finally:
        if temp_path is not None:
            try:
                temp_path.unlink(missing_ok=True)
            except OSError:
                pass


def validate(paths: Sequence[Path], manifest: Path) -> int:
    bbmodel_path, geometry_path, main_path, glow_path, animation_path = paths
    geometry = validate_geometry(_load_json(geometry_path, "geometry"))
    glow_count, main_bytes = validate_textures(main_path, glow_path)
    animations = validate_animations(_load_json(animation_path, "animation"), set(geometry["bones"]))
    validate_bbmodel(_load_json(bbmodel_path, "bbmodel"), geometry, animations, main_bytes)
    _atomic_write(manifest, _manifest_bytes(paths))
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
    root = args.root.resolve()
    overrides = (args.bbmodel, args.geometry, args.main, args.glow, args.animation)
    paths = tuple((override.resolve() if override is not None else root / relative) for override, relative in zip(overrides, RELATIVE_PATHS))
    manifest = args.manifest.resolve() if args.manifest is not None else root / MANIFEST_RELATIVE
    try:
        glow_count = validate(paths, manifest)
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
