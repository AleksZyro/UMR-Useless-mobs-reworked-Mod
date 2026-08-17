import hashlib
import importlib.util
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


def mask_java_non_code(source: str, *, comments: bool = True, literals: bool = True) -> str:
    masked = list(source)
    index = 0
    state = "code"

    def blank(position: int, length: int = 1) -> None:
        for offset in range(length):
            current = position + offset
            masked[current] = "\n" if source[current] == "\n" else " "

    while index < len(source):
        char = source[index]
        following = source[index + 1] if index + 1 < len(source) else ""
        if state == "code":
            if char == "/" and following == "/":
                if comments:
                    blank(index, 2)
                index += 2
                state = "line_comment"
                continue
            if char == "/" and following == "*":
                if comments:
                    blank(index, 2)
                index += 2
                state = "block_comment"
                continue
            if source.startswith('"""', index):
                index += 3
                state = "text_block"
                continue
            if char == '"':
                state = "string"
            elif char == "'":
                state = "character"
        elif state == "line_comment":
            if comments:
                blank(index)
            if char == "\n":
                state = "code"
        elif state == "block_comment":
            if char == "*" and following == "/":
                if comments:
                    blank(index, 2)
                index += 2
                state = "code"
                continue
            if comments:
                blank(index)
        elif state == "text_block":
            if char == "\\" and following:
                if literals:
                    blank(index, 2)
                index += 2
                continue
            if source.startswith('"""', index):
                index += 3
                state = "code"
                continue
            if literals:
                blank(index)
        else:
            if char == "\\" and following:
                if literals:
                    blank(index, 2)
                index += 2
                continue
            if (state == "string" and char == '"') or (state == "character" and char == "'"):
                state = "code"
            elif literals:
                blank(index)
        index += 1
    if state == "block_comment":
        raise AssertionError("unterminated Java block comment")
    if state in {"string", "character", "text_block"}:
        raise AssertionError("unterminated Java string, character, or text block literal")
    return "".join(masked)


def strip_java_comments(source: str) -> str:
    return mask_java_non_code(source, comments=True, literals=False)


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


def mask_java_literal_contents(source: str) -> str:
    return mask_java_non_code(source, comments=False, literals=True)


def java_top_level_units(source: str) -> list[str]:
    units = []
    start = None
    stack = []
    quote = None
    index = 0
    pairs = {"(": ")", "[": "]", "{": "}"}
    while index < len(source):
        char = source[index]
        if start is None:
            if char.isspace():
                index += 1
                continue
            start = index
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
        elif char in ")]}":
            if not stack or char != stack[-1]:
                raise AssertionError("unbalanced Java top-level flow")
            stack.pop()
            if char == "}" and not stack:
                units.append(source[start : index + 1])
                start = None
        elif char == ";" and not stack:
            units.append(source[start : index + 1])
            start = None
        index += 1
    if quote or stack or (start is not None and source[start:].strip()):
        raise AssertionError("incomplete Java top-level flow")
    return units


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
    searchable_source = mask_java_literal_contents(source)
    cuboids = []
    paired_tex_offs = 0
    paired_add_boxes = 0
    for match in re.finditer(r"\.addOrReplaceChild\s*\(", searchable_source):
        opening = searchable_source.find("(", match.start())
        closing = matching_java_delimiter(searchable_source, opening, "(", ")")
        arguments = split_java_arguments(source[opening + 1 : closing])
        if len(arguments) < 2:
            continue
        part_match = re.fullmatch(r'"([^"\\]+)"', arguments[0])
        if not part_match:
            continue
        part = part_match.group(1)
        builder = arguments[1]
        searchable_builder = mask_java_literal_contents(builder)
        calls = []
        for call in re.finditer(r"\b(texOffs|addBox)\s*\(", searchable_builder):
            call_opening = searchable_builder.find("(", call.start())
            call_closing = matching_java_delimiter(searchable_builder, call_opening, "(", ")")
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
    total_tex_offs = len(re.findall(r"\btexOffs\s*\(", searchable_source))
    total_add_boxes = len(re.findall(r"\baddBox\s*\(", searchable_source))
    if paired_tex_offs != total_tex_offs or paired_add_boxes != total_add_boxes:
        raise AssertionError(
            f"parsed {paired_tex_offs}/{total_tex_offs} texOffs and {paired_add_boxes}/{total_add_boxes} addBox calls"
        )
    return cuboids


