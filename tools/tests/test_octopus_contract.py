import json
import unittest
from hashlib import sha256
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
OCTOPUS = ROOT / "Modelle/Exports/octopus_v1"


class OctopusContract(unittest.TestCase):
    def test_approved_octopus_source_is_archived_and_hashed(self):
        provenance_path = OCTOPUS / "review/source_provenance.json"
        provenance = json.loads(provenance_path.read_text(encoding="utf-8"))
        source = (provenance_path.parent / provenance["source_glb"]).resolve()

        self.assertEqual("Tripo Multi-View Builder", provenance["generator"])
        self.assertEqual("4096x4096", provenance["texture_quality"])
        self.assertTrue(provenance["approved"])
        self.assertEqual(sha256(source.read_bytes()).hexdigest(), provenance["source_sha256"])
        self.assertEqual("-Z", provenance["approved_front_axis"])


if __name__ == "__main__":
    unittest.main()
