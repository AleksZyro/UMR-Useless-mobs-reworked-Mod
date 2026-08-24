import math
import unittest
from collections import Counter

from tools.corrupted_silverfish_v3 import spec
from tools.corrupted_silverfish_v3.spec import ANIMATIONS, BONES, CUBES


class ModelSpecContract(unittest.TestCase):
    def test_has_exact_bone_and_cube_counts(self):
        self.assertEqual(32, len(BONES))
        self.assertEqual(112, len(CUBES))

    def test_bone_and_cube_names_are_unique(self):
        bone_names = [bone.name for bone in BONES]
        cube_names = [cube.name for cube in CUBES]
        self.assertEqual(len(bone_names), len(set(bone_names)))
        self.assertEqual(len(cube_names), len(set(cube_names)))

    def test_all_bone_references_exist(self):
        bone_names = {bone.name for bone in BONES}
        self.assertTrue(all(bone.parent is None or bone.parent in bone_names for bone in BONES))
        self.assertTrue(all(cube.bone in bone_names for cube in CUBES))

    def test_bone_pivots_are_finite_vec3_values(self):
        for bone in BONES:
            with self.subTest(bone=bone.name):
                self.assertEqual(3, len(bone.pivot))
                self.assertTrue(all(math.isfinite(value) for value in bone.pivot))

    def test_bone_hierarchy_has_one_root_and_no_cycles(self):
        parents = {bone.name: bone.parent for bone in BONES}
        roots = [name for name, parent in parents.items() if parent is None]
        self.assertEqual(["root"], roots)

        for bone_name in parents:
            with self.subTest(bone=bone_name):
                current = bone_name
                visited = set()
                while parents[current] is not None:
                    self.assertNotIn(current, visited)
                    visited.add(current)
                    current = parents[current]
                self.assertEqual("root", current)

    def test_cube_geometry_is_positive_and_finite(self):
        for cube in CUBES:
            with self.subTest(cube=cube.name):
                self.assertEqual(3, len(cube.size))
                self.assertTrue(all(value > 0 for value in cube.size))
                self.assertEqual(3, len(cube.origin))
                self.assertEqual(3, len(cube.rotation))
                self.assertTrue(all(math.isfinite(value) for value in cube.origin))
                self.assertTrue(all(math.isfinite(value) for value in cube.rotation))

    def test_model_stays_in_bounds_and_fills_the_silhouette(self):
        minimum = tuple(min(cube.origin[axis] for cube in CUBES) for axis in range(3))
        maximum = tuple(
            max(cube.origin[axis] + cube.size[axis] for cube in CUBES)
            for axis in range(3)
        )
        self.assertGreaterEqual(minimum[0], -7)
        self.assertGreaterEqual(minimum[1], 0)
        self.assertGreaterEqual(minimum[2], -16)
        self.assertLessEqual(maximum[0], 7)
        self.assertLessEqual(maximum[1], 12)
        self.assertLessEqual(maximum[2], 21)
        self.assertLessEqual(minimum[0], -5)
        self.assertGreaterEqual(maximum[0], 5)
        self.assertTrue(
            any(
                cube.category == "body_core_and_tail"
                and cube.origin[2] + cube.size[2] >= 19
                for cube in CUBES
            )
        )

    def test_animation_metadata_is_exact(self):
        self.assertEqual(
            {
                "idle": 1.6,
                "walk": 0.8,
                "attack": 0.45,
                "hurt": 0.3,
                "death": 1.0,
            },
            ANIMATIONS,
        )
        self.assertEqual(["idle", "walk", "attack", "hurt", "death"], list(ANIMATIONS))

    def test_animation_metadata_rejects_assignment(self):
        with self.assertRaises(TypeError):
            ANIMATIONS["mutant"] = 9.9

    def test_animation_metadata_has_no_mutable_backing_alias(self):
        self.assertFalse(hasattr(spec, "_animations"))

    def test_rotated_cube_pivot_is_its_exact_geometric_center(self):
        cube = next(cube for cube in CUBES if cube.name == "forehead_left")
        self.assertNotEqual((0.0, 0.0, 0.0), cube.rotation)
        for actual, expected in zip(spec.cube_pivot(cube), (-2.3, 6.05, -10.8)):
            self.assertAlmostEqual(expected, actual)

    def test_cube_category_budgets_are_exact(self):
        self.assertEqual(
            {
                "body_core_and_tail": 27,
                "layered_armor": 38,
                "legs_and_feet": 24,
                "mandibles_and_mouth": 4,
                "crystals": 19,
            },
            dict(Counter(cube.category for cube in CUBES)),
        )


if __name__ == "__main__":
    unittest.main()
