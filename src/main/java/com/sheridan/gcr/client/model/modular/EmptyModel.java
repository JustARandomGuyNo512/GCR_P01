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
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Map;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public final class EmptyModel implements IModularModel{
    public static final EmptyModel INSTANCE = new EmptyModel();
    private final Bone rootBone = new Bone(0, "root", 0, null);

    @Override
    public void render(ModuleRenderContext context) {}

    @Override
    public @Nullable PoseStack.Pose getBonePose(String name) {
        return null;
    }

    @Override
    public @NotNull PoseStack.Pose getRootPose() {
        return new PoseStack().last();
    }

    @Override
    public @NotNull Bone getRootBone() {
        return rootBone;
    }

    @Override
    public Bone getBone(String boneName) {
        return "root".equals(boneName) ? rootBone : null;
    }

    @Override
    public boolean hasSlot(String name) {
        return false;
    }

    @Override
    public void updateBoneRenderStatus(ModuleRenderContext context) {}

    @Override
    public void afterAllRendered(ModuleRenderContext context) {}

    @Override
    public void preFirstPersonRender(FirstPersonRenderContext context) {

    }

    @Override
    public void copyRenderStatus(Map<String, BoneRenderStatus> statusStorage) {

    }

    @Override
    public IModularModel setHeatMapTexPath(ResourceLocation path) {
        return this;
    }

    @Override
    public @Nullable ResourceLocation getHeatMapTexPath() {
        return null;
    }

    @Override
    public void compile(RenderType type) {

    }

    @Override
    public void offsetPos(Vector3f vector3f) {}

    @Override
    public void offsetRotation(Vector3f vector3f) {}

    @Override
    public void offsetScale(Vector3f vector3f) {}

    @Override
    public Optional<IAnimated> findByName(String pName) {
        return Optional.empty();
    }

}
