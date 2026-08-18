from __future__ import annotations

import json
import math
import re
import tempfile
import unittest
import warnings
from collections import Counter
from pathlib import Path
from unittest import mock

from PIL import Image, UnidentifiedImageError


ROOT = Path(__file__).resolve().parents[3]
MOD_ITEMS = ROOT / "src/main/java/com/Momik/usless_mobs/registry/ModItems.java"
PATH_CROWN_ITEM = ROOT / "src/main/java/com/Momik/usless_mobs/item/PathCrownItem.java"
TRUE_CROWN_ITEM = ROOT / "src/main/java/com/Momik/usless_mobs/item/TrueCrownItem.java"
RECIPES = ROOT / "src/main/resources/data/usless_mobs/recipes"
MODELS = ROOT / "src/main/resources/assets/usless_mobs/models/item"
TEXTURES = ROOT / "src/main/resources/assets/usless_mobs/textures/item"
LANG = ROOT / "src/main/resources/assets/usless_mobs/lang"
CROWN_TAG = ROOT / "src/main/resources/data/curios/tags/items/crown.json"

ROYAL_IDS = {
    "royal_void_crown": "void_reaper_king",
    "royal_celestial_crown": "god_king",
    "royal_living_crown": "living_king",
    "royal_balance_crown": "true_crown",
}
ALL_IDS = set(ROYAL_IDS) | set(ROYAL_IDS.values())
VALID_FACE_DIRECTIONS = {"down", "up", "north", "south", "west", "east"}
BUILTIN_ITEM_PARENTS = {"minecraft:item/generated", "minecraft:item/handheld"}
BUILTIN_ITEM_ELEMENTS = [
    {
        "from": [0, 0, 0],
        "to": [16, 16, 0.1],
        "faces": {"north": {"texture": "#layer0"}},
    }
]
BASELINE_CROWN_TAG_VALUES = {
    "usless_mobs:king_slime_krone",
    "usless_mobs:netherite_kings_krone",
    "usless_mobs:void_reaper_king",
    "usless_mobs:god_king",
    "usless_mobs:living_king",
}


def load_json_object(path: Path, problems: list[str]) -> dict | None:
    """Load a JSON object while turning I/O/schema problems into test diagnostics."""
    try:
        exact_path = exact_case_file(path, ROOT)
        if exact_path is None:
            problems.append(f"missing exact-case JSON file: {path.relative_to(ROOT)}")
            return None
        document = json.loads(exact_path.read_text(encoding="utf-8"))
    except FileNotFoundError:
        problems.append(f"missing JSON file: {path.relative_to(ROOT)}")
        return None
    except (OSError, UnicodeError) as error:
        problems.append(f"cannot read {path.relative_to(ROOT)}: {error}")
        return None
    except json.JSONDecodeError as error:
        problems.append(
            f"invalid JSON in {path.relative_to(ROOT)} at line {error.lineno}, "
            f"column {error.colno}: {error.msg}"
        )
        return None
    if not isinstance(document, dict):
        problems.append(
            f"{path.relative_to(ROOT)} must contain a JSON object, got "
            f"{type(document).__name__}"
        )
        return None
    return document


def java_tokens(source: str):
    """Yield identifiers, punctuation, and decoded strings outside Java comments."""
    index = 0
    length = len(source)
    while index < length:
        char = source[index]
        if char.isspace():
            index += 1
            continue
        if source.startswith("//", index):
            newline = source.find("\n", index + 2)
            index = length if newline < 0 else newline + 1
            continue
        if source.startswith("/*", index):
            end = source.find("*/", index + 2)
            index = length if end < 0 else end + 2
            continue
        if char == '"':
            start = index
            index += 1
            escaped = False
            while index < length:
                current = source[index]
                index += 1
                if escaped:
                    escaped = False
                elif current == "\\":
                    escaped = True
                elif current == '"':
                    break
            literal = source[start:index]
            try:
                yield ("string", json.loads(literal))
            except json.JSONDecodeError:
                yield ("invalid_string", literal)
            continue
        if char.isalpha() or char in "_$":
            start = index
            index += 1
            while index < length and (source[index].isalnum() or source[index] in "_$"):
                index += 1
            yield ("identifier", source[start:index])
            continue
        yield ("punctuation", char)
        index += 1


def legacy_constructor_delegates_to_combat(source: str, class_name: str) -> bool:
    """Return whether the public legacy constructor delegates to the combat form."""
    expected = [
        ("identifier", "public"),
        ("identifier", class_name),
        ("punctuation", "("),
        ("identifier", "Path"),
        ("identifier", "path"),
        ("punctuation", ","),
        ("identifier", "Properties"),
        ("identifier", "properties"),
        ("punctuation", ")"),
        ("punctuation", "{"),
        ("identifier", "this"),
        ("punctuation", "("),
        ("identifier", "path"),
        ("punctuation", ","),
        ("identifier", "CrownForm"),
        ("punctuation", "."),
        ("identifier", "COMBAT"),
        ("punctuation", ","),
        ("identifier", "properties"),
        ("punctuation", ")"),
        ("punctuation", ";"),
        ("punctuation", "}"),
    ]
    tokens = list(java_tokens(source))
    return any(
        tokens[index : index + len(expected)] == expected
        for index in range(len(tokens) - len(expected) + 1)
    )


