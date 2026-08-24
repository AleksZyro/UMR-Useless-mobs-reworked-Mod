# All Armor Skin System Design

## Ziel

Alle spielbaren Rüstungen erhalten dieselbe sichtbare Materialtiefe wie der Corrupted Silverfish, ohne ihre bestehende Form, Slot-Zuordnung oder Spielmechanik zu verändern. Das System umfasst vier vollständige Sets sowie zwei Einzelteile:

- True Void: Helm, Brustplatte, Hose, Stiefel
- True Celestial: Helm, Brustplatte, Hose, Stiefel
- True Living: Helm, Brustplatte, Hose, Stiefel
- Armor of Balance: Helm, Brustplatte, Hose, Stiefel
- Corrupted Crystal: Hose
- Schleimreaktor: Brustplatte

Die freigegebene visuelle Referenz liegt unter `Modelle/Exports/armor_graphics_concepts/all_armor_material_reference_v1.png`.

## Unveränderliche Grenzen

- Keine Änderung an Rüstungswerten, Effekten, Rezepten, Registrierungen oder Entity-Code.
- Keine Änderung an bestehenden Modellformen oder Bone-Zuordnungen.
- Die bestehende True-Void-Crystal-Knight-Geometrie bleibt unverändert.
- Alle existierenden Item-Model-JSONs behalten ihre Eltern, Elemente, Displays und Texturbindungen.
- Änderungen beschränken sich auf Rüstungstexturen, Itemtexturen, den deterministischen Generator, fokussierte Tests und Reviewbilder.

## Gemeinsame Pixelsprache

Jede grosse sichtbare Fläche verwendet eine strukturierte Palette aus mindestens fünf Rollen:

1. tiefer Fugenschatten,
2. dunkles Grundmaterial,
3. mittlerer Materialton,
4. gerichtetes Kantenlicht,
5. heller Akzent oder Kern.

Zusätzlich gelten folgende Regeln:

- Keine zusammenhängende sichtbare Grundfarbenfläche darf wie ein ungegliederter Farbblock wirken.
- Platten erhalten mindestens eine dunkle Fuge und eine gerichtete helle Kante.
- Details werden als 1–3 Pixel grosse Cluster gesetzt, nicht als zufälliges Vollbildrauschen.
- Vorder-, Seiten-, Ober- und Unterflächen erhalten unterschiedliche Helligkeiten.
- Symmetrische Teile bleiben geometrisch symmetrisch; Abnutzung und organische Cluster dürfen kontrolliert asymmetrisch sein.
- Kristalle besitzen mindestens Schatten, Körper, Glow und Kernlicht.
- Icons bleiben bei 16×16 lesbar und verwenden eine klare Silhouette, höchstens 15 sichtbare Farbtöne und transparente Aussenbereiche.

## Familienidentität

### True Void

Obsidian-schwarzes Metall mit violett-grauen Platten, kalten Amethystkanten, feinen Void-Runen und einem hellen violetten Kristallkern. Die verschachtelten V-Platten bleiben das Hauptmotiv. Schwarze Bereiche erhalten subtile violette Materialcluster statt gleichfarbiger Flächen.

### True Celestial

Elfenbein-/Silberplatten mit warmem Goldrand, cyanfarbenen Energiespalten und kleinen sternförmigen Lichtpunkten. Das Material wirkt sauberer als Void, aber nicht flach: Silber erhält kühle Schatten, Gold mindestens drei Stufen.

### True Living

Dunkles Holz, Moosgrün, Rindenfugen und wenige hellgrüne Lebensadern. Organische Cluster sind unregelmässig, die Plattenstruktur bleibt dennoch als Rüstung lesbar. Der Kern ist limetten-/bernsteinfarben.

### Armor of Balance

Präzise geteilte Void- und Celestial-Materialien mit spiegelbildlichen Motiven. Keine zufällige Vermischung: dunkle und helle Hälfte bleiben klar, werden aber durch gemeinsame Gold-, Violett- und Cyanlinien verbunden.

### Corrupted Crystal

Silbergraue Chitinplatten, tiefe violette Schatten, magentafarbene Korruptionsadern und kleine blaue Fokussteine. Die Hose übernimmt die Materiallogik des Corrupted Silverfish, ohne dessen Textur direkt zu kopieren.

### Schleimreaktor

Dunkles technisches Metall mit eingelassenen smaragd-/cyanfarbenen Schleimkanälen, Segmentfugen, kleinen Bolzen und einem hellen Reaktorkern. Grün bleibt auf Kanäle und Kern begrenzt; das Metall dominiert die tragende Form.

## Technische Umsetzung

Ein neuer Generator `tools/armor_graphics/build_all_armor_skins.py` erzeugt alle betroffenen PNGs deterministisch. Gemeinsame Hilfen zeichnen Platten, Fugen, Kanten, Cluster, Kristalle und transparente Item-Silhouetten. Jede Familie definiert eine unveränderliche Palette und ein eigenes Motivrezept.

Der Generator schreibt atomar: zuerst in eindeutige temporäre Dateien im jeweiligen Zielordner, danach mittels `os.replace`. Fehler hinterlassen bestehende Produktionsdateien unverändert und räumen temporäre Dateien auf.

## Testvertrag

Fokussierte Tests prüfen:

- exakt erwartete Ausgabewege und PNG-Dimensionen,
- RGBA und gültige Alpha-Silhouetten,
- deterministische Bytes bei zwei Builds,
- begrenzte Palette und vorhandene fünf Materialrollen,
- keine zu grossen gleichfarbigen sichtbaren Regionen,
- gerichtete Kanten-/Fugenkontraste,
- familienrichtige Kern- und Akzentfarben,
- korrekte Item-Model-Bindungen,
- unveränderte Java-Geometrie und Slot-Routing,
- atomisches Schreiben und Cleanup bei simulierten Fehlern.

## Visuelle Abnahme

Für jede Familie wird ein Kontaktblatt mit Item-Icons sowie Front-/Rückenansicht der getragenen Texturen erzeugt. Zuerst wird Void als Referenzfamilie abgeschlossen. Danach folgen Celestial, Living, Balance, Corrupted Crystal und Schleimreaktor. Eine Familie gilt erst als fertig, wenn sie im Reviewbild erkennbare Materialtiefe zeigt und keine fehlenden, abgeschnittenen oder einfarbig wirkenden Flächen besitzt.

