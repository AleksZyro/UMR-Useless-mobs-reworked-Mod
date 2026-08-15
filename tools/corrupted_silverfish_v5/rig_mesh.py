"""Build a lossless, segmented Blockbench rig from the approved Tripo mesh."""

from __future__ import annotations

from collections import Counter
import copy
import json
import math
from pathlib import Path
from statistics import median
from typing import Any, Dict, Iterable, List, Tuple
from uuid import UUID, uuid5


JsonObject = Dict[str, Any]
UUID_NAMESPACE = UUID("adc0aed5-25ba-5f3f-b8c7-8fa3e16fb397")
REGION_ORDER = (
    "tail",
    "body_rear",
    "body_middle",
    "body_front",
    "head",
    "leg_front_left",
    "leg_front_right",
    "leg_middle_left",
    "leg_middle_right",
    "leg_rear_left",
    "leg_rear_right",
)
PARENTS = {
    "body_rear": "root",
    "tail": "body_rear",
    "body_middle": "body_rear",
    "body_front": "body_middle",
    "head": "body_front",
    "leg_front_left": "body_front",
    "leg_front_right": "body_front",
    "leg_middle_left": "body_middle",
    "leg_middle_right": "body_middle",
    "leg_rear_left": "body_rear",
    "leg_rear_right": "body_rear",
}


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


def classify_centroid(point: Iterable[float]) -> str:
    """Assign a face centroid to one deterministic movable region."""
    values = tuple(_number(value) for value in point)
    if len(values) != 3:
        raise ValueError("Mesh centroid must be a Vec3")
    if not all(math.isfinite(value) for value in values):
        raise ValueError("Mesh centroid values must be finite")
    x, y, z = values
    if y < 4.4 and abs(x) > 4.0:
        station = min(
            ((6.0, "front"), (0.0, "middle"), (-6.0, "rear")),
            key=lambda item: (abs(z - item[0]), -item[0]),
        )[1]
        side = "left" if x < 0 else "right"
        return f"leg_{station}_{side}"
    if z < -10:
        return "tail"
    if z < -3:
        return "body_rear"
    if z < 4:
        return "body_middle"
    if z < 10:
        return "body_front"
    return "head"


def _mesh_elements(document: JsonObject) -> List[JsonObject]:
    elements = [
        element
        for element in document.get("elements", [])
        if element.get("type") == "mesh"
    ]
    if not elements:
        raise ValueError("Blockbench model contains no mesh elements")
    return elements


def _face_centroid(vertices: JsonObject, face: JsonObject) -> Tuple[float, float, float]:
    vertex_ids = face.get("vertices", [])
    if len(vertex_ids) != 3:
        raise ValueError("Only triangular Tripo mesh faces are supported")
    points = []
    for vertex_id in vertex_ids:
        try:
            point = vertices[vertex_id]
        except KeyError as exc:
            raise ValueError(f"Mesh face references missing vertex {vertex_id!r}") from exc
        if len(point) != 3:
            raise ValueError("Mesh positions must be Vec3")
        points.append(tuple(_number(value) for value in point))
    return tuple(sum(point[axis] for point in points) / 3.0 for axis in range(3))


def _stable_id(kind: str, name: str) -> str:
    return str(uuid5(UUID_NAMESPACE, f"{kind}:{name}"))


def rig_bytes(document: JsonObject) -> bytes:
    """Serialize a rig deterministically as UTF-8 JSON with one trailing newline."""
    return (json.dumps(document, ensure_ascii=False, indent=2) + "\n").encode("utf-8")


def _region_origin(
    region: str,
    region_elements: Dict[str, JsonObject],
    fallback_y: float,
) -> List[float]:
    element = region_elements.get(region)
    points = list(element["vertices"].values()) if element is not None else []
    if region.startswith("leg_"):
        station = 6.0 if "front" in region else (0.0 if "middle" in region else -6.0)
        fallback_x = -4.0 if region.endswith("left") else 4.0
        if not points:
            return [fallback_x, 4.4, station]
        return [
            float(median(point[0] for point in points)),
            float(max(point[1] for point in points)),
            float(median(point[2] for point in points)),
        ]
    boundary_z = {
        "tail": -10.0,
        "body_rear": -10.0,
        "body_middle": -3.0,
        "body_front": 4.0,
        "head": 10.0,
    }[region]
    region_y = float(median(point[1] for point in points)) if points else fallback_y
    return [0.0, region_y, boundary_z]


