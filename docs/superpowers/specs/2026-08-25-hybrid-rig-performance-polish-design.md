# UMR Hybrid-Rig- und Performance-Politur

Stand: 25. August 2026

## Ziel

Frost Stray, Web Cave Spider und Helping Allay sollen natürlich und eindeutig ausgerichtet animiert werden. Arme, Beine, Flügel, Kopf und gehaltene Gegenstände müssen unabhängig reagieren, ohne die zusammenhängenden Tripo-Oberflächen an Regionsgrenzen aufzureissen. Gleichzeitig wird die Laufzeitlast weiter reduziert. Ein neuer Pre-Release darf erst nach automatischen und echten Clienttests entstehen.

## Abgrenzung

In diesem Durchlauf werden ausschliesslich folgende Bereiche verändert:

- Frost-Stray-Rig, Bogenhaltung, Laufanimation und Hitboxprüfung
- Web-Cave-Spider-Vorwärtsachse, Achtbein-Gang und Hitboxprüfung
- Helping-Allay-Flügel-, Arm-, Kopf-, Kern- und Itemanimation
- gemeinsame Exact-Mesh-Animations-LOD- und Textur-Performance
- Tests, Dokumentation und Release-Verifikation dieser Änderungen

Andere Modelle, Gameplay-Angriffe, Weltgenerierung, Rüstungen und Kronen werden nicht neu gestaltet.

## Gewählter Ansatz: gewichtetes Hybrid-Rig

Die archivierten Tripo-Quellen, UVs und Materialien bleiben erhalten. Das Laufzeitsystem erhält logische Bones beziehungsweise Anker, deren Einfluss auf die zusammenhängende Oberfläche über weiche, positionsbasierte Gewichte berechnet wird. Gemeinsame Grenzflächen erhalten kompatible Übergangsgewichte, damit keine sichtbaren Schnitte entstehen.

Harte Rotation vollständig getrennter Meshregionen wird nicht verwendet, weil die Quellen keine klassische Skinning-Gewichtung besitzen und dieser Ansatz bereits zu offenen Nähten geführt hat. Eine reine Verstärkung der bestehenden Oberflächenverformung reicht ebenfalls nicht aus, weil damit gehaltene Gegenstände keinen verlässlichen Handanker erhalten.

## Frost Stray

Das logische Rig umfasst Kopf, Wirbelsäule, linke und rechte Oberarme, Unterarme, Hände, Oberschenkel und Unterschenkel. Die sichtbare Oberfläche wird durch weiche Zonen um Schulter, Ellbogen, Hüfte und Knie beeinflusst.

Im Laufzustand bewegen sich gegenüberliegende Arme und Beine gegenläufig. Im Zielzustand ersetzt eine zweihändige Bogenpose den normalen Armschwung:

- die Bogenhand besitzt einen festen Item-Anker;
- der zweite Arm greift sichtbar zur Bogen-/Sehnenposition;
- Kopf und Oberkörper richten sich auf das tatsächliche Ziel aus;
- Beine behalten eine stabile Stand- oder Laufbewegung;
- beim Wechsel zwischen Lauf und Zielen werden die Posen weich überblendet.

Das vorhandene Vanilla-`ItemInHandLayer` darf nicht länger von einem unsichtbaren Basisarm statt vom sichtbaren Exact-Mesh-Rig bestimmt werden. Ein dedizierter Frost-Stray-Item-Layer verwendet den neuen Handanker und dieselbe Pose wie die sichtbare Oberfläche.

## Web Cave Spider

Das Rig umfasst Körper, Kopf und für jedes der acht Beine eine Wurzel- und Spitzenzone. Die Modellfront wird mechanisch aus Kopf-/Augenposition und Körperlängsachse bestimmt und mit Minecrafts Bewegungsrichtung abgeglichen. Nur wenn die aktive Quelle tatsächlich entgegengesetzt ausgerichtet ist, wird eine einmalige 180-Grad-Korrektur im Renderer angewendet.

Vier diagonale Beinpaare erhalten versetzte Phasen. Die Beinspitzen bewegen sich deutlich, während der Einfluss zum Körper weich gegen null läuft. Der Körper bleibt tief, erhält eine kleine vertikale Federung und darf weder schweben noch rückwärts zur tatsächlichen Fortbewegung zeigen.

Die registrierte Hitbox wird aus den skalierten statischen Grenzen und den maximalen nahen Animationsauslenkungen validiert. Sie bleibt rechteckig und darf das sichtbare Modell weder abschneiden noch unnötig weit überragen.

## Helping Allay

Das Rig umfasst Körper, Kopf, beide Arme, einen Item-/Handanker, beide Flügelwurzeln, beide Flügelspitzen und den Seelenkern. Flügel und Arme werden unabhängig gewichtet.

