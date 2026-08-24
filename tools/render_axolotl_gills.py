"""Render the transparent Minecraft item icon for the Axolotl Gills relic."""

from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
OUTPUTS = (
    ROOT / "src/main/resources/assets/usless_mobs/textures/item/living/axolotl_gills.png",
    ROOT / "src/main/resources/assets/usless_mobs/textures/item/ocean/staged/axolotl_gills.png",
)

OUTLINE = (79, 38, 82, 255)
SHADOW = (151, 55, 104, 255)
MID = (222, 93, 139, 255)
LIGHT = (255, 153, 176, 255)
HIGHLIGHT = (255, 211, 206, 255)
CYAN = (104, 225, 221, 255)


def rect(image: Image.Image, box: tuple[int, int, int, int], color: tuple[int, int, int, int]) -> None:
    for y in range(box[1], box[3] + 1):
        for x in range(box[0], box[2] + 1):
            image.putpixel((x, y), color)


def render() -> Image.Image:
    image = Image.new("RGBA", (32, 32), (0, 0, 0, 0))

    # Dark silhouette first: a paired relic rather than a flat pink blob.
    for box in ((6, 10, 10, 24), (21, 10, 25, 24), (10, 14, 13, 27), (18, 14, 21, 27)):
        rect(image, box, OUTLINE)
    for box in ((3, 7, 8, 12), (3, 15, 8, 20), (5, 23, 11, 27),
                (23, 7, 28, 12), (23, 15, 28, 20), (20, 23, 26, 27)):
        rect(image, box, OUTLINE)

    # Layered fronds with a darker base, midtone body and bright tips.
    for box in ((7, 11, 9, 23), (22, 11, 24, 23), (11, 15, 12, 25), (19, 15, 20, 25)):
        rect(image, box, SHADOW)
    for box in ((4, 8, 7, 11), (4, 16, 7, 19), (6, 24, 10, 26),
                (24, 8, 27, 11), (24, 16, 27, 19), (21, 24, 25, 26)):
        rect(image, box, MID)
    for box in ((5, 8, 6, 9), (5, 16, 6, 17), (7, 24, 8, 24),
                (25, 8, 26, 9), (25, 16, 26, 17), (23, 24, 24, 24)):
        rect(image, box, LIGHT)
    rect(image, (6, 11, 7, 14), LIGHT)
    rect(image, (24, 11, 25, 14), LIGHT)
    rect(image, (6, 8, 6, 8), HIGHLIGHT)
    rect(image, (25, 8, 25, 8), HIGHLIGHT)

    # Small living-water cores make the relic readable beside other pink drops.
    rect(image, (10, 18, 11, 20), CYAN)
    rect(image, (20, 18, 21, 20), CYAN)
    rect(image, (11, 17, 11, 17), HIGHLIGHT)
    rect(image, (20, 17, 20, 17), HIGHLIGHT)
    return image


if __name__ == "__main__":
    icon = render()
    for output in OUTPUTS:
        output.parent.mkdir(parents=True, exist_ok=True)
        icon.save(output)
        print(output)
