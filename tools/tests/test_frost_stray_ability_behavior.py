from tools.frost_stray_timeline_model import (
    TimelineClientModel,
    TimelinePacket,
    VolleyServerModel,
)


def packet(kind: str, instance: str, start: int, server_time: int) -> TimelinePacket:
    return TimelinePacket(kind, instance, start, server_time)


def test_duplicate_start_and_release_are_idempotent():
    client = TimelineClientModel()
    start = packet("START", "a", 100, 100)

    client.receive(start, local_time=100)
    client.receive(start, local_time=100)
    client.receive(packet("RELEASE", "a", 100, 118), local_time=118)
    client.receive(packet("RELEASE", "a", 100, 118), local_time=118)

    assert client.start_sounds == 1
    assert client.release_cues == 1
    assert client.active is None


def test_late_tracking_enters_progress_without_replaying_start_sound():
    client = TimelineClientModel()

    client.receive(packet("START", "a", 100, 112), local_time=112)

    assert client.active is not None
    assert client.active[1] == 12
    assert client.start_sounds == 0


def test_cancelled_instance_cannot_be_reactivated_by_delayed_start():
    client = TimelineClientModel()
    start = packet("START", "a", 100, 100)

    client.receive(start, local_time=100)
    client.receive(packet("CANCEL", "a", 100, 105), local_time=105)
    client.receive(start, local_time=100)

    assert client.active is None
    assert client.start_sounds == 1


def test_duplicate_impact_is_emitted_once():
    client = TimelineClientModel()

    client.receive_impact("impact-1")
    client.receive_impact("impact-1")
    client.receive_impact("impact-2")

    assert client.impacts == 2


def test_two_clients_receive_one_release_and_one_impact_each():
    clients = [TimelineClientModel(), TimelineClientModel()]
    start = packet("START", "a", 100, 100)
    release = packet("RELEASE", "a", 100, 118)

    for client in clients:
        client.receive(start, local_time=100)
        client.receive(release, local_time=118)
        client.receive(release, local_time=118)
        client.receive_impact("impact-1")
        client.receive_impact("impact-1")

    assert [(client.start_sounds, client.release_cues, client.impacts) for client in clients] == [
        (1, 1, 1),
        (1, 1, 1),
    ]


def test_delayed_release_for_old_instance_does_not_end_new_instance():
    client = TimelineClientModel()

    client.receive(packet("START", "old", 100, 100), local_time=100)
    client.receive(packet("RELEASE", "old", 100, 118), local_time=118)
    client.receive(packet("START", "new", 130, 130), local_time=130)
    client.receive(packet("RELEASE", "old", 100, 140), local_time=140)

    assert client.active is not None
    assert client.active[0].instance == "new"


def test_server_normal_attack_hit_and_miss_are_distinct_from_volley_release():
    server = VolleyServerModel()

    assert server.normal_attack(target_alive=True, in_range=True)
    assert not server.normal_attack(target_alive=True, in_range=False)
    assert (server.normal_attack_attempts, server.normal_attack_hits, server.normal_attack_misses) == (2, 1, 1)

    assert server.start("a")
    server.tick(0)
    server.tick(18)
    server.tick(19)

    assert (server.releases, server.projectiles) == (1, 3)


def test_server_hit_damage_and_impact_are_not_double_triggered():
    server = VolleyServerModel()
    assert server.start("a")
    server.tick(18)

    server.projectile_hit("arrow-1", target_hit=True)
    server.projectile_hit("arrow-1", target_hit=True)
    server.projectile_missed("arrow-2")
    server.projectile_missed("arrow-2")

    assert server.damage_events == 1
    assert server.impact_events == 1


def test_server_interrupt_and_death_prevent_projectile_release():
    interrupted = VolleyServerModel()
    assert interrupted.start("interrupted")
    interrupted.cancel()
    interrupted.tick(18)

    dead = VolleyServerModel()
    assert dead.start("dead")
    dead.die()
    dead.tick(18)

    assert (interrupted.releases, interrupted.projectiles) == (0, 0)
    assert (dead.state, dead.releases, dead.projectiles) == ("dead", 0, 0)
