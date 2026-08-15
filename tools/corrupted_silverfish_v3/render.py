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


def _textured_face(texture: Image.Image, projected: Sequence[Tuple[float, float]], uv: Mapping[str, Any]) -> Tuple[Image.Image, Tuple[int, int]]:
    try:
        start = _vec3([*uv["uv"], 0], "face uv")[:2]
        size = _vec3([*uv["uv_size"], 0], "face uv_size")[:2]
    except (KeyError, TypeError):
        _fail("cube face must define explicit uv and uv_size")
    min_x, max_x = math.floor(min(p[0] for p in projected)), math.ceil(max(p[0] for p in projected))
    min_y, max_y = math.floor(min(p[1] for p in projected)), math.ceil(max(p[1] for p in projected))
    width, height = max_x - min_x + 1, max_y - min_y + 1
    p0, p1, _, p3 = projected
    ax, ay = p1[0] - p0[0], p1[1] - p0[1]
    bx, by = p3[0] - p0[0], p3[1] - p0[1]
    determinant = ax * by - ay * bx
    if abs(determinant) < 1e-9:
        return Image.new("RGBA", (1, 1)), (min_x, min_y)
    ds_dx, ds_dy = by / determinant, -bx / determinant
    dt_dx, dt_dy = -ay / determinant, ax / determinant
    local_zero_x, local_zero_y = min_x + 0.5 - p0[0], min_y + 0.5 - p0[1]
    u0, v0 = start
    du, dv = size
    coefficients = (
        du * ds_dx, du * ds_dy, u0 + du * (ds_dx * local_zero_x + ds_dy * local_zero_y),
        dv * dt_dx, dv * dt_dy, v0 + dv * (dt_dx * local_zero_x + dt_dy * local_zero_y),
    )
    tile = texture.transform((width, height), Image.Transform.AFFINE, coefficients, resample=Image.Resampling.NEAREST)
    mask = Image.new("L", (width, height), 0)
    ImageDraw.Draw(mask).polygon([(round(x - min_x), round(y - min_y)) for x, y in projected], fill=255)
    tile.putalpha(Image.composite(tile.getchannel("A"), Image.new("L", tile.size, 0), mask))
    return tile, (min_x, min_y)


def render_cubes(cubes: Iterable[Tuple[Mapping[str, Any], Matrix]], texture: Image.Image,
                 camera: Tuple[Vec3, Vec3, Vec3], canvas_size: Tuple[int, int] = CANVAS_SIZE,
                 pixels_per_unit: float = PIXELS_PER_UNIT, center_world: Vec3 = MODEL_CENTER) -> Image.Image:
    outward = camera[0]
    faces = []
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
            depth = sum(_dot(point, outward) for point in points) / 4
            faces.append((depth, projected, uv_faces[face_name]))
    image = Image.new("RGBA", canvas_size, (0, 0, 0, 0))
    for _, projected, uv in sorted(faces, key=lambda item: item[0]):
        tile, position = _textured_face(texture, projected, uv)
        image.alpha_composite(tile, position)
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


def _atomic_write(path: Path, contents: bytes) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    descriptor, temporary_name = tempfile.mkstemp(prefix=f".{path.name}.", suffix=".tmp", dir=str(path.parent))
    temporary = Path(temporary_name)
    try:
        with os.fdopen(descriptor, "wb") as handle:
            handle.write(contents)
            handle.flush()
            os.fsync(handle.fileno())
        os.replace(temporary, path)
    except BaseException:
        try:
            os.close(descriptor)
        except OSError:
            pass
        temporary.unlink(missing_ok=True)
        raise


def _contact_sheet(images: Mapping[str, Image.Image], concept_path: Optional[Path]) -> Image.Image:
    sheet = Image.new("RGBA", CANVAS_SIZE, (0, 0, 0, 0))
    draw = ImageDraw.Draw(sheet)
    entries = [
        ("candidate_front.png", "FRONT"), ("candidate_right.png", "RIGHT"),
        ("candidate_back.png", "BACK"), ("candidate_top.png", "TOP"),
        ("candidate_idle.png", "IDLE 0.8 s"), ("candidate_walk.png", "WALK 0.2 s"),
        ("candidate_attack.png", "ATTACK 0.225 s"), (None, "CONCEPT"),
    ]
    cell_w, cell_h = 192, 384
    for index, (name, label) in enumerate(entries):
        left, top = index % 4 * cell_w, index // 4 * cell_h
        draw.rounded_rectangle((left + 4, top + 4, left + cell_w - 5, top + cell_h - 5), 7, fill=(24, 23, 31, 235), outline=(87, 82, 99, 255))
        source: Optional[Image.Image]
        if name is not None:
            source = images[name]
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
            copy.thumbnail((cell_w - 16, cell_h - 42), CONCEPT_RESAMPLING if name is None else Image.Resampling.NEAREST)
            x = left + (cell_w - copy.width) // 2
            y = top + 27 + (cell_h - 35 - copy.height) // 2
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
        "candidate_walk.png": render_model(assets, "three_quarter", "animation.corrupted_silverfish.walk", 0.2),
        "candidate_attack.png": render_model(assets, "three_quarter", "animation.corrupted_silverfish.attack", 0.225),
    }
    images["candidate_contact_sheet.png"] = _contact_sheet(images, concept_path)
    paths: Dict[str, Path] = {}
    for name in OUTPUT_NAMES:
        path = output_root / name
        _atomic_write(path, _png_bytes(images[name]))
        paths[name] = path
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
