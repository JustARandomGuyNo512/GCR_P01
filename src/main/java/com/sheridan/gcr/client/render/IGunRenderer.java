package com.sheridan.gcr.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sheridan.gcr.client.model.modular.animation.eventSys.EventType;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public interface IGunRenderer {
    void renderFirstPerson(LocalPlayer entity, ItemStack itemStack, IGun gun, PoseStack poseStack, int light, int overlay);

    void renderOther(LivingEntity entity, ItemStack itemStack, IGun gun, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay);

    void dispatchAnimationEvent(EventType eventType);

    void dispatchAnimationEvent(EventType eventType, String ... params);

    void dispatchAnimationEvent(EventType eventType, @Nullable Map<String, String> params);

    void tick(LocalPlayer player);

    void setHideFPRender(boolean hide);

    boolean isHideFPRender();

    void renderGunModifyScreen(ItemStack itemStack, IGun gun, ModuleRenderNode node, float x, float y, float rx, float ry, float scale, Consumer<ModifyScreenRenderContext> guiCallback);

    float getSightPoseDistance();

    Matrix4f firstPersonModelViewMat();

    Vector3f getGunLocalPos();

}
