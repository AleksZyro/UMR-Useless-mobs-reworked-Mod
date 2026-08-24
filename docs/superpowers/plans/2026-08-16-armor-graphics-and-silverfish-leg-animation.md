# Armor Graphics and Silverfish Leg Animation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Repair every affected armour inventory/worn graphic and make the Corrupted Silverfish walk leg motion about 60 percent stronger without clipping.

**Architecture:** Keep the existing per-set 3D item models and the shared `WornTruePathArmorModel`; add one focused Python contract suite that validates model inheritance, texture resolution, UV/display bounds, worn-layer dimensions, and animation amplitude. Apply only evidence-backed JSON/Java corrections, regenerate the runtime animation from its existing canonical Python source, then verify the complete result in Forge and in a local visual comparison.

**Tech Stack:** Minecraft Forge 1.20.1, Java 17, vanilla item-model JSON, Forge armour extensions, GeckoLib 4.8.3 animation JSON, Python 3 `unittest`, Pillow, Gradle.

---

## File map

- Create `tools/armor_graphics/tests/__init__.py` — test package marker.
- Create `tools/armor_graphics/tests/test_armor_graphics.py` — complete static asset and Java integration contract.
- Modify `src/main/resources/assets/usless_mobs/models/item/true_*_{helmet,chestplate,leggings,boots}.json` — repair only failing inventory geometry, UVs, texture bindings, or display transforms.
- Modify `src/main/resources/assets/usless_mobs/models/item/armor_of_balance_{helmet,chestplate,leggings,boots}.json` — retain inheritance while correcting any failing overrides.
- Modify `src/main/mobs/endermite/resources/assets/usless_mobs/models/item/corrupted_crystal_leggings.json` — retain the intended leggings parent and correct its texture/display contract.
- Modify `src/main/java/com/Momik/usless_mobs/client/WornTruePathArmorModel.java` — correct worn-part geometry/visibility only where the new contract or client capture proves a fault.
- Modify `tools/corrupted_silverfish_v5/animate_rig.py` — increase walk stride/lift amplitudes from `10.4/4.0` to `16.64/6.4` degrees.
- Modify `tools/corrupted_silverfish_v5/tests/test_animate_rig.py` — lock the 60-percent increase, alternating tripod phases, smoothness, and safe bounds.
- Regenerate `src/main/mobs/endermite/resources/assets/usless_mobs/animations/corrupted_silverfish.animation.json` and the matching v5 editable/export animation documents through the existing generator.
- Create `Modelle/Exports/armor_graphics_review/` — reviewed screenshots and browser comparison only; do not overwrite unrelated model exports.

### Task 1: Establish the armour asset contract

**Files:**
- Create: `tools/armor_graphics/tests/__init__.py`
- Create: `tools/armor_graphics/tests/test_armor_graphics.py`

- [ ] **Step 1: Capture a deterministic visual baseline before changing assets**

In the existing Forge test world, use clear daylight and capture `inventory_before.png`, `void_before.png`, `celestial_before.png`, `living_before.png`, `balance_before.png`, and `corrupted_leggings_before.png` under `Modelle/Exports/armor_graphics_review/`. Use the same player position, third-person front view, field of view, and window size for all worn captures. Record each visible defect as one of: missing texture, missing part, clipping part, reversed part, off-centre inventory model, or excessive scale.

- [ ] **Step 2: Write the failing model/texture contract**

Implement a `unittest.TestCase` with the exact inventory IDs below:

```python
SETS = ("true_void", "true_celestial", "true_living", "armor_of_balance")
SLOTS = ("helmet", "chestplate", "leggings", "boots")
ITEMS = tuple(f"{set_name}_{slot}" for set_name in SETS for slot in SLOTS) + (
    "corrupted_crystal_leggings",
)
DISPLAY_CONTEXTS = {
    "gui", "ground", "fixed", "head",
    "thirdperson_righthand", "thirdperson_lefthand",
    "firstperson_righthand", "firstperson_lefthand",
}
```

The tests must resolve parent chains across both resource roots, reject cycles/missing parents, require `textures.main` and `textures.particle` to resolve to existing PNGs, require every face texture to resolve through `#main`, require finite `from`/`to`/display vectors, constrain UVs to `[0, 16]`, and constrain GUI scale to `(0, 1.2]`.

- [ ] **Step 3: Add worn-resource and Java-wiring assertions**

Read the four `true_*_layer_{1,2}.png`, two `true_balance_layer_{1,2}.png`, and `corrupted_crystal_layer_2.png`; assert PNG format and the `128 x 64` size expected by `LayerDefinition.create(mesh, 128, 64)`. Assert that `TruePathArmorItem` uses layer 2 only for leggings, `ArmorOfBalanceItem` uses `true_balance_layer_2.png` only for leggings, and `CorruptedCrystalLeggingsItem` resolves `corrupted_crystal_layer_2.png`.

- [ ] **Step 4: Encode each baseline defect and record RED evidence**

Run:

