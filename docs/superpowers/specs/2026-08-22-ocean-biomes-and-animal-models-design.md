# UMR Ocean Biomes and Animal Models Design

**Date:** 2026-08-22

**Status:** Approved by Andrin on 2026-08-22

**Target:** Minecraft 1.20.1, Forge 47.4.16, Java 17
**Active worktree:** `slime/.worktrees/corrupted-silverfish-v3`

## 1. Objective

Extend UMR with two naturally generated Overworld ocean biomes, a large underwater boss ruin, a Giant Squid boss, distinct squid and octopus identities, and a strict quality-controlled model pipeline for the remaining animals.

The active Corrupted Silverfish is the visual and technical quality baseline. Future approved Tripo models must preserve their exact source mesh and original texture placement instead of being approximated with generated cubes.

## 2. Approved content order

The implementation order is fixed:

1. Helping Allay
2. True new Octopus
3. New Squid with eight arms and two long catching tentacles
3a. Giant Squid boss, Deep Ocean, and Ancient Whale Ruin
4. Polar Bear
5. Frost Stray
6. Coral Drowned
7. Glow Squid and Big Underwater Cave
8. Axolotl
9. Ocelot
10. Polish already integrated animals

The ocean world-generation work is attached to the creature that needs it. It must not silently reorder the approved model sequence.

## 3. World-generation architecture

### 3.1 Chosen approach

Use a hybrid Overworld solution:

- TerraBlender inserts the custom biome climate points compatibly into the normal Overworld biome source.
- UMR-owned configured and placed features create the reliable large cave geometry.
- UMR-owned structures create the Ancient Whale Ruin and its boss arena.
- Forge biome and structure modifiers attach features and spawns without replacing unrelated vanilla generation.

This is preferred over a separate dimension and over pretending that a structure inside a vanilla ocean is a custom biome.

### 3.2 Compatibility

- New biomes and structures generate only in newly generated chunks.
- Existing chunks, inventories, entities, and saves remain valid.
- The implementation must not replace the global Overworld noise settings.
- Spawn and placement weights must be configuration-backed so modpacks can reduce or disable them.
- TerraBlender is a required dependency for builds that include the custom biome integration.

Primary reference: <https://github.com/Glitchfiend/TerraBlender>

## 4. Deep Ocean biome

The UMR Deep Ocean is a rare, naturally integrated Overworld ocean biome.

### Environment

- Very deep seabed with long dark sightlines.
- Dark blue water and restrained ambient particles.
- Low natural visibility without permanently blinding the player.
- Sparse light sources that guide exploration toward ruins.
- Deep, distant custom ambience and creature calls.

### Generation

- Climate placement must overlap only valid ocean climate points.
- Terrain depth should produce a recognisably deeper basin than vanilla deep ocean.
- The biome contains occasional whale-bone fields and very rare Ancient Whale Ruins.
- Ruins must not intersect land, the ocean surface, or unloaded void-like terrain.

## 5. Big Underwater Cave biome

The Big Underwater Cave is a rare, fully flooded cave biome beneath ocean terrain.

### Environment

- Large navigable chambers rather than narrow noise cracks.
- Multiple entrances, but no mandatory one-block passages.
- Luminous plants, mineral clusters, and suspended particles.
- Distinct turquoise and cold-white lighting identity.
- Safe pockets and navigation landmarks so the cave is explorable without permanent Night Vision.

### Glow creature role

The cave contains the new Glow Squid variant. Its defensive ability is the opposite of ordinary squid ink:

- A normal squid releases dark ink and causes Darkness or Blindness-like visual obstruction.
- The cave Glow Squid releases an intense light pulse.
- The pulse rapidly raises screen brightness, briefly washes out colour and contrast, and disorients nearby attackers.
- The effect must be short, telegraphed, and accessibility-configurable.
- Repeated pulses use immunity and cooldown windows so the player cannot be permanently flash-locked.

## 6. Ancient Whale Ruin

The boss arena is a large ruined complex, not a small decorative ruin.

### Composition

- A central ancient temple or treasury.
- Several giant whale skeletons built mainly from bone blocks.
- Broken arches, collapsed chambers, dark alcoves, and multiple swimming routes.
- Several gold blocks concentrated in the central treasure chamber.
- Deliberate boss movement volume around and above the ruin.

### Activation

The encounter begins when a player breaks the first protected central gold block:

1. The ruin records the encounter as activated.
2. Ambient light and visibility fall.
3. A deep warning sound and distant silhouette telegraph the boss.
4. Strong currents temporarily make immediate escape harder without hard-locking the player.
5. The Giant Squid emerges from a dark alcove outside the player's direct view.
6. Remaining protected treasure becomes fully recoverable after victory.

The trigger must be stored per structure so relogging, chunk unloading, or server restart cannot duplicate the boss or rewards.

## 7. Giant Squid boss

### Visual identity

- Ten limbs: eight arms plus two substantially longer catching tentacles.
- Large mantle with side fins and a recognisable forward-facing head.
- Exact approved textured mesh, not a cube reconstruction.
- Segmented rig with mantle, head, fins, arm roots, arm tips, catching-tentacle chains, and weak-point regions.

### Combat phases

1. **Stalking:** distant silhouette, ink clouds, tentacle probes, and short jet repositioning.
2. **Hunt:** sweeping arms, two-tentacle grab, body charge, and current pull.
3. **Ruin collapse:** falling debris warnings, whirlpool zones, stronger ink, and exposed luminous weak points.
4. **Desperation:** rapid jet dashes and alternating grab/ink patterns without unavoidable attack overlap.

