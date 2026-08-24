"""Deterministic, headless review renderer for committed Corrupted Silverfish v3 assets."""

from __future__ import annotations

import argparse
from io import BytesIO
import json
import math
import os
from pathlib import Path
import tempfile
from typing import Any, Dict, Iterable, Mapping, Optional, Sequence, Tuple

from PIL import Image, ImageDraw, UnidentifiedImageError


Vec3 = Tuple[float, float, float]
Matrix = Tuple[Tuple[float, float, float, float], ...]

GEOMETRY_RELATIVE = Path("Modelle/Exports/corrupted_silverfish_v3/geo/corrupted_silverfish.geo.json")
ANIMATION_RELATIVE = Path("Modelle/Exports/corrupted_silverfish_v3/animations/corrupted_silverfish.animation.json")
TEXTURE_RELATIVE = Path("Modelle/Exports/corrupted_silverfish_v3/textures/entity/corrupted_silverfish.png")
REVIEW_RELATIVE = Path("Modelle/Exports/corrupted_silverfish_v3/review")
DEFAULT_CONCEPT = Path("Modelle/Exports/corrupted_silverfish_v2/concept/concept_sheet_raw.png")
FACE_NAMES = ("north", "east", "south", "west", "up", "down")
OUTPUT_NAMES = (
    "candidate_front.png", "candidate_right.png", "candidate_back.png", "candidate_top.png",
    "candidate_idle.png", "candidate_walk.png", "candidate_attack.png", "candidate_contact_sheet.png",
)
CANVAS_SIZE = (768, 768)
PIXELS_PER_UNIT = 18.0
MODEL_CENTER = (-0.15, 5.95, 2.8)
CONCEPT_RESAMPLING = Image.Resampling.BICUBIC


class RenderFailure(Exception):
    """A user-actionable render input or output failure."""


def _fail(message: str) -> None:
    raise RenderFailure(message)


def _vec3(value: Any, context: str) -> Vec3:
    if not isinstance(value, (list, tuple)) or len(value) != 3:
        _fail(f"{context} must be a finite Vec3")
    if any(isinstance(item, bool) or not isinstance(item, (int, float)) or not math.isfinite(item) for item in value):
        _fail(f"{context} must be a finite Vec3")
    return tuple(float(item) for item in value)  # type: ignore[return-value]


def identity_matrix() -> Matrix:
    return ((1.0, 0.0, 0.0, 0.0), (0.0, 1.0, 0.0, 0.0),
            (0.0, 0.0, 1.0, 0.0), (0.0, 0.0, 0.0, 1.0))


def _multiply(left: Matrix, right: Matrix) -> Matrix:
    return tuple(tuple(sum(left[row][k] * right[k][column] for k in range(4)) for column in range(4)) for row in range(4))


def _translate(vector: Vec3) -> Matrix:
    x, y, z = vector
    return ((1, 0, 0, x), (0, 1, 0, y), (0, 0, 1, z), (0, 0, 0, 1))


def _scale(vector: Vec3) -> Matrix:
    x, y, z = vector
    return ((x, 0, 0, 0), (0, y, 0, 0), (0, 0, z, 0), (0, 0, 0, 1))


def _rotation(vector: Vec3) -> Matrix:
    rx, ry, rz = (math.radians(value) for value in vector)
    cx, sx, cy, sy, cz, sz = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
    mx: Matrix = ((1, 0, 0, 0), (0, cx, -sx, 0), (0, sx, cx, 0), (0, 0, 0, 1))
    my: Matrix = ((cy, 0, sy, 0), (0, 1, 0, 0), (-sy, 0, cy, 0), (0, 0, 0, 1))
    mz: Matrix = ((cz, -sz, 0, 0), (sz, cz, 0, 0), (0, 0, 1, 0), (0, 0, 0, 1))
    return _multiply(mz, _multiply(my, mx))


