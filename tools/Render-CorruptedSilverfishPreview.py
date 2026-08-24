from __future__ import annotations

import json
import math
from pathlib import Path

from PIL import Image, ImageDraw


PROJECT_ROOT = Path(__file__).resolve().parents[1]
EXPORT_ROOT = PROJECT_ROOT / "Modelle" / "Exports" / "corrupted_silverfish_v2"
GEOMETRY_PATH = EXPORT_ROOT / "geo" / "corrupted_silverfish.geo.json"
REVIEW_ROOT = EXPORT_ROOT / "review"

PALETTE = {
    "outline": (24, 21, 31, 255),
    "shadow": (43, 41, 51, 255),
    "silver": (89, 96, 107, 255),
    "light_silver": (140, 150, 163, 255),
    "highlight": (201, 209, 218, 255),
    "deep_corruption": (49, 15, 47, 255),
    "dark_red": (122, 23, 61, 255),
    "crimson": (181, 42, 79, 255),
    "corruption_light": (239, 92, 120, 255),
    "violet": (75, 35, 110, 255),
    "energy": (216, 137, 255, 255),
}

FACE_INDICES = (
    (0, 1, 2, 3),
    (4, 7, 6, 5),
    (0, 4, 5, 1),
    (1, 5, 6, 2),
    (2, 6, 7, 3),
    (3, 7, 4, 0),
)

VIEWS = {
    "front": {"camera": (0.0, 0.0, -1.0), "screen_x": (1.0, 0.0, 0.0), "screen_y": (0.0, 1.0, 0.0)},
    "right": {"camera": (-1.0, 0.0, 0.0), "screen_x": (0.0, 0.0, 1.0), "screen_y": (0.0, 1.0, 0.0)},
    "back": {"camera": (0.0, 0.0, 1.0), "screen_x": (-1.0, 0.0, 0.0), "screen_y": (0.0, 1.0, 0.0)},
    "top": {"camera": (0.0, 1.0, 0.0), "screen_x": (1.0, 0.0, 0.0), "screen_y": (0.0, 0.0, -1.0)},
}

IDLE_ROTATIONS = {
    "head": (0.0, -1.5, 0.0),
    "front_shell": (0.0, 2.5, 0.0),
    "middle_shell": (0.0, -2.0, 0.0),
    "tail_shell": (0.0, 3.5, 0.0),
    "tail_tip": (0.0, -5.0, 0.0),
    "left_front_leg": (0.0, 0.0, 2.0),
    "right_front_leg": (0.0, 0.0, -2.0),
    "left_middle_leg": (0.0, 0.0, -1.5),
    "right_middle_leg": (0.0, 0.0, 1.5),
    "left_back_leg": (0.0, 0.0, -2.0),
    "right_back_leg": (0.0, 0.0, 2.0),
}


def dot(a, b):
    return sum(x * y for x, y in zip(a, b))


def subtract(a, b):
    return tuple(x - y for x, y in zip(a, b))


def cross(a, b):
    return (
        a[1] * b[2] - a[2] * b[1],
        a[2] * b[0] - a[0] * b[2],
        a[0] * b[1] - a[1] * b[0],
    )


def rotate(point, pivot, rotation):
    x, y, z = subtract(point, pivot)
    rx, ry, rz = (math.radians(value) for value in rotation)

    y, z = y * math.cos(rx) - z * math.sin(rx), y * math.sin(rx) + z * math.cos(rx)
    x, z = x * math.cos(ry) + z * math.sin(ry), -x * math.sin(ry) + z * math.cos(ry)
    x, y = x * math.cos(rz) - y * math.sin(rz), x * math.sin(rz) + y * math.cos(rz)
    return (x + pivot[0], y + pivot[1], z + pivot[2])


def cube_vertices(origin, size):
    x, y, z = origin
    sx, sy, sz = size
    return [
        (x, y, z),
        (x + sx, y, z),
        (x + sx, y + sy, z),
        (x, y + sy, z),
        (x, y, z + sz),
        (x + sx, y, z + sz),
        (x + sx, y + sy, z + sz),
        (x, y + sy, z + sz),
    ]


def cube_color(name):
    if "crystal" in name:
        return PALETTE["crimson"]
    if "spine" in name:
        return PALETTE["dark_red"]
    if "mandible" in name:
        return PALETTE["highlight"]
    if "leg" in name:
        return PALETTE["silver"]
    if name == "tail_tip":
        return PALETTE["shadow"]
    if name in {"middle_shell", "tail_shell"}:
        return PALETTE["silver"]
    return PALETTE["light_silver"]


