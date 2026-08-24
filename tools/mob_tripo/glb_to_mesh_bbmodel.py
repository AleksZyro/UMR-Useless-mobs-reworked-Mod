"""Losslessly convert a Tripo triangle mesh to a textured Blockbench mesh.

The conversion deliberately does not voxelise, merge, decimate or rebuild the
source.  Every indexed GLB triangle is emitted once.  The four approved views
provide projection UVs because Tripo's geometry-only downloads have none.
"""

from __future__ import annotations

import argparse
import base64
from io import BytesIO
import json
import math
import os
from pathlib import Path
import tempfile
from typing import Any, Dict, Mapping
from uuid import UUID, uuid5

import numpy as np
from PIL import Image

from tools.armor_graphics.tripo_to_blockbench import Geometry, load_geometry


UUID_NAMESPACE = UUID("25849624-7343-55f4-8f70-e51b5c2d86e8")
VIEW_NAMES = ("front", "back", "left", "right")


def _stable_uuid(kind: str, index: int | str) -> str:
    return str(uuid5(UUID_NAMESPACE, f"{kind}:{index}"))


def transformed_positions(positions: np.ndarray, longest_span: float = 24.0) -> np.ndarray:
    points = np.asarray(positions, dtype=np.float64)
    if points.ndim != 2 or points.shape[1] != 3 or len(points) < 3:
        raise ValueError("Mesh positions must be a non-empty Nx3 array")
    if not np.isfinite(points).all() or not math.isfinite(longest_span) or longest_span <= 0:
        raise ValueError("Mesh coordinates and target span must be finite and positive")
    lower = points.min(axis=0)
    upper = points.max(axis=0)
    source_span = float((upper - lower).max())
    if source_span <= 0:
        raise ValueError("Mesh bounds have zero size")
    result = points * (float(longest_span) / source_span)
    result[:, 0] -= (result[:, 0].min() + result[:, 0].max()) / 2.0
    result[:, 2] -= (result[:, 2].min() + result[:, 2].max()) / 2.0
    result[:, 1] -= result[:, 1].min()
    return result


