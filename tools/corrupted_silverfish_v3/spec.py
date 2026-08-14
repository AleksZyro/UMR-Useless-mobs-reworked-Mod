"""Canonical deterministic model specification for Corrupted Silverfish v3."""

from dataclasses import dataclass
from types import MappingProxyType
from typing import Dict, List, Mapping, Optional, Sequence, Tuple


Vec3 = Tuple[float, float, float]
CUBE_ROTATION_CONVENTION = (
    "rotation contains three Euler angles in degrees in X/Y/Z order and rotates "
    "around the cube center returned by cube_pivot(cube): origin + size / 2"
)


@dataclass(frozen=True)
class Bone:
    name: str
    parent: Optional[str]
    pivot: Vec3


@dataclass(frozen=True)
class Cube:
    """Cuboid whose rotation is X/Y/Z Euler degrees around its geometric center.

    The center of rotation is ``origin + size / 2`` on every axis, exactly as
    returned by :func:`cube_pivot`.
    """

    name: str
    bone: str
    origin: Vec3
    size: Vec3
    material: str
    category: str
    rotation: Vec3 = (0.0, 0.0, 0.0)


def cube_pivot(cube: Cube) -> Vec3:
    """Return the cube rotation pivot, its exact geometric center."""

    return (
        cube.origin[0] + cube.size[0] / 2.0,
        cube.origin[1] + cube.size[1] / 2.0,
        cube.origin[2] + cube.size[2] / 2.0,
    )


_bones: List[Bone] = [
    Bone("root", None, (0.0, 0.0, 0.0)),
    Bone("body", "root", (0.0, 3.0, 1.0)),
    Bone("head", "body", (0.0, 4.0, -8.0)),
    Bone("thorax", "body", (0.0, 4.0, -3.0)),
    Bone("shell_front", "thorax", (0.0, 4.5, -1.0)),
    Bone("shell_mid", "shell_front", (0.0, 4.5, 4.0)),
    Bone("shell_rear", "shell_mid", (0.0, 4.0, 8.0)),
    Bone("abdomen", "shell_rear", (0.0, 3.5, 12.0)),
    Bone("tail_base", "abdomen", (0.0, 3.0, 15.0)),
    Bone("tail_tip", "tail_base", (0.0, 2.5, 18.0)),
]

for side, x in (("left", 4.0), ("right", -4.0)):
    for position, z in (("front", -4.0), ("mid", 2.0), ("rear", 8.0)):
        upper = f"leg_{side}_{position}_upper"
        _bones.append(Bone(upper, "body", (x, 2.5, z)))
        _bones.append(Bone(f"leg_{side}_{position}_lower", upper, (x * 1.25, 1.2, z + 0.5)))

_bones.extend(
    (
        Bone("mandible_left", "head", (2.0, 2.5, -12.0)),
        Bone("mandible_right", "head", (-2.0, 2.5, -12.0)),
        Bone("mouth_core", "head", (0.0, 2.5, -12.0)),
    )
)

for number, (parent, pivot) in enumerate(
    (
        ("thorax", (2.8, 5.5, -6.0)),
        ("shell_front", (0.0, 7.0, -2.0)),
        ("shell_mid", (0.0, 7.5, 3.0)),
        ("shell_rear", (0.0, 7.0, 8.0)),
        ("shell_mid", (-3.2, 5.0, 4.0)),
        ("abdomen", (2.4, 4.5, 12.0)),
        ("head", (-3.2, 5.0, -10.0)),
    ),
    start=1,
):
    _bones.append(Bone(f"crystal_cluster_{number}", parent, pivot))

BONES: Tuple[Bone, ...] = tuple(_bones)


def channel(times: Sequence[float], vectors: Sequence[Vec3]) -> Dict[str, dict]:
    """Create a deterministic GeckoLib channel with linear Vec3 keyframes."""

    if len(times) != len(vectors):
        raise ValueError("animation channel times and vectors must have equal lengths")
    return {
        f"{time:g}": {"post": list(vector), "lerp_mode": "linear"}
        for time, vector in zip(times, vectors)
    }


