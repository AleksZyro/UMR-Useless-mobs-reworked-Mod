import json
import math
import re
import struct
import tempfile
import unittest
from pathlib import Path

from PIL import Image


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


def resolve_texture_binding(textures: dict, binding: str) -> str:
    seen = []
    current = binding
    while True:
        if current in seen:
            raise AssertionError(f"texture binding cycle: {' -> '.join(seen + [current])}")
        seen.append(current)
        if current not in textures:
            raise AssertionError(f"missing texture binding #{current}")
        value = textures[current]
        if not isinstance(value, str) or not value:
            raise AssertionError(f"invalid texture binding #{current}: {value!r}")
        if not value.startswith("#"):
            return value
        current = value[1:]
        if not current:
            raise AssertionError(f"missing texture binding name referenced by #{seen[-1]}")


def assert_raw_item_model_contract(item_id: str, model: dict) -> None:
    slot = item_id.rsplit("_", 1)[1]
    expected_texture = f"{NAMESPACE}:item/{item_id}"
    expected = {
        "parent": f"{NAMESPACE}:item/template/armor_{slot}_3d",
        "textures": {
            "main": expected_texture,
            "particle": expected_texture,
        },
    }
    if model != expected:
        raise AssertionError(f"{item_id} must be an exact parent-only model: expected {expected!r}, found {model!r}")


def assert_finite_vector(test: unittest.TestCase, vector, length: int, label: str) -> None:
    test.assertIsInstance(vector, list, label)
    test.assertEqual(length, len(vector), label)
    test.assertTrue(
        all(not isinstance(value, bool) and isinstance(value, (int, float)) and math.isfinite(value) for value in vector),
        label,
    )


def rotate_point(point: tuple[float, float, float], rotation: list[float]) -> tuple[float, float, float]:
    x, y, z = point
    for axis in (2, 1, 0):
        degrees = rotation[axis]
        angle = math.radians(degrees)
        cosine = math.cos(angle)
        sine = math.sin(angle)
        if axis == 0:
            y, z = y * cosine - z * sine, y * sine + z * cosine
        elif axis == 1:
            x, z = x * cosine + z * sine, -x * sine + z * cosine
        else:
            x, y = x * cosine - y * sine, x * sine + y * cosine
    return x, y, z


def gui_projected_bounds(model: dict) -> tuple[float, float, float, float]:
    gui = model["display"]["gui"]
    rotation = gui.get("rotation", [0.0, 0.0, 0.0])
    translation = gui.get("translation", [0.0, 0.0, 0.0])
    scale = gui.get("scale", [1.0, 1.0, 1.0])
    projected = []
    for element in model["elements"]:
        for x in (element["from"][0], element["to"][0]):
            for y in (element["from"][1], element["to"][1]):
                for z in (element["from"][2], element["to"][2]):
                    rotated = rotate_point((x - 8.0, y - 8.0, z - 8.0), rotation)
                    projected.append(
                        (
                            rotated[0] * scale[0] + translation[0],
                            rotated[1] * scale[1] + translation[1],
                        )
                    )
    xs = [point[0] for point in projected]
    ys = [point[1] for point in projected]
    return min(xs), max(xs), min(ys), max(ys)


def png_dimensions(path: Path) -> tuple[int, int]:
    try:
        with Image.open(path) as image:
            if image.format != "PNG":
                raise AssertionError(f"not a PNG: {path}")
            dimensions = image.size
            image.verify()
            return dimensions
    except (OSError, SyntaxError, ValueError) as error:
        raise AssertionError(f"invalid PNG: {path}: {error}") from error


def strip_java_comments(source: str) -> str:
    output = []
    index = 0
    state = "code"
    while index < len(source):
        char = source[index]
        following = source[index + 1] if index + 1 < len(source) else ""
        if state == "code":
            if char == "/" and following == "/":
                output.extend((" ", " "))
                index += 2
                state = "line_comment"
                continue
            if char == "/" and following == "*":
                output.extend((" ", " "))
                index += 2
                state = "block_comment"
                continue
            output.append(char)
            if char == '"':
                state = "string"
            elif char == "'":
                state = "character"
        elif state == "line_comment":
            output.append("\n" if char == "\n" else " ")
            if char == "\n":
                state = "code"
        elif state == "block_comment":
            if char == "*" and following == "/":
                output.extend((" ", " "))
                index += 2
                state = "code"
                continue
            output.append("\n" if char == "\n" else " ")
        else:
            output.append(char)
            if char == "\\" and following:
                output.append(following)
                index += 2
                continue
            if (state == "string" and char == '"') or (state == "character" and char == "'"):
                state = "code"
        index += 1
    if state == "block_comment":
        raise AssertionError("unterminated Java block comment")
    return "".join(output)


