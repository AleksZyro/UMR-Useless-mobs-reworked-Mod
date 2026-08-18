"""Add deterministic Blockbench animations to the approved Tripo mesh rig."""

from __future__ import annotations

import argparse
import copy
import math
from pathlib import Path
from typing import Any, Dict, Iterable, List, Tuple

from tools.corrupted_silverfish_v5.rig_mesh import (
    DEFAULT_OUTPUT as DEFAULT_RIG,
    _publish_transaction,
    _stable_id,
    load_document,
    rig_bytes,
)


JsonObject = Dict[str, Any]
PROJECT_ROOT = Path(__file__).resolve().parents[2]
EXPORT_ROOT = PROJECT_ROOT / "Modelle" / "Exports" / "corrupted_silverfish_v5"
DEFAULT_OUTPUT = EXPORT_ROOT / "blockbench" / "Corrupted Silverfish v5 Tripo Animated.bbmodel"


def _channel(times: Iterable[float], vectors: Iterable[Iterable[float]]) -> Tuple[Tuple[float, List[float]], ...]:
    pairs = tuple((float(time), [float(value) for value in vector]) for time, vector in zip(times, vectors))
    if not pairs or any(len(vector) != 3 for _, vector in pairs):
        raise ValueError("Animation channels require at least one Vec3 keyframe")
    if any(not math.isfinite(value) for time, vector in pairs for value in (time, *vector)):
        raise ValueError("Animation keyframe values must be finite")
    if tuple(time for time, _ in pairs) != tuple(sorted(time for time, _ in pairs)):
        raise ValueError("Animation keyframe times must be ordered")
    return pairs


def _animation_specs() -> Dict[str, JsonObject]:
    idle_times = (0, 0.8, 1.6)
    idle = {
        "body": {
            "position": _channel(idle_times, ((0, 0, 0), (0, 0.08, 0), (0, 0, 0))),
            "rotation": _channel(idle_times, ((0, 0, 0), (0, 0.4, 0.35), (0, 0, 0))),
        },
    }

    walk_times = tuple(index * 0.05 for index in range(17))
    phase_angles = tuple(2 * math.pi * time / 0.8 for time in walk_times)
    stride = tuple(round(24.0 * math.cos(angle), 6) for angle in phase_angles)
    bob = tuple(
        round(0.18 * math.sin(angle) ** 2, 6)
        for angle in phase_angles
    )
    sway = tuple(round(math.cos(angle), 6) for angle in phase_angles)
    walk: Dict[str, JsonObject] = {
        "body": {
            "position": _channel(walk_times, ((0, value, 0) for value in bob)),
            "rotation": _channel(
                walk_times,
                ((0, 0.5 * value, 0.7 * value) for value in sway),
            ),
        },
    }
    first_tripod = {"leg_front_left", "leg_middle_right", "leg_rear_left"}
    for bone in (
        "leg_front_left", "leg_front_right", "leg_middle_left",
        "leg_middle_right", "leg_rear_left", "leg_rear_right",
    ):
        phase = 1 if bone in first_tripod else -1
        lift_sign = -1 if bone.endswith("left") else 1
        lift = tuple(
            round(lift_sign * 12.0 * max(0.0, phase * math.sin(angle)), 6)
            for angle in phase_angles
        )
        walk[bone] = {
            "rotation": _channel(
                walk_times,
                ((0, phase * y, z) for y, z in zip(stride, lift)),
            )
        }

    attack_times = (0, 0.225, 0.45)
    attack = {
        "body": {"position": _channel(attack_times, ((0, 0, 0), (0, 0, 0.55), (0, 0, 0)))},
        "leg_front_left": {"rotation": _channel(attack_times, ((0, 0, 0), (0, 12, -8), (0, 0, 0)))},
        "leg_front_right": {"rotation": _channel(attack_times, ((0, 0, 0), (0, -12, 8), (0, 0, 0)))},
    }

    hurt_times = (0, 0.1, 0.2, 0.3)
    hurt = {
        "body": {"rotation": _channel(hurt_times, ((0, 0, 0), (0, 0, 7), (0, 0, -3), (0, 0, 0)))},
    }

    death_times = (0, 0.55, 1.0)
    death: Dict[str, JsonObject] = {
        "body": {
            "position": _channel(death_times, ((0, 0, 0), (0, -0.6, 0), (0, -1.5, 0))),
            "rotation": _channel(death_times, ((0, 0, 0), (0, 0, 38), (0, 0, 82))),
        },
    }
    for bone in (
        "leg_front_left", "leg_middle_left", "leg_rear_left",
        "leg_front_right", "leg_middle_right", "leg_rear_right",
    ):
        fold = 48 if bone.endswith("left") else -48
        death[bone] = {
            "rotation": _channel(death_times, ((0, 0, 0), (0, 0, fold / 2), (0, 0, fold)))
        }

    return {
        "animation.corrupted_silverfish.idle": {"loop": True, "length": 1.6, "bones": idle},
        "animation.corrupted_silverfish.walk": {"loop": True, "length": 0.8, "bones": walk},
        "animation.corrupted_silverfish.attack": {"loop": False, "length": 0.45, "bones": attack},
        "animation.corrupted_silverfish.hurt": {"loop": False, "length": 0.3, "bones": hurt},
        "animation.corrupted_silverfish.death": {"loop": False, "length": 1.0, "bones": death},
    }


