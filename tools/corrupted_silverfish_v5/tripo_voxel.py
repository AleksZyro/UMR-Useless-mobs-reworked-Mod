"""Convert the approved Tripo GLB into a deterministic cuboid Blockbench model."""

from __future__ import annotations

from dataclasses import dataclass
from io import BytesIO
import json
import math
from pathlib import Path
import struct
from typing import Any, Dict, List, Tuple

import numpy as np
from PIL import Image


GLB_MAGIC = 0x46546C67
JSON_CHUNK = 0x4E4F534A
BIN_CHUNK = 0x004E4942


@dataclass(frozen=True)
class MeshData:
    positions: np.ndarray
    uvs: np.ndarray
    triangles: np.ndarray
    base_colour: Image.Image


def _parse_glb(path: Path) -> Tuple[Dict[str, Any], bytes]:
    data = path.read_bytes()
    if len(data) < 20:
        raise ValueError(f"GLB file is too short: {path}")
    magic, version, declared_length = struct.unpack_from("<III", data, 0)
    if magic != GLB_MAGIC or version != 2 or declared_length != len(data):
        raise ValueError(f"Invalid GLB 2.0 header: {path}")

    offset = 12
    chunks: Dict[int, bytes] = {}
    while offset < len(data):
        if offset + 8 > len(data):
            raise ValueError("Truncated GLB chunk header")
        length, chunk_type = struct.unpack_from("<II", data, offset)
        offset += 8
        end = offset + length
        if end > len(data):
            raise ValueError("Truncated GLB chunk")
        if chunk_type in chunks:
            raise ValueError(f"Duplicate GLB chunk type {chunk_type:#x}")
        chunks[chunk_type] = data[offset:end]
        offset = end

    if JSON_CHUNK not in chunks or BIN_CHUNK not in chunks:
        raise ValueError("GLB requires one JSON and one BIN chunk")
    try:
        document = json.loads(chunks[JSON_CHUNK].decode("utf-8").rstrip(" \t\r\n\0"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise ValueError("Invalid GLB JSON chunk") from exc
    return document, chunks[BIN_CHUNK]


_COMPONENTS = {
    5121: np.dtype("u1"),
    5123: np.dtype("<u2"),
    5125: np.dtype("<u4"),
    5126: np.dtype("<f4"),
}
_WIDTHS = {"SCALAR": 1, "VEC2": 2, "VEC3": 3, "VEC4": 4}


def _accessor(document: Dict[str, Any], binary: bytes, index: int) -> np.ndarray:
    try:
        accessor = document["accessors"][index]
        view = document["bufferViews"][accessor["bufferView"]]
        dtype = _COMPONENTS[accessor["componentType"]]
        width = _WIDTHS[accessor["type"]]
        count = int(accessor["count"])
    except (KeyError, IndexError, TypeError, ValueError) as exc:
        raise ValueError(f"Invalid GLB accessor {index}") from exc
    if count <= 0 or view.get("buffer", 0) != 0 or accessor.get("sparse") is not None:
        raise ValueError(f"Unsupported GLB accessor {index}")

    start = int(view.get("byteOffset", 0)) + int(accessor.get("byteOffset", 0))
    stride = int(view.get("byteStride", dtype.itemsize * width))
    packed = dtype.itemsize * width
    if stride < packed or start < 0:
        raise ValueError(f"Invalid GLB accessor stride {index}")
    final = start + (count - 1) * stride + packed
    view_end = int(view.get("byteOffset", 0)) + int(view["byteLength"])
    if final > min(len(binary), view_end):
        raise ValueError(f"GLB accessor {index} exceeds its buffer view")

    if stride == packed:
        array = np.frombuffer(binary, dtype=dtype, count=count * width, offset=start)
        return array.reshape(count, width).copy()
    result = np.empty((count, width), dtype=dtype)
    for row in range(count):
        result[row] = np.frombuffer(binary, dtype=dtype, count=width, offset=start + row * stride)
    return result


def _embedded_image(document: Dict[str, Any], binary: bytes, image_index: int) -> Image.Image:
    try:
        image = document["images"][image_index]
        view = document["bufferViews"][image["bufferView"]]
        start = int(view.get("byteOffset", 0))
        end = start + int(view["byteLength"])
    except (KeyError, IndexError, TypeError, ValueError) as exc:
        raise ValueError("Base-colour image is not embedded in the GLB") from exc
    if view.get("buffer", 0) != 0 or start < 0 or end > len(binary):
        raise ValueError("Embedded base-colour image exceeds the GLB buffer")
    try:
        with Image.open(BytesIO(binary[start:end])) as source:
            return source.convert("RGBA")
    except Exception as exc:
        raise ValueError("Invalid embedded base-colour image") from exc


def load_glb(path: Path) -> MeshData:
    """Load triangle positions, UVs and the material's embedded base-colour image."""

    document, binary = _parse_glb(Path(path))
    try:
        mesh = document["meshes"][0]
        primitives = mesh["primitives"]
    except (KeyError, IndexError, TypeError) as exc:
        raise ValueError("GLB contains no mesh primitives") from exc

    all_positions: List[np.ndarray] = []
    all_uvs: List[np.ndarray] = []
    all_triangles: List[np.ndarray] = []
    vertex_offset = 0
    material_index = None
    for primitive in primitives:
        if primitive.get("mode", 4) != 4:
            raise ValueError("Only triangle GLB primitives are supported")
        attributes = primitive.get("attributes", {})
        if "POSITION" not in attributes or "TEXCOORD_0" not in attributes or "indices" not in primitive:
            raise ValueError("GLB primitive requires POSITION, TEXCOORD_0 and indices")
        positions = _accessor(document, binary, int(attributes["POSITION"]))
        uvs = _accessor(document, binary, int(attributes["TEXCOORD_0"]))
        indices = _accessor(document, binary, int(primitive["indices"])).reshape(-1)
        if positions.shape[1] != 3 or uvs.shape != (len(positions), 2):
            raise ValueError("GLB position/UV accessor shape mismatch")
        if len(indices) % 3 or indices.max(initial=0) >= len(positions):
            raise ValueError("GLB triangle indices are invalid")
        if not np.isfinite(positions).all() or not np.isfinite(uvs).all():
            raise ValueError("GLB mesh contains non-finite coordinates")
        current_material = int(primitive.get("material", 0))
        if material_index is None:
            material_index = current_material
        elif material_index != current_material:
            raise ValueError("Multiple GLB materials are not supported")
        all_positions.append(positions.astype(np.float64))
        all_uvs.append(uvs.astype(np.float64))
        all_triangles.append(indices.reshape(-1, 3).astype(np.int64) + vertex_offset)
        vertex_offset += len(positions)

    if not all_positions:
        raise ValueError("GLB contains no triangles")
    try:
        material = document["materials"][material_index or 0]
        texture_index = material["pbrMetallicRoughness"]["baseColorTexture"]["index"]
        image_index = document["textures"][texture_index]["source"]
    except (KeyError, IndexError, TypeError) as exc:
        raise ValueError("GLB material has no embedded base-colour texture") from exc

    return MeshData(
        positions=np.concatenate(all_positions),
        uvs=np.concatenate(all_uvs),
        triangles=np.concatenate(all_triangles),
        base_colour=_embedded_image(document, binary, int(image_index)),
    )


def normalise_positions(positions: np.ndarray, target_length: float = 32.0) -> np.ndarray:
    """Scale the longest model axis and centre X/Z while placing Y on the floor."""

    points = np.asarray(positions, dtype=np.float64)
    if points.ndim != 2 or points.shape[1] != 3 or not np.isfinite(points).all():
        raise ValueError("Positions must be a finite Nx3 array")
    if not math.isfinite(target_length) or target_length <= 0:
        raise ValueError("Target length must be positive and finite")
    lower = points.min(axis=0)
    upper = points.max(axis=0)
    longest = float((upper - lower).max())
    if longest <= 0:
        raise ValueError("Mesh bounds have zero size")
    result = points * (target_length / longest)
    lower = result.min(axis=0)
    upper = result.max(axis=0)
    result[:, 0] -= (lower[0] + upper[0]) / 2
    result[:, 2] -= (lower[2] + upper[2]) / 2
    result[:, 1] -= lower[1]
    return result