def registered_item_ids(source: str) -> list[str]:
    tokens = list(java_tokens(source))
    depths = []
    depth = 0
    for token in tokens:
        depths.append(depth)
        if token == ("punctuation", "{"):
            depth += 1
        elif token == ("punctuation", "}"):
            depth -= 1

    class_open = None
    for index in range(len(tokens) - 2):
        if tokens[index][0:2] == ("identifier", "class"):
            for candidate in range(index + 1, len(tokens)):
                if tokens[candidate] == ("punctuation", "{"):
                    class_open = candidate
                    break
            break
    if class_open is None:
        return []

    class_depth = depths[class_open] + 1
    registrations = []
    for index in range(class_open + 1, len(tokens) - 14):
        if depths[index] != class_depth:
            continue
        window = tokens[index : index + 15]
        if (
            window[0] == ("identifier", "public")
            and window[1] == ("identifier", "static")
            and window[2] == ("identifier", "final")
            and window[3] == ("identifier", "RegistryObject")
            and window[4] == ("punctuation", "<")
            and window[5] == ("identifier", "Item")
            and window[6] == ("punctuation", ">")
            and window[7][0] == "identifier"
            and window[8] == ("punctuation", "=")
            and window[9] == ("identifier", "ITEMS")
            and window[10] == ("punctuation", ".")
            and window[11] == ("identifier", "register")
            and window[12] == ("punctuation", "(")
            and window[13][0] == "string"
        ):
            field_name = window[7][1]
            item_id = window[13][1]
            if field_name == item_id.upper():
                registrations.append(item_id)
    return registrations


def all_registered_royal_ids(source: str) -> list[str]:
    tokens = list(java_tokens(source))
    registrations = []
    for index in range(len(tokens) - 4):
        window = tokens[index : index + 5]
        if (
            window[0] == ("identifier", "ITEMS")
            and window[1] == ("punctuation", ".")
            and window[2] == ("identifier", "register")
            and window[3] == ("punctuation", "(")
            and window[4][0] == "string"
            and window[4][1].startswith("royal_")
        ):
            registrations.append(window[4][1])
    return registrations


def royal_registration_problems(source: str) -> list[str]:
    problems = []
    expected = set(ROYAL_IDS)
    canonical_counts = Counter(registered_item_ids(source))
    canonical_royal_counts = Counter(
        {
            item_id: count
            for item_id, count in canonical_counts.items()
            if item_id.startswith("royal_")
        }
    )
    global_counts = Counter(all_registered_royal_ids(source))
    actual_canonical = set(canonical_royal_counts)
    missing = sorted(expected - actual_canonical)
    unexpected = sorted(actual_canonical - expected)
    wrong_canonical_counts = {
        item_id: canonical_royal_counts[item_id]
        for item_id in expected
        if canonical_counts[item_id] != 1
    }
    if missing:
        problems.append(f"missing Royal canonical fields: {missing}")
    if unexpected:
        problems.append(f"unexpected canonical royal_ fields: {unexpected}")
    if wrong_canonical_counts or sum(canonical_royal_counts.values()) != 4:
        problems.append(
            "each Royal ID must have exactly one canonical public static final field; "
            f"observed {dict(sorted(canonical_royal_counts.items()))}"
        )

    unexpected_global = sorted(set(global_counts) - expected)
    wrong_global_counts = {
        item_id: global_counts[item_id]
        for item_id in expected
        if global_counts[item_id] != 1
    }
    if unexpected_global:
        problems.append(
            f"unexpected executable royal_ registrations anywhere in source: {unexpected_global}"
        )
    if wrong_global_counts or sum(global_counts.values()) != 4:
        problems.append(
            "source must contain exactly four executable Royal registrations globally; "
            f"observed {dict(sorted(global_counts.items()))}"
        )
    return problems


RESOURCE_LOCATION = re.compile(r"[a-z0-9_.-]+:[a-z0-9_.\-/]+")
TEXTURE_ALIAS = re.compile(r"[a-z0-9_.-]+")
ALLOWED_BUILTIN_FACE_TEXTURES = frozenset()


def resource_location_parts(value: str) -> tuple[str, str] | None:
    if not isinstance(value, str) or not RESOURCE_LOCATION.fullmatch(value):
        return None
    namespace, resource_path = value.split(":", 1)
    segments = resource_path.split("/")
    if not segments or any(segment in {"", ".", ".."} for segment in segments):
        return None
    return namespace, resource_path


def display_path(path: Path) -> str:
    try:
        return str(path.relative_to(ROOT))
    except ValueError:
        return str(path)


def resolved_within(path: Path, root: Path) -> bool:
    try:
        path.resolve(strict=True).relative_to(root.resolve(strict=True))
        return True
    except (OSError, ValueError):
        return False


def exact_case_file(path: Path, root: Path) -> Path | None:
    try:
        relative = path.relative_to(root)
    except ValueError:
        return None
    current = root
    for component in relative.parts:
        if not current.is_dir():
            return None
        matches = [candidate for candidate in current.iterdir() if candidate.name == component]
        if len(matches) != 1:
            return None
        current = matches[0]
    if not current.is_file() or not resolved_within(current, root):
        return None
    return current


def exact_case_directory(path: Path, root: Path) -> Path | None:
    try:
        relative = path.relative_to(root)
    except ValueError:
        return None
    current = root
    for component in relative.parts:
        if not current.is_dir():
            return None
        matches = [candidate for candidate in current.iterdir() if candidate.name == component]
        if len(matches) != 1:
            return None
        current = matches[0]
    if not current.is_dir() or not resolved_within(current, root):
        return None
    return current


