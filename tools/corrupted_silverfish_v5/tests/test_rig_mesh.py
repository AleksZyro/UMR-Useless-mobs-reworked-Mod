import copy
import json
import math
from pathlib import Path
import tempfile
import unittest
from unittest import mock

from tools.corrupted_silverfish_v5.rig_mesh import (
    build_rig_document,
    canonical_faces,
    classify_centroid,
    rig_bytes,
    texture_signature,
    write_rig_files,
)


def make_two_triangle_fixture():
    return {
        "meta": {"format_version": "5.0", "model_format": "free"},
        "name": "fixture",
        "resolution": {"width": 16, "height": 16},
        "elements": [
            {
                "name": "source_mesh",
                "type": "mesh",
                "uuid": "source-element",
                "origin": [0, 0, 0],
                "rotation": [0, 0, 0],
                "vertices": {
                    "a": [-5, 1, -6],
                    "b": [-5, 2, -6],
                    "c": [-5, 1, -5],
                    "d": [5, 1, 6],
                    "e": [5, 2, 6],
                    "f": [5, 1, 5],
                },
                "faces": {
                    "left": {
                        "texture": 0,
                        "vertices": ["a", "b", "c"],
                        "uv": {"a": [1, 1], "b": [1, 2], "c": [2, 1]},
                    },
                    "right": {
                        "texture": 0,
                        "vertices": ["d", "e", "f"],
                        "uv": {"d": [3, 3], "e": [3, 4], "f": [4, 3]},
                    },
                },
            }
        ],
        "outliner": ["source-element"],
        "textures": [
            {
                "name": "texture",
                "source": "data:image/png;base64,Zml4dHVyZQ==",
                "width": 4096,
                "height": 4096,
                "uv_width": 16,
                "uv_height": 16,
            }
        ],
    }


def make_shared_seam_fixture():
    document = make_two_triangle_fixture()
    mesh = document["elements"][0]
    mesh["vertices"]["seam"] = [0, 1, 0]
    mesh["faces"]["left"]["vertices"][2] = "seam"
    mesh["faces"]["left"]["uv"].pop("c")
    mesh["faces"]["left"]["uv"]["seam"] = [2, 1]
    mesh["faces"]["right"]["vertices"][2] = "seam"
    mesh["faces"]["right"]["uv"].pop("f")
    mesh["faces"]["right"]["uv"]["seam"] = [4, 3]
    mesh["vertices"].pop("c")
    mesh["vertices"].pop("f")
    return document


class LosslessRigContractTests(unittest.TestCase):
    def test_repartition_preserves_every_position_uv_and_texture(self):
        source = make_two_triangle_fixture()

        rigged, _ = build_rig_document(copy.deepcopy(source))

        self.assertEqual(canonical_faces(rigged), canonical_faces(source))
        self.assertEqual(texture_signature(rigged), texture_signature(source))

    def test_source_document_is_not_mutated(self):
        source = make_two_triangle_fixture()
        original = copy.deepcopy(source)

        build_rig_document(source)

        self.assertEqual(source, original)


class RegionClassificationTests(unittest.TestCase):
    def test_leg_regions_are_symmetric_and_use_three_z_stations(self):
        points = (
            (-6, 2, 6), (6, 2, 6),
            (-6, 2, 0), (6, 2, 0),
            (-6, 2, -6), (6, 2, -6),
        )

        names = {classify_centroid(point) for point in points}

        self.assertEqual(
            names,
            {
                "leg_front_left", "leg_front_right",
                "leg_middle_left", "leg_middle_right",
                "leg_rear_left", "leg_rear_right",
            },
        )

    def test_longitudinal_boundaries_have_deterministic_owners(self):
        self.assertEqual(classify_centroid((0, 6, -10.0001)), "tail")
        self.assertEqual(classify_centroid((0, 6, -10)), "body_rear")
        self.assertEqual(classify_centroid((0, 6, -3)), "body_middle")
        self.assertEqual(classify_centroid((0, 6, 4)), "body_front")
        self.assertEqual(classify_centroid((0, 6, 10)), "head")

    def test_nonfinite_centroid_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "finite"):
            classify_centroid((float("nan"), 1, 1))

    def test_fixture_is_split_without_changing_faces(self):
        source = make_two_triangle_fixture()

        rigged, report = build_rig_document(source)

        self.assertEqual(len(rigged["elements"]), 2)
        self.assertEqual(
            {element["name"] for element in rigged["elements"]},
            {"leg_rear_left_mesh", "leg_front_right_mesh"},
        )
        self.assertEqual(report["regions"], {
            "leg_rear_left": 1,
            "leg_front_right": 1,
        })
        self.assertEqual(canonical_faces(rigged), canonical_faces(source))

    def test_only_cross_region_seam_vertices_are_duplicated(self):
        source = make_shared_seam_fixture()

        rigged, report = build_rig_document(source)

        self.assertEqual(report["source_vertices"], 5)
        self.assertEqual(report["output_vertices"], 6)
        self.assertEqual(report["duplicated_boundary_vertices"], 1)
        self.assertEqual(canonical_faces(rigged), canonical_faces(source))

    def test_serialized_rig_bytes_are_deterministic(self):
        source = make_two_triangle_fixture()

        first = rig_bytes(build_rig_document(source)[0])
        second = rig_bytes(build_rig_document(copy.deepcopy(source))[0])

        self.assertEqual(first, second)
        self.assertTrue(first.endswith(b"\n"))