def _group_record(name: str, origin: List[float]) -> JsonObject:
    return {
        "name": name,
        "uuid": _stable_id("group", name),
        "export": True,
        "locked": False,
        "origin": origin,
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


def _groups_and_outliner(region_elements: Dict[str, JsonObject]) -> Tuple[List[JsonObject], List[JsonObject]]:
    all_points = [
        point
        for element in region_elements.values()
        for point in element["vertices"].values()
    ]
    fallback_y = float(median(point[1] for point in all_points)) if all_points else 0.0
    groups_by_name = {"root": _group_record("root", [0.0, 0.0, 0.0])}
    for region in REGION_ORDER:
        groups_by_name[region] = _group_record(
            region,
            _region_origin(region, region_elements, fallback_y),
        )

    nodes = {
        name: {"uuid": group["uuid"], "isOpen": True, "children": []}
        for name, group in groups_by_name.items()
    }
    for region, element in region_elements.items():
        nodes[region]["children"].append(element["uuid"])
    for region in REGION_ORDER:
        nodes[PARENTS[region]]["children"].append(nodes[region])

    ordered_groups = [groups_by_name["root"]] + [
        groups_by_name[region] for region in REGION_ORDER
    ]
    return ordered_groups, [nodes["root"]]


def build_rig_document(source: JsonObject) -> Tuple[JsonObject, JsonObject]:
    """Split source faces into deterministic mesh regions without changing rendering data."""
    result = copy.deepcopy(source)
    region_elements: Dict[str, JsonObject] = {}
    region_vertex_ids: Dict[str, Dict[Tuple[str, str], str]] = {}
    referenced_source_vertices = set()

    for source_index, source_element in enumerate(_mesh_elements(source)):
        source_vertices = source_element.get("vertices", {})
        for face_id, source_face in source_element.get("faces", {}).items():
            region = classify_centroid(_face_centroid(source_vertices, source_face))
            if region not in region_elements:
                element = copy.deepcopy(source_element)
                element["name"] = f"{region}_mesh"
                element["uuid"] = _stable_id("element", region)
                element["vertices"] = {}
                element["faces"] = {}
                region_elements[region] = element
                region_vertex_ids[region] = {}

            target = region_elements[region]
            copied_face = copy.deepcopy(source_face)
            copied_vertices = []
            copied_uv = {}
            for source_vertex_id in source_face["vertices"]:
                source_key = (str(source_index), source_vertex_id)
                referenced_source_vertices.add(source_key)
                target_vertex_id = region_vertex_ids[region].get(source_key)
                if target_vertex_id is None:
                    target_vertex_id = _stable_id(
                        "vertex", f"{region}:{source_index}:{source_vertex_id}"
                    )
                    region_vertex_ids[region][source_key] = target_vertex_id
                    target["vertices"][target_vertex_id] = copy.deepcopy(
                        source_vertices[source_vertex_id]
                    )
                copied_vertices.append(target_vertex_id)
                copied_uv[target_vertex_id] = copy.deepcopy(
                    source_face["uv"][source_vertex_id]
                )
            copied_face["vertices"] = copied_vertices
            copied_face["uv"] = copied_uv
            target["faces"][face_id] = copied_face

    ordered_regions = [region for region in REGION_ORDER if region in region_elements]
    result["elements"] = [region_elements[region] for region in ordered_regions]
    result["groups"], result["outliner"] = _groups_and_outliner(region_elements)
    result["animations"] = []
    source_faces = sum(canonical_faces(source).values())
    result_faces = sum(canonical_faces(result).values())
    if canonical_faces(result) != canonical_faces(source):
        raise ValueError("Rig segmentation changed source geometry or UV data")
    output_vertices = sum(
        len(region_elements[region]["vertices"]) for region in ordered_regions
    )
    report = {
        "source_faces": source_faces,
        "output_faces": result_faces,
        "regions": {
            region: len(region_elements[region]["faces"])
            for region in ordered_regions
        },
        "source_vertices": len(referenced_source_vertices),
        "output_vertices": output_vertices,
        "duplicated_boundary_vertices": output_vertices - len(referenced_source_vertices),
    }
    return result, report