def worn_model_child_owners(source: str) -> dict[str, str]:
    source = strip_java_comments(source)
    searchable = mask_java_literal_contents(source)
    owners = {}
    for match in re.finditer(r"\broot\.getChild\s*\(", searchable):
        bone_opening = searchable.find("(", match.start())
        bone_closing = matching_java_delimiter(searchable, bone_opening, "(", ")")
        bone_arguments = split_java_arguments(source[bone_opening + 1 : bone_closing])
        if len(bone_arguments) != 1:
            raise AssertionError("root.getChild must receive exactly one bone name")
        bone_match = re.fullmatch(r'"([^"\\]+)"', bone_arguments[0])
        if not bone_match:
            continue
        child_call = re.match(r"\s*\.addOrReplaceChild\s*\(", searchable[bone_closing + 1 :])
        if not child_call:
            continue
        child_opening = bone_closing + 1 + child_call.end() - 1
        child_closing = matching_java_delimiter(searchable, child_opening, "(", ")")
        child_arguments = split_java_arguments(source[child_opening + 1 : child_closing])
        if not child_arguments:
            raise AssertionError(f"{bone_match.group(1)} addOrReplaceChild requires a child name")
        child_match = re.fullmatch(r'"([^"\\]+)"', child_arguments[0])
        if not child_match:
            continue
        child = child_match.group(1)
        if child in owners:
            raise AssertionError(f"duplicate worn child {child}")
        owners[child] = bone_match.group(1)
    return owners


