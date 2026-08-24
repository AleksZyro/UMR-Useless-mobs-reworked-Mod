# True-Void Crystal Knight Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the bar-like True-Void chestplate pilot with the approved fitted Crystal Knight silhouette, deterministic texture and icon, then prove the result in the real Forge client.

**Architecture:** Keep the existing `HumanoidModel` integration and pose-copying path. Isolate the new Void-only geometry in a dedicated builder method so the other paths and the Balance chestplate retain their current geometry. Generate both raster assets deterministically from one Python source and validate geometry, ownership, UVs, palette, reproducibility and real-client appearance.

**Tech Stack:** Java 17, Forge 1.20.1, Minecraft `HumanoidModel`/`ModelPart`, Python 3, Pillow, `unittest`, Gradle.

---

## File map

- Modify `src/main/java/com/Momik/usless_mobs/client/WornTruePathArmorModel.java`: add the approved Void-only cuboid hierarchy and preserve the legacy Balance composition.
- Modify `tools/armor_graphics/build_true_void_chestplate_assets.py`: paint the approved 128 × 64 armor atlas and 16 × 16 icon deterministically.
- Modify `tools/armor_graphics/tests/test_armor_graphics.py`: enforce exact runtime bone ownership, silhouette constraints, palette discipline and deterministic bytes.
- Create `Modelle/Exports/armor_graphics_review/true_void_crystal_knight_*.png`: real Forge review captures only after automated verification.

### Task 1: Lock the Crystal Knight geometry contract

**Files:**
- Modify: `tools/armor_graphics/tests/test_armor_graphics.py:909-931`
- Test: `tools/armor_graphics/tests/test_armor_graphics.py`

- [ ] **Step 1: Replace the pilot ownership expectation with the approved exact part set**

Use this exact mapping in `test_true_void_chestplate_parts_have_exact_humanoid_bone_owners`:

```python
expected = {
    "true_void_front_upper_left": "body",
    "true_void_front_upper_right": "body",
    "true_void_front_middle_left": "body",
    "true_void_front_middle_right": "body",
    "true_void_front_lower_left": "body",
    "true_void_front_lower_right": "body",
    "true_void_front_tip": "body",
    "true_void_chest_crystal": "body",
    "true_void_back_left": "body",
    "true_void_back_right": "body",
    "true_void_back_crystal": "body",
    "true_void_right_shoulder_plate": "right_arm",
    "true_void_right_shoulder_crystal": "right_arm",
    "true_void_left_shoulder_plate": "left_arm",
    "true_void_left_shoulder_crystal": "left_arm",
}
```

Keep the existing exact-set comparison so every stale `true_void_` pilot part is rejected. Add assertions that the dedicated method name `addVoidCrystalKnightDetails` occurs exactly twice: one declaration and one call from the `Path.VOID` branch.

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```powershell
python -m unittest tools.armor_graphics.tests.test_armor_graphics.WornArmorContract.test_true_void_chestplate_parts_have_exact_humanoid_bone_owners -v
```

Expected: `FAIL`; the actual source still exposes pilot names such as `true_void_chest_left` and lacks `true_void_front_upper_left`.

- [ ] **Step 3: Commit the failing contract**

```powershell
git add -- tools/armor_graphics/tests/test_armor_graphics.py
git commit -m "test: define crystal knight chest geometry"
```

### Task 2: Build the fitted V-plate hierarchy

**Files:**
- Modify: `src/main/java/com/Momik/usless_mobs/client/WornTruePathArmorModel.java:20-87,232-282`
- Test: `tools/armor_graphics/tests/test_armor_graphics.py`

- [ ] **Step 1: Route only the standalone Void chestplate through the new builder**

Replace the chestplate branch in `create` with:

```java
if (type == ArmorItem.Type.CHESTPLATE) {
    if (path == TruePathArmorItem.Path.VOID) {
        addVoidCrystalKnightDetails(root);
    } else {
        addChestDetails(root, path);
    }
}
```

Keep `createBalanced` on the legacy geometry by replacing its Void call with `addLegacyBalanceVoidDetails(root)`, then retaining `addChestDetails(root, TruePathArmorItem.Path.LIVING)`. This prevents the approved standalone redesign from silently altering the Balance set.

- [ ] **Step 2: Move the old Void-only cuboids into the Balance legacy helper**

Create `addLegacyBalanceVoidDetails(PartDefinition root)` from the former Void branch. Rename its child literals from `true_void_*` to `balance_void_*`; keep all UV offsets, boxes and poses byte-for-byte equivalent so the rendered Balance geometry remains unchanged.

- [ ] **Step 3: Add the approved standalone geometry**

Add `addVoidCrystalKnightDetails(PartDefinition root)` with these exact cuboids and owners:

