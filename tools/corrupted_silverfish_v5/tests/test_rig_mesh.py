import copy
import unittest

from tools.corrupted_silverfish_v5.rig_mesh import (
    build_rig_document,
    canonical_faces,
    texture_signature,
)


def make_two_triangle_fixture():
    return {
        "meta": {"format_version": "5.0", "model_format": "free"},
        "name": "fixture",
        "resolution": {"width": 16, "height": 16},
        "elements": [
            {
                "name": "source_mesh",
                "type": "mesh",
                "uuid": "source-element",
                "origin": [0, 0, 0],
                "rotation": [0, 0, 0],
                "vertices": {
                    "a": [-5, 1, -6],
                    "b": [-5, 2, -6],
                    "c": [-5, 1, -5],
                    "d": [5, 1, 6],
                    "e": [5, 2, 6],
                    "f": [5, 1, 5],
                },
                "faces": {
                    "left": {
                        "texture": 0,
                        "vertices": ["a", "b", "c"],
                        "uv": {"a": [1, 1], "b": [1, 2], "c": [2, 1]},
                    },
                    "right": {
                        "texture": 0,
                        "vertices": ["d", "e", "f"],
                        "uv": {"d": [3, 3], "e": [3, 4], "f": [4, 3]},
                    },
                },
            }
        ],
        "outliner": ["source-element"],
        "textures": [
            {
                "name": "texture",
                "source": "data:image/png;base64,fixture",
                "width": 4096,
                "height": 4096,
                "uv_width": 16,
                "uv_height": 16,
            }
        ],
    }


class LosslessRigContractTests(unittest.TestCase):
    def test_repartition_preserves_every_position_uv_and_texture(self):
        source = make_two_triangle_fixture()

        rigged, _ = build_rig_document(copy.deepcopy(source))

        self.assertEqual(canonical_faces(rigged), canonical_faces(source))
        self.assertEqual(texture_signature(rigged), texture_signature(source))

    def test_source_document_is_not_mutated(self):
        source = make_two_triangle_fixture()
        original = copy.deepcopy(source)

        build_rig_document(source)

        self.assertEqual(source, original)


if __name__ == "__main__":
    unittest.main()
