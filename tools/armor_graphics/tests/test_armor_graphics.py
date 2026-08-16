import json
import math
import struct
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[3]
NAMESPACE = "usless_mobs"
RESOURCE_ROOTS = (
    REPO_ROOT / "src/main/resources",
    REPO_ROOT / "src/main/mobs/endermite/resources",
)
SETS = ("true_void", "true_celestial", "true_living", "armor_of_balance")
SLOTS = ("helmet", "chestplate", "leggings", "boots")
ITEMS = tuple(f"{set_name}_{slot}" for set_name in SETS for slot in SLOTS) + (
    "corrupted_crystal_leggings",
)
DISPLAY_CONTEXTS = {
    "gui",
    "ground",
    "fixed",
    "head",
    "thirdperson_righthand",
    "thirdperson_lefthand",
    "firstperson_righthand",
    "firstperson_lefthand",
}
BASELINES = (
    "inventory_before.png",
    "void_before.png",
    "celestial_before.png",
    "living_before.png",
    "balance_before.png",
    "corrupted_leggings_before.png",
)


def resource_path(location: str, kind: str, suffix: str) -> Path:
    namespace, relative = (location.split(":", 1) if ":" in location else ("minecraft", location))
    if namespace != NAMESPACE:
        raise AssertionError(f"unsupported external resource {location!r}")
    candidates = [root / "assets" / namespace / kind / f"{relative}{suffix}" for root in RESOURCE_ROOTS]
    matches = [candidate for candidate in candidates if candidate.is_file()]
    if len(matches) != 1:
        raise AssertionError(f"expected one resource for {location!r}, found {matches}")
    return matches[0]


def merge_model(parent: dict, child: dict) -> dict:
    merged = dict(parent)
    for key, value in child.items():
        if key == "textures":
            merged[key] = {**parent.get(key, {}), **value}
        elif key == "display":
            merged[key] = {**parent.get(key, {}), **value}
        else:
            merged[key] = value
    return merged


def resolve_model(item_id: str) -> dict:
    location = f"{NAMESPACE}:item/{item_id}"
    seen = []
    resolved = {}
    while location:
        if location in seen:
            raise AssertionError(f"model parent cycle: {' -> '.join(seen + [location])}")
        seen.append(location)
        path = resource_path(location, "models", ".json")
        with path.open(encoding="utf-8") as stream:
            model = json.load(stream)
        resolved = merge_model(model, resolved)
        location = model.get("parent")
        if location and ":" not in location:
            raise AssertionError(f"{path}: parent must use an explicit namespace: {location!r}")
    return resolved


def assert_finite_vector(test: unittest.TestCase, vector, length: int, label: str) -> None:
    test.assertIsInstance(vector, list, label)
    test.assertEqual(length, len(vector), label)
    test.assertTrue(
        all(not isinstance(value, bool) and isinstance(value, (int, float)) and math.isfinite(value) for value in vector),
        label,
    )


def png_dimensions(path: Path) -> tuple[int, int]:
    with path.open("rb") as stream:
        header = stream.read(24)
    if len(header) != 24 or header[:8] != b"\x89PNG\r\n\x1a\n" or header[12:16] != b"IHDR":
        raise AssertionError(f"not a PNG: {path}")
    return struct.unpack(">II", header[16:24])


