# True Octopus Exact Model Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Finish the true Octopus as a correctly scaled, animated, exact textured Tripo creature with eight equal arms, intelligent camouflage, squeezing, object interaction, ambush behaviour, custom sounds, and in-game visual proof.

**Architecture:** Preserve `octopus_tripo_textured_4k_20260821.glb` as the immutable visual source and export every source triangle once into the existing nine-region exact runtime mesh (`body` plus eight arms). Keep gameplay authoritative in `OctopusEntity`, expose only compact synced visual states to the renderer, and extend the shared exact layer only where the Octopus needs action-specific poses. Do not use the old cube preview as runtime geometry and do not touch unrelated dirty mob work.

**Tech Stack:** Minecraft 1.20.1, Forge 47.4.16, Java 17, existing UMR exact-mesh renderer, Tripo GLB/4K albedo, Python `unittest`, Pillow, Gradle.

---

## File map

- `Modelle/Exports/octopus_v1/tripo_export/octopus_tripo_textured_4k_20260821.glb`: immutable approved Tripo source.
- `Modelle/Exports/octopus_v1/review/source_provenance.json`: source URL, hash, orientation, scale, texture, and approval record.
- `tools/mob_tripo/exact_runtime.py`: lossless GLB-to-runtime region export; no voxelisation or decimation.
- `tools/mob_tripo/tests/test_exact_runtime.py`: triangle, bone, size, UV, and deterministic-export contract.
- `tools/tests/test_octopus_contract.py`: entity, renderer, behaviour, sound, dimensions, and resource contract.
- `src/main/java/com/Momik/usless_mobs/entity/OctopusEntity.java`: server-authoritative state machine and gameplay.
- `src/main/java/com/Momik/usless_mobs/client/OctopusRenderer.java`: transparent vanilla carrier plus exact Tripo layer.
- `src/main/java/com/Momik/usless_mobs/client/ExactMobMeshLayer.java`: arm poses and visual response to synced Octopus actions.
- `src/main/java/com/Momik/usless_mobs/registry/ModSounds.java`: Octopus sound registrations.
- `src/main/resources/assets/usless_mobs/sounds.json`: Octopus sound event definitions.
- `src/main/resources/assets/usless_mobs/meshes/entity/custom3d/octopus.mesh`: generated exact runtime mesh.
- `src/main/resources/assets/usless_mobs/meshes/entity/custom3d/octopus.report.json`: generated audit report.
- `src/main/resources/assets/usless_mobs/textures/entity/custom3d/exact/octopus.png`: untouched 4096 × 4096 Tripo albedo.
- `Modelle/Exports/octopus_v1/review/`: front/right/back/top/perspective, action, hitbox, and comparison evidence.

### Task 1: Freeze and validate the approved source

**Files:**
- Create: `Modelle/Exports/octopus_v1/review/source_provenance.json`
- Create: `tools/tests/test_octopus_contract.py`
- Test: `tools/tests/test_octopus_contract.py`

- [ ] **Step 1: Write the failing provenance test**

```python
def test_approved_octopus_source_is_archived_and_hashed(self):
    provenance = json.loads((OCTOPUS / "review/source_provenance.json").read_text("utf-8"))
    source = OCTOPUS / provenance["source_glb"]
    self.assertEqual("Tripo Multi-View Builder", provenance["generator"])
    self.assertEqual("4096x4096", provenance["texture_quality"])
    self.assertTrue(provenance["approved"])
    self.assertEqual(sha256(source.read_bytes()).hexdigest(), provenance["source_sha256"])
    self.assertEqual("-Z", provenance["approved_front_axis"])
```

- [ ] **Step 2: Run the test and confirm the missing-file failure**

Run: `python -m unittest tools.tests.test_octopus_contract.OctopusContract.test_approved_octopus_source_is_archived_and_hashed -v`

Expected: FAIL because `source_provenance.json` does not exist.

- [ ] **Step 3: Record the real source metadata**

Compute the SHA-256 with `Get-FileHash -Algorithm SHA256 Modelle/Exports/octopus_v1/tripo_export/octopus_tripo_textured_4k_20260821.glb` and create:

```json
{
  "mob": "octopus",
  "generator": "Tripo Multi-View Builder",
  "workspace_url": "https://studio.tripo3d.ai/workspace/generate/e3087477-bc37-41b6-b3e2-39fe8c25dd7e",
  "source_glb": "../tripo_export/octopus_tripo_textured_4k_20260821.glb",
  "source_sha256": "53f93da6502fb4bd13f0255529c8bf41db077e8401305188e5558c27d4b885e9",
  "texture_quality": "4096x4096",
  "pbr": true,
  "approved_front_axis": "-Z",
  "approved": true,
  "runtime_geometry": "exact-triangles",
  "runtime_cubes": 0
}
```

