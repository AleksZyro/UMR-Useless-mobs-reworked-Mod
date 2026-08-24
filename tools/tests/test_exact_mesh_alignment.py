from pathlib import Path
import re

from tools.mob_tripo.exact_runtime import decode_mesh


ROOT = Path(__file__).resolve().parents[2]
MESH_ROOT = ROOT / "src/main/resources/assets/usless_mobs/meshes/entity/custom3d"


def _bounds(name: str):
    parts = decode_mesh((MESH_ROOT / f"{name}.mesh").read_bytes())
    positions = [corner[0] for part in parts.values() for face in part["faces"] for corner in face]
    return tuple(min(p[axis] for p in positions) for axis in range(3)), tuple(
        max(p[axis] for p in positions) for axis in range(3)
    )


def test_every_exact_mesh_uses_vanilla_floor_without_double_translation():
    source = (ROOT / "src/main/java/com/Momik/usless_mobs/client/ExactMobMeshLayer.java").read_text()
    allay = (ROOT / "src/main/java/com/Momik/usless_mobs/client/HelpingAllayExactLayer.java").read_text()

    # LivingEntityRenderer already applies Minecraft's standard 24-pixel floor
    # transform. Translating by maxY again lifts the complete model into the air;
    # scaled meshes instead compensate only the scale around that 1.5-block floor.
    assert "poseStack.translate(0F, -this.mesh.maxY(), 0F);" not in source
    assert "poseStack.translate(0F, -this.mesh.maxY(), 0F);" not in allay
    assert "float floorScaleY = modelScale;" in source
    assert "floorScaleY *= 0.55F;" in source
    assert "poseStack.translate(0F, 1.5F / floorScaleY - 1.5F, 0F);" in source
    assert "float modelScale = 1.35F;" in allay
    assert "poseStack.translate(0F, 1.5F / modelScale - 1.5F, 0F);" in allay

    for mesh_path in MESH_ROOT.glob("*.mesh"):
        _, maximum = _bounds(mesh_path.stem)
        assert abs(maximum[1] - 24.0) < 0.001, (
            mesh_path.stem,
            maximum[1],
            "exact meshes must retain Minecraft's vanilla Y=24 floor",
        )


def test_squid_hitbox_contains_the_scaled_exact_mesh():
    minimum, maximum = _bounds("squid")
    scale = 1.80
    visible_width = max(maximum[0] - minimum[0], maximum[2] - minimum[2]) * scale / 16
    visible_height = (maximum[1] - minimum[1]) * scale / 16
    entities = (ROOT / "src/main/java/com/Momik/usless_mobs/registry/ModEntities.java").read_text()

    assert visible_width < 2.60
    assert visible_height < 1.30
    assert ".sized(2.60F, 1.30F)" in entities


def test_glow_squid_hitbox_contains_the_scaled_exact_mesh():
    minimum, maximum = _bounds("glow_squid")
    scale = 1.15
    visible_width = max(maximum[0] - minimum[0], maximum[2] - minimum[2]) * scale / 16
    visible_height = (maximum[1] - minimum[1]) * scale / 16
    entities = (ROOT / "src/main/java/com/Momik/usless_mobs/registry/ModEntities.java").read_text()

    assert visible_width <= 1.45
    assert visible_height <= 1.80
    assert ".sized(1.45F, 1.80F)" in entities


def test_continuous_exact_meshes_have_no_unweighted_per_region_rotation():
    source = (ROOT / "src/main/java/com/Momik/usless_mobs/client/ExactMobMeshLayer.java").read_text()
    assert "case LIVING_BOSS, WEB_CAVE_SPIDER, OCTOPUS, SQUID, GLOW_SQUID," in source
    assert "WITCH_BOSS, LIVING_BAT, ROOTED_HUSK, POLAR_BEAR, FROST_STRAY," in source
    assert "CORAL_DROWNED -> true" in source
    assert "AXOLOTL, OCELOT -> true" in source
    assert "if (usesRigidRootAnimation())" in source

    allay = (ROOT / "src/main/java/com/Momik/usless_mobs/client/HelpingAllayExactLayer.java").read_text()
    assert "BonePose animation = BonePose.ZERO;" in allay


def test_squid_uses_continuous_surface_swim_deformation_without_region_rotation():
    mesh = (ROOT / "src/main/java/com/Momik/usless_mobs/client/ExactMobMesh.java").read_text()
    layer = (ROOT / "src/main/java/com/Momik/usless_mobs/client/ExactMobMeshLayer.java").read_text()

    assert "void renderSquidBone(" in mesh
    assert "deformSquidVertex(" in mesh
    assert "float tipWeight" in mesh
    assert "this.mesh.renderSquidBone(" in layer
    assert "entity.isInWater()" in layer
    assert "case LIVING_BOSS, WEB_CAVE_SPIDER, OCTOPUS, SQUID, GLOW_SQUID," in layer


