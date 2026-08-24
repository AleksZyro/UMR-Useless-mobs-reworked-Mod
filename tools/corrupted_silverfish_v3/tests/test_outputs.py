import base64
import io
import json
import math
import os
from pathlib import Path
import sys
import tempfile
import unittest
from unittest import mock
from uuid import UUID, uuid5

from PIL import Image

from tools.corrupted_silverfish_v3 import build
try:
    from tools.corrupted_silverfish_v3 import paint
except ImportError:
    paint = None
from tools.corrupted_silverfish_v3.spec import BONES, CUBES, Cube, cube_pivot

ROOT = Path(__file__).resolve().parents[3]
EXPORT = ROOT / "Modelle" / "Exports" / "corrupted_silverfish_v3"
FACE_ORDER = ("north", "east", "south", "west", "up", "down")


class PaintedTextureContract(unittest.TestCase):
    def setUp(self):
        self.assertIsNotNone(paint, "tools.corrupted_silverfish_v3.paint is required")
        self.uvs = build._pack_uvs()
        self.cubes = {cube.name: cube for cube in CUBES}

    def face_pixels(self, image, cube_name, face):
        u, v, width, height = self.uvs[(cube_name, face)]
        return [
            image.getpixel((x, y))
            for y in range(v, v + height)
            for x in range(u, u + width)
        ]

    def transaction_targets(self, directory):
        root = Path(directory)
        targets = (
            root / "main" / "corrupted_silverfish.png",
            root / "glow" / "corrupted_silverfish_glowmask.png",
            root / "review" / "texture_atlas_preview.png",
            root / "model" / "Corrupted Silverfish v3.bbmodel",
        )
        for index, target in enumerate(targets):
            target.parent.mkdir(parents=True)
            target.write_bytes(f"original-{index}".encode("ascii"))
        return targets

    def assert_transaction_restored(self, targets):
        for index, target in enumerate(targets):
            self.assertEqual(f"original-{index}".encode("ascii"), target.read_bytes())
            self.assertEqual([target], list(target.parent.iterdir()))

    def test_committed_pngs_are_rgba_atlas_images_without_pure_green(self):
        for path in (paint.MAIN_TEXTURE_PATH, paint.GLOWMASK_PATH):
            with self.subTest(path=path):
                self.assertTrue(path.is_file())
                with Image.open(path) as image:
                    self.assertEqual("RGBA", image.mode)
                    self.assertEqual((256, 256), image.size)
                    self.assertNotIn(
                        (0, 255, 0),
                        {pixel[:3] for pixel in image.getdata() if pixel[3]},
                    )

    def test_main_fills_every_uv_island_and_keeps_gutters_transparent(self):
        main, _glow = paint.paint_images()
        inside = set()
        face_area = 0
        for (cube_name, face), (u, v, width, height) in self.uvs.items():
            pixels = self.face_pixels(main, cube_name, face)
            self.assertTrue(any(pixel[3] for pixel in pixels), f"empty {cube_name}/{face}")
            face_area += width * height
            inside.update(
                (x, y)
                for y in range(v, v + height)
                for x in range(u, u + width)
            )
        opaque = {
            (x, y)
            for y in range(256)
            for x in range(256)
            if main.getpixel((x, y))[3]
        }
        self.assertEqual(18918, face_area)
        self.assertGreaterEqual(len(opaque), 8000)
        self.assertTrue(opaque <= inside)

    def test_palette_and_alpha_are_exact_without_antialias_colours(self):
        main, glow = paint.paint_images()
        specified_palette = {
            "seam": (22, 18, 28, 255),
            "underside": (38, 34, 46, 255),
            "steel_dark": (76, 82, 92, 255),
            "steel": (126, 136, 147, 255),
            "steel_light": (188, 198, 207, 255),
            "steel_highlight": (224, 230, 235, 255),
            "corruption_root": (45, 10, 43, 255),
            "corruption_dark": (91, 15, 58, 255),
            "crimson": (181, 31, 82, 255),
            "magenta": (234, 55, 112, 255),
            "violet": (82, 37, 104, 255),
            "eye": (86, 190, 255, 255),
        }
        self.assertEqual(specified_palette, dict(paint.PALETTE))
        allowed_main = set(specified_palette.values())
        allowed_glow = {
            (86, 190, 255, 255),
            (234, 55, 112, 255),
        }
        self.assertTrue({pixel for pixel in main.getdata() if pixel[3]} <= allowed_main)
        self.assertTrue({pixel for pixel in glow.getdata() if pixel[3]} <= allowed_glow)
        self.assertEqual({0, 255}, {pixel[3] for pixel in main.getdata()})
        self.assertTrue({pixel[3] for pixel in glow.getdata()} <= {0, 255})

    def test_glow_is_small_main_aligned_and_limited_to_eye_or_crystal_faces(self):
        main, glow = paint.paint_images()
        glow_positions = {
            (x, y)
            for y in range(256)
            for x in range(256)
            if glow.getpixel((x, y))[3]
        }
        self.assertGreaterEqual(len(glow_positions), 20)
        self.assertLessEqual(len(glow_positions), 800)
        self.assertTrue(all(main.getpixel(position)[3] for position in glow_positions))

        permitted = set()
        for (cube_name, _face), (u, v, width, height) in self.uvs.items():
            if self.cubes[cube_name].material in {"eye", "crystal"}:
                permitted.update(
                    (x, y)
                    for y in range(v, v + height)
                    for x in range(u, u + width)
                )
        self.assertTrue(glow_positions <= permitted)

    def test_material_faces_have_expected_semantic_colours_and_shading(self):
        main, _glow = paint.paint_images()

        def mean_luma(material, face):
            pixels = [
                pixel
                for cube in CUBES
                if cube.material == material
                for pixel in self.face_pixels(main, cube.name, face)
                if pixel[3]
            ]
            return sum(sum(pixel[:3]) for pixel in pixels) / len(pixels)

        for material in ("armor", "armor_dark"):
            self.assertGreater(mean_luma(material, "up"), mean_luma(material, "down"))
        self.assertLess(mean_luma("underside", "up"), mean_luma("armor_dark", "up"))

        crystal_colours = {
            pixel
            for cube in CUBES
            if cube.material == "crystal"
            for face in FACE_ORDER
            for pixel in self.face_pixels(main, cube.name, face)
            if pixel[3]
        }
        self.assertIn(paint.PALETTE["corruption_root"], crystal_colours)
        self.assertTrue(
            {paint.PALETTE["crimson"], paint.PALETTE["magenta"]} <= crystal_colours
        )
        eye_colours = {
            pixel
            for cube in CUBES
            if cube.material == "eye"
            for face in FACE_ORDER
            for pixel in self.face_pixels(main, cube.name, face)
            if pixel[3]
        }
        self.assertIn(paint.PALETTE["eye"], eye_colours)

        left = next(cube for cube in CUBES if cube.name == "forehead_left")
        right = next(cube for cube in CUBES if cube.name == "forehead_right")
        left_bytes = bytes(
            sum((list(pixel) for pixel in self.face_pixels(main, left.name, "north")), [])
        )
        right_bytes = bytes(
            sum((list(pixel) for pixel in self.face_pixels(main, right.name, "north")), [])
        )
        self.assertNotEqual(left_bytes, right_bytes)

    def test_painting_is_deterministic(self):
        first = paint.paint_images()
        second = paint.paint_images()
        self.assertEqual(first[0].tobytes(), second[0].tobytes())
        self.assertEqual(first[1].tobytes(), second[1].tobytes())

    def test_bbmodel_embeds_exactly_the_main_texture(self):
        main_bytes = paint.MAIN_TEXTURE_PATH.read_bytes()
        source = "data:image/png;base64," + base64.b64encode(main_bytes).decode("ascii")
        document = build.bbmodel_document(source)
        self.assertEqual(1, len(document["textures"]))
        texture = document["textures"][0]
        self.assertEqual("", texture["path"])
        self.assertEqual("corrupted_silverfish.png", texture["name"])
        self.assertEqual("entity", texture["folder"])
        self.assertEqual("usless_mobs", texture["namespace"])
        self.assertEqual("0", texture["id"])
        self.assertEqual("bitmap", texture["mode"])
        self.assertTrue(texture["saved"])
        self.assertEqual("default", texture["render_mode"])
        self.assertEqual(source, texture["source"])
        self.assertEqual(5, UUID(texture["uuid"]).version)
        self.assertEqual(main_bytes, base64.b64decode(texture["source"].split(",", 1)[1]))
        self.assertEqual(5, len(document["animations"]))
        self.assertEqual((32, 112), (len(document["groups"]), len(document["elements"])))
        self.assertTrue(
            all(
                face["texture"] == 0
                for element in document["elements"]
                for face in element["faces"].values()
            )
        )

    def test_committed_outputs_match_fresh_paint_and_embedded_document(self):
        main, glow = paint.paint_images()
        for image, path in (
            (main, paint.MAIN_TEXTURE_PATH),
            (glow, paint.GLOWMASK_PATH),
            (paint._preview(main), paint.PREVIEW_PATH),
        ):
            with Image.open(path) as committed:
                committed.load()
                expected = image.convert("RGBA")
                actual = committed.convert("RGBA")
                self.assertEqual(expected.size, actual.size)
                self.assertEqual(expected.tobytes(), actual.tobytes())
        source = "data:image/png;base64," + base64.b64encode(
            paint.MAIN_TEXTURE_PATH.read_bytes()
        ).decode("ascii")
        expected = (
            json.dumps(build.bbmodel_document(source), ensure_ascii=False, indent=2) + "\n"
        ).encode("utf-8")
        self.assertEqual(expected, build.BBMODEL_PATH.read_bytes())

    def test_write_textures_rolls_back_all_targets_on_fourth_write_failure(self):
        real_fdopen = os.fdopen
        calls = 0

        def fail_fourth_write(*args, **kwargs):
            nonlocal calls
            calls += 1
            if calls == 4:
                raise OSError("simulated fourth write failure")
            return real_fdopen(*args, **kwargs)

        with tempfile.TemporaryDirectory() as directory:
            targets = self.transaction_targets(directory)
            with mock.patch.multiple(
                paint,
                MAIN_TEXTURE_PATH=targets[0],
                GLOWMASK_PATH=targets[1],
                PREVIEW_PATH=targets[2],
            ), mock.patch.object(
                build, "BBMODEL_PATH", targets[3]
            ), mock.patch(
                "os.fdopen", side_effect=fail_fourth_write
            ), self.assertRaisesRegex(
                OSError, "simulated fourth write failure"
            ):
                paint.write_textures()

            self.assertEqual(4, calls)
            self.assert_transaction_restored(targets)

    def test_write_textures_rolls_back_all_targets_on_fourth_replace_failure(self):
        real_replace = os.replace
        calls = 0

        def fail_fourth_replace(source, target):
            nonlocal calls
            calls += 1
            if calls == 4:
                raise OSError("simulated fourth replace failure")
            return real_replace(source, target)

        with tempfile.TemporaryDirectory() as directory:
            targets = self.transaction_targets(directory)
            with mock.patch.multiple(
                paint,
                MAIN_TEXTURE_PATH=targets[0],
                GLOWMASK_PATH=targets[1],
                PREVIEW_PATH=targets[2],
            ), mock.patch.object(
                build, "BBMODEL_PATH", targets[3]
            ), mock.patch(
                "os.replace", side_effect=fail_fourth_replace
            ), self.assertRaisesRegex(
                OSError, "simulated fourth replace failure"
            ):
                paint.write_textures()

            self.assertGreaterEqual(calls, 4)
            self.assert_transaction_restored(targets)

    def test_combined_publish_and_rollback_failure_retains_unrestored_backup(self):
        real_replace = os.replace
        calls = 0

        def fail_publish_then_glow_rollback(source, target):
            nonlocal calls
            calls += 1
            if calls == 4:
                raise OSError("simulated bbmodel publish failure")
            if calls == 6:
                raise OSError("simulated glow rollback failure")
            return real_replace(source, target)

        with tempfile.TemporaryDirectory() as directory:
            targets = self.transaction_targets(directory)
            with mock.patch.multiple(
                paint,
                MAIN_TEXTURE_PATH=targets[0],
                GLOWMASK_PATH=targets[1],
                PREVIEW_PATH=targets[2],
            ), mock.patch.object(
                build, "BBMODEL_PATH", targets[3]
            ), mock.patch(
                "os.replace", side_effect=fail_publish_then_glow_rollback
            ), self.assertRaisesRegex(
                RuntimeError, "texture transaction rollback failed"
            ) as raised:
                paint.write_textures()

            self.assertEqual(7, calls)
            self.assertEqual(b"original-0", targets[0].read_bytes())
            self.assertNotEqual(b"original-1", targets[1].read_bytes())
            self.assertEqual(b"original-2", targets[2].read_bytes())
            self.assertEqual(b"original-3", targets[3].read_bytes())

            glow_backups = [
                path for path in targets[1].parent.iterdir() if path != targets[1]
            ]
            self.assertEqual(1, len(glow_backups))
            retained_backup = glow_backups[0]
            self.assertIn(".backup.", retained_backup.name)
            self.assertEqual(b"original-1", retained_backup.read_bytes())
            self.assertIn(str(targets[1]), str(raised.exception))
            self.assertIn(str(retained_backup), str(raised.exception))
            self.assertIn("simulated bbmodel publish failure", str(raised.exception))
            self.assertIn("simulated glow rollback failure", str(raised.exception))

            rollback_cause = raised.exception.__cause__
            self.assertIsInstance(rollback_cause, OSError)
            self.assertEqual("simulated glow rollback failure", str(rollback_cause))
            publish_cause = rollback_cause.__cause__
            self.assertIsInstance(publish_cause, OSError)
            self.assertEqual(
                "simulated bbmodel publish failure",
                str(publish_cause),
            )

            for target in (targets[0], targets[2], targets[3]):
                self.assertEqual([target], list(target.parent.iterdir()))
            probe = retained_backup.with_suffix(".handle-check")
            os.replace(retained_backup, probe)
            os.replace(probe, retained_backup)
            self.assertEqual(b"original-1", retained_backup.read_bytes())

    def test_write_textures_uses_unique_temps_and_backups_in_target_directories(self):
        created = []
        real_mkstemp = tempfile.mkstemp

        def tracked_mkstemp(*args, **kwargs):
            file_descriptor, name = real_mkstemp(*args, **kwargs)
            created.append(Path(name))
            return file_descriptor, name

        with tempfile.TemporaryDirectory() as directory:
            targets = self.transaction_targets(directory)
            with mock.patch.multiple(
                paint,
                MAIN_TEXTURE_PATH=targets[0],
                GLOWMASK_PATH=targets[1],
                PREVIEW_PATH=targets[2],
            ), mock.patch.object(
                build, "BBMODEL_PATH", targets[3]
            ), mock.patch(
                "tempfile.mkstemp", side_effect=tracked_mkstemp
            ):
                paint.write_textures()

            self.assertEqual(8, len(created))
            self.assertEqual(8, len(set(created)))
            self.assertEqual(
                sorted([target.parent for target in targets] * 2),
                sorted(path.parent for path in created),
            )
            self.assertTrue(all(not path.exists() for path in created))
            for target in targets:
                self.assertEqual([target], list(target.parent.iterdir()))

    def test_atomic_png_writer_cleans_temp_and_preserves_target_on_save_failure(self):
        with tempfile.TemporaryDirectory() as directory:
            target = Path(directory) / "texture.png"
            target.write_bytes(b"original")
            with mock.patch.object(
                Image.Image, "save", side_effect=OSError("simulated save failure")
            ), self.assertRaisesRegex(OSError, "simulated save failure"):
                paint._atomic_png_write(target, Image.new("RGBA", (1, 1)))

            self.assertEqual(b"original", target.read_bytes())
            self.assertEqual([target], list(target.parent.iterdir()))

    def test_atomic_png_writer_cleans_temp_and_preserves_target_on_replace_failure(self):
        with tempfile.TemporaryDirectory() as directory:
            target = Path(directory) / "texture.png"
            target.write_bytes(b"original")
            with mock.patch(
                "os.replace", side_effect=OSError("simulated replace failure")
            ), self.assertRaisesRegex(OSError, "simulated replace failure"):
                paint._atomic_png_write(target, Image.new("RGBA", (1, 1)))

            self.assertEqual(b"original", target.read_bytes())
            self.assertEqual([target], list(target.parent.iterdir()))

    def test_main_without_explicit_argv_parses_sys_argv_before_writing(self):
        outputs = (
            Path("main.png"),
            Path("glow.png"),
            Path("preview.png"),
            Path("model.bbmodel"),
        )
        with mock.patch.object(
            sys, "argv", ["paint", "--unexpected"]
        ), mock.patch.object(
            paint, "write_textures", return_value=outputs
        ) as writer, mock.patch(
            "sys.stderr", new_callable=io.StringIO
        ), self.assertRaises(SystemExit) as raised:
            paint.main()

        self.assertEqual(2, raised.exception.code)
        writer.assert_not_called()

