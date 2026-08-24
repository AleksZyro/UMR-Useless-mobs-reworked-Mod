# True-Void Player Chestplate Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a clean True-Void 3D chestplate that fits the Minecraft humanoid rig, follows body and arm animation, and has matching worn and item visuals.

**Architecture:** Extend the existing `WornTruePathArmorModel` with a small set of path-specific cuboids whose ownership is split strictly between `body`, `right_arm`, and `left_arm`. Keep the existing `TruePathArmorItem` pose-copy and slot-selection flow. Generate the two PNG assets deterministically from a reviewed dark-violet palette, while retaining the existing item JSON transform contract.

**Tech Stack:** Java 17, Minecraft Forge 1.20.1 `HumanoidModel`, Python 3 `unittest`, Pillow, JSON item models, PNG textures.

---

### Task 1: Lock the player-rig geometry contract

**Files:**
- Modify: `tools/armor_graphics/tests/test_armor_graphics.py`
- Test: `tools/armor_graphics/tests/test_armor_graphics.py`

- [ ] **Step 1: Add a parser that records the owning humanoid bone**

Extend the existing Java parser with a helper returning `(bone, child_name)` for every chained `getChild` and `addOrReplaceChild` call. It must ignore comments and string decoys and reject duplicate child names:

```python
def worn_model_child_owners(source: str) -> dict[str, str]:
    source = strip_java_comments(source)
    searchable = mask_java_literal_contents(source)
    owners = {}
    pattern = re.compile(
        r'root\.getChild\(\s*"([^"]+)"\s*\)\.addOrReplaceChild\(\s*"([^"]+)"'
    )
    for bone, child in pattern.findall(searchable):
        if child in owners:
            raise AssertionError(f"duplicate worn child {child}")
        owners[child] = bone
    return owners
```

- [ ] **Step 2: Add failing ownership and silhouette tests**

Require these exact pilot parts:

```python
expected = {
    "true_void_chest_left": "body",
    "true_void_chest_right": "body",
    "true_void_chest_keel": "body",
    "true_void_abdomen_upper": "body",
    "true_void_abdomen_lower": "body",
    "true_void_back_shell": "body",
    "true_void_right_shoulder_cap": "right_arm",
    "true_void_left_shoulder_cap": "left_arm",
}
```

Also assert that every `true_void_*shoulder*` child belongs to one arm, every other new chest child belongs to `body`, no name appears on multiple bones, and the existing `showForType` and `getHumanoidArmorModel` contracts still pass.

- [ ] **Step 3: Run the focused test and confirm RED**

Run:

```powershell
$env:PYTHONDONTWRITEBYTECODE='1'
python -m unittest tools.armor_graphics.tests.test_armor_graphics.WornArmorContract -v
```

Expected: the new ownership test fails because the eight pilot child names do not exist yet; all pre-existing contract tests remain green.

- [ ] **Step 4: Commit the red contract**

```powershell
git add -- tools/armor_graphics/tests/test_armor_graphics.py
git commit -m "test: define true void chestplate rig contract"
```

### Task 2: Build the body- and arm-bound worn model

**Files:**
- Modify: `src/main/java/com/Momik/usless_mobs/client/WornTruePathArmorModel.java`
- Test: `tools/armor_graphics/tests/test_armor_graphics.py`

- [ ] **Step 1: Add the path-specific body plates**

Inside the `VOID` branch of `addChestDetails`, add six body children with positive, finite dimensions and UVs inside 128 × 64. Use separate left/right upper plates, a narrow centre keel, two lower plates, and one rear shell. The geometry must remain inside approximately `x=-5.3..5.3`, `y=-0.7..12.6`, `z=-3.6..3.5` around the Vanilla body.

```java
PartDefinition body = root.getChild("body");
body.addOrReplaceChild("true_void_chest_left",
        CubeListBuilder.create().texOffs(0, 0)
                .addBox(-5.05F, 0.15F, -3.35F, 4.55F, 5.05F, 1.05F, new CubeDeformation(0.06F)),
        PartPose.rotation(0.0F, 0.0F, -0.04F));
body.addOrReplaceChild("true_void_chest_right",
        CubeListBuilder.create().texOffs(14, 0)
                .addBox(0.50F, 0.15F, -3.35F, 4.55F, 5.05F, 1.05F, new CubeDeformation(0.06F)),
        PartPose.rotation(0.0F, 0.0F, 0.04F));
```

Implement the remaining four named body parts with the same explicit style. Do not create any body child whose bounds extend into an arm pivot.