```java
PartDefinition body = root.getChild("body");
body.addOrReplaceChild("true_void_front_upper_left",
        CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4.35F, 0.35F, -3.18F, 4.35F, 2.10F, 0.72F, new CubeDeformation(0.04F)),
        PartPose.rotation(0.0F, 0.0F, -0.16F));
body.addOrReplaceChild("true_void_front_upper_right",
        CubeListBuilder.create().texOffs(12, 0)
                .addBox(0.0F, 0.35F, -3.18F, 4.35F, 2.10F, 0.72F, new CubeDeformation(0.04F)),
        PartPose.rotation(0.0F, 0.0F, 0.16F));
body.addOrReplaceChild("true_void_front_middle_left",
        CubeListBuilder.create().texOffs(24, 0)
                .addBox(-4.05F, 2.65F, -3.24F, 4.05F, 1.65F, 0.76F, new CubeDeformation(0.04F)),
        PartPose.rotation(0.0F, 0.0F, -0.12F));
body.addOrReplaceChild("true_void_front_middle_right",
        CubeListBuilder.create().texOffs(36, 0)
                .addBox(0.0F, 2.65F, -3.24F, 4.05F, 1.65F, 0.76F, new CubeDeformation(0.04F)),
        PartPose.rotation(0.0F, 0.0F, 0.12F));
body.addOrReplaceChild("true_void_front_lower_left",
        CubeListBuilder.create().texOffs(48, 0)
                .addBox(-3.55F, 4.65F, -3.20F, 3.55F, 1.45F, 0.72F, new CubeDeformation(0.04F)),
        PartPose.rotation(0.0F, 0.0F, -0.09F));
body.addOrReplaceChild("true_void_front_lower_right",
        CubeListBuilder.create().texOffs(58, 0)
                .addBox(0.0F, 4.65F, -3.20F, 3.55F, 1.45F, 0.72F, new CubeDeformation(0.04F)),
        PartPose.rotation(0.0F, 0.0F, 0.09F));
body.addOrReplaceChild("true_void_front_tip",
        CubeListBuilder.create().texOffs(68, 0)
                .addBox(-2.50F, 6.45F, -3.16F, 5.0F, 1.20F, 0.68F, new CubeDeformation(0.04F)),
        PartPose.ZERO);
body.addOrReplaceChild("true_void_chest_crystal",
        CubeListBuilder.create().texOffs(82, 0)
                .addBox(-1.15F, 1.35F, -3.62F, 2.30F, 2.30F, 0.82F, new CubeDeformation(0.04F)),
        PartPose.rotation(0.0F, 0.0F, 0.7853982F));
body.addOrReplaceChild("true_void_back_left",
        CubeListBuilder.create().texOffs(92, 0)
                .addBox(-4.20F, 0.55F, 2.40F, 4.20F, 6.70F, 0.70F, new CubeDeformation(0.04F)),
        PartPose.rotation(0.0F, 0.0F, -0.07F));
body.addOrReplaceChild("true_void_back_right",
        CubeListBuilder.create().texOffs(104, 0)
                .addBox(0.0F, 0.55F, 2.40F, 4.20F, 6.70F, 0.70F, new CubeDeformation(0.04F)),
        PartPose.rotation(0.0F, 0.0F, 0.07F));
body.addOrReplaceChild("true_void_back_crystal",
        CubeListBuilder.create().texOffs(116, 0)
                .addBox(-0.90F, 2.10F, 2.92F, 1.80F, 1.80F, 0.72F, new CubeDeformation(0.04F)),
        PartPose.rotation(0.0F, 0.0F, 0.7853982F));
```

Add the shoulder parts with these exact boxes and owners:

```java
root.getChild("right_arm").addOrReplaceChild("true_void_right_shoulder_plate",
        CubeListBuilder.create().texOffs(0, 16)
                .addBox(-4.10F, -2.60F, -2.68F, 4.10F, 1.55F, 5.35F, new CubeDeformation(0.05F)),
        PartPose.rotation(0.0F, 0.0F, -0.10F));
root.getChild("right_arm").addOrReplaceChild("true_void_right_shoulder_crystal",
        CubeListBuilder.create().texOffs(16, 16)
                .addBox(-3.90F, -3.15F, -0.62F, 1.25F, 1.60F, 1.25F, new CubeDeformation(0.04F)),
        PartPose.rotation(0.0F, 0.0F, -0.32F));
root.getChild("left_arm").addOrReplaceChild("true_void_left_shoulder_plate",
        CubeListBuilder.create().texOffs(24, 16)
                .addBox(0.0F, -2.60F, -2.68F, 4.10F, 1.55F, 5.35F, new CubeDeformation(0.05F)),
        PartPose.rotation(0.0F, 0.0F, 0.10F));
root.getChild("left_arm").addOrReplaceChild("true_void_left_shoulder_crystal",
        CubeListBuilder.create().texOffs(40, 16)
                .addBox(2.65F, -3.15F, -0.62F, 1.25F, 1.60F, 1.25F, new CubeDeformation(0.04F)),
        PartPose.rotation(0.0F, 0.0F, 0.32F));
```

