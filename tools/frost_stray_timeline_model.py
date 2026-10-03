"""Small executable models of the Frost Stray timeline contract.

This deliberately does not import Minecraft or Forge.  It is a deterministic
protocol model used to exercise ordering, deduplication and late-observer
semantics while the real implementation is verified by the Forge CI build.
"""

from dataclasses import dataclass, field


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


@dataclass
class VolleyServerModel:
    """Model the server-side one-shot contract used by one Frost Stray volley."""

    duration: int = 18
    projectile_count: int = 3
    state: str = "idle"
    instance: str | None = None
    starts: int = 0
    releases: int = 0
    cancellations: int = 0
    projectiles: int = 0
    damage_events: int = 0
    impact_events: int = 0
    normal_attack_attempts: int = 0
    normal_attack_hits: int = 0
    normal_attack_misses: int = 0
    handled_projectiles: set[str] = field(default_factory=set)

    def normal_attack(self, *, target_alive: bool, in_range: bool) -> bool:
        self.normal_attack_attempts += 1
        if target_alive and in_range:
            self.normal_attack_hits += 1
            return True
        self.normal_attack_misses += 1
        return False

    def start(self, instance: str) -> bool:
        if self.state != "idle":
            return False
        self.state = "charging"
        self.instance = instance
        self.starts += 1
        return True

    def tick(self, elapsed: int, *, target_alive: bool = True, in_range: bool = True) -> None:
        if self.state != "charging":
            return
        if not target_alive or not in_range:
            self.cancel()
            return
        if elapsed >= self.duration:
            self.state = "released"
            self.releases += 1
            self.projectiles += self.projectile_count

    def cancel(self) -> None:
        if self.state == "charging":
            self.state = "idle"
            self.cancellations += 1
            self.instance = None

    def die(self) -> None:
        if self.state == "charging":
            self.cancel()
        self.state = "dead"

    def projectile_hit(self, projectile_id: str, *, target_hit: bool) -> None:
        """Resolve one server projectile collision at most once."""
        if projectile_id in self.handled_projectiles:
            return
        self.handled_projectiles.add(projectile_id)
        self.impact_events += 1
        if target_hit:
            self.damage_events += 1

    def projectile_missed(self, projectile_id: str) -> None:
        """Resolve a projectile that expires without a collision."""
        self.handled_projectiles.add(projectile_id)
