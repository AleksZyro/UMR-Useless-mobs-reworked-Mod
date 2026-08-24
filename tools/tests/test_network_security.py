from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


def test_slime_toggle_packet_is_server_bound():
    source = (
        ROOT / "src/main/java/com/Momik/usless_mobs/network/ModNetwork.java"
    ).read_text(encoding="utf-8")

    assert "NetworkDirection.PLAY_TO_SERVER" in source
    assert "context.getSender()" in (
        ROOT / "src/main/java/com/Momik/usless_mobs/network/ToggleSlimeEffectsPacket.java"
    ).read_text(encoding="utf-8")


def test_death_mark_packet_is_client_bound_and_clamped():
    networking = (
        ROOT / "src/main/mobs/endermite/java/net/mysith/network/ModNetworking.java"
    ).read_text(encoding="utf-8")
    client = (
        ROOT / "src/main/mobs/endermite/java/net/mysith/client/ClientDeathMarkSync.java"
    ).read_text(encoding="utf-8")

    assert "NetworkDirection.PLAY_TO_CLIENT" in networking
    assert "entityId <= 0 || durationTicks <= 0" in client
    assert "Math.min(durationTicks, 20 * 60)" in client