class ArmorItemModelContract(unittest.TestCase):
    def test_all_expected_item_models_resolve(self):
        self.assertEqual(17, len(ITEMS))
        for item_id in ITEMS:
            with self.subTest(item=item_id):
                resolve_model(item_id)

    def test_texture_bindings_and_faces_resolve(self):
        for item_id in ITEMS:
            with self.subTest(item=item_id):
                model = resolve_model(item_id)
                textures = model.get("textures", {})
                for binding in ("main", "particle"):
                    self.assertIn(binding, textures)
                    self.assertTrue(resource_path(textures[binding], "textures", ".png").is_file())
                for element in model.get("elements", []):
                    for face in element.get("faces", {}).values():
                        self.assertEqual("#main", face.get("texture"))

    def test_geometry_uv_and_display_numbers_are_bounded(self):
        for item_id in ITEMS:
            with self.subTest(item=item_id):
                model = resolve_model(item_id)
                self.assertEqual(DISPLAY_CONTEXTS, set(model.get("display", {})))
                elements = model.get("elements", [])
                self.assertTrue(elements, f"{item_id} must resolve non-empty geometry")
                for index, element in enumerate(elements):
                    assert_finite_vector(self, element.get("from"), 3, f"{item_id} element {index} from")
                    assert_finite_vector(self, element.get("to"), 3, f"{item_id} element {index} to")
                    faces = element.get("faces", {})
                    self.assertTrue(faces, f"{item_id} element {index} must have faces")
                    for face_name, face in faces.items():
                        uv = face.get("uv")
                        assert_finite_vector(self, uv, 4, f"{item_id} element {index} {face_name} UV")
                        self.assertTrue(all(0.0 <= value <= 16.0 for value in uv))
                for context, transform in model["display"].items():
                    for field in ("rotation", "translation", "scale"):
                        if field in transform:
                            assert_finite_vector(self, transform[field], 3, f"{item_id} {context} {field}")

    def test_gui_models_are_centred_and_not_excessively_scaled(self):
        for item_id in ITEMS:
            with self.subTest(item=item_id):
                gui = resolve_model(item_id)["display"]["gui"]
                translation = gui.get("translation", [0.0, 0.0, 0.0])
                scale = gui.get("scale", [1.0, 1.0, 1.0])
                self.assertTrue(all(-4.0 <= value <= 4.0 for value in translation))
                self.assertTrue(all(0.0 < value <= 1.2 for value in scale))


class WornArmorContract(unittest.TestCase):
    LAYERS = tuple(
        f"{set_name}_layer_{layer}"
        for set_name in ("true_void", "true_celestial", "true_living", "true_balance")
        for layer in (1, 2)
    ) + ("corrupted_crystal_layer_2",)

    def test_worn_textures_are_png_128_by_64(self):
        for layer in self.LAYERS:
            with self.subTest(layer=layer):
                path = resource_path(f"{NAMESPACE}:models/armor/{layer}", "textures", ".png")
                self.assertEqual((128, 64), png_dimensions(path), f"{path} must match the 128x64 worn model atlas")

    def test_java_items_select_the_correct_worn_layer(self):
        true_path = (REPO_ROOT / "src/main/java/com/Momik/usless_mobs/item/TruePathArmorItem.java").read_text(encoding="utf-8")
        balance = (REPO_ROOT / "src/main/java/com/Momik/usless_mobs/item/ArmorOfBalanceItem.java").read_text(encoding="utf-8")
        corrupted = (REPO_ROOT / "src/main/mobs/endermite/java/net/mysith/silverfish/CorruptedCrystalLeggingsItem.java").read_text(encoding="utf-8")
        leggings_only = 'getType() == Type.LEGGINGS ? "_layer_2.png" : "_layer_1.png"'
        self.assertIn(leggings_only, true_path)
        self.assertIn('":textures/models/armor/" + path.key + layer', true_path)
        self.assertIn(leggings_only, balance)
        self.assertIn('":textures/models/armor/true_balance" + layer', balance)
        self.assertIn('"usless_mobs:textures/models/armor/corrupted_crystal_layer_2.png"', corrupted)


class VisualBaselineContract(unittest.TestCase):
    @unittest.skip("captured in dedicated UI task")
    def test_before_images_exist_and_are_real_window_captures(self):
        review = REPO_ROOT / "Modelle/Exports/armor_graphics_review"
        dimensions = []
        for filename in BASELINES:
            with self.subTest(filename=filename):
                path = review / filename
                self.assertTrue(path.is_file(), f"missing deterministic baseline {path}")
                width, height = png_dimensions(path)
                self.assertGreaterEqual(width, 800)
                self.assertGreaterEqual(height, 480)
                dimensions.append((width, height))
        self.assertEqual(1, len(set(dimensions)), "all baseline captures must use one window size")


if __name__ == "__main__":
    unittest.main()
