# Ozelot Exact Model Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Einen eigenen UMR-Ozelot mit konsistentem Vierseiten-Konzept, unverändertem texturiertem 4K-Tripo-Mesh, nahtsicherer Katzenanimation, passender Hitbox und eigenem Spawn-Ei ins Spiel integrieren.

**Architecture:** Der Bildgenerator liefert nur die vier Referenzansichten. Tripo erzeugt daraus nach separater Creditfreigabe das texturierte GLB; `tools/mob_tripo/exact_runtime.py` übernimmt jede Quellfläche genau einmal in das vorhandene UMR-Meshformat. Eine eigene `LivingOcelotEntity` verwendet den Vanilla-Ozelot als Verhaltensbasis, während ein eigener Renderer ausschliesslich die exakte Tripo-Oberfläche zeichnet.

**Tech Stack:** Minecraft Forge 1.20.1, Java 17, Python 3.9, pytest, UMR ExactMobMesh, OpenAI-Bildgenerator, Tripo Multi-View.

---

### Task 1: Konsistentes Vierseiten-Konzept

**Files:**
- Create: `Modelle/Exports/ocelot_v1/concept/ocelot_multiview.png`
- Create: `Modelle/Exports/ocelot_v1/tripo_input/front.png`
- Create: `Modelle/Exports/ocelot_v1/tripo_input/left.png`
- Create: `Modelle/Exports/ocelot_v1/tripo_input/back.png`
- Create: `Modelle/Exports/ocelot_v1/tripo_input/right.png`

- [x] **Step 1: Vierseitenblatt generieren**

Prompt: `Exakt derselbe kräftige Minecraft-Ozelot in vier getrennten orthografischen Ansichten: vorne, links, hinten, rechts. Goldorange Felltextur, dunkle Rosetten, heller Bauch und helle Schnauze, smaragdgrüne Augen, langer Katzenschwanz, grosse Ohren. Hochwertige blockige Minecraft-3D-Pixelästhetik. Alle Proportionen und Fellzeichnungen müssen zwischen den Ansichten identisch sein. Echter transparenter Hintergrund, kein Boden, kein Schatten, kein Text, keine Requisiten, keine Rüstung.`

- [x] **Step 2: Bild visuell prüfen**

Erwartet: vier vollständige, nicht abgeschnittene Ansichten; gleiche Ohren, Schwanzlänge, Beinlänge und Rosettenpositionen.

- [x] **Step 3: Einzelansichten verlustfrei ausschneiden**

Run: `python tools/mob_tripo/prepare_explicit_multiview.py Modelle/Exports/ocelot_v1/concept/ocelot_multiview.png Modelle/Exports/ocelot_v1/tripo_input`

Das erzeugte Blatt enthält überlappende, ungleich grosse Ansichtsbereiche. Das Spezialwerkzeug verwendet deshalb vier explizite Ausschnitte, entfernt nur den vom Bildrand erreichbaren hellen Hintergrund und skaliert alle vier Tiere mit demselben Faktor. Dadurch bleiben weisse Fellbereiche sowie die vollständigen Schwänze erhalten.

Ergebnis: vier visuell geprüfte RGBA-PNGs mit `1024 × 1024`, transparenten Ecken, vollständigen Tieren und korrekter Zuordnung `FRONT/LEFT/BACK/RIGHT`.

### Task 2: Tripo-Quelle erzeugen und Provenienz sichern

**Files:**
- Create: `Modelle/Exports/ocelot_v1/source/ocelot_textured_4k.glb`
- Create: `Modelle/Exports/ocelot_v1/source/provenance.json`

- [x] **Step 1: Multi-View-Vorschau laden**

In Tripo `front.png`, `left.png`, `back.png` und `right.png` den entsprechenden Slots zuweisen. Vorne darf nicht mit hinten vertauscht sein.

- [x] **Step 2: Kosten-Gate prüfen**

Sichtbarer Preis am 23. August 2026: **55 Credits** für `HD-Modell`, AI-Modell `v3.1 – Beste Qualität`. Die vier Slots sind geprüft als `Vorne/Links/Rechts/Zurück`. Ohne neue ausdrückliche Freigabe keinen Generieren-, Textur-, Retopology- oder Export-Schritt auslösen.

- [x] **Step 3: Nach Freigabe 4K-Modell exportieren**

Einstellungen: Multi-View, PBR-Textur, Texture Quality 4K, keine Neuinterpretation durch einen zusätzlichen Prompt.

- [x] **Step 4: Provenienz schreiben**

Run: `Get-FileHash Modelle/Exports/ocelot_v1/source/ocelot_textured_4k.glb -Algorithm SHA256`

