# Helping Allay Exact Model Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert a bonded vanilla Allay into a dedicated Helping Allay entity with an approved exact textured Tripo mesh, synchronised support animations, and preserved existing gameplay.

**Architecture:** The Amethyst interaction replaces the vanilla Allay with a registered `HelpingAllayEntity`, which carries owner, support duration, and an explicitly synchronised action state. A dedicated renderer draws only the exact exported Tripo triangles over a transparent vanilla-compatible base model; the source GLB, albedo, runtime mesh, report, and comparison renders remain auditable.

**Tech Stack:** Minecraft 1.20.1, Forge 47.4.16, Java 17, GeckoLib-compatible rig conventions, UMR exact `.mesh` runtime, Python 3, Pillow, NumPy, Tripo multi-view export

---

## File map

- Create `Modelle/Exports/helping_allay_v1/concept/helping_allay_four_view.png`: approved multi-view concept.
- Create `Modelle/Exports/helping_allay_v1/tripo_input/front.png`: cropped Tripo front input.
- Create `Modelle/Exports/helping_allay_v1/tripo_input/right.png`: cropped Tripo right input.
- Create `Modelle/Exports/helping_allay_v1/tripo_input/back.png`: cropped Tripo back input.
- Create `Modelle/Exports/helping_allay_v1/tripo_input/left.png`: cropped Tripo left input.
- Create `Modelle/Exports/helping_allay_v1/tripo_export/helping_allay.glb`: untouched downloaded Tripo source.
- Create `Modelle/Exports/helping_allay_v1/review/source_provenance.json`: source URL, generation settings, hashes, and approval state.
- Modify `tools/mob_tripo/exact_runtime.py`: add exact Helping Allay segmentation and runtime export.
- Modify `tools/mob_tripo/tests/test_exact_runtime.py`: test exact triangle and bone preservation.
- Create `tools/tests/test_helping_allay_contract.py`: protect entity registration, conversion, synced actions, renderer, and sounds.
- Create `src/main/java/com/Momik/usless_mobs/entity/HelpingAllayEntity.java`: bonded helper state and support actions.
- Modify `src/main/java/com/Momik/usless_mobs/event/HelpingSoulHandler.java`: convert vanilla Allays and migrate legacy helped Allays.
- Modify `src/main/java/com/Momik/usless_mobs/registry/ModEntities.java`: register the dedicated entity type.
- Create `src/main/java/com/Momik/usless_mobs/client/HelpingAllayRenderer.java`: transparent base plus exact mesh layer.
- Create `src/main/java/com/Momik/usless_mobs/client/HelpingAllayExactLayer.java`: action-aware exact mesh animation.
- Modify `src/main/java/com/Momik/usless_mobs/client/CustomMobModelLayers.java`: add exact texture location.
- Modify `src/main/java/com/Momik/usless_mobs/Usless_mobs.java`: register attributes and renderer.
- Modify `src/main/resources/assets/usless_mobs/sounds.json`: add custom layered Helping Allay events.
- Create `src/main/resources/assets/usless_mobs/meshes/entity/custom3d/helping_allay.mesh`: exact runtime triangles.
- Create `src/main/resources/assets/usless_mobs/meshes/entity/custom3d/helping_allay.report.json`: exact export audit.
- Create `src/main/resources/assets/usless_mobs/textures/entity/custom3d/exact/helping_allay.png`: untouched Tripo albedo.

### Task 1: Create and review the four-view Helping Allay concept

- [ ] **Step 1: Generate one consistent four-view reference sheet**

Use the built-in image generator with this prompt:

```text
Create one 2x2 orthographic Minecraft voxel-game creature reference sheet for a small spectral Helping Allay. Place FRONT at top-left, RIGHT at top-right, BACK at bottom-left, and LEFT at bottom-right. Show exactly four separate full-body views of the same character. Use a flat removable background, no scenery, no perspective, no shadows crossing the character, and equal scale and alignment in every view. Recognisable Allay silhouette: small floating body, large friendly square head, two slim arms, two symmetrical pairs of layered wings. Refined cyan soul-light body, amethyst accents, restrained gold trim, a clearly visible glowing soul core in the centre of the chest, readable face and eyes. Detailed pixel/voxel texture variation like the approved Corrupted Silverfish, but no armour bulk and no extra limbs. All four views must preserve identical proportions, markings, wing count, and materials.
```

