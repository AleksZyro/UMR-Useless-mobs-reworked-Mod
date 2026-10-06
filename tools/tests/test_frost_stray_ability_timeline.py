import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java/com/Momik/usless_mobs"


def _source(relative: str) -> str:
    return (JAVA / relative).read_text(encoding="utf-8")


def test_frost_stray_runtime_facts_match_the_report_and_versioned_sources():
    report = json.loads((
        ROOT / "src/main/resources/assets/usless_mobs/meshes/entity/custom3d/frost_stray.report.json"
    ).read_text(encoding="utf-8"))

    assert report["output_triangles"] == 98_103
    assert (report["source_texture_width"], report["source_texture_height"]) == (4096, 4096)
    assert (report["runtime_texture_width"], report["runtime_texture_height"]) == (2048, 2048)
    for asset in (
        "source/frost_stray_textured_4k_v3_candidate.glb",
        "blockbench/frost_stray Tripo Rig.bbmodel",
    ):
        assert (ROOT / "Modelle/Exports/frost_stray_v1" / asset).is_file()


def test_server_authoritative_timeline_has_identity_start_abort_and_late_tracking_sync():
    entity = _source("entity/FrostStrayEntity.java")
    timeline = _source("ability/FrostStrayAbilityTimeline.java")
    sync = _source("event/FrostStrayAbilitySyncHandler.java")

    assert "UUID.randomUUID()" in entity
    assert "FrostStrayAbilityTimeline" in entity
    assert "ICE_VOLLEY_START_TIME" in entity
    assert "ICE_VOLLEY_INSTANCE_ID" in entity
    assert "FrostStrayAbilityPacket.Type.START" in entity
    assert "FrostStrayAbilityPacket.Type.RELEASE" in entity
    assert "FrostStrayAbilityPacket.Type.CANCEL" in entity
    assert "public boolean hurt" in entity and "cancelIceVolley(serverLevel)" in entity
    assert "public void die" in entity
    assert "PlayerEvent.StartTracking" in sync
    assert "syncActiveVolleyTo(player)" in sync
    assert "elapsedAt" in timeline and "isActiveAt" in timeline


def test_client_cues_are_tick_driven_idempotent_and_do_not_replay_stale_sounds():
    client = _source("client/FrostStrayAbilityClient.java")

    assert "TickEvent.ClientTickEvent" in client
    assert "FINISHED.containsKey(packet.instanceId())" in client
    assert "elapsed <= 3L" in client
    assert "ACTIVE.remove(packet.entityId())" in client
    assert "CLOCK.elapsed(active.startGameTime, level.getGameTime())" in client
    assert "CLOCK.observe(packet.serverGameTime(), level.getGameTime())" in client
    clock = _source("ability/FrostStrayTimelineClock.java")
    assert "highestServerGameTime" in clock
    assert "packetServerGameTime > this.highestServerGameTime" in clock
    assert "EntityRenderer" not in client


def test_collision_position_and_vfx_profiles_are_declarative_and_distance_budgeted():
    projectile = _source("entity/FrostStrayVolleyArrow.java")
    profiles = _source("client/FrostStrayVfxProfiles.java")
    profile_json = json.loads((
        ROOT / "src/main/resources/assets/usless_mobs/vfx_profiles/frost_stray_volley.json"
    ).read_text(encoding="utf-8"))

    assert "result.getLocation()" in projectile
    assert "!this.level().isClientSide" in projectile
    assert "12.0D * 12.0D" in profiles
    assert "24.0D * 24.0D" in profiles
    assert "32.0D * 32.0D" in profiles
    assert set(profile_json) == {"charge", "release", "impact"}
    assert profile_json["charge"]["important"] is True


def test_frost_stray_v2_is_selectable_and_has_reproducible_twelve_socket_preview():
    registry = _source("registry/ModEntities.java")
    client_events = _source("client/ClientModEvents.java")
    v2_renderer = _source("client/FrostStrayV2Renderer.java")
    rig = json.loads((ROOT / "Modelle/Exports/frost_stray_v2/frost_stray_v2_rig.json").read_text(encoding="utf-8"))
    manifest = json.loads((ROOT / "Modelle/Exports/frost_stray_v2/frost_stray_v2.manifest.json").read_text(encoding="utf-8"))

    assert 'register("frost_stray_v2"' in registry
    assert "ModEntities.FROST_STRAY_V2.get()" in client_events
    assert "FrostStrayV2OverlayLayer" in v2_renderer
    assert len(rig["bones"]) == 12
    assert manifest["runtime_resolution"] == [64, 64]
    assert len(manifest["overlay_bones"]) == 12
    assert (ROOT / manifest["runtime_texture"]).is_file()


def test_ci_server_smoke_requires_a_real_done_marker_before_accepting_timeout():
    workflow = (ROOT / ".github/workflows/build.yml").read_text(encoding="utf-8")

    assert 'grep -Fq "Done (" dedicated-server.log' in workflow
    assert 'exit 1' in workflow
    assert '[[ "$status" -ne 0 && "$status" -ne 124 ]]' in workflow
