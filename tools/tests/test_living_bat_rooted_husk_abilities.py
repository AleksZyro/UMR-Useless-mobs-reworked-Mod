from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java/com/Momik/usless_mobs"


def test_living_bat_owns_its_venom_echo_attack():
    entity = (JAVA / "entity/LivingBatEntity.java").read_text(encoding="utf-8")
    handler = (JAVA / "event/LivingMobReworkHandler.java").read_text(encoding="utf-8")

    assert "private static final int ECHO_ATTACK_COOLDOWN_TICKS = 80;" in entity
    assert "public void aiStep()" in entity
    assert "MobEffects.POISON" in entity
    assert "MobEffects.BLINDNESS" in entity
    assert "ParticleTypes.SCULK_SOUL" in entity
    assert "instanceof Bat bat" not in handler
    assert "tickBat(" not in handler


def test_rooted_husk_owns_its_rooting_strike():
    entity = (JAVA / "entity/RootedHuskEntity.java").read_text(encoding="utf-8")
    handler = (JAVA / "event/LivingCombatHandler.java").read_text(encoding="utf-8")

    assert "public boolean doHurtTarget(Entity target)" in entity
    assert "MobEffects.HUNGER" in entity
    assert "MobEffects.MOVEMENT_SLOWDOWN" in entity
    assert "ParticleTypes.SPORE_BLOSSOM_AIR" in entity
    assert "SoundEvents.ROOTED_DIRT_BREAK" in entity
    assert "source instanceof Husk" not in handler
