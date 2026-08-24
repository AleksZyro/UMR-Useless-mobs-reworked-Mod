# True-Void-Brustplatte am Spieler – Design

## Ziel

Die True-Void-Brustplatte wird als klar lesbare, Minecraft-native 3D-Rüstung umgesetzt. Sie soll dem freigegebenen Konzept mit dunklen geschichteten Platten, violetten Leuchtlinien und zentralem Kristall folgen, korrekt am Standard-Spieler sitzen und alle normalen Spieleranimationen ohne auseinanderfallende oder körperübergreifende Bauteile mitmachen.

## Abgrenzung

Dieser Pilot umfasst ausschliesslich die True-Void-Brustplatte:

- angezogenes 3D-Modell am Spieler;
- Brustplatten-Textur inklusive violetter Akzente;
- dazu passende Inventar- und Handdarstellung;
- statische und visuelle Prüfungen für Passform und Bewegung.

Helm, Leggings, Stiefel, andere Rüstungsfamilien und die abgelehnten automatischen Tripo-Blockkonvertierungen werden nicht verändert. Der Pilot wird zuerst sichtbar abgenommen und dient danach als wiederverwendbares Muster.

## Gewählter Ansatz

Das Modell wird direkt mit Minecraft-`HumanoidModel`-Teilen aufgebaut. Torsoelemente werden Kinder des `body`-Bones; rechte und linke Schulterelemente werden Kinder von `right_arm` beziehungsweise `left_arm`. Dadurch übernimmt das vorhandene `copyPropertiesTo` automatisch Laufen, Schleichen, Schlagen und weitere Spielerposen.

Ein importiertes Tripo-Dreiecksmesh wird nicht als Runtime-Modell verwendet. Es passt nicht zuverlässig zum humanoiden Rig und entspricht nicht dem bestehenden cube- und bone-basierten Renderer. Ein reines Item-JSON reicht ebenfalls nicht, weil es keine angezogene Rüstung animiert.

## Geometrie

Der Torso besteht aus wenigen grossen, kontrollierten Bauteilen:

- Grundpanzer mit kleiner, gleichmässiger Distanz zum 8 × 12 × 4 grossen Spielertorso;
- obere Brustplatten links und rechts;
- mittlere Frontplatte mit zentralem Kristall;
- zwei bis drei gestaffelte untere Bauchplatten;
- eine schlichte Rückenschale ohne Frontkristall;
- getrennte Schulterplatten pro Arm.

Kein Würfel darf Körper und Arm gleichzeitig überbrücken. Dekorationen am Torso bleiben am `body`; Schulterdekorationen bleiben am jeweiligen Arm. Alle Abmessungen bleiben endlich, positiv und innerhalb eines engen Bereichs um die Standard-Spielerhülle. Die Silhouette darf breiter als Vanilla-Rüstung sein, aber nicht wie ein frei schwebendes Kreaturenmodell wirken.

## Textur und Leuchten

Die bestehende 128 × 64-Rüstungsatlasstruktur bleibt erhalten. Die Farbpalette ist:

- fast schwarzes Void-Metall;
- dunkles, entsättigtes Violett für sekundäre Flächen;
- wenige helle violette Pixel für Nähte und Kristall;
- keine grünen oder zufälligen Fremdfarben.

Die violetten Linien bleiben schmal und folgen den Plattenkanten. Der Brustkristall ist der einzige grosse Blickfang. Für diesen Pilot wird kein neuer Shadervertrag eingeführt; das Modell muss auch ohne Emissive-Layer sauber aussehen.

## Inventar und Hand

Das Item-Modell bleibt getrennt vom getragenen Humanoid-Modell. Es wird als vereinfachte, aber eindeutig passende Brustplatte aufgebaut und nutzt dieselbe visuelle Hierarchie: dunkle Schale, Schulterform, zentrale violette Raute und untere Platten. GUI-, Boden-, Fixed- und Handtransforms müssen das vollständige Item zeigen, ohne Abschneiden oder unleserliche extreme Perspektive.

## Bewegungsvertrag

Der vorhandene Clientpfad kopiert die Pose des Vanilla-`HumanoidModel` auf das Custom-Modell. Der Pilot muss mechanisch nachweisen:

- Torsoelemente gehören ausschliesslich zum `body`-Bone;
- rechte Schulterelemente gehören ausschliesslich zum `right_arm`-Bone;
- linke Schulterelemente gehören ausschliesslich zum `left_arm`-Bone;
- die Brustplatte ist nur im `CHESTPLATE`-Slot sichtbar;
- alle drei Hauptteile übernehmen ihre jeweilige Vanilla-Pose.

Visuell werden Front, Rücken und Seite im Stand sowie mindestens eine deutliche Laufpose und eine Armbewegung geprüft. Es dürfen keine sichtbaren Schnitte durch den Torso, keine verbundenen Schulterbrücken und keine fehlenden Texturen erscheinen.

## Fehlerbehandlung und Schutz

Der vorhandene Slot-Guard und Modellcache in `TruePathArmorItem` bleiben erhalten. Andere Pfade und Rüstungsteile dürfen durch den Pilot nicht verändert werden. Tests müssen den exakten Bone-Besitz, positive endliche Cube-Dimensionen, Texturbounds, Itemtransforms und die unveränderte Sichtbarkeitslogik abdecken.

## Abnahme

Der Pilot gilt erst als fertig, wenn:

1. die fokussierten Rüstungsvertragstests grün sind;
2. `compileJava` erfolgreich ist oder ein externer Gradle-Blocker ehrlich dokumentiert wurde;
3. ein visueller Zwischenstand am Spieler Front, Rücken, Seite und Bewegung zeigt;
4. Inventar und Handdarstellung klar zur getragenen Brustplatte passen;
5. keine Produktionsdateien anderer Rüstungen oder die geschützten Silverfish-Artefakte unbeabsichtigt geändert wurden.