Die ausgegebene Prüfsumme und die UUID aus der geöffneten Tripo-Projekt-URL werden wörtlich in `provenance.json` eingetragen. Die übrigen festen Felder lauten `source: tripo_multiview`, `texture: 4096x4096`, `front_axis: -Z` und `approved_concept: ../concept/ocelot_multiview.png`. Die Datei wird erst gespeichert, wenn beide dynamischen Werte vorhanden sind; dadurch enthält sie nie Platzhalterdaten.

### Task 3: Exakten Ozelot-Konverter testgetrieben ergänzen

**Files:**
- Modify: `tools/mob_tripo/exact_runtime.py`
- Modify: `tools/mob_tripo/tests/test_exact_runtime.py`

- [x] **Step 1: Fehlenden Ozelot-Vertrag testen**

```python
def test_ocelot_spec_is_a_seven_region_quadruped():
    spec = MOB_SPECS["ocelot"]
    assert spec.bones == (
        "body", "head", "tail", "leg_front_left", "leg_front_right",
        "leg_rear_left", "leg_rear_right",
    )
    assert spec.classifier == "ocelot"
    assert spec.fit_axis == 2
    assert spec.fit_span == 23.2
```

- [x] **Step 2: Test rot ausführen**

Run: `python -m pytest tools/mob_tripo/tests/test_exact_runtime.py::ExactRuntimeTest::test_ocelot_spec_is_a_seven_region_quadruped -q`

Erwartet: FAIL mit fehlendem Schlüssel `ocelot`.

- [x] **Step 3: Spezifikation und Klassifizierung ergänzen**

```python
"ocelot": MobSpec(
    ("body", "head", "tail", "leg_front_left", "leg_front_right", "leg_rear_left", "leg_rear_right"),
    "ocelot", fit_axis=2, fit_span=23.2,
),
```

Die Klassifizierung trennt Kopf am vorderen Ende, Schwanz am hinteren Ende und vier Beine über Y-Höhe, X-Seite und Z-Station. Keine Fläche darf verloren gehen.

- [x] **Step 4: Exakte Laufzeitressourcen bauen**

Run: `python tools/mob_tripo/exact_runtime.py --mob ocelot --source Modelle/Exports/ocelot_v1/source/ocelot_textured_4k.glb --write-runtime`

Erwartet: `ocelot.mesh`, `ocelot.report.json` und `ocelot.png`; `source_triangles == output_triangles`, `cubes == 0`, Textur `4096 × 4096`.

### Task 4: Eigene Entität, Spawn-Ei und Renderer

**Files:**
- Create: `src/main/java/com/Momik/usless_mobs/entity/LivingOcelotEntity.java`
- Create: `src/main/java/com/Momik/usless_mobs/client/LivingOcelotRenderer.java`
- Modify: `src/main/java/com/Momik/usless_mobs/registry/ModEntities.java`
- Modify: `src/main/java/com/Momik/usless_mobs/registry/ModItems.java`
- Modify: `src/main/java/com/Momik/usless_mobs/registry/ModCreativeTabs.java`
- Modify: `src/main/java/com/Momik/usless_mobs/Usless_mobs.java`
- Modify: `src/main/resources/assets/usless_mobs/lang/de_de.json`
- Modify: `src/main/resources/assets/usless_mobs/lang/en_us.json`
- Test: `tools/tests/test_dedicated_tripo_entities.py`

- [x] **Step 1: Fehlenden dedizierten Ozelot testen**

```python
def test_ocelot_is_dedicated_and_does_not_replace_vanilla_renderer(self):
    assert 'register("living_ocelot"' in ENTITIES
    assert "class LivingOcelotEntity extends Ocelot" in OCELOT_ENTITY
    assert "ModEntities.LIVING_OCELOT.get(), LivingOcelotRenderer::new" in SETUP
    assert "registerEntityRenderer(EntityType.OCELOT" not in SETUP
    assert "LIVING_OCELOT_SPAWN_EGG" in ITEMS
```

- [x] **Step 2: Minimale Entität implementieren**

```java
public final class LivingOcelotEntity extends Ocelot {
    public LivingOcelotEntity(EntityType<? extends Ocelot> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder createAttributes() {
        return Ocelot.createAttributes().add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D).add(Attributes.ATTACK_DAMAGE, 5.0D);
    }
}
```

- [x] **Step 3: Registry und Renderer anbinden**

Die gemessene Meshgrösse wird in `.sized(width, height)` übernommen. `LivingOcelotRenderer` löscht Vanilla-Layer und fügt `ExactMobMeshLayer` mit Variante `OCELOT` und transparenter Basistexur hinzu.

- [x] **Step 4: Tests ausführen**

