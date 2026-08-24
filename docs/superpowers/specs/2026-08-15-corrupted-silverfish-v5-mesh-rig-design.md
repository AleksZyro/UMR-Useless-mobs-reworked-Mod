# Corrupted Silverfish v5 – Mesh-Rig-Design

## Ziel

Das direkt aus Tripo importierte Modell bleibt die visuelle Quelle. Geometrie, UVs und die eingebettete 4096×4096-Textur dürfen beim Rigging nicht neu interpretiert, voxelisiert oder vereinfacht werden.

## Ausgangslage

- Blockbench-Projekt: `Corrupted Silverfish v5 Tripo Mesh.bbmodel`
- Format: Generic Model / Mesh
- 50'897 Vertices, 101'723 Dreiecksflächen
- eine eingebettete Textur
- noch keine Animationen

## Gewählter Ansatz

Das Mesh wird anhand räumlicher Gelenkgrenzen in bewegliche Regionen getrennt. Die Quelldatei besitzt nur vier zusammenhängende Komponenten; 101'672 von 101'723 Flächen bilden eine einzige verschweisste Komponente. Eine reine Komponentenaufteilung kann deshalb keine Beine oder Körpersegmente erzeugen. An den Gelenkgrenzen dürfen Vertex-Referenzen technisch dupliziert werden, während Positionen, Flächen, UV-Koordinaten und Texturreferenz unverändert bleiben. Daraus entsteht eine klare Knochenhierarchie für Root, drei Körpersegmente, Kopf/Mandibeln, sechs Beine, Schwanz und sichtbare Kristallgruppen.

Eine erneute Cuboid-/Voxel-Konvertierung wird nicht verwendet, weil sie die bereits sichtbar beanstandete Abweichung erzeugt. Ein eigener Minecraft-Mesh-Renderer wird ebenfalls noch nicht gebaut; zuerst wird das visuell korrekte Blockbench-Mastermodell geriggt und geprüft.

## Sicherheits- und Qualitätsregeln

1. Die gespeicherte Tripo-Mesh-Datei bleibt unverändert als Referenz erhalten.
2. Das geriggte Modell wird als neue Datei gespeichert.
3. Vorher-/Nachher-Prüfung: gleiche Texturbytes, exakt dieselben Dreiecksflächen mit denselben Positionen und UV-Daten sowie eine pixelidentische Ruhepose. Zusätzliche Vertex-IDs sind ausschliesslich an Gelenkgrenzen zulässig, weil Blockbench Mesh-Objekte keine Vertex-Gewichte über mehrere Knochen unterstützen.
4. Keine Produktionsassets, Java-Dateien oder v2–v4-Modelle werden in dieser Stufe verändert.
5. Erst nach visueller Freigabe des Rigs folgen Idle-, Walk-, Attack-, Hurt- und Death-Animationen.

## Zwischenstände

1. Segmentierungsübersicht mit benannten Körperteilen
2. Geriggte Ruhepose in Blockbench
3. Erst danach Animationsvorschauen

## Bekannte Grenze

Das Generic-Mesh-Mastermodell ist nicht direkt mit dem bisherigen cube-basierten GeckoLib-`geo.json`-Pfad kompatibel. Nach der visuellen Freigabe wird separat entschieden, ob ein optimiertes Runtime-Modell oder ein eigener Mesh-Renderpfad verwendet wird. Diese Entscheidung darf die freigegebene Optik nicht rückwirkend verändern.
