"""Create a Blockbench inspection project from an active UMR exact runtime mesh.

The exporter preserves every runtime triangle, UV coordinate, bone assignment,
and pivot.  It does not voxelise or re-segment the approved Tripo surface.  The
result is an inspection/rigging source; animation is deliberately left empty
until the model has passed Blockbench and seam QA.
"""

from __future__ import annotations

import argparse
import base64
import json
import math
import os
from pathlib import Path
import tempfile
from typing import Any, Mapping
from uuid import UUID, uuid5

from PIL import Image

from tools.mob_tripo.exact_runtime import decode_mesh


UUID_NAMESPACE = UUID("64d4c121-2f2a-5ead-8879-df7c3cfd93f5")


def _uuid(*parts: object) -> str:
    return str(uuid5(UUID_NAMESPACE, ":".join(map(str, parts))))


def _texture_source(path: Path) -> tuple[str, int, int]:
    payload = path.read_bytes()
    with Image.open(path) as image:
        image.load()
        width, height = image.size
    return "data:image/png;base64," + base64.b64encode(payload).decode("ascii"), width, height


def _blockbench_position(position: tuple[float, float, float] | list[float]) -> list[float]:
    return [
        round(float(position[0]), 7),
        round(24.0 - float(position[1]), 7),
        round(float(position[2]), 7),
    ]


def _combined_mesh_element(
    name: str,
    parts: Mapping[str, Mapping[str, Any]],
    texture_width: int,
    texture_height: int,
) -> tuple[dict[str, Any], dict[str, set[str]]]:
    vertices: dict[str, list[float]] = {}
    vertex_lookup: dict[tuple[float, float, float, float, float], str] = {}
    position_owners: dict[tuple[float, float, float], set[str]] = {}
    vertex_positions: dict[str, tuple[float, float, float]] = {}
    faces: dict[str, Any] = {}

    global_face_index = 0
    for owner, part in parts.items():
        for face_index, face in enumerate(part["faces"]):
            face_vertices: list[str] = []
            face_uv: dict[str, list[float]] = {}
            for position, uv in face:
                values = (*position, *uv)
                if len(values) != 5 or not all(math.isfinite(float(value)) for value in values):
                    raise ValueError(f"{owner} contains a non-finite vertex")
                position_key = tuple(round(float(value), 7) for value in position)
                position_owners.setdefault(position_key, set()).add(owner)
                key = tuple(round(float(value), 7) for value in values)
                vertex_id = vertex_lookup.get(key)
                if vertex_id is None:
                    vertex_id = _uuid("vertex", name, len(vertex_lookup))
                    vertex_lookup[key] = vertex_id
                    vertex_positions[vertex_id] = position_key
                    # The runtime mesh is Minecraft Y-down with its floor at Y=24;
                    # Blockbench's generic workspace is Y-up.
                    vertices[vertex_id] = _blockbench_position(position)
                face_vertices.append(vertex_id)
                # The active runtime texture and decoded UVs already use the same
                # top-down convention consumed by Blockbench.
                face_uv[vertex_id] = [
                    round(float(uv[0]) * texture_width, 5),
                    round(float(uv[1]) * texture_height, 5),
                ]
            if len(set(face_vertices)) != 3:
                raise ValueError(f"{owner} contains a degenerate UV/position triangle at face {face_index}")
            faces[_uuid("face", name, global_face_index)] = {
                "uv": face_uv,
                "texture": 0,
                "vertices": face_vertices,
            }
            global_face_index += 1

    element_id = _uuid("element", name)
    element = {
        "name": f"{name}_exact_surface",
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
        "uuid": element_id,
    }
    vertex_owners = {vertex_id: position_owners[position] for vertex_id, position in vertex_positions.items()}
    return element, vertex_owners


