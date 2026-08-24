# Helping Amethyst Item Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a unique ChatGPT-generated 2D Seelen-Amethyst item that alone converts a vanilla Allay into the Helping Allay, without consuming or repurposing the vanilla Amethyst Shard interaction.

**Architecture:** Register one rare foil item in the existing Forge item registry and expose it through the existing creative-tab paths. Keep the visual as a transparent pixel-art PNG rendered through Minecraft's `item/generated` parent, and switch the existing conversion handler to the new registry item. A focused source/resource contract test protects the trigger, recipe, localisation, model, and texture.

**Tech Stack:** Java 17, Minecraft Forge 1.20.1, JSON resource/data pack assets, Python `unittest`, OpenAI image generation.

---

### Task 1: Protect the Allay trigger and item contract

**Files:**
- Modify: `tools/tests/test_helping_allay_contract.py`

- [x] **Step 1: Write the failing contract test**

```python
def test_helping_amethyst_is_the_only_conversion_item(self):
    items = (ROOT / "src/main/java/com/Momik/usless_mobs/registry/ModItems.java").read_text(encoding="utf-8")
    handler = (ROOT / "src/main/java/com/Momik/usless_mobs/event/HelpingSoulHandler.java").read_text(encoding="utf-8")
    self.assertIn('HELPING_AMETHYST = ITEMS.register("helping_amethyst"', items)
    self.assertIn("ModItems.HELPING_AMETHYST.get()", handler)
    self.assertNotIn("Items.AMETHYST_SHARD", handler)
```

Add resource assertions for `models/item/helping_amethyst.json`, `textures/item/helping_amethyst.png`, `recipes/helping_amethyst.json`, and both language keys. Parse JSON and assert the recipe contains exactly `minecraft:amethyst_shard`, `minecraft:glow_ink_sac`, and `minecraft:gold_nugget`, with result `usless_mobs:helping_amethyst`.

- [x] **Step 2: Run the test and confirm RED**

Run: `python -m unittest tools.tests.test_helping_allay_contract -v`

Expected: FAIL because `HELPING_AMETHYST` and its resources do not exist yet.

### Task 2: Generate and integrate the 2D item asset

**Files:**
- Create: `src/main/resources/assets/usless_mobs/textures/item/helping_amethyst.png`
- Create: `src/main/resources/assets/usless_mobs/models/item/helping_amethyst.json`

- [x] **Step 1: Generate the approved icon**

Use ChatGPT image generation for one centred transparent Minecraft pixel-art icon: an asymmetric violet amethyst cluster, cyan soul core, restrained gold resonance bands, crisp square pixels, no text, border, scene, watermark, or ground shadow.

- [x] **Step 2: Add Minecraft's generated-item model**

```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "usless_mobs:item/helping_amethyst"
  }
}
```

- [x] **Step 3: Inspect the PNG**

Confirm it is a square PNG with transparency and a readable silhouette. Do not turn it into Blockbench geometry: `item/generated` provides the intended thin pseudo-3D extrusion in hand.

### Task 3: Register the item and isolate the conversion interaction

**Files:**
- Modify: `src/main/java/com/Momik/usless_mobs/registry/ModItems.java`
- Modify: `src/main/java/com/Momik/usless_mobs/event/HelpingSoulHandler.java`
- Modify: `src/main/java/com/Momik/usless_mobs/registry/ModCreativeTabs.java`
- Modify: `src/main/java/com/Momik/usless_mobs/Usless_mobs.java`

- [x] **Step 1: Register the rare foil item**

```java
public static final RegistryObject<Item> HELPING_AMETHYST = ITEMS.register("helping_amethyst",
        () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)) {
            @Override
            public boolean isFoil(ItemStack stack) {
                return true;
            }
        });
```

- [x] **Step 2: Replace the conversion trigger**

Import `ModItems` in `HelpingSoulHandler` and change the guard to:

```java
if (!stack.is(ModItems.HELPING_AMETHYST.get())
        || allay instanceof HelpingAllayEntity
        || allay.getPersistentData().getBoolean(HELPED_KEY)) {
    return;
}
```

Remove the now-unused `Items` import. Add `HELPING_AMETHYST` beside `HELPING_SOUL` in both existing creative inventory lists.

### Task 4: Add crafting and player-facing text

**Files:**
- Create: `src/main/resources/data/usless_mobs/recipes/helping_amethyst.json`
- Modify: `src/main/resources/assets/usless_mobs/lang/de_de.json`
- Modify: `src/main/resources/assets/usless_mobs/lang/en_us.json`

- [x] **Step 1: Add the shapeless recipe**

```json
{
  "type": "minecraft:crafting_shapeless",
  "ingredients": [
    { "item": "minecraft:amethyst_shard" },
    { "item": "minecraft:glow_ink_sac" },
    { "item": "minecraft:gold_nugget" }
  ],
  "result": { "item": "usless_mobs:helping_amethyst", "count": 1 }
}
```

- [x] **Step 2: Add and correct localisation**

Add `item.usless_mobs.helping_amethyst` as `Seelen-Amethyst` in German and `Helping Amethyst` in English. Update the Allay codex and JEI lines so they name the new item rather than instructing players to use a normal Amethyst Shard.

### Task 5: Verify, review, and commit

**Files:**
- Test: `tools/tests/test_helping_allay_contract.py`
- Verify: all files listed above

- [x] **Step 1: Run focused tests**

Run: `python -m unittest tools.tests.test_helping_allay_contract -v`

Expected: all Helping Allay tests PASS.

- [x] **Step 2: Run the project build**

Run: `.\gradlew.bat build --console=plain --no-daemon`

Expected: `BUILD SUCCESSFUL`.

- [x] **Step 3: Re-run project truth**

Run: `python tools/verify_umr_project_truth.py`

Expected: `UMR_PROJECT_TRUTH_PASS` and the active Corrupted Silverfish signature remains unchanged.

- [x] **Step 4: Perform visual/runtime verification**

Restart the development client, give the player `usless_mobs:helping_amethyst`, inspect its inventory/in-hand rendering, and confirm that it converts a vanilla Allay while a normal Amethyst Shard does not call the mod conversion path.

- [x] **Step 5: Commit only this feature's files**

Stage only the plan, contract test, item code, handler, creative tabs, recipe, model, texture, and two language files. Do not include unrelated dirty mob, renderer, boss, or Silverfish work.
