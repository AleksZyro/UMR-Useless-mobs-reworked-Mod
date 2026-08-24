# Living Boss Tripo Pilot Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Den Living Boss als originalgetreues, texturiertes Tripo-Modell mit sicherem Blockbench-Rig, natürlichen GeckoLib-Animationen, passender Hitbox und geprüfter Forge-Laufzeit neu aufbauen.

**Architecture:** Ein unveränderlicher Tripo-GLB-Export bildet die visuelle Quelle. Kleine Python-Werkzeuge prüfen und konvertieren die getrennten GLB-Komponenten verlustfrei in ein Blockbench-Rig sowie ein binäres Dreiecksmesh; GeckoLib liefert nur Bone-Transformationen und ein eigener Renderer zeichnet die originalen Dreiecke. Produktionsassets bleiben unverändert, bis ein isolierter Preview-Pack und der echte Client visuell freigegeben sind.

**Tech Stack:** Tripo Studio, OpenAI Image Generation für die Mehransichten-Vorlage, GLB 2.0, Python 3, NumPy, Pillow, Blockbench `.bbmodel`, GeckoLib 4.8.3, Forge 1.20.1/47.4.16, Java 17, `unittest`, Gradle.

---

## File Map

- Create: `Modelle/Exports/living_boss_v1/concept/living_boss_multiview.png` — identische Front-, Seiten-, Rück- und Draufsicht als Tripo-Eingabe.
- Create: `Modelle/Exports/living_boss_v1/tripo_export/living_boss_tripo.glb` — unveränderliche, texturierte Tripo-Quelle.
- Create: `Modelle/Exports/living_boss_v1/source_manifest.json` — Hashes, Lizenznotiz, GLB-Struktur, Bounds, Polygonzahl und Texturmetadaten.
- Create: `Modelle/Exports/living_boss_v1/blockbench/Living Boss Tripo Source.bbmodel` — verlustfreie Blockbench-Quelle.
- Create: `Modelle/Exports/living_boss_v1/blockbench/Living Boss Tripo Rig.bbmodel` — benanntes Ruhepose-Rig.
- Create: `Modelle/Exports/living_boss_v1/blockbench/Living Boss Tripo Animated.bbmodel` — Rig mit Animationen.
- Create: `Modelle/Exports/living_boss_v1/review/` — feste Vergleichsansichten und Kontaktblatt.
- Create: `tools/tripo_pipeline/glb.py` — sichere GLB-Ladung und kanonische Geometrie-/UV-Signaturen.
- Create: `tools/tripo_pipeline/blockbench.py` — deterministische Blockbench-Konvertierung und atomische Publikation.
- Create: `tools/tripo_pipeline/runtime_mesh.py` — deterministischer Runtime-Mesh-Encoder/Decoder.
- Create: `tools/living_boss_tripo/spec.py` — exakte Bone-Hierarchie, erlaubte Komponenten und Animationen.
- Create: `tools/living_boss_tripo/build.py` — Pilot-CLI für Quelle, Rig, Animationen und Preview-Bundle.
- Create: `tools/living_boss_tripo/render_review.py` — feste Front-/Seiten-/Rück-/Top-/Perspektiv- und Bewegungsansichten.
- Create: `tools/living_boss_tripo/validate.py` — unabhängiger Gesamtvalidator.
- Create: `tools/living_boss_tripo/tests/` — Vertrags-, Fehlerpfad-, Determinismus- und Integrationsprüfungen.
- Create: `src/main/java/com/Momik/usless_mobs/client/LivingBossGeoModel.java` — GeckoLib-Ressourcenauflösung.
- Create: `src/main/java/com/Momik/usless_mobs/client/LivingBossTripoMesh.java` — begrenzter, validierender Mesh-Loader und Renderer.
- Create: `src/main/java/com/Momik/usless_mobs/client/LivingBossTripoRenderer.java` — GeckoLib-Bone-Rendering des exakten Meshes.
- Modify: `src/main/java/com/Momik/usless_mobs/client/LivingBossRenderer.java` — sichere Preview-Auswahl mit Legacy-Fallback.
- Modify: `src/main/java/com/Momik/usless_mobs/entity/LivingBossEntity.java` — GeckoLib-Controller und Ereignistrigger ohne Änderung der Kampfwerte.
- Modify: `src/main/java/com/Momik/usless_mobs/registry/ModEntities.java` — anhand des geprüften Modellberichts bestimmte Hitbox.
- Modify: `src/main/java/com/Momik/usless_mobs/Usless_mobs.java` — Renderer-Factory statt direktem Legacy-Constructor.
- Create: `run/resourcepacks/living_boss_v1_preview/` — isolierter, nicht produktiver Preview-Pack.

