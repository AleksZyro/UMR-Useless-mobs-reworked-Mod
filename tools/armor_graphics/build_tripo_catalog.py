from __future__ import annotations

import hashlib
import json
from pathlib import Path

from PIL import Image


FAMILIES = ("true_void", "true_celestial", "true_living", "armor_of_balance")
PIECES = ("helmet", "chestplate", "leggings", "boots")
TRIPO_VIEWS = ("front", "left", "back", "right")
EXTRA_VIEWS = ("top",)


def build(root: Path) -> None:
    assets = []
    cards = []
    for family in FAMILIES:
        for piece in PIECES:
            entries = {}
            for view in TRIPO_VIEWS + EXTRA_VIEWS:
                path = root / family / piece / f"{view}.png"
                payload = path.read_bytes()
                with Image.open(path) as image:
                    image.load()
                    if image.mode != "RGBA" or image.size != (768, 768):
                        raise ValueError(f"{path}: expected RGBA 768x768")
                    alpha = image.getchannel("A")
                    if alpha.getbbox() is None:
                        raise ValueError(f"{path}: empty subject")
                    if any(image.getpixel(point)[3] for point in ((0, 0), (767, 0), (0, 767), (767, 767))):
                        raise ValueError(f"{path}: background is not transparent at every corner")
                relative = path.relative_to(root).as_posix()
                entries[view] = {
                    "file": relative,
                    "sha256": hashlib.sha256(payload).hexdigest(),
                    "width": 768,
                    "height": 768,
                    "mode": "RGBA",
                }
            assets.append({
                "family": family,
                "piece": piece,
                "tripo_upload_order": list(TRIPO_VIEWS),
                "views": entries,
            })
            images = "".join(
                f'<figure><img src="{entries[view]["file"]}" alt="{family} {piece} {view}"><figcaption>{view}</figcaption></figure>'
                for view in TRIPO_VIEWS + EXTRA_VIEWS
            )
            cards.append(f'<section><h2>{family} / {piece}</h2><div class="views">{images}</div></section>')

    manifest = {
        "schema": 1,
        "purpose": "Tripo multiview input; top is an extra review view and is not uploaded to a directional slot.",
        "official_upload_order": list(TRIPO_VIEWS),
        "asset_count": len(assets),
        "assets": assets,
    }
    (root / "tripo-input-manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    html = """<!doctype html><html lang="de"><meta charset="utf-8"><title>Armor Tripo Inputs</title>
<style>body{margin:0;background:#11131a;color:#eef1ff;font:15px system-ui;padding:28px}h1{margin:0 0 8px}p{color:#aeb6cc}section{margin:24px 0;padding:18px;background:#1b1e29;border:1px solid #303548;border-radius:14px}.views{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:12px}figure{margin:0}img{width:100%;aspect-ratio:1;object-fit:contain;background:repeating-conic-gradient(#252938 0 25%,#1d202c 0 50%) 50%/20px 20px;border-radius:9px}figcaption{text-align:center;margin-top:5px;color:#cbd2e8}@media(max-width:900px){.views{grid-template-columns:repeat(2,1fr)}}</style>
<h1>Armor Tripo Multiview Inputs</h1><p>Upload-Reihenfolge pro Teil: Front → Links → Rückseite → Rechts. Oben ist nur eine zusätzliche Referenz.</p>
""" + "".join(cards) + "</html>\n"
    (root / "review.html").write_text(html, encoding="utf-8")
    print(f"TRIPO_CATALOG_PASS ASSETS={len(assets)} UPLOAD_VIEWS={len(assets) * len(TRIPO_VIEWS)} EXTRA_VIEWS={len(assets)}")


if __name__ == "__main__":
    build(Path("Modelle/Exports/armor_graphics_tripo"))
