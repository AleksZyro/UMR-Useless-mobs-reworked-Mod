# Changelog

Alle wichtigen Änderungen an Useless Mobs Reworked werden hier dokumentiert. Das Projekt verwendet Semantic Versioning, solange dies mit den Minecraft-/Forge-Kompatibilitätsgrenzen vereinbar ist.

## [Unreleased]

### Fixed

- Dependabot no longer proposes isolated Gradle or ForgeGradle major upgrades that cannot build together; compatible minor, patch, and security updates remain enabled.
- GitHub Actions cancels superseded builds for the same branch or pull request instead of producing multiple obsolete 276 MB artifacts in parallel.

### Changed

- Client-Renderer, Modell-Layer, Tastenbelegung und Item-Properties aus dem gemeinsamen Mod-Einstiegspunkt in eine strikt clientseitige Eventklasse ausgelagert
- Entity-Attribute und Spawnplatzierungen aus dem Mod-Einstiegspunkt in eine gemeinsame Mod-Eventklasse ausgelagert
- Braurezepte in eine eigene Registry-Hilfsklasse ausgelagert
- Inhalte der Vanilla-Creative-Tabs in eine eigene Mod-Eventklasse ausgelagert
- GitHub Actions auf Node-24-basierte Versionen aktualisiert

### Added

- monatliche Dependabot-Prüfungen für Gradle und GitHub Actions
- Regressionstest gegen versehentliche Client-Klassenverknüpfungen im Dedicated-Server-Einstiegspunkt

## [1.0.0-alpha.2] – 2026-08-24

### Changed

- sichtbaren Modnamen, Autoren und Lizenzmetadaten vereinheitlicht
- Dependency-Versionen in gradle.properties zentralisiert
- zeitabhängigen Manifest-Eintrag für besser reproduzierbare Builds entfernt

### Fixed

- Backup-Datei aus dem produktiven Java-Quellbaum entfernt
- UTF-8-Verarbeitung für expandierte Mod-Ressourcen festgelegt
- Netzwerkpakete auf ihre erlaubte Richtung begrenzt und clientseitige Death-Mark-Werte validiert

## [1.0.0-alpha.1] – 2026-08-24

### Added

- dedizierte UMR-Entitäten, Bosskampfsysteme und Progression
- Corrupted Silverfish mit GeckoLib-Rig und exaktem 4K-Laufzeit-Mesh
- Exact-Mesh-Pipeline für zahlreiche Tripo-Modelle
- eigene Modelle, Texturen, Sounds, Effekte, Spawn-Eier und Hitboxverträge
- Python-Regressionsprüfungen und vollständiger Forge-Build

### Known issues

- Alpha-Stand: abschliessende visuelle QA und Balancing bleiben vor einem stabilen Release erforderlich

[Unreleased]: https://github.com/AleksZyro/UMR-Useless-mobs-reworked-Mod/compare/v1.0.0-alpha.2...HEAD
[1.0.0-alpha.2]: https://github.com/AleksZyro/UMR-Useless-mobs-reworked-Mod/compare/v1.0.0-alpha.1...v1.0.0-alpha.2
[1.0.0-alpha.1]: https://github.com/AleksZyro/UMR-Useless-mobs-reworked-Mod/releases/tag/v1.0.0-alpha.1
