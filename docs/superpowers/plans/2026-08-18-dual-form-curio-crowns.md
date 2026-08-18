# Dual-Form Curio Crowns Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build combat and royal visual forms for the Void, Celestial, Living, and Balance crowns, with Curio head rendering, helmet lift, identical effects, and the approved 1-Netherite-plus-7-diamond conversion.

**Architecture:** Existing item IDs remain the combat forms. Four new royal item IDs reuse the same effect classes and Curio capability logic. Item JSON selects deterministic 3D crown geometry and textures; the shared Curio renderer follows the humanoid head and raises either form when a helmet is equipped.

**Tech Stack:** Forge 1.20.1, Java 17, Curios 5.10.0, Minecraft item-model JSON, Python 3/Pillow, `unittest`, ChatGPT built-in image generation.

---

## File map

- `Modelle/Exports/armor_crowns/concept/dual_form_crowns.png`: generated visual reference only.
- `tools/armor_graphics/build_curio_crowns.py`: deterministic model/texture generator and atomic publisher.
- `tools/armor_graphics/tests/test_curio_crowns.py`: asset, recipe, registry, renderer, and determinism contracts.
- `src/main/java/com/Momik/usless_mobs/item/CrownForm.java`: shared `COMBAT`/`ROYAL` form type.
- `src/main/java/com/Momik/usless_mobs/item/PathCrownItem.java`: path plus form, with unchanged effects.
- `src/main/java/com/Momik/usless_mobs/item/TrueCrownItem.java`: Balance form metadata while preserving existing head-slot behavior until Curio migration is proven.
- `src/main/java/com/Momik/usless_mobs/compat/curios/CuriosCompat.java`: capabilities and render registrations for all eight forms.
- `src/main/java/com/Momik/usless_mobs/compat/curios/KingSlimeCrownCurioRenderer.java`: shared head-follow and helmet-lift renderer.
- `src/main/java/com/Momik/usless_mobs/registry/ModItems.java`: four royal item registrations.
- `src/main/resources/assets/usless_mobs/models/item/*.json`: eight generated crown models.
- `src/main/resources/assets/usless_mobs/textures/item/*.png`: eight generated crown textures.
- `src/main/resources/data/usless_mobs/recipes/*_royal.json`: four costly upgrades.
- `src/main/resources/data/usless_mobs/recipes/*_combat.json`: four free reversions.
- `src/main/resources/data/curios/tags/items/crown.json`: all Curio-compatible crown IDs.
- `src/main/resources/assets/usless_mobs/lang/en_us.json` and `de_de.json`: royal names and form tooltips.
- `Modelle/Exports/armor_graphics_review/crowns_*.png`: final real-client evidence.

### Task 1: Generate and approve the concept reference

**Files:**
- Create: `Modelle/Exports/armor_crowns/concept/dual_form_crowns.png`

- [ ] **Step 1: Generate the concept sheet**

Use the built-in ChatGPT image generator with this prompt:

```text
Use case: Minecraft mod crown concept sheet.
Create eight high-quality voxel crown designs arranged as four rows: Void, Celestial, Living, Balance. Each row shows a compact COMBAT crown and a taller ROYAL crown. Show front, side, back, and top orthographic views for every design. All crowns are closed wearable rings that sit on a Minecraft player head or helmet. No floating parts. Combat forms use three short protected peaks. Royal forms use five stepped peaks, a larger central gemstone, and connected side ornaments. Match the detailed material language of a premium Minecraft voxel creature: four or more shades per material, crisp edge highlights, dark seams, controlled pixel clusters, readable gemstone cores. Void uses black-purple metal and magenta crystals; Celestial uses ivory metal, gold, and cyan gems; Living uses dark wood-metal, moss green, and lime life cores; Balance combines all three symmetrically. Neutral dark background, even lighting, no text, no logos, no watermark, no green screen, no perspective distortion.
```

- [ ] **Step 2: Inspect the generated image**

