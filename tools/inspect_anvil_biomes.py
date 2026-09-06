#!/usr/bin/env python3
"""Read-only biome-cell lookup for Minecraft Anvil chunks used by UMR QA."""

from __future__ import annotations

import argparse
import math
from collections import defaultdict
from pathlib import Path

try:
    from .inspect_anvil_blocks import load_chunk
except ImportError:  # Direct script execution keeps tools/ on sys.path.
    from inspect_anvil_blocks import load_chunk


def biome_cells(chunk, wanted: set[str] | None = None):
    """Yield biome cell origins as ``(name, x, y, z)`` tuples."""
    chunk_x = int(chunk["xPos"])
    chunk_z = int(chunk["zPos"])
    for section in chunk.get("sections", []):
        biomes = section.get("biomes")
        if not biomes or "palette" not in biomes:
            continue

        palette = [str(entry) for entry in biomes["palette"]]
        data = biomes.get("data")
        if data is None:
            indexes = [0] * 64
        else:
            bits = max(1, math.ceil(math.log2(len(palette))))
            per_long = 64 // bits
            mask = (1 << bits) - 1
            unsigned = [int(value) & ((1 << 64) - 1) for value in data]
            indexes = [
                (unsigned[i // per_long] >> ((i % per_long) * bits)) & mask
                for i in range(64)
            ]

        section_y = int(section["Y"])
        for index, palette_index in enumerate(indexes):
            if palette_index >= len(palette):
                continue
            name = palette[palette_index]
            if wanted and name not in wanted:
                continue
            local_x = index & 3
            local_z = (index >> 2) & 3
            local_y = (index >> 4) & 3
            yield (
                name,
                chunk_x * 16 + local_x * 4,
                section_y * 16 + local_y * 4,
                chunk_z * 16 + local_z * 4,
            )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("world", type=Path)
    parser.add_argument("center_x", type=int)
    parser.add_argument("center_z", type=int)
    parser.add_argument("--radius-chunks", type=int, default=2)
    parser.add_argument("--biome", action="append")
    args = parser.parse_args()

    center_chunk_x = args.center_x // 16
    center_chunk_z = args.center_z // 16
    grouped: dict[str, list[tuple[int, int, int]]] = defaultdict(list)
    wanted = set(args.biome) if args.biome else None
    scanned = 0
    for chunk_x in range(center_chunk_x - args.radius_chunks, center_chunk_x + args.radius_chunks + 1):
        for chunk_z in range(center_chunk_z - args.radius_chunks, center_chunk_z + args.radius_chunks + 1):
            chunk = load_chunk(args.world, chunk_x, chunk_z)
            if chunk is None:
                continue
            scanned += 1
            for name, x, y, z in biome_cells(chunk, wanted):
                grouped[name].append((x, y, z))

    for name in sorted(grouped):
        positions = grouped[name]
        xs, ys, zs = zip(*positions)
        sample = min(positions, key=lambda pos: (abs(pos[0] - args.center_x) + abs(pos[2] - args.center_z), abs(pos[1])))
        print(
            f"{name} cells={len(positions)} "
            f"x={min(xs)}..{max(xs) + 3} y={min(ys)}..{max(ys) + 3} "
            f"z={min(zs)}..{max(zs) + 3} sample={sample[0]},{sample[1]},{sample[2]}"
        )
    print(f"ANVIL_BIOME_SCAN chunks={scanned} biomes={len(grouped)} cells={sum(map(len, grouped.values()))}")


if __name__ == "__main__":
    main()
