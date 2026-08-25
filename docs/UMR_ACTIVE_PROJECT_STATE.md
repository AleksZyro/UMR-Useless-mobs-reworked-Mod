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

Die vollständigen Tripo-Exporte und 4K-Texturen bleiben als Qualitätsquellen erhalten. Für das Spiel werden UV-erhaltend vereinfachte Dreiecksmeshes mit höchstens 110’000 sichtbaren Dreiecken verwendet:

`Tripo-Export -> archivierte 4K-Quelle -> UV-erhaltendes Performance-Mesh -> binäres Laufzeit-Mesh -> GeckoLib-Bones -> eigener Mesh-Renderer -> Spielprüfung`

Keine automatische Umwandlung in ein Würfelmodell, ausser Andrin verlangt dies ausdrücklich. Die Optimierung muss die Dreiecksoberfläche, UVs, Materialien und 4096 × 4096-Albedo behalten. Eine Webseiten-Vorschau gilt erst dann als übernommen, wenn ihr Export als Quelle archiviert und das davon abgeleitete Laufzeitmesh mechanisch nachgewiesen ist.

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
- Qualitätsquelle: `Modelle/Exports/polar_bear_v1/source/polar_bear_textured_4k.glb`; Laufzeitquelle: `polar_bear_runtime_optimized_4k.glb`.
- Laufzeit-Signatur: **6 Regionen, 0 Cubes, 84’682 Dreiecke, 4096 × 4096 Textur**. Die UV-erhaltende Optimierung reduzierte 684’939 Quelldreiecke auf 95’888; die Zusammenhangsprüfung entfernte daraus zusätzlich die abgetrennte Fremdkomponente mit 11’206 Dreiecken.
- Animation: kontinuierliche Lauf- und Kopfverformung ohne offene Schnitte zwischen den ungewichteten Tripo-Regionen.
- Das eigene Spawn-Ei, eigene zusammengesetzte Geräusche und bestehende Bärenklauen-/Halsketten-Spielmechaniken sind angebunden.

## Aktiver Axolotl v1

- Eigene Entität: `usless_mobs:living_axolotl`; kein Vanilla-Renderer-Austausch.
- Eigene Hitbox: 1,35 × 0,65 Blöcke, abgeleitet aus dem länglichen Z-Achsen-Verhältnis des Exportmodells.
- Qualitätsquelle: `Modelle/Exports/axolotl_v1/source/axolotl_textured_4k_v6_user_export.glb`; Laufzeitquelle: `axolotl_runtime_optimized_4k.glb`.
- Tripo-Projekt: `307f00c8-0017-482a-a470-e0feb829d659`.
- Laufzeit-Signatur: **7 Regionen, 0 Cubes, 98’535 Dreiecke, 4096 × 4096 Textur**.
- Regionen: `body`, `head`, `tail`, `leg_front_left`, `leg_front_right`, `leg_rear_left`, `leg_rear_right`.
- Animation: ein kontinuierliches Positionsfeld bewegt Schwanz, vier Beine und die sechs äusseren Kiemenäste, ohne Schnitte zwischen Regionen zu öffnen.
- Eigenes Spawn-Ei, deutsche/englische Namen, Registry, Attribute und Exaktrenderer sind angebunden.
- Automatische Tests und Java-Kompilierung sind grün. Die visuelle Laufzeitprüfung im Spiel ist bestanden: vollständige 4K-Textur, geschlossene Geometrie, registrierte 1,35 × 0,65-Hitbox und aktive Schwimmbewegung ohne Renderabsturz.

## Aktiver Ozelot v1

