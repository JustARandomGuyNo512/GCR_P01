package com.sheridan.gcr.client.recoil;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface IRecoilCameraHandler {
    void update(float deltaTicks);

    void clear();

    void onBobbingView(PoseStack poseStack, float partialTicks, IGun gun);

    float getUp();
}
