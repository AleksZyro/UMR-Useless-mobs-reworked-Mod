# Corrupted Silverfish v5 Mesh Rig Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Das texturierte Tripo-Mesh verlustfrei in benannte, bewegliche Blockbench-Gruppen aufteilen und als neue Rig-Datei mit prüfbarer Ruhepose speichern.

**Architecture:** Ein lokales Python-Werkzeug liest das bestehende `.bbmodel`, ordnet jede Dreiecksfläche über explizite räumliche Gelenkgrenzen genau einer Region zu und erzeugt pro Region ein Mesh-Element. Ein unabhängiger Validator vergleicht kanonische Dreiecks-/UV-Signaturen, Texturbytes und Ruhepose mit der Quelle. Die Quelldatei und sämtliche Produktionsassets bleiben schreibgeschützt.

**Tech Stack:** Python 3 Standardbibliothek, Blockbench `.bbmodel` JSON, `unittest`, Blockbench Desktop für die visuelle Abnahme.

---

## File Map

- Create: `tools/corrupted_silverfish_v5/rig_mesh.py` — verlustfreie Segmentierung, Hierarchie, atomisches Schreiben und CLI.
- Create: `tools/corrupted_silverfish_v5/tests/test_rig_mesh.py` — synthetische und echte Vertragsprüfungen.
- Create: `Modelle/Exports/corrupted_silverfish_v5/blockbench/Corrupted Silverfish v5 Tripo Rig.bbmodel` — neue geriggte Arbeitsdatei.
- Create: `Modelle/Exports/corrupted_silverfish_v5/review/rig_segmentation.json` — reproduzierbarer Segmentierungsbericht.
- Create: `Modelle/Exports/corrupted_silverfish_v5/review/Corrupted Silverfish v5 Tripo Rig.png` — Blockbench-Zwischenstand.
- Read only: `Modelle/Exports/corrupted_silverfish_v5/blockbench/Corrupted Silverfish v5 Tripo Mesh.bbmodel`.

### Task 1: Lossless Geometry Contract

**Files:**
- Create: `tools/corrupted_silverfish_v5/tests/test_rig_mesh.py`
- Create: `tools/corrupted_silverfish_v5/rig_mesh.py`

- [ ] **Step 1: Write the failing canonical-signature tests**

Use a two-triangle fixture whose shared seam is assigned to different regions. Assert that `canonical_faces(source)` equals `canonical_faces(rigged)` and that texture `source`, width, height, UV width and UV height remain identical.

```python
def test_repartition_preserves_every_position_uv_and_texture():
    source = make_two_triangle_fixture()
    rigged, _ = build_rig_document(source)
    assert canonical_faces(rigged) == canonical_faces(source)
    assert texture_signature(rigged) == texture_signature(source)
```

- [ ] **Step 2: Run the focused test and verify RED**

Run: `python -m unittest tools.corrupted_silverfish_v5.tests.test_rig_mesh -v`

Expected: import failure for missing `rig_mesh` API.

- [ ] **Step 3: Implement canonical signatures and immutable loading**

Define these public functions with JSON-compatible dictionaries only:

```python
def load_document(path: Path) -> dict: ...
def canonical_faces(document: dict) -> Counter[tuple]: ...
def texture_signature(document: dict) -> tuple: ...
def build_rig_document(source: dict) -> tuple[dict, dict]: ...
```

The face signature contains the ordered triples `(position_xyz, uv_xy)` and the texture index. It deliberately ignores generated element, face and vertex IDs.

- [ ] **Step 4: Run the focused test and verify GREEN**

Run the same unittest command. Expected: all Task-1 tests pass.

- [ ] **Step 5: Commit the contract slice**

```text
git add tools/corrupted_silverfish_v5/rig_mesh.py tools/corrupted_silverfish_v5/tests/test_rig_mesh.py
git commit -m "test: define lossless tripo mesh rig contract"
```

### Task 2: Deterministic Joint Segmentation