- Eigene Entität: `usless_mobs:living_ocelot`; Vanilla-Ozelots und ihr Renderer bleiben unverändert.
- Eigene Hitbox: 1,45 × 0,90 Blöcke, abgeleitet aus der gemessenen sichtbaren Körperlänge von 23,2 Modellpixeln.
- Qualitätsquelle: `Modelle/Exports/ocelot_v1/source/ocelot_textured_4k.glb`; Laufzeitquelle: `ocelot_runtime_optimized_4k.glb`.
- Tripo-Projekt: `b3347f03-f887-448b-a491-783bfc546935`.
- SHA-256 der unveränderten GLB-Quelle: `5EFB7EB9F75494BE92C695C55A414D038666B21595E44DEA15626722B4A5699D`.
- Laufzeit-Signatur: **7 Regionen, 0 Cubes, 101’058 Dreiecke, 4096 × 4096 Textur**. Das vollständige Original bleibt archiviert; das Spiel verwendet die UV-erhaltende Performance-Version.
- Regionen: `body`, `head`, `tail`, `leg_front_left`, `leg_front_right`, `leg_rear_left`, `leg_rear_right`.
- Animation: kontinuierliches Positionsfeld für gegenläufige Beine, Sprungstreckung, Kopfbewegung und Schwanz-Gegensteuerung, damit keine sichtbaren Schnitte zwischen ungewichteten Tripo-Regionen entstehen.
- Eigenes Spawn-Ei, deutsche/englische Namen, Registry, Attribute und Exaktrenderer sind angebunden. Die bestehende Besitzer-, Zielmarkierungs-, Teleport- und Sprungangriffsmechanik gilt ausschliesslich für die neue Entität.
- Automatische Mesh-, Hitbox- und Entity-Prüfungen sind mit **40/40 Tests** bestanden; `compileJava` ist erfolgreich.
- Der frische `runClient`-Durchlauf bestätigt Registry/Summon, vollständige Textur, geschlossene Vorder- und Seitenansicht, Bodenlage und die registrierte Hitbox ohne Renderausnahme. Die abschliessende Prüfung mit normaler aktiver KI und einem Hühnerziel zeigt Lauf-/Schleichhaltung, diagonal gegenläufige Beine, gestreckte Sprungpose und Schwanz-Gegensteuerung; die frühere `NoAI`-Diagnose ist damit nicht mehr der einzige visuelle Nachweis.

## Aktueller Politurstand der restlichen Tripo-Mobs