- [ ] **Step 4: Run the test and confirm it passes**

Run: `python -m unittest tools.tests.test_octopus_contract.OctopusContract.test_approved_octopus_source_is_archived_and_hashed -v`

Expected: PASS.

- [ ] **Step 5: Commit only the Octopus provenance slice**

```powershell
git add -- Modelle/Exports/octopus_v1/review/source_provenance.json tools/tests/test_octopus_contract.py
git diff --cached --check
git commit -m "test: lock approved Octopus Tripo source"
```

### Task 2: Prove lossless exact geometry and correct scale

**Files:**
- Modify: `tools/mob_tripo/tests/test_exact_runtime.py`
- Modify only if the new test proves it necessary: `tools/mob_tripo/exact_runtime.py`
- Generate: `src/main/resources/assets/usless_mobs/meshes/entity/custom3d/octopus.mesh`
- Generate: `src/main/resources/assets/usless_mobs/meshes/entity/custom3d/octopus.report.json`
- Generate: `src/main/resources/assets/usless_mobs/textures/entity/custom3d/exact/octopus.png`

- [ ] **Step 1: Add a failing Octopus-specific exact-export test**

```python
def test_octopus_preserves_all_triangles_in_nine_nonempty_regions(self):
    source = load_glb(ROOT / "Modelle/Exports/octopus_v1/tripo_export/octopus_tripo_textured_4k_20260821.glb")
    payload, texture, report = build_runtime_assets("octopus", source)
    decoded = decode_mesh(payload)
    self.assertEqual(source.triangles.shape[0], report["source_triangles"])
    self.assertEqual(report["source_triangles"], report["output_triangles"])
    self.assertEqual({"body", *(f"tentacle{i}" for i in range(8))}, set(decoded))
    self.assertTrue(all(len(part["faces"]) > 0 for part in decoded.values()))
    self.assertEqual((4096, 4096), (report["texture_width"], report["texture_height"]))
    self.assertEqual(0, report["cubes"])
    self.assertEqual(14.4, report["fit_span"])
```

- [ ] **Step 2: Run the focused test and verify RED for any missing contract**

Run: `python -m unittest tools.mob_tripo.tests.test_exact_runtime.ExactRuntimeTest.test_octopus_preserves_all_triangles_in_nine_nonempty_regions -v`

Expected: FAIL until the source-based contract and any required classifier correction are complete.

- [ ] **Step 3: Correct only proven classifier or orientation defects**

Keep the declared specification equivalent to:

```python
"octopus": MobSpec(
    ("body",) + tuple(f"tentacle{index}" for index in range(8)),
    "octopus",
    fit_axis=0,
    fit_span=14.4,
)
```

The classifier may reassign complete source triangles among the nine regions, but it must not add, remove, decimate, remesh, voxelise, or rebake UVs.

- [ ] **Step 4: Export deterministically from the approved GLB**

Run:

```powershell
python tools/mob_tripo/exact_runtime.py octopus Modelle/Exports/octopus_v1/tripo_export/octopus_tripo_textured_4k_20260821.glb
python tools/mob_tripo/diagnose_uv_fidelity.py octopus
```

Expected: `EXACT_MOB_EXPORT_PASS ... triangles=95946 bones=9 texture=4096x4096 cubes=0`, followed by a UV-fidelity pass with no missing material or UV data.

- [ ] **Step 5: Run all exact-runtime tests**

Run: `python -m unittest tools.mob_tripo.tests.test_exact_runtime -v`

Expected: PASS.

- [ ] **Step 6: Commit the exact Octopus asset slice**

```powershell
git add -- tools/mob_tripo/tests/test_exact_runtime.py tools/mob_tripo/exact_runtime.py src/main/resources/assets/usless_mobs/meshes/entity/custom3d/octopus.mesh src/main/resources/assets/usless_mobs/meshes/entity/custom3d/octopus.report.json src/main/resources/assets/usless_mobs/textures/entity/custom3d/exact/octopus.png
git diff --cached --check
git commit -m "feat: export exact textured Octopus mesh"
```

Do not stage `octopus Tripo Cubes.bbmodel` or `tripo_cube_preview.png` as runtime proof.

### Task 3: Add a server-authoritative Octopus action state

**Files:**
- Modify: `src/main/java/com/Momik/usless_mobs/entity/OctopusEntity.java`
- Modify: `tools/tests/test_octopus_contract.py`

