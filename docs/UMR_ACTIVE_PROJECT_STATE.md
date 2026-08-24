# UMR – aktive Projekt-Wahrheit

Stand: 24. August 2026. Diese Datei verhindert, dass ein alter Worktree oder ein komprimierter Chatverlauf mit dem aktiven Spielstand verwechselt wird.

## Wahrheitsreihenfolge

1. Tatsächlich geladene Laufzeitressourcen im aktuellen Worktree
2. Aktueller Branch, Git-Status und ausführbarer Prüfer
3. Freigegebene Spezifikationen und Pläne dieses Worktrees
4. Alte Branches, andere Worktrees, Webseiten-Vorschauen und Screenshots
5. Chat-Erinnerung

Vor jeder Modellarbeit:

```powershell
python tools/verify_umr_project_truth.py
git branch --show-current
git status --short
```

## Aktiver Corrupted Silverfish

- Kanonischer Arbeitsstand: Branch `feature/corrupted-silverfish-v3`
- Geometrie: `src/main/mobs/endermite/resources/assets/usless_mobs/geo/corrupted_silverfish.geo.json`
- Mesh: `src/main/mobs/endermite/resources/assets/usless_mobs/meshes/entity/corrupted_silverfish.mesh`
- Animation: `src/main/mobs/endermite/resources/assets/usless_mobs/animations/corrupted_silverfish.animation.json`
- Textur: `src/main/mobs/endermite/resources/assets/usless_mobs/textures/entity/corrupted_silverfish.png`
- Das aktive Spielmodell ist ein animiertes, texturiertes Tripo-Mesh mit GeckoLib-Bones und einem eigenen Triangle-Mesh-Renderer.
- Laufzeit-Signatur: **8 Bones, 0 Cubes, 7 Mesh-Regionen, 101,723 Dreiecke, 4096 × 4096 Textur**.
- Regionen: `body`, `leg_front_left`, `leg_front_right`, `leg_middle_left`, `leg_middle_right`, `leg_rear_left`, `leg_rear_right`.
- Das alte 11-Cube-Modell in anderen Worktrees ist nicht das aktive Spielmodell.

## Verbindliche Pipeline für weitere Mobs

Die genaue Tripo-Geometrie und Textur bleiben erhalten:

`Tripo-Export -> verlustfreies Blockbench-Rig -> binäres Laufzeit-Mesh -> GeckoLib-Bones -> eigener Mesh-Renderer -> Spielprüfung`

Keine automatische Umwandlung in ein vereinfachtes Würfelmodell, ausser Andrin verlangt dies ausdrücklich. Eine Webseiten-Vorschau gilt erst dann als übernommen, wenn genau ihr exportiertes Mesh und ihre Textur in den Laufzeitressourcen nachgewiesen sind.

Freigegebene Grundlagen:

- `docs/superpowers/specs/2026-08-20-remaining-mobs-tripo-design.md`
- `docs/superpowers/plans/2026-08-20-living-boss-tripo-pilot.md`

Reihenfolge der noch zu bearbeitenden Mobs:

1. Living Boss
2. Frost Stray
3. Web Cave Spider
4. Coral Drowned
5. Octopus
6. Witch Boss
7. Living Bat
8. Rooted Husk

Nicht Teil dieser Liste: Slimes, Corrupted Silverfish, Endermite, Rüstungen und Kronen.

## Aktiver Eisbär v1

- Eigene Entität: `usless_mobs:living_polar_bear`; kein Vanilla-Renderer-Austausch.
- Eigene Hitbox: 1,90 × 1,40 Blöcke; sie umschliesst die gemessene sichtbare Körperlänge von ungefähr 1,90 Blöcken.
- Quelle: `Modelle/Exports/polar_bear_v1/source/polar_bear_textured_4k.glb`.
- Laufzeit-Signatur: **6 Regionen, 0 Cubes, 684’939 Dreiecke, 4096 × 4096 Textur**.
- Animation: kontinuierliche Lauf- und Kopfverformung ohne offene Schnitte zwischen den ungewichteten Tripo-Regionen.
- Das eigene Spawn-Ei, eigene zusammengesetzte Geräusche und bestehende Bärenklauen-/Halsketten-Spielmechaniken sind angebunden.

