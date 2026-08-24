"""Build the curated high-detail Web Cave Spider cube model."""

from __future__ import annotations

from dataclasses import dataclass
import argparse
import base64
import hashlib
import io
import json
import math
import os
from pathlib import Path
import tempfile
from typing import Iterable, Mapping, Sequence
from uuid import UUID, uuid5

from PIL import Image


Vec3 = tuple[float, float, float]
FACES = ("north", "east", "south", "west", "up", "down")
SIZE = 256
GUTTER = 1
NAMESPACE = UUID("983510ff-a1ee-50f0-8bc0-e50708423216")
ROOT = Path(__file__).resolve().parents[2]
EXPORT = ROOT / "Modelle" / "Exports" / "web_cave_spider_v2"
MODEL = EXPORT / "blockbench" / "Web Cave Spider v2 Curated.bbmodel"
TEXTURE = EXPORT / "textures" / "web_cave_spider.png"
GLOW = EXPORT / "textures" / "web_cave_spider_glowmask.png"


@dataclass(frozen=True)
class Bone:
    name: str
    parent: str | None
    pivot: Vec3


@dataclass(frozen=True)
class Cube:
    name: str
    bone: str
    origin: Vec3
    size: Vec3
    material: str
    rotation: Vec3 = (0.0, 0.0, 0.0)


@dataclass(frozen=True)
class Payloads:
    bbmodel: bytes
    texture: bytes
    glowmask: bytes


BONES = (
    Bone("root", None, (0, 0, 0)),
    Bone("head", "root", (0, 10, -7)),
    Bone("thorax", "root", (0, 10, 0)),
    Bone("abdomen", "thorax", (0, 10, 6)),
) + tuple(
    Bone(f"web_leg_{index}", "thorax", ((-1 if index < 4 else 1) * 4.5, 9, (-6, -2, 3, 8)[index % 4]))
    for index in range(8)
)


def cube(name: str, bone: str, origin: Vec3, size: Vec3, material: str, rotation: Vec3 = (0, 0, 0)) -> Cube:
    return Cube(name, bone, origin, size, material, rotation)


