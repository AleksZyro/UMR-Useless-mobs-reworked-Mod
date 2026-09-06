"""Find detached geometric shells in Tripo GLBs without changing the source."""

from __future__ import annotations

import argparse
from pathlib import Path

import numpy as np
from scipy import sparse
from scipy.sparse.csgraph import connected_components

from tools.corrupted_silverfish_v5.tripo_voxel import MeshData, load_glb


def _component_data(mesh: MeshData, decimals: int):
    positions = np.round(np.asarray(mesh.positions, dtype=np.float64), decimals)
    unique_positions, welded = np.unique(positions, axis=0, return_inverse=True)
    triangles = welded[np.asarray(mesh.triangles, dtype=np.int64)]
    edges = np.concatenate((triangles[:, [0, 1]], triangles[:, [1, 2]], triangles[:, [2, 0]]))
    rows = np.concatenate((edges[:, 0], edges[:, 1]))
    columns = np.concatenate((edges[:, 1], edges[:, 0]))
    graph = sparse.coo_matrix(
        (np.ones(len(rows), dtype=np.uint8), (rows, columns)),
        shape=(int(welded.max()) + 1, int(welded.max()) + 1),
    ).tocsr()
    _, labels = connected_components(graph, directed=False)
    return unique_positions, triangles, labels[triangles[:, 0]]


def component_stats(mesh: MeshData, decimals: int = 5) -> list[dict[str, object]]:
    """Return triangle counts per spatially connected shell, largest first.

    Positions are welded before the graph is built so harmless UV-seam vertex
    duplication does not get reported as floating geometry.
    """
    unique_positions, triangles, face_labels = _component_data(mesh, decimals)
    stats = []
    for label, face_count in enumerate(np.bincount(face_labels).astype(int)):
        if not face_count:
            continue
        vertices = np.unique(triangles[face_labels == label])
        points = unique_positions[vertices]
        stats.append(
            {
                "faces": int(face_count),
                "minimum": points.min(axis=0).round(4).tolist(),
                "maximum": points.max(axis=0).round(4).tolist(),
                "centre": points.mean(axis=0).round(4).tolist(),
            }
        )
    return sorted(stats, key=lambda item: int(item["faces"]), reverse=True)


def component_face_counts(mesh: MeshData, decimals: int = 5) -> list[int]:
    return [int(item["faces"]) for item in component_stats(mesh, decimals)]


def largest_component_mesh(mesh: MeshData, decimals: int = 5) -> MeshData:
    """Return the largest connected shell with original UVs and texture."""
    _, _, face_labels = _component_data(mesh, decimals)
    largest_label = int(np.bincount(face_labels).argmax())
    selected = np.asarray(mesh.triangles, dtype=np.int64)[face_labels == largest_label]
    used = np.unique(selected)
    remap = np.full(len(mesh.positions), -1, dtype=np.int64)
    remap[used] = np.arange(len(used), dtype=np.int64)
    return MeshData(
        positions=np.asarray(mesh.positions)[used],
        uvs=np.asarray(mesh.uvs)[used],
        triangles=remap[selected],
        base_colour=mesh.base_colour,
    )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("glb", nargs="+", type=Path)
    args = parser.parse_args()
    for path in args.glb:
        stats = component_stats(load_glb(path))
        counts = [int(item["faces"]) for item in stats]
        detached = counts[1:]
        tiny = sum(count for count in detached if count < counts[0] * 0.001)
        print(
            f"{path}: triangles={sum(counts)} shells={len(counts)} "
            f"largest={counts[:8]} tiny_detached_faces={tiny}"
        )
        for index, item in enumerate(stats[:8]):
            print(f"  shell[{index}]={item}")


if __name__ == "__main__":
    main()
