from __future__ import annotations

import subprocess
import sys
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


class UmrProjectTruthContractTests(unittest.TestCase):
    def test_persistent_context_files_exist_and_name_the_runtime_truth(self) -> None:
        agents = (ROOT / "AGENTS.md").read_text(encoding="utf-8")
        state = (ROOT / "docs" / "UMR_ACTIVE_PROJECT_STATE.md").read_text(encoding="utf-8")

        for text in (agents, state):
            self.assertIn("verify_umr_project_truth.py", text)
        self.assertIn("feature/corrupted-silverfish-v3", state)
        self.assertIn("101,723", state)
        self.assertIn("4096", state)
        self.assertIn("0 Cubes", state)
        self.assertIn("Tripo-Mesh", state)

    def test_verifier_accepts_the_active_runtime(self) -> None:
        result = subprocess.run(
            [sys.executable, "tools/verify_umr_project_truth.py", "--root", str(ROOT)],
            cwd=ROOT,
            text=True,
            capture_output=True,
            check=False,
        )
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("UMR_PROJECT_TRUTH_PASS", result.stdout)
        self.assertIn("triangles=101723", result.stdout)
        self.assertIn("cubes=0", result.stdout)


if __name__ == "__main__":
    unittest.main()
