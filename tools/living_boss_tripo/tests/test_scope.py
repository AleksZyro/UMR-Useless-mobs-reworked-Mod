from __future__ import annotations

from pathlib import Path
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
class LivingBossPilotScopeTests(unittest.TestCase):
    def test_exact_mesh_renderers_have_a_valid_invisible_base_pass(self) -> None:
        self.assertTrue(TRANSPARENT_BASE.is_file())
        sources = "\n".join(
            (CLIENT / filename).read_text(encoding="utf-8")
            for filename in TARGET_RENDERERS
        )
        layers = (CLIENT / "CustomMobModelLayers.java").read_text(encoding="utf-8")
        self.assertIn("TRANSPARENT_BASE_TEXTURE", sources + layers)
        self.assertIn('texture("textures/entity/custom3d/transparent_base.png")', layers)

    def test_all_integrated_target_renderers_are_present(self) -> None:
        missing = [filename for filename in TARGET_RENDERERS if not (CLIENT / filename).is_file()]
        self.assertEqual([], missing)


if __name__ == "__main__":
    unittest.main()