Run: `python -m pytest tools/tests/test_dedicated_tripo_entities.py -q`

Erwartet: alle Verträge grün.

### Task 5: Nahtsichere Katzenanimation

**Files:**
- Modify: `src/main/java/com/Momik/usless_mobs/client/CustomMob3DModel.java`
- Modify: `src/main/java/com/Momik/usless_mobs/client/CustomMobModelLayers.java`
- Modify: `src/main/java/com/Momik/usless_mobs/client/ExactMobMeshLayer.java`
- Modify: `src/main/java/com/Momik/usless_mobs/client/ExactMobMesh.java`
- Modify: `tools/tests/test_exact_mesh_alignment.py`

- [x] **Step 1: Animationsvertrag rot schreiben**

```python
def test_ocelot_has_continuous_leg_tail_and_pounce_deformation():
    assert "void renderOcelotBone(" in MESH
    assert "deformOcelotVertex(" in MESH
    assert "float tailWeight" in MESH
    assert "float legWeight" in MESH
    assert "CustomMob3DModel.Variant.OCELOT" in LAYER
```

- [x] **Step 2: Positionsfeld implementieren**

Das Feld bewegt vier Pfoten gegenphasig, senkt den Körper beim Schleichen, streckt ihn beim Sprung und lässt den Schwanz weich gegensteuern. Identische Quellpositionen erhalten unabhängig von ihrer Region identische Transformationen.

- [x] **Step 3: Mesh- und Hitboxverträge ausführen**

Run: `python -m pytest tools/tests/test_exact_mesh_alignment.py -q`

Erwartet: keine Schnitte und Hitbox umschliesst die skalierte statische Meshgrenze.

### Task 6: Bestehendes Ozelot-Gameplay auf die neue Entität begrenzen

**Files:**
- Modify: `src/main/java/com/Momik/usless_mobs/event/LivingMobReworkHandler.java`
- Modify: `src/main/java/com/Momik/usless_mobs/event/LivingDropsHandler.java`
- Test: `tools/tests/test_dedicated_tripo_entities.py`

- [x] **Step 1: Gameplay-Vertrag schreiben**

```python
assert "instanceof LivingOcelotEntity" in HANDLER
assert "EntityType.OCELOT" not in OCELOT_DROP_BRANCH
```

- [x] **Step 2: Rekrutierung und Jagdmechanik übertragen**

Nur `LivingOcelotEntity` erhält Eigentümerbindung, Zielmarkierung, Teleport zum Besitzer und Sprungangriff. Vanilla-Ozelots bleiben unverändert.

- [x] **Step 3: Gameplay-Tests ausführen**

Run: `python -m pytest tools/tests/test_dedicated_tripo_entities.py -q`

Erwartet: eigene Entität nutzt das vorhandene Gameplay; Vanilla-Renderer und Vanilla-Verhalten bleiben unangetastet.

### Task 7: Abschlussprüfung und Dokumentation

**Files:**
- Modify: `docs/UMR_ACTIVE_PROJECT_STATE.md`

- [x] **Step 1: Automatische Prüfungen ausführen**

Run: `python tools/verify_umr_project_truth.py`

Run: `python -m pytest tools/mob_tripo/tests/test_exact_runtime.py tools/tests/test_exact_mesh_alignment.py tools/tests/test_dedicated_tripo_entities.py`

Run: `.\gradlew.bat compileJava`

Erwartet: Projektwahrheit PASS, alle Tests PASS, `BUILD SUCCESSFUL`.

- [x] **Step 2: Im Spiel prüfen**

Mit Spawn-Ei und `/summon usless_mobs:living_ocelot` testen. `F3+B` aktivieren und Vorderseite, Grösse, Bodenposition, Laufen, Schleichen, Sprung, Schwanz sowie Textur aus mehreren Blickwinkeln prüfen.

Bestanden im frischen Kreativwelt-Durchlauf: Der normale Ozelot wurde mit aktiver KI neben einem Hühnerziel geprüft. Vorder- und Seitenansicht zeigen die geschlossene 4K-Oberfläche, korrekte Bodenlage und Hitbox sowie den Übergang von Lauf-/Schleichhaltung in die gestreckte Sprungpose mit diagonalem Beinwechsel und Schwanz-Gegensteuerung. Im Client-Log trat keine Ozelot-Renderausnahme auf.

- [x] **Step 3: Aktiven Projektstatus aktualisieren**

Projekt-ID, SHA-256, Regionen, Dreiecke, 4K-Textur, Hitbox, Animation, Spawn-Ei und Laufzeitprüfung in `docs/UMR_ACTIVE_PROJECT_STATE.md` eintragen.
