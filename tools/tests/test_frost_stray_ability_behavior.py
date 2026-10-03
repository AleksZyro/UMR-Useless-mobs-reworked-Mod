from tools.frost_stray_timeline_model import TimelineClientModel, TimelinePacket


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