## Aktiver Axolotl v1

- Eigene Entität: `usless_mobs:living_axolotl`; kein Vanilla-Renderer-Austausch.
- Eigene Hitbox: 1,35 × 0,65 Blöcke, abgeleitet aus dem länglichen Z-Achsen-Verhältnis des Exportmodells.
- Quelle: `Modelle/Exports/axolotl_v1/source/axolotl_textured_4k.glb`.
- Tripo-Projekt: `307f00c8-0017-482a-a470-e0feb829d659`.
- Laufzeit-Signatur: **7 Regionen, 0 Cubes, 725’114 Dreiecke, 4096 × 4096 Textur**.
- Regionen: `body`, `head`, `tail`, `leg_front_left`, `leg_front_right`, `leg_rear_left`, `leg_rear_right`.
- Animation: ein kontinuierliches Positionsfeld bewegt Schwanz, vier Beine und die sechs äusseren Kiemenäste, ohne Schnitte zwischen Regionen zu öffnen.
- Eigenes Spawn-Ei, deutsche/englische Namen, Registry, Attribute und Exaktrenderer sind angebunden.
- Automatische Tests und Java-Kompilierung sind grün. Die visuelle Laufzeitprüfung im Spiel ist bestanden: vollständige 4K-Textur, geschlossene Geometrie, registrierte 1,35 × 0,65-Hitbox und aktive Schwimmbewegung ohne Renderabsturz.

## Aktiver Ozelot v1

- Eigene Entität: `usless_mobs:living_ocelot`; Vanilla-Ozelots und ihr Renderer bleiben unverändert.
- Eigene Hitbox: 1,45 × 0,90 Blöcke, abgeleitet aus der gemessenen sichtbaren Körperlänge von 23,2 Modellpixeln.
- Quelle: `Modelle/Exports/ocelot_v1/source/ocelot_textured_4k.glb`.
- Tripo-Projekt: `b3347f03-f887-448b-a491-783bfc546935`.
- SHA-256 der unveränderten GLB-Quelle: `5EFB7EB9F75494BE92C695C55A414D038666B21595E44DEA15626722B4A5699D`.
- Laufzeit-Signatur: **7 Regionen, 0 Cubes, 721’849 Dreiecke, 4096 × 4096 Textur**; alle 721’849 Quelldreiecke sind übernommen.
- Regionen: `body`, `head`, `tail`, `leg_front_left`, `leg_front_right`, `leg_rear_left`, `leg_rear_right`.
- Animation: kontinuierliches Positionsfeld für gegenläufige Beine, Sprungstreckung, Kopfbewegung und Schwanz-Gegensteuerung, damit keine sichtbaren Schnitte zwischen ungewichteten Tripo-Regionen entstehen.
- Eigenes Spawn-Ei, deutsche/englische Namen, Registry, Attribute und Exaktrenderer sind angebunden. Die bestehende Besitzer-, Zielmarkierungs-, Teleport- und Sprungangriffsmechanik gilt ausschliesslich für die neue Entität.
- Automatische Mesh-, Hitbox- und Entity-Prüfungen sind mit **40/40 Tests** bestanden; `compileJava` ist erfolgreich.
- Der frische `runClient`-Durchlauf bestätigt Registry/Summon, vollständige Textur, geschlossene Vorder- und Seitenansicht, Bodenlage und die registrierte Hitbox ohne Renderausnahme. Die abschliessende Prüfung mit normaler aktiver KI und einem Hühnerziel zeigt Lauf-/Schleichhaltung, diagonal gegenläufige Beine, gestreckte Sprungpose und Schwanz-Gegensteuerung; die frühere `NoAI`-Diagnose ist damit nicht mehr der einzige visuelle Nachweis.

## Aktueller Politurstand der restlichen Tripo-Mobs