def transform_point(matrix: Matrix, point: Vec3) -> Vec3:
    vector = (*point, 1.0)
    return tuple(sum(matrix[row][column] * vector[column] for column in range(4)) for row in range(3))  # type: ignore[return-value]


def _pivot_matrix(pivot: Vec3, position: Vec3, rotation: Vec3, scale: Vec3) -> Matrix:
    return _multiply(
        _translate(tuple(pivot[index] + position[index] for index in range(3))),
        _multiply(_rotation(rotation), _multiply(_scale(scale), _translate(tuple(-value for value in pivot)))),
    )


def sample_channel(channel: Mapping[str, Any], time: float, length: float, loop: bool, context: str = "channel") -> Vec3:
    if not isinstance(time, (int, float)) or not math.isfinite(time):
        _fail(f"{context} sample time must be finite")
    if not isinstance(length, (int, float)) or not math.isfinite(length) or length <= 0:
        _fail(f"{context} animation length must be positive")
    points = []
    for raw_time, keyframe in channel.items():
        try:
            key_time = float(raw_time)
        except (TypeError, ValueError):
            _fail(f"{context} keyframe time {raw_time!r} must be numeric")
        if not math.isfinite(key_time) or not isinstance(keyframe, dict):
            _fail(f"{context} keyframe {raw_time!r} is malformed")
        value = keyframe.get("post", keyframe.get("pre"))
        points.append((key_time, _vec3(value, f"{context} keyframe {raw_time}")))
    if not points:
        _fail(f"{context} has no keyframes")
    points.sort(key=lambda item: item[0])
    sample_time = float(time) % float(length) if loop else min(max(float(time), points[0][0]), points[-1][0])
    if sample_time <= points[0][0]:
        return points[0][1]
    if sample_time >= points[-1][0]:
        return points[-1][1]
    for (left_time, left), (right_time, right) in zip(points, points[1:]):
        if sample_time <= right_time:
            amount = (sample_time - left_time) / (right_time - left_time)
            return tuple(left[index] + (right[index] - left[index]) * amount for index in range(3))  # type: ignore[return-value]
    return points[-1][1]


def build_bone_transforms(bones: Sequence[Mapping[str, Any]], pose: Mapping[str, Mapping[str, Vec3]]) -> Dict[str, Matrix]:
    by_name: Dict[str, Mapping[str, Any]] = {}
    for index, bone in enumerate(bones):
        name = bone.get("name")
        if not isinstance(name, str) or not name:
            _fail(f"bone {index} has no valid name")
        if name in by_name:
            _fail(f"duplicate bone {name}")
        by_name[name] = bone
    for name, bone in by_name.items():
        parent = bone.get("parent")
        if parent is not None and parent not in by_name:
            _fail(f"bone {name} has unknown parent {parent}")
    unknown = set(pose) - set(by_name)
    if unknown:
        _fail(f"animation references unknown bone {sorted(unknown)[0]}")
    result: Dict[str, Matrix] = {}
    visiting = set()

    def visit(name: str) -> Matrix:
        if name in result:
            return result[name]
        if name in visiting:
            _fail(f"bone parent cycle contains {name}")
        visiting.add(name)
        bone = by_name[name]
        animated = pose.get(name, {})
        pivot = _vec3(bone.get("pivot", (0, 0, 0)), f"bone {name} pivot")
        base_rotation = _vec3(bone.get("rotation", (0, 0, 0)), f"bone {name} rotation")
        position = _vec3(animated.get("position", (0, 0, 0)), f"bone {name} position")
        extra_rotation = _vec3(animated.get("rotation", (0, 0, 0)), f"bone {name} rotation animation")
        scale = _vec3(animated.get("scale", (1, 1, 1)), f"bone {name} scale")
        if any(value < 0 for value in scale):
            _fail(f"negative scale is unsupported for bone {name}")
        local = _pivot_matrix(pivot, position, tuple(base_rotation[i] + extra_rotation[i] for i in range(3)), scale)
        parent = bone.get("parent")
        result[name] = _multiply(visit(parent), local) if parent else local
        visiting.remove(name)
        return result[name]

    for bone_name in by_name:
        visit(bone_name)
    return result


