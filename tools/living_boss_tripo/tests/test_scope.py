from __future__ import annotations

from pathlib import Path
import subprocess
import unittest


ROOT = Path(__file__).resolve().parents[3]
CLIENT = ROOT / "src" / "main" / "java" / "com" / "Momik" / "usless_mobs" / "client"
TARGET_RENDERERS = (
    "LivingBossRenderer.java",
    "FrostStrayRenderer.java",
    "WebCaveSpiderRenderer.java",
    "CoralDrownedRenderer.java",
    "OctopusRenderer.java",
    "WitchBossRenderer.java",
    "LivingBatRenderer.java",
    "RootedHuskRenderer.java",
)
TRANSPARENT_BASE = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "usless_mobs"
    / "textures"
    / "entity"
    / "custom3d"
    / "transparent_base.png"
)
PROTECTED_MARKERS = (
    "slime",
    "silverfish",
    "endermite",
    "armor",
    "armour",
    "crown",
    "worntruepatharmormodel",
)


class LivingBossPilotScopeTests(unittest.TestCase):
    def test_rejected_transparent_base_workaround_is_absent(self) -> None:
        self.assertFalse(TRANSPARENT_BASE.exists())
        sources = "\n".join(
            (CLIENT / filename).read_text(encoding="utf-8")
            for filename in TARGET_RENDERERS
        )
        layers = (CLIENT / "CustomMobModelLayers.java").read_text(encoding="utf-8")
        self.assertNotIn("TRANSPARENT_BASE_TEXTURE", sources + layers)
        self.assertNotIn("this.layers.clear();", sources)

    def test_excluded_tracked_paths_have_no_worktree_changes(self) -> None:
        completed = subprocess.run(
            ["git", "diff", "--name-only", "HEAD", "--"],
            cwd=ROOT,
            check=True,
            capture_output=True,
            text=True,
        )
        changed = [line.strip().lower() for line in completed.stdout.splitlines() if line.strip()]
        protected = [
            path
            for path in changed
            if any(marker in path for marker in PROTECTED_MARKERS)
        ]
        self.assertEqual([], protected, f"Excluded tracked paths changed: {protected}")


if __name__ == "__main__":
    unittest.main()
