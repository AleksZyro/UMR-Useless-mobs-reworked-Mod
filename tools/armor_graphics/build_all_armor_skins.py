from __future__ import annotations

import io
import os
import runpy
import tempfile
from typing import NamedTuple
from pathlib import Path

from PIL import Image, ImageDraw


class OutputSpec(NamedTuple):
    relative_path: str
    family: str
    slot: str
    size: tuple[int, int]


FAMILY_PALETTES = {
    "void": (
        (13, 9, 20, 255), (29, 22, 42, 255), (45, 31, 61, 255),
        (58, 44, 76, 255), (101, 61, 143, 255), (177, 78, 255, 255),
        (225, 174, 255, 255),
    ),
    "celestial": (
        (55, 48, 49, 255), (102, 91, 82, 255), (166, 157, 143, 255),
        (222, 216, 196, 255), (238, 181, 51, 255), (82, 207, 229, 255),
        (221, 255, 255, 255),
    ),
    "living": (
        (25, 20, 13, 255), (55, 39, 22, 255), (80, 61, 31, 255),
        (74, 91, 30, 255), (111, 136, 42, 255), (139, 186, 55, 255),
        (220, 244, 126, 255),
    ),
    "balance": (
        (10, 8, 16, 255), (31, 24, 39, 255), (82, 75, 73, 255),
        (207, 198, 173, 255), (224, 168, 48, 255), (91, 197, 220, 255),
        (218, 157, 255, 255),
    ),
    "corrupted": (
        (20, 14, 25, 255), (46, 35, 55, 255), (84, 78, 87, 255),
        (145, 143, 140, 255), (111, 37, 93, 255), (218, 28, 115, 255),
        (255, 142, 211, 255),
    ),
    "reactor": (
        (10, 15, 16, 255), (25, 34, 34, 255), (48, 61, 58, 255),
        (76, 91, 83, 255), (42, 134, 91, 255), (55, 219, 133, 255),
        (168, 255, 214, 255),
    ),
}


def _worn(
    name: str,
    family: str,
    slot: str = "worn",
    *,
    endermite: bool = False,
    size: tuple[int, int] = (128, 64),
) -> OutputSpec:
    prefix = "src/main/mobs/endermite/resources" if endermite else "src/main/resources"
    return OutputSpec(
        f"{prefix}/assets/usless_mobs/textures/models/armor/{name}.png", family, slot, size
    )


def _item(
    name: str,
    family: str,
    slot: str,
    *,
    endermite: bool = False,
    size: tuple[int, int] = (32, 32),
) -> OutputSpec:
    prefix = "src/main/mobs/endermite/resources" if endermite else "src/main/resources"
    return OutputSpec(
        f"{prefix}/assets/usless_mobs/textures/item/{name}.png", family, slot, size
    )


OUTPUT_SPECS = (
    _worn("true_void_layer_1", "void"),
    _worn("true_void_layer_2", "void"),
    _worn("true_void_chestplate_layer_1", "void", "chestplate"),
    _worn("true_celestial_layer_1", "celestial"),
    _worn("true_celestial_layer_2", "celestial"),
    _worn("true_living_layer_1", "living"),
    _worn("true_living_layer_2", "living"),
    _worn("true_balance_layer_1", "balance"),
    _worn("true_balance_layer_2", "balance"),
    _worn("corrupted_crystal_layer_2", "corrupted", "leggings", endermite=True),
    _worn("schleimreaktor_layer_1", "reactor", "chestplate", size=(1024, 512)),
    *(
        _item(f"{family}_{slot}", palette, slot)
        for family, palette in (
            ("true_void", "void"),
            ("true_celestial", "celestial"),
            ("true_living", "living"),
            ("armor_of_balance", "balance"),
        )
        for slot in ("helmet", "chestplate", "leggings", "boots")
    ),
    _item("corrupted_crystal_leggings", "corrupted", "leggings", endermite=True, size=(128, 128)),
    _item("schleimreaktor_brustpanzer", "reactor", "chestplate", size=(256, 256)),
)

# The bespoke True Void chestplate icon intentionally retains its compact vanilla-sized source.
OUTPUT_SPECS = tuple(
    spec._replace(size=(16, 16))
    if spec.relative_path.endswith("/true_void_chestplate.png")
    else spec
    for spec in OUTPUT_SPECS
)


def png_bytes(image: Image.Image) -> bytes:
    stream = io.BytesIO()
    image.save(stream, format="PNG", optimize=False, compress_level=9)
    return stream.getvalue()


