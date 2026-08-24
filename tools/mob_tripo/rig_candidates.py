"""Segment Tripo cube candidates into deterministic Blockbench motion bones."""

from __future__ import annotations

from dataclasses import dataclass
import argparse
import json
from pathlib import Path
from typing import Callable, Mapping, Sequence
from uuid import UUID, uuid5


ROOT = Path(__file__).resolve().parents[2]
NAMESPACE = UUID("e7daf25a-7e32-56ab-9592-9b403286bb71")


@dataclass(frozen=True)
class Job:
    source: Path
    target: Path
    bones: tuple[str, ...]
    kind: str


HUMANOID_BONES = ("root", "body", "head", "right_arm", "left_arm", "right_leg", "left_leg")
SPIDER_BONES = ("root", "body", "head") + tuple(f"web_leg_{index}" for index in range(8))
OCTOPUS_BONES = ("root", "body") + tuple(f"tentacle{index}" for index in range(8))
BAT_BONES = ("root", "body", "head", "right_wing", "right_wing_tip", "left_wing", "left_wing_tip")


def _job(name: str, bones: tuple[str, ...], kind: str) -> Job:
    base = Path("Modelle") / "Exports" / f"{name}_v1"
    return Job(
        base / "blockbench" / f"{name} Tripo Cubes.bbmodel",
        base / "blockbench" / f"{name} Tripo Rig.bbmodel",
        bones,
        kind,
    )


JOBS: Mapping[str, Job] = {
    "frost_stray": _job("frost_stray", HUMANOID_BONES, "humanoid"),
    "web_cave_spider": _job("web_cave_spider", SPIDER_BONES, "spider"),
    "coral_drowned": _job("coral_drowned", HUMANOID_BONES, "humanoid"),
    "octopus": _job("octopus", OCTOPUS_BONES, "octopus"),
    "witch_boss": _job("witch_boss", HUMANOID_BONES, "humanoid"),
    "living_bat": _job("living_bat", BAT_BONES, "bat"),
    "rooted_husk": _job("rooted_husk", HUMANOID_BONES, "humanoid"),
}


def _centre(element: dict) -> tuple[float, float, float]:
    return tuple((float(element["from"][axis]) + float(element["to"][axis])) / 2 for axis in range(3))


def _normalised_centres(elements: Sequence[dict]) -> dict[str, tuple[float, float, float]]:
    centres = {element["uuid"]: _centre(element) for element in elements}
    lower = [min(point[axis] for point in centres.values()) for axis in range(3)]
    upper = [max(point[axis] for point in centres.values()) for axis in range(3)]
    spans = [max(upper[axis] - lower[axis], 1e-9) for axis in range(3)]
    return {
        uuid: tuple((point[axis] - lower[axis]) / spans[axis] for axis in range(3))
        for uuid, point in centres.items()
    }


def _classify_humanoid(point: tuple[float, float, float]) -> str:
    x, y, _z = point
    if y >= 0.73:
        return "head"
    if y <= 0.40:
        return "right_leg" if x < 0.5 else "left_leg"
    if x <= 0.25:
        return "right_arm"
    if x >= 0.75:
        return "left_arm"
    return "body"


def _classify_spider(point: tuple[float, float, float]) -> str:
    x, _y, z = point
    if 0.28 <= x <= 0.72:
        return "head" if z <= 0.40 else "body"
    lane = min(3, max(0, int(z * 4)))
    return f"web_leg_{lane if x < 0.5 else lane + 4}"


def _classify_octopus(point: tuple[float, float, float]) -> str:
    import math

    x, y, z = point
    dx, dz = x - 0.5, z - 0.5
    if y >= 0.42 and dx * dx + dz * dz <= 0.16:
        return "body"
    angle = (math.atan2(dz, dx) + math.tau) % math.tau
    return f"tentacle{int((angle + math.pi / 8) // (math.pi / 4)) % 8}"


def _classify_bat(point: tuple[float, float, float]) -> str:
    x, y, _z = point
    distance = abs(x - 0.5)
    if y >= 0.72 and distance <= 0.23:
        return "head"
    if distance >= 0.23:
        side = "right" if x < 0.5 else "left"
        return f"{side}_wing_tip" if distance >= 0.38 else f"{side}_wing"
    return "body"


CLASSIFIERS: Mapping[str, Callable[[tuple[float, float, float]], str]] = {
    "humanoid": _classify_humanoid,
    "spider": _classify_spider,
    "octopus": _classify_octopus,
    "bat": _classify_bat,
}


