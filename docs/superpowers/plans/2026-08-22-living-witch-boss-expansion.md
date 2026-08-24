# Living and Witch Boss Expansion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expand the existing Living and Witch bosses with readable phase mechanics, difficulty-specific patterns and drops, including a complete temporary rabbit-player hunt phase.

**Architecture:** Preserve the existing entity classes and extract only shared difficulty values and rabbit-state lifecycle into focused helpers. The rabbit transformation is a synchronized capability-like state on the existing player, with server-enforced restrictions and a client render substitution; the player entity is never replaced.

**Tech Stack:** Minecraft Forge 1.20.1 events/networking, Java 17, vanilla entity renderers, pytest structural tests, Forge client run.

---

### Task 1: Add explicit boss difficulty profiles

**Files:**
- Create: `src/main/java/com/Momik/usless_mobs/entity/boss/BossDifficultyProfile.java`
- Modify: `src/main/java/com/Momik/usless_mobs/entity/LivingBossEntity.java`
- Modify: `src/main/java/com/Momik/usless_mobs/entity/WitchBossEntity.java`
- Create: `tools/tests/test_boss_difficulty_profiles.py`

- [ ] Add a failing test requiring Easy, Normal and Hard values for cooldown, damage, summon cap and reward tier.
- [ ] Implement one immutable profile factory based on `Difficulty`.
- [ ] Replace scattered binary Hard checks only where the new mechanics need the profile; preserve unrelated behaviour.
- [ ] Run pytest and `.\gradlew.bat compileJava`; expect PASS.

### Task 2: Expand Living Boss phases

**Files:**
- Modify: `src/main/java/com/Momik/usless_mobs/entity/LivingBossEntity.java`
- Create: `tools/tests/test_living_boss_expansion.py`

- [ ] Add failing tests requiring Wurzelwelle, Bodenbruch, phase-two threshold and telegraph timers.
- [ ] Add a visible wind-up before every new damaging area.
- [ ] Implement Wurzelwelle as expanding rings with at least one safe angular corridor.
- [ ] Implement Bodenbruch as marked patches that erupt after the wind-up, never immediately under every available escape tile.
- [ ] Under 50 percent health, combine one existing and one new mechanic but retain a recovery window.
- [ ] Run tests and compile; expect PASS.

### Task 3: Add difficulty-tiered rewards

**Files:**
- Modify: `src/main/java/com/Momik/usless_mobs/entity/LivingBossEntity.java`
- Modify: `src/main/java/com/Momik/usless_mobs/entity/WitchBossEntity.java`
- Modify: `tools/tests/test_boss_difficulty_profiles.py`

- [ ] Add failing assertions for deterministic base rewards and bounded Easy/Normal/Hard bonus tables.
- [ ] Implement reward tiers without removing current signature drops.
- [ ] Store defeated difficulty on trophy-like rewards where applicable.
- [ ] Run tests and compile; expect PASS.

### Task 4: Implement synchronized rabbit transformation

**Files:**
- Create: `src/main/java/com/Momik/usless_mobs/witch/RabbitTransformationState.java`
- Create: `src/main/java/com/Momik/usless_mobs/witch/RabbitTransformationEvents.java`
- Create: `src/main/java/com/Momik/usless_mobs/network/RabbitTransformationPayload.java`
- Modify: `src/main/java/com/Momik/usless_mobs/Usless_mobs.java`
- Modify: `src/main/java/com/Momik/usless_mobs/entity/WitchBossEntity.java`
- Create: `tools/tests/test_rabbit_transformation.py`

- [ ] Add failing lifecycle tests for start, tick, end, death, logout, dimension change and boss removal.
- [ ] Store owner boss UUID, remaining ticks and original dimensions on the player persistent data.
- [ ] Synchronize start/end to tracking clients.
- [ ] While active, apply rabbit-sized dimensions, sprint/jump mobility and server-side item-use/attack cancellation.
- [ ] Restore all state idempotently on every exit path.
- [ ] Run tests and compile; expect PASS.

### Task 5: Render the player as a rabbit

**Files:**
- Create: `src/main/java/com/Momik/usless_mobs/client/RabbitTransformedPlayerRenderer.java`
- Modify: `src/main/java/com/Momik/usless_mobs/client/ClientModEvents.java`
- Modify: `tools/tests/test_rabbit_transformation.py`

- [ ] Add a failing test requiring player pre-render cancellation and rabbit renderer delegation only while transformed.
- [ ] Render the vanilla rabbit at the player pose and yaw, hide the humanoid and held items, and retain the player nameplate.
- [ ] Show a compact remaining-time indicator without obscuring combat.
- [ ] Compile and visually verify in client.

### Task 6: Add the Witch hunt phase

**Files:**
- Create: `src/main/java/com/Momik/usless_mobs/entity/WitchHoundEntity.java`
- Modify: `src/main/java/com/Momik/usless_mobs/registry/ModEntities.java`
- Modify: `src/main/java/com/Momik/usless_mobs/entity/WitchBossEntity.java`
- Modify: `tools/tests/test_rabbit_transformation.py`

- [ ] Add failing tests for telegraph, transformation, hound ownership, transformed-player targeting and timed despawn.
- [ ] Replace the current decoy-only event at the selected cooldown with the hunt phase; retain the decoy as a separate dodge.
- [ ] Spawn a difficulty-profiled number of hounds that only target transformed players from the owning boss.
- [ ] End all owned hounds and restore players when the timer or encounter ends.
- [ ] Run tests and compile; expect PASS.

### Task 7: Sounds, effects and final acceptance

**Files:**
- Modify: `src/main/java/com/Momik/usless_mobs/registry/ModSounds.java`
- Modify: `src/main/resources/assets/usless_mobs/sounds.json`
- Modify: both boss entity classes

- [ ] Register distinct telegraph, eruption, transformation, hunt and restoration sound events using valid fallback layers.
- [ ] Run `python tools/verify_umr_project_truth.py`, focused pytest suites and `.\gradlew.bat build`.
- [ ] Launch the client and capture Living phase one/two plus Witch transformation/hunt/restoration evidence.
- [ ] Correct only measured gameplay or rendering failures and rerun the narrowest failed check.