Save the accepted output as `Modelle/Exports/helping_allay_v1/concept/helping_allay_four_view.png`.

- [ ] **Step 2: Validate the concept mechanically**

Run:

```powershell
python tools/mob_tripo/prepare_multiview_sheet.py `
  Modelle/Exports/helping_allay_v1/concept/helping_allay_four_view.png `
  Modelle/Exports/helping_allay_v1/tripo_input
```

Expected: a non-empty multi-view PNG with four consistently sized views and no crop through wings or arms.

- [ ] **Step 3: Show the concept checkpoint**

Display the absolute concept image path to Andrin and report any visible mismatch. Continue only with an image that clearly reads as an Allay and has a visible front-facing soul core.

- [ ] **Step 4: Commit the concept checkpoint**

```powershell
git add -- Modelle/Exports/helping_allay_v1/concept/helping_allay_four_view.png Modelle/Exports/helping_allay_v1/tripo_input/front.png Modelle/Exports/helping_allay_v1/tripo_input/right.png Modelle/Exports/helping_allay_v1/tripo_input/back.png Modelle/Exports/helping_allay_v1/tripo_input/left.png
git commit -m "art: add approved Helping Allay multiview concept"
```

### Task 2: Generate and archive the textured Tripo source

- [ ] **Step 1: Generate from Multi-View Builder**

Open Tripo's Multi-View Builder, upload the front/left/back/right views in their matching slots, enable PBR and the highest useful texture quality available to the authenticated account, and generate one textured model. Do not use text-to-3D or a single-view fallback.

- [ ] **Step 2: Reject wrong generation states**

Reject and regenerate if any of these are visible:

```text
wrong front axis
missing or duplicated wing
hidden chest core
different markings between sides
solid fused arms
unreadable face
untextured white material
back-facing head
severe holes or detached geometry
```

- [ ] **Step 3: Export the untouched source**

Download the textured GLB to:

```text
Modelle/Exports/helping_allay_v1/tripo_export/helping_allay.glb
```

Do not run voxelisation, cube conversion, decimation, or texture rebaking.

- [ ] **Step 4: Record source provenance**

Run `Get-FileHash -Algorithm SHA256 Modelle/Exports/helping_allay_v1/tripo_export/helping_allay.glb`. Create `Modelle/Exports/helping_allay_v1/review/source_provenance.json` with `mob` set to `helping_allay`, `generator` set to `Tripo Multi-View Builder`, `workspace_url` copied verbatim from the completed Tripo workspace, `texture_quality` copied from the selected export setting, `pbr` set to `true`, `source_glb` set to `../tripo_export/helping_allay.glb`, `source_sha256` set to the hash printed by the command, `approved_front_axis` set to `-Z`, and `approved` set to `true`.

- [ ] **Step 5: Commit the immutable source**

```powershell
git add -- Modelle/Exports/helping_allay_v1/tripo_export/helping_allay.glb Modelle/Exports/helping_allay_v1/review/source_provenance.json
git commit -m "art: archive textured Helping Allay Tripo source"
```

### Task 3: Add the exact Helping Allay runtime exporter

- [ ] **Step 1: Write failing exporter tests**

Extend `tools/mob_tripo/tests/test_exact_runtime.py` with:

```python
def test_helping_allay_spec_uses_nine_visible_motion_regions(self):
    spec = MOB_SPECS["helping_allay"]
    self.assertEqual(1, spec.fit_axis)
    self.assertAlmostEqual(10.4, spec.fit_span)
    self.assertEqual(
        (
            "body", "head", "right_arm", "left_arm",
            "right_wing", "right_wing_tip",
            "left_wing", "left_wing_tip", "soul_core",
        ),
        spec.bones,
    )

def test_helping_allay_export_preserves_every_source_triangle(self):
    mesh_bytes, texture_bytes, report = build_runtime_assets("helping_allay", self.mesh)
    decoded = decode_mesh(mesh_bytes)
    self.assertEqual(report["source_triangles"], report["output_triangles"])
    self.assertEqual(report["source_triangles"], sum(len(part["faces"]) for part in decoded.values()))
    self.assertEqual(set(MOB_SPECS["helping_allay"].bones), set(decoded))
    self.assertTrue(texture_bytes.startswith(b"\x89PNG\r\n\x1a\n"))
```

