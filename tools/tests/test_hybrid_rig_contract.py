from pathlib import Path
import re


ROOT = Path(__file__).resolve().parents[2]
CLIENT = ROOT / "src/main/java/com/Momik/usless_mobs/client"


def _source(name: str) -> str:
    return (CLIENT / name).read_text(encoding="utf-8")


def _render_loop(source: str) -> str:
    start = source.index("for (String bone :")
    end = source.index("poseStack.popPose();", start)
    return source[start:end]


def _block_after(source: str, marker: str) -> str:
    marker_start = source.index(marker)
    block_start = source.index("{", marker_start) + 1
    depth = 1
    for index in range(block_start, len(source)):
        if source[index] == "{":
            depth += 1
        elif source[index] == "}":
            depth -= 1
            if depth == 0:
                return source[block_start:index]
    raise AssertionError(f"unclosed Java block after {marker!r}")


def _call(source: str, method: str) -> str:
    start = source.index(method)
    return source[start:source.index(";", start) + 1]


def _method_body(source: str, signature: str) -> str:
    return _block_after(source, signature)


def _compact(source: str) -> str:
    return re.sub(r"\s+", " ", source).strip()


def test_exact_animation_lod_has_ordered_boundaries_and_rejects_invalid_distance():
    source = _source("ExactAnimationLod.java")
    at_body = _method_body(source, "static ExactAnimationLod at(double distanceSquared)")

    assert re.search(r"enum\s+ExactAnimationLod\s*\{\s*NEAR,\s*MID,\s*FAR", source)
    assert "NEAR_DISTANCE_SQUARED = 144.0D" in source
    assert "MID_DISTANCE_SQUARED = 576.0D" in source
    assert source.index("NEAR_DISTANCE_SQUARED") < source.index("MID_DISTANCE_SQUARED")

    invalid = "if (!Double.isFinite(distanceSquared) || distanceSquared < 0.0D)"
    near = "if (distanceSquared <= NEAR_DISTANCE_SQUARED)"
    mid = "if (distanceSquared <= MID_DISTANCE_SQUARED)"
    assert at_body.count(invalid) == 1
    assert at_body.count(near) == 1
    assert at_body.count(mid) == 1
    assert at_body.index(invalid) < at_body.index(near) < at_body.index(mid)
    assert _compact(_block_after(at_body, invalid)) == (
        'throw new IllegalArgumentException("distanceSquared must be finite and non-negative");'
    )
    assert _compact(_block_after(at_body, near)) == "return NEAR;"
    assert _compact(_block_after(at_body, mid)) == "return MID;"
    assert _compact(at_body).endswith("return FAR;")


def test_quantized_age_changes_only_mid_and_preserves_near_and_far_age():
    lod = _source("ExactAnimationLod.java")
    body = _compact(_method_body(lod, "float quantizedAge(float ageInTicks)"))

    assert body == (
        "return this == MID ? Mth.floor(ageInTicks * 0.5F) * 2.0F : ageInTicks;"
    )


def test_rig_pose_preserves_every_variant_root_frequency():
    pose = _source("ExactRigPose.java")
    variant_source = _source("CustomMob3DModel.java")
    update = _method_body(pose, "void updateFor(CustomMob3DModel.Variant variant, LivingEntity entity,")
    speed_switch = _block_after(update, "switch (variant)")

    variants = {
        name.strip()
        for name in _block_after(variant_source, "public enum Variant").split(",")
        if name.strip()
    }
    expected = {
        "LIVING_BOSS": "0.0F",
        "FROST_STRAY": "0.0F",
        "WEB_CAVE_SPIDER": "0.20F",
        "CORAL_DROWNED": "0.10F",
        "OCTOPUS": "0.08F",
        "SQUID": "0.12F",
        "GLOW_SQUID": "0.14F",
        "WITCH_BOSS": "0.10F",
        "LIVING_BAT": "0.16F",
        "ROOTED_HUSK": "0.08F",
        "POLAR_BEAR": "0.0F",
        "AXOLOTL": "0.16F",
        "OCELOT": "0.0F",
    }
    actual = {}
    for names, speed in re.findall(r"case\s+([A-Z_, ]+)\s*->\s*([0-9.]+F);", speed_switch):
        for name in names.split(","):
            actual[name.strip()] = speed

    assert variants == set(expected)
    assert "default" not in speed_switch
    assert actual == expected
    assert "this.walkCos = Mth.cos(walk);" in update
    assert "this.rootSin = Mth.sin(ageInTicks * rootSpeed);" in update


