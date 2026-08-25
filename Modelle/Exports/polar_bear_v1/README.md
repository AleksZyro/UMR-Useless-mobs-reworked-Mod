# Eisbär v1

- Tripo-Aufgabe: `bfda1abd-890a-4000-a7df-bbcffcb99631`
- Erzeugung: Tripo v3.1, Multi-View-Builder, vier zugeordnete Ansichten
- Quelle: `source/polar_bear_textured_4k.glb`
- SHA-256: `10C6DCF3069DEB53FC526CDE230231900641A48674FBD03902FABBDC40D0C447`
- Quelle: 364’791 Vertices, 684’939 Dreiecke, 4096 × 4096 Albedo
- Laufzeit: 0 Cubes, 6 Regionen, 635’841 Dreiecke; die einzige vollständig abgetrennte seitliche Fremdkomponente mit 49’098 Dreiecken wird beim Export verworfen
- Regionen: `body`, `head`, `leg_front_left`, `leg_front_right`, `leg_rear_left`, `leg_rear_right`
- Verifizierte Vorwärtsachse: `-Z`
- Zielgrösse: 30,4 Modellpixel beziehungsweise 1,9 Blöcke Körperlänge

Hauptkörper, UVs und 4K-Albedo bleiben unverändert. Die Laufzeitanimation verwendet ein kontinuierliches positionsbasiertes Verformungsfeld. Dadurch bewegen sich Kopf und Pfoten, ohne dass sich das ungewichtete Tripo-Mesh an Regionsgrenzen aufschneidet.