def worn_method_geometry(source: str, method_name: str) -> dict[str, tuple]:
    searchable_source = mask_java_non_code(source)
    declaration = re.compile(
        rf"\bprivate\s+static\s+void\s+{re.escape(method_name)}\s*\(\s*PartDefinition\s+root\s*\)\s*\{{"
    )
    declarations = list(declaration.finditer(searchable_source))
    if len(declarations) != 1:
        raise AssertionError(f"expected one executable {method_name} geometry method, found {len(declarations)}")
    opening = searchable_source.find("{", declarations[0].start())
    closing = matching_java_delimiter(searchable_source, opening, "{", "}")
    executable_body = searchable_source[opening + 1 : closing]
    source_body = strip_java_comments(source)[opening + 1 : closing]

    geometry = {}
    child_calls = list(re.finditer(r"\.addOrReplaceChild\s*\(", executable_body))
    root_calls = list(re.finditer(r"\broot\.getChild\s*\(", executable_body))
    if len(child_calls) != len(root_calls):
        raise AssertionError(f"{method_name} geometry must attach every child through a literal root bone")

    for root_call in root_calls:
        bone_opening = executable_body.find("(", root_call.start())
        bone_closing = matching_java_delimiter(executable_body, bone_opening, "(", ")")
        bone_arguments = split_java_arguments(source_body[bone_opening + 1 : bone_closing])
        if len(bone_arguments) != 1:
            raise AssertionError(f"{method_name} root.getChild must receive one bone")
        bone = re.fullmatch(r'"([^"\\]+)"', bone_arguments[0])
        if not bone:
            raise AssertionError(f"{method_name} geometry bone must be a string literal")

        child_call = re.match(r"\s*\.addOrReplaceChild\s*\(", executable_body[bone_closing + 1 :])
        if not child_call:
            raise AssertionError(f"{method_name} root.getChild must immediately attach its child")
        child_opening = bone_closing + 1 + child_call.end() - 1
        child_closing = matching_java_delimiter(executable_body, child_opening, "(", ")")
        child_arguments = split_java_arguments(source_body[child_opening + 1 : child_closing])
        if len(child_arguments) != 3:
            raise AssertionError(f"{method_name} addOrReplaceChild must have name, cube, and pose")
        child = re.fullmatch(r'"([^"\\]+)"', child_arguments[0])
        if not child:
            raise AssertionError(f"{method_name} geometry child must be a string literal")
        part = child.group(1)
        if part in geometry:
            raise AssertionError(f"duplicate {method_name} geometry child {part}")

        builder = child_arguments[1]
        searchable_builder = mask_java_non_code(builder)
        calls = []
        for call in re.finditer(r"\b(texOffs|addBox)\s*\(", searchable_builder):
            call_opening = searchable_builder.find("(", call.start())
            call_closing = matching_java_delimiter(searchable_builder, call_opening, "(", ")")
            calls.append((call.group(1), call.start(), call_opening, call_closing))
        if [call[0] for call in calls] != ["texOffs", "addBox"]:
            raise AssertionError(f"{part} must contain exactly one texOffs/addBox pair")
        tex_call, box_call = calls
        if normalize_java(builder[: tex_call[1]]) != "CubeListBuilder.create().":
            raise AssertionError(f"{part} must start with CubeListBuilder.create().texOffs")
        if normalize_java(builder[tex_call[3] + 1 : box_call[1]]) != ".":
            raise AssertionError(f"{part} texOffs must directly precede addBox")
        if normalize_java(builder[box_call[3] + 1 :]):
            raise AssertionError(f"{part} must end after its addBox")
        tex_args = split_java_arguments(builder[tex_call[2] + 1 : tex_call[3]])
        box_args = split_java_arguments(builder[box_call[2] + 1 : box_call[3]])
        if len(tex_args) != 2 or len(box_args) != 7:
            raise AssertionError(f"{part} has malformed texture or box arguments")
        uv = tuple(java_number(value, f"{part} texture coordinate") for value in tex_args)
        origin = tuple(java_number(value, f"{part} box origin") for value in box_args[:3])
        dimensions = tuple(java_number(value, f"{part} box dimension") for value in box_args[3:6])
        deformation_match = re.fullmatch(r"new\s+CubeDeformation\s*\((.*)\)", box_args[6], re.DOTALL)
        if not deformation_match:
            raise AssertionError(f"{part} must use one literal CubeDeformation")
        deformation_args = split_java_arguments(deformation_match.group(1))
        if len(deformation_args) != 1:
            raise AssertionError(f"{part} CubeDeformation must have one argument")
        deformation = java_number(deformation_args[0], f"{part} deformation")

        pose = normalize_java(child_arguments[2])
        if pose == "PartPose.ZERO":
            pose_type = "ZERO"
            offset = (0.0, 0.0, 0.0)
            rotation = (0.0, 0.0, 0.0)
        else:
            pose_match = re.fullmatch(r"PartPose\.(rotation|offsetAndRotation)\s*\((.*)\)", child_arguments[2], re.DOTALL)
            if not pose_match:
                raise AssertionError(f"{part} has unsupported pose {pose!r}")
            pose_values = tuple(
                java_number(value, f"{part} pose value")
                for value in split_java_arguments(pose_match.group(2))
            )
            pose_type = pose_match.group(1)
            if pose_type == "rotation" and len(pose_values) == 3:
                offset = (0.0, 0.0, 0.0)
                rotation = pose_values
            elif pose_type == "offsetAndRotation" and len(pose_values) == 6:
                offset = pose_values[:3]
                rotation = pose_values[3:]
            else:
                raise AssertionError(f"{part} has malformed {pose_type} pose")

        geometry[part] = (bone.group(1), uv, origin, dimensions, deformation, pose_type, offset, rotation)
    return geometry


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
    body = java_method_body(source, "getHumanoidArmorModel")
    guard = normalize_java("if (slot != getType().getSlot()) { return original; }")
    copy_pose = normalize_java("((HumanoidModel) original).copyPropertiesTo((HumanoidModel) model);")
    show_slot = normalize_java("WornTruePathArmorModel.showForType(model, getType());")
    factory_call = normalize_java(factory_call)
    factory_block = normalize_java(f"if (model == null) {{ {factory_call} }}")
    expected_flow = [guard, factory_block, copy_pose, show_slot, "return model;"]
    actual_flow = [normalize_java(unit) for unit in java_top_level_units(body)]
    if actual_flow != expected_flow:
        raise AssertionError(
            "getHumanoidArmorModel must create, pose, select, then return the custom model in the exact top-level flow"
        )


def assert_true_path_texture_contract(source: str) -> None:
    body = java_method_body(source, "getArmorTexture")
    expected_flow = [
        normalize_java('String layer = getType() == Type.LEGGINGS ? "_layer_2.png" : "_layer_1.png";'),
        normalize_java('''if (path == Path.VOID && getType() == Type.CHESTPLATE) {
            return Usless_mobs.MODID + ":textures/models/armor/true_void_chestplate_layer_1.png";
        }'''),
        normalize_java('return Usless_mobs.MODID + ":textures/models/armor/" + path.key + layer;'),
    ]
    actual_flow = [normalize_java(unit) for unit in java_top_level_units(body)]
    if actual_flow != expected_flow:
        raise AssertionError("getArmorTexture must isolate the dedicated atlas to the True-Void chestplate")


