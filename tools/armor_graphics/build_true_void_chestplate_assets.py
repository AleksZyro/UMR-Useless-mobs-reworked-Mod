from io import BytesIO
from pathlib import Path

from PIL import Image, ImageDraw


REPO_ROOT = Path(__file__).resolve().parents[2]
WORN_PATH = (
    REPO_ROOT
    / "src/main/resources/assets/usless_mobs/textures/models/armor/true_void_chestplate_layer_1.png"
)
ITEM_PATH = REPO_ROOT / "src/main/resources/assets/usless_mobs/textures/item/true_void_chestplate.png"

TRANSPARENT = (0, 0, 0, 0)
VOID_BLACK = (13, 9, 20, 255)
VOID_SHADOW = (29, 22, 42, 255)
VOID_METAL = (58, 44, 76, 255)
VOID_HIGHLIGHT = (82, 62, 104, 255)
VOID_MID = (101, 61, 143, 255)
VOID_GLOW = (177, 78, 255, 255)
VOID_CORE = (225, 174, 255, 255)


def panel(draw, box, seam=False):
    x0, y0, x1, y1 = box
    draw.rectangle(box, fill=VOID_SHADOW)
    draw.line((x0, y0, x1, y0), fill=VOID_HIGHLIGHT)
    draw.line((x0, y1, x1, y1), fill=VOID_METAL)
    if seam and x1 - x0 >= 4:
        draw.line((x0 + 2, y1 - 1, x1 - 2, y1 - 1), fill=VOID_MID)


def crystal(draw, center_x, center_y, radius):
    points = (
        (center_x, center_y - radius),
        (center_x + radius, center_y),
        (center_x, center_y + radius),
        (center_x - radius, center_y),
    )
    draw.polygon(points, fill=VOID_GLOW)
    draw.line((center_x, center_y - radius + 1, center_x, center_y + radius - 1), fill=VOID_CORE)


def build_worn_texture() -> Image.Image:
    image = Image.new("RGBA", (128, 64), VOID_BLACK)
    draw = ImageDraw.Draw(image)
    for box, seam in (
        ((16, 16, 39, 31), False),
        ((40, 16, 55, 31), False),
        ((32, 48, 47, 63), False),
        ((0, 0, 11, 7), True),
        ((12, 0, 23, 7), True),
        ((24, 0, 35, 7), True),
        ((36, 0, 47, 7), True),
        ((48, 0, 57, 7), True),
        ((58, 0, 67, 7), True),
        ((68, 0, 81, 7), True),
        ((92, 0, 103, 13), False),
        ((104, 0, 115, 13), False),
        ((0, 16, 15, 27), True),
        ((24, 16, 39, 27), True),
        ((56, 32, 67, 43), True),
        ((68, 32, 79, 43), True),
        ((80, 32, 91, 43), True),
        ((92, 32, 103, 43), True),
    ):
        panel(draw, box, seam)

    for point in ((24, 0), (48, 0), (68, 0), (24, 16), (48, 16), (80, 32)):
        draw.point(point, fill=VOID_METAL)

    draw.point((84, 2), fill=VOID_CORE)
    for point in ((84, 1), (83, 2), (85, 2)):
        draw.point(point, fill=VOID_GLOW)

    crystal(draw, 118, 1, 1)
    for core, glow in (
        ((18, 18), ((18, 17), (17, 18), (19, 18))),
        ((42, 18), ((42, 17), (41, 18), (43, 18))),
    ):
        draw.point(core, fill=VOID_CORE)
        for point in glow:
            draw.point(point, fill=VOID_GLOW)
    return image


def build_item_texture() -> Image.Image:
    image = Image.new("RGBA", (16, 16), TRANSPARENT)
    draw = ImageDraw.Draw(image)

    draw.polygon(
        ((2, 3), (4, 2), (7, 3), (8, 4), (9, 3), (12, 2), (13, 3), (13, 9), (11, 12), (9, 14), (7, 14), (5, 12), (3, 9)),
        fill=VOID_BLACK,
    )
    draw.polygon(((2, 3), (4, 2), (7, 3), (6, 5), (3, 6)), fill=VOID_METAL)
    draw.polygon(((9, 3), (12, 2), (13, 3), (13, 6), (10, 5)), fill=VOID_METAL)
    draw.polygon(((3, 6), (8, 10), (13, 6), (12, 9), (8, 13), (4, 9)), fill=VOID_SHADOW)

    draw.line((3, 6, 8, 10, 13, 6), fill=VOID_METAL)
    draw.line((4, 8, 8, 11, 12, 8), fill=VOID_MID)
    draw.line((6, 10, 8, 12, 10, 10), fill=VOID_GLOW)
    draw.polygon(((8, 5), (10, 7), (8, 9), (6, 7)), fill=VOID_GLOW)
    draw.line((8, 6, 8, 8), fill=VOID_CORE)
    return image


def png_bytes(image: Image.Image) -> bytes:
    output = BytesIO()
    image.save(output, format="PNG", optimize=False, compress_level=9)
    return output.getvalue()


def main() -> None:
    WORN_PATH.parent.mkdir(parents=True, exist_ok=True)
    ITEM_PATH.parent.mkdir(parents=True, exist_ok=True)
    WORN_PATH.write_bytes(png_bytes(build_worn_texture()))
    ITEM_PATH.write_bytes(png_bytes(build_item_texture()))
    print(f"ARMOR_ASSETS_PASS WORN={WORN_PATH} ITEM={ITEM_PATH}")


if __name__ == "__main__":
    main()