### Task 1: Abgelehnten Renderer-Workaround entfernen und Schutzvertrag festlegen

**Files:**
- Delete: `tools/tests/test_remaining_mob_renderers.py`
- Delete: `src/main/resources/assets/usless_mobs/textures/entity/custom3d/transparent_base.png`
- Modify: `src/main/java/com/Momik/usless_mobs/client/CustomMobModelLayers.java`
- Modify: die acht vom Workaround berührten Rendererdateien
- Create: `tools/living_boss_tripo/tests/test_scope.py`

- [ ] **Step 1: Schutztest vor jeder Bereinigung schreiben**

Der Test speichert SHA-256-Werte aller ausgeschlossenen Slime-, Corrupted-Silverfish-, Endermite-, Rüstungs- und Kronenpfade aus `git show HEAD:<path>` und vergleicht sie nach jeder Pilotphase mit dem Arbeitsbaum. Er verlangt zusätzlich, dass kein Renderer `TRANSPARENT_BASE_TEXTURE` oder `layers.clear()` enthält.

```python
def test_rejected_transparent_base_workaround_is_absent():
    assert not (ROOT / "src/main/resources/assets/usless_mobs/textures/entity/custom3d/transparent_base.png").exists()
    java = "\n".join(path.read_text(encoding="utf-8") for path in TARGET_RENDERERS)
    assert "TRANSPARENT_BASE_TEXTURE" not in java
    assert ".layers.clear()" not in java
```

- [ ] **Step 2: RED nachweisen**

Run: `python -m unittest tools.living_boss_tripo.tests.test_scope -v`

Expected: FAIL wegen vorhandener transparenter Basis und Renderer-Manipulationen.

- [ ] **Step 3: Nur die exakt abgelehnten eigenen Änderungen per `apply_patch` zurücknehmen**

Die acht Renderer, `CustomMobModelLayers.java` und `CustomMob3DLayer.java` werden in ihren `HEAD`-Inhalt zurückgeführt. Die zwei ausschliesslich für den Workaround erzeugten Dateien werden entfernt. Keine andere untracked Datei wird gelöscht oder verschoben.

- [ ] **Step 4: Schutztest und Diff prüfen**

Run: `python -m unittest tools.living_boss_tripo.tests.test_scope -v`

Expected: PASS. `git diff -- src/main/java/com/Momik/usless_mobs/client` zeigt danach keine Workaround-Differenz mehr.

- [ ] **Step 5: Schutzvertrag committen**

```text
git add tools/living_boss_tripo/tests/test_scope.py
git commit -m "test: protect excluded mobs during Tripo pilot"
```

### Task 2: Living-Boss-Konzept und Tripo-Quelle freigeben

**Files:**
- Create: `Modelle/Exports/living_boss_v1/concept/living_boss_multiview.png`
- Create: `Modelle/Exports/living_boss_v1/tripo_export/living_boss_tripo.glb`
- Create: `Modelle/Exports/living_boss_v1/source_manifest.json`

- [ ] **Step 1: Mehransichten-Konzept generieren**

OpenAI Image Generation erhält diesen verbindlichen Inhalt: ein breiter, vierbeiniger Minecraft-Fantasyboss namens Living Warden; massiver dunkler Holz-/Steinkörper; Moos, Wurzeln und leuchtende hellgrüne Lebenskristalle; grosser geschützter Kopf; vier klar getrennte Beine mit sichtbaren Gelenken; keine Plattform, keine Landschaft, keine Beschriftung; identische Front-, rechte Seiten-, Rück- und Draufsicht; neutrale einfarbige Fläche; gleiche Skalierung und Details in allen Ansichten.

- [ ] **Step 2: Konzeptbild visuell prüfen**

Die vier Ansichten müssen dieselbe Zahl und Position von Beinen, Kristallen, Hörnern und Wurzeln zeigen. Bei Widersprüchen wird das Bild verworfen, bevor Tripo Credits verbraucht.

- [ ] **Step 3: Bild in Tripo hochladen und Modell erzeugen**

In Tripo `Image to 3D` verwenden. Nur ein Ergebnis weiterführen, dessen Silhouette aus Front und Seite dem Konzept entspricht. Danach die Tripo-Segmentierung verwenden und exakt diese Komponenten benennen: `body`, `head`, `leg_front_left`, `leg_front_right`, `leg_rear_left`, `leg_rear_right`, `roots_body`. Keine Retopologie oder Auto-Rigging-Stufe darf die Textur neu generieren.

