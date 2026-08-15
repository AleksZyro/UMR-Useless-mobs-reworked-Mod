"""Convert the approved Tripo GLB into a deterministic cuboid Blockbench model."""

from __future__ import annotations

from dataclasses import dataclass
import argparse
import base64
from io import BytesIO
import json
import math
import os
from pathlib import Path
import struct
import tempfile
from typing import Any, Dict, List, Tuple
from uuid import UUID, uuid5

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

    @property
    def texture_size(self) -> Tuple[int, int]:
        return (16, 16)

    @property
    def all_uvs_in_bounds(self) -> bool:
        return all(0 <= cuboid.colour < 256 for cuboid in self.cuboids)


PROJECT_ROOT = Path(__file__).resolve().parents[2]
EXPORT_ROOT = PROJECT_ROOT / "Modelle" / "Exports" / "corrupted_silverfish_v5"
DEFAULT_GLB = EXPORT_ROOT / "tripo_export" / "corrupted_silverfish_tripo_multiview_v5.glb"
DEFAULT_MODEL = EXPORT_ROOT / "blockbench" / "Corrupted Silverfish v5 Tripo Cubes.bbmodel"
DEFAULT_TEXTURE = EXPORT_ROOT / "blockbench" / "corrupted_silverfish_v5_palette.png"
UUID_NAMESPACE = UUID("730de6a5-2a4c-5ef5-a466-3c96560bdcab")
FACE_NAMES = ("north", "east", "south", "west", "up", "down")


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


def _sample_surface(mesh: MeshData) -> Tuple[np.ndarray, np.ndarray]:
    """Return dense, deterministic vertex/edge/centre samples and their UVs."""

    tri_pos = normalise_positions(mesh.positions)[mesh.triangles]
    tri_uv = mesh.uvs[mesh.triangles]
    points = [normalise_positions(mesh.positions), tri_pos.mean(axis=1)]
    texcoords = [mesh.uvs, tri_uv.mean(axis=1)]
    for left, right in ((0, 1), (1, 2), (2, 0)):
        points.append((tri_pos[:, left] + tri_pos[:, right]) * 0.5)
        texcoords.append((tri_uv[:, left] + tri_uv[:, right]) * 0.5)
    return np.concatenate(points), np.concatenate(texcoords)


