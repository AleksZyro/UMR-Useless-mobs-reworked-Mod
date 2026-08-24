# Endermite / MySith Dark-Side Module

This folder contains the dark side of the mod as if it were its own small mod:

- `java/net/mysith`: MySith gameplay code, events, registries, commands, client code, and networking.
- `java/net/mysith/silverfish`: Corrupted Silverfish rework, dark-side tools, drops, and Crystal Leggings.
- `resources`: MySith assets plus dark-side silverfish models, recipes, advancements, loot modifiers, language files, and mixins.

The MySith package/resource namespace still uses `net.mysith` / `mysith`.
Silverfish registries and assets intentionally keep their existing `usless_mobs` ids so saves, commands, and recipes keep working.
