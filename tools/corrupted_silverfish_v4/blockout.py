"""Build a reference-first, textured Blockbench blockout for visual approval."""

from __future__ import annotations

import base64
from io import BytesIO
import json
import os
from pathlib import Path
import tempfile
from typing import Dict, Iterable, List, Mapping
import uuid

from PIL import Image, ImageDraw, ImageFont

from tools.corrupted_silverfish_v3 import render as renderer


ROOT = Path(__file__).resolve().parents[2]
EDITABLE = ROOT / "Modelle/Editierbar/Corrupted Silverfish v4 Blockout.bbmodel"
EXPORT = ROOT / "Modelle/Exports/corrupted_silverfish_v4"
TEXTURE = EXPORT / "blockout/corrupted_silverfish_v4_blockout.png"
REVIEW = EXPORT / "review"
REFERENCE = ROOT / "Modelle/Exports/corrupted_silverfish_v2/concept/concept_sheet_raw.png"
NAMESPACE = uuid.UUID("9b99680e-af4a-4b48-8663-6ccb7116ec67")
FACES = ("north", "east", "south", "west", "up", "down")

PALETTE = {
    "seam": (25, 20, 31, 255),
    "core": (48, 34, 55, 255),
    "steel_dark": (82, 88, 96, 255),
    "steel": (139, 148, 155, 255),
    "steel_light": (198, 205, 210, 255),
    "corruption": (92, 16, 66, 255),
    "magenta": (225, 35, 105, 255),
    "violet": (67, 27, 82, 255),
    "eye": (74, 183, 255, 255),
}
MATERIALS = tuple(PALETTE)


def _cube(name, bone, origin, size, material, rotation=(0, 0, 0)):
    return {
        "name": name, "bone": bone, "origin": tuple(origin), "size": tuple(size),
        "material": material, "rotation": tuple(rotation),
    }


def _crystal(result: List[dict], name: str, bone: str, x: float, y: float, z: float, height: float):
    tiers = ((2.6, min(2.2, height * .34), "corruption"),
             (1.9, min(2.2, height * .34), "magenta"),
             (1.15, max(1.0, height - min(4.4, height * .68)), "violet"))
    current = y
    for index, (width, tier_height, material) in enumerate(tiers, 1):
        result.append(_cube(
            f"{name}_{index}", bone,
            (x - width / 2, current, z - width / 2), (width, tier_height, width), material,
        ))
        current += tier_height


