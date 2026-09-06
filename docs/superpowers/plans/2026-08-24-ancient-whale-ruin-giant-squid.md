# Ancient Whale Ruin and Giant Squid Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a rare, locatable Ancient Whale Ruin to the UMR Deep Ocean and start one persistent Giant Squid boss encounter when its central gold treasure is first disturbed.

**Architecture:** A registered custom `Structure` and `StructurePiece` procedurally build the ruin, whale skeletons and treasury without requiring an external NBT template. A `SavedData` record keyed by dimension and structure-start chunk owns activation, boss UUID and victory state. The Giant Squid is a dedicated boss entity that reuses the approved exact squid source mesh (734,627 triangles, 4K texture, eight arms plus two catching tentacles) at boss scale; it does not replace or mutate the normal squid.

**Tech Stack:** Minecraft 1.20.1, Forge 47.4.16, Java 17, TerraBlender 3.0.1.10, dynamic structure JSON, Forge events, pytest contract tests, Gradle.

---

### Task 1: Register a locatable deep-ocean structure

**Files:**
- Create: `src/main/java/com/Momik/usless_mobs/worldgen/ModStructures.java`
- Create: `src/main/java/com/Momik/usless_mobs/worldgen/AncientWhaleRuinStructure.java`
- Create: `src/main/java/com/Momik/usless_mobs/worldgen/AncientWhaleRuinPiece.java`
- Modify: `src/main/java/com/Momik/usless_mobs/Usless_mobs.java`
- Create: `src/main/resources/data/usless_mobs/worldgen/structure/ancient_whale_ruin.json`
- Create: `src/main/resources/data/usless_mobs/worldgen/structure_set/ancient_whale_ruins.json`
- Create: `src/main/resources/data/usless_mobs/tags/worldgen/biome/has_structure/ancient_whale_ruin.json`
- Test: `tools/tests/test_ancient_whale_ruin.py`

- [x] **Step 1: Write failing registry/resource tests**

Require custom structure and piece registries, a `deep_ocean` biome tag, `surface_structures` generation, and a random-spread structure set with spacing greater than separation.

- [x] **Step 2: Run the test and confirm the missing implementation fails**

Run: `python -m pytest tools/tests/test_ancient_whale_ruin.py -q`

- [x] **Step 3: Implement the custom structure and procedural piece**

Reject generation unless the ocean floor is at least 14 blocks below sea level. Build a 49 x 17 x 45 bounded ruin containing a central prismarine treasury, broken arches, navigable water routes, two large bone-block whale skeletons and seven central gold blocks. Every block write must be clipped to the current chunk's structure bounding box.

- [x] **Step 4: Register and attach data resources**

Register both codecs on the mod event bus. Configure rare random-spread placement and target only `usless_mobs:deep_ocean` through the biome tag.

- [x] **Step 5: Run the focused test**

Expected: structure registry and resource assertions pass.

### Task 2: Persist the treasure encounter

**Files:**
- Create: `src/main/java/com/Momik/usless_mobs/worldgen/WhaleRuinEncounterData.java`
- Create: `src/main/java/com/Momik/usless_mobs/event/WhaleRuinEncounterHandler.java`
- Modify: `src/main/java/com/Momik/usless_mobs/Config.java`
- Test: `tools/tests/test_ancient_whale_ruin.py`

- [x] **Step 1: Add failing persistence and trigger tests**

Require an activation record keyed by dimension and structure-start chunk, NBT save/load, first-gold activation, duplicate suppression, remaining-gold protection before victory, and configurable structure spacing.

- [x] **Step 2: Confirm the tests fail**

Run the focused pytest target and confirm failures are caused by absent saved data and event code.

- [x] **Step 3: Implement server-authoritative activation**

On `BlockEvent.BreakEvent`, accept only vanilla gold blocks inside the registered Ancient Whale Ruin piece. Activate once, store the encounter before spawning, apply a short Darkness warning, play a deep sound and create a temporary inward current. Cancel later protected-gold breaks while the encounter is active; allow treasure after victory.

- [x] **Step 4: Verify persistence contracts**

Run the focused tests and compile Java.

### Task 3: Add the dedicated Giant Squid boss

**Files:**
- Create: `src/main/java/com/Momik/usless_mobs/entity/GiantSquidEntity.java`
- Create: `src/main/java/com/Momik/usless_mobs/client/GiantSquidRenderer.java`
- Modify: `src/main/java/com/Momik/usless_mobs/registry/ModEntities.java`
- Modify: `src/main/java/com/Momik/usless_mobs/event/CommonModEvents.java`
- Modify: `src/main/java/com/Momik/usless_mobs/client/ClientModEvents.java`
- Modify: `src/main/java/com/Momik/usless_mobs/event/WhaleRuinEncounterHandler.java`
- Modify: `src/main/resources/assets/usless_mobs/lang/en_us.json`
- Modify: `src/main/resources/assets/usless_mobs/lang/de_de.json`
- Test: `tools/tests/test_giant_squid_boss.py`

- [x] **Step 1: Write failing entity, renderer and combat-contract tests**

Require a distinct boss registry ID, boss bar, 5.8 x 3.2 block hitbox, water-safe movement, persistent ruin key, four health phases, telegraphed ink/current/grab/dash attacks, cooldowns, difficulty scaling and victory callback.

- [x] **Step 2: Confirm tests fail**

Run: `python -m pytest tools/tests/test_giant_squid_boss.py -q`

- [x] **Step 3: Implement minimal complete boss loop**

Use server-side phase timers with non-overlapping telegraphs. Reuse the exact squid mesh and 4K UV texture through `ExactMobMeshLayer`, adding only boss-scale renderer transforms. Do not create a cuboid substitute and do not alter `LivingSquidEntity`.

- [x] **Step 4: Connect activation and victory**

Spawn once in water outside the treasury's direct centre view, save its UUID, reacquire or respawn only if the stored active encounter has no living boss, and mark the ruin defeated on boss death.

- [x] **Step 5: Run focused tests and compile**

Expected: focused tests pass and `gradlew.bat compileJava` succeeds.

### Task 4: Verify fresh-world generation and persistence

**Files:**
- Modify: `docs/UMR_ACTIVE_PROJECT_STATE.md`

- [x] **Step 1: Run all focused Python contracts**

Run: `python -m pytest tools/tests/test_ocean_worldgen.py tools/tests/test_ancient_whale_ruin.py tools/tests/test_giant_squid_boss.py -q`

- [x] **Step 2: Run project truth and model contracts**

Run: `python tools/verify_umr_project_truth.py`

- [x] **Step 3: Compile and build**

Run: `.\gradlew.bat compileJava` and `.\gradlew.bat build`.

- [x] **Step 4: Perform fresh-world client validation**

Locate both UMR biomes and `usless_mobs:ancient_whale_ruin`, inspect flooded placement and `F3+B`, break the first central gold block, verify one boss, save/reload, defeat it, and confirm remaining gold becomes recoverable without a duplicate boss.

- [x] **Step 5: Record only observed results**

Document commands and actual outcomes. Keep any unperformed visual or restart check explicitly open.