def test_mid_quantizes_age_while_near_keeps_it_and_far_is_static_in_layers():
    lod = _source("ExactAnimationLod.java")
    exact_layer = _source("ExactMobMeshLayer.java")
    allay_layer = _source("HelpingAllayExactLayer.java")

    assert "this == MID ? Mth.floor(ageInTicks * 0.5F) * 2.0F : ageInTicks" in lod
    for layer in (exact_layer, allay_layer):
        render = _method_body(layer, "public void render(")
        lod_selection = "ExactAnimationLod.at(entity.distanceToSqr(cameraPosition))"
        assert render.count(lod_selection) == 1
        assert render.count("animationLod.quantizedAge(ageInTicks)") == 1
        assert render.index(lod_selection) < render.index("animationLod.quantizedAge(ageInTicks)")

    assert "boolean animateSurface = animationLod != ExactAnimationLod.FAR;" in exact_layer
    exact_loop = _render_loop(exact_layer)
    static_pose_branch = _block_after(
        exact_loop,
        "if (animationLod == ExactAnimationLod.FAR || usesRigidRootAnimation()) {",
    )
    animated_pose_branch = _block_after(exact_loop, "} else {")
    assert _compact(static_pose_branch) == "animation = BonePose.ZERO;"
    assert "poseFor(" in animated_pose_branch
    assert "lodAgeInTicks" in _call(animated_pose_branch, "poseFor(")
    assert exact_loop.index("if (!animateSurface) {") < exact_loop.index("renderOctopusBone(")
    far_branch = _block_after(exact_loop, "if (!animateSurface) {")
    assert "this.mesh.renderBone(bone, poseStack, buffer, materialLight, overlay);" in far_branch
    assert not re.search(r"render(?!Bone)\w+Bone\(", far_branch)

    rigid_root_branch = _block_after(
        exact_layer[:exact_layer.index("for (String bone :")],
        "if (usesRigidRootAnimation()) {",
    )
    root_branch = _block_after(rigid_root_branch, "if (animateSurface) {")
    assert "poseStack.translate(0F, rigidRootYOffset(limbSwingAmount), 0F);" in root_branch

    assert "lodAgeInTicks" in _call(exact_loop, "poseFor(")
    for method in (
        "renderOctopusBone(",
        "renderSquidBone(",
        "renderGlowSquidBone(",
        "renderAxolotlBone(",
        "renderOcelotBone(",
        "renderBatBone(",
        "renderHumanoidBone(",
    ):
        assert "lodAgeInTicks" in _call(exact_loop, method)

    humanoid_branch = _block_after(
        exact_loop,
        "else if (this.variant == CustomMob3DModel.Variant.FROST_STRAY",
    )
    assert "boolean aimingBow =" in humanoid_branch
    assert "boolean aggressiveMelee =" in humanoid_branch
    humanoid_call = _call(humanoid_branch, "renderHumanoidBone(")
    assert re.search(r"rigPose\.inWater\(\),\s*aimingBow,\s*aggressiveMelee", humanoid_call)

    assert "ANIMATION_LOD_DISTANCE_SQUARED" not in exact_layer


def test_exact_rig_pose_is_a_reusable_mutable_primitive_pose_object():
    source = _source("ExactRigPose.java")

    assert "final class ExactRigPose" in source
    assert "void updateFor(CustomMob3DModel.Variant variant, LivingEntity entity," in source
    assert "Mth.sin(" in source
    assert "Mth.cos(" in source
    assert "new Vector3f" not in source
    assert not re.search(r"\b(Float|Double|Integer|Boolean|Vector3f)\s+\w+\s*[;=]", source)


def test_pose_update_happens_only_for_near_or_mid_before_the_render_loop():
    layer = _source("ExactMobMeshLayer.java")
    render = _method_body(layer, "public void render(")
    before_loop = render[:render.index("for (String bone :")]
    update_guard = _block_after(before_loop, "if (animateSurface) {")

    assert "private final ExactRigPose rigPose = new ExactRigPose();" in layer
    assert render.count("this.rigPose.updateFor(") == 1
    assert "this.rigPose.updateFor(" in update_guard
    assert "lodAgeInTicks" in _call(update_guard, "this.rigPose.updateFor(")
    assert before_loop.index("if (animateSurface) {") < before_loop.index("poseStack.pushPose();")
    loop = _render_loop(layer)
    assert "new Vector3f" not in loop
    assert "Mth.sin(" not in loop
    assert "Mth.cos(" not in loop


def test_allay_inner_render_loop_keeps_action_state_without_allocations_or_trig():
    layer = _source("HelpingAllayExactLayer.java")
    render = _method_body(layer, "public void render(")
    before_loop = render[:render.index("for (String bone :")]
    loop = _render_loop(layer)

    root_branch = _block_after(
        before_loop, "if (animationLod != ExactAnimationLod.FAR) {"
    )
    animated_branch = _block_after(
        loop, "if (animationLod != ExactAnimationLod.FAR) {"
    )
    far_branch = _block_after(loop, "} else {")
    animated_call = _call(animated_branch, "renderAllayBone(")

    assert "Mth.sin(lodAgeInTicks * 0.12F)" in root_branch
    assert "lodAgeInTicks" in animated_call
    assert "helpingAllay.action()" in animated_call
    assert "renderBone(" not in animated_branch
    assert "this.mesh.renderBone(bone, poseStack, buffer," in far_branch
    assert "renderAllayBone(" not in far_branch
    assert loop.index("if (animationLod != ExactAnimationLod.FAR) {") < loop.index("} else {")
    assert "new Vector3f" not in loop
    assert "Mth.sin(" not in loop
    assert "Mth.cos(" not in loop
