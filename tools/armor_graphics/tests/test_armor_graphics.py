import hashlib
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


def split_java_arguments(arguments: str) -> list[str]:
    parts = []
    start = 0
    stack = []
    quote = None
    index = 0
    pairs = {"(": ")", "[": "]", "{": "}"}
    while index < len(arguments):
        char = arguments[index]
        if quote:
            if char == "\\":
                index += 2
                continue
            if char == quote:
                quote = None
        elif char in ('"', "'"):
            quote = char
        elif char in pairs:
            stack.append(pairs[char])
        elif stack and char == stack[-1]:
            stack.pop()
        elif char == "," and not stack:
            parts.append(arguments[start:index].strip())
            start = index + 1
        index += 1
    if quote or stack:
        raise AssertionError("unbalanced Java argument list")
    parts.append(arguments[start:].strip())
    return parts


JAVA_NUMBER = re.compile(r"[-+]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][-+]?\d+)?[fFdD]?")


def java_number(value: str, label: str) -> float:
    value = value.strip()
    if not JAVA_NUMBER.fullmatch(value):
        raise AssertionError(f"{label} must be a finite numeric literal, found {value!r}")
    number = float(value.rstrip("fFdD"))
    if not math.isfinite(number):
        raise AssertionError(f"{label} must be finite, found {value!r}")
    return number


def worn_model_cuboids(source: str) -> list[tuple[str, float, float, tuple[float, float, float]]]:
    source = strip_java_comments(source)
    cuboids = []
    paired_tex_offs = 0
    paired_add_boxes = 0
    for match in re.finditer(r"\.addOrReplaceChild\s*\(", source):
        opening = source.find("(", match.start())
        closing = matching_java_delimiter(source, opening, "(", ")")
        arguments = split_java_arguments(source[opening + 1 : closing])
        if len(arguments) < 2:
            continue
        part_match = re.fullmatch(r'"([^"\\]+)"', arguments[0])
        if not part_match:
            continue
        part = part_match.group(1)
        builder = arguments[1]
        calls = []
        for call in re.finditer(r"\b(texOffs|addBox)\s*\(", builder):
            call_opening = builder.find("(", call.start())
            call_closing = matching_java_delimiter(builder, call_opening, "(", ")")
            calls.append((call.group(1), split_java_arguments(builder[call_opening + 1 : call_closing])))
        if not calls:
            continue
        if len(calls) % 2 or any(
            calls[index][0] != ("texOffs" if index % 2 == 0 else "addBox")
            for index in range(len(calls))
        ):
            raise AssertionError(f"{part}: every texOffs must be followed by its corresponding addBox")
        for index in range(0, len(calls), 2):
            tex_args = calls[index][1]
            box_args = calls[index + 1][1]
            if len(tex_args) != 2 or len(box_args) < 6:
                raise AssertionError(f"{part}: malformed texOffs/addBox cuboid")
            u = java_number(tex_args[0], f"{part} texture U")
            v = java_number(tex_args[1], f"{part} texture V")
            dimensions = tuple(java_number(box_args[axis], f"{part} dimension {axis - 2}") for axis in range(3, 6))
            cuboids.append((part, u, v, dimensions))
            paired_tex_offs += 1
            paired_add_boxes += 1
    total_tex_offs = len(re.findall(r"\btexOffs\s*\(", source))
    total_add_boxes = len(re.findall(r"\baddBox\s*\(", source))
    if paired_tex_offs != total_tex_offs or paired_add_boxes != total_add_boxes:
        raise AssertionError(
            f"parsed {paired_tex_offs}/{total_tex_offs} texOffs and {paired_add_boxes}/{total_add_boxes} addBox calls"
        )
    return cuboids


