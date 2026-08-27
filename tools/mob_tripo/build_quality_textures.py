"""Build lightweight 2K runtime textures and a lossless optional 4K pack."""

from __future__ import annotations

import argparse
import json
import os
import sys
import tempfile
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools.corrupted_silverfish_v5.tripo_voxel import load_glb


REPORT_ROOT = ROOT / "src/main/resources/assets/usless_mobs/meshes/entity/custom3d"
RUNTIME_ROOT = ROOT / "src/main/resources/assets/usless_mobs/textures/entity/custom3d/exact"
PACK_ROOT = ROOT / "quality-packs/UMR-Exact-4K"
QUALITY_ROOT = PACK_ROOT / "assets/usless_mobs/textures/entity/custom3d/exact"
SOURCE_GLB = {
    "axolotl": ROOT / "Modelle/Exports/axolotl_v1/source/axolotl_textured_4k_v2.glb",
    "coral_drowned": ROOT / "Modelle/Exports/coral_drowned_v1/source/coral_drowned_textured_4k_v2.glb",
    "frost_stray": ROOT / "Modelle/Exports/frost_stray_v1/source/frost_stray_textured_4k_v2.glb",
    "glow_squid": ROOT / "Modelle/Exports/glow_squid_v1/source/glow_squid_textured_4k.glb",
    "helping_allay": ROOT / "Modelle/Exports/helping_allay_v1/tripo_export/helping_allay.glb",
    "living_bat": ROOT / "Modelle/Exports/living_bat_v1/tripo_export/living_bat_tripo_retopo50k_textured_4k_20260821.glb",
    "living_boss": ROOT / "Modelle/Exports/living_boss_v1/tripo_export/living_boss_tripo_retopo50k_textured_4k_20260821.glb",
    "ocelot": ROOT / "Modelle/Exports/ocelot_v1/source/ocelot_textured_4k.glb",
    "octopus": ROOT / "Modelle/Exports/octopus_v1/tripo_export/octopus_tripo_textured_4k_20260821.glb",
    "polar_bear": ROOT / "Modelle/Exports/polar_bear_v1/source/polar_bear_textured_4k.glb",
    "rooted_husk": ROOT / "Modelle/Exports/rooted_husk_v1/tripo_export/rooted_husk_tripo_retopo50k_textured_4k_20260821.glb",
    "squid": ROOT / "Modelle/Exports/squid_v1/source/squid_textured_4k.glb",
    "web_cave_spider": ROOT / "Modelle/Exports/web_cave_spider_v1/tripo_export/web_cave_spider_tripo_textured_4k_20260821.glb",
    "witch_boss": ROOT / "Modelle/Exports/witch_boss_v1/tripo_export/witch_boss_tripo_textured_4k_20260821.glb",
}


def _reports() -> list[tuple[Path, dict]]:
    reports = [
        (path, json.loads(path.read_text(encoding="utf-8")))
        for path in sorted(REPORT_ROOT.glob("*.report.json"))
    ]
    if not reports:
        raise RuntimeError("No exact-mesh reports found")
    return reports


def _source_image(name: str) -> Image.Image:
    quality = QUALITY_ROOT / f"{name}.png"
    if quality.is_file():
        with Image.open(quality) as image:
            image.load()
            return image.copy()
    try:
        source = SOURCE_GLB[name]
    except KeyError as exc:
        raise FileNotFoundError(f"Missing approved GLB mapping for {name}") from exc
    if not source.is_file():
        raise FileNotFoundError(f"Missing approved GLB source for {name}: {source}")
    return load_glb(source).base_colour.copy()


def build_all() -> None:
    reports = _reports()
    with tempfile.TemporaryDirectory(prefix="umr_exact_textures_", dir=ROOT) as temporary:
        stage = Path(temporary)
        stage_runtime = stage / "runtime"
        stage_quality = stage / "quality"
        stage_reports = stage / "reports"
        stage_runtime.mkdir(parents=True)
        stage_quality.mkdir(parents=True)
        stage_reports.mkdir(parents=True)

        for report_path, report in reports:
            name = report["mob"]
            with _source_image(name) as image:
                if image.size != (4096, 4096):
                    raise ValueError(f"The approved texture for {name} is not 4K")
                mode = image.mode
                resized = image.resize((2048, 2048), Image.Resampling.LANCZOS)
                resized.save(stage_runtime / f"{name}.png", format="PNG", optimize=True)
                image.save(stage_quality / f"{name}.png", format="PNG", optimize=False, compress_level=9)
            with Image.open(stage_runtime / f"{name}.png") as runtime_check:
                if runtime_check.size != (2048, 2048) or runtime_check.mode != mode:
                    raise AssertionError(f"Invalid staged runtime texture for {name}")

            report["source_texture_width"] = 4096
            report["source_texture_height"] = 4096
            report["runtime_texture_width"] = 2048
            report["runtime_texture_height"] = 2048
            (stage_reports / report_path.name).write_text(
                json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8"
            )

        RUNTIME_ROOT.mkdir(parents=True, exist_ok=True)
        QUALITY_ROOT.mkdir(parents=True, exist_ok=True)
        for staged in sorted(stage_runtime.glob("*.png")):
            os.replace(staged, RUNTIME_ROOT / staged.name)
        for staged in sorted(stage_quality.glob("*.png")):
            os.replace(staged, QUALITY_ROOT / staged.name)
        for staged in sorted(stage_reports.glob("*.json")):
            os.replace(staged, REPORT_ROOT / staged.name)

    PACK_ROOT.mkdir(parents=True, exist_ok=True)
    (PACK_ROOT / "pack.mcmeta").write_text(
        json.dumps({"pack": {"pack_format": 15, "description": "UMR Exact Mesh 4K Textures"}}, indent=2)
        + "\n",
        encoding="utf-8",
    )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--all", action="store_true", help="Build every active exact texture")
    arguments = parser.parse_args()
    if not arguments.all:
        parser.error("--all is required")
    build_all()


if __name__ == "__main__":
    main()