- Living Boss: Das Laufzeit-Mesh übernimmt exakt den texturierten 4K-Retopo-Export mit **97’428 Dreiecken**. Körper und vier Beine verwenden eine kontinuierliche quadrupede Laufverformung statt getrennter, aufreissender Knochenrotationen. Der Renderer skaliert die sichtbare Form nun auf ungefähr **3,63 Blöcke** maximale Ausdehnung; die dazu passende Boss-Hitbox misst **3,70 × 2,95 Blöcke** und der Schattenradius 1,45. Ein frischer Client mit `F3+B` bestätigt, dass die vergrösserte Form innerhalb der Hitbox steht und mit den Füssen am Boden endet.
- Frost Stray: Die vollständige Qualitätsquelle `frost_stray_textured_4k_v3_candidate.glb` bleibt archiviert; das aktive `frost_stray_runtime_optimized_4k.glb` besitzt **98’103 Dreiecke** und dieselbe 4096 × 4096-Textur. Der regenerierte Export stimmt bereits mit der Minecraft-Vorwärtsachse überein und benötigt keine 180°-Korrektur. Die sichtbaren Mesh-Arme wechseln bei aggressiver KI in eine Bogen-Zielpose, während das vorhandene `ItemInHandLayer` den Bogen rendert. Die gemessene Form füllt 99–100 % der registrierten **1,10 × 1,95-Blöcke-Hitbox**. Umgebung, Treffer, Tod und Eis-Salve verwenden eine eigene vierteilige Frost-Klangfamilie.
- Web Cave Spider: Das Laufzeit-Mesh enthält exakt **97’234 Dreiecke**, 4096 × 4096 Textur und acht Beinregionen. Eine kontinuierliche diagonale Achtbein-Laufverformung bewegt die Beinspitzen stark und fällt am Körper weich auf null ab, damit keine Schnitte zwischen ungewichteten Tripo-Regionen entstehen. Die frühere 0,70-Block-Darstellung wurde mechanisch als zu klein erkannt: Der Renderer skaliert das exakte Mesh nun auf **1,26 Blöcke sichtbare Breite**, die passende Registry-Hitbox misst **1,30 × 0,56 Blöcke**, und der Schatten wurde gemeinsam auf 0,65 angepasst. Umgebung, Treffer, Tod und Netzangriff besitzen eine eigene vierteilige Web-/Chitin-Klangfamilie; Vanilla-Spinnen bleiben unverändert. Ein frischer Client mit `F3+B` bestätigt die geschlossene Geometrie, Bodenlage und passende Hitbox. Bei Nacht verliess der Spider seine Ausgangsposition, verfolgte den Spieler und führte seinen Nahkampfangriff samt Effekten erfolgreich aus. Die gezielten Mesh-/Renderer-/Soundtests und der vollständige Build sind grün.
- Coral Drowned: Die vollständige Benutzerquelle `coral_drowned_textured_4k_v5_user_export.glb` bleibt archiviert; das aktive `coral_drowned_runtime_optimized_4k.glb` besitzt **102’563 Dreiecke**, sechs Regionen und dieselbe 4096 × 4096-Textur. Der regenerierte Export stimmt bereits mit der Minecraft-Vorwärtsachse überein und benötigt keine 90°-Rendererrotation. Die kontinuierliche Humanoid-Verformung bewegt Arme und Beine im Wasser; im aggressiven Zustand wechseln die sichtbaren Mesh-Arme zusätzlich in die Angriffsstellung. Die gemessene Form bleibt innerhalb der registrierten **1,40 × 1,95-Blöcke-Hitbox**. Umgebung, Treffer, Tod und Korallen-Surge besitzen eine eigene vierteilige Unterwasser-/Korallen-Klangfamilie.
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
- Zur Reduktion der CPU- und Garbage-Collector-Last bleibt die vollständige Oberflächenverformung innerhalb von 12 Blöcken aktiv. Weiter entfernte Entitäten zeigen dasselbe Performance-Mesh und dieselbe 4K-Textur statisch; der Renderer verwendet dabei eine wiederverwendete Normaleninstanz statt für jedes Dreieck ein neues Java-Objekt anzulegen.
- Die Vertex-Ausgabe und sämtliche nahen Verformungsfelder arbeiten nun ebenfalls mit wiederverwendeten Positions-, Kanten- und Normalenvektoren. Die vorherige Implementierung erzeugte pro sichtbarem Dreieck mehrere temporäre `Vector3f`-/`Vector4f`-Objekte; bei einem 734’000-Dreieck-Mob waren das mehrere Millionen kurzlebige Objekte pro Bild. Die neue Berechnung verwendet dieselben Matrizen, UVs, Normalen und Dreiecke ohne diese bildweisen Objektallokationen.
- Die Zusammenhangsprüfung über alle 14 aktiven Tripo-Quellen fand eine einzige grosse, räumlich abgetrennte Fremdkomponente: 49’098 Dreiecke im Eisbär-Export. Sie wird reproduzierbar entfernt. Die übrigen grossen Oberflächen sind zusammenhängend; kleine innere Allay-Komponenten bleiben als Kern-/Modelldetails erhalten.

## Vollständige Qualitätsprüfung vom 24. August 2026