- [ ] **Step 1: Write failing state and persistence contracts**

```python
def test_octopus_syncs_and_persists_visual_action_state(self):
    source = ENTITY.read_text("utf-8")
    self.assertIn("EntityDataAccessor<Byte> ACTION_STATE", source)
    self.assertIn("EntityDataAccessor<Boolean> SQUEEZING", source)
    self.assertIn("entityData.define(ACTION_STATE, ACTION_IDLE)", source)
    self.assertIn('tag.putByte("OctopusAction", getActionState())', source)
    self.assertIn('tag.putBoolean("OctopusSqueezing", isSqueezing())', source)
    self.assertIn("setActionState(tag.getByte(\"OctopusAction\"))", source)
```

- [ ] **Step 2: Run the test and verify RED**

Run: `python -m unittest tools.tests.test_octopus_contract.OctopusContract.test_octopus_syncs_and_persists_visual_action_state -v`

Expected: FAIL because the synced fields do not yet exist.

- [ ] **Step 3: Add the minimal explicit state API**

Add byte constants `ACTION_IDLE`, `ACTION_SWIM`, `ACTION_AMBUSH`, `ACTION_GRAB`, `ACTION_INK`, `ACTION_CAMOUFLAGE`, and `ACTION_OBJECT`. Define synced data in `defineSynchedData`, validate state values in `setActionState`, and save/load `OctopusAction` plus `OctopusSqueezing`. Server gameplay writes the state; the client renderer only reads it.

- [ ] **Step 4: Run the focused contract**

Run: `python -m unittest tools.tests.test_octopus_contract.OctopusContract.test_octopus_syncs_and_persists_visual_action_state -v`

Expected: PASS.

- [ ] **Step 5: Compile and commit**

```powershell
.\gradlew.bat compileJava
git add -- src/main/java/com/Momik/usless_mobs/entity/OctopusEntity.java tools/tests/test_octopus_contract.py
git diff --cached --check
git commit -m "feat: synchronise Octopus action states"
```

Expected: `BUILD SUCCESSFUL`.

### Task 4: Implement intelligent behaviour without turning it into a Squid

**Files:**
- Modify: `src/main/java/com/Momik/usless_mobs/entity/OctopusEntity.java`
- Modify: `src/main/java/com/Momik/usless_mobs/registry/ModEntities.java`
- Modify: `tools/tests/test_octopus_contract.py`

- [ ] **Step 1: Write failing gameplay contracts**

```python
def test_octopus_has_eight_arm_ambush_camouflage_squeeze_and_object_behaviour(self):
    entity = ENTITY.read_text("utf-8")
    registry = ENTITIES.read_text("utf-8")
    self.assertIn("beginAmbush", entity)
    self.assertIn("beginCamouflage", entity)
    self.assertIn("updateSqueezing", entity)
    self.assertIn("interactWithNearbyObject", entity)
    self.assertIn("refreshDimensions()", entity)
    self.assertIn("getDimensions(Pose pose)", entity)
    self.assertIn("EntityDimensions.scalable(0.62F, 0.48F)", entity)
    self.assertIn(".sized(1.15F, 1.20F)", registry)
```

- [ ] **Step 2: Run the test and verify RED**

Run: `python -m unittest tools.tests.test_octopus_contract.OctopusContract.test_octopus_has_eight_arm_ambush_camouflage_squeeze_and_object_behaviour -v`

Expected: FAIL on the missing explicit behaviours and audited dimensions.

- [ ] **Step 3: Implement the behaviour state machine**

Use these fixed rules:

- `IDLE/SWIM`: normal aquatic navigation; never create fins or long catching tentacles.
- `AMBUSH`: while unseen and within 8 blocks, pause/camouflage for 30–45 ticks, then jet toward the target; cancel if line of sight or water is lost.
- `CAMOUFLAGE`: apply invisibility for at most 110 ticks, clear hostile mob targets, and enforce the existing cooldown so it cannot be chained permanently.
- `SQUEEZING`: only when horizontally blocked and a smaller forward collision probe is free; switch to `0.62 × 0.48` dimensions, call `refreshDimensions`, and exit after clearing the obstruction. The normal registered body remains `1.15 × 1.20`.
- `OBJECT`: at most once per 200 ticks, take one nearby floating item into the main-hand equipment slot if empty; drop it unchanged on hurt, death, or after 160 ticks. Never consume containers or duplicate stacks.
- `GRAB`: retain the current telegraphed pull, but drive the synced `ACTION_GRAB` state for renderer timing.
- `INK`: retain blindness and target clearing, but drive `ACTION_INK`; normal Squid ink remains independent.

