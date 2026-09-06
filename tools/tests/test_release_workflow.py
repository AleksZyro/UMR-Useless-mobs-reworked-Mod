from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


def test_tag_release_is_built_and_verified_before_publication():
    workflow = (ROOT / ".github/workflows/release.yml").read_text(encoding="utf-8")

    assert 'tags: ["v*"]' in workflow
    assert "permissions:\n  contents: write" in workflow
    assert "python tools/verify_umr_project_truth.py" in workflow
    assert "python tools/mob_tripo/build_quality_textures.py --all" in workflow
    assert "python -m pytest -q" in workflow
    assert "./gradlew clean build --no-daemon" in workflow
    assert 'EXPECTED_TAG="v${MOD_VERSION}"' in workflow
    assert "Exactly one production JAR" in workflow
    assert "META-INF/mods.toml" in workflow
    assert 'version = \\"${MOD_VERSION}\\"' in workflow
    assert "sha256sum" in workflow
    assert "UMR-Exact-4K-${MOD_VERSION}.zip" in workflow
    assert "gh release create" in workflow


def test_release_documents_all_mandatory_runtime_dependencies():
    workflow = (ROOT / ".github/workflows/release.yml").read_text(encoding="utf-8")

    assert "Minecraft 1.20.1" in workflow
    assert "Forge 47.4.16" in workflow
    assert "GeckoLib 4.8.3" in workflow
    assert "TerraBlender 3.0.1.10" in workflow
