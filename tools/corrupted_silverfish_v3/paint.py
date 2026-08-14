"""Paint deterministic pixel-art textures for Corrupted Silverfish v3."""

from __future__ import annotations

import base64
import hashlib
import os
from pathlib import Path
import tempfile
from types import MappingProxyType
from typing import Mapping, Sequence

from PIL import Image, ImageDraw

from . import build
from .spec import CUBES, Cube


MAIN_TEXTURE_PATH = build.EXPORT_ROOT / "textures" / "entity" / "corrupted_silverfish.png"
GLOWMASK_PATH = build.EXPORT_ROOT / "textures" / "entity" / "corrupted_silverfish_glowmask.png"
PREVIEW_PATH = build.EXPORT_ROOT / "review" / "texture_atlas_preview.png"

PALETTE: Mapping[str, tuple[int, int, int, int]] = MappingProxyType(
    {
        "seam": (22, 18, 28, 255),
        "underside": (38, 34, 46, 255),
        "steel_dark": (76, 82, 92, 255),
        "steel": (126, 136, 147, 255),
        "steel_light": (188, 198, 207, 255),
        "steel_highlight": (224, 230, 235, 255),
        "corruption_root": (45, 10, 43, 255),
        "corruption_dark": (91, 15, 58, 255),
        "crimson": (181, 31, 82, 255),
        "magenta": (234, 55, 112, 255),
        "violet": (82, 37, 104, 255),
        "eye": (86, 190, 255, 255),
    }
)
TRANSPARENT = (0, 0, 0, 0)


def _digest(cube_name: str, face: str) -> bytes:
    return hashlib.sha256(f"{cube_name}/{face}".encode("utf-8")).digest()


def _base_colour(cube: Cube, face: str) -> tuple[int, int, int, int]:
    material = cube.material
    if material == "underside":
        return PALETTE["underside"] if face != "down" else PALETTE["seam"]
    if material == "armor":
        return {
            "up": PALETTE["steel_light"],
            "down": PALETTE["steel_dark"],
        }.get(face, PALETTE["steel"])
    if material == "armor_dark":
        return {
            "up": PALETTE["steel"],
            "down": PALETTE["seam"],
        }.get(face, PALETTE["steel_dark"])
    if material == "leg":
        if cube.name.startswith(("foot_", "toe_")) or face == "down":
            return PALETTE["steel_dark"]
        return PALETTE["steel"] if face == "up" else PALETTE["steel_dark"]
    if material == "mandible":
        return PALETTE["steel_light"] if face == "up" else PALETTE["steel"]
    if material == "crystal":
        return PALETTE["crimson"]
    if material == "eye":
        return PALETTE["steel_dark"]
    raise ValueError(f"Unsupported material {material!r} on {cube.name}")


def _paint_metal(
    image: Image.Image,
    cube: Cube,
    face: str,
    rect: tuple[int, int, int, int],
) -> None:
    u, v, width, height = rect
    pixels = image.load()
    digest = _digest(cube.name, face)
    if width >= 3 and height >= 3:
        for x in range(u, u + width):
            pixels[x, v] = PALETTE["seam"]
            pixels[x, v + height - 1] = PALETTE["seam"]
        for y in range(v, v + height):
            pixels[u, y] = PALETTE["seam"]
            pixels[u + width - 1, y] = PALETTE["seam"]

    if width >= 3:
        edge = PALETTE["steel_highlight"] if face == "up" else PALETTE["steel_light"]
        y = v + (1 if height >= 3 else 0)
        for x in range(u + 1, u + width - 1):
            pixels[x, y] = edge
    if height >= 4 and width >= 3:
        band_y = v + 1 + digest[0] % (height - 2)
        band = PALETTE["steel_dark"] if face != "up" else PALETTE["steel"]
        for x in range(u + 1, u + width - 1, 2):
            pixels[x, band_y] = band
    if width >= 4 and height >= 4:
        chip_x = u + 1 + digest[1] % (width - 2)
        chip_y = v + 1 + digest[2] % (height - 2)
        pixels[chip_x, chip_y] = PALETTE["seam"]

    if width >= 2 and height >= 2 and ("_left" in cube.name or "_right" in cube.name):
        pixels[u + width - 1, v] = (
            PALETTE["steel_light"] if "_left" in cube.name else PALETTE["steel_dark"]
        )