- [ ] **Step 4: GLB herunterladen und Quelle unveränderlich dokumentieren**

Der GLB-Export muss Textur, UVs, Dreiecke und benannte Komponenten enthalten. `source_manifest.json` enthält `sha256`, `bytes`, `triangle_count`, `component_names`, `bounds`, `texture_width`, `texture_height` und `license_note: "Generated in the user's authenticated Tripo workspace"`.

- [ ] **Step 5: Konzept und Quelle committen**

```text
git add Modelle/Exports/living_boss_v1/concept/living_boss_multiview.png Modelle/Exports/living_boss_v1/tripo_export/living_boss_tripo.glb Modelle/Exports/living_boss_v1/source_manifest.json
git commit -m "feat: add approved Living Boss Tripo source"
```

### Task 3: Sicheren GLB-Vertrag implementieren

**Files:**
- Create: `tools/tripo_pipeline/__init__.py`
- Create: `tools/tripo_pipeline/glb.py`
- Create: `tools/living_boss_tripo/tests/test_glb.py`

- [ ] **Step 1: Failing Tests für GLB 2.0 schreiben**

```python
def test_load_preserves_components_triangles_uvs_and_texture(source_glb):
    source = load_glb(source_glb)
    assert set(source.components) == EXPECTED_COMPONENTS
    assert source.triangle_count > 0
    assert source.texture.mode == "RGBA"
    assert all(np.isfinite(component.positions).all() for component in source.components.values())

def test_rejects_missing_uvs_external_texture_and_path_escape(fixtures):
    for fixture in fixtures:
        with self.subTest(fixture=fixture), self.assertRaises(ValueError):
            load_glb(fixture)
```

- [ ] **Step 2: RED nachweisen**

Run: `python -m unittest tools.living_boss_tripo.tests.test_glb -v`

Expected: ERROR wegen fehlendem `tools.tripo_pipeline.glb`.

- [ ] **Step 3: Minimalen Loader implementieren**

```python
@dataclass(frozen=True)
class Component:
    name: str
    positions: np.ndarray
    uvs: np.ndarray
    triangles: np.ndarray

@dataclass(frozen=True)
class SourceModel:
    components: Mapping[str, Component]
    texture: Image.Image
    source_sha256: str

def load_glb(path: Path, *, expected_components: frozenset[str]) -> SourceModel:
    data = Path(path).read_bytes()
    document, binary = parse_glb_container(data)
    components = load_named_triangle_components(document, binary)
    if frozenset(components) != expected_components:
        raise ValueError(f"GLB components differ: {sorted(components)}")
    texture = load_single_embedded_base_colour(document, binary)
    return SourceModel(MappingProxyType(components), texture, sha256(data).hexdigest())

def canonical_face_signature(model: SourceModel) -> Counter[tuple]:
    signature = Counter()
    for name, component in model.components.items():
        for triangle in component.triangles:
            corners = tuple(
                (component.positions[index].astype("<f4").tobytes(),
                 component.uvs[index].astype("<f4").tobytes())
                for index in triangle
            )
            signature[(name, corners)] += 1
    return signature
```

Nur GLB 2.0, eingebettete BIN-Chunks, Dreiecke, endliche Floatwerte, genau eine eingebettete RGBA-Basistextur und die exakten Komponenten werden akzeptiert. Sparse Accessors, externe URIs, absolute Pfade, `..`, fehlende UVs und zusätzliche Komponenten werden abgewiesen.

- [ ] **Step 4: Fehlerpfade und echte Quelle prüfen**

Run: `python -m unittest tools.living_boss_tripo.tests.test_glb -v`

Expected: PASS; die echte Quelle liefert exakt sieben Komponenten und eine nicht leere Textur.

- [ ] **Step 5: Commit**

```text
git add tools/tripo_pipeline tools/living_boss_tripo/tests/test_glb.py
git commit -m "feat: validate segmented Tripo GLB sources"
```

### Task 4: Verlustfreies Blockbench-Rig bauen

**Files:**
- Create: `tools/tripo_pipeline/blockbench.py`
- Create: `tools/living_boss_tripo/spec.py`
- Create: `tools/living_boss_tripo/build.py`
- Create: `tools/living_boss_tripo/tests/test_rig.py`
- Create: `Modelle/Exports/living_boss_v1/blockbench/Living Boss Tripo Source.bbmodel`
- Create: `Modelle/Exports/living_boss_v1/blockbench/Living Boss Tripo Rig.bbmodel`