def assert_void_crystal_knight_routing_contract(source: str) -> None:
    searchable = mask_java_non_code(source)
    method_name = "addVoidCrystalKnightDetails"
    declaration_pattern = rf"\bprivate\s+static\s+void\s+{method_name}\s*\("
    if len(re.findall(declaration_pattern, searchable)) != 1:
        raise AssertionError(f"expected exactly one executable {method_name} declaration")

    create_declaration_pattern = r"\bpublic\s+static\s+[\w.<>?]+\s+create\s*\("
    if len(re.findall(create_declaration_pattern, searchable)) != 1:
        raise AssertionError("expected exactly one public static create method")
    create_body = java_method_body(searchable, "create")

    def unique_if_body(container: str, condition: str, label: str) -> str:
        matches = list(re.finditer(rf"\bif\s*\(\s*{condition}\s*\)\s*\{{", container))
        if len(matches) != 1:
            raise AssertionError(f"expected one {label} branch in public static create")
        opening = container.find("{", matches[0].start())
        closing = matching_java_delimiter(container, opening, "{", "}")
        return container[opening + 1 : closing]

    chestplate_body = unique_if_body(
        create_body,
        r"type\s*==\s*ArmorItem\.Type\.CHESTPLATE",
        "CHESTPLATE",
    )
    void_body = unique_if_body(
        chestplate_body,
        r"path\s*==\s*TruePathArmorItem\.Path\.VOID",
        "Path.VOID chestplate",
    )
    direct_call = f"{method_name}(root);"
    chestplate_units = [normalize_java(unit) for unit in java_top_level_units(chestplate_body)]
    expected_chestplate_units = [
        normalize_java(
            f"if (path == TruePathArmorItem.Path.VOID) {{ {direct_call} }}"
        ),
        normalize_java("else { addChestDetails(root, path); }"),
    ]
    if chestplate_units != expected_chestplate_units:
        raise AssertionError(
            f"{method_name} must be the direct Path.VOID chestplate branch with the legacy else branch"
        )
    if [normalize_java(unit) for unit in java_top_level_units(void_body)] != [direct_call]:
        raise AssertionError(f"{method_name} must be a direct top-level Path.VOID call")

    invocation_pattern = rf"\b{method_name}\s*\("
    if len(re.findall(invocation_pattern, searchable)) != 2:
        raise AssertionError(f"expected exactly one executable {method_name} declaration and call")
    call_pattern = rf"(?<![\w.$]){method_name}\s*\(\s*root\s*\)\s*;"
    if len(re.findall(call_pattern, searchable)) != 1:
        raise AssertionError(f"expected exactly one executable {method_name} call")


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
        expected = [("named_part", 1.0, 2.0, (3.0, 4.0, 5.0))]
        self.assertEqual(expected, worn_model_cuboids(valid))
        diagnostic = valid.replace(
            "void build(PartDefinition root)",
            'String diagnostic = "texOffs(99, 99).addBox(0, 0, 0, 1, 1, 1)";\n            void build(PartDefinition root)',
        )
        self.assertEqual(expected, worn_model_cuboids(diagnostic))
        mutations = (
            valid.replace("3.0F", "true", 1),
            valid.replace(".addBox", ".mirror()"),
            valid.replace("texOffs(1, 2)", "texOffs(1, Float.NaN)"),
        )
        for mutation in mutations:
            with self.subTest(mutation=mutation), self.assertRaisesRegex(AssertionError, "named_part|parsed"):
                worn_model_cuboids(mutation)

    def test_worn_child_owner_parser_ignores_decoys_and_rejects_duplicates(self):
        valid = '''class Example {
            void build(PartDefinition root) {
                // root.getChild("wrong").addOrReplaceChild("comment", factory(), PartPose.ZERO);
                String decoy = "root.getChild(\\\"wrong\\\").addOrReplaceChild(\\\"literal\\\", x, y)";
                root.getChild("body").addOrReplaceChild("plate", factory(), PartPose.ZERO);
            }
        }'''
        self.assertEqual({"plate": "body"}, worn_model_child_owners(valid))
        duplicate = valid.replace(
            "root.getChild(\"body\").addOrReplaceChild(\"plate\", factory(), PartPose.ZERO);",
            '''root.getChild("body").addOrReplaceChild("plate", factory(), PartPose.ZERO);
                root.getChild("left_arm").addOrReplaceChild("plate", factory(), PartPose.ZERO);''',
        )
        with self.assertRaisesRegex(AssertionError, "duplicate worn child plate"):
            worn_model_child_owners(duplicate)

    def test_worn_method_geometry_parser_is_method_scoped_and_ignores_non_code_decoys(self):
        valid = '''class Example {
            // private static void build(PartDefinition root) { root.getChild("wrong").addOrReplaceChild("comment", x, y); }
            String decoy = "private static void build(PartDefinition root) { root.getChild(\\\"wrong\\\").addOrReplaceChild(\\\"literal\\\", x, y); }";
            private static void unrelated(PartDefinition root) {
                root.getChild("wrong").addOrReplaceChild("legacy",
                    CubeListBuilder.create().texOffs(99, 99)
                        .addBox(9, 9, 9, 9, 9, 9, new CubeDeformation(9)),
                    PartPose.ZERO);
            }
            private static void build(PartDefinition root) {
                // root.getChild("wrong").addOrReplaceChild("comment_inside", x, y);
                String inside = "root.getChild(\\\"wrong\\\").addOrReplaceChild(\\\"literal_inside\\\", x, y)";
                root.getChild("body").addOrReplaceChild("plate",
                    CubeListBuilder.create().texOffs(1, 2)
                        .addBox(-1.0F, -2.0F, -3.0F, 4.0F, 5.0F, 6.0F, new CubeDeformation(0.04F)),
                    PartPose.offsetAndRotation(7.0F, 8.0F, 9.0F, 0.1F, 0.2F, 0.3F));
            }
        }'''
        expected = {
            "plate": (
                "body",
                (1.0, 2.0),
                (-1.0, -2.0, -3.0),
                (4.0, 5.0, 6.0),
                0.04,
                "offsetAndRotation",
                (7.0, 8.0, 9.0),
                (0.1, 0.2, 0.3),
            )
        }
        self.assertEqual(expected, worn_method_geometry(valid, "build"))

        mutations = (
            valid.replace('root.getChild("body")', 'body'),
            valid.replace("PartPose.offsetAndRotation", "PartPose.offset"),
            valid.replace("new CubeDeformation(0.04F)", "CubeDeformation.NONE"),
        )
        for mutation in mutations:
            with self.subTest(mutation=mutation), self.assertRaises(AssertionError):
                worn_method_geometry(mutation, "build")

    def test_void_crystal_knight_routing_rejects_comment_literal_and_unrelated_decoys(self):
        valid = '''class Example {
            public static Example create(Path path, ArmorItem.Type type) {
                PartDefinition root = mesh.getRoot();
                if (type == ArmorItem.Type.CHESTPLATE) {
                    if (path == TruePathArmorItem.Path.VOID) {
                        addVoidCrystalKnightDetails(root);
                    } else {
                        addChestDetails(root, path);
                    }
                }
                return new Example();
            }

            private static void addVoidCrystalKnightDetails(PartDefinition root) {}
        }'''
        assert_void_crystal_knight_routing_contract(valid)
        text_block_decoy = valid.replace(
            "PartDefinition root = mesh.getRoot();",
            'String decoy = """\n'
            "                    // addVoidCrystalKnightDetails(root);\n"
            "                    /* private static void addVoidCrystalKnightDetails(PartDefinition root) {} */\n"
            "                    builder.addVoidCrystalKnightDetails(root);\n"
            '                    """;\n'
            "                PartDefinition root = mesh.getRoot();",
        )
        assert_void_crystal_knight_routing_contract(text_block_decoy)
        mutations = (
            valid.replace(
                "private static void addVoidCrystalKnightDetails(PartDefinition root) {}",
                "// private static void addVoidCrystalKnightDetails(PartDefinition root) {}",
            ),
            valid.replace(
                "addVoidCrystalKnightDetails(root);",
                "// addVoidCrystalKnightDetails(root);",
                1,
            ),
            valid.replace(
                "private static void addVoidCrystalKnightDetails(PartDefinition root) {}",
                'String declarationDecoy = "private static void addVoidCrystalKnightDetails(PartDefinition root) {}";',
            ),
            valid.replace(
                "addVoidCrystalKnightDetails(root);",
                'String callDecoy = "addVoidCrystalKnightDetails(root);";',
                1,
            ),
            valid.replace(
                "addVoidCrystalKnightDetails(root);",
                "helper.addVoidCrystalKnightDetails(root);",
                1,
            ),
            valid.replace(
                "addVoidCrystalKnightDetails(root);",
                "new Helper().addVoidCrystalKnightDetails(root);",
                1,
            ),
            valid.replace(
                "addVoidCrystalKnightDetails(root);",
                "if (false) { addVoidCrystalKnightDetails(root); }",
                1,
            ),
            valid.replace(
                "addVoidCrystalKnightDetails(root);",
                "if (enabled) { addVoidCrystalKnightDetails(root); }",
                1,
            ),
            valid.replace("addVoidCrystalKnightDetails(root);", "", 1).replace(
                "private static void addVoidCrystalKnightDetails(PartDefinition root) {}",
                "void unrelated(PartDefinition root) { addVoidCrystalKnightDetails(root); }\n"
                "            private static void addVoidCrystalKnightDetails(PartDefinition root) {}",
            ),
            valid.replace("addVoidCrystalKnightDetails(root);", "", 1).replace(
                "private static void addVoidCrystalKnightDetails(PartDefinition root) {}",
                'String textBlockDecoy = """\n'
                "                // addVoidCrystalKnightDetails(root);\n"
                "                /* private static void addVoidCrystalKnightDetails(PartDefinition root) {} */\n"
                '                builder.addVoidCrystalKnightDetails(root);\n'
                '                """;',
            ),
        )
        for mutation in mutations:
            with self.subTest(mutation=mutation), self.assertRaisesRegex(
                AssertionError, "addVoidCrystalKnightDetails"
            ):
                assert_void_crystal_knight_routing_contract(mutation)

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
        unreachable_pose = valid_method.replace(
            "((HumanoidModel) original).copyPropertiesTo((HumanoidModel) model);\n            WornTruePathArmorModel.showForType(model, getType());",
            "if (false) { ((HumanoidModel) original).copyPropertiesTo((HumanoidModel) model);\n"
            "                WornTruePathArmorModel.showForType(model, getType()); }",
        )
        with self.assertRaisesRegex(AssertionError, "getHumanoidArmorModel"):
            assert_custom_armor_model_contract(f"class Example {{ {unreachable_pose} }}", factory)

    def test_true_path_texture_contract_rejects_overbroad_routing(self):
        valid_method = '''String getArmorTexture() {
            String layer = getType() == Type.LEGGINGS ? "_layer_2.png" : "_layer_1.png";
            if (path == Path.VOID && getType() == Type.CHESTPLATE) {
                return Usless_mobs.MODID + ":textures/models/armor/true_void_chestplate_layer_1.png";
            }
            return Usless_mobs.MODID + ":textures/models/armor/" + path.key + layer;
        }'''
        assert_true_path_texture_contract(f"class Example {{ {valid_method} }}")
        for mutation in (
            valid_method.replace("path == Path.VOID && getType() == Type.CHESTPLATE", "path == Path.VOID"),
            valid_method.replace("true_void_chestplate_layer_1.png", "true_void_layer_1.png"),
            valid_method.replace("path.key + layer", '"true_void_chestplate_layer_1.png"'),
        ):
            with self.subTest(mutation=mutation), self.assertRaisesRegex(AssertionError, "dedicated atlas"):
                assert_true_path_texture_contract(f"class Example {{ {mutation} }}")


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
    ) + ("corrupted_crystal_layer_2", "true_void_chestplate_layer_1")

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

    def test_true_void_chestplate_parts_have_exact_humanoid_bone_owners(self):
        path = REPO_ROOT / "src/main/java/com/Momik/usless_mobs/client/WornTruePathArmorModel.java"
        source = path.read_text(encoding="utf-8")

        def geometry(
            owner,
            uv,
            origin,
            dimensions,
            deformation=0.04,
            pose_type="rotation",
            offset=(0.0, 0.0, 0.0),
            rotation=(0.0, 0.0, 0.0),
        ):
            return owner, uv, origin, dimensions, deformation, pose_type, offset, rotation

        expected = {
            "true_void_front_upper_left": geometry("body", (0.0, 0.0), (-4.35, 0.35, -3.18), (4.35, 2.10, 0.72), rotation=(0.0, 0.0, -0.16)),
            "true_void_front_upper_right": geometry("body", (12.0, 0.0), (0.0, 0.35, -3.18), (4.35, 2.10, 0.72), rotation=(0.0, 0.0, 0.16)),
            "true_void_front_middle_left": geometry("body", (24.0, 0.0), (-4.05, 2.65, -3.24), (4.05, 1.65, 0.76), rotation=(0.0, 0.0, -0.12)),
            "true_void_front_middle_right": geometry("body", (36.0, 0.0), (0.0, 2.65, -3.24), (4.05, 1.65, 0.76), rotation=(0.0, 0.0, 0.12)),
            "true_void_front_lower_left": geometry("body", (48.0, 0.0), (-3.55, 4.65, -3.20), (3.55, 1.45, 0.72), rotation=(0.0, 0.0, -0.09)),
            "true_void_front_lower_right": geometry("body", (58.0, 0.0), (0.0, 4.65, -3.20), (3.55, 1.45, 0.72), rotation=(0.0, 0.0, 0.09)),
            "true_void_front_tip": geometry("body", (68.0, 0.0), (-2.50, 6.45, -3.16), (5.0, 1.20, 0.68), pose_type="ZERO"),
            "true_void_chest_crystal": geometry(
                "body", (82.0, 0.0), (-1.15, -1.15, -0.41), (2.30, 2.30, 0.82),
                pose_type="offsetAndRotation", offset=(0.0, 2.50, -3.21), rotation=(0.0, 0.0, 0.7853982),
            ),
            "true_void_back_left": geometry("body", (92.0, 0.0), (-4.20, 0.55, 2.40), (4.20, 6.70, 0.70), rotation=(0.0, 0.0, -0.07)),
            "true_void_back_right": geometry("body", (104.0, 0.0), (0.0, 0.55, 2.40), (4.20, 6.70, 0.70), rotation=(0.0, 0.0, 0.07)),
            "true_void_back_crystal": geometry(
                "body", (116.0, 0.0), (-0.90, -0.90, -0.36), (1.80, 1.80, 0.72),
                pose_type="offsetAndRotation", offset=(0.0, 3.00, 3.28), rotation=(0.0, 0.0, 0.7853982),
            ),
            "true_void_right_shoulder_plate": geometry("right_arm", (0.0, 16.0), (-4.10, -2.60, -2.68), (4.10, 1.55, 5.35), 0.05, rotation=(0.0, 0.0, -0.10)),
            "true_void_right_shoulder_crystal": geometry("right_arm", (16.0, 16.0), (-3.90, -3.15, -0.62), (1.25, 1.60, 1.25), rotation=(0.0, 0.0, -0.32)),
            "true_void_left_shoulder_plate": geometry("left_arm", (24.0, 16.0), (0.0, -2.60, -2.68), (4.10, 1.55, 5.35), 0.05, rotation=(0.0, 0.0, 0.10)),
            "true_void_left_shoulder_crystal": geometry("left_arm", (40.0, 16.0), (2.65, -3.15, -0.62), (1.25, 1.60, 1.25), rotation=(0.0, 0.0, 0.32)),
        }
        actual = worn_method_geometry(source, "addVoidCrystalKnightDetails")
        self.assertEqual(expected, actual)

        mirrored_pairs = (
            ("true_void_front_upper_left", "true_void_front_upper_right"),
            ("true_void_front_middle_left", "true_void_front_middle_right"),
            ("true_void_front_lower_left", "true_void_front_lower_right"),
            ("true_void_back_left", "true_void_back_right"),
            ("true_void_right_shoulder_plate", "true_void_left_shoulder_plate"),
            ("true_void_right_shoulder_crystal", "true_void_left_shoulder_crystal"),
        )
        for negative_name, positive_name in mirrored_pairs:
            with self.subTest(mirrored_pair=(negative_name, positive_name)):
                negative = actual[negative_name]
                positive = actual[positive_name]
                self.assertAlmostEqual(-negative[2][0] - negative[3][0], positive[2][0])
                self.assertEqual(negative[2][1:], positive[2][1:])
                self.assertEqual(negative[3:5], positive[3:5])
                self.assertEqual(negative[5], positive[5])
                self.assertAlmostEqual(-negative[6][0], positive[6][0])
                self.assertEqual(negative[6][1:], positive[6][1:])
                self.assertEqual(negative[7][:2], positive[7][:2])
                self.assertAlmostEqual(-negative[7][2], positive[7][2])

        pre_change_void = {
            "true_void_rib": geometry("body", (108.0, 52.0), (-4.0, 5.6, -3.12), (8.0, 1.0, 0.8), 0.06, rotation=(0.0, 0.0, -0.20)),
            "true_void_chest_left": geometry("body", (0.0, 0.0), (-5.05, 0.15, -3.35), (4.55, 5.05, 1.05), 0.06, rotation=(0.0, 0.0, -0.04)),
            "true_void_chest_right": geometry("body", (14.0, 0.0), (0.50, 0.15, -3.35), (4.55, 5.05, 1.05), 0.06, rotation=(0.0, 0.0, 0.04)),
            "true_void_chest_keel": geometry("body", (28.0, 0.0), (-0.70, 1.40, -3.60), (1.40, 6.80, 1.30), 0.05, pose_type="ZERO"),
            "true_void_abdomen_upper": geometry("body", (36.0, 0.0), (-4.20, 5.10, -3.32), (8.40, 2.10, 1.05), 0.05, pose_type="ZERO"),
            "true_void_abdomen_lower": geometry("body", (58.0, 0.0), (-3.60, 7.20, -3.28), (7.20, 2.00, 1.00), 0.05, pose_type="ZERO"),
            "true_void_back_shell": geometry("body", (78.0, 0.0), (-4.40, 0.40, 2.28), (8.80, 8.80, 1.00), 0.05, pose_type="ZERO"),
            "true_void_right_shoulder_cap": geometry("right_arm", (100.0, 0.0), (-4.15, -3.40, -3.15), (4.35, 2.20, 6.30), 0.08, rotation=(0.0, 0.0, -0.08)),
            "true_void_left_shoulder_cap": geometry("left_arm", (0.0, 16.0), (-0.20, -3.40, -3.15), (4.35, 2.20, 6.30), 0.08, rotation=(0.0, 0.0, 0.08)),
        }
        legacy_as_pre_change = {
            name.replace("balance_void_", "true_void_", 1): value
            for name, value in worn_method_geometry(source, "addLegacyBalanceVoidDetails").items()
        }
        self.assertEqual(pre_change_void, legacy_as_pre_change)
        assert_void_crystal_knight_routing_contract(source)

    def test_true_void_chestplate_palette_and_identity(self):
        worn_path = resource_path(
            f"{NAMESPACE}:models/armor/true_void_chestplate_layer_1", "textures", ".png"
        )
        item_path = resource_path(f"{NAMESPACE}:item/true_void_chestplate", "textures", ".png")
        generator_path = REPO_ROOT / "tools/armor_graphics/build_true_void_chestplate_assets.py"
        spec = importlib.util.spec_from_file_location("true_void_chestplate_assets", generator_path)
        self.assertIsNotNone(spec)
        self.assertIsNotNone(spec.loader)
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)

        with Image.open(worn_path) as worn, Image.open(item_path) as item:
            worn.load()
            item.load()
            self.assertEqual((128, 64), worn.size)
            self.assertEqual((16, 16), item.size)
            self.assertEqual("RGBA", worn.mode)
            self.assertEqual("RGBA", item.mode)
            self.assertTrue(any(pixel[3] == 0 for pixel in item.getdata()), "item background must be transparent")
            combined = tuple(worn.getdata()) + tuple(item.getdata())
            self.assertFalse(
                any(green > 180 and red < 80 and blue < 120 and alpha for red, green, blue, alpha in combined),
                "generated concept background colours must never leak into runtime textures",
            )
            self.assertTrue(any(red < 25 and blue < 35 and alpha for red, _, blue, alpha in combined))
            self.assertTrue(any(60 <= blue <= 150 and red < 120 and alpha for red, _, blue, alpha in combined))
            self.assertTrue(any(blue > 200 and red > 130 and alpha for red, _, blue, alpha in combined))
            centre = [item.getpixel((x, y)) for y in range(5, 10) for x in range(6, 11)]
            self.assertTrue(any(red > 190 and blue > 230 and alpha for red, _, blue, alpha in centre))
            accent_colours = {
                module.VOID_HIGHLIGHT,
                module.VOID_MID,
                module.VOID_GLOW,
                module.VOID_CORE,
            }
            longest_horizontal_accent_run = 0
            for y in range(worn.height):
                run = 0
                for x in range(worn.width):
                    if worn.getpixel((x, y)) in accent_colours:
                        run += 1
                        longest_horizontal_accent_run = max(longest_horizontal_accent_run, run)
                    else:
                        run = 0
            self.assertLessEqual(longest_horizontal_accent_run, 24)

            worn_pixels = tuple(worn.getdata())
            glow_and_core_count = sum(
                pixel in {module.VOID_GLOW, module.VOID_CORE} for pixel in worn_pixels
            )
            self.assertLess(glow_and_core_count, 360)
            self.assertGreater(worn_pixels.count(module.VOID_CORE), 8)
            self.assertEqual(module.VOID_CORE, item.getpixel((8, 7)))
            self.assertEqual(module.VOID_CORE, item.getpixel((8, 8)))

        self.assertEqual(worn_path.read_bytes(), module.png_bytes(module.build_worn_texture()))
        self.assertEqual(item_path.read_bytes(), module.png_bytes(module.build_item_texture()))

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
        assert_true_path_texture_contract(true_path)
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
