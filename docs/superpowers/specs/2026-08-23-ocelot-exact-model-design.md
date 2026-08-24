# Ozelot: exaktes Tripo-Laufzeitmodell

## Ziel

Der bestehende Vanilla-Ozelot wird nicht lediglich umtexturiert. UMR erhält eine eigene Ozelot-Entität mit eigenem Spawn-Ei, einem unverwechselbaren Modell und der bereits vorhandenen Jagdgefährten-Mechanik als Gameplay-Grundlage.

## Visuelles Konzept

- Stil: natürlicher, kräftiger Dschungeljäger in hochwertiger Minecraft-Pixelästhetik.
- Farben: goldorange Grundfarbe, dunkle Rosetten und Streifen, helle Bauch- und Schnauzenpartien sowie smaragdgrüne Augen.
- Silhouette: erkennbarer Katzenkörper mit kräftigen Schultern, beweglichem langem Schwanz, klar getrennten Pfoten und leicht übergrossen Ohren.
- Keine Rüstung, Schmuckstücke, Pflanzen, Waffen oder künstlichen Leuchteffekte. Das Tier soll natürlich bleiben und sich deutlich von einem Tiger oder Hauskater unterscheiden.
- Zielgrösse: ungefähr 1,45 Blöcke Körperlänge. Die endgültige Hitbox wird aus den gemessenen Laufzeitgrenzen abgeleitet und nicht geschätzt.

## Bild- und Tripo-Pipeline

1. Ein konsistentes Vierseitenblatt erzeugen: vorne, links, hinten und rechts.
2. Alle Ansichten zeigen exakt dasselbe Tier, dieselben Proportionen und dieselbe Fellzeichnung.
3. Orthografische Kameras, neutrale Beleuchtung und echter transparenter Hintergrund; kein Boden, kein Schatten und kein Text.
4. Die vier Einzelansichten verlustfrei ausschneiden und in Tripo Multi-View laden.
5. Vor dem kostenpflichtigen Generieren den angezeigten Creditpreis festhalten und die Freigabe abwarten.
6. Das texturierte 4K-GLB unverändert exportieren. Keine Voxel-Neuerstellung und kein Ersatz durch Würfel.

## Laufzeitintegration

- Eigene Registry-ID und eigenes Spawn-Ei.
- Exakte Quellgeometrie und Tripo-Textur über den bestehenden UMR-Mesh-Lader.
- Regionen: Körper, Kopf, Schwanz sowie vier Beine. Jede Quellfläche wird genau einer Region zugeordnet.
- Kontinuierliche positionsbasierte Verformung für Laufen, Schleichen, Sprung und Schwanzbewegung, damit keine sichtbaren Schnitte entstehen.
- Bestehende Rekrutierungs-, Markierungs- und Sprungangriff-Mechaniken werden auf die neue Entität übertragen.
- Der Vanilla-Ozelot-Renderer bleibt unangetastet.

## Abnahme

- Quelle, Tripo-Projekt-ID, Hash, Dreiecksanzahl und Texturauflösung sind dokumentiert.
- Vorderseite und Laufrichtung stimmen überein.
- Textur sitzt in allen Ansichten exakt auf der Geometrie.
- Modell und Hitbox stimmen bei `F3+B` überein.
- Laufen, Schleichen, Springen und Schwanzbewegung wirken natürlich und öffnen keine Nähte.
- Spawn-Ei, Namen, Registry, Attribute und bestehendes Gameplay funktionieren.
- Modelltests, Projektwahrheitsprüfung und Java-Build sind grün.
