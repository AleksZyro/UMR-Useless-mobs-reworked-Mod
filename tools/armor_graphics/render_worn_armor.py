"""Deterministic offline previews of the real worn-armour Java geometry."""

from __future__ import annotations

import argparse
import math
from functools import lru_cache
from pathlib import Path
from typing import Iterable

from PIL import Image, ImageDraw

from tools.armor_graphics.tests.test_armor_graphics import worn_method_geometry
from tools.corrupted_silverfish_v3 import render as cube_renderer

FAMILIES = ("void", "celestial", "living")
SLOTS = ("helmet", "chestplate", "leggings", "boots")
VIEWS = ("front", "right", "back", "three_quarter")
BONE_OFFSETS = {
    "head": (0.0, 0.0, 0.0), "body": (0.0, 0.0, 0.0),
    "right_arm": (-5.0, 2.0, 0.0), "left_arm": (5.0, 2.0, 0.0),
    "right_leg": (-1.9, 12.0, 0.0), "left_leg": (1.9, 12.0, 0.0),
}
_BASE_NAMES = {
    "helmet": {"true_outer_helm"},
    "chestplate": {"true_breastplate", "true_right_arm_plate", "true_left_arm_plate"},
    "leggings": {"true_belt", "true_right_thigh", "true_left_thigh"},
    "boots": {"true_right_boot", "true_left_boot"},
}
_CROWN_COMMON = {"true_crown_band", "true_crown_center_spike", "true_crown_left_spike", "true_crown_right_spike"}
_CROWN_FAMILY = {
    "void": {"true_void_left_horn", "true_void_right_horn"},
    "celestial": {"true_celestial_left_wing", "true_celestial_right_wing"},
    "living": {"true_living_vine", "true_living_leaf"},
}
_DETAIL_METHOD = {
    ("void", "helmet"): "addVoidHelmetDetails", ("celestial", "helmet"): "addCelestialHelmetDetails",
    ("living", "helmet"): "addLivingHelmetDetails", ("void", "chestplate"): "addVoidCrystalKnightDetails",
    ("celestial", "chestplate"): "addCelestialChestDetails", ("living", "chestplate"): "addLivingChestDetails",
    ("void", "leggings"): "addVoidLegDetails", ("celestial", "leggings"): "addCelestialLegDetails",
    ("living", "leggings"): "addLivingLegDetails", ("void", "boots"): "addVoidBootDetails",
    ("celestial", "boots"): "addCelestialBootDetails", ("living", "boots"): "addLivingBootDetails",
}


def _validate_choice(value: str, choices: Iterable[str], label: str) -> None:
    if value not in choices:
        raise ValueError(f"unknown {label} {value!r}; expected one of {', '.join(choices)}")


@lru_cache(maxsize=8)
def _parsed_methods(repo_root: str) -> dict[str, dict[str, tuple]]:
    source_path = Path(repo_root) / "src/main/java/com/Momik/usless_mobs/client/WornTruePathArmorModel.java"
    source = source_path.read_text(encoding="utf-8")
    methods = {"addBaseVolume", "addPathCrown", "addLegDetails", "addBootDetails", *_DETAIL_METHOD.values()}
    return {method: worn_method_geometry(source, method) for method in methods}


def _select_geometry(methods: dict[str, dict[str, tuple]], family: str, slot: str) -> dict[str, tuple]:
    selected = {name: value for name, value in methods["addBaseVolume"].items() if name in _BASE_NAMES[slot]}
    if slot == "helmet":
        names = _CROWN_COMMON | _CROWN_FAMILY[family]
        selected.update({name: value for name, value in methods["addPathCrown"].items() if name in names})
    elif slot == "leggings":
        names = {"true_right_knee", "true_left_knee"}
        if family == "celestial":
            names |= {"true_right_shin_star", "true_left_shin_star"}
        selected.update({name: value for name, value in methods["addLegDetails"].items() if name in names})
    elif slot == "boots":
        names = {"true_right_toe", "true_left_toe"}
        if family == "living":
            names |= {"true_right_boot_leaf", "true_left_boot_leaf"}
        selected.update({name: value for name, value in methods["addBootDetails"].items() if name in names})
    selected.update(methods[_DETAIL_METHOD[(family, slot)]])
    return selected


