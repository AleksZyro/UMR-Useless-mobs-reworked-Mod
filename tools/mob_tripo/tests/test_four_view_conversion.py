from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from PIL import Image

from tools.armor_graphics.tripo_to_blockbench import VIEW_NAMES, load_views


class FourViewConversionContract(unittest.TestCase):
    def test_four_views_can_be_loaded_without_an_invented_top_view(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for name in VIEW_NAMES[:-1]:
                Image.new("RGBA", (8, 8), (20, 40, 60, 255)).save(root / f"{name}.png")

            views = load_views(root, VIEW_NAMES[:-1])

            self.assertEqual(set(VIEW_NAMES[:-1]), set(views))
            self.assertNotIn("top", views)

    def test_chroma_green_canvas_is_not_sampled_as_mob_texture(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for name in VIEW_NAMES[:-1]:
                image = Image.new("RGBA", (8, 8), (0, 255, 0, 255))
                for y in range(2, 6):
                    for x in range(3, 5):
                        image.putpixel((x, y), (90, 40, 20, 255))
                image.save(root / f"{name}.png")

            views = load_views(root, VIEW_NAMES[:-1])

            for view in views.values():
                self.assertEqual((3, 2, 4, 5), view.bbox)
                self.assertEqual(0, int(view.pixels[0, 0, 3]))
                self.assertEqual(255, int(view.pixels[3, 3, 3]))


if __name__ == "__main__":
    unittest.main()
