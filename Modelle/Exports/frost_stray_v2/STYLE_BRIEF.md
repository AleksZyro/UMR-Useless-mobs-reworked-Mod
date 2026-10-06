# Frost Stray v2 – Style-Brief

## Richtung

Ein verwitterter Eiswächter: eine lesbare, hohe Knochen-Silhouette unter schweren
eisblauen Schulterplatten, eine helle gefrorene Krone als Kopfakzent und ein
sichtbar vorbereiteter Bogenangriff. Die bestehende 4K-Quelle bleibt die
organische Basis; die v2-Akzente sind bewusst getrennte, harte Eisstücke.

## Drei sichtbare Ziele

| Bisheriger Mangel | Konkrete Verbesserung | Abnahmebild |
| --- | --- | --- |
| Kopf und Oberkörper verschmelzen aus mittlerer Distanz. | Krone, Gesichtsscherbe und Halskragen bilden eine helle Kopf- und Schulterlinie. | Front bei 12 Blöcken, vor dunklem Fichtenwald. |
| Der Angriff beginnt nahe am Oberkörper und erklärt den Bogen nicht. | Charge-Partikel wandern vom Brustanker zum dokumentierten Bogengriff; Release beginnt am Projektil-Socket. | Spieleransicht bei 12 Blöcken: Vorbereitung, Freigabe, Einschlag. |
| Eis wirkt bisher wie eine einheitliche blaue Fläche. | Die Overlay-Palette unterscheidet tiefes Blau, Frostkante und zurückhaltendes Cyan; Knochen, Stoff und Metall bleiben in der vorhandenen Basisquelle lesbar. | Seite/Rücken bei 4 und 24 Blöcken mit aktivierten Mipmaps. |

## Materialregel

Der Vanilla-Entity-Renderer wertet für diese Meshschicht Albedo und Alpha aus,
keine separate Normal- oder Roughness-Map. Deshalb werden keine wirkungslosen
PBR-Kanäle vorgetäuscht. Eis erhält helle Kanten und wenige Cyan-Akzente;
Knochen, Stoff und Metall verbleiben in der geprüften 4K-Basisalbedo.

## Herkunft

Die Basis ist die versionierte Projektquelle
`../frost_stray_v1/source/frost_stray_textured_4k_v3_candidate.glb`. Die v2-
Overlay-Textur wird ausschliesslich aus dem versionierten Python-Generator
erstellt. Die drei neuen Klang-Cues referenzieren nur vorhandene
Minecraft-Soundevents; es wird kein fremdes Audio eingebettet.
