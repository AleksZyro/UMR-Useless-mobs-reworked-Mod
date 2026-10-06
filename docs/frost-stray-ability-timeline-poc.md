# Frost-Stray-Ability-Timeline – PoC

## Tatsächlicher Ausgangszustand

- Minecraft **1.20.1**, Forge **47.4.16**, GeckoLib **4.8.3**.
- Runtime-Mesh: sechs Regionen, **98'103 Dreiecke**.
- Quell-Albedo: **4096 × 4096**; ausgelieferte Runtime-Textur:
  **2048 × 2048**. Die frühere Dokumentation, die für Frost Stray eine 4K-
  Runtime-Textur behauptete, ist damit korrigiert.
- Renderer: `FrostStrayRenderer` + `ExactMobMeshLayer`; die bestehende Salve
  hatte 18 Ticks Warmup und erzeugte Partikel/Sound direkt aus der Server-KI.

## PoC-Vertrag

Die Eis-Salve besitzt nun eine UUID als Instanz-ID, die UUID des Frost Strays,
den Server-Starttick und eine feste Dauer von 18 Ticks. Der Server entscheidet
allein über Start, Abbruch, Projektilerzeugung und Schaden.

`START` wird an beobachtende Spieler gesendet. Ein Spieler, der später mit dem
Tracking beginnt, erhält einen Snapshot derselben Instanz und steigt anhand
von `serverGameTime - startGameTime` in die laufende Emission ein. Der
Client spielt dabei keinen verspäteten Startsound ab. `RELEASE` und `CANCEL`
beenden die Emission. Dieser erste PoC verwendet bewusst ausschliesslich
zeitnahe Start-/Release-One-Shots und keinen Loop-Sound; deshalb kann kein
Audio-Loop hängen bleiben. Wiederholte oder verspätete Startpakete für eine
abgeschlossene UUID werden verworfen.

Der Projektil-Impact wird ausschliesslich aus `HitResult#getLocation()` auf dem
Server erzeugt und erhält pro Kollision eine eigene Impact-UUID. Ein
Animationszeitpunkt behauptet keinen Treffer.

`FrostStrayTimelineClock` ist die gemeinsame Client-Zeitbasis für VFX und
Bogenpose. Sie übernimmt nur streng neuere Serverticks. Ein doppeltes START-
Paket bei Servertick 100 kann deshalb nach bereits verstrichenem Tick 106 den
Offset nicht mehr auf 100 zurücksetzen. Bei verspätetem Erstpaket wird der
Starttick aus dem Paket verwendet; alte Cues werden nicht nachgespielt,
laufende Emissionen steigen beim aktuellen Fortschritt ein. Bei neuer
Dimension oder neuer Verbindung wird die Client-Uhr samt aktiven Cues geleert.

## VFX-Budget

`assets/usless_mobs/vfx_profiles/frost_stray_volley.json` definiert Charge,
Release und Impact. Innerhalb von 12 Blöcken gelten die Near-Werte, von 12 bis
24 Blöcken die Mid-Werte. Wichtige Telegraphen bleiben dort mit mindestens
einem Partikel lesbar; einmalige wichtige Impacts bleiben bis 32 Blöcke mit
einem Partikel sichtbar.

## Frost Stray v2 Preview

`/summon usless_mobs:frost_stray_v2 ~ ~ ~` wählt die parallele Vorschau, ohne
`frost_stray` oder bestehende Welten zu ersetzen. Die Vorschau bewahrt den
zusammenhängenden, ungewichteten sechs-Regionen-Körper und ergänzt zwölf
seam-sichere Eis-Overlay-Sockets für Krone, Gesicht, Schulter, Unterarm,
Wirbelsäule und Hüfte. Die editierbare Quelle liegt in
`Modelle/Exports/frost_stray_v2/frost_stray_v2_rig.json`; die Runtime-Textur ist
bewusst ein separates 64×64-Overlay. Der reproduzierbare Export lautet:

```powershell
python tools/frost_stray_v2/build_assets.py --check
```

Ein vollständig gewichtetes 10–14-Bone-Ersatzrig des verbundenen Körpers ist
noch offen, weil dafür eine gewichtete Quelle fehlt und die vorhandene
Rigging-Prüfung starre Splits mit sichtbaren Gelenkspalten ablehnen muss.

## Nachweisstatus