- [ ] **Step 1: Exakte Hierarchie und Geometrie als RED definieren**

```python
PARENTS = {
    "body": "root",
    "head": "body",
    "leg_front_left": "body",
    "leg_front_right": "body",
    "leg_rear_left": "body",
    "leg_rear_right": "body",
    "roots_body": "body",
}

def test_rig_is_lossless_and_has_exact_hierarchy(source, rig):
    assert canonical_faces(rig) == canonical_faces(source)
    assert parent_map(rig) == PARENTS
    assert texture_hash(rig) == texture_hash(source)
```

- [ ] **Step 2: RED nachweisen**

Run: `python -m unittest tools.living_boss_tripo.tests.test_rig -v`

Expected: ERROR wegen fehlendem Blockbench-Builder.

- [ ] **Step 3: Blockbench-Quell- und Rig-Dokumente erzeugen**

`build_source_document()` übernimmt jedes Dreieck und jeden UV-Punkt ohne Quantisierung. `build_rig_document()` ändert nur Outliner, Gruppenzuordnung und Pivots. Pivots liegen am Komponentenanschluss: Kopf am unteren Halszentrum, Beine jeweils am obersten innenliegenden Rand, `roots_body` im Körperzentrum. UUIDv5 verwendet feste Namen und bleibt bei Wiederholung bytegleich.

```python
def build_source_document(model: SourceModel) -> dict:
    elements = [mesh_element(name, component) for name, component in model.components.items()]
    return blockbench_document(elements, embedded_png_data_uri(model.texture), groups=[])

def build_rig_document(source_document: dict, parents: Mapping[str, str]) -> dict:
    result = deepcopy(source_document)
    result["groups"] = build_groups_with_attachment_pivots(result["elements"], parents)
    result["outliner"] = build_outliner(result["groups"], result["elements"])
    return result

def publish_json_transaction(payloads: Mapping[Path, bytes]) -> None:
    candidates = stage_unique_sibling_files(payloads)
    backups = snapshot_existing_targets(payloads)
    publish_or_restore(candidates, backups, payloads)
```

- [ ] **Step 4: Seam-Audit erzwingen**

Gemeinsame Vertexpositionen zwischen zwei Komponenten sind nur an den sechs bewussten Gelenkpaaren `body:head`, `body:leg_front_left`, `body:leg_front_right`, `body:leg_rear_left`, `body:leg_rear_right`, `body:roots_body` erlaubt. Jeder weitere Cross-Bone-Seam bricht den Build ab.

- [ ] **Step 5: Determinismus und atomische Fehlerpfade prüfen**

Zwei Builds müssen byteidentisch sein. Tests injizieren Stage-, Write-, Replace- und Rollbackfehler und prüfen, dass bestehende Ziele unverändert bleiben und nur transaktionseigene temporäre Dateien entfernt werden.

- [ ] **Step 6: Kandidaten bauen und committen**

Run: `python tools/living_boss_tripo/build.py --stage rig`

Expected: `LIVING_BOSS_RIG_PASS COMPONENTS=7` mit identischer Face- und Textursignatur.

```text
git add tools/tripo_pipeline/blockbench.py tools/living_boss_tripo Modelle/Exports/living_boss_v1/blockbench
git commit -m "feat: build lossless Living Boss Tripo rig"
```

### Task 5: Natürliche Living-Boss-Animationen erstellen

**Files:**
- Modify: `tools/living_boss_tripo/spec.py`
- Modify: `tools/living_boss_tripo/build.py`
- Create: `tools/living_boss_tripo/tests/test_animation.py`
- Create: `Modelle/Exports/living_boss_v1/blockbench/Living Boss Tripo Animated.bbmodel`

- [ ] **Step 1: Animationsvertrag als RED schreiben**

Exakt diese Animationen sind erforderlich: `animation.living_boss.idle`, `walk`, `attack`, `hurt`, `death`, `root_cast`, `heart_pulse`. Idle und Walk loopen; die übrigen spielen einmal. Walk dauert `1.0` Sekunden mit `21` Keyframe-Zeitpunkten im Abstand `0.05`.

```python
def test_walk_is_closed_and_uses_opposed_quadruped_pairs(animated):
    assert frame(animated, "walk", 0.0) == frame(animated, "walk", 1.0)
    assert y_rotation(animated, "leg_front_left", 0.25) == -y_rotation(animated, "leg_rear_left", 0.25)
    assert y_rotation(animated, "leg_front_right", 0.25) == -y_rotation(animated, "leg_rear_right", 0.25)
```

