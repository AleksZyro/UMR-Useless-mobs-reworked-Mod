"""Build the curated Living Boss cuboid model from the approved Tripo reference."""

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
from typing import Dict, Iterable, Mapping, Sequence, Tuple
from uuid import UUID, uuid5

from PIL import Image


Vec3 = Tuple[float, float, float]
FACE_NAMES = ("north", "east", "south", "west", "up", "down")
TEXTURE_SIZE = 256
GUTTER = 1
UUID_NAMESPACE = UUID("4ac019b4-fc74-518d-88cb-dcd0fe2be52a")
PROJECT_ROOT = Path(__file__).resolve().parents[2]
EXPORT_ROOT = PROJECT_ROOT / "Modelle" / "Exports" / "living_boss_v1"
MODEL_PATH = EXPORT_ROOT / "blockbench" / "Living Boss Tripo Cuboids.bbmodel"
TEXTURE_PATH = EXPORT_ROOT / "textures" / "living_boss.png"
GLOWMASK_PATH = EXPORT_ROOT / "textures" / "living_boss_glowmask.png"


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
    category: str
    rotation: Vec3 = (0.0, 0.0, 0.0)


@dataclass(frozen=True)
class Payloads:
    bbmodel: bytes
    texture: bytes
    glowmask: bytes


BONES = (
    Bone("root", None, (0.0, 0.0, 0.0)),
    Bone("body", "root", (0.0, 10.0, 0.0)),
    Bone("head", "body", (0.0, 13.0, -10.0)),
    Bone("leg_front_left", "body", (-8.0, 8.0, -6.5)),
    Bone("leg_front_right", "body", (8.0, 8.0, -6.5)),
    Bone("leg_rear_left", "body", (-8.0, 8.0, 7.0)),
    Bone("leg_rear_right", "body", (8.0, 8.0, 7.0)),
    Bone("roots_body", "body", (0.0, 16.0, 0.0)),
)


def _cube(
    name: str,
    bone: str,
    origin: Vec3,
    size: Vec3,
    material: str,
    category: str,
    rotation: Vec3 = (0.0, 0.0, 0.0),
) -> Cube:
    return Cube(name, bone, origin, size, material, category, rotation)