- Living Boss: Das Laufzeit-Mesh übernimmt exakt den texturierten 4K-Retopo-Export mit **97’428 Dreiecken**. Körper und vier Beine verwenden eine kontinuierliche quadrupede Laufverformung statt getrennter, aufreissender Knochenrotationen. Der Renderer skaliert die sichtbare Form nun auf ungefähr **3,63 Blöcke** maximale Ausdehnung; die dazu passende Boss-Hitbox misst **3,70 × 2,95 Blöcke** und der Schattenradius 1,45. Ein frischer Client mit `F3+B` bestätigt, dass die vergrösserte Form innerhalb der Hitbox steht und mit den Füssen am Boden endet.
- Frost Stray: Das Laufzeit-Mesh übernimmt exakt `Modelle/Exports/frost_stray_v1/source/frost_stray_textured_4k.glb` mit **109’255 Dreiecken** und unveränderter 4096 × 4096-Textur. Die 180°-Quellachsenkorrektur bleibt aktiv: Ein frischer Client mit `F3+B` zeigt Schädel und Rippen entlang des blauen Blickvektors; ohne diese Korrektur lag stattdessen die Eiswirbelsäule vorne. Die sichtbaren Mesh-Arme wechseln bei aggressiver KI in eine Bogen-Zielpose, während das vorhandene `ItemInHandLayer` den Bogen rendert. Die gemessene Form füllt 99–100 % der registrierten **1,10 × 1,95-Blöcke-Hitbox**, deshalb wurde ihre Grösse nicht geraten oder unnötig geändert. Umgebung, Treffer, Tod und Eis-Salve verwenden eine eigene vierteilige Frost-Klangfamilie. Die aktive KI verfolgte und zielte im Laufzeittest korrekt; Modell, Füsse und Blickrichtung blieben in der Hitbox. Die fokussierten Tests und `compileJava` sind grün.
- Web Cave Spider: Das Laufzeit-Mesh enthält exakt **97’234 Dreiecke**, 4096 × 4096 Textur und acht Beinregionen. Eine kontinuierliche diagonale Achtbein-Laufverformung bewegt die Beinspitzen stark und fällt am Körper weich auf null ab, damit keine Schnitte zwischen ungewichteten Tripo-Regionen entstehen. Die frühere 0,70-Block-Darstellung wurde mechanisch als zu klein erkannt: Der Renderer skaliert das exakte Mesh nun auf **1,26 Blöcke sichtbare Breite**, die passende Registry-Hitbox misst **1,30 × 0,56 Blöcke**, und der Schatten wurde gemeinsam auf 0,65 angepasst. Umgebung, Treffer, Tod und Netzangriff besitzen eine eigene vierteilige Web-/Chitin-Klangfamilie; Vanilla-Spinnen bleiben unverändert. Ein frischer Client mit `F3+B` bestätigt die geschlossene Geometrie, Bodenlage und passende Hitbox. Bei Nacht verliess der Spider seine Ausgangsposition, verfolgte den Spieler und führte seinen Nahkampfangriff samt Effekten erfolgreich aus. Die gezielten Mesh-/Renderer-/Soundtests und der vollständige Build sind grün.
- Coral Drowned: Das Laufzeit-Mesh übernimmt exakt `Modelle/Exports/coral_drowned_v1/source/coral_drowned_textured_4k.glb` (SHA-256 `E5A6C9517A6CAF228FEAE50A0C0598B021E3ED02331A6E24CD5FF19E0692FE2E`) mit **103’115 Dreiecken**, sechs Regionen und unveränderter 4096 × 4096-Textur. Die Tripo-Quelle liegt im Spiel um 90° zur Entity-Vorwärtsachse; die korrigierte Rendererrotation von **+90°** stellt Augen, Brustplatte und vorderen Korallenkern entlang des Entity-Blickvektors dar. Das frühere negative Vorzeichen zeigte fälschlich den grossen Rückenpanzer als Vorderseite. Geometrie, Pixel und UVs bleiben dabei unverändert. Die kontinuierliche Humanoid-Verformung bewegt Arme und Beine im Wasser; im aggressiven Zustand wechseln die sichtbaren Mesh-Arme zusätzlich in die Angriffsstellung des unsichtbaren Vanilla-/Item-Layers. Die gemessene Form füllt 99–100 % der registrierten **1,40 × 1,95-Blöcke-Hitbox**. Umgebung, Treffer, Tod und Korallen-Surge besitzen eine eigene vierteilige Unterwasser-/Korallen-Klangfamilie. Ein frischer Client mit `F3+B` bestätigt Front, vollständige Textur, Bodenlage, Hitbox und aktive Verfolgung. Die fokussierten Exact-Mesh-, Sound- und Coral-Regressionstests sowie `compileJava` sind grün.
- Octopus: Das Laufzeit-Mesh übernimmt exakt `Modelle/Exports/octopus_v1/tripo_export/octopus_tripo_textured_4k_20260821.glb` (SHA-256 `53F93DA6502FB4BD13F0255529C8BF41DB077E8401305188E5558C27D4B885E9`) mit **95’946 Dreiecken**, neun Regionen und unveränderter 4096 × 4096-Textur. Ein kontinuierliches positionsbasiertes Achtarm-Feld verformt alle Regionen nahtlos und unterscheidet Schwimmen, Tarnung/Lauerstellung, Greifen, Tintenstoss und Objektinteraktion; dadurch entstehen an den ungewichteten Tripo-Grenzen keine Schnitte. Der Quetschzustand skaliert die sichtbare Form nun auf 0,49 × 0,55 × 0,49 und passt damit zur synchronisierten kleinen Hitbox von 0,62 × 0,48 Blöcken. Die fokussierten Exact-Mesh-, Entity- und Rendererprüfungen sowie `compileJava` sind grün. Ein frischer `runClient`-Durchlauf bestätigt die vollständige 4K-Textur, acht sichtbare Arme, die laufende Armdeformation und den vorhandenen Tinten-/Blindheitseffekt.
- Witch Boss: Das Laufzeit-Mesh übernimmt exakt `Modelle/Exports/witch_boss_v1/tripo_export/witch_boss_tripo_textured_4k_20260821.glb` (SHA-256 `11971F35F411A9D850B0A7218936BBD026DE0F93B117078FD0719F15905917EC`) mit **89’610 Dreiecken**, sechs Regionen und unveränderter 4096 × 4096-Textur. Kopf, Arme und Beine verwenden nun die kontinuierliche Humanoid-Verformung statt einer starren Ganzkörperbewegung; gemeinsame Tripo-Grenzen bleiben dabei geschlossen. Der fokussierte Regressionstest und `compileJava` sind grün. Ein frischer Client bestätigt das vollständige Mesh, die korrekte Textur, aktive Verfolgung sowie die bereits vorhandenen Fluch-, Hasen- und Jagdhundphasen.
- Living Bat: Das Laufzeit-Mesh übernimmt exakt `Modelle/Exports/living_bat_v1/tripo_export/living_bat_tripo_textured_4k_20260821.glb` (SHA-256 `6F16BFADBB675D602ACC506E72CF615310887CBFB6A5568A041A27702697CB93`) mit **99’081 Dreiecken**, sechs Regionen und unveränderter 4096 × 4096-Textur. Ein kontinuierliches Flügelfeld erzeugt kräftige symmetrische Schläge, nachgiebige Flügelspitzen, eine eingeklappte Ruhehaltung und leichte Kopfbewegung, ohne an den ungewichteten Regionengrenzen Schnitte zu öffnen. Eine eigene dreiteilige Klangfamilie kombiniert tiefe Fledermausrufe mit Blätter- und Wurzelgeräuschen für Umgebung, Treffer und Tod. Der Giftschlag mit nächtlicher Blindheit und Schwäche gehört nun ausschliesslich der dedizierten Living-Bat-Entität; Vanilla-Fledermäuse lösen ihn nicht mehr aus. Die fokussierten Regressionstests und `compileJava` sind grün; zwei Nahaufnahmen im frischen Client zeigen deutlich unterschiedliche Flügelstellungen bei unverändert geschlossener Texturfläche.
- Rooted Husk: Das Laufzeit-Mesh übernimmt exakt `Modelle/Exports/rooted_husk_v1/tripo_export/rooted_husk_tripo_textured_4k_20260821.glb` (SHA-256 `ACD61F812AB02DD4D8A106B95D6555AA1363164557E415DCB9F9EB1DB730B1E8`) mit **95’854 Dreiecken**, sechs Regionen und unveränderter 4096 × 4096-Textur. Kopf, Arme und Beine verwenden nun ebenfalls die kontinuierliche Humanoid-Verformung; bei aggressiver KI wechseln die Mesh-Arme zusätzlich in die Nahkampfangriffshaltung. Seine eigene dreiteilige Klangfamilie legt Wurzelboden-Knacken unter tiefe Husk-Stimmen. Der dedizierte Rooted Husk besitzt jetzt selbst den verwurzelnden Treffer: Hunger, Verlangsamung, eine kleine Selbstheilung, Sporen und hörbares Wurzelknacken; Vanilla-Husks erhalten diese UMR-Kombination nicht mehr. Die fokussierten Regressionstests und `compileJava` sind grün. Der frische Client bestätigt die breite Front, vollständige braun-grüne Textur, geschlossene Geometrie und aktive Eigenbewegung; der Husk verliess nach der Landung sichtbar seine Ausgangsposition.
- Die acht festgelegten Tripo-Kandidaten sind damit im Code und in der Laufzeit auf nahtlose Bewegungsfelder umgestellt. Die gemeinsame Grössen-, Hitbox-, Sound- und Effektpolitur läuft; Web Cave Spider, Frost Stray, Coral Drowned, Living Bat und Rooted Husk besitzen bereits ihre korrigierten dedizierten Verträge.

