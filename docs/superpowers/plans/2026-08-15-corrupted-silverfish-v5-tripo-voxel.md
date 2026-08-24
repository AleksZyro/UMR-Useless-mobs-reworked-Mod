# Corrupted Silverfish v5 Tripo Voxel Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert the approved textured Tripo GLB into a separate deterministic cuboid-based Blockbench v5 candidate with visual review renders.

**Architecture:** A pure-Python converter parses GLB accessors and the embedded base-colour image, voxelises the real mesh on a bounded Minecraft-sized grid, quantises sampled colours, merges voxels into cuboids, and writes a palette-textured `.bbmodel`. A separate renderer produces fixed review views without touching production assets.

**Tech Stack:** Python 3.9 standard library, NumPy, Pillow, Blockbench `.bbmodel` JSON, `unittest`.

---

### Task 1: Parse and normalise the Tripo GLB

**Files:**
- Create: `tools/corrupted_silverfish_v5/tripo_voxel.py`
- Create: `tools/corrupted_silverfish_v5/tests/test_tripo_voxel.py`

- [ ] **Step 1: Write the failing GLB extraction test**

```python
def test_load_glb_extracts_mesh_uv_and_base_colour(self):
    model = load_glb(self.glb_path)
    self.assertEqual(model.positions.shape[1], 3)
    self.assertEqual(model.uvs.shape[1], 2)
    self.assertEqual(model.triangles.shape[1], 3)
    self.assertEqual(model.base_colour.mode, "RGBA")
```

- [ ] **Step 2: Verify RED**

Run: `python -m unittest tools.corrupted_silverfish_v5.tests.test_tripo_voxel -v`
Expected: FAIL because `tripo_voxel` does not exist.

- [ ] **Step 3: Implement strict GLB parsing and 32-unit normalisation**

Implement `load_glb(path)` and `normalise_positions(positions, target_length=32.0)` with bounds checks for chunk types, component types, accessor ranges, finite values, UVs, indices, and embedded PNG/JPEG data.

- [ ] **Step 4: Verify GREEN and commit**

Run the same unittest command; expected PASS. Commit only the parser and its tests with `feat: parse tripo glb for voxel conversion`.

### Task 2: Voxelise, colour, merge, and export Blockbench

**Files:**
- Modify: `tools/corrupted_silverfish_v5/tripo_voxel.py`
- Modify: `tools/corrupted_silverfish_v5/tests/test_tripo_voxel.py`
- Create: `Modelle/Exports/corrupted_silverfish_v5/blockbench/Corrupted Silverfish v5 Tripo Cubes.bbmodel`
- Create: `Modelle/Exports/corrupted_silverfish_v5/blockbench/corrupted_silverfish_v5_palette.png`

- [ ] **Step 1: Write failing geometry-contract tests**

```python
def test_candidate_is_bounded_merged_and_textured(self):
    candidate = build_candidate(self.glb_path)
    self.assertLess(candidate.cuboid_count, candidate.occupied_voxel_count)
    self.assertLess(candidate.cuboid_count, 5000)
    self.assertEqual(candidate.texture_size, (16, 16))
    self.assertTrue(candidate.all_uvs_in_bounds)
```

Add tests for deterministic bytes, empty/corrupt input, excessive cuboid count, and protected output paths.

- [ ] **Step 2: Verify RED**

Run the focused unittest; expected failures for missing voxel and export functions.

- [ ] **Step 3: Implement the minimum converter**

Implement triangle sampling into integer voxels, bounded interior filling, nearest-UV base-colour sampling, a fixed quantised palette, deterministic axis-aligned greedy merging, palette UV generation, and atomic `.bbmodel`/PNG writes. Name body regions from position bands so later rigging can split head, shell sections, legs, and tail.

- [ ] **Step 4: Build, verify, and commit**

Run the focused tests and converter CLI. Verify JSON parses, PNG is RGBA 16x16, cuboid count is below 5000, outputs are byte-deterministic, and protected paths have no diff. Commit with `feat: voxelise tripo silverfish into blockbench cubes`.

### Task 3: Render the visual approval set

**Files:**
- Create: `tools/corrupted_silverfish_v5/render_candidate.py`
- Create: `tools/corrupted_silverfish_v5/tests/test_render_candidate.py`
- Create: `Modelle/Exports/corrupted_silverfish_v5/review/*.png`

- [ ] **Step 1: Write failing render tests**

```python
def test_review_set_contains_fixed_views(self):
    paths = render_review_set(self.bbmodel, self.output_dir)
    self.assertEqual({p.name for p in paths}, {
        "front.png", "right.png", "back.png", "top.png", "perspective.png"
    })
```

Require non-empty alpha bounds, identical framing scale, and deterministic PNG bytes.

- [ ] **Step 2: Verify RED**

Run: `python -m unittest discover -s tools/corrupted_silverfish_v5/tests -v`
Expected: render tests fail because the renderer is absent.

- [ ] **Step 3: Implement orthographic cuboid rendering**

Reuse the proven v3 software-rendering conventions for camera transforms, per-face depth, nearest palette sampling, transparent backgrounds, and atomic multi-output publication.

- [ ] **Step 4: Verify, inspect, and commit**

Run all v5 tests, open all five images for visual inspection, compare silhouette/features against the Tripo reference, and confirm no production/v2-v4 diff. Commit with `feat: render tripo voxel candidate review views`.

The next phase (bones and animation) starts only after the user approves these model-and-texture review images.
