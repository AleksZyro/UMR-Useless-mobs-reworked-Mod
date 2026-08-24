import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
RESOURCE_ROOTS = (
    ROOT / "src/main/resources",
    ROOT / "src/main/common/resources",
    ROOT / "src/main/mobs/endermite/resources",
    ROOT / "src/main/mobs/slime/resources",
    ROOT / "src/generated/resources",
)


def load_language(locale: str) -> dict[str, str]:
    merged: dict[str, str] = {}
    for root in RESOURCE_ROOTS:
        path = root / f"assets/usless_mobs/lang/{locale}.json"
        if path.exists():
            merged.update(json.loads(path.read_text(encoding="utf-8")))
    return merged


def registered_translation_keys() -> set[str]:
    keys: set[str] = set()
    registry_specs = (
        ("item", "ModItems.java"),
        ("entity", "ModEntities.java"),
        ("effect", "ModEffects.java"),
        ("block", "ModBlocks.java"),
    )
    for prefix, filename in registry_specs:
        for path in ROOT.glob(f"src/main/**/registry/{filename}"):
            ids = re.findall(
                r'\.register\(\s*["\']([a-z0-9_]+)["\']',
                path.read_text(encoding="utf-8"),
            )
            keys.update(f"{prefix}.usless_mobs.{registry_id}" for registry_id in ids)
    return keys


def literal_translation_keys() -> set[str]:
    keys: set[str] = set()
    pattern = re.compile(r'(?:Component|I18n)\.translatable\(\s*["\']([^"\']+)')
    for path in ROOT.glob("src/main/**/*.java"):
        keys.update(pattern.findall(path.read_text(encoding="utf-8")))
    # A trailing dot marks a prefix whose suffix is deliberately assembled at runtime.
    return {key for key in keys if not key.endswith(".")}


def test_registered_content_and_literal_messages_are_localized():
    required = registered_translation_keys() | literal_translation_keys()
    for locale in ("en_us", "de_de"):
        translations = load_language(locale)
        assert required <= translations.keys(), (
            f"{locale} is missing translations: "
            f"{sorted(required - translations.keys())}"
        )


def test_supported_languages_expose_the_same_keys():
    english = load_language("en_us")
    german = load_language("de_de")
    assert english.keys() == german.keys(), {
        "only_en_us": sorted(english.keys() - german.keys()),
        "only_de_de": sorted(german.keys() - english.keys()),
    }


def test_format_placeholders_match_between_supported_languages():
    english = load_language("en_us")
    german = load_language("de_de")
    placeholder = re.compile(r"%(?:\d+\$)?[a-zA-Z%]")
    mismatches = {
        key: {
            "en_us": sorted(placeholder.findall(english[key])),
            "de_de": sorted(placeholder.findall(german[key])),
        }
        for key in english.keys() & german.keys()
        if sorted(placeholder.findall(english[key]))
        != sorted(placeholder.findall(german[key]))
    }
    assert not mismatches, mismatches