def _read_json(path: Path, label: str) -> Any:
    if not path.is_file():
        _fail(f"{label} asset is missing: {path}")
    try:
        return json.loads(path.read_text(encoding="utf-8-sig"))
    except (OSError, UnicodeError, json.JSONDecodeError) as exc:
        _fail(f"{label} asset is not valid JSON ({path}): {exc}")


def load_assets(root: Path) -> Dict[str, Any]:
    root = Path(root).resolve()
    geometry_doc = _read_json(root / GEOMETRY_RELATIVE, "geometry")
    try:
        geometries = geometry_doc["minecraft:geometry"]
        geometry = geometries[0]
        description = geometry["description"]
        bones = geometry["bones"]
    except (KeyError, IndexError, TypeError) as exc:
        _fail(f"geometry asset has malformed Bedrock structure: {exc}")
    if description.get("texture_width") != 256 or description.get("texture_height") != 256:
        _fail("geometry asset must declare a 256x256 texture")
    if not isinstance(bones, list):
        _fail("geometry asset bones must be an array")
    animation_doc = _read_json(root / ANIMATION_RELATIVE, "animation")
    animations = animation_doc.get("animations") if isinstance(animation_doc, dict) else None
    if not isinstance(animations, dict):
        _fail("animation asset must contain an animations object")
    texture_path = root / TEXTURE_RELATIVE
    if not texture_path.is_file():
        _fail(f"main texture asset is missing: {texture_path}")
    try:
        with Image.open(texture_path) as source:
            source.load()
            texture = source.convert("RGBA")
    except (OSError, UnidentifiedImageError) as exc:
        _fail(f"main texture asset is not a valid PNG ({texture_path}): {exc}")
    if texture.size != (256, 256):
        _fail(f"main texture asset must be 256x256, got {texture.size[0]}x{texture.size[1]}")
    build_bone_transforms(bones, {})
    return {"geometry": geometry, "bones": bones, "animations": animations, "texture": texture}


def _pose_for(assets: Mapping[str, Any], animation_name: Optional[str], time: float) -> Dict[str, Dict[str, Vec3]]:
    if animation_name is None:
        return {}
    animation = assets["animations"].get(animation_name)
    if not isinstance(animation, dict):
        _fail(f"animation asset has no animation named {animation_name}")
    length = animation.get("animation_length")
    if not isinstance(length, (int, float)) or isinstance(length, bool) or length <= 0:
        _fail(f"animation {animation_name} has invalid animation_length")
    loop = animation.get("loop") is True
    pose: Dict[str, Dict[str, Vec3]] = {}
    for bone_name, channels in animation.get("bones", {}).items():
        if not isinstance(channels, dict):
            _fail(f"animation {animation_name} bone {bone_name} must contain channels")
        for channel_name, keyframes in channels.items():
            if channel_name not in {"position", "rotation", "scale"} or not isinstance(keyframes, dict):
                _fail(f"animation {animation_name} bone {bone_name} has malformed {channel_name} channel")
            pose.setdefault(bone_name, {})[channel_name] = sample_channel(
                keyframes, time, float(length), loop, f"animation {animation_name} bone {bone_name} {channel_name}"
            )
    return pose


def _dot(left: Vec3, right: Vec3) -> float:
    return sum(left[index] * right[index] for index in range(3))


def _cross(left: Vec3, right: Vec3) -> Vec3:
    return (left[1] * right[2] - left[2] * right[1], left[2] * right[0] - left[0] * right[2], left[0] * right[1] - left[1] * right[0])


def _subtract(left: Vec3, right: Vec3) -> Vec3:
    return tuple(left[index] - right[index] for index in range(3))  # type: ignore[return-value]


def _normalize(vector: Vec3) -> Vec3:
    length = math.sqrt(_dot(vector, vector))
    return tuple(value / length for value in vector)  # type: ignore[return-value]


