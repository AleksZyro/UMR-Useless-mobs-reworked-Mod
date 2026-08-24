# Corrupted Silverfish Rectangular Hitbox Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Match the active Corrupted Silverfish's approximately 1.10 × 0.92 × 2.00-block body with one damage-safe multipart hitbox and keep it out of spaces that cannot contain its full body.

**Architecture:** Keep a 1.10 × 0.92 navigation core and attach front/rear `PartEntity` hit segments that rotate with the mob. All part damage delegates once to the parent. A server-side clearance tracker records safe positions and performs a bounded escape only after sustained collision.

**Tech Stack:** Minecraft Forge 1.20.1, Java 17, GeckoLib 4.8.3, pytest structural regression tests.

---

### Task 1: Lock the measured dimensions

**Files:**
- Modify: `tools/corrupted_silverfish_v5/tests/test_runtime_export.py`
- Modify: `src/main/java/com/Momik/usless_mobs/registry/ModEntities.java`

- [ ] **Step 1: Change the structural test to require a 1.10-wide core**

Assert that `ModEntities.java` contains `.sized(1.10F, 0.92F)` and that the decoded mesh stays within `1.10 ± 0.02` width, `0.92 ± 0.02` height and `2.00 ± 0.02` length.

- [ ] **Step 2: Run the focused test and verify that the old square registration fails**

Run: `python -m pytest tools/corrupted_silverfish_v5/tests/test_runtime_export.py -q`

Expected: FAIL because the current registration is `.sized(2.0F, 0.92F)`.

- [ ] **Step 3: Change only the entity core registration**

```java
.sized(1.10F, 0.92F)
```

- [ ] **Step 4: Re-run the focused test**

Expected: PASS.

### Task 2: Add the multipart body

**Files:**
- Create: `src/main/mobs/endermite/java/net/mysith/silverfish/CorruptedSilverfishPart.java`
- Modify: `src/main/mobs/endermite/java/net/mysith/silverfish/CorruptedSilverfishEntity.java`
- Create: `tools/corrupted_silverfish_v5/tests/test_multipart_hitbox.py`

- [ ] **Step 1: Add a failing structural test**

Require three stable parts named `front`, `core` and `rear`, `isMultipartEntity() == true`, `getParts()` returning the array, and part damage delegating to `parent.hurt(source, amount)`.

- [ ] **Step 2: Run the new test and verify that it fails**

Run: `python -m pytest tools/corrupted_silverfish_v5/tests/test_multipart_hitbox.py -q`

Expected: FAIL because no multipart class exists.

- [ ] **Step 3: Implement the part type**

Create a small `PartEntity<CorruptedSilverfishEntity>` with a stable name, explicit dimensions and `hurt` delegation. It must not maintain independent health, drops or effects.

- [ ] **Step 4: Position parts from body yaw each tick**

Use forward vector `(-sin(yaw), 0, cos(yaw))`; place front and rear roughly `0.55` blocks from the parent centre. Update old and current positions together to prevent interpolation streaks. Return all parts from `getParts()`.

- [ ] **Step 5: Run structural test and Java compilation**

Run: `python -m pytest tools/corrupted_silverfish_v5/tests/test_multipart_hitbox.py -q`

Run: `.\gradlew.bat compileJava`

Expected: both PASS.

### Task 3: Prevent duplicate damage

**Files:**
- Modify: `src/main/mobs/endermite/java/net/mysith/silverfish/CorruptedSilverfishEntity.java`
- Modify: `tools/corrupted_silverfish_v5/tests/test_multipart_hitbox.py`

- [ ] **Step 1: Add a failing regression test**

Require a same-tick damage signature containing source entity id, game tick and damage amount, and require repeated matching part hits to return without applying health loss twice.

- [ ] **Step 2: Implement bounded deduplication**

Cache only the most recent delegated part hit. Do not suppress distinct attackers, different damage values, environmental damage or hits in later ticks.

- [ ] **Step 3: Run tests and compile**

Run both focused pytest modules, then `.\gradlew.bat compileJava`.

Expected: PASS.

### Task 4: Add clearance and stuck escape

**Files:**
- Create: `src/main/mobs/endermite/java/net/mysith/silverfish/CorruptedSilverfishClearance.java`
- Modify: `src/main/mobs/endermite/java/net/mysith/silverfish/CorruptedSilverfishEntity.java`
- Create: `tools/corrupted_silverfish_v5/tests/test_clearance_escape.py`

- [ ] **Step 1: Add failing structural tests**

Require full-body collision checks, a last-safe-position record, at least 30 ticks of sustained collision before escape, and a maximum escape distance of 4 blocks.

- [ ] **Step 2: Implement safe-position tracking**

Every server tick, build an oriented union AABB for the three parts. Record the current position only if `level.noCollision(entity, bounds)` succeeds.

- [ ] **Step 3: Implement the bounded escape**

After sustained collision and negligible movement, teleport to the last safe position only if it remains loaded and collision-free. Reset velocity and cooldown. Never trigger from ordinary player damage.

- [ ] **Step 4: Add visible escape feedback**

Emit magenta corruption dust and sculk-soul particles at departure and arrival and play the registered escape event.

- [ ] **Step 5: Run tests and compile**

Expected: focused pytest PASS and `compileJava` PASS.

### Task 5: Register the Silverfish sound identity

**Files:**
- Create: `src/main/java/com/Momik/usless_mobs/registry/ModSounds.java`
- Modify: `src/main/java/com/Momik/usless_mobs/Usless_mobs.java`
- Modify: `src/main/mobs/endermite/java/net/mysith/silverfish/CorruptedSilverfishEntity.java`
- Modify: `src/main/resources/assets/usless_mobs/sounds.json`
- Create: `tools/corrupted_silverfish_v5/tests/test_sound_identity.py`

- [ ] **Step 1: Add a failing registry/resource test**

Require five IDs: `corrupted_silverfish_ambient`, `hurt`, `attack`, `escape`, `death`, and require every ID in `sounds.json`.

- [ ] **Step 2: Register variable-range sound events**

Use the main `usless_mobs` deferred register and attach it to the mod event bus.

- [ ] **Step 3: Route entity callbacks and attacks to the new IDs**

Override ambient, hurt and death events and use explicit attack/escape sounds at their gameplay events.

- [ ] **Step 4: Define free fallback layers in `sounds.json`**

Compose each event from existing `minecraft:` sounds with altered volume and pitch. Do not add fake or missing `.ogg` paths.

- [ ] **Step 5: Verify**

Run all Silverfish pytest tests, `python tools/verify_umr_project_truth.py`, and `.\gradlew.bat compileJava`.

Expected: all PASS.

### Task 6: Visual in-game acceptance

**Files:**
- No source edit unless the observed result contradicts the measured model.

- [ ] **Step 1: Launch the client**

Run: `.\gradlew.bat runClient`

- [ ] **Step 2: Capture three hitbox views**

With `F3+B`, capture front, side and three-quarter views of the active runtime entity.

- [ ] **Step 3: Verify interactions**

Hit front, core and rear once each; verify single damage per attack. Lead it toward a one-block-wide opening; verify rejection or bounded escape.

- [ ] **Step 4: Save evidence**

Store the screenshots under `artifacts/corrupted_silverfish_v5/hitbox_acceptance/` and link them in the handoff.