- [ ] **Step 2: Run the focused test and verify failure**

Run:

```powershell
python -m unittest tools.mob_tripo.tests.test_exact_runtime -v
```

Expected: FAIL because `helping_allay` is absent from `MOB_SPECS`.

- [ ] **Step 3: Add the exact exporter spec and classifier**

Add this spec to `tools/mob_tripo/exact_runtime.py`:

```python
"helping_allay": MobSpec(
    (
        "body", "head", "right_arm", "left_arm",
        "right_wing", "right_wing_tip",
        "left_wing", "left_wing_tip", "soul_core",
    ),
    "allay",
    fit_axis=1,
    fit_span=10.4,
),
```

Add this classifier:

```python
def _classify_allay(points: np.ndarray) -> str:
    x = points[:, 0] - 0.5
    y = points[:, 1]
    z = points[:, 2] - 0.5
    absolute_x = np.abs(x)
    if float(y.min()) > 0.68 and float(absolute_x.max()) < 0.40:
        return "head"
    if 0.26 < float(y.mean()) < 0.70 and np.all(absolute_x > 0.20) and np.all(absolute_x < 0.36):
        return "right_arm" if float(x.mean()) < 0 else "left_arm"
    if np.all(absolute_x > 0.34):
        side = "right" if float(x.mean()) < 0 else "left"
        suffix = "_tip" if np.all(absolute_x > 0.44) else ""
        return f"{side}_wing{suffix}"
    if 0.40 < float(y.mean()) < 0.68 and float(absolute_x.max()) < 0.16 and float(z.mean()) < -0.03:
        return "soul_core"
    return "body"
```

Register it as `"allay": _classify_allay`. Add this `_pivots` branch:

```python
elif spec.classifier == "allay":
    result.update({
        "head": (0.0, 24.0 - height * 0.70, -depth * 0.04),
        "right_arm": (-width * 0.20, 24.0 - height * 0.55, -depth * 0.05),
        "left_arm": (width * 0.20, 24.0 - height * 0.55, -depth * 0.05),
        "right_wing": (-width * 0.25, 24.0 - height * 0.58, depth * 0.06),
        "right_wing_tip": (-width * 0.42, 24.0 - height * 0.56, depth * 0.08),
        "left_wing": (width * 0.25, 24.0 - height * 0.58, depth * 0.06),
        "left_wing_tip": (width * 0.42, 24.0 - height * 0.56, depth * 0.08),
        "soul_core": (0.0, 24.0 - height * 0.53, -depth * 0.28),
    })
```

- [ ] **Step 4: Run exporter tests**

```powershell
python -m unittest tools.mob_tripo.tests.test_exact_runtime -v
```

Expected: all exact runtime tests PASS and source/output triangle counts match.

- [ ] **Step 5: Export the real runtime assets**

```powershell
python tools/mob_tripo/exact_runtime.py helping_allay Modelle/Exports/helping_allay_v1/tripo_export/helping_allay.glb
```

Expected: one `EXACT_MOB_EXPORT_PASS` line for `mob=helping_allay`, with a positive triangle count, `bones=9`, positive texture dimensions, and `cubes=0`.

- [ ] **Step 6: Commit exporter and assets**

```powershell
git add -- tools/mob_tripo/exact_runtime.py tools/mob_tripo/tests/test_exact_runtime.py src/main/resources/assets/usless_mobs/meshes/entity/custom3d/helping_allay.mesh src/main/resources/assets/usless_mobs/meshes/entity/custom3d/helping_allay.report.json src/main/resources/assets/usless_mobs/textures/entity/custom3d/exact/helping_allay.png
git commit -m "feat: export exact Helping Allay runtime mesh"
```

### Task 4: Register a dedicated Helping Allay entity

- [ ] **Step 1: Write the failing entity contract test**

Create `tools/tests/test_helping_allay_contract.py`:

