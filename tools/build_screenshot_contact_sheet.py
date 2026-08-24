from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--limit", type=int, default=120)
    parser.add_argument("--columns", type=int, default=5)
    args = parser.parse_args()

    files = sorted(
        args.source.glob("*.png"), key=lambda path: path.stat().st_mtime, reverse=True
    )[: args.limit]
    thumb_w, thumb_h, label_h = 256, 144, 24
    rows = (len(files) + args.columns - 1) // args.columns
    sheet = Image.new("RGB", (args.columns * thumb_w, rows * (thumb_h + label_h)), "#11131a")
    draw = ImageDraw.Draw(sheet)
    font = ImageFont.load_default()

    for index, path in enumerate(files):
        x = (index % args.columns) * thumb_w
        y = (index // args.columns) * (thumb_h + label_h)
        with Image.open(path) as image:
            image = image.convert("RGB")
            image.thumbnail((thumb_w, thumb_h), Image.Resampling.LANCZOS)
            ox = x + (thumb_w - image.width) // 2
            oy = y + (thumb_h - image.height) // 2
            sheet.paste(image, (ox, oy))
        draw.rectangle((x, y + thumb_h, x + thumb_w, y + thumb_h + label_h), fill="#1b1e28")
        draw.text((x + 6, y + thumb_h + 6), path.name, fill="#f3f4f6", font=font)

    args.output.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(args.output)
    print(args.output.resolve())


if __name__ == "__main__":
    main()
