"""Convert Tripo's geometry-only GLBs and approved multiview PNGs to cuboid bbmodels."""

from __future__ import annotations

from dataclasses import dataclass
import argparse
import base64
from collections import Counter
from io import BytesIO
import json
import math
from pathlib import Path
import struct
from typing import Dict, Iterable, Mapping, Sequence, Tuple
from uuid import UUID, uuid5

import numpy as np
from PIL import Image, ImageDraw

try:
    import meshoptimizer
except ImportError as exc:  # pragma: no cover - exercised by the CLI environment
    raise RuntimeError("meshoptimizer is required to decode Tripo GLBs") from exc


GLB_MAGIC = 0x46546C67
JSON_CHUNK = 0x4E4F534A
BIN_CHUNK = 0x004E4942
FACE_NAMES = ("north", "east", "south", "west", "up", "down")
VIEW_NAMES = ("front", "left", "right", "back", "top")
UUID_NAMESPACE = UUID("91646df7-cb75-56ab-a65c-f408587d17bb")


@dataclass(frozen=True)
class Geometry:
    positions: np.ndarray
    triangles: np.ndarray


@dataclass(frozen=True)
class View:
    pixels: np.ndarray
    bbox: Tuple[int, int, int, int]


@dataclass(frozen=True)
class Cuboid:
    lower: Tuple[int, int, int]
    upper: Tuple[int, int, int]
    colour: int


@dataclass(frozen=True)
class Candidate:
    cuboids: Tuple[Cuboid, ...]
    palette: Tuple[Tuple[int, int, int, int], ...]
    occupied_voxel_count: int

    @property
    def cuboid_count(self) -> int:
        return len(self.cuboids)


def _glb_chunks(path: Path) -> Tuple[dict, bytes]:
    payload = Path(path).read_bytes()
    if len(payload) < 20:
        raise ValueError(f"GLB file is too short: {path}")
    magic, version, declared = struct.unpack_from("<III", payload, 0)
    if magic != GLB_MAGIC or version != 2 or declared != len(payload):
        raise ValueError(f"Invalid GLB 2.0 header: {path}")
    offset = 12
    chunks = {}
    while offset < len(payload):
        if offset + 8 > len(payload):
            raise ValueError("Truncated GLB chunk header")
        length, kind = struct.unpack_from("<II", payload, offset)
        offset += 8
        end = offset + length
        if end > len(payload):
            raise ValueError("Truncated GLB chunk")
        chunks[kind] = payload[offset:end]
        offset = end
    if JSON_CHUNK not in chunks or BIN_CHUNK not in chunks:
        raise ValueError("GLB requires JSON and BIN chunks")
    document = json.loads(chunks[JSON_CHUNK].decode("utf-8").rstrip(" \t\r\n\0"))
    return document, chunks[BIN_CHUNK]


def _decoded_view(document: dict, binary: bytes, view_index: int) -> bytes:
    try:
        view = document["bufferViews"][view_index]
        extension = view.get("extensions", {}).get("EXT_meshopt_compression")
    except (KeyError, IndexError, TypeError) as exc:
        raise ValueError(f"Invalid GLB buffer view {view_index}") from exc
    if extension is None:
        if view.get("buffer", 0) != 0:
            raise ValueError("External or fallback GLB buffers are unsupported")
        start = int(view.get("byteOffset", 0))
        end = start + int(view["byteLength"])
        return binary[start:end]
    if extension.get("buffer", 0) != 0:
        raise ValueError("Meshopt data must be stored in the GLB BIN chunk")
    start = int(extension.get("byteOffset", 0))
    end = start + int(extension["byteLength"])
    encoded = binary[start:end]
    count = int(extension["count"])
    stride = int(extension["byteStride"])
    mode = extension["mode"]
    if mode == "ATTRIBUTES":
        decoded = meshoptimizer.decode_vertex_buffer(count, stride, encoded)
        if extension.get("filter") == "EXPONENTIAL":
            decoded = meshoptimizer.decode_filter_exp(decoded, count, stride)
        return decoded.view(np.uint8).tobytes()
    if mode == "TRIANGLES":
        return meshoptimizer.decode_index_buffer(count, stride, encoded).astype(f"<u{stride}").tobytes()
    raise ValueError(f"Unsupported meshopt mode: {mode}")


