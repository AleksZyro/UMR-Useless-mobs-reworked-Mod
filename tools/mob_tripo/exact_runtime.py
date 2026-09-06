"""Export exact textured Tripo triangles into the UMR animated runtime format.

The exporter never voxelises, rebuilds or decimates geometry. Retopology is an
explicit upstream Tripo step; every triangle present in the selected GLB is
written exactly once to one animation bone.
"""

from __future__ import annotations

import argparse
from dataclasses import dataclass
from io import BytesIO
import json
import math
import os
from pathlib import Path
import struct
import tempfile
from typing import Callable, Dict, Iterable, Mapping, Sequence

import numpy as np

from tools.corrupted_silverfish_v5.tripo_voxel import MeshData, load_glb
from tools.mob_tripo.audit_connected_components import largest_component_mesh


MAGIC = b"UMMESH1\0"
PROJECT_ROOT = Path(__file__).resolve().parents[2]
RUNTIME_ASSET_ROOT = PROJECT_ROOT / "src/main/resources/assets/usless_mobs"


@dataclass(frozen=True)
class MobSpec:
    bones: tuple[str, ...]
    classifier: str
    fit_axis: int
    fit_span: float


MOB_SPECS: Mapping[str, MobSpec] = {
    "living_boss": MobSpec(
        ("body", "leg_front_left", "leg_front_right", "leg_rear_left", "leg_rear_right"),
        "quadruped",
        fit_axis=0,
        fit_span=31.2,
    ),
    "web_cave_spider": MobSpec(
        ("body",) + tuple(f"web_leg_{index}" for index in range(8)),
        "spider",
        fit_axis=0,
        fit_span=11.2,
    ),
    "octopus": MobSpec(
        ("body",) + tuple(f"tentacle{index}" for index in range(8)),
        "octopus",
        fit_axis=0,
        fit_span=14.4,
    ),
    "squid": MobSpec(
        ("body",)
        + tuple(f"arm{index}" for index in range(8))
        + ("catching_tentacle0", "catching_tentacle1"),
        "squid",
        fit_axis=2,
        fit_span=22.4,
    ),
    "glow_squid": MobSpec(
        ("body",)
        + tuple(f"arm{index}" for index in range(8))
        + ("catching_tentacle0", "catching_tentacle1"),
        "glow_squid",
        fit_axis=1,
        fit_span=24.0,
    ),
    "witch_boss": MobSpec(
        ("body", "head", "right_arm", "left_arm", "right_leg", "left_leg"),
        "humanoid",
        fit_axis=1,
        fit_span=31.2,
    ),
    "living_bat": MobSpec(
        ("body", "head", "right_wing", "left_wing", "right_wing_tip", "left_wing_tip"),
        "bat",
        fit_axis=0,
        fit_span=8.0,
    ),
    "rooted_husk": MobSpec(
        ("body", "head", "right_arm", "left_arm", "right_leg", "left_leg"),
        "humanoid",
        fit_axis=1,
        fit_span=31.2,
    ),
    "helping_allay": MobSpec(
        (
            "body",
            "head",
            "right_arm",
            "left_arm",
            "right_wing",
            "right_wing_tip",
            "left_wing",
            "left_wing_tip",
            "soul_core",
        ),
        "allay",
        fit_axis=1,
        fit_span=10.4,
    ),
    "polar_bear": MobSpec(
        ("body", "head", "leg_front_left", "leg_front_right", "leg_rear_left", "leg_rear_right"),
        "bear",
        fit_axis=2,
        fit_span=30.4,
    ),
    "frost_stray": MobSpec(
        ("body", "head", "right_arm", "left_arm", "right_leg", "left_leg"),
        "humanoid",
        fit_axis=1,
        fit_span=31.2,
    ),
    "coral_drowned": MobSpec(
        ("body", "head", "right_arm", "left_arm", "right_leg", "left_leg"),
        "humanoid",
        fit_axis=1,
        fit_span=31.2,
    ),
    "axolotl": MobSpec(
        ("body", "head", "tail", "leg_front_left", "leg_front_right", "leg_rear_left", "leg_rear_right"),
        "axolotl",
        fit_axis=2,
        fit_span=20.8,
    ),
    "ocelot": MobSpec(
        ("body", "head", "tail", "leg_front_left", "leg_front_right", "leg_rear_left", "leg_rear_right"),
        "ocelot",
        fit_axis=2,
        fit_span=23.2,
    ),
}