- [ ] **Step 2: RED nachweisen**

Run: `python -m unittest tools.living_boss_tripo.tests.test_animation -v`

Expected: FAIL wegen fehlenden sieben Animationen.

- [ ] **Step 3: Animationen erzeugen**

Walk verwendet einen diagonalen Vierbein-Gang: vorne links und hinten rechts teilen eine Phase; vorne rechts und hinten links die Gegenphase. Beinschwung beträgt `±24°`, zusätzlicher Lift `10°`, Body-Bob maximal `0.22` Blockbench-Einheiten. Kein benachbarter Rotationssprung überschreitet `8°`. Attack senkt den Kopf und stösst den Body nach vorne; Root Cast hebt `roots_body`; Heart Pulse weitet Body/Roots über Scale höchstens `1.06`; Death endet nach `1.2` Sekunden in einer stabilen Seitenlage.

- [ ] **Step 4: Bone-, Zeit-, Finite- und Loop-Tests ausführen**

Run: `python -m unittest tools.living_boss_tripo.tests.test_animation -v`

Expected: PASS; jeder Keyframe zielt auf einen existierenden Bone, alle Werte sind endlich, erster und letzter Loop-Frame sind identisch.

- [ ] **Step 5: Animierten Kandidaten committen**

```text
git add tools/living_boss_tripo/spec.py tools/living_boss_tripo/build.py tools/living_boss_tripo/tests/test_animation.py "Modelle/Exports/living_boss_v1/blockbench/Living Boss Tripo Animated.bbmodel"
git commit -m "feat: animate Living Boss Tripo rig"
```

### Task 6: Runtime-Bundle und Preview-Pack bauen

**Files:**
- Create: `tools/tripo_pipeline/runtime_mesh.py`
- Modify: `tools/living_boss_tripo/build.py`
- Create: `tools/living_boss_tripo/validate.py`
- Create: `tools/living_boss_tripo/tests/test_runtime.py`
- Create: `run/resourcepacks/living_boss_v1_preview/pack.mcmeta`
- Create: vier Living-Boss-Ressourcen im Preview-Pack

- [ ] **Step 1: Runtime-Vertrag als RED schreiben**

Der Preview-Pack enthält exakt `geo/living_boss.geo.json`, `animations/living_boss.animation.json`, `textures/entity/living_boss.png` und `meshes/entity/living_boss.mesh` unter `assets/usless_mobs/`, plus `pack.mcmeta`. Geometry-Bones, Animation-Bones und Mesh-Komponenten müssen exakt dieselbe Menge besitzen.

- [ ] **Step 2: RED nachweisen**

Run: `python -m unittest tools.living_boss_tripo.tests.test_runtime -v`

Expected: FAIL wegen fehlendem Bundle.

- [ ] **Step 3: Begrenztes Meshformat implementieren**

```python
MAGIC = b"LBMESH1\0"
Triangle = tuple[
    tuple[float, float, float, float, float],
    tuple[float, float, float, float, float],
    tuple[float, float, float, float, float],
]

def encode_mesh(rig: dict) -> bytes:
    output = BytesIO()
    output.write(MAGIC)
    write_bounded_parts(output, extract_triangles_by_bone(rig))
    return output.getvalue()

def decode_mesh(payload: bytes) -> Mapping[str, Sequence[Triangle]]:
    source = BytesIO(payload)
    if source.read(len(MAGIC)) != MAGIC:
        raise ValueError("Living Boss mesh magic differs")
    result = read_bounded_parts(source)
    if source.read(1):
        raise ValueError("Living Boss mesh contains trailing bytes")
    return MappingProxyType(result)

def build_runtime_bundle(animated: dict) -> Mapping[str, bytes]:
    return MappingProxyType({
        "geo/living_boss.geo.json": json_bytes(geo_from_groups(animated)),
        "animations/living_boss.animation.json": json_bytes(animations_from_blockbench(animated)),
        "textures/entity/living_boss.png": embedded_texture_bytes(animated),
        "meshes/entity/living_boss.mesh": encode_mesh(animated),
    })
```

Jedes Dreieck speichert drei Positionen und drei UV-Paare als Little-Endian-Floats. Der Decoder begrenzt Bones auf `32`, Namen auf `128` UTF-8-Bytes, Faces pro Bone auf `250_000`, Gesamtfaces auf den exakten Manifestwert und lehnt NaN, Infinity, Duplikate und Trailing Bytes ab.