- [ ] **Step 4: Run gameplay contracts and compile**

Run:

```powershell
python -m unittest tools.tests.test_octopus_contract -v
.\gradlew.bat compileJava
```

Expected: all Octopus contracts PASS and `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit the gameplay slice**

```powershell
git add -- src/main/java/com/Momik/usless_mobs/entity/OctopusEntity.java src/main/java/com/Momik/usless_mobs/registry/ModEntities.java tools/tests/test_octopus_contract.py
git diff --cached --check
git commit -m "feat: add intelligent Octopus behaviours"
```

### Task 5: Animate all eight exact arms and the action states

**Files:**
- Modify: `src/main/java/com/Momik/usless_mobs/client/ExactMobMeshLayer.java`
- Modify: `src/main/java/com/Momik/usless_mobs/client/OctopusRenderer.java`
- Modify: `tools/tests/test_octopus_contract.py`

- [ ] **Step 1: Write failing renderer contracts**

```python
def test_octopus_renderer_uses_exact_mesh_and_all_action_states(self):
    renderer = RENDERER.read_text("utf-8")
    layer = EXACT_LAYER.read_text("utf-8")
    self.assertEqual(1, renderer.count("new ExactMobMeshLayer<>("))
    self.assertIn("OCTOPUS_EXACT_TEXTURE", renderer)
    self.assertIn("octopusPose", layer)
    for state in ("ACTION_SWIM", "ACTION_AMBUSH", "ACTION_GRAB", "ACTION_INK", "ACTION_CAMOUFLAGE", "ACTION_OBJECT"):
        self.assertIn(state, layer)
    for index in range(8):
        self.assertIn(f'"tentacle{index}"', layer)
```

- [ ] **Step 2: Run the renderer contract and verify RED**

Run: `python -m unittest tools.tests.test_octopus_contract.OctopusContract.test_octopus_renderer_uses_exact_mesh_and_all_action_states -v`

Expected: FAIL because the current generic sine motion ignores action state.

- [ ] **Step 3: Add Octopus-specific poses**

Extract `octopusPose(String bone, OctopusEntity octopus, float age)` and use eight phase offsets. Apply:

- idle: slow independent curling;
- swim: backward power stroke followed by relaxed recovery;
- ambush: arms compressed close to the mantle, then a short forward flare;
- grab: two nearest arms reach together while the other six brace;
- ink: mantle recoil and all-arm outward pulse;
- camouflage: near-motionless breathing only;
- object: one front arm curls inward while seven continue reduced idle motion;
- squeezing: body scaled vertically and arms folded backward, with no mesh cut because each source triangle stays in one region.

Do not change UV coordinates or substitute cubes. Keep the transparent vanilla base and exact 4K texture.

- [ ] **Step 4: Run renderer tests and compile**

Run:

```powershell
python -m unittest tools.tests.test_octopus_contract tools.tests.test_remaining_mob_renderers -v
.\gradlew.bat compileJava
```

Expected: PASS and `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit the animation slice**

```powershell
git add -- src/main/java/com/Momik/usless_mobs/client/ExactMobMeshLayer.java src/main/java/com/Momik/usless_mobs/client/OctopusRenderer.java tools/tests/test_octopus_contract.py
git diff --cached --check
git commit -m "feat: animate exact Octopus action poses"
```

### Task 6: Add a distinct custom sound identity

**Files:**
- Modify: `src/main/java/com/Momik/usless_mobs/registry/ModSounds.java`
- Modify: `src/main/resources/assets/usless_mobs/sounds.json`
- Create: `src/main/resources/assets/usless_mobs/sounds/entity/octopus/ambient_1.ogg`
- Create: `src/main/resources/assets/usless_mobs/sounds/entity/octopus/ink_1.ogg`
- Create: `src/main/resources/assets/usless_mobs/sounds/entity/octopus/grab_1.ogg`
- Create: `src/main/resources/assets/usless_mobs/sounds/entity/octopus/camouflage_1.ogg`
- Create: `src/main/resources/assets/usless_mobs/sounds/entity/octopus/squeeze_1.ogg`
- Modify: `src/main/java/com/Momik/usless_mobs/entity/OctopusEntity.java`
- Modify: `tools/tests/test_octopus_contract.py`

- [ ] **Step 1: Write a failing sound contract**