def _opaque(mask: Image.Image, x: int, y: int) -> bool:
    return 0 <= x < mask.width and 0 <= y < mask.height and mask.getpixel((x, y)) > 0


def _cell_hash(x: int, y: int, seed: int) -> int:
    return ((x * 73856093) ^ (y * 19349663) ^ (seed * 83492791)) & 0xFFFFFFFF


def _base_index(x: int, y: int, height: int, seed: int, scale: int) -> int:
    vertical_light = 3 if y * 3 < height else 2 if y * 3 < height * 2 else 1
    cell = max(1, 2 * scale)
    value = _cell_hash(x // cell, y // cell, seed) % 11
    variation = -1 if value < 3 else 1 if value < 6 else 0
    return max(0, min(4, vertical_light + variation))


def _material_colour(
    family: str, x: int, y: int, width: int, height: int, seed: int, scale: int
):
    palette = FAMILY_PALETTES[family]
    index = _base_index(x, y, height, seed, scale)
    cell = max(1, 2 * scale)
    value = _cell_hash(x // cell, y // cell, seed + 17) % 13
    if family == "balance":
        index = 1 if x < width // 2 else 3
        variation = -1 if value < 4 else 1 if value < 8 else 0
        index = max(0, min(4, index + variation))
    elif family == "living" and value in (0, 1, 2):
        index = 4
    elif family == "corrupted" and value in (0, 1):
        index = 4
    elif family == "reactor" and value in (0, 1):
        index = 4
    return palette[index]


def _accent_positions(family: str, width: int, height: int, seed: int):
    if family == "void":
        return lambda x, y: (x + y * 2 + seed) % 29 == 0
    if family == "celestial":
        return lambda x, y: (x * 2 + y * 3 + seed) % 31 == 0
    if family == "living":
        return lambda x, y: (x * 5 - y * 2 + seed) % 23 == 0
    if family == "balance":
        return lambda x, y: abs(x - width // 2) <= 1 and (y + seed) % 5 == 0
    if family == "corrupted":
        return lambda x, y: (x - y * 2 + seed) % 17 == 0
    return lambda x, y: (x + seed) % 11 == 0 and y % 4 in (1, 2)


def _paint_from_alpha(source: Image.Image, family: str, slot: str) -> Image.Image:
    source = source.convert("RGBA")
    alpha = source.getchannel("A")
    result = Image.new("RGBA", source.size, (0, 0, 0, 0))
    pixels = result.load()
    seed = sum(ord(char) for char in family + slot)
    scale = max(1, source.width // 64)
    accent = _accent_positions(family, source.width, source.height, seed)
    palette = FAMILY_PALETTES[family]
    opaque_points = []
    for y in range(source.height):
        for x in range(source.width):
            value = alpha.getpixel((x, y))
            if value == 0:
                continue
            opaque_points.append((x, y))
            colour = _material_colour(
                family, x, y, source.width, source.height, seed, scale
            )
            top_edge = not _opaque(alpha, x, y - 1)
            bottom_edge = not _opaque(alpha, x, y + 1)
            left_edge = not _opaque(alpha, x - 1, y)
            if bottom_edge:
                colour = palette[0]
            elif top_edge or left_edge:
                colour = palette[3]
            elif _cell_hash(x // scale, y // scale, seed + 43) % 37 == 0:
                colour = palette[1]
            elif accent(x, y):
                colour = palette[5]
            pixels[x, y] = (*colour[:3], value)

    if opaque_points:
        centre_x = source.width // 2
        centre_y = source.height // 2
        fully_opaque = [point for point in opaque_points if alpha.getpixel(point) == 255]
        core = min(fully_opaque or opaque_points, key=lambda point: (
            abs(point[0] - centre_x) + abs(point[1] - centre_y), point[1], point[0]
        ))
        pixels[core[0], core[1]] = palette[-1]
    return result


def build_documents(repo_root: Path) -> dict[str, Image.Image]:
    documents = {}
    void_builder = runpy.run_path(
        str(repo_root / "tools/armor_graphics/build_true_void_chestplate_assets.py")
    )
    for spec in OUTPUT_SPECS:
        path = repo_root / spec.relative_path
        if not path.is_file():
            raise FileNotFoundError(f"missing armor texture source: {path}")
        with Image.open(path) as source:
            source.load()
            if source.size != spec.size:
                raise ValueError(
                    f"{spec.relative_path}: expected {spec.size[0]}x{spec.size[1]}, "
                    f"got {source.width}x{source.height}"
                )
            if spec.relative_path.endswith("/true_void_chestplate_layer_1.png"):
                documents[spec.relative_path] = void_builder["build_worn_texture"]()
            elif spec.relative_path.endswith("/true_void_chestplate.png"):
                documents[spec.relative_path] = void_builder["build_item_texture"]()
            elif spec.relative_path.endswith("/corrupted_crystal_layer_2.png"):
                base = source.resize((64, 32), Image.Resampling.NEAREST)
                detailed = _paint_from_alpha(base, spec.family, spec.slot)
                documents[spec.relative_path] = detailed.resize(
                    spec.size, Image.Resampling.NEAREST
                )
            else:
                documents[spec.relative_path] = _paint_from_alpha(source, spec.family, spec.slot)
    return documents


def _stage_payload(path: Path, payload: bytes, suffix: str) -> Path:
    path.parent.mkdir(parents=True, exist_ok=True)
    descriptor, temporary_name = tempfile.mkstemp(
        prefix=f".{path.name}.", suffix=suffix, dir=path.parent
    )
    temporary = Path(temporary_name)
    try:
        with os.fdopen(descriptor, "wb") as stream:
            stream.write(payload)
            stream.flush()
            os.fsync(stream.fileno())
    except BaseException:
        if temporary.exists():
            temporary.unlink()
        raise
    return temporary


def publish_documents(repo_root: Path, documents: dict[str, Image.Image]) -> None:
    staged = {}
    backups = {}
    prepared = []
    try:
        for relative_path, image in documents.items():
            target = repo_root / relative_path
            staged[relative_path] = _stage_payload(target, png_bytes(image), ".candidate")
        for relative_path in documents:
            target = repo_root / relative_path
            backup = None
            if target.exists():
                descriptor, backup_name = tempfile.mkstemp(
                    prefix=f".{target.name}.", suffix=".backup", dir=target.parent
                )
                os.close(descriptor)
                backup = Path(backup_name)
                os.replace(target, backup)
            backups[relative_path] = backup
            prepared.append(relative_path)
            os.replace(staged[relative_path], target)
        for backup in backups.values():
            if backup is not None and backup.exists():
                backup.unlink()
    except BaseException:
        for relative_path in reversed(prepared):
            target = repo_root / relative_path
            backup = backups.get(relative_path)
            if backup is not None and backup.exists():
                os.replace(backup, target)
            elif target.exists():
                target.unlink()
        raise
    finally:
        for candidate in staged.values():
            if candidate.exists():
                candidate.unlink()
        for backup in backups.values():
            if backup is not None and backup.exists():
                backup.unlink()


def build_contact_sheet(documents: dict[str, Image.Image]) -> Image.Image:
    sheet = Image.new("RGBA", (1200, 1200), (15, 14, 20, 255))
    draw = ImageDraw.Draw(sheet)
    cell_width, cell_height = 240, 200
    for index, spec in enumerate(OUTPUT_SPECS):
        column, row = index % 5, index // 5
        left, top = column * cell_width, row * cell_height
        draw.rectangle(
            (left + 5, top + 5, left + cell_width - 5, top + cell_height - 5),
            fill=(24, 22, 30, 255),
            outline=(58, 53, 69, 255),
        )
        image = documents[spec.relative_path]
        scale = min(200 / image.width, 150 / image.height)
        preview = image.resize(
            (max(1, round(image.width * scale)), max(1, round(image.height * scale))),
            Image.Resampling.NEAREST,
        )
        x = left + (cell_width - preview.width) // 2
        y = top + 28 + (150 - preview.height) // 2
        sheet.alpha_composite(preview, (x, y))
        draw.text((left + 10, top + 10), f"{spec.family} / {spec.slot}", fill=(231, 226, 239, 255))
        draw.text((left + 10, top + 180), Path(spec.relative_path).name, fill=(153, 145, 166, 255))
    return sheet


def write_all(repo_root: Path) -> None:
    documents = build_documents(repo_root)
    publish_documents(repo_root, documents)
    review_path = repo_root / "Modelle/Exports/armor_graphics_review/all_armor_skins_contact.png"
    candidate = _stage_payload(review_path, png_bytes(build_contact_sheet(documents)), ".candidate")
    try:
        os.replace(candidate, review_path)
    finally:
        if candidate.exists():
            candidate.unlink()


if __name__ == "__main__":
    write_all(Path(__file__).resolve().parents[2])