def matching_java_delimiter(source: str, start: int, opening: str, closing: str) -> int:
    depth = 0
    quote = None
    index = start
    while index < len(source):
        char = source[index]
        if quote:
            if char == "\\":
                index += 2
                continue
            if char == quote:
                quote = None
        elif char in ('"', "'"):
            quote = char
        elif char == opening:
            depth += 1
        elif char == closing:
            depth -= 1
            if depth == 0:
                return index
        index += 1
    raise AssertionError(f"unbalanced Java delimiter {opening}{closing}")


def java_method_body(source: str, method_name: str) -> str:
    source_without_comments = strip_java_comments(source)
    bodies = []
    for match in re.finditer(rf"\b{re.escape(method_name)}\s*\(", source_without_comments):
        opening_parenthesis = source_without_comments.find("(", match.start())
        closing_parenthesis = matching_java_delimiter(source_without_comments, opening_parenthesis, "(", ")")
        opening_brace = closing_parenthesis + 1
        while opening_brace < len(source_without_comments) and source_without_comments[opening_brace].isspace():
            opening_brace += 1
        if source_without_comments.startswith("throws", opening_brace):
            opening_brace = source_without_comments.find("{", opening_brace)
        if opening_brace < 0 or opening_brace >= len(source_without_comments) or source_without_comments[opening_brace] != "{":
            continue
        closing_brace = matching_java_delimiter(source_without_comments, opening_brace, "{", "}")
        bodies.append(source_without_comments[opening_brace + 1 : closing_brace])
    if len(bodies) != 1:
        raise AssertionError(f"expected one brace-balanced {method_name} method, found {len(bodies)}")
    return bodies[0]


def normalize_java(source: str) -> str:
    return " ".join(source.split())


def assert_java_method_contains(source: str, method_name: str, *fragments: str) -> None:
    body = normalize_java(java_method_body(source, method_name))
    for fragment in fragments:
        normalized_fragment = normalize_java(fragment)
        if normalized_fragment not in body:
            raise AssertionError(f"{method_name} is missing {normalized_fragment!r}")


class ContractHelperTests(unittest.TestCase):
    def test_display_rotation_matches_forge_z_then_y_then_x_point_order(self):
        rotated = rotate_point((1.0, 0.0, 0.0), [90.0, 90.0, 0.0])
        for actual, expected in zip(rotated, (0.0, 1.0, 0.0)):
            self.assertAlmostEqual(expected, actual, places=7)

    def test_raw_item_model_contract_rejects_identity_and_inheritance_mutations(self):
        valid = {
            "parent": "usless_mobs:item/template/armor_chestplate_3d",
            "textures": {
                "main": "usless_mobs:item/true_void_chestplate",
                "particle": "usless_mobs:item/true_void_chestplate",
            },
        }
        mutations = (
            {**valid, "textures": {**valid["textures"], "particle": "usless_mobs:item/common_armor"}},
            {**valid, "parent": "usless_mobs:item/template/armor_leggings_3d"},
            {**valid, "elements": [{"from": [0, 0, 0], "to": [16, 16, 16]}]},
        )
        for model in mutations:
            with self.subTest(model=model), self.assertRaises(AssertionError):
                assert_raw_item_model_contract("true_void_chestplate", model)

    def test_texture_aliases_resolve_to_the_final_resource_location(self):
        textures = {"main": "#material", "material": "usless_mobs:item/armor/example"}
        self.assertEqual("usless_mobs:item/armor/example", resolve_texture_binding(textures, "main"))

    def test_texture_alias_cycles_are_rejected(self):
        with self.assertRaisesRegex(AssertionError, "cycle"):
            resolve_texture_binding({"main": "#material", "material": "#main"}, "main")

    def test_missing_texture_aliases_are_rejected(self):
        with self.assertRaisesRegex(AssertionError, "missing"):
            resolve_texture_binding({"main": "#material"}, "main")

    def test_corrupt_or_truncated_png_is_rejected(self):
        fixtures = {
            "corrupt": b"not a png",
            "truncated": b"\x89PNG\r\n\x1a\n" + b"\x00\x00\x00\rIHDR" + struct.pack(">II", 128, 64),
        }
        with tempfile.TemporaryDirectory() as directory:
            for name, contents in fixtures.items():
                with self.subTest(name=name):
                    path = Path(directory) / f"{name}.png"
                    path.write_bytes(contents)
                    with self.assertRaisesRegex(AssertionError, "invalid PNG"):
                        png_dimensions(path)

    def test_java_wiring_fragments_in_comments_or_other_methods_are_rejected(self):
        expected = 'return "usless_mobs:textures/models/armor/example.png";'
        for decoy in (
            f"// String getArmorTexture() {{ {expected} }}",
            f"/* String getArmorTexture() {{ {expected} }} */",
            f"String unrelated() {{ {expected} }}",
        ):
            with self.subTest(decoy=decoy):
                source = f'''class Example {{
                    {decoy}
                    String getArmorTexture() {{ return "wrong"; }}
                }}'''
                with self.assertRaisesRegex(AssertionError, "getArmorTexture"):
                    assert_java_method_contains(source, "getArmorTexture", expected)


