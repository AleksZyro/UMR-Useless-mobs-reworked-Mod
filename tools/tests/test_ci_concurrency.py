from pathlib import Path

import yaml


ROOT = Path(__file__).resolve().parents[2]


def test_build_workflow_cancels_superseded_runs_for_the_same_ref():
    workflow = yaml.safe_load((ROOT / ".github/workflows/build.yml").read_text(encoding="utf-8"))
    concurrency = workflow.get("concurrency", {})

    assert "github.workflow" in concurrency.get("group", "")
    assert "github.ref" in concurrency.get("group", "")
    assert concurrency.get("cancel-in-progress") is True
