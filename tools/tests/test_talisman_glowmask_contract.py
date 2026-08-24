from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[2]
TEXTURE_DIR = ROOT / "src/main/resources/assets/usless_mobs/textures/item"


def test_every_talisman_glowmask_contains_visible_pixels() -> None:
    for path_name in ("void", "celestial", "living"):
        glowmask = Image.open(
            TEXTURE_DIR / f"{path_name}_talisman_geo_glowmask.png"
        ).convert("RGBA")

        assert glowmask.getbbox() is not None, (
            f"{path_name} talisman glowmask is fully transparent; "
            "GeckoLib AutoGlowingGeoLayer crashes when registering it"
        )


def test_talisman_glowmasks_match_their_base_texture_size() -> None:
    for path_name in ("void", "celestial", "living"):
        base = Image.open(TEXTURE_DIR / f"{path_name}_talisman_geo.png")
        glowmask = Image.open(
            TEXTURE_DIR / f"{path_name}_talisman_geo_glowmask.png"
        )

        assert base.size == (32, 32)
        assert glowmask.size == base.size