class ArmorItemModelContract(unittest.TestCase):
    def test_all_expected_item_models_resolve(self):
        self.assertEqual(17, len(ITEMS))
        for item_id in ITEMS:
            with self.subTest(item=item_id):
                path = resource_path(f"{NAMESPACE}:item/{item_id}", "models", ".json")
                with path.open(encoding="utf-8") as stream:
                    assert_raw_item_model_contract(item_id, json.load(stream))
                resolve_model(item_id)

    def test_texture_bindings_and_faces_resolve(self):
        for item_id in ITEMS:
            with self.subTest(item=item_id):
                model = resolve_model(item_id)
                textures = model.get("textures", {})
                for binding in ("main", "particle"):
                    location = resolve_texture_binding(textures, binding)
                    width, height = png_dimensions(resource_path(location, "textures", ".png"))
                    self.assertGreater(width, 0)
                    self.assertGreater(height, 0)
                for element in model.get("elements", []):
                    for face in element.get("faces", {}).values():
                        self.assertEqual("#main", face.get("texture"))
                        resolve_texture_binding(textures, face["texture"][1:])

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

    def test_inventory_geometry_is_nontrivial_and_genuinely_three_dimensional(self):
        for item_id in ITEMS:
            with self.subTest(item=item_id):
                elements = resolve_model(item_id)["elements"]
                self.assertGreaterEqual(len(elements), 2, f"{item_id} cannot be an empty or single-cube icon")
                for index, element in enumerate(elements):
                    dimensions = [element["to"][axis] - element["from"][axis] for axis in range(3)]
                    self.assertTrue(
                        all(dimension >= 0.4 for dimension in dimensions),
                        f"{item_id} element {index} must retain visible 3D depth",
                    )

    def test_chestplates_have_a_wide_armour_body_around_a_narrow_central_detail(self):
        for item_id in (f"{set_name}_chestplate" for set_name in SETS):
            with self.subTest(item=item_id):
                elements = resolve_model(item_id)["elements"]
                left = min(element["from"][0] for element in elements)
                right = max(element["to"][0] for element in elements)
                front = min(element["from"][2] for element in elements)
                central_details = [
                    element
                    for element in elements
                    if element["from"][2] <= front + 0.1
                    and element["to"][0] - element["from"][0] <= 4.0
                    and element["from"][0] < 8.0 < element["to"][0]
                ]
                self.assertGreaterEqual(right - left, 10.0, f"{item_id} needs recognisable shoulder/body width")
                self.assertTrue(central_details, f"{item_id} needs a visible central gem detail")
                detail_width = max(element["to"][0] - element["from"][0] for element in central_details)
                self.assertGreater(right - left, detail_width * 2.5)

    def test_leggings_have_two_separated_lower_leg_extents(self):
        for item_id in tuple(f"{set_name}_leggings" for set_name in SETS) + ("corrupted_crystal_leggings",):
            with self.subTest(item=item_id):
                lower_elements = [element for element in resolve_model(item_id)["elements"] if element["to"][1] <= 8.5]
                left_legs = [element for element in lower_elements if element["to"][0] <= 7.5]
                right_legs = [element for element in lower_elements if element["from"][0] >= 8.5]
                self.assertTrue(left_legs, f"{item_id} needs a distinct left leg")
                self.assertTrue(right_legs, f"{item_id} needs a distinct right leg")
                gap = min(element["from"][0] for element in right_legs) - max(
                    element["to"][0] for element in left_legs
                )
                self.assertGreaterEqual(gap, 1.0, f"{item_id} leg gap must remain visible in inventory")

    def test_gui_projection_is_centred_and_fits_the_inventory_icon(self):
        for item_id in ITEMS:
            with self.subTest(item=item_id):
                left, right, bottom, top = gui_projected_bounds(resolve_model(item_id))
                self.assertGreaterEqual(left, -7.75)
                self.assertLessEqual(right, 7.75)
                self.assertGreaterEqual(bottom, -7.75)
                self.assertLessEqual(top, 7.75)
                self.assertLessEqual(abs((left + right) / 2.0), 0.75)
                self.assertLessEqual(abs((bottom + top) / 2.0), 0.75)
                self.assertGreaterEqual(right - left, 5.0)
                self.assertGreaterEqual(top - bottom, 5.0)


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
        assert_java_method_contains(
            true_path,
            "getArmorTexture",
            leggings_only,
            'return Usless_mobs.MODID + ":textures/models/armor/" + path.key + layer;',
        )
        assert_java_method_contains(
            balance,
            "getArmorTexture",
            leggings_only,
            'return Usless_mobs.MODID + ":textures/models/armor/true_balance" + layer;',
        )
        assert_java_method_contains(
            corrupted,
            "getArmorTexture",
            'return "usless_mobs:textures/models/armor/corrupted_crystal_layer_2.png";',
        )


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
