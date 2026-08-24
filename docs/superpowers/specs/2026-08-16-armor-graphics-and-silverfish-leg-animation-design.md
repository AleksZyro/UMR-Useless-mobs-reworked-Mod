# Rüstungsgrafiken und stärkere Silverfish-Beinanimation

## Ziel

Alle sichtbaren Rüstungsgrafiken der Sets Void, Celestial, Living und Balance sowie der Corrupted-Crystal-Hose werden zuverlässig dargestellt. Das gilt sowohl für die Inventaransicht als auch für die angezogene Rüstung. Gleichzeitig erhält der Corrupted Silverfish eine deutlichere, aber weiterhin natürliche Beinbewegung.

## Gewählter Ansatz

Die bestehenden gemeinsamen 3D-Rüstungsvorlagen werden repariert und weiterverwendet. Dadurch bleiben die individuellen dreidimensionalen Formen erhalten, während Korrekturen an Helm, Brustplatte, Hose und Stiefeln automatisch allen davon abgeleiteten Sets zugutekommen. Einzelne Set-Dateien dürfen nur dort angepasst werden, wo ihre Textur- oder Darstellungsanforderungen tatsächlich abweichen.

## Umfang

- Inventarmodelle für Helm, Brustplatte, Hose und Stiefel prüfen und reparieren.
- Angelegte Modelle für Void, Celestial, Living und Balance prüfen und reparieren.
- Corrupted-Crystal-Hose als eigenes Rüstungsteil prüfen und reparieren.
- Modell-Eltern, Texturreferenzen, UV-Bereiche, Anzeige-Transformationen und sichtbare Körperteile mechanisch validieren.
- Keine Gegenstandswerte, Rezepte oder Spielmechaniken verändern.
- Bestehende, nicht zugehörige Modell- und Exportdateien nicht verändern.

## Beinanimation

Die bestehende alternierende Laufbewegung des Corrupted Silverfish wird gegenüber dem aktuellen Stand um ungefähr 60 Prozent verstärkt. Linke und rechte Beine bleiben in gegensätzlichen Phasen. Die Bewegung darf weder durch den Körper schneiden noch sichtbar unter den Boden geraten. Idle-, Angriffs-, Treffer- und Todesanimation bleiben unverändert, sofern eine notwendige technische Korrektur nicht direkt aus der Laufänderung folgt.

## Prüfung

- Zuerst automatisierte Vertragstests für Modellreferenzen, Texturen, UV-Grenzen und die verstärkte Animationsamplitude ergänzen.
- Danach die gemeinsamen Vorlagen und erforderlichen Set-Dateien reparieren.
- Java- und Asset-Build ausführen.
- Alle betroffenen Rüstungsteile im echten Forge-Client im Inventar und angezogen prüfen.
- Den Corrupted Silverfish beim Laufen aus mehreren Blickwinkeln prüfen.
- Einen kompakten Vorher-/Nachher-Vergleich im lokalen Browser zeigen.

## Erfolgskriterien

- Keine fehlenden Texturen oder violett-schwarzen Fehlerflächen.
- Keine verschwundenen, verdrehten, übergrossen oder falsch positionierten Rüstungsteile.
- Brustplatte und Hose sind im Inventar sowie am Spieler klar lesbar.
- Alle abgeleiteten Sets behalten ihre eigene Farb- und Texturidentität.
- Die Beinbewegung ist sichtbar kräftiger, aber bleibt natürlich und frei von Körper- oder Bodenüberschneidungen.
- Automatisierte Tests, Build und visuelle Client-Prüfung bestehen.
