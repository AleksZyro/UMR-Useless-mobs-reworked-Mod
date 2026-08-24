from pathlib import Path
import tempfile
import unittest

from PIL import Image, ImageDraw

from tools.mob_tripo.prepare_explicit_multiview import prepare_explicit_views


class PrepareExplicitMultiviewTests(unittest.TestCase):
    def test_extracts_complete_uneven_views_with_shared_scale_and_transparency(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            source = root / "sheet.png"
            output = root / "views"
            sheet = Image.new("RGB", (600, 500), (253, 253, 253))
            draw = ImageDraw.Draw(sheet)
            for y in range(0, 500, 20):
                for x in range(0, 600, 20):
                    if (x // 20 + y // 20) % 2:
                        draw.rectangle((x, y, x + 19, y + 19), fill=(247, 247, 247))

            subjects = {
                "front": ((30, 30, 129, 229), (180, 70, 20)),
                "left": ((190, 40, 519, 219), (190, 80, 30)),
                "back": ((40, 270, 149, 469), (200, 90, 40)),
                "right": ((180, 280, 529, 459), (210, 100, 50)),
            }
            for box, colour in subjects.values():
                draw.rectangle(box, fill=colour)
            sheet.save(source)

            crops = {
                "front": (10, 10, 160, 250),
                "left": (170, 10, 550, 250),
                "back": (10, 250, 170, 490),
                "right": (170, 250, 550, 490),
            }
            outputs = prepare_explicit_views(source, output, crops, canvas_size=512, padding=24)

            self.assertEqual(set(outputs), set(subjects))
            visible_sizes = {}
            for name, (_, colour) in subjects.items():
                with Image.open(outputs[name]) as image:
                    self.assertEqual(image.mode, "RGBA")
                    self.assertEqual(image.size, (512, 512))
                    self.assertEqual(image.getpixel((0, 0))[3], 0)
                    alpha_box = image.getchannel("A").getbbox()
                    self.assertIsNotNone(alpha_box)
                    visible_sizes[name] = (alpha_box[2] - alpha_box[0], alpha_box[3] - alpha_box[1])
                    centre = ((alpha_box[0] + alpha_box[2]) // 2, (alpha_box[1] + alpha_box[3]) // 2)
                    self.assertEqual(image.getpixel(centre)[:3], colour)

            # Every view receives exactly the same scale factor.
            self.assertAlmostEqual(visible_sizes["front"][1] / 200, visible_sizes["left"][1] / 180, delta=0.02)
            self.assertAlmostEqual(visible_sizes["left"][0] / 330, visible_sizes["right"][0] / 350, delta=0.02)


if __name__ == "__main__":
    unittest.main()