def _build_cubes() -> Tuple[Cube, ...]:
    cubes = [
        _cube("body_core", "body", (-7, 8, -5), (14, 10, 15), "wood", "mass"),
        _cube("body_rear", "body", (-6, 9, 9), (12, 8, 7), "wood_dark", "mass"),
        _cube("body_chest", "body", (-6.5, 8, -9), (13, 10, 5), "stone", "mass"),
        _cube("body_belly", "body", (-5, 6, -3), (10, 4, 11), "wood_dark", "mass"),
        _cube("head_core", "head", (-4.5, 10, -15), (9, 8, 6), "stone", "mass"),
        _cube("head_brow", "head", (-5, 15, -15.5), (10, 3, 5.5), "stone_dark", "mass"),
        _cube("head_muzzle", "head", (-3.5, 9, -18), (7, 5, 3.5), "wood", "mass"),
        _cube("head_jaw", "head", (-3, 8, -17.5), (6, 2, 3), "stone_dark", "mass"),
        _cube("forehead_plate", "head", (-2.5, 14, -18.2), (5, 4, 0.9), "stone_light", "plate"),
        _cube("nose_plate", "head", (-1.5, 10.5, -18.6), (3, 3, 0.8), "stone_dark", "plate"),
        _cube("face_core_frame", "head", (-2.1, 12, -19), (4.2, 4.5, 0.7), "stone_dark", "face"),
        _cube("face_core", "head", (-1.25, 12.7, -19.45), (2.5, 3, 0.65), "crystal", "face"),
        _cube("eye_left", "head", (-3.15, 12.2, -18.7), (1, 0.8, 0.6), "eye", "face"),
        _cube("eye_right", "head", (2.15, 12.2, -18.7), (1, 0.8, 0.6), "eye", "face"),
        _cube("tusk_left", "head", (-4.2, 8.3, -18.5), (1.2, 3.2, 1.2), "stone_light", "face", (12, 0, -8)),
        _cube("tusk_right", "head", (3, 8.3, -18.5), (1.2, 3.2, 1.2), "stone_light", "face", (12, 0, 8)),
    ]

    # Layered body armour follows the large forms instead of tessellating the
    # Tripo triangles. This is the same readable large-to-small strategy used
    # by the approved Corrupted Silverfish.
    body_plates = (
        ("chest_plate_center", (-3.6, 11, -9.7), (7.2, 6, 1.0), "stone_light"),
        ("chest_plate_left", (-6.8, 10, -8.9), (3, 6.5, 1.1), "stone"),
        ("chest_plate_right", (3.8, 10, -8.9), (3, 6.5, 1.1), "stone"),
        ("back_plate_center", (-3.5, 11, 15.3), (7, 5.5, 1.0), "stone"),
        ("back_plate_left", (-6.2, 10, 13.6), (3, 6, 1.0), "stone_dark"),
        ("back_plate_right", (3.2, 10, 13.6), (3, 6, 1.0), "stone_dark"),
        ("spine_plate_front", (-2.4, 18, -4), (4.8, 1.1, 5), "stone_light"),
        ("spine_plate_mid", (-2.8, 18.1, 2), (5.6, 1.0, 5), "moss"),
        ("spine_plate_rear", (-2.4, 17, 9), (4.8, 1.0, 5), "stone"),
        ("belly_band_front", (-5.4, 7, -4.5), (10.8, 1.2, 2), "stone_dark"),
        ("belly_band_rear", (-5.2, 7.2, 5), (10.4, 1.0, 2), "stone_dark"),
        ("heart_frame", (-2.3, 12.2, -10.2), (4.6, 4.8, 0.7), "stone_dark"),
        ("heart_crystal", (-1.3, 13.1, -10.65), (2.6, 3, 0.65), "crystal", "crystal"),
    )
    for item in body_plates:
        if len(item) == 5:
            name, origin, size, material, category = item
        else:
            name, origin, size, material = item
            category = "plate"
        cubes.append(_cube(name, "body", origin, size, material, category))

    # Side armour is mirrored and deliberately layered with visible gaps.
    for side, sign in (("left", -1), ("right", 1)):
        outer_x = -8.2 if sign < 0 else 6.8
        edge_x = -8.7 if sign < 0 else 7.7
        cubes.extend(
            (
                _cube(f"shoulder_{side}_mass", "body", (outer_x, 11, -6), (1.4, 6, 7), "wood", "mass"),
                _cube(f"shoulder_{side}_plate", "body", (edge_x, 12, -5.6), (1.0, 5, 6.2), "stone", "plate"),
                _cube(f"flank_{side}_mass", "body", (outer_x, 10, 3), (1.4, 6, 8), "wood_dark", "mass"),
                _cube(f"flank_{side}_plate", "body", (edge_x, 11, 4), (1.0, 4.5, 6), "stone_dark", "plate"),
                _cube(f"shoulder_{side}_rim_top", "body", (edge_x - 0.1, 16.7, -5), (1.2, 0.8, 5), "stone_light", "plate"),
                _cube(f"flank_{side}_rim", "body", (edge_x - 0.1, 14.8, 5), (1.2, 0.8, 5), "moss", "plate"),
                _cube(f"shoulder_{side}_gem_frame", "body", (edge_x - 0.2, 13, -3.5), (1.3, 2.8, 2.8), "stone_dark", "crystal"),
                _cube(f"shoulder_{side}_gem", "body", (edge_x - 0.45, 13.6, -2.9), (0.7, 1.6, 1.6), "crystal", "crystal"),
            )
        )

    # Four complete legs. Each bone owns all of its upper/lower/foot details so
    # the runtime animation can move the full limb without cuts.
    for station, z in (("front", -7.0), ("rear", 7.0)):
        for side, sign in (("left", -1), ("right", 1)):
            bone = f"leg_{station}_{side}"
            x = -9.7 if sign < 0 else 6.5
            foot_z = z - 1.0 if station == "front" else z + 0.5
            cubes.extend(
                (
                    _cube(f"{bone}_upper", bone, (x, 6.5, z - 2.5), (3.2, 7, 5), "wood", "mass"),
                    _cube(f"{bone}_upper_plate", bone, (x - 0.45 if sign < 0 else x + 2.75, 8, z - 2.1), (0.9, 4.5, 4.2), "stone", "plate"),
                    _cube(f"{bone}_knee", bone, (x - 0.2, 5.3, z - 2.2), (3.6, 2.3, 4.4), "stone_dark", "plate"),
                    _cube(f"{bone}_knee_gem", bone, (x + 1.0, 5.8, z - 2.55), (1.3, 1.3, 0.7), "crystal", "crystal"),
                    _cube(f"{bone}_lower", bone, (x + 0.2, 1.5, z - 2.0), (2.8, 4.8, 4), "wood_dark", "mass"),
                    _cube(f"{bone}_ankle_band", bone, (x - 0.1, 1.4, z - 2.3), (3.4, 1.3, 4.6), "stone", "plate"),
                    _cube(f"{bone}_foot", bone, (x - 0.4, 0, foot_z - 2.5), (4, 2, 5.5), "stone_dark", "mass"),
                    _cube(f"{bone}_toe_outer", bone, (x - 0.4, 0, foot_z - 3.1), (1.2, 1.2, 1.6), "stone_light", "plate"),
                    _cube(f"{bone}_toe_center", bone, (x + 1.0, 0, foot_z - 3.3), (1.2, 1.2, 1.8), "stone_light", "plate"),
                    _cube(f"{bone}_toe_inner", bone, (x + 2.4, 0, foot_z - 3.1), (1.2, 1.2, 1.6), "stone_light", "plate"),
                )
            )

    # Root armour and antlers. Rotated local planks keep the silhouette close
    # to the Tripo reference while remaining ordinary Blockbench cubes.
    root_specs = (
        ("root_spine_1", (-0.6, 17.5, 5), (1.2, 1.2, 9), (0, 0, 0)),
        ("root_spine_2", (-0.5, 18.2, -4), (1.0, 1.0, 8), (0, 0, 0)),
        ("root_cross_left", (-6, 17.7, 0), (6, 1.0, 1.0), (0, 0, 0)),
        ("root_cross_right", (0, 17.7, 0), (6, 1.0, 1.0), (0, 0, 0)),
        ("antler_left_base", (-5.5, 17, -13), (2, 6, 2), (0, 0, 0)),
        ("antler_left_arm_1", (-8.5, 21, -13), (4, 2, 2), (0, 0, 0)),
        ("antler_left_arm_2", (-10.5, 22, -13), (3, 2, 2), (0, 0, 0)),
        ("antler_left_tip", (-11.5, 23, -13), (2, 5, 2), (0, 0, 0)),
        ("antler_left_branch", (-8.5, 22, -15), (2, 4, 3), (0, 0, 0)),
        ("antler_right_base", (3.5, 17, -13), (2, 6, 2), (0, 0, 0)),
        ("antler_right_arm_1", (4.5, 21, -13), (4, 2, 2), (0, 0, 0)),
        ("antler_right_arm_2", (7.5, 22, -13), (3, 2, 2), (0, 0, 0)),
        ("antler_right_tip", (9.5, 23, -13), (2, 5, 2), (0, 0, 0)),
        ("antler_right_branch", (6.5, 22, -15), (2, 4, 3), (0, 0, 0)),
        ("rear_root_left_base", (-5, 17, 12), (2, 4, 2), (0, 0, 0)),
        ("rear_root_left_tip", (-7, 20, 12), (4, 2, 2), (0, 0, 0)),
        ("rear_root_right_base", (3, 17, 12), (2, 4, 2), (0, 0, 0)),
        ("rear_root_right_tip", (3, 20, 12), (4, 2, 2), (0, 0, 0)),
        ("side_vine_left", (-8.5, 10, -1), (0.7, 7, 0.8), (0, 0, 0)),
        ("side_vine_right", (7.8, 10, 1), (0.7, 7, 0.8), (0, 0, 0)),
    )
    for name, origin, size, rotation in root_specs:
        cubes.append(_cube(name, "roots_body", origin, size, "root", "root", rotation))

    crystal_positions = (
        ("root_crystal_left_1", (-10.9, 25.0, -11.4), (1.3, 2.2, 1.3)),
        ("root_crystal_left_2", (-8.8, 24.4, -13.8), (1.1, 1.8, 1.1)),
        ("root_crystal_right_1", (9.6, 25.0, -11.4), (1.3, 2.2, 1.3)),
        ("root_crystal_right_2", (7.7, 24.4, -13.8), (1.1, 1.8, 1.1)),
        ("back_crystal_1", (-1.0, 18.8, 8.0), (2.0, 2.8, 2.0)),
        ("back_crystal_2", (-4.8, 17.2, 10.2), (1.6, 2.2, 1.6)),
        ("back_crystal_3", (3.2, 17.2, 10.2), (1.6, 2.2, 1.6)),
        ("crown_crystal", (-1.2, 19.0, -11.8), (2.4, 3.2, 2.0)),
    )
    for name, origin, size in crystal_positions:
        cubes.append(_cube(name, "roots_body", origin, size, "crystal", "crystal"))

    # Small moss and plate accents provide controlled detail without turning
    # the whole creature back into a noisy one-voxel scan.
    accents = (
        ("moss_patch_front_left", (-5.8, 16.8, -9.9), (2.2, 1.2, 0.7), "moss"),
        ("moss_patch_front_right", (3.6, 15.8, -9.9), (2.2, 1.2, 0.7), "moss"),
        ("moss_patch_back", (-2.8, 16.9, 15.8), (5.6, 0.8, 0.6), "moss"),
        ("moss_patch_top_left", (-5.8, 18.4, 2), (3.4, 0.7, 3), "moss"),
        ("moss_patch_top_right", (2.4, 18.4, 3), (3.4, 0.7, 3), "moss"),
        ("head_side_plate_left", (-5.4, 11, -15), (1.0, 5, 4), "stone_dark"),
        ("head_side_plate_right", (4.4, 11, -15), (1.0, 5, 4), "stone_dark"),
        ("head_moss_left", (-4.8, 16.7, -14), (2, 0.7, 2), "moss"),
        ("head_moss_right", (2.8, 16.7, -14), (2, 0.7, 2), "moss"),
    )
    for name, origin, size, material in accents:
        cubes.append(_cube(name, "body" if name.startswith("moss_patch") else "head", origin, size, material, "plate"))

    # One-pixel life motes are intentional highlights and prove that the model
    # supports fine details without making every structural cube tiny.
    for index, (x, y, z) in enumerate(((-6, 19, 4), (6, 18, 7), (-9, 17, -4), (9, 16, 1), (-2, 20, 13), (2, 18, -7))):
        cubes.append(_cube(f"life_mote_{index + 1}", "roots_body", (x, y, z), (1, 1, 1), "crystal", "crystal"))
    return tuple(cubes)