### Ausführbar geprüft

- Die bisherigen Python-Verträge prüfen überwiegend Quelltext- und Asset-
  Muster. Sie beweisen nicht, dass Minecraft die Java-Klassen ausführt.
- Ergänzend prüft `tools/tests/test_frost_stray_ability_behavior.py` ein
  ausführbares, deterministisches Protokollmodell für doppelte Starts und
  Releases, spätes Tracking, Abbruch mit verspätetem Start sowie doppelte
  Impacts. Die fokussierte Gruppe umfasst aktuell **18 bestanden**. Das ist
  ein Verhaltenstest des Netzwerkvertrags, aber noch kein Forge-Laufzeittest.
- `tools/java_tests/FrostStrayTimelineClockTest.java` kompiliert und prüft die
  produktive Java-Zeitkomponente direkt ohne Forge. Der Test deckt den
  ursprünglichen Rücksprung bei doppeltem START, verspätete Erstpakete und
  ältere Nachrichten ab; er lief lokal mit `FrostStrayTimelineClockTest PASS`
  und wird zusätzlich in der CI vor dem Forge-Build ausgeführt.
- Die v2-Preview-Assets werden im CI reproduzierbar erzeugt und auf zwölf
  eindeutige Sockets, 64×64 Runtime-Auflösung und SHA-256 geprüft.
- Die gemeinsamen Network-Packet-Klassen enthalten keine Client-Imports mehr.
  Sie übergeben Nachrichten über `FrostStrayAbilityPacketEvent` bzw.
  `FrostStrayImpactPacketEvent` an den clientseitigen Cue-Player. Der
  Projektilpfad verwirft wiederholte Kollisionsaufrufe mit einer
  `collisionHandled`-Sperre.
- `.github/workflows/build.yml` führt in der CI einen echten Forge-Build und
  danach einen Dedicated-Server-Startup-Smoke-Test aus. Der Server muss
  `Done (` loggen; der kontrollierte Timeout danach ist erwartbar. Der
  Nachweis ist nach dem nächsten CI-Lauf zu protokollieren.

### Noch offen, weil lokal nicht ausführbar

Die lokale Java-17-JDK ist vorhanden, Gradle 8.14.5 scheitert aber vor der
Projektkonfiguration an einer gesperrten lokalen Loopback-Verbindung:
`java.io.IOException: Unable to establish loopback connection`, verursacht
durch `java.net.SocketException: Invalid argument: connect` in Gradle's
`PipeImpl`/Unix-Domain-Socket-IPC. Das wurde sowohl mit dem installierten
Android-JDK 17 als auch mit dem vorhandenen 64-Bit-JDK 25 reproduziert. Der
normale Aufruf ohne gesetztes `JAVA_HOME` verwendet zusätzlich das 32-Bit-Java
8 und scheitert bereits vorher mit `Could not reserve enough space for
3145728KB object heap`. Der direkte, unabhängige Java-Test der Clock ist davon
nicht betroffen und bestand; ein lokaler Forge-Kompilationsnachweis existiert
weiterhin nicht.

Noch nicht ausgeführt und daher ausdrücklich offen sind: CI-Ergebnis des
Forge-Builds, Dedicated-Server-Start, zwei echte Clients für Treffer,
Fehlschlag, Unterbrechung, Tod, spätes Tracking sowie verspätete und doppelte
Timeline-Nachrichten, reale Ingame-Aufnahmen bei 4, 12 und 24 Blöcken, die
Timing-Bildfolge sowie der Vergleich von Lesbarkeit, Partikelzahl und
Framezeit mit dem Ausgangszustand. Diese Punkte dürfen erst nach einem
reproduzierbaren Spieltest als abgenommen markiert werden.

Die globalen Python-Checks sind ein bestehendes Repository-Problem: `pytest -q`
ohne Modulaufruf scheitert an fehlenden `tools`-Imports; `python -m pytest -q`
benötigt die in `requirements-ci.txt` aufgeführten externen Pakete
`meshoptimizer` und `nbtlib`. `ruff check src tests` kann nicht ausgeführt
werden, weil dieses Repository kein `tests`-Verzeichnis besitzt; `ruff check
src tools` meldet bestehende Lintfehler ausserhalb dieses PoC. Die fokussierten
PoC-Tests und der neue Verhaltenstest sind davon getrennt zu berichten.