def transform_positions(
    positions: np.ndarray,
    longest_span: float | None = 24.0,
    *,
    fit_axis: int | None = None,
    fit_span: float | None = None,
) -> np.ndarray:
    """Uniformly scale the GLB into vanilla model coordinates (Y points down)."""

    points = np.asarray(positions, dtype=np.float64)
    if points.ndim != 2 or points.shape[1] != 3 or len(points) < 3 or not np.isfinite(points).all():
        raise ValueError("Mesh positions must be a finite non-empty Nx3 array")
    lower = points.min(axis=0)
    upper = points.max(axis=0)
    spans = upper - lower
    if fit_axis is None:
        if longest_span is None or not math.isfinite(longest_span) or longest_span <= 0:
            raise ValueError("Longest span must be finite and positive")
        source_span = float(spans.max())
        target_span = float(longest_span)
    else:
        if fit_axis not in (0, 1, 2):
            raise ValueError("Fit axis must be 0, 1 or 2")
        if fit_span is None or not math.isfinite(fit_span) or fit_span <= 0:
            raise ValueError("Fit span must be finite and positive")
        source_span = float(spans[fit_axis])
        target_span = float(fit_span)
    if source_span <= 0:
        raise ValueError("Mesh bounds have zero size")
    result = points * (target_span / source_span)
    result[:, 0] -= (result[:, 0].min() + result[:, 0].max()) / 2.0
    result[:, 2] -= (result[:, 2].min() + result[:, 2].max()) / 2.0
    result[:, 1] -= result[:, 1].min()
    result[:, 1] = 24.0 - result[:, 1]
    return result


def _unit_positions(positions: np.ndarray) -> np.ndarray:
    lower = positions.min(axis=0)
    span = positions.max(axis=0) - lower
    if np.any(span <= 0):
        raise ValueError("Mesh must have non-zero bounds on all axes")
    return (positions - lower) / span


def _classify_humanoid(points: np.ndarray) -> str:
    x = points[:, 0] - 0.5
    y = points[:, 1]
    if float(y.min()) > 0.72:
        return "head"
    if float(y.min()) > 0.32 and float(y.max()) < 0.76 and np.all(np.abs(x) > 0.20):
        return "left_arm" if float(x.mean()) < 0 else "right_arm"
    if float(y.max()) < 0.44 and np.all(np.abs(x) > 0.035):
        return "left_leg" if float(x.mean()) < 0 else "right_leg"
    return "body"


def _classify_quadruped(points: np.ndarray) -> str:
    x = points[:, 0] - 0.5
    y = points[:, 1]
    z = points[:, 2] - 0.5
    if float(y.max()) < 0.46 and np.all(np.abs(x) > 0.16):
        station = "front" if float(z.mean()) < 0 else "rear"
        side = "left" if float(x.mean()) < 0 else "right"
        return f"leg_{station}_{side}"
    return "body"


def _classify_bear(points: np.ndarray) -> str:
    """Split the generated bear along its verified -Z forward axis."""

    x = points[:, 0] - 0.5
    y = points[:, 1]
    z = points[:, 2]
    if float(y.max()) < 0.48 and np.all(np.abs(x) > 0.12):
        station = "front" if float(z.mean()) < 0.52 else "rear"
        side = "left" if float(x.mean()) < 0 else "right"
        return f"leg_{station}_{side}"
    if float(z.mean()) < 0.30 and float(y.mean()) > 0.34:
        return "head"
    return "body"


