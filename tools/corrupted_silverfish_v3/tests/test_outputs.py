from pathlib import Path
import unittest

from tools.corrupted_silverfish_v3 import build

ROOT = Path(__file__).resolve().parents[3]
EXPORT = ROOT / "Modelle" / "Exports" / "corrupted_silverfish_v3"
FACE_ORDER = ("north", "east", "south", "west", "up", "down")

class GeneratedOutputContract(unittest.TestCase):
    def test_required_candidate_files_exist(self):
        required = (
            EXPORT / "geo" / "corrupted_silverfish.geo.json",
            EXPORT / "textures" / "entity" / "corrupted_silverfish.png",
            EXPORT / "textures" / "entity" / "corrupted_silverfish_glowmask.png",
            EXPORT / "animations" / "corrupted_silverfish.animation.json",
            ROOT / "Modelle" / "Editierbar" / "Corrupted Silverfish v3.bbmodel",
        )
        self.maxDiff = None
        self.assertEqual([], [str(path) for path in required if not path.is_file()])

    def test_geometry_document_has_expected_metadata_and_counts(self):
        document = build.geometry_document()
        self.assertEqual("1.12.0", document["format_version"])
        self.assertEqual(1, len(document["minecraft:geometry"]))
        geometry = document["minecraft:geometry"][0]
        description = geometry["description"]
        self.assertEqual("geometry.corrupted_silverfish", description["identifier"])
        self.assertEqual((256, 256), (description["texture_width"], description["texture_height"]))
        self.assertEqual(2.6, description["visible_bounds_width"])
        self.assertEqual(1.7, description["visible_bounds_height"])
        self.assertEqual([0, 0.55, 0], description["visible_bounds_offset"])
        self.assertEqual(32, len(geometry["bones"]))
        self.assertEqual(112, sum(len(bone.get("cubes", [])) for bone in geometry["bones"]))

    def test_geometry_hierarchy_is_unique_valid_and_parent_first(self):
        bones = build.geometry_document()["minecraft:geometry"][0]["bones"]
        names = [bone["name"] for bone in bones]
        self.assertEqual(len(names), len(set(names)))
        self.assertEqual(["root"], [bone["name"] for bone in bones if "parent" not in bone])
        seen = set()
        for bone in bones:
            if "parent" in bone:
                self.assertIn(bone["parent"], seen)
            seen.add(bone["name"])

    def test_geometry_cubes_have_valid_transform_and_explicit_faces(self):
        bones = build.geometry_document()["minecraft:geometry"][0]["bones"]
        for bone in bones:
            for cube in bone.get("cubes", []):
                with self.subTest(cube=cube["name"]):
                    self.assertEqual(3, len(cube["origin"]))
                    self.assertEqual(3, len(cube["size"]))
                    self.assertTrue(all(value > 0 for value in cube["size"]))
                    self.assertEqual(set(FACE_ORDER), set(cube["uv"]))
                    if "rotation" in cube:
                        self.assertEqual(3, len(cube["rotation"]))
                        self.assertEqual(3, len(cube["pivot"]))
                    for face_name in FACE_ORDER:
                        face = cube["uv"][face_name]
                        self.assertEqual(2, len(face["uv"]))
                        self.assertEqual(2, len(face["uv_size"]))
                        self.assertTrue(all(value > 0 for value in face["uv_size"]))
                        u, v = face["uv"]
                        width, height = face["uv_size"]
                        self.assertGreaterEqual(u, 0)
                        self.assertGreaterEqual(v, 0)
                        self.assertLessEqual(u + width, 256)
                        self.assertLessEqual(v + height, 256)

    def test_bbmodel_is_structural_geckolib_project(self):
        document = build.bbmodel_document()
        self.assertEqual(
            {"format_version": "5.0", "model_format": "geckolib_model", "box_uv": False},
            document["meta"],
        )
        self.assertEqual("Corrupted Silverfish v3", document["name"])
        self.assertEqual("geometry.corrupted_silverfish", document["model_identifier"])
        self.assertEqual([2.6, 1.7, 0], document["visible_box"])
        self.assertEqual({"width": 256, "height": 256}, document["resolution"])
        self.assertEqual("usless_mobs", document["geckolib_modid"])
        self.assertEqual("Entity", document["geckolib_model_type"])
        self.assertEqual(32, len(document["groups"]))
        self.assertEqual(112, len(document["elements"]))
        self.assertEqual([], document["textures"])
        self.assertEqual([], document["animations"])
        for element in document["elements"]:
            with self.subTest(element=element["name"]):
                self.assertEqual("cube", element["type"])
                self.assertEqual(3, len(element["from"]))
                self.assertEqual(3, len(element["to"]))
                self.assertEqual(3, len(element["origin"]))
                self.assertEqual(3, len(element["rotation"]))
                self.assertEqual(set(FACE_ORDER), set(element["faces"]))

    def test_bbmodel_uuids_are_unique_stable_and_hierarchy_has_one_root(self):
        first = build.bbmodel_document()
        second = build.bbmodel_document()
        first_uuids = [item["uuid"] for item in first["groups"] + first["elements"]]
        second_uuids = [item["uuid"] for item in second["groups"] + second["elements"]]
        self.assertEqual(len(first_uuids), len(set(first_uuids)))
        self.assertEqual(first_uuids, second_uuids)
        self.assertEqual(1, len(first["outliner"]))
        self.assertEqual(build.stable_uuid("bone", "root"), first["outliner"][0]["uuid"])

        groups = {group["uuid"]: group for group in first["groups"]}
        elements = {element["uuid"]: element for element in first["elements"]}
        visited_groups = []
        visited_elements = []

        def visit(node):
            visited_groups.append(node["uuid"])
            group = groups[node["uuid"]]
            expected_elements = [
                element["uuid"] for element in first["elements"] if element["bone"] == group["name"]
            ]
            self.assertEqual(expected_elements, node["children"][: len(expected_elements)])
            for child in node["children"]:
                if isinstance(child, str):
                    self.assertIn(child, elements)
                    visited_elements.append(child)
                else:
                    visit(child)

        visit(first["outliner"][0])
        self.assertEqual(set(groups), set(visited_groups))
        self.assertEqual(set(elements), set(visited_elements))

    def test_geometry_and_bbmodel_names_and_uvs_correspond(self):
        geometry = build.geometry_document()["minecraft:geometry"][0]
        bbmodel = build.bbmodel_document()
        geo_bones = {bone["name"]: bone for bone in geometry["bones"]}
        self.assertEqual(list(geo_bones), [group["name"] for group in bbmodel["groups"]])
        for group in bbmodel["groups"]:
            self.assertEqual(geo_bones[group["name"]]["pivot"], group["origin"])
        geo_cubes = {
            cube["name"]: (bone["name"], cube)
            for bone in geometry["bones"]
            for cube in bone.get("cubes", [])
        }
        self.assertEqual(list(geo_cubes), [element["name"] for element in bbmodel["elements"]])
        for element in bbmodel["elements"]:
            bone_name, cube = geo_cubes[element["name"]]
            self.assertEqual(bone_name, element["bone"])
            for face_name in FACE_ORDER:
                geo_face = cube["uv"][face_name]
                u, v = geo_face["uv"]
                width, height = geo_face["uv_size"]
                self.assertEqual([u, v, u + width, v + height], element["faces"][face_name]["uv"])

    def test_documents_are_deterministic(self):
        self.assertEqual(build.geometry_document(), build.geometry_document())
        self.assertEqual(build.bbmodel_document(), build.bbmodel_document())

if __name__ == "__main__":
    unittest.main()