def cubes() -> List[dict]:
    c: List[dict] = [
        _cube("head_core", "head", (-7, 2.2, -9), (14, 5.8, 6.5), "core"),
        _cube("head_armor", "head", (-7.7, 6.0, -8.6), (15.4, 4.0, 5.5), "steel"),
        _cube("head_brow_left", "head", (-6.4, 4.8, -9.35), (5.1, 2.2, .7), "steel_light"),
        _cube("head_brow_right", "head", (1.3, 4.8, -9.35), (5.1, 2.2, .7), "steel_light"),
        _cube("face_corruption", "head", (-1.1, 5.1, -9.48), (2.2, 3.8, .5), "magenta"),
        _cube("eye_left", "head", (-4.7, 4.0, -9.5), (1.0, 1.0, .55), "eye"),
        _cube("eye_right", "head", (3.7, 4.0, -9.5), (1.0, 1.0, .55), "eye"),
        _cube("mandible_left", "mandible_left", (-3.9, 1.2, -12.2), (2.4, 4.0, 3.4), "steel_light"),
        _cube("mandible_right", "mandible_right", (1.5, 1.2, -12.2), (2.4, 4.0, 3.4), "steel_light"),
        _cube("body_core", "body", (-6.4, 2.3, -3.2), (12.8, 5.3, 19.2), "core"),
        _cube("armor_front", "shell_front", (-8.0, 5.0, -2.6), (16.0, 5.1, 6.8), "steel"),
        _cube("armor_front_highlight", "shell_front", (-7.4, 9.0, -2.1), (14.8, 1.4, 5.8), "steel_light"),
        _cube("armor_mid", "shell_mid", (-7.7, 4.8, 3.5), (15.4, 5.1, 7.0), "steel"),
        _cube("armor_mid_highlight", "shell_mid", (-7.1, 8.8, 4.0), (14.2, 1.4, 6.0), "steel_light"),
        _cube("armor_rear", "shell_rear", (-7.0, 4.2, 9.8), (14.0, 4.8, 6.8), "steel"),
        _cube("armor_rear_highlight", "shell_rear", (-6.4, 8.0, 10.3), (12.8, 1.35, 5.8), "steel_light"),
        _cube("front_seam", "body", (-6.7, 3.9, 3.0), (13.4, 1.4, 1.0), "corruption"),
        _cube("rear_seam", "body", (-6.2, 3.6, 9.4), (12.4, 1.4, 1.0), "corruption"),
        _cube("corruption_panel_left_front", "shell_front", (-8.25, 4.7, -.9), (.7, 2.5, 3.0), "magenta"),
        _cube("corruption_panel_right_front", "shell_front", (7.55, 4.7, -.9), (.7, 2.5, 3.0), "magenta"),
        _cube("corruption_panel_left_mid", "shell_mid", (-7.95, 4.5, 5.5), (.7, 2.5, 3.0), "corruption"),
        _cube("corruption_panel_right_mid", "shell_mid", (7.25, 4.5, 5.5), (.7, 2.5, 3.0), "corruption"),
        _cube("corruption_panel_left_rear", "shell_rear", (-7.25, 4.0, 11.8), (.7, 2.3, 2.8), "magenta"),
        _cube("corruption_panel_right_rear", "shell_rear", (6.55, 4.0, 11.8), (.7, 2.3, 2.8), "magenta"),
        _cube("tail_base", "tail", (-5.0, 3.0, 16.0), (10.0, 4.5, 3.3), "steel_dark"),
        _cube("tail_mid", "tail", (-3.5, 2.7, 19.2), (7.0, 3.7, 3.0), "steel"),
        _cube("tail_end", "tail", (-2.2, 2.4, 22.1), (4.4, 3.0, 2.4), "steel_dark"),
        _cube("tail_tip", "tail", (-1.1, 2.15, 24.4), (2.2, 2.3, 2.1), "violet"),
    ]
    for index, z in enumerate((-0.5, 5.8, 12.0), 1):
        for side, sign in (("left", -1), ("right", 1)):
            upper_x = -10.2 if sign < 0 else 6.2
            lower_x = -12.3 if sign < 0 else 9.3
            c.append(_cube(f"leg_{side}_{index}_upper", f"leg_{side}_{index}",
                           (upper_x, 2.0, z), (4.0, 2.2, 2.7), "violet"))
            c.append(_cube(f"leg_{side}_{index}_lower", f"leg_{side}_{index}",
                           (lower_x, 0.0, z + .15), (3.0, 2.5, 2.4), "steel_dark"))
    _crystal(c, "crystal_crown", "crystals_front", 0, 10.0, -.3, 7.0)
    _crystal(c, "crystal_front_left", "crystals_front", -5.2, 10.0, 1.2, 4.5)
    _crystal(c, "crystal_mid_right", "crystals_mid", 4.2, 9.8, 6.4, 6.0)
    _crystal(c, "crystal_rear_left", "crystals_rear", -4.3, 9.0, 12.2, 4.2)
    # Small asymmetric corruption cluster on one flank.
    c.extend([
        _cube("side_crystal_left_1", "crystals_rear", (-8.1, 6.0, 11.2), (1.5, 3.2, 1.5), "magenta", (0, 0, -18)),
        _cube("side_crystal_left_2", "crystals_rear", (-8.4, 5.0, 13.0), (1.3, 2.5, 1.3), "violet", (0, 0, -28)),
    ])
    return c


def model_summary() -> dict:
    items = cubes()
    minimum_z = min(c["origin"][2] for c in items)
    maximum_z = max(c["origin"][2] + c["size"][2] for c in items)
    minimum_x = min(c["origin"][0] for c in items)
    maximum_x = max(c["origin"][0] + c["size"][0] for c in items)
    return {
        "length": maximum_z - minimum_z,
        "width": maximum_x - minimum_x,
        "head_width": 15.4,
        "armor_width": 16.0,
        "tail_length": 10.5,
        "legs": 6,
        "main_armor_plates": 3,
    }


