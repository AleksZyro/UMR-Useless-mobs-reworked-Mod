package net.mysith.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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

/** Loads and renders the approved Tripo triangles inside GeckoLib bone transforms. */
final class CorruptedSilverfishMesh {
    static final ResourceLocation RESOURCE = ResourceLocation.tryBuild(
            "usless_mobs", "meshes/entity/corrupted_silverfish.mesh");
    private static final byte[] MAGIC = new byte[]{'C', 'S', 'M', 'E', 'S', 'H', '1', 0};
    private static final int EXPECTED_FACE_COUNT = 101_723;
    private static final int MAX_BONES = 64;
    private static final int MAX_FACES_PER_BONE = 200_000;
    private static final Set<String> EXPECTED_BONES = Set.of(
            "body", "leg_front_left", "leg_front_right", "leg_middle_left",
            "leg_middle_right", "leg_rear_left", "leg_rear_right");

    private final Map<String, MeshPart> parts;

    private CorruptedSilverfishMesh(Map<String, MeshPart> parts) {
        this.parts = Map.copyOf(parts);
    }

    static CorruptedSilverfishMesh load(ResourceManager resourceManager) throws IOException {
        Resource resource = resourceManager.getResource(RESOURCE)
                .orElseThrow(() -> new IOException("Missing exact Tripo mesh resource: " + RESOURCE));
        try (DataInputStream input = new DataInputStream(new BufferedInputStream(resource.open()))) {
            byte[] magic = input.readNBytes(MAGIC.length);
            if (!Arrays.equals(magic, MAGIC)) {
                throw new IOException("Invalid exact Tripo mesh header: " + RESOURCE);
            }
            int boneCount = readIntLe(input);
            if (boneCount <= 0 || boneCount > MAX_BONES) {
                throw new IOException("Invalid exact Tripo mesh bone count: " + boneCount);
            }

            Map<String, MeshPart> parts = new HashMap<>();
            int totalFaces = 0;
            for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
                int nameLength = readUnsignedShortLe(input);
                if (nameLength <= 0 || nameLength > 128) {
                    throw new IOException("Invalid exact Tripo mesh bone name length: " + nameLength);
                }
                String name = new String(input.readNBytes(nameLength), StandardCharsets.UTF_8);
                if (!EXPECTED_BONES.contains(name) || parts.containsKey(name)) {
                    throw new IOException("Invalid or duplicate exact Tripo mesh bone: " + name);
                }
                int faceCount = readIntLe(input);
                if (faceCount < 0 || faceCount > MAX_FACES_PER_BONE) {
                    throw new IOException("Invalid exact Tripo mesh face count for " + name + ": " + faceCount);
                }
                totalFaces = Math.addExact(totalFaces, faceCount);
                parts.put(name, readPart(input, faceCount));
            }
            if (!parts.keySet().equals(EXPECTED_BONES)) {
                throw new IOException("Exact Tripo mesh bone set does not match the animation rig");
            }
            if (totalFaces != EXPECTED_FACE_COUNT) {
                throw new IOException("Exact Tripo mesh face count must be " + EXPECTED_FACE_COUNT + ", got " + totalFaces);
            }
            if (input.read() != -1) {
                throw new IOException("Exact Tripo mesh contains trailing bytes");
            }
            return new CorruptedSilverfishMesh(parts);
        } catch (EOFException exception) {
            throw new IOException("Exact Tripo mesh is truncated: " + RESOURCE, exception);
        } catch (ArithmeticException exception) {
            throw new IOException("Exact Tripo mesh count overflow: " + RESOURCE, exception);
        }
    }

    private static MeshPart readPart(DataInputStream input, int faceCount) throws IOException {
        float[] data = new float[Math.multiplyExact(faceCount, 18)];
        int cursor = 0;
        for (int face = 0; face < faceCount; face++) {
            int faceStart = cursor;
            for (int vertex = 0; vertex < 3; vertex++) {
                data[cursor++] = -readFiniteFloatLe(input) / 16F;
                data[cursor++] = readFiniteFloatLe(input) / 16F;
                data[cursor++] = readFiniteFloatLe(input) / 16F;
                data[cursor++] = readFiniteFloatLe(input) / 16F;
                data[cursor++] = readFiniteFloatLe(input) / 16F;
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
        return new MeshPart(data, faceCount);
    }

    void renderBone(String boneName, PoseStack poseStack, VertexConsumer buffer,
                    int packedLight, int packedOverlay,
                    float red, float green, float blue, float alpha) {
        MeshPart part = this.parts.get(boneName);
        if (part == null) {
            return;
        }
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();
        float[] data = part.data();
        int cursor = 0;
        for (int face = 0; face < part.faceCount(); face++) {
            int verticesStart = cursor;
            int normalStart = cursor + 15;
            Vector3f transformedNormal = normalMatrix.transform(new Vector3f(
                    data[normalStart], data[normalStart + 1], data[normalStart + 2]));
            emitVertex(data, verticesStart, pose, transformedNormal, buffer,
                    packedLight, packedOverlay, red, green, blue, alpha);
            emitVertex(data, verticesStart + 5, pose, transformedNormal, buffer,
                    packedLight, packedOverlay, red, green, blue, alpha);
            emitVertex(data, verticesStart + 10, pose, transformedNormal, buffer,
                    packedLight, packedOverlay, red, green, blue, alpha);
            emitVertex(data, verticesStart + 10, pose, transformedNormal, buffer,
                    packedLight, packedOverlay, red, green, blue, alpha);
            cursor += 18;
        }
    }

    private static void emitVertex(float[] data, int offset, Matrix4f pose, Vector3f normal,
                                   VertexConsumer buffer, int packedLight, int packedOverlay,
                                   float red, float green, float blue, float alpha) {
        Vector4f position = pose.transform(new Vector4f(
                data[offset], data[offset + 1], data[offset + 2], 1));
        buffer.vertex(position.x(), position.y(), position.z(), red, green, blue, alpha,
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

    private record MeshPart(float[] data, int faceCount) {}
}
