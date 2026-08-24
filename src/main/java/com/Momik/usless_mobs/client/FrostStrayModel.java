package com.Momik.usless_mobs.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.RangedAttackMob;

public class FrostStrayModel<T extends Mob & RangedAttackMob> extends SkeletonModel<T> {
    public FrostStrayModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        float shiver = (Mth.cos(ageInTicks * 1.7F) + Mth.cos(ageInTicks * 2.31F)) * 0.025F;
        float fineShiver = Mth.cos(ageInTicks * 3.7F) * 0.018F;
        float hurtBoost = 1.0F + (1.0F - Mth.clamp(entity.getHealth() / entity.getMaxHealth(), 0.0F, 1.0F)) * 1.5F;

        this.head.xRot += fineShiver * hurtBoost;
        this.head.zRot += shiver * 0.7F * hurtBoost;
        this.body.zRot += shiver * 0.4F * hurtBoost;
        this.rightArm.zRot += shiver * hurtBoost;
        this.leftArm.zRot -= shiver * hurtBoost;
        this.rightLeg.xRot += fineShiver * 0.3F;
        this.leftLeg.xRot -= fineShiver * 0.3F;
    }
}
