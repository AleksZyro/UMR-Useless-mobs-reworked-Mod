from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "src/main/java/com/Momik/usless_mobs/Usless_mobs.java"
CLIENT_EVENTS = ROOT / "src/main/java/com/Momik/usless_mobs/client/ClientModEvents.java"


def test_common_entrypoint_does_not_link_client_only_classes():
    source = MAIN.read_text(encoding="utf-8")

    assert "net.minecraft.client" not in source
    assert "net.minecraftforge.client" not in source
    assert "com.Momik.usless_mobs.client" not in source
    assert "net.mysith.client" not in source
    assert "Dist.CLIENT" not in source


def test_client_registration_is_isolated_and_dist_guarded():
    source = CLIENT_EVENTS.read_text(encoding="utf-8")

    assert "value = Dist.CLIENT" in source
    assert "RegisterRenderers" in source
    assert "RegisterLayerDefinitions" in source
    assert "RegisterKeyMappingsEvent" in source
    assert "FMLClientSetupEvent" in source