def _target(bone: str) -> tuple[float, float, float]:
    if bone == "body":
        return (0.5, 0.55, 0.5)
    if bone == "head":
        return (0.5, 0.86, 0.25)
    if bone == "right_arm":
        return (0.08, 0.58, 0.5)
    if bone == "left_arm":
        return (0.92, 0.58, 0.5)
    if bone == "right_leg":
        return (0.36, 0.18, 0.5)
    if bone == "left_leg":
        return (0.64, 0.18, 0.5)
    if bone.startswith("web_leg_"):
        index = int(bone.rsplit("_", 1)[1])
        return (0.06 if index < 4 else 0.94, 0.35, (index % 4 + 0.5) / 4)
    if bone.startswith("tentacle"):
        import math

        index = int(bone.removeprefix("tentacle"))
        angle = index * math.tau / 8
        return (0.5 + math.cos(angle) * 0.42, 0.16, 0.5 + math.sin(angle) * 0.42)
    if "wing" in bone:
        right = bone.startswith("right")
        tip = bone.endswith("tip")
        return ((0.03 if right else 0.97) if tip else (0.22 if right else 0.78), 0.55, 0.5)
    return (0.5, 0.5, 0.5)


def _ensure_nonempty(assignments: dict[str, str], centres: Mapping[str, tuple[float, float, float]], bones: Sequence[str]) -> None:
    for bone in bones:
        if bone == "root" or bone in assignments.values():
            continue
        counts = {candidate: list(assignments.values()).count(candidate) for candidate in set(assignments.values())}
        tx, ty, tz = _target(bone)
        candidates = [uuid for uuid, owner in assignments.items() if counts[owner] > 1]
        if not candidates:
            raise ValueError(f"Cannot allocate visible geometry to bone {bone}")
        chosen = min(candidates, key=lambda uuid: sum((centres[uuid][axis] - (tx, ty, tz)[axis]) ** 2 for axis in range(3)))
        assignments[chosen] = bone


def _pivot(bone: str, owned: Sequence[dict]) -> list[float]:
    points = [_centre(element) for element in owned]
    centre = [sum(point[axis] for point in points) / len(points) for axis in range(3)]
    if bone == "head":
        centre[1] = min(float(element["from"][1]) for element in owned)
    elif any(token in bone for token in ("arm", "leg", "wing", "tentacle")):
        centre[1] = max(float(element["to"][1]) for element in owned)
    return [round(value, 6) for value in centre]


def _group(name: str, origin: Sequence[float], children: list) -> dict:
    return {
        "name": name,
        "origin": list(origin),
        "uuid": str(uuid5(NAMESPACE, f"group:{name}")),
        "children": children,
    }


def _group_uuid(name: str) -> str:
    return str(uuid5(NAMESPACE, f"group:{name}"))


def _keyframe(animation: str, bone: str, channel: str, time: float, values: Sequence[float]) -> dict:
    return {
        "channel": channel,
        "data_points": [{axis: f"{float(value):g}" for axis, value in zip(("x", "y", "z"), values)}],
        "uuid": str(uuid5(NAMESPACE, f"keyframe:{animation}:{bone}:{channel}:{time:g}")),
        "time": float(time),
        "color": -1,
        "interpolation": "linear",
    }


def _animation(name: str, label: str, length: float, tracks: Mapping[str, Sequence[tuple[str, float, Sequence[float]]]]) -> dict:
    animation_name = f"animation.{name}.{label}"
    animators = {}
    for bone, frames in tracks.items():
        animators[_group_uuid(bone)] = {
            "name": bone,
            "type": "bone",
            "rotation_global": False,
            "quaternion_interpolation": False,
            "keyframes": [
                _keyframe(animation_name, bone, channel, time, values)
                for channel, time, values in frames
            ],
        }
    return {
        "uuid": str(uuid5(NAMESPACE, f"animation:{animation_name}")),
        "name": animation_name,
        "path": f"{name}.animation.json",
        "loop": "loop",
        "override": False,
        "snapping": 20,
        "length": float(length),
        "selected_item": None,
        "anim_time_update": "",
        "blend_weight": "",
        "start_delay": "",
        "loop_delay": "",
        "animators": animators,
    }