def _paint_crystal(
    main: Image.Image,
    glow: Image.Image,
    cube: Cube,
    face: str,
    rect: tuple[int, int, int, int],
) -> None:
    u, v, width, height = rect
    pixels = main.load()
    glow_pixels = glow.load()
    digest = _digest(cube.name, face)
    for x in range(u, u + width):
        pixels[x, v] = PALETTE["corruption_root"]
        pixels[x, v + height - 1] = PALETTE["corruption_root"]
    for y in range(v, v + height):
        pixels[u, y] = PALETTE["corruption_root"]
        pixels[u + width - 1, y] = PALETTE["corruption_root"]

    if width >= 2 and height >= 2:
        shadow_x = u + (digest[0] % width)
        shadow_y = v + height - 1
        pixels[shadow_x, shadow_y] = PALETTE["corruption_dark"]
        violet_x = u + width - 1
        violet_y = v + (digest[1] % height)
        pixels[violet_x, violet_y] = PALETTE["violet"]

    tip_x = u + (digest[2] % width)
    tip_y = v if face in {"north", "east", "south", "west"} else v + digest[3] % height
    pixels[tip_x, tip_y] = PALETTE["magenta"]
    glow_pixels[tip_x, tip_y] = PALETTE["magenta"]


def _paint_eye(
    main: Image.Image,
    glow: Image.Image,
    rect: tuple[int, int, int, int],
) -> None:
    u, v, width, height = rect
    pixels = main.load()
    glow_pixels = glow.load()
    eye_x = u + width // 2
    eye_y = v + height // 2
    pixels[eye_x, eye_y] = PALETTE["eye"]
    glow_pixels[eye_x, eye_y] = PALETTE["eye"]
    if width > 1:
        highlight_x = max(u, eye_x - 1)
        pixels[highlight_x, eye_y] = PALETTE["eye"]
        glow_pixels[highlight_x, eye_y] = PALETTE["eye"]


def paint_images() -> tuple[Image.Image, Image.Image]:
    """Return a deterministic main atlas and aligned emissive glow mask."""

    main = Image.new("RGBA", (build.TEXTURE_SIZE, build.TEXTURE_SIZE), TRANSPARENT)
    glow = Image.new("RGBA", (build.TEXTURE_SIZE, build.TEXTURE_SIZE), TRANSPARENT)
    uvs = build._pack_uvs()
    for cube in CUBES:
        for face in build.FACE_ORDER:
            rect = uvs[(cube.name, face)]
            u, v, width, height = rect
            ImageDraw.Draw(main).rectangle(
                (u, v, u + width - 1, v + height - 1),
                fill=_base_colour(cube, face),
            )
            if cube.material == "crystal":
                _paint_crystal(main, glow, cube, face, rect)
            elif cube.material == "eye":
                _paint_eye(main, glow, rect)
            else:
                _paint_metal(main, cube, face, rect)
    return main, glow


def _atomic_png_write(path: Path, image: Image.Image) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    file_descriptor, temporary_name = tempfile.mkstemp(
        prefix=f".{path.name}.", suffix=".tmp", dir=str(path.parent)
    )
    temporary = Path(temporary_name)
    try:
        with os.fdopen(file_descriptor, "wb") as handle:
            file_descriptor = -1
            image.save(handle, format="PNG")
        os.replace(temporary, path)
    finally:
        if file_descriptor != -1:
            os.close(file_descriptor)
        temporary.unlink(missing_ok=True)


def _preview(main: Image.Image) -> Image.Image:
    scaled = main.resize((1024, 1024), resample=Image.Resampling.NEAREST)
    preview = Image.new("RGBA", scaled.size, (30, 27, 35, 255))
    draw = ImageDraw.Draw(preview)
    tile = 32
    for y in range(0, preview.height, tile):
        for x in range(0, preview.width, tile):
            colour = (48, 44, 54, 255) if (x // tile + y // tile) % 2 else (32, 29, 37, 255)
            draw.rectangle((x, y, x + tile - 1, y + tile - 1), fill=colour)
    preview.alpha_composite(scaled)
    return preview


def write_textures() -> tuple[Path, Path, Path, Path]:
    """Atomically write both atlases, preview, and textured Blockbench project."""

    main, glow = paint_images()
    _atomic_png_write(MAIN_TEXTURE_PATH, main)
    _atomic_png_write(GLOWMASK_PATH, glow)
    _atomic_png_write(PREVIEW_PATH, _preview(main))
    source = "data:image/png;base64," + base64.b64encode(
        MAIN_TEXTURE_PATH.read_bytes()
    ).decode("ascii")
    build.write_textured_bbmodel(source)
    return MAIN_TEXTURE_PATH, GLOWMASK_PATH, PREVIEW_PATH, build.BBMODEL_PATH


def main(argv: Sequence[str] | None = None) -> int:
    if argv:
        raise SystemExit("paint does not accept arguments")
    main_path, glow_path, preview_path, bbmodel_path = write_textures()
    print(f"Main texture: {main_path}")
    print(f"Glow mask: {glow_path}")
    print(f"Preview: {preview_path}")
    print(f"Blockbench: {bbmodel_path} (one embedded main texture)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
