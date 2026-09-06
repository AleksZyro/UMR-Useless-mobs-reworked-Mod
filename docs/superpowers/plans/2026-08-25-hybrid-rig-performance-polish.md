# UMR Hybrid-Rig and Performance Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Frost Stray, Web Cave Spider und Helping Allay erhalten nahtsichere, voneinander unabhängige Körperbewegungen, korrekte Item-Anker, passende Hitboxen und eine messbar günstigere Exact-Mesh-Darstellung.

**Architecture:** Die bestehenden Tripo-Dreiecke und UVs bleiben unverändert. Ein gemeinsamer `ExactRigPose` berechnet pro Entity und Animationsschritt alle trigonometrischen Posewerte einmal; `ExactMobMesh` wendet diese Werte mit weichen positionsbasierten Gewichten ohne neue Objekte im inneren Vertexpfad an. Ein dreistufiger `ExactAnimationLod` steuert volle, zeitlich quantisierte und statische Darstellung. Dedizierte Item-Layer verwenden exakt dieselben Handanker wie die sichtbaren Oberflächen.

**Tech Stack:** Minecraft Forge 1.20.1, Java 17, Tripo Exact-Mesh-Laufzeitformat, Mojang `PoseStack`/`ItemInHandRenderer`, Python 3.9, Pillow, pytest, Gradle 8.8.

---

## Arbeitsregeln

- Ausschliesslich im kanonischen Worktree `slime/.worktrees/corrupted-silverfish-v3` arbeiten.
- Vor jedem Task `python tools/verify_umr_project_truth.py` ausführen.
- Die ungetrackten Tripo-Kandidaten unter `Modelle/Exports/*/source/` sowie `Modelle/Exports/model_qa/hybrid_rig_design.*` weder löschen noch pauschal stagen.
- Neue Verhaltenstests zuerst rot ausführen, danach minimal implementieren und erneut grün ausführen.
- Keine Geometrie voxelisieren, neu erfinden oder UVs neu backen.
- Kein Release erstellen, bevor Task 9 vollständig bestanden ist.

## Dateikarte

- `src/main/java/com/Momik/usless_mobs/client/ExactRigPose.java` — neue vorab berechnete Posewerte und Item-Anker.
- `src/main/java/com/Momik/usless_mobs/client/ExactAnimationLod.java` — neue drei Distanzstufen und Mittelbereichs-Quantisierung.
- `src/main/java/com/Momik/usless_mobs/client/ExactMobMesh.java` — weiche Gewichte und Vertexdeformation ohne innere Allokationen.
- `src/main/java/com/Momik/usless_mobs/client/ExactMobMeshLayer.java` — Auswahl von Pose, LOD, Modellfront und Mobskalierung.
- `src/main/java/com/Momik/usless_mobs/client/ExactHeldItemLayer.java` — neuer sichtbarer Handanker für Exact-Mesh-Mobs.
- `src/main/java/com/Momik/usless_mobs/client/FrostStrayRenderer.java` — Vanilla-Item-Layer entfernen, Exact-Item-Layer einbauen.
- `src/main/java/com/Momik/usless_mobs/client/HelpingAllayRenderer.java` — Vanilla-Item-Layer entfernen, Exact-Item-Layer einbauen.
- `src/main/java/com/Momik/usless_mobs/client/HelpingAllayExactLayer.java` — gemeinsamen Pose-/LOD-Pfad verwenden.
- `src/main/java/com/Momik/usless_mobs/registry/ModEntities.java` — nur mechanisch validierte Hitboxwerte ändern.
- `tools/mob_tripo/exact_runtime.py` — Spider-Kopfregion, Modellfront und Bounds in den Report exportieren.
- `tools/mob_tripo/build_quality_textures.py` — deterministische 2K-Standard- und 4K-Qualitätspaket-Erzeugung.
- `tools/tests/test_hybrid_rig_contract.py` — neue Rig-, Anker-, LOD- und Allokationsverträge.
- `tools/tests/test_exact_mesh_alignment.py` — bestehende Deformations- und Hitboxverträge erweitern.
- `tools/tests/test_remaining_mob_renderers.py` — Vanilla- gegen Exact-Item-Layer-Vertrag korrigieren.
- `tools/tests/test_exact_mesh_performance_budget.py` — 2K-Standard und optionale 4K-Qualität prüfen.
- `tools/tests/test_exact_quality_textures.py` — Pixelgrösse, UV-Identität und deterministische Ausgabe prüfen.
- `quality-packs/UMR-Exact-4K/pack.mcmeta` und `quality-packs/UMR-Exact-4K/assets/usless_mobs/textures/entity/custom3d/exact/{mob}.png` — optionales 4K-Ressourcenpaket.
- `docs/UMR_ACTIVE_PROJECT_STATE.md` — verifizierten Laufzeitstand und Testresultate dokumentieren.

