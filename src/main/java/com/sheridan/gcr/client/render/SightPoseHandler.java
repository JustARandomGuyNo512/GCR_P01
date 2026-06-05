package com.sheridan.gcr.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sheridan.gcr.Client;
import com.sheridan.gcr.Utils;
import com.sheridan.gcr.client.model.BoneRenderStatus;
import com.sheridan.gcr.client.model.modular.IGunModel;
import com.sheridan.gcr.client.model.modular.IScopeModel;
import com.sheridan.gcr.client.model.modular.ISightModel;
import com.sheridan.gcr.items.DisplayData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class SightPoseHandler {
    public static final SightPoseHandler INSTANCE = new SightPoseHandler();

    private final float D_TAN_35 = (float) (1.0 / Math.tan(Math.toRadians(35)));
    private final float[] fromPos = new float[] {0, 0, 0};
    private final float[] fromRot = new float[] {0, 0, 0};

    private final float[] toPos = new float[] {0, 0, 0};
    private final float[] toRot = new float[] {0, 0, 0};

    private final float[] currentPos = new float[] {0, 0, 0};
    private final float[] currentRot = new float[] {0, 0, 0};

    private boolean noSight = true;

    private final Quaternionf rotQuat = new Quaternionf();

    private float switchProgress = 0;
    private float switchProgressLast = 0;

    private float scopeRearLensZ = Float.NaN;
    private float toFovModifier = 70;
    private float fromFovModifier = 70;

    public SightPoseHandler() {}

    public void switchToPos(float x, float y, float z, float rx, float ry, float rz) {
        fromPos[0] = currentPos[0];
        fromPos[1] = currentPos[1];
        fromPos[2] = currentPos[2];

        fromRot[0] = currentRot[0];
        fromRot[1] = currentRot[1];
        fromRot[2] = currentRot[2];

        toPos[0] = x;
        toPos[1] = y;
        toPos[2] = z;

        toRot[0] = rx;
        toRot[1] = ry;
        toRot[2] = rz;
        noSight = false;
        switchProgress = 0;
        switchProgressLast = 0;
    }

    public void switchToFov(float fovModifier) {
        fromFovModifier = toFovModifier;
        toFovModifier = fovModifier;
    }

    public float getPosX() {
        return currentPos[0];
    }

    public float getPosY() {
        return currentPos[1];
    }

    public float getPosZ() {
        return currentPos[2];
    }

    public float getPosRotX() {
        return currentRot[0];
    }

    public float getPosRotY() {
        return currentRot[1];
    }

    public float getPosRotZ() {
        return currentRot[2];
    }

    public void tick(LocalPlayer player) {
        switchProgressLast = switchProgress;
        switchProgress = Math.min(1, switchProgress + 1 * 0.3f);
    }

    public float[] getCurrentPos(float adsProgress) {
        lerp(adsProgress, fromPos, toPos, currentPos);

        return currentPos;
    }

    public float getCurrentFov(float adsProgress) {
        return Mth.lerp(adsProgress, fromFovModifier, toFovModifier);
    }

    public float[] getCurrentRot(float adsProgress) {
        lerp(adsProgress, fromRot, toRot, currentRot);
        return currentRot;
    }

    public void setNoSight() {
        noSight = true;
        scopeRearLensZ = Float.NaN;
        toFovModifier = 70;
        Arrays.fill(currentRot, 0);
        Arrays.fill(currentPos, 0);
        Arrays.fill(fromPos, 0);
        Arrays.fill(fromRot, 0);
        Arrays.fill(toPos, 0);
        Arrays.fill(toRot, 0);
    }

    private void lerp(float progress, float[] from, float[] to, float[] out) {
        out[0] = from[0] + (to[0] - from[0]) * progress;
        out[1] = from[1] + (to[1] - from[1]) * progress;
        out[2] = from[2] + (to[2] - from[2]) * progress;
    }

    public float getSwitchProgress(float partialTicks) {
        return Mth.lerp(partialTicks, switchProgressLast, switchProgress);
    }

    public void handleFirstPersonTransform(PoseStack poseStack, DisplayData displayData, float partialTicks) {
        if (noSight) {
            displayData.applyFirstPersonTranslation(poseStack);
            return;
        }
        float aimingProgress = Client.WEAPON_STATUS.getAimingProgress(partialTicks);
        if (aimingProgress <= 1e-6) {
            displayData.applyFirstPersonTranslation(poseStack);
        } else {
            float switchProgress = getSwitchProgress(partialTicks);
            float[] fpTrans = displayData.getFirstPersonTranslate();
            float[] pos = getCurrentPos(switchProgress);
            float[] rot = getCurrentRot(switchProgress);
            //float currentFov = getCurrentFov(switchProgress);
            float k = 1;//(float) (Math.tan(Math.toRadians(currentFov / 2f)) * D_TAN_35);
            //k = Mth.lerp(aimingProgress * aimingProgress, 1, k);

            rotQuat.rotateXYZ(
                    Mth.lerp(aimingProgress, fpTrans[3], rot[0]),
                    Mth.lerp(aimingProgress, fpTrans[4], rot[1]),
                    Mth.lerp(aimingProgress, fpTrans[5], rot[2]));

            poseStack.mulPose(rotQuat);

            poseStack.translate(
                    Mth.lerp(aimingProgress, fpTrans[0], pos[0]),
                    Mth.lerp(aimingProgress * aimingProgress, fpTrans[1], pos[1]),
                    Mth.lerp(aimingProgress, fpTrans[2], pos[2]));

            if (!Float.isNaN(scopeRearLensZ)) {
                float rawPos = scopeRearLensZ;
                float zoomedPos = k * scopeRearLensZ;
                float dist = Math.abs(rawPos - zoomedPos);
                poseStack.translate(0, 0, dist);
            }

            poseStack.scale(fpTrans[6], fpTrans[7], fpTrans[8]);

            poseStack.scale(1, 1, k);

            rotQuat.x = 0;
            rotQuat.y = 0;
            rotQuat.z = 0;
            rotQuat.w = 1;
        }
    }

    public void calculateSightPose(String usingSightID, FirstPersonRenderContext context, Runnable renderCallback) {
        if (context == null) {
            setNoSight();
            return;
        }
        DisplayData displayData = context.gun.getDisplayData();
        if (displayData == null) {
            setNoSight();
            return;
        }

        context.renderMode = false;
        context.saveTempPose = true;
        scopeRearLensZ = Float.NaN;
        int indexFor = displayData.getIndexFor(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND);
        float scaleX = displayData.get(indexFor, 6);
        float scaleY = displayData.get(indexFor, 7);
        float scaleZ = displayData.get(indexFor, 8);
        PoseStack poseStack = new PoseStack();
        poseStack.scale(scaleX, scaleY, scaleZ);
        context.frameUpdate(poseStack, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0f);
        renderCallback.run();
        Map<ModuleRenderNode, Map<String, BoneRenderStatus>> tempPoseMap = context.getRenderStatusMap();
        Map<String, BoneRenderStatus> rootPoseMap = tempPoseMap.get(context.root);
        float farthestZ = -9999;
        if (context.root.model instanceof IGunModel gunModel) {
            String farthestSightZName = gunModel.getFarthestSightZName(context);
            if (rootPoseMap != null && farthestSightZName != null) {
                BoneRenderStatus status = rootPoseMap.get(farthestSightZName);
                if (status != null) {
                    Matrix4f mat = status.pose.pose();
                    Vector3f translation = mat.getTranslation(new Vector3f());
                    farthestZ = translation.z;
                }
            }
        }
        for (ModuleRenderNode node : tempPoseMap.keySet()) {
            if (usingSightID.equals(node.id) && node.model instanceof ISightModel model) {
                String rearLensBoneName = null;
                if (model instanceof IScopeModel scopeModel) {
                    rearLensBoneName = scopeModel.getRearLensBone().name;
                    switchToFov(scopeModel.getFovModify());
                } else {
                    switchToFov(70);
                }
                String sightPoseBoneName = model.getSightPoseBoneName(context);
                Map<String, BoneRenderStatus> stringPoseMap = tempPoseMap.get(node);
                if (stringPoseMap == null) {
                    setNoSight();
                    break;
                }
                BoneRenderStatus sightPoseStatus = stringPoseMap.get(sightPoseBoneName);
                BoneRenderStatus rearLensStatus = stringPoseMap.get(rearLensBoneName);
                if (sightPoseStatus != null) {
                    Matrix4f mat = sightPoseStatus.pose.pose();
                    Vector3f translation = mat.getTranslation(new Vector3f());
                    Quaternionf q = Utils.extractPureRotation(mat);
                    Vector3f euler = q.getEulerAnglesXYZ(new Vector3f());
                    float z = Math.max(translation.z, farthestZ);
                    scopeRearLensZ = z;

                    if (rearLensStatus != null) {
                        Matrix4f mat2 = rearLensStatus.pose.pose();
                        Vector3f translation2 = mat2.getTranslation(new Vector3f());
                        scopeRearLensZ = translation2.z;
                    }

                    switchToPos(
                            -translation.x,
                            -translation.y,
                            -z,
                            -euler.x,
                            -euler.y,
                            -euler.z
                    );
                }
            }
        }
        context.renderMode = true;
        context.saveTempPose = false;
    }
}
