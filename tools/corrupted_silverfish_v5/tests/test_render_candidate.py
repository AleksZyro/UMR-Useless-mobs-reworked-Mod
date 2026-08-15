from pathlib import Path
import tempfile
import unittest

from PIL import Image

from tools.corrupted_silverfish_v5.render_candidate import render_review_set
from tools.corrupted_silverfish_v5.tripo_voxel import DEFAULT_MODEL


class CandidateRenderTests(unittest.TestCase):
    def test_review_set_contains_fixed_nonempty_views(self):
        with tempfile.TemporaryDirectory() as directory:
            paths = render_review_set(DEFAULT_MODEL, Path(directory))
            self.assertEqual(
                {path.name for path in paths},
                {"front.png", "right.png", "back.png", "top.png", "perspective.png"},
            )
            for path in paths:
                with Image.open(path) as image:
                    self.assertEqual(image.mode, "RGBA")
                    self.assertIsNotNone(image.getbbox(), path.name)

    def test_review_bytes_are_deterministic(self):
        with tempfile.TemporaryDirectory() as first, tempfile.TemporaryDirectory() as second:
            paths_a = render_review_set(DEFAULT_MODEL, Path(first))
            paths_b = render_review_set(DEFAULT_MODEL, Path(second))
            self.assertEqual(
                [path.read_bytes() for path in paths_a],
                [path.read_bytes() for path in paths_b],
            )


if __name__ == "__main__":
    unittest.main()
