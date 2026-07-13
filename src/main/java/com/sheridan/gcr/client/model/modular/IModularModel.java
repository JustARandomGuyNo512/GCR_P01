package com.sheridan.gcr.client.model.modular;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sheridan.gcr.client.animation.IAnimated;
import com.sheridan.gcr.client.model.Bone;
import com.sheridan.gcr.client.model.BoneRenderStatus;
import com.sheridan.gcr.client.render.FirstPersonRenderContext;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public interface IModularModel extends IAnimated {

    void render(ModuleRenderContext context);

    @Nullable
    PoseStack.Pose getBonePose(String name);

    @NotNull
    PoseStack.Pose getRootPose();

    @NotNull
    Bone getRootBone();

    Bone getBone(String boneName);

    boolean hasSlot(String name);

    void updateBoneRenderStatus(ModuleRenderContext context);

    void afterAllRendered(ModuleRenderContext context);

    void preFirstPersonRender(FirstPersonRenderContext context);

    void copyRenderStatus(Map<String, BoneRenderStatus> statusStorage);

    IModularModel setHeatMapTexPath(ResourceLocation path);

    @Nullable
    ResourceLocation getHeatMapTexPath();

    void compile(RenderType type);

    IModularModel modifyHeatSensitive(float heatSensitive);
}
