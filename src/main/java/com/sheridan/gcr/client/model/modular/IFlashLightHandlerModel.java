package com.sheridan.gcr.client.model.modular;

import com.sheridan.gcr.client.DrawHolsterHandler;
import com.sheridan.gcr.client.model.Bone;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.client.render.fx.FlashLightRenderer;
import com.sheridan.gcr.compat.IrisCompat;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public interface IFlashLightHandlerModel {
    Vector3f DIRECTION = new Vector3f(0, 0, -1);
    Vector3f DIRECTION_LOCAL = new Vector3f(0, 0, -1);
    float MAX_ANGLE_DEG = 8f;
    float COS_THRESHOLD = (float) Math.cos(Math.toRadians(MAX_ANGLE_DEG));

    Bone getLightDirPoseBone();

    default void handleFlashLightEffect(ModuleRenderContext context, float luminance, float range, float angle, float partialTicks) {
        if (!context.isFirstPerson() || IrisCompat.isRenderingShadowPass()) {
            return;
        }
        float equipProgress = DrawHolsterHandler.get().getEquipProgress(partialTicks);
        if (equipProgress < 0.1f) {
            return;
        }
        Bone lightDirPoseBone = getLightDirPoseBone();
        if (lightDirPoseBone == null) {
            return;
        }
        Matrix4f globalPose = context.poseStack.last().pose();
        DIRECTION.set(0, 0, -1);
        globalPose.transformDirection(DIRECTION);
        DIRECTION.normalize();

        Matrix4f localPose = lightDirPoseBone.renderStatus.pose.pose();
        DIRECTION_LOCAL.set(0, 0, -1);
        localPose.transformDirection(DIRECTION_LOCAL);
        DIRECTION_LOCAL.normalize();

        float dot = DIRECTION.dot(DIRECTION_LOCAL);
        dot = Mth.clamp(dot, -1.0f, 1.0f);
        float factor = (dot - COS_THRESHOLD) / (1.0f - COS_THRESHOLD);
        factor = Mth.clamp(factor, 0.0f, 1.0f);

        FlashLightRenderer.recordEffectCall(luminance * factor, range * factor, angle * factor, DIRECTION);
    }
}