def assert_show_for_type_contract(source: str) -> None:
    body = java_method_body(source, "showForType")
    switch_match = re.search(r"\bswitch\s*\(\s*type\s*\)\s*\{", body)
    if not switch_match:
        raise AssertionError("showForType must switch on type")
    switch_opening = body.find("{", switch_match.start())
    switch_closing = matching_java_delimiter(body, switch_opening, "{", "}")
    prefix = normalize_java(body[: switch_match.start()])
    if prefix != "model.setAllVisible(false);":
        raise AssertionError("showForType must first hide every base model part")
    if normalize_java(body[switch_closing + 1 :]):
        raise AssertionError("showForType must not execute anything after its slot switch")
    if len(re.findall(r"\bmodel\.setAllVisible\s*\(", body)) != 1:
        raise AssertionError("showForType must call setAllVisible exactly once")
    expected = {
        "HELMET": {"head", "hat"},
        "CHESTPLATE": {"body", "rightArm", "leftArm"},
        "LEGGINGS": {"body", "rightLeg", "leftLeg"},
        "BOOTS": {"rightLeg", "leftLeg"},
    }
    actual = {}
    switch_body = body[switch_opening + 1 : switch_closing]
    for case in re.finditer(r"\bcase\s+(HELMET|CHESTPLATE|LEGGINGS|BOOTS)\s*->\s*\{", switch_body):
        opening = switch_body.find("{", case.start())
        closing = matching_java_delimiter(switch_body, opening, "{", "}")
        case_body = switch_body[opening + 1 : closing]
        visibility_assignment = r"\bmodel\.(\w+)\.visible\s*=\s*(true|false)\s*;"
        assignments = re.findall(visibility_assignment, case_body)
        if normalize_java(re.sub(visibility_assignment, "", case_body)):
            raise AssertionError(f"showForType {case.group(1)} may contain only visibility assignments")
        if any(value != "true" for _, value in assignments):
            raise AssertionError(f"showForType {case.group(1)} may only reveal its required parts")
        parts = [part for part, _ in assignments]
        if len(parts) != len(set(parts)):
            raise AssertionError(f"showForType {case.group(1)} contains duplicate visibility assignments")
        actual[case.group(1)] = set(parts)
    if actual != expected:
        raise AssertionError(f"showForType visibility mapping must be {expected!r}, found {actual!r}")
    if len(re.findall(r"\.visible\s*=", body)) != sum(len(parts) for parts in expected.values()):
        raise AssertionError("showForType contains visibility assignments outside its four slot cases")