def _animations(name: str, job: Job) -> list[dict]:
    idle_bone = "head" if "head" in job.bones else "body"
    idle_channel = "rotation" if idle_bone == "head" else "position"
    idle_values = ((0, -3, 0), (0, 3, 0), (0, -3, 0)) if idle_channel == "rotation" else ((0, 0, 0), (0, 0.25, 0), (0, 0, 0))
    idle = _animation(
        name,
        "idle",
        1.6,
        {idle_bone: [(idle_channel, time, value) for time, value in zip((0.0, 0.8, 1.6), idle_values)]},
    )

    if job.kind == "humanoid":
        tracks = {}
        for bone, sign in (("right_arm", 1), ("left_arm", -1), ("right_leg", -1), ("left_leg", 1)):
            tracks[bone] = [
                ("rotation", 0.0, (35 * sign, 0, 0)),
                ("rotation", 0.4, (-35 * sign, 0, 0)),
                ("rotation", 0.8, (35 * sign, 0, 0)),
            ]
        motion = _animation(name, "walk", 0.8, tracks)
    elif job.kind == "spider":
        tracks = {}
        for index in range(8):
            sign = 1 if index % 2 == 0 else -1
            tracks[f"web_leg_{index}"] = [
                ("rotation", 0.0, (0, 28 * sign, 12 * sign)),
                ("rotation", 0.4, (0, -28 * sign, -12 * sign)),
                ("rotation", 0.8, (0, 28 * sign, 12 * sign)),
            ]
        motion = _animation(name, "walk", 0.8, tracks)
    elif job.kind == "octopus":
        tracks = {}
        for index in range(8):
            sign = 1 if index % 2 == 0 else -1
            tracks[f"tentacle{index}"] = [
                ("rotation", 0.0, (30 * sign, 0, 18 * sign)),
                ("rotation", 0.6, (-30 * sign, 0, -18 * sign)),
                ("rotation", 1.2, (30 * sign, 0, 18 * sign)),
            ]
        motion = _animation(name, "swim", 1.2, tracks)
    else:
        tracks = {
            "right_wing": [("rotation", 0.0, (0, -48, 0)), ("rotation", 0.3, (0, 18, 0)), ("rotation", 0.6, (0, -48, 0))],
            "left_wing": [("rotation", 0.0, (0, 48, 0)), ("rotation", 0.3, (0, -18, 0)), ("rotation", 0.6, (0, 48, 0))],
            "right_wing_tip": [("rotation", 0.0, (0, -32, 0)), ("rotation", 0.3, (0, 14, 0)), ("rotation", 0.6, (0, -32, 0))],
            "left_wing_tip": [("rotation", 0.0, (0, 32, 0)), ("rotation", 0.3, (0, -14, 0)), ("rotation", 0.6, (0, 32, 0))],
        }
        motion = _animation(name, "fly", 0.6, tracks)
    return [idle, motion]


def rig_document(name: str, source: dict) -> dict:
    if name not in JOBS:
        raise ValueError(f"Unknown mob rig: {name}")
    job = JOBS[name]
    elements = source.get("elements")
    if not isinstance(elements, list) or not elements:
        raise ValueError(f"{name} candidate has no elements")
    centres = _normalised_centres(elements)
    classify = CLASSIFIERS[job.kind]
    assignments = {element["uuid"]: classify(centres[element["uuid"]]) for element in elements}
    _ensure_nonempty(assignments, centres, job.bones)

    motion_groups = []
    for bone in job.bones:
        if bone == "root":
            continue
        owned = [element for element in elements if assignments[element["uuid"]] == bone]
        motion_groups.append(_group(bone, _pivot(bone, owned), [element["uuid"] for element in owned]))
    result = dict(source)
    result["name"] = f"{name}_tripo_rig"
    result["outliner"] = [_group("root", (0.0, 0.0, 0.0), motion_groups)]
    result["animations"] = _animations(name, job)
    return result


def build_one(name: str) -> Path:
    job = JOBS[name]
    source = json.loads((ROOT / job.source).read_text(encoding="utf-8"))
    target = ROOT / job.target
    target.parent.mkdir(parents=True, exist_ok=True)
    with target.open("w", encoding="utf-8", newline="\n") as handle:
        handle.write(json.dumps(rig_document(name, source), ensure_ascii=False, indent=2) + "\n")
    return target


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("names", nargs="*", metavar="MOB")
    args = parser.parse_args(argv)
    selected = args.names or list(JOBS)
    for name in selected:
        if name not in JOBS:
            parser.error(f"unknown mob: {name}")
        print(f"MOB_RIG_PASS {name} {build_one(name)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