def _bone_layout(name: str, parts: Mapping[str, Mapping[str, Any]]) -> list[tuple[str, str | None, tuple[float, float, float]]]:
    root = ("root", None, (0.0, 24.0, 0.0))
    if name == "frost_stray":
        order = [root, ("body", "root", tuple(parts["body"]["pivot"]))]
        order.extend((part, "body", tuple(parts[part]["pivot"])) for part in ("head", "right_arm", "left_arm", "right_leg", "left_leg"))
        order.append(("bow_anchor", "right_arm", tuple(parts["right_arm"]["pivot"])))
    elif name == "web_cave_spider":
        order = [root, ("body", "root", tuple(parts["body"]["pivot"]))]
        order.extend((f"web_leg_{index}", "body", tuple(parts[f"web_leg_{index}"]["pivot"])) for index in range(8))
    elif name == "helping_allay":
        order = [root, ("body", "root", tuple(parts["body"]["pivot"]))]
        order.extend((part, "body", tuple(parts[part]["pivot"])) for part in ("head", "right_arm", "left_arm", "right_wing", "left_wing", "soul_core"))
        order.extend([
            ("right_wing_tip", "right_wing", tuple(parts["right_wing_tip"]["pivot"])),
            ("left_wing_tip", "left_wing", tuple(parts["left_wing_tip"]["pivot"])),
            ("item_anchor", "right_arm", tuple(parts["right_arm"]["pivot"])),
        ])
    else:
        order = [root]
        order.extend((part, "root", tuple(data["pivot"])) for part, data in parts.items())
    return order


def _armature_elements(
    name: str,
    parts: Mapping[str, Mapping[str, Any]],
    mesh_id: str,
    vertex_owners: Mapping[str, set[str]],
) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    layout = _bone_layout(name, parts)
    bone_ids = {bone_name: _uuid("armature_bone", name, bone_name) for bone_name, _, _ in layout}
    children: dict[str, list[str]] = {bone_name: [] for bone_name in bone_ids}
    for bone_name, parent, _ in layout:
        if parent:
            children[parent].append(bone_ids[bone_name])

    weight_key_prefix = mesh_id[:6] + ":"
    weights: dict[str, dict[str, float]] = {bone_name: {} for bone_name in bone_ids}
    for vertex_id, owners in vertex_owners.items():
        owners = {owner for owner in owners if owner in parts}
        if not owners:
            continue
        weight = round(1.0 / len(owners), 7)
        for owner in owners:
            weights[owner][weight_key_prefix + vertex_id] = weight

    bones = []
    for color, (bone_name, _parent, pivot) in enumerate(layout):
        bones.append({
            "isOpen": True,
            "uuid": bone_ids[bone_name],
            "type": "armature_bone",
            "name": bone_name,
            "children": children[bone_name],
            "origin": _blockbench_position(pivot),
            "rotation": [0, 0, 0],
            "length": 4,
            "width": 1.5,
            "connected": False,
            "color": color % 8,
            "vertex_weights": weights[bone_name],
            "export": True,
            "locked": False,
            "visibility": True,
        })
    armature = {
        "isOpen": True,
        "uuid": _uuid("armature", name),
        "type": "armature",
        "name": f"{name}_armature",
        "children": [mesh_id, bone_ids["root"]],
        "origin": [0, 0, 0],
        "export": True,
        "locked": False,
        "visibility": True,
    }
    return armature, bones


def _outliner_tree(armature: Mapping[str, Any], bones: list[Mapping[str, Any]], mesh_id: str) -> list[dict[str, Any]]:
    by_id = {bone["uuid"]: bone for bone in bones}

    def node(bone_id: str) -> dict[str, Any]:
        bone = by_id[bone_id]
        return {
            "uuid": bone_id,
            "isOpen": True,
            "children": [node(child_id) for child_id in bone["children"]],
        }

    root_ids = [child_id for child_id in armature["children"] if child_id != mesh_id]
    return [{
        "uuid": armature["uuid"],
        "isOpen": True,
        "children": [mesh_id, *(node(root_id) for root_id in root_ids)],
    }]


