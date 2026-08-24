"""Fail-closed verifier for the active UMR Corrupted Silverfish runtime assets."""

from __future__ import annotations

import argparse
import json
import struct
import subprocess
import sys
from pathlib import Path


EXPECTED_BONES = 8
EXPECTED_CUBES = 0
EXPECTED_REGIONS = (
    "body",
    "leg_front_left",
    "leg_front_right",
    "leg_middle_left",
    "leg_middle_right",
    "leg_rear_left",
    "leg_rear_right",
)
EXPECTED_TRIANGLES = 101_723
EXPECTED_TEXTURE = (4096, 4096)
MAGIC = b"CSMESH1\0"
ASSET_ROOT = Path("src/main/mobs/endermite/resources/assets/usless_mobs")


def fail(message: str) -> int:
    print(f"UMR_PROJECT_TRUTH_FAIL: {message}", file=sys.stderr)
    return 1


def read_branch(root: Path) -> str:
    result = subprocess.run(
        ["git", "-C", str(root), "branch", "--show-current"],
        text=True,
        capture_output=True,
        check=False,
    )
    return result.stdout.strip() if result.returncode == 0 else "unknown"


def count_cubes(value: object) -> int:
    if isinstance(value, dict):
        return len(value.get("cubes", [])) + sum(count_cubes(item) for item in value.values())
    if isinstance(value, list):
        return sum(count_cubes(item) for item in value)
    return 0


def decode_mesh_header(payload: bytes) -> tuple[tuple[str, ...], int]:
    if len(payload) < 12 or payload[:8] != MAGIC:
        raise ValueError("ungültiger Mesh-Header")
    offset = 8

    def take(fmt: str) -> tuple[object, ...]:
        nonlocal offset
        size = struct.calcsize(fmt)
        if offset + size > len(payload):
            raise ValueError("abgeschnittenes Laufzeit-Mesh")
        values = struct.unpack_from(fmt, payload, offset)
        offset += size
        return values

    region_count = int(take("<I")[0])
    regions: list[str] = []
    triangles = 0
    for _ in range(region_count):
        name_length = int(take("<H")[0])
        if offset + name_length > len(payload):
            raise ValueError("abgeschnittener Regionenname")
        name = payload[offset : offset + name_length].decode("utf-8")
        offset += name_length
        face_count = int(take("<I")[0])
        regions.append(name)
        triangles += face_count
        byte_count = face_count * 3 * 5 * 4
        if offset + byte_count > len(payload):
            raise ValueError("abgeschnittene Dreiecksdaten")
        offset += byte_count
    if offset != len(payload):
        raise ValueError("unerwartete Daten nach dem Mesh")
    return tuple(regions), triangles


def png_dimensions(payload: bytes) -> tuple[int, int]:
    if len(payload) < 24 or payload[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("Textur ist keine gültige PNG-Datei")
    return struct.unpack(">II", payload[16:24])


def verify(root: Path) -> int:
    root = root.resolve()
    geo_path = root / ASSET_ROOT / "geo/corrupted_silverfish.geo.json"
    mesh_path = root / ASSET_ROOT / "meshes/entity/corrupted_silverfish.mesh"
    animation_path = root / ASSET_ROOT / "animations/corrupted_silverfish.animation.json"
    texture_path = root / ASSET_ROOT / "textures/entity/corrupted_silverfish.png"
    missing = [path for path in (geo_path, mesh_path, animation_path, texture_path) if not path.is_file()]
    if missing:
        return fail("aktive Laufzeitressourcen fehlen: " + ", ".join(str(path) for path in missing))

    try:
        geo = json.loads(geo_path.read_text(encoding="utf-8"))
        geometry = geo["minecraft:geometry"][0]
        bones = geometry["bones"]
        bone_count = len(bones)
        cube_count = count_cubes(bones)
        regions, triangles = decode_mesh_header(mesh_path.read_bytes())
        texture = png_dimensions(texture_path.read_bytes())
        json.loads(animation_path.read_text(encoding="utf-8"))
    except (KeyError, IndexError, TypeError, ValueError, json.JSONDecodeError) as exc:
        return fail(f"Laufzeitressourcen sind nicht lesbar: {exc}")

    facts = {
        "bones": bone_count,
        "cubes": cube_count,
        "regions": regions,
        "triangles": triangles,
        "texture": texture,
    }
    expected = {
        "bones": EXPECTED_BONES,
        "cubes": EXPECTED_CUBES,
        "regions": EXPECTED_REGIONS,
        "triangles": EXPECTED_TRIANGLES,
        "texture": EXPECTED_TEXTURE,
    }
    if facts != expected:
        return fail(f"Laufzeitsignatur hat sich geändert; zuerst prüfen und Projektdokument aktualisieren: {facts}")

    print(
        "UMR_PROJECT_TRUTH_PASS "
        f"branch={read_branch(root)} bones={bone_count} cubes={cube_count} "
        f"regions={len(regions)} triangles={triangles} texture={texture[0]}x{texture[1]}"
    )
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    return verify(parser.parse_args().root)


if __name__ == "__main__":
    raise SystemExit(main())