def test_octopus_uses_continuous_eight_arm_action_deformation():
    mesh = (ROOT / "src/main/java/com/Momik/usless_mobs/client/ExactMobMesh.java").read_text()
    layer = (ROOT / "src/main/java/com/Momik/usless_mobs/client/ExactMobMeshLayer.java").read_text()

    assert "void renderOctopusBone(" in mesh
    assert "deformOctopusVertex(" in mesh
    assert "float tentacleTipWeight" in mesh
    assert "OctopusEntity.ACTION_GRAB" in mesh
    assert "OctopusEntity.ACTION_INK" in mesh
    assert "this.mesh.renderOctopusBone(" in layer


def test_octopus_squeezed_visuals_fit_the_synchronised_small_hitbox():
    layer = (ROOT / "src/main/java/com/Momik/usless_mobs/client/ExactMobMeshLayer.java").read_text()

    assert "poseStack.scale(0.49F, 0.55F, 0.49F)" in layer


def test_witch_boss_uses_seam_safe_humanoid_deformation():
    layer = (ROOT / "src/main/java/com/Momik/usless_mobs/client/ExactMobMeshLayer.java").read_text()

    assert "|| this.variant == CustomMob3DModel.Variant.WITCH_BOSS" in layer
    assert "this.mesh.renderHumanoidBone(" in layer


def test_living_bat_uses_seam_safe_wing_and_tip_deformation():
    mesh = (ROOT / "src/main/java/com/Momik/usless_mobs/client/ExactMobMesh.java").read_text()
    layer = (ROOT / "src/main/java/com/Momik/usless_mobs/client/ExactMobMeshLayer.java").read_text()

    assert "void renderBatBone(" in mesh
    assert "deformBatVertex(" in mesh
    assert "float wingTipWeight" in mesh
    assert "this.mesh.renderBatBone(" in layer
    assert "bat.isResting()" in layer


def test_rooted_husk_uses_seam_safe_humanoid_walk_and_attack_deformation():
    mesh = (ROOT / "src/main/java/com/Momik/usless_mobs/client/ExactMobMesh.java").read_text()
    layer = (ROOT / "src/main/java/com/Momik/usless_mobs/client/ExactMobMeshLayer.java").read_text()

    assert "void renderHumanoidBone(" in mesh
    assert "|| this.variant == CustomMob3DModel.Variant.ROOTED_HUSK)" in layer
    assert "entity instanceof RootedHuskEntity husk && husk.isAggressive()" in layer


def test_glow_squid_uses_vertical_continuous_tentacle_deformation():
    mesh = (ROOT / "src/main/java/com/Momik/usless_mobs/client/ExactMobMesh.java").read_text()
    layer = (ROOT / "src/main/java/com/Momik/usless_mobs/client/ExactMobMeshLayer.java").read_text()

    assert "void renderGlowSquidBone(" in mesh
    assert "deformGlowSquidVertex(" in mesh
    assert "this.mesh.renderGlowSquidBone(" in layer
    assert "CustomMob3DModel.Variant.GLOW_SQUID" in layer


def test_polar_bear_uses_continuous_walk_deformation_without_split_seams():
    mesh = (ROOT / "src/main/java/com/Momik/usless_mobs/client/ExactMobMesh.java").read_text()
    layer = (ROOT / "src/main/java/com/Momik/usless_mobs/client/ExactMobMeshLayer.java").read_text()

    assert "void renderQuadrupedBone(" in mesh
    assert "deformQuadrupedVertex(" in mesh
    assert "float pawWeight" in mesh
    assert "this.mesh.renderQuadrupedBone(" in layer
    assert "CustomMob3DModel.Variant.POLAR_BEAR" in layer


def test_living_boss_uses_continuous_quadruped_walk_deformation_without_split_seams():
    mesh = (ROOT / "src/main/java/com/Momik/usless_mobs/client/ExactMobMesh.java").read_text()
    layer = (ROOT / "src/main/java/com/Momik/usless_mobs/client/ExactMobMeshLayer.java").read_text()

    assert "void renderQuadrupedBone(" in mesh
    assert "deformQuadrupedVertex(" in mesh
    assert "this.mesh.renderQuadrupedBone(" in layer
    assert "this.variant == CustomMob3DModel.Variant.LIVING_BOSS" in layer


def test_frost_stray_uses_continuous_humanoid_deformation_and_corrected_forward_axis():
    mesh = (ROOT / "src/main/java/com/Momik/usless_mobs/client/ExactMobMesh.java").read_text()
    layer = (ROOT / "src/main/java/com/Momik/usless_mobs/client/ExactMobMeshLayer.java").read_text()

    assert "void renderHumanoidBone(" in mesh
    assert "deformHumanoidVertex(" in mesh
    assert "this.mesh.renderHumanoidBone(" in layer
    assert "case FROST_STRAY -> 180F" in layer