def _classify_axolotl(points: np.ndarray) -> str:
    """Split the verified -Z-facing Axolotl while retaining every source face."""

    x = points[:, 0] - 0.5
    y = points[:, 1]
    z = points[:, 2]
    if float(y.max()) < 0.44 and np.all(np.abs(x) > 0.16):
        station = "front" if float(z.mean()) < 0.48 else "rear"
        side = "left" if float(x.mean()) < 0 else "right"
        return f"leg_{station}_{side}"
    if float(z.mean()) < 0.30:
        return "head"
    if float(z.mean()) > 0.68:
        return "tail"
    return "body"


def _classify_ocelot(points: np.ndarray) -> str:
    """Split the -Z-facing Ocelot into continuous feline motion regions."""

    x = points[:, 0] - 0.5
    y = points[:, 1]
    z = points[:, 2]
    if float(y.max()) < 0.46 and np.all(np.abs(x) > 0.12):
        station = "front" if float(z.mean()) < 0.50 else "rear"
        side = "left" if float(x.mean()) < 0 else "right"
        return f"leg_{station}_{side}"
    if float(z.mean()) < 0.30 and float(y.mean()) > 0.34:
        return "head"
    if float(z.mean()) > 0.72 and float(y.mean()) > 0.25:
        return "tail"
    return "body"


def _classify_spider(points: np.ndarray) -> str:
    x = points[:, 0] - 0.5
    if np.all(np.abs(x) > 0.19):
        side_offset = 0 if float(x.mean()) < 0 else 4
        station = min(3, max(0, int(float(points[:, 2].mean()) * 4.0)))
        return f"web_leg_{side_offset + station}"
    return "body"


def _classify_octopus(points: np.ndarray) -> str:
    x = points[:, 0] - 0.5
    z = points[:, 2] - 0.5
    radius = np.sqrt(x * x + z * z)
    if float(points[:, 1].max()) < 0.45 and np.all(radius > 0.10):
        angle = math.atan2(float(z.mean()), float(x.mean()))
        sector = int(math.floor((angle + math.pi) * 8.0 / (2.0 * math.pi))) % 8
        return f"tentacle{sector}"
    return "body"


def _classify_squid_regions(units: np.ndarray, triangles: np.ndarray) -> Dict[str, list[int]]:
    """Split the longitudinal Tripo squid into eight arms and two long tentacles.

    The source swims along Z.  Appendages are grouped around that axis; the two
    sectors reaching furthest towards negative Z are the catching tentacles.
    """

    centroids = units[triangles].mean(axis=1)
    radial = np.sqrt((centroids[:, 0] - 0.5) ** 2 + (centroids[:, 1] - 0.5) ** 2)
    appendage = (centroids[:, 2] < 0.58) & (radial > 0.075)
    angles = (np.arctan2(centroids[:, 1] - 0.5, centroids[:, 0] - 0.5) + math.pi) % (2.0 * math.pi)
    sectors = np.floor(angles * 10.0 / (2.0 * math.pi)).astype(np.int64) % 10

    reaches = []
    for sector in range(10):
        sector_faces = np.flatnonzero(appendage & (sectors == sector))
        reach = float(np.quantile(centroids[sector_faces, 2], 0.08)) if len(sector_faces) else math.inf
        reaches.append((reach, sector))
    catching_sectors = {sector for _, sector in sorted(reaches)[:2]}
    arm_sectors = [sector for sector in range(10) if sector not in catching_sectors]
    arm_names = {sector: f"arm{index}" for index, sector in enumerate(arm_sectors)}
    catching_names = {
        sector: f"catching_tentacle{index}"
        for index, sector in enumerate(sorted(catching_sectors))
    }

    regions: Dict[str, list[int]] = {
        "body": [],
        **{f"arm{index}": [] for index in range(8)},
        "catching_tentacle0": [],
        "catching_tentacle1": [],
    }
    for face_index in range(len(triangles)):
        if not appendage[face_index]:
            regions["body"].append(face_index)
            continue
        sector = int(sectors[face_index])
        bone = catching_names[sector] if sector in catching_names else arm_names[sector]
        regions[bone].append(face_index)
    return regions


