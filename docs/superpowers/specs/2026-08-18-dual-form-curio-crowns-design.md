# Doppelform-Curio-Kronen

## Ziel

Die Pfadkronen erhalten eine kompakte Kampfform und eine aufwendigere königliche Form. Beide Formen werden im vorhandenen Curio-Kronenslot getragen, liegen sichtbar auf Kopf oder Helm und besitzen identische Spieleffekte. Die Änderung ist funktional ein Reskin mit einer materialgebundenen zweiten Form.

## Umfang

Betroffen sind die Kronenfamilien Void, Celestial, Living und Balance. Für jede Familie entstehen:

- eine kompakte Kampfkrone;
- eine höhere Königskrone;
- ein eigenständiges 3D-Itemmodell pro Form;
- eine deterministisch erzeugte Pixeltextur pro Form;
- ein Umwandlungsrezept zur Königskrone;
- ein kostenloses Rückwandlungsrezept zur Kampfform.

Rüstungswerte, Kroneneffekte, Allegiance-Logik und Curio-Slot bleiben unverändert.

## Visuelle Gestaltung

### Gemeinsame Formensprache

Alle Kronen bilden einen geschlossenen Ring. Es gibt keine schwebenden Würfel, getrennten Hörner oder Teile, die nur durch Perspektive verbunden erscheinen. Gedrehte Elemente verwenden lokale, zentrierte Drehpunkte.

Die Texturen orientieren sich an der Detailqualität des Corrupted Silverfish:

- mindestens vier Materialhelligkeiten pro Hauptmaterial;
- klar abgesetzte Kanten, Fugen und Schatten;
- kleine, kontrollierte Pixelcluster statt gleichmässiger Vollflächen;
- eindeutig lesbare Edelsteinkerne;
- familientypische Akzentfarben ohne zufälliges Bildrauschen.

### Kampfform

Die Kampfform liegt flach und kompakt am Helm. Sie besitzt einen stabilen Stirnreif, drei kurze Hauptspitzen und kleine geschützte Familienornamente. Die Silhouette darf beim Laufen oder Kämpfen nicht wie eine hohe Festkrone wirken.

### Königliche Form

Die königliche Form besitzt einen höheren Ring, fünf gestufte Spitzen, einen grösseren zentralen Edelstein und zwei klar verbundene Seitenornamente. Sie bleibt breit genug, damit sie über einem Helm nicht in diesen einschneidet, und kompakt genug für die Spieleransicht.

### Familien

- Void: dunkles Metall, violette Kristallkerne und kurze seitliche Hörner.
- Celestial: helles Metall, Goldkanten, cyanfarbene Edelsteine und flügelartige Seitenplatten.
- Living: dunkles Holzmetall, Moosgrün, leuchtende Lebenskerne und verbundene Rankenblätter.
- Balance: symmetrische Verbindung aus Void, Celestial und Living mit einem zentralen Balance-Edelstein.

## Bildgenerierung und Übertragung

ChatGPT Image Generation erzeugt eine Konzepttafel mit neutralem dunklem Hintergrund. Jede Kronenfamilie wird in Kampf- und Königsgestalt aus Vorder-, Seiten-, Rück- und Draufsicht dargestellt. Die Bilder sind Designreferenzen und werden nicht direkt als Minecraft-Textur ausgeliefert.

Ein lokaler deterministischer Generator überträgt anschliessend die freigegebenen Formen in Minecraft-Geometrie, UV-Belegung und Pixeltexturen. Dadurch bleiben Auflösung, UV-Grenzen, Symmetrie und reproduzierbare Builds kontrollierbar.

## Items und Rezepte

Jede Kampfform besitzt eine zugehörige Königsversion mit identischen Effekten.

Das Freischaltungsrezept verwendet exakt neun Plätze eines 3x3-Craftingfelds:

- eine Kampfkrone;
- ein Netheritebarren;
- sieben Diamanten.

Das Muster lautet `DDD / DCD / DND`: `D` steht für Diamant, `C` für die jeweilige Kampfkrone und `N` für den Netheritebarren.

Das Ergebnis ist die entsprechende Königskrone. Die Königskrone kann allein im Craftingfeld kostenlos wieder in ihre Kampfform zurückverwandelt werden. Eine erneute Aufwertung kostet wieder die Materialien; die beiden Items speichern keine kontoweite Freischaltung.

## Curio-Rendering

Beide Formen verwenden den vorhandenen Curio-Slot `crown` und folgen der Kopfrotation. Der Renderer prüft den Rüstungsslot `HEAD`:

- ohne Helm liegt die Krone direkt auf dem Kopf;
- mit Helm wird sie um den bereits bei der Slime-Krone verwendeten Offset angehoben;
- der Offset ist für Kampf- und Königsgestalt separat feinjustierbar, bleibt aber an dieselbe Helmprüfung gebunden.

Path-Crown- und Balance-Crown-Items werden explizit beim Curio-Renderer registriert. Die Registrierung bleibt clientseitig; Effekte und Tick-Logik bleiben serverseitig.

## Daten und Kompatibilität

Die bestehenden Kampfkronen bleiben unter ihren bisherigen Item-IDs erhalten, damit Welten nicht brechen. Nur die Königsversionen erhalten neue IDs. Vorhandene Kronen werden nicht automatisch umgewandelt.

Texturnamen, Modellpfade, Übersetzungen, Item-Tags, Curio-Tags und Rezepte werden vollständig ergänzt. Es gibt keine Abhängigkeit von einem externen Webdienst zur Laufzeit oder beim normalen Build.

## Fehlerbehandlung

Der Assetgenerator schreibt mehrere Dateien transaktional. Ungültige Auflösungen, transparente Leerausgaben, UV-Überläufe, doppelte Namen, getrennte Kronenteile und nicht reproduzierbare Bytes brechen den Build mit verständlicher Meldung ab. Bestehende Ausgaben bleiben bei einem Fehler unverändert.

## Prüfung und Abnahme

Automatisierte Prüfungen sichern:

- exakte Item- und Rezeptmengen;
- identische Effekte beider Formen;
- Curio-Registrierung und Helm-Offset;
- geschlossene Kronenringe und verbundene Ornamente;
- lokale Drehpunkte für alle gedrehten Teile;
- UV-Grenzen und Texturauflösungen;
- deterministische Assetbytes;
- keine fehlenden Modell- oder Texturpfade.

Die visuelle Abnahme erfolgt im echten Forge-Client mit Vorder-, Seiten- und Rückansicht, jeweils ohne Helm und mit einem vollständigen Helm. Zusätzlich werden Inventaransichten beider Formen aufgenommen. Erst danach gelten die Kronen als fertig.
