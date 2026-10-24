package com.sheridan.gcr.client.model.modular;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sheridan.gcr.client.model.Bone;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface IArmHandlerModel {
    PoseStack.Pose getPose(boolean rightArm, boolean slim);
    Bone getBone(boolean rightArm, boolean slim);
    boolean has(boolean rightArm);
}
