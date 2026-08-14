import base64
import json
import math
import os
from pathlib import Path
import tempfile
import unittest
from unittest import mock

from tools.corrupted_silverfish_v3 import build, spec
from tools.corrupted_silverfish_v3.spec import ANIMATIONS, ANIMATION_SPECS, BONES


EXPECTED = {
    "animation.corrupted_silverfish.idle": (1.6, True),
    "animation.corrupted_silverfish.walk": (0.8, True),
    "animation.corrupted_silverfish.attack": (0.45, False),
    "animation.corrupted_silverfish.hurt": (0.3, False),
    "animation.corrupted_silverfish.death": (1.1, False),
}


class AnimationContract(unittest.TestCase):
    def setUp(self):
        self.document = build.animation_document()
        self.animations = self.document["animations"]

    def channel_values(self, animation, bone, channel):
        return [
            keyframe["post"]
            for keyframe in animation["bones"][bone][channel].values()
        ]

    def transaction_fixture(self, directory):
        root = Path(directory)
        geometry = root / "geometry.json"
        animations = root / "animation.json"
        bbmodel = root / "model.bbmodel"
        source = "data:image/png;base64," + base64.b64encode(b"embedded-texture").decode("ascii")
        geometry.write_bytes(b"old-geometry")
        animations.write_bytes(b"old-animation")
        bbmodel.write_text(json.dumps(build.bbmodel_document(source)), encoding="utf-8")
        return (geometry, animations, bbmodel), tuple(path.read_bytes() for path in (geometry, animations, bbmodel))

    def test_exact_ids_lengths_and_geckolib_loop_flags(self):
        self.assertEqual("1.8.0", self.document["format_version"])
        self.assertEqual(list(EXPECTED), list(self.animations))
        for name, (length, loops) in EXPECTED.items():
            animation = self.animations[name]
            self.assertEqual(length, animation["animation_length"])
            self.assertIs(loops, animation["loop"])

    def test_legacy_animation_lengths_are_derived_from_deeply_immutable_specs(self):
        def assert_rejects_assignment(mapping, key, replacement):
            original = mapping.get(key)
            try:
                with self.assertRaises(TypeError):
                    mapping[key] = replacement
            finally:
                if isinstance(mapping, dict):
                    mapping[key] = original

        expected = {
            name.removeprefix("animation.corrupted_silverfish."): animation["animation_length"]
            for name, animation in ANIMATION_SPECS.items()
        }
        self.assertEqual(expected, dict(ANIMATIONS))
        assert_rejects_assignment(ANIMATION_SPECS, "new", {})
        idle = ANIMATION_SPECS["animation.corrupted_silverfish.idle"]
        assert_rejects_assignment(idle, "animation_length", 99)
        assert_rejects_assignment(idle["bones"], "body", {})
        position = idle["bones"]["body"]["position"]
        assert_rejects_assignment(position, "0", {})
        self.assertIsInstance(position["0"]["post"], tuple)

    def test_all_channels_are_linear_valid_and_only_use_spec_bones(self):
        bone_names = {bone.name for bone in BONES}
        for name, animation in self.animations.items():
            length = animation["animation_length"]
            self.assertTrue(set(animation["bones"]).issubset(bone_names))
            self.assertEqual(len(animation["bones"]), len(set(animation["bones"])))
            for bone, channels in animation["bones"].items():
                for channel_name, keyframes in channels.items():
                    self.assertIn(channel_name, {"rotation", "position", "scale"})
                    numeric_times = []
                    for encoded_time, keyframe in keyframes.items():
                        time = float(encoded_time)
                        numeric_times.append(time)
                        self.assertTrue(math.isfinite(time))
                        self.assertGreaterEqual(time, 0)
                        self.assertLessEqual(time, length)
                        self.assertEqual("linear", keyframe["lerp_mode"])
                        vector = keyframe["post"]
                        self.assertEqual(3, len(vector))
                        self.assertTrue(all(
                            isinstance(value, (int, float))
                            and not isinstance(value, bool)
                            and math.isfinite(value)
                            for value in vector
                        ))
                    self.assertEqual(numeric_times, sorted(numeric_times))
                    if animation["loop"]:
                        self.assertEqual(0, numeric_times[0])
                        self.assertEqual(length, numeric_times[-1])
                        self.assertEqual(
                            next(iter(keyframes.values()))["post"],
                            next(reversed(keyframes.values()))["post"],
                        )

    def test_idle_motion_contract(self):
        idle = self.animations["animation.corrupted_silverfish.idle"]
        self.assertEqual([0, 0.12, 0], [v[1] for v in self.channel_values(idle, "body", "position")])
        for bone in ("head", "thorax", "shell_front", "shell_mid", "shell_rear", "abdomen", "tail_base", "tail_tip"):
            values = self.channel_values(idle, bone, "rotation")
            self.assertEqual(1.5, max(abs(vector[1]) for vector in values))
        for number in range(1, 8):
            values = self.channel_values(idle, f"crystal_cluster_{number}", "scale")
            self.assertEqual([1, 1.025, 1], [vector[0] for vector in values])

    def test_walk_motion_contract(self):
        walk = self.animations["animation.corrupted_silverfish.walk"]
        visible = ("head", "thorax", "shell_front", "shell_mid", "shell_rear", "abdomen", "tail_base", "tail_tip")
        for bone in visible:
            rotations = walk["bones"][bone]["rotation"]
            self.assertEqual(5, len(rotations))
            self.assertEqual(next(iter(rotations.values()))["post"], next(reversed(rotations.values()))["post"])
        for side in ("left", "right"):
            for position in ("front", "mid", "rear"):
                upper = self.channel_values(walk, f"leg_{side}_{position}_upper", "rotation")
                lower = self.channel_values(walk, f"leg_{side}_{position}_lower", "rotation")
                self.assertEqual(16, max(abs(vector[0]) for vector in upper))
                self.assertEqual(10, max(abs(vector[0]) for vector in lower))

    def test_attack_hurt_and_death_motion_contracts(self):
        attack = self.animations["animation.corrupted_silverfish.attack"]
        self.assertEqual([0, -1.2, 0], [v[2] for v in self.channel_values(attack, "head", "position")])
        self.assertEqual(24, max(v[1] for v in self.channel_values(attack, "mandible_left", "rotation")))
        self.assertEqual(-24, min(v[1] for v in self.channel_values(attack, "mandible_right", "rotation")))
        for side in ("left", "right"):
            values = self.channel_values(attack, f"leg_{side}_front_upper", "rotation")
            self.assertEqual(8, max(abs(v[0]) for v in values))

        hurt = self.animations["animation.corrupted_silverfish.hurt"]
        self.assertEqual([0, 6, -2, 0], [v[2] for v in self.channel_values(hurt, "body", "rotation")])
        for bone in ("shell_front", "shell_mid", "shell_rear"):
            self.assertEqual(0.96, min(v[0] for v in self.channel_values(hurt, bone, "scale")))

        death = self.animations["animation.corrupted_silverfish.death"]
        self.assertEqual(-1.4, self.channel_values(death, "body", "position")[-1][1])
        self.assertEqual(12, abs(self.channel_values(death, "tail_tip", "rotation")[-1][0]))
        for side in ("left", "right"):
            expected = 48 if side == "left" else -48
            for position in ("front", "mid", "rear"):
                self.assertEqual(expected, self.channel_values(death, f"leg_{side}_{position}_upper", "rotation")[-1][2])
        for number in range(1, 8):
            self.assertEqual([0.82, 0.82, 0.82], self.channel_values(death, f"crystal_cluster_{number}", "scale")[-1])

    def test_hurt_crystal_world_scale_is_exact_after_parent_compensation(self):
        hurt = self.animations["animation.corrupted_silverfish.hurt"]
        parents = {bone.name: bone.parent for bone in BONES}
        expected_scaled_ancestors = (0, 1, 2, 3, 2, 3, 0)
        for number in range(1, 8):
            cluster = f"crystal_cluster_{number}"
            local = hurt["bones"][cluster]["scale"]
            scaled_ancestors = spec._scaled_ancestor_count(
                cluster,
                parents,
                frozenset(("shell_front", "shell_mid", "shell_rear")),
            )
            self.assertEqual(expected_scaled_ancestors[number - 1], scaled_ancestors)
            for time, expected_world in (("0.1", 1.04), ("0.2", 0.98)):
                world_scale = local[time]["post"][0]
                current = parents[cluster]
                visited = {cluster}
                while current is not None:
                    if current in visited:
                        self.fail(f"cycle while traversing {cluster}: {current}")
                    visited.add(current)
                    animated_scale = hurt["bones"].get(current, {}).get("scale")
                    if animated_scale is not None:
                        world_scale *= animated_scale[time]["post"][0]
                    current = parents[current]
                self.assertAlmostEqual(
                    expected_world,
                    world_scale,
                    places=14,
                    msg=f"{cluster} at {time}",
                )
        self.assertEqual(1.04 / 0.96, hurt["bones"]["crystal_cluster_2"]["scale"]["0.1"]["post"][0])
        self.assertEqual(0.98 / (1.02 ** 3), hurt["bones"]["crystal_cluster_6"]["scale"]["0.2"]["post"][0])

    def test_scaled_ancestor_counter_rejects_cycles(self):
        with self.assertRaisesRegex(ValueError, "cycle.*a"):
            spec._scaled_ancestor_count(
                "a",
                {"a": "b", "b": "a"},
                frozenset(("a", "b")),
            )

    def test_bbmodel_animations_are_derived_from_same_channels(self):
        bbmodel = build.bbmodel_document("data:image/png;base64,AA==")
        self.assertEqual(5, len(bbmodel["animations"]))
        by_name = {animation["name"]: animation for animation in bbmodel["animations"]}
        self.assertEqual(set(self.animations), set(by_name))
        group_names = {group["name"]: group["uuid"] for group in bbmodel["groups"]}
        for name, gecko in self.animations.items():
            blockbench = by_name[name]
            self.assertEqual(gecko["animation_length"], blockbench["length"])
            self.assertEqual("loop" if gecko["loop"] else "once", blockbench["loop"])
            for bone, channels in gecko["bones"].items():
                animator = blockbench["animators"][group_names[bone]]
                self.assertEqual(bone, animator["name"])
                actual = {}
                for keyframe in animator["keyframes"]:
                    actual.setdefault(keyframe["channel"], {})[f"{keyframe['time']:g}"] = {
                        "post": [float(keyframe["data_points"][0][axis]) for axis in "xyz"],
                        "lerp_mode": keyframe["interpolation"],
                    }
                self.assertEqual(channels, actual)

    def test_build_all_submits_geometry_animation_and_bbmodel_to_one_transaction(self):
        with mock.patch.object(build, "_atomic_json_write") as atomic, mock.patch.object(build, "_publish_transaction") as publish:
            build.build_all()
        atomic.assert_not_called()
        publish.assert_called_once()
        payloads = publish.call_args.args[0]
        self.assertEqual([build.GEOMETRY_PATH, build.ANIMATION_PATH, build.BBMODEL_PATH], [path for path, _contents in payloads])

    def test_full_build_write_failure_preserves_all_targets_and_cleans_temps(self):
        real_fdopen = os.fdopen
        calls = 0

        def fail_second_write(*args, **kwargs):
            nonlocal calls
            calls += 1
            if calls == 2:
                raise OSError("simulated animation write failure")
            return real_fdopen(*args, **kwargs)

        with tempfile.TemporaryDirectory() as directory:
            targets, originals = self.transaction_fixture(directory)
            with mock.patch.multiple(build, GEOMETRY_PATH=targets[0], ANIMATION_PATH=targets[1], BBMODEL_PATH=targets[2]), mock.patch("os.fdopen", side_effect=fail_second_write), self.assertRaisesRegex(OSError, "simulated animation write failure"):
                build.build_all()
            self.assertEqual(originals, tuple(path.read_bytes() for path in targets))
            self.assertEqual(set(targets), set(Path(directory).iterdir()))

    def test_full_build_last_publish_failure_restores_all_three_targets(self):
        real_replace = os.replace
        calls = 0

        def fail_bbmodel_publish(src, dst):
            nonlocal calls
            calls += 1
            if calls == 3:
                raise OSError("simulated bbmodel publish failure")
            return real_replace(src, dst)

        with tempfile.TemporaryDirectory() as directory:
            targets, originals = self.transaction_fixture(directory)
            with mock.patch.multiple(build, GEOMETRY_PATH=targets[0], ANIMATION_PATH=targets[1], BBMODEL_PATH=targets[2]), mock.patch("os.replace", side_effect=fail_bbmodel_publish), self.assertRaisesRegex(OSError, "simulated bbmodel publish failure"):
                build.build_all()
            self.assertEqual(originals, tuple(path.read_bytes() for path in targets))
            self.assertEqual(set(targets), set(Path(directory).iterdir()))

    def test_full_build_combined_failure_retains_unrestored_geometry_backup(self):
        real_replace = os.replace
        calls = 0

        def fail_publish_then_geometry_rollback(src, dst):
            nonlocal calls
            calls += 1
            if calls == 3:
                raise OSError("simulated bbmodel publish failure")
            if calls == 5:
                raise OSError("simulated geometry rollback failure")
            return real_replace(src, dst)

        with tempfile.TemporaryDirectory() as directory:
            targets, originals = self.transaction_fixture(directory)
            with mock.patch.multiple(build, GEOMETRY_PATH=targets[0], ANIMATION_PATH=targets[1], BBMODEL_PATH=targets[2]), mock.patch("os.replace", side_effect=fail_publish_then_geometry_rollback), self.assertRaisesRegex(RuntimeError, "animation transaction rollback failed") as raised:
                build.build_all()
            self.assertNotEqual(originals[0], targets[0].read_bytes())
            self.assertEqual(originals[1:], tuple(path.read_bytes() for path in targets[1:]))
            backups = [path for path in Path(directory).iterdir() if path not in targets]
            self.assertEqual(1, len(backups))
            self.assertEqual(originals[0], backups[0].read_bytes())
            self.assertIn(str(backups[0]), str(raised.exception))
            probe = backups[0].with_suffix(".handle-check")
            os.replace(backups[0], probe)
            os.replace(probe, backups[0])

    def test_pair_publish_retains_backup_when_rollback_itself_fails(self):
        real_replace = os.replace
        calls = 0

        def fail_publish_then_rollback(src, dst):
            nonlocal calls
            calls += 1
            if calls == 2:
                raise OSError("simulated bbmodel publish failure")
            if calls == 3:
                raise OSError("simulated animation rollback failure")
            return real_replace(src, dst)

        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            animation = root / "animations" / "animation.json"
            bbmodel = root / "model" / "model.bbmodel"
            animation.parent.mkdir()
            bbmodel.parent.mkdir()
            animation.write_bytes(b"old-animation")
            bbmodel.write_bytes(b"old-bbmodel")
            with mock.patch("os.replace", side_effect=fail_publish_then_rollback), self.assertRaisesRegex(RuntimeError, "animation transaction rollback failed") as raised:
                build._publish_transaction(((animation, b"new-animation"), (bbmodel, b"new-bbmodel")))
            backups = [path for path in animation.parent.iterdir() if path != animation]
            self.assertEqual(1, len(backups))
            self.assertEqual(b"old-animation", backups[0].read_bytes())
            self.assertIn(str(backups[0]), str(raised.exception))

    def test_successful_publish_reports_all_cleanup_failures_without_rollback(self):
        real_unlink = Path.unlink
        cleanup_attempts = []

        def fail_backup_cleanup(path, *args, **kwargs):
            if ".backup." in path.name:
                cleanup_attempts.append(path)
                raise OSError(f"simulated cleanup failure: {path.name}")
            return real_unlink(path, *args, **kwargs)

        with tempfile.TemporaryDirectory() as directory:
            first = Path(directory) / "first.json"
            second = Path(directory) / "second.json"
            first.write_bytes(b"old-first")
            second.write_bytes(b"old-second")
            with mock.patch.object(Path, "unlink", autospec=True, side_effect=fail_backup_cleanup), self.assertRaisesRegex(RuntimeError, "published, cleanup incomplete") as raised:
                build._publish_transaction(((first, b"new-first"), (second, b"new-second")))
            self.assertEqual((b"new-first", b"new-second"), (first.read_bytes(), second.read_bytes()))
            self.assertEqual(2, len(cleanup_attempts))
            for failed_path in cleanup_attempts:
                self.assertIn(str(failed_path), str(raised.exception))
                probe = failed_path.with_suffix(".handle-check")
                os.replace(failed_path, probe)
                os.replace(probe, failed_path)

    def test_publish_failure_keeps_primary_error_and_collects_multiple_cleanup_failures(self):
        real_replace = os.replace
        real_unlink = Path.unlink
        replace_calls = 0
        cleanup_attempts = []

        def fail_last_publish(src, dst):
            nonlocal replace_calls
            replace_calls += 1
            if replace_calls == 3:
                raise OSError("primary bbmodel publish failure")
            return real_replace(src, dst)

        def fail_model_cleanup(path, *args, **kwargs):
            if path.name.startswith(".model.bbmodel."):
                cleanup_attempts.append(path)
                raise OSError(f"secondary cleanup failure: {path.name}")
            return real_unlink(path, *args, **kwargs)

        with tempfile.TemporaryDirectory() as directory:
            targets, originals = self.transaction_fixture(directory)
            with mock.patch("os.replace", side_effect=fail_last_publish), mock.patch.object(Path, "unlink", autospec=True, side_effect=fail_model_cleanup), self.assertRaisesRegex(OSError, "primary bbmodel publish failure") as raised:
                build._publish_transaction(tuple((target, f"new-{index}".encode()) for index, target in enumerate(targets)))
            self.assertEqual(originals, tuple(path.read_bytes() for path in targets))
            self.assertEqual(2, len(cleanup_attempts))
            self.assertIn("cleanup incomplete", str(raised.exception))
            for failed_path in cleanup_attempts:
                self.assertIn(str(failed_path), str(raised.exception))
                probe = failed_path.with_suffix(".handle-check")
                os.replace(failed_path, probe)
                os.replace(probe, failed_path)

    def test_rollback_failure_chain_survives_multiple_cleanup_failures(self):
        real_replace = os.replace
        real_unlink = Path.unlink
        replace_calls = 0
        cleanup_attempts = []

        def fail_publish_then_rollback(src, dst):
            nonlocal replace_calls
            replace_calls += 1
            if replace_calls == 3:
                raise OSError("primary bbmodel publish failure")
            if replace_calls == 5:
                raise OSError("geometry rollback failure")
            return real_replace(src, dst)

        def fail_model_cleanup(path, *args, **kwargs):
            if path.name.startswith(".model.bbmodel."):
                cleanup_attempts.append(path)
                raise OSError(f"secondary cleanup failure: {path.name}")
            return real_unlink(path, *args, **kwargs)

        with tempfile.TemporaryDirectory() as directory:
            targets, originals = self.transaction_fixture(directory)
            with mock.patch("os.replace", side_effect=fail_publish_then_rollback), mock.patch.object(Path, "unlink", autospec=True, side_effect=fail_model_cleanup), self.assertRaisesRegex(RuntimeError, "rollback failed") as raised:
                build._publish_transaction(tuple((target, f"new-{index}".encode()) for index, target in enumerate(targets)))
            self.assertNotEqual(originals[0], targets[0].read_bytes())
            self.assertEqual(originals[1:], tuple(path.read_bytes() for path in targets[1:]))
            self.assertEqual(2, len(cleanup_attempts))
            self.assertIn("cleanup incomplete", str(raised.exception))
            retained = [path for path in Path(directory).iterdir() if ".geometry.json.backup." in path.name]
            self.assertEqual(1, len(retained))
            self.assertEqual(originals[0], retained[0].read_bytes())
            for failed_path in (*cleanup_attempts, retained[0]):
                probe = failed_path.with_suffix(".handle-check")
                os.replace(failed_path, probe)
                os.replace(probe, failed_path)

    def test_committed_animation_bytes_are_exact_and_repeatable(self):
        expected = (json.dumps(build.animation_document(), ensure_ascii=False, indent=2) + "\n").encode("utf-8")
        self.assertEqual(expected, build.ANIMATION_PATH.read_bytes())
        self.assertEqual(build.animation_document(), build.animation_document())


if __name__ == "__main__":
    unittest.main()
