# Modelle Workspace

Editiere hier deine 3D-Modelle (Blockbench) und Animationen für GeckoLib.

## Dateien

| Datei                              | Was es ist                                                    |
|------------------------------------|---------------------------------------------------------------|
| `King slime.bbmodel`               | Blockbench-Quelldatei (geckolib_model format) für King Slime  |
| `Soul slime.bbmodel`               | Blockbench-Quelldatei für den Soul Slime (noch nicht im Code) |
| `king_slime.geo.json`              | GeckoLib Geometry (cube definitions + UVs) — wird vom Spiel geladen |
| `king_slime.animation.json`        | GeckoLib Animations (idle, jump) — wird vom Spiel geladen     |

## Workflow

### Modell ändern (in Blockbench)
1. Öffne `King slime.bbmodel` in Blockbench
2. Modell editieren (Cubes verschieben, Bones umbenennen, UVs ändern)
3. **Export** → `Export Bedrock Geometry` → speichere als `king_slime.geo.json` in diesen Ordner
4. Sync zurück:
   ```powershell
   .\sync_assets.ps1
   ```
5. Im Spiel: `F3+T` für Resource-Reload oder Mod neu starten

### Animation ändern
1. In Blockbench → Animations-Tab → Animation editieren
2. **Export** → `Export Bedrock Animation` → speichere als `king_slime.animation.json` hier
3. Sync + Reload wie oben

### Neues Modell hinzufügen (z.B. Soul Slime)
1. `.bbmodel` in Blockbench bauen
2. Geo + Animation hier in `Modelle/` speichern
3. Java-Code muss erweitert werden (neue Entity-Klasse, Renderer-Klasse, GeoModel-Klasse) — sag dem Assistant Bescheid

## Wichtig zu wissen

- **Format**: `geckolib_model` in Blockbench (oben rechts beim Erstellen wählen)
- **Bones brauchen Namen** (nicht null!) damit GeckoLib animieren kann. Mindestens `root` → `body`
- **Texture**: GeckoLib lädt eine Textur pro Geometry. Der Pfad ist im Code (`KingSlimeModel.java`) auf `usless_mobs:textures/entity/king_slime_geo.png` gesetzt.
- **`.bbmodel` selbst wird NICHT vom Spiel gelesen** — nur das exportierte `.geo.json`. Die `.bbmodel` ist nur deine Quelldatei.