def exact_asset_file(assets_root: Path, parts: tuple[str, ...]) -> Path | None:
    current = assets_root
    for index, part in enumerate(parts):
        if not current.is_dir():
            return None
        matches = [path for path in current.iterdir() if path.name == part]
        if len(matches) != 1:
            return None
        current = matches[0]
        if index < len(parts) - 1 and not current.is_dir():
            return None
    if not current.is_file() or not resolved_within(current, assets_root):
        return None
    return current


def png_decode_problem(path: Path) -> str | None:
    try:
        with warnings.catch_warnings():
            warnings.simplefilter("error", Image.DecompressionBombWarning)
            with Image.open(path) as image:
                image.load()
                if image.format != "PNG":
                    return f"must be PNG, got {image.format!r}"
    except Exception as error:
        return f"is not a decodable PNG: {type(error).__name__}: {error}"
    return None


def item_model_problems(model_path: Path, item_id: str) -> list[str]:
    problems = []

    def report(message: str) -> None:
        if message not in problems:
            problems.append(message)

    assets_root = next((parent for parent in model_path.parents if parent.name == "assets"), None)
    if assets_root is None:
        return [f"{display_path(model_path)} is not below an assets directory"]

    resolved_assets_root = assets_root.resolve()
    if not resolved_within(model_path, assets_root):
        return [f"model {display_path(model_path)} resolves outside its assets root"]
    cache = {}

    def read_model(path: Path) -> dict | None:
        try:
            document = json.loads(path.read_text(encoding="utf-8"))
        except FileNotFoundError:
            report(f"missing parent model: {display_path(path)}")
            return None
        except (OSError, UnicodeError) as error:
            report(f"cannot read model {display_path(path)}: {error}")
            return None
        except json.JSONDecodeError as error:
            report(
                f"invalid JSON in model {display_path(path)} at line {error.lineno}, "
                f"column {error.colno}: {error.msg}"
            )
            return None
        if not isinstance(document, dict):
            report(f"model {display_path(path)} must contain a JSON object")
            return None
        return document

    def local_parent_path(resource: str, owner: Path) -> Path | None:
        parts = resource_location_parts(resource)
        if parts is None:
            report(f"{display_path(owner)} has invalid parent ResourceLocation {resource!r}")
            return None
        namespace, resource_path = parts
        parent_path = assets_root / namespace / "models" / f"{resource_path}.json"
        try:
            parent_path.resolve().relative_to(resolved_assets_root)
        except ValueError:
            report(f"{display_path(owner)} parent escapes assets root: {resource!r}")
            return None
        exact_matches = exact_resource_matches(parent_path.parent, parent_path.stem, ".json")
        if len(exact_matches) != 1:
            report(
                f"{display_path(owner)} parent {resource!r} must resolve to exactly one "
                f"repo-local model at {display_path(parent_path)}; found {len(exact_matches)}"
            )
            return None
        if not resolved_within(exact_matches[0], assets_root):
            report(f"{display_path(owner)} parent resolves outside its assets root")
            return None
        return exact_matches[0]

    def effective_model(path: Path, stack: tuple[Path, ...]):
        canonical = path.resolve()
        if canonical in stack:
            cycle = " -> ".join(display_path(entry) for entry in (*stack, canonical))
            report(f"model parent cycle detected: {cycle}")
            return {}, None
        if canonical in cache:
            return cache[canonical]

        document = read_model(path)
        if document is None:
            return {}, None
        parent = document.get("parent")
        has_parent = isinstance(parent, str) and bool(parent.strip())
        owns_elements = "elements" in document
        if not has_parent and not owns_elements:
            report(
                f"{display_path(path)} must define a non-empty parent and/or elements"
            )

        inherited_textures = {}
        inherited_elements = None
        if "parent" in document:
            if not has_parent:
                report(f"{display_path(path)} parent must be a non-empty ResourceLocation")
            else:
                if parent in BUILTIN_ITEM_PARENTS:
                    inherited_elements = BUILTIN_ITEM_ELEMENTS
                else:
                    parent_path = local_parent_path(parent, path)
                    if parent_path is not None:
                        inherited_textures, inherited_elements = effective_model(
                            parent_path, (*stack, canonical)
                        )

        textures = document.get("textures", {})
        if not isinstance(textures, dict):
            report(f"{display_path(path)} textures must be a JSON object")
            textures = {}
        valid_textures = {}
        for alias, value in textures.items():
            if not isinstance(alias, str) or not TEXTURE_ALIAS.fullmatch(alias):
                report(f"{display_path(path)} has invalid texture alias {alias!r}")
                continue
            if not isinstance(value, str) or not value:
                report(f"{display_path(path)} texture {alias!r} must be a non-empty string")
                continue
            valid_textures[alias] = value

        combined_textures = dict(inherited_textures)
        combined_textures.update(valid_textures)
        elements = document.get("elements") if owns_elements else inherited_elements
        if owns_elements and (not isinstance(elements, list) or not elements):
            report(f"{display_path(path)} elements must be a non-empty list when present")
        result = combined_textures, elements
        cache[canonical] = result
        return result

    texture_mapping, elements = effective_model(model_path, ())
    resolved_mapping = {}

    def resolve_texture(reference: str, aliases: tuple[str, ...] = ()) -> str | None:
        if reference.startswith("#"):
            alias = reference[1:]
            if not TEXTURE_ALIAS.fullmatch(alias):
                report(f"invalid texture alias reference {reference!r}")
                return None
            if alias in aliases:
                report(f"texture alias cycle detected: {' -> '.join((*aliases, alias))}")
                return None
            if alias not in texture_mapping:
                report(f"unresolved texture alias {reference!r}")
                return None
            return resolve_texture(texture_mapping[alias], (*aliases, alias))
        if resource_location_parts(reference) is None:
            report(f"invalid texture ResourceLocation {reference!r}")
            return None
        return reference

    for alias in sorted(texture_mapping):
        resolved_mapping[alias] = resolve_texture(f"#{alias}")

    resolved_faces = []
    if elements is BUILTIN_ITEM_ELEMENTS:
        layer_aliases = sorted(
            (
                alias
                for alias in texture_mapping
                if re.fullmatch(r"layer[0-9]+", alias)
            ),
            key=lambda alias: int(alias[5:]),
        )
        if not layer_aliases:
            report("builtin item parent requires at least one layerN texture")
        for alias in layer_aliases:
            resolved = resolve_texture(f"#{alias}")
            if resolved is not None:
                resolved_faces.append(resolved)
    elif not isinstance(elements, list) or not elements:
        report("effective model must provide non-empty elements with textured faces")
    else:
        for element_index, element in enumerate(elements):
            if not isinstance(element, dict):
                report(f"elements[{element_index}] must be a JSON object")
                continue
            coordinates_by_corner = {}
            for corner in ("from", "to"):
                coordinates = element.get(corner)
                if not (
                    isinstance(coordinates, list)
                    and len(coordinates) == 3
                    and all(
                        isinstance(value, (int, float))
                        and not isinstance(value, bool)
                        and math.isfinite(float(value))
                        for value in coordinates
                    )
                ):
                    report(
                        f"elements[{element_index}].{corner} must contain three finite numbers"
                    )
                else:
                    coordinates_by_corner[corner] = coordinates
            faces = element.get("faces")
            if not isinstance(faces, dict) or not faces:
                report(f"elements[{element_index}].faces must be a non-empty object")
                continue
            for face_name, face in faces.items():
                if face_name not in VALID_FACE_DIRECTIONS:
                    report(
                        f"elements[{element_index}].faces has invalid direction {face_name!r}"
                    )
                    continue
                if len(coordinates_by_corner) != 2:
                    continue
                plane_axes = {
                    "north": (0, 1),
                    "south": (0, 1),
                    "west": (2, 1),
                    "east": (2, 1),
                    "down": (0, 2),
                    "up": (0, 2),
                }[face_name]
                start = coordinates_by_corner["from"]
                end = coordinates_by_corner["to"]
                dimensions = tuple(end[axis] - start[axis] for axis in plane_axes)
                if not all(dimension > 0 for dimension in dimensions):
                    report(
                        f"elements[{element_index}].faces.{face_name} is degenerate; "
                        f"plane dimensions must be strictly positive, got {dimensions}"
                    )
                    continue
                if not isinstance(face, dict):
                    report(f"elements[{element_index}].faces.{face_name} must be an object")
                    continue
                texture = face.get("texture")
                if not isinstance(texture, str) or not texture:
                    report(
                        f"elements[{element_index}].faces.{face_name}.texture must be non-empty"
                    )
                    continue
                resolved = resolve_texture(texture)
                if resolved is not None:
                    resolved_faces.append(resolved)

    expected_texture = f"usless_mobs:item/{item_id}"
    if not resolved_faces:
        report("effective model must contain at least one renderable valid-direction face")
    if expected_texture not in resolved_faces:
        report(
            f"effective faces must reference own texture {expected_texture!r}; "
            f"resolved {sorted(set(resolved_faces))!r}"
        )
    for texture_resource in sorted(set(resolved_faces)):
        parts = resource_location_parts(texture_resource)
        if parts is None:
            continue
        namespace, texture_path = parts
        local_texture = exact_asset_file(
            assets_root,
            (
                namespace,
                "textures",
                *texture_path.split("/")[:-1],
                f"{texture_path.split('/')[-1]}.png",
            ),
        )
        if local_texture is None:
            if (
                namespace == "minecraft"
                and texture_resource in ALLOWED_BUILTIN_FACE_TEXTURES
            ):
                continue
            report(
                f"active face texture {texture_resource!r} must resolve to an exactly "
                "lowercase repo-local PNG"
            )
            continue
        if not resolved_within(local_texture, assets_root):
            report(
                f"active face texture {texture_resource!r} resolves outside its assets root"
            )
            continue
        decode_problem = png_decode_problem(local_texture)
        if decode_problem is not None:
            report(f"active face texture {display_path(local_texture)} {decode_problem}")
    return problems


