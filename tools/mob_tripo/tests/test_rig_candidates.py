from __future__ import annotations

import json
import unittest
from pathlib import Path

from tools.mob_tripo.rig_candidates import JOBS, rig_document


ROOT = Path(__file__).resolve().parents[3]


def flatten_groups(items):
    result = []
    for item in items:
        if isinstance(item, dict):
            result.append(item)
            result.extend(flatten_groups(item.get("children", [])))
    return result


class MobRigContract(unittest.TestCase):
    def test_every_candidate_is_segmented_without_changing_its_cubes(self):
        for name, job in JOBS.items():
            with self.subTest(name=name):
                source = json.loads((ROOT / job.source).read_text(encoding="utf-8"))
                rigged = rig_document(name, source)
                self.assertEqual(source["elements"], rigged["elements"])
                groups = flatten_groups(rigged["outliner"])
                self.assertEqual(set(job.bones), {group["name"] for group in groups})
                element_ids = {element["uuid"] for element in source["elements"]}
                assigned = [child for group in groups for child in group["children"] if isinstance(child, str)]
                self.assertEqual(element_ids, set(assigned))
                self.assertEqual(len(element_ids), len(assigned))

    def test_all_motion_bones_own_visible_geometry(self):
        for name, job in JOBS.items():
            with self.subTest(name=name):
                source = json.loads((ROOT / job.source).read_text(encoding="utf-8"))
                rigged = rig_document(name, source)
                groups = flatten_groups(rigged["outliner"])
                by_name = {group["name"]: group for group in groups}
                for bone in job.bones:
                    if bone != "root":
                        self.assertTrue(
                            any(isinstance(child, str) for child in by_name[bone]["children"]),
                            f"{name}:{bone}",
                        )

    def test_rig_contains_looping_idle_and_motion_previews(self):
        for name, job in JOBS.items():
            with self.subTest(name=name):
                source = json.loads((ROOT / job.source).read_text(encoding="utf-8"))
                rigged = rig_document(name, source)
                animations = rigged["animations"]
                self.assertGreaterEqual(len(animations), 2)
                self.assertTrue(all(animation["loop"] == "loop" for animation in animations))
                self.assertIn("idle", animations[0]["name"])
                self.assertRegex(animations[1]["name"], r"walk|swim|fly")
                group_ids = {group["uuid"] for group in flatten_groups(rigged["outliner"])}
                amplitudes = []
                for animation in animations:
                    self.assertTrue(set(animation["animators"]) <= group_ids)
                    for animator in animation["animators"].values():
                        for keyframe in animator["keyframes"]:
                            amplitudes.extend(abs(float(value)) for value in keyframe["data_points"][0].values())
                self.assertGreaterEqual(max(amplitudes), 20.0)


if __name__ == "__main__":
    unittest.main()