Reject it if any design has an open ring, detached ornament, fewer than the requested views, colour bleeding between families, text, or non-voxel smooth surfaces.

- [ ] **Step 3: Save the selected image into the workspace**

Copy the selected built-in output to the exact project path without overwriting unrelated files.

- [ ] **Step 4: Commit the reference**

```powershell
git add -- Modelle/Exports/armor_crowns/concept/dual_form_crowns.png
git commit -m "art: add dual-form crown concept"
```

### Task 2: Lock the item and recipe contract with failing tests

**Files:**
- Create: `tools/armor_graphics/tests/test_curio_crowns.py`

- [ ] **Step 1: Write exact manifest and recipe tests**

```python
ROYAL_IDS = {
    "royal_void_crown": "void_reaper_king",
    "royal_celestial_crown": "god_king",
    "royal_living_crown": "living_king",
    "royal_balance_crown": "true_crown",
}

def test_upgrade_recipes_use_exact_nine_slot_pattern(self):
    for royal, combat in ROYAL_IDS.items():
        recipe = json.loads((ROOT / f"src/main/resources/data/usless_mobs/recipes/{royal}.json").read_text())
        self.assertEqual(["DDD", "DCD", "DND"], recipe["pattern"])
        self.assertEqual("minecraft:diamond", recipe["key"]["D"]["item"])
        self.assertEqual("minecraft:netherite_ingot", recipe["key"]["N"]["item"])
        self.assertEqual(f"usless_mobs:{combat}", recipe["key"]["C"]["item"])
        self.assertEqual(f"usless_mobs:{royal}", recipe["result"]["item"])

def test_reversion_recipes_are_one_item_shapeless(self):
    for royal, combat in ROYAL_IDS.items():
        recipe = json.loads((ROOT / f"src/main/resources/data/usless_mobs/recipes/{royal}_combat.json").read_text())
        self.assertEqual([{"item": f"usless_mobs:{royal}"}], recipe["ingredients"])
        self.assertEqual(f"usless_mobs:{combat}", recipe["result"]["item"])
```

- [ ] **Step 2: Add registry, Curio-tag, model, texture, and language assertions**

```python
ALL_IDS = set(ROYAL_IDS) | set(ROYAL_IDS.values())
self.assertEqual(ALL_IDS, set(load_crown_tag()["values"]))
for item_id in ALL_IDS:
    self.assertTrue((MODELS / f"{item_id}.json").is_file())
    with Image.open(TEXTURES / f"{item_id}.png") as image:
        self.assertEqual("RGBA", image.mode)
        self.assertEqual((64, 64), image.size)
    self.assertIn(f"item.usless_mobs.{item_id}", load_lang("en_us"))
    self.assertIn(f"item.usless_mobs.{item_id}", load_lang("de_de"))
```

- [ ] **Step 3: Run the new test and verify RED**

Run:

```powershell
python -m unittest tools.armor_graphics.tests.test_curio_crowns -v
```

Expected: failures listing the four missing royal registrations, recipes, models, textures, and translations.

- [ ] **Step 4: Commit the RED contract**

```powershell
git add -- tools/armor_graphics/tests/test_curio_crowns.py
git commit -m "test: define dual-form crown contract"
```

### Task 3: Add form-aware items and recipes without changing effects

**Files:**
- Create: `src/main/java/com/Momik/usless_mobs/item/CrownForm.java`
- Modify: `src/main/java/com/Momik/usless_mobs/item/PathCrownItem.java`
- Modify: `src/main/java/com/Momik/usless_mobs/item/TrueCrownItem.java`
- Modify: `src/main/java/com/Momik/usless_mobs/registry/ModItems.java`
- Create: eight recipe JSON files under `src/main/resources/data/usless_mobs/recipes/`
- Modify: `src/main/resources/data/curios/tags/items/crown.json`
- Modify: `src/main/resources/assets/usless_mobs/lang/en_us.json`
- Modify: `src/main/resources/assets/usless_mobs/lang/de_de.json`

