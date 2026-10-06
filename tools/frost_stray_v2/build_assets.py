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
OUTPUT = ROOT / "src/main/resources/assets/usless_mobs/textures/entity/custom3d/frost_stray_v2_overlay.png"
MANIFEST = ROOT / "Modelle/Exports/frost_stray_v2/frost_stray_v2.manifest.json"


def _texture() -> Image.Image:
    image = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    # Compact, UV-safe swatches used by the overlay model: deep ice, frost edge,
    # and a restrained cyan emission accent.
    draw.rectangle((0, 0, 63, 63), fill=(18, 56, 104, 255))
    draw.rectangle((2, 2, 29, 29), fill=(126, 218, 255, 255))
    draw.rectangle((34, 2, 61, 29), fill=(226, 250, 255, 255))
    draw.rectangle((2, 34, 29, 61), fill=(43, 126, 190, 255))
    draw.rectangle((34, 34, 61, 61), fill=(104, 230, 255, 255))
    draw.line((2, 2, 29, 29), fill=(255, 255, 255, 255), width=2)
    draw.line((34, 2, 61, 29), fill=(196, 244, 255, 255), width=2)
    draw.line((2, 34, 29, 61), fill=(118, 220, 255, 255), width=2)
    draw.line((34, 34, 61, 61), fill=(238, 255, 255, 255), width=2)
    return image


def build() -> dict[str, object]:
    source = json.loads(SOURCE.read_text(encoding="utf-8"))
    bones = source.get("bones", [])
    if len(bones) != 12 or len({bone["name"] for bone in bones}) != 12:
        raise ValueError("Frost Stray v2 overlay must define exactly twelve unique sockets")
    if any(len(bone.get("pivot", [])) != 3 for bone in bones):
        raise ValueError("Every Frost Stray v2 socket needs a three-axis pivot")

    image = _texture()
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    image.save(OUTPUT, format="PNG", optimize=False, compress_level=9)
    digest = hashlib.sha256(OUTPUT.read_bytes()).hexdigest()
    source_mesh = ROOT / "Modelle/Exports/frost_stray_v1/source/frost_stray_textured_4k_v3_candidate.glb"
    manifest = {
        "source": SOURCE.relative_to(ROOT).as_posix(),
        "source_mesh": source_mesh.relative_to(ROOT).as_posix(),
        "source_mesh_sha256": hashlib.sha256(source_mesh.read_bytes()).hexdigest(),
        "license": source.get("license"),
        "runtime_texture": OUTPUT.relative_to(ROOT).as_posix(),
        "runtime_resolution": [64, 64],
        "overlay_bones": [bone["name"] for bone in bones],
        "runtime_triangle_budget": 288,
        "runtime_particle_budget": {"charge_near": 6, "charge_mid": 3, "impact": 8},
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
