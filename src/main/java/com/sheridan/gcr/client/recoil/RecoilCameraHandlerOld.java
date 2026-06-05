package com.sheridan.gcr.client.recoil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sheridan.gcr.Client;
import com.sheridan.gcr.GCR;
import com.sheridan.gcr.Utils;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;


@OnlyIn(Dist.CLIENT)

public class RecoilCameraHandlerOld implements IRecoilCameraHandler {
    private static IRecoilCameraHandler INSTANCE;
    private static final float QUARTER_PI = (float) Math.PI / 4;
    private float lastRawXRot;
    private float lastRawYRot;
    private float lastAdditionalXRot;
    private float lastAdditionalYRot;
    private float up;
    private float upSpeed;
    private float random;
    private float randomSpeed;
    private float shake;
    float recoilControlPitch = 0.08f;
    float recoilControlYaw = 0.1f;
    public static float impact = 1.5f;


    public void onShoot(IGun gun, ItemStack itemStack, float randomDirectionX, float randomDirectionY, float yRate, float pRate) {
        //float recentUnstableFactor = RecoilAnimationHandler.INSTANCE.getRecentUnstableFactor();
        upSpeed -= impact;
        randomSpeed += 0.1f * randomDirectionY;
        shake = 0.015f * randomDirectionX;
    }

    @Override
    public void update(float deltaTicks) {
        recoilUpdate();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            float xRot = player.getXRot() - lastAdditionalXRot;
            player.setXRot(xRot + up * 0.25f);
            lastAdditionalXRot = up * 0.25f;
        }
    }


    private void recoilUpdate() {
        upSpeed *= Math.clamp((1 - recoilControlPitch), 0, 1);
        up *= Math.clamp((1 - recoilControlPitch), 0, 1);
        up += upSpeed;
        randomSpeed *= Math.clamp((1 - recoilControlYaw), 0, 1);
        randomSpeed -= random * (recoilControlYaw * 0.25f);
        random *= Math.clamp((1 - recoilControlYaw), 0, 1);
        random += randomSpeed;
    }


    @Override

    public void clear() {

    }


    @Override
    public void onBobbingView(PoseStack poseStack, float partialTicks, IGun gun) {
        float timeDis = Client.distFromLastShoot();
        float rotZ = (float) Utils.dampedOscillation(timeDis, shake, 40, 0.25f, QUARTER_PI);
        poseStack.mulPose(Axis.ZP.rotation(rotZ));
        //CameraAnimationHandler.INSTANCE.mix(0, (float) Math.toRadians(lastAdditionalXRot) * 0.5f, 0);
    }


    public float getUp() {
        return up * 0.25f;
    }


    public static void _debugReloadInstance(IRecoilCameraHandler recoilCameraHandler) {
        if (!GCR.IS_DEVELOPMENT) {
            return;
        }
        INSTANCE = recoilCameraHandler;
    }


    public static IRecoilCameraHandler getInstance() {
        return INSTANCE;
    }


    static {
        INSTANCE = new RecoilCameraHandler();
    }
}