**Files:**
- Modify: `tools/corrupted_silverfish_v5/rig_mesh.py`
- Modify: `tools/corrupted_silverfish_v5/tests/test_rig_mesh.py`

- [ ] **Step 1: Write failing region-classification tests**

Cover left/right separation by the sign of X, the three leg stations along Z, head/tail direction, three shell sections, deterministic boundary ownership and rejection of NaN/non-triangle data.

```python
def test_leg_regions_are_symmetric_and_use_three_z_stations():
    names = {classify_centroid(point) for point in LEG_CENTROIDS}
    assert names == {
        "leg_front_left", "leg_front_right",
        "leg_middle_left", "leg_middle_right",
        "leg_rear_left", "leg_rear_right",
    }
```

- [ ] **Step 2: Verify RED**

Run the focused unittest module. Expected: `classify_centroid` is missing.

- [ ] **Step 3: Implement explicit ordered classification**

Use the measured source bounds X `[-8.82498, 8.82498]`, Y `[0, 14.67634]`, Z `[-15.9814, 15.9814]`. Low lateral faces (`centroid_y < 4.4` and `abs(centroid_x) > 4.0`) are assigned first to a side and the nearest leg station Z `-6`, `0` or `6`. Remaining faces use the ordered bands `tail: z < -10`, `body_rear: -10 <= z < -3`, `body_middle: -3 <= z < 4`, `body_front: 4 <= z < 10`, `head: z >= 10`. Crystal faces remain attached to their owning shell/head region in this rig stage so the visible silhouette stays welded to that part.

- [ ] **Step 4: Add deterministic IDs and seam-local duplication**

For each face, copy only its referenced vertices into its region mesh. Reuse a copied vertex inside the same region and duplicate it only when the same source vertex is referenced from another region. Generate UUIDv5 element/group IDs from stable names and preserve the original face IDs where unique.

- [ ] **Step 5: Verify GREEN and determinism**

Run the focused tests twice and assert byte-identical JSON output for identical input.

- [ ] **Step 6: Commit the segmentation slice**

```text
git add tools/corrupted_silverfish_v5/rig_mesh.py tools/corrupted_silverfish_v5/tests/test_rig_mesh.py
git commit -m "feat: segment tripo mesh at rig joints"
```

### Task 3: Blockbench Bone Hierarchy and Pivots

**Files:**
- Modify: `tools/corrupted_silverfish_v5/rig_mesh.py`
- Modify: `tools/corrupted_silverfish_v5/tests/test_rig_mesh.py`

- [ ] **Step 1: Write failing hierarchy tests**

Require one `root` group, body chain `body_rear -> body_middle -> body_front -> head`, `tail` under `body_rear`, both front legs under `body_front`, both middle legs under `body_middle`, both rear legs under `body_rear`, and exactly one mesh UUID under every movable group.

- [ ] **Step 2: Verify RED**

Expected: generated outliner has no groups.

- [ ] **Step 3: Implement groups and pivots**

Create Blockbench group objects with stable UUIDv5 values and zero rest rotations. Body pivots use `[0, median_region_y, boundary_z]` at Z `-10`, `-3`, `4` and `10`; the tail pivot uses its attachment plane Z `-10`. Every leg pivot uses `[median_region_x, max_region_y, median_region_z]`, which places it at the upper attachment edge. Keep mesh coordinates in the source coordinate system; do not offset vertices when assigning a group.

```python
PARENTS = {
    "body_rear": "root", "tail": "body_rear",
    "body_middle": "body_rear", "body_front": "body_middle",
    "head": "body_front",
    "leg_front_left": "body_front", "leg_front_right": "body_front",
    "leg_middle_left": "body_middle", "leg_middle_right": "body_middle",
    "leg_rear_left": "body_rear", "leg_rear_right": "body_rear",
}
```

- [ ] **Step 4: Verify hierarchy, rest pose and zero animations**

Assert group parentage, finite pivots, all rotations `[0, 0, 0]`, `animations` absent or empty, and canonical geometry equality.

- [ ] **Step 5: Commit the rig slice**

