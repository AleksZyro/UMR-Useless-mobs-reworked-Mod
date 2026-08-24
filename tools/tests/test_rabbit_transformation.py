from pathlib import Path
import json
import unittest


ROOT = Path(__file__).resolve().parents[2]
EFFECTS = ROOT / "src/main/java/com/Momik/usless_mobs/registry/ModEffects.java"
EVENTS = ROOT / "src/main/java/com/Momik/usless_mobs/event/RabbitTransformationHandler.java"
LIVING_EVENTS = ROOT / "src/main/java/com/Momik/usless_mobs/event/LivingMobReworkHandler.java"
RENDERER = ROOT / "src/main/java/com/Momik/usless_mobs/client/RabbitTransformationRenderer.java"
WITCH = ROOT / "src/main/java/com/Momik/usless_mobs/entity/WitchBossEntity.java"
ICON = ROOT / "src/main/resources/assets/usless_mobs/textures/mob_effect/rabbit_form.png"


class RabbitTransformationContractTests(unittest.TestCase):
    def test_effect_is_registered_for_vanilla_synchronization(self):
        source = EFFECTS.read_text(encoding="utf-8")

        self.assertIn("RABBIT_FORM", source)
        self.assertIn('register("rabbit_form"', source)

    def test_effect_has_a_real_icon_instead_of_missing_texture(self):
        self.assertTrue(ICON.is_file(), f"missing rabbit effect icon: {ICON}")
        self.assertEqual(ICON.read_bytes()[:8], b"\x89PNG\r\n\x1a\n")

    def test_effect_name_is_localized_in_both_supported_languages(self):
        lang = ROOT / "src/main/resources/assets/usless_mobs/lang"
        english = json.loads((lang / "en_us.json").read_text(encoding="utf-8"))
        german = json.loads((lang / "de_de.json").read_text(encoding="utf-8"))

        self.assertEqual("Rabbit Form", english["effect.usless_mobs.rabbit_form"])
        self.assertEqual("Hasenform", german["effect.usless_mobs.rabbit_form"])

    def test_server_enforces_small_hitbox_and_no_weapons(self):
        self.assertTrue(EVENTS.is_file(), f"missing rabbit lifecycle handler: {EVENTS}")
        source = EVENTS.read_text(encoding="utf-8")

        self.assertIn("EntityEvent.Size", source)
        self.assertIn("EntityDimensions.scalable(0.45F, 0.60F)", source)
        self.assertIn("AttackEntityEvent", source)
        self.assertIn("PlayerInteractEvent.RightClickItem", source)
        self.assertIn("event.setCanceled(true)", source)
        self.assertIn("PlayerEvent.PlayerChangedDimensionEvent", source)
        self.assertIn("PlayerEvent.PlayerLoggedOutEvent", source)
        self.assertIn("LivingDeathEvent", source)

    def test_size_hook_does_not_read_effects_during_player_construction(self):
        source = EVENTS.read_text(encoding="utf-8")

        self.assertIn("player.isAddedToWorld()", source)

    def test_client_replaces_only_transformed_player_render_with_rabbit(self):
        self.assertTrue(RENDERER.is_file(), f"missing rabbit player renderer: {RENDERER}")
        source = RENDERER.read_text(encoding="utf-8")

        self.assertIn("RenderPlayerEvent.Pre", source)
        self.assertIn("ModEffects.RABBIT_FORM", source)
        self.assertIn("EntityType.RABBIT.create", source)
        self.assertIn("event.setCanceled(true)", source)

    def test_witch_starts_difficulty_scaled_hunt_without_replacing_decoy(self):
        source = WITCH.read_text(encoding="utf-8")

        self.assertIn("startRabbitHunt", source)
        self.assertIn("rabbitDodge", source)
        self.assertIn("MobEffects.MOVEMENT_SPEED", source)
        self.assertIn("MobEffects.JUMP", source)
        self.assertIn("EntityType.WOLF.create", source)
        self.assertIn("profile.huntHoundCount()", source)
        self.assertIn("difficultyProfile().cooldown(340)", source)

    def test_hunt_hounds_expire_and_cannot_outlive_their_owner(self):
        witch = WITCH.read_text(encoding="utf-8")
        lifecycle = LIVING_EVENTS.read_text(encoding="utf-8")

        self.assertIn("HUNT_HOUND_TICKS_KEY", witch)
        self.assertIn("putInt(HUNT_HOUND_TICKS_KEY, duration + 40)", witch)
        self.assertIn("tickWitchHound", lifecycle)
        self.assertIn("level.getEntity(ownerId) instanceof WitchBossEntity", lifecycle)
        self.assertIn("hound.discard()", lifecycle)
        self.assertIn("public void remove(RemovalReason reason)", witch)
        self.assertIn("finishRabbitHunt", witch)


if __name__ == "__main__":
    unittest.main()