def _voxel_colours(mesh: MeshData) -> Dict[Tuple[int, int, int], Tuple[int, int, int, int]]:
    points, uvs = _sample_surface(mesh)
    cells = np.floor(points + np.array([0.5, 0.0, 0.5])).astype(np.int32)
    texture = np.asarray(mesh.base_colour, dtype=np.uint8)
    width, height = mesh.base_colour.size
    px = np.clip(np.rint(uvs[:, 0] * (width - 1)), 0, width - 1).astype(np.int32)
    py = np.clip(np.rint((1.0 - uvs[:, 1]) * (height - 1)), 0, height - 1).astype(np.int32)
    colours = texture[py, px]

    # Use each voxel's dominant quantised texel instead of averaging contrasting
    # armour/crystal pixels into muddy colours.
    order = np.lexsort((cells[:, 2], cells[:, 1], cells[:, 0]))
    cells = cells[order]
    colours = colours[order]
    unique, starts, counts = np.unique(cells, axis=0, return_index=True, return_counts=True)
    result: Dict[Tuple[int, int, int], Tuple[int, int, int, int]] = {}
    for cell, start, count in zip(unique, starts, counts):
        histogram: Dict[Tuple[int, int, int, int], int] = {}
        for rgba in colours[start : start + count]:
            quantised = tuple(min(255, (int(channel) // 8) * 8 + 4) for channel in rgba[:3]) + (255,)
            histogram[quantised] = histogram.get(quantised, 0) + 1
        dominant = min(histogram, key=lambda colour: (-histogram[colour], colour))
        result[tuple(map(int, cell))] = dominant
    return result


def _palette_indices(
    voxels: Dict[Tuple[int, int, int], Tuple[int, int, int, int]],
) -> Tuple[Tuple[Tuple[int, int, int, int], ...], Dict[Tuple[int, int, int], int]]:
    # Five bits per RGB channel retain Tripo shading while allowing a compact fixed atlas.
    histogram: Dict[Tuple[int, int, int, int], int] = {}
    quantised: Dict[Tuple[int, int, int], Tuple[int, int, int, int]] = {}
    for cell, rgba in voxels.items():
        colour = tuple(min(255, (channel // 8) * 8 + 4) for channel in rgba[:3]) + (255,)
        quantised[cell] = colour
        histogram[colour] = histogram.get(colour, 0) + 1
    ranked = sorted(histogram, key=lambda colour: (-histogram[colour], colour))
    accents = sorted(
        histogram,
        key=lambda colour: (-(max(colour[:3]) - min(colour[:3])), -max(colour[:3]), colour),
    )[:32]
    selected = list(dict.fromkeys(accents + ranked))[:256]
    palette = tuple(selected)
    palette_rgb = np.asarray([colour[:3] for colour in palette], dtype=np.int16)
    lookup: Dict[Tuple[int, int, int, int], int] = {}
    for colour in sorted(histogram):
        if colour in palette:
            lookup[colour] = palette.index(colour)
        else:
            delta = palette_rgb - np.asarray(colour[:3], dtype=np.int16)
            lookup[colour] = int(np.argmin(np.sum(delta.astype(np.int32) ** 2, axis=1)))
    return palette, {cell: lookup[colour] for cell, colour in quantised.items()}


def _greedy_merge(labels: Dict[Tuple[int, int, int], int]) -> Tuple[Cuboid, ...]:
    remaining = dict(labels)
    result: List[Cuboid] = []
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
        while all(
            remaining.get((x, y, z1)) == colour
            for y in range(y0, y1)
            for x in range(x0, x1)
        ):
            z1 += 1
        for z in range(z0, z1):
            for y in range(y0, y1):
                for x in range(x0, x1):
                    del remaining[(x, y, z)]
        result.append(Cuboid((x0, y0, z0), (x1, y1, z1), colour))
    return tuple(result)


def build_candidate(path: Path = DEFAULT_GLB) -> Candidate:
    voxels = _voxel_colours(load_glb(Path(path)))
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
    image = Image.new("RGBA", candidate.texture_size, (0, 0, 0, 0))
    pixels = image.load()
    for index, colour in enumerate(candidate.palette):
        pixels[index % 16, index // 16] = colour
    output = BytesIO()
    image.save(output, format="PNG", optimize=False, compress_level=9)
    return output.getvalue()


def _region_for(cuboid: Cuboid, minimum_z: int, span_z: int) -> str:
    centre = (cuboid.lower[2] + cuboid.upper[2]) / 2
    band = min(4, max(0, int((centre - minimum_z) * 5 / max(1, span_z))))
    return f"section_{band}"


def candidate_bytes(candidate: Candidate) -> Tuple[bytes, bytes]:
    texture_bytes = _palette_png(candidate)
    texture_source = "data:image/png;base64," + base64.b64encode(texture_bytes).decode("ascii")
    minimum_z = min(cuboid.lower[2] for cuboid in candidate.cuboids)
    maximum_z = max(cuboid.upper[2] for cuboid in candidate.cuboids)
    region_names = [f"section_{index}" for index in range(5)]
    grouped: Dict[str, List[str]] = {name: [] for name in region_names}
    elements = []
    for index, cuboid in enumerate(candidate.cuboids):
        name = f"voxel_box_{index:04d}"
        element_uuid = _stable_uuid("element", name)
        region = _region_for(cuboid, minimum_z, maximum_z - minimum_z)
        grouped[region].append(element_uuid)
        u = cuboid.colour % 16
        v = cuboid.colour // 16
        faces = {face: {"uv": [u, v, u + 1, v + 1], "texture": 0} for face in FACE_NAMES}
        lower = list(cuboid.lower)
        upper = list(cuboid.upper)
        elements.append(
            {
                "name": name,
                "box_uv": False,
                "from": lower,
                "to": upper,
                "origin": [(lower[i] + upper[i]) / 2 for i in range(3)],
                "rotation": [0, 0, 0],
                "faces": faces,
                "type": "cube",
                "uuid": element_uuid,
                "bone": region,
            }
        )
    groups = []
    outliner = []
    for name in region_names:
        group_uuid = _stable_uuid("group", name)
        groups.append(
            {
                "name": name,
                "uuid": group_uuid,
                "export": True,
                "locked": False,
                "origin": [0, 0, 0],
                "rotation": [0, 0, 0],
                "color": 0,
                "children": [],
                "reset": False,
                "shade": True,
                "mirror_uv": False,
                "visibility": True,
                "autouv": 0,
                "isOpen": True,
            }
        )
        outliner.append({"uuid": group_uuid, "isOpen": True, "children": grouped[name]})
    document = {
        "meta": {"format_version": "5.0", "model_format": "geckolib_model", "box_uv": False},
        "name": "Corrupted Silverfish v5 Tripo Cubes",
        "model_identifier": "geometry.corrupted_silverfish_v5",
        "visible_box": [3.5, 2.0, 0],
        "variable_placeholders": "",
        "timeline_setups": [],
        "unhandled_root_fields": {},
        "geckolib_modid": "usless_mobs",
        "geckolib_filepath_cache": "",
        "resolution": {"width": 16, "height": 16},
        "elements": elements,
        "groups": groups,
        "outliner": outliner,
        "textures": [
            {
                "path": "",
                "name": "corrupted_silverfish_v5_palette.png",
                "folder": "entity",
                "namespace": "usless_mobs",
                "id": "0",
                "particle": False,
                "render_mode": "default",
                "visible": True,
                "mode": "bitmap",
                "saved": True,
                "uuid": _stable_uuid("texture", "palette"),
                "source": texture_source,
            }
        ],
        "animations": [],
        "geckolib_model_type": "Entity",
    }
    model_bytes = (json.dumps(document, ensure_ascii=False, indent=2) + "\n").encode("utf-8")
    return model_bytes, texture_bytes


def _atomic_write(path: Path, payload: bytes) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    descriptor, temporary_name = tempfile.mkstemp(prefix=f".{path.name}.", suffix=".tmp", dir=str(path.parent))
    temporary = Path(temporary_name)
    try:
        with os.fdopen(descriptor, "wb") as handle:
            handle.write(payload)
            handle.flush()
            os.fsync(handle.fileno())
        os.replace(temporary, path)
    finally:
        temporary.unlink(missing_ok=True)


def write_candidate(candidate: Candidate, model_path: Path, texture_path: Path) -> None:
    expected_root = EXPORT_ROOT.resolve()
    for path in (model_path, texture_path):
        if expected_root not in Path(path).resolve().parents:
            raise ValueError(f"Refusing output outside v5 export root: {path}")
    model_bytes, texture_bytes = candidate_bytes(candidate)
    _atomic_write(Path(texture_path), texture_bytes)
    _atomic_write(Path(model_path), model_bytes)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, default=DEFAULT_GLB)
    parser.add_argument("--model", type=Path, default=DEFAULT_MODEL)
    parser.add_argument("--texture", type=Path, default=DEFAULT_TEXTURE)
    args = parser.parse_args()
    candidate = build_candidate(args.input)
    write_candidate(candidate, args.model, args.texture)
    print(
        f"TRIPO_VOXEL_PASS VOXELS={candidate.occupied_voxel_count} "
        f"CUBOIDS={candidate.cuboid_count} PALETTE={len(candidate.palette)}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
