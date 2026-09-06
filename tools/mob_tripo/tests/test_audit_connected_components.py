import numpy as np
from PIL import Image

from tools.corrupted_silverfish_v5.tripo_voxel import MeshData
from tools.mob_tripo.audit_connected_components import (
    component_face_counts,
    component_stats,
    largest_component_mesh,
)


def test_component_audit_welds_uv_seams_but_finds_detached_shells():
    mesh = MeshData(
        positions=np.asarray(
            [
                [0, 0, 0], [1, 0, 0], [0, 1, 0],
                [1, 0, 0], [1, 1, 0], [0, 1, 0],
                [10, 0, 0], [11, 0, 0], [10, 1, 0],
            ],
            dtype=np.float32,
        ),
        triangles=np.asarray([[0, 1, 2], [3, 4, 5], [6, 7, 8]], dtype=np.int64),
        uvs=np.zeros((9, 2), dtype=np.float32),
        base_colour=Image.new("RGBA", (1, 1)),
    )

    assert component_face_counts(mesh) == [2, 1]
    assert component_stats(mesh)[1]["minimum"] == [10.0, 0.0, 0.0]
    cleaned = largest_component_mesh(mesh)
    assert cleaned.triangles.shape == (2, 3)
    # Keep UV-seam duplicates; only detached geometry is removed.
    assert cleaned.positions.shape == (6, 3)