## Gemeinsame Boden- und Nahtkorrektur

- Sämtliche aktiven Exact-Mesh-Dateien verwenden dieselbe Minecraft-Bodenebene bei Modell-Y **24,0**. Der normale `LivingEntityRenderer` verankert diese Ebene bereits am Entity-Boden.
- Der frühere zusätzliche Versatz um die komplette Meshhöhe wurde entfernt; er war die gemeinsame Ursache dafür, dass Eisbär, Living Boss, Ozelot, Frost Stray und weitere Modelle über ihrer Hitbox schwebten.
- Nach individueller Modellskalierung hält nun ein skalenabhängiger Ausgleich exakt dieselbe Bodenebene fest. Dadurch sinken vergrösserte oder gequetschte Modelle weder ein noch schweben sie.
- Ungewichtete Tripo-Oberflächen werden über kontinuierliche Positionsfelder oder als geschlossene Wurzel bewegt. Dadurch bleiben gemeinsame Dreiecksgrenzen geschlossen und die zuvor sichtbaren Schnitte entstehen nicht erneut.
- Die Ozelot-Hitbox verwendet wieder die aus dem Mesh gemessene Länge von **1,45 × 0,90 Blöcken** statt des fehlerhaften Zwischenwerts 0,95 × 0,90.

## Vollständige Laufzeitprüfung vom 23. August 2026