def exact_resource_matches(directory: Path, item_id: str, extension: str) -> list[Path]:
    try:
        directory.relative_to(ROOT)
        anchor = ROOT
    except ValueError:
        anchor = next(
            (candidate for candidate in (directory, *directory.parents) if candidate.name == "assets"),
            directory,
        )
    exact_directory = exact_case_directory(directory, anchor)
    if exact_directory is None:
        return []
    expected_name = f"{item_id}{extension}"
    return [
        path
        for path in exact_directory.iterdir()
        if path.is_file()
        and path.name == expected_name
        and resolved_within(path, anchor)
    ]


def append_mismatch(
    problems: list[str], path: Path, field: str, actual, expected
) -> None:
    if actual != expected:
        problems.append(
            f"{path.relative_to(ROOT)}: {field} must be {expected!r}, got {actual!r}"
        )


class CurioCrownContract(unittest.TestCase):
    def assert_no_problems(self, problems: list[str]) -> None:
        self.assertFalse(problems, "\n- " + "\n- ".join(problems))

    def test_java_crown_contracts(self):
        try:
            source = MOD_ITEMS.read_text(encoding="utf-8")
        except (OSError, UnicodeError) as error:
            self.fail(f"cannot read {MOD_ITEMS.relative_to(ROOT)}: {error}")
        self.assert_no_problems(royal_registration_problems(source))
        for path, class_name in (
            (PATH_CROWN_ITEM, "PathCrownItem"),
            (TRUE_CROWN_ITEM, "TrueCrownItem"),
        ):
            with self.subTest(class_name=class_name):
                try:
                    crown_source = path.read_text(encoding="utf-8")
                except (OSError, UnicodeError) as error:
                    self.fail(f"cannot read {path.relative_to(ROOT)}: {error}")
                self.assertTrue(
                    legacy_constructor_delegates_to_combat(crown_source, class_name),
                    f"{path.relative_to(ROOT)} must preserve the public "
                    f"{class_name}(Path, Properties) constructor and delegate it "
                    "to CrownForm.COMBAT",
                )

    def test_upgrade_recipes_use_exact_nine_slot_pattern(self):
        problems = []
        for royal, combat in ROYAL_IDS.items():
            path = RECIPES / f"{royal}.json"
            recipe = load_json_object(path, problems)
            if recipe is None:
                continue
            append_mismatch(problems, path, "type", recipe.get("type"), "minecraft:crafting_shaped")
            append_mismatch(problems, path, "pattern", recipe.get("pattern"), ["DDD", "DCD", "DND"])
            key = recipe.get("key")
            if not isinstance(key, dict):
                problems.append(f"{path.relative_to(ROOT)}: key must be a JSON object")
            else:
                append_mismatch(problems, path, "key symbols", set(key), {"D", "C", "N"})
                for symbol, expected_item in {
                    "D": "minecraft:diamond",
                    "C": f"usless_mobs:{combat}",
                    "N": "minecraft:netherite_ingot",
                }.items():
                    append_mismatch(
                        problems,
                        path,
                        f"key.{symbol}",
                        key.get(symbol),
                        {"item": expected_item},
                    )
            append_mismatch(
                problems,
                path,
                "result",
                recipe.get("result"),
                {"item": f"usless_mobs:{royal}", "count": 1},
            )
        self.assert_no_problems(problems)

    def test_reversion_recipes_are_exactly_one_item_shapeless(self):
        problems = []
        for royal, combat in ROYAL_IDS.items():
            path = RECIPES / f"{royal}_combat.json"
            recipe = load_json_object(path, problems)
            if recipe is None:
                continue
            append_mismatch(problems, path, "type", recipe.get("type"), "minecraft:crafting_shapeless")
            append_mismatch(
                problems,
                path,
                "ingredients",
                recipe.get("ingredients"),
                [{"item": f"usless_mobs:{royal}"}],
            )
            append_mismatch(
                problems,
                path,
                "result",
                recipe.get("result"),
                {"item": f"usless_mobs:{combat}", "count": 1},
            )
        self.assert_no_problems(problems)

    def test_curios_crown_tag_contains_all_eight_path_and_balance_ids(self):
        problems = []
        tag = load_json_object(CROWN_TAG, problems)
        if tag is not None:
            if tag.get("replace", False) is not False:
                problems.append(
                    f"{CROWN_TAG.relative_to(ROOT)}: replace must be false or omitted"
                )
            values = tag.get("values")
            if not isinstance(values, list) or not all(isinstance(value, str) for value in values):
                problems.append(
                    f"{CROWN_TAG.relative_to(ROOT)}: values must be a list of item-ID strings"
                )
            else:
                expected = {f"usless_mobs:{item_id}" for item_id in ALL_IDS}
                value_set = set(values)
                missing_contract = sorted(expected - value_set)
                missing_baseline = sorted(BASELINE_CROWN_TAG_VALUES - value_set)
                if missing_contract:
                    problems.append(
                        f"{CROWN_TAG.relative_to(ROOT)}: missing crown contract values: "
                        f"{missing_contract}"
                    )
                if missing_baseline:
                    problems.append(
                        f"{CROWN_TAG.relative_to(ROOT)}: removed baseline crown values: "
                        f"{missing_baseline}"
                    )
        self.assert_no_problems(problems)

    def test_every_crown_has_exactly_one_valid_item_model(self):
        problems = []
        for item_id in sorted(ALL_IDS):
            matches = exact_resource_matches(MODELS, item_id, ".json")
            if len(matches) != 1:
                problems.append(
                    f"expected exactly one model at "
                    f"{(MODELS / f'{item_id}.json').relative_to(ROOT)}, found {len(matches)}"
                )
                continue
            problems.extend(item_model_problems(matches[0], item_id))
        self.assert_no_problems(problems)

    def test_every_crown_has_exactly_one_64x64_rgba_texture(self):
        problems = []
        for item_id in sorted(ALL_IDS):
            expected = TEXTURES / f"{item_id}.png"
            matches = exact_resource_matches(TEXTURES, item_id, ".png")
            if len(matches) != 1:
                problems.append(
                    f"expected exactly one texture at {expected.relative_to(ROOT)}, found {len(matches)}"
                )
                continue
            decode_problem = png_decode_problem(matches[0])
            if decode_problem is not None:
                problems.append(
                    f"invalid texture {matches[0].relative_to(ROOT)}: {decode_problem}"
                )
                continue
            try:
                with warnings.catch_warnings():
                    warnings.simplefilter("error", Image.DecompressionBombWarning)
                    with Image.open(matches[0]) as image:
                        if image.mode != "RGBA":
                            problems.append(
                                f"{matches[0].relative_to(ROOT)} must use RGBA mode, "
                                f"got {image.mode!r}"
                            )
                        if image.size != (64, 64):
                            problems.append(
                                f"{matches[0].relative_to(ROOT)} must be 64x64, got {image.size}"
                            )
            except Exception as error:
                problems.append(
                    f"invalid texture {matches[0].relative_to(ROOT)} during metadata read: "
                    f"{type(error).__name__}: {error}"
                )
        self.assert_no_problems(problems)

    def test_english_and_german_define_every_crown_translation_key(self):
        problems = []
        for locale in ("en_us", "de_de"):
            path = LANG / f"{locale}.json"
            translations = load_json_object(path, problems)
            if translations is None:
                continue
            for item_id in sorted(ALL_IDS):
                key = f"item.usless_mobs.{item_id}"
                value = translations.get(key)
                if not isinstance(value, str) or not value.strip():
                    problems.append(
                        f"{path.relative_to(ROOT)}: missing non-empty translation for {key}"
                    )
        self.assert_no_problems(problems)


