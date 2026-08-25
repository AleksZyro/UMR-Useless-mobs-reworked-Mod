# Ocean Worldgen Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the approved UMR Deep Ocean and Big Underwater Cave as real, naturally selected Overworld biomes and generate large flooded chambers in the cave biome.

**Architecture:** TerraBlender 3.0.1.10 overlays two narrowly scoped ocean climate regions without replacing vanilla noise settings. The biome definitions remain data-driven. A UMR `Feature<NoneFeatureConfiguration>` creates bounded, fully flooded ellipsoid chambers only through the Big Underwater Cave biome's generation list. The Ancient Whale Ruin, persistent treasure trigger and Giant Squid encounter remain a separate structure plan because they require saved per-structure state.

**Tech Stack:** Minecraft 1.20.1, Forge 47.4.16, Java 17, TerraBlender 3.0.1.10, Forge biome JSON, pytest contract tests, Gradle.

---

### Task 1: Lock the dependency and biome contracts

**Files:**
- Create: `tools/tests/test_ocean_worldgen.py`
- Modify: `gradle.properties`
- Modify: `build.gradle`
- Modify: `src/main/resources/META-INF/mods.toml`

- [x] **Step 1: Write failing dependency and resource tests**

Create tests that require `terrablender_version=3.0.1.10`, the Forge Maven dependency `com.github.glitchfiend:TerraBlender-forge`, a mandatory `terrablender` entry in `mods.toml`, and both biome JSON files.

- [x] **Step 2: Run the focused test and confirm it fails**

Run: `python -m pytest tools/tests/test_ocean_worldgen.py -q`

Expected: FAIL because TerraBlender and the biome resources are not yet declared.

- [x] **Step 3: Add the exact Forge 1.20.1 dependency**

Add to `gradle.properties`:

```properties
terrablender_version=3.0.1.10
```

Add to `build.gradle`:

```groovy
maven { url = "https://maven.minecraftforge.net/releases/" }
implementation fg.deobf("com.github.glitchfiend:TerraBlender-forge:${minecraft_version}-${terrablender_version}")
```

Add a mandatory BOTH-side dependency in `mods.toml` with version range `[3.0.1.10,3.1)`.

- [x] **Step 4: Run the focused test**

Run: `python -m pytest tools/tests/test_ocean_worldgen.py -q`

Expected: only the still-unimplemented biome/region assertions fail.

### Task 2: Register the two climate-scoped ocean biomes

**Files:**
- Create: `src/main/java/com/Momik/usless_mobs/worldgen/ModBiomes.java`
- Create: `src/main/java/com/Momik/usless_mobs/worldgen/UmrOceanRegion.java`
- Modify: `src/main/java/com/Momik/usless_mobs/Usless_mobs.java`
- Create: `src/main/resources/data/usless_mobs/worldgen/biome/deep_ocean.json`
- Create: `src/main/resources/data/usless_mobs/worldgen/biome/big_underwater_cave.json`
- Test: `tools/tests/test_ocean_worldgen.py`

- [x] **Step 1: Extend the test with region assertions**

Require two `ResourceKey<Biome>` values, `RegionType.OVERWORLD`, a low region weight, `Continentalness.DEEP_OCEAN` for both mappings, `Depth.SURFACE` for Deep Ocean, `Depth.UNDERGROUND` for Big Underwater Cave, and registration from `FMLCommonSetupEvent.enqueueWork`.

- [x] **Step 2: Confirm the new assertions fail**

Run: `python -m pytest tools/tests/test_ocean_worldgen.py -q`

- [x] **Step 3: Implement keys and TerraBlender overlay**

`ModBiomes` owns only the two resource keys. `UmrOceanRegion` extends `Region` and uses `VanillaParameterOverlayBuilder` plus `ParameterPointListBuilder`. Surface points map to `DEEP_OCEAN`; underground deep-ocean points map to `BIG_UNDERWATER_CAVE`. Use weight `1` so vanilla and other mods remain dominant.

- [x] **Step 4: Add data-driven biome definitions**

