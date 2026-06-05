package com.sheridan.gcr.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface IHardCodeAnimationHandler {

    void applyTransformPre(PoseStack poseStack, IGun gun, float partialTicks, LocalPlayer player);

    void applyTransformPost(PoseStack poseStack, IGun gun, float partialTicks, LocalPlayer player);

    void clientTick(LocalPlayer player);

    void update(float delta);

    void updateOnRenderTick(float particleTicks);
}