- [ ] **Step 2: Add independent shoulder caps**

Add one compact cap to each arm, mirrored around the local arm origin:

```java
root.getChild("right_arm").addOrReplaceChild("true_void_right_shoulder_cap",
        CubeListBuilder.create().texOffs(54, 0)
                .addBox(-4.15F, -3.40F, -3.15F, 4.35F, 2.20F, 6.30F, new CubeDeformation(0.08F)),
        PartPose.rotation(0.0F, 0.0F, -0.08F));
root.getChild("left_arm").addOrReplaceChild("true_void_left_shoulder_cap",
        CubeListBuilder.create().texOffs(76, 0)
                .addBox(-0.20F, -3.40F, -3.15F, 4.35F, 2.20F, 6.30F, new CubeDeformation(0.08F)),
        PartPose.rotation(0.0F, 0.0F, 0.08F));
```

No new cuboid may be parented to `root` directly or span from one arm to the other.

- [ ] **Step 3: Run focused tests and confirm GREEN**

Run:

```powershell
$env:PYTHONDONTWRITEBYTECODE='1'
python -m unittest tools.armor_graphics.tests.test_armor_graphics.WornArmorContract -v
```

Expected: all worn-model tests pass, including atlas bounds, exact slot visibility, exact model pose-copy flow, and new bone ownership.

- [ ] **Step 4: Run all armour tests**

```powershell
$env:PYTHONDONTWRITEBYTECODE='1'
python -m unittest discover -s tools/armor_graphics/tests -v
```

Expected: all executable tests pass; only the existing dedicated UI baseline test may be skipped.

- [ ] **Step 5: Commit the worn model**

```powershell
git add -- src/main/java/com/Momik/usless_mobs/client/WornTruePathArmorModel.java
git commit -m "feat: fit true void chestplate to player rig"
```

### Task 3: Paint deterministic matching textures

**Files:**
- Create: `tools/armor_graphics/build_true_void_chestplate_assets.py`
- Modify: `tools/armor_graphics/tests/test_armor_graphics.py`
- Modify: `src/main/java/com/Momik/usless_mobs/item/TruePathArmorItem.java`
- Create: `src/main/resources/assets/usless_mobs/textures/models/armor/true_void_chestplate_layer_1.png`
- Modify: `src/main/resources/assets/usless_mobs/textures/item/true_void_chestplate.png`

- [ ] **Step 1: Add failing palette and identity tests**

Tests must decode both PNGs with Pillow and require:

- exact dimensions: worn atlas 128 × 64 and item icon 16 × 16;
- RGBA mode and at least one transparent pixel in the item icon;
- no bright green leakage (`G > 180 and R < 80 and B < 120`);
- dark pixels, mid-violet pixels, and bright-violet pixels all present;
- a compact bright centre crystal region in the item icon;
- byte-for-byte deterministic regeneration.

- [ ] **Step 2: Run the focused texture test and confirm RED**

```powershell
$env:PYTHONDONTWRITEBYTECODE='1'
python -m unittest tools.armor_graphics.tests.test_armor_graphics.WornArmorContract.test_true_void_chestplate_palette_and_identity -v
```

Expected: fail on the new exact reviewed palette/hash contract before the generator is applied.

- [ ] **Step 3: Implement a deterministic Pillow generator**

The script must write only the two named PNGs, use nearest-neighbour pixel blocks, and expose pure functions before its CLI entry point:

```python
VOID_BLACK = (13, 9, 20, 255)
VOID_METAL = (37, 29, 52, 255)
VOID_MID = (84, 49, 122, 255)
VOID_GLOW = (177, 78, 255, 255)
VOID_CORE = (225, 174, 255, 255)

def build_worn_texture() -> Image.Image:
    image = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw.rectangle((0, 0, 127, 31), fill=VOID_BLACK)
    draw.rectangle((2, 2, 97, 14), fill=VOID_METAL)
    draw.line((4, 4, 46, 4), fill=VOID_MID, width=2)
    draw.line((50, 4, 94, 4), fill=VOID_GLOW, width=1)
    draw.polygon(((47, 16), (51, 20), (47, 24), (43, 20)), fill=VOID_CORE)
    return image

def build_item_texture() -> Image.Image:
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw.polygon(((2, 3), (5, 1), (8, 3), (11, 1), (14, 3), (13, 14), (3, 14)), fill=VOID_METAL)
    draw.line((3, 5, 8, 11, 13, 5), fill=VOID_GLOW, width=1)
    draw.polygon(((8, 5), (10, 7), (8, 9), (6, 7)), fill=VOID_CORE)
    return image
```

