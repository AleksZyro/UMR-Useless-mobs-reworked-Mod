# Helping Amethyst – Itemdesign

## Ziel

Der normale Minecraft-Amethystscherben darf ausschliesslich sein Vanilla-Verhalten behalten. Die Umwandlung eines Allays zum Helping Soul Allay wird nur durch das neue Mod-Item `usless_mobs:helping_amethyst` ausgelöst.

## Darstellung

- ChatGPT erzeugt eine eigenständige quadratische 2D-Pixelgrafik mit transparentem Hintergrund.
- Motiv: violett-cyaner Amethyst mit leuchtendem Seelenkern, goldener Resonanzfassung und klarer Silhouette.
- Keine Schrift, kein Hintergrund, kein Schatten und kein Wasserzeichen.
- Das Itemmodell verwendet `minecraft:item/generated`. Minecraft extrudiert die sichtbaren Pixel automatisch zu einem dünnen pseudo-3D-Item in Hand, Inventar und Welt.
- Das Item besitzt einen Verzauberungsglanz, damit es sich zusätzlich vom normalen Amethystscherben unterscheidet.

## Gameplay

- Technische ID: `usless_mobs:helping_amethyst`
- Deutsche Bezeichnung: `Seelen-Amethyst`
- Englische Bezeichnung: `Helping Amethyst`
- Stapelgrösse: 16, Seltenheit: selten.
- Rezept: ein Amethystscherben, ein Leuchttintenbeutel und ein Goldnugget als shapeless recipe.
- Nur dieses Item kann einen Vanilla-Allay in einen Helping Soul Allay umwandeln.
- Der normale Amethystscherben löst keine Mod-Umwandlung aus und bleibt dadurch konfliktfrei für Vanilla-Breeding.

## Prüfung

- Vertragstest verlangt Registrierung, Modell, Textur, Rezept und beide Übersetzungen.
- Vertragstest verbietet `Items.AMETHYST_SHARD` als Umwandlungsbedingung im Handler.
- Java-Kompilierung und Projektwahrheitsprüfung müssen bestehen.
- Im Client wird geprüft, dass der normale Scherben den Mod-Effekt nicht auslöst und der Seelen-Amethyst die Umwandlung ausführt.
