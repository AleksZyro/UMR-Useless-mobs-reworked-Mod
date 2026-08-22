package com.Momik.usless_mobs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.Momik.usless_mobs.Usless_mobs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
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
    private static final int MAX_FACES_PER_BONE = 400_000;

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
