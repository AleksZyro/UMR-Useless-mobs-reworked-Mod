# Corrupted Silverfish Hitbox and Boss Combat Design

## Ziel

Der aktive Corrupted Silverfish erhält eine zum Tripo-Laufzeitmodell passende rechteckige Trefferfläche, sicheren Umgang mit engen Löchern sowie eine eigene akustische und visuelle Identität. Der Living Boss und die Witch Boss werden auf den bereits vorhandenen Angriffen aufgebaut und zu vollständigen, schwierigkeitsabhängigen Bosskämpfen erweitert.

## Verbindliche Laufzeitgrundlage

- Aktives Silverfish-Modell: `assets/usless_mobs/meshes/entity/corrupted_silverfish.mesh`.
- Gemessene sichtbare Ausdehnung: etwa `1.10 × 0.92 × 2.00` Blöcke (Breite × Höhe × Länge).
- Die aktuelle Registrierung `sized(2.0F, 0.92F)` erzeugt eine unnötige quadratische `2 × 2` Grundfläche und wird ersetzt.
- Das Modell, seine Textur und Animationen werden nicht neu interpretiert oder ersetzt.

## Corrupted Silverfish

### Mehrteilige Trefferfläche

Die Basiseinheit verwendet eine navigierbare Kernfläche von ungefähr `1.10 × 0.92`. Zwei mit der Blickrichtung mitrotierende Treffersegmente decken Vorder- und Hinterkörper ab, sodass die gesamte verwundbare Länge etwa zwei Blöcke beträgt. Treffer auf Kern oder Segment werden genau einmal an die Haupteinheit weitergereicht; ein Flächenschlag darf nicht mehrfach zählen.

### Loch- und Feststeckschutz

Vor einem Navigationsschritt wird geprüft, ob der längliche Körper in den nächsten Raum passt. Ein ein Block breiter Durchgang, in den nur der Kern, aber nicht der ganze Körper passt, wird nicht als sicherer Weg akzeptiert. Bleibt der Silverfish trotzdem längere Zeit nahezu unbewegt und kollidiert, führt er eine kurze Corruption-Flucht zur letzten sicheren Position aus. Die Flucht verursacht keinen Schaden und verhindert kein legitimes Töten durch Spieler.

### Identität

Ambient-, Verletzungs-, Angriffs-, Flucht- und Todessound erhalten eigene registrierte Sound-IDs. Bis echte Audiodateien vorhanden sind, werden diese IDs über sorgfältig geschichtete vorhandene Minecraft-Sounds bedient; dadurch ist die Registry stabil und die Dateien können später ohne Java-Änderung ausgetauscht werden. Magenta Corruption-Partikel markieren Angriff und Flucht deutlich, ohne die Sicht zu verdecken.

## Living Boss

Bestehende Fähigkeiten bleiben erhalten: Wurzelkäfig, Heilpuls, Dornenkonter, Spinnen und Wurzelgeister. Ergänzt werden eine klar angekündigte Wurzelwelle, ein Bodenbruch mit sicheren Ausweichkorridoren und eine zweite Kampfphase unter 50 Prozent Leben. Easy, Normal und Hard verändern nicht nur Zahlen, sondern auch Musteranzahl, Beschwörungsgrenze und Erholungsfenster. Belohnungen werden je Schwierigkeitsgrad eindeutig abgestuft.

## Witch Boss

Bestehende Flüche, Giftwolken, Heiltrank, Geister und der Hasen-Köder bleiben bestehen. Die neue Jagdphase verwandelt den getroffenen Spieler vollständig:

- sichtbares Hasenmodell statt Spielermodell;
- kleine, zum Hasen passende Hitbox;
- keine Waffen- oder Itembenutzung;
- nur Rennen und Springen als Verteidigung;
- beschworene Jagdhunde verfolgen ausschliesslich verwandelte Spieler;
- ein sichtbarer Timer und Partikel zeigen Beginn und Ende;
- Tod, Dimensionswechsel, Logout und Bossende stellen Modell, Hitbox und Steuerung sicher wieder her.

Die technische Umsetzung verwendet einen synchronisierten Spielerzustand statt eines Entity-Austauschs. Inventar, Kamera, Fortschritt und Netzwerkidentität bleiben damit erhalten. Ein reiner Render-Trick wäre zu oberflächlich; ein echter Austausch des Spielers wäre unnötig riskant.

## Sichtbare Abnahme

1. Silverfish mit eingeschalteten Hitboxen von vorne, Seite und schräg.
2. Nachweis, dass Vorder- und Hintersegment Treffer annehmen, aber ein Schlag nur einmal zählt.
3. Silverfish vor einem zu engen Loch und bei ausgelöster Flucht.
4. Living Boss in beiden Phasen auf Easy, Normal und Hard.
5. Witch-Jagdphase mit sichtbarem Hasenspieler, kleiner Hitbox, blockierter Waffe, Jagdhunden und sauberer Rückverwandlung.

## Nicht-Ziele

- Keine erneute Tripo-Generierung des bereits freigegebenen Silverfish.
- Keine geratenen Grössen für andere Tripo-Mobs.
- Keine vollständige Neuschreibung der vorhandenen Boss-KI.
- Keine bezahlten externen Dienste für Sounds oder Effekte.
