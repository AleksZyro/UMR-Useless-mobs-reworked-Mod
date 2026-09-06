import json
from pathlib import Path
import subprocess
import sys

from PIL import Image

from tools.mob_tripo import build_quality_textures


ROOT = Path(__file__).resolve().parents[2]
REPORT_ROOT = ROOT / "src/main/resources/assets/usless_mobs/meshes/entity/custom3d"
RUNTIME_ROOT = ROOT / "src/main/resources/assets/usless_mobs/textures/entity/custom3d/exact"
QUALITY_ROOT = ROOT / "quality-packs/UMR-Exact-4K/assets/usless_mobs/textures/entity/custom3d/exact"


def test_every_exact_mesh_has_2k_runtime_and_matching_4k_quality_texture():
    reports = [json.loads(path.read_text(encoding="utf-8")) for path in REPORT_ROOT.glob("*.report.json")]
    assert reports

    expected = {f"{report['mob']}.png" for report in reports}
    assert {path.name for path in RUNTIME_ROOT.glob("*.png")} == expected
    assert {path.name for path in QUALITY_ROOT.glob("*.png")} == expected

    for report in reports:
        runtime_path = RUNTIME_ROOT / f"{report['mob']}.png"
        quality_path = QUALITY_ROOT / f"{report['mob']}.png"
        with Image.open(runtime_path) as runtime, Image.open(quality_path) as quality:
            assert runtime.size == (2048, 2048)
            assert quality.size == (4096, 4096)
            assert runtime.mode == quality.mode
        assert report["source_texture_width"] == 4096
        assert report["source_texture_height"] == 4096
        assert report["runtime_texture_width"] == 2048
        assert report["runtime_texture_height"] == 2048


def test_quality_pack_has_minecraft_1201_metadata():
    metadata = json.loads((ROOT / "quality-packs/UMR-Exact-4K/pack.mcmeta").read_text(encoding="utf-8"))
    assert metadata["pack"]["pack_format"] == 15


def test_quality_texture_generator_can_bootstrap_from_a_tracked_glb(tmp_path, monkeypatch):
    source = build_quality_textures.SOURCE_GLB["helping_allay"]
    assert source.is_file()
    assert "helping_allay.glb" in source.name
    monkeypatch.setattr(build_quality_textures, "QUALITY_ROOT", tmp_path / "missing-quality-pack")

    with build_quality_textures._source_image("helping_allay") as image:
        assert image.size == (4096, 4096)


def test_quality_texture_generator_has_a_working_direct_cli():
    result = subprocess.run(
        [sys.executable, ROOT / "tools/mob_tripo/build_quality_textures.py", "--help"],
        cwd=ROOT,
        check=False,
        capture_output=True,
        text=True,
    )

    assert result.returncode == 0, result.stderr
    assert "--all" in result.stdout
