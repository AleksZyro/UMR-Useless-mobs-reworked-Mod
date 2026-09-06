from tools.mob_tripo.diagnose_uv_fidelity import runtime_contract_passes


def test_accepts_exact_runtime_mesh():
    assert runtime_contract_passes({
        "pixels_equal": True,
        "uv_exact": True,
        "expected_runtime_triangles": 100,
        "runtime_triangles": 100,
    })


def test_accepts_only_a_recorded_verified_shell_removal():
    assert runtime_contract_passes({
        "pixels_equal": True,
        "uv_exact": True,
        "expected_runtime_triangles": 90,
        "runtime_triangles": 90,
    })
    assert not runtime_contract_passes({
        "pixels_equal": True,
        "uv_exact": True,
        "expected_runtime_triangles": 90,
        "runtime_triangles": 89,
    })


def test_rejects_texture_or_uv_changes_even_when_triangle_count_matches():
    base = {
        "pixels_equal": True,
        "uv_exact": True,
        "expected_runtime_triangles": 90,
        "runtime_triangles": 90,
    }
    assert not runtime_contract_passes({**base, "pixels_equal": False})
    assert not runtime_contract_passes({**base, "uv_exact": False})