def camera_for(name: str) -> Tuple[Vec3, Vec3, Vec3]:
    fixed = {
        "front": ((0, 0, -1), (1, 0, 0), (0, 1, 0)),
        "right": ((1, 0, 0), (0, 0, 1), (0, 1, 0)),
        "back": ((0, 0, 1), (-1, 0, 0), (0, 1, 0)),
        "top": ((0, 1, 0), (1, 0, 0), (0, 0, -1)),
    }
    if name in fixed:
        return fixed[name]  # type: ignore[return-value]
    if name == "three_quarter":
        outward = _normalize((0.9, 0.62, -1.0))
        screen_x = _normalize(_cross((0, 1, 0), outward))
        screen_y = _normalize(_cross(outward, screen_x))
        return outward, screen_x, screen_y
    _fail(f"unknown camera view {name}")


_FACE_VERTICES = {
    "north": (0, 3, 2, 1), "east": (1, 2, 6, 5), "south": (4, 5, 6, 7),
    "west": (0, 4, 7, 3), "up": (3, 7, 6, 2), "down": (0, 1, 5, 4),
}
_FACE_UV_CORNERS = {
    "north": (3, 0, 1, 2), "east": (3, 0, 1, 2),
    "south": (2, 3, 0, 1), "west": (2, 3, 0, 1),
    "up": (0, 3, 2, 1), "down": (3, 2, 1, 0),
}


def _cube_vertices(cube: Mapping[str, Any]) -> Tuple[Vec3, ...]:
    origin = _vec3(cube.get("origin"), f"cube {cube.get('name', '<unnamed>')} origin")
    size = _vec3(cube.get("size"), f"cube {cube.get('name', '<unnamed>')} size")
    if any(value <= 0 for value in size):
        _fail(f"cube {cube.get('name', '<unnamed>')} size must be positive")
    x, y, z = origin
    sx, sy, sz = size
    return ((x, y, z), (x + sx, y, z), (x + sx, y + sy, z), (x, y + sy, z),
            (x, y, z + sz), (x + sx, y, z + sz), (x + sx, y + sy, z + sz), (x, y + sy, z + sz))


def _cube_matrix(cube: Mapping[str, Any]) -> Matrix:
    rotation = _vec3(cube.get("rotation", (0, 0, 0)), f"cube {cube.get('name', '<unnamed>')} rotation")
    if rotation == (0, 0, 0):
        return identity_matrix()
    origin, size = _vec3(cube.get("origin"), "cube origin"), _vec3(cube.get("size"), "cube size")
    pivot = _vec3(cube.get("pivot", tuple(origin[i] + size[i] / 2 for i in range(3))), "cube pivot")
    return _pivot_matrix(pivot, (0, 0, 0), rotation, (1, 1, 1))


def _project(point: Vec3, camera: Tuple[Vec3, Vec3, Vec3], canvas_size: Tuple[int, int], scale: float, center: Vec3) -> Tuple[float, float]:
    _, screen_x, screen_y = camera
    relative = _subtract(point, center)
    return (canvas_size[0] / 2 + _dot(relative, screen_x) * scale, canvas_size[1] / 2 - _dot(relative, screen_y) * scale)


def _face_uv_coordinates(face_name: str, uv: Mapping[str, Any]) -> Tuple[Tuple[float, float], ...]:
    try:
        start = _vec3([*uv["uv"], 0], "face uv")[:2]
        size = _vec3([*uv["uv_size"], 0], "face uv_size")[:2]
    except (KeyError, TypeError):
        _fail("cube face must define explicit uv and uv_size")
    u0, v0 = start
    du, dv = size
    corners = ((u0, v0), (u0 + du, v0), (u0 + du, v0 + dv), (u0, v0 + dv))
    return tuple(corners[index] for index in _FACE_UV_CORNERS[face_name])


