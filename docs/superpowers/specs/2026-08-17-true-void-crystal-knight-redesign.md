# True-Void-Kristallritter – Redesign

## Freigegebene Vorlage

Die True-Void-Brustplatte wird nach dem vom Bildgenerator erzeugten Vierseitenkonzept `Modelle/Exports/armor_graphics_concepts/true_void_chestplate_crystal_knight_v1.png` überarbeitet. Die Vorlage bestimmt Silhouette, Plattenrichtung und Farbgewichtung. Sie wird nicht direkt als Textur verwendet, sondern kontrolliert in Minecraft-Cuboids und einen 128 × 64-Atlas übersetzt.

## Zielbild

Die angezogene Brustplatte wirkt wie eine elegante Void-Kristallritter-Rüstung:

- obsidianschwarze, körpernahe Grundrüstung;
- deutlich lesbare V-förmige Brust- und Bauchplatten;
- ein einzelner heller, rautenförmiger Brustkristall als Hauptblickfang;
- kleine violette Energienähte statt flächiger horizontaler Streifen;
- kompakte, nach hinten auslaufende Schulterplatten mit je einer kleinen Kristallspitze;
- eine gegliederte Rückenschale mit kleinerem Rückenkristall;
- schlanke Taille ohne frei schwebende oder übergrosse Bauteile.

## Geometrische Übersetzung

Die Vanilla-Humanoid-Grundhülle bleibt erhalten. Die bisherigen breiten horizontalen Detailblöcke werden durch gerichtete Plattengruppen ersetzt:

- zwei obere Brustplatten am `body`, die zur Brustmitte hin ein V bilden;
- zwei mittlere Platten am `body`, die den Kristall einfassen;
- zwei schmalere Bauchplatten und eine kurze Mittelspitze am `body`;
- ein kleiner, um 45 Grad gedrehter Brustkristall am `body`;
- zwei Rückenschalenplatten und ein kleiner Rückenkristall am `body`;
- pro Arm eine kompakte Schulterkappe und eine kleine Kristallspitze am jeweiligen Arm-Bone.

Kein Teil überbrückt Körper und Arm. Torso- und Rückenteile sind ausschliesslich Kinder von `body`; rechte Schulterteile gehören zu `right_arm`, linke zu `left_arm`. Schulterteile enden unterhalb des Kopfes und dürfen bei normalem Armschwung weder Kopf noch Brust durchschneiden. Das Modell bleibt symmetrisch; leichte Asymmetrie entsteht nur durch einzelne Texturpixel.

## Textur

Der Atlas verwendet eine begrenzte Palette aus Schwarz-Pflaume, dunklem Violett, Amethyst und wenigen Lavendellichtern. Grosse zufällige Muster und durchgehende horizontale Linien entfallen. Jede belegte UV-Insel erhält:

- eine dunkle Grundfläche;
- eine klar definierte obere oder äussere Kante;
- höchstens eine schmale violette Energienaht;
- helle Pixel nur an Brustkristall, Rückenkristall und Schulterspitzen.

Der Brustkristall ist vorne heller und grösser als alle übrigen Akzente. Das Design muss auch ohne Emissive-Shader lesbar bleiben.

## Item-Darstellung

Das Inventar- und Hand-Icon übernimmt dieselbe V-Silhouette, den zentralen Kristall und die kompakteren Schulterplatten. Die 16 × 16-Darstellung wird nicht aus dem Konzeptbild verkleinert, sondern als eigenständige Pixelgrafik erzeugt, damit sie im Inventar klar bleibt.

## Bewegungs- und Clippingvertrag

Das bestehende `copyPropertiesTo` bleibt unverändert. Automatische Tests sichern den exakten Bone-Besitz jedes neuen Teils, positive endliche Cuboid-Masse, UV-Grenzen und den ausschliesslichen Brustplatten-Texturpfad. Die reale Forge-Abnahme umfasst:

1. Frontansicht ohne Balkenwirkung oder Körperschnitt;
2. Rückansicht mit kompakter Schale;
3. Seitenansicht ohne schwebende Platten;
4. deutliche Armbewegung, bei der beide Schulterkappen am richtigen Arm bleiben;
5. Inventar- und Handdarstellung mit klarer V-Silhouette.

## Abgrenzung

Dieser Schritt verbessert ausschliesslich die True-Void-Brustplatte und ihr Item-Icon. Helm, Leggings, Stiefel und andere Rüstungsfamilien bleiben unverändert, bis dieses visuelle Muster im echten Client freigegeben ist. Silverfish-, Tripo- und Produktionsartefakte ausserhalb dieser Brustplatte werden nicht verändert.

## Abnahme

Die Überarbeitung ist fertig, wenn die fokussierten Tests und `compileJava` erfolgreich sind, die Assets deterministisch reproduziert werden und die fünf realen Forge-Prüfungen das freigegebene Konzept erkennbar treffen. Wenn eine Ansicht clippt oder der Kristall nicht klar lesbar ist, wird nicht auf weitere Rüstungsteile übertragen.