CUBES = _build_cubes()


def _stable_uuid(kind: str, name: str) -> str:
    return str(uuid5(UUID_NAMESPACE, f"{kind}:{name}"))


def _face_dimensions(cube: Cube, face: str) -> Tuple[int, int]:
    x, y, z = cube.size
    width, height = {
        "north": (x, y), "south": (x, y),
        "east": (z, y), "west": (z, y),
        "up": (x, z), "down": (x, z),
    }[face]
    # Match the approved Silverfish density: two texture pixels per model unit.
    return max(1, math.ceil(width * 2)), max(1, math.ceil(height * 2))


def pack_uvs(cubes: Iterable[Cube] = CUBES) -> Dict[Tuple[str, str], Tuple[int, int, int, int]]:
    islands = []
    for cube in cubes:
        for face in FACE_NAMES:
            width, height = _face_dimensions(cube, face)
            islands.append((cube.name, face, width, height))
    islands.sort(key=lambda value: (-value[3], -value[2], value[0], value[1]))
    result = {}
    x = y = shelf = 0
    for name, face, width, height in islands:
        if x and x + width > TEXTURE_SIZE:
            y += shelf + GUTTER
            x = shelf = 0
        if y + height > TEXTURE_SIZE:
            raise ValueError(f"Living Boss texture atlas overflow at {name}/{face}")
        result[(name, face)] = (x, y, x + width, y + height)
        x += width + GUTTER
        shelf = max(shelf, height)
    return result