def _classify_glow_squid_regions(units: np.ndarray, triangles: np.ndarray) -> Dict[str, list[int]]:
    """Split the upright glow squid into eight arms and two catching tentacles.

    Unlike the longitudinal normal squid, this source has its mantle on +Y and
    all appendages extend towards -Y.  Angular sectors around Y preserve the
    visibly separate limbs; the two sectors reaching furthest down become the
    catching tentacles.
    """

    centroids = units[triangles].mean(axis=1)
    radial = np.sqrt((centroids[:, 0] - 0.5) ** 2 + (centroids[:, 2] - 0.5) ** 2)
    appendage = (centroids[:, 1] < 0.48) & (radial > 0.075)
    angles = (np.arctan2(centroids[:, 2] - 0.5, centroids[:, 0] - 0.5) + math.pi) % (2.0 * math.pi)
    sectors = np.floor(angles * 10.0 / (2.0 * math.pi)).astype(np.int64) % 10

    reaches = []
    for sector in range(10):
        sector_faces = np.flatnonzero(appendage & (sectors == sector))
        reach = float(np.quantile(centroids[sector_faces, 1], 0.08)) if len(sector_faces) else math.inf
        reaches.append((reach, sector))
    catching_sectors = {sector for _, sector in sorted(reaches)[:2]}
    arm_sectors = [sector for sector in range(10) if sector not in catching_sectors]
    arm_names = {sector: f"arm{index}" for index, sector in enumerate(arm_sectors)}
    catching_names = {
        sector: f"catching_tentacle{index}"
        for index, sector in enumerate(sorted(catching_sectors))
    }

    regions: Dict[str, list[int]] = {
        "body": [],
        **{f"arm{index}": [] for index in range(8)},
        "catching_tentacle0": [],
        "catching_tentacle1": [],
    }
    for face_index in range(len(triangles)):
        if not appendage[face_index]:
            regions["body"].append(face_index)
            continue
        sector = int(sectors[face_index])
        bone = catching_names[sector] if sector in catching_names else arm_names[sector]
        regions[bone].append(face_index)
    return regions


def _classify_bat(points: np.ndarray) -> str:
    x = points[:, 0] - 0.5
    absolute = np.abs(x)
    if np.all(absolute > 0.12):
        side = "left" if float(x.mean()) < 0 else "right"
        suffix = "_tip" if np.all(absolute > 0.31) else ""
        return f"{side}_wing{suffix}"
    if float(points[:, 1].min()) > 0.62:
        return "head"
    return "body"


def _classify_allay(points: np.ndarray) -> str:
    x = points[:, 0] - 0.5
    y = points[:, 1]
    z = points[:, 2] - 0.5
    absolute_x = np.abs(x)
    if float(y.min()) > 0.68 and float(absolute_x.max()) < 0.40:
        return "head"
    if 0.26 < float(y.mean()) < 0.70 and np.all(absolute_x > 0.20) and np.all(absolute_x < 0.36):
        return "right_arm" if float(x.mean()) < 0 else "left_arm"
    if np.all(absolute_x > 0.34):
        side = "right" if float(x.mean()) < 0 else "left"
        suffix = "_tip" if np.all(absolute_x > 0.44) else ""
        return f"{side}_wing{suffix}"
    if 0.42 < float(y.mean()) < 0.60 and float(absolute_x.max()) < 0.12 and float(z.mean()) > 0.38:
        return "soul_core"
    return "body"


CLASSIFIERS: Mapping[str, Callable[[np.ndarray], str]] = {
    "humanoid": _classify_humanoid,
    "quadruped": _classify_quadruped,
    "bear": _classify_bear,
    "spider": _classify_spider,
    "octopus": _classify_octopus,
    "bat": _classify_bat,
    "allay": _classify_allay,
    "axolotl": _classify_axolotl,
    "ocelot": _classify_ocelot,
}


