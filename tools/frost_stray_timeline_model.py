"""Small executable model of the Frost Stray client timeline contract.

This deliberately does not import Minecraft or Forge.  It is a deterministic
protocol model used to exercise ordering, deduplication and late-observer
semantics while the real implementation is verified by the Forge CI build.
"""

from dataclasses import dataclass


@dataclass(frozen=True)
class TimelinePacket:
    kind: str
    instance: str
    start: int
    server_time: int
    duration: int = 18


class TimelineClientModel:
    """Model the externally visible state transitions of the client cue player."""

    def __init__(self) -> None:
        self.active: tuple[TimelinePacket, int] | None = None
        self.finished: set[str] = set()
        self.start_sounds = 0
        self.release_cues = 0
        self.impact_ids: set[str] = set()
        self.impacts = 0

    def receive(self, packet: TimelinePacket, local_time: int) -> None:
        if packet.kind == "START":
            self._start(packet, local_time)
        elif packet.kind == "RELEASE":
            self._release(packet)
        elif packet.kind == "CANCEL":
            self._cancel(packet)
        else:
            raise ValueError(f"unknown packet kind: {packet.kind}")

    def receive_impact(self, impact_id: str) -> None:
        if impact_id not in self.impact_ids:
            self.impact_ids.add(impact_id)
            self.impacts += 1

    def _start(self, packet: TimelinePacket, local_time: int) -> None:
        if packet.instance in self.finished:
            return
        elapsed = max(0, local_time - packet.start)
        if elapsed >= packet.duration:
            self.finished.add(packet.instance)
            return
        if self.active is not None:
            current, _ = self.active
            if current.instance == packet.instance or current.start > packet.start:
                return
            self.finished.add(current.instance)
        self.active = (packet, elapsed)
        if elapsed <= 3:
            self.start_sounds += 1

    def _release(self, packet: TimelinePacket) -> None:
        if self.active is not None and self.active[0].instance == packet.instance:
            self.release_cues += 1
            self.active = None
        self.finished.add(packet.instance)

    def _cancel(self, packet: TimelinePacket) -> None:
        if self.active is not None and self.active[0].instance == packet.instance:
            self.active = None
        self.finished.add(packet.instance)
