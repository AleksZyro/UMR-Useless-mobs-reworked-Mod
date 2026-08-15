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

    def test_motion_contract_is_visible_and_directional(self):
        specs = ANIMATION_SPECS
        walk = specs["animation.corrupted_silverfish.walk"]["bones"]
        first = walk["leg_front_left"]["rotation"][0][1]
        opposite = walk["leg_front_right"]["rotation"][0][1]
        self.assertEqual(first, [-value for value in opposite])
        self.assertGreater(abs(first[1]), 10)

        attack = specs["animation.corrupted_silverfish.attack"]["bones"]
        self.assertGreater(attack["head"]["position"][1][1][2], 1)

        death = specs["animation.corrupted_silverfish.death"]["bones"]
        self.assertGreater(abs(death["body_rear"]["rotation"][-1][1][2]), 70)

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