- `python tools/verify_umr_project_truth.py`: bestanden; der aktive Corrupted-Silverfish-Spielstand besitzt weiterhin 8 Bones, 0 Cubes, 7 Regionen, 101’723 Dreiecke und eine 4096 × 4096-Textur.
- Gesamte Python-Suite nach der Performance-, Renderer- und Komponentenprüfung: **507 bestanden, 4 bewusst übersprungen, 0 Fehler**.
- Forge-Produktionsbuild: `BUILD SUCCESSFUL`; das neu erzeugte `usless_mobs-1.0.0-alpha.2.jar` enthält 14 Exact-Meshes und 14 zugehörige Exact-Texturen.
- Ein vollständig neu gestarteter Forge-Client lud die bestehende Modellgalerie mit `F3+B`. Die bereinigte Eisbär-Laufzeitressource zeigte keine seitlich schwebende Fremdkomponente mehr; die registrierten Hitboxen, Texturen und Boss-Skalierungen wurden geladen.
- `latest.log` und `debug.log` enthalten im frischen Lauf keine UMR-Render-, Mesh-, Textur-, Registry- oder Ressourcenfehler.
- Alle 14 aktiven Exact-Meshes liegen nun zwischen **84’682 und 102’845 Dreiecken**. Zusammen umfassen sie **1’362’210 Dreiecke und rund 78 MiB Meshdaten** statt zuvor 6’228’107 Dreiecken und 356,5 MiB. Das reduziert Geometrie- und Meshspeicher um ungefähr 78 %, ohne die 4K-Albedo zu verkleinern.
- Die 14 Laufzeittexturen benötigen als unkomprimierte 4096 × 4096-RGBA-Basisstufen weiterhin ungefähr **896 MiB** Grafikspeicher, mit vollständigen Mipmaps theoretisch rund **1,17 GiB**, sofern alle gleichzeitig geladen werden. Eine optionale 2K-Texturstufe wäre der nächste Schritt für Geräte mit wenig Grafikspeicher, ist aber nicht Teil dieser geometrischen Optimierung.
- Der neue Forge-Produktionsbuild ist erfolgreich und erzeugt ein **209,09-MiB-JAR** mit den 14 budgetierten Exact-Meshes und ihren 4K-Texturen. Eine neue visuelle Clientprüfung der Performance-Meshes ist noch offen; ältere Sichtprüfungen gelten nur für die archivierten Vollquellen.

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
- Der Giant-Squid-Boss ist inzwischen eine eigene aktive Laufzeitentität mit Registry, Attributen, Bossleiste, vier Kampfphasen, Telegraphen, schwierigkeitsabhängigen Werten, persistentem Ruinenbezug und eigenem Exact-Mesh-Renderer. Er ersetzt weder den normalen Squid noch dessen KI.

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

## Modell- und Ocean-Worldgen-Prüfung vom 24. August 2026

