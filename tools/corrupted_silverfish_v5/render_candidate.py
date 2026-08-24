"""Render fixed visual-review views of the v5 Tripo cuboid candidate."""

from __future__ import annotations

import argparse
import base64
from io import BytesIO
import json
import math
from pathlib import Path
from typing import Iterable, Mapping, Tuple

from PIL import Image

from tools.corrupted_silverfish_v3.render import (
    _png_bytes,
    _publish_transaction,
    camera_for,
    identity_matrix,
    render_cubes,
)
from tools.corrupted_silverfish_v5.tripo_voxel import DEFAULT_MODEL, EXPORT_ROOT, FACE_NAMES


CANVAS_SIZE = (640, 640)
REVIEW_ROOT = EXPORT_ROOT / "review"
VIEWS = (
    ("front", "front.png"),
    ("right", "right.png"),
    ("back", "back.png"),
    ("top", "top.png"),
    ("three_quarter", "perspective.png"),
)


def _load_model(path: Path):
    try:
        document = json.loads(Path(path).read_text(encoding="utf-8"))
        texture_source = document["textures"][0]["source"]
        encoded = texture_source.split(",", 1)[1]
        with Image.open(BytesIO(base64.b64decode(encoded, validate=True))) as image:
            texture = image.convert("RGBA")
    except (OSError, ValueError, KeyError, IndexError, json.JSONDecodeError) as exc:
        raise ValueError(f"Invalid v5 Blockbench candidate: {path}") from exc

    cubes = []
    all_points = []
    for element in document.get("elements", []):
        lower = tuple(float(value) for value in element["from"])
        upper = tuple(float(value) for value in element["to"])
        size = tuple(upper[index] - lower[index] for index in range(3))
        if any(not math.isfinite(value) or value <= 0 for value in size):
            raise ValueError(f"Invalid cuboid bounds: {element.get('name')}")
        uv_faces = {}
        for face in FACE_NAMES:
            rectangle = element["faces"][face]["uv"]
            uv_faces[face] = {
                "uv": rectangle[:2],
                "uv_size": [rectangle[2] - rectangle[0], rectangle[3] - rectangle[1]],
            }
        cube = {
            "name": element["name"],
            "origin": lower,
            "size": size,
            "rotation": (0, 0, 0),
            "uv": uv_faces,
        }
        cubes.append((cube, identity_matrix()))
        all_points.extend((lower, upper))
    if not cubes:
        raise ValueError("Blockbench candidate contains no cuboids")
    lower = tuple(min(point[axis] for point in all_points) for axis in range(3))
    upper = tuple(max(point[axis] for point in all_points) for axis in range(3))
    center = tuple((lower[axis] + upper[axis]) / 2 for axis in range(3))
    return cubes, texture, lower, upper, center


def _scale_for(camera, lower, upper) -> float:
    points = [
        (x, y, z)
        for x in (lower[0], upper[0])
        for y in (lower[1], upper[1])
        for z in (lower[2], upper[2])
    ]
    screen_x, screen_y = camera[1], camera[2]
    width = max(sum(p[i] * screen_x[i] for i in range(3)) for p in points) - min(
        sum(p[i] * screen_x[i] for i in range(3)) for p in points
    )
    height = max(sum(p[i] * screen_y[i] for i in range(3)) for p in points) - min(
        sum(p[i] * screen_y[i] for i in range(3)) for p in points
    )
    return min((CANVAS_SIZE[0] - 72) / max(width, 1), (CANVAS_SIZE[1] - 72) / max(height, 1))


def render_review_set(model_path: Path = DEFAULT_MODEL, output_root: Path = REVIEW_ROOT) -> Tuple[Path, ...]:
    cubes, texture, lower, upper, center = _load_model(Path(model_path))
    payloads = []
    paths = []
    for view, filename in VIEWS:
        camera = camera_for(view)
        image = render_cubes(
            cubes,
            texture,
            camera,
            canvas_size=CANVAS_SIZE,
            pixels_per_unit=_scale_for(camera, lower, upper),
            center_world=center,
        )
        path = Path(output_root) / filename
        payloads.append((path, _png_bytes(image)))
        paths.append(path)
    _publish_transaction(payloads)
    return tuple(paths)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--model", type=Path, default=DEFAULT_MODEL)
    parser.add_argument("--output", type=Path, default=REVIEW_ROOT)
    args = parser.parse_args()
    paths = render_review_set(args.model, args.output)
    print(f"TRIPO_RENDER_PASS FILES={len(paths)} OUTPUT={args.output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
