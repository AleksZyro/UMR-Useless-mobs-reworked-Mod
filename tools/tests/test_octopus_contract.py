import json
import unittest
from hashlib import sha256
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
OCTOPUS = ROOT / "Modelle/Exports/octopus_v1"
ENTITY = ROOT / "src/main/java/com/Momik/usless_mobs/entity/OctopusEntity.java"
ENTITIES = ROOT / "src/main/java/com/Momik/usless_mobs/registry/ModEntities.java"
RENDERER = ROOT / "src/main/java/com/Momik/usless_mobs/client/OctopusRenderer.java"
EXACT_LAYER = ROOT / "src/main/java/com/Momik/usless_mobs/client/ExactMobMeshLayer.java"


class OctopusContract(unittest.TestCase):
    def test_approved_octopus_source_is_archived_and_hashed(self):
        provenance_path = OCTOPUS / "review/source_provenance.json"
        provenance = json.loads(provenance_path.read_text(encoding="utf-8"))
        source = (provenance_path.parent / provenance["source_glb"]).resolve()

        self.assertEqual("Tripo Multi-View Builder", provenance["generator"])
        self.assertEqual("4096x4096", provenance["texture_quality"])
        self.assertTrue(provenance["approved"])
        self.assertEqual(sha256(source.read_bytes()).hexdigest(), provenance["source_sha256"])
        self.assertEqual("-Z", provenance["approved_front_axis"])

    def test_octopus_syncs_and_persists_visual_action_state(self):
        source = ENTITY.read_text(encoding="utf-8")

        self.assertIn("EntityDataAccessor<Byte> ACTION_STATE", source)
        self.assertIn("EntityDataAccessor<Boolean> SQUEEZING", source)
        self.assertIn("this.entityData.define(ACTION_STATE, ACTION_IDLE)", source)
        self.assertIn('tag.putByte("OctopusAction", getActionState())', source)
        self.assertIn('tag.putBoolean("OctopusSqueezing", isSqueezing())', source)
        self.assertIn('setActionState(tag.getByte("OctopusAction"))', source)

    def test_octopus_has_eight_arm_ambush_camouflage_squeeze_and_object_behaviour(self):
        entity = ENTITY.read_text(encoding="utf-8")
        registry = ENTITIES.read_text(encoding="utf-8")

        self.assertIn("beginAmbush", entity)
        self.assertIn("beginCamouflage", entity)
        self.assertIn("updateSqueezing", entity)
        self.assertIn("interactWithNearbyObject", entity)
        self.assertIn("refreshDimensions()", entity)
        self.assertIn("getDimensions(Pose pose)", entity)
        self.assertIn("EntityDimensions.scalable(0.62F, 0.48F)", entity)
        self.assertIn(".sized(1.15F, 1.20F)", registry)
        self.assertIn('tag.putInt("OctopusCarriedTicks", carriedObjectTicks)', entity)
        self.assertIn('carriedObjectTicks = tag.getInt("OctopusCarriedTicks")', entity)
        self.assertIn("AABB normalProbe", entity)
        self.assertIn("this.level().noCollision(this, normalProbe)", entity)
        self.assertIn("dropCarriedObject();\n        super.dropCustomDeathLoot", entity)

    def test_octopus_renderer_uses_exact_mesh_and_all_action_states(self):
        renderer = RENDERER.read_text(encoding="utf-8")
        layer = EXACT_LAYER.read_text(encoding="utf-8")

        self.assertEqual(1, renderer.count("new ExactMobMeshLayer<>("))
        self.assertIn("OCTOPUS_EXACT_TEXTURE", renderer)
        self.assertIn("octopusPose", layer)
        for state in (
            "ACTION_SWIM",
            "ACTION_AMBUSH",
            "ACTION_GRAB",
            "ACTION_INK",
            "ACTION_CAMOUFLAGE",
            "ACTION_OBJECT",
        ):
            self.assertIn(state, layer)
        for index in range(8):
            self.assertIn(f'"tentacle{index}"', layer)

if __name__ == "__main__":
    unittest.main()
