from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[3]
EXPORT = ROOT / "Modelle" / "Exports" / "corrupted_silverfish_v3"

class GeneratedOutputContract(unittest.TestCase):
    def test_required_candidate_files_exist(self):
        required = (
            EXPORT / "geo" / "corrupted_silverfish.geo.json",
            EXPORT / "textures" / "entity" / "corrupted_silverfish.png",
            EXPORT / "textures" / "entity" / "corrupted_silverfish_glowmask.png",
            EXPORT / "animations" / "corrupted_silverfish.animation.json",
            ROOT / "Modelle" / "Editierbar" / "Corrupted Silverfish v3.bbmodel",
        )
        self.assertEqual([], [str(path) for path in required if not path.is_file()])

if __name__ == "__main__":
    unittest.main()
