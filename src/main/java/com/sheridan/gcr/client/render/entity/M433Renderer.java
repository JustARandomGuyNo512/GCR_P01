package com.sheridan.gcr.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sheridan.gcr.client.render.RenderTypes;
import com.sheridan.gcr.client.render.entity.model.CommonTracer;
import com.sheridan.gcr.client.render.entity.model.M433;
import com.sheridan.gcr.entity.projectile.BulletEntity;
import com.sheridan.gcr.entity.projectile.GrenadeEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class M433Renderer extends EntityRenderer<GrenadeEntity> {
    public M433Renderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull GrenadeEntity entity) {
        return M433.TEXTURE;
    }

    @Override
    public void render(@NotNull GrenadeEntity entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        if (entity.tickCount < 3) {
            return;
        }
        poseStack.pushPose();
        poseStack.scale(0.25f, 0.25f, 0.25f);
        poseStack.mulPose(Axis.YP.rotationDegrees(
                Mth.lerp(partialTick, entity.yRotO + 180,
                        entity.getYRot() + 180
                )
        ));
        poseStack.mulPose(Axis.XP.rotationDegrees(
                Mth.lerp(partialTick, entity.xRotO,
                        entity.getXRot()
                )
        ));
        M433.INSTANCE.render(poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