- [ ] **Step 1: Add the form enum**

```java
package com.Momik.usless_mobs.item;

public enum CrownForm {
    COMBAT,
    ROYAL
}
```

- [ ] **Step 2: Store form on both crown item classes**

```java
private final CrownForm form;

public CrownForm getForm() {
    return form;
}
```

Add `CrownForm form` to the constructors and assign it once. Existing IDs pass `CrownForm.COMBAT`; royal IDs pass `CrownForm.ROYAL`. Do not branch effect application on `form`.

- [ ] **Step 3: Register the four royal items**

```java
public static final RegistryObject<Item> ROYAL_VOID_CROWN = ITEMS.register("royal_void_crown",
        () -> new PathCrownItem(PathCrownItem.Path.VOID, CrownForm.ROYAL, crownProperties()));
public static final RegistryObject<Item> ROYAL_CELESTIAL_CROWN = ITEMS.register("royal_celestial_crown",
        () -> new PathCrownItem(PathCrownItem.Path.CELESTIAL, CrownForm.ROYAL, crownProperties()));
public static final RegistryObject<Item> ROYAL_LIVING_CROWN = ITEMS.register("royal_living_crown",
        () -> new PathCrownItem(PathCrownItem.Path.LIVING, CrownForm.ROYAL, crownProperties()));
public static final RegistryObject<Item> ROYAL_BALANCE_CROWN = ITEMS.register("royal_balance_crown",
        () -> new TrueCrownItem(TrueCrownItem.Path.BALANCED, CrownForm.ROYAL, crownProperties()));
```

Use a private `crownProperties()` helper returning `new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)` so all eight forms stay aligned.

- [ ] **Step 4: Add the exact upgrade and reverse recipes**

Each upgrade is shaped with `DDD/DCD/DND`. Each reverse recipe is shapeless with only the royal item. Use result count `1`:

```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["DDD", "DCD", "DND"],
  "key": {
    "D": {"item": "minecraft:diamond"},
    "C": {"item": "usless_mobs:void_reaper_king"},
    "N": {"item": "minecraft:netherite_ingot"}
  },
  "result": {"item": "usless_mobs:royal_void_crown", "count": 1}
}
```

- [ ] **Step 5: Add tags and translations**

Add the four royal IDs to the crown tag and these exact keys while keeping every existing effect tooltip unchanged:

```json
"item.usless_mobs.royal_void_crown": "Königliche Void-Krone",
"item.usless_mobs.royal_celestial_crown": "Königliche himmlische Krone",
"item.usless_mobs.royal_living_crown": "Königliche lebende Krone",
"item.usless_mobs.royal_balance_crown": "Königliche Balance-Krone",
"item.usless_mobs.crown.form.combat": "Kampfform",
"item.usless_mobs.crown.form.royal": "Königliche Form"
```

- [ ] **Step 6: Run contract tests**

```powershell
python -m unittest tools.armor_graphics.tests.test_curio_crowns -v
```

Expected: registry, recipe, tag, and translation tests pass; asset tests remain RED.

- [ ] **Step 7: Commit**

```powershell
git add -- src/main/java/com/Momik/usless_mobs/item/CrownForm.java src/main/java/com/Momik/usless_mobs/item/PathCrownItem.java src/main/java/com/Momik/usless_mobs/item/TrueCrownItem.java src/main/java/com/Momik/usless_mobs/registry/ModItems.java src/main/resources/data/usless_mobs/recipes src/main/resources/data/curios/tags/items/crown.json src/main/resources/assets/usless_mobs/lang/en_us.json src/main/resources/assets/usless_mobs/lang/de_de.json
git commit -m "feat: add royal crown forms and conversions"
```

### Task 4: Generate deterministic crown geometry and textures

