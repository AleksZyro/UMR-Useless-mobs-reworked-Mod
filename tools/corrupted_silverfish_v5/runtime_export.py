"""Export the approved Tripo Blockbench rig for the custom GeckoLib mesh renderer."""

from __future__ import annotations

import argparse
import base64
import json
import math
from pathlib import Path
import struct
from typing import Any, Dict, List, Tuple

from tools.corrupted_silverfish_v5.rig_mesh import PARENTS, _publish_transaction, load_document


JsonObject = Dict[str, Any]
PROJECT_ROOT = Path(__file__).resolve().parents[2]
EXPORT_ROOT = PROJECT_ROOT / "Modelle" / "Exports" / "corrupted_silverfish_v5"
DEFAULT_SOURCE = EXPORT_ROOT / "blockbench" / "Corrupted Silverfish v5 Tripo Animated.bbmodel"
DEFAULT_RUNTIME_ROOT = (
    PROJECT_ROOT
    / "src"
    / "main"
    / "mobs"
    / "endermite"
    / "resources"
    / "assets"
    / "usless_mobs"
)
MAGIC = b"CSMESH1\0"
RUNTIME_PATHS = (
    "geo/corrupted_silverfish.geo.json",
    "animations/corrupted_silverfish.animation.json",
    "textures/entity/corrupted_silverfish.png",
    "meshes/entity/corrupted_silverfish.mesh",
)


def _json_bytes(document: JsonObject) -> bytes:
    return (json.dumps(document, ensure_ascii=False, indent=2) + "\n").encode("utf-8")


def _finite_number(value: Any, context: str, *, allow_string: bool = False) -> float:
    valid_string = allow_string and isinstance(value, str) and value.strip() != ""
    if isinstance(value, bool) or (not isinstance(value, (int, float)) and not valid_string):
        raise ValueError(f"{context} must be numeric")
    try:
        number = float(value)
    except ValueError as exc:
        raise ValueError(f"{context} must be numeric") from exc
    if not math.isfinite(number):
        raise ValueError(f"{context} must be finite")
    return number


def _texture_bytes(document: JsonObject) -> bytes:
    textures = document.get("textures", [])
    if len(textures) != 1:
        raise ValueError("Runtime export requires exactly one embedded texture")
    source = textures[0].get("source")
    marker = ";base64,"
    if not isinstance(source, str) or marker not in source:
        raise ValueError("Runtime texture must be an embedded base64 data URI")
    try:
        payload = base64.b64decode(source.split(marker, 1)[1], validate=True)
    except (ValueError, base64.binascii.Error) as exc:
        raise ValueError("Runtime texture contains invalid base64 data") from exc
    if not payload.startswith(b"\x89PNG\r\n\x1a\n"):
        raise ValueError("Runtime texture must be PNG data")
    return payload


def _mesh_bytes(document: JsonObject) -> bytes:
    elements = document.get("elements", [])
    if not elements:
        raise ValueError("Runtime mesh contains no elements")
    payload = bytearray(MAGIC)
    payload.extend(struct.pack("<I", len(elements)))
    seen_names = set()
    for element in elements:
        element_name = element.get("name")
        if not isinstance(element_name, str) or not element_name.endswith("_mesh"):
            raise ValueError(f"Runtime mesh element must use the '<bone>_mesh' name: {element_name!r}")
        name = element_name.removesuffix("_mesh")
        if not name or name in seen_names:
            raise ValueError(f"Runtime mesh element maps to an invalid bone name: {element_name!r}")
        seen_names.add(name)
        if element.get("type") != "mesh":
            raise ValueError(f"Runtime element is not a mesh: {name}")
        encoded_name = name.encode("utf-8")
        if len(encoded_name) > 65535:
            raise ValueError(f"Runtime mesh element name is too long: {name}")
        faces = element.get("faces", {})
        vertices = element.get("vertices", {})
        payload.extend(struct.pack("<H", len(encoded_name)))
        payload.extend(encoded_name)
        payload.extend(struct.pack("<I", len(faces)))
        for face_name, face in faces.items():
            vertex_ids = face.get("vertices", [])
            if len(vertex_ids) != 3:
                raise ValueError(f"Runtime face must be triangular: {name}/{face_name}")
            if face.get("texture") not in (0, "0"):
                raise ValueError(f"Runtime face must use texture 0: {name}/{face_name}")
            face_uvs = face.get("uv", {})
            for vertex_id in vertex_ids:
                if vertex_id not in vertices or vertex_id not in face_uvs:
                    raise ValueError(f"Runtime face references missing vertex or UV: {name}/{face_name}")
                position = vertices[vertex_id]
                uv = face_uvs[vertex_id]
                if len(position) != 3 or len(uv) != 2:
                    raise ValueError(f"Runtime vertex must contain Vec3 and Vec2: {name}/{face_name}")
                values = [
                    _finite_number(value, f"{name}/{face_name} vertex")
                    for value in (*position, *uv)
                ]
                payload.extend(struct.pack("<5f", *values))
    return bytes(payload)