Base `deep_ocean.json` on vanilla 1.20.1 Deep Ocean, retaining valid feature-step cardinality and using darker water/fog colours. Base `big_underwater_cave.json` on vanilla Lush Caves, retain underground ores and aquatic spawns, use turquoise water/fog colours, and add the UMR cave chamber placed feature only after it exists in Task 3.

- [x] **Step 5: Register the region in common setup**

Inside the existing enqueue-work block:

```java
Regions.register(new UmrOceanRegion(
        ResourceLocation.tryBuild(MODID, "ocean_region"), 1));
```

- [x] **Step 6: Run focused tests**

Run: `python -m pytest tools/tests/test_ocean_worldgen.py -q`

Expected: PASS for dependency, biome JSON and region contracts.

### Task 3: Generate bounded fully flooded chambers

**Files:**
- Create: `src/main/java/com/Momik/usless_mobs/world/BigUnderwaterCaveFeature.java`
- Modify: `src/main/java/com/Momik/usless_mobs/registry/ModFeatures.java`
- Create: `src/main/resources/data/usless_mobs/worldgen/configured_feature/big_underwater_cave.json`
- Create: `src/main/resources/data/usless_mobs/worldgen/placed_feature/big_underwater_cave.json`
- Modify: `src/main/resources/data/usless_mobs/worldgen/biome/big_underwater_cave.json`
- Test: `tools/tests/test_ocean_worldgen.py`

- [x] **Step 1: Add failing chamber safety tests**

Require horizontal radii in the navigable 10–18 block range, vertical radius 6–10, a solid-rock and ocean-depth precondition, bounded block loops, water rather than air for the chamber interior, no bedrock writes, and a frequency/rarity placement chain.

- [x] **Step 2: Confirm the safety tests fail**

Run: `python -m pytest tools/tests/test_ocean_worldgen.py -q`

- [x] **Step 3: Implement the feature**

The feature rejects origins above sea level minus 14, bedrock-adjacent origins, and non-solid anchors. It evaluates a normalised ellipsoid. Interior replaceable stone blocks become water; the shell remains existing terrain except for sparse prismarine, calcite and sea-lantern landmarks. Every write remains inside the calculated radii.

- [x] **Step 4: Register and attach the feature**

Register `big_underwater_cave` in `ModFeatures`, create configured/placed JSON with rarity, in-square, height-range and biome filters, and reference the placed feature in the cave biome's local-modifications generation step.

- [x] **Step 5: Run focused tests**

Run: `python -m pytest tools/tests/test_ocean_worldgen.py -q`

Expected: PASS.

### Task 4: Verify integration without weakening existing model work

**Files:**
- Modify: `docs/UMR_ACTIVE_PROJECT_STATE.md`

- [x] **Step 1: Parse every new JSON resource**

Run: `python -m pytest tools/tests/test_ocean_worldgen.py -q`

- [x] **Step 2: Run project truth and model contracts**

Run: `python tools/verify_umr_project_truth.py`

Run: `python -m pytest tools/tests/test_umr_project_truth.py tools/tests/test_exact_mesh_alignment.py tools/tests/test_remaining_mob_renderers.py -q`

- [x] **Step 3: Compile and build**

Run: `.\gradlew.bat compileJava`

Run: `.\gradlew.bat build`

- [x] **Step 4: Perform fresh-world client validation**

Start the Forge client with TerraBlender present, create a fresh world, run `/locate biome usless_mobs:deep_ocean` and `/locate biome usless_mobs:big_underwater_cave`, teleport to both, and inspect cave flooding and boundaries. Do not claim worldgen completion if either locate command fails.

Verified with the same Forge/TerraBlender runtime in two complementary runs: Minecraft's own commands located `deep_ocean` at `[-256, 64, -1776]` and `big_underwater_cave` at `[-192, 0, 208]` on the local Dedicated Server; the fresh client world separately loaded the generated Deep Ocean/Whale Ruin and the flooded Big Underwater Cave at their saved-world coordinates. The Anvil scan confirms their distinct vertical biome ranges and the cave's bounded flooded landmark composition.

- [x] **Step 5: Record verified state**

Document dependency, biome keys, feature resources, commands and actual results in `docs/UMR_ACTIVE_PROJECT_STATE.md`. Explicitly list the Ancient Whale Ruin and Giant Squid encounter as the next separate package until implemented.