def shade(color, face_index, corrupted):
    if corrupted and face_index in {1, 3}:
        return PALETTE["violet"]
    factors = (0.82, 1.0, 0.72, 0.9, 0.78, 0.96)
    factor = factors[face_index]
    return tuple(min(255, round(channel * factor)) for channel in color[:3]) + (255,)


def load_scene(idle=False):
    data = json.loads(GEOMETRY_PATH.read_text(encoding="utf-8-sig"))
    bones = data["minecraft:geometry"][0]["bones"]
    by_name = {bone["name"]: bone for bone in bones}
    scene = []

    def transform_for_bone(point, bone_name):
        current = by_name[bone_name]
        result = point
        while current:
            rotation = list(current.get("rotation", (0.0, 0.0, 0.0)))
            if idle and current["name"] in IDLE_ROTATIONS:
                added = IDLE_ROTATIONS[current["name"]]
                rotation = [rotation[index] + added[index] for index in range(3)]
            result = rotate(result, current.get("pivot", (0.0, 0.0, 0.0)), rotation)
            current = by_name.get(current.get("parent"))
        return result

    for bone in bones:
        for cube in bone.get("cubes", []):
            vertices = [transform_for_bone(vertex, bone["name"]) for vertex in cube_vertices(cube["origin"], cube["size"])]
            scene.append((bone["name"], vertices))
    return scene


def render(view_name, output_name, idle=False):
    view = VIEWS[view_name]
    camera = view["camera"]
    screen_x = view["screen_x"]
    screen_y = view["screen_y"]
    scene = load_scene(idle=idle)
    faces = []
    projected_points = []

    for bone_name, vertices in scene:
        for face_index, indices in enumerate(FACE_INDICES):
            face = [vertices[index] for index in indices]
            normal = cross(subtract(face[1], face[0]), subtract(face[2], face[1]))
            if dot(normal, camera) <= 0:
                continue
            projected = [(dot(point, screen_x), dot(point, screen_y)) for point in face]
            projected_points.extend(projected)
            depth = sum(dot(point, camera) for point in face) / 4.0
            color = shade(cube_color(bone_name), face_index, "corruption" in bone_name)
            faces.append((depth, bone_name, projected, color))

    min_x = min(point[0] for point in projected_points)
    max_x = max(point[0] for point in projected_points)
    min_y = min(point[1] for point in projected_points)
    max_y = max(point[1] for point in projected_points)
    scale = min(480.0 / max(max_x - min_x, 1.0), 480.0 / max(max_y - min_y, 1.0))
    center_x = (min_x + max_x) / 2.0
    center_y = (min_y + max_y) / 2.0

    image = Image.new("RGBA", (640, 640), PALETTE["outline"])
    draw = ImageDraw.Draw(image)
    for y in range(0, 640, 24):
        for x in range(0, 640, 24):
            if ((x // 24) + (y // 24)) % 2 == 0:
                draw.rectangle((x, y, x + 23, y + 23), fill=(31, 29, 39, 255))

    for _, bone_name, projected, color in sorted(faces, key=lambda item: item[0]):
        polygon = [
            (round(320 + (x - center_x) * scale), round(320 - (y - center_y) * scale))
            for x, y in projected
        ]
        draw.polygon(polygon, fill=color, outline=PALETTE["outline"], width=3)
        if "corruption" in bone_name:
            center = (
                round(sum(point[0] for point in polygon) / len(polygon)),
                round(sum(point[1] for point in polygon) / len(polygon)),
            )
            radius = max(2, round(scale * 0.08))
            draw.rectangle((center[0] - radius, center[1] - radius, center[0] + radius, center[1] + radius), fill=PALETTE["corruption_light"])

    label = f"{view_name.upper()} - {'IDLE 0.3 s' if idle else 'REST POSE'}"
    draw.rounded_rectangle((18, 18, 250, 52), radius=7, fill=(24, 21, 31, 230), outline=PALETTE["silver"], width=2)
    draw.text((30, 29), label, fill=PALETTE["highlight"])
    image.save(REVIEW_ROOT / output_name)


def main():
    REVIEW_ROOT.mkdir(parents=True, exist_ok=True)
    for view_name in ("front", "right", "back", "top"):
        render(view_name, f"candidate_{view_name}.png")
    render("right", "candidate_idle.png", idle=True)
    print("CANDIDATE_RENDERS=WRITTEN")


if __name__ == "__main__":
    main()
