from pathlib import Path
import tempfile
import unittest

from PIL import Image, ImageDraw

from tools.armor_graphics.prepare_tripo_multiview import prepare, prepare_left_views


class PrepareTripoMultiviewTests(unittest.TestCase):
    def test_splits_four_views_and_removes_connected_checker_background(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            source = root / "sheet.png"
            output = root / "prepared"
            sheet = Image.new("RGB", (400, 400), (254, 254, 254))
            draw = ImageDraw.Draw(sheet)
            for y in range(0, 400, 20):
                for x in range(0, 400, 20):
                    if (x // 20 + y // 20) % 2:
                        draw.rectangle((x, y, x + 19, y + 19), fill=(248, 248, 248))
            colours = {
                "front": (80, 20, 120),
                "right": (100, 30, 140),
                "back": (120, 40, 160),
                "top": (140, 50, 180),
            }
            positions = [(50, 50), (250, 50), (50, 250), (250, 250)]
            for colour, (x, y) in zip(colours.values(), positions):
                draw.rectangle((x, y, x + 99, y + 99), fill=colour)
            sheet.save(source)

            prepare(source, output)

            for name, colour in colours.items():
                image = Image.open(output / f"{name}.png").convert("RGBA")
                self.assertEqual(image.size, (768, 768))
                self.assertEqual(image.getpixel((0, 0))[3], 0)
                self.assertEqual(image.getchannel("A").getbbox(), (334, 334, 434, 434))
                self.assertEqual(image.getpixel((384, 384))[:3], colour)

    def test_splits_family_left_sheet_into_piece_directories(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            source = root / "left_sheet.png"
            sheet = Image.new("RGB", (400, 400), (0, 255, 0))
            draw = ImageDraw.Draw(sheet)
            colours = {
                "helmet": (200, 20, 20),
                "chestplate": (20, 180, 180),
                "leggings": (20, 20, 200),
                "boots": (200, 200, 20),
            }
            for index, colour in enumerate(colours.values()):
                column, row = index % 2, index // 2
                left, top = column * 200 + 50, row * 200 + 50
                draw.rectangle((left, top, left + 99, top + 99), fill=colour)
            draw.rectangle((198, 0, 201, 399), fill=(0, 0, 0))
            draw.rectangle((0, 198, 399, 201), fill=(0, 0, 0))
            sheet.save(source)

            outputs = prepare_left_views(source, root / "family")

            self.assertEqual(set(outputs), set(colours))
            for name, colour in colours.items():
                with Image.open(outputs[name]) as image:
                    self.assertEqual(image.size, (768, 768))
                    self.assertEqual(image.mode, "RGBA")
                    self.assertEqual(image.getpixel((0, 0))[3], 0)
                    self.assertEqual(image.getchannel("A").getbbox(), (334, 334, 434, 434))
                    self.assertEqual(image.getpixel((384, 384))[:3], colour)


if __name__ == "__main__":
    unittest.main()
