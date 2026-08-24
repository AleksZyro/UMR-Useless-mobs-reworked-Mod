from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


def test_build_workflow_cancels_superseded_runs_for_the_same_ref():
    workflow = (ROOT / ".github/workflows/build.yml").read_text(encoding="utf-8")

    assert "concurrency:\n  group: build-${{ github.workflow }}-${{ github.ref }}" in workflow
    assert "  cancel-in-progress: true" in workflow