```python
def test_octopus_has_distinct_registered_sound_events_and_files(self):
    registry = SOUNDS_JAVA.read_text("utf-8")
    manifest = json.loads(SOUNDS_JSON.read_text("utf-8"))
    for event in ("octopus_ambient", "octopus_ink", "octopus_grab", "octopus_camouflage", "octopus_squeeze"):
        self.assertIn(event.upper(), registry)
        self.assertIn(event, manifest)
        for entry in manifest[event]["sounds"]:
            self.assertTrue((ASSETS / f"{entry['name']}.ogg").is_file())
```

- [ ] **Step 2: Run the sound contract and verify RED**

Run: `python -m unittest tools.tests.test_octopus_contract.OctopusContract.test_octopus_has_distinct_registered_sound_events_and_files -v`

Expected: FAIL because Octopus-specific events do not exist.

- [ ] **Step 3: Create and wire the sound set**

Create short original local OGG layers with wet low clicks, soft mantle pulses, suction pops, and a forceful ink burst. Register exactly the five event IDs above and trigger them from the matching server-authoritative state transitions. Do not reuse the Corrupted Silverfish identity and do not call a paid generation service.

- [ ] **Step 4: Validate resources and compile**

Run:

```powershell
python -m unittest tools.tests.test_octopus_contract -v
python -c "import json, pathlib; json.loads(pathlib.Path('src/main/resources/assets/usless_mobs/sounds.json').read_text('utf-8')); print('SOUNDS_JSON_PASS')"
.\gradlew.bat compileJava
```

Expected: Octopus tests PASS, `SOUNDS_JSON_PASS`, and `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit the sound slice**

```powershell
git add -- src/main/java/com/Momik/usless_mobs/registry/ModSounds.java src/main/java/com/Momik/usless_mobs/entity/OctopusEntity.java src/main/resources/assets/usless_mobs/sounds.json src/main/resources/assets/usless_mobs/sounds/entity/octopus tools/tests/test_octopus_contract.py
git diff --cached --check
git commit -m "feat: add intelligent Octopus sound identity"
```

### Task 7: Produce visual comparisons and verify the real client

**Files:**
- Create: `Modelle/Exports/octopus_v1/review/octopus_source_runtime_comparison.png`
- Create: `Modelle/Exports/octopus_v1/review/octopus_ingame_front.png`
- Create: `Modelle/Exports/octopus_v1/review/octopus_ingame_right.png`
- Create: `Modelle/Exports/octopus_v1/review/octopus_ingame_back.png`
- Create: `Modelle/Exports/octopus_v1/review/octopus_ingame_top.png`
- Create: `Modelle/Exports/octopus_v1/review/octopus_ingame_swim.png`
- Create: `Modelle/Exports/octopus_v1/review/octopus_ingame_ambush.png`
- Create: `Modelle/Exports/octopus_v1/review/octopus_ingame_squeeze_hitbox.png`

- [ ] **Step 1: Run the complete automated gate**

```powershell
python tools/verify_umr_project_truth.py
python -m unittest tools.mob_tripo.tests.test_exact_runtime tools.tests.test_octopus_contract tools.tests.test_remaining_mob_renderers -v
.\gradlew.bat build
```

Expected: truth pass, all focused tests PASS, and `BUILD SUCCESSFUL`.

- [ ] **Step 2: Start the Forge client and inspect only the Octopus**

Run: `.\gradlew.bat runClient`

Create a fresh test world, summon `usless_mobs:octopus`, enable `F3+B`, and verify:

- exactly eight equal arms;
- no side fins and no pair of long catching tentacles;
- face points along movement direction;
- original 4K texture remains attached to the same source faces;
- normal body is approximately `1.15 × 1.20` blocks and the squeeze box becomes `0.62 × 0.48` only while squeezing;
- swim loop visibly moves all eight arms without cuts or detached triangles;
- ambush, grab, ink, camouflage, object, and squeeze actions are telegraphed and terminate;
- camouflage cooldown prevents permanent invisibility;
- collected objects are neither consumed nor duplicated;
- the entity does not suffocate from its temporary squeeze transition.

- [ ] **Step 3: Capture the required evidence**

Capture front, right, back, top, swim, ambush, and squeeze-hitbox frames. Build one same-camera source/runtime comparison sheet. Reject the candidate if the texture, scale, orientation, arm count, or silhouette differs materially from the approved Tripo source.

- [ ] **Step 4: Re-run the gate after any visual correction**

Run the exact commands from Step 1 again.

Expected: all checks remain green.

- [ ] **Step 5: Commit only accepted Octopus proof**

```powershell
git add -- Modelle/Exports/octopus_v1/review
git diff --cached --check
git commit -m "test: verify exact Octopus in game"
```

Do not begin the new Squid until the Octopus source report, behaviour tests, build, hitbox view, and comparison images all pass.
