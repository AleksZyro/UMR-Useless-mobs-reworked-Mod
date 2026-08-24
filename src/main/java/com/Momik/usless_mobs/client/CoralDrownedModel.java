package com.Momik.usless_mobs.client;

import net.minecraft.client.model.DrownedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Zombie;

public class CoralDrownedModel<T extends Zombie> extends DrownedModel<T> {
    public CoralDrownedModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        boolean swimming = entity.isInWater();
        float currentStrength = swimming ? 1.0F : 0.35F;
        float slow = ageInTicks * 0.06F;

        float bodySway = Mth.sin(slow) * 0.09F * currentStrength;
        float armDrift = Mth.cos(slow * 1.2F) * 0.18F * currentStrength;
        float legDrift = Mth.sin(slow * 0.9F + 1.0F) * 0.14F * currentStrength;

        this.body.zRot += bodySway;
        this.head.zRot += bodySway * 0.7F;
        this.head.xRot += Mth.sin(slow * 0.7F) * 0.05F;

        if (!entity.isAggressive() || swimming) {
            this.rightArm.zRot += armDrift;
            this.leftArm.zRot -= armDrift;
            this.rightArm.xRot += Mth.cos(slow * 1.1F) * 0.1F * currentStrength;
            this.leftArm.xRot += Mth.sin(slow * 1.1F) * 0.1F * currentStrength;
        }

        this.rightLeg.xRot += legDrift;
        this.leftLeg.xRot -= legDrift;
    }
}