def assert_custom_armor_model_contract(source: str, factory_call: str) -> None:
    body = normalize_java(java_method_body(source, "getHumanoidArmorModel"))
    guard = normalize_java("if (slot != getType().getSlot()) { return original; }")
    if not body.startswith(guard):
        raise AssertionError("getHumanoidArmorModel must first return the original model for every other slot")
    copy_pose = normalize_java("((HumanoidModel) original).copyPropertiesTo((HumanoidModel) model);")
    show_slot = normalize_java("WornTruePathArmorModel.showForType(model, getType());")
    factory_call = normalize_java(factory_call)
    factory_block = normalize_java(f"if (model == null) {{ {factory_call} }}")
    if body.count(factory_block) != 1:
        raise AssertionError("getHumanoidArmorModel must create its custom model only inside the null guard")
    model_assignments = re.findall(r"(?<![\w.])(?:this\.)?model\s*=(?!=)[^;]+;", body)
    if model_assignments != [factory_call]:
        raise AssertionError("getHumanoidArmorModel may only assign model with its custom factory")
    required = (factory_call, copy_pose, show_slot, "return model;")
    positions = []
    for fragment in required:
        if body.count(fragment) != 1:
            raise AssertionError(f"getHumanoidArmorModel must contain exactly one {fragment!r}")
        positions.append(body.index(fragment))
    if positions != sorted(positions):
        raise AssertionError("getHumanoidArmorModel must create, pose, select, then return the custom model")
    if body.count("return original;") != 1 or body.count("return model;") != 1:
        raise AssertionError("getHumanoidArmorModel may only return the original guard or the correctly posed custom model")


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

    def test_worn_cuboid_parser_names_parts_and_rejects_invalid_or_unpaired_geometry(self):
        valid = '''class Example {
            void build(PartDefinition root) {
                root.addOrReplaceChild("named_part",
                    CubeListBuilder.create().texOffs(1, 2)
                        .addBox(0.0F, 0.0F, 0.0F, 3.0F, 4.0F, 5.0F, new CubeDeformation(0.0F)),
                    PartPose.ZERO);
            }
        }'''
        self.assertEqual([("named_part", 1.0, 2.0, (3.0, 4.0, 5.0))], worn_model_cuboids(valid))
        mutations = (
            valid.replace("3.0F", "true", 1),
            valid.replace(".addBox", ".mirror()"),
            valid.replace("texOffs(1, 2)", "texOffs(1, Float.NaN)"),
        )
        for mutation in mutations:
            with self.subTest(mutation=mutation), self.assertRaisesRegex(AssertionError, "named_part|parsed"):
                worn_model_cuboids(mutation)

    def test_show_for_type_contract_ignores_comments_and_dead_methods(self):
        valid_method = '''static void showForType(HumanoidModel<?> model, ArmorItem.Type type) {
            model.setAllVisible(false);
            switch (type) {
                case HELMET -> { model.head.visible = true; model.hat.visible = true; }
                case CHESTPLATE -> { model.body.visible = true; model.rightArm.visible = true; model.leftArm.visible = true; }
                case LEGGINGS -> { model.body.visible = true; model.rightLeg.visible = true; model.leftLeg.visible = true; }
                case BOOTS -> { model.rightLeg.visible = true; model.leftLeg.visible = true; }
            }
        }'''
        assert_show_for_type_contract(f"class Example {{ {valid_method} }}")
        bad_method = valid_method.replace("model.leftLeg.visible = true;", "", 1)
        dead_method = valid_method.replace("showForType", "deadMethod", 1)
        for decoy in (
            f"// {valid_method}",
            f"/* {valid_method} */",
            dead_method,
        ):
            with self.subTest(decoy=decoy), self.assertRaisesRegex(AssertionError, "showForType"):
                assert_show_for_type_contract(f"class Example {{ {decoy} {bad_method} }}")
        mutations = (
            valid_method.replace(
                "            }\n        }",
                "            }\n            model.setAllVisible(true);\n        }",
            ),
            valid_method.replace(
                "case HELMET -> { model.head.visible = true;",
                "case HELMET -> { model.setAllVisible(true); model.head.visible = true;",
            ),
        )
        for mutation in mutations:
            with self.subTest(mutation=mutation), self.assertRaisesRegex(AssertionError, "showForType"):
                assert_show_for_type_contract(f"class Example {{ {mutation} }}")

    def test_custom_armor_model_contract_rejects_comment_dead_method_and_order_mutations(self):
        factory = "model = WornTruePathArmorModel.create(path, getType());"
        valid_method = f'''HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack,
                EquipmentSlot slot, HumanoidModel<?> original) {{
            if (slot != getType().getSlot()) {{ return original; }}
            if (model == null) {{ {factory} }}
            ((HumanoidModel) original).copyPropertiesTo((HumanoidModel) model);
            WornTruePathArmorModel.showForType(model, getType());
            return model;
        }}'''
        assert_custom_armor_model_contract(f"class Example {{ {valid_method} }}", factory)
        bad_method = valid_method.replace("if (slot != getType().getSlot()) { return original; }", "")
        dead_method = valid_method.replace("getHumanoidArmorModel", "deadMethod", 1)
        for decoy in (
            f"// {valid_method}",
            f"/* {valid_method} */",
            dead_method,
        ):
            with self.subTest(decoy=decoy), self.assertRaisesRegex(AssertionError, "getHumanoidArmorModel"):
                assert_custom_armor_model_contract(f"class Example {{ {decoy} {bad_method} }}", factory)
        reversed_pose = valid_method.replace(
            "((HumanoidModel) original).copyPropertiesTo((HumanoidModel) model);\n            WornTruePathArmorModel.showForType(model, getType());",
            "WornTruePathArmorModel.showForType(model, getType());\n            ((HumanoidModel) original).copyPropertiesTo((HumanoidModel) model);",
        )
        with self.assertRaisesRegex(AssertionError, "create, pose, select"):
            assert_custom_armor_model_contract(f"class Example {{ {reversed_pose} }}", factory)
        reassigned_model = valid_method.replace(
            "if (slot != getType().getSlot()) { return original; }",
            "if (slot != getType().getSlot()) { return original; } model = (HumanoidModel) original;",
        )
        with self.assertRaisesRegex(AssertionError, "getHumanoidArmorModel"):
            assert_custom_armor_model_contract(f"class Example {{ {reassigned_model} }}", factory)


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

    def test_corrupted_layer_is_an_exact_two_by_nearest_neighbour_upscale(self):
        path = resource_path(f"{NAMESPACE}:models/armor/corrupted_crystal_layer_2", "textures", ".png")
        with Image.open(path) as image:
            image.load()
            self.assertEqual("RGBA", image.mode, f"{path} must preserve RGBA transparency semantics")
            self.assertEqual((128, 64), image.size)
            pixels = image.load()
            downsampled = bytearray()
            for source_y in range(32):
                for source_x in range(64):
                    block = {
                        pixels[source_x * 2 + offset_x, source_y * 2 + offset_y]
                        for offset_y in range(2)
                        for offset_x in range(2)
                    }
                    self.assertEqual(1, len(block), f"source pixel ({source_x}, {source_y}) must remain one sharp 2x2 block")
                    downsampled.extend(next(iter(block)))
        self.assertEqual(
            "340a3af52d5e547e9990a8ca765099e4035871053d27f59985e07010674d1c41",
            hashlib.sha256(downsampled).hexdigest(),
            "downsampled decoded pixels must exactly match the original 64x32 RGBA texture",
        )

    def test_worn_java_cuboids_fit_the_declared_128_by_64_atlas(self):
        path = REPO_ROOT / "src/main/java/com/Momik/usless_mobs/client/WornTruePathArmorModel.java"
        cuboids = worn_model_cuboids(path.read_text(encoding="utf-8"))
        self.assertGreaterEqual(len(cuboids), 30, "worn model parsing must be non-vacuous")
        self.assertIn("true_outer_helm", {part for part, _, _, _ in cuboids})
        self.assertIn("true_left_boot_leaf", {part for part, _, _, _ in cuboids})
        for part, u, v, dimensions in cuboids:
            with self.subTest(part=part, u=u, v=v, dimensions=dimensions):
                width, height, depth = dimensions
                self.assertTrue(all(dimension > 0.0 for dimension in dimensions), f"{part} must have positive finite dimensions")
                self.assertGreaterEqual(u, 0.0, f"{part} texture U")
                self.assertGreaterEqual(v, 0.0, f"{part} texture V")
                self.assertLessEqual(u + 2.0 * (width + depth), 128.0, f"{part} cuboid UV width exceeds the atlas")
                self.assertLessEqual(v + depth + height, 64.0, f"{part} cuboid UV height exceeds the atlas")

    def test_worn_java_slot_visibility_mapping_is_exact(self):
        path = REPO_ROOT / "src/main/java/com/Momik/usless_mobs/client/WornTruePathArmorModel.java"
        assert_show_for_type_contract(path.read_text(encoding="utf-8"))

    def test_custom_worn_models_are_slot_guarded_and_copy_the_original_pose(self):
        contracts = {
            "TruePathArmorItem.java": "model = WornTruePathArmorModel.create(path, getType());",
            "ArmorOfBalanceItem.java": "model = WornTruePathArmorModel.createBalanced(getType());",
        }
        for filename, factory_call in contracts.items():
            with self.subTest(item=filename):
                path = REPO_ROOT / "src/main/java/com/Momik/usless_mobs/item" / filename
                assert_custom_armor_model_contract(path.read_text(encoding="utf-8"), factory_call)

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
