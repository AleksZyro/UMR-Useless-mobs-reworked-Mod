import copy
import json
from pathlib import Path
import tempfile
import unittest

from tools.corrupted_silverfish_v5.animate_rig import (
    ANIMATION_SPECS,
    add_animations,
    animation_bytes,
    write_animated_model,
)
from tools.corrupted_silverfish_v5.rig_mesh import (
    build_rig_document,
    canonical_faces,
    texture_signature,
)
from tools.corrupted_silverfish_v5.tests.test_rig_mesh import make_two_triangle_fixture


class AnimatedRigContractTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.rig, _ = build_rig_document(make_two_triangle_fixture())
        cls.animated = add_animations(cls.rig)

    def test_contains_exactly_five_named_animations(self):
        by_name = {animation["name"]: animation for animation in self.animated["animations"]}

        self.assertEqual(set(by_name), set(ANIMATION_SPECS))
        self.assertEqual(
            {name: (animation["length"], animation["loop"]) for name, animation in by_name.items()},
            {
                "animation.corrupted_silverfish.idle": (1.6, "loop"),
                "animation.corrupted_silverfish.walk": (0.8, "loop"),
                "animation.corrupted_silverfish.attack": (0.45, "once"),
                "animation.corrupted_silverfish.hurt": (0.3, "once"),
                "animation.corrupted_silverfish.death": (1.0, "once"),
            },
        )

    def test_geometry_texture_and_hierarchy_are_unchanged(self):
        self.assertEqual(canonical_faces(self.animated), canonical_faces(self.rig))
        self.assertEqual(texture_signature(self.animated), texture_signature(self.rig))
        self.assertEqual(self.animated["groups"], self.rig["groups"])
        self.assertEqual(self.animated["outliner"], self.rig["outliner"])

    def test_source_rig_is_not_mutated(self):
        source = copy.deepcopy(self.rig)

        add_animations(source)

        self.assertEqual(source, self.rig)

    def test_every_animator_targets_an_existing_group(self):
        group_uuids = {group["uuid"] for group in self.animated["groups"]}

        for animation in self.animated["animations"]:
            self.assertTrue(animation["animators"])
            self.assertLessEqual(set(animation["animators"]), group_uuids)

    def test_keyframes_are_finite_ordered_and_end_at_animation_length(self):
        for animation in self.animated["animations"]:
            for animator in animation["animators"].values():
                by_channel = {}
                for keyframe in animator["keyframes"]:
                    by_channel.setdefault(keyframe["channel"], []).append(keyframe)
                    self.assertEqual(keyframe["interpolation"], "linear")
                    self.assertEqual(len(keyframe["data_points"]), 1)
                    for value in keyframe["data_points"][0].values():
                        self.assertTrue(float(value) == float(value))
                for keyframes in by_channel.values():
                    times = [keyframe["time"] for keyframe in keyframes]
                    self.assertEqual(times, sorted(times))
                    self.assertEqual(times[0], 0)
                    self.assertEqual(times[-1], animation["length"])

    def test_loop_animations_return_to_their_start_pose(self):
        for animation in self.animated["animations"]:
            if animation["loop"] != "loop":
                continue
            for animator in animation["animators"].values():
                by_channel = {}
                for keyframe in animator["keyframes"]:
                    by_channel.setdefault(keyframe["channel"], []).append(keyframe)
                for keyframes in by_channel.values():
                    self.assertEqual(keyframes[0]["data_points"], keyframes[-1]["data_points"])

    def test_walk_has_strong_natural_alternating_tripod_motion(self):
        walk = ANIMATION_SPECS["animation.corrupted_silverfish.walk"]["bones"]
        first_tripod = ("leg_front_left", "leg_middle_right", "leg_rear_left")
        opposite_tripod = ("leg_front_right", "leg_middle_left", "leg_rear_right")
        leg_bones = first_tripod + opposite_tripod
        expected_times = tuple(index * 0.025 for index in range(33))

        self.assertEqual(set(walk), {"body", *leg_bones})
        self.assertEqual(set(walk["body"]), {"position", "rotation"})

        reference = walk[first_tripod[0]]["rotation"]
        for bone in leg_bones:
            self.assertEqual(set(walk[bone]), {"rotation"})
            rotations = walk[bone]["rotation"]
            self.assertEqual(tuple(time for time, _ in rotations), expected_times)
            self.assertEqual(len(rotations), 33)
            self.assertEqual(max(abs(vector[1]) for _, vector in rotations), 30.0)
            self.assertEqual(max(abs(vector[2]) for _, vector in rotations), 16.0)
            self.assertTrue(all(vector[0] == 0 for _, vector in rotations))
            adjacent_stride_steps = [
                abs(second[1] - first[1])
                for (_, first), (_, second) in zip(rotations, rotations[1:])
            ]
            adjacent_lift_steps = [
                abs(second[2] - first[2])
                for (_, first), (_, second) in zip(rotations, rotations[1:])
            ]
            self.assertLessEqual(max(adjacent_stride_steps), 5.86)
            self.assertLessEqual(max(adjacent_lift_steps), 3.13)
            lift_values = [vector[2] for _, vector in rotations]
            if bone.endswith("left"):
                self.assertTrue(all(value <= 0 for value in lift_values))
            else:
                self.assertTrue(all(value >= 0 for value in lift_values))

        for bone in first_tripod:
            for (_, first), (_, candidate) in zip(reference, walk[bone]["rotation"]):
                self.assertEqual(first[1], candidate[1])
                self.assertEqual(abs(first[2]), abs(candidate[2]))
        # At each quarter-cycle one tripod clears the ground while the other stays planted.
        for index in (8, 24):
            lifted_first = any(abs(walk[bone]["rotation"][index][1][2]) == 16 for bone in first_tripod)
            lifted_second = any(abs(walk[bone]["rotation"][index][1][2]) == 16 for bone in opposite_tripod)
            self.assertNotEqual(lifted_first, lifted_second)

        body_positions = walk["body"]["position"]
        self.assertEqual(max(vector[1] for _, vector in body_positions), 0.24)

        for bone in opposite_tripod:
            opposite = walk[bone]["rotation"]
            for (first_time, first), (opposite_time, second) in zip(reference, opposite):
                self.assertEqual(opposite_time, first_time)
                self.assertEqual(first[1], -second[1])

    def test_non_walk_specs_remain_at_the_independent_literal_contract(self):
        expected = {
            "animation.corrupted_silverfish.idle": {
                "loop": True,
                "length": 1.6,
                "bones": {
                    "body": {
                        "position": ((0.0, [0.0, 0.0, 0.0]), (0.8, [0.0, 0.08, 0.0]), (1.6, [0.0, 0.0, 0.0])),
                        "rotation": ((0.0, [0.0, 0.0, 0.0]), (0.8, [0.0, 0.4, 0.35]), (1.6, [0.0, 0.0, 0.0])),
                    }
                },
            },
            "animation.corrupted_silverfish.attack": {
                "loop": False,
                "length": 0.45,
                "bones": {
                    "body": {"position": ((0.0, [0.0, 0.0, 0.0]), (0.225, [0.0, 0.0, 0.55]), (0.45, [0.0, 0.0, 0.0]))},
                    "leg_front_left": {"rotation": ((0.0, [0.0, 0.0, 0.0]), (0.225, [0.0, 12.0, -8.0]), (0.45, [0.0, 0.0, 0.0]))},
                    "leg_front_right": {"rotation": ((0.0, [0.0, 0.0, 0.0]), (0.225, [0.0, -12.0, 8.0]), (0.45, [0.0, 0.0, 0.0]))},
                },
            },
            "animation.corrupted_silverfish.hurt": {
                "loop": False,
                "length": 0.3,
                "bones": {
                    "body": {"rotation": ((0.0, [0.0, 0.0, 0.0]), (0.1, [0.0, 0.0, 7.0]), (0.2, [0.0, 0.0, -3.0]), (0.3, [0.0, 0.0, 0.0]))}
                },
            },
            "animation.corrupted_silverfish.death": {
                "loop": False,
                "length": 1.0,
                "bones": {
                    "body": {
                        "position": ((0.0, [0.0, 0.0, 0.0]), (0.55, [0.0, -0.6, 0.0]), (1.0, [0.0, -1.5, 0.0])),
                        "rotation": ((0.0, [0.0, 0.0, 0.0]), (0.55, [0.0, 0.0, 38.0]), (1.0, [0.0, 0.0, 82.0])),
                    },
                    "leg_front_left": {"rotation": ((0.0, [0.0, 0.0, 0.0]), (0.55, [0.0, 0.0, 24.0]), (1.0, [0.0, 0.0, 48.0]))},
                    "leg_middle_left": {"rotation": ((0.0, [0.0, 0.0, 0.0]), (0.55, [0.0, 0.0, 24.0]), (1.0, [0.0, 0.0, 48.0]))},
                    "leg_rear_left": {"rotation": ((0.0, [0.0, 0.0, 0.0]), (0.55, [0.0, 0.0, 24.0]), (1.0, [0.0, 0.0, 48.0]))},
                    "leg_front_right": {"rotation": ((0.0, [0.0, 0.0, 0.0]), (0.55, [0.0, 0.0, -24.0]), (1.0, [0.0, 0.0, -48.0]))},
                    "leg_middle_right": {"rotation": ((0.0, [0.0, 0.0, 0.0]), (0.55, [0.0, 0.0, -24.0]), (1.0, [0.0, 0.0, -48.0]))},
                    "leg_rear_right": {"rotation": ((0.0, [0.0, 0.0, 0.0]), (0.55, [0.0, 0.0, -24.0]), (1.0, [0.0, 0.0, -48.0]))},
                },
            },
        }

        self.assertEqual(
            {name: ANIMATION_SPECS[name] for name in expected},
            expected,
        )

    def test_serialized_bytes_are_deterministic(self):
        first = animation_bytes(add_animations(self.rig))
        second = animation_bytes(add_animations(copy.deepcopy(self.rig)))

        self.assertEqual(first, second)
        self.assertTrue(first.endswith(b"\n"))


class AnimatedRigWriterTests(unittest.TestCase):
    def test_source_cannot_be_overwritten(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory) / "rig.bbmodel"
            source.write_text(json.dumps(build_rig_document(make_two_triangle_fixture())[0]), encoding="utf-8")

            with self.assertRaisesRegex(ValueError, "source"):
                write_animated_model(source, source)

    def test_success_writes_five_animations(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "rig.bbmodel"
            output = root / "animated.bbmodel"
            source.write_text(json.dumps(build_rig_document(make_two_triangle_fixture())[0]), encoding="utf-8")

            result = write_animated_model(source, output)

            written = json.loads(output.read_text(encoding="utf-8"))
            self.assertEqual(len(written["animations"]), 5)
            self.assertEqual(result["animations"], 5)


if __name__ == "__main__":
    unittest.main()
