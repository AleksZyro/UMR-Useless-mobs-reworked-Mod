# Frost Stray v2 – Produktionsstand

Stand: Arbeitsbranch `feature/frost-stray-ability-timeline`; dieser Bericht trennt
mechanisch geprüfte Fakten, Implementierung und noch fehlende Spielabnahme.

## Bestandsaufnahme

| Bereich | Aktueller, versionierter Stand |
| --- | --- |
| Bearbeitbare Basis | `Modelle/Exports/frost_stray_v1/source/frost_stray_textured_4k_v3_candidate.glb`; verbundene, ungewichtete Tripo-Oberfläche. |
| Tatsächlich geladene Basis | `meshes/entity/custom3d/frost_stray.mesh` mit 98'103 Dreiecken und `textures/entity/custom3d/exact/frost_stray.png` mit 2048 × 2048 Pixeln. |
| v2-Ergänzung | zwölf getrennte Eisakzente, 144 berechnete Cuboid-Dreiecke, 64 × 64 deterministische Overlay-Albedo. |
| Bewegung | Basis: kontinuierliche `ExactRigPose`-Deformation; v2: prozedurale Akzentclips. Es existiert kein GeckoLib-Clip und kein gewichtet animiertes Körpermesh. |
| Material | Basisalbedo plus transparente/fullbright gerenderte v2-Eisakzente. Der verwendete Entity-Renderer besitzt hier keine getrennte Normal- oder Roughness-Map-Auswertung. |
| Bogen und Ereignisse | vorhandener Held-Item-Anchor; die neue visuelle Charge-/Release-Position verwendet fünf dokumentierte Entity-lokale Sockets. Schaden, Projektilfreigabe und Impact bleiben serverautorativ. |

Die gemessenen Mesh-/Texturfakten stammen aus dem aktuellen Projekt-Wahrheitscheck
und dem Frost-Stray-Report, nicht aus alten Dateinamen.

## Gestalterische Umsetzung

Der verbindliche [Style-Brief](../Modelle/Exports/frost_stray_v2/STYLE_BRIEF.md)
definiert die drei sichtbaren Ziele. Die produktive Implementierung ergänzt:

- Bewegte Krone, Gesichtsscherbe, Schultern, Unterarme, Rücken und Hüfte für
  Idle, Walk/Run, Bogenladung, Hurt und Death, ohne die verbundene Grundform
  aufzuschneiden.
- Charge-Partikel vom Brustanker zum Bogengriff, Release am Projektil-Socket
  und einen bestätigten Kollisions-Impact mit Schnee- und Splitterakzent.
- Getrennte Charge-, Release- und Impact-Sounds aus dokumentierten vorhandenen
  Minecraft-Soundevents. Späte Beobachter erhalten keine alte Charge erneut.

Die exakten VFX-Budgets und Partikeldistanzen liegen deklarativ in
`assets/usless_mobs/vfx_profiles/frost_stray_volley.json`: maximal 4 Charge-
Partikel nahe, 16 Release- und 24 Impact-Partikel inklusive Akzent bei 12
Blöcken; wichtige One-Shots bleiben bis 32 Blöcke mit einem Minimal-Cue lesbar.

## Reproduzierbarer Export

```powershell
python tools/frost_stray_v2/build_assets.py --check
```

Der Befehl validiert zwölf eindeutige Overlay-Bones, fünf Sockets und sieben
Clipdefinitionen, erzeugt die Runtime-Textur und aktualisiert das Manifest mit
Quellhash, Werkzeugversionen, Koordinatenkonvention, Sockets, Clipdauern und
Output-Hash.

## Nachweise und offene Abnahme

- Fokussierte Frost-Stray-/Sound-/Rig-Verträge: 30 bestanden.
- Entity-Matrix nach vollständiger v2-Spawn-Ei-Integration: 5 bestanden.
- Assetgenerator und JSON-Parsing: bestanden.
- Der direkte Java-Test der Timeline-Clock besteht; der lokale Forge-Start ist
  weiterhin durch die bekannte Windows-Loopback-Grenze blockiert.
- CI-Lauf #41 scheiterte vor Forge beim vollständigen Python-Lauf. Die konkrete
  bekannte Branch-Regressionsursache – unvollständige v2-Entity-Matrix – ist in
  diesem Arbeitsstand behoben und wird mit dem nächsten Push erneut geprüft.
- Zwei-Client-Test, echter Forge-Client, Dedicated-Server, Screenshots/Video
  bei 4/12/24 Blöcken sowie Framezeit-/Serverlastmessung sind nicht ausgeführt
  und ausdrücklich nicht als bestanden markiert.

## Zweiter Körpertyp und Folgearbeit

Der Web Cave Spider ist der richtige zweite Durchlauf: acht Beine und eine
flache quadrupede Silhouette prüfen Annahmen, die der humanoide Frost Stray
nicht abdeckt. Der bestehende Spider besitzt bereits eine getrennte
Achtbein-Deformation; ein neuer Durchlauf beginnt erst nach Frost-Stray-v2-
Clientabnahme, damit kein unbearbeiteter Assetordner als Qualitätsfreigabe
ausgegeben wird.

| Priorität | Mob | Nächstes abgegrenztes Paket |
| --- | --- | --- |
| 1 | Frost Stray v2 | Forge-Client, zwei Clients, Capture-Matrix und sichtbare Abnahme. |
| 2 | Web Cave Spider | dieselbe Manifest-/Pose-/Capture-Pipeline für acht Beine validieren. |
| 3 | Octopus | Socket-/Trail-Konzept auf Tentakelbewegung und Wasser-LOD übertragen. |
| 4 | Coral Drowned | humanoiden Nah-/Fernkampf-Übergang mit Material- und Sound-Politur prüfen. |
