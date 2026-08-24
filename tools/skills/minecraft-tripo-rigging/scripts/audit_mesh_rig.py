"""Reject unapproved shared-position seams between animated Blockbench mesh bones."""

from __future__ import annotations

import argparse
import json
import math
from pathlib import Path
from typing import Any


def _pair(value: str) -> tuple[str, str]:
    names = tuple(part.strip() for part in value.split(":", 1))
    if len(names) != 2 or not all(names) or names[0] == names[1]:
        raise argparse.ArgumentTypeError("seam must be BONE_A:BONE_B")
    return tuple(sorted(names))


def _owners(document: dict[str, Any]) -> dict[str, str]:
    groups = {group["uuid"]: group["name"] for group in document.get("groups", [])}
    result: dict[str, str] = {}

    def visit(node: Any, owner: str | None = None) -> None:
        if isinstance(node, str):
            if owner is not None:
                if node in result:
                    raise ValueError(f"element has multiple owners: {node}")
                result[node] = owner
            return
        if not isinstance(node, dict) or node.get("uuid") not in groups:
            raise ValueError("invalid outliner group")
        group = groups[node["uuid"]]
        for child in node.get("children", []):
            visit(child, group)

    for root in document.get("outliner", []):
        visit(root)
    return result


def audit(path: Path, allowed: set[tuple[str, str]]) -> tuple[list[tuple[str, str, int]], int]:
    document = json.loads(path.read_text(encoding="utf-8-sig"))
    owners = _owners(document)
    positions: dict[str, set[tuple[float, float, float]]] = {}
    face_count = 0
    for element in document.get("elements", []):
        if element.get("type") != "mesh":
            continue
        owner = owners.get(element.get("uuid"))
        if owner is None:
            raise ValueError(f"mesh has no owning bone: {element.get('name')}")
        bucket = positions.setdefault(owner, set())
        for raw in element.get("vertices", {}).values():
            if len(raw) != 3 or not all(
                isinstance(value, (int, float))
                and not isinstance(value, bool)
                and math.isfinite(value)
                for value in raw
            ):
                raise ValueError(f"invalid vertex in {element.get('name')}")
            bucket.add(tuple(round(float(value), 6) for value in raw))
        face_count += len(element.get("faces", {}))

    seams = []
    bones = sorted(positions)
    for index, left in enumerate(bones):
        for right in bones[index + 1:]:
            shared = len(positions[left] & positions[right])
            if shared:
                seams.append((left, right, shared))
    unsafe = [seam for seam in seams if tuple(sorted(seam[:2])) not in allowed]
    return unsafe, face_count


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("model", type=Path)
    parser.add_argument("--allow-seam", action="append", default=[], type=_pair)
    args = parser.parse_args()
    try:
        unsafe, faces = audit(args.model, set(args.allow_seam))
    except (OSError, UnicodeDecodeError, json.JSONDecodeError, KeyError, TypeError, ValueError) as exc:
        parser.exit(1, f"RIG_AUDIT_FAILED: {exc}\n")
    if unsafe:
        details = ", ".join(f"{left}:{right}={count}" for left, right, count in unsafe)
        parser.exit(1, f"RIG_AUDIT_FAILED: unapproved animated seams: {details}\n")
    print(f"RIG_AUDIT_PASS BONES=COHESIVE FACES={faces} APPROVED_SEAMS={len(args.allow_seam)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