class GeneratedOutputContract(unittest.TestCase):
    def assert_finite_vec3(self, value):
        self.assertEqual(3, len(value))
        self.assertTrue(
            all(
                isinstance(component, (int, float))
                and not isinstance(component, bool)
                and math.isfinite(component)
                for component in value
            )
        )

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
        spec_cubes_by_bone = {
            bone.name: [cube for cube in CUBES if cube.bone == bone.name]
            for bone in BONES
        }
        for bone in bones:
            generated_cubes = bone.get("cubes", [])
            spec_cubes = spec_cubes_by_bone[bone["name"]]
            self.assertEqual(len(spec_cubes), len(generated_cubes))
            for cube, spec_cube in zip(generated_cubes, spec_cubes):
                with self.subTest(cube=spec_cube.name):
                    self.assertEqual(spec_cube.name, cube.get("name"))
                    self.assert_finite_vec3(cube["origin"])
                    self.assert_finite_vec3(cube["size"])
                    self.assertEqual(list(spec_cube.origin), cube["origin"])
                    self.assertEqual(list(spec_cube.size), cube["size"])
                    self.assertTrue(all(value > 0 for value in cube["size"]))
                    if spec_cube.rotation != (0.0, 0.0, 0.0):
                        self.assert_finite_vec3(cube["rotation"])
                        self.assert_finite_vec3(cube["pivot"])
                        self.assertEqual(list(spec_cube.rotation), cube["rotation"])
                        self.assertEqual(list(cube_pivot(spec_cube)), cube["pivot"])
                    else:
                        self.assertNotIn("rotation", cube)
                        self.assertNotIn("pivot", cube)
                    self.assertEqual(set(FACE_ORDER), set(cube["uv"]))
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

    def test_shelf_packer_breaks_equal_height_ties_by_lexical_face_name(self):
        cube = Cube(
            "tie_cube",
            "root",
            (0.0, 0.0, 0.0),
            (1.0, 1.0, 1.0),
            "test",
            "test",
        )
        packed = build._pack_uvs((cube,))
        packed_order = sorted(
            FACE_ORDER,
            key=lambda face: (packed[(cube.name, face)][1], packed[(cube.name, face)][0]),
        )
        self.assertEqual(sorted(FACE_ORDER), packed_order)

    def test_shelf_packer_uses_exact_two_pixel_gutter_and_allows_atlas_edge(self):
        packed = build._pack_islands(
            (
                ("first", "north", 10, 10),
                ("second", "north", 10, 10),
            )
        )
        first = packed[("first", "north")]
        second = packed[("second", "north")]
        self.assertEqual(2, second[0] - (first[0] + first[2]))

        edge = build._pack_islands((("edge", "north", 256, 1),))
        u, _v, width, _height = edge[("edge", "north")]
        self.assertEqual(256, u + width)

    def test_packer_rejects_invalid_cube_dimensions_with_face_context(self):
        invalid_dimensions = (
            (float("nan"), 1.0, 1.0),
            (-1.0, 1.0, 1.0),
            (0.0, 1.0, 1.0),
        )
        for index, size in enumerate(invalid_dimensions):
            cube = Cube(
                f"invalid_{index}",
                "root",
                (0.0, 0.0, 0.0),
                size,
                "test",
                "test",
            )
            with self.subTest(cube=cube.name), self.assertRaisesRegex(
                ValueError,
                rf"{cube.name}/north.*invalid dimensions",
            ):
                build._pack_uvs((cube,))

    def test_packer_rejects_oversize_and_atlas_overflow_with_context(self):
        with self.assertRaisesRegex(ValueError, r"oversize/north.*larger than 256x256"):
            build._pack_islands((("oversize", "north", 257, 1),))
        with self.assertRaisesRegex(ValueError, r"overflow/north.*overflows.*atlas"):
            build._pack_islands(
                (
                    ("full", "north", 256, 256),
                    ("overflow", "north", 1, 1),
                )
            )

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
        self.assertEqual(5, len(document["animations"]))
        for element in document["elements"]:
            with self.subTest(element=element["name"]):
                self.assertEqual("cube", element["type"])
                self.assert_finite_vec3(element["from"])
                self.assert_finite_vec3(element["to"])
                self.assert_finite_vec3(element["origin"])
                self.assert_finite_vec3(element["rotation"])
                self.assertEqual(set(FACE_ORDER), set(element["faces"]))

    def test_bbmodel_uuids_are_unique_stable_and_hierarchy_has_one_root(self):
        first = build.bbmodel_document()
        second = build.bbmodel_document()
        first_uuids = [item["uuid"] for item in first["groups"] + first["elements"]]
        second_uuids = [item["uuid"] for item in second["groups"] + second["elements"]]
        self.assertEqual(len(first_uuids), len(set(first_uuids)))
        self.assertTrue(all(UUID(item_uuid).version == 5 for item_uuid in first_uuids))
        self.assertEqual(first_uuids, second_uuids)
        self.assertEqual(1, len(first["outliner"]))
        expected_root_uuid = str(uuid5(build.UUID_NAMESPACE, "group:root"))
        expected_element_uuid = str(uuid5(build.UUID_NAMESPACE, "element:head_core"))
        self.assertEqual(expected_root_uuid, first["outliner"][0]["uuid"])
        self.assertEqual(
            expected_root_uuid,
            next(group["uuid"] for group in first["groups"] if group["name"] == "root"),
        )
        self.assertEqual(
            expected_element_uuid,
            next(
                element["uuid"]
                for element in first["elements"]
                if element["name"] == "head_core"
            ),
        )

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
            spec_cube = next(cube for cube in CUBES if cube.name == element["name"])
            bone_name, cube = geo_cubes[spec_cube.name]
            self.assertEqual(bone_name, element["bone"])
            self.assertEqual(list(spec_cube.origin), element["from"])
            self.assertEqual(
                [spec_cube.origin[axis] + spec_cube.size[axis] for axis in range(3)],
                element["to"],
            )
            self.assertEqual(list(cube_pivot(spec_cube)), element["origin"])
            self.assertEqual(list(spec_cube.rotation), element["rotation"])
            for face_name in FACE_ORDER:
                geo_face = cube["uv"][face_name]
                u, v = geo_face["uv"]
                width, height = geo_face["uv_size"]
                self.assertEqual([u, v, u + width, v + height], element["faces"][face_name]["uv"])

    def test_documents_are_deterministic(self):
        self.assertEqual(build.geometry_document(), build.geometry_document())
        self.assertEqual(build.bbmodel_document(), build.bbmodel_document())

    def test_geometry_only_build_preserves_existing_textured_bbmodel(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            geometry_path = root / "geo" / "model.geo.json"
            bbmodel_path = root / "model" / "model.bbmodel"
            bbmodel_path.parent.mkdir(parents=True)
            original = b"textured bbmodel bytes\n"
            bbmodel_path.write_bytes(original)
            with mock.patch.object(
                build, "GEOMETRY_PATH", geometry_path
            ), mock.patch.object(build, "BBMODEL_PATH", bbmodel_path):
                written = build.build_geometry()

            self.assertEqual(geometry_path, written)
            self.assertEqual(original, bbmodel_path.read_bytes())

    def test_atomic_writer_uses_unique_temporary_names(self):
        created = []
        real_mkstemp = tempfile.mkstemp

        def tracked_mkstemp(*args, **kwargs):
            file_descriptor, name = real_mkstemp(*args, **kwargs)
            created.append(Path(name))
            return file_descriptor, name

        with tempfile.TemporaryDirectory() as directory, mock.patch(
            "tempfile.mkstemp",
            side_effect=tracked_mkstemp,
        ):
            target = Path(directory) / "output.json"
            build._atomic_json_write(target, {"build": 1})
            build._atomic_json_write(target, {"build": 2})

        self.assertEqual(2, len(created))
        self.assertEqual(2, len(set(created)))
        self.assertTrue(all(path.parent == target.parent for path in created))
        self.assertTrue(all(not path.exists() for path in created))

    def test_atomic_writer_preserves_target_and_cleans_temp_on_replace_failure(self):
        with tempfile.TemporaryDirectory() as directory:
            target = Path(directory) / "output.json"
            original = b"original bytes\n"
            target.write_bytes(original)
            with mock.patch(
                "os.replace",
                side_effect=OSError("simulated replace failure"),
            ), self.assertRaisesRegex(OSError, "simulated replace failure"):
                build._atomic_json_write(target, {"replacement": True})

            self.assertEqual(original, target.read_bytes())
            self.assertEqual([target], list(target.parent.iterdir()))

    def test_atomic_writer_preserves_target_and_cleans_temp_on_write_failure(self):
        with tempfile.TemporaryDirectory() as directory:
            target = Path(directory) / "output.json"
            original = b"original bytes\n"
            target.write_bytes(original)
            with mock.patch(
                "os.fdopen",
                side_effect=OSError("simulated write failure"),
            ), self.assertRaisesRegex(OSError, "simulated write failure"):
                build._atomic_json_write(target, {"replacement": True})

            self.assertEqual(original, target.read_bytes())
            self.assertEqual([target], list(target.parent.iterdir()))

    def test_atomic_writer_reports_cleanup_failure_after_successful_publish(self):
        real_unlink = Path.unlink

        def fail_cleanup(path, *args, **kwargs):
            if path.suffix == ".tmp":
                raise OSError("simulated atomic cleanup failure")
            return real_unlink(path, *args, **kwargs)

        with tempfile.TemporaryDirectory() as directory:
            target = Path(directory) / "output.json"
            target.write_bytes(b"original")
            with mock.patch.object(Path, "unlink", autospec=True, side_effect=fail_cleanup), self.assertRaisesRegex(RuntimeError, "published, cleanup incomplete"):
                build._atomic_json_write(target, {"replacement": True})
            self.assertEqual(
                (json.dumps({"replacement": True}, ensure_ascii=False, indent=2) + "\n").encode("utf-8"),
                target.read_bytes(),
            )

    def test_atomic_writer_keeps_replace_error_primary_when_cleanup_fails(self):
        real_unlink = Path.unlink

        def fail_cleanup(path, *args, **kwargs):
            if path.suffix == ".tmp":
                raise OSError("simulated atomic cleanup failure")
            return real_unlink(path, *args, **kwargs)

        with tempfile.TemporaryDirectory() as directory:
            target = Path(directory) / "output.json"
            target.write_bytes(b"original")
            with mock.patch("os.replace", side_effect=OSError("primary atomic replace failure")), mock.patch.object(Path, "unlink", autospec=True, side_effect=fail_cleanup), self.assertRaisesRegex(OSError, "primary atomic replace failure") as raised:
                build._atomic_json_write(target, {"replacement": True})
            self.assertEqual(b"original", target.read_bytes())
            self.assertIn("cleanup incomplete", str(raised.exception))
            temporary = next(path for path in target.parent.iterdir() if path != target)
            probe = temporary.with_suffix(".handle-check")
            os.replace(temporary, probe)
            os.replace(probe, temporary)

    def test_committed_outputs_are_exact_deterministic_document_bytes(self):
        expected_geometry = (
            json.dumps(build.geometry_document(), ensure_ascii=False, indent=2) + "\n"
        ).encode("utf-8")
        main_texture = EXPORT / "textures" / "entity" / "corrupted_silverfish.png"
        texture_source = "data:image/png;base64," + base64.b64encode(
            main_texture.read_bytes()
        ).decode("ascii")
        expected_bbmodel = (
            json.dumps(
                build.bbmodel_document(texture_source), ensure_ascii=False, indent=2
            )
            + "\n"
        ).encode("utf-8")
        self.assertEqual(expected_geometry, build.GEOMETRY_PATH.read_bytes())
        self.assertEqual(expected_bbmodel, build.BBMODEL_PATH.read_bytes())

if __name__ == "__main__":
    unittest.main()
