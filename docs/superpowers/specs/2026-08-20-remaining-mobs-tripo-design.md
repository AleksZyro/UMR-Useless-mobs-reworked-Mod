# Tripo-Neuaufbau der übrigen Mobs

## Ziel

Die acht noch nicht zufriedenstellenden Mobs werden einzeln über denselben qualitätsgesicherten Tripo-, Blockbench- und GeckoLib-Ablauf neu aufgebaut, der beim Corrupted Silverfish als Referenz dient. Das Ergebnis soll die Form und Textur des freigegebenen Tripo-Modells im Spiel möglichst originalgetreu erhalten und zugleich natürliche, geschlossene Animationen ermöglichen.

In diesem Vorhaben enthalten sind:

1. Living Boss
2. Frost Stray
3. Web Cave Spider
4. Coral Drowned
5. Octopus
6. Witch Boss
7. Living Bat
8. Rooted Husk

Ausdrücklich ausgeschlossen sind sämtliche Slimes, Corrupted Silverfish, Endermite sowie alle Rüstungen und Kronen.

## Vorgehen

Der Living Boss ist der Pilot. Erst nachdem sein vollständiger Ablauf visuell und technisch bestätigt wurde, wird dieselbe Pipeline nacheinander auf die sieben weiteren Mobs angewendet. So werden fehlerhafte Annahmen nicht achtfach vervielfacht und Tripo-Credits nicht unnötig verbraucht.

Für jeden Mob gelten dieselben Phasen:

1. Bestehende Identität, Farbpalette, Textur, Fähigkeiten und Silhouette im Projekt erfassen.
2. Eine eindeutige Mehransichten-Vorlage mit Front, Seite, Rückseite und Draufsicht herstellen. Alle Ansichten zeigen dasselbe Wesen, dieselben Proportionen und dieselben Details vor neutralem Hintergrund.
3. Die Vorlage in Tripo als texturiertes 3D-Modell erzeugen. Varianten werden nicht blind weiterverarbeitet; zuerst wird ein klar bestes Ergebnis ausgewählt.
4. Das freigegebene Tripo-Modell als unveränderliche GLB-Quelle sichern. Hash, Polygonzahl, Bounds, UVs, Texturen und vier Referenzansichten werden dokumentiert.
5. Das Modell in Blockbench übernehmen. Zusammenhängende sichtbare Körperflächen bleiben zusammen. Nur anatomisch getrennte Teile oder echte Gelenke werden getrennten Bones zugeordnet.
6. GeckoLib-Rig und Animationen passend zur Kreatur erstellen. Jede Animation besitzt einen geschlossenen, glatten Loop; Beine, Flügel, Arme oder Tentakel bewegen sich sichtbar und natürlich.
7. Modell, Textur, Glowmaske falls nötig, Geometry und Animationen deterministisch exportieren und validieren.
8. Einen isolierten Preview-Pack oder gleichwertigen sicheren Kandidaten bauen. Bestehende Produktionsdateien bleiben bis zur visuellen Freigabe erhalten.
9. Im echten Forge-Client prüfen: Ausrichtung, Hitbox, Bewegung, Textur, Silhouette, Schnitte, Clipping, fehlende Flächen und Performance.

## Modell- und Rig-Regeln

- Die Tripo-Dreiecksgeometrie ist die visuelle Referenz. Eine automatische Quaderrekonstruktion ist nicht die Standardlösung, weil sie die Silhouette deutlich verändern kann.
- Zusammenhängende Torso-, Kopf- oder Schwanzflächen dürfen nicht nach Position oder Dreiecksmitte auf mehrere starre Bones verteilt werden. Das würde beim Animieren sichtbare Schnitte erzeugen.
- Bewegliche Gliedmassen werden möglichst bereits als getrennte Komponenten erzeugt oder nur an echten Gelenken getrennt.
- Wenn die Laufzeit keine sichere Verformung eines verbundenen Meshes unterstützt, bleibt der verbundene Hauptkörper auf einem Body-Bone; nur klar getrennte Anhänge werden animiert.
- Die Vorwärtsrichtung wird im echten Client anhand der Bewegung geprüft. Eine falsche Richtung wird einmal auf Renderer-Ebene korrigiert, nicht durch Umformen des Meshes oder der UVs.
- Hitboxen werden an die spielrelevante Hauptmasse angepasst, nicht an einzelne lange Dekorationen.

## Visuelle Freigabepunkte

Vor jeder Integration werden folgende Bilder gezeigt:

- Front
- rechte Seite
- Rückseite
- Draufsicht
- Perspektive
- Idle
- charakteristische Bewegungsphase

Die Tripo-Quelle und der Blockbench-/GeckoLib-Kandidat werden mit gleicher Kamera und möglichst gleicher Skalierung nebeneinander gezeigt. Ohne sichtbare Übereinstimmung wird nicht integriert.

## Qualitätsgrenzen

Ein Mob gilt erst als fertig, wenn alle folgenden Nachweise vorliegen:

- Silhouette, Hauptfarben und wichtige Details entsprechen dem freigegebenen Tripo-Modell.
- Textur, UVs und Face-Zuordnung sind vollständig; keine fehlenden oder transparenten Flächen.
- Keine unzulässigen Risse, Schnitte, schwebenden Teile oder z-fighting-Flächen.
- Animationen sind geschlossen, ausreichend stark sichtbar und ohne grosse Sprünge zwischen benachbarten Keyframes.
- Der Kopf zeigt in Bewegungsrichtung; Füsse oder Tentakel gleiten nicht auffällig.
- Hitbox und sichtbarer Körper stimmen im Spiel sinnvoll überein.
- Forge-Kompilierung und die mob-spezifischen Strukturtests bestehen.
- Der echte Client lädt den Mob; mindestens Idle und eine Bewegung werden visuell geprüft.
- Vorherige Produktionsassets bleiben bis zur ausdrücklichen visuellen Freigabe erhalten.

## Fehlerbehandlung und Kostenschutz

- Pro Mob wird nur eine kleine Zahl klar begründeter Tripo-Varianten erzeugt.
- Ein offensichtlich falsches Ergebnis wird vor Retopologie, Rigging oder Export verworfen.
- Downloads und Exporte werden lokal unverändert archiviert; generierte Dateien überschreiben keine freigegebenen Quellen.
- Fehlende UVs, beschädigte Texturen, leere Exporte oder unpassende Topologie stoppen die Pipeline vor dem Rigging.
- Ein erfolgreicher Export allein ist keine Freigabe. Entscheidend sind Vergleichsbilder und der echte Client.

## Umgang mit dem abgelehnten Renderer-Workaround

Die noch nicht committeden Änderungen, welche die Vanilla-Basis nur transparent schalten, sind keine Modellneuerstellung und gehören nicht zum bestätigten Ansatz. Sie werden vor der Implementierung gezielt entfernt, ohne fremde oder bereits bestehende Arbeitsdateien anzutasten.

## Reihenfolge

1. Living Boss als vollständiger Pilot
2. Frost Stray
3. Web Cave Spider
4. Coral Drowned
5. Octopus
6. Witch Boss
7. Living Bat
8. Rooted Husk

Nach dem Living Boss wird geprüft, ob die Pipeline ohne Änderungen wiederverwendbar ist. Technische Anpassungen werden dokumentiert, bevor der nächste Mob beginnt.
