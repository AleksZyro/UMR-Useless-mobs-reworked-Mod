from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont, ImageOps


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "Modelle/Exports/tripo_ingame_comparison"

PAIRS = {
    "living_boss": {
        "title": "LIVING BOSS / LIVING WARDEN",
        "tripo": OUT / "tripo/living_boss.png",
        "ingame": Path(r"C:\Users\Andrin\AppData\Local\Temp\codex-clipboard-9d8779c2-4bbc-4625-b18b-2d90babe7e51.png"),
        "verdict": "NICHT SICHTBAR 1:1 – Minecraft gibt das Tripo-Material anders wieder",
    },
    "web_cave_spider": {
        "title": "WEB CAVE SPIDER",
        "tripo": OUT / "tripo/web_cave_spider.png",
        "ingame": Path(r"C:\Users\Andrin\AppData\Local\Temp\codex-clipboard-b99e32f2-4cad-4a94-89f7-216aa64c68a5.png"),
        "verdict": "NICHT SICHTBAR 1:1 – UV-Daten gleich, Materialwiedergabe jedoch verschieden",
    },
}


def cover(image: Image.Image, size: tuple[int, int]) -> Image.Image:
    return ImageOps.fit(image.convert("RGB"), size, Image.Resampling.LANCZOS, centering=(0.5, 0.5))


def contain(image: Image.Image, size: tuple[int, int]) -> Image.Image:
    panel = Image.new("RGB", size, "#20232b")
    fitted = ImageOps.contain(image.convert("RGB"), size, Image.Resampling.LANCZOS)
    panel.paste(fitted, ((size[0] - fitted.width) // 2, (size[1] - fitted.height) // 2))
    return panel


def build(name: str, item: dict[str, object]) -> Path:
    width, panel_h = 720, 480
    canvas = Image.new("RGB", (width * 2, panel_h + 118), "#0f1117")
    draw = ImageDraw.Draw(canvas)
    regular = Path(r"C:\Windows\Fonts\segoeui.ttf")
    bold = Path(r"C:\Windows\Fonts\segoeuib.ttf")
    font = ImageFont.truetype(regular, 22) if regular.exists() else ImageFont.load_default()
    title_font = ImageFont.truetype(bold, 25) if bold.exists() else font

    with Image.open(item["tripo"]) as source:
        # Keep the Tripo viewport and discard most controls.
        source = source.crop((315, 65, 1015, 690))
        left = cover(source, (width, panel_h))
    with Image.open(item["ingame"]) as source:
        right = contain(source, (width, panel_h))

    canvas.paste(left, (0, 58))
    canvas.paste(right, (width, 58))
    draw.rectangle((0, 0, width * 2, 58), fill="#171a23")
    draw.text((24, 15), str(item["title"]), fill="#ffffff", font=title_font)
    draw.text((width - 90, 18), "TRIPO", fill="#7dd3fc", font=font)
    draw.text((width + 24, 18), "AKTUELL IM SPIEL", fill="#f9a8d4", font=font)
    draw.line((width, 58, width, 58 + panel_h), fill="#ffffff", width=3)
    draw.rectangle((0, 58 + panel_h, width * 2, panel_h + 118), fill="#171a23")
    draw.text((24, panel_h + 73), str(item["verdict"]), fill="#fca5a5", font=font)

    output = OUT / f"{name}_tripo_vs_ingame.png"
    output.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(output)
    return output


def main() -> None:
    outputs = [build(name, item) for name, item in PAIRS.items()]
    with Image.open(outputs[0]) as first, Image.open(outputs[1]) as second:
        overview = Image.new("RGB", (first.width, first.height + second.height + 16), "#08090d")
        overview.paste(first, (0, 0))
        overview.paste(second, (0, first.height + 16))
        overview.save(OUT / "verified_tripo_vs_ingame_overview.png")
    for output in outputs:
        print(output.resolve())


if __name__ == "__main__":
    main()
