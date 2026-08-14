package com.Momik.usless_mobs.client;

import com.Momik.usless_mobs.entity.OctopusEntity;
import net.minecraft.client.model.SquidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

public class OctopusModel<T extends OctopusEntity> extends SquidModel<T> {
    private final ModelPart[] tentacles = new ModelPart[8];

    public OctopusModel(ModelPart root) {
        super(root);
        for (int i = 0; i < 8; i++) {
            this.tentacles[i] = findChild(root, "tentacle" + i);
        }
    }

    private static ModelPart findChild(ModelPart root, String name) {
        try {
            return root.getChild(name);
        } catch (java.util.NoSuchElementException ex) {
            return null;
        }
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        float time = ageInTicks * 0.18F;
        float inWater = entity.isInWater() ? 1.0F : 0.4F;

        for (int i = 0; i < 8; i++) {
            ModelPart t = tentacles[i];
            if (t == null) continue;
            float phase = i * (Mth.PI / 4.0F);
            float wave = Mth.sin(time + phase) * 0.45F * inWater;
            float spread = Mth.cos(time * 0.6F + phase) * 0.25F * inWater;
            t.xRot += wave;
            t.zRot += spread;
        }
    }
}
