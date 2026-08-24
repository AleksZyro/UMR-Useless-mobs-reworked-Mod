from __future__ import annotations

import argparse
import json
from pathlib import Path

import matplotlib
import numpy as np
from PIL import Image, ImageDraw
from scipy import ndimage

matplotlib.use("Agg")
import matplotlib.pyplot as plt


GRID_X = 30
GRID_Y = 24
GRID_Z = 48


def isolate_view(image: Image.Image, bounds: tuple[int, int, int, int]) -> tuple[Image.Image, Image.Image]:
    crop = image.crop(bounds).convert("RGB")
    rgb = np.asarray(crop, dtype=np.float32) / 255.0
    maximum = rgb.max(axis=2)
    minimum = rgb.min(axis=2)
    saturation = (maximum - minimum) / np.maximum(maximum, 0.001)
    luminance = rgb.mean(axis=2)

    seed = (saturation > 0.16) | (luminance > 0.39)
    seed = ndimage.binary_closing(seed, iterations=2)
    seed = ndimage.binary_dilation(seed, iterations=2)
    labels, count = ndimage.label(seed)
    sizes = np.bincount(labels.ravel())
    keep = np.zeros(count + 1, dtype=bool)
    if count:
        minimum_component = max(18, int(sizes[1:].max() * 0.003))
        keep[1:] = sizes[1:] >= minimum_component
    mask = keep[labels]
    mask = ndimage.binary_closing(mask, iterations=3)
    mask = ndimage.binary_fill_holes(mask)
    mask = ndimage.binary_erosion(mask, iterations=1)

    ys, xs = np.where(mask)
    if not len(xs):
        raise ValueError(f"No foreground detected in crop {bounds}")
    pad = 3
    box = (
        max(0, int(xs.min()) - pad),
        max(0, int(ys.min()) - pad),
        min(crop.width, int(xs.max()) + pad + 1),
        min(crop.height, int(ys.max()) + pad + 1),
    )
    mask_image = Image.fromarray(mask.astype(np.uint8) * 255).crop(box)
    return crop.crop(box), mask_image


def resize_view(
    color: Image.Image,
    mask: Image.Image,
    size: tuple[int, int],
    *,
    flip_x: bool = False,
) -> tuple[np.ndarray, np.ndarray]:
    color = color.resize(size, Image.Resampling.BOX)
    mask = mask.resize(size, Image.Resampling.BOX)
    if flip_x:
        color = color.transpose(Image.Transpose.FLIP_LEFT_RIGHT)
        mask = mask.transpose(Image.Transpose.FLIP_LEFT_RIGHT)
    return np.asarray(color, dtype=np.float32) / 255.0, np.asarray(mask) >= 64