```powershell
python -m unittest discover -s tools/armor_graphics/tests -v
```

Expected: at least one failure naming the exact item and the property corresponding to each recorded baseline defect. A purely aesthetic observation must be converted into a numeric contract: centred GUI translation in `[-4, 4]`, GUI scale in `(0, 1.2]`, finite cuboid bounds, valid UVs, resolvable texture, or correct worn-slot visibility. Preserve that failure as the repair target; do not weaken the assertion to make the current asset pass.

- [ ] **Step 5: Commit the failing contract and baseline evidence**

```powershell
git add -- tools/armor_graphics/tests/__init__.py tools/armor_graphics/tests/test_armor_graphics.py Modelle/Exports/armor_graphics_review
git commit -m "test: define armour graphics contract"
```

### Task 2: Repair inventory models without removing their 3D identity

**Files:**
- Modify: `src/main/resources/assets/usless_mobs/models/item/true_void_*.json`
- Modify: `src/main/resources/assets/usless_mobs/models/item/true_celestial_*.json`
- Modify: `src/main/resources/assets/usless_mobs/models/item/true_living_*.json`
- Modify: `src/main/resources/assets/usless_mobs/models/item/armor_of_balance_*.json`
- Modify: `src/main/mobs/endermite/resources/assets/usless_mobs/models/item/corrupted_crystal_leggings.json`
- Test: `tools/armor_graphics/tests/test_armor_graphics.py`

- [ ] **Step 1: Add regression assertions for the observed failures**

For each failure from Task 1, add an assertion that names the item and property. Parent-only variants must inherit geometry and override both texture keys exactly:

```python
self.assertEqual(model["textures"], {
    "main": f"usless_mobs:item/{item_id}",
    "particle": f"usless_mobs:item/{item_id}",
})
```

Require chestplates and leggings to retain non-empty geometry after parent resolution, and require their resolved GUI transform to remain centred with all translations between `-4.0` and `4.0`.

- [ ] **Step 2: Run the focused test to confirm the regression is RED**

Run the individual failing test with `python -m unittest tools.armor_graphics.tests.test_armor_graphics.ArmorGraphicsTests.<test_name> -v`.

Expected: FAIL on the current broken item and exact property.

- [ ] **Step 3: Apply the minimal JSON repair**

Preserve each set's element list and colour identity. Correct only invalid parent IDs, `#main` face bindings, UV coordinates, or display transforms. Derived Balance/Corrupted files must remain small parent overrides and must not duplicate inherited element arrays. Serialize as UTF-8 JSON with two-space indentation and one final newline.

- [ ] **Step 4: Verify all inventory contracts**

```powershell
python -m unittest discover -s tools/armor_graphics/tests -v
```

Expected: all armour-graphics tests PASS.

- [ ] **Step 5: Commit the inventory repair**

```powershell
git add -- src/main/resources/assets/usless_mobs/models/item src/main/mobs/endermite/resources/assets/usless_mobs/models/item/corrupted_crystal_leggings.json tools/armor_graphics/tests/test_armor_graphics.py
git commit -m "fix: repair armour inventory graphics"
```

### Task 3: Repair worn chestplate, leggings, helmet, and boots geometry

**Files:**
- Modify: `src/main/java/com/Momik/usless_mobs/client/WornTruePathArmorModel.java`
- Test: `tools/armor_graphics/tests/test_armor_graphics.py`

- [ ] **Step 1: Add worn-model regression tests**

Parse `CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, width, height, depth, ...)` declarations and verify `u >= 0`, `v >= 0`, `u + 2 * (depth + width) <= 128`, and `v + depth + height <= 64`. Assert slot visibility exactly:

```text
HELMET      head, hat
CHESTPLATE  body, rightArm, leftArm
LEGGINGS    body, rightLeg, leftLeg
BOOTS       rightLeg, leftLeg
```

Also assert that every armour item returns the cached custom model only for its own equipment slot.

- [ ] **Step 2: Run the worn-model test and confirm RED for each real defect**

Run:

```powershell
python -m unittest tools.armor_graphics.tests.test_armor_graphics.ArmorGraphicsTests.test_worn_model_contract -v
```

Expected: FAIL with the first offending part/UV/visibility mapping when a worn-model fault exists. If the static worn-model contract is already green, use the baseline captured in Task 1 as the required evidence and add a numeric regression assertion for its exact clipping/position defect before changing Java geometry.

- [ ] **Step 3: Correct only proven worn-model defects**

Keep `128 x 64`, the existing per-path detail methods, model caching, and `copyPropertiesTo`. Reposition or resize only parts that clip or use invalid UV space; do not alter armour stats, effects, recipes, or item registration.

- [ ] **Step 4: Compile and rerun focused tests**

```powershell
python -m unittest discover -s tools/armor_graphics/tests -v
.\gradlew.bat compileJava
```