def _build_cubes() -> tuple[Cube, ...]:
    result = [
        cube("head_core", "head", (-4.5, 7, -11), (9, 7, 7), "chitin"),
        cube("head_top_plate", "head", (-4.9, 13.3, -10.7), (9.8, 1.2, 6.4), "chitin_light"),
        cube("head_brow", "head", (-4.7, 11.2, -11.6), (9.4, 2.2, 1.1), "chitin_dark"),
        cube("head_face_plate", "head", (-3.8, 8.1, -11.8), (7.6, 3.4, 1.0), "chitin_mid"),
        cube("mandible_left", "head", (-3.8, 5.5, -12.0), (2.1, 3.4, 2.3), "fang", (8, 0, -8)),
        cube("mandible_right", "head", (1.7, 5.5, -12.0), (2.1, 3.4, 2.3), "fang", (8, 0, 8)),
        cube("fang_left", "head", (-3.1, 4.8, -12.7), (1.0, 2.6, 1.1), "web", (14, 0, -6)),
        cube("fang_right", "head", (2.1, 4.8, -12.7), (1.0, 2.6, 1.1), "web", (14, 0, 6)),
        cube("thorax_core", "thorax", (-5.0, 7.0, -4.5), (10, 7.5, 9), "chitin"),
        cube("thorax_top", "thorax", (-5.4, 13.5, -4.2), (10.8, 1.3, 8.4), "chitin_light"),
        cube("thorax_band_front", "thorax", (-5.2, 9.0, -4.9), (10.4, 2.0, 1.0), "chitin_mid"),
        cube("thorax_band_rear", "thorax", (-5.2, 9.2, 3.9), (10.4, 1.8, 1.0), "chitin_dark"),
        cube("abdomen_core", "abdomen", (-6.3, 6.2, 3.5), (12.6, 9.5, 14.5), "chitin_mid"),
        cube("abdomen_top", "abdomen", (-6.0, 14.8, 4.0), (12.0, 1.4, 13.4), "web"),
        cube("abdomen_left_shell", "abdomen", (-6.8, 8.0, 5.0), (1.2, 6.8, 11.5), "chitin_light"),
        cube("abdomen_right_shell", "abdomen", (5.6, 8.0, 5.0), (1.2, 6.8, 11.5), "chitin_light"),
        cube("abdomen_rear_cap", "abdomen", (-5.4, 7.5, 17.2), (10.8, 7.5, 1.5), "web"),
        cube("abdomen_lower", "abdomen", (-5.1, 5.4, 5.0), (10.2, 2.0, 11.5), "chitin_dark"),
        cube("spinneret_left", "abdomen", (-3.0, 5.1, 18.2), (2.0, 2.2, 2.6), "fang"),
        cube("spinneret_right", "abdomen", (1.0, 5.1, 18.2), (2.0, 2.2, 2.6), "fang"),
    ]

    eye_positions = ((-3.1, 10.1), (-1.2, 10.7), (1.2, 10.7), (3.1, 10.1), (-2.7, 8.7), (-0.9, 9.1), (0.9, 9.1), (2.7, 8.7))
    for index, (x, y) in enumerate(eye_positions):
        size = 1.15 if index in (1, 2, 5, 6) else 0.9
        result.append(cube(f"eye_{index}", "head", (x - size / 2, y - size / 2, -12.25), (size, size, 0.75), "eye"))

    web_accents = (
        ("web_spine", (-0.45, 15.8, 4.6), (0.9, 0.55, 12.0)),
        ("web_cross_front", (-5.4, 15.85, 7.0), (10.8, 0.5, 0.8)),
        ("web_cross_mid", (-5.6, 15.9, 10.6), (11.2, 0.5, 0.8)),
        ("web_cross_rear", (-5.2, 15.85, 14.2), (10.4, 0.5, 0.8)),
        ("web_diag_left_front", (-5.4, 15.9, 5.0), (1.0, 0.5, 5.0)),
        ("web_diag_right_front", (4.4, 15.9, 5.0), (1.0, 0.5, 5.0)),
        ("web_diag_left_rear", (-4.6, 15.9, 11.5), (1.0, 0.5, 5.0)),
        ("web_diag_right_rear", (3.6, 15.9, 11.5), (1.0, 0.5, 5.0)),
        ("rear_web_vertical", (-0.5, 8.0, 18.3), (1.0, 5.2, 0.5)),
        ("rear_web_horizontal", (-4.3, 10.2, 18.35), (8.6, 0.8, 0.5)),
        ("thorax_web_left", (-4.2, 14.7, -1.0), (3.0, 0.45, 0.8)),
        ("thorax_web_right", (1.2, 14.7, -1.0), (3.0, 0.45, 0.8)),
        ("thorax_web_centre", (-0.5, 14.65, -3.2), (1.0, 0.5, 6.0)),
    )
    result.extend(cube(name, "abdomen" if name.startswith(("web_", "rear_")) else "thorax", origin, size, "web") for name, origin, size in web_accents)

    stations = (-6.0, -2.0, 3.0, 8.0)
    yaw = (28.0, 12.0, -10.0, -26.0)
    for index in range(8):
        side = -1 if index < 4 else 1
        lane = index % 4
        bone = f"web_leg_{index}"
        z = stations[lane]
        x0 = -10.8 if side < 0 else 4.8
        x1 = -16.0 if side < 0 else 10.0
        x2 = -20.2 if side < 0 else 15.2
        z_shift = (-2.0, -0.8, 0.8, 2.0)[lane]
        result.extend((
            cube(f"{bone}_upper", bone, (x0, 8.2, z - 1.2), (6.0, 2.4, 2.4), "chitin", (0, side * yaw[lane], side * -9)),
            cube(f"{bone}_upper_band", bone, (x0 + (1.0 if side < 0 else 3.8), 8.0, z - 1.35), (1.2, 2.8, 2.7), "web", (0, side * yaw[lane], side * -9)),
            cube(f"{bone}_knee", bone, (x1, 5.8, z + z_shift - 1.35), (5.8, 2.6, 2.7), "chitin_mid", (0, side * yaw[lane], side * 15)),
            cube(f"{bone}_knee_band", bone, (x1 + (1.6 if side < 0 else 3.0), 5.6, z + z_shift - 1.5), (1.0, 3.0, 3.0), "web", (0, side * yaw[lane], side * 15)),
            cube(f"{bone}_lower", bone, (x2, 2.8, z + z_shift * 1.6 - 1.1), (5.2, 2.1, 2.2), "chitin_dark", (0, side * yaw[lane], side * 20)),
            cube(f"{bone}_foot", bone, (x2 + (-2.4 if side < 0 else 3.4), 1.3, z + z_shift * 1.8 - 0.9), (3.6, 1.5, 1.8), "fang", (0, side * yaw[lane], side * 8)),
        ))
    return tuple(result)


