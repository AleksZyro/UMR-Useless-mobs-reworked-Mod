# All Armor Skin System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Alle 18 Rüstungsteile erhalten deterministische, materialreiche Worn- und Itemtexturen in der Qualitätsrichtung des Corrupted Silverfish.

**Architecture:** Ein gemeinsamer Python-Generator enthält kleine, testbare Pixelprimitive und sechs getrennte Familienrezepte. Bestehende Java-Geometrie und JSON-Modelle bleiben unverändert; Tests sichern Dateivertrag, Materialkontrast, UV-/Alpha-Grenzen, Determinismus und atomische Publikation.

**Tech Stack:** Python 3, Pillow, `unittest`, Forge 1.20.1, Java 17, PNG/RGBA.

---

### Task 1: Ausgabe- und Materialvertrag test-first definieren

**Files:**
- Modify: `tools/armor_graphics/tests/test_armor_graphics.py`
- Create: `tools/armor_graphics/build_all_armor_skins.py`

- [ ] Schreibe fehlschlagende Tests für die 11 Worn-Texturen und 18 Itemtexturen, ihre Dimensionen und RGBA-Modi.
- [ ] Ergänze fehlschlagende Tests für fünf Materialrollen, familienrichtige Akzentfarben, Alpha-Silhouetten und eine Obergrenze für zusammenhängende gleichfarbige Regionen.
- [ ] Führe die neuen Tests aus und bestätige den erwarteten RED-Status wegen fehlendem Generatorvertrag.
- [ ] Implementiere nur Pfadmanifest, Paletten-Dataclasses und eine In-Memory-Ausgabeschnittstelle.
- [ ] Führe die Vertragstests erneut aus und committe den grünen Grundvertrag.

### Task 2: Sichere gemeinsame Pixelprimitive implementieren

**Files:**
- Modify: `tools/armor_graphics/build_all_armor_skins.py`
- Modify: `tools/armor_graphics/tests/test_armor_graphics.py`

- [ ] Schreibe fehlschlagende Tests für Plattenrand, Fugenschatten, gerichtetes Licht, kontrollierte 1–3-Pixel-Cluster und Kristall mit vier Stufen.
- [ ] Implementiere `paint_panel`, `paint_cluster`, `paint_crystal` und `paint_icon_silhouette` ohne Zufallsquelle.
- [ ] Schreibe Fehlerpfadtests für eindeutige temporäre Dateien, Target-Erhalt sowie Cleanup bei Write-/Replace-Fehlern.
- [ ] Implementiere atomische Mehrdatei-Publikation mit vollständigem Rollback.
- [ ] Führe die fokussierten Tests aus und committe die gemeinsamen Primitive.

### Task 3: True Void als Referenzfamilie detaillieren

**Files:**
- Modify: `tools/armor_graphics/build_all_armor_skins.py`
- Modify: `tools/armor_graphics/tests/test_armor_graphics.py`
- Modify: `src/main/resources/assets/usless_mobs/textures/models/armor/true_void_layer_1.png`
- Modify: `src/main/resources/assets/usless_mobs/textures/models/armor/true_void_layer_2.png`
- Modify: `src/main/resources/assets/usless_mobs/textures/models/armor/true_void_chestplate_layer_1.png`
- Modify: `src/main/resources/assets/usless_mobs/textures/item/true_void_helmet.png`
- Modify: `src/main/resources/assets/usless_mobs/textures/item/true_void_chestplate.png`
- Modify: `src/main/resources/assets/usless_mobs/textures/item/true_void_leggings.png`
- Modify: `src/main/resources/assets/usless_mobs/textures/item/true_void_boots.png`

- [ ] Schreibe RED-Tests für Obsidian-Materialstufen, Amethystkanten, Void-Runen und vierstufige Kristallkerne.
- [ ] Implementiere die drei Worn-Rezepte und vier Itemrezepte unter Beibehaltung der vorhandenen V-Geometrie.
- [ ] Erzeuge die sieben PNGs zweimal und prüfe byteidentische Ausgaben.
- [ ] Erzeuge ein Void-Kontaktblatt und kontrolliere grosse schwarze Flächen sowie Kristalllesbarkeit.
- [ ] Committe die Referenzfamilie.

