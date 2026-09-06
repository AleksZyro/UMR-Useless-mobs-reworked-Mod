#!/usr/bin/env python3
"""Read-only block lookup for Minecraft Anvil chunks used by UMR runtime QA."""

from __future__ import annotations

import argparse
import io
import math
import struct
import zlib
from pathlib import Path

import nbtlib


def load_chunk(world: Path, chunk_x: int, chunk_z: int):
    region_x = chunk_x // 32
    region_z = chunk_z // 32
    region = world / "region" / f"r.{region_x}.{region_z}.mca"
    if not region.exists():
        return None

    index = (chunk_x % 32) + (chunk_z % 32) * 32
    with region.open("rb") as handle:
        handle.seek(index * 4)
        location = handle.read(4)
        sector = int.from_bytes(location[:3], "big")
        if sector == 0:
            return None
        handle.seek(sector * 4096)
        length = struct.unpack(">I", handle.read(4))[0]
        compression = handle.read(1)[0]
        payload = handle.read(length - 1)

    if compression == 1:
        import gzip

        payload = gzip.decompress(payload)
    elif compression == 2:
        payload = zlib.decompress(payload)
    elif compression != 3:
        raise ValueError(f"Unsupported Anvil compression type {compression}")
    return nbtlib.File.parse(io.BytesIO(payload))


def palette_name(entry) -> str:
    return str(entry["Name"])


def block_positions(chunk, wanted: set[str]):
    chunk_x = int(chunk["xPos"])
    chunk_z = int(chunk["zPos"])
    for section in chunk.get("sections", []):
        states = section.get("block_states")
        if not states or "palette" not in states:
            continue
        palette = states["palette"]
        wanted_indexes = {
            index for index, entry in enumerate(palette) if palette_name(entry) in wanted
        }
        if not wanted_indexes:
            continue

        data = states.get("data")
        if data is None:
            indexes = [0] * 4096
        else:
            bits = max(4, math.ceil(math.log2(len(palette))))
            per_long = 64 // bits
            mask = (1 << bits) - 1
            unsigned = [int(value) & ((1 << 64) - 1) for value in data]
            indexes = [
                (unsigned[i // per_long] >> ((i % per_long) * bits)) & mask
                for i in range(4096)
            ]

        section_y = int(section["Y"])
        for index, palette_index in enumerate(indexes):
            if palette_index not in wanted_indexes:
                continue
            local_x = index & 15
            local_z = (index >> 4) & 15
            local_y = (index >> 8) & 15
            yield (
                palette_name(palette[palette_index]),
                chunk_x * 16 + local_x,
                section_y * 16 + local_y,
                chunk_z * 16 + local_z,
            )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("world", type=Path)
    parser.add_argument("center_x", type=int)
    parser.add_argument("center_z", type=int)
    parser.add_argument("--radius-chunks", type=int, default=2)
    parser.add_argument("--block", action="append", required=True)
    args = parser.parse_args()

    center_chunk_x = args.center_x // 16
    center_chunk_z = args.center_z // 16
    found = []
    for chunk_x in range(center_chunk_x - args.radius_chunks, center_chunk_x + args.radius_chunks + 1):
        for chunk_z in range(center_chunk_z - args.radius_chunks, center_chunk_z + args.radius_chunks + 1):
            chunk = load_chunk(args.world, chunk_x, chunk_z)
            if chunk is not None:
                found.extend(block_positions(chunk, set(args.block)))

    for name, x, y, z in sorted(found, key=lambda item: (item[0], item[2], item[1], item[3])):
        print(f"{name} {x} {y} {z}")
    print(f"ANVIL_BLOCK_SCAN count={len(found)}")


if __name__ == "__main__":
    main()