- Im aktiven `runClient` wurden Corrupted Silverfish, Living Boss, Frost Stray, Web Cave Spider, Coral Drowned, Octopus, Witch Boss, Helping Allay, Living Squid, Living Glow Squid, Living Polar Bear, Living Axolotl, Living Ocelot, Living Bat und Rooted Husk einzeln mit `F3+B` geprüft.
- Die statische Prüfung bestätigt bei allen 15 Entitäten vollständige Texturen, geschlossene Oberflächen ohne sichtbaren Schnitt, korrekte Vorderseite sowie eine Form innerhalb der registrierten Hitbox. Die Bodenmodelle berühren im natürlichen Terrain die Bodenebene; Flug- und Wassermodelle wurden zusätzlich in ihrer passenden Umgebung geprüft.
- Living Squid und Helping Allay wirken in der Vorderansicht wegen ihrer länglichen Silhouette schmal. Die zusätzliche 90°-Seitenansicht bestätigt ihre tatsächliche Längsausdehnung. Eine anschliessende mechanische Vermessung deckte beim Helping Allay jedoch einen Registry-Fehler auf: Das sichtbare Mesh misst skaliert ungefähr **0,905 × 0,878 × 0,544 Blöcke**, während die frühere Hitbox unnötig **1,45 Blöcke** breit war. Die registrierte Hitbox ist deshalb auf **0,95 × 0,90 Blöcke** korrigiert. Ein Regressionstest vergleicht nun für alle exakten Laufzeit-Meshes die gemessenen Abmessungen direkt mit `ModEntities`, damit Bild, Modell und Hitbox nicht erneut auseinanderlaufen.
- Der Living Squid wurde im Wasser mit wieder aktivierter KI geprüft. Körper und Tentakel bleiben bei der Schwimmbewegung geschlossen und die unveränderte 4K-Textur bleibt korrekt auf dem Mesh.
- Die vollständige Python-Suite bestand anschliessend mit **439 bestandenen und 4 umgebungsbedingt übersprungenen Tests**. Der aktuelle Client-Log enthält keine fehlenden Modelle, Texturen, Animationen, GeckoLib- oder Registry-Fehler.