### Task 4: Celestial und Living umsetzen

**Files:**
- Modify: `tools/armor_graphics/build_all_armor_skins.py`
- Modify: `tools/armor_graphics/tests/test_armor_graphics.py`
- Modify: `src/main/resources/assets/usless_mobs/textures/models/armor/true_celestial_layer_1.png`
- Modify: `src/main/resources/assets/usless_mobs/textures/models/armor/true_celestial_layer_2.png`
- Modify: `src/main/resources/assets/usless_mobs/textures/models/armor/true_living_layer_1.png`
- Modify: `src/main/resources/assets/usless_mobs/textures/models/armor/true_living_layer_2.png`
- Modify: `src/main/resources/assets/usless_mobs/textures/item/true_celestial_*.png`
- Modify: `src/main/resources/assets/usless_mobs/textures/item/true_living_*.png`

- [ ] Schreibe RED-Tests für Silber/Gold/Cyan sowie Holz/Moos/Lebensadern.
- [ ] Implementiere beide Worn-Rezepte mit flächenspezifischer Beleuchtung.
- [ ] Implementiere je vier slotlesbare Item-Silhouetten.
- [ ] Prüfe Determinismus, Farbfamilien und Kontaktblätter.
- [ ] Committe Celestial und Living als getrennte atomische Änderung.

### Task 5: Balance, Corrupted Crystal und Schleimreaktor umsetzen

**Files:**
- Modify: `tools/armor_graphics/build_all_armor_skins.py`
- Modify: `tools/armor_graphics/tests/test_armor_graphics.py`
- Modify: `src/main/resources/assets/usless_mobs/textures/models/armor/true_balance_layer_1.png`
- Modify: `src/main/resources/assets/usless_mobs/textures/models/armor/true_balance_layer_2.png`
- Modify: `src/main/resources/assets/usless_mobs/textures/models/armor/corrupted_crystal_layer_2.png`
- Modify: `src/main/resources/assets/usless_mobs/textures/models/armor/schleimreaktor_layer_1.png`
- Modify: `src/main/resources/assets/usless_mobs/textures/item/armor_of_balance_*.png`
- Modify: `src/main/resources/assets/usless_mobs/textures/item/corrupted_crystal_leggings.png`
- Modify: `src/main/resources/assets/usless_mobs/textures/item/schleimreaktor_brustpanzer.png`

- [ ] Schreibe RED-Tests für spiegelgeteilte Balance-Flächen, Silverfish-nahe Chitin/Korruptions-Kontraste und begrenzte Reaktorkanäle.
- [ ] Implementiere die vier Worn-Rezepte.
- [ ] Implementiere die sechs Itemrezepte.
- [ ] Prüfe Symmetrie, Materialrollen, Alpha und Determinismus.
- [ ] Committe die drei Familien getrennt nach überprüfbarem Ergebnis.

### Task 6: Integration und echte visuelle Abnahme

**Files:**
- Create: `Modelle/Exports/armor_graphics_review/all_armor_skins_contact.png`
- Modify: `tools/armor_graphics/tests/test_armor_graphics.py`

- [ ] Prüfe alle 29 PNGs gegen den vollständigen Vertrag und gegen ihre Item-Model-Bindungen.
- [ ] Führe `python -m unittest tools.armor_graphics.tests.test_armor_graphics -v` aus; erwartet werden nur grüne Tests und der dokumentierte UI-Baseline-Skip.
- [ ] Führe `gradlew.bat compileJava --rerun-tasks` aus; erwartet wird `BUILD SUCCESSFUL`.
- [ ] Lade die Assets im echten Forge-Client neu und erfasse je Familie eine klare Worn-/Inventory-Ansicht.
- [ ] Prüfe Kontaktblatt und echte Clientbilder auf flache Flächen, fehlende Texturen, Schnitte und abgeschnittene Icons.
- [ ] Führe `git diff --check` und einen Scope-Diff aus; committe nur Generator, Tests, die 29 Texturen und Reviewbilder.