def _animation_specs() -> Dict[str, dict]:
    """Return the single canonical source used by all animation exporters."""

    idle_bones: Dict[str, dict] = {
        "body": {"position": channel((0, 0.8, 1.6), ((0, 0, 0), (0, 0.12, 0), (0, 0, 0)))},
    }
    main_segments = (
        "head", "thorax", "shell_front", "shell_mid",
        "shell_rear", "abdomen", "tail_base", "tail_tip",
    )
    for index, bone in enumerate(main_segments):
        amplitude = 1.5 if index % 2 == 0 else -1.5
        idle_bones[bone] = {
            "rotation": channel((0, 0.8, 1.6), ((0, 0, 0), (0, amplitude, 0), (0, 0, 0)))
        }
    for number in range(1, 8):
        idle_bones[f"crystal_cluster_{number}"] = {
            "scale": channel((0, 0.8, 1.6), ((1, 1, 1), (1.025, 1.025, 1.025), (1, 1, 1)))
        }

    walk_times = (0, 0.2, 0.4, 0.6, 0.8)
    walk_bones: Dict[str, dict] = {}
    for index, bone in enumerate(main_segments):
        amplitude = 3 if index % 2 == 0 else -3
        walk_bones[bone] = {
            "rotation": channel(
                walk_times,
                ((0, amplitude, 0), (0, 0, 0), (0, -amplitude, 0), (0, 0, 0), (0, amplitude, 0)),
            )
        }
    first_tripod = {("left", "front"), ("right", "mid"), ("left", "rear")}
    for side in ("left", "right"):
        for position in ("front", "mid", "rear"):
            phase = 1 if (side, position) in first_tripod else -1
            walk_bones[f"leg_{side}_{position}_upper"] = {
                "rotation": channel(
                    walk_times,
                    tuple((phase * value, 0, 0) for value in (16, 0, -16, 0, 16)),
                )
            }
            walk_bones[f"leg_{side}_{position}_lower"] = {
                "rotation": channel(
                    walk_times,
                    tuple((-phase * value, 0, 0) for value in (10, 0, -10, 0, 10)),
                )
            }

    attack_bones: Dict[str, dict] = {
        "head": {"position": channel((0, 0.225, 0.45), ((0, 0, 0), (0, 0, -1.2), (0, 0, 0)))},
        "mandible_left": {"rotation": channel((0, 0.225, 0.45), ((0, 0, 0), (0, 24, 0), (0, 0, 0)))},
        "mandible_right": {"rotation": channel((0, 0.225, 0.45), ((0, 0, 0), (0, -24, 0), (0, 0, 0)))},
        "leg_left_front_upper": {"rotation": channel((0, 0.225, 0.45), ((0, 0, 0), (8, 0, 0), (0, 0, 0)))},
        "leg_right_front_upper": {"rotation": channel((0, 0.225, 0.45), ((0, 0, 0), (-8, 0, 0), (0, 0, 0)))},
    }

    hurt_times = (0, 0.1, 0.2, 0.3)
    hurt_bones: Dict[str, dict] = {
        "body": {"rotation": channel(hurt_times, ((0, 0, 0), (0, 0, 6), (0, 0, -2), (0, 0, 0)))},
    }
    for bone in ("shell_front", "shell_mid", "shell_rear"):
        hurt_bones[bone] = {
            "scale": channel(hurt_times, ((1, 1, 1), (0.96, 0.96, 0.96), (1.02, 1.02, 1.02), (1, 1, 1)))
        }
    for number in range(1, 8):
        parent_is_scaled = number in (2, 3, 4, 5)
        pulse = 1.04 / 0.96 if parent_is_scaled else 1.04
        recoil = 0.98 / 1.02 if parent_is_scaled else 0.98
        hurt_bones[f"crystal_cluster_{number}"] = {
            "scale": channel(
                hurt_times,
                ((1, 1, 1), (pulse, pulse, pulse), (recoil, recoil, recoil), (1, 1, 1)),
            )
        }

    death_times = (0, 0.55, 1.1)
    death_bones: Dict[str, dict] = {
        "body": {"position": channel(death_times, ((0, 0, 0), (0, -0.4, 0), (0, -1.4, 0)))},
        "tail_base": {"rotation": channel(death_times, ((0, 0, 0), (3, 0, 0), (6, 0, 0)))},
        "tail_tip": {"rotation": channel(death_times, ((0, 0, 0), (6, 0, 0), (12, 0, 0)))},
    }
    for side, fold in (("left", 48), ("right", -48)):
        for position in ("front", "mid", "rear"):
            death_bones[f"leg_{side}_{position}_upper"] = {
                "rotation": channel(death_times, ((0, 0, 0), (0, 0, fold / 2), (0, 0, fold)))
            }
    for number in range(1, 8):
        death_bones[f"crystal_cluster_{number}"] = {
            "scale": channel(death_times, ((1, 1, 1), (0.94, 0.94, 0.94), (0.82, 0.82, 0.82)))
        }

    return {
        "animation.corrupted_silverfish.idle": {"loop": True, "animation_length": 1.6, "bones": idle_bones},
        "animation.corrupted_silverfish.walk": {"loop": True, "animation_length": 0.8, "bones": walk_bones},
        "animation.corrupted_silverfish.attack": {"loop": False, "animation_length": 0.45, "bones": attack_bones},
        "animation.corrupted_silverfish.hurt": {"loop": False, "animation_length": 0.3, "bones": hurt_bones},
        "animation.corrupted_silverfish.death": {"loop": False, "animation_length": 1.1, "bones": death_bones},
    }