_COMPONENT_TYPES = {
    5120: np.dtype("i1"),
    5121: np.dtype("u1"),
    5122: np.dtype("<i2"),
    5123: np.dtype("<u2"),
    5125: np.dtype("<u4"),
    5126: np.dtype("<f4"),
}
_WIDTHS = {"SCALAR": 1, "VEC2": 2, "VEC3": 3, "VEC4": 4}


def _accessor(document: dict, binary: bytes, index: int) -> np.ndarray:
    try:
        accessor = document["accessors"][index]
        view_index = int(accessor["bufferView"])
        view = document["bufferViews"][view_index]
        dtype = _COMPONENT_TYPES[int(accessor["componentType"])]
        width = _WIDTHS[accessor["type"]]
        count = int(accessor["count"])
    except (KeyError, IndexError, TypeError, ValueError) as exc:
        raise ValueError(f"Invalid GLB accessor {index}") from exc
    if count <= 0 or accessor.get("sparse") is not None:
        raise ValueError(f"Unsupported GLB accessor {index}")
    decoded = _decoded_view(document, binary, view_index)
    start = int(accessor.get("byteOffset", 0))
    packed = dtype.itemsize * width
    stride = int(view.get("byteStride", packed))
    final = start + (count - 1) * stride + packed
    if start < 0 or stride < packed or final > len(decoded):
        raise ValueError(f"GLB accessor {index} exceeds its buffer view")
    if stride == packed:
        return np.frombuffer(decoded, dtype=dtype, count=count * width, offset=start).reshape(count, width).copy()
    result = np.empty((count, width), dtype=dtype)
    for row in range(count):
        result[row] = np.frombuffer(decoded, dtype=dtype, count=width, offset=start + row * stride)
    return result


def load_geometry(path: Path) -> Geometry:
    document, binary = _glb_chunks(Path(path))
    try:
        primitives = document["meshes"][0]["primitives"]
    except (KeyError, IndexError, TypeError) as exc:
        raise ValueError("GLB contains no mesh primitives") from exc
    positions = []
    triangles = []
    vertex_offset = 0
    for primitive in primitives:
        if primitive.get("mode", 4) != 4 or "indices" not in primitive:
            raise ValueError("Only indexed triangle primitives are supported")
        attributes = primitive.get("attributes", {})
        if "POSITION" not in attributes:
            raise ValueError("GLB primitive requires POSITION")
        current_positions = _accessor(document, binary, int(attributes["POSITION"])).astype(np.float64)
        current_indices = _accessor(document, binary, int(primitive["indices"])).reshape(-1).astype(np.int64)
        if current_positions.shape[1] != 3 or len(current_indices) % 3:
            raise ValueError("Invalid GLB triangle shape")
        if not np.isfinite(current_positions).all() or current_indices.min(initial=0) < 0:
            raise ValueError("GLB mesh contains invalid coordinates or indices")
        if current_indices.max(initial=0) >= len(current_positions):
            raise ValueError("GLB triangle index exceeds vertex count")
        # Tripo's multiview exports are depth/up/width; Blockbench uses width/up/depth.
        positions.append(current_positions[:, [2, 1, 0]])
        triangles.append(current_indices.reshape(-1, 3) + vertex_offset)
        vertex_offset += len(current_positions)
    if not positions:
        raise ValueError("GLB contains no triangle geometry")
    return Geometry(np.concatenate(positions), np.concatenate(triangles))


