# Corrupted Silverfish v5: Tripo-to-Blockbench design

## Goal

Create a separate Minecraft-compatible v5 block model from the approved textured Tripo GLB. The existing v2-v4 models and production assets remain unchanged.

## Chosen approach

- Parse the local GLB and its embedded base-colour texture without cloud services.
- Scale the real Tripo mesh to a roughly 32-unit Minecraft entity length.
- Voxelise the mesh surface and fill its closed volume on a bounded grid.
- Greedily merge adjacent voxels with the same quantised colour into cuboids.
- Generate a small power-of-two palette texture and deterministic per-face UVs.
- Export a new Blockbench `.bbmodel` plus front, side, back, top, and perspective review images.

The Tripo triangle mesh is the geometry source. It is not shipped directly because the project's GeckoLib 4 `.geo.json` runtime path is cube-and-bone based.

## Acceptance criteria

- The silhouette retains the Tripo head, three shell sections, six legs, tail, dorsal crystals, grey armour, magenta corruption, and blue eyes.
- Output is deterministic, finite, bounded, and uses substantially fewer cuboids than one cube per occupied voxel.
- Texture dimensions are powers of two and every generated face references a valid UV region.
- No v2-v4 or production file changes.
- Model and texture are visually reviewed before rigging or animation begins.

## Failure handling

Invalid GLB chunks, unsupported accessors, missing UVs/textures, empty voxel output, or excessive cuboid count fail with a clear message and do not overwrite a previous candidate.

## Verification

Unit tests cover GLB extraction, scaling, occupancy, greedy merging, palette/UV bounds, deterministic output, and protected-path checks. Review images provide the user-facing visual gate.
