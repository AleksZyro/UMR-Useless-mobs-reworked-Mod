from pathlib import Path
import re
import unittest


ROOT = Path(__file__).resolve().parents[3]
ENTITY = ROOT / (
    "src/main/mobs/endermite/java/net/mysith/silverfish/"
    "CorruptedSilverfishEntity.java"
)


def java_lexical_view(source: str, mask_literals: bool) -> str:
    """Mask comments, and optionally literals, while preserving source offsets."""

    output = list(source)
    index = 0
    state = "code"
    while index < len(source):
        char = source[index]
        following = source[index + 1] if index + 1 < len(source) else ""
        if state == "code":
            if char == "/" and following == "/":
                output[index] = output[index + 1] = " "
                index += 2
                state = "line_comment"
                continue
            if char == "/" and following == "*":
                output[index] = output[index + 1] = " "
                index += 2
                state = "block_comment"
                continue
            if char in {'"', "'"}:
                state = "string" if char == '"' else "char"
                if mask_literals:
                    output[index] = " "
        elif state == "line_comment":
            if char == "\n":
                state = "code"
            else:
                output[index] = " "
        elif state == "block_comment":
            if char == "*" and following == "/":
                output[index] = output[index + 1] = " "
                index += 2
                state = "code"
                continue
            if char not in "\r\n":
                output[index] = " "
        else:
            if mask_literals and char not in "\r\n":
                output[index] = " "
            delimiter = '"' if state == "string" else "'"
            if char == "\\" and following:
                if mask_literals and following not in "\r\n":
                    output[index + 1] = " "
                index += 2
                continue
            if char == delimiter:
                state = "code"
        index += 1
    return "".join(output)


def java_without_comments(source: str) -> str:
    return java_lexical_view(source, mask_literals=False)


def brace_body(source: str, opening: int) -> str:
    structure = java_lexical_view(source, mask_literals=True)
    clean = java_without_comments(source)
    depth = 0
    for index in range(opening, len(structure)):
        if structure[index] == "{":
            depth += 1
        elif structure[index] == "}":
            depth -= 1
            if depth == 0:
                return clean[opening + 1 : index]
    raise AssertionError("unterminated Java block")


def method_body(source: str, signature_pattern: str) -> str:
    structure = java_lexical_view(source, mask_literals=True)
    signature = re.search(signature_pattern, structure)
    if signature is None:
        raise AssertionError(f"missing method: {signature_pattern}")
    opening = structure.find("{", signature.end())
    if opening < 0:
        raise AssertionError(f"missing method body: {signature_pattern}")
    return brace_body(source, opening)


def guarded_body(source: str, condition_pattern: str) -> str:
    structure = java_lexical_view(source, mask_literals=True)
    guard = re.search(r"\bif\s*\(\s*" + condition_pattern + r"\s*\)\s*\{", structure)
    if guard is None:
        raise AssertionError(f"missing guarded block: {condition_pattern}")
    return brace_body(source, structure.rfind("{", guard.start(), guard.end()))


def top_level_statements(source: str) -> list[str]:
    """Split a Java method body without treating nested statements as top-level."""

    structure = java_lexical_view(source, mask_literals=True)
    clean = java_without_comments(source)
    statements = []
    start = 0
    parentheses = 0
    brackets = 0
    braces = 0
    index = 0
    while index < len(structure):
        char = structure[index]
        if char == "(":
            parentheses += 1
        elif char == ")":
            parentheses -= 1
        elif char == "[":
            brackets += 1
        elif char == "]":
            brackets -= 1
        elif char == "{":
            braces += 1
        elif char == "}":
            braces -= 1
            if braces == 0 and parentheses == 0 and brackets == 0:
                following = index + 1
                while following < len(structure) and structure[following].isspace():
                    following += 1
                if not re.match(r"(?:else|catch|finally)\b", structure[following:]):
                    statement = clean[start : index + 1].strip()
                    if statement:
                        statements.append(statement)
                    start = index + 1
        elif char == ";" and braces == 0 and parentheses == 0 and brackets == 0:
            statement = clean[start : index + 1].strip()
            if statement:
                statements.append(statement)
            start = index + 1
        index += 1
    if clean[start:].strip():
        raise AssertionError(f"unterminated top-level Java statement: {clean[start:].strip()}")
    return statements


def return_statement_count(source: str) -> int:
    structure = java_lexical_view(source, mask_literals=True)
    return len(re.findall(r"\breturn\b", structure))


def compact(source: str) -> str:
    return re.sub(r"\s+", " ", source).strip()