## Schutz vor Wiederholungsfehlern

- Niemals einen Dateipfad aus einem anderen Worktree als Beweis für den aktiven Spielstand verwenden.
- Vor einer Anzahl wie Cubes, Bones, Regionen oder Dreiecke immer den Prüfer ausführen.
- Bei Widerspruch gewinnt die mechanisch geprüfte Laufzeitressource.
- Der Worktree enthält absichtlich uncommittete Arbeit. Unverwandte Änderungen bleiben unangetastet.

## Bossgrössen- und Schwierigkeitsprüfung vom 24. August 2026

- King Slime bleibt der vollständig ausgebaute Referenzboss: dynamische Schleimgrösse **8**, **320 Basisleben**, konfigurierbare Lebens-, Schadens- und Geschwindigkeitsmultiplikatoren, mehrere Phasen, Telegraphen, Minions sowie gestufte Easy-/Normal-/Hard-Belohnungen.
- Living Boss bleibt bei der mechanisch bestätigten **3,70 × 2,95-Blöcke-Hitbox** und **220 Basisleben**. Das sichtbare 4K-Mesh füllt diese Hitbox zu ungefähr 98 Prozent; eine weitere Vergrösserung wäre geometrisch falsch. Wurzelwelle, Bodenbruch, Käfig, Heilrhythmus, Wurzelpuls, Dornenkonter, Spinnen und Wurzelgeister verwenden nun ein gemeinsames dreistufiges Schwierigkeitsprofil.
- Witch Boss bleibt bei der exakt passenden humanoiden **1,15 × 1,95-Blöcke-Hitbox** und **155 Basisleben**. Ihre Bosswirkung entsteht aus Flüchen, Giftflächen, Heiltrank, Geistern, Ausweichmanöver und Hasenjagd statt aus einer künstlich übergrossen Trefferfläche. Sämtliche Angriffspausen, Spezialschäden, Geisterzahlen, Jagdhunde und Belohnungen verwenden nun ebenfalls drei getrennte Stufen.
- Das gemeinsame Profil setzt Spezial- und Nahkampfschaden auf **72 % / 100 % / 130 %**, Cooldowns auf **125 % / 100 % / 78 %**, Living-Beschwörungsgrenzen auf **2 / 4 / 6**, Witch-Geister auf **1 / 2 / 3**, Jagdhunde auf **2 / 3 / 4** und Belohnungsstufen auf **0 / 1 / 2** für Easy / Normal / Hard.
- Jagdhunde besitzen jetzt eine endliche Laufzeit und werden entfernt, sobald ihre Besitzer-Witch nicht mehr existiert. Tod, Entfernung, Dimensionswechsel, Logout oder Ablauf beenden die Hasenform und stellen die normale Spielergrösse wieder her.
- Ein eigener Giant-Squid-Boss ist noch keine aktive Laufzeitentität. Deep-Ocean-Designtexte sind kein Ersatz für Registry, freigegebenes Exact-Mesh, Renderer, KI und Spielprüfung; deshalb wird dieser unabhängige Modell-/Worldgen-Abschnitt nicht fälschlich als fertig bezeichnet.