def build_hull(source: Path) -> tuple[np.ndarray, np.ndarray, dict[str, Image.Image]]:
    image = Image.open(source).convert("RGB")
    width, height = image.size
    crops = {
        "front": (int(width * 0.13), int(height * 0.10), int(width * 0.50), int(height * 0.58)),
        "right": (int(width * 0.47), int(height * 0.11), int(width * 0.98), int(height * 0.59)),
        "back": (int(width * 0.13), int(height * 0.52), int(width * 0.51), height),
        "top": (int(width * 0.46), int(height * 0.51), int(width * 0.98), int(height * 0.94)),
    }
    isolated = {name: isolate_view(image, bounds) for name, bounds in crops.items()}

    front_c, front_m = resize_view(*isolated["front"], (GRID_X, GRID_Y))
    back_c, back_m = resize_view(*isolated["back"], (GRID_X, GRID_Y), flip_x=True)
    side_c, side_m = resize_view(*isolated["right"], (GRID_Z, GRID_Y))
    top_c, top_m = resize_view(*isolated["top"], (GRID_Z, GRID_X), flip_x=True)

    # Image rows run top-to-bottom; model Y runs bottom-to-top.
    front_m = np.flipud(front_m).T
    back_m = np.flipud(back_m).T
    side_m = np.flipud(side_m).T
    # Top image rows represent model X and columns represent model Z.
    front_c = np.flipud(front_c).transpose(1, 0, 2)
    back_c = np.flipud(back_c).transpose(1, 0, 2)
    side_c = np.flipud(side_c).transpose(1, 0, 2)

    frontal = front_m | back_m
    occupied = frontal[:, :, None] & side_m.T[None, :, :] & top_m[:, None, :]
    occupied = ndimage.binary_closing(occupied, iterations=1)

    colors = np.zeros((*occupied.shape, 4), dtype=np.float32)
    colors[..., 3] = occupied
    colors[..., :3] = (0.35, 0.35, 0.38)

    for x, y, z in np.argwhere(occupied):
        column_y = np.where(occupied[x, :, z])[0]
        row_x = np.where(occupied[:, y, z])[0]
        row_z = np.where(occupied[x, y, :])[0]
        if y == column_y.max():
            color = top_c[x, z]
        elif z == row_z.min():
            color = front_c[x, y]
        elif z == row_z.max():
            color = back_c[x, y]
        elif x in (row_x.min(), row_x.max()):
            color = side_c[z, y]
        else:
            color = (front_c[x, y] + side_c[z, y] + top_c[x, z]) / 3.0
        colors[x, y, z, :3] = np.clip(color, 0.0, 1.0)

    debug = {name: pair[1] for name, pair in isolated.items()}
    return occupied, colors, debug


def render_preview(occupied: np.ndarray, colors: np.ndarray, output: Path) -> None:
    fig = plt.figure(figsize=(12, 8), facecolor="#15161a")
    ax = fig.add_subplot(111, projection="3d", facecolor="#15161a")
    voxels = occupied.transpose(0, 2, 1)
    voxel_colors = colors.transpose(0, 2, 1, 3)
    ax.voxels(voxels, facecolors=voxel_colors, edgecolor=(0.03, 0.03, 0.04, 0.48), linewidth=0.16)
    ax.view_init(elev=24, azim=-58)
    ax.set_box_aspect((GRID_X, GRID_Z, GRID_Y))
    ax.set_axis_off()
    ax.set_xlim(0, GRID_X)
    ax.set_ylim(0, GRID_Z)
    ax.set_zlim(0, GRID_Y)
    fig.subplots_adjust(0, 0, 1, 1)
    output.parent.mkdir(parents=True, exist_ok=True)
    fig.savefig(output, dpi=150, facecolor=fig.get_facecolor(), bbox_inches="tight", pad_inches=0.05)
    plt.close(fig)


def render_masks(debug: dict[str, Image.Image], output: Path) -> None:
    canvas = Image.new("RGB", (1024, 1024), (20, 21, 24))
    draw = ImageDraw.Draw(canvas)
    positions = {"front": (0, 0), "right": (512, 0), "back": (0, 512), "top": (512, 512)}
    for name, mask in debug.items():
        fitted = mask.copy()
        fitted.thumbnail((460, 430), Image.Resampling.NEAREST)
        tile = Image.new("RGB", (512, 512), (20, 21, 24))
        tile.paste((230, 230, 235), ((512 - fitted.width) // 2, (512 - fitted.height) // 2), fitted)
        x, y = positions[name]
        canvas.paste(tile, (x, y))
        draw.text((x + 18, y + 18), name.upper(), fill=(255, 70, 150))
    output.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(output)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("output_dir", type=Path)
    args = parser.parse_args()
    occupied, colors, debug = build_hull(args.source)
    args.output_dir.mkdir(parents=True, exist_ok=True)
    np.savez_compressed(args.output_dir / "visual_hull.npz", occupied=occupied, colors=colors)
    render_preview(occupied, colors, args.output_dir / "visual_hull_preview.png")
    render_masks(debug, args.output_dir / "visual_hull_masks.png")
    stats = {
        "grid": [GRID_X, GRID_Y, GRID_Z],
        "occupied_voxels": int(occupied.sum()),
    }
    (args.output_dir / "visual_hull_stats.json").write_text(json.dumps(stats, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(stats))


if __name__ == "__main__":
    main()
