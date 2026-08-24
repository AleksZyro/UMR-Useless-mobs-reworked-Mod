from pathlib import Path

import yaml


ROOT = Path(__file__).resolve().parents[2]


def test_gradle_major_updates_are_deferred_until_the_forge_toolchain_is_migrated():
    config = yaml.safe_load((ROOT / ".github/dependabot.yml").read_text(encoding="utf-8"))
    gradle = next(update for update in config["updates"] if update["package-ecosystem"] == "gradle")
    ignored_major_updates = {
        entry["dependency-name"]
        for entry in gradle.get("ignore", [])
        if "version-update:semver-major" in entry.get("update-types", [])
    }

    assert "gradle-wrapper" in ignored_major_updates
    assert "net.minecraftforge.gradle" in ignored_major_updates
