# Frost Stray v2 – reproduzierbare Sichtprüfung

## Arena

Flache, schneebedeckte Fläche bei Tageszeit, Kamera auf Y 66, Sichtweite 16,
FOV 70 und Partikel auf «Alle». Ein v1-Frost-Stray und ein v2-Frost-Stray stehen
nebeneinander; v2 wird mit `/summon usless_mobs:frost_stray_v2 ~ ~ ~` erzeugt.
Für die Angriffsbilder erhält der Mob ein Ziel in 6–25 Blöcken Entfernung.

## Capture-Matrix

| Zustand | 4 Blöcke | 12 Blöcke | 24 Blöcke | Muss sichtbar sein |
| --- | --- | --- | --- | --- |
| Idle | ja | ja | ja | Krone, Gesicht, Materialtrennung |
| Walk/Run | ja | ja | ja | geschlossene Basis, Gegenbewegung der Akzente |
| Volley vorbereiten | ja | ja | ja | Partikel zum Bogengriff, einmaliger Charge-Sound |
| Freigabe/Flug/Impact | ja | ja | ja | Mündungsblitz, Einschlag an echter Kollisionsposition |
| Hurt/Abbruch/Death | ja | ja | nein | keine weiterlaufende Charge und kein nachträglicher Start |

## Noch offene reale Nachweise

Screenshots, Bildfolge/Video, zwei echte Clients, Framezeiten und Gruppenlast
sind erst nach einem erfolgreichen Forge-Clientlauf als bestanden zu markieren.
Diese Datei ist ein Testszenario, kein behaupteter Capture-Nachweis.
