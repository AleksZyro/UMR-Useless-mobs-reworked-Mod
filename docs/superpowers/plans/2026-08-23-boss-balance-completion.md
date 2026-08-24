# Boss Balance Completion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make every already registered UMR boss use explicit Easy, Normal and Hard combat profiles while preserving the mechanically verified model sizes and existing signature attacks.

**Architecture:** Keep King Slime's mature configuration-driven balancing unchanged. Add one small immutable profile for Living Boss and Witch Boss, then route damage, cooldowns, summon limits and rewards through it. Keep mesh scale and hitboxes unchanged because the exact-mesh measurements already prove that Living Boss and Witch Boss fit them.

**Tech Stack:** Minecraft Forge 1.20.1, Java 17, Python pytest contract tests.

---

### Task 1: Shared boss difficulty contract

**Files:**
- Create: `src/main/java/com/Momik/usless_mobs/entity/boss/BossDifficultyProfile.java`
- Create: `tools/tests/test_boss_difficulty_profiles.py`

- [x] **Step 1: Write a failing profile contract test**

Require a Java record containing separate Easy, Normal and Hard values for damage, cooldowns, Living summon cap, Witch spirit count, hunt hound count and reward tier.

- [x] **Step 2: Verify RED**

Run: `python -m pytest -q tools/tests/test_boss_difficulty_profiles.py`

Expected: FAIL because `BossDifficultyProfile.java` does not exist.

- [x] **Step 3: Implement the profile**

Use these explicit tiers:

```java
EASY   -> new BossDifficultyProfile(0.72F, 1.25F, 2, 1, 2, 0)
NORMAL -> new BossDifficultyProfile(1.00F, 1.00F, 4, 2, 3, 1)
HARD   -> new BossDifficultyProfile(1.30F, 0.78F, 6, 3, 4, 2)
```

Expose bounded `damage(float)` and `cooldown(int)` helpers. Treat Peaceful as Easy defensively.

- [x] **Step 4: Verify GREEN**

Run the focused profile test and expect PASS.

### Task 2: Living Boss uses all three tiers

**Files:**
- Modify: `src/main/java/com/Momik/usless_mobs/entity/LivingBossEntity.java`
- Modify: `tools/tests/test_boss_difficulty_profiles.py`

- [x] **Step 1: Extend the failing test**

Require every Living Boss special-damage path, cooldown and root-spirit cap to use `difficultyProfile()` instead of binary Hard checks.

- [x] **Step 2: Verify RED**

Run the focused profile test and expect the Living integration assertions to fail.

- [x] **Step 3: Route the existing mechanics through the profile**

Use `profile.cooldown(baseTicks)` for Wurzelwelle, Bodenbruch, Wurzelkäfig and Beschwörung. Use `profile.damage(baseDamage)` for Welle, Bruch, Käfig, Wurzelpuls and Dornenkonter. Limit spirits with `profile.livingSummonCap()` and retain every existing telegraph and safe corridor.

- [x] **Step 4: Verify GREEN**

Run Living expansion, profile and exact-mesh tests; expect PASS.

### Task 3: Witch Boss uses all three tiers and cleans hunts safely

**Files:**
- Modify: `src/main/java/com/Momik/usless_mobs/entity/WitchBossEntity.java`
- Modify: `src/main/java/com/Momik/usless_mobs/event/LivingMobReworkHandler.java`
- Modify: `tools/tests/test_boss_difficulty_profiles.py`
- Modify: `tools/tests/test_rabbit_transformation.py`

- [x] **Step 1: Extend failing tests**

Require profile-driven Witch cooldowns, damage, spirits and hounds. Require a finite hound lifetime and owner-existence cleanup.

- [x] **Step 2: Verify RED**

Run the two focused tests and expect the new integration assertions to fail.

- [x] **Step 3: Implement the Witch integration**

Use `profile.cooldown(baseTicks)`, `profile.damage(baseDamage)`, `profile.witchSpiritCount()` and `profile.huntHoundCount()`. Store a hound lifetime in persistent data. The global living tick handler discards hunt hounds when their timer ends or their owning Witch Boss no longer exists.

- [x] **Step 4: Verify GREEN**

Run the two focused tests plus Rabbit transformation tests; expect PASS.

### Task 4: Size and full-project acceptance

**Files:**
- Modify: `docs/UMR_ACTIVE_PROJECT_STATE.md`

- [x] **Step 1: Preserve measured boss sizes**

Keep Living Boss at `3.70 × 2.95`, Witch Boss at `1.15 × 1.95`, and King Slime at its dynamic size-8 contract. Do not enlarge a hitbox beyond its measured mesh merely to make an entity feel stronger.

- [x] **Step 2: Run automated acceptance**

Run:

```powershell
python tools/verify_umr_project_truth.py
python -m pytest -q tools/tests/test_boss_difficulty_profiles.py tools/tests/test_living_boss_expansion.py tools/tests/test_rabbit_transformation.py tools/tests/test_exact_mesh_alignment.py
python -m pytest -q
```

Expected: project truth PASS and zero failed tests.

- [x] **Step 3: Compile when the local Gradle distribution is available**

Run: `.\gradlew.bat compileJava`

Expected: `BUILD SUCCESSFUL`. If the sandbox blocks the Gradle 8.8 distribution download before compilation starts, report that environmental blocker and retain the last successful build log as secondary evidence.

## Deferred independent subsystem

The Giant Squid boss and its Deep Ocean/underwater-ruin encounter remain a separate model-and-world-generation feature. No approved Giant Squid exact mesh exists in the active runtime, so this plan must not pretend that scaling an unrelated Squid model completes that requirement.