class BoneHierarchyTests(unittest.TestCase):
    EXPECTED_PARENTS = {
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

    @staticmethod
    def _parent_maps(document):
        groups = {group["uuid"]: group["name"] for group in document["groups"]}
        group_parents = {}
        element_owners = {}

        def visit(node, parent_name=None):
            name = groups[node["uuid"]]
            if parent_name is not None:
                group_parents[name] = parent_name
            for child in node["children"]:
                if isinstance(child, dict):
                    visit(child, name)
                else:
                    element_owners[child] = name

        for root in document["outliner"]:
            visit(root)
        return group_parents, element_owners

    def test_expected_group_hierarchy_and_mesh_ownership(self):
        rigged, _ = build_rig_document(make_two_triangle_fixture())

        group_parents, element_owners = self._parent_maps(rigged)
        groups = {group["name"]: group for group in rigged["groups"]}

        self.assertEqual(set(groups), {"root", *self.EXPECTED_PARENTS})
        self.assertEqual(group_parents, self.EXPECTED_PARENTS)
        for element in rigged["elements"]:
            region = element["name"].removesuffix("_mesh")
            self.assertEqual(element_owners[element["uuid"]], region)

    def test_pivots_are_finite_and_rest_rotations_are_zero(self):
        rigged, _ = build_rig_document(make_two_triangle_fixture())

        for group in rigged["groups"]:
            self.assertEqual(group["rotation"], [0, 0, 0])
            self.assertEqual(len(group["origin"]), 3)
            self.assertTrue(all(math.isfinite(value) for value in group["origin"]))
        self.assertEqual(rigged["animations"], [])


class SafeRigWriterTests(unittest.TestCase):
    def test_source_may_not_be_used_as_output(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory) / "source.bbmodel"
            report = Path(directory) / "report.json"
            source.write_text(json.dumps(make_two_triangle_fixture()), encoding="utf-8")

            with self.assertRaisesRegex(ValueError, "source"):
                write_rig_files(source, source, report)

            self.assertEqual(json.loads(source.read_text(encoding="utf-8"))["name"], "fixture")
            self.assertFalse(report.exists())

    def test_publish_failure_restores_both_existing_targets(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "source.bbmodel"
            output = root / "rig.bbmodel"
            report = root / "report.json"
            source.write_text(json.dumps(make_two_triangle_fixture()), encoding="utf-8")
            output.write_bytes(b"OLD_RIG")
            report.write_bytes(b"OLD_REPORT")
            real_replace = __import__("os").replace
            calls = 0

            def fail_second_publish(old, new):
                nonlocal calls
                calls += 1
                if calls == 4:
                    raise OSError("injected second publish failure")
                return real_replace(old, new)

            with mock.patch(
                "tools.corrupted_silverfish_v5.rig_mesh.os.replace",
                side_effect=fail_second_publish,
            ):
                with self.assertRaisesRegex(OSError, "second publish"):
                    write_rig_files(source, output, report)

            self.assertEqual(output.read_bytes(), b"OLD_RIG")
            self.assertEqual(report.read_bytes(), b"OLD_REPORT")
            self.assertEqual(
                sorted(path.name for path in root.iterdir()),
                ["report.json", "rig.bbmodel", "source.bbmodel"],
            )

    def test_success_writes_valid_rig_and_report(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "source.bbmodel"
            output = root / "rig.bbmodel"
            report = root / "report.json"
            source.write_text(json.dumps(make_two_triangle_fixture()), encoding="utf-8")

            result = write_rig_files(source, output, report)

            rigged = json.loads(output.read_text(encoding="utf-8"))
            written_report = json.loads(report.read_text(encoding="utf-8"))
            self.assertEqual(canonical_faces(rigged), canonical_faces(make_two_triangle_fixture()))
            self.assertEqual(written_report, result)
            self.assertEqual(written_report["output_faces"], 2)


if __name__ == "__main__":
    unittest.main()
