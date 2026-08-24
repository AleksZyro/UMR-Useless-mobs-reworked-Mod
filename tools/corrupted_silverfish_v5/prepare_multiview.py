from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image


def remove_green(image: Image.Image) -> Image.Image:
    rgba = image.convert("RGBA")
    pixels = []
    for red, green, blue, alpha in rgba.getdata():
        is_green = green >= 150 and green >= red * 1.35 and green >= blue * 1.35
        pixels.append((0, 0, 0, 0) if is_green else (red, green, blue, alpha))
    rgba.putdata(pixels)
    return rgba


def prepare(source: Path, output_dir: Path) -> None:
    sheet = Image.open(source).convert("RGBA")
    half_height = sheet.height // 2
    split_x = round(sheet.width * 0.415)

    quadrants = {
        "front": (0, 0, split_x, half_height),
        "right": (split_x, 0, sheet.width, half_height),
        "back": (0, half_height, split_x, sheet.height),
    }

    output_dir.mkdir(parents=True, exist_ok=True)
    prepared = {}
    for name, bounds in quadrants.items():
        isolated = remove_green(sheet.crop(bounds))
        alpha_bounds = isolated.getchannel("A").getbbox()
        if alpha_bounds is None:
            raise ValueError(f"No subject pixels found for {name}")
        subject = isolated.crop(alpha_bounds)
        canvas = Image.new("RGBA", (768, 768), (0, 0, 0, 0))
        offset = ((canvas.width - subject.width) // 2, (canvas.height - subject.height) // 2)
        canvas.alpha_composite(subject, offset)
        prepared[name] = canvas
    prepared["left"] = prepared["right"].transpose(Image.Transpose.FLIP_LEFT_RIGHT)

    for name in ("front", "left", "right", "back"):
        prepared[name].save(output_dir / f"{name}.png", optimize=True)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("output_dir", type=Path)
    args = parser.parse_args()
    prepare(args.source, args.output_dir)


if __name__ == "__main__":
    main()
