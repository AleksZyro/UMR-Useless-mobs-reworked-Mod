from __future__ import annotations

import argparse
from pathlib import Path
import sys
from typing import Mapping

from PIL import Image

if __package__ in (None, ""):
    sys.path.insert(0, str(Path(__file__).resolve().parents[2]))

from tools.armor_graphics.prepare_tripo_multiview import _remove_connected_background


OCELOT_BOXES = {
    "front": (60, 90, 315, 570),
    "left": (380, 90, 1245, 570),
    "back": (60, 600, 315, 1120),
    "right": (335, 635, 1245, 1095),
}


def prepare_explicit_views(
    source: Path,
    output_dir: Path,
    boxes: Mapping[str, tuple[int, int, int, int]],
    *,
    canvas_size: int = 1024,
    padding: int = 48,
) -> dict[str, Path]:
    """Extract unevenly arranged views while preserving one shared subject scale."""
    if canvas_size <= padding * 2:
        raise ValueError("canvas must be larger than twice the padding")

    with Image.open(source) as opened:
        sheet = opened.convert("RGBA")

    subjects: dict[str, Image.Image] = {}
    for name, box in boxes.items():
        left, top, right, bottom = box
        if left < 0 or top < 0 or right > sheet.width or bottom > sheet.height or right <= left or bottom <= top:
            raise ValueError(f"invalid crop for {name}: {box}")
        isolated = _remove_connected_background(sheet.crop(box))
        alpha_box = isolated.getchannel("A").getbbox()
        if alpha_box is None:
            raise ValueError(f"view {name} contains no visible subject")
        subjects[name] = isolated.crop(alpha_box)

    usable = canvas_size - padding * 2
    scale = min(
        usable / max(subject.width for subject in subjects.values()),
        usable / max(subject.height for subject in subjects.values()),
    )

    output_dir.mkdir(parents=True, exist_ok=True)
    outputs: dict[str, Path] = {}
    for name, subject in subjects.items():
        size = (max(1, round(subject.width * scale)), max(1, round(subject.height * scale)))
        resized = subject.resize(size, Image.Resampling.LANCZOS)
        canvas = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
        canvas.alpha_composite(resized, ((canvas_size - resized.width) // 2, (canvas_size - resized.height) // 2))
        target = output_dir / f"{name}.png"
        canvas.save(target, format="PNG", optimize=True)
        outputs[name] = target
    return outputs


def main() -> int:
    parser = argparse.ArgumentParser(description="Prepare the uneven Ocelot 2x2 concept sheet for Tripo Multi-View.")
    parser.add_argument("source", type=Path)
    parser.add_argument("output_dir", type=Path)
    args = parser.parse_args()
    outputs = prepare_explicit_views(args.source, args.output_dir, OCELOT_BOXES)
    print("TRIPO_VIEWS_READY " + " ".join(f"{name}={path}" for name, path in outputs.items()))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
