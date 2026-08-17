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


def build_worn_texture() -> Image.Image:
    image = Image.new("RGBA", (128, 64), VOID_BLACK)
    pixels = image.load()
    for y in range(64):
        for x in range(128):
            if (x // 3 + y // 3) % 2 == 0:
                pixels[x, y] = VOID_SHADOW
            if (x + 2 * y) % 11 == 0:
                pixels[x, y] = VOID_METAL

    draw = ImageDraw.Draw(image)
    for y in (3, 12, 19, 27, 36, 45, 54, 61):
        draw.line((0, y, 127, y), fill=VOID_HIGHLIGHT, width=1)
    for start, end in ((0, 11), (14, 25), (28, 34), (36, 55), (58, 75), (78, 98), (100, 122)):
        draw.line((start, 1, end, 1), fill=VOID_MID, width=1)
        draw.line((start + 1, 2, end - 1, 2), fill=VOID_GLOW, width=1)
    draw.line((0, 17, 21, 17), fill=VOID_MID, width=1)
    draw.line((1, 18, 20, 18), fill=VOID_GLOW, width=1)
    draw.polygon(((46, 18), (49, 21), (46, 24), (43, 21)), fill=VOID_GLOW)
    draw.point((46, 21), fill=VOID_CORE)
    return image


def build_item_texture() -> Image.Image:
    image = Image.new("RGBA", (16, 16), TRANSPARENT)
    draw = ImageDraw.Draw(image)

    draw.polygon(
        ((2, 3), (4, 1), (7, 2), (8, 3), (9, 2), (12, 1), (14, 3), (13, 13), (10, 15), (6, 15), (3, 13)),
        fill=VOID_BLACK,
    )
    draw.polygon(((3, 4), (5, 2), (7, 3), (8, 5), (9, 3), (11, 2), (13, 4), (12, 12), (9, 14), (7, 14), (4, 12)), fill=VOID_METAL)
    draw.polygon(((4, 5), (7, 6), (8, 8), (9, 6), (12, 5), (11, 10), (9, 12), (7, 12), (5, 10)), fill=VOID_SHADOW)
    draw.line((3, 4, 7, 6), fill=VOID_MID, width=1)
    draw.line((13, 4, 9, 6), fill=VOID_MID, width=1)
    draw.line((4, 11, 7, 13), fill=VOID_GLOW, width=1)
    draw.line((12, 11, 9, 13), fill=VOID_GLOW, width=1)
    draw.polygon(((8, 5), (10, 7), (8, 9), (6, 7)), fill=VOID_GLOW)
    draw.point((8, 6), fill=VOID_CORE)
    draw.point((8, 7), fill=VOID_CORE)
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