def _freeze(value):
    """Recursively freeze canonical animation data against accidental drift."""

    if isinstance(value, dict):
        return MappingProxyType({key: _freeze(item) for key, item in value.items()})
    if isinstance(value, (list, tuple)):
        return tuple(_freeze(item) for item in value)
    return value


ANIMATION_SPECS: Mapping[str, Mapping] = _freeze(_animation_specs())
ANIMATIONS: Mapping[str, float] = MappingProxyType(
    {
        animation_id.removeprefix("animation.corrupted_silverfish."): animation["animation_length"]
        for animation_id, animation in ANIMATION_SPECS.items()
    }
)


_cubes: List[Cube] = []


def add_box(
    name: str,
    bone: str,
    origin: Vec3,
    size: Vec3,
    material: str,
    category: str,
    rotation: Vec3 = (0.0, 0.0, 0.0),
) -> None:
    _cubes.append(Cube(name, bone, origin, size, material, category, rotation))


def add_pair(
    name: str,
    bone: str,
    x_inner: float,
    y: float,
    z: float,
    size: Vec3,
    material: str,
    category: str,
    rotation: Vec3 = (0.0, 0.0, 0.0),
) -> None:
    width = size[0]
    add_box(f"{name}_left", bone, (x_inner, y, z), size, material, category, rotation)
    add_box(
        f"{name}_right",
        bone,
        (-x_inner - width, y, z),
        size,
        material,
        category,
        (rotation[0], -rotation[1], -rotation[2]),
    )


def add_plate_stack(
    name: str,
    bone: str,
    origin: Vec3,
    sizes: Sequence[Vec3],
) -> None:
    x, y, z = origin
    for level, size in enumerate(sizes, start=1):
        inset = 0.25 * (level - 1)
        add_box(
            f"{name}_{level}",
            bone,
            (x + inset, y + 0.45 * (level - 1), z + 0.12 * (level - 1)),
            size,
            "armor" if level == 1 else "armor_dark",
            "layered_armor",
        )


