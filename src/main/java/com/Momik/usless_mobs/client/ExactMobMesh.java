package com.Momik.usless_mobs.client;

import com.Momik.usless_mobs.entity.HelpingAllayEntity;
import com.Momik.usless_mobs.entity.OctopusEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.Momik.usless_mobs.Usless_mobs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Loads losslessly exported textured Tripo triangles for the animated mob layer. */
final class ExactMobMesh {
    private static final byte[] MAGIC = new byte[]{'U', 'M', 'M', 'E', 'S', 'H', '1', 0};
    private static final int MAX_BONES = 32;
    // Verified 4K Tripo sources can legitimately contain more than 400k faces
    // in one continuous shell (Glow Squid body: 463,283). Keep a finite upper
    // bound for malformed resources while accepting the approved source set.
    private static final int MAX_FACES_PER_BONE = 1_000_000;

    private final ResourceLocation resource;
    private final Map<String, MeshPart> parts;

    private ExactMobMesh(ResourceLocation resource, Map<String, MeshPart> parts) {
        this.resource = resource;
        this.parts = Map.copyOf(parts);
    }

    static ExactMobMesh load(ResourceManager resourceManager, String mobName, Set<String> expectedBones)
            throws IOException {
        ResourceLocation location = ResourceLocation.tryBuild(
                Usless_mobs.MODID, "meshes/entity/custom3d/" + mobName + ".mesh");
        if (location == null) {
            throw new IOException("Invalid exact Tripo mesh resource name: " + mobName);
        }
        Resource resource = resourceManager.getResource(location)
                .orElseThrow(() -> new IOException("Missing exact Tripo mesh resource: " + location));
        try (DataInputStream input = new DataInputStream(new BufferedInputStream(resource.open()))) {
            if (!Arrays.equals(input.readNBytes(MAGIC.length), MAGIC)) {
                throw new IOException("Invalid exact Tripo mesh header: " + location);
            }
            int boneCount = readIntLe(input);
            if (boneCount <= 0 || boneCount > MAX_BONES) {
                throw new IOException("Invalid exact Tripo mesh bone count: " + boneCount);
            }
            Map<String, MeshPart> parts = new HashMap<>();
            for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
                int nameLength = readUnsignedShortLe(input);
                if (nameLength <= 0 || nameLength > 128) {
                    throw new IOException("Invalid exact Tripo mesh bone name length: " + nameLength);
                }
                String name = new String(input.readNBytes(nameLength), StandardCharsets.UTF_8);
                if (!expectedBones.contains(name) || parts.containsKey(name)) {
                    throw new IOException("Invalid or duplicate exact Tripo mesh bone: " + name);
                }
                Vector3f pivot = new Vector3f(
                        readFiniteFloatLe(input), readFiniteFloatLe(input), readFiniteFloatLe(input));
                int faceCount = readIntLe(input);
                if (faceCount < 0 || faceCount > MAX_FACES_PER_BONE) {
                    throw new IOException("Invalid exact Tripo face count for " + name + ": " + faceCount);
                }
                parts.put(name, readPart(input, pivot, faceCount));
            }
            if (!parts.keySet().equals(expectedBones)) {
                throw new IOException("Exact Tripo mesh bone set does not match its animation rig");
            }
            if (input.read() != -1) {
                throw new IOException("Exact Tripo mesh contains trailing bytes: " + location);
            }
            return new ExactMobMesh(location, parts);
        } catch (EOFException exception) {
            throw new IOException("Exact Tripo mesh is truncated: " + location, exception);
        } catch (ArithmeticException exception) {
            throw new IOException("Exact Tripo mesh count overflow: " + location, exception);
        }
    }

    Set<String> boneNames() {
        return this.parts.keySet();
    }

    Vector3f pivot(String boneName) {
        MeshPart part = this.parts.get(boneName);
        if (part == null) {
            throw new IllegalArgumentException("Unknown exact Tripo bone " + boneName + " in " + this.resource);
        }
        return new Vector3f(part.pivot());
    }

    void renderBone(String boneName, PoseStack poseStack, VertexConsumer buffer,
                    int packedLight, int packedOverlay) {
        MeshPart part = this.parts.get(boneName);
        if (part == null || part.faceCount() == 0) {
            return;
        }
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();
        float[] data = part.data();
        int cursor = 0;
        Vector3f normal = new Vector3f();
        for (int face = 0; face < part.faceCount(); face++) {
            int verticesStart = cursor;
            int normalStart = cursor + 15;
            normal.set(data[normalStart], data[normalStart + 1], data[normalStart + 2]);
            normalMatrix.transform(normal);
            emitVertex(data, verticesStart, pose, normal, buffer, packedLight, packedOverlay);
            emitVertex(data, verticesStart + 5, pose, normal, buffer, packedLight, packedOverlay);
            emitVertex(data, verticesStart + 10, pose, normal, buffer, packedLight, packedOverlay);
            emitVertex(data, verticesStart + 10, pose, normal, buffer, packedLight, packedOverlay);
            cursor += 18;
        }
    }

    /**
     * Moves the unweighted Helping Allay as one continuous surface. The weights
     * depend only on source position, so coincident vertices at region borders
     * always receive the same transform and cannot open visible cracks.
     */
    void renderAllayBone(String boneName, PoseStack poseStack, VertexConsumer buffer,
                         int packedLight, int packedOverlay, float ageInTicks,
                         float netHeadYaw, float headPitch, byte actionState) {
        MeshPart part = this.parts.get(boneName);
        if (part == null || part.faceCount() == 0) {
            return;
        }
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();
        float[] data = part.data();
        Vector3f normal = new Vector3f();
        Vector3f edge = new Vector3f();
        Vector3f first = new Vector3f();
        Vector3f second = new Vector3f();
        Vector3f third = new Vector3f();
        Vector3f original = new Vector3f();
        Vector3f temporary = new Vector3f();
        int cursor = 0;
        for (int face = 0; face < part.faceCount(); face++) {
            int verticesStart = cursor;
            deformAllayVertex(data, verticesStart, ageInTicks, netHeadYaw,
                    headPitch, actionState, first, original, temporary);
            deformAllayVertex(data, verticesStart + 5, ageInTicks, netHeadYaw,
                    headPitch, actionState, second, original, temporary);
            deformAllayVertex(data, verticesStart + 10, ageInTicks, netHeadYaw,
                    headPitch, actionState, third, original, temporary);
            calculateFaceNormal(first, second, third, normal, edge);
            normalMatrix.transform(normal);
            emitVertex(data, verticesStart, first, pose, normal, buffer, packedLight, packedOverlay);
            emitVertex(data, verticesStart + 5, second, pose, normal, buffer, packedLight, packedOverlay);
            emitVertex(data, verticesStart + 10, third, pose, normal, buffer, packedLight, packedOverlay);
            emitVertex(data, verticesStart + 10, third, pose, normal, buffer, packedLight, packedOverlay);
            cursor += 18;
        }
    }

    /**
     * Applies one continuous position-based swim field to every squid region.
     * Coincident seam vertices therefore receive exactly the same displacement,
     * unlike independent rigid bone rotations on this unweighted Tripo surface.
     */
    void renderSquidBone(String boneName, PoseStack poseStack, VertexConsumer buffer,
                         int packedLight, int packedOverlay, float ageInTicks,
                         boolean inWater) {
        MeshPart part = this.parts.get(boneName);
        if (part == null || part.faceCount() == 0) {
            return;
        }
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();
        float[] data = part.data();
        float strength = inWater ? 1.0F : 0.18F;
        float waveSin = Mth.sin(ageInTicks * 0.18F);
        float waveCos = Mth.cos(ageInTicks * 0.145F);
        Vector3f normal = new Vector3f();
        Vector3f edge = new Vector3f();
        Vector3f first = new Vector3f();
        Vector3f second = new Vector3f();
        Vector3f third = new Vector3f();
        int cursor = 0;
        for (int face = 0; face < part.faceCount(); face++) {
            int verticesStart = cursor;
            deformSquidVertex(data, verticesStart, waveSin, waveCos, strength, first);
            deformSquidVertex(data, verticesStart + 5, waveSin, waveCos, strength, second);
            deformSquidVertex(data, verticesStart + 10, waveSin, waveCos, strength, third);
            calculateFaceNormal(first, second, third, normal, edge);
            normalMatrix.transform(normal);
            emitVertex(data, verticesStart, first, pose, normal, buffer, packedLight, packedOverlay);
            emitVertex(data, verticesStart + 5, second, pose, normal, buffer, packedLight, packedOverlay);
            emitVertex(data, verticesStart + 10, third, pose, normal, buffer, packedLight, packedOverlay);
            emitVertex(data, verticesStart + 10, third, pose, normal, buffer, packedLight, packedOverlay);
            cursor += 18;
        }
    }

    /** Continuous swim field for the upright glow-squid source (+Y mantle, -Y arms). */
    void renderGlowSquidBone(String boneName, PoseStack poseStack, VertexConsumer buffer,
                             int packedLight, int packedOverlay, float ageInTicks,
                             boolean inWater) {
        MeshPart part = this.parts.get(boneName);
        if (part == null || part.faceCount() == 0) {
            return;
        }
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();
        float[] data = part.data();
        float strength = inWater ? 1.0F : 0.18F;
        float waveSin = Mth.sin(ageInTicks * 0.20F);
        float waveCos = Mth.cos(ageInTicks * 0.155F);
        Vector3f normal = new Vector3f();
        Vector3f edge = new Vector3f();
        Vector3f first = new Vector3f();
        Vector3f second = new Vector3f();
        Vector3f third = new Vector3f();
        int cursor = 0;
        for (int face = 0; face < part.faceCount(); face++) {
            int verticesStart = cursor;
            deformGlowSquidVertex(data, verticesStart, waveSin, waveCos, strength, first);
            deformGlowSquidVertex(data, verticesStart + 5, waveSin, waveCos, strength, second);
            deformGlowSquidVertex(data, verticesStart + 10, waveSin, waveCos, strength, third);
            calculateFaceNormal(first, second, third, normal, edge);
            normalMatrix.transform(normal);
            emitVertex(data, verticesStart, first, pose, normal, buffer, packedLight, packedOverlay);
            emitVertex(data, verticesStart + 5, second, pose, normal, buffer, packedLight, packedOverlay);
            emitVertex(data, verticesStart + 10, third, pose, normal, buffer, packedLight, packedOverlay);
            emitVertex(data, verticesStart + 10, third, pose, normal, buffer, packedLight, packedOverlay);
            cursor += 18;
        }
    }

    /** Seam-safe eight-arm field for the complete unweighted Tripo octopus. */
    void renderOctopusBone(String boneName, PoseStack poseStack, VertexConsumer buffer,
                            int packedLight, int packedOverlay, float ageInTicks,
                            boolean inWater, byte actionState, boolean squeezing) {
        MeshPart part = this.parts.get(boneName);
        if (part == null || part.faceCount() == 0) {
            return;
        }
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();
        float[] data = part.data();
        Vector3f normal = new Vector3f();
        Vector3f edge = new Vector3f();
        Vector3f first = new Vector3f();
        Vector3f second = new Vector3f();
        Vector3f third = new Vector3f();
        int cursor = 0;
        for (int face = 0; face < part.faceCount(); face++) {
            int verticesStart = cursor;
            deformOctopusVertex(data, verticesStart, ageInTicks,
                    inWater, actionState, squeezing, first);
            deformOctopusVertex(data, verticesStart + 5, ageInTicks,
                    inWater, actionState, squeezing, second);
            deformOctopusVertex(data, verticesStart + 10, ageInTicks,
                    inWater, actionState, squeezing, third);
            calculateFaceNormal(first, second, third, normal, edge);
            normalMatrix.transform(normal);
            emitVertex(data, verticesStart, first, pose, normal, buffer, packedLight, packedOverlay);
            emitVertex(data, verticesStart + 5, second, pose, normal, buffer, packedLight, packedOverlay);
            emitVertex(data, verticesStart + 10, third, pose, normal, buffer, packedLight, packedOverlay);
            emitVertex(data, verticesStart + 10, third, pose, normal, buffer, packedLight, packedOverlay);
            cursor += 18;
        }
    }

    /** Seam-safe swimming field for the complete unweighted Axolotl surface. */
    void renderAxolotlBone(String boneName, PoseStack poseStack, VertexConsumer buffer,
                           int packedLight, int packedOverlay, float ageInTicks,
                           float limbSwing, float limbSwingAmount, boolean inWater) {
        MeshPart part = this.parts.get(boneName);
        if (part == null || part.faceCount() == 0) {
            return;
        }
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();
        float[] data = part.data();
        Vector3f normal = new Vector3f();
        Vector3f edge = new Vector3f();
        Vector3f first = new Vector3f();
        Vector3f second = new Vector3f();
        Vector3f third = new Vector3f();
        int cursor = 0;
        for (int face = 0; face < part.faceCount(); face++) {
            int verticesStart = cursor;
            deformAxolotlVertex(data, verticesStart, ageInTicks,
                    limbSwing, limbSwingAmount, inWater, first);
            deformAxolotlVertex(data, verticesStart + 5, ageInTicks,
                    limbSwing, limbSwingAmount, inWater, second);
            deformAxolotlVertex(data, verticesStart + 10, ageInTicks,
                    limbSwing, limbSwingAmount, inWater, third);
            calculateFaceNormal(first, second, third, normal, edge);
            normalMatrix.transform(normal);
            emitVertex(data, verticesStart, first, pose, normal, buffer, packedLight, packedOverlay);
            emitVertex(data, verticesStart + 5, second, pose, normal, buffer, packedLight, packedOverlay);
            emitVertex(data, verticesStart + 10, third, pose, normal, buffer, packedLight, packedOverlay);
            emitVertex(data, verticesStart + 10, third, pose, normal, buffer, packedLight, packedOverlay);
            cursor += 18;
        }
    }

    /** Seam-safe feline walk, tail, look and pounce field for the exact Ocelot surface. */
    void renderOcelotBone(String boneName, PoseStack poseStack, VertexConsumer buffer,
                          int packedLight, int packedOverlay, float ageInTicks,
                          float limbSwing, float limbSwingAmount, float netHeadYaw,
                          float headPitch, boolean sprinting, boolean airborne) {
        MeshPart part = this.parts.get(boneName);
        if (part == null || part.faceCount() == 0) {
            return;
        }
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();
        float[] data = part.data();
        Vector3f normal = new Vector3f();
        Vector3f edge = new Vector3f();
        Vector3f first = new Vector3f();
        Vector3f second = new Vector3f();
        Vector3f third = new Vector3f();
        int cursor = 0;
        for (int face = 0; face < part.faceCount(); face++) {
            int verticesStart = cursor;
            deformOcelotVertex(data, verticesStart, ageInTicks, limbSwing,
                    limbSwingAmount, netHeadYaw, headPitch, sprinting, airborne, first);
            deformOcelotVertex(data, verticesStart + 5, ageInTicks, limbSwing,
                    limbSwingAmount, netHeadYaw, headPitch, sprinting, airborne, second);
            deformOcelotVertex(data, verticesStart + 10, ageInTicks, limbSwing,
                    limbSwingAmount, netHeadYaw, headPitch, sprinting, airborne, third);
            calculateFaceNormal(first, second, third, normal, edge);
            normalMatrix.transform(normal);
            emitVertex(data, verticesStart, first, pose, normal, buffer, packedLight, packedOverlay);
            emitVertex(data, verticesStart + 5, second, pose, normal, buffer, packedLight, packedOverlay);
            emitVertex(data, verticesStart + 10, third, pose, normal, buffer, packedLight, packedOverlay);
            emitVertex(data, verticesStart + 10, third, pose, normal, buffer, packedLight, packedOverlay);
            cursor += 18;
        }
    }

    /** Seam-safe diagonal gait for the complete unweighted spider surface. */
    void renderSpiderBone(String boneName, PoseStack poseStack, VertexConsumer buffer,
                          int packedLight, int packedOverlay, float limbSwing,
                          float limbSwingAmount) {
        MeshPart part = this.parts.get(boneName);
        if (part == null || part.faceCount() == 0) {
            return;
        }
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();
        float[] data = part.data();
        Vector3f normal = new Vector3f();
        Vector3f edge = new Vector3f();
        Vector3f first = new Vector3f();
        Vector3f second = new Vector3f();
        Vector3f third = new Vector3f();
        int cursor = 0;
        for (int face = 0; face < part.faceCount(); face++) {
            int verticesStart = cursor;
            deformSpiderVertex(data, verticesStart, limbSwing, limbSwingAmount, first);
            deformSpiderVertex(data, verticesStart + 5, limbSwing, limbSwingAmount, second);
            deformSpiderVertex(data, verticesStart + 10, limbSwing, limbSwingAmount, third);
            calculateFaceNormal(first, second, third, normal, edge);
            normalMatrix.transform(normal);
            emitVertex(data, verticesStart, first, pose, normal, buffer, packedLight, packedOverlay);
            emitVertex(data, verticesStart + 5, second, pose, normal, buffer, packedLight, packedOverlay);
            emitVertex(data, verticesStart + 10, third, pose, normal, buffer, packedLight, packedOverlay);
            emitVertex(data, verticesStart + 10, third, pose, normal, buffer, packedLight, packedOverlay);
            cursor += 18;
        }
    }

    /**
     * Deforms a complete unweighted quadruped surface with one position field.
     * The same source position always receives the same movement, even when
     * neighbouring triangles belong to different export regions.
     */
    void renderQuadrupedBone(String boneName, PoseStack poseStack, VertexConsumer buffer,
                             int packedLight, int packedOverlay, float limbSwing,
                             float limbSwingAmount, float netHeadYaw, float headPitch) {
        MeshPart part = this.parts.get(boneName);
        if (part == null || part.faceCount() == 0) {
            return;
        }
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();
        float[] data = part.data();
        Vector3f normal = new Vector3f();
        Vector3f edge = new Vector3f();
        Vector3f first = new Vector3f();
        Vector3f second = new Vector3f();
        Vector3f third = new Vector3f();
        int cursor = 0;
        for (int face = 0; face < part.faceCount(); face++) {
            int verticesStart = cursor;
            deformQuadrupedVertex(data, verticesStart, limbSwing, limbSwingAmount,
                    netHeadYaw, headPitch, first);
            deformQuadrupedVertex(data, verticesStart + 5, limbSwing, limbSwingAmount,
                    netHeadYaw, headPitch, second);
            deformQuadrupedVertex(data, verticesStart + 10, limbSwing, limbSwingAmount,
                    netHeadYaw, headPitch, third);
            calculateFaceNormal(first, second, third, normal, edge);
            normalMatrix.transform(normal);
            emitVertex(data, verticesStart, first, pose, normal, buffer, packedLight, packedOverlay);
            emitVertex(data, verticesStart + 5, second, pose, normal, buffer, packedLight, packedOverlay);
            emitVertex(data, verticesStart + 10, third, pose, normal, buffer, packedLight, packedOverlay);
            emitVertex(data, verticesStart + 10, third, pose, normal, buffer, packedLight, packedOverlay);
            cursor += 18;
        }
    }

    /**
     * Walks the complete unweighted humanoid surface with one continuous field.
     * Position-derived weights keep shared seam vertices coincident while still
     * giving the arms, legs and head visible motion.
     */
    void renderHumanoidBone(String boneName, PoseStack poseStack, VertexConsumer buffer,
                            int packedLight, int packedOverlay, float limbSwing,
                            float limbSwingAmount, float ageInTicks, float netHeadYaw,
                            float headPitch, boolean swimming, boolean aimingBow,
                            boolean aggressiveMelee) {
        MeshPart part = this.parts.get(boneName);
        if (part == null || part.faceCount() == 0) {
            return;
        }
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();
        float[] data = part.data();
        Vector3f normal = new Vector3f();
        Vector3f edge = new Vector3f();
        Vector3f first = new Vector3f();
        Vector3f second = new Vector3f();
        Vector3f third = new Vector3f();
        Vector3f original = new Vector3f();
        Vector3f temporary = new Vector3f();
        int cursor = 0;
        for (int face = 0; face < part.faceCount(); face++) {
            int verticesStart = cursor;
            deformHumanoidVertex(data, verticesStart, limbSwing, limbSwingAmount,
                    ageInTicks, netHeadYaw, headPitch, swimming, aimingBow, aggressiveMelee,
                    first, original, temporary);
            deformHumanoidVertex(data, verticesStart + 5, limbSwing, limbSwingAmount,
                    ageInTicks, netHeadYaw, headPitch, swimming, aimingBow, aggressiveMelee,
                    second, original, temporary);
            deformHumanoidVertex(data, verticesStart + 10, limbSwing, limbSwingAmount,
                    ageInTicks, netHeadYaw, headPitch, swimming, aimingBow, aggressiveMelee,
                    third, original, temporary);
            calculateFaceNormal(first, second, third, normal, edge);
            normalMatrix.transform(normal);
            emitVertex(data, verticesStart, first, pose, normal, buffer, packedLight, packedOverlay);
            emitVertex(data, verticesStart + 5, second, pose, normal, buffer, packedLight, packedOverlay);
            emitVertex(data, verticesStart + 10, third, pose, normal, buffer, packedLight, packedOverlay);
            emitVertex(data, verticesStart + 10, third, pose, normal, buffer, packedLight, packedOverlay);
            cursor += 18;
        }
    }

    /**
     * Flaps the complete unweighted bat surface with one position-based field.
     * Shared vertices at the body, wing and wing-tip borders therefore receive
     * identical transforms and cannot open the seams present in a rigid rig.
     */
    void renderBatBone(String boneName, PoseStack poseStack, VertexConsumer buffer,
                       int packedLight, int packedOverlay, float ageInTicks,
                       boolean resting) {
        MeshPart part = this.parts.get(boneName);
        if (part == null || part.faceCount() == 0) {
            return;
        }
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();
        float[] data = part.data();
        Vector3f normal = new Vector3f();
        Vector3f edge = new Vector3f();
        Vector3f first = new Vector3f();
        Vector3f second = new Vector3f();
        Vector3f third = new Vector3f();
        Vector3f original = new Vector3f();
        Vector3f temporary = new Vector3f();
        int cursor = 0;
        for (int face = 0; face < part.faceCount(); face++) {
            int verticesStart = cursor;
            deformBatVertex(data, verticesStart, ageInTicks, resting, first, original, temporary);
            deformBatVertex(data, verticesStart + 5, ageInTicks, resting, second, original, temporary);
            deformBatVertex(data, verticesStart + 10, ageInTicks, resting, third, original, temporary);
            calculateFaceNormal(first, second, third, normal, edge);
            normalMatrix.transform(normal);
            emitVertex(data, verticesStart, first, pose, normal, buffer, packedLight, packedOverlay);
            emitVertex(data, verticesStart + 5, second, pose, normal, buffer, packedLight, packedOverlay);
            emitVertex(data, verticesStart + 10, third, pose, normal, buffer, packedLight, packedOverlay);
            emitVertex(data, verticesStart + 10, third, pose, normal, buffer, packedLight, packedOverlay);
            cursor += 18;
        }
    }

    private static void deformBatVertex(float[] data, int offset, float ageInTicks,
                                        boolean resting, Vector3f output,
                                        Vector3f original, Vector3f temporary) {
        original.set(data[offset], data[offset + 1], data[offset + 2]);
        float x = original.x();
        float side = x < 0.0F ? -1.0F : 1.0F;
        float wingWeight = Mth.clamp((Math.abs(x) - 0.72F) / 0.62F, 0.0F, 1.0F);
        float wingTipWeight = Mth.clamp((Math.abs(x) - 2.35F) / 1.45F, 0.0F, 1.0F);
        float flap = resting
                ? -0.72F
                : Mth.sin(ageInTicks * 0.78F) * 0.62F - 0.10F;
        rotateAroundZ(original, side * 0.82F, 21.38F, side * flap, temporary);
        output.set(original).lerp(temporary, wingWeight);

        if (resting) {
            rotateAroundY(output, side * 0.82F, 0.05F, -side * 0.88F, temporary);
            output.lerp(temporary, wingWeight * 0.72F);
        } else {
            float flex = Mth.sin(ageInTicks * 0.78F + 0.85F) * 0.22F * wingTipWeight;
            output.add(0.0F, flex, -Math.abs(flex) * 0.42F);
        }

        float headWeight = Mth.clamp((21.20F - original.y()) / 0.55F, 0.0F, 1.0F)
                * Mth.clamp((1.28F - Math.abs(x)) / 0.30F, 0.0F, 1.0F);
        if (!resting && headWeight > 0.0F) {
            rotateAroundY(original, 0.0F, -0.65F,
                    Mth.sin(ageInTicks * 0.11F) * 0.10F, temporary);
            output.lerp(temporary, headWeight * 0.45F);
        }
    }

    private static void deformAllayVertex(float[] data, int offset, float ageInTicks,
                                          float netHeadYaw, float headPitch,
                                          byte actionState, Vector3f output,
                                          Vector3f original, Vector3f temporary) {
        original.set(data[offset], data[offset + 1], data[offset + 2]);
        output.set(original);
        float x = original.x();
        float y = original.y();
        float z = original.z();
        float side = x < 0.0F ? -1.0F : 1.0F;

        float wingWeight = Mth.clamp((Math.abs(x) - 3.55F) / 0.82F, 0.0F, 1.0F)
                * Mth.clamp((-z + 1.30F) / 0.65F, 0.0F, 1.0F);
        float flapSpeed = actionState == HelpingAllayEntity.ACTION_TELEPORT ? 1.34F
                : actionState == HelpingAllayEntity.ACTION_SHIELD
                || actionState == HelpingAllayEntity.ACTION_HEAL ? 1.05F : 0.78F;
        float flapStrength = actionState == HelpingAllayEntity.ACTION_TELEPORT ? 1.05F
                : actionState == HelpingAllayEntity.ACTION_SHIELD
                || actionState == HelpingAllayEntity.ACTION_HEAL ? 0.86F : 0.62F;
        float flap = Mth.sin(ageInTicks * flapSpeed) * flapStrength;
        rotateAroundY(original, side * 3.55F, -1.45F, side * flap, temporary);
        output.lerp(temporary, wingWeight);

        float armWeight = Mth.clamp((Math.abs(x) - 1.88F) / 0.58F, 0.0F, 1.0F)
                * Mth.clamp((4.08F - Math.abs(x)) / 0.42F, 0.0F, 1.0F)
                * Mth.clamp((y - 16.45F) / 0.72F, 0.0F, 1.0F)
                * Mth.clamp((21.70F - y) / 0.72F, 0.0F, 1.0F)
                * Mth.clamp((z + 2.02F) / 0.52F, 0.0F, 1.0F);
        float armAngle;
        if (actionState == HelpingAllayEntity.ACTION_REVEAL) {
            armAngle = -1.05F;
        } else if (actionState == HelpingAllayEntity.ACTION_SHIELD) {
            armAngle = -1.24F;
        } else if (actionState == HelpingAllayEntity.ACTION_HEAL
                || actionState == HelpingAllayEntity.ACTION_BOND) {
            armAngle = -0.72F;
        } else {
            armAngle = Mth.sin(ageInTicks * 0.10F + (side < 0.0F ? Mth.PI : 0.0F)) * 0.12F;
        }
        rotateAroundX(original, 16.85F, 0.0F, armAngle, temporary);
        output.lerp(temporary, armWeight);

        float headWeight = Mth.clamp((17.25F - y) / 0.72F, 0.0F, 1.0F)
                * Mth.clamp((4.50F - Math.abs(x)) / 0.42F, 0.0F, 1.0F);
        float yaw = Mth.clamp(netHeadYaw * Mth.DEG_TO_RAD, -0.55F, 0.55F);
        float pitch = Mth.clamp(headPitch * Mth.DEG_TO_RAD, -0.42F, 0.42F);
        rotateAroundY(original, 0.0F, 0.15F, yaw, temporary);
        rotateAroundX(temporary, 16.85F, 0.15F, pitch, temporary);
        output.lerp(temporary, headWeight * headWeight);

        float coreWeight = Mth.clamp((z - 2.30F) / 0.34F, 0.0F, 1.0F)
                * Mth.clamp((1.45F - Math.abs(x)) / 0.32F, 0.0F, 1.0F)
                * Mth.clamp((y - 17.40F) / 0.38F, 0.0F, 1.0F)
                * Mth.clamp((20.05F - y) / 0.38F, 0.0F, 1.0F);
        float corePulse = actionState == HelpingAllayEntity.ACTION_HEAL ? 0.30F
                : actionState == HelpingAllayEntity.ACTION_BOND ? 0.20F : 0.08F;
        output.add(0.0F, Mth.sin(ageInTicks * 0.42F) * corePulse * coreWeight,
                Mth.cos(ageInTicks * 0.42F) * corePulse * 0.35F * coreWeight);
    }

    private static void rotateAroundZ(Vector3f point, float pivotX, float pivotY, float angle,
                                      Vector3f output) {
        float relativeX = point.x() - pivotX;
        float relativeY = point.y() - pivotY;
        float cosine = Mth.cos(angle);
        float sine = Mth.sin(angle);
        output.set(
                relativeX * cosine - relativeY * sine + pivotX,
                relativeX * sine + relativeY * cosine + pivotY,
                point.z());
    }

    private static void rotateAroundY(Vector3f point, float pivotX, float pivotZ, float angle,
                                      Vector3f output) {
        float relativeX = point.x() - pivotX;
        float relativeZ = point.z() - pivotZ;
        float cosine = Mth.cos(angle);
        float sine = Mth.sin(angle);
        output.set(
                relativeX * cosine + relativeZ * sine + pivotX,
                point.y(),
                -relativeX * sine + relativeZ * cosine + pivotZ);
    }

    private static void deformHumanoidVertex(float[] data, int offset, float limbSwing,
                                             float limbSwingAmount, float ageInTicks,
                                             float netHeadYaw, float headPitch,
                                             boolean swimming, boolean aimingBow,
                                             boolean aggressiveMelee, Vector3f output,
                                             Vector3f original, Vector3f temporary) {
        original.set(data[offset], data[offset + 1], data[offset + 2]);
        output.set(original);
        float x = original.x();
        float y = original.y();
        float walk = limbSwing * 0.6662F;

        float armSide = Mth.clamp((Math.abs(x) - 0.24F) / 0.20F, 0.0F, 1.0F);
        float armVertical = Mth.clamp((0.92F - y) / 0.34F, 0.0F, 1.0F)
                * Mth.clamp((y + 0.08F) / 0.30F, 0.0F, 1.0F);
        float armWeight = armSide * armVertical;
        float armPhase = x < 0.0F ? Mth.PI : 0.0F;
        float armAngle = aimingBow ? -1.18F : aggressiveMelee ?
                -1.28F + Mth.sin(ageInTicks * 0.18F + armPhase) * 0.08F : swimming
                ? -1.05F + Mth.sin(ageInTicks * 0.12F + armPhase) * 0.18F
                : Mth.cos(walk + armPhase) * 0.82F * limbSwingAmount;
        rotateAroundX(original, 0.18F, 0.0F, armAngle, temporary);
        output.lerp(temporary, armWeight);

        float legWeight = Mth.clamp((y - 0.62F) / 0.66F, 0.0F, 1.0F)
                * Mth.clamp((Math.abs(x) - 0.02F) / 0.13F, 0.0F, 1.0F);
        float legPhase = x < 0.0F ? 0.0F : Mth.PI;
        float legAngle = swimming
                ? Mth.sin(ageInTicks * 0.16F + legPhase) * 0.34F
                : Mth.cos(walk + legPhase) * 0.88F * limbSwingAmount;
        rotateAroundX(original, 0.70F, 0.0F, legAngle, temporary);
        output.lerp(temporary, legWeight);

        float headWeight = Mth.clamp((0.20F - y) / 0.32F, 0.0F, 1.0F);
        rotateHead(original,
                Mth.clamp(netHeadYaw * Mth.DEG_TO_RAD, -0.55F, 0.55F),
                Mth.clamp(headPitch * Mth.DEG_TO_RAD, -0.42F, 0.42F), temporary);
        output.lerp(temporary, headWeight * headWeight);
        if (swimming) {
            float bodyWeight = 1.0F - Math.max(armWeight, legWeight);
            output.add(Mth.sin(ageInTicks * 0.08F + y * 2.0F) * 0.025F * bodyWeight, 0.0F, 0.0F);
        }
    }

    private static void rotateAroundX(Vector3f point, float pivotY, float pivotZ, float angle,
                                      Vector3f output) {
        float relativeY = point.y() - pivotY;
        float relativeZ = point.z() - pivotZ;
        float cosine = Mth.cos(angle);
        float sine = Mth.sin(angle);
        output.set(point.x(),
                relativeY * cosine - relativeZ * sine + pivotY,
                relativeY * sine + relativeZ * cosine + pivotZ);
    }

    private static void rotateHead(Vector3f point, float yaw, float pitch, Vector3f output) {
        float pivotY = -0.02F;
        float pivotZ = 0.0F;
        float yawCosine = Mth.cos(yaw);
        float yawSine = Mth.sin(yaw);
        float yawedX = point.x() * yawCosine + (point.z() - pivotZ) * yawSine;
        float yawedZ = -point.x() * yawSine + (point.z() - pivotZ) * yawCosine;
        float pitchCosine = Mth.cos(pitch);
        float pitchSine = Mth.sin(pitch);
        float relativeY = point.y() - pivotY;
        output.set(yawedX,
                relativeY * pitchCosine - yawedZ * pitchSine + pivotY,
                relativeY * pitchSine + yawedZ * pitchCosine + pivotZ);
    }

    private static void deformQuadrupedVertex(float[] data, int offset, float limbSwing,
                                              float limbSwingAmount, float netHeadYaw,
                                              float headPitch, Vector3f output) {
        float x = data[offset];
        float y = data[offset + 1];
        float z = data[offset + 2];

        float lowerBody = Mth.clamp((y - 0.70F) / 0.72F, 0.0F, 1.0F);
        float lateral = Mth.clamp((Math.abs(x) - 0.12F) / 0.30F, 0.0F, 1.0F);
        float pawWeight = lowerBody * lowerBody * lateral;
        float phase = x * z >= 0.0F ? 0.0F : Mth.PI;
        float step = Mth.cos(limbSwing * 0.6662F + phase) * limbSwingAmount;
        z += step * 0.20F * pawWeight;
        y -= Math.abs(step) * 0.045F * pawWeight;

        float headWeight = Mth.clamp((-z - 0.18F) / 0.42F, 0.0F, 1.0F);
        headWeight *= headWeight;
        float yaw = Mth.clamp(netHeadYaw * Mth.DEG_TO_RAD, -0.45F, 0.45F) * headWeight;
        float pitch = Mth.clamp(headPitch * Mth.DEG_TO_RAD, -0.32F, 0.32F) * headWeight;
        float pivotY = 0.64F;
        float pivotZ = -0.43F;
        float relativeZ = z - pivotZ;
        float rotatedX = x * Mth.cos(yaw) + relativeZ * Mth.sin(yaw);
        float yawedZ = -x * Mth.sin(yaw) + relativeZ * Mth.cos(yaw);
        float relativeY = y - pivotY;
        float rotatedY = relativeY * Mth.cos(pitch) - yawedZ * Mth.sin(pitch);
        float rotatedZ = relativeY * Mth.sin(pitch) + yawedZ * Mth.cos(pitch);
        output.set(rotatedX, rotatedY + pivotY, rotatedZ + pivotZ);
    }

    private static void deformSpiderVertex(float[] data, int offset, float limbSwing,
                                           float limbSwingAmount, Vector3f output) {
        float x = data[offset];
        float y = data[offset + 1];
        float z = data[offset + 2];
        float legTipWeight = Mth.clamp((Math.abs(x) - 0.125F) / 0.225F, 0.0F, 1.0F);
        legTipWeight *= legTipWeight;
        float phase = x * z >= 0.0F ? 0.0F : Mth.PI;
        float step = Mth.cos(limbSwing * 1.45F + phase) * limbSwingAmount;
        z += step * 0.115F * legTipWeight;
        y -= Math.abs(step) * 0.042F * legTipWeight;
        x += Math.signum(x) * Mth.sin(limbSwing * 1.45F + phase)
                * 0.022F * limbSwingAmount * legTipWeight;
        output.set(x, y, z);
    }

    private static void deformSquidVertex(float[] data, int offset, float waveSin,
                                          float waveCos, float strength, Vector3f output) {
        float x = data[offset];
        float y = data[offset + 1];
        float z = data[offset + 2];
        // The mantle points towards +Z and the ten appendages towards -Z.
        // Keep the attachment zone rigid, then increase motion smoothly towards
        // the arm tips and the two substantially longer catching tentacles.
        float tipWeight = Mth.clamp((-z - 0.10F) / 0.60F, 0.0F, 1.0F);
        tipWeight *= tipWeight;
        float radialY = y - 1.14F;
        float catchingBoost = z < -0.45F ? 1.30F : 1.0F;
        float amplitude = tipWeight * strength * catchingBoost;
        float displacedX = x + amplitude * (waveSin * 0.040F + radialY * waveCos * 0.085F);
        float displacedY = y + amplitude * (waveCos * 0.030F - x * waveSin * 0.085F);
        float displacedZ = z + amplitude * waveSin * 0.012F;
        output.set(displacedX, displacedY, displacedZ);
    }

    private static void deformOctopusVertex(float[] data, int offset, float ageInTicks,
                                            boolean inWater, byte actionState,
                                            boolean squeezing, Vector3f output) {
        float x = data[offset];
        float y = data[offset + 1];
        float z = data[offset + 2];
        float radius = Mth.sqrt(x * x + z * z);
        float verticalWeight = Mth.clamp((y - 0.98F) / 0.46F, 0.0F, 1.0F);
        float radialWeight = Mth.clamp((radius - 0.10F) / 0.35F, 0.0F, 1.0F);
        float tentacleTipWeight = verticalWeight * radialWeight;
        tentacleTipWeight *= tentacleTipWeight;

        float strength = inWater ? 1.0F : 0.28F;
        float speed;
        float amplitude;
        switch (actionState) {
            case OctopusEntity.ACTION_SWIM -> {
                speed = 0.34F;
                amplitude = 1.30F;
            }
            case OctopusEntity.ACTION_AMBUSH, OctopusEntity.ACTION_CAMOUFLAGE -> {
                speed = 0.07F;
                amplitude = 0.28F;
            }
            case OctopusEntity.ACTION_GRAB -> {
                speed = 0.42F;
                amplitude = 1.45F;
            }
            case OctopusEntity.ACTION_INK -> {
                speed = 0.72F;
                amplitude = 1.55F;
            }
            case OctopusEntity.ACTION_OBJECT -> {
                speed = 0.16F;
                amplitude = 0.85F;
            }
            default -> {
                speed = 0.18F;
                amplitude = 0.72F;
            }
        }

        float polarPhase = (float) Math.atan2(z, x);
        float wave = Mth.sin(ageInTicks * speed + polarPhase * 2.0F);
        float curl = Mth.cos(ageInTicks * speed * 0.73F - polarPhase * 3.0F);
        float motion = tentacleTipWeight * strength * amplitude;
        float radialScale = 1.0F + wave * 0.075F * motion;
        if (actionState == OctopusEntity.ACTION_INK) {
            radialScale -= (0.10F + Mth.abs(wave) * 0.08F) * tentacleTipWeight;
        }
        if (squeezing) {
            radialScale -= 0.10F * tentacleTipWeight;
        }
        float tangent = curl * 0.060F * motion;
        float directionX = radius > 1.0E-5F ? x / radius : 0.0F;
        float directionZ = radius > 1.0E-5F ? z / radius : 0.0F;
        x = x * radialScale - directionZ * tangent;
        z = z * radialScale + directionX * tangent;
        y += Mth.abs(wave) * 0.035F * motion;

        if (actionState == OctopusEntity.ACTION_GRAB) {
            float forwardReach = Mth.clamp((-z - 0.04F) / 0.34F, 0.0F, 1.0F)
                    * tentacleTipWeight;
            z -= 0.22F * forwardReach;
            y -= 0.07F * forwardReach;
        } else if (actionState == OctopusEntity.ACTION_OBJECT) {
            float holdingArm = Mth.clamp((-x - 0.05F) / 0.30F, 0.0F, 1.0F)
                    * Mth.clamp((-z - 0.02F) / 0.30F, 0.0F, 1.0F)
                    * tentacleTipWeight;
            x += 0.10F * holdingArm;
            z += 0.08F * holdingArm;
            y -= 0.10F * holdingArm;
        }
        output.set(x, y, z);
    }

    private static void deformGlowSquidVertex(float[] data, int offset, float waveSin,
                                              float waveCos, float strength, Vector3f output) {
        float x = data[offset];
        float y = data[offset + 1];
        float z = data[offset + 2];
        // Runtime Y grows from mantle top (0) towards the dangling tentacle tips.
        // A smooth weight keeps the mantle and every shared attachment vertex sealed.
        float tipWeight = Mth.clamp((y - 0.58F) / 0.78F, 0.0F, 1.0F);
        tipWeight *= tipWeight;
        float radialPhase = x * 2.7F + z * 2.1F;
        float amplitude = tipWeight * strength;
        float displacedX = x + amplitude * (waveSin * 0.075F + z * waveCos * 0.10F);
        float displacedZ = z + amplitude * (waveCos * 0.075F - x * waveSin * 0.10F);
        float displacedY = y + amplitude * Mth.sin(radialPhase + waveSin) * 0.028F;
        output.set(displacedX, displacedY, displacedZ);
    }

    private static void deformAxolotlVertex(float[] data, int offset, float ageInTicks,
                                            float limbSwing, float limbSwingAmount,
                                            boolean inWater, Vector3f output) {
        float x = data[offset];
        float y = data[offset + 1];
        float z = data[offset + 2];
        float waterStrength = inWater ? 1.0F : 0.28F;

        // The head points towards -Z and the tail towards +Z. A smooth field
        // bends every shared tail vertex identically, so no cut can open.
        float tailWeight = Mth.clamp((z - 0.04F) / 0.61F, 0.0F, 1.0F);
        tailWeight *= tailWeight;
        float tailWave = Mth.sin(ageInTicks * 0.23F + z * 3.2F) * waterStrength;
        x += tailWave * 0.17F * tailWeight;
        z += Mth.cos(ageInTicks * 0.19F + z * 2.4F) * 0.018F * tailWeight * waterStrength;

        // Feet step more visibly on land and paddle with a softer stroke in water.
        float legWeight = Mth.clamp((y - 1.29F) / 0.20F, 0.0F, 1.0F)
                * Mth.clamp((Math.abs(x) - 0.17F) / 0.24F, 0.0F, 1.0F);
        float legPhase = x * z >= 0.0F ? 0.0F : Mth.PI;
        float step = Mth.cos(limbSwing * 1.15F + legPhase) * limbSwingAmount;
        z += step * (inWater ? 0.055F : 0.13F) * legWeight;
        y -= Math.abs(step) * 0.035F * legWeight;

        // The regenerated model carries its external gill/frond silhouette on
        // the upper lateral rear of the body. A continuous positional field
        // reaches those surfaces without moving the face or opening seams.
        float gillWeight = Mth.clamp((Math.abs(x) - 0.24F) / 0.08F, 0.0F, 1.0F)
                * Mth.clamp((z - 0.08F) / 0.08F, 0.0F, 1.0F)
                * Mth.clamp((0.34F - z) / 0.08F, 0.0F, 1.0F)
                * Mth.clamp((1.28F - y) / 0.10F, 0.0F, 1.0F);
        float gillSide = x < 0.0F ? -1.0F : 1.0F;
        x += gillSide * Mth.sin(ageInTicks * 0.31F + y * 8.0F) * 0.035F * gillWeight;
        y += Mth.cos(ageInTicks * 0.27F + z * 7.0F) * 0.018F * gillWeight;
        output.set(x, y, z);
    }

    private static void deformOcelotVertex(float[] data, int offset, float ageInTicks,
                                           float limbSwing, float limbSwingAmount,
                                           float netHeadYaw, float headPitch,
                                           boolean sprinting, boolean airborne,
                                           Vector3f output) {
        float x = data[offset];
        float y = data[offset + 1];
        float z = data[offset + 2];

        // The four paws move on diagonal phases. Position weights make the
        // movement strong at the paws and fade it smoothly into the shoulders.
        float legWeight = Mth.clamp((y - 18.55F) / 5.10F, 0.0F, 1.0F)
                * Mth.clamp((Math.abs(x) - 0.48F) / 1.15F, 0.0F, 1.0F);
        legWeight *= legWeight;
        float legPhase = x * z >= 0.0F ? 0.0F : Mth.PI;
        float strideScale = sprinting ? 1.32F : 1.0F;
        float step = Mth.cos(limbSwing * 0.92F + legPhase) * limbSwingAmount * strideScale;
        z += step * 0.58F * legWeight;
        y -= Math.abs(step) * 0.16F * legWeight;

        // Running and airborne movement stretches the silhouette into a clear
        // feline pounce while keeping every coincident seam vertex identical.
        float pounceStretch = Mth.clamp((sprinting ? 0.62F : 0.0F)
                + (airborne ? 0.58F : 0.0F) + limbSwingAmount * 0.18F, 0.0F, 1.0F);
        float bodyArc = Mth.sin(Mth.clamp((z + 11.6F) / 23.2F, 0.0F, 1.0F) * Mth.PI);
        z *= 1.0F + pounceStretch * 0.045F;
        y -= bodyArc * pounceStretch * 0.22F;

        // The long tail counterbalances walking and pouncing with a soft wave.
        float tailWeight = Mth.clamp((z - 4.70F) / 6.90F, 0.0F, 1.0F);
        tailWeight *= tailWeight;
        float tailWave = Mth.sin(ageInTicks * 0.16F + z * 0.31F)
                + step * 0.35F;
        x += tailWave * (0.25F + pounceStretch * 0.10F) * tailWeight;
        y -= Mth.cos(ageInTicks * 0.12F + z * 0.24F) * 0.08F * tailWeight;

        // Head motion fades through the neck instead of rotating a detached part.
        float headWeight = Mth.clamp((-z - 3.80F) / 5.20F, 0.0F, 1.0F)
                * Mth.clamp((21.2F - y) / 4.8F, 0.0F, 1.0F);
        headWeight *= headWeight;
        float yaw = Mth.clamp(netHeadYaw * Mth.DEG_TO_RAD, -0.55F, 0.55F) * headWeight;
        float pitch = Mth.clamp(headPitch * Mth.DEG_TO_RAD, -0.38F, 0.38F) * headWeight;
        float pivotY = 18.65F;
        float pivotZ = -5.10F;
        float relativeZ = z - pivotZ;
        float rotatedX = x * Mth.cos(yaw) + relativeZ * Mth.sin(yaw);
        float yawedZ = -x * Mth.sin(yaw) + relativeZ * Mth.cos(yaw);
        float relativeY = y - pivotY;
        float rotatedY = relativeY * Mth.cos(pitch) - yawedZ * Mth.sin(pitch);
        float rotatedZ = relativeY * Mth.sin(pitch) + yawedZ * Mth.cos(pitch);
        output.set(rotatedX, rotatedY + pivotY, rotatedZ + pivotZ);
    }

    private static MeshPart readPart(DataInputStream input, Vector3f pivot, int faceCount)
            throws IOException {
        float[] data = new float[Math.multiplyExact(faceCount, 18)];
        int cursor = 0;
        for (int face = 0; face < faceCount; face++) {
            int faceStart = cursor;
            for (int vertex = 0; vertex < 3; vertex++) {
                data[cursor++] = readFiniteFloatLe(input) / 16F;
                data[cursor++] = readFiniteFloatLe(input) / 16F;
                data[cursor++] = readFiniteFloatLe(input) / 16F;
                data[cursor++] = readFiniteFloatLe(input);
                data[cursor++] = readFiniteFloatLe(input);
            }
            Vector3f edgeOne = new Vector3f(
                    data[faceStart + 5] - data[faceStart],
                    data[faceStart + 6] - data[faceStart + 1],
                    data[faceStart + 7] - data[faceStart + 2]);
            Vector3f edgeTwo = new Vector3f(
                    data[faceStart + 10] - data[faceStart],
                    data[faceStart + 11] - data[faceStart + 1],
                    data[faceStart + 12] - data[faceStart + 2]);
            Vector3f normal = edgeOne.cross(edgeTwo);
            if (normal.lengthSquared() < 1.0E-12F) {
                normal.set(0, 1, 0);
            } else {
                normal.normalize();
            }
            data[cursor++] = normal.x();
            data[cursor++] = normal.y();
            data[cursor++] = normal.z();
        }
        return new MeshPart(pivot, data, faceCount);
    }

    private static void calculateFaceNormal(Vector3f first, Vector3f second, Vector3f third,
                                            Vector3f normal, Vector3f edge) {
        normal.set(second).sub(first);
        edge.set(third).sub(first);
        normal.cross(edge);
        if (normal.lengthSquared() < 1.0E-12F) {
            normal.set(0, 1, 0);
        } else {
            normal.normalize();
        }
    }

    private static void emitVertex(float[] data, int offset, Matrix4f pose, Vector3f normal,
                                   VertexConsumer buffer, int packedLight, int packedOverlay) {
        float modelX = data[offset];
        float modelY = data[offset + 1];
        float modelZ = data[offset + 2];
        float x = pose.m00() * modelX + pose.m10() * modelY + pose.m20() * modelZ + pose.m30();
        float y = pose.m01() * modelX + pose.m11() * modelY + pose.m21() * modelZ + pose.m31();
        float z = pose.m02() * modelX + pose.m12() * modelY + pose.m22() * modelZ + pose.m32();
        buffer.vertex(x, y, z, 1F, 1F, 1F, 1F,
                data[offset + 3], data[offset + 4], packedOverlay, packedLight,
                normal.x(), normal.y(), normal.z());
    }

    private static void emitVertex(float[] data, int offset, Vector3f modelPosition,
                                    Matrix4f pose, Vector3f normal, VertexConsumer buffer,
                                    int packedLight, int packedOverlay) {
        float modelX = modelPosition.x();
        float modelY = modelPosition.y();
        float modelZ = modelPosition.z();
        float x = pose.m00() * modelX + pose.m10() * modelY + pose.m20() * modelZ + pose.m30();
        float y = pose.m01() * modelX + pose.m11() * modelY + pose.m21() * modelZ + pose.m31();
        float z = pose.m02() * modelX + pose.m12() * modelY + pose.m22() * modelZ + pose.m32();
        buffer.vertex(x, y, z, 1F, 1F, 1F, 1F,
                data[offset + 3], data[offset + 4], packedOverlay, packedLight,
                normal.x(), normal.y(), normal.z());
    }

    private static int readIntLe(DataInputStream input) throws IOException {
        return Integer.reverseBytes(input.readInt());
    }

    private static int readUnsignedShortLe(DataInputStream input) throws IOException {
        return Short.toUnsignedInt(Short.reverseBytes(input.readShort()));
    }

    private static float readFiniteFloatLe(DataInputStream input) throws IOException {
        float value = Float.intBitsToFloat(readIntLe(input));
        if (!Float.isFinite(value)) {
            throw new IOException("Exact Tripo mesh contains a non-finite value");
        }
        return value;
    }

    private record MeshPart(Vector3f pivot, float[] data, int faceCount) {}
}