def _pivots(spec: MobSpec, positions: np.ndarray) -> Dict[str, tuple[float, float, float]]:
    width = float(np.ptp(positions[:, 0]))
    height = float(np.ptp(positions[:, 1]))
    depth = float(np.ptp(positions[:, 2]))
    body = (0.0, 24.0 - height * 0.50, 0.0)
    result = {name: body for name in spec.bones}
    if spec.classifier == "humanoid":
        result.update({
            "head": (0.0, 24.0 - height * 0.72, -depth * 0.04),
            "left_arm": (-width * 0.22, 24.0 - height * 0.64, 0.0),
            "right_arm": (width * 0.22, 24.0 - height * 0.64, 0.0),
            "left_leg": (-width * 0.11, 24.0 - height * 0.38, 0.0),
            "right_leg": (width * 0.11, 24.0 - height * 0.38, 0.0),
        })
    elif spec.classifier == "quadruped":
        for station, z in (("front", -depth * 0.27), ("rear", depth * 0.27)):
            result[f"leg_{station}_left"] = (-width * 0.27, 24.0 - height * 0.34, z)
            result[f"leg_{station}_right"] = (width * 0.27, 24.0 - height * 0.34, z)
    elif spec.classifier == "bear":
        result["head"] = (0.0, 24.0 - height * 0.62, -depth * 0.31)
        for station, z in (("front", -depth * 0.25), ("rear", depth * 0.29)):
            result[f"leg_{station}_left"] = (-width * 0.25, 24.0 - height * 0.42, z)
            result[f"leg_{station}_right"] = (width * 0.25, 24.0 - height * 0.42, z)
    elif spec.classifier == "axolotl":
        result["head"] = (0.0, 24.0 - height * 0.55, -depth * 0.31)
        result["tail"] = (0.0, 24.0 - height * 0.50, depth * 0.24)
        for station, z in (("front", -depth * 0.20), ("rear", depth * 0.19)):
            result[f"leg_{station}_left"] = (-width * 0.24, 24.0 - height * 0.25, z)
            result[f"leg_{station}_right"] = (width * 0.24, 24.0 - height * 0.25, z)
    elif spec.classifier == "ocelot":
        result["head"] = (0.0, 24.0 - height * 0.68, -depth * 0.32)
        result["tail"] = (0.0, 24.0 - height * 0.56, depth * 0.29)
        for station, z in (("front", -depth * 0.23), ("rear", depth * 0.24)):
            result[f"leg_{station}_left"] = (-width * 0.24, 24.0 - height * 0.34, z)
            result[f"leg_{station}_right"] = (width * 0.24, 24.0 - height * 0.34, z)
    elif spec.classifier == "spider":
        for index in range(8):
            side = -1.0 if index < 4 else 1.0
            station = index if index < 4 else index - 4
            result[f"web_leg_{index}"] = (
                side * width * 0.20,
                24.0 - height * 0.32,
                depth * (-0.34 + station * 0.226),
            )
    elif spec.classifier == "octopus":
        for index in range(8):
            angle = -math.pi + (index + 0.5) * 2.0 * math.pi / 8.0
            result[f"tentacle{index}"] = (
                math.cos(angle) * width * 0.18,
                24.0 - height * 0.30,
                math.sin(angle) * depth * 0.18,
            )
    elif spec.classifier in ("squid", "glow_squid"):
        appendage_names = (*tuple(f"arm{index}" for index in range(8)), "catching_tentacle0", "catching_tentacle1")
        for index, name in enumerate(appendage_names):
            angle = -math.pi + (index + 0.5) * 2.0 * math.pi / 10.0
            result[name] = (
                math.cos(angle) * width * 0.13,
                24.0 - height * (0.50 + math.sin(angle) * 0.13),
                -depth * 0.08,
            )
    elif spec.classifier == "bat":
        result["head"] = (0.0, 24.0 - height * 0.66, -depth * 0.12)
        result["left_wing"] = (-width * 0.14, 24.0 - height * 0.55, 0.0)
        result["right_wing"] = (width * 0.14, 24.0 - height * 0.55, 0.0)
        result["left_wing_tip"] = (-width * 0.33, 24.0 - height * 0.54, 0.0)
        result["right_wing_tip"] = (width * 0.33, 24.0 - height * 0.54, 0.0)
    elif spec.classifier == "allay":
        result.update({
            "head": (0.0, 24.0 - height * 0.70, depth * 0.04),
            "right_arm": (-width * 0.20, 24.0 - height * 0.55, depth * 0.05),
            "left_arm": (width * 0.20, 24.0 - height * 0.55, depth * 0.05),
            "right_wing": (-width * 0.25, 24.0 - height * 0.58, -depth * 0.06),
            "right_wing_tip": (-width * 0.42, 24.0 - height * 0.56, -depth * 0.08),
            "left_wing": (width * 0.25, 24.0 - height * 0.58, -depth * 0.06),
            "left_wing_tip": (width * 0.42, 24.0 - height * 0.56, -depth * 0.08),
            "soul_core": (0.0, 24.0 - height * 0.53, depth * 0.28),
        })
    return result