def add_crystal_cluster(number: int, boxes: Sequence[Tuple[Vec3, Vec3, Vec3]]) -> None:
    bone = f"crystal_cluster_{number}"
    for index, (origin, size, rotation) in enumerate(boxes, start=1):
        add_box(
            f"crystal_{number}_{index}",
            bone,
            origin,
            size,
            "crystal",
            "crystals",
            rotation,
        )


# Body core and tail (27): broad head, six staggered core/underside segments,
# followed by a multi-stage taper reaching the long rear silhouette.
add_box("head_core", "head", (-4.5, 2.0, -12.0), (9.0, 3.5, 5.0), "armor_dark", "body_core_and_tail")
add_box("head_brow", "head", (-4.0, 5.0, -11.5), (8.0, 1.0, 3.5), "armor", "body_core_and_tail")
add_pair("head_cheek_core", "head", 2.8, 2.5, -12.8, (1.7, 2.0, 3.0), "armor_dark", "body_core_and_tail")
add_pair("eye", "head", 3.45, 4.1, -13.05, (0.7, 0.7, 0.5), "eye", "body_core_and_tail")

for name, bone, origin, size in (
    ("thorax_core", "thorax", (-5.0, 2.2, -7.0), (10.0, 3.6, 5.0)),
    ("front_core", "shell_front", (-5.2, 2.0, -2.5), (10.4, 4.0, 4.5)),
    ("middle_core", "shell_mid", (-4.8, 1.9, 1.5), (9.6, 4.1, 4.5)),
    ("rear_core", "shell_rear", (-4.2, 1.8, 5.5), (8.4, 3.9, 4.5)),
    ("abdomen_core", "abdomen", (-3.5, 1.7, 9.5), (7.0, 3.5, 4.0)),
    ("tail_root_core", "tail_base", (-2.8, 1.6, 13.0), (5.6, 3.0, 3.5)),
):
    add_box(name, bone, origin, size, "armor_dark", "body_core_and_tail")

for index, (bone, x, width, z, depth) in enumerate(
    (
        ("thorax", -4.4, 8.8, -6.5, 4.0),
        ("shell_front", -4.6, 9.2, -2.0, 3.8),
        ("shell_mid", -4.2, 8.4, 2.0, 3.8),
        ("shell_rear", -3.7, 7.4, 6.0, 3.7),
        ("abdomen", -3.0, 6.0, 10.0, 3.5),
        ("tail_base", -2.3, 4.6, 13.5, 3.0),
    ),
    start=1,
):
    add_box(f"underside_segment_{index}", bone, (x, 0.8, z), (width, 1.4, depth), "underside", "body_core_and_tail")

for index, (bone, origin, size) in enumerate(
    (
        ("tail_base", (-2.4, 1.5, 14.5), (4.8, 2.8, 2.0)),
        ("tail_base", (-2.0, 1.4, 16.2), (4.0, 2.5, 1.8)),
        ("tail_tip", (-1.7, 1.3, 17.7), (3.4, 2.2, 1.6)),
        ("tail_tip", (-1.4, 1.2, 19.0), (2.8, 1.9, 1.3)),
        ("tail_tip", (-1.0, 1.1, 20.0), (2.0, 1.5, 1.0)),
        ("tail_base", (-1.8, 3.6, 15.5), (3.6, 0.8, 1.4)),
        ("tail_tip", (-1.4, 3.2, 17.3), (2.8, 0.7, 1.3)),
        ("tail_tip", (-1.0, 2.9, 18.7), (2.0, 0.6, 1.1)),
        ("tail_tip", (-0.6, 2.5, 19.8), (1.2, 0.5, 1.0)),
    ),
    start=1,
):
    add_box(f"tail_taper_{index}", bone, origin, size, "armor_dark", "body_core_and_tail")