PALETTES: Mapping[str, Tuple[Tuple[int, int, int], ...]] = {
    "wood": ((35, 24, 16), (62, 42, 25), (91, 62, 34), (125, 88, 48)),
    "wood_dark": ((19, 16, 13), (37, 29, 20), (61, 43, 26), (87, 61, 35)),
    "stone": ((39, 42, 39), (64, 69, 64), (91, 98, 91), (130, 138, 128)),
    "stone_dark": ((22, 24, 23), (39, 43, 41), (60, 65, 62), (86, 92, 87)),
    "stone_light": ((65, 67, 62), (103, 106, 98), (145, 148, 137), (188, 190, 175)),
    "moss": ((23, 37, 8), (42, 67, 13), (67, 101, 20), (108, 145, 36)),
    "root": ((31, 20, 12), (55, 35, 18), (82, 52, 25), (116, 77, 36)),
    "crystal": ((23, 61, 18), (52, 128, 36), (108, 212, 72), (199, 255, 151)),
    "eye": ((11, 49, 13), (40, 151, 44), (111, 245, 101), (222, 255, 204)),
}


def _paint_atlases(uvs: Mapping[Tuple[str, str], Tuple[int, int, int, int]]) -> Tuple[bytes, bytes]:
    texture = Image.new("RGBA", (TEXTURE_SIZE, TEXTURE_SIZE), (0, 0, 0, 0))
    glow = Image.new("RGBA", (TEXTURE_SIZE, TEXTURE_SIZE), (0, 0, 0, 0))
    pixels = texture.load()
    glow_pixels = glow.load()
    face_light = {"up": 1.12, "down": 0.58, "north": 0.94, "south": 0.82, "east": 0.74, "west": 0.86}
    for cube in CUBES:
        palette = PALETTES[cube.material]
        for face in FACE_NAMES:
            left, top, right, bottom = uvs[(cube.name, face)]
            digest = hashlib.sha256(f"{cube.name}/{face}".encode()).digest()
            for py in range(top, bottom):
                for px in range(left, right):
                    local_x, local_y = px - left, py - top
                    edge = local_x in (0, right - left - 1) or local_y in (0, bottom - top - 1)
                    noise = digest[(local_x * 7 + local_y * 11) % len(digest)]
                    shade = 0 if edge else 1 + (noise >= 150) + (noise >= 232)
                    if cube.material in {"wood", "wood_dark", "root"} and local_x % 3 == digest[0] % 3:
                        shade = max(0, shade - 1)
                    if cube.material == "moss" and (local_x + local_y + digest[1]) % 4 == 0:
                        shade = min(3, shade + 1)
                    colour = palette[min(3, shade)]
                    factor = face_light[face]
                    rgba = tuple(min(255, round(channel * factor)) for channel in colour) + (255,)
                    pixels[px, py] = rgba
            if cube.material in {"crystal", "eye"}:
                cx = left + (right - left) // 2
                cy = top + (bottom - top) // 2
                radius = max(1, min(right - left, bottom - top) // 3)
                for py in range(max(top, cy - radius), min(bottom, cy + radius + 1)):
                    for px in range(max(left, cx - radius), min(right, cx + radius + 1)):
                        if abs(px - cx) + abs(py - cy) <= radius + 1:
                            core = PALETTES[cube.material][3] + (255,)
                            pixels[px, py] = core
                            glow_pixels[px, py] = core
    return _png_bytes(texture), _png_bytes(glow)


def _png_bytes(image: Image.Image) -> bytes:
    buffer = io.BytesIO()
    image.save(buffer, format="PNG", optimize=False, compress_level=9)
    return buffer.getvalue()


def _pivot(cube: Cube) -> list[float]:
    return [cube.origin[index] + cube.size[index] / 2 for index in range(3)]


def _outliner() -> list[dict]:
    children_by_bone = {bone.name: [] for bone in BONES}
    for cube in CUBES:
        children_by_bone[cube.bone].append(_stable_uuid("element", cube.name))
    descendants = {bone.name: [] for bone in BONES}
    for bone in BONES:
        if bone.parent:
            descendants[bone.parent].append(bone.name)

    def node(name: str) -> dict:
        return {
            "name": name,
            "origin": list(next(bone.pivot for bone in BONES if bone.name == name)),
            "uuid": _stable_uuid("group", name),
            "children": children_by_bone[name] + [node(child) for child in descendants[name]],
        }

    return [node("root")]


def build_payloads() -> Payloads:
    uvs = pack_uvs()
    texture, glowmask = _paint_atlases(uvs)
    elements = []
    for cube in CUBES:
        faces = {}
        for face in FACE_NAMES:
            faces[face] = {"uv": list(uvs[(cube.name, face)]), "texture": 0}
        element = {
            "name": cube.name,
            "box_uv": False,
            "from": list(cube.origin),
            "to": [cube.origin[index] + cube.size[index] for index in range(3)],
            "origin": _pivot(cube),
            "faces": faces,
            "type": "cube",
            "uuid": _stable_uuid("element", cube.name),
            "bone": cube.bone,
        }
        if cube.rotation != (0.0, 0.0, 0.0):
            element["rotation"] = list(cube.rotation)
        elements.append(element)
    document = {
        "meta": {"format_version": "5.0", "model_format": "free", "box_uv": False},
        "name": "living_boss_tripo_cuboids",
        "resolution": {"width": TEXTURE_SIZE, "height": TEXTURE_SIZE},
        "elements": elements,
        "outliner": _outliner(),
        "textures": [{
            "name": "living_boss.png",
            "id": "0",
            "particle": False,
            "render_mode": "default",
            "visible": True,
            "mode": "bitmap",
            "saved": True,
            "uuid": _stable_uuid("texture", "living_boss"),
            "source": "data:image/png;base64," + base64.b64encode(texture).decode("ascii"),
        }],
        "animations": [],
    }
    bbmodel = (json.dumps(document, ensure_ascii=False, indent=2) + "\n").encode("utf-8")
    return Payloads(bbmodel, texture, glowmask)


def _atomic_write(path: Path, data: bytes) -> None:
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


def write_outputs(model: Path = MODEL_PATH, texture: Path = TEXTURE_PATH, glowmask: Path = GLOWMASK_PATH) -> Payloads:
    payloads = build_payloads()
    _atomic_write(model, payloads.bbmodel)
    _atomic_write(texture, payloads.texture)
    _atomic_write(glowmask, payloads.glowmask)
    return payloads


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--model", type=Path, default=MODEL_PATH)
    parser.add_argument("--texture", type=Path, default=TEXTURE_PATH)
    parser.add_argument("--glowmask", type=Path, default=GLOWMASK_PATH)
    args = parser.parse_args(argv)
    try:
        payloads = write_outputs(args.model, args.texture, args.glowmask)
    except (OSError, ValueError) as exc:
        print(f"LIVING_CUBOID_FAILED: {exc}")
        return 1
    print(f"LIVING_CUBOID_PASS CUBES={len(CUBES)} BONES={len(BONES)} TEXTURE={len(payloads.texture)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
