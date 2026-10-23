package com.sheridan.gcr.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sheridan.gcr.Client;
import com.sheridan.gcr.GCR;
import com.sheridan.gcr.Utils;
import com.sheridan.gcr.network.c2s.PlaySoundPacket;
import com.sheridan.gcr.sound.ModSounds;
import com.sheridan.gcr.client.model.BoneRenderStatus;
import com.sheridan.gcr.client.model.modular.IGunModel;
import com.sheridan.gcr.client.model.modular.IScopeModel;
import com.sheridan.gcr.client.model.modular.ISightModel;
import com.sheridan.gcr.items.DisplayData;
import com.sheridan.gcr.modularSys.modules.ISight;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class GunPoseHandler {
    public static final GunPoseHandler INSTANCE = new GunPoseHandler();

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
    /** Real-time playback of the aim clip. Independent of how fast the sight aligns. */
    private float punchTime = AIM_LENGTH;
    private float punchTimeLast = AIM_LENGTH;
    private boolean wasAiming;

    private float scopeRearLensZ = Float.NaN;
    private float toFovModifier = 70;
    private float fromFovModifier = 70;

    public GunPoseHandler() {}

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
        if (Client.WEAPON_STATUS.isAiming()) {
            restartPunch();
        }
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
        switchProgress = Math.min(1, switchProgress + 0.3f);
        boolean aiming = Client.WEAPON_STATUS.isAiming();
        if (aiming != wasAiming) {
            restartPunch();
            wasAiming = aiming;
        }
        punchTimeLast = punchTime;
        if (punchTime < AIM_LENGTH) {
            punchTime = Math.min(AIM_LENGTH, punchTime + 0.05f);
        }
        if (punchTimeLast < AIM_SOUND_TIME && punchTime >= AIM_SOUND_TIME) {
            playAimSound(player);
        }
    }

    private void restartPunch() {
        punchTime = 0f;
        punchTimeLast = 0f;
    }

    /** The aim clip's one sound key, m4new_smallmove3, at 0.11s. */
    private static final float AIM_SOUND_TIME = 0.11f;
    private static final ResourceLocation AIM_SOUND = GCR.RL("m4new_smallmove3");

    private void playAimSound(LocalPlayer player) {
        if (player == null) {
            return;
        }
        ModSounds.sound(1f, 1f, player, AIM_SOUND);
        PacketDistributor.sendToServer(new PlaySoundPacket(
                AIM_SOUND.toString(), 1f, 1f, player.getX(), player.getY(), player.getZ()));
    }

    public float[] getCurrentPos(float adsProgress) {
        lerp(adsProgress, fromPos, toPos, currentPos);

        return currentPos;
    }

    @Deprecated
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

    /**
     * Authored {@code aim} clip from the M4 viewmodel. Values are Blockbench pixels and degrees.
     * The curve starts and ends at rest, so it is added on top of the sight alignment.
     * Column 4 is 1 when the segment leaving that key is catmull-rom.
     */
    private static final float AIM_LENGTH = 0.6f;
    private static final float BLOCKBENCH_PIXEL = 1f / 16f;
    private static final float[][] AIM_POS = {
            {0f, 0f, 0f, 0f, 0f},
            {0.11f, 0f, -1.195f, -3.005f, 1f},
            {0.23f, 0f, 0.055f, -3.005f, 1f},
            {0.34f, 0f, 0f, -0.2f, 1f},
            {0.44f, 0f, 0f, 0f, 1f},
            {0.54f, 0f, 0f, 0f, 1f},
            {0.6f, 0f, 0f, 0f, 0f}
    };
    private static final float[][] AIM_ROT = {
            {0f, 0f, 0f, 0f, 0f},
            {0.11f, 0f, 0f, 2.05f, 1f},
            {0.2f, 0f, 0f, 2.05f, 1f},
            {0.33f, 0f, 0f, -0.4f, 1f},
            {0.45f, 0f, 0f, 0.125f, 1f},
            {0.55f, 0f, 0f, 0f, 1f},
            {0.6f, 0f, 0f, 0f, 0f}
    };
    private static final float[][] AIM_CAM_ROT = {
            {0f, 0f, 0f, 0f, 0f},
            {0.17f, -0.075f, 0f, -0.05f, 1f},
            {0.33f, -0.15f, 0f, -0.1f, 1f},
            {0.36f, 0.1f, 0f, 0.125f, 1f},
            {0.46f, 0f, 0f, 0f, 1f},
            {0.6f, 0f, 0f, 0f, 0f}
    };
    private void lerp(float progress, float[] from, float[] to, float[] out) {
        out[0] = from[0] + (to[0] - from[0]) * progress;
        out[1] = from[1] + (to[1] - from[1]) * progress;
        out[2] = from[2] + (to[2] - from[2]) * progress;
    }

    /** keys are {time, x, y, z, catmull}. {@code component} is 1, 2, or 3. */
    private static float sample(float[][] keys, int component, float time) {
        int last = keys.length - 1;
        if (time <= keys[0][0]) {
            return keys[0][component];
        }
        if (time >= keys[last][0]) {
            return keys[last][component];
        }
        int i = 0;
        while (i < last - 1 && time >= keys[i + 1][0]) {
            i++;
        }
        float span = keys[i + 1][0] - keys[i][0];
        float u = span <= 1.0e-6f ? 0f : (time - keys[i][0]) / span;
        float p1 = keys[i][component];
        float p2 = keys[i + 1][component];
        if (keys[i][4] < 0.5f) {
            return Mth.lerp(u, p1, p2);
        }
        float p0 = keys[Math.max(0, i - 1)][component];
        float p3 = keys[Math.min(last, i + 2)][component];
        return catmull(u, p0, p1, p2, p3);
    }

    private static float catmull(float u, float p0, float p1, float p2, float p3) {
        float u2 = u * u;
        float u3 = u2 * u;
        return 0.5f * ((2f * p1)
                + (-p0 + p2) * u
                + (2f * p0 - 5f * p1 + 4f * p2 - p3) * u2
                + (-p0 + 3f * p1 - 3f * p2 + p3) * u3);
    }

    /** Aim-clip camera nod, in radians, added to the view camera. */
    public void addAimCamera(float partialTicks, Vector3f cameraRot) {
        float punch = Mth.lerp(partialTicks, punchTimeLast, punchTime);
        cameraRot.add(
                (float) Math.toRadians(sample(AIM_CAM_ROT, 1, punch)),
                (float) Math.toRadians(sample(AIM_CAM_ROT, 2, punch)),
                (float) Math.toRadians(sample(AIM_CAM_ROT, 3, punch)));
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
        float punch = Mth.lerp(partialTicks, punchTimeLast, punchTime);
        boolean punchPlaying = punch > 1.0e-4f && punch < AIM_LENGTH - 1.0e-4f;
        if (aimingProgress <= 1e-6 && !punchPlaying) {
            displayData.applyFirstPersonTranslation(poseStack);
        } else {
            float switchProgress = getSwitchProgress(partialTicks);
            float[] fpTrans = displayData.getFirstPersonTranslate();
            float[] pos = getCurrentPos(switchProgress);
            float[] rot = getCurrentRot(switchProgress);

            float align = aimingProgress;
            float kickX = sample(AIM_POS, 1, punch) * BLOCKBENCH_PIXEL;
            float kickY = sample(AIM_POS, 2, punch) * BLOCKBENCH_PIXEL;
            float kickZ = sample(AIM_POS, 3, punch) * BLOCKBENCH_PIXEL;
            float kickRx = (float) Math.toRadians(sample(AIM_ROT, 1, punch));
            float kickRy = (float) Math.toRadians(sample(AIM_ROT, 2, punch));
            float kickRz = (float) Math.toRadians(sample(AIM_ROT, 3, punch));

            rotQuat.rotateXYZ(
                    Mth.lerp(align, fpTrans[3], rot[0]) + kickRx,
                    Mth.lerp(align, fpTrans[4], rot[1]) + kickRy,
                    Mth.lerp(align, fpTrans[5], rot[2]) + kickRz);

            poseStack.mulPose(rotQuat);
            poseStack.translate(
                    Mth.lerp(align, fpTrans[0], pos[0]) + kickX,
                    Mth.lerp(align, fpTrans[1], pos[1]) + kickY,
                    Mth.lerp(align, fpTrans[2], pos[2]) + kickZ);

            poseStack.scale(fpTrans[6], fpTrans[7], fpTrans[8]);

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
        float farthestZSide = -9999;
        if (context.root.model instanceof IGunModel gunModel) {
            String farthestSightZName = gunModel.getFarthestSightZName(context);
            String farthestSideSightZName = gunModel.getFarthestSideSightZName(context);
            if (rootPoseMap != null && (farthestSightZName != null || farthestSideSightZName != null)) {
                BoneRenderStatus status = rootPoseMap.get(farthestSightZName);
                if (status != null) {
                    Matrix4f mat = status.pose.pose();
                    Vector3f translation = mat.getTranslation(new Vector3f());
                    farthestZ = translation.z;
                }
                status = rootPoseMap.get(farthestSideSightZName);
                if (status != null) {
                    Matrix4f mat = status.pose.pose();
                    Vector3f translation = mat.getTranslation(new Vector3f());
                    farthestZSide = translation.z;
                } else {
                    farthestZSide = farthestZ;
                }
            }
        }
        for (ModuleRenderNode node : tempPoseMap.keySet()) {
            if (usingSightID.equals(node.id) && node.model instanceof ISightModel model) {
                Map<String, BoneRenderStatus> bonePoseMap = tempPoseMap.get(node);
                String rearLensBoneName = null;
                if (model instanceof IScopeModel scopeModel) {
                    rearLensBoneName = scopeModel.getRearLensBone().name;
                }
                Map<String, BoneRenderStatus> stringPoseMap = tempPoseMap.get(node);
                if (stringPoseMap == null) {
                    setNoSight();
                    break;
                }
                BoneRenderStatus sightPoseStatus = bonePoseMap.get(model.getSightPoseBoneName(context));
                BoneRenderStatus rearLensStatus = stringPoseMap.get(rearLensBoneName);
                if (sightPoseStatus != null) {
                    Matrix4f mat = sightPoseStatus.pose.pose();
                    Vector3f translation = mat.getTranslation(new Vector3f());
                    Quaternionf q = Utils.extractPureRotation(mat);
                    Vector3f euler = q.getEulerAnglesXYZ(new Vector3f());
                    float z = Math.max(
                            translation.z,
                            node.getCustomParam(ISight.ON_SIDE_POSITION) == -1 ? farthestZ : farthestZSide
                    );
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