Die Fluganimation verwendet gegenläufige Flügelwellen mit zusätzlicher Spitzenverzögerung. Kopf und Körper reagieren leicht auf Flugrichtung. Der Seelenkern pulsiert ohne die Körperoberfläche zu verschieben. Heilen, Schild, Enthüllen, Binden und Rückkehr erhalten klar unterscheidbare Arm-, Flügel- und Kernposen. Ein gehaltenes Item folgt dem dedizierten Handanker und nicht einem verdeckten Vanilla-Arm.

## Performance

Die Standardausgabe verwendet 2048 × 2048 grosse Laufzeittexturen. Die bestehenden 4096 × 4096-Quellen bleiben unverändert archiviert und werden als separates optionales High-Quality-Ressourcenpaket angeboten. UVs und Materialzuordnung müssen in beiden Stufen identisch bleiben.

Die Animation erhält drei Distanzstufen:

1. Nahbereich: vollständige gewichtete Oberflächenanimation in jedem Bild.
2. Mittelbereich: dieselbe sichtbare Geometrie, aber reduzierte Aktualisierungsrate der Deformationswerte und Wiederverwendung der letzten berechneten Pose.
3. Fernbereich: statisches Performance-Mesh ohne Vertexdeformation.

Die exakten Distanzgrenzen werden anhand eines echten Clientprofils gewählt, nicht geraten. Die bestehenden wiederverwendeten Vektor- und Normalenobjekte bleiben erhalten. Neue Deformationspfade dürfen weder pro Vertex noch pro Dreieck neue Java-Objekte anlegen.

Das bestehende Dreiecksbudget von höchstens 110'000 pro aktivem Exact-Mesh bleibt verbindlich. Die Texturreduktion und LOD-Änderung dürfen weder Meshgeometrie noch Bone-/Regionszuordnung verändern.

## Fehlerverhalten

Fehlt ein erforderlicher Bone, Handanker, Meshreport oder eine Texturstufe, schlägt der automatisierte Build mit einer konkreten Fehlermeldung fehl. Der Renderer fällt nicht still auf eine falsche Pose oder veraltete Textur zurück. Optionales 4K-Material darf fehlen, ohne den Standardclient zu blockieren; die 2K-Standardressource ist verpflichtend.

## Automatische Prüfung

Vor der Implementierung werden fehlschlagende Regressionstests für folgende Verträge ergänzt:

- vollständige logische Bone-/Ankerlisten pro betroffenem Mob;
- voneinander unabhängige Arm-, Bein- und Flügelgewichte;
- gemeinsamer nahtsicherer Einfluss an Übergängen;
- Frost-Stray-Bogenanker und zweihändige Zielpose;
- Spider-Kopf-/Augenrichtung stimmt mit der Laufvorwärtsachse überein;
- statische und maximal animierte Grenzen bleiben innerhalb der Hitbox;
- 2K-Standardtexturen und pixel-/UV-identische optionale 4K-Zuordnung;
- drei LOD-Stufen und keine neuen Objektallokationen in den inneren Renderpfaden;
- bestehendes Dreiecks- und Meshgrössenbudget.

Nach jedem kleinen Implementierungsschritt laufen die betroffenen Tests und die Java-Kompilierung. Vor Freigabe laufen die vollständige Python-Suite, Projektwahrheitsprüfung, Ressourcenvalidierung und der Forge-Produktionsbuild.

## Echter Clienttest

Der Clienttest verwendet einen frischen Forge-Start und eine kontrollierte Testwelt. Für jeden der drei Mobs werden Vorder-, Seiten- und Dreiviertelansicht sowie `F3+B` geprüft.

- Frost Stray: Idle, Laufen, Zielerfassung, Schuss und Posenübergänge.
- Web Cave Spider: gerader Vorwärtslauf, Kurve, Verfolgung und Angriff.
- Helping Allay: Idle-Flug, Bewegung, gehaltenes Item und alle unterstützten Aktionen.
- Belastung: mehrere Instanzen gleichzeitig im Nah-, Mittel- und Fernbereich.

Der Test gilt nur als bestanden, wenn keine falsche Vorwärtsrichtung, schwebenden Teile, offenen Schnitte, falschen Itempositionen oder abgeschnittenen Hitboxen sichtbar sind und `latest.log` keine UMR-Modell-, Textur-, Renderer-, Registry- oder GeckoLib-Ausnahme enthält. Der Client wird über den normalen Speichern-und-Beenden-Weg geschlossen.

## Release-Regel

Ein neuer Pre-Release wird nur erstellt, wenn:

- alle automatischen Tests grün sind;
- der Forge-Produktionsbuild erfolgreich ist;
- GitHub CI erfolgreich ist;
- der echte Clienttest vollständig bestanden ist;
- Standard- und optionales Qualitätsmaterial eindeutig dokumentiert sind;
- das veröffentlichte CI-JAR mechanisch auf Version, Abhängigkeiten, Einträge und SHA-256 geprüft wurde.

Eine absolute Fehlerfreiheit wird nicht behauptet. Der Release beschreibt stattdessen exakt die getestete Minecraft-, Forge-, GeckoLib- und TerraBlender-Konfiguration sowie die beobachteten Ergebnisse.
