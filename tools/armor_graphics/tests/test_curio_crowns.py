from __future__ import annotations

import json
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
    registrations = []
    for index in range(len(tokens) - 4):
        window = tokens[index : index + 5]
        if (
            window[0] == ("identifier", "ITEMS")
            and window[1] == ("punctuation", ".")
            and window[2] == ("identifier", "register")
            and window[3] == ("punctuation", "(")
            and window[4][0] == "string"
        ):
            registrations.append(window[4][1])
    return registrations


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
            values = tag.get("values")
            if not isinstance(values, list) or not all(isinstance(value, str) for value in values):
                problems.append(
                    f"{CROWN_TAG.relative_to(ROOT)}: values must be a list of item-ID strings"
                )
            else:
                expected = {f"usless_mobs:{item_id}" for item_id in ALL_IDS}
                missing = sorted(expected - set(values))
                if missing:
                    problems.append(f"{CROWN_TAG.relative_to(ROOT)}: missing crown tag values: {missing}")
        self.assert_no_problems(problems)

    def test_every_crown_has_exactly_one_valid_item_model(self):
        problems = []
        candidates = list(MODELS.iterdir()) if MODELS.is_dir() else []
        for item_id in sorted(ALL_IDS):
            matches = [
                path
                for path in candidates
                if path.is_file() and path.suffix.lower() == ".json" and path.stem == item_id
            ]
            if len(matches) != 1:
                problems.append(
                    f"expected exactly one model at "
                    f"{(MODELS / f'{item_id}.json').relative_to(ROOT)}, found {len(matches)}"
                )
                continue
            load_json_object(matches[0], problems)
        self.assert_no_problems(problems)

    def test_every_crown_has_exactly_one_64x64_rgba_texture(self):
        problems = []
        candidates = list(TEXTURES.iterdir()) if TEXTURES.is_dir() else []
        for item_id in sorted(ALL_IDS):
            expected = TEXTURES / f"{item_id}.png"
            matches = [
                path
                for path in candidates
                if path.is_file() and path.suffix.lower() == ".png" and path.stem == item_id
            ]
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


if __name__ == "__main__":
    unittest.main()