CUBES = _build_cubes()


PALETTES: Mapping[str, tuple[tuple[int, int, int], ...]] = {
    "chitin": ((10, 11, 15), (22, 24, 31), (39, 42, 52), (63, 67, 81)),
    "chitin_dark": ((5, 6, 9), (13, 14, 20), (25, 27, 35), (44, 46, 58)),
    "chitin_mid": ((16, 17, 22), (34, 35, 43), (58, 59, 69), (88, 88, 99)),
    "chitin_light": ((43, 44, 50), (75, 76, 83), (111, 112, 119), (153, 154, 160)),
    "web": ((65, 66, 73), (116, 117, 125), (176, 178, 185), (232, 236, 240)),
    "fang": ((35, 36, 42), (82, 84, 91), (144, 147, 153), (222, 225, 229)),
    "eye": ((7, 28, 43), (12, 73, 105), (37, 156, 213), (164, 235, 255)),
}


def _uuid(kind: str, name: str) -> str:
    return str(uuid5(NAMESPACE, f"{kind}:{name}"))


def _face_size(item: Cube, face: str) -> tuple[int, int]:
    x, y, z = item.size
    a, b = {"north": (x, y), "south": (x, y), "east": (z, y), "west": (z, y), "up": (x, z), "down": (x, z)}[face]
    return max(1, math.ceil(a * 2)), max(1, math.ceil(b * 2))


def pack_uvs(items: Iterable[Cube] = CUBES) -> dict[tuple[str, str], tuple[int, int, int, int]]:
    islands = [(item.name, face, *_face_size(item, face)) for item in items for face in FACES]
    islands.sort(key=lambda value: (-value[3], -value[2], value[0], value[1]))
    result = {}
    x = y = shelf = 0
    for name, face, width, height in islands:
        if x and x + width > SIZE:
            y += shelf + GUTTER
            x = shelf = 0
        if y + height > SIZE:
            raise ValueError(f"Spider texture overflow at {name}/{face}")
        result[(name, face)] = (x, y, x + width, y + height)
        x += width + GUTTER
        shelf = max(shelf, height)
    return result


def _png(image: Image.Image) -> bytes:
    output = io.BytesIO()
    image.save(output, format="PNG", optimize=False, compress_level=9)
    return output.getvalue()


def _paint(uvs) -> tuple[bytes, bytes]:
    texture = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    glow = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    pixels, glow_pixels = texture.load(), glow.load()
    face_light = {"up": 1.12, "down": 0.56, "north": 0.96, "south": 0.78, "east": 0.72, "west": 0.86}
    for item in CUBES:
        palette = PALETTES[item.material]
        for face in FACES:
            left, top, right, bottom = uvs[(item.name, face)]
            digest = hashlib.sha256(f"{item.name}/{face}".encode()).digest()
            for py in range(top, bottom):
                for px in range(left, right):
                    lx, ly = px - left, py - top
                    edge = lx in (0, right - left - 1) or ly in (0, bottom - top - 1)
                    noise = digest[(lx * 11 + ly * 7) % 32]
                    shade = 0 if edge else 1 + (noise > 145) + (noise > 225)
                    if item.material == "web" and (lx + ly + digest[0]) % 4 == 0:
                        shade = min(3, shade + 1)
                    colour = palette[shade]
                    pixels[px, py] = tuple(min(255, round(channel * face_light[face])) for channel in colour) + (255,)
            if item.material == "eye":
                cx, cy = (left + right) // 2, (top + bottom) // 2
                pixels[cx, cy] = PALETTES["eye"][3] + (255,)
                glow_pixels[cx, cy] = PALETTES["eye"][3] + (255,)
                if cx + 1 < right:
                    glow_pixels[cx + 1, cy] = PALETTES["eye"][2] + (255,)
    return _png(texture), _png(glow)


def _outliner() -> list[dict]:
    owned = {bone.name: [] for bone in BONES}
    descendants = {bone.name: [] for bone in BONES}
    for item in CUBES:
        owned[item.bone].append(_uuid("element", item.name))
    for bone in BONES:
        if bone.parent:
            descendants[bone.parent].append(bone.name)
    def node(name: str) -> dict:
        bone = next(value for value in BONES if value.name == name)
        return {"name": name, "origin": list(bone.pivot), "uuid": _uuid("group", name), "children": owned[name] + [node(child) for child in descendants[name]]}
    return [node("root")]