- Blockbench Desktop wurde mit `Corrupted Silverfish v5 Tripo Animated.bbmodel` geöffnet. Das vorhandene `.bbmodel` zeigt die 4096 × 4096-Textur und das zusammenhängende Referenzmodell korrekt. Die installierte Blockbench-Instanz kann die aktiven Tripo-GLBs ohne zusätzliches Import-Plugin nicht direkt öffnen; sie meldet bei `axolotl_textured_4k_v2.glb` ausdrücklich ein nicht unterstütztes Format. Deshalb bleiben Exact-Runtime-Tests und der Forge-Client die Wahrheit für die 14 GLB-basierten Laufzeitmodelle.
- Die fokussierte Gesamtprüfung bestand mit **72/72** Exact-Runtime-/Komponentenprüfungen sowie **45/45** Projekt-Truth-, Renderer-, Ausrichtungs-, Grössen- und Hitbox-Verträgen. Der aktuelle Client lud die Modellgalerie ohne UMR-Mesh-, Textur- oder Registryfehler; der dokumentierte Überblick liegt in `run/screenshots/2026-08-24_22.59.53.png`.
- Die UV-/Texturprüfung bestätigt bei 13 Modellen eine vollständige 1:1-Übernahme. Beim Eisbären werden ausschliesslich die bereits verifizierten **49’098** Dreiecke der schwebenden Fremdkomponente entfernt: 684’939 Quelldreiecke, 635’841 Runtime-Dreiecke, identischer 4K-Pixelhash und exakte UVs aller behaltenen Flächen. Der Prüfer erlaubt jetzt genau diese im Report dokumentierte Entfernung und lehnt jede weitere Dreiecks-, UV- oder Texturabweichung weiterhin ab.
- TerraBlender Forge **1.20.1-3.0.1.10** ist eine verpflichtende BOTH-side-Abhängigkeit. `usless_mobs:deep_ocean` wird auf seltene Deep-Ocean-Oberflächenpunkte gelegt; `usless_mobs:big_underwater_cave` wird auf unterirdische Deep-Ocean-Klimapunkte gelegt. Die Region ersetzt weder Vanilla-Noise-Settings noch andere Overworld-Biome.
- `worldgen.oceanBiomeRegionWeight` ist als Common-Config von 0 bis 10 definiert, Standard 1. Der Wert 0 deaktiviert die Registrierung der UMR-Ocean-Region.
- `usless_mobs:big_underwater_cave` erzeugt begrenzte Ellipsoidkammern mit horizontal 10–18 und vertikal 6–10 Blöcken Radius. Der Generator arbeitet mindestens 14 Blöcke unter dem Meeresspiegel, ersetzt weder Bedrock noch Block-Entities und füllt geeigneten Innenraum mit Wasser; Prismarin, Kalzit, Dark Prismarine und seltene Sea Lanterns bilden Navigationspunkte.
- Eine vollständig frische Creative-Welt wurde mit dem neuen Stand erzeugt und bis ins Spiel geladen. `latest.log` bestätigt `Registered region usless_mobs:ocean_region` und die erfolgreiche TerraBlender-Initialisierung des Overworld-Biome-Sources ohne UMR-Ressourcenfehler. Der Client wurde danach über `Save and Quit to Title` sauber beendet.
- `python -m pytest tools/tests/test_ocean_worldgen.py -q`: **5 bestanden**. Die zusätzlichen Diagnose-Regressionstests bestehen mit **3 bestanden**. `python tools/verify_umr_project_truth.py` bleibt grün. Der abschliessende Forge-Produktionsbuild ist `BUILD SUCCESSFUL`.
- Die Ancient Whale Ruin wurde in einer frischen Welt per `/locate structure usless_mobs:ancient_whale_ruin` bei ungefähr `-480/-448` gefunden und im echten Client kontrolliert. Ein anschliessender read-only Anvil-Scan derselben gespeicherten Welt weist beide UMR-Biome direkt in den tatsächlich erzeugten Chunkdaten nach: `usless_mobs:big_underwater_cave` liegt im geprüften Bereich auf Y **-64 bis 23**, `usless_mobs:deep_ocean` auf Y **28 bis 47**. Damit sind Untergrund- und Oberflächenklimapunkte getrennt gespeichert und nicht nur registriert.
- Die erzeugte Big Underwater Cave um ungefähr `-494/-25/-405` enthält in ihrer geprüften Kammer **4’897 Wasserblöcke**, **14 Prismarin**, **2 Dark Prismarine**, **8 Kalzit** und **1 Sea Lantern**. Der echte Client lud den Spieler an diesem Punkt in die überflutete Prismarinkammer und zeigte dort einen lebenden Unterwasser-Mob. `latest.log` enthält dabei keine UMR-Worldgen-, Biome-, Registry-, Modell- oder Texturfehler; nur die bekannte bedeutungslose Realms-Authentifizierungsinformation. Der Client speicherte sauber und `runClient` endete mit `BUILD SUCCESSFUL`.
- Minecrafts eigener `locate biome`-Befehl bestätigt beide Registrierungen zusätzlich im lokalen Dedicated-Server-Lauf: `usless_mobs:deep_ocean` wurde bei `[-256, 64, -1776]` in **1’785 Blöcken** Entfernung gefunden, `usless_mobs:big_underwater_cave` bei `[-192, 0, 208]` in **465 Blöcken** Entfernung. Der Serverlog misst dafür 1’420 ms beziehungsweise 73 ms. Der Server wurde danach über seinen normalen `stop`-Befehl gespeichert, `runServer` endete mit `BUILD SUCCESSFUL`, und die temporäre lokale RCON-Konfiguration wurde vollständig zurückgesetzt.