Expected: all focused tests PASS and Gradle ends with `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit the worn-model repair**

```powershell
git add -- src/main/java/com/Momik/usless_mobs/client/WornTruePathArmorModel.java tools/armor_graphics/tests/test_armor_graphics.py
git commit -m "fix: align worn armour models"
```

### Task 4: Increase Corrupted Silverfish leg motion by 60 percent

**Files:**
- Modify: `tools/corrupted_silverfish_v5/tests/test_animate_rig.py`
- Modify: `tools/corrupted_silverfish_v5/animate_rig.py`
- Regenerate: `src/main/mobs/endermite/resources/assets/usless_mobs/animations/corrupted_silverfish.animation.json`

- [ ] **Step 1: Tighten the walk-motion test first**

Replace the loose `abs(first[1]) > 10` assertion with exact amplitude and safety assertions:

```python
self.assertAlmostEqual(max(abs(v[1]) for _, v in walk["leg_front_left"]["rotation"]), 16.64)
self.assertAlmostEqual(max(abs(v[2]) for _, v in walk["leg_front_left"]["rotation"]), 6.4)
self.assertEqual(first, [-value for value in opposite])
self.assertLessEqual(max(abs(a - b) for a, b in zip(leg_y, leg_y[1:])), 5.2)
```

- [ ] **Step 2: Run the animation test to verify RED**

```powershell
python -m unittest tools.corrupted_silverfish_v5.tests.test_animate_rig -v
```

Expected: FAIL because the current maximum stride/lift is `10.4/4.0`.

- [ ] **Step 3: Implement the exact 60-percent increase**

In `animate_rig.py`, change only the walk definitions:

```python
stride = tuple(round(16.64 * math.cos(2 * math.pi * time / 0.8), 6) for time in walk_times)
lift = tuple(round(6.4 * math.cos(2 * math.pi * time / 0.8), 6) for time in walk_times)
```

- [ ] **Step 4: Regenerate runtime/editable outputs through the existing v5 generator**

Run:

```powershell
python -m tools.corrupted_silverfish_v5.animate_rig
python -m tools.corrupted_silverfish_v5.runtime_export
```

Expected: `ANIMATION_PASS ANIMATIONS=5`, then `RUNTIME_EXPORT_PASS` with five animations. Verify that only the canonical v5 animated Blockbench model and runtime animation change; reject any unexpected geometry, mesh, texture, v2, v3, or v4 delta.

- [ ] **Step 5: Run animation and runtime-output tests**

```powershell
python -m unittest discover -s tools/corrupted_silverfish_v5/tests -v
```

Expected: all v5 tests PASS with 17 walk keyframes, exact opposite tripods, `16.64/6.4` amplitudes, and unchanged non-walk animations.

- [ ] **Step 6: Commit the animation change**

```powershell
git add -- tools/corrupted_silverfish_v5/animate_rig.py tools/corrupted_silverfish_v5/tests/test_animate_rig.py src/main/mobs/endermite/resources/assets/usless_mobs/animations/corrupted_silverfish.animation.json Modelle/Exports/corrupted_silverfish_v5
git commit -m "feat: strengthen corrupted silverfish leg motion"
```

### Task 5: Full build and visual verification

**Files:**
- Create: `Modelle/Exports/armor_graphics_review/*.png`
- Create: `.superpowers/brainstorm/<session>/content/armor-before-after.html`

- [ ] **Step 1: Run the complete narrow verification set**

```powershell
python -m unittest discover -s tools/armor_graphics/tests -v
python -m unittest discover -s tools/corrupted_silverfish_v5/tests -v
.\gradlew.bat compileJava
```

Expected: all Python tests PASS and Gradle ends with `BUILD SUCCESSFUL`.

- [ ] **Step 2: Capture the armour baseline/result matrix in Forge**

In one controlled test world, use clear daylight and third-person front/back views. Capture Void, Celestial, Living, and Balance full sets plus Corrupted-Crystal leggings. For every set capture the inventory icon matrix and the worn player from front/back; verify no missing texture, hidden slot, oversized part, reversed part, or body clipping.

- [ ] **Step 3: Capture the strengthened walking animation**

Spawn one Corrupted Silverfish on a flat contrasting surface, force a walking target, and capture side/front frames near opposite stride extrema. Verify all six legs remain attached, alternate in two tripods, clear the body, and do not visibly sink below the floor.

- [ ] **Step 4: Publish the local browser comparison**

Create a new visual-companion HTML fragment with side-by-side labelled before/after armour images and a two-frame leg-stride comparison. Do not embed remote assets or upload project files.

- [ ] **Step 5: Final repository checks**

```powershell
git diff --check
git status --short
git diff --name-only 47639f4..HEAD
```

Expected: no whitespace errors; only armour tests/assets, proven worn-model code, v5 walk animation outputs, review images, design/plan files; pre-existing untracked Tripo/visual-hull files remain untouched.

- [ ] **Step 6: Commit review evidence only if the repository already tracks review images**

```powershell
git add -- Modelle/Exports/armor_graphics_review docs/superpowers/plans/2026-08-16-armor-graphics-and-silverfish-leg-animation.md
git commit -m "docs: record armour and animation visual review"
```