def assemble_piece(repo_root: Path, family: str, slot: str) -> list[dict]:
    """Return the cubes which the Java model actually attaches for one item."""
    _validate_choice(family, FAMILIES, "family")
    _validate_choice(slot, SLOTS, "slot")
    geometry = _select_geometry(_parsed_methods(str(repo_root.resolve())), family, slot)
    cubes = []
    for name, (bone, uv, origin, dimensions, deformation, pose_type, offset, rotation) in geometry.items():
        cubes.append({
            "name": name, "bone": bone, "uv_origin": uv,
            "origin": tuple(value - deformation for value in origin),
            "size": tuple(value + 2.0 * deformation for value in dimensions),
            "source_size": dimensions, "deformation": deformation, "pose_type": pose_type,
            "offset": offset, "rotation": rotation,
        })
    return cubes


def box_uv_faces(uv: tuple[float, float], dimensions: tuple[float, float, float]) -> dict[str, dict[str, list[float]]]:
    """Expand Minecraft's box UV layout into the six explicit face rectangles."""
    u, v = uv
    width, height, depth = dimensions
    rectangles = {
        "down": (u + depth, v, width, depth), "up": (u + depth + width, v, width, depth),
        "west": (u, v + depth, depth, height), "north": (u + depth, v + depth, width, height),
        "east": (u + depth + width, v + depth, depth, height),
        "south": (u + 2.0 * depth + width, v + depth, width, height),
    }
    faces = {}
    for face, (face_u, face_v, face_width, face_height) in rectangles.items():
        if face_width <= 0 or face_height <= 0 or face_u < 0 or face_v < 0:
            raise ValueError(f"invalid {face} UV rectangle")
        if face_u + face_width > 128 or face_v + face_height > 64:
            raise ValueError(f"{face} UV rectangle exceeds 128x64 atlas")
        faces[face] = {"uv": [face_u, face_v], "uv_size": [face_width, face_height]}
    return faces


def _texture_path(repo_root: Path, family: str, slot: str) -> Path:
    directory = repo_root / "src/main/resources/assets/usless_mobs/textures/models/armor"
    if family == "void" and slot == "chestplate":
        name = "true_void_chestplate_layer_1.png"
    else:
        name = f"true_{family}_layer_{2 if slot == 'leggings' else 1}.png"
    return directory / name


def _render_inputs(repo_root: Path, family: str, slot: str):
    reflection = cube_renderer._scale((1.0, -1.0, 1.0))
    result = []
    for part in assemble_piece(repo_root, family, slot):
        rotation = tuple(math.degrees(value) for value in part["rotation"])
        matrix = cube_renderer._multiply(
            reflection,
            cube_renderer._multiply(
                cube_renderer._translate(BONE_OFFSETS[part["bone"]]),
                cube_renderer._multiply(cube_renderer._translate(part["offset"]), cube_renderer._rotation(rotation)),
            ),
        )
        cube = {
            "name": part["name"], "origin": part["origin"], "size": part["size"],
            "uv": box_uv_faces(part["uv_origin"], part["source_size"]),
        }
        result.append((cube, matrix))
    return result


def _mannequin_inputs(slot: str):
    definitions = {
        "head": ((-4.0, -8.0, -4.0), (8.0, 8.0, 8.0)),
        "body": ((-4.0, 0.0, -2.0), (8.0, 12.0, 4.0)),
        "right_arm": ((-3.0, -2.0, -2.0), (4.0, 12.0, 4.0)),
        "left_arm": ((-1.0, -2.0, -2.0), (4.0, 12.0, 4.0)),
        "right_leg": ((-2.0, 0.0, -2.0), (4.0, 12.0, 4.0)),
        "left_leg": ((-2.0, 0.0, -2.0), (4.0, 12.0, 4.0)),
    }
    bones = {
        "helmet": ("head",),
        "chestplate": ("body", "right_arm", "left_arm"),
        "leggings": ("body", "right_leg", "left_leg"),
        "boots": ("right_leg", "left_leg"),
        "set": ("head", "body", "right_arm", "left_arm", "right_leg", "left_leg"),
    }[slot]
    reflection = cube_renderer._scale((1.0, -1.0, 1.0))
    flat_uv = {face: {"uv": [0, 0], "uv_size": [1, 1]} for face in ("north", "east", "south", "west", "up", "down")}
    return [
        (
            {"name": f"mannequin_{bone}", "origin": definitions[bone][0], "size": definitions[bone][1], "uv": flat_uv},
            cube_renderer._multiply(reflection, cube_renderer._translate(BONE_OFFSETS[bone])),
        )
        for bone in bones
    ]