### Task 1: Gemeinsame Rig-Pose und drei LOD-Stufen vertraglich absichern

**Files:**
- Create: `tools/tests/test_hybrid_rig_contract.py`
- Create: `src/main/java/com/Momik/usless_mobs/client/ExactRigPose.java`
- Create: `src/main/java/com/Momik/usless_mobs/client/ExactAnimationLod.java`
- Modify: `src/main/java/com/Momik/usless_mobs/client/ExactMobMeshLayer.java`
- Modify: `src/main/java/com/Momik/usless_mobs/client/HelpingAllayExactLayer.java`

- [ ] **Step 1: Projektwahrheit und Ausgangstests sichern**

Run:

```powershell
python tools/verify_umr_project_truth.py
python -m pytest tools/tests/test_exact_mesh_alignment.py tools/tests/test_exact_mesh_performance_budget.py tools/tests/test_remaining_mob_renderers.py -q
```

Expected: Projektwahrheit PASS; bestehende Tests PASS.

- [ ] **Step 2: Roten LOD-/Posevertrag schreiben**

Der Test verlangt `NEAR`, `MID`, `FAR`, geordnete quadrierte Distanzen, zeitliche Quantisierung nur in `MID`, statische Darstellung in `FAR` und eine Poseklasse mit vorab berechneten Sinus-/Kosinuswerten. Beispiel:

```python
def test_exact_animation_has_three_ordered_lod_levels():
    source = (CLIENT / "ExactAnimationLod.java").read_text(encoding="utf-8")
    assert "enum ExactAnimationLod" in source
    assert "NEAR" in source and "MID" in source and "FAR" in source
    assert "quantizedAge" in source

def test_inner_vertex_deformers_do_not_compute_trigonometry_or_allocate():
    mesh = (CLIENT / "ExactMobMesh.java").read_text(encoding="utf-8")
    inner = mesh[mesh.index("private static void deformAllayVertex"):mesh.index("private static void emitVertex")]
    assert "new Vector3f" not in inner
    assert "Mth.sin" not in inner
    assert "Mth.cos" not in inner
```

Run: `python -m pytest tools/tests/test_hybrid_rig_contract.py -q`

Expected: FAIL, weil beide Java-Klassen fehlen.

- [ ] **Step 3: Minimale gemeinsame Klassen implementieren**

`ExactAnimationLod` kapselt die Stufen und behält die bisherige, bereits aktive 12-Block-Nahgrenze als Profil-Baseline; 24 Blöcke sind zunächst die messbare Mittelgrenze:

```java
enum ExactAnimationLod {
    NEAR, MID, FAR;

    static final double NEAR_DISTANCE_SQUARED = 144.0D;
    static final double MID_DISTANCE_SQUARED = 576.0D;

    static ExactAnimationLod at(double distanceSquared) {
        if (distanceSquared <= NEAR_DISTANCE_SQUARED) return NEAR;
        if (distanceSquared <= MID_DISTANCE_SQUARED) return MID;
        return FAR;
    }

    float quantizedAge(float ageInTicks) {
        return this == MID ? Mth.floor(ageInTicks * 0.5F) * 2.0F : ageInTicks;
    }
}
```

`ExactRigPose` ist ein wiederverwendbares, mutierbares Primitive-Datenobjekt. `updateFor(CustomMob3DModel.Variant, LivingEntity, float, float, float, float, float)` berechnet Trigonometrie einmal pro Entity-Renderaufruf; Deformer lesen nur Felder.

- [ ] **Step 4: Beide Layer auf dieselbe LOD-Auswahl umstellen**

In beiden Layern:

```java
double distanceSquared = entity.distanceToSqr(cameraPosition);
ExactAnimationLod lod = ExactAnimationLod.at(distanceSquared);
float poseAge = lod.quantizedAge(ageInTicks);
this.rigPose.updateFor(this.variant, entity, limbSwing, limbSwingAmount,
        poseAge, netHeadYaw, headPitch);
```

