from __future__ import annotations

import argparse
from collections import deque
from pathlib import Path

from PIL import Image


OUTPUT_SIZE = 768
GRID_TRIM = 4


def _is_background(pixel: tuple[int, int, int, int]) -> bool:
    red, green, blue, alpha = pixel
    if alpha == 0:
        return True
    neutral_bright = min(red, green, blue) >= 220 and max(red, green, blue) - min(red, green, blue) <= 18
    chroma_green = green >= 140 and green >= red * 1.30 and green >= blue * 1.30
    return neutral_bright or chroma_green


def _remove_connected_background(image: Image.Image) -> Image.Image:
    result = image.convert("RGBA")
    pixels = result.load()
    width, height = result.size
    queue: deque[tuple[int, int]] = deque()
    visited: set[tuple[int, int]] = set()

    for x in range(width):
        queue.append((x, 0))
        queue.append((x, height - 1))
    for y in range(height):
        queue.append((0, y))
        queue.append((width - 1, y))

    while queue:
        x, y = queue.popleft()
        if (x, y) in visited:
            continue
        visited.add((x, y))
        if not _is_background(pixels[x, y]):
            continue
        pixels[x, y] = (0, 0, 0, 0)
        if x > 0:
            queue.append((x - 1, y))
        if x + 1 < width:
            queue.append((x + 1, y))
        if y > 0:
            queue.append((x, y - 1))
        if y + 1 < height:
            queue.append((x, y + 1))

    return result


def _center_on_canvas(image: Image.Image) -> Image.Image:
    alpha_box = image.getchannel("A").getbbox()
    if alpha_box is None:
        raise ValueError("view contains no visible pixels after background removal")
    subject = image.crop(alpha_box)
    if subject.width > OUTPUT_SIZE or subject.height > OUTPUT_SIZE:
        scale = min(OUTPUT_SIZE / subject.width, OUTPUT_SIZE / subject.height)
        size = (max(1, round(subject.width * scale)), max(1, round(subject.height * scale)))
        subject = subject.resize(size, Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (OUTPUT_SIZE, OUTPUT_SIZE), (0, 0, 0, 0))
    canvas.alpha_composite(subject, ((OUTPUT_SIZE - subject.width) // 2, (OUTPUT_SIZE - subject.height) // 2))
    return canvas


def _trim_grid_edge(box: tuple[int, int, int, int]) -> tuple[int, int, int, int]:
    left, top, right, bottom = box
    if right - left <= GRID_TRIM * 2 or bottom - top <= GRID_TRIM * 2:
        raise ValueError("grid cell is too small")
    return left + GRID_TRIM, top + GRID_TRIM, right - GRID_TRIM, bottom - GRID_TRIM


def prepare(source: Path, output_dir: Path) -> dict[str, Path]:
    with Image.open(source) as opened:
        sheet = opened.convert("RGBA")
    width, height = sheet.size
    if width < 2 or height < 2:
        raise ValueError("source sheet must contain a 2x2 view grid")
    middle_x, middle_y = width // 2, height // 2
    boxes = {
        "front": (0, 0, middle_x, middle_y),
        "right": (middle_x, 0, width, middle_y),
        "back": (0, middle_y, middle_x, height),
        "top": (middle_x, middle_y, width, height),
    }
    output_dir.mkdir(parents=True, exist_ok=True)
    outputs: dict[str, Path] = {}
    for name, box in boxes.items():
        view = _center_on_canvas(_remove_connected_background(sheet.crop(_trim_grid_edge(box))))
        target = output_dir / f"{name}.png"
        view.save(target, format="PNG", optimize=True)
        outputs[name] = target
    return outputs


def prepare_left_views(source: Path, family_dir: Path) -> dict[str, Path]:
    """Split a 2x2 family sheet ordered helmet/chestplate/leggings/boots."""
    with Image.open(source) as opened:
        sheet = opened.convert("RGBA")
    width, height = sheet.size
    if width < 2 or height < 2:
        raise ValueError("left-view sheet must contain a 2x2 piece grid")
    middle_x, middle_y = width // 2, height // 2
    boxes = {
        "helmet": (0, 0, middle_x, middle_y),
        "chestplate": (middle_x, 0, width, middle_y),
        "leggings": (0, middle_y, middle_x, height),
        "boots": (middle_x, middle_y, width, height),
    }
    outputs: dict[str, Path] = {}
    for name, box in boxes.items():
        view = _center_on_canvas(_remove_connected_background(sheet.crop(_trim_grid_edge(box))))
        piece_dir = family_dir / name
        piece_dir.mkdir(parents=True, exist_ok=True)
        target = piece_dir / "left.png"
        view.save(target, format="PNG", optimize=True)
        outputs[name] = target
    return outputs


def main() -> int:
    parser = argparse.ArgumentParser(description="Prepare a generated 2x2 armour sheet for Tripo multi-view input.")
    parser.add_argument("source", type=Path)
    parser.add_argument("output_dir", type=Path)
    args = parser.parse_args()
    outputs = prepare(args.source, args.output_dir)
    print("TRIPO_VIEWS_READY " + " ".join(f"{name}={path}" for name, path in outputs.items()))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
