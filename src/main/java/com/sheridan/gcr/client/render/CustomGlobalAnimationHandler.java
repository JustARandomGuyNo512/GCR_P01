package com.sheridan.gcr.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 对于自定义动画的全局动画处理器实现
 * */
@OnlyIn(Dist.CLIENT)
public class CustomGlobalAnimationHandler implements IGlobalAnimationHandler{
    @Override
    public void applyTransformPre(PoseStack poseStack, IGun gun, float partialTicks, LocalPlayer player) {

    }

    @Override
    public void applyTransformPost(PoseStack poseStack, IGun gun, float partialTicks, LocalPlayer player) {

    }

    @Override
    public void clientTick(LocalPlayer player) {

    }

    @Override
    public void update(float delta) {

    }

    @Override
    public void updateOnRenderTick(float particleTicks) {

    }
}