```python
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

class HelpingAllayContractTests(unittest.TestCase):
    def test_entity_has_persistent_bond_and_synced_action(self):
        source = (ROOT / "src/main/java/com/Momik/usless_mobs/entity/HelpingAllayEntity.java").read_text(encoding="utf-8")
        self.assertIn("extends Allay", source)
        self.assertIn("OWNER", source)
        self.assertIn("SUPPORT_UNTIL", source)
        self.assertIn("ACTION", source)
        self.assertIn("addAdditionalSaveData", source)
        self.assertIn("readAdditionalSaveData", source)

    def test_registry_and_client_setup_include_helping_allay(self):
        entities = (ROOT / "src/main/java/com/Momik/usless_mobs/registry/ModEntities.java").read_text(encoding="utf-8")
        setup = (ROOT / "src/main/java/com/Momik/usless_mobs/Usless_mobs.java").read_text(encoding="utf-8")
        self.assertIn('register("helping_allay"', entities)
        self.assertIn("ModEntities.HELPING_ALLAY.get()", setup)
        self.assertIn("HelpingAllayRenderer::new", setup)

if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 2: Run the contract test and verify failure**

```powershell
python -m unittest tools.tests.test_helping_allay_contract -v
```

Expected: FAIL because the entity class does not exist.

- [ ] **Step 3: Create the entity with synced action states**

Create `HelpingAllayEntity.java` with:

```java
public final class HelpingAllayEntity extends Allay {
    public static final byte ACTION_IDLE = 0;
    public static final byte ACTION_REVEAL = 1;
    public static final byte ACTION_SHIELD = 2;
    public static final byte ACTION_HEAL = 3;
    public static final byte ACTION_BOND = 4;
    public static final byte ACTION_TELEPORT = 5;

