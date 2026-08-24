import hashlib
import json
from pathlib import Path
import re
import unittest

from PIL import Image


ROOT = Path(__file__).resolve().parents[3]
CLIENT = ROOT / "src/main/mobs/endermite/java/net/mysith/client"
RENDERER = CLIENT / "CorruptedSilverfishRenderer.java"
LAYER = CLIENT / "CorruptedSilverfishGlowLayer.java"
EXPORT_GLOW = ROOT / (
    "Modelle/Exports/corrupted_silverfish_v3/textures/entity/"
    "corrupted_silverfish_glowmask.png"
)
PRODUCTION_GLOW = ROOT / (
    "src/main/mobs/endermite/resources/assets/usless_mobs/textures/entity/"
    "corrupted_silverfish_glowmask.png"
)
MANIFEST = ROOT / (
    "Modelle/Exports/corrupted_silverfish_v3/review/candidate-sha256.json"
)


class GlowLayerIntegrationContract(unittest.TestCase):
    def test_layer_registration_is_single_and_resource_guarded(self):
        layer_source = LAYER.read_text(encoding="utf-8")
        renderer_source = RENDERER.read_text(encoding="utf-8")
        client_source = "\n".join(
            path.read_text(encoding="utf-8") for path in CLIENT.glob("*.java")
        )

        self.assertEqual(
            1,
            len(re.findall(r"\bclass\s+CorruptedSilverfishGlowLayer\b", client_source)),
        )
        self.assertIn(
            "extends AutoGlowingGeoLayer<CorruptedSilverfishEntity>", layer_source
        )
        registration = "addRenderLayer(new CorruptedSilverfishGlowLayer(this));"
        self.assertEqual(1, client_source.count(registration))

        self.assertRegex(
            renderer_source,
            r"static\s+final\s+ResourceLocation\s+\w+\s*=\s*ResourceLocation\.tryBuild\("
            r'\s*"usless_mobs"\s*,\s*"textures/entity/corrupted_silverfish_glowmask\.png"\s*\)',
        )
        guarded_registration = re.compile(
            r"if\s*\(\s*context\.getResourceManager\(\)\.getResource\(\w+\)"
            r"\.isPresent\(\)\s*\)\s*\{[^{}]*"
            r"addRenderLayer\(new\s+CorruptedSilverfishGlowLayer\(this\)\);[^{}]*\}",
            re.DOTALL,
        )
        guard_reason = (
            "production glowmask is absent, so layer registration must be guarded"
            if not PRODUCTION_GLOW.exists()
            else "layer registration must remain guarded by resource existence"
        )
        self.assertRegex(renderer_source, guarded_registration, guard_reason)
        self.assertNotIn("Minecraft.getInstance()", renderer_source)

    def test_export_glow_matches_candidate_manifest(self):
        self.assertTrue(EXPORT_GLOW.is_file())
        with Image.open(EXPORT_GLOW) as image:
            self.assertEqual("RGBA", image.mode)
            self.assertEqual((256, 256), image.size)

        manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
        relative = EXPORT_GLOW.relative_to(ROOT).as_posix()
        self.assertIn(relative, manifest)
        digest = hashlib.sha256(EXPORT_GLOW.read_bytes()).hexdigest().upper()
        self.assertEqual(manifest[relative], digest)


if __name__ == "__main__":
    unittest.main()