def test_frost_stray_visible_arms_follow_its_aggressive_bow_pose():
    mesh = (ROOT / "src/main/java/com/Momik/usless_mobs/client/ExactMobMesh.java").read_text()
    layer = (ROOT / "src/main/java/com/Momik/usless_mobs/client/ExactMobMeshLayer.java").read_text()

    assert "entity instanceof AbstractSkeleton skeleton && skeleton.isAggressive()" in layer
    assert "boolean aimingBow" in mesh
    assert "aimingBow ?" in mesh


def test_web_cave_spider_uses_continuous_eight_leg_walk_deformation():
    mesh = (ROOT / "src/main/java/com/Momik/usless_mobs/client/ExactMobMesh.java").read_text()
    layer = (ROOT / "src/main/java/com/Momik/usless_mobs/client/ExactMobMeshLayer.java").read_text()

    assert "void renderSpiderBone(" in mesh
    assert "deformSpiderVertex(" in mesh
    assert "float legTipWeight" in mesh
    assert "this.mesh.renderSpiderBone(" in layer
    assert "this.variant == CustomMob3DModel.Variant.WEB_CAVE_SPIDER" in layer


def test_coral_drowned_uses_continuous_humanoid_swim_deformation():
    mesh = (ROOT / "src/main/java/com/Momik/usless_mobs/client/ExactMobMesh.java").read_text()
    layer = (ROOT / "src/main/java/com/Momik/usless_mobs/client/ExactMobMeshLayer.java").read_text()

    assert "void renderHumanoidBone(" in mesh
    assert "boolean swimming" in mesh
    assert "CustomMob3DModel.Variant.CORAL_DROWNED" in layer
    assert "entity.isInWater()" in layer
    assert "case CORAL_DROWNED -> 90F" in layer
    assert "case CORAL_DROWNED -> -90F" not in layer


def test_coral_drowned_visible_arms_follow_its_aggressive_attack_pose():
    mesh = (ROOT / "src/main/java/com/Momik/usless_mobs/client/ExactMobMesh.java").read_text()
    layer = (ROOT / "src/main/java/com/Momik/usless_mobs/client/ExactMobMeshLayer.java").read_text()

    assert "boolean aggressiveMelee" in mesh
    assert "aggressiveMelee ?" in mesh
    assert "entity instanceof CoralDrownedEntity drowned && drowned.isAggressive()" in layer


def test_axolotl_uses_continuous_tail_leg_and_gill_deformation():
    mesh = (ROOT / "src/main/java/com/Momik/usless_mobs/client/ExactMobMesh.java").read_text()
    layer = (ROOT / "src/main/java/com/Momik/usless_mobs/client/ExactMobMeshLayer.java").read_text()

    assert "void renderAxolotlBone(" in mesh
    assert "deformAxolotlVertex(" in mesh
    assert "float tailWeight" in mesh
    assert "float legWeight" in mesh
    assert "float gillWeight" in mesh
    assert "this.mesh.renderAxolotlBone(" in layer
    assert "CustomMob3DModel.Variant.AXOLOTL" in layer


def test_axolotl_gill_field_reaches_the_six_external_head_branches():
    source = (ROOT / "src/main/java/com/Momik/usless_mobs/client/ExactMobMesh.java").read_text()
    parts = decode_mesh((MESH_ROOT / "axolotl.mesh").read_bytes())
    positions = [
        tuple(coordinate / 16.0 for coordinate in corner[0])
        for part in parts.values()
        for face in part["faces"]
        for corner in face
    ]

    def clamp(value):
        return max(0.0, min(1.0, value))

    # The generated model's six gills are its farthest lateral surfaces in a
    # narrow band at the head/body junction. The runtime field must strongly
    # reach those branches while excluding the face in front of z=-0.36.
    weights = [
        clamp((abs(x) - 0.24) / 0.09)
        * clamp((z + 0.38) / 0.10)
        * clamp((-z - 0.12) / 0.10)
        * clamp((y - 1.02) / 0.12)
        for x, y, z in positions
    ]

    strongly_animated = [
        position for position, weight in zip(positions, weights) if weight >= 0.75
    ]
    assert max(weights) == 1.0
    assert len(strongly_animated) >= len(positions) * 0.05
    assert all(position[2] > -0.36 for position in strongly_animated)
    assert "(Math.abs(x) - 0.24F) / 0.09F" in source
    assert "(z + 0.38F) / 0.10F" in source
    assert "(-z - 0.12F) / 0.10F" in source
    assert "(y - 1.02F) / 0.12F" in source


