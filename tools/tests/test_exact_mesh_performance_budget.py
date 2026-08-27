import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
REPORT_ROOT = ROOT / "src/main/resources/assets/usless_mobs/meshes/entity/custom3d"


def _active_reports() -> list[dict]:
    return [
        json.loads(path.read_text(encoding="utf-8"))
        for path in sorted(REPORT_ROOT.glob("*.report.json"))
    ]


def test_exact_runtime_meshes_stay_within_the_per_mob_triangle_budget():
    reports = _active_reports()

    offenders = {
        report["mob"]: report["output_triangles"]
        for report in reports
        if report["output_triangles"] > 110_000
    }
    assert offenders == {}


def test_exact_runtime_mesh_collection_stays_within_the_frame_budget():
    reports = _active_reports()

    assert sum(report["output_triangles"] for report in reports) <= 1_500_000


def test_performance_meshes_keep_4k_sources_but_ship_2k_runtime_textures():
    reports = _active_reports()

    assert reports
    assert all(report["texture_width"] == 4096 for report in reports)
    assert all(report["texture_height"] == 4096 for report in reports)
    assert all(report["source_texture_width"] == 4096 for report in reports)
    assert all(report["source_texture_height"] == 4096 for report in reports)
    assert all(report["runtime_texture_width"] == 2048 for report in reports)
    assert all(report["runtime_texture_height"] == 2048 for report in reports)