Keep every front depth at or behind `z=-3.62`, every back depth at or ahead of `z=2.40`, and every shoulder box start at `y >= -3.15`.

- [ ] **Step 4: Run the exact ownership test and verify GREEN**

Run the command from Task 1 Step 2.

Expected: `OK`, one test passed.

- [ ] **Step 5: Run all focused geometry contracts**

```powershell
python -m unittest tools.armor_graphics.tests.test_armor_graphics.WornArmorContract -v
```

Expected: every `WornArmorContract` test passes.

- [ ] **Step 6: Commit the geometry slice**

```powershell
git add -- src/main/java/com/Momik/usless_mobs/client/WornTruePathArmorModel.java
git commit -m "feat: shape true void crystal knight chestplate"
```

### Task 3: Replace noisy stripes with deterministic plate shading

**Files:**
- Modify: `tools/armor_graphics/tests/test_armor_graphics.py:932-964`
- Modify: `tools/armor_graphics/build_true_void_chestplate_assets.py`
- Modify: `src/main/resources/assets/usless_mobs/textures/models/armor/true_void_chestplate_layer_1.png`
- Modify: `src/main/resources/assets/usless_mobs/textures/item/true_void_chestplate.png`

- [ ] **Step 1: Write failing texture-style assertions**

Extend `test_true_void_chestplate_palette_and_identity` with:

```python
bright = {VOID_GLOW, VOID_CORE}
rows_with_too_much_glow = [
    y for y in range(worn.height)
    if sum(worn.getpixel((x, y)) in bright for x in range(worn.width)) > 24
]
self.assertEqual(rows_with_too_much_glow, [])
self.assertLess(sum(pixel in bright for pixel in worn.getdata()), 360)
self.assertGreater(sum(pixel == VOID_CORE for pixel in worn.getdata()), 8)
self.assertEqual(item.getpixel((8, 7)), VOID_CORE)
self.assertEqual(item.getpixel((8, 8)), VOID_CORE)
```

Import the palette constants from the generator module instead of duplicating their values. Keep the existing generator-byte equality checks.

- [ ] **Step 2: Run the palette test and verify RED**

```powershell
python -m unittest tools.armor_graphics.tests.test_armor_graphics.WornArmorContract.test_true_void_chestplate_palette_and_identity -v
```

Expected: `FAIL`; the pilot generator paints repeated full-width stripes and does not satisfy the new core placement/count contract.

- [ ] **Step 3: Implement explicit atlas regions**

In `build_worn_texture`, start with `Image.new("RGBA", (128, 64), VOID_BLACK)`. Remove the checker/noise loops and every full-width horizontal line. Add helpers:

```python
def panel(draw, box, seam=False):
    x0, y0, x1, y1 = box
    draw.rectangle(box, fill=VOID_SHADOW)
    draw.line((x0, y0, x1, y0), fill=VOID_HIGHLIGHT)
    draw.line((x0, y1, x1, y1), fill=VOID_METAL)
    if seam and x1 - x0 >= 4:
        draw.line((x0 + 2, y1 - 1, x1 - 2, y1 - 1), fill=VOID_MID)


def crystal(draw, center_x, center_y, radius):
    points = (
        (center_x, center_y - radius),
        (center_x + radius, center_y),
        (center_x, center_y + radius),
        (center_x - radius, center_y),
    )
    draw.polygon(points, fill=VOID_GLOW)
    draw.line((center_x, center_y - radius + 1, center_x, center_y + radius - 1), fill=VOID_CORE)
```

Paint the base humanoid regions and the new custom regions with this explicit table:

```python
for box, seam in (
    ((16, 16, 39, 31), False),  # Vanilla torso
    ((40, 16, 55, 31), False),  # Vanilla right arm
    ((32, 48, 47, 63), False),  # Vanilla left arm
    ((0, 0, 11, 7), True),
    ((12, 0, 23, 7), True),
    ((24, 0, 35, 7), True),
    ((36, 0, 47, 7), True),
    ((48, 0, 57, 7), True),
    ((58, 0, 67, 7), True),
    ((68, 0, 81, 7), True),
    ((92, 0, 103, 13), False),
    ((104, 0, 115, 13), False),
    ((0, 16, 15, 27), True),
    ((24, 16, 39, 27), True),
):
    panel(draw, box, seam=seam)

crystal(draw, 87, 4, 3)
crystal(draw, 121, 4, 2)
draw.point((18, 19), fill=VOID_CORE)
draw.point((42, 19), fill=VOID_CORE)
```

