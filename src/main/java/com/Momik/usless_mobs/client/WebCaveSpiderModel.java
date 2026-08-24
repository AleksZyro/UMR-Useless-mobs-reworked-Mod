package com.Momik.usless_mobs.client;

import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public class WebCaveSpiderModel<T extends Entity> extends SpiderModel<T> {
    private final ModelPart head;
    private final ModelPart body0;
    private final ModelPart body1;

    public WebCaveSpiderModel(ModelPart root) {
        super(root);
        this.head = findChild(root, "head");
        this.body0 = findChild(root, "body0");
        this.body1 = findChild(root, "body1");
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

        float pulse = Mth.sin(ageInTicks * 0.18F);
        float fastPulse = Mth.cos(ageInTicks * 0.42F);

        if (body1 != null) {
            body1.zRot += pulse * 0.04F;
            body1.y += fastPulse * 0.35F;
        }
        if (body0 != null) {
            body0.zRot += pulse * 0.02F;
        }
        if (head != null) {
            head.zRot += Mth.cos(ageInTicks * 0.3F) * 0.06F;
            head.yRot += Mth.sin(ageInTicks * 0.21F) * 0.08F;
        }
    }
}