def _stable(kind: str, name: str) -> str:
    return str(uuid.uuid5(NAMESPACE, f"{kind}:{name}"))


def _material_uv(material: str) -> dict:
    index = MATERIALS.index(material)
    x = (index % 8) * 16
    y = (index // 8) * 16
    return {face: {"uv": [x, y], "uv_size": [16, 16]} for face in FACES}


def _render_cube(cube: Mapping) -> dict:
    return {
        "name": cube["name"], "origin": list(cube["origin"]), "size": list(cube["size"]),
        "rotation": list(cube["rotation"]), "pivot": [cube["origin"][i] + cube["size"][i] / 2 for i in range(3)],
        "uv": _material_uv(cube["material"]),
    }


def _texture_image() -> Image.Image:
    image = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    for index, material in enumerate(MATERIALS):
        x = (index % 8) * 16
        y = (index // 8) * 16
        colour = PALETTE[material]
        dark = tuple(max(0, channel - 24) for channel in colour[:3]) + (255,)
        light = tuple(min(255, channel + 22) for channel in colour[:3]) + (255,)
        draw.rectangle((x, y, x + 15, y + 15), fill=colour)
        draw.line((x, y + 15, x + 15, y + 15), fill=dark, width=2)
        draw.line((x + 15, y, x + 15, y + 15), fill=dark, width=2)
        draw.line((x, y, x + 14, y), fill=light, width=2)
        draw.line((x, y, x, y + 14), fill=light, width=2)
        if material.startswith("steel"):
            draw.rectangle((x + 4, y + 5, x + 11, y + 6), fill=light)
        elif material in {"magenta", "violet", "corruption"}:
            draw.rectangle((x + 5, y + 3, x + 7, y + 12), fill=light)
    return image


def _png_bytes(image: Image.Image) -> bytes:
    buffer = BytesIO()
    image.save(buffer, format="PNG", optimize=False, compress_level=9)
    return buffer.getvalue()


def _write(path: Path, data: bytes) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    descriptor, temporary_name = tempfile.mkstemp(prefix=f".{path.name}.", suffix=".tmp", dir=str(path.parent))
    temporary = Path(temporary_name)
    try:
        with os.fdopen(descriptor, "wb") as handle:
            descriptor = -1
            handle.write(data)
            handle.flush()
            os.fsync(handle.fileno())
        os.replace(temporary, path)
    finally:
        if descriptor != -1:
            os.close(descriptor)
        if temporary.exists():
            temporary.unlink()


def _bbmodel(texture_bytes: bytes) -> dict:
    items = cubes()
    bone_names = ["root"] + list(dict.fromkeys(c["bone"] for c in items))
    elements = []
    by_bone: Dict[str, List[str]] = {name: [] for name in bone_names}
    for cube in items:
        uvs = _material_uv(cube["material"])
        element_id = _stable("element", cube["name"])
        by_bone[cube["bone"]].append(element_id)
        elements.append({
            "name": cube["name"], "box_uv": False,
            "from": list(cube["origin"]),
            "to": [cube["origin"][i] + cube["size"][i] for i in range(3)],
            "origin": [cube["origin"][i] + cube["size"][i] / 2 for i in range(3)],
            "rotation": list(cube["rotation"]),
            "faces": {face: {"uv": [uvs[face]["uv"][0], uvs[face]["uv"][1], uvs[face]["uv"][0] + 16, uvs[face]["uv"][1] + 16], "texture": 0} for face in FACES},
            "type": "cube", "uuid": element_id, "bone": cube["bone"],
        })
    groups = []
    for name in bone_names:
        groups.append({
            "name": name, "uuid": _stable("group", name), "export": True, "locked": False,
            "origin": [0, 5, 5], "rotation": [0, 0, 0], "color": 0, "children": [],
            "reset": False, "shade": True, "mirror_uv": False, "visibility": True,
            "autouv": 0, "isOpen": True,
        })
    children = []
    for name in bone_names[1:]:
        children.append({"uuid": _stable("group", name), "isOpen": True, "children": by_bone[name]})
    source = "data:image/png;base64," + base64.b64encode(texture_bytes).decode("ascii")
    return {
        "meta": {"format_version": "5.0", "model_format": "geckolib_model", "box_uv": False},
        "name": "Corrupted Silverfish v4 Blockout", "model_identifier": "geometry.corrupted_silverfish_v4_blockout",
        "visible_box": [3, 2, 0], "variable_placeholders": "", "timeline_setups": [], "unhandled_root_fields": {},
        "geckolib_modid": "usless_mobs", "geckolib_filepath_cache": "", "resolution": {"width": 256, "height": 256},
        "elements": elements, "groups": groups,
        "outliner": [{"uuid": _stable("group", "root"), "isOpen": True, "children": children}],
        "textures": [{
            "path": "", "name": TEXTURE.name, "folder": "entity", "namespace": "usless_mobs", "id": "0",
            "particle": False, "render_mode": "default", "visible": True, "mode": "bitmap", "saved": True,
            "uuid": _stable("texture", TEXTURE.name), "source": source,
        }],
        "animations": [], "geckolib_model_type": "Entity",
    }


def _render_views(texture: Image.Image) -> Dict[str, Image.Image]:
    scene = [(_render_cube(cube), renderer.identity_matrix()) for cube in cubes()]
    center = (0.0, 7.0, 6.5)
    return {
        "front": renderer.render_cubes(scene, texture, renderer.camera_for("front"), pixels_per_unit=18, center_world=center),
        "right": renderer.render_cubes(scene, texture, renderer.camera_for("right"), pixels_per_unit=18, center_world=center),
        "top": renderer.render_cubes(scene, texture, renderer.camera_for("top"), pixels_per_unit=18, center_world=center),
        "perspective": renderer.render_cubes(scene, texture, renderer.camera_for("three_quarter"), pixels_per_unit=18, center_world=center),
    }


def _fit(source: Image.Image, box: tuple[int, int]) -> Image.Image:
    bbox = source.getbbox()
    if bbox:
        source = source.crop(bbox)
    source.thumbnail(box, Image.Resampling.NEAREST)
    return source


def _contact(views: Mapping[str, Image.Image]) -> Image.Image:
    sheet = Image.new("RGBA", (1536, 768), (18, 17, 24, 255))
    draw = ImageDraw.Draw(sheet)
    if REFERENCE.is_file():
        with Image.open(REFERENCE) as raw:
            concept = raw.convert("RGBA")
        concept.thumbnail((700, 700), Image.Resampling.LANCZOS)
        sheet.alpha_composite(concept, (34, 34))
    draw.text((34, 10), "REFERENCE", fill=(235, 235, 240, 255), font=ImageFont.load_default())
    for index, name in enumerate(("front", "right", "top", "perspective")):
        cell_x = 768 + (index % 2) * 384
        cell_y = (index // 2) * 384
        view = _fit(views[name], (344, 320))
        sheet.alpha_composite(view, (cell_x + (384 - view.width) // 2, cell_y + 38 + (320 - view.height) // 2))
        draw.text((cell_x + 12, cell_y + 12), name.upper(), fill=(235, 235, 240, 255), font=ImageFont.load_default())
    return sheet


def build() -> Dict[str, Path]:
    texture = _texture_image()
    texture_bytes = _png_bytes(texture)
    views = _render_views(texture)
    contact = _contact(views)
    paths = {
        "bbmodel": EDITABLE, "texture": TEXTURE,
        "front": REVIEW / "blockout_front.png", "right": REVIEW / "blockout_right.png",
        "top": REVIEW / "blockout_top.png", "perspective": REVIEW / "blockout_perspective.png",
        "contact_sheet": REVIEW / "blockout_contact_sheet.png",
    }
    _write(TEXTURE, texture_bytes)
    model_bytes = (json.dumps(_bbmodel(texture_bytes), ensure_ascii=False, indent=2) + "\n").encode("utf-8")
    _write(EDITABLE, model_bytes)
    for name, image in views.items():
        _write(paths[name], _png_bytes(image))
    _write(paths["contact_sheet"], _png_bytes(contact))
    return paths


def main() -> int:
    paths = build()
    print(f"V4_BLOCKOUT=PASS;CUBES={len(cubes())};PATH={paths['bbmodel']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