The chest crystal is therefore the largest bright region, the back crystal remains secondary, and each shoulder crystal receives one core highlight.

- [ ] **Step 4: Redraw the 16 × 16 icon**

Use a transparent canvas. Paint a compact V silhouette bounded by `(2, 2, 13, 14)`, two tapered shoulders, three nested V bands and a centre diamond. Keep `(8, 7)` and `(8, 8)` equal to `VOID_CORE`; use no more than 18 glow/core pixels total.

- [ ] **Step 5: Generate the committed PNGs**

```powershell
python tools/armor_graphics/build_true_void_chestplate_assets.py
```

Expected stdout begins with `ARMOR_ASSETS_PASS` and lists both production paths.

- [ ] **Step 6: Run the palette test and all focused armor tests**

```powershell
python -m unittest tools.armor_graphics.tests.test_armor_graphics -v
```

Expected: 29 or more tests run, all relevant tests pass, only the dedicated visual-baseline test may skip.

- [ ] **Step 7: Prove deterministic bytes**

Run the generator twice and compare SHA-256 after each run:

```powershell
python tools/armor_graphics/build_true_void_chestplate_assets.py
Get-FileHash -Algorithm SHA256 src/main/resources/assets/usless_mobs/textures/models/armor/true_void_chestplate_layer_1.png,src/main/resources/assets/usless_mobs/textures/item/true_void_chestplate.png
python tools/armor_graphics/build_true_void_chestplate_assets.py
Get-FileHash -Algorithm SHA256 src/main/resources/assets/usless_mobs/textures/models/armor/true_void_chestplate_layer_1.png,src/main/resources/assets/usless_mobs/textures/item/true_void_chestplate.png
```

Expected: both hash pairs are identical.

- [ ] **Step 8: Commit the asset slice**

```powershell
git add -- tools/armor_graphics/build_true_void_chestplate_assets.py tools/armor_graphics/tests/test_armor_graphics.py src/main/resources/assets/usless_mobs/textures/models/armor/true_void_chestplate_layer_1.png src/main/resources/assets/usless_mobs/textures/item/true_void_chestplate.png
git commit -m "feat: paint true void crystal knight armor"
```

### Task 4: Compile and perform real-client acceptance

**Files:**
- Create: `Modelle/Exports/armor_graphics_review/true_void_crystal_knight_front.png`
- Create: `Modelle/Exports/armor_graphics_review/true_void_crystal_knight_back.png`
- Create: `Modelle/Exports/armor_graphics_review/true_void_crystal_knight_side.png`
- Create: `Modelle/Exports/armor_graphics_review/true_void_crystal_knight_arm_swing.png`
- Create: `Modelle/Exports/armor_graphics_review/true_void_crystal_knight_inventory.png`

- [ ] **Step 1: Compile once**

```powershell
.\gradlew.bat compileJava
```

Expected: `BUILD SUCCESSFUL`. If the wrapper cannot download because the network is blocked, record the exact external error and do not claim compile success.

- [ ] **Step 2: Start or reload the real Forge client**

Use the existing `runClient` workflow. Equip only `usless_mobs:true_void_chestplate`, set clear daytime weather and use dry ground. Do not use a generated renderer as a substitute for this acceptance step.

- [ ] **Step 3: Capture the five required views**

Save real 854 × 480 Forge screenshots at the exact paths listed under Task 4. The arm-swing capture must visibly separate the arms from the torso enough to prove each shoulder plate remains on its own arm. The inventory capture must show the final 16 × 16 icon and the worn preview.

- [ ] **Step 4: Compare against the approved concept**

Reject the result if any of these are visible: full-width horizontal-bar appearance, chest crystal hidden by another plate, shoulder touching the head, front/back plate floating away from the torso, arm-crossing bridge, missing texture, or icon clipping. If rejected, add a failing regression assertion before changing geometry or texture.

- [ ] **Step 5: Re-run final verification**

```powershell
python -m unittest tools.armor_graphics.tests.test_armor_graphics -v
git diff --check
git status --short
```

Expected: focused tests pass; diff check is clean; status contains only the five intended review captures plus previously known unrelated untracked files.

- [ ] **Step 6: Commit real-client evidence**

```powershell
git add -- Modelle/Exports/armor_graphics_review/true_void_crystal_knight_front.png Modelle/Exports/armor_graphics_review/true_void_crystal_knight_back.png Modelle/Exports/armor_graphics_review/true_void_crystal_knight_side.png Modelle/Exports/armor_graphics_review/true_void_crystal_knight_arm_swing.png Modelle/Exports/armor_graphics_review/true_void_crystal_knight_inventory.png
git commit -m "test: capture true void crystal knight fit"
```
