# UMR – Produktionsplan ab Frost Stray v2

Dieser Plan trennt implementierte Technik, tatsächliche Spielabnahme und die
noch fehlende gewichtete Assetquelle. Er ersetzt keine Runtime-Nachweise durch
statische Tests.

## Lieferung 1 – Frost-Stray-v2-Technik stabilisieren

- [x] Serverseitige Ability-Timeline mit Instanz-UUID, Starttick, Release,
  Cancel und bestätigtem Kollisionsimpact.
- [x] Produktive, Forge-unabhängige Client-Historie für doppelte und verspätete
  Timeline-/Impact-Nachrichten mit direktem Java-Regressionstest.
- [x] V2-Vorschau parallel zur bestehenden Frost-Stray-Entität, reproduzierbare
  Overlay-Assets, Style-Brief und Exportmanifest.
- [ ] Forge-Build und Dedicated-Server-Smoke dieses Arbeitsstands in CI
  abschliessen; ein fehlender `Done (`-Marker muss weiterhin fehlschlagen.

## Lieferung 2 – echte Frost-Stray-v2-Abnahme

Voraussetzung ist eine ausführbare Forge-Clientumgebung. Die Capture-Matrix
nutzt dieselbe Arena, Kamera und Einstellungen für 4, 12 und 24 Blöcke sowie
Idle, Walk, Run, Charge, Release, Treffer, Fehlschlag, Hurt, Abbruch und Tod.
Zwei echte Clients prüfen ausserdem spätes Tracking, Duplikate,
Dimensions-/Sitzungswechsel und die serverseitige Einmaligkeit von Schaden und
Impact. Framezeit und Partikelzahl werden erst nach diesen echten Messungen
verglichen.

## Lieferung 3 – gewichtete Frost-Stray-Quelle

Die vorhandene Frost-Stray-Basis ist eine verbundene, ungewichtete Tripo-Fläche.
Die zwölf v2-Akzente ergänzen sie nahtsicher, sind aber kein Ersatz für ein
gewichtetes Körperrig. Sobald Andrin eine versionierte gewichtete Quelle
bereitstellt, folgen UV-/Pivot-Rückvergleich, Hand-/Bogengriff-Posen und echte
Körperclips. Bis dahin werden keine starren Flächensplits am verbundenen Körper
eingeführt.

## Lieferung 4 – zweiter Körpertyp

Erst nach der v2-Spielabnahme validiert der Web Cave Spider die Pipeline mit
acht Beinen und flacher Bodenlage. Die wiederverwendbaren Verträge sind
Zeitbasis, Ereignisidentität, Asset-Manifest, Sockets, VFX-Budget und
Capture-Manifest; die Achtbein-Deformation bleibt ein spezieller Adapter.

## Priorisierung danach

| Reihenfolge | Mob | Nächstes enges Paket | Done-Bedingung |
| --- | --- | --- | --- |
| 1 | Web Cave Spider | Achtbein-Manifest, Bewegungs-/Angriffscaptures und VFX-Anker | echter Clientvergleich und keine Gelenklücken |
| 2 | Octopus | Tentakel-Sockets, Wassertrail und LOD-Prüfung | Capture in Wasser und Budgetmessung |
| 3 | Coral Drowned | Nah-/Fernkampf-Übergang und Materiallesbarkeit | zwei Clientzustände und Vergleichsaufnahme |

## Reproduzierbarer Prüfweg

```powershell
python tools/frost_stray_v2/build_assets.py --check
python -m pytest -q tools/tests/test_frost_stray_ability_timeline.py tools/tests/test_frost_stray_ability_behavior.py
```

Die zusätzlichen produktiven Java-Regressionen werden in der CI mit Java 17
kompiliert und ausgeführt. Lokal bleibt der Forge-Start separat offen, solange
die dokumentierte Gradle-Loopback-Sperre besteht.