class CrownContractParserRegression(unittest.TestCase):
    def validate_fixture(
        self,
        model: dict,
        parents: dict[str, dict] | None = None,
        texture_payloads: dict[str, bytes] | None = None,
    ):
        with tempfile.TemporaryDirectory() as directory:
            assets = Path(directory) / "assets"
            model_path = assets / "usless_mobs/models/item/test_crown.json"
            model_path.parent.mkdir(parents=True)
            model_path.write_text(json.dumps(model), encoding="utf-8")
            for resource, document in (parents or {}).items():
                namespace, resource_path = resource.split(":", 1)
                parent_path = assets / namespace / "models" / f"{resource_path}.json"
                parent_path.parent.mkdir(parents=True, exist_ok=True)
                parent_path.write_text(json.dumps(document), encoding="utf-8")
            own_texture = assets / "usless_mobs/textures/item/test_crown.png"
            own_texture.parent.mkdir(parents=True, exist_ok=True)
            Image.new("RGBA", (2, 2), (255, 255, 255, 255)).save(own_texture)
            for resource, payload in (texture_payloads or {}).items():
                namespace, resource_path = resource.split(":", 1)
                texture_path = assets / namespace / "textures" / f"{resource_path}.png"
                texture_path.parent.mkdir(parents=True, exist_ok=True)
                texture_path.write_bytes(payload)
            return item_model_problems(model_path, "test_crown")

    def test_registry_parser_rejects_method_local_and_unassigned_decoys(self):
        source = r'''
            public final class ModItems {
                String text = "ITEMS.register(\"royal_string_decoy\", () -> create())";
                // ITEMS.register("royal_comment_decoy", () -> create());
                public static final RegistryObject<Item> ROYAL_VOID_CROWN =
                    ITEMS.register("royal_void_crown", () -> create());

                static {
                    ITEMS.register("royal_celestial_crown", () -> create());
                }

                public static void addDecoys() {
                    RegistryObject<Item> local =
                        ITEMS.register("royal_living_crown", () -> create());
                }
            }
        '''
        self.assertEqual(["royal_void_crown"], registered_item_ids(source))
        self.assertEqual(
            ["royal_void_crown", "royal_celestial_crown", "royal_living_crown"],
            all_registered_royal_ids(source),
        )

    def test_registry_parser_requires_public_static_final_registry_object_item(self):
        source = r'''
            public final class ModItems {
                private static final RegistryObject<Item> PRIVATE_CROWN =
                    ITEMS.register("royal_void_crown", () -> create());
                public final RegistryObject<Item> INSTANCE_CROWN =
                    ITEMS.register("royal_celestial_crown", () -> create());
                public static final RegistryObject<Block> WRONG_REGISTRY =
                    ITEMS.register("royal_living_crown", () -> create());
                public static final RegistryObject<Item> UNUSED =
                    ITEMS.register("royal_void_crown", () -> create());
                public static final RegistryObject<Item> ROYAL_BALANCE_CROWN =
                    ITEMS.register("royal_balance_crown", () -> create());
            }
        '''
        self.assertEqual(["royal_balance_crown"], registered_item_ids(source))

    def test_registry_contract_rejects_extra_global_royal_registration(self):
        fields = "\n".join(
            f"public static final RegistryObject<Item> {item_id.upper()} = "
            f'ITEMS.register("{item_id}", () -> create());'
            for item_id in ROYAL_IDS
        )
        source = f'''
            public final class ModItems {{
                {fields}
                static {{
                    ITEMS.register("royal_bonus_crown", () -> create());
                }}
                // ITEMS.register("royal_comment_crown", () -> create());
                String decoy = "ITEMS.register(\\\"royal_string_crown\\\", ignored)";
            }}
        '''
        problems = royal_registration_problems(source)
        self.assertTrue(any("royal_bonus_crown" in problem for problem in problems))
        self.assertFalse(any("comment_crown" in problem for problem in problems))
        self.assertFalse(any("string_crown" in problem for problem in problems))

    def test_model_validator_rejects_empty_and_untextured_template_models(self):
        self.assertTrue(self.validate_fixture({}))
        self.assertTrue(self.validate_fixture({"elements": []}))
        self.assertTrue(self.validate_fixture({"parent": "minecraft:item/generated"}))
        self.assertTrue(
            self.validate_fixture(
                {"textures": {"main": "usless_mobs:item/crown"}, "elements": [{}]}
            )
        )

    def test_model_validator_rejects_missing_parent_model(self):
        self.assertTrue(
            self.validate_fixture({"parent": "usless_mobs:item/does_not_exist"})
        )

    def test_model_validator_rejects_invalid_parent_resource_location(self):
        self.assertTrue(self.validate_fixture({"parent": "Not A Resource Location"}))

    def test_model_validator_rejects_wrong_crown_texture(self):
        self.assertTrue(
            self.validate_fixture(
                {
                    "textures": {"main": "usless_mobs:item/wrong_crown"},
                    "elements": [
                        {
                            "from": [1, 2, 3],
                            "to": [4, 5, 6],
                            "faces": {"north": {"texture": "#main"}},
                        }
                    ],
                }
            )
        )

    def test_model_validator_rejects_missing_secondary_face_texture(self):
        self.assertTrue(
            self.validate_fixture(
                {
                    "textures": {
                        "main": "usless_mobs:item/test_crown",
                        "missing": "usless_mobs:item/does_not_exist",
                    },
                    "elements": [
                        {
                            "from": [1, 2, 3],
                            "to": [4, 5, 6],
                            "faces": {
                                "north": {"texture": "#main"},
                                "south": {"texture": "#missing"},
                            },
                        }
                    ],
                }
            )
        )

    def test_model_validator_rejects_invalid_face_direction(self):
        model = {
            "textures": {"main": "usless_mobs:item/test_crown"},
            "elements": [
                {
                    "from": [1, 2, 3],
                    "to": [4, 5, 6],
                    "faces": {"banana": {"texture": "#main"}},
                }
            ],
        }
        self.assertTrue(self.validate_fixture(model))

    def test_model_validator_rejects_degenerate_north_face(self):
        model = {
            "textures": {"main": "usless_mobs:item/test_crown"},
            "elements": [
                {
                    "from": [4, 4, 4],
                    "to": [4, 4, 4],
                    "faces": {"north": {"texture": "#main"}},
                }
            ],
        }
        self.assertTrue(self.validate_fixture(model))
        partially_degenerate = {
            "textures": {"main": "usless_mobs:item/test_crown"},
            "elements": [
                {
                    "from": [4, 1, 1],
                    "to": [4, 8, 6],
                    "faces": {"north": {"texture": "#main"}},
                }
            ],
        }
        self.assertTrue(self.validate_fixture(partially_degenerate))

    def test_model_validator_accepts_vanilla_generated_and_handheld_parents(self):
        for parent in ("minecraft:item/generated", "minecraft:item/handheld"):
            with self.subTest(parent=parent):
                self.assertEqual(
                    [],
                    self.validate_fixture(
                        {
                            "parent": parent,
                            "textures": {"layer0": "usless_mobs:item/test_crown"},
                        }
                    ),
                )
        self.assertEqual(
            [],
            self.validate_fixture(
                {
                    "parent": "minecraft:item/generated",
                    "textures": {
                        "layer0": "usless_mobs:item/test_crown",
                        "layer3": "usless_mobs:item/test_crown",
                    },
                }
            ),
        )

    def test_builtin_parent_validates_every_present_layer_texture(self):
        model = {
            "parent": "minecraft:item/generated",
            "textures": {
                "layer0": "usless_mobs:item/test_crown",
                "layer1": "usless_mobs:item/missing_overlay",
            },
        }
        self.assertTrue(self.validate_fixture(model))

    def test_model_validator_rejects_undecodable_active_texture(self):
        model = {
            "textures": {
                "main": "usless_mobs:item/test_crown",
                "corrupt": "usless_mobs:item/corrupt",
            },
            "elements": [
                {
                    "from": [1, 2, 3],
                    "to": [4, 5, 6],
                    "faces": {
                        "north": {"texture": "#main"},
                        "south": {"texture": "#corrupt"},
                    },
                }
            ],
        }
        self.assertTrue(
            self.validate_fixture(
                model,
                texture_payloads={"usless_mobs:item/corrupt": b"not a png"},
            )
        )

    def test_model_validator_rejects_missing_texture_alias(self):
        self.assertTrue(
            self.validate_fixture(
                {
                    "textures": {"main": "#missing"},
                    "elements": [
                        {
                            "from": [1, 2, 3],
                            "to": [4, 5, 6],
                            "faces": {"north": {"texture": "#main"}},
                        }
                    ],
                }
            )
        )

    def test_model_validator_accepts_generated_elements_templates_and_inheritance(self):
        direct = {
                "textures": {"main": "usless_mobs:item/test_crown"},
                "elements": [
                    {
                        "from": [1, 2, 3],
                        "to": [4, 5, 6],
                        "faces": {"north": {"texture": "#main"}},
                    }
                ],
            }
        shared_geometry = {
            "textures": {"main": "usless_mobs:item/parent_default"},
            "elements": direct["elements"],
        }
        inherited = {
            "parent": "usless_mobs:item/shared_crown_geometry",
            "textures": {"main": "usless_mobs:item/test_crown"},
        }
        local_template = {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": "usless_mobs:item/test_crown"},
        }
        generated_template = {
            "elements": [
                {
                    "from": [0, 0, 0],
                    "to": [16, 16, 0.1],
                    "faces": {"north": {"texture": "#layer0"}},
                }
            ]
        }
        self.assertEqual([], self.validate_fixture(direct))
        self.assertEqual(
            [],
            self.validate_fixture(
                inherited,
                {"usless_mobs:item/shared_crown_geometry": shared_geometry},
            ),
        )
        self.assertEqual(
            [],
            self.validate_fixture(
                local_template, {"minecraft:item/generated": generated_template}
            ),
        )

    def test_model_validator_detects_parent_and_alias_cycles(self):
        self.assertTrue(
            self.validate_fixture(
                {"parent": "usless_mobs:item/parent_a"},
                {
                    "usless_mobs:item/parent_a": {
                        "parent": "usless_mobs:item/test_crown"
                    }
                },
            )
        )
        cyclic_alias = {
            "textures": {"a": "#b", "b": "#a"},
            "elements": [
                {
                    "from": [1, 2, 3],
                    "to": [4, 5, 6],
                    "faces": {"north": {"texture": "#a"}},
                }
            ],
        }
        self.assertTrue(self.validate_fixture(cyclic_alias))

    def test_resource_matcher_rejects_uppercase_extensions(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            uppercase = root / "royal_void_crown.JSON"
            uppercase.write_text("{}", encoding="utf-8")
            self.assertEqual([], exact_resource_matches(root, "royal_void_crown", ".json"))
            uppercase.unlink()
            lowercase = root / "royal_void_crown.json"
            lowercase.write_text("{}", encoding="utf-8")
            self.assertEqual(
                [lowercase], exact_resource_matches(root, "royal_void_crown", ".json")
            )

    def test_exact_path_checks_reject_uppercase_directory_components(self):
        with tempfile.TemporaryDirectory() as directory:
            assets = Path(directory) / "assets"
            wrong_case = assets / "usless_mobs/Models/item"
            wrong_case.mkdir(parents=True)
            (wrong_case / "royal_void_crown.json").write_text("{}", encoding="utf-8")
            expected = assets / "usless_mobs/models/item"
            self.assertEqual(
                [], exact_resource_matches(expected, "royal_void_crown", ".json")
            )

    def test_resolved_containment_rejects_outside_model_and_texture_targets(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            assets = root / "assets"
            assets.mkdir()
            outside = root / "outside.png"
            Image.new("RGBA", (2, 2), (255, 255, 255, 255)).save(outside)
            self.assertFalse(resolved_within(outside, assets))

            model_path = assets / "usless_mobs/models/item/test_crown.json"
            model_path.parent.mkdir(parents=True)
            model_path.write_text(
                json.dumps(
                    {
                        "textures": {"main": "usless_mobs:item/test_crown"},
                        "elements": [
                            {
                                "from": [1, 2, 3],
                                "to": [4, 5, 6],
                                "faces": {"north": {"texture": "#main"}},
                            }
                        ],
                    }
                ),
                encoding="utf-8",
            )
            with mock.patch(
                f"{__name__}.exact_asset_file", return_value=outside
            ):
                problems = item_model_problems(model_path, "test_crown")
                self.assertTrue(any("outside" in problem for problem in problems))

            own_texture = assets / "usless_mobs/textures/item/test_crown.png"
            own_texture.parent.mkdir(parents=True)
            Image.new("RGBA", (2, 2), (255, 255, 255, 255)).save(own_texture)
            outside_model = root / "outside.json"
            outside_model.write_text(
                json.dumps(
                    {
                        "textures": {"main": "usless_mobs:item/test_crown"},
                        "elements": [
                            {
                                "from": [1, 2, 3],
                                "to": [4, 5, 6],
                                "faces": {"north": {"texture": "#main"}},
                            }
                        ],
                    }
                ),
                encoding="utf-8",
            )
            model_path.write_text(
                json.dumps({"parent": "usless_mobs:item/outside"}), encoding="utf-8"
            )
            with mock.patch(
                f"{__name__}.exact_resource_matches", return_value=[outside_model]
            ):
                problems = item_model_problems(model_path, "test_crown")
                self.assertTrue(any("outside" in problem for problem in problems))

    def test_pillow_decode_exceptions_become_contract_problems(self):
        errors = [
            Image.DecompressionBombError("too large"),
            Image.DecompressionBombWarning("suspicious"),
            ValueError("decoder failed"),
        ]
        for error in errors:
            with self.subTest(error=type(error).__name__):
                with mock.patch.object(Image, "open", side_effect=error):
                    self.assertTrue(
                        self.validate_fixture(
                            {
                                "textures": {"main": "usless_mobs:item/test_crown"},
                                "elements": [
                                    {
                                        "from": [1, 2, 3],
                                        "to": [4, 5, 6],
                                        "faces": {"north": {"texture": "#main"}},
                                    }
                                ],
                            }
                        )
                    )


if __name__ == "__main__":
    unittest.main()