def _load_view(path: Path, cell_size: int) -> Image.Image:
    if not path.is_file():
        raise ValueError(f"Missing required view: {path}")
    with Image.open(path) as source:
        image = source.convert("RGBA")
        image.load()
    pixels = np.asarray(image, dtype=np.uint8).copy()
    rgb = pixels[:, :, :3].astype(np.int16)
    green = (
        (rgb[:, :, 1] >= 140)
        & (rgb[:, :, 1] >= rgb[:, :, 0] + 55)
        & (rgb[:, :, 1] >= rgb[:, :, 2] + 55)
    )
    pixels[green, 3] = 0
    image = Image.fromarray(pixels)
    image.thumbnail((cell_size, cell_size), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (cell_size, cell_size), (0, 0, 0, 0))
    canvas.alpha_composite(image, ((cell_size - image.width) // 2, (cell_size - image.height) // 2))
    return canvas


def build_texture_atlas(views: Path, cell_size: int = 512) -> Image.Image:
    if cell_size < 8 or cell_size > 2048:
        raise ValueError("Atlas cell size must be between 8 and 2048")
    atlas = Image.new("RGBA", (cell_size * 2, cell_size * 2), (0, 0, 0, 0))
    locations = {"front": (0, 0), "back": (1, 0), "left": (0, 1), "right": (1, 1)}
    for name in VIEW_NAMES:
        cell = _load_view(Path(views) / f"{name}.png", cell_size)
        column, row = locations[name]
        atlas.alpha_composite(cell, (column * cell_size, row * cell_size))
    return atlas


def _normalised(points: np.ndarray) -> np.ndarray:
    lower = points.min(axis=0)
    span = points.max(axis=0) - lower
    return np.clip((points - lower) / np.where(span > 0, span, 1.0), 0.0, 1.0)


def _projection_for_triangle(points: np.ndarray) -> tuple[str, np.ndarray]:
    edge_a = points[1] - points[0]
    edge_b = points[2] - points[0]
    normal = np.cross(edge_a, edge_b)
    if abs(float(normal[2])) >= abs(float(normal[0])):
        if normal[2] <= 0:
            return "front", np.column_stack((points[:, 0], 1.0 - points[:, 1]))
        return "back", np.column_stack((1.0 - points[:, 0], 1.0 - points[:, 1]))
    if normal[0] <= 0:
        return "left", np.column_stack((1.0 - points[:, 2], 1.0 - points[:, 1]))
    return "right", np.column_stack((points[:, 2], 1.0 - points[:, 1]))


def _cell_uv(name: str, unit_uv: np.ndarray) -> np.ndarray:
    column, row = {"front": (0, 0), "back": (1, 0), "left": (0, 1), "right": (1, 1)}[name]
    return np.column_stack(((unit_uv[:, 0] + column) * 8.0, (unit_uv[:, 1] + row) * 8.0))


def _png_source(image: Image.Image) -> str:
    output = BytesIO()
    image.save(output, format="PNG", optimize=False, compress_level=9)
    return "data:image/png;base64," + base64.b64encode(output.getvalue()).decode("ascii")


def build_document(name: str, geometry: Geometry, atlas: Image.Image, longest_span: float = 24.0) -> Dict[str, Any]:
    positions = transformed_positions(geometry.positions, longest_span)
    normalised = _normalised(positions)
    triangles = np.asarray(geometry.triangles, dtype=np.int64)
    if triangles.ndim != 2 or triangles.shape[1] != 3 or not len(triangles):
        raise ValueError("Mesh triangles must be a non-empty Nx3 array")
    if triangles.min() < 0 or triangles.max() >= len(positions):
        raise ValueError("Mesh triangle index exceeds vertex count")
    vertex_ids = [_stable_uuid("vertex", index) for index in range(len(positions))]
    vertices = {
        vertex_ids[index]: [round(float(value), 8) for value in point]
        for index, point in enumerate(positions)
    }
    faces: Dict[str, Any] = {}
    for face_index, triangle in enumerate(triangles):
        ids = [vertex_ids[int(index)] for index in triangle]
        view, projected = _projection_for_triangle(normalised[triangle])
        uv = _cell_uv(view, projected)
        faces[_stable_uuid("face", face_index)] = {
            "uv": {vertex_id: [round(float(value), 6) for value in pair] for vertex_id, pair in zip(ids, uv)},
            "texture": 0,
            "vertices": ids,
        }
    element_uuid = _stable_uuid("element", name)
    mesh = {
        "name": name,
        "color": 0,
        "origin": [0, 0, 0],
        "rotation": [0, 0, 0],
        "shading": True,
        "export": True,
        "visibility": True,
        "locked": False,
        "render_order": "default",
        "scope": "",
        "allow_mirror_modeling": True,
        "vertices": vertices,
        "faces": faces,
        "type": "mesh",
        "uuid": element_uuid,
    }
    return {
        "meta": {"format_version": "5.0", "model_format": "free", "box_uv": False},
        "name": name,
        "model_identifier": name.lower().replace(" ", "_"),
        "visible_box": [1, 1, 0],
        "variable_placeholders": "",
        "multi_file_ruleset": "",
        "variable_placeholder_buttons": [],
        "timeline_setups": [],
        "unhandled_root_fields": {},
        "resolution": {"width": 16, "height": 16},
        "elements": [mesh],
        "groups": [],
        "textures": [{
            "name": "tripo_multiview_atlas.png", "path": "", "folder": "", "namespace": "", "id": "0",
            "group": "", "scope": 0, "width": atlas.width, "height": atlas.height,
            "uv_width": 16, "uv_height": 16, "particle": False, "use_as_default": True,
            "layers_enabled": False, "sync_to_project": "", "file_format": "png",
            "render_mode": "default", "render_sides": "auto", "wrap_mode": "limited",
            "pbr_channel": "color", "fps": 7, "frame_time": 1, "frame_order_type": "loop",
            "frame_order": "", "frame_interpolate": False, "visible": True, "internal": True,
            "saved": False, "uuid": _stable_uuid("texture", name), "source": _png_source(atlas),
        }],
        "animations": [],
        "outliner": [element_uuid],
    }


def write_document(path: Path, document: Mapping[str, Any]) -> None:
    target = Path(path)
    target.parent.mkdir(parents=True, exist_ok=True)
    descriptor, temp_name = tempfile.mkstemp(prefix=f".{target.name}.", suffix=".tmp", dir=target.parent)
    try:
        with os.fdopen(descriptor, "w", encoding="utf-8", newline="\n") as handle:
            json.dump(document, handle, ensure_ascii=False, separators=(",", ":"), allow_nan=False)
            handle.write("\n")
            handle.flush()
            os.fsync(handle.fileno())
        os.replace(temp_name, target)
    finally:
        try:
            Path(temp_name).unlink(missing_ok=True)
        except OSError:
            pass


def convert(glb: Path, views: Path, output: Path, name: str, longest_span: float = 24.0, cell_size: int = 512) -> Dict[str, int]:
    geometry = load_geometry(Path(glb))
    atlas = build_texture_atlas(Path(views), cell_size)
    document = build_document(name, geometry, atlas, longest_span)
    write_document(Path(output), document)
    return {"vertices": len(geometry.positions), "triangles": len(geometry.triangles), "cubes": 0}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--glb", type=Path, required=True)
    parser.add_argument("--views", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--name", required=True)
    parser.add_argument("--span", type=float, default=24.0)
    parser.add_argument("--cell-size", type=int, default=512)
    args = parser.parse_args()
    try:
        result = convert(args.glb, args.views, args.output, args.name, args.span, args.cell_size)
    except (OSError, ValueError, json.JSONDecodeError) as exc:
        print(f"MESH_CONVERSION_FAILED: {exc}")
        return 1
    print(f"MESH_CONVERSION_PASS: VERTICES={result['vertices']} TRIANGLES={result['triangles']} CUBES=0 OUTPUT={args.output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