# Layered armour (38): distinct forehead, cheek, lateral and dorsal plates.
for name, origin, size, rotation in (
    ("forehead_left", (-4.2, 5.7, -12.2), (3.8, 0.7, 2.8), (0.0, 0.0, -4.0)),
    ("forehead_right", (0.4, 5.7, -12.2), (3.8, 0.7, 2.8), (0.0, 0.0, 4.0)),
    ("forehead_crown", (-2.5, 6.1, -11.4), (5.0, 0.6, 2.2), (0.0, 0.0, 0.0)),
    ("forehead_nose", (-1.5, 5.5, -13.2), (3.0, 0.8, 1.3), (-6.0, 0.0, 0.0)),
):
    add_box(name, "head", origin, size, "armor", "layered_armor", rotation)

for index, (x, y, z, size) in enumerate(
    (
        (3.8, 3.0, -12.5, (1.5, 2.3, 2.5)),
        (4.2, 3.6, -10.5, (1.2, 2.0, 2.2)),
        (3.6, 2.4, -9.0, (1.4, 1.8, 2.0)),
    ),
    start=1,
):
    add_pair(f"cheek_plate_{index}", "head", x, y, z, size, "armor_dark", "layered_armor", (0.0, 8.0, 5.0))

for index, (bone, x, y, z, size) in enumerate(
    (
        ("thorax", 4.8, 3.1, -7.0, (1.4, 2.6, 3.0)),
        ("shell_front", 5.1, 3.0, -3.5, (1.5, 2.8, 3.2)),
        ("shell_front", 5.0, 3.0, -0.5, (1.4, 2.6, 2.8)),
        ("shell_mid", 4.6, 2.8, 2.5, (1.4, 2.5, 3.0)),
        ("shell_rear", 3.9, 2.6, 6.2, (1.3, 2.3, 3.0)),
    ),
    start=1,
):
    add_pair(f"side_plate_{index}", bone, x, y, z, size, "armor", "layered_armor", (0.0, 5.0, 4.0))

for name, bone, origin, sizes in (
    ("dorsal_thorax", "thorax", (-4.4, 5.7, -6.3), ((8.8, 0.8, 2.8), (8.3, 0.7, 2.5), (7.8, 0.6, 2.2))),
    ("dorsal_front", "shell_front", (-4.7, 6.0, -2.8), ((9.4, 0.8, 2.8), (8.9, 0.7, 2.5), (8.4, 0.6, 2.2))),
    ("dorsal_mid", "shell_mid", (-4.4, 6.0, 1.4), ((8.8, 0.8, 2.8), (8.3, 0.7, 2.5), (7.8, 0.6, 2.2))),
    ("dorsal_rear", "shell_rear", (-3.8, 5.7, 5.7), ((7.6, 0.8, 2.7), (7.1, 0.7, 2.4), (6.6, 0.6, 2.1))),
    ("dorsal_abdomen", "abdomen", (-3.1, 5.1, 9.8), ((6.2, 0.8, 2.5), (5.7, 0.7, 2.2), (5.2, 0.6, 1.9))),
    ("dorsal_tail", "tail_base", (-2.3, 4.5, 13.5), ((4.6, 0.7, 2.2), (4.1, 0.6, 1.9), (3.6, 0.5, 1.6))),
):
    add_plate_stack(name, bone, origin, sizes)


# Six independently articulated legs, four cubes each (24).
for side, direction in (("left", 1.0), ("right", -1.0)):
    for position, z in (("front", -5.0), ("mid", 1.0), ("rear", 7.0)):
        upper_bone = f"leg_{side}_{position}_upper"
        lower_bone = f"leg_{side}_{position}_lower"
        upper_x = 4.2 if direction > 0 else -5.8
        lower_x = 5.0 if direction > 0 else -6.5
        add_box(f"leg_{side}_{position}_upper", upper_bone, (upper_x, 2.0, z), (1.6, 1.3, 2.8), "leg", "legs_and_feet", (0.0, 0.0, -12.0 * direction))
        add_box(f"leg_{side}_{position}_lower", lower_bone, (lower_x, 0.8, z + 0.4), (1.5, 1.8, 2.1), "leg", "legs_and_feet", (0.0, 0.0, 10.0 * direction))
        add_box(f"foot_{side}_{position}", lower_bone, (5.2 if direction > 0 else -6.7, 0.0, z - 0.1), (1.5, 0.8, 2.8), "leg", "legs_and_feet")
        add_box(f"toe_{side}_{position}", lower_bone, (5.5 if direction > 0 else -7.0, 0.0, z - 0.6), (1.0, 0.45, 1.2), "armor_dark", "legs_and_feet")