def build_document(name: str, parts: Mapping[str, Mapping[str, Any]], texture_path: Path) -> dict[str, Any]:
    source, width, height = _texture_source(texture_path)
    mesh, vertex_owners = _combined_mesh_element(name, parts, width, height)
    armature, bones = _armature_elements(name, parts, mesh["uuid"], vertex_owners)
    document = {
        "meta": {"format_version": "5.0", "model_format": "free", "box_uv": False},
        "name": f"{name} Exact Rig QA",
        "model_identifier": f"{name}_exact_rig_qa",
        "visible_box": [4, 4, 0],
        "resolution": {"width": width, "height": height},
        "elements": [mesh, armature, *bones],
        "groups": [],
        "textures": [{
            "name": texture_path.name,
            "path": str(texture_path.resolve()),
            "folder": "",
            "namespace": "",
            "id": "0",
            "group": "",
            "scope": 0,
            "width": width,
            "height": height,
            "uv_width": width,
            "uv_height": height,
            "particle": False,
            "use_as_default": True,
            "layers_enabled": False,
            "file_format": "png",
            "render_mode": "default",
            "render_sides": "auto",
            "wrap_mode": "repeat",
            "pbr_channel": "color",
            "visible": True,
            "internal": True,
            "saved": True,
            "uuid": _uuid("texture", name),
            "source": source,
        }],
        "animations": [],
        "outliner": _outliner_tree(armature, bones, mesh["uuid"]),
    }
    validate_document(document)
    return document


def validate_document(document: Mapping[str, Any]) -> dict[str, int]:
    meshes = [element for element in document["elements"] if element.get("type") == "mesh"]
    armatures = [element for element in document["elements"] if element.get("type") == "armature"]
    bones = [element for element in document["elements"] if element.get("type") == "armature_bone"]
    if len(meshes) != 1 or len(armatures) != 1 or not bones:
        raise ValueError("weighted QA project requires exactly one mesh, one armature, and at least one bone")
    mesh = meshes[0]
    prefix = mesh["uuid"][:6] + ":"
    vectors: dict[str, tuple[tuple[str, float], ...]] = {}
    positions: dict[tuple[float, float, float], tuple[tuple[str, float], ...]] = {}
    for vertex_id, position in mesh["vertices"].items():
        key = prefix + vertex_id
        vector = tuple(sorted(
            (bone["name"], float(bone.get("vertex_weights", {}).get(key, 0.0)))
            for bone in bones if bone.get("vertex_weights", {}).get(key, 0.0)
        ))
        if not vector or not math.isclose(sum(weight for _, weight in vector), 1.0, abs_tol=1e-5):
            raise ValueError(f"vertex {vertex_id} does not have normalized armature weights")
        vectors[vertex_id] = vector
        position_key = tuple(round(float(value), 7) for value in position)
        previous = positions.setdefault(position_key, vector)
        if previous != vector:
            raise ValueError(f"coincident vertices at {position_key} have mismatched weights")
    vertex_ids = set(mesh["vertices"])
    if any(not set(face["vertices"]).issubset(vertex_ids) for face in mesh["faces"].values()):
        raise ValueError("mesh face references a missing vertex")
    if document.get("animations"):
        raise ValueError("QA rig must remain unanimated until visual and seam approval")
    return {"meshes": 1, "bones": len(bones), "vertices": len(vectors), "triangles": len(mesh["faces"])}


def write_document(path: Path, document: Mapping[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    descriptor, temp_name = tempfile.mkstemp(prefix=f".{path.name}.", suffix=".tmp", dir=path.parent)
    try:
        with os.fdopen(descriptor, "w", encoding="utf-8", newline="\n") as handle:
            json.dump(document, handle, ensure_ascii=False, separators=(",", ":"), allow_nan=False)
            handle.write("\n")
            handle.flush()
            os.fsync(handle.fileno())
        os.replace(temp_name, path)
    finally:
        Path(temp_name).unlink(missing_ok=True)


def convert(name: str, mesh_path: Path, texture_path: Path, output: Path) -> dict[str, int]:
    parts = decode_mesh(mesh_path.read_bytes())
    document = build_document(name, parts, texture_path)
    write_document(output, document)
    return {
        "bones": len(parts),
        "triangles": sum(len(part["faces"]) for part in parts.values()),
        "cubes": 0,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--name", required=True)
    parser.add_argument("--mesh", type=Path, required=True)
    parser.add_argument("--texture", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    try:
        result = convert(args.name, args.mesh, args.texture, args.output)
    except (OSError, ValueError, json.JSONDecodeError) as exc:
        print(f"EXACT_BBMODEL_FAILED: {exc}")
        return 1
    print(
        "EXACT_BBMODEL_PASS "
        f"BONES={result['bones']} TRIANGLES={result['triangles']} CUBES=0 OUTPUT={args.output}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