- [ ] **Step 4: Preview-Pack transaktional publizieren**

Alle fünf Dateien werden zuerst in ein eindeutiges Geschwisterverzeichnis geschrieben, erneut validiert und dann gemeinsam ausgetauscht. Stale Dateien, leere Zusatzordner, Symlinks und Reparse Points führen zu einem nicht-destruktiven Fehler.

- [ ] **Step 5: Bundle und Validator prüfen**

Run: `python tools/living_boss_tripo/build.py --stage preview`

Expected: `LIVING_BOSS_PREVIEW_PASS FILES=5`.

Run: `python tools/living_boss_tripo/validate.py --root run/resourcepacks/living_boss_v1_preview`

Expected: `LIVING_BOSS_VALIDATE_PASS COMPONENTS=7 ANIMATIONS=7`.

- [ ] **Step 6: Tooling committen, Preview-Pack nicht committen**

```text
git add tools/tripo_pipeline/runtime_mesh.py tools/living_boss_tripo
git commit -m "feat: build validated Living Boss preview pack"
```

### Task 7: Sichere Java-Integration mit Legacy-Fallback

**Files:**
- Create: `src/main/java/com/Momik/usless_mobs/client/LivingBossGeoModel.java`
- Create: `src/main/java/com/Momik/usless_mobs/client/LivingBossTripoMesh.java`
- Create: `src/main/java/com/Momik/usless_mobs/client/LivingBossTripoRenderer.java`
- Modify: `src/main/java/com/Momik/usless_mobs/client/LivingBossRenderer.java`
- Modify: `src/main/java/com/Momik/usless_mobs/entity/LivingBossEntity.java`
- Modify: `src/main/java/com/Momik/usless_mobs/Usless_mobs.java`
- Create: `tools/living_boss_tripo/tests/test_java_integration.py`

- [ ] **Step 1: Statische Integrationsverträge als RED schreiben**

Tests verlangen: genau eine Renderer-Registrierung über `LivingBossRenderer::createRenderer`; Tripo-Auswahl nur wenn alle vier Preview-Ressourcen vorhanden sind; sonst unveränderte Legacy-Instanz; Mesh-Loader mit `LBMESH1` und Manifest-Facezahl; sieben RawAnimations; genau zwei Controller `movement` und `action`; keine Client-Imports in der Entity.

- [ ] **Step 2: RED nachweisen**

Run: `python -m unittest tools.living_boss_tripo.tests.test_java_integration -v`

Expected: FAIL wegen fehlenden Tripo-Klassen und GeckoLib-Vertrag.

- [ ] **Step 3: GeoEntity-Vertrag ergänzen**

`LivingBossEntity` implementiert `GeoEntity`, besitzt einen `AnimatableInstanceCache` und registriert:

```java
controllers.add(new AnimationController<>(this, "movement", 4, state ->
        state.setAndContinue(state.isMoving() ? WALK_ANIM : IDLE_ANIM)));
controllers.add(new AnimationController<>(this, "action", 0, state -> PlayState.STOP)
        .triggerableAnim("attack", ATTACK_ANIM)
        .triggerableAnim("hurt", HURT_ANIM)
        .triggerableAnim("death", DEATH_ANIM)
        .triggerableAnim("root_cast", ROOT_CAST_ANIM)
        .triggerableAnim("heart_pulse", HEART_PULSE_ANIM));
```

Erfolgreiche `doHurtTarget`, erfolgreiche `hurt`, `die`, `startRootCage` und `heartPulse` lösen serverseitig exakt den passenden Trigger aus. Kampfwerte, Cooldowns, Effekte, Drops und Bossbar bleiben unverändert.

- [ ] **Step 4: Ressourcenmodell, Mesh-Loader und Renderer implementieren**

`LivingBossGeoModel` verweist ausschliesslich auf die vier Living-Boss-Previewpfade. `LivingBossTripoMesh` prüft Magic, Bone-Menge, exakte Facezahl, Grenzen und Endlichkeit. `LivingBossTripoRenderer` überschreibt `renderRecursively` und rendert pro GeckoLib-Bone nur dessen Meshpart.

- [ ] **Step 5: Legacy-Fallback implementieren**

```java
public static EntityRenderer<? super LivingBossEntity> createRenderer(Context context) {
    ResourceManager resources = context.getResourceManager();
    return REQUIRED_TRIPO_RESOURCES.stream().allMatch(id -> resources.getResource(id).isPresent())
            ? new LivingBossTripoRenderer(context)
            : new LivingBossRenderer(context);
}
```