**Files:**
- Create: `tools/armor_graphics/build_curio_crowns.py`
- Modify: `tools/armor_graphics/tests/test_curio_crowns.py`
- Create/modify: eight item-model JSON files and eight item-texture PNG files

- [ ] **Step 1: Extend tests for geometry and texture semantics**

Require for every crown:

```python
self.assertGreaterEqual(len(model["elements"]), 7 if form == "combat" else 11)
self.assertTrue(closed_ring_overlap(model["elements"]))
self.assertFalse(detached_components(model["elements"]))
self.assertGreaterEqual(material_shade_count(texture), 4)
self.assertGreaterEqual(gem_core_pixels(texture, family), 3)
self.assertTrue(all_uvs_within(texture.size, model))
```

Also run the generator twice into separate temporary directories and compare every output byte.

- [ ] **Step 2: Implement family/form specifications**

Use immutable dictionaries containing ring dimensions, three or five peak heights, central gem dimensions, connected side-ornament boxes, palette, and UV islands:

```python
FORMS = {
    "combat": {"peaks": (3.0, 4.0, 3.0), "ring_height": 2.0, "minimum_elements": 7},
    "royal": {"peaks": (3.5, 5.0, 6.5, 5.0, 3.5), "ring_height": 2.5, "minimum_elements": 11},
}
```

All rotated cubes must be expressed around local centres and placed with explicit pivots.

- [ ] **Step 3: Implement textures**

Produce 64x64 RGBA textures with transparent unused space, four material shades, edge light, seams, and family cores. The painting entry point is:

```python
def build_texture(family: str, form: str) -> Image.Image:
    image = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    paint_uv_islands(image, FAMILY_SPECS[family], FORMS[form])
    validate_texture(image, family)
    return image
```

Do not sample the concept PNG at build time; encode the approved design as deterministic palette and geometry data.

- [ ] **Step 4: Implement atomic publication**

Stage every JSON and PNG under unique temporary names in the destination directory. Publish all sixteen outputs as one transaction, restore every original on failure, and remove candidates/backups after success. Add injected failures at candidate 8 and publish 8; both tests must prove all original bytes remain and no `.candidate` files survive.

- [ ] **Step 5: Run generator and tests**

```powershell
python tools/armor_graphics/build_curio_crowns.py
python -m unittest tools.armor_graphics.tests.test_curio_crowns -v
```

Expected: all model, texture, connectivity, UV, and determinism tests pass.

- [ ] **Step 6: Inspect a generated contact sheet**

Generate `Modelle/Exports/armor_crowns/review/crown_forms_contact.png` showing actual rendered model views, not flat UV atlases. Confirm eight readable silhouettes and no detached parts.

- [ ] **Step 7: Commit**

```powershell
git add -- tools/armor_graphics/build_curio_crowns.py tools/armor_graphics/tests/test_curio_crowns.py src/main/resources/assets/usless_mobs/models/item src/main/resources/assets/usless_mobs/textures/item Modelle/Exports/armor_crowns/review/crown_forms_contact.png
git commit -m "feat: generate detailed dual-form crown assets"
```

### Task 5: Wire Curio rendering and helmet lift

**Files:**
- Modify: `src/main/java/com/Momik/usless_mobs/compat/curios/CuriosCompat.java`
- Modify: `src/main/java/com/Momik/usless_mobs/compat/curios/KingSlimeCrownCurioRenderer.java`
- Modify: `src/main/java/com/Momik/usless_mobs/item/TrueCrownItem.java`
- Modify: `tools/armor_graphics/tests/test_curio_crowns.py`

- [ ] **Step 1: Add static renderer contract tests**

```python
self.assertEqual(8, source.count("registerCrownRenderer(ModItems."))
self.assertEqual(1, source.count("KingSlimeCrownCurioRenderer::new"))
self.assertEqual(1, renderer.count("humanoid.head.translateAndRotate(matrixStack)"))
self.assertIn("hasHelmetUnderCrown ? helmetOffset : bareOffset", renderer)
self.assertNotIn("CrownForm", extract_method(path_crown_source, "curioTick"))
```