def _texture_png(mesh: MeshData) -> bytes:
    output = BytesIO()
    mesh.base_colour.save(output, format="PNG", optimize=False, compress_level=9)
    return output.getvalue()


def build_runtime_assets(name: str, mesh: MeshData) -> tuple[bytes, bytes, dict]:
    try:
        spec = MOB_SPECS[name]
    except KeyError as exc:
        raise ValueError(f"Unknown exact Tripo mob: {name}") from exc
    source_triangle_count = int(len(mesh.triangles))
    discarded_detached_triangles = 0
    if name == "polar_bear":
        mesh = largest_component_mesh(mesh)
        discarded_detached_triangles = source_triangle_count - int(len(mesh.triangles))
    positions = transform_positions(
        mesh.positions,
        longest_span=None,
        fit_axis=spec.fit_axis,
        fit_span=spec.fit_span,
    )
    units = _unit_positions(np.asarray(mesh.positions, dtype=np.float64))
    triangles = np.asarray(mesh.triangles, dtype=np.int64)
    uvs = np.asarray(mesh.uvs, dtype=np.float64)
    if triangles.ndim != 2 or triangles.shape[1] != 3 or not len(triangles):
        raise ValueError("Mesh triangles must be a non-empty Nx3 array")
    if triangles.min() < 0 or triangles.max() >= len(positions) or uvs.shape != (len(positions), 2):
        raise ValueError("Mesh indices or UVs are invalid")
    if not np.isfinite(uvs).all():
        raise ValueError("Mesh UVs must be finite")

    if spec.classifier == "squid":
        regions = _classify_squid_regions(units, triangles)
    elif spec.classifier == "glow_squid":
        regions = _classify_glow_squid_regions(units, triangles)
    else:
        classifier = CLASSIFIERS[spec.classifier]
        regions = {bone: [] for bone in spec.bones}
        for face_index, triangle in enumerate(triangles):
            bone = classifier(units[triangle])
            if bone not in regions:
                raise ValueError(f"Classifier returned an undeclared bone: {bone}")
            regions[bone].append(face_index)
    if sum(map(len, regions.values())) != len(triangles):
        raise AssertionError("Exact mesh classification lost triangles")

    pivots = _pivots(spec, positions)
    payload = bytearray(MAGIC)
    payload.extend(struct.pack("<I", len(spec.bones)))
    for bone in spec.bones:
        encoded = bone.encode("utf-8")
        payload.extend(struct.pack("<H", len(encoded)))
        payload.extend(encoded)
        payload.extend(struct.pack("<3f", *pivots[bone]))
        payload.extend(struct.pack("<I", len(regions[bone])))
        for face_index in regions[bone]:
            triangle = triangles[face_index]
            # Y-up to Y-down is a reflection; reverse winding to retain outward normals.
            for vertex_index in (int(triangle[0]), int(triangle[2]), int(triangle[1])):
                point = positions[vertex_index]
                uv = uvs[vertex_index]
                values = (float(point[0]), float(point[1]), float(point[2]), float(uv[0]), float(uv[1]))
                if not all(math.isfinite(value) for value in values):
                    raise ValueError("Runtime vertex contains a non-finite value")
                payload.extend(struct.pack("<5f", *values))
    report = {
        "mob": name,
        "source_triangles": source_triangle_count,
        "output_triangles": int(sum(map(len, regions.values()))),
        "discarded_detached_triangles": discarded_detached_triangles,
        "bones": {bone: len(regions[bone]) for bone in spec.bones},
        "texture_width": mesh.base_colour.width,
        "texture_height": mesh.base_colour.height,
        "fit_axis": spec.fit_axis,
        "fit_span": spec.fit_span,
        "cubes": 0,
    }
    return bytes(payload), _texture_png(mesh), report