## Projektweite Integritätsprüfung vom 24. August 2026

- Registrierungen, Attribute, Renderer, Spawn-Eier, Itemmodelle, Sprachdateien, Exact-Meshes, Texturen, Sounds und der letzte Client-Log wurden erneut gegeneinander geprüft. Der Client-Log enthält keine fehlenden UMR-Modelle, Texturen, Animationen oder Registryeinträge.
- Sämtliche Runtime-JSON-Dateien sind syntaktisch gültig, keine Runtime-Ressource ist leer und alle geprüften PNG-Dateien lassen sich vollständig dekodieren.
- Der vollständige Forge-Build verarbeitet Ressourcen, erstellt und reobfuskiert das Mod-JAR erfolgreich. Das erzeugte JAR enthält 1’139 eindeutige Einträge ohne Duplikate; die Exact-Meshes und 4K-Texturen sind enthalten.
- Die beiden verbliebenen veralteten `ResourceLocation`-Konstruktoren des Corrupted Silverfish verwenden nun `ResourceLocation.tryBuild`. Der absichtlich nötige Forge-1.20.1-`onPlace`-Override des Living Crystal Blocks ist lokal dokumentiert und gezielt von der direkten-Aufruf-Deprecation ausgenommen.
- Die vollständige Python-Suite besteht auf diesem Stand mit **448 bestandenen und 4 umgebungsbedingt übersprungenen Tests**. Java kompiliert auch mit aktiviertem Deprecation-Lint ohne projektinterne Warnung.
- Die verbleibende Gradle-9-Warnung entsteht innerhalb von ForgeGradle 6.0.47. Sie gehört nicht zum UMR-Quellcode und kann für Minecraft Forge 1.20.1 nur durch eine separate Loader-/Buildsystemmigration entfernt werden.

## Laufzeit-Crashprüfung vom 24. August 2026

- Ein frischer `runClient`-Durchlauf mit fallengelassenen Gegenständen reproduzierte einen echten GeckoLib-Absturz: `TalismanGlowLayer` übergab vollständig transparente Glowmasken an `AutoGlowingGeoLayer`, das mindestens ein sichtbares Pixel verlangt.
- `celestial_talisman_geo_glowmask.png` und `living_talisman_geo_glowmask.png` enthalten nun ausschliesslich ausgewählte helle Akzentpixel ihrer vorhandenen Basistexturen und sind nicht mehr leer. Ein neuer Regressionstest prüft alle drei Talisman-Glowmasken auf korrektes PNG-Format, 32 × 32 Pixel und mindestens ein sichtbares Pixel.
- Der anschliessende vollständige Clientlauf lud die Welt, Entitäten und fallengelassenen Gegenstände ohne erneuten Glowmask-Absturz. Repräsentative `F3+B`-Kontrollen bestätigten Living Boss, Witch Boss, Helping Allay, Living Glow Squid, Living Polar Bear, Web Cave Spider und Octopus innerhalb ihrer Hitboxen; der Living Boss steht auf dem Boden und füllt seine Boss-Hitbox aus.
- Der Client wurde über `Save and Quit to Title` sauber beendet; `runClient` endete mit `BUILD SUCCESSFUL`. Der aktuelle Log enthält weder die frühere `Invalid glow layer texture`-Ausnahme noch fehlende UMR-Modelle, Texturen, Animationen oder Registryeinträge.
- Die vollständige Python-Suite besteht danach mit **450 bestandenen und 4 umgebungsbedingt übersprungenen Tests**. Der vollständige Forge-Build samt Reobfuskierung des Mod-JARs ist erfolgreich.