So bleibt der bisherige Mob ohne Preview-Pack funktionsfähig und das neue Modell wird noch nicht produktiv erzwungen.

- [ ] **Step 6: Tests und Compile ausführen**

Run: `python -m unittest tools.living_boss_tripo.tests.test_java_integration -v`

Expected: PASS.

Run: `.\gradlew.bat compileJava`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Java-Integration committen**

```text
git add src/main/java/com/Momik/usless_mobs/client/LivingBossGeoModel.java src/main/java/com/Momik/usless_mobs/client/LivingBossTripoMesh.java src/main/java/com/Momik/usless_mobs/client/LivingBossTripoRenderer.java src/main/java/com/Momik/usless_mobs/client/LivingBossRenderer.java src/main/java/com/Momik/usless_mobs/entity/LivingBossEntity.java src/main/java/com/Momik/usless_mobs/Usless_mobs.java tools/living_boss_tripo/tests/test_java_integration.py
git commit -m "feat: preview exact Living Boss Tripo mesh"
```

### Task 8: Hitbox aus dem freigegebenen Modell ableiten

**Files:**
- Modify: `tools/living_boss_tripo/validate.py`
- Modify: `tools/living_boss_tripo/tests/test_runtime.py`
- Modify: `src/main/java/com/Momik/usless_mobs/registry/ModEntities.java`

- [ ] **Step 1: Hitbox-Bericht und RED-Test ergänzen**

Der Validator berechnet die spielrelevante Hauptmasse aus `body`, `head` und vier Beinen; `roots_body`-Dekorationen werden ignoriert. Breite ist `ceil(max(X-Spanne, Z-Spanne) / 16 * 20) / 20`, Höhe ist `ceil((maxY - minY) / 16 * 20) / 20`. Beide Werte werden auf mindestens `0.5` und höchstens `4.0` begrenzt.

```python
def test_registry_hitbox_equals_validated_main_mass(report, mod_entities_source):
    assert f".sized({report['hitbox_width']:.2f}F, {report['hitbox_height']:.2f}F)" in mod_entities_source
```

- [ ] **Step 2: RED nachweisen**

Run: `python -m unittest tools.living_boss_tripo.tests.test_runtime -v`

Expected: FAIL, falls die bestehende `1.95F, 2.2F`-Hitbox nicht exakt dem Bericht entspricht.

- [ ] **Step 3: Nur die beiden Living-Boss-Literale ersetzen**

Die vom Validator ausgegebenen Werte werden unverändert in `ModEntities.LIVING_BOSS.sized(width, height)` eingesetzt. Keine andere Entity-Grösse wird geändert.

- [ ] **Step 4: Tests und Compile ausführen**

Run: `python -m unittest tools.living_boss_tripo.tests.test_runtime -v`

Expected: PASS.

Run: `.\gradlew.bat compileJava`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Hitbox committen**

```text
git add tools/living_boss_tripo/validate.py tools/living_boss_tripo/tests/test_runtime.py src/main/java/com/Momik/usless_mobs/registry/ModEntities.java
git commit -m "fix: match Living Boss hitbox to Tripo body"
```

### Task 9: Reproduzierbare Vergleichsbilder erstellen

**Files:**
- Create: `tools/living_boss_tripo/render_review.py`
- Create: `tools/living_boss_tripo/tests/test_render_review.py`
- Create: `Modelle/Exports/living_boss_v1/review/front.png`
- Create: `Modelle/Exports/living_boss_v1/review/right.png`
- Create: `Modelle/Exports/living_boss_v1/review/back.png`
- Create: `Modelle/Exports/living_boss_v1/review/top.png`
- Create: `Modelle/Exports/living_boss_v1/review/perspective.png`
- Create: `Modelle/Exports/living_boss_v1/review/idle.png`
- Create: `Modelle/Exports/living_boss_v1/review/walk_midstride.png`
- Create: `Modelle/Exports/living_boss_v1/review/contact_sheet.png`

- [ ] **Step 1: Rendervertrag als RED schreiben**

Alle acht PNGs sind RGBA, mindestens `640x640`, nicht leer, besitzen identische Modellskalierung und werden in einer einzigen Multioutput-Transaktion publiziert. Idle und Walk müssen sich sichtbar an allen vier Beinen unterscheiden; die Ruhepose-Silhouette muss gegenüber der GLB-Quelle innerhalb einer normalisierten Maskenabweichung von höchstens `2 %` liegen.

