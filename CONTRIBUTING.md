# Zu UMR beitragen

Vielen Dank für dein Interesse an Useless Mobs Reworked.

## Entwicklungsumgebung

- Java 17
- Git
- Minecraft Forge 1.20.1 / Forge 47.4.16
- Python 3.9 oder neuer für Asset- und Regressionstests
- IntelliJ IDEA ist empfohlen, aber nicht vorgeschrieben

## Repository einrichten

    git clone https://github.com/AleksZyro/UMR-Useless-mobs-reworked-Mod.git
    cd UMR-Useless-mobs-reworked-Mod
    .\gradlew.bat build

Der Entwicklungsclient startet mit:

    .\gradlew.bat runClient

## Branches und Commits

- grössere Änderungen in einem eigenen Feature-Branch entwickeln
- beschreibende Branch-Namen wie feature/octopus-ai oder fix/frost-stray-animation verwenden
- Commit-Präfixe wie feat:, fix:, test:, docs: und art: beibehalten
- pro Commit möglichst eine logisch abgeschlossene Änderung
- keine Backup-, Cache-, Build- oder Zugangsdaten committen

## Pflichtprüfungen

Vor einem Pull Request:

    python tools/verify_umr_project_truth.py
    python -m pytest -q
    .\gradlew.bat clean build

Bei Client-, Renderer- oder Gameplayänderungen zusätzlich die betroffene Funktion im Spiel prüfen. Ein generiertes Vorschaubild ersetzt keine Laufzeitprüfung.

## Modelle und Texturen

Für Tripo-/Blockbench-Modelle gilt:

1. aktive Projektwahrheit in docs/UMR_ACTIVE_PROJECT_STATE.md lesen
2. unveränderte Quellgeometrie und Textur nachvollziehbar archivieren
3. Konvertierung über die vorhandenen Skripte unter tools/ ausführen
4. Mesh-, UV-, Hitbox-, Boden- und Vorwärtsachsenverträge testen
5. mindestens Vorder-, Seiten-, Bewegungs- und Hitboxbild aus dem Spiel beilegen

Keine aktive Exact-Mesh-Ressource manuell durch ein vereinfachtes Würfelmodell ersetzen.

## Pull Requests

Jeder Pull Request beschreibt:

- was geändert wurde
- warum die Änderung nötig ist
- wie sie getestet wurde
- bekannte Einschränkungen
- Vorher-/Nachher-Bilder bei visuellen Änderungen

Änderungen an Registry-IDs, Netzwerkprotokollen, Weltformaten oder Lizenzen müssen ausdrücklich als potenziell inkompatibel markiert werden.