def decode_mesh(payload: bytes) -> Dict[str, dict]:
    view = memoryview(payload)
    if len(view) < 12 or bytes(view[:8]) != MAGIC:
        raise ValueError("Runtime mesh has an invalid header")
    offset = 8

    def take(fmt: str):
        nonlocal offset
        size = struct.calcsize(fmt)
        if offset + size > len(view):
            raise ValueError("Runtime mesh is truncated")
        values = struct.unpack_from(fmt, view, offset)
        offset += size
        return values

    count = take("<I")[0]
    parts: Dict[str, dict] = {}
    for _ in range(count):
        name_length = take("<H")[0]
        if offset + name_length > len(view):
            raise ValueError("Runtime mesh name is truncated")
        name = bytes(view[offset:offset + name_length]).decode("utf-8")
        offset += name_length
        pivot = tuple(take("<3f"))
        face_count = take("<I")[0]
        faces = []
        for _ in range(face_count):
            corners = []
            for _ in range(3):
                values = take("<5f")
                corners.append((tuple(values[:3]), tuple(values[3:])))
            faces.append(tuple(corners))
        if not name or name in parts:
            raise ValueError(f"Invalid duplicate runtime bone: {name!r}")
        parts[name] = {"pivot": pivot, "faces": faces}
    if offset != len(view):
        raise ValueError("Runtime mesh contains trailing bytes")
    return parts


def _atomic_write(path: Path, payload: bytes) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    descriptor, temporary = tempfile.mkstemp(prefix=f".{path.name}.", suffix=".tmp", dir=path.parent)
    try:
        with os.fdopen(descriptor, "wb") as handle:
            handle.write(payload)
            handle.flush()
            os.fsync(handle.fileno())
        os.replace(temporary, path)
    finally:
        Path(temporary).unlink(missing_ok=True)


def export_one(name: str, glb: Path, asset_root: Path = RUNTIME_ASSET_ROOT) -> dict:
    mesh_bytes, texture_bytes, report = build_runtime_assets(name, load_glb(Path(glb)))
    _atomic_write(Path(asset_root) / f"meshes/entity/custom3d/{name}.mesh", mesh_bytes)
    _atomic_write(Path(asset_root) / f"textures/entity/custom3d/exact/{name}.png", texture_bytes)
    report_bytes = (json.dumps(report, indent=2, sort_keys=True) + "\n").encode("utf-8")
    _atomic_write(Path(asset_root) / f"meshes/entity/custom3d/{name}.report.json", report_bytes)
    return report


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("mob", choices=tuple(MOB_SPECS))
    parser.add_argument("glb", type=Path)
    parser.add_argument("--asset-root", type=Path, default=RUNTIME_ASSET_ROOT)
    args = parser.parse_args(argv)
    try:
        report = export_one(args.mob, args.glb, args.asset_root)
    except (OSError, ValueError) as exc:
        parser.exit(1, f"EXACT_MOB_EXPORT_FAILED: {exc}\n")
    print(
        f"EXACT_MOB_EXPORT_PASS mob={args.mob} triangles={report['output_triangles']} "
        f"bones={len(report['bones'])} texture={report['texture_width']}x{report['texture_height']} cubes=0"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