- [ ] **Step 2: RED nachweisen**

Run: `python -m unittest tools.living_boss_tripo.tests.test_render_review -v`

Expected: FAIL wegen fehlendem Renderer.

- [ ] **Step 3: Dreiecksrenderer und Kontaktblatt implementieren**

Per-Pixel-Z-Buffer, baryzentrische UV-Interpolation, Alpha-Compositing und deterministischer Face-Rank werden verwendet. Alle Kandidaten werden auf ihre Alpha-Bounds zugeschnitten und mit einer gemeinsamen Skalierung dargestellt.

- [ ] **Step 4: Bilder bauen und visuell prüfen**

Run: `python tools/living_boss_tripo/render_review.py`

Expected: `LIVING_BOSS_RENDER_PASS FILES=8`.

Front, Seite, Rücken, Top, Perspektive, Idle, Mid-Stride und Kontaktblatt werden tatsächlich geöffnet. Schwarze Flächen, fehlende UVs, Schnitte, schwebende Beine oder abweichende Silhouette blockieren den nächsten Task.

- [ ] **Step 5: Review-Artefakte committen**

```text
git add tools/living_boss_tripo/render_review.py tools/living_boss_tripo/tests/test_render_review.py Modelle/Exports/living_boss_v1/review
git commit -m "test: render Living Boss Tripo review set"
```

### Task 10: Echten Forge-Client prüfen und Freigabe einholen

**Files:**
- Create: `Modelle/Exports/living_boss_v1/review/client_idle.png`
- Create: `Modelle/Exports/living_boss_v1/review/client_walk_a.png`
- Create: `Modelle/Exports/living_boss_v1/review/client_walk_b.png`
- Create: `Modelle/Exports/living_boss_v1/review/client_hitbox.png`
- Create: `Modelle/Exports/living_boss_v1/review/comparison.html`

- [ ] **Step 1: Gesamte Pilot-Suite ausführen**

Run: `python -m unittest discover -s tools/living_boss_tripo/tests -v`

Expected: alle Tests PASS, keine Skips in den nicht visuellen Verträgen.

Run: `python tools/living_boss_tripo/validate.py --root run/resourcepacks/living_boss_v1_preview`

Expected: exakt eine `LIVING_BOSS_VALIDATE_PASS`-Zeile.

Run: `.\gradlew.bat compileJava`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Client mit Preview-Pack starten**

`runClient` starten, den lokalen Teststand laden, Preview-Pack aktivieren und einen Living Boss mit `NoAI:1b` für Idle sowie einen zweiten in einem trockenen, eingegrenzten Testbereich für Bewegung spawnen.

- [ ] **Step 3: Laufzeitnachweise aufnehmen**

Idle, zwei gegenüberliegende Laufphasen und `F3+B`-Hitbox aufnehmen. Prüfen: Kopf zeigt entlang der tatsächlichen Verschiebung; vier Beine bewegen sich diagonal gegensinnig; keine Schnitte oder schwebenden Teile; Textur und Kristalle bleiben korrekt; Hitbox umfasst die Hauptmasse und schneidet den Körper nicht ab.

- [ ] **Step 4: Vergleichsseite erstellen**

`comparison.html` zeigt Konzept, Tripo-Quelle, Blockbench-Review und vier echte Clientbilder nebeneinander. Jede Behauptung nennt ihre Quelle; eine noch nicht beobachtete Animation wird ausdrücklich als offen markiert.

- [ ] **Step 5: Finale Nichtregression prüfen**

Run: `git diff --check`

Run: `python -m unittest tools.living_boss_tripo.tests.test_scope -v`

Expected: PASS. Slimes, Corrupted Silverfish, Endermite, Rüstungen und Kronen sind unverändert.

- [ ] **Step 6: Clientnachweise committen und stoppen**

```text
git add Modelle/Exports/living_boss_v1/review/client_idle.png Modelle/Exports/living_boss_v1/review/client_walk_a.png Modelle/Exports/living_boss_v1/review/client_walk_b.png Modelle/Exports/living_boss_v1/review/client_hitbox.png Modelle/Exports/living_boss_v1/review/comparison.html
git commit -m "test: verify Living Boss Tripo model in client"
```

Nach diesem Commit wird gestoppt und die visuelle Freigabe des Living Boss eingeholt. Produktionsassets werden erst in einem separaten, ausdrücklich bestätigten Promotion-Schritt ersetzt. Die restlichen sieben Mobs erhalten danach eigene Pläne auf Basis der tatsächlich bewährten Pilot-Pipeline.