`FAR` ruft weiterhin `renderBone`; `NEAR` und `MID` verwenden dieselbe Geometrie und die vorberechnete Pose.

- [ ] **Step 5: Fokustests und Kompilierung**

Run:

```powershell
python -m pytest tools/tests/test_hybrid_rig_contract.py tools/tests/test_exact_mesh_alignment.py -q
.\gradlew.bat compileJava --console=plain --no-daemon
```

Expected: PASS und `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```powershell
git add -- tools/tests/test_hybrid_rig_contract.py src/main/java/com/Momik/usless_mobs/client/ExactRigPose.java src/main/java/com/Momik/usless_mobs/client/ExactAnimationLod.java src/main/java/com/Momik/usless_mobs/client/ExactMobMeshLayer.java src/main/java/com/Momik/usless_mobs/client/HelpingAllayExactLayer.java
git commit -m "perf: add shared exact mesh pose lod"
```

### Task 2: Frost Stray mit Ober-/Unterarmen, Beinen und echtem Bogenanker ausstatten

**Files:**
- Modify: `tools/tests/test_hybrid_rig_contract.py`
- Modify: `tools/tests/test_remaining_mob_renderers.py`
- Modify: `src/main/java/com/Momik/usless_mobs/client/ExactRigPose.java`
- Modify: `src/main/java/com/Momik/usless_mobs/client/ExactMobMesh.java`
- Create: `src/main/java/com/Momik/usless_mobs/client/ExactHeldItemLayer.java`
- Modify: `src/main/java/com/Momik/usless_mobs/client/FrostStrayRenderer.java`
- Modify: `src/main/java/com/Momik/usless_mobs/client/ExactMobMeshLayer.java`

- [ ] **Step 1: Roten Stray-Vertrag ergänzen**

Verlange getrennte Schulter-/Ellbogen- und Hüft-/Kniegewichte, zweihändige Bogenpose, Zielblendwert, sichtbaren Handanker und das Fehlen des Vanilla-Item-Layers:

```python
def test_frost_stray_uses_exact_two_hand_bow_anchor():
    renderer = (CLIENT / "FrostStrayRenderer.java").read_text(encoding="utf-8")
    pose = (CLIENT / "ExactRigPose.java").read_text(encoding="utf-8")
    assert "new ExactHeldItemLayer<>(" in renderer
    assert "new ItemInHandLayer<>(" not in renderer
    assert "frostStrayMainHand" in pose
    assert "bowBlend" in pose
