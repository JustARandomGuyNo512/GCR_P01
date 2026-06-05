package com.sheridan.gcr.client.model.modular;

import com.mojang.blaze3d.vertex.PoseStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public interface IMuzzleFlashRendererModel {
    @Nullable
    PoseStack.Pose getBonePose(String name);

    IMuzzleFlashRenderer getRenderer();
}