def decode_runtime_mesh(payload: bytes) -> Dict[str, List[Tuple[Tuple[Tuple[float, ...], Tuple[float, ...]], ...]]]:
    """Decode generated mesh bytes for independent validation and tests."""
    view = memoryview(payload)
    if len(view) < 12 or bytes(view[:8]) != MAGIC:
        raise ValueError("Runtime mesh has an invalid header")
    offset = 8

    def take(fmt: str) -> Tuple[Any, ...]:
        nonlocal offset
        size = struct.calcsize(fmt)
        if offset + size > len(view):
            raise ValueError("Runtime mesh is truncated")
        result = struct.unpack_from(fmt, view, offset)
        offset += size
        return result

    bone_count = take("<I")[0]
    decoded: Dict[str, List[Tuple[Tuple[Tuple[float, ...], Tuple[float, ...]], ...]]] = {}
    for _ in range(bone_count):
        name_length = take("<H")[0]
        if offset + name_length > len(view):
            raise ValueError("Runtime mesh name is truncated")
        name = bytes(view[offset : offset + name_length]).decode("utf-8")
        offset += name_length
        if not name or name in decoded:
            raise ValueError(f"Runtime mesh has invalid duplicate bone name: {name!r}")
        face_count = take("<I")[0]
        faces = []
        for _ in range(face_count):
            corners = []
            for _ in range(3):
                values = take("<5f")
                corners.append((tuple(values[:3]), tuple(values[3:])))
            faces.append(tuple(corners))
        decoded[name] = faces
    if offset != len(view):
        raise ValueError("Runtime mesh contains trailing bytes")
    return decoded


def _geo_document(document: JsonObject) -> JsonObject:
    groups = document.get("groups", [])
    group_names = {group.get("name") for group in groups}
    expected_names = {"root", *PARENTS}
    if group_names != expected_names:
        raise ValueError(f"Runtime rig groups do not match expected hierarchy: {sorted(group_names)}")
    all_points = [
        [_finite_number(value, "visible bound") for value in point]
        for element in document.get("elements", [])
        for point in element.get("vertices", {}).values()
    ]
    if not all_points:
        raise ValueError("Runtime model has no vertices")
    x_values = [point[0] for point in all_points]
    y_values = [point[1] for point in all_points]
    z_values = [point[2] for point in all_points]
    width = max(max(x_values) - min(x_values), max(z_values) - min(z_values)) / 16
    height = (max(y_values) - min(y_values)) / 16
    offset_y = (max(y_values) + min(y_values)) / 32
    texture = document["textures"][0]
    bones = []
    for group in groups:
        name = group["name"]
        pivot = [_finite_number(value, f"{name} pivot") for value in group.get("origin", [])]
        if len(pivot) != 3:
            raise ValueError(f"Runtime bone pivot must be Vec3: {name}")
        bone: JsonObject = {"name": name, "pivot": pivot}
        if name != "root":
            bone["parent"] = PARENTS[name]
        bones.append(bone)
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [
            {
                "description": {
                    "identifier": "geometry.corrupted_silverfish",
                    "texture_width": int(texture["width"]),
                    "texture_height": int(texture["height"]),
                    "visible_bounds_width": round(width, 5),
                    "visible_bounds_height": round(height, 5),
                    "visible_bounds_offset": [0, round(offset_y, 5), 0],
                },
                "bones": bones,
            }
        ],
    }