def _framing(cubes, camera):
    points = []
    for cube, matrix in cubes:
        points.extend(cube_renderer.transform_point(matrix, point) for point in cube_renderer._cube_vertices(cube))
    _, screen_x, screen_y = camera
    projected_x = [cube_renderer._dot(point, screen_x) for point in points]
    projected_y = [cube_renderer._dot(point, screen_y) for point in points]
    span_x = max(projected_x) - min(projected_x)
    span_y = max(projected_y) - min(projected_y)
    scale = min(420.0 / max(span_x, 1.0), 420.0 / max(span_y, 1.0), 28.0)
    center = tuple((min(point[i] for point in points) + max(point[i] for point in points)) / 2.0 for i in range(3))
    return scale, center


def render_piece(repo_root: Path, family: str, slot: str, view: str) -> Image.Image:
    _validate_choice(view, VIEWS, "view")
    cubes = _render_inputs(repo_root, family, slot)
    camera = cube_renderer.camera_for(view)
    scale, center = _framing(cubes, camera)
    with Image.open(_texture_path(repo_root, family, slot)) as source:
        texture = source.convert("RGBA")
    if texture.size != (128, 64):
        raise ValueError(f"expected 128x64 armour texture, got {texture.size}")
    mannequin = cube_renderer.render_cubes(
        _mannequin_inputs(slot), Image.new("RGBA", (1, 1), (50, 46, 58, 255)), camera,
        canvas_size=(512, 512), pixels_per_unit=scale, center_world=center,
    )
    armour = cube_renderer.render_cubes(
        cubes, texture, camera, canvas_size=(512, 512), pixels_per_unit=scale, center_world=center
    )
    mannequin.alpha_composite(armour)
    return mannequin


def render_set(repo_root: Path, family: str, view: str) -> Image.Image:
    """Render one complete family from the same four runtime item layers."""
    _validate_choice(family, FAMILIES, "family")
    _validate_choice(view, VIEWS, "view")
    per_slot = {slot: _render_inputs(repo_root, family, slot) for slot in SLOTS}
    all_cubes = [entry for slot in SLOTS for entry in per_slot[slot]]
    camera = cube_renderer.camera_for(view)
    scale, center = _framing(all_cubes, camera)
    image = cube_renderer.render_cubes(
        _mannequin_inputs("set"), Image.new("RGBA", (1, 1), (50, 46, 58, 255)), camera,
        canvas_size=(512, 512), pixels_per_unit=scale, center_world=center,
    )
    for slot in SLOTS:
        with Image.open(_texture_path(repo_root, family, slot)) as source:
            texture = source.convert("RGBA")
        layer = cube_renderer.render_cubes(
            per_slot[slot], texture, camera, canvas_size=(512, 512),
            pixels_per_unit=scale, center_world=center,
        )
        image.alpha_composite(layer)
    return image


def build_contact_sheet(repo_root: Path) -> Image.Image:
    sheet = Image.new("RGBA", (2048, 2048), (17, 16, 23, 255))
    draw = ImageDraw.Draw(sheet)
    draw.text((28, 22), "WORN ARMOUR - REAL JAVA GEOMETRY + REAL TEXTURES", fill=(240, 238, 247, 255))
    for row, family in enumerate(FAMILIES):
        for column, view in enumerate(VIEWS):
            image = render_set(repo_root, family, view)
            x, y = 24 + column * 500, 100 + row * 620
            draw.rounded_rectangle((x, y, x + 476, y + 586), 14, fill=(26, 24, 34, 255), outline=(65, 59, 78, 255), width=2)
            sheet.alpha_composite(image.resize((476, 476), Image.Resampling.NEAREST), (x, y + 42))
            draw.text((x + 14, y + 14), f"{family.upper()} / {view.upper()}", fill=(220, 216, 230, 255))
    return sheet


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    image = build_contact_sheet(args.root.resolve())
    args.output.parent.mkdir(parents=True, exist_ok=True)
    image.save(args.output, format="PNG")
    print(f"ARMOUR_RENDER_PASS: {args.output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