Refine these exact deterministic primitives into a dark-metal atlas for the allocated cuboid UV regions, narrow violet edge bands, and a compact diamond core on the icon. Never sample colours from the generated concept background. The generator creates a dedicated chestplate atlas so `true_void_layer_1.png`, used by the other Void pieces, remains byte-identical.

- [ ] **Step 4: Select the dedicated atlas only for the Void chestplate**

Change `TruePathArmorItem.getArmorTexture` so only `path == Path.VOID && getType() == Type.CHESTPLATE` returns:

```java
return Usless_mobs.MODID + ":textures/models/armor/true_void_chestplate_layer_1.png";
```

All other True-Path slots retain the existing `path.key + layer` result. Extend the Java wiring test to prove both branches and reject a condition that would route every Void item to the chestplate atlas.

- [ ] **Step 5: Generate and verify both assets**

```powershell
python tools/armor_graphics/build_true_void_chestplate_assets.py
$env:PYTHONDONTWRITEBYTECODE='1'
python -m unittest tools.armor_graphics.tests.test_armor_graphics -v
```

Expected: all armour tests pass and a second generator run produces identical SHA-256 hashes.

- [ ] **Step 6: Commit the textures and generator**

```powershell
git add -- tools/armor_graphics/build_true_void_chestplate_assets.py tools/armor_graphics/tests/test_armor_graphics.py src/main/java/com/Momik/usless_mobs/item/TruePathArmorItem.java src/main/resources/assets/usless_mobs/textures/models/armor/true_void_chestplate_layer_1.png src/main/resources/assets/usless_mobs/textures/item/true_void_chestplate.png
git commit -m "feat: paint true void chestplate textures"
```

### Task 4: Verify item, build, and player motion

**Files:**
- Modify only if a failing contract proves necessary: `src/main/resources/assets/usless_mobs/models/item/template/armor_chestplate_3d.json`
- Create: `Modelle/Exports/armor_graphics_review/true_void_chestplate_front.png`
- Create: `Modelle/Exports/armor_graphics_review/true_void_chestplate_back.png`
- Create: `Modelle/Exports/armor_graphics_review/true_void_chestplate_side.png`
- Create: `Modelle/Exports/armor_graphics_review/true_void_chestplate_walk.png`

- [ ] **Step 1: Run the item-model contracts**

```powershell
$env:PYTHONDONTWRITEBYTECODE='1'
python -m unittest tools.armor_graphics.tests.test_armor_graphics.ArmorItemModelContract -v
```

Expected: the True-Void chestplate resolves the shared chestplate template, all eight display contexts are finite, the GUI projection is centred, and its central detail remains visible. Do not change the template if these tests already pass.

- [ ] **Step 2: Compile the Java integration once**

```powershell
.\gradlew.bat compileJava
```

Expected: `BUILD SUCCESSFUL`. If the Gradle wrapper is blocked by the known external download restriction, record the exact blocker and do not claim compilation succeeded.

- [ ] **Step 3: Reload the running client or start one controlled client**

Use the existing Forge client if available. Reload resources, equip only `true_void_chestplate`, and capture the same 854 × 480 window in dry daylight with HUD hidden.

- [ ] **Step 4: Capture four real-player views**

Capture front, back, side, and one clear walking or arm-swing frame. Confirm shoulder caps move with their arms, torso plates remain together, and no body-to-arm bridge tears across the model.

- [ ] **Step 5: Inspect inventory and hand**

Confirm the icon is centred and the first- and third-person held item are fully visible. If an item transform fails, adjust only the relevant transform in `armor_chestplate_3d.json`, rerun `ArmorItemModelContract`, and recapture the affected view.

- [ ] **Step 6: Run final verification**

```powershell
$env:PYTHONDONTWRITEBYTECODE='1'
python -m unittest discover -s tools/armor_graphics/tests -v
git diff --check
git status --short
```

Expected: all executable tests pass, only the documented UI-only skip remains, `git diff --check` is clean, and unrelated untracked Silverfish/Tripo files are untouched.

- [ ] **Step 7: Commit only approved review evidence**

```powershell
git add -- Modelle/Exports/armor_graphics_review/true_void_chestplate_front.png Modelle/Exports/armor_graphics_review/true_void_chestplate_back.png Modelle/Exports/armor_graphics_review/true_void_chestplate_side.png Modelle/Exports/armor_graphics_review/true_void_chestplate_walk.png
git commit -m "test: capture true void chestplate motion"
```
