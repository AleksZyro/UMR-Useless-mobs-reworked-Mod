"""Generate deterministic original OGG layers for the UMR Octopus."""

from __future__ import annotations

from pathlib import Path

import numpy as np
import soundfile as sf


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "src/main/resources/assets/usless_mobs/sounds/entity/octopus"
RATE = 44_100
RNG = np.random.default_rng(0x0C70_505)


def envelope(length: int, attack: float = 0.03, release: float = 0.20) -> np.ndarray:
    attack_samples = max(1, int(length * attack))
    release_samples = max(1, int(length * release))
    result = np.ones(length, dtype=np.float64)
    result[:attack_samples] = np.linspace(0.0, 1.0, attack_samples)
    result[-release_samples:] = np.linspace(1.0, 0.0, release_samples)
    return result


def low_noise(length: int, window: int = 64) -> np.ndarray:
    raw = RNG.normal(0.0, 1.0, length + window)
    kernel = np.ones(window, dtype=np.float64) / window
    return np.convolve(raw, kernel, mode="valid")[:length]


def save(name: str, samples: np.ndarray) -> None:
    peak = float(np.max(np.abs(samples)))
    if peak <= 0.0:
        raise ValueError(f"silent sound: {name}")
    OUTPUT.mkdir(parents=True, exist_ok=True)
    sf.write(OUTPUT / f"{name}.ogg", samples / peak * 0.82, RATE, format="OGG", subtype="VORBIS")


def ambient() -> np.ndarray:
    length = int(RATE * 0.92)
    t = np.arange(length) / RATE
    mantle = np.sin(2.0 * np.pi * (72.0 + 9.0 * np.sin(t * 5.0)) * t) * 0.30
    wet = low_noise(length, 110) * 1.8
    clicks = np.zeros(length)
    for position in (0.18, 0.43, 0.71):
        start = int(position * RATE)
        width = int(0.035 * RATE)
        local = np.arange(width) / RATE
        clicks[start:start + width] += np.sin(2 * np.pi * 310 * local) * np.exp(-local * 75) * 0.45
    return (mantle + wet + clicks) * envelope(length, 0.08, 0.25)


def ink() -> np.ndarray:
    length = int(RATE * 0.46)
    t = np.arange(length) / RATE
    burst = RNG.normal(0.0, 1.0, length) * np.exp(-t * 8.0)
    jet = np.sin(2 * np.pi * (120.0 - 70.0 * t) * t) * np.exp(-t * 5.5)
    return (burst * 0.55 + jet * 0.65) * envelope(length, 0.01, 0.38)


def grab() -> np.ndarray:
    length = int(RATE * 0.68)
    t = np.arange(length) / RATE
    suction = np.sin(2 * np.pi * (58.0 + 115.0 * t) * t) * (0.35 + 0.65 * t)
    cups = np.sin(2 * np.pi * 240.0 * t) * np.maximum(0.0, np.sin(2 * np.pi * 8.0 * t)) ** 12
    return (suction * 0.65 + cups * 0.35 + low_noise(length, 48)) * envelope(length, 0.04, 0.28)


def camouflage() -> np.ndarray:
    length = int(RATE * 0.78)
    t = np.arange(length) / RATE
    shimmer = np.sin(2 * np.pi * (180.0 + 90.0 * t) * t) * 0.32
    undertone = np.sin(2 * np.pi * 54.0 * t) * 0.40
    return (shimmer + undertone + low_noise(length, 150) * 2.1) * envelope(length, 0.12, 0.36)


def squeeze() -> np.ndarray:
    length = int(RATE * 0.38)
    t = np.arange(length) / RATE
    pop = np.sin(2 * np.pi * (165.0 - 95.0 * t) * t) * np.exp(-t * 10.0)
    friction = low_noise(length, 32) * np.exp(-t * 4.5) * 2.5
    return (pop + friction) * envelope(length, 0.02, 0.45)


def main() -> None:
    for name, generator in {
        "ambient_1": ambient,
        "ink_1": ink,
        "grab_1": grab,
        "camouflage_1": camouflage,
        "squeeze_1": squeeze,
    }.items():
        save(name, generator())
        print(f"OCTOPUS_SOUND_PASS name={name}")


if __name__ == "__main__":
    main()
