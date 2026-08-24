from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


def test_gradle_major_updates_are_deferred_until_the_forge_toolchain_is_migrated():
    config = (ROOT / ".github/dependabot.yml").read_text(encoding="utf-8")

    for dependency in ("gradle-wrapper", "net.minecraftforge.gradle"):
        entry = f"- dependency-name: {dependency}\n        update-types:\n          - version-update:semver-major"
        assert entry in config
