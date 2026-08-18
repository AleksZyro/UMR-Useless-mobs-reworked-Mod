from __future__ import annotations

import json
import re
import tempfile
import unittest
from collections import Counter
from pathlib import Path

from PIL import Image, UnidentifiedImageError


ROOT = Path(__file__).resolve().parents[3]
MOD_ITEMS = ROOT / "src/main/java/com/Momik/usless_mobs/registry/ModItems.java"
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
        document = json.loads(path.read_text(encoding="utf-8"))
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


RESOURCE_LOCATION = re.compile(r"[a-z0-9_.-]+:[a-z0-9_.\-/]+")
TEXTURE_ALIAS = re.compile(r"[a-z0-9_.-]+")


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


def item_model_problems(model_path: Path, item_id: str) -> list[str]:
    problems = []

    def report(message: str) -> None:
        if message not in problems:
            problems.append(message)

    assets_root = next((parent for parent in model_path.parents if parent.name == "assets"), None)
    if assets_root is None:
        return [f"{display_path(model_path)} is not below an assets directory"]

    resolved_assets_root = assets_root.resolve()
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
    if not isinstance(elements, list) or not elements:
        report("effective model must provide non-empty elements with textured faces")
    else:
        for element_index, element in enumerate(elements):
            if not isinstance(element, dict):
                report(f"elements[{element_index}] must be a JSON object")
                continue
            for corner in ("from", "to"):
                coordinates = element.get(corner)
                if not (
                    isinstance(coordinates, list)
                    and len(coordinates) == 3
                    and all(isinstance(value, (int, float)) for value in coordinates)
                ):
                    report(f"elements[{element_index}].{corner} must contain three numbers")
            faces = element.get("faces")
            if not isinstance(faces, dict) or not faces:
                report(f"elements[{element_index}].faces must be a non-empty object")
                continue
            for face_name, face in faces.items():
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
    if expected_texture not in resolved_faces:
        report(
            f"effective faces must reference own texture {expected_texture!r}; "
            f"resolved {sorted(set(resolved_faces))!r}"
        )
    return problems


def exact_resource_matches(directory: Path, item_id: str, extension: str) -> list[Path]:
    if not directory.is_dir():
        return []
    expected_name = f"{item_id}{extension}"
    return [path for path in directory.iterdir() if path.is_file() and path.name == expected_name]


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

    def test_mod_items_registers_exactly_the_four_royal_ids(self):
        problems = []
        try:
            source = MOD_ITEMS.read_text(encoding="utf-8")
        except (OSError, UnicodeError) as error:
            self.fail(f"cannot read {MOD_ITEMS.relative_to(ROOT)}: {error}")

        registrations = registered_item_ids(source)
        counts = Counter(registrations)
        expected = set(ROYAL_IDS)
        actual_royal = {item_id for item_id in counts if item_id.startswith("royal_")}
        missing = sorted(expected - actual_royal)
        unexpected = sorted(actual_royal - expected)
        duplicates = {item_id: counts[item_id] for item_id in expected if counts[item_id] != 1}
        if missing:
            problems.append(f"missing Royal ITEMS.register calls: {missing}")
        if unexpected:
            problems.append(f"unexpected royal_ ITEMS.register calls: {unexpected}")
        if duplicates:
            problems.append(
                "each Royal ID must have exactly one ITEMS.register call; "
                f"observed counts: {dict(sorted(duplicates.items()))}"
            )
        if sum(counts[item_id] for item_id in actual_royal) != 4:
            problems.append(
                "ModItems must contain exactly four royal_ registrations; "
                f"found {sum(counts[item_id] for item_id in actual_royal)}"
            )
        self.assert_no_problems(problems)
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
            try:
                with Image.open(matches[0]) as image:
                    image.load()
                    if image.format != "PNG":
                        problems.append(
                            f"{matches[0].relative_to(ROOT)} must be PNG, got {image.format!r}"
                        )
                    if image.mode != "RGBA":
                        problems.append(
                            f"{matches[0].relative_to(ROOT)} must use RGBA mode, got {image.mode!r}"
                        )
                    if image.size != (64, 64):
                        problems.append(
                            f"{matches[0].relative_to(ROOT)} must be 64x64, got {image.size}"
                        )
            except (OSError, UnidentifiedImageError) as error:
                problems.append(f"invalid texture {matches[0].relative_to(ROOT)}: {error}")
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
    def validate_fixture(self, model: dict, parents: dict[str, dict] | None = None):
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
            return item_model_problems(model_path, "test_crown")

    def test_registry_parser_rejects_method_local_and_unassigned_decoys(self):
        source = r'''
            public final class ModItems {
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


if __name__ == "__main__":
    unittest.main()
