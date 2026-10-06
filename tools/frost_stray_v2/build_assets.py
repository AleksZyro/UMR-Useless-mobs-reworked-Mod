"""Build and validate the deterministic Frost Stray v2 overlay texture."""

from __future__ import annotations

import argparse
import hashlib
import json
import platform
from pathlib import Path

from PIL import Image, ImageDraw
from PIL import __version__ as pillow_version

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "Modelle/Exports/frost_stray_v2/frost_stray_v2_rig.json"
CLIPS = ROOT / "Modelle/Exports/frost_stray_v2/frost_stray_v2_clips.json"
STYLE_BRIEF = ROOT / "Modelle/Exports/frost_stray_v2/STYLE_BRIEF.md"
OUTPUT = ROOT / "src/main/resources/assets/usless_mobs/textures/entity/custom3d/frost_stray_v2_overlay.png"
MANIFEST = ROOT / "Modelle/Exports/frost_stray_v2/frost_stray_v2.manifest.json"


def _texture() -> Image.Image:
    image = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    # UV-safe 16 px material tiles: deep ice, frosted edge, old blue metal and a
    # restrained charge accent. Formulae are deterministic and avoid random noise.
    palette = ((18, 56, 104), (70, 145, 202), (126, 218, 255), (224, 249, 255))
    for tile_y in range(4):
        for tile_x in range(4):
            base = palette[(tile_x + tile_y * 2) % len(palette)]
            left, top = tile_x * 16, tile_y * 16
            draw.rectangle((left, top, left + 15, top + 15), fill=(*base, 255))
            for pixel_y in range(1, 15):
                for pixel_x in range(1, 15):
                    crystal = (pixel_x * 5 + pixel_y * 3 + tile_x * 7 + tile_y * 11) % 13
                    if crystal in (0, 1):
                        draw.point((left + pixel_x, top + pixel_y), fill=(196, 244, 255, 255))
                    elif crystal == 2:
                        draw.point((left + pixel_x, top + pixel_y), fill=(35, 101, 159, 255))
            draw.line((left + 1, top + 14, left + 14, top + 1), fill=(238, 255, 255, 255), width=1)
            draw.rectangle((left, top, left + 15, top + 15), outline=(12, 39, 77, 255), width=1)
    return image


def build() -> dict[str, object]:
    source = json.loads(SOURCE.read_text(encoding="utf-8"))
    clips = json.loads(CLIPS.read_text(encoding="utf-8"))
    bones = source.get("bones", [])
    if len(bones) != 12 or len({bone["name"] for bone in bones}) != 12:
        raise ValueError("Frost Stray v2 overlay must define exactly twelve unique sockets")
    if any(len(bone.get("pivot", [])) != 3 for bone in bones):
        raise ValueError("Every Frost Stray v2 socket needs a three-axis pivot")
    sockets = source.get("sockets", [])
    required_sockets = {"main_hand", "off_hand", "bow_grip", "projectile_release", "charge_anchor"}
    if {socket.get("name") for socket in sockets} != required_sockets:
        raise ValueError("Frost Stray v2 requires the five documented visual sockets")
    if any(len(socket.get("pivot", [])) != 3 for socket in sockets):
        raise ValueError("Every Frost Stray v2 visual socket needs a three-axis pivot")
    expected_clips = {"spawn", "idle", "walk", "run", "bow_volley", "hurt", "death"}
    if {clip.get("name") for clip in clips.get("clips", [])} != expected_clips:
        raise ValueError("Frost Stray v2 clip catalogue is incomplete")
    if any(clip.get("duration_ticks", 0) <= 0 for clip in clips["clips"]):
        raise ValueError("Frost Stray v2 clips need positive durations")
    if not STYLE_BRIEF.is_file():
        raise ValueError("Frost Stray v2 style brief is missing")

    image = _texture()
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    image.save(OUTPUT, format="PNG", optimize=False, compress_level=9)
    digest = hashlib.sha256(OUTPUT.read_bytes()).hexdigest()
    source_mesh = ROOT / "Modelle/Exports/frost_stray_v1/source/frost_stray_textured_4k_v3_candidate.glb"
    manifest = {
        "sources": {
            "rig": SOURCE.relative_to(ROOT).as_posix(),
            "clips": CLIPS.relative_to(ROOT).as_posix(),
            "style_brief": STYLE_BRIEF.relative_to(ROOT).as_posix(),
        },
        "source_mesh": source_mesh.relative_to(ROOT).as_posix(),
        "source_mesh_sha256": hashlib.sha256(source_mesh.read_bytes()).hexdigest(),
        "license": source.get("license"),
        "runtime_texture": OUTPUT.relative_to(ROOT).as_posix(),
        "runtime_resolution": [64, 64],
        "overlay_bones": [bone["name"] for bone in bones],
        "visual_sockets": sockets,
        "coordinate_convention": source["coordinate_convention"],
        "unit_scale": source["unit_scale"],
        "clips": clips["clips"],
        "runtime_triangle_count": len(bones) * 12,
        "runtime_triangle_budget": 288,
        "runtime_particle_budget": {"charge_near": 4, "charge_mid": 1, "release_near": 16,
                                    "impact_near": 24, "max_distance_blocks": 32},
        "export_parameters": {"texture_format": "PNG RGBA", "compress_level": 9, "optimize": False},
        "tool_versions": {
            "python": platform.python_version(),
            "pillow": pillow_version,
            "generator": "frost_stray_v2/build_assets.py",
        },
        "sha256": digest,
        "generator": "tools/frost_stray_v2/build_assets.py",
    }
    MANIFEST.parent.mkdir(parents=True, exist_ok=True)
    MANIFEST.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    return manifest


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="build and print the manifest")
    parser.parse_args()
    manifest = build()
    print(f"FROST_STRAY_V2_ASSETS_PASS BONES={len(manifest['overlay_bones'])} SHA256={manifest['sha256']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
