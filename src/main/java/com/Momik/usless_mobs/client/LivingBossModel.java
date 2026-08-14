package com.Momik.usless_mobs.client;

import com.Momik.usless_mobs.entity.LivingBossEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.RavagerModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Ravager;

public class LivingBossModel extends RavagerModel {
    private final ModelPart head;
    private final ModelPart neck;
    private final ModelPart body;

    public LivingBossModel(ModelPart root) {
        super(root);
        this.head = findChild(root, "head");
        this.neck = findChild(root, "neck");
        this.body = findChild(root, "body");
    }

    private static ModelPart findChild(ModelPart root, String name) {
        try {
            return root.getChild(name);
        } catch (java.util.NoSuchElementException ex) {
            return null;
        }
    }

    @Override
    public void setupAnim(Ravager entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        if (!(entity instanceof LivingBossEntity)) {
            return;
        }

        float healthFrac = Mth.clamp(entity.getHealth() / entity.getMaxHealth(), 0.0F, 1.0F);
        float urgency = 1.0F + (1.0F - healthFrac) * 1.6F;
        float pulse = (Mth.sin(ageInTicks * 0.22F * urgency) + 1.0F) * 0.5F;
        float deepPulse = Mth.sin(ageInTicks * 0.11F * urgency);
        float scale = 1.0F + pulse * 0.04F;

        if (body != null) {
            body.y += deepPulse * 0.25F;
            body.xScale = scale;
            body.zScale = scale;
        }
        if (neck != null) {
            neck.xRot += deepPulse * 0.05F;
        }
        if (head != null) {
            head.xRot += Mth.sin(ageInTicks * 0.13F) * 0.06F;
            head.yRot += Mth.cos(ageInTicks * 0.09F) * 0.09F;
        }
    }
}
