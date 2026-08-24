import importlib.util
import os
import tempfile
import unittest
from collections import deque
from pathlib import Path
from unittest import mock

from PIL import Image


REPO_ROOT = Path(__file__).resolve().parents[3]
GENERATOR_PATH = REPO_ROOT / "tools/armor_graphics/build_all_armor_skins.py"


def load_generator():
    spec = importlib.util.spec_from_file_location("all_armor_skins", GENERATOR_PATH)
    if spec is None or spec.loader is None:
        raise AssertionError(f"cannot load {GENERATOR_PATH}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def largest_same_colour_component(image: Image.Image) -> int:
    pixels = image.load()
    visited = set()
    largest = 0
    for y in range(image.height):
        for x in range(image.width):
            if (x, y) in visited or pixels[x, y][3] == 0:
                continue
            colour = pixels[x, y]
            queue = deque([(x, y)])
            visited.add((x, y))
            size = 0
            while queue:
                px, py = queue.popleft()
                size += 1
                for nx, ny in ((px - 1, py), (px + 1, py), (px, py - 1), (px, py + 1)):
                    if not (0 <= nx < image.width and 0 <= ny < image.height):
                        continue
                    if (nx, ny) in visited or pixels[nx, ny] != colour:
                        continue
                    visited.add((nx, ny))
                    queue.append((nx, ny))
            largest = max(largest, size)
    return largest


class AllArmorSkinContract(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.generator = load_generator()

    def test_output_manifest_is_exact(self):
        expected_worn = {
            "true_void_layer_1.png",
            "true_void_layer_2.png",
            "true_void_chestplate_layer_1.png",
            "true_celestial_layer_1.png",
            "true_celestial_layer_2.png",
            "true_living_layer_1.png",
            "true_living_layer_2.png",
            "true_balance_layer_1.png",
            "true_balance_layer_2.png",
            "corrupted_crystal_layer_2.png",
            "schleimreaktor_layer_1.png",
        }
        expected_items = {
            f"{family}_{slot}.png"
            for family in ("true_void", "true_celestial", "true_living", "armor_of_balance")
            for slot in ("helmet", "chestplate", "leggings", "boots")
        } | {"corrupted_crystal_leggings.png", "schleimreaktor_brustpanzer.png"}
        paths = {Path(spec.relative_path) for spec in self.generator.OUTPUT_SPECS}
        self.assertEqual(expected_worn, {path.name for path in paths if "models/armor" in path.as_posix()})
        self.assertEqual(expected_items, {path.name for path in paths if "textures/item" in path.as_posix()})
        self.assertEqual(29, len(paths))

    def test_palettes_define_material_depth(self):
        self.assertEqual(
            {"void", "celestial", "living", "balance", "corrupted", "reactor"},
            set(self.generator.FAMILY_PALETTES),
        )
        for family, palette in self.generator.FAMILY_PALETTES.items():
            with self.subTest(family=family):
                self.assertGreaterEqual(len(set(palette)), 7)
                self.assertTrue(all(len(colour) == 4 and colour[3] == 255 for colour in palette))

    def test_documents_are_deterministic_detailed_rgba(self):
        first = self.generator.build_documents(REPO_ROOT)
        second = self.generator.build_documents(REPO_ROOT)
        self.assertEqual(set(first), set(second))
        specs = {spec.relative_path: spec for spec in self.generator.OUTPUT_SPECS}
        for relative_path, image in first.items():
            with self.subTest(path=relative_path):
                self.assertEqual("RGBA", image.mode)
                self.assertEqual(specs[relative_path].size, image.size)
                self.assertEqual(
                    self.generator.png_bytes(image),
                    self.generator.png_bytes(second[relative_path]),
                )
                opaque = [pixel for pixel in image.getdata() if pixel[3]]
                self.assertTrue(opaque)
                self.assertGreaterEqual(len(set(opaque)), 5)
                limit = max(34, (image.width * image.height) // 8)
                self.assertLessEqual(largest_same_colour_component(image), limit)

    def test_family_accent_colours_are_visible(self):
        documents = self.generator.build_documents(REPO_ROOT)
        for spec in self.generator.OUTPUT_SPECS:
            with self.subTest(path=spec.relative_path):
                pixels = set(documents[spec.relative_path].getdata())
                palette = self.generator.FAMILY_PALETTES[spec.family]
                self.assertIn(palette[-1], pixels, "bright family core must be visible")
                self.assertGreaterEqual(len(pixels.intersection(palette)), 5)

    def test_material_clusters_do_not_form_a_repeating_diagonal_checker(self):
        documents = self.generator.build_documents(REPO_ROOT)
        for spec in self.generator.OUTPUT_SPECS:
            if spec.relative_path.endswith(("/true_void_chestplate.png", "/true_void_chestplate_layer_1.png")):
                continue
            image = documents[spec.relative_path]
            scale = max(1, image.width // 64)
            shift = 2 * scale
            equal = 0
            compared = 0
            for y in range(shift, image.height):
                for x in range(image.width - shift):
                    first = image.getpixel((x, y))
                    second = image.getpixel((x + shift, y - shift))
                    if first[3] and second[3]:
                        compared += 1
                        equal += first == second
            if compared >= 20:
                with self.subTest(path=spec.relative_path):
                    self.assertLess(equal / compared, 0.50)

    def test_committed_outputs_match_generator(self):
        documents = self.generator.build_documents(REPO_ROOT)
        for relative_path, image in documents.items():
            with self.subTest(path=relative_path):
                with Image.open(REPO_ROOT / relative_path) as committed:
                    committed.load()
                    expected = image.convert("RGBA")
                    actual = committed.convert("RGBA")
                    self.assertEqual(expected.size, actual.size)
                    self.assertEqual(expected.tobytes(), actual.tobytes())

    def test_multi_file_publish_rolls_back_every_target(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            targets = {"a.png": b"OLD-A", "nested/b.png": b"OLD-B"}
            for relative_path, payload in targets.items():
                path = root / relative_path
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(payload)
            documents = {
                relative_path: Image.new("RGBA", (2, 2), (index * 80, 20, 40, 255))
                for index, relative_path in enumerate(targets, start=1)
            }
            real_replace = os.replace
            publishes = 0

            def fail_second_publish(source, destination):
                nonlocal publishes
                if str(source).endswith(".candidate"):
                    publishes += 1
                    if publishes == 2:
                        raise OSError("injected second publish failure")
                return real_replace(source, destination)

            with mock.patch.object(self.generator.os, "replace", side_effect=fail_second_publish):
                with self.assertRaisesRegex(OSError, "injected second publish failure"):
                    self.generator.publish_documents(root, documents)

            self.assertEqual(targets, {path: (root / path).read_bytes() for path in targets})
            self.assertEqual([], list(root.rglob("*.candidate")))
            self.assertEqual([], list(root.rglob("*.backup")))

    def test_contact_sheet_contains_all_outputs(self):
        documents = self.generator.build_documents(REPO_ROOT)
        entries = self.generator.contact_entries(documents, REPO_ROOT)
        self.assertEqual(24, len(entries))
        self.assertEqual(
            {"void", "celestial", "living", "balance", "corrupted", "reactor"},
            {entry[0] for entry in entries if entry[1] == "full set"},
        )
        self.assertFalse(
            any("layer_" in filename for _family, _slot, filename, _image in entries),
            "technical worn UV atlases must not be presented as finished graphics",
        )
        sheet = self.generator.build_contact_sheet(documents, REPO_ROOT)
        self.assertEqual("RGBA", sheet.mode)
        self.assertEqual((1200, 1200), sheet.size)
        self.assertGreater(len(set(sheet.getdata())), 30)


if __name__ == "__main__":
    unittest.main()