# Two jaws and two central mouth/sensor elements (4).
add_box("mandible_left_cube", "mandible_left", (1.0, 1.3, -15.4), (2.0, 1.2, 3.4), "mandible", "mandibles_and_mouth", (0.0, -10.0, 0.0))
add_box("mandible_right_cube", "mandible_right", (-3.0, 1.3, -15.4), (2.0, 1.2, 3.4), "mandible", "mandibles_and_mouth", (0.0, 10.0, 0.0))
add_box("mouth_core_cube", "mouth_core", (-1.0, 2.0, -14.0), (2.0, 1.2, 1.5), "mandible", "mandibles_and_mouth")
add_box("mouth_sensor_cube", "mouth_core", (-0.4, 2.6, -15.0), (0.8, 0.8, 1.5), "eye", "mandibles_and_mouth", (-12.0, 0.0, 0.0))


# Asymmetric stepped crystal clusters, counts 3, 3, 4, 3, 2, 2, 2 (19).
add_crystal_cluster(1, (((2.2, 6.0, -6.5), (1.2, 3.4, 1.4), (0.0, 0.0, -8.0)), ((3.2, 5.8, -5.8), (0.9, 2.5, 1.0), (8.0, 0.0, 12.0)), ((1.5, 5.9, -5.2), (0.7, 1.8, 0.8), (-5.0, 0.0, -5.0))))
add_crystal_cluster(2, (((-0.7, 7.0, -2.5), (1.4, 4.2, 1.5), (0.0, 0.0, -4.0)), ((0.6, 7.1, -1.7), (0.9, 2.8, 1.0), (5.0, 0.0, 10.0)), ((-1.5, 6.8, -1.2), (0.8, 2.1, 0.9), (-7.0, 0.0, -8.0))))
add_crystal_cluster(3, (((-0.8, 7.4, 2.5), (1.5, 4.5, 1.6), (0.0, 0.0, 3.0)), ((0.8, 7.2, 3.2), (1.0, 3.4, 1.1), (6.0, 0.0, 9.0)), ((-1.7, 7.0, 3.7), (0.9, 2.6, 1.0), (-8.0, 0.0, -7.0)), ((1.6, 6.9, 4.2), (0.7, 1.9, 0.8), (4.0, 0.0, 12.0))))
add_crystal_cluster(4, (((-0.5, 6.9, 7.5), (1.3, 4.0, 1.4), (0.0, 0.0, -5.0)), ((0.8, 6.7, 8.2), (0.9, 2.9, 1.0), (7.0, 0.0, 8.0)), ((-1.4, 6.5, 8.6), (0.8, 2.2, 0.9), (-6.0, 0.0, -9.0))))
add_crystal_cluster(5, (((-3.8, 5.2, 3.7), (1.1, 3.2, 1.2), (0.0, 0.0, 10.0)), ((-2.8, 5.0, 4.5), (0.8, 2.1, 0.9), (-5.0, 0.0, -8.0))))
add_crystal_cluster(6, (((1.9, 5.4, 11.6), (1.0, 3.0, 1.1), (0.0, 0.0, -8.0)), ((2.8, 5.1, 12.4), (0.8, 2.0, 0.9), (6.0, 0.0, 9.0))))
add_crystal_cluster(7, (((-3.8, 5.4, -10.7), (1.0, 2.8, 1.1), (0.0, 0.0, 9.0)), ((-2.9, 5.1, -9.8), (0.8, 1.9, 0.9), (-5.0, 0.0, -8.0))))


CUBES: Tuple[Cube, ...] = tuple(_cubes)
