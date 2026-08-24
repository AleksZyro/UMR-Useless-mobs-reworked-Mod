package com.Momik.usless_mobs.client;

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
import org.joml.Vector4f;

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
        for (int face = 0; face < part.faceCount(); face++) {
            int verticesStart = cursor;
            int normalStart = cursor + 15;
            Vector3f normal = normalMatrix.transform(new Vector3f(
                    data[normalStart], data[normalStart + 1], data[normalStart + 2]));
            emitVertex(data, verticesStart, pose, normal, buffer, packedLight, packedOverlay);
            emitVertex(data, verticesStart + 5, pose, normal, buffer, packedLight, packedOverlay);
            emitVertex(data, verticesStart + 10, pose, normal, buffer, packedLight, packedOverlay);
            emitVertex(data, verticesStart + 10, pose, normal, buffer, packedLight, packedOverlay);
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
        int cursor = 0;
        for (int face = 0; face < part.faceCount(); face++) {
            int verticesStart = cursor;
            Vector3f first = deformSquidVertex(data, verticesStart, waveSin, waveCos, strength);
            Vector3f second = deformSquidVertex(data, verticesStart + 5, waveSin, waveCos, strength);
            Vector3f third = deformSquidVertex(data, verticesStart + 10, waveSin, waveCos, strength);
            Vector3f edgeOne = new Vector3f(second).sub(first);
            Vector3f edgeTwo = new Vector3f(third).sub(first);
            Vector3f normal = edgeOne.cross(edgeTwo);
            if (normal.lengthSquared() < 1.0E-12F) {
                normal.set(0, 1, 0);
            } else {
                normal.normalize();
            }
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
        int cursor = 0;
        for (int face = 0; face < part.faceCount(); face++) {
            int verticesStart = cursor;
            Vector3f first = deformGlowSquidVertex(data, verticesStart, waveSin, waveCos, strength);
            Vector3f second = deformGlowSquidVertex(data, verticesStart + 5, waveSin, waveCos, strength);
            Vector3f third = deformGlowSquidVertex(data, verticesStart + 10, waveSin, waveCos, strength);
            Vector3f normal = new Vector3f(second).sub(first).cross(new Vector3f(third).sub(first));
            if (normal.lengthSquared() < 1.0E-12F) {
                normal.set(0, 1, 0);
            } else {
                normal.normalize();
            }
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
        int cursor = 0;
        for (int face = 0; face < part.faceCount(); face++) {
            int verticesStart = cursor;
            Vector3f first = deformOctopusVertex(data, verticesStart, ageInTicks,
                    inWater, actionState, squeezing);
            Vector3f second = deformOctopusVertex(data, verticesStart + 5, ageInTicks,
                    inWater, actionState, squeezing);
            Vector3f third = deformOctopusVertex(data, verticesStart + 10, ageInTicks,
                    inWater, actionState, squeezing);
            Vector3f normal = new Vector3f(second).sub(first).cross(new Vector3f(third).sub(first));
            if (normal.lengthSquared() < 1.0E-12F) {
                normal.set(0, 1, 0);
            } else {
                normal.normalize();
            }
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
        int cursor = 0;
        for (int face = 0; face < part.faceCount(); face++) {
            int verticesStart = cursor;
            Vector3f first = deformAxolotlVertex(data, verticesStart, ageInTicks,
                    limbSwing, limbSwingAmount, inWater);
            Vector3f second = deformAxolotlVertex(data, verticesStart + 5, ageInTicks,
                    limbSwing, limbSwingAmount, inWater);
            Vector3f third = deformAxolotlVertex(data, verticesStart + 10, ageInTicks,
                    limbSwing, limbSwingAmount, inWater);
            Vector3f normal = new Vector3f(second).sub(first).cross(new Vector3f(third).sub(first));
            if (normal.lengthSquared() < 1.0E-12F) {
                normal.set(0, 1, 0);
            } else {
                normal.normalize();
            }
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
        int cursor = 0;
        for (int face = 0; face < part.faceCount(); face++) {
            int verticesStart = cursor;
            Vector3f first = deformOcelotVertex(data, verticesStart, ageInTicks, limbSwing,
                    limbSwingAmount, netHeadYaw, headPitch, sprinting, airborne);
            Vector3f second = deformOcelotVertex(data, verticesStart + 5, ageInTicks, limbSwing,
                    limbSwingAmount, netHeadYaw, headPitch, sprinting, airborne);
            Vector3f third = deformOcelotVertex(data, verticesStart + 10, ageInTicks, limbSwing,
                    limbSwingAmount, netHeadYaw, headPitch, sprinting, airborne);
            Vector3f normal = new Vector3f(second).sub(first).cross(new Vector3f(third).sub(first));
            if (normal.lengthSquared() < 1.0E-12F) {
                normal.set(0, 1, 0);
            } else {
                normal.normalize();
            }
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
        int cursor = 0;
        for (int face = 0; face < part.faceCount(); face++) {
            int verticesStart = cursor;
            Vector3f first = deformSpiderVertex(data, verticesStart, limbSwing, limbSwingAmount);
            Vector3f second = deformSpiderVertex(data, verticesStart + 5, limbSwing, limbSwingAmount);
            Vector3f third = deformSpiderVertex(data, verticesStart + 10, limbSwing, limbSwingAmount);
            Vector3f normal = new Vector3f(second).sub(first).cross(new Vector3f(third).sub(first));
            if (normal.lengthSquared() < 1.0E-12F) {
                normal.set(0, 1, 0);
            } else {
                normal.normalize();
            }
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
        int cursor = 0;
        for (int face = 0; face < part.faceCount(); face++) {
            int verticesStart = cursor;
            Vector3f first = deformQuadrupedVertex(data, verticesStart, limbSwing, limbSwingAmount,
                    netHeadYaw, headPitch);
            Vector3f second = deformQuadrupedVertex(data, verticesStart + 5, limbSwing, limbSwingAmount,
                    netHeadYaw, headPitch);
            Vector3f third = deformQuadrupedVertex(data, verticesStart + 10, limbSwing, limbSwingAmount,
                    netHeadYaw, headPitch);
            Vector3f normal = new Vector3f(second).sub(first).cross(new Vector3f(third).sub(first));
            if (normal.lengthSquared() < 1.0E-12F) {
                normal.set(0, 1, 0);
            } else {
                normal.normalize();
            }
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
        int cursor = 0;
        for (int face = 0; face < part.faceCount(); face++) {
            int verticesStart = cursor;
            Vector3f first = deformHumanoidVertex(data, verticesStart, limbSwing, limbSwingAmount,
                    ageInTicks, netHeadYaw, headPitch, swimming, aimingBow, aggressiveMelee);
            Vector3f second = deformHumanoidVertex(data, verticesStart + 5, limbSwing, limbSwingAmount,
                    ageInTicks, netHeadYaw, headPitch, swimming, aimingBow, aggressiveMelee);
            Vector3f third = deformHumanoidVertex(data, verticesStart + 10, limbSwing, limbSwingAmount,
                    ageInTicks, netHeadYaw, headPitch, swimming, aimingBow, aggressiveMelee);
            Vector3f normal = new Vector3f(second).sub(first).cross(new Vector3f(third).sub(first));
            if (normal.lengthSquared() < 1.0E-12F) {
                normal.set(0, 1, 0);
            } else {
                normal.normalize();
            }
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
        int cursor = 0;
        for (int face = 0; face < part.faceCount(); face++) {
            int verticesStart = cursor;
            Vector3f first = deformBatVertex(data, verticesStart, ageInTicks, resting);
            Vector3f second = deformBatVertex(data, verticesStart + 5, ageInTicks, resting);
            Vector3f third = deformBatVertex(data, verticesStart + 10, ageInTicks, resting);
            Vector3f normal = new Vector3f(second).sub(first).cross(new Vector3f(third).sub(first));
            if (normal.lengthSquared() < 1.0E-12F) {
                normal.set(0, 1, 0);
            } else {
                normal.normalize();
            }
            normalMatrix.transform(normal);
            emitVertex(data, verticesStart, first, pose, normal, buffer, packedLight, packedOverlay);
            emitVertex(data, verticesStart + 5, second, pose, normal, buffer, packedLight, packedOverlay);
            emitVertex(data, verticesStart + 10, third, pose, normal, buffer, packedLight, packedOverlay);
            emitVertex(data, verticesStart + 10, third, pose, normal, buffer, packedLight, packedOverlay);
            cursor += 18;
        }
    }

    private static Vector3f deformBatVertex(float[] data, int offset, float ageInTicks,
                                             boolean resting) {
        Vector3f original = new Vector3f(data[offset], data[offset + 1], data[offset + 2]);
        float x = original.x();
        float side = x < 0.0F ? -1.0F : 1.0F;
        float wingWeight = Mth.clamp((Math.abs(x) - 0.72F) / 0.62F, 0.0F, 1.0F);
        float wingTipWeight = Mth.clamp((Math.abs(x) - 2.35F) / 1.45F, 0.0F, 1.0F);
        float flap = resting
                ? -0.72F
                : Mth.sin(ageInTicks * 0.78F) * 0.62F - 0.10F;
        Vector3f flapped = rotateAroundZ(original, side * 0.82F, 21.38F, side * flap);
        Vector3f result = new Vector3f(original).lerp(flapped, wingWeight);

        if (resting) {
            Vector3f tucked = rotateAroundY(result, side * 0.82F, 0.05F, -side * 0.88F);
            result.lerp(tucked, wingWeight * 0.72F);
        } else {
            float flex = Mth.sin(ageInTicks * 0.78F + 0.85F) * 0.22F * wingTipWeight;
            result.add(0.0F, flex, -Math.abs(flex) * 0.42F);
        }

        float headWeight = Mth.clamp((21.20F - original.y()) / 0.55F, 0.0F, 1.0F)
                * Mth.clamp((1.28F - Math.abs(x)) / 0.30F, 0.0F, 1.0F);
        if (!resting && headWeight > 0.0F) {
            Vector3f head = rotateAroundY(original, 0.0F, -0.65F,
                    Mth.sin(ageInTicks * 0.11F) * 0.10F);
            result.lerp(head, headWeight * 0.45F);
        }
        return result;
    }

    private static Vector3f rotateAroundZ(Vector3f point, float pivotX, float pivotY, float angle) {
        float relativeX = point.x() - pivotX;
        float relativeY = point.y() - pivotY;
        float cosine = Mth.cos(angle);
        float sine = Mth.sin(angle);
        return new Vector3f(
                relativeX * cosine - relativeY * sine + pivotX,
                relativeX * sine + relativeY * cosine + pivotY,
                point.z());
    }

    private static Vector3f rotateAroundY(Vector3f point, float pivotX, float pivotZ, float angle) {
        float relativeX = point.x() - pivotX;
        float relativeZ = point.z() - pivotZ;
        float cosine = Mth.cos(angle);
        float sine = Mth.sin(angle);
        return new Vector3f(
                relativeX * cosine + relativeZ * sine + pivotX,
                point.y(),
                -relativeX * sine + relativeZ * cosine + pivotZ);
    }

    private static Vector3f deformHumanoidVertex(float[] data, int offset, float limbSwing,
                                                  float limbSwingAmount, float ageInTicks,
                                                  float netHeadYaw, float headPitch,
                                                  boolean swimming, boolean aimingBow,
                                                  boolean aggressiveMelee) {
        Vector3f original = new Vector3f(data[offset], data[offset + 1], data[offset + 2]);
        Vector3f result = new Vector3f(original);
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
        Vector3f arm = rotateAroundX(original, 0.18F, 0.0F, armAngle);
        result.lerp(arm, armWeight);

        float legWeight = Mth.clamp((y - 0.62F) / 0.66F, 0.0F, 1.0F)
                * Mth.clamp((Math.abs(x) - 0.02F) / 0.13F, 0.0F, 1.0F);
        float legPhase = x < 0.0F ? 0.0F : Mth.PI;
        float legAngle = swimming
                ? Mth.sin(ageInTicks * 0.16F + legPhase) * 0.34F
                : Mth.cos(walk + legPhase) * 0.88F * limbSwingAmount;
        Vector3f leg = rotateAroundX(original, 0.70F, 0.0F, legAngle);
        result.lerp(leg, legWeight);

        float headWeight = Mth.clamp((0.20F - y) / 0.32F, 0.0F, 1.0F);
        Vector3f head = rotateHead(original,
                Mth.clamp(netHeadYaw * Mth.DEG_TO_RAD, -0.55F, 0.55F),
                Mth.clamp(headPitch * Mth.DEG_TO_RAD, -0.42F, 0.42F));
        result.lerp(head, headWeight * headWeight);
        if (swimming) {
            float bodyWeight = 1.0F - Math.max(armWeight, legWeight);
            result.add(Mth.sin(ageInTicks * 0.08F + y * 2.0F) * 0.025F * bodyWeight, 0.0F, 0.0F);
        }
        return result;
    }

    private static Vector3f rotateAroundX(Vector3f point, float pivotY, float pivotZ, float angle) {
        float relativeY = point.y() - pivotY;
        float relativeZ = point.z() - pivotZ;
        float cosine = Mth.cos(angle);
        float sine = Mth.sin(angle);
        return new Vector3f(point.x(),
                relativeY * cosine - relativeZ * sine + pivotY,
                relativeY * sine + relativeZ * cosine + pivotZ);
    }

    private static Vector3f rotateHead(Vector3f point, float yaw, float pitch) {
        float pivotY = -0.02F;
        float pivotZ = 0.0F;
        float yawCosine = Mth.cos(yaw);
        float yawSine = Mth.sin(yaw);
        float yawedX = point.x() * yawCosine + (point.z() - pivotZ) * yawSine;
        float yawedZ = -point.x() * yawSine + (point.z() - pivotZ) * yawCosine;
        float pitchCosine = Mth.cos(pitch);
        float pitchSine = Mth.sin(pitch);
        float relativeY = point.y() - pivotY;
        return new Vector3f(yawedX,
                relativeY * pitchCosine - yawedZ * pitchSine + pivotY,
                relativeY * pitchSine + yawedZ * pitchCosine + pivotZ);
    }

    private static Vector3f deformQuadrupedVertex(float[] data, int offset, float limbSwing,
                                                   float limbSwingAmount, float netHeadYaw,
                                                   float headPitch) {
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
        return new Vector3f(rotatedX, rotatedY + pivotY, rotatedZ + pivotZ);
    }

    private static Vector3f deformSpiderVertex(float[] data, int offset, float limbSwing,
                                                float limbSwingAmount) {
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
        return new Vector3f(x, y, z);
    }

    private static Vector3f deformSquidVertex(float[] data, int offset, float waveSin,
                                               float waveCos, float strength) {
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
        return new Vector3f(displacedX, displacedY, displacedZ);
    }

    private static Vector3f deformOctopusVertex(float[] data, int offset, float ageInTicks,
                                                 boolean inWater, byte actionState,
                                                 boolean squeezing) {
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
        return new Vector3f(x, y, z);
    }

    private static Vector3f deformGlowSquidVertex(float[] data, int offset, float waveSin,
                                                   float waveCos, float strength) {
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
        return new Vector3f(displacedX, displacedY, displacedZ);
    }

    private static Vector3f deformAxolotlVertex(float[] data, int offset, float ageInTicks,
                                                 float limbSwing, float limbSwingAmount,
                                                 boolean inWater) {
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

        // Six external gills around the rear of the head flutter independently
        // while remaining part of the exact continuous surface.
        float gillWeight = Mth.clamp((-z - 0.24F) / 0.30F, 0.0F, 1.0F)
                * Mth.clamp((1.31F - y) / 0.24F, 0.0F, 1.0F)
                * Mth.clamp((Math.abs(x) - 0.14F) / 0.25F, 0.0F, 1.0F);
        float gillSide = x < 0.0F ? -1.0F : 1.0F;
        x += gillSide * Mth.sin(ageInTicks * 0.31F + y * 8.0F) * 0.035F * gillWeight;
        y += Mth.cos(ageInTicks * 0.27F + z * 7.0F) * 0.018F * gillWeight;
        return new Vector3f(x, y, z);
    }

    private static Vector3f deformOcelotVertex(float[] data, int offset, float ageInTicks,
                                                float limbSwing, float limbSwingAmount,
                                                float netHeadYaw, float headPitch,
                                                boolean sprinting, boolean airborne) {
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
        return new Vector3f(rotatedX, rotatedY + pivotY, rotatedZ + pivotZ);
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

    private static void emitVertex(float[] data, int offset, Matrix4f pose, Vector3f normal,
                                   VertexConsumer buffer, int packedLight, int packedOverlay) {
        Vector4f position = pose.transform(new Vector4f(
                data[offset], data[offset + 1], data[offset + 2], 1));
        buffer.vertex(position.x(), position.y(), position.z(), 1F, 1F, 1F, 1F,
                data[offset + 3], data[offset + 4], packedOverlay, packedLight,
                normal.x(), normal.y(), normal.z());
    }

    private static void emitVertex(float[] data, int offset, Vector3f modelPosition,
                                   Matrix4f pose, Vector3f normal, VertexConsumer buffer,
                                   int packedLight, int packedOverlay) {
        Vector4f position = pose.transform(new Vector4f(modelPosition, 1));
        buffer.vertex(position.x(), position.y(), position.z(), 1F, 1F, 1F, 1F,
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