    private static final EntityDataAccessor<Optional<UUID>> OWNER =
            SynchedEntityData.defineId(HelpingAllayEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Long> SUPPORT_UNTIL =
            SynchedEntityData.defineId(HelpingAllayEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Byte> ACTION =
            SynchedEntityData.defineId(HelpingAllayEntity.class, EntityDataSerializers.BYTE);
    private int actionTicks;

    public HelpingAllayEntity(EntityType<? extends Allay> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Allay.createAttributes();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER, Optional.empty());
        this.entityData.define(SUPPORT_UNTIL, 0L);
        this.entityData.define(ACTION, ACTION_IDLE);
    }

    public void bind(UUID owner, long supportUntil) {
        this.entityData.set(OWNER, Optional.of(owner));
        this.entityData.set(SUPPORT_UNTIL, supportUntil);
    }

    public byte action() {
        return this.entityData.get(ACTION);
    }

    public void playAction(byte action, int ticks) {
        this.entityData.set(ACTION, action);
        this.actionTicks = Math.max(1, ticks);
    }
}
```

Complete `aiStep`, owner lookup, support expiry, reveal, shield, heal, teleport, and NBT methods by moving the existing behaviour from `HelpingSoulHandler` without changing its numerical balance.

- [ ] **Step 4: Register the entity, attributes, and renderer hook**

Add `HELPING_ALLAY` to `ModEntities`:

```java
public static final RegistryObject<EntityType<HelpingAllayEntity>> HELPING_ALLAY =
        ENTITY_TYPES.register("helping_allay", () ->
                EntityType.Builder.of(HelpingAllayEntity::new, MobCategory.CREATURE)
                        .sized(0.35F, 0.6F)
                        .clientTrackingRange(10)
                        .updateInterval(2)
                        .build(Usless_mobs.MODID + ":helping_allay"));
```

Register `HelpingAllayEntity.createAttributes().build()` and `HelpingAllayRenderer::new` in `Usless_mobs.ModEvents` and `Usless_mobs.ClientModEvents`.

- [ ] **Step 5: Run contract and Java compilation**

```powershell
python -m unittest tools.tests.test_helping_allay_contract -v
.\gradlew.bat compileJava
```

Expected: contract PASS and `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit the entity slice**

```powershell
git add -- tools/tests/test_helping_allay_contract.py src/main/java/com/Momik/usless_mobs/entity/HelpingAllayEntity.java src/main/java/com/Momik/usless_mobs/registry/ModEntities.java src/main/java/com/Momik/usless_mobs/Usless_mobs.java
git commit -m "feat: add bonded Helping Allay entity"
```

### Task 5: Convert and migrate bonded vanilla Allays

- [ ] **Step 1: Add failing conversion assertions**

Extend `test_helping_allay_contract.py`:

```python
def test_handler_converts_and_migrates_vanilla_allays(self):
    source = (ROOT / "src/main/java/com/Momik/usless_mobs/event/HelpingSoulHandler.java").read_text(encoding="utf-8")
    self.assertIn("convertToHelpingAllay", source)
    self.assertIn("ModEntities.HELPING_ALLAY.get().create", source)
    self.assertIn("copyAllayState", source)
    self.assertIn("allay.discard()", source)
```

- [ ] **Step 2: Verify failure**

```powershell
python -m unittest tools.tests.test_helping_allay_contract -v
```

Expected: FAIL because conversion helpers are absent.

- [ ] **Step 3: Implement lossless conversion**

Refactor `HelpingSoulHandler` so Amethyst interaction creates `HelpingAllayEntity`, copies position, rotation, custom name, persistence, health, held item, no-AI state, silence state, and owner/support data, then adds the replacement before discarding the vanilla entity. Keep Glow Flare extension on the custom entity.

On server tick, migrate legacy vanilla Allays carrying `UslessMobsHelpingSoulGiven`, `UslessMobsHelpingSoulOwner`, and `UslessMobsHelpingSoulSupportUntil` through the same conversion path. Never convert ordinary Allays.

- [ ] **Step 4: Run focused checks**

```powershell
python -m unittest tools.tests.test_helping_allay_contract -v
.\gradlew.bat compileJava
```

Expected: PASS and `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit conversion**

```powershell
git add -- src/main/java/com/Momik/usless_mobs/event/HelpingSoulHandler.java src/main/java/com/Momik/usless_mobs/entity/HelpingAllayEntity.java tools/tests/test_helping_allay_contract.py
git commit -m "feat: convert bonded Allays without losing state"
```

### Task 6: Render and animate the exact Helping Allay

- [ ] **Step 1: Add failing renderer assertions**

Extend `test_helping_allay_contract.py`:

```python
def test_renderer_uses_only_exact_mesh_and_original_albedo(self):
    renderer = (ROOT / "src/main/java/com/Momik/usless_mobs/client/HelpingAllayRenderer.java").read_text(encoding="utf-8")
    layer = (ROOT / "src/main/java/com/Momik/usless_mobs/client/HelpingAllayExactLayer.java").read_text(encoding="utf-8")
    self.assertIn("TRANSPARENT_BASE_TEXTURE", renderer)
    self.assertIn('ExactMobMesh.load(resourceManager, "helping_allay"', layer)
    self.assertIn("HELPING_ALLAY_EXACT_TEXTURE", layer)
    self.assertIn("ACTION_SHIELD", layer)
    self.assertIn("ACTION_HEAL", layer)
    self.assertNotIn("CustomMob3DLayer", renderer + layer)
```

- [ ] **Step 2: Verify failure**

```powershell
python -m unittest tools.tests.test_helping_allay_contract -v
```

Expected: FAIL because renderer files are absent.

- [ ] **Step 3: Create the renderer**

Create `HelpingAllayRenderer` as a `MobRenderer<HelpingAllayEntity, AllayModel<HelpingAllayEntity>>`, use `ModelLayers.ALLAY`, return `TRANSPARENT_BASE_TEXTURE`, keep a small shadow, add vanilla `ItemInHandLayer`, and add `HelpingAllayExactLayer` last.

- [ ] **Step 4: Create action-aware exact animation**

`HelpingAllayExactLayer` must load exactly these bones:

```java
Set.of(
    "body", "head", "right_arm", "left_arm",
    "right_wing", "right_wing_tip",
    "left_wing", "left_wing_tip", "soul_core"
)
```

Use continuous wing motion for idle/flight, increased amplitude for shield and heal actions, forward arm motion for reveal, chest-core pulse translation for healing, and a fast closed-wing pose during teleport. Render the original albedo with stable material lighting and no cuboid overlay.

- [ ] **Step 5: Register the exact texture location**

Add to `CustomMobModelLayers`:

```java
public static final ResourceLocation HELPING_ALLAY_EXACT_TEXTURE =
        texture("textures/entity/custom3d/exact/helping_allay.png");
```

- [ ] **Step 6: Run renderer and compile checks**

```powershell
python -m unittest tools.tests.test_helping_allay_contract tools.tests.test_remaining_mob_renderers -v
.\gradlew.bat compileJava
```

Expected: all tests PASS and `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit rendering**

```powershell
git add -- src/main/java/com/Momik/usless_mobs/client/HelpingAllayRenderer.java src/main/java/com/Momik/usless_mobs/client/HelpingAllayExactLayer.java src/main/java/com/Momik/usless_mobs/client/CustomMobModelLayers.java tools/tests/test_helping_allay_contract.py
git commit -m "feat: render animated exact Helping Allay mesh"
```

### Task 7: Add distinct sound identity and synchronise effects

- [ ] **Step 1: Add failing sound contract assertions**

Add to `test_helping_allay_contract.py`:

```python
def test_helping_allay_has_bond_reveal_shield_heal_and_return_sounds(self):
    sounds = (ROOT / "src/main/resources/assets/usless_mobs/sounds.json").read_text(encoding="utf-8")
    for event in ("helping_allay_bond", "helping_allay_reveal", "helping_allay_shield", "helping_allay_heal", "helping_allay_return"):
        self.assertIn(f'"{event}"', sounds)
```

- [ ] **Step 2: Add custom layered sound events**

Define five sound events in `sounds.json` using restrained layers of existing amethyst, allay, beacon, and soul sounds with distinct pitch ranges. Register corresponding `SoundEvent` objects in a focused main-module `ModSounds` registry and play each event from the server action that sets the matching synced animation state.

- [ ] **Step 3: Verify**

```powershell
python -m unittest tools.tests.test_helping_allay_contract -v
.\gradlew.bat compileJava
```

Expected: PASS and no missing sound registry errors at compile time.

- [ ] **Step 4: Commit sounds**

```powershell
git add -- src/main/java/com/Momik/usless_mobs/registry/ModSounds.java src/main/java/com/Momik/usless_mobs/Usless_mobs.java src/main/java/com/Momik/usless_mobs/entity/HelpingAllayEntity.java src/main/resources/assets/usless_mobs/sounds.json tools/tests/test_helping_allay_contract.py
git commit -m "feat: add Helping Allay action sounds"
```

### Task 8: Full verification and in-game visual checkpoint

- [ ] **Step 1: Run project-truth and focused Python suites**

```powershell
python tools/verify_umr_project_truth.py
python -m unittest tools.mob_tripo.tests.test_exact_runtime tools.tests.test_helping_allay_contract tools.tests.test_remaining_mob_renderers -v
```

Expected: `UMR_PROJECT_TRUTH_PASS` and all focused tests PASS.

- [ ] **Step 2: Build the mod**

```powershell
.\gradlew.bat build
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Run the client and verify the real entity**

Test this sequence in a development world:

```text
1. Spawn an ordinary Allay.
2. Give it an Amethyst Shard.
3. Confirm exactly one Helping Allay replaces it.
4. Confirm held item, name, position, and persistence survive conversion.
5. Trigger enemy reveal, shield, healing, and long-range return.
6. Confirm each action has matching animation, particles, and sound.
7. Enable F3+B and compare the hitbox with the visible body.
8. Save, quit, reload, and confirm owner/support state remains.
9. Verify ordinary Allays still use their vanilla model and behaviour.
```

- [ ] **Step 4: Produce comparison images**

Capture front, side, back, flight, shield, and healing frames under `Modelle/Exports/helping_allay_v1/review/`. Compare them against the approved four-view concept and Tripo preview.

- [ ] **Step 5: Commit verified review evidence**

```powershell
git add -- Modelle/Exports/helping_allay_v1/review
git commit -m "test: verify Helping Allay in game"
```

- [ ] **Step 6: Mark model number 1 complete only after acceptance**

Do not start the Octopus until the exact mesh report, build, persistence test, hitbox test, and comparison images all pass.
