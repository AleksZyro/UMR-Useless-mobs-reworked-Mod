from pathlib import Path
import re
import unittest


ROOT = Path(__file__).resolve().parents[3]
ENTITY = ROOT / (
    "src/main/mobs/endermite/java/net/mysith/silverfish/"
    "CorruptedSilverfishEntity.java"
)


def method_body(source: str, signature: str) -> str:
    try:
        start = source.index(signature)
    except ValueError as error:
        raise AssertionError(f"missing method: {signature}") from error
    opening = source.index("{", start)
    depth = 0
    for index in range(opening, len(source)):
        if source[index] == "{":
            depth += 1
        elif source[index] == "}":
            depth -= 1
            if depth == 0:
                return source[opening + 1 : index]
    raise AssertionError(f"unterminated method: {signature}")


def compact(source: str) -> str:
    return re.sub(r"\s+", " ", source).strip()


class EntityAnimationIntegrationContract(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.source = ENTITY.read_text(encoding="utf-8")

    def test_declares_exact_animation_constants_and_play_modes(self):
        declarations = re.findall(
            r"private\s+static\s+final\s+RawAnimation\s+(\w+)\s*=\s*"
            r"RawAnimation\.begin\(\)\.(thenLoop|thenPlay)\(\"([^\"]+)\"\)\s*;",
            self.source,
        )
        self.assertEqual(
            [
                ("IDLE_ANIM", "thenLoop", "animation.corrupted_silverfish.idle"),
                ("WALK_ANIM", "thenLoop", "animation.corrupted_silverfish.walk"),
                ("ATTACK_ANIM", "thenPlay", "animation.corrupted_silverfish.attack"),
                ("HURT_ANIM", "thenPlay", "animation.corrupted_silverfish.hurt"),
                ("DEATH_ANIM", "thenPlay", "animation.corrupted_silverfish.death"),
            ],
            declarations,
        )

    def test_registers_exact_movement_and_action_controllers(self):
        body = compact(method_body(self.source, "void registerControllers("))
        self.assertEqual(2, body.count("controllers.add("))
        self.assertRegex(
            body,
            r'controllers\.add\(new AnimationController<>\(this, "movement", 3, '
            r'state -> state\.setAndContinue\(state\.isMoving\(\) \? WALK_ANIM : IDLE_ANIM\)\)\);',
        )
        self.assertRegex(
            body,
            r'controllers\.add\(new AnimationController<>\(this, "action", 0, '
            r'state -> PlayState\.STOP\)'
            r'\s*\.triggerableAnim\("attack", ATTACK_ANIM\)'
            r'\s*\.triggerableAnim\("hurt", HURT_ANIM\)'
            r'\s*\.triggerableAnim\("death", DEATH_ANIM\)\);',
        )
        self.assertNotIn('"main"', body)
        self.assertNotIn("setAnimation(IDLE_ANIM)", body)

    def test_attack_triggers_only_after_successful_server_hit_and_keeps_effects(self):
        body = compact(method_body(self.source, "boolean doHurtTarget("))
        self.assertEqual(1, body.count("super.doHurtTarget(target)"))
        trigger = 'if (hurt && !this.level().isClientSide) { this.triggerAnim("action", "attack"); }'
        self.assertIn(trigger, body)
        self.assertLess(body.index("super.doHurtTarget(target)"), body.index(trigger))
        self.assertIn("if (hurt && target instanceof LivingEntity living)", body)
        self.assertIn("MobEffects.DIG_SLOWDOWN, 70, 0", body)
        self.assertIn("MobEffects.DARKNESS, 45, 0", body)
        self.assertEqual(1, body.count("return hurt;"))

    def test_hurt_triggers_only_for_accepted_nonlethal_server_damage(self):
        body = compact(method_body(self.source, "boolean hurt(DamageSource"))
        self.assertEqual(1, body.count("super.hurt(source, amount)"))
        trigger = (
            'if (hurt && !this.level().isClientSide && !this.isDeadOrDying()) { '
            'this.triggerAnim("action", "hurt"); }'
        )
        self.assertIn(trigger, body)
        self.assertLess(body.index("super.hurt(source, amount)"), body.index(trigger))
        self.assertIn(
            "hurt && !this.level().isClientSide && this.swarmCallCooldown <= 0 "
            "&& source.getEntity() instanceof LivingEntity attacker",
            body,
        )
        self.assertIn("callSwarm(attacker);", body)
        self.assertIn("this.swarmCallCooldown = 150 + this.random.nextInt(60);", body)
        self.assertEqual(1, body.count("return hurt;"))

    def test_death_triggers_once_after_accepted_server_death(self):
        body = compact(method_body(self.source, "void die(DamageSource"))
        self.assertEqual(1, body.count("super.die(source)"))
        self.assertEqual(1, body.count('this.triggerAnim("action", "death")'))
        self.assertIn("boolean wasDead = this.dead;", body)
        guard = (
            "if (!this.level().isClientSide && !wasDead && this.dead) { "
            'this.triggerAnim("action", "death"); }'
        )
        self.assertIn(guard, body)
        self.assertLess(body.index("super.die(source)"), body.index(guard))
        self.assertNotIn("deathTime", body)
        self.assertNotIn("remove(", body)

    def test_entity_has_no_client_only_imports(self):
        imports = re.findall(r"^import\s+([^;]+);", self.source, re.MULTILINE)
        self.assertFalse(
            [name for name in imports if ".client." in name or name.startswith("net.minecraft.client")]
        )


if __name__ == "__main__":
    unittest.main()