- [ ] **Step 2: Register every form**

Create one private registration helper and call it for the four combat and four royal IDs. Keep registration inside `FMLClientSetupEvent.enqueueWork`.

- [ ] **Step 3: Make helmet lift form-aware**

Use the existing helmet check and explicit offsets:

```java
boolean royal = stack.getItem() instanceof PathCrownItem crown && crown.getForm() == CrownForm.ROYAL
        || stack.getItem() instanceof TrueCrownItem crown && crown.getForm() == CrownForm.ROYAL;
double bareOffset = royal ? -0.365D : -0.385D;
double helmetOffset = royal ? -0.225D : -0.250D;
matrixStack.translate(0.0D, hasHelmetUnderCrown ? helmetOffset : bareOffset, 0.0D);
```

- [ ] **Step 4: Preserve Balance behavior**

Keep the existing Balance crown as a valid helmet item for world compatibility. Add Curio capability/render support without removing head-slot support. Refactor aura/guard application into this shared entry point, called by both `inventoryTick` and the Curio tick:

```java
public void applyEquippedEffects(Player player, Level level) {
    if (SlimePowerToggle.areEffectsDisabled(player)) return;
    applyPathAura(player);
    sendAuraParticles(level, player);
    tickGuard(player, level);
}
```

- [ ] **Step 5: Run tests and compile**

```powershell
python -m unittest tools.armor_graphics.tests.test_curio_crowns -v
.\gradlew.bat compileJava
```

Expected: crown tests pass and Gradle reports `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```powershell
git add -- src/main/java/com/Momik/usless_mobs/compat/curios/CuriosCompat.java src/main/java/com/Momik/usless_mobs/compat/curios/KingSlimeCrownCurioRenderer.java src/main/java/com/Momik/usless_mobs/item/TrueCrownItem.java tools/armor_graphics/tests/test_curio_crowns.py
git commit -m "feat: render crown forms in Curios slot"
```

### Task 6: Full verification and real-client evidence

**Files:**
- Create: `Modelle/Exports/armor_crowns/review/crowns_combat_bare.png`
- Create: `Modelle/Exports/armor_crowns/review/crowns_combat_helmet.png`
- Create: `Modelle/Exports/armor_crowns/review/crowns_royal_bare.png`
- Create: `Modelle/Exports/armor_crowns/review/crowns_royal_helmet.png`
- Create: `Modelle/Exports/armor_crowns/review/crowns_inventory.png`

- [ ] **Step 1: Run all crown and armour tests**

```powershell
python -m unittest discover -s tools/armor_graphics/tests -v
```

Expected: all relevant tests pass; only the existing explicitly documented UI/symlink skips may remain.

- [ ] **Step 2: Compile from current sources**

```powershell
.\gradlew.bat compileJava --rerun-tasks
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Start the real Forge client and reload assets**

Equip each form in the Curio crown slot. Capture front, side, and back views without a helmet, then repeat with a full helmet. The crown must move upward, remain attached to the head, and never intersect the helmet.

- [ ] **Step 4: Verify conversions in-game**

Craft one royal crown using exactly seven diamonds, one Netherite ingot, and its combat crown. Confirm the reverse recipe returns the combat form and that both forms show identical effect tooltips and active effects.

- [ ] **Step 5: Capture inventory evidence**

Show all eight icons at the same GUI scale. Reject clipped, off-centre, flat, or visually duplicated forms.

- [ ] **Step 6: Final mechanical checks**

```powershell
git diff --check
git status --short
```

Confirm no unrelated untracked Tripo, Silverfish, `.superpowers`, or `.codex-tmp` files are staged.

- [ ] **Step 7: Commit evidence only**

```powershell
git add -- Modelle/Exports/armor_crowns/review
git commit -m "test: capture dual-form crown validation"
```
