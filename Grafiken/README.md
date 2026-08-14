# Grafiken Workspace

Editiere hier deine Texturen — geordnet nach Kategorie.

## Struktur

```
Grafiken/
├── entity/        # Mob-Texturen (Slime-Body, Ender-Slime, King-Slime, etc.)
├── item/          # Item-Icons (Schwert, Kompass, Schleimball, etc.)
├── block/         # Block-Texturen (Schleimblock, etc.)
└── mob_effect/    # Mob-Effekt-Icons (Elasticity, Golden Flow)
```

## Workflow

1. **Editiere** eine Datei direkt hier (z.B. `entity/king_slime.png` in Aseprite/Photoshop/ChatGPT)
2. **Sync zurück** in die Mod-Assets:
   ```powershell
   .\sync_assets.ps1
   ```
   (im Projekt-Root, nicht in `Grafiken/`)
3. **Reload** im laufenden Spiel mit `F3+T` ODER neu starten mit `.\gradlew.bat runClient`

## Wichtige Pfade

| Was du editierst                  | Wo es im Mod landet                                     |
|-----------------------------------|---------------------------------------------------------|
| `entity/blauer_schleim.png`       | `assets/usless_mobs/textures/entity/blauer_schleim.png` |
| `item/king_slime_krone.png`       | `assets/usless_mobs/textures/item/king_slime_krone.png` |
| `block/blauer_schleimblock.png`   | `assets/usless_mobs/textures/block/...`                 |
| `mob_effect/golden_flow.png`      | `assets/usless_mobs/textures/mob_effect/...`            |

## Hinweise

- **Pixel-Art-Stil**: 16×16 oder 32×32 nativ, dann 8×/16× hochskalieren (Nearest-Neighbor)
- **HD-Stil**: 256×256 oder 512×512 direkt (so wie deine ChatGPT-Items aktuell)
- **Entity-Textur-Layout**: 64×32 oder 64×64 virtuell (Slime hat 64×32 Vanilla-Layout)
- **Tipp**: Wenn du eine neue Datei erstellst die nicht hier existiert, leg sie einfach in den richtigen Subfolder und sync. Existierende Dateien werden überschrieben.
