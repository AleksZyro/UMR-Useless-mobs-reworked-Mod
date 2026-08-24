import json
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/usless_mobs"


def test_every_referenced_item_texture_has_visible_pixels():
    for model_path in (ASSETS / "models/item").glob("*.json"):
        model = json.loads(model_path.read_text(encoding="utf-8"))
        for texture in model.get("textures", {}).values():
            if not isinstance(texture, str) or texture.startswith("#"):
                continue
            namespace, separator, relative = texture.partition(":")
            if separator and namespace != "usless_mobs":
                continue
            relative = relative if separator else namespace
            texture_path = ASSETS / "textures" / f"{relative}.png"
            assert texture_path.is_file(), (model_path.name, texture)
            with Image.open(texture_path) as image:
                visible = image.convert("RGBA").getchannel("A").getbbox()
            assert visible is not None, (model_path.name, texture, "fully transparent")
