# Corrupted Silverfish v2 – reproduzierbarer Modellbau

Dieses Vorschaupaket wird vollständig aus versionierten Eingaben erzeugt. Die produktiven Ressourcen bleiben bis zur ausdrücklichen Freigabe unverändert.

## Ein-Befehl-Ablauf

Aus dem Ordner `slime`:

```powershell
.\tools\Build-CorruptedSilverfishV2.ps1
python .\tools\Render-CorruptedSilverfishPreview.py
.\tools\Test-CorruptedSilverfishAssets.ps1 -Target Preview
```

Der Generator erstellt:

- die editierbare Blockbench-Datei mit 19 Knochen, 18 Würfeln und eingebetteter Idle-Animation,
- den GeckoLib-Geometrieexport,
- die 128 × 64-Pixeltextur aus der festen Projektpalette,
- die GeckoLib-Idle-Animation mit 13 animierten Knochen,
- vier orthografische Kandidatenansichten und eine Pose bei 0,3 Sekunden.

## Visuelle Quellen

`concept/concept_sheet_raw.png` ist die unveränderte Ausgabe der ChatGPT-Bildgenerierung. `concept/concept_sheet_cropped.png` ist die geprüfte, transparente und beschnittene Stilreferenz. Beide werden nicht direkt als Spieltextur verwendet. Der Atlas wird deterministisch aus den festgelegten UV-Bereichen und Palettenfarben aufgebaut.

## Blockbench

`../../Editierbar/Corrupted Silverfish v2.bbmodel` lässt sich direkt in Blockbench Desktop öffnen. Die Datei enthält Geometrie, Gruppenhierarchie, eingebettete Textur und die vollständige Idle-Timeline. Blockbench Web blockierte im automatisierten Browser den lokalen Dateiimport; deshalb erzeugt `Render-CorruptedSilverfishPreview.py` zusätzlich reproduzierbare, lokale Kontrollansichten ohne manuellen Upload.

## Dateischutz

`review/baseline-sha256.json` schützt die vier Originaldateien. `review/candidate-sha256.json` identifiziert den aktuellen Kandidaten. Eine Übernahme in `src/main/...` erfolgt erst nach einer separaten visuellen Freigabe.