def test_ocelot_has_continuous_leg_tail_head_and_pounce_deformation():
    mesh = (ROOT / "src/main/java/com/Momik/usless_mobs/client/ExactMobMesh.java").read_text()
    layer = (ROOT / "src/main/java/com/Momik/usless_mobs/client/ExactMobMeshLayer.java").read_text()

    assert "void renderOcelotBone(" in mesh
    assert "deformOcelotVertex(" in mesh
    assert "float tailWeight" in mesh
    assert "float legWeight" in mesh
    assert "float pounceStretch" in mesh
    assert "float headWeight" in mesh
    assert "this.mesh.renderOcelotBone(" in layer
    assert "CustomMob3DModel.Variant.OCELOT" in layer


def test_runtime_loader_accepts_verified_high_detail_tripo_regions():
    source = (ROOT / "src/main/java/com/Momik/usless_mobs/client/ExactMobMesh.java").read_text()
    glow_report = __import__("json").loads(
        (MESH_ROOT / "glow_squid.report.json").read_text(encoding="utf-8")
    )
    largest_verified_region = max(glow_report["bones"].values())

    assert largest_verified_region == 463283
    assert "MAX_FACES_PER_BONE = 1_000_000" in source


def test_all_exact_mob_hitboxes_contain_their_scaled_static_meshes():
    # Minecraft hitboxes are square in X/Z, so width must cover the longer
    # horizontal visual axis. Values mirror the renderer scales and registry.
    contracts = {
        "living_boss": (1.35, 3.70, 2.95),
        "web_cave_spider": (1.80, 1.30, 0.56),
        "octopus": (1.40, 1.50, 1.40),
        "witch_boss": (1.00, 1.15, 1.95),
        "helping_allay": (1.35, 0.95, 0.90),
        "squid": (1.80, 2.60, 1.30),
        "glow_squid": (1.15, 1.45, 1.80),
        "living_bat": (1.60, 0.90, 0.65),
        "rooted_husk": (1.10, 1.10, 2.15),
        "polar_bear": (1.00, 1.90, 1.40),
        "frost_stray": (1.00, 1.10, 1.95),
        "coral_drowned": (1.00, 1.40, 1.95),
        "axolotl": (1.00, 1.35, 0.65),
        "ocelot": (1.00, 1.45, 0.90),
    }
    registry_names = {
        "squid": "living_squid",
        "glow_squid": "living_glow_squid",
        "polar_bear": "living_polar_bear",
        "axolotl": "living_axolotl",
        "ocelot": "living_ocelot",
    }

    entities = (ROOT / "src/main/java/com/Momik/usless_mobs/registry/ModEntities.java").read_text()

    for name, (scale, hitbox_width, hitbox_height) in contracts.items():
        minimum, maximum = _bounds(name)
        visual_width = max(maximum[0] - minimum[0], maximum[2] - minimum[2]) * scale / 16
        visual_height = (maximum[1] - minimum[1]) * scale / 16
        assert visual_width <= hitbox_width + 0.01, (name, visual_width, hitbox_width)
        assert visual_height <= hitbox_height + 0.01, (name, visual_height, hitbox_height)

        registry_name = registry_names.get(name, name)
        registry_match = re.search(
            rf'ENTITY_TYPES\.register\("{registry_name}"[\s\S]*?\.sized\(([0-9.]+)F, ([0-9.]+)F\)',
            entities,
        )
        assert registry_match is not None, (name, "missing registered entity dimensions")
        assert float(registry_match.group(1)) == hitbox_width, (name, registry_match.group(1), hitbox_width)
        assert float(registry_match.group(2)) == hitbox_height, (name, registry_match.group(2), hitbox_height)


def test_web_cave_spider_has_distinct_elite_scale_hitbox_and_shadow():
    layer = (ROOT / "src/main/java/com/Momik/usless_mobs/client/ExactMobMeshLayer.java").read_text()
    entities = (ROOT / "src/main/java/com/Momik/usless_mobs/registry/ModEntities.java").read_text()
    renderer = (ROOT / "src/main/java/com/Momik/usless_mobs/client/WebCaveSpiderRenderer.java").read_text()

    assert "case WEB_CAVE_SPIDER -> 1.80F;" in layer
    assert ".sized(1.30F, 0.56F)" in entities
    assert "this.shadowRadius = 0.65F;" in renderer


def test_living_boss_has_boss_scale_matching_hitbox_and_shadow():
    layer = (ROOT / "src/main/java/com/Momik/usless_mobs/client/ExactMobMeshLayer.java").read_text()
    entities = (ROOT / "src/main/java/com/Momik/usless_mobs/registry/ModEntities.java").read_text()
    renderer = (ROOT / "src/main/java/com/Momik/usless_mobs/client/LivingBossRenderer.java").read_text()

    assert "case LIVING_BOSS -> 1.35F;" in layer
    assert ".sized(3.70F, 2.95F)" in entities
    assert "this.shadowRadius = 1.45F;" in renderer
