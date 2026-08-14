package com.Momik.usless_mobs.client;

import com.Momik.usless_mobs.entity.WitchBossEntity;
import net.minecraft.client.model.WitchModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Witch;

public class WitchBossModel<T extends Witch> extends WitchModel<T> {
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart arms;

    public WitchBossModel(ModelPart root) {
        super(root);
        this.head = findChild(root, "head");
        this.body = findChild(root, "body");
        this.arms = findChild(root, "arms");
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

        if (!(entity instanceof WitchBossEntity)) {
            return;
        }

        boolean drinking = entity.isDrinkingPotion();
        float healthFrac = Mth.clamp(entity.getHealth() / entity.getMaxHealth(), 0.0F, 1.0F);
        float menace = 1.0F + (1.0F - healthFrac) * 0.9F;
        float bodySway = Mth.sin(ageInTicks * 0.07F) * 0.04F * menace;

        if (body != null) {
            body.zRot += bodySway;
        }
        if (head != null) {
            head.zRot += bodySway * 0.6F;
            head.yRot += Mth.sin(ageInTicks * 0.05F) * 0.07F;
        }
        if (arms != null && !drinking) {
            float stir = ageInTicks * 0.18F;
            arms.xRot += Mth.sin(stir) * 0.12F * menace;
            arms.zRot += Mth.cos(stir) * 0.08F * menace;
            arms.y += Mth.sin(stir * 0.5F) * 0.4F * menace;
        }
    }
}
