from __future__ import annotations

import hashlib
import json
from collections import Counter
from pathlib import Path

import numpy as np
from PIL import Image

from tools.corrupted_silverfish_v5.tripo_voxel import _parse_glb, load_glb
from tools.mob_tripo.exact_runtime import decode_mesh


ROOT = Path(__file__).resolve().parents[2]
SOURCES = {
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


def audit_source(name: str, glb_path: Path) -> dict:
    runtime_texture_path = (
        ROOT / f"src/main/resources/assets/usless_mobs/textures/entity/custom3d/exact/{name}.png"
    )
    runtime_mesh_path = ROOT / f"src/main/resources/assets/usless_mobs/meshes/entity/custom3d/{name}.mesh"
    with Image.open(runtime_texture_path) as image:
        runtime_pixels = np.asarray(image.convert("RGBA"))
    decoded = decode_mesh(runtime_mesh_path.read_bytes())
    emitted_uvs = np.asarray(
        [
            corner[1]
            for part in decoded.values()
            for face in part["faces"]
            for corner in face
        ]
    )
    emitted_faces = emitted_uvs.reshape(-1, 3, 2)
    document, _ = _parse_glb(glb_path)
    source = load_glb(glb_path)
    source_pixels = np.asarray(source.base_colour.convert("RGBA"))
    expected_faces = source.uvs[source.triangles[:, [0, 2, 1]]]
    same_shape = expected_faces.shape == emitted_faces.shape
    signature = lambda faces: Counter(
        tuple(tuple(round(float(value), 6) for value in corner) for corner in face)
        for face in faces
    )
    runtime_signature = signature(emitted_faces) if same_shape else Counter()
    direct_exact = same_shape and signature(expected_faces) == runtime_signature
    flipped_faces = expected_faces.copy()
    flipped_faces[:, :, 1] = 1.0 - flipped_faces[:, :, 1]
    flipped_exact = same_shape and signature(flipped_faces) == runtime_signature
    pbr = document["materials"][0].get("pbrMetallicRoughness", {})
    return {
        "mob": name,
        "glb": str(glb_path.relative_to(ROOT)),
        "triangles": int(len(source.triangles)),
        "runtime_triangles": int(len(emitted_uvs) // 3),
        "pixels_equal": bool(np.array_equal(source_pixels, runtime_pixels)),
        "source_pixel_hash": hashlib.sha256(source_pixels.tobytes()).hexdigest(),
        "runtime_pixel_hash": hashlib.sha256(runtime_pixels.tobytes()).hexdigest(),
        "uv_shape_equal": same_shape,
        "uv_exact": bool(direct_exact),
        "uv_exact_if_v_flipped": bool(flipped_exact),
        "uv_min": source.uvs.min(axis=0).tolist(),
        "uv_max": source.uvs.max(axis=0).tolist(),
        "base_color_texture": pbr.get("baseColorTexture"),
        "base_color_factor": pbr.get("baseColorFactor"),
        "extensions_used": document.get("extensionsUsed", []),
    }


def audit_all() -> list[dict]:
    return [audit_source(name, path) for name, path in sorted(SOURCES.items())]


if __name__ == "__main__":
    failed = False
    for result in audit_all():
        print(json.dumps(result, sort_keys=True))
        failed |= not (
            result["pixels_equal"]
            and result["uv_shape_equal"]
            and result["uv_exact"]
            and result["triangles"] == result["runtime_triangles"]
        )
    raise SystemExit(1 if failed else 0)