```text
git add tools/corrupted_silverfish_v5/rig_mesh.py tools/corrupted_silverfish_v5/tests/test_rig_mesh.py
git commit -m "feat: add blockbench hierarchy to tripo mesh"
```

### Task 4: Safe CLI, Report and Real Candidate

**Files:**
- Modify: `tools/corrupted_silverfish_v5/rig_mesh.py`
- Modify: `tools/corrupted_silverfish_v5/tests/test_rig_mesh.py`
- Create: `Modelle/Exports/corrupted_silverfish_v5/blockbench/Corrupted Silverfish v5 Tripo Rig.bbmodel`
- Create: `Modelle/Exports/corrupted_silverfish_v5/review/rig_segmentation.json`

- [ ] **Step 1: Write failing CLI/atomic-write tests**

Test missing input, invalid JSON, output equal to input, write failure, replace failure, temporary-file cleanup and preservation of an existing target after every failure.

- [ ] **Step 2: Verify RED**

Expected: CLI and atomic writer are missing.

- [ ] **Step 3: Implement the CLI**

```text
python tools/corrupted_silverfish_v5/rig_mesh.py \
  --source "Modelle/Exports/corrupted_silverfish_v5/blockbench/Corrupted Silverfish v5 Tripo Mesh.bbmodel" \
  --output "Modelle/Exports/corrupted_silverfish_v5/blockbench/Corrupted Silverfish v5 Tripo Rig.bbmodel" \
  --report "Modelle/Exports/corrupted_silverfish_v5/review/rig_segmentation.json"
```

Stage both outputs with unique temporary files in their destination directories, close handles, validate staged bytes, then publish. On failure, retain the previous targets and remove only transaction-owned temporary files.

- [ ] **Step 4: Run the full v5 test module and build the candidate**

Expected CLI output: one `RIG_PASS` line containing source/output face counts, region count and texture hash.

- [ ] **Step 5: Independently validate the real candidate**

Re-read both files and assert equal canonical faces, equal texture signature, identical source texture SHA-256, expected hierarchy, zero animations and unchanged source SHA-256.

- [ ] **Step 6: Commit candidate and tooling**

```text
git add tools/corrupted_silverfish_v5/rig_mesh.py tools/corrupted_silverfish_v5/tests/test_rig_mesh.py "Modelle/Exports/corrupted_silverfish_v5/blockbench/Corrupted Silverfish v5 Tripo Rig.bbmodel" Modelle/Exports/corrupted_silverfish_v5/review/rig_segmentation.json
git commit -m "feat: build faithful tripo mesh rig"
```

### Task 5: Blockbench Visual Checkpoint

**Files:**
- Create: `Modelle/Exports/corrupted_silverfish_v5/review/Corrupted Silverfish v5 Tripo Rig.png`

- [ ] **Step 1: Open only the new rig file in Blockbench Desktop**

Verify that the title names `Corrupted Silverfish v5 Tripo Rig`, the Outliner shows the expected hierarchy, the embedded texture resolves, and the model is not magenta/black missing-texture material.

- [ ] **Step 2: Compare the same rest-pose camera against the source**

Capture the rig from the same camera direction and compare visible silhouette, armour seams, crystals, eyes, legs and texture placement. Any visible difference blocks approval and must be fixed before animation work.

- [ ] **Step 3: Save and inspect the checkpoint PNG**

Use Blockbench's screenshot dialog, save the PNG in the review directory, inspect it with the local image viewer and record dimensions/SHA-256.

- [ ] **Step 4: Final non-regression checks**

Run the focused v5 tests, `git diff --check`, and verify no tracked file under production assets, Java sources or `corrupted_silverfish_v2` through `v4` changed.

- [ ] **Step 5: Commit the visual checkpoint**

```text
git add "Modelle/Exports/corrupted_silverfish_v5/review/Corrupted Silverfish v5 Tripo Rig.png"
git commit -m "docs: add tripo mesh rig checkpoint"
```

Stop after this checkpoint and request visual approval. Do not create animations or promote runtime assets in this plan.
