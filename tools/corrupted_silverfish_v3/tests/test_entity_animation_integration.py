from pathlib import Path
import re
import unittest


ROOT = Path(__file__).resolve().parents[3]
ENTITY = ROOT / (
    "src/main/mobs/endermite/java/net/mysith/silverfish/"
    "CorruptedSilverfishEntity.java"
)


def method_body(source: str, signature_pattern: str) -> str:
    signature = re.search(signature_pattern, source)
    if signature is None:
        raise AssertionError(f"missing method: {signature_pattern}")
    opening = source.find("{", signature.end())
    if opening < 0:
        raise AssertionError(f"missing method body: {signature_pattern}")
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
            r"(?m)^\s*((?:(?:public|protected|private|static|final)\s+)*)"
            r"RawAnimation\s+(\w+)\s*=\s*([^;]+);",
            self.source,
        )
        self.assertEqual(
            [
                (
                    "private static final ",
                    "IDLE_ANIM",
                    'RawAnimation.begin().thenLoop("animation.corrupted_silverfish.idle")',
                ),
                (
                    "private static final ",
                    "WALK_ANIM",
                    'RawAnimation.begin().thenLoop("animation.corrupted_silverfish.walk")',
                ),
                (
                    "private static final ",
                    "ATTACK_ANIM",
                    'RawAnimation.begin().thenPlay("animation.corrupted_silverfish.attack")',
                ),
                (
                    "private static final ",
                    "HURT_ANIM",
                    'RawAnimation.begin().thenPlay("animation.corrupted_silverfish.hurt")',
                ),
                (
                    "private static final ",
                    "DEATH_ANIM",
                    'RawAnimation.begin().thenPlay("animation.corrupted_silverfish.death")',
                ),
            ],
            [(modifiers, name, compact(value)) for modifiers, name, value in declarations],
        )

    def test_registers_exact_movement_and_action_controllers(self):
        body = compact(
            method_body(
                self.source,
                r"\bvoid\s+registerControllers\s*\(\s*"
                r"AnimatableManager\.ControllerRegistrar\s+\w+\s*\)",
            )
        )
        self.assertEqual(2, body.count("controllers.add("))
        self.assertEqual(2, len(re.findall(r"\bcontrollers\.add\s*\(", self.source)))
        self.assertEqual(2, len(re.findall(r"\bnew\s+AnimationController\b", self.source)))
        self.assertEqual(
            compact(
                """
                controllers.add(new AnimationController<>(this, "movement", 3,
                        state -> state.setAndContinue(state.isMoving() ? WALK_ANIM : IDLE_ANIM)));
                controllers.add(new AnimationController<>(this, "action", 0, state -> PlayState.STOP)
                        .triggerableAnim("attack", ATTACK_ANIM)
                        .triggerableAnim("hurt", HURT_ANIM)
                        .triggerableAnim("death", DEATH_ANIM));
                """
            ),
            body,
        )

    def test_attack_triggers_only_after_successful_server_hit_and_keeps_effects(self):
        body = compact(
            method_body(
                self.source,
                r"\bboolean\s+doHurtTarget\s*\(\s*Entity\s+target\s*\)",
            )
        )
        self.assertEqual(1, len(re.findall(r"\bsuper\.doHurtTarget\s*\(", self.source)))
        self.assertEqual(
            [("action", "attack"), ("action", "hurt"), ("action", "death")],
            re.findall(
                r'\btriggerAnim\s*\(\s*"([^"]+)"\s*,\s*"([^"]+)"\s*\)',
                self.source,
            ),
        )
        self.assertEqual(3, len(re.findall(r"\btriggerAnim\s*\(", self.source)))
        self.assertEqual(
            compact(
                """
                boolean hurt = super.doHurtTarget(target);
                if (hurt && !this.level().isClientSide) {
                    this.triggerAnim("action", "attack");
                }
                if (hurt && target instanceof LivingEntity living) {
                    living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 70, 0));
                    if (this.level().getMaxLocalRawBrightness(this.blockPosition()) <= 4) {
                        living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 45, 0));
                    }
                }
                return hurt;
                """
            ),
            body,
        )

    def test_hurt_triggers_only_for_accepted_nonlethal_server_damage(self):
        body = compact(
            method_body(
                self.source,
                r"\bboolean\s+hurt\s*\(\s*DamageSource\s+source\s*,\s*float\s+amount\s*\)",
            )
        )
        self.assertEqual(1, len(re.findall(r"\bsuper\.hurt\s*\(", self.source)))
        self.assertEqual(
            compact(
                """
                boolean hurt = super.hurt(source, amount);
                if (hurt && !this.level().isClientSide && !this.isDeadOrDying()) {
                    this.triggerAnim("action", "hurt");
                }
                if (hurt && !this.level().isClientSide && this.swarmCallCooldown <= 0 && source.getEntity() instanceof LivingEntity attacker) {
                    callSwarm(attacker);
                    this.swarmCallCooldown = 150 + this.random.nextInt(60);
                }
                return hurt;
                """
            ),
            body,
        )

    def test_death_triggers_once_after_accepted_server_death(self):
        body = compact(
            method_body(
                self.source,
                r"\bvoid\s+die\s*\(\s*DamageSource\s+source\s*\)",
            )
        )
        self.assertEqual(1, len(re.findall(r"\bsuper\.die\s*\(", self.source)))
        self.assertEqual(
            compact(
                """
                boolean wasDead = this.dead;
                super.die(source);
                if (!this.level().isClientSide && !wasDead && this.dead) {
                    this.triggerAnim("action", "death");
                }
                """
            ),
            body,
        )

    def test_entity_has_no_client_only_imports(self):
        imports = re.findall(r"^import\s+([^;]+);", self.source, re.MULTILINE)
        self.assertFalse(
            [name for name in imports if ".client." in name or name.startswith("net.minecraft.client")]
        )

    def assert_mutant_rejected(self, mutant, contract_name):
        contract = EntityAnimationIntegrationContract(contract_name)
        contract.source = mutant
        with self.assertRaises(AssertionError):
            getattr(contract, contract_name)()

    def test_contract_rejects_extra_raw_animation_field(self):
        mutant = self.source.replace(
            "private boolean panicBurstUsed = false;",
            "private static final RawAnimation EXTRA_ANIM = RawAnimation.copyOf(IDLE_ANIM);\n"
            "    private boolean panicBurstUsed = false;",
        )
        self.assert_mutant_rejected(
            mutant, "test_declares_exact_animation_constants_and_play_modes"
        )

    def test_contract_rejects_extra_unguarded_attack_trigger(self):
        mutant = self.source.replace(
            "super.aiStep();",
            'super.aiStep();\n        this.triggerAnim("action", "attack");',
        )
        self.assert_mutant_rejected(
            mutant, "test_attack_triggers_only_after_successful_server_hit_and_keeps_effects"
        )

    def test_contract_rejects_extra_poison_effect(self):
        mutant = self.source.replace(
            "living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 70, 0));",
            "living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 70, 0));\n"
            "            living.addEffect(new MobEffectInstance(MobEffects.POISON, 20, 0));",
        )
        self.assert_mutant_rejected(
            mutant, "test_attack_triggers_only_after_successful_server_hit_and_keeps_effects"
        )


if __name__ == "__main__":
    unittest.main()
