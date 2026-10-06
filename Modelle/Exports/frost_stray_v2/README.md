# Frost Stray v2 – bearbeitbare Produktionsquelle

Dieser Ordner enthält die versionierte Quelle der v2-Darstellung:

- `frost_stray_v2_rig.json`: zwölf abgetrennte Eisakzente und fünf benannte
  visuelle Sockets für Hände, Bogen, Freigabe und Charge.
- `frost_stray_v2_clips.json`: Dauer, Zweck und Pose-Prüfungen der sieben
  prozeduralen Präsentationsclips.
- `STYLE_BRIEF.md`: gestalterische Ziele, Runtime-Materialgrenzen und Herkunft.
- `TEST_PLAN.md`: reproduzierbare Capture-Matrix, ohne noch nicht gemachte
  Spielaufnahmen als bestanden auszugeben.

Die zusammenhängende Frost-Stray-Basis bleibt auf dem genehmigten Runtime-Mesh.
Die Rigging-Prüfung verbietet, die ungewichtete Oberfläche in starre Körperknochen
aufzuschneiden. Die Clips animieren daher nur die getrennten Eisakzente, während
`ExactRigPose` die bestehende geschlossene Basis bewegt.

The runtime overlay is generated with:

```powershell
python tools/frost_stray_v2/build_assets.py
```

Die generierte 64×64-Overlay-Textur bleibt bewusst getrennt von der vorhandenen
2048×2048-Frost-Stray-Albedo. Farbpalette, UV-sicheres Raster und Runtime-Budget
sind im Generator versioniert. Der Generator schreibt ausserdem ein Manifest mit
Prüfsummen, Sockets, Clips, Quellen und Werkzeugversionen:

```text
/summon usless_mobs:frost_stray_v2 ~ ~ ~
```

Die v2-Testentität ist parallel auswählbar mit:

```text
/summon usless_mobs:frost_stray_v2 ~ ~ ~
```

Ein vollständiger Ersatz der verbundenen Basis durch ein gewichtetes 10–14-Bone-
Körperrig bleibt offen, bis eine bearbeitbare gewichtete Quelle vorliegt und die
Nahtprüfung besteht.