class EntityAnimationIntegrationContract(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.source = ENTITY.read_text(encoding="utf-8")

    def test_declares_exact_animation_constants_and_play_modes(self):
        source = java_without_comments(self.source)
        declarations = re.findall(
            r"(?m)^\s*((?:(?:public|protected|private|static|final)\s+)*)"
            r"RawAnimation\s+(\w+)\s*=\s*([^;]+);",
            source,
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
        source = java_without_comments(self.source)
        body = method_body(
            self.source,
            r"\bvoid\s+registerControllers\s*\(\s*"
            r"AnimatableManager\.ControllerRegistrar\s+\w+\s*\)",
        )
        statements = top_level_statements(body)
        self.assertEqual(2, len(statements))
        self.assertEqual(0, return_statement_count(body))
        self.assertEqual(2, len(re.findall(r"\bcontrollers\.add\s*\(", body)))
        self.assertEqual(2, len(re.findall(r"\bcontrollers\.add\s*\(", source)))
        self.assertEqual(2, len(re.findall(r"\bnew\s+AnimationController\b", source)))
        self.assertEqual(
            [("movement", "3"), ("action", "0")],
            re.findall(
                r'new\s+AnimationController<>\s*\(\s*this\s*,\s*"([^"]+)"\s*,\s*(\d+)\s*,',
                body,
            ),
        )
        self.assertEqual(
            [("attack", "ATTACK_ANIM"), ("hurt", "HURT_ANIM"), ("death", "DEATH_ANIM")],
            re.findall(
                r'\.triggerableAnim\s*\(\s*"([^"]+)"\s*,\s*(\w+)\s*\)',
                body,
            ),
        )
        self.assertEqual(
            1,
            len(
                re.findall(
                    r"state\s*->\s*state\.setAndContinue\s*\(\s*state\.isMoving\s*\(\s*\)"
                    r"\s*\?\s*WALK_ANIM\s*:\s*IDLE_ANIM\s*\)",
                    body,
                )
            ),
        )
        self.assertEqual(
            1,
            len(
                re.findall(
                    r"state\s*->\s*this\.isDeadOrDying\s*\(\s*\)\s*\?\s*"
                    r"state\.setAndContinue\s*\(\s*DEATH_ANIM\s*\)\s*:\s*PlayState\.STOP",
                    body,
                )
            ),
        )
        self.assertNotRegex(body, r"setAnimation\s*\(\s*IDLE_ANIM\s*\)")

    def test_attack_triggers_only_after_successful_server_hit_and_keeps_effects(self):
        source = java_without_comments(self.source)
        body = method_body(
            self.source,
            r"\bboolean\s+doHurtTarget\s*\(\s*Entity\s+target\s*\)",
        )
        statements = top_level_statements(body)
        self.assertEqual(4, len(statements))
        self.assertRegex(
            compact(statements[0]),
            r"^boolean\s+hurt\s*=\s*super\.doHurtTarget\s*\(\s*target\s*\)\s*;$",
        )
        self.assertRegex(
            compact(statements[1]),
            r"^if\s*\(\s*hurt\s*&&\s*!\s*this\.level\s*\(\s*\)\.isClientSide\s*\)\s*\{",
        )
        self.assertRegex(
            compact(statements[2]),
            r"^if\s*\(\s*hurt\s*&&\s*target\s+instanceof\s+LivingEntity\s+living\s*\)\s*\{",
        )
        self.assertRegex(compact(statements[3]), r"^return\s+hurt\s*;$")
        self.assertEqual(1, return_statement_count(body))
        self.assertEqual(1, len(re.findall(r"\bsuper\.doHurtTarget\s*\(", source)))
        self.assertEqual(
            [("action", "attack"), ("action", "hurt"), ("action", "death")],
            re.findall(
                r'\btriggerAnim\s*\(\s*"([^"]+)"\s*,\s*"([^"]+)"\s*\)',
                source,
            ),
        )
        self.assertEqual(3, len(re.findall(r"\btriggerAnim\s*\(", source)))
        attack_guard = guarded_body(
            body, r"hurt\s*&&\s*!\s*this\.level\s*\(\s*\)\.isClientSide"
        )
        self.assertEqual(
            [("action", "attack")],
            re.findall(r'triggerAnim\s*\(\s*"([^"]+)"\s*,\s*"([^"]+)"\s*\)', attack_guard),
        )
        self.assertEqual(1, len(re.findall(r'triggerAnim\s*\(\s*"action"\s*,\s*"attack"\s*\)', body)))
        effects = re.findall(
            r"new\s+MobEffectInstance\s*\(\s*MobEffects\.(\w+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)",
            body,
        )
        self.assertEqual([("DIG_SLOWDOWN", "70", "0"), ("DARKNESS", "45", "0")], effects)
        self.assertEqual(2, len(re.findall(r"\b\w+\.addEffect\s*\(", body)))
        effect_guard = guarded_body(
            body, r"hurt\s*&&\s*target\s+instanceof\s+LivingEntity\s+living"
        )
        self.assertEqual(2, len(re.findall(r"\bliving\.addEffect\s*\(", effect_guard)))
        darkness_guard = guarded_body(
            effect_guard,
            r"this\.level\s*\(\s*\)\.getMaxLocalRawBrightness\s*\(\s*this\.blockPosition\s*\(\s*\)\s*\)\s*<=\s*4",
        )
        self.assertEqual(1, len(re.findall(r"MobEffects\.DARKNESS", darkness_guard)))
        ordered = [
            re.search(pattern, body).start()
            for pattern in (
                r"super\.doHurtTarget\s*\(",
                r'triggerAnim\s*\(\s*"action"\s*,\s*"attack"',
                r"MobEffects\.DIG_SLOWDOWN",
                r"MobEffects\.DARKNESS",
                r"return\s+hurt\s*;",
            )
        ]
        self.assertEqual(ordered, sorted(ordered))
        self.assertEqual(1, len(re.findall(r"\breturn\s+hurt\s*;", body)))

    def test_hurt_triggers_only_for_accepted_nonlethal_server_damage(self):
        source = java_without_comments(self.source)
        body = method_body(
            self.source,
            r"\bboolean\s+hurt\s*\(\s*DamageSource\s+source\s*,\s*float\s+amount\s*\)",
        )
        statements = top_level_statements(body)
        self.assertEqual(4, len(statements))
        self.assertRegex(
            compact(statements[0]),
            r"^boolean\s+hurt\s*=\s*super\.hurt\s*\(\s*source\s*,\s*amount\s*\)\s*;$",
        )
        self.assertRegex(
            compact(statements[1]),
            r"^if\s*\(\s*hurt\s*&&\s*!\s*this\.level\s*\(\s*\)\.isClientSide\s*&&\s*"
            r"!\s*this\.isDeadOrDying\s*\(\s*\)\s*\)\s*\{",
        )
        self.assertRegex(
            compact(statements[2]),
            r"^if\s*\(\s*hurt\s*&&\s*!\s*this\.level\s*\(\s*\)\.isClientSide\s*&&\s*"
            r"this\.swarmCallCooldown\s*<=\s*0\s*&&\s*source\.getEntity\s*\(\s*\)\s*"
            r"instanceof\s+LivingEntity\s+attacker\s*\)\s*\{",
        )
        self.assertRegex(compact(statements[3]), r"^return\s+hurt\s*;$")
        self.assertEqual(1, return_statement_count(body))
        self.assertEqual(1, len(re.findall(r"\bsuper\.hurt\s*\(", source)))
        hurt_guard = guarded_body(
            body,
            r"hurt\s*&&\s*!\s*this\.level\s*\(\s*\)\.isClientSide\s*&&\s*"
            r"!\s*this\.isDeadOrDying\s*\(\s*\)",
        )
        self.assertEqual(
            [("action", "hurt")],
            re.findall(r'triggerAnim\s*\(\s*"([^"]+)"\s*,\s*"([^"]+)"\s*\)', hurt_guard),
        )
        self.assertEqual(1, len(re.findall(r'triggerAnim\s*\(\s*"action"\s*,\s*"hurt"\s*\)', body)))
        swarm_guard = guarded_body(
            body,
            r"hurt\s*&&\s*!\s*this\.level\s*\(\s*\)\.isClientSide\s*&&\s*"
            r"this\.swarmCallCooldown\s*<=\s*0\s*&&\s*source\.getEntity\s*\(\s*\)\s*"
            r"instanceof\s+LivingEntity\s+attacker",
        )
        self.assertEqual(1, len(re.findall(r"\bcallSwarm\s*\(\s*attacker\s*\)", swarm_guard)))
        self.assertEqual(
            1,
            len(
                re.findall(
                    r"this\.swarmCallCooldown\s*=\s*150\s*\+\s*this\.random\.nextInt\s*\(\s*60\s*\)\s*;",
                    swarm_guard,
                )
            ),
        )
        ordered = [
            re.search(pattern, body).start()
            for pattern in (
                r"super\.hurt\s*\(",
                r'triggerAnim\s*\(\s*"action"\s*,\s*"hurt"',
                r"callSwarm\s*\(\s*attacker",
                r"return\s+hurt\s*;",
            )
        ]
        self.assertEqual(ordered, sorted(ordered))
        self.assertEqual(1, len(re.findall(r"\breturn\s+hurt\s*;", body)))

    def test_death_triggers_once_after_accepted_server_death(self):
        source = java_without_comments(self.source)
        body = method_body(
            self.source,
            r"\bvoid\s+die\s*\(\s*DamageSource\s+source\s*\)",
        )
        statements = top_level_statements(body)
        self.assertEqual(3, len(statements))
        self.assertRegex(compact(statements[0]), r"^boolean\s+wasDead\s*=\s*this\.dead\s*;$")
        self.assertRegex(compact(statements[1]), r"^super\.die\s*\(\s*source\s*\)\s*;$")
        self.assertRegex(
            compact(statements[2]),
            r"^if\s*\(\s*!\s*this\.level\s*\(\s*\)\.isClientSide\s*&&\s*!\s*wasDead\s*&&\s*"
            r"this\.dead\s*\)\s*\{",
        )
        self.assertEqual(0, return_statement_count(body))
        self.assertEqual(1, len(re.findall(r"\bsuper\.die\s*\(", source)))
        self.assertEqual(1, len(re.findall(r"\bboolean\s+wasDead\s*=\s*this\.dead\s*;", body)))
        death_guard = guarded_body(
            body,
            r"!\s*this\.level\s*\(\s*\)\.isClientSide\s*&&\s*!\s*wasDead\s*&&\s*this\.dead",
        )
        self.assertEqual(
            [("action", "death")],
            re.findall(r'triggerAnim\s*\(\s*"([^"]+)"\s*,\s*"([^"]+)"\s*\)', death_guard),
        )
        self.assertEqual(1, len(re.findall(r'triggerAnim\s*\(\s*"action"\s*,\s*"death"\s*\)', body)))
        ordered = [
            re.search(pattern, body).start()
            for pattern in (
                r"boolean\s+wasDead",
                r"super\.die\s*\(",
                r'triggerAnim\s*\(\s*"action"\s*,\s*"death"',
            )
        ]
        self.assertEqual(ordered, sorted(ordered))
        self.assertNotRegex(body, r"\bdeathTime\b|\bremove\s*\(")

    def test_entity_has_no_client_only_imports(self):
        imports = re.findall(r"^import\s+([^;]+);", java_without_comments(self.source), re.MULTILINE)
        self.assertFalse(
            [name for name in imports if ".client." in name or name.startswith("net.minecraft.client")]
        )

    def assert_mutant_rejected(self, mutant, contract_name):
        contract = EntityAnimationIntegrationContract(contract_name)
        contract.source = mutant
        with self.assertRaises(AssertionError):
            getattr(contract, contract_name)()

    def assert_mutant_accepted(self, mutant, contract_name):
        contract = EntityAnimationIntegrationContract(contract_name)
        contract.source = mutant
        getattr(contract, contract_name)()

    def test_contract_rejects_commented_out_override(self):
        start = self.source.index("    @Override\n    public boolean doHurtTarget")
        end = self.source.index("\n    @Override\n    public void aiStep", start)
        mutant = self.source[:start] + "    /*\n" + self.source[start:end] + "\n    */" + self.source[end:]
        self.assert_mutant_rejected(
            mutant, "test_attack_triggers_only_after_successful_server_hit_and_keeps_effects"
        )

    def test_contract_accepts_neutral_comments_and_whitespace(self):
        mutant = self.source.replace(
            "boolean hurt = super.doHurtTarget(target);",
            "// Preserve the vanilla hit result.\n"
            "        boolean   hurt = super.doHurtTarget( target );",
        )
        self.assert_mutant_accepted(
            mutant, "test_attack_triggers_only_after_successful_server_hit_and_keeps_effects"
        )

    def test_contract_rejects_hidden_early_return(self):
        mutant = self.source.replace(
            "boolean hurt = super.doHurtTarget(target);",
            "if (Boolean.TRUE.booleanValue()) { return false; }\n"
            "        boolean hurt = super.doHurtTarget(target);",
        )
        self.assert_mutant_rejected(
            mutant, "test_attack_triggers_only_after_successful_server_hit_and_keeps_effects"
        )

    def test_contract_rejects_expected_logic_nested_under_false_guard(self):
        start = self.source.index("        boolean hurt = super.doHurtTarget(target);")
        end = self.source.index("\n    }\n\n    @Override\n    public void aiStep", start)
        original_body = self.source[start:end]
        nested_body = "\n".join("    " + line for line in original_body.splitlines())
        mutant = (
            self.source[:start]
            + "        if (false) {\n"
            + nested_body
            + "\n        }\n        return false;"
            + self.source[end:]
        )
        self.assert_mutant_rejected(
            mutant, "test_attack_triggers_only_after_successful_server_hit_and_keeps_effects"
        )

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