def load_views(directory: Path, names: Sequence[str] = VIEW_NAMES) -> Dict[str, View]:
    result = {}
    for name in names:
        path = Path(directory) / f"{name}.png"
        if not path.is_file():
            raise ValueError(f"Missing required multiview image: {path}")
        with Image.open(path) as source:
            pixels = np.asarray(source.convert("RGBA"), dtype=np.uint8)
        # Image generators commonly use a bright chroma-green studio
        # background. It is reference canvas, not creature texture data.
        rgb = pixels[:, :, :3].astype(np.int16)
        chroma_green = (
            (rgb[:, :, 1] >= 140)
            & (rgb[:, :, 1] >= rgb[:, :, 0] + 55)
            & (rgb[:, :, 1] >= rgb[:, :, 2] + 55)
        )
        pixels = pixels.copy()
        pixels[chroma_green, 3] = 0
        alpha = pixels[:, :, 3]
        ys, xs = np.nonzero(alpha >= 16)
        if not len(xs):
            raise ValueError(f"Empty required multiview image: {path}")
        result[name] = View(pixels, (int(xs.min()), int(ys.min()), int(xs.max()), int(ys.max())))
    return result


def _normalise(points: np.ndarray, resolution: int) -> Tuple[np.ndarray, np.ndarray]:
    if resolution < 8 or resolution > 64:
        raise ValueError("Resolution must be between 8 and 64")
    lower = points.min(axis=0)
    upper = points.max(axis=0)
    span = upper - lower
    longest = float(span.max())
    if not math.isfinite(longest) or longest <= 0:
        raise ValueError("Mesh bounds have zero size")
    unit = (points - lower) / longest
    scaled = unit * (resolution - 1)
    scaled[:, 0] -= (scaled[:, 0].min() + scaled[:, 0].max()) / 2
    scaled[:, 2] -= (scaled[:, 2].min() + scaled[:, 2].max()) / 2
    scaled[:, 1] -= scaled[:, 1].min()
    normalized = (points - lower) / np.where(span > 0, span, 1.0)
    return scaled, np.clip(normalized, 0.0, 1.0)


def _sample(view: View, u: float, v: float) -> Tuple[int, int, int, int] | None:
    left, top, right, bottom = view.bbox
    x = int(round(left + min(1.0, max(0.0, u)) * (right - left)))
    y = int(round(top + min(1.0, max(0.0, v)) * (bottom - top)))
    rgba = tuple(map(int, view.pixels[y, x]))
    return rgba if rgba[3] >= 16 else None