def _raster_triangle(
    image: Image.Image,
    texture: Image.Image,
    z_buffer: list[list[float]],
    rank_buffer: list[list[int]],
    translucent: Dict[Tuple[int, int], list[Tuple[float, int, Tuple[int, int, int, int]]]],
    projected: Sequence[Tuple[float, float]],
    depths: Sequence[float],
    texture_uvs: Sequence[Tuple[float, float]],
    rank: int,
) -> None:
    (x0, y0), (x1, y1), (x2, y2) = projected
    denominator = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2)
    if abs(denominator) < 1e-9:
        return
    minimum_x = max(0, math.floor(min(x0, x1, x2)))
    maximum_x = min(image.width, math.ceil(max(x0, x1, x2)))
    minimum_y = max(0, math.floor(min(y0, y1, y2)))
    maximum_y = min(image.height, math.ceil(max(y0, y1, y2)))
    pixels = image.load()
    texture_pixels = texture.load()
    epsilon = 1e-9
    for y in range(minimum_y, maximum_y):
        sample_y = y + 0.5
        for x in range(minimum_x, maximum_x):
            sample_x = x + 0.5
            first = ((y1 - y2) * (sample_x - x2) + (x2 - x1) * (sample_y - y2)) / denominator
            second = ((y2 - y0) * (sample_x - x2) + (x0 - x2) * (sample_y - y2)) / denominator
            third = 1.0 - first - second
            if first < -epsilon or second < -epsilon or third < -epsilon:
                continue
            depth = first * depths[0] + second * depths[1] + third * depths[2]
            if depth < z_buffer[y][x] - epsilon:
                continue
            if abs(depth - z_buffer[y][x]) <= epsilon and rank <= rank_buffer[y][x]:
                continue
            u = first * texture_uvs[0][0] + second * texture_uvs[1][0] + third * texture_uvs[2][0]
            v = first * texture_uvs[0][1] + second * texture_uvs[1][1] + third * texture_uvs[2][1]
            texture_x = min(texture.width - 1, max(0, math.floor(u)))
            texture_y = min(texture.height - 1, max(0, math.floor(v)))
            colour = texture_pixels[texture_x, texture_y]
            if colour[3] == 0:
                continue
            if colour[3] < 255:
                translucent.setdefault((x, y), []).append((depth, rank, colour))
                continue
            pixels[x, y] = colour
            z_buffer[y][x] = depth
            rank_buffer[y][x] = rank


