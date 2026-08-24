from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java/com/Momik/usless_mobs"
MAIN = JAVA / "Usless_mobs.java"
CREATIVE_EVENTS = JAVA / "event/CreativeTabEvents.java"


def test_entrypoint_does_not_own_creative_tab_contents():
    source = MAIN.read_text(encoding="utf-8")

    assert "BuildCreativeModeTabContentsEvent" not in source
    assert "CreativeModeTabs" not in source
    assert "addCreative" not in source


def test_creative_tab_event_owns_existing_tab_contracts():
    source = CREATIVE_EVENTS.read_text(encoding="utf-8")

    assert "bus = Mod.EventBusSubscriber.Bus.MOD" in source
    for tab in (
        "INGREDIENTS",
        "FUNCTIONAL_BLOCKS",
        "TOOLS_AND_UTILITIES",
        "COMBAT",
        "SPAWN_EGGS",
    ):
        assert f"CreativeModeTabs.{tab}" in source
    assert "ModItems.CORRUPTED_SILVERFISH_SPAWN_EGG" in source
    assert "ModItems.KING_SLIME_TROPHY" in source
