---
name: minecraft-tripo-rigging
description: Use when converting Tripo, GLB, OBJ, FBX, or textured mesh creatures into Blockbench and GeckoLib models, especially when animation creates cracks, sliced bodies, detached parts, backwards movement, stiff walking, broken UVs, or visual differences from the source.
---

# Minecraft Tripo Rigging

## Core principle

Preserve topology before adding motion. Never split a connected visible shell into rigid bones by face centroid or coordinate planes: rotations will open the duplicated boundary and produce visible cuts.

**REQUIRED BACKGROUND:** Use `minecraft-geckolib-curios-blockbench`, `systematic-debugging`, `test-driven-development`, and `verification-before-completion`.

## Workflow

1. Save immutable source files, texture hashes, face count, bounds, and four reference views. Confirm the model licence.
2. Inspect connected components, shared vertices, UVs, normals, pivots, scale, and forward axis before editing.
3. Choose the rig representation:
   - Use weighted skinning when the runtime supports deforming connected meshes.
   - Use rigid bones only for disconnected pieces or intentional hinges.
   - If weights are unavailable, keep the connected torso/head/tail shell on one body bone. Animate appendages only at anatomical joints.
   - Use a cube rebuild only when a Minecraft-cube result is explicitly wanted.
4. Run `python scripts/audit_mesh_rig.py MODEL.bbmodel`. Every reported cross-bone seam is unsafe until justified. Allow only deliberate joints with repeated `--allow-seam body:leg_name`; never allowlist torso-to-torso cuts.
5. Create pivots at attachment points. Keep the body transform subtle. For insect walking, alternate two tripods and sample a smooth closed cycle at 16–20 keyframes per second. Avoid large adjacent angular jumps.
6. Test orientation by moving the entity forward in the real client. If the head trails movement, correct one renderer-level Y rotation; do not rewrite mesh coordinates or UVs.
7. Export Blockbench, GeckoLib geometry, animations, texture, and any custom mesh from one approved source. Validate exact face/UV preservation and deterministic output.
8. Show checkpoints before integration: front, side, top, perspective, idle, and a mid-stride frame.
9. Compile and run the actual mod client. Observe a moving entity from third person. Reject gaps, detached limbs, backwards travel, foot sliding, clipping, missing textures, or a silhouette that differs materially from the source.

## Release gate

Do not call a model finished unless all are true:

| Check | Required evidence |
| --- | --- |
| Source fidelity | Matching texture hash, face count, UVs, and silhouette |
| Cohesion | No unapproved shared-position seam between animated bones |
| Motion | Closed loop, smooth adjacent steps, visible alternating legs |
| Direction | Head points along measured displacement |
| Runtime | Build passes, client loads, moving screenshot/video inspected |
| Safety | Previous production assets preserved until user approval |

## Common mistakes

- A successful export does not prove visual quality.
- A static screenshot does not prove walking direction or animation quality.
- More bones do not create better motion when the mesh has no weights.
- Triangle-to-cube conversion does not preserve an exact Tripo model.
- Do not promote generated assets before the user approves the visual checkpoint.

## Example

```powershell
python scripts/audit_mesh_rig.py creature.bbmodel `
  --allow-seam body:leg_front_left `
  --allow-seam body:leg_front_right
```

Treat a nonzero exit as a blocked rig, not as a warning to ignore.