def render_cubes(cubes: Iterable[Tuple[Mapping[str, Any], Matrix]], texture: Image.Image,
                 camera: Tuple[Vec3, Vec3, Vec3], canvas_size: Tuple[int, int] = CANVAS_SIZE,
                 pixels_per_unit: float = PIXELS_PER_UNIT, center_world: Vec3 = MODEL_CENTER) -> Image.Image:
    outward = camera[0]
    image = Image.new("RGBA", canvas_size, (0, 0, 0, 0))
    z_buffer = [[-math.inf] * canvas_size[0] for _ in range(canvas_size[1])]
    rank_buffer = [[-1] * canvas_size[0] for _ in range(canvas_size[1])]
    translucent: Dict[Tuple[int, int], list[Tuple[float, int, Tuple[int, int, int, int]]]] = {}
    rank = 0
    for cube, bone_matrix in cubes:
        matrix = _multiply(bone_matrix, _cube_matrix(cube))
        vertices = tuple(transform_point(matrix, point) for point in _cube_vertices(cube))
        uv_faces = cube.get("uv")
        if not isinstance(uv_faces, dict):
            _fail(f"cube {cube.get('name', '<unnamed>')} must use explicit face UVs")
        for face_name in FACE_NAMES:
            if face_name not in uv_faces:
                _fail(f"cube {cube.get('name', '<unnamed>')} is missing {face_name} UV")
            points = tuple(vertices[index] for index in _FACE_VERTICES[face_name])
            normal = _cross(_subtract(points[1], points[0]), _subtract(points[2], points[1]))
            if _dot(normal, outward) <= 1e-9:
                continue
            projected = tuple(_project(point, camera, canvas_size, pixels_per_unit, center_world) for point in points)
            depths = tuple(_dot(point, outward) for point in points)
            texture_uvs = _face_uv_coordinates(face_name, uv_faces[face_name])
            for triangle in ((0, 1, 2), (0, 2, 3)):
                _raster_triangle(
                    image, texture, z_buffer, rank_buffer, translucent,
                    tuple(projected[index] for index in triangle),
                    tuple(depths[index] for index in triangle),
                    tuple(texture_uvs[index] for index in triangle), rank,
                )
                rank += 1
    pixels = image.load()
    epsilon = 1e-9
    for (x, y), fragments in translucent.items():
        opaque_depth = z_buffer[y][x]
        opaque_rank = rank_buffer[y][x]
        visible = [
            fragment for fragment in fragments
            if fragment[0] > opaque_depth + epsilon
            or (abs(fragment[0] - opaque_depth) <= epsilon and fragment[1] > opaque_rank)
        ]
        destination = pixels[x, y]
        for _depth, _rank, source in sorted(visible, key=lambda item: (item[0], item[1])):
            source_alpha = source[3] / 255.0
            destination_alpha = destination[3] / 255.0
            output_alpha = source_alpha + destination_alpha * (1.0 - source_alpha)
            if output_alpha == 0:
                destination = (0, 0, 0, 0)
            else:
                destination = (
                    *(round((source[channel] * source_alpha + destination[channel] * destination_alpha * (1.0 - source_alpha)) / output_alpha) for channel in range(3)),
                    round(output_alpha * 255),
                )
        pixels[x, y] = destination
    return image


def _scene_cubes(assets: Mapping[str, Any], animation_name: Optional[str], time: float):
    transforms = build_bone_transforms(assets["bones"], _pose_for(assets, animation_name, time))
    for bone in assets["bones"]:
        for cube in bone.get("cubes", []):
            if not isinstance(cube, dict):
                _fail(f"bone {bone['name']} contains a malformed cube")
            yield cube, transforms[bone["name"]]


def render_model(assets: Mapping[str, Any], view: str, animation_name: Optional[str] = None, time: float = 0) -> Image.Image:
    return render_cubes(_scene_cubes(assets, animation_name, time), assets["texture"], camera_for(view))


def _png_bytes(image: Image.Image) -> bytes:
    buffer = BytesIO()
    image.save(buffer, format="PNG", optimize=False, compress_level=9)
    return buffer.getvalue()


def _stage_bytes(path: Path, contents: bytes, role: str) -> Path:
    path.parent.mkdir(parents=True, exist_ok=True)
    descriptor, temporary_name = tempfile.mkstemp(
        prefix=f".{path.name}.{role}.", suffix=".tmp", dir=str(path.parent)
    )
    temporary = Path(temporary_name)
    try:
        with os.fdopen(descriptor, "wb") as handle:
            descriptor = -1
            handle.write(contents)
            handle.flush()
            os.fsync(handle.fileno())
        return temporary
    except BaseException as primary_error:
        cleanup_failures = []
        if descriptor != -1:
            try:
                os.close(descriptor)
            except BaseException as error:
                cleanup_failures.append((f"file_descriptor={descriptor}", error))
        try:
            temporary.unlink(missing_ok=True)
        except BaseException as error:
            cleanup_failures.append((str(temporary), error))
        if cleanup_failures:
            _append_cleanup_details(primary_error, cleanup_failures)
        raise primary_error.with_traceback(primary_error.__traceback__)


def _cleanup_paths(paths: Iterable[Path]) -> list[tuple[str, BaseException]]:
    failures = []
    for path in paths:
        try:
            path.unlink(missing_ok=True)
        except BaseException as error:
            failures.append((str(path), error))
    return failures


def _cleanup_details(failures: Sequence[tuple[str, BaseException]]) -> str:
    return "; ".join(
        f"path={path}, cleanup_error={type(error).__name__}: {error}"
        for path, error in failures
    )


