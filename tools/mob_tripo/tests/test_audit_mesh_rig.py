from __future__ import annotations

import importlib.util
import json
from pathlib import Path
import tempfile


SCRIPT = Path(__file__).resolve().parents[2] / "skills/minecraft-tripo-rigging/scripts/audit_mesh_rig.py"
SPEC = importlib.util.spec_from_file_location("audit_mesh_rig", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(MODULE)


def _mesh(name: str, uuid: str, shared: tuple[float, float, float]):
    return {
        "name": name,
        "uuid": uuid,
        "type": "mesh",
        "vertices": {"a": list(shared), "b": [1, 0, 0], "c": [0, 1, 0]},
        "faces": {"face": {"vertices": ["a", "b", "c"]}},
    }


def test_nested_blockbench_groups_assign_one_owner_and_require_explicit_seams():
    document = {
        "elements": [_mesh("body_surface", "body-element", (0, 0, 0)), _mesh("arm_surface", "arm-element", (0, 0, 0))],
        "outliner": [{
            "name": "root", "uuid": "root", "children": [{
                "name": "body", "uuid": "body", "children": ["body-element", {
                    "name": "right_arm", "uuid": "right-arm", "children": ["arm-element"],
                }],
            }],
        }],
    }
    with tempfile.TemporaryDirectory() as temp:
        path = Path(temp) / "rig.bbmodel"
        path.write_text(json.dumps(document), encoding="utf-8")
        unsafe, faces = MODULE.audit(path, set())
        assert faces == 2
        assert unsafe == [("body", "right_arm", 3)]
        safe, _ = MODULE.audit(path, {("body", "right_arm")})
        assert safe == []
