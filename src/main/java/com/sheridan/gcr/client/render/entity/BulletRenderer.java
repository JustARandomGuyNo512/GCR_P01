package com.sheridan.gcr.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sheridan.gcr.Client;
import com.sheridan.gcr.GCR;
import com.sheridan.gcr.client.render.RenderTypes;
import com.sheridan.gcr.client.render.entity.model.CommonTracer;
import com.sheridan.gcr.compat.IrisCompat;
import com.sheridan.gcr.entity.projectile.BulletEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class BulletRenderer extends EntityRenderer<BulletEntity> {
    private static final ResourceLocation TEXTURE = GCR.RL("textures/entity/tracers.png");

    public BulletRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull BulletEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(@NotNull BulletEntity entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        if (IrisCompat.isRenderingShadowPass()) {
            return;
        }
        boolean firstPerson = Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON;
        Vector3f translation = poseStack.last().pose().getTranslation(new Vector3f());
        if (firstPerson) {
            if (entity.getShooterId() != Client.LOCAL_PLAYER_ID && entity.tickCount < 2) {
                return;
            }
            if (translation.length() < 6.5f) {
                return;
            }
        } else {
            if (entity.tickCount < 2) {
                return;
            }
        }
        Vector3f velocity = entity.getEntityData().get(BulletEntity.EXACT_VELOCITY);
        if (velocity.length() < 0.001f) {
            return;
        }

        poseStack.pushPose();

        Camera mainCamera = Minecraft.getInstance().gameRenderer.getMainCamera();
        LocalPlayer player = Minecraft.getInstance().player;

        boolean doFPOffsetTrans = player != null && player.getId() == entity.getShooterId() && firstPerson;

        if (doFPOffsetTrans) {
            Vector3f visualFPMuzzlePos = Client.getGunRenderer().getGunLocalPos();
            if (visualFPMuzzlePos != null) {
                float dist = translation.length();
                float exp = (float) Math.exp((-dist) * 0.07f);
                if (exp > 0.001f) {
                    Vector3f offset = new Vector3f(visualFPMuzzlePos);
                    offset.rotate(mainCamera.rotation());
                    offset.mul(exp);
                    poseStack.translate(offset.x, offset.y, offset.z);
                }
            }
        }


        float yaw = (float) (Mth.atan2(velocity.x, velocity.z) * (180F / Math.PI));
        float hDist = (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        float pitch = (float) (Mth.atan2(velocity.y, hDist) * (180F / Math.PI));

        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        VertexConsumer buffer = bufferSource.getBuffer(RenderTypes.energySwirl(TEXTURE, 0, 0));
        CommonTracer.INSTANCE.renderToBuffer(poseStack, buffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, -1);

        poseStack.popPose();
    }
}
