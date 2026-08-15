import base64
import importlib.util
import json
from pathlib import Path
import struct
import unittest


ROOT = Path(__file__).resolve().parents[3]
MODULE_PATH = ROOT / "tools" / "corrupted_silverfish_v5" / "runtime_export.py"
SOURCE = (
    ROOT
    / "Modelle"
    / "Exports"
    / "corrupted_silverfish_v5"
    / "blockbench"
    / "Corrupted Silverfish v5 Tripo Animated.bbmodel"
)
RUNTIME_ROOT = ROOT / "src" / "main" / "mobs" / "endermite" / "resources" / "assets" / "usless_mobs"
RENDERER = ROOT / "src" / "main" / "mobs" / "endermite" / "java" / "net" / "mysith" / "client" / "CorruptedSilverfishRenderer.java"
MESH_CLASS = ROOT / "src" / "main" / "mobs" / "endermite" / "java" / "net" / "mysith" / "client" / "CorruptedSilverfishMesh.java"


def load_runtime_export():
    if not MODULE_PATH.is_file():
        raise AssertionError(f"runtime exporter is missing: {MODULE_PATH}")
    spec = importlib.util.spec_from_file_location("runtime_export_under_test", MODULE_PATH)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


class RuntimeExporterPresenceTest(unittest.TestCase):
    def test_runtime_exporter_exists(self):
        self.assertTrue(MODULE_PATH.is_file(), f"runtime exporter is missing: {MODULE_PATH}")


class RuntimeExportContractTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        if not MODULE_PATH.is_file():
            raise unittest.SkipTest(f"runtime exporter not implemented yet: {MODULE_PATH}")
        cls.module = load_runtime_export()
        cls.source_document = json.loads(SOURCE.read_text(encoding="utf-8"))
        cls.bundle = cls.module.build_runtime_bundle(cls.source_document)

    def test_bundle_contains_exact_runtime_files(self):
        self.assertEqual(
            set(self.bundle),
            {
                "geo/corrupted_silverfish.geo.json",
                "animations/corrupted_silverfish.animation.json",
                "textures/entity/corrupted_silverfish.png",
                "meshes/entity/corrupted_silverfish.mesh",
            },
        )

    def test_binary_mesh_preserves_every_triangle_position_and_uv(self):
        decoded = self.module.decode_runtime_mesh(
            self.bundle["meshes/entity/corrupted_silverfish.mesh"]
        )
        expected = {}
        def f32(value):
            return struct.unpack("<f", struct.pack("<f", float(value)))[0]

        for element in self.source_document["elements"]:
            expected_faces = []
            for face in element["faces"].values():
                expected_faces.append(
                    tuple(
                        (
                            tuple(f32(value) for value in element["vertices"][vertex_id]),
                            tuple(f32(value) for value in face["uv"][vertex_id]),
                        )
                        for vertex_id in face["vertices"]
                    )
                )
            expected[element["name"]] = expected_faces

        self.assertEqual(sum(len(faces) for faces in decoded.values()), 101_723)
        self.assertEqual(decoded, expected)

    def test_geo_contains_exact_rig_hierarchy_without_fake_cubes(self):
        geo = json.loads(self.bundle["geo/corrupted_silverfish.geo.json"])
        bones = geo["minecraft:geometry"][0]["bones"]
        source_groups = {group["name"]: group for group in self.source_document["groups"]}

        self.assertEqual({bone["name"] for bone in bones}, set(source_groups))
        for bone in bones:
            self.assertEqual(bone["pivot"], source_groups[bone["name"]]["origin"])
            self.assertNotIn("cubes", bone)

    def test_animation_json_matches_all_embedded_blockbench_channels(self):
        runtime = json.loads(self.bundle["animations/corrupted_silverfish.animation.json"])["animations"]
        source_by_name = {animation["name"]: animation for animation in self.source_document["animations"]}

        self.assertEqual(set(runtime), set(source_by_name))
        self.assertEqual(len(runtime), 5)
        for name, animation in runtime.items():
            source = source_by_name[name]
            self.assertEqual(animation["animation_length"], source["length"])
            self.assertEqual(animation["loop"], source["loop"] == "loop")
            self.assertTrue(animation["bones"])

    def test_texture_is_the_exact_embedded_tripo_texture(self):
        source = self.source_document["textures"][0]["source"]
        expected = base64.b64decode(source.split(";base64,", 1)[1], validate=True)

        self.assertEqual(self.bundle["textures/entity/corrupted_silverfish.png"], expected)
        self.assertTrue(expected.startswith(b"\x89PNG\r\n\x1a\n"))

    def test_bundle_bytes_are_deterministic(self):
        second = self.module.build_runtime_bundle(
            json.loads(SOURCE.read_text(encoding="utf-8"))
        )

        self.assertEqual(self.bundle, second)


class ProductionIntegrationContractTests(unittest.TestCase):
    def test_committed_runtime_assets_match_the_approved_bundle(self):
        module = load_runtime_export()
        bundle = module.build_runtime_bundle(json.loads(SOURCE.read_text(encoding="utf-8")))

        for relative_path, expected in bundle.items():
            target = RUNTIME_ROOT / relative_path
            self.assertTrue(target.is_file(), f"runtime asset missing: {target}")
            self.assertEqual(target.read_bytes(), expected, f"runtime asset is stale: {target}")

    def test_renderer_uses_the_custom_mesh_inside_geckolib_bone_transforms(self):
        self.assertTrue(MESH_CLASS.is_file(), f"custom mesh loader is missing: {MESH_CLASS}")
        renderer = RENDERER.read_text(encoding="utf-8")
        mesh_class = MESH_CLASS.read_text(encoding="utf-8")

        self.assertIn("CorruptedSilverfishMesh", renderer)
        self.assertIn("renderRecursively", renderer)
        self.assertIn("RenderUtils.prepMatrixForBone", renderer)
        self.assertIn("meshes/entity/corrupted_silverfish.mesh", mesh_class)
        self.assertNotIn("Minecraft.getInstance()", mesh_class)


if __name__ == "__main__":
    unittest.main()
