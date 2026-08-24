from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image


VIEW_BOUNDS = {
    "front": (0, 0, 1, 1),
    "right": (1, 0, 2, 1),
    "back": (0, 1, 1, 2),
    "left": (1, 1, 2, 2),
}


def prepare(source: Path, output_dir: Path) -> None:
    with Image.open(source) as opened:
        sheet = opened.convert("RGBA")
    if sheet.width % 2 or sheet.height % 2:
        raise ValueError("Multiview sheet must have even width and height")

    half_width = sheet.width // 2
    half_height = sheet.height // 2
    output_dir.mkdir(parents=True, exist_ok=True)
    for name, (left, top, right, bottom) in VIEW_BOUNDS.items():
        view = sheet.crop(
            (
                left * half_width,
                top * half_height,
                right * half_width,
                bottom * half_height,
            )
        )
        view.save(output_dir / f"{name}.png", optimize=True)


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Crop an equal 2x2 FRONT/RIGHT/BACK/LEFT concept sheet for Tripo."
    )
    parser.add_argument("source", type=Path)
    parser.add_argument("output_dir", type=Path)
    args = parser.parse_args()
    prepare(args.source, args.output_dir)


if __name__ == "__main__":
    main()