```

Run: `python -m pytest tools/tests/test_hybrid_rig_contract.py tools/tests/test_remaining_mob_renderers.py -q`

Expected: FAIL am neuen Exact-Item-Layer und den fehlenden Posefeldern.

- [ ] **Step 2: Stray-Pose einmalig vorberechnen**

`ExactRigPose.updateFrostStray(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, boolean aimingBow)` berechnet gegenläufigen Gang, Kniebeugung, Kopfziel, Oberkörperziel und einen auf 0..1 begrenzten `bowBlend`. Der Übergang verwendet `Mth.lerp`, damit Laufen und Zielen nicht springen.

- [ ] **Step 3: Weiche Zwei-Segment-Gewichte anwenden**

`deformHumanoidVertex` wird in mob-spezifische Posewerte gespeist. Schulter, Ellbogen, Hüfte und Knie verwenden glatte Überlappung statt harte Y-Schnitte:

```java
float upperArmWeight = smoothBand(y, pose.shoulderLow, pose.elbowHigh) * sideWeight;
float lowerArmWeight = smoothBand(y, pose.elbowLow, pose.handHigh) * sideWeight;
rotateAroundX(original, pose.shoulderY, pose.shoulderZ, pose.armAngle(side), temporary);
output.lerp(temporary, upperArmWeight);
rotateAroundX(output, pose.elbowY, pose.elbowZ, pose.forearmAngle(side), temporary);
output.lerp(temporary, lowerArmWeight);
```

`smoothBand` besteht nur aus `Mth.clamp` und Multiplikationen; im inneren Pfad werden keine Objekte und keine trigonometrischen Werte erzeugt.

- [ ] **Step 4: Dedizierten Item-Layer implementieren**

`ExactHeldItemLayer` erhält Variant, `ItemInHandRenderer` und dieselbe Poseberechnung. Für Frost Stray wird die Haupthand an `frostStrayMainHand(PoseStack)` transformiert und danach mit `THIRD_PERSON_RIGHT_HAND` gerendert. Leere Hände werden sofort übersprungen. Fehlende Variant-Anker lösen `IllegalStateException` aus statt still auf Vanilla zurückzufallen.

- [ ] **Step 5: Vanilla-Layer ersetzen und fokussiert prüfen**

Run:

```powershell
python -m pytest tools/tests/test_hybrid_rig_contract.py tools/tests/test_remaining_mob_renderers.py tools/tests/test_exact_mesh_alignment.py -q
.\gradlew.bat compileJava --console=plain --no-daemon
```

Expected: PASS; kein `ItemInHandLayer` mehr im Frost-Stray-Renderer.

- [ ] **Step 6: Commit**

```powershell
git add -- tools/tests/test_hybrid_rig_contract.py tools/tests/test_remaining_mob_renderers.py src/main/java/com/Momik/usless_mobs/client/ExactRigPose.java src/main/java/com/Momik/usless_mobs/client/ExactMobMesh.java src/main/java/com/Momik/usless_mobs/client/ExactHeldItemLayer.java src/main/java/com/Momik/usless_mobs/client/FrostStrayRenderer.java src/main/java/com/Momik/usless_mobs/client/ExactMobMeshLayer.java
git commit -m "fix: rig frost stray bow and limbs"
```

### Task 3: Spiderfront mechanisch bestimmen und acht Beine unabhängig animieren

**Files:**
- Modify: `tools/mob_tripo/exact_runtime.py`
- Modify: `tools/mob_tripo/tests/test_exact_runtime.py`
- Modify: `tools/tests/test_hybrid_rig_contract.py`
- Modify: `src/main/resources/assets/usless_mobs/meshes/entity/custom3d/web_cave_spider.mesh`
- Modify: `src/main/resources/assets/usless_mobs/meshes/entity/custom3d/web_cave_spider.report.json`
- Modify: `src/main/java/com/Momik/usless_mobs/client/ExactRigPose.java`
- Modify: `src/main/java/com/Momik/usless_mobs/client/ExactMobMesh.java`
- Modify: `src/main/java/com/Momik/usless_mobs/client/ExactMobMeshLayer.java`

- [ ] **Step 1: Roten Exportvertrag für Kopf und Vorwärtsachse schreiben**

Der Report muss `head`, acht Beine, `model_forward_axis`, `bounds` und `max_animated_bounds` enthalten. Die Summe der Bone-Dreiecke muss exakt `output_triangles` ergeben.

Run: `python -m pytest tools/mob_tripo/tests/test_exact_runtime.py -q`

Expected: FAIL, weil Spider derzeit nur `body` plus acht Beine und keine Richtungs-/Boundsmetadaten exportiert.

- [ ] **Step 2: Spider-Klassifikation verlustfrei erweitern**

`MOB_SPECS["web_cave_spider"]` erhält `head`. `_classify_spider_regions` trennt zentrale vordere Kopfdreiecke über die verifizierte Kopf-/Augenseite von Bein- und Körperdreiecken; jedes Quelldreieck bleibt genau einer Region zugeordnet. Der Export schreibt die transformierten statischen Grenzen und die konservative maximale Beinauslenkung in den JSON-Report.

- [ ] **Step 3: Runtime-Assets aus der bereits genehmigten Quelle neu exportieren**

Run mit der vorhandenen, im aktiven Zustandsdokument verankerten Web-Cave-Spider-GLB und dem bestehenden Exact-Runtime-CLI. Danach:

```powershell
python -m pytest tools/mob_tripo/tests/test_exact_runtime.py tools/tests/test_exact_mesh_performance_budget.py -q
```

Expected: PASS; Geometrie- und UV-Signaturen bleiben erhalten, Bonezahl wird zehn.

- [ ] **Step 4: Vier diagonale Gangpaare implementieren**

`ExactRigPose.updateSpider(float limbSwing, float limbSwingAmount, float ageInTicks)` berechnet vier Phasen einmal. `deformSpiderVertex` nutzt pro Vertex Seiten-, Stations-, Wurzel- und Spitzengewicht. Die Spitze erhält Vor-/Rückschritt und Hub, die Wurzel blendet zum Körper auf null. `model_forward_axis` wird genau einmal in einen Renderer-Yaw übersetzt; keine zusätzliche Drehung darf Entity-AI oder Navigation verändern.

- [ ] **Step 5: Richtungs- und Nahttests ausführen**

Run:

```powershell
python -m pytest tools/tests/test_hybrid_rig_contract.py tools/tests/test_exact_mesh_alignment.py tools/mob_tripo/tests/test_exact_runtime.py -q
.\gradlew.bat compileJava --console=plain --no-daemon
```

Expected: PASS; Modellfront entspricht der Minecraft-Vorwärtsachse, acht unterschiedliche Beinphasen sind vorhanden.

- [ ] **Step 6: Commit**

```powershell
git add -- tools/mob_tripo/exact_runtime.py tools/mob_tripo/tests/test_exact_runtime.py tools/tests/test_hybrid_rig_contract.py src/main/resources/assets/usless_mobs/meshes/entity/custom3d/web_cave_spider.mesh src/main/resources/assets/usless_mobs/meshes/entity/custom3d/web_cave_spider.report.json src/main/java/com/Momik/usless_mobs/client/ExactRigPose.java src/main/java/com/Momik/usless_mobs/client/ExactMobMesh.java src/main/java/com/Momik/usless_mobs/client/ExactMobMeshLayer.java
git commit -m "fix: align and rig web cave spider"
```

### Task 4: Spider-Hitbox aus statischen und animierten Grenzen validieren

**Files:**
- Modify: `tools/tests/test_hybrid_rig_contract.py`
- Modify: `src/main/java/com/Momik/usless_mobs/registry/ModEntities.java`
- Modify: `src/main/java/com/Momik/usless_mobs/client/WebCaveSpiderRenderer.java`

- [ ] **Step 1: Roten Bounds-gegen-Hitbox-Test schreiben**

Der Test liest `web_cave_spider.report.json`, wendet Renderer-Scale 1.80 an und verlangt, dass X/Z-Maximum samt Animationsmarge innerhalb der registrierten horizontalen Breite und Y innerhalb der Höhe liegt. Zusätzlich darf die Hitbox höchstens 12.5 % grösser als die animierte Hülle sein.

Run: `python -m pytest tools/tests/test_hybrid_rig_contract.py -q`

Expected: je nach echten Reportgrenzen FAIL am bisherigen `.sized(1.30F, 0.56F)` oder PASS ohne unnötige Änderung.

- [ ] **Step 2: Nur bei rotem Test mechanisch korrigieren**

Berechne:

```python
required_width = 2 * max(abs(min_x), abs(max_x), abs(min_z), abs(max_z)) * 1.80
required_height = (max_y - min_y) * 1.80
```

Runde nur auf 0.05 Blöcke nach oben und ändere `.sized(width, height)`. Weil Forge `EntityType.Builder.sized` horizontal quadratisch ist, wird keine nicht existente rechteckige X/Z-Hitbox behauptet. Shadowradius wird nur angepasst, falls er ausserhalb der neuen halben Breite liegt.

- [ ] **Step 3: Test und Kompilierung**

Run:

```powershell
python -m pytest tools/tests/test_hybrid_rig_contract.py tools/tests/test_dedicated_tripo_entities.py -q
.\gradlew.bat compileJava --console=plain --no-daemon
```

Expected: PASS.

- [ ] **Step 4: Commit**

```powershell
git add -- tools/tests/test_hybrid_rig_contract.py src/main/java/com/Momik/usless_mobs/registry/ModEntities.java src/main/java/com/Momik/usless_mobs/client/WebCaveSpiderRenderer.java
git commit -m "fix: fit web cave spider hitbox"
```

### Task 5: Helping Allay mit unabhängigen Flügeln, Armen, Kern und Item-Anker polieren

**Files:**
- Modify: `tools/tests/test_hybrid_rig_contract.py`
- Modify: `tools/tests/test_remaining_mob_renderers.py`
- Modify: `src/main/java/com/Momik/usless_mobs/client/ExactRigPose.java`
- Modify: `src/main/java/com/Momik/usless_mobs/client/ExactMobMesh.java`
- Modify: `src/main/java/com/Momik/usless_mobs/client/HelpingAllayExactLayer.java`
- Modify: `src/main/java/com/Momik/usless_mobs/client/HelpingAllayRenderer.java`
- Modify: `src/main/java/com/Momik/usless_mobs/client/ExactHeldItemLayer.java`

- [ ] **Step 1: Roten Allay-Aktionsvertrag schreiben**

Für `REVEAL`, `SHIELD`, `HEAL`, `BOND`, `TELEPORT` und Rückkehr werden unterschiedliche Posewerte verlangt. Linker/rechter Flügel, Flügelwurzel/-spitze, linker/rechter Arm, Kopf und Kern müssen getrennte Felder besitzen. `ItemInHandLayer` ist verboten.

- [ ] **Step 2: Allay-Pose in `ExactRigPose` zentralisieren**

Die Flügelspitzen laufen gegenüber der Wurzel zeitlich verzögert. Armposen werden pro Aktion gesetzt. Der Kern erhält nur Leucht-/Pulsparameter; seine Geometrie verschiebt den Körper nicht. Kopfneigung und leichte Körperneigung reagieren auf Flugbewegung.

- [ ] **Step 3: Sichtbare Oberfläche auf Posewerte umstellen**

`deformAllayVertex` enthält danach keine `Mth.sin`/`Mth.cos`-Aufrufe. Die weichen Gewichte bleiben positionsbasiert und überlappen an Schulter und Flügelwurzel, damit keine Schnitte entstehen.

- [ ] **Step 4: Exact-Item-Anker anbinden**

`HelpingAllayRenderer` verwendet `ExactHeldItemLayer` vor `HelpingAllayExactLayer`. Beide erhalten dieselbe Entity-Aktion und dasselbe quantisierte Posealter. Das Item folgt der sichtbaren Hand auch während Heilen, Schild und Rückkehr.

- [ ] **Step 5: Fokustests und Kompilierung**

Run:

```powershell
python -m pytest tools/tests/test_hybrid_rig_contract.py tools/tests/test_helping_allay_contract.py tools/tests/test_remaining_mob_renderers.py tools/tests/test_exact_mesh_alignment.py -q
.\gradlew.bat compileJava --console=plain --no-daemon
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add -- tools/tests/test_hybrid_rig_contract.py tools/tests/test_remaining_mob_renderers.py src/main/java/com/Momik/usless_mobs/client/ExactRigPose.java src/main/java/com/Momik/usless_mobs/client/ExactMobMesh.java src/main/java/com/Momik/usless_mobs/client/HelpingAllayExactLayer.java src/main/java/com/Momik/usless_mobs/client/HelpingAllayRenderer.java src/main/java/com/Momik/usless_mobs/client/ExactHeldItemLayer.java
git commit -m "fix: rig helping allay actions and item"
```

### Task 6: 2K-Standardtexturen und optionales 4K-Paket deterministisch erzeugen

**Files:**
- Create: `tools/mob_tripo/build_quality_textures.py`
- Create: `tools/tests/test_exact_quality_textures.py`
- Modify: `tools/tests/test_exact_mesh_performance_budget.py`
- Modify: `src/main/resources/assets/usless_mobs/textures/entity/custom3d/exact/*.png`
- Create: `quality-packs/UMR-Exact-4K/pack.mcmeta`
- Create: `quality-packs/UMR-Exact-4K/assets/usless_mobs/textures/entity/custom3d/exact/*.png`
- Modify: `src/main/resources/assets/usless_mobs/meshes/entity/custom3d/*.report.json`

- [ ] **Step 1: Roten Qualitätspaket-Test schreiben**

Der Test verlangt für jeden aktiven Exact-Mesh-Report genau eine 2048×2048-Standardtextur, optional genau eine 4096×4096-Datei im Qualitätspaket, gleiche RGBA-Farbart und identische Dateinamen. Die Reports erhalten `runtime_texture_width`, `runtime_texture_height` und `source_texture_*`.

Run: `python -m pytest tools/tests/test_exact_quality_textures.py tools/tests/test_exact_mesh_performance_budget.py -q`

Expected: FAIL, weil Standard aktuell 4096×4096 ist und kein separates Paket existiert.

- [ ] **Step 2: Atomaren Generator implementieren**

Der Generator prüft vor dem Schreiben alle Quellen, kopiert die unveränderten 4K-PNGs ins Paket und erzeugt 2K mit Pillow `Image.Resampling.LANCZOS`. UVs bleiben normiert und werden nicht verändert. Alle Dateien werden zuerst in einem temporären Verzeichnis aufgebaut und erst nach vollständiger Validierung ersetzt.

- [ ] **Step 3: Standard- und Qualitätspaket bauen**

Run:

```powershell
python tools/mob_tripo/build_quality_textures.py --all
python -m pytest tools/tests/test_exact_quality_textures.py tools/tests/test_exact_mesh_performance_budget.py -q
```

Expected: PASS; Standard-JAR enthält nur 2K, 4K liegt nur unter `quality-packs/UMR-Exact-4K`.

- [ ] **Step 4: Reproduzierbarkeit prüfen**

Generator zweimal ausführen und SHA-256-Manifest vergleichen. Expected: byteidentische Ausgabe beim zweiten Lauf.

- [ ] **Step 5: Commit**

```powershell
git add -- tools/mob_tripo/build_quality_textures.py tools/tests/test_exact_quality_textures.py tools/tests/test_exact_mesh_performance_budget.py src/main/resources/assets/usless_mobs/textures/entity/custom3d/exact src/main/resources/assets/usless_mobs/meshes/entity/custom3d quality-packs/UMR-Exact-4K
git commit -m "perf: ship 2k exact textures with optional 4k pack"
```

### Task 7: LOD-Grenzen im echten Client messen und festschreiben

**Files:**
- Create: `tools/tests/test_exact_lod_profile.py`
- Create: `docs/qa/exact-mesh-lod-profile-2026-08-25.md`
- Modify: `src/main/java/com/Momik/usless_mobs/client/ExactAnimationLod.java`
- Modify: `docs/UMR_ACTIVE_PROJECT_STATE.md`

- [ ] **Step 1: Kontrollierte Profilszene vorbereiten**

Frischen Dev-Client starten. In einer flachen Testwelt je vier Frost Strays, Web Cave Spiders und Helping Allays in 8, 16, 24 und 32 Block Distanz platzieren. Keine anderen Mobs oder Shader verwenden. Renderdistanz, Auflösung, Java-Version und Hardware im Profilbericht notieren.

- [ ] **Step 2: Baseline und neue LODs messen**

Je 60 Sekunden Idle und Bewegung messen. Festhalten: durchschnittliche FPS, 1%-Low, P95-Framezeit, sichtbare Animationssprünge und `latest.log`-Warnungen. Akzeptanz: Die neue Konfiguration verschlechtert keine 1%-Lows und senkt die P95-Framezeit der Belastungsszene gegenüber dem bisherigen Zwei-Stufen-Stand.

- [ ] **Step 3: Grenzen nach Messregel festschreiben**

12/24 Blöcke bleiben nur, wenn die Akzeptanz erfüllt ist. Andernfalls wird die Nahgrenze auf den letzten Abstand mit stabiler voller Animation und die Mittelgrenze auf den ersten Abstand ohne sichtbaren Unterschied zur statischen Stufe gesetzt. Der Bericht enthält Rohwerte und die daraus abgeleitete Entscheidung.

- [ ] **Step 4: Vertrag und Kompilierung**

`test_exact_lod_profile.py` liest die dokumentierten Grenzen und verlangt exakt dieselben quadrierten Konstanten in `ExactAnimationLod.java`.

Run:

```powershell
python -m pytest tools/tests/test_exact_lod_profile.py tools/tests/test_hybrid_rig_contract.py -q
.\gradlew.bat compileJava --console=plain --no-daemon
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add -- tools/tests/test_exact_lod_profile.py docs/qa/exact-mesh-lod-profile-2026-08-25.md docs/UMR_ACTIVE_PROJECT_STATE.md src/main/java/com/Momik/usless_mobs/client/ExactAnimationLod.java
git commit -m "perf: calibrate exact mesh lod distances"
```

### Task 8: Vollständige automatische Regression ausführen

**Files:**
- Modify only if a real defect is found; do not weaken tests.

- [ ] **Step 1: Projektwahrheit und fokussierte Suite**

```powershell
python tools/verify_umr_project_truth.py
python -m pytest tools/tests/test_hybrid_rig_contract.py tools/tests/test_exact_quality_textures.py tools/tests/test_exact_lod_profile.py tools/tests/test_exact_mesh_alignment.py tools/tests/test_exact_mesh_performance_budget.py tools/tests/test_remaining_mob_renderers.py tools/tests/test_helping_allay_contract.py tools/tests/test_dedicated_tripo_entities.py tools/mob_tripo/tests/test_exact_runtime.py -q
```

Expected: alles PASS.

- [ ] **Step 2: Vollständige Python-Suite**

Run: `python -m pytest -q`

Expected: alles PASS; bekannte Skip-Gründe werden dokumentiert, keine neuen Skips hinzugefügt.

- [ ] **Step 3: Produktionsbuild**

Run:

```powershell
.\gradlew.bat clean build --console=plain --no-daemon
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: JAR-Inhalt prüfen**

Prüfe mechanisch, dass das JAR die neuen Java-Klassen, drei 2K-Texturen, alle Meshes und keine `quality-packs/UMR-Exact-4K`-Dateien enthält. SHA-256 des JARs festhalten.

### Task 9: Echter visueller Clienttest und Release-Gate

**Files:**
- Create: `docs/qa/hybrid-rig-client-acceptance-2026-08-25.md`
- Create: `Modelle/Exports/model_qa/hybrid-rig-client-acceptance/` screenshots
- Modify: `docs/UMR_ACTIVE_PROJECT_STATE.md`

- [ ] **Step 1: Frischen Client normal starten**

Run: `.\gradlew.bat runClient --console=plain --no-daemon`

Verwende keine alte laufende Instanz. Lade eine kontrollierte Testwelt, aktiviere `F3+B` und bestätige die geladene Modversion.

- [ ] **Step 2: Frost Stray prüfen**

Vorne, Seite und Dreiviertel: Idle, gerader Lauf, Kurve, Zielerfassung, Schuss, Wechsel Laufen↔Zielen. Abnahme: Kopf und Brust zielen, beide Arme greifen sinnvoll, Bogen sitzt in der sichtbaren Hand, Beine bleiben unabhängig, keine Naht und Hitbox umfasst das Modell.

- [ ] **Step 3: Web Cave Spider prüfen**

Vorne, Seite und Dreiviertel: Idle, gerader Lauf, Kurve, Verfolgung, Angriff. Abnahme: Kopf zeigt in Bewegungsrichtung, alle acht Beine bilden den diagonalen Gang, Körper bleibt am Boden, keine Rückwärtsbewegung, Hitbox schneidet keine animierte Spitze ab.

- [ ] **Step 4: Helping Allay prüfen**

Idle, Flug, gehaltenes Item und jede Aktion `REVEAL`, `SHIELD`, `HEAL`, `BOND`, `TELEPORT`/Rückkehr. Abnahme: Flügelwurzeln und -spitzen bewegen sich getrennt, Arme folgen der Aktion, Item folgt der sichtbaren Hand, Kern pulsiert ohne Körperversatz.

- [ ] **Step 5: LOD- und Belastungsszene prüfen**

Mehrere Instanzen in Nah-, Mittel- und Fernstufe. Abnahme: kein Flackern, kein Verschwinden, keine offenen Schnitte, keine auffälligen Posesprünge; gemessene Werte stimmen mit Task 7 überein.

- [ ] **Step 6: Log und sauberes Beenden**

`run/logs/latest.log` nach `ERROR`, `Exception`, `usless_mobs`, `mesh`, `texture`, `renderer`, `GeckoLib` durchsuchen. Nur vollständig erklärte Fremdhinweise akzeptieren. Welt speichern und Client über das Menü beenden.

- [ ] **Step 7: Abnahme dokumentieren und committen**

```powershell
git add -- docs/qa/hybrid-rig-client-acceptance-2026-08-25.md docs/UMR_ACTIVE_PROJECT_STATE.md Modelle/Exports/model_qa/hybrid-rig-client-acceptance
git commit -m "test: record hybrid rig client acceptance"
```

- [ ] **Step 8: Release nur nach grüner CI**

Branch pushen, GitHub-CI abwarten und deren JAR herunterladen. CI-JAR gegen lokalen Build auf Modversion, Forge-/GeckoLib-/TerraBlender-Abhängigkeiten, Dateieinträge und SHA-256 prüfen. Erst danach Pre-Release mit exakter Testmatrix sowie separatem 4K-Pack veröffentlichen. Keine Behauptung einer absoluten Fehlerfreiheit.

## Abschlusskriterien

- Alle drei Mobs erfüllen die visuellen Abnahmepunkte im echten Client.
- Frost Stray und Helping Allay verwenden keinen unsichtbaren Vanilla-Handanker mehr.
- Spiderfront und Bewegungsrichtung stimmen überein.
- Hitboxen beruhen auf exportierten statischen plus maximal animierten Grenzen.
- Standardtexturen sind 2K; 4K ist getrennt und optional.
- Drei LOD-Stufen sind durch Profilwerte und Tests abgesichert.
- Kein innerer Vertex-/Dreieckspfad erzeugt neue Objekte oder berechnet Trigonometrie.
- Projektwahrheit, fokussierte Tests, Gesamtsuite, Gradle-Build, Clienttest und CI sind grün.
- Ungetrackte Nutzer-/Tripo-Dateien bleiben unberührt.