def _frame(animation: str, bone: str, time: float, rotation: Vec3) -> dict:
    return {"channel": "rotation", "data_points": [{axis: f"{value:g}" for axis, value in zip(("x", "y", "z"), rotation)}], "uuid": _uuid("keyframe", f"{animation}:{bone}:{time:g}"), "time": time, "color": -1, "interpolation": "linear"}


def _animation(label: str, length: float, tracks: Mapping[str, Sequence[tuple[float, Vec3]]]) -> dict:
    name = f"animation.web_cave_spider.{label}"
    return {
        "uuid": _uuid("animation", name), "name": name, "path": "web_cave_spider.animation.json", "loop": "loop", "override": False,
        "snapping": 20, "length": length, "selected_item": None, "anim_time_update": "", "blend_weight": "", "start_delay": "", "loop_delay": "",
        "animators": {_uuid("group", bone): {"name": bone, "type": "bone", "rotation_global": False, "quaternion_interpolation": False, "keyframes": [_frame(name, bone, time, value) for time, value in values]} for bone, values in tracks.items()},
    }


def _animations() -> list[dict]:
    idle = _animation("idle", 1.6, {"abdomen": ((0, (0, 0, -1.5)), (0.8, (0, 0, 1.5)), (1.6, (0, 0, -1.5)))})
    tracks = {}
    for index in range(8):
        sign = 1 if index % 2 == 0 else -1
        tracks[f"web_leg_{index}"] = ((0, (0, 30 * sign, 14 * sign)), (0.4, (0, -30 * sign, -14 * sign)), (0.8, (0, 30 * sign, 14 * sign)))
    return [idle, _animation("walk", 0.8, tracks)]


def build_payloads() -> Payloads:
    uvs = pack_uvs()
    texture, glowmask = _paint(uvs)
    elements = []
    for item in CUBES:
        element = {
            "name": item.name, "box_uv": False, "from": list(item.origin), "to": [item.origin[i] + item.size[i] for i in range(3)],
            "origin": [item.origin[i] + item.size[i] / 2 for i in range(3)], "faces": {face: {"uv": list(uvs[(item.name, face)]), "texture": 0} for face in FACES},
            "type": "cube", "uuid": _uuid("element", item.name), "bone": item.bone,
        }
        if item.rotation != (0, 0, 0):
            element["rotation"] = list(item.rotation)
        elements.append(element)
    document = {
        "meta": {"format_version": "5.0", "model_format": "free", "box_uv": False}, "name": "web_cave_spider_v2_curated",
        "resolution": {"width": SIZE, "height": SIZE}, "elements": elements, "outliner": _outliner(),
        "textures": [{"name": "web_cave_spider.png", "id": "0", "particle": False, "render_mode": "default", "visible": True, "mode": "bitmap", "saved": True, "uuid": _uuid("texture", "web_cave_spider"), "source": "data:image/png;base64," + base64.b64encode(texture).decode("ascii")}],
        "animations": _animations(),
    }
    return Payloads((json.dumps(document, ensure_ascii=False, indent=2) + "\n").encode(), texture, glowmask)


def _write(path: Path, data: bytes) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    descriptor, name = tempfile.mkstemp(prefix=f".{path.name}.", suffix=".candidate", dir=str(path.parent))
    candidate = Path(name)
    try:
        with os.fdopen(descriptor, "wb") as handle:
            descriptor = -1
            handle.write(data)
        os.replace(candidate, path)
    finally:
        if descriptor != -1:
            os.close(descriptor)
        candidate.unlink(missing_ok=True)


def write_outputs(model: Path = MODEL, texture: Path = TEXTURE, glow: Path = GLOW) -> Payloads:
    payloads = build_payloads()
    _write(model, payloads.bbmodel)
    _write(texture, payloads.texture)
    _write(glow, payloads.glowmask)
    return payloads


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--model", type=Path, default=MODEL)
    parser.add_argument("--texture", type=Path, default=TEXTURE)
    parser.add_argument("--glowmask", type=Path, default=GLOW)
    args = parser.parse_args(argv)
    try:
        write_outputs(args.model, args.texture, args.glowmask)
    except (OSError, ValueError) as exc:
        print(f"SPIDER_V2_FAILED: {exc}")
        return 1
    print(f"SPIDER_V2_PASS CUBES={len(CUBES)} BONES={len(BONES)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
