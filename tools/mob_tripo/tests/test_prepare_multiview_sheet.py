from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from PIL import Image

from tools.mob_tripo.prepare_multiview_sheet import prepare


class PrepareMultiviewSheetTests(unittest.TestCase):
    def test_crops_equal_two_by_two_sheet_in_tripo_order(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            source = root / "sheet.png"
            output = root / "views"
            sheet = Image.new("RGB", (200, 160))
            sheet.paste((240, 20, 20), (0, 0, 100, 80))
            sheet.paste((20, 240, 20), (100, 0, 200, 80))
            sheet.paste((20, 20, 240), (0, 80, 100, 160))
            sheet.paste((240, 220, 20), (100, 80, 200, 160))
            sheet.save(source)

            prepare(source, output)

            expected = {
                "front.png": (240, 20, 20),
                "right.png": (20, 240, 20),
                "back.png": (20, 20, 240),
                "left.png": (240, 220, 20),
            }
            self.assertEqual(set(expected), {path.name for path in output.iterdir()})
            for name, colour in expected.items():
                with Image.open(output / name) as view:
                    self.assertEqual((100, 80), view.size)
                    self.assertEqual(colour, view.convert("RGB").getpixel((50, 40)))

    def test_rejects_odd_sheet_dimensions(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            source = root / "sheet.png"
            Image.new("RGB", (201, 160), "green").save(source)

            with self.assertRaisesRegex(ValueError, "even width and height"):
                prepare(source, root / "views")


if __name__ == "__main__":
    unittest.main()