ANIMATION_SPECS = _animation_specs()


def _blockbench_animations(document: JsonObject) -> List[JsonObject]:
    groups = {group.get("name"): group.get("uuid") for group in document.get("groups", [])}
    animations = []
    for animation_name, spec in ANIMATION_SPECS.items():
        animators = {}
        for bone_name, channels in spec["bones"].items():
            group_uuid = groups.get(bone_name)
            if not isinstance(group_uuid, str):
                raise ValueError(f"Animation target group is missing: {bone_name}")
            keyframes = []
            for channel_name, channel in channels.items():
                for time, vector in channel:
                    keyframes.append(
                        {
                            "channel": channel_name,
                            "data_points": [{"x": f"{vector[0]:g}", "y": f"{vector[1]:g}", "z": f"{vector[2]:g}"}],
                            "uuid": _stable_id("keyframe", f"{animation_name}:{bone_name}:{channel_name}:{time:g}"),
                            "time": time,
                            "color": -1,
                            "interpolation": "linear",
                        }
                    )
            animators[group_uuid] = {
                "name": bone_name,
                "type": "bone",
                "rotation_global": False,
                "quaternion_interpolation": False,
                "keyframes": keyframes,
            }
        animations.append(
            {
                "uuid": _stable_id("animation", animation_name),
                "name": animation_name,
                "path": "corrupted_silverfish.animation.json",
                "loop": "loop" if spec["loop"] else "once",
                "override": False,
                "snapping": 20,
                "length": spec["length"],
                "selected_item": None,
                "anim_time_update": "",
                "blend_weight": "",
                "start_delay": "",
                "loop_delay": "",
                "animators": animators,
            }
        )
    return animations


def add_animations(rig: JsonObject) -> JsonObject:
    """Return an animated copy while preserving all approved rig data."""
    existing = rig.get("animations", [])
    if existing not in (None, []):
        raise ValueError("Source rig already contains animations")
    result = copy.deepcopy(rig)
    result["animations"] = _blockbench_animations(result)
    return result


def animation_bytes(document: JsonObject) -> bytes:
    return rig_bytes(document)


def write_animated_model(source: Path, output: Path) -> JsonObject:
    source = source.resolve()
    output = output.resolve()
    if source == output:
        raise ValueError("Animated output may not overwrite the source rig")
    animated = add_animations(load_document(source))
    payload = animation_bytes(animated)
    _publish_transaction({output: payload})
    return {"animations": len(animated["animations"]), "bytes": len(payload)}


def main(argv: List[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, default=DEFAULT_RIG)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    args = parser.parse_args(argv)
    try:
        result = write_animated_model(args.source, args.output)
    except (OSError, ValueError) as exc:
        print(f"ANIMATION_FAILED: {exc}")
        return 1
    print(f"ANIMATION_PASS ANIMATIONS={result['animations']} BYTES={result['bytes']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