def _animation_document(document: JsonObject) -> JsonObject:
    groups = {group.get("uuid"): group.get("name") for group in document.get("groups", [])}
    animations: JsonObject = {}
    for source in document.get("animations", []):
        name = source.get("name")
        if not isinstance(name, str) or name in animations:
            raise ValueError(f"Runtime animation has invalid name: {name!r}")
        bones: JsonObject = {}
        for group_uuid, animator in source.get("animators", {}).items():
            bone_name = groups.get(group_uuid)
            if not isinstance(bone_name, str):
                raise ValueError(f"Runtime animation targets missing group: {name}/{group_uuid}")
            channels: JsonObject = {}
            for keyframe in animator.get("keyframes", []):
                channel = keyframe.get("channel")
                if channel not in ("position", "rotation", "scale"):
                    raise ValueError(f"Unsupported runtime animation channel: {name}/{bone_name}/{channel}")
                points = keyframe.get("data_points", [])
                if len(points) != 1:
                    raise ValueError(f"Runtime keyframe must have exactly one data point: {name}/{bone_name}")
                point = points[0]
                vector = [
                    _finite_number(
                        point.get(axis),
                        f"{name}/{bone_name}/{channel}/{axis}",
                        allow_string=True,
                    )
                    for axis in ("x", "y", "z")
                ]
                time = _finite_number(keyframe.get("time"), f"{name}/{bone_name}/{channel} time")
                channels.setdefault(channel, {})[f"{time:g}"] = {
                    "post": vector,
                    "lerp_mode": "linear",
                }
            if channels:
                bones[bone_name] = channels
        animations[name] = {
            "loop": source.get("loop") == "loop",
            "animation_length": _finite_number(source.get("length"), f"{name} length"),
            "bones": bones,
        }
    if len(animations) != 5:
        raise ValueError(f"Runtime export requires five animations, got {len(animations)}")
    return {"format_version": "1.8.0", "animations": animations}


def build_runtime_bundle(document: JsonObject) -> Dict[str, bytes]:
    """Build deterministic runtime assets without mutating the Blockbench source."""
    return {
        "geo/corrupted_silverfish.geo.json": _json_bytes(_geo_document(document)),
        "animations/corrupted_silverfish.animation.json": _json_bytes(_animation_document(document)),
        "textures/entity/corrupted_silverfish.png": _texture_bytes(document),
        "meshes/entity/corrupted_silverfish.mesh": _mesh_bytes(document),
    }


def write_runtime_bundle(source: Path, runtime_root: Path) -> JsonObject:
    source = source.resolve()
    runtime_root = runtime_root.resolve()
    document = load_document(source)
    bundle = build_runtime_bundle(document)
    targets = {runtime_root / relative: payload for relative, payload in bundle.items()}
    if source in targets:
        raise ValueError("Runtime output may not overwrite the approved source model")
    _publish_transaction(targets)
    decoded = decode_runtime_mesh(bundle["meshes/entity/corrupted_silverfish.mesh"])
    return {
        "bones": len(decoded),
        "faces": sum(len(faces) for faces in decoded.values()),
        "animations": len(document["animations"]),
        "texture_bytes": len(bundle["textures/entity/corrupted_silverfish.png"]),
    }


def main(argv: List[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, default=DEFAULT_SOURCE)
    parser.add_argument("--runtime-root", type=Path, default=DEFAULT_RUNTIME_ROOT)
    args = parser.parse_args(argv)
    try:
        result = write_runtime_bundle(args.source, args.runtime_root)
    except (OSError, UnicodeDecodeError, ValueError, struct.error) as exc:
        parser.exit(1, f"RUNTIME_EXPORT_FAILED: {exc}\n")
    print(
        "RUNTIME_EXPORT_PASS "
        f"BONES={result['bones']} FACES={result['faces']} "
        f"ANIMATIONS={result['animations']} TEXTURE_BYTES={result['texture_bytes']}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
