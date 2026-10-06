# Frost Stray v2 – editable preview source

This folder contains the editable declaration for the v2 visual preview.
`frost_stray_v2_rig.json` defines twelve ice overlay sockets and their pivots.
The connected Frost Stray shell remains on the approved six-region runtime mesh;
the rigging audit forbids splitting that unweighted shell into rigid body bones.

The runtime overlay is generated with:

```powershell
python tools/frost_stray_v2/build_assets.py
```

The generated 64×64 overlay texture is deliberately separate from the existing
2048×2048 Frost Stray albedo. Its source colours, UV-safe layout and runtime
budget are encoded in the generator, so the asset can be recreated without
relying on ignored work files. The v2 test entity is selectable with:

```text
/summon usless_mobs:frost_stray_v2 ~ ~ ~
```

The full replacement of the connected shell with a weighted 10–14-bone body
remains a follow-up until an editable weighted source is supplied and passes
the seam audit.