def _append_cleanup_details(primary_error: BaseException, failures: Sequence[tuple[str, BaseException]]) -> None:
    primary_error.args = (
        f"{primary_error}; cleanup incomplete: {_cleanup_details(failures)}",
        *primary_error.args[1:],
    )


def _publish_transaction(payloads: Sequence[Tuple[Path, bytes]]) -> None:
    """Publish all review PNGs together or restore every previous byte."""

    targets = [target for target, _contents in payloads]
    if len(targets) != len(set(targets)):
        raise ValueError("review transaction targets must be unique")
    candidates: Dict[Path, Path] = {}
    backups: Dict[Path, Optional[Path]] = {}
    published: list[Path] = []
    retained_backups: set[Path] = set()
    operation_error: Optional[BaseException] = None
    try:
        for target, contents in payloads:
            candidates[target] = _stage_bytes(target, contents, "candidate")
        for target in targets:
            backups[target] = _stage_bytes(target, target.read_bytes(), "backup") if target.is_file() else None
        contents_by_target = dict(payloads)
        for target in targets:
            os.replace(candidates[target], target)
            published.append(target)
            if target.read_bytes() != contents_by_target[target]:
                raise OSError(f"review publish verification failed: {target}")
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
            chain: BaseException = publish_error
            for _target, _backup, rollback_error in rollback_failures:
                rollback_error.__cause__ = chain
                chain = rollback_error
            operation_error = RuntimeError(
                "review transaction rollback failed after "
                f"publish_error={type(publish_error).__name__}: {publish_error}; {details}"
            )
            operation_error.__cause__ = chain
        else:
            operation_error = publish_error

    cleanup_failures = _cleanup_paths(
        temporary
        for temporary in (*candidates.values(), *backups.values())
        if temporary is not None and temporary not in retained_backups
    )
    if operation_error is not None:
        if cleanup_failures:
            _append_cleanup_details(operation_error, cleanup_failures)
        raise operation_error.with_traceback(operation_error.__traceback__)
    if cleanup_failures:
        raise RuntimeError(
            "review images published, cleanup incomplete: " + _cleanup_details(cleanup_failures)
        )


def _candidate_layout(
    images: Mapping[str, Image.Image], content_size: Tuple[int, int]
) -> Tuple[Dict[str, Image.Image], float]:
    cropped: Dict[str, Image.Image] = {}
    for name, image in images.items():
        bounds = image.getchannel("A").getbbox()
        if bounds is None:
            _fail(f"candidate review image {name} is empty")
        cropped[name] = image.crop(bounds)
    scale = min(
        content_size[0] / max(image.width for image in cropped.values()),
        content_size[1] / max(image.height for image in cropped.values()),
    )
    fitted = {
        name: image.resize(
            (max(1, round(image.width * scale)), max(1, round(image.height * scale))),
            Image.Resampling.NEAREST,
        )
        for name, image in cropped.items()
    }
    return fitted, scale


