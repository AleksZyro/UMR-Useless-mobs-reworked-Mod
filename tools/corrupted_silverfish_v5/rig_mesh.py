"""Build a lossless, segmented Blockbench rig from the approved Tripo mesh."""

from __future__ import annotations

from collections import Counter
import copy
import json
from pathlib import Path
from typing import Any, Dict, Tuple


JsonObject = Dict[str, Any]


def load_document(path: Path) -> JsonObject:
    """Load one Blockbench document without retaining mutable source bytes."""
    try:
        document = json.loads(path.read_text(encoding="utf-8-sig"))
    except (OSError, UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise ValueError(f"Unable to read Blockbench model {path}: {exc}") from exc
    if not isinstance(document, dict):
        raise ValueError(f"Blockbench model root must be an object: {path}")
    return document


def _number(value: Any) -> float:
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        raise ValueError(f"Expected numeric mesh coordinate, got {value!r}")
    return float(value)


def canonical_faces(document: JsonObject) -> Counter[Tuple[Any, ...]]:
    """Return geometry/UV signatures independent of generated Blockbench IDs."""
    signatures: Counter[Tuple[Any, ...]] = Counter()
    for element in document.get("elements", []):
        if element.get("type") != "mesh":
            continue
        vertices = element.get("vertices", {})
        for face in element.get("faces", {}).values():
            corners = []
            for vertex_id in face.get("vertices", []):
                try:
                    position = vertices[vertex_id]
                    uv = face["uv"][vertex_id]
                except (KeyError, TypeError) as exc:
                    raise ValueError(f"Mesh face references missing vertex or UV {vertex_id!r}") from exc
                if len(position) != 3 or len(uv) != 2:
                    raise ValueError("Mesh positions must be Vec3 and UV coordinates must be Vec2")
                corners.append(
                    (
                        tuple(_number(value) for value in position),
                        tuple(_number(value) for value in uv),
                    )
                )
            if len(corners) != 3:
                raise ValueError("Only triangular Tripo mesh faces are supported")
            signatures[(tuple(corners), face.get("texture"))] += 1
    return signatures


def texture_signature(document: JsonObject) -> Tuple[Any, ...]:
    textures = document.get("textures", [])
    if len(textures) != 1:
        raise ValueError("Tripo mesh rig requires exactly one embedded texture")
    texture = textures[0]
    return (
        texture.get("source"),
        texture.get("width"),
        texture.get("height"),
        texture.get("uv_width"),
        texture.get("uv_height"),
    )


def build_rig_document(source: JsonObject) -> Tuple[JsonObject, JsonObject]:
    """Return an independent document; segmentation is added in the next slice."""
    result = copy.deepcopy(source)
    report = {"source_faces": sum(canonical_faces(source).values())}
    return result, report
