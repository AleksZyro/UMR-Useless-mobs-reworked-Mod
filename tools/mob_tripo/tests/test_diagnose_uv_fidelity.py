import importlib


EXPECTED_EXACT_MOBS = {
    "axolotl",
    "coral_drowned",
    "frost_stray",
    "glow_squid",
    "helping_allay",
    "living_bat",
    "living_boss",
    "ocelot",
    "octopus",
    "polar_bear",
    "rooted_husk",
    "squid",
    "web_cave_spider",
    "witch_boss",
}


def test_uv_fidelity_audit_covers_every_exact_runtime_mob() -> None:
    module = importlib.import_module("tools.mob_tripo.diagnose_uv_fidelity")

    assert hasattr(module, "SOURCES"), "audit must expose its authoritative source map"
    assert set(module.SOURCES) == EXPECTED_EXACT_MOBS
    assert all(path.is_file() for path in module.SOURCES.values())
