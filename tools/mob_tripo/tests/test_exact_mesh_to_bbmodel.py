from __future__ import annotations

from pathlib import Path
import tempfile

from PIL import Image

from tools.mob_tripo.exact_mesh_to_bbmodel import build_document


def _part(pivot=(0.0, 8.0, 0.0)):
    return {
        "pivot": pivot,
        "faces": [(
            ((0.0, 0.0, 0.0), (0.0, 0.0)),
            ((1.0, 0.0, 0.0), (1.0, 0.0)),
            ((0.0, 1.0, 0.0), (0.0, 1.0)),
        )],
    }


def _elements_by_type(document, element_type):
    return [element for element in document["elements"] if element["type"] == element_type]


def test_exact_preview_preserves_faces_uvs_and_has_no_cubes():
    parts = {
        "body": _part(), "head": _part(), "right_arm": _part(),
        "left_arm": _part(), "right_leg": _part(), "left_leg": _part(),
    }
    with tempfile.TemporaryDirectory() as temp:
        texture = Path(temp) / "texture.png"
        Image.new("RGBA", (16, 8), (255, 255, 255, 255)).save(texture)
        document = build_document("frost_stray", parts, texture)

    meshes = _elements_by_type(document, "mesh")
    bones = _elements_by_type(document, "armature_bone")
    armatures = _elements_by_type(document, "armature")
    assert len(meshes) == 1
    assert len(armatures) == 1
    assert sum(len(element["faces"]) for element in meshes) == 6
    first_uvs = next(iter(meshes[0]["faces"].values()))["uv"].values()
    assert [0.0, 0.0] in first_uvs
    assert [16.0, 0.0] in first_uvs
    assert [0.0, 8.0] in first_uvs
    first_vertices = list(meshes[0]["vertices"].values())
    assert [0.0, 24.0, 0.0] in first_vertices
    assert [0.0, 23.0, 0.0] in first_vertices
    names = [bone["name"] for bone in bones]
    assert names == ["root", "body", "head", "right_arm", "left_arm", "right_leg", "left_leg", "bow_anchor"]
    assert document["outliner"][0]["uuid"] == armatures[0]["uuid"]
    assert document["outliner"][0]["children"][0] == meshes[0]["uuid"]
    assert meshes[0]["uuid"] in armatures[0]["children"]
    assert all(bone["vertex_weights"] or bone["name"] in {"root", "bow_anchor"} for bone in bones)
    assert document["animations"] == []


def test_spider_and_allay_have_anatomical_hierarchy_before_animation():
    with tempfile.TemporaryDirectory() as temp:
        texture = Path(temp) / "texture.png"
        Image.new("RGBA", (8, 8), (255, 255, 255, 255)).save(texture)

        spider = {"body": _part(), **{f"web_leg_{index}": _part() for index in range(8)}}
        spider_doc = build_document("web_cave_spider", spider, texture)
        spider_names = [bone["name"] for bone in _elements_by_type(spider_doc, "armature_bone")]
        assert spider_names == ["root", "body", *[f"web_leg_{index}" for index in range(8)]]

        allay = {
            name: _part() for name in (
                "body", "head", "right_arm", "left_arm", "right_wing", "right_wing_tip",
                "left_wing", "left_wing_tip", "soul_core",
            )
        }
        allay_doc = build_document("helping_allay", allay, texture)
        bones = _elements_by_type(allay_doc, "armature_bone")
        by_name = {bone["name"]: bone for bone in bones}
        assert by_name["right_wing_tip"]["uuid"] in by_name["right_wing"]["children"]
        assert by_name["left_wing_tip"]["uuid"] in by_name["left_wing"]["children"]
        assert "item_anchor" in by_name
        assert allay_doc["animations"] == []


def test_coincident_seam_vertices_receive_identical_blended_weights():
    def translated(dx):
        part = _part()
        part["faces"][0] = tuple(((position[0] + dx, position[1], position[2]), uv) for position, uv in part["faces"][0])
        return part

    body = _part()
    arm = _part()
    arm["faces"][0] = (
        ((0.0, 0.0, 0.0), (0.5, 0.5)),
        ((-1.0, 0.0, 0.0), (0.0, 0.5)),
        ((0.0, -1.0, 0.0), (0.5, 0.0)),
    )
    parts = {
        "body": body, "head": translated(10), "right_arm": arm,
        "left_arm": translated(20), "right_leg": translated(30), "left_leg": translated(40),
    }
    with tempfile.TemporaryDirectory() as temp:
        texture = Path(temp) / "texture.png"
        Image.new("RGBA", (16, 16), (255, 255, 255, 255)).save(texture)
        document = build_document("frost_stray", parts, texture)

    mesh = _elements_by_type(document, "mesh")[0]
    bones = _elements_by_type(document, "armature_bone")
    seam_vertices = [key for key, value in mesh["vertices"].items() if value == [0.0, 24.0, 0.0]]
    assert len(seam_vertices) == 2  # same position, different UVs
    weight_vectors = []
    for vertex in seam_vertices:
        suffix = f":{vertex}"
        vector = {bone["name"]: weight for bone in bones for key, weight in bone["vertex_weights"].items() if key.endswith(suffix)}
        weight_vectors.append(vector)
    assert weight_vectors == [{"body": 0.5, "right_arm": 0.5}] * 2