def _colour_for(point: np.ndarray, views: Mapping[str, View]) -> Tuple[int, int, int, int]:
    x, y, z = map(float, point)
    projections = [
        (z, "front", x, 1 - y),
        (1 - z, "back", 1 - x, 1 - y),
        (x, "left", 1 - z, 1 - y),
        (1 - x, "right", z, 1 - y),
    ]
    if "top" in views:
        projections.append((1 - y, "top", x, z))
    for _distance, name, u, v in sorted(projections):
        colour = _sample(views[name], u, v)
        if colour is not None:
            return tuple((channel // 8) * 8 + 4 for channel in colour[:3]) + (255,)
    return (128, 128, 128, 255)


def _voxel_colours(geometry: Geometry, views: Mapping[str, View], resolution: int) -> Dict[Tuple[int, int, int], Tuple[int, int, int, int]]:
    scaled, normalized = _normalise(geometry.positions, resolution)
    cells = np.floor(scaled + np.array([0.5, 0.0, 0.5])).astype(np.int32)
    order = np.lexsort((cells[:, 2], cells[:, 1], cells[:, 0]))
    cells = cells[order]
    normalized = normalized[order]
    unique, starts, counts = np.unique(cells, axis=0, return_index=True, return_counts=True)
    result = {}
    for cell, start, count in zip(unique, starts, counts):
        colours = Counter(_colour_for(point, views) for point in normalized[start : start + count])
        result[tuple(map(int, cell))] = min(colours, key=lambda colour: (-colours[colour], colour))
    return result


def _palette_indices(voxels: Mapping[Tuple[int, int, int], Tuple[int, int, int, int]]):
    histogram = Counter(voxels.values())
    palette = tuple(sorted(histogram, key=lambda colour: (-histogram[colour], colour))[:256])
    palette_rgb = np.asarray([colour[:3] for colour in palette], dtype=np.int16)
    lookup = {}
    for colour in histogram:
        if colour in palette:
            lookup[colour] = palette.index(colour)
        else:
            delta = palette_rgb - np.asarray(colour[:3], dtype=np.int16)
            lookup[colour] = int(np.argmin(np.sum(delta.astype(np.int32) ** 2, axis=1)))
    return palette, {cell: lookup[colour] for cell, colour in voxels.items()}


def _greedy_merge(labels: Mapping[Tuple[int, int, int], int]) -> Tuple[Cuboid, ...]:
    remaining = dict(labels)
    result = []
    while remaining:
        x0, y0, z0 = min(remaining, key=lambda cell: (cell[2], cell[1], cell[0]))
        colour = remaining[(x0, y0, z0)]
        x1 = x0 + 1
        while remaining.get((x1, y0, z0)) == colour:
            x1 += 1
        y1 = y0 + 1
        while all(remaining.get((x, y1, z0)) == colour for x in range(x0, x1)):
            y1 += 1
        z1 = z0 + 1
        while all(remaining.get((x, y, z1)) == colour for y in range(y0, y1) for x in range(x0, x1)):
            z1 += 1
        for z in range(z0, z1):
            for y in range(y0, y1):
                for x in range(x0, x1):
                    del remaining[(x, y, z)]
        result.append(Cuboid((x0, y0, z0), (x1, y1, z1), colour))
    return tuple(result)


def build_candidate(glb_path: Path, views_path: Path, resolution: int = 24) -> Candidate:
    view_names = VIEW_NAMES if (Path(views_path) / "top.png").is_file() else VIEW_NAMES[:-1]
    voxels = _voxel_colours(load_geometry(glb_path), load_views(views_path, view_names), resolution)
    if not voxels:
        raise ValueError("Tripo mesh produced no occupied voxels")
    palette, labels = _palette_indices(voxels)
    cuboids = _greedy_merge(labels)
    if len(cuboids) >= 5000:
        raise ValueError(f"Tripo candidate has excessive cuboid count: {len(cuboids)}")
    return Candidate(cuboids, palette, len(voxels))


def _stable_uuid(kind: str, name: str) -> str:
    return str(uuid5(UUID_NAMESPACE, f"{kind}:{name}"))


def _palette_png(candidate: Candidate) -> bytes:
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    pixels = image.load()
    for index, colour in enumerate(candidate.palette):
        pixels[index % 16, index // 16] = colour
    output = BytesIO()
    image.save(output, format="PNG", optimize=False, compress_level=9)
    return output.getvalue()


def render_preview(candidate: Candidate, size: int = 768) -> bytes:
    """Render a deterministic isometric checkpoint without depending on Blockbench UI."""

    if size < 128:
        raise ValueError("Preview size must be at least 128 pixels")

    def project(point):
        x, y, z = point
        return np.array((x - z, (x + z) * 0.5 - y), dtype=np.float64)

    projected_bounds = []
    for cuboid in candidate.cuboids:
        for x in (cuboid.lower[0], cuboid.upper[0]):
            for y in (cuboid.lower[1], cuboid.upper[1]):
                for z in (cuboid.lower[2], cuboid.upper[2]):
                    projected_bounds.append(project((x, y, z)))
    bounds = np.asarray(projected_bounds)
    lower, upper = bounds.min(axis=0), bounds.max(axis=0)
    span = upper - lower
    scale = (size * 0.82) / max(float(span.max()), 1.0)
    centre = (lower + upper) / 2

    def screen(point):
        value = (project(point) - centre) * scale
        return (float(size / 2 + value[0]), float(size / 2 + value[1]))

    image = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image, "RGBA")
    ordered = sorted(
        candidate.cuboids,
        key=lambda cube: sum(cube.lower) + sum(cube.upper),
    )
    for cuboid in ordered:
        x0, y0, z0 = cuboid.lower
        x1, y1, z1 = cuboid.upper
        base = candidate.palette[cuboid.colour]
        faces = (
            (((x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)), 1.10),
            (((x1, y0, z0), (x1, y1, z0), (x1, y1, z1), (x1, y0, z1)), 0.86),
            (((x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)), 0.68),
        )
        for vertices, shade in faces:
            colour = tuple(min(255, int(channel * shade)) for channel in base[:3]) + (255,)
            draw.polygon([screen(vertex) for vertex in vertices], fill=colour)
    output = BytesIO()
    image.save(output, format="PNG", optimize=False, compress_level=9)
    return output.getvalue()


def candidate_bytes(candidate: Candidate, family: str, piece: str) -> Tuple[bytes, bytes]:
    identifier = f"{family}_{piece}_tripo_cubes"
    texture_bytes = _palette_png(candidate)
    elements = []
    children = []
    for index, cuboid in enumerate(candidate.cuboids):
        name = f"voxel_box_{index:04d}"
        uuid = _stable_uuid(identifier, name)
        children.append(uuid)
        u, v = cuboid.colour % 16, cuboid.colour // 16
        elements.append({
            "name": name,
            "box_uv": False,
            "from": list(cuboid.lower),
            "to": list(cuboid.upper),
            "origin": [(cuboid.lower[i] + cuboid.upper[i]) / 2 for i in range(3)],
            "faces": {face: {"uv": [u, v, u + 1, v + 1], "texture": 0} for face in FACE_NAMES},
            "type": "cube",
            "uuid": uuid,
        })
    texture_source = "data:image/png;base64," + base64.b64encode(texture_bytes).decode("ascii")
    group_uuid = _stable_uuid(identifier, "root")
    document = {
        "meta": {"format_version": "5.0", "model_format": "free", "box_uv": False},
        "name": identifier,
        "resolution": {"width": 16, "height": 16},
        "elements": elements,
        "outliner": [{"name": identifier, "origin": [0, 0, 0], "uuid": group_uuid, "children": children}],
        "textures": [{
            "name": f"{identifier}_palette.png",
            "id": "0",
            "particle": False,
            "render_mode": "default",
            "visible": True,
            "mode": "bitmap",
            "saved": True,
            "uuid": _stable_uuid(identifier, "texture"),
            "source": texture_source,
        }],
        "animations": [],
    }
    return (json.dumps(document, ensure_ascii=False, indent=2) + "\n").encode("utf-8"), texture_bytes


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--glb", type=Path, required=True)
    parser.add_argument("--views", type=Path, required=True)
    parser.add_argument("--family", required=True)
    parser.add_argument("--piece", required=True)
    parser.add_argument("--model", type=Path, required=True)
    parser.add_argument("--texture", type=Path, required=True)
    parser.add_argument("--resolution", type=int, default=24)
    args = parser.parse_args(argv)
    candidate = build_candidate(args.glb, args.views, args.resolution)
    model, texture = candidate_bytes(candidate, args.family, args.piece)
    args.model.parent.mkdir(parents=True, exist_ok=True)
    args.texture.parent.mkdir(parents=True, exist_ok=True)
    args.model.write_bytes(model)
    args.texture.write_bytes(texture)
    print(f"ARMOR_TRIPO_PASS VOXELS={candidate.occupied_voxel_count} CUBOIDS={candidate.cuboid_count} PALETTE={len(candidate.palette)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
