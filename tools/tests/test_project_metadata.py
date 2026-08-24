from pathlib import Path
import subprocess


ROOT = Path(__file__).resolve().parents[2]


def _properties() -> dict[str, str]:
    values: dict[str, str] = {}
    for line in (ROOT / "gradle.properties").read_text(encoding="ascii").splitlines():
        if line and not line.startswith("#") and "=" in line:
            key, value = line.split("=", 1)
            values[key] = value
    return values


def test_public_metadata_is_consistent_and_registry_id_stays_compatible():
    values = _properties()

    assert values["mod_id"] == "usless_mobs"
    assert values["mod_name"] == "Useless Mobs Reworked"
    assert values["mod_version"] == "1.0.0-alpha.2"
    assert values["mod_license"] == "GPL-3.0-only"
    assert values["mod_authors"] == "Andrin Maag, Aleksandar Nikolic"
    assert (ROOT / "LICENSE").read_text(encoding="utf-8").startswith(
        "                    GNU GENERAL PUBLIC LICENSE"
    )


def test_dependency_versions_are_centralized():
    values = _properties()
    build = (ROOT / "build.gradle").read_text(encoding="utf-8")

    assert values["curios_version"] == "5.10.0+1.20.1"
    assert values["geckolib_version"] == "4.8.3"
    assert values["jei_version"] == "15.20.0.112"
    assert "curios-forge:${curios_version}" in build
    assert "geckolib-forge-1.20.1:${geckolib_version}" in build
    assert "jei-1.20.1-forge:${jei_version}" in build


def test_build_resources_use_utf8_and_manifest_is_time_independent():
    build = (ROOT / "build.gradle").read_text(encoding="utf-8")

    assert "filteringCharset = 'UTF-8'" in build
    assert "Implementation-Timestamp" not in build


def test_no_tracked_backup_files_remain():
    forbidden = {".bak", ".tmp", ".orig"}
    tracked = subprocess.run(
        ["git", "-c", f"safe.directory={ROOT.as_posix()}", "ls-files", "-z"],
        cwd=ROOT,
        check=True,
        capture_output=True,
    ).stdout.decode("utf-8").split("\0")
    offenders = [
        path
        for value in tracked
        if value
        and (path := Path(value)).suffix.lower() in forbidden
        and (ROOT / path).exists()
    ]
    assert offenders == []