## Ancient Whale Ruin und Giant Squid vom 25. August 2026

- `usless_mobs:ancient_whale_ruin` ist eine registrierte, auffindbare Struktur für `usless_mobs:deep_ocean`. Ihr prozedurales, chunk-sicher begrenztes Stück misst 49 × 17 × 45 Blöcke und enthält eine überflutete Prismarinruine, zwei grosse Wal-Skelette aus Knochenblöcken, Durchgänge, Beleuchtung und sieben geschützte Goldblöcke im Zentrum.
- Der erste Abbauversuch eines zentralen Goldblocks aktiviert genau einen persistenten Encounter. Die Struktur-ID, Aktivierung, Boss-UUID und der Siegzustand werden als `SavedData` gespeichert. Bis zum Sieg bleiben die Goldblöcke geschützt; danach sind sie normal abbaubar.
- Die Aktivierung erzeugt Dunkelheit, Tinte, einen hörbaren Tiefenimpuls und eine einwärts gerichtete Strömung. Der Giant Squid besitzt eine 5,80 × 3,20-Blöcke-Hitbox, 360 Basisleben sowie die Phasen `STALKING`, `HUNT`, `RUIN_COLLAPSE` und `DESPERATION` mit angekündigten Tinten-, Greif-, Strömungs- und Dash-Angriffen.
- Der Boss verwendet das aus dem freigegebenen Squid-Quellmesh abgeleitete aktive Performance-Mesh mit **102’845 Dreiecken**, **4096 × 4096 Textur**, acht Armen und zwei langen Fangtentakeln. Die vollständige 734’627-Dreieck-Quelle bleibt archiviert. Er ist eine eigene Entität und verändert den normalen Living Squid nicht.
- `test_ocean_worldgen.py`, `test_ancient_whale_ruin.py`, `test_giant_squid_boss.py` sowie die Projekt-Truth-, Exact-Mesh- und Renderer-Verträge bestehen gemeinsam mit **55 bestandenen Tests**. `compileJava` und der vollständige Forge-Build sind erfolgreich.
- Eine frische Clientwelt sowie ein frischer Dedicated Server starteten mit der neuen Registry und TerraBlender bis zum vollständig geladenen Overworld ohne UMR-Worldgen-, Struktur- oder Entityfehler.
- Die Ruine wurde im Client 717 Blöcke vom Spawn entfernt gefunden. Der gespeicherte Chunk bestätigt sieben Goldblöcke auf Y 46 und **176 Knochenblöcke** in zwei getrennten Wal-Skeletten. Überflutete Prismarinräume, Innenwände und die zentrale Goldkammer waren sichtbar.
- Ein präziser Abbauversuch am Gold löste Dunkelheit, Tinten-/Strömungseffekte und die violette `Giant Squid`-Bossleiste aus. Nach vollständigem Beenden und erneutem Laden der Welt blieb der Encounter aktiv; `SavedData` enthielt weiterhin dieselbe Boss-UUID und `Defeated=0`.
- Für den Siegtest tötete ein ausschliesslich in der Wegwerf-Testwelt geladener temporärer Datapack den vorhandenen Boss über den normalen Minecraft-`kill`-Befehl. Der echte Entity-Tod setzte danach `Active=0`, `Defeated=1`, entfernte die Boss-UUID und liess die Bossleiste verschwinden. Der Datapack wurde unmittelbar danach wieder entfernt.
- Nach dem Sieg konnte ein zuvor geschützter Goldblock im Client abgebaut werden. Der anschliessende Anvil-Scan bestätigte **sechs** verbleibende Goldblöcke. Der komplette Locate-/Aktivieren-/Speichern-/Reload-/Besiegen-/Freigeben-Zyklus ist damit beobachtet und mechanisch bestätigt.

## Vollständige Entity-, Modell-, Animations- und Angriffsmatrix vom 25. August 2026

