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

## VFX-Budget

`assets/usless_mobs/vfx_profiles/frost_stray_volley.json` definiert Charge,
Release und Impact. Innerhalb von 12 Blöcken gelten die Near-Werte, von 12 bis
24 Blöcken die Mid-Werte. Wichtige Telegraphen bleiben dort mit mindestens
einem Partikel lesbar; einmalige wichtige Impacts bleiben bis 32 Blöcke mit
einem Partikel sichtbar.

## Nachweisstatus

Die fokussierten Python-Verträge bestehen. Eine lokale Java-17-JDK ist zwar
vorhanden, aber Gradle scheitert vor der Projektkonfiguration an einer
gesperrten lokalen Loopback-Verbindung. Deshalb sind Java-Kompilation,
Dedicated-Server-Start sowie echte Client-Screenshots bei 4, 12 und 24 Blöcken
ausdrücklich noch nicht nachgewiesen.
