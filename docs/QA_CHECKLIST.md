# UMR Release-QA

Diese Matrix trennt automatische Prüfungen von manueller Laufzeit- und Sichtprüfung.

## Automatisch vor jedem Pull Request

- [ ] python tools/verify_umr_project_truth.py
- [ ] python -m pytest -q
- [ ] ./gradlew clean build --no-daemon
- [ ] keine leeren oder syntaktisch ungültigen Laufzeitressourcen
- [ ] keine fehlenden Registry-, Renderer-, Textur- oder Meshverträge
- [ ] keine Zugangsdaten oder privaten Schlüssel

## Client

- [ ] neues Singleplayer-Spiel startet
- [ ] vorhandene Testwelt lädt
- [ ] Creative-Inventar und Spawn-Eier funktionieren
- [ ] UMR-Befehle funktionieren
- [ ] Speichern und Rückkehr ins Hauptmenü ohne Crash

## Dedicated Server

- [ ] Server startet ohne Client-only-Classloading-Fehler
- [ ] Client kann beitreten und verlassen
- [ ] Packet-Richtungen und serverseitige Wertevalidierung geprüft
- [ ] Chunk-Unload/-Reload und Save/Reload geprüft

## Mobs und Bosse

- [ ] Spawn-Bedingungen und Spawn-Eier
- [ ] Verfolgung, Navigation und Spezialangriffe
- [ ] Schwierigkeit Easy/Normal/Hard
- [ ] Drops und Progression
- [ ] Tod und Entfernung aller temporären Helfer
- [ ] Bossbar, Sounds und Partikeleffekte

## Modelle

- [ ] Vorderseite stimmt mit der Blickrichtung überein
- [ ] Textur und UVs stimmen mit der freigegebenen Quelle überein
- [ ] Skalierung und Hitbox passen
- [ ] Bodenmodelle schweben nicht und sinken nicht ein
- [ ] Lauf-, Schwimm-, Flug- und Angriffsanimationen bewegen die richtigen Teile
- [ ] keine offenen Schnitte, sichtbaren Regionswürfel oder Z-Fighting-Flächen
- [ ] mindestens Vorder-, Seiten-, Bewegungs- und F3+B-Nachweis gespeichert

## Kompatibilität

- [ ] GeckoLib 4.8.3
- [ ] ohne Curios
- [ ] mit Curios 5.10+
- [ ] ohne JEI
- [ ] mit JEI 15.20+
