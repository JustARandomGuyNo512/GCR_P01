package com.sheridan.gcr.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sheridan.gcr.client.render.entity.model.GP25;
import com.sheridan.gcr.client.render.entity.model.M433;
import com.sheridan.gcr.entity.projectile.GrenadeEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class GrenadeRenderer extends EntityRenderer<GrenadeEntity> {
    public GrenadeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull GrenadeEntity entity) {
        if (entity.modelType == 0) {
            return M433.TEXTURE;
        } else {
            return GP25.TEXTURE;
        }
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
        if (entity.modelType == 0) {
            M433.INSTANCE.render(poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY);
        } else {
            GP25.INSTANCE.render(poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY);
        }

        poseStack.popPose();
    }
}