- Die Prüfung umfasst beide aktiven Registries: **22 lebende Entitäten** und **2 Projektil-/Item-Entities**. Neben den 20 Haupt-Entities sind damit auch Soul Endermite, Void Reaper und die geworfene Sense ausdrücklich erfasst.
- Für sämtliche registrierten Entities sind Renderer und lokalisierte Namen vorhanden; alle lebenden Entities besitzen registrierte Attribute. Alle 20 normal per Kreativinventar erzeugbaren Haupt-Mobs besitzen Spawn-Ei, Itemmodell und deutsche/englische Namen. Der Soul Endermite besitzt sein separates Spawn-Ei; der Void Reaper bleibt absichtlich an seinen Void-Beschwörer gebunden.
- Alle **14 aktiven Exact-Meshes** behalten ihre geprüften Dreiecke, UVs und Texturen. Ihre sichtbaren, individuell skalierten Abmessungen liegen innerhalb der registrierten Hitboxen. Die zusätzliche Giant-Squid-Kombination aus 1,80-facher Exact-Layer-Skalierung und 2,23-facher Boss-Skalierung ergibt aus dem verifizierten 22,4-Pixel-Quellspan ungefähr **5,62 sichtbare Blöcke** innerhalb der 5,80 × 3,20-Blöcke-Hitbox.
- Beim Helping Allay wurde ein echter Laufzeitfehler gefunden: Die vorhandene Aktionslogik berechnete Posen, der Renderer ersetzte sie jedoch immer durch `BonePose.ZERO`. Das exakte Mesh verwendet nun ein kontinuierliches, positionsbasiertes Bewegungsfeld für Flügel, Arme, Kopf und Seelenkern. Dadurch funktionieren Teleport-, Schild-, Heil-, Enthüllungs- und Bindungsaktionen ohne Risse an ungewichteten Tripo-Regionsgrenzen. Ab 12 Blöcken bleibt die statische Exact-Mesh-LOD aktiv.
- Die vollständige Angriffsmatrix klassifiziert **16 feindliche beziehungsweise Boss-Entities** mit eigenem Nahkampf-, Fernkampf-, Effekt- oder Spezialangriffsweg. Helping Allay, Living Squid, Living Glow Squid, Living Polar Bear, Living Axolotl und Living Ocelot sind ausdrücklich passive/helfende Tiere und erhalten deshalb keine erfundene Kampf-KI.
- Soul Endermite und Void Reaper besitzen gültige GeckoLib-Geometrien, Texturen und Animationsdateien. Jeder von Java angeforderte Clip existiert, und jeder in einem Clip bewegte Bone ist im zugehörigen Modell vorhanden. Der Void Reaper behält seine sieben Aktionsanimationen und Spezialangriffe; der Soul Endermite seinen Bewegungsclip und Nahkampfeffekt.
- Die zuvor fehlende Kreativtab-Freigabe des Corrupted-Silverfish-Spawn-Eis ist ergänzt. Der Giant Squid besitzt jetzt ebenfalls ein eigenes episches Spawn-Ei samt Itemmodell und deutscher/englischer Lokalisierung.
- Die fokussierte Exact-Mesh-/Allay-Prüfung besteht mit **41/41 Tests**, die gezielte Kampf-/Bossprüfung mit **35/35 Tests** und die erweiterte Entity-Matrix mit **5/5 Tests**. `verify_umr_project_truth.py` bleibt grün: 8 Bones, 0 Cubes, 7 Regionen, 101’723 Dreiecke und 4096 × 4096 Textur für den aktiven Corrupted Silverfish.
- Die aktuelle Java-Kompilierung ist nicht am Quellcode, sondern vor der Compile-Phase an der externen Auflösung von `net.minecraft:client:1.20.1` gescheitert: Sämtliche konfigurierten Maven-Endpunkte brachen die Verbindung beim fehlenden `client-1.20.1-extra.jar` ab. Deshalb wird für diesen Stand kein neuer Clientlauf behauptet, bis das Artefakt wieder erreichbar ist.