def _contact_sheet(images: Mapping[str, Image.Image], concept_path: Optional[Path]) -> Image.Image:
    sheet = Image.new("RGBA", CANVAS_SIZE, (0, 0, 0, 0))
    draw = ImageDraw.Draw(sheet)
    entries = [
        ("candidate_front.png", "FRONT"), ("candidate_right.png", "RIGHT"),
        ("candidate_back.png", "BACK"), ("candidate_top.png", "TOP"),
        ("candidate_idle.png", "IDLE 0.8 s"), ("candidate_walk.png", "WALK 0.1 s"),
        ("candidate_attack.png", "ATTACK 0.225 s"), (None, "CONCEPT"),
    ]
    columns = 3
    cell_w, cell_h = 256, 256
    content_size = (cell_w - 12, cell_h - 38)
    candidate_names = [name for name, _label in entries if name is not None]
    fitted_candidates, _scale = _candidate_layout(
        {name: images[name] for name in candidate_names}, content_size
    )
    for index, (name, label) in enumerate(entries):
        left, top = index % columns * cell_w, index // columns * cell_h
        draw.rounded_rectangle((left + 4, top + 4, left + cell_w - 5, top + cell_h - 5), 7, fill=(24, 23, 31, 235), outline=(87, 82, 99, 255))
        source: Optional[Image.Image]
        if name is not None:
            source = fitted_candidates[name]
        elif concept_path is not None and concept_path.is_file():
            try:
                with Image.open(concept_path) as concept:
                    concept.load()
                    source = concept.convert("RGBA")
            except (OSError, UnidentifiedImageError) as exc:
                _fail(f"concept image is not a valid image ({concept_path}): {exc}")
        else:
            source = None
        if source is not None:
            copy = source.copy()
            if name is None:
                copy.thumbnail(content_size, CONCEPT_RESAMPLING)
            x = left + (cell_w - copy.width) // 2
            y = top + 30 + (cell_h - 34 - copy.height) // 2
            sheet.alpha_composite(copy, (x, y))
        else:
            draw.text((left + 60, top + cell_h // 2), "NOT AVAILABLE", fill=(165, 160, 175, 255))
        draw.text((left + 10, top + 11), label, fill=(230, 226, 236, 255))
    return sheet


def _resolve_concept_path(root: Path, concept_path: Optional[Path]) -> Optional[Path]:
    if concept_path is None:
        return None
    path = Path(concept_path)
    if not path.is_absolute():
        path = root / path
    path = path.resolve()
    if not path.is_file():
        _fail(f"concept image is missing: {path}")
    try:
        with Image.open(path) as concept:
            concept.load()
    except (OSError, UnidentifiedImageError) as exc:
        _fail(f"concept image is not a valid image ({path}): {exc}")
    return path


def render_review_set(root: Path, output_root: Optional[Path] = None, concept_path: Optional[Path] = DEFAULT_CONCEPT) -> Dict[str, Path]:
    root = Path(root).resolve()
    concept_path = _resolve_concept_path(root, concept_path)
    assets = load_assets(root)
    output_root = Path(output_root) if output_root is not None else Path(root) / REVIEW_RELATIVE
    images = {
        "candidate_front.png": render_model(assets, "front"),
        "candidate_right.png": render_model(assets, "right"),
        "candidate_back.png": render_model(assets, "back"),
        "candidate_top.png": render_model(assets, "top"),
        "candidate_idle.png": render_model(assets, "three_quarter", "animation.corrupted_silverfish.idle", 0.8),
        "candidate_walk.png": render_model(assets, "three_quarter", "animation.corrupted_silverfish.walk", 0.1),
        "candidate_attack.png": render_model(assets, "three_quarter", "animation.corrupted_silverfish.attack", 0.225),
    }
    images["candidate_contact_sheet.png"] = _contact_sheet(images, concept_path)
    paths: Dict[str, Path] = {}
    payloads = []
    for name in OUTPUT_NAMES:
        path = output_root / name
        payloads.append((path, _png_bytes(images[name])))
        paths[name] = path
    _publish_transaction(tuple(payloads))
    return paths


def _parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument("--output", type=Path)
    parser.add_argument("--concept", type=Path, default=DEFAULT_CONCEPT)
    parser.add_argument("--no-concept", action="store_true")
    return parser


def main(argv: Optional[Sequence[str]] = None) -> int:
    args = _parser().parse_args(argv)
    try:
        paths = render_review_set(args.root, args.output, None if args.no_concept else args.concept)
    except RenderFailure as exc:
        print(f"RENDER_FAILED: {exc}", file=os.sys.stderr)
        return 1
    except Exception as exc:
        print(f"RENDER_FAILED: unexpected renderer error: {exc}", file=os.sys.stderr)
        return 1
    print(f"RENDER=PASS;TARGET=V3;FILES={len(paths)};SIZE=768x768")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