### Fairness

- Every large attack has a distinct animation and sound telegraph.
- Grabs allow escape through damage, movement, or a timed interaction.
- The boss cannot suffocate or die from touching normal terrain cavities.
- Its multipart hitboxes follow mantle, head, and tentacles closely enough for fair melee and projectile hits.
- Difficulty changes health, cooldowns, add count, and loot quantity rather than only multiplying damage.

## 8. Exact Tripo-to-runtime model pipeline

Every new animal follows the active Corrupted Silverfish truth pipeline:

1. Produce and approve front, back, left, and right concept views with consistent proportions and texture placement.
2. Generate one textured multi-view Tripo model.
3. Export the textured source in the highest useful available quality.
4. Archive the untouched source export and provenance.
5. Verify material references, texture files, orientation, and UVs before rigging.
6. Segment the exact source mesh into semantic regions without remeshing or texture rebaking unless explicitly approved.
7. Rig the semantic regions in Blockbench/GeckoLib-compatible form.
8. Export the exact runtime mesh and retain the original texture mapping.
9. Validate triangle count, region count, texture dimensions, hashes, missing UVs, empty regions, seams, scale, and forward axis.
10. Render front, side, back, top, and perspective comparison images before enabling the model in game.
11. Test animation, hitbox, spawn size, shadows, culling, lighting, and texture placement in the actual Forge client.

Generated concepts are references. They are never presented as completed in-game models. A Tripo preview is not accepted as runtime proof until the exported source has passed the exact-mesh checks.

## 9. Helping Allay — first implementation target

### Existing gameplay to preserve

The existing Helping Allay is a bonded vanilla Allay state:

- activated with an Amethyst Shard;
- follows its owner and teleports back at long range;
- marks and weakens nearby hostile mobs;
- grants temporary absorption and resistance under pressure;
- periodically heals its owner;
- support duration can be extended with a Glow Flare.

### New visual design

- Recognisable Allay silhouette with a refined spectral guardian identity.
- Cyan soul-light base, amethyst accents, and restrained gold details.
- A visible soul core in the chest.
- Layered translucent-looking wings represented through real textured geometry and controlled render passes, not missing-opacity pixels.
- Clear face and forward axis in all source views.
- No armour-like bulk that prevents it reading as a small flying helper.

### Rig and animation set

- `root`
- `body`
- `head`
- `arm_left`
- `arm_right`
- independent upper and lower wing regions for both sides
- `soul_core`

Animations:

- hovering idle with secondary wing motion;
- forward flight and braking;
- owner-follow acceleration;
- held-item pose;
- enemy reveal/weakness cast;
- shield pulse;
- healing pulse;
- bond activation;
- long-range return/teleport.

Effects and sounds must be triggered from the same server-authoritative action state that drives the client animation. Cosmetic particles must not duplicate gameplay effects.

## 10. Remaining creature identities

### True Octopus

- Eight similar arms, broad soft mantle, no squid fins, no two catching tentacles.
- Intelligent camouflage, invisibility, squeezing, object interaction, and ambush behaviour.

### Squid

- Eight arms plus two long catching tentacles, side fins, and streamlined mantle.
- Dark ink defence, jet escape, and coordinated tentacle animation.

### Polar Bear

- Correct bear scale and grounded quadruped proportions.
- Preserve charge, resistance, claw loot, and necklace systems.
- Add model, textures, animation, hitbox audit, and custom sounds.

### Frost Stray and Coral Drowned

- Regenerate or re-export correct forward-facing source models where needed.
- Preserve exact Tripo textures and correct misplaced UV/material binding.
- Replace temporary cuboid or overlay rendering only after exact-runtime QA passes.

### Glow Squid, Axolotl, and Ocelot

- Each receives a distinct approved concept and exact textured runtime model.
- Existing gameplay handlers remain the baseline and are expanded only where the visual design requires telegraphed actions.

## 11. Validation and acceptance

### Automated checks

- `python tools/verify_umr_project_truth.py`
- focused model-pipeline tests
- JSON parsing for biome, feature, structure, loot, animation, and sound resources
- `gradlew.bat compileJava`
- `gradlew.bat build`
- data generation when registry data changes

### In-game checks

- Locate both biomes in a fresh world.
- Confirm all caves are flooded and navigable.
- Locate the ruin and verify its bounding box and terrain placement.
- Activate the boss once, unload/reload chunks, and restart the client/server.
- Verify no duplicate boss or treasure state.
- Test flashbang accessibility settings and cooldown immunity.
- Compare each runtime model against approved Tripo views from all major angles.
- Verify model scale and hitbox using `F3+B`.

### Acceptance gate for each model

A model is complete only when:

- source provenance is recorded;
- exact geometry and texture mapping are verified;
- orientation and size are correct;
- animations are natural and do not cut the mesh;
- gameplay hitboxes fit the visible body;
- client comparison images are approved;
- compilation and targeted tests pass.

## 12. Scope protection

- Do not rewrite unrelated dirty worktree changes.
- Do not replace the active Corrupted Silverfish with v2 or an approximation.
- Do not treat old main-worktree assets as active runtime truth.
- Do not enable a generated model merely because it looks acceptable in Tripo.
- Implement and verify one approved creature slice at a time, starting with Helping Allay.
