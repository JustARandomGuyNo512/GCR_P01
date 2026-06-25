package com.sheridan.gcr.client.recoil;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sheridan.gcr.Client;
import com.sheridan.gcr.GCR;
import com.sheridan.gcr.Utils;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector2f;

@OnlyIn(Dist.CLIENT)
public class RecoilCameraHandler implements IRecoilCameraHandler {
    private static IRecoilCameraHandler INSTANCE;
    private static final float QUARTER_PI = (float) Math.PI / 4;
    private float scale = 1.22f;

    // 记录当前相机由于后座力产生的“待回落位移量”
    private float pitchToRecovery = 0f;
    private float pitchTotal = 0f;
    private float yawToRecovery = 0f;

    // 记录上一帧玩家的原始旋转，用来准确捕获这一帧玩家的鼠标输入 (mouseDelta)
    private float lastPlayerXRot;
    private float lastPlayerYRot;

    private boolean isFirstFrame = true;

    /**
     * 判断当前轴是否满足开始进行后座力恢复的条件
     * @param speed 当前轴的后座力速度
     * @return 是否开始恢复
     */
    private boolean shouldStartRecovery(float speed) {
        float minTimeDist = Math.max(Client.WEAPON_STATUS.getFireInterval() + 0.005f, 0.1f);
        return Client.distFromLastShoot() > minTimeDist &&
                RecoilHandler.INSTANCE.getRecoilUpdater().getRecoilHeat() < 1f;
    }

    @Override
    public void update(float deltaTicks) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            isFirstFrame = true;
            return;
        }

        // 初始化或者断开连接后重置，防止跳帧
        if (isFirstFrame) {
            lastPlayerXRot = player.getXRot();
            lastPlayerYRot = player.getYRot();
            isFirstFrame = false;
            return;
        }

        // 1. 获取玩家这一帧本来的鼠标移动量 (Mouse Delta)
        float mouseDeltaX = player.getXRot() - lastPlayerXRot;
        float mouseDeltaY = player.getYRot() - lastPlayerYRot;

        IRecoilUpdater updater = RecoilHandler.INSTANCE.getRecoilUpdater();
        Vector2f recoilSpeed = (updater != null) ? updater.getCameraSpeed() : new Vector2f(0, 0);

        // 2. 处理后座力上升阶段
        float recoilPitchDelta = recoilSpeed.x * scale;
        float recoilYawDelta = recoilSpeed.y * scale;


        pitchToRecovery += recoilPitchDelta;
        pitchTotal += recoilPitchDelta;
        yawToRecovery += recoilYawDelta;

        // 3. 处理玩家的手动压枪抵消 (当玩家鼠标输入与后座力积攒量方向相反时)
        if (pitchToRecovery != 0 && Math.signum(mouseDeltaX) != Math.signum(pitchToRecovery)) {
            float oldPitchToRecovery = pitchToRecovery;
            pitchToRecovery += mouseDeltaX; // 符号相反，相加等于扣减
            // 防止压枪过度导致回落池反向变号
            if (Math.signum(pitchToRecovery) != Math.signum(oldPitchToRecovery)) {
                pitchToRecovery = 0f;
            }
        }

        if (yawToRecovery != 0 && Math.signum(mouseDeltaY) != Math.signum(yawToRecovery)) {
            float oldYawToRecovery = yawToRecovery;
            yawToRecovery += mouseDeltaY;
            if (Math.signum(yawToRecovery) != Math.signum(oldYawToRecovery)) {
                yawToRecovery = 0f;
            }
        }

        if ((pitchToRecovery != 0 || pitchTotal != 0) && shouldStartRecovery(recoilSpeed.x)) {
            float recoverySpeed = 0.2f * deltaTicks * 20f;
            recoverySpeed = Math.min(recoverySpeed, 1.0f);
            float pitchRecovery = pitchToRecovery * recoverySpeed;
            if (pitchToRecovery != 0) {
                pitchToRecovery -= pitchRecovery;
                recoilPitchDelta -= pitchRecovery;
            }
            if (pitchTotal != 0) {
                pitchTotal -= pitchTotal * recoverySpeed;
            }
        }

        if (yawToRecovery != 0 && shouldStartRecovery(recoilSpeed.y)) {
            float recoverySpeed = 0.2f * deltaTicks * 20f;
            float yawRecovery = yawToRecovery * Math.min(recoverySpeed, 1.0f);
            yawToRecovery -= yawRecovery;
            recoilYawDelta -= yawRecovery;
        }

        player.setXRot(player.getXRot() + recoilPitchDelta);
        player.setYRot(player.getYRot() + recoilYawDelta);

        lastPlayerXRot = player.getXRot();
        lastPlayerYRot = player.getYRot();
    }

    @Override
    public void clear() {
        pitchToRecovery = 0f;
        yawToRecovery = 0f;
        isFirstFrame = true;
    }

    @Override
    public void onBobbingView(PoseStack poseStack, float partialTicks, IGun gun) {
        IRecoilUpdater updater = RecoilHandler.INSTANCE.getRecoilUpdater();
        if (updater != null) {
            float timeDis = updater.distFromLastShoot();
            float currentShake = updater.getCamShakeZ();
            float rotZ = (float) Utils.dampedOscillation(timeDis, currentShake, 40, 0.25f, QUARTER_PI);
            poseStack.mulPose(Axis.ZP.rotation(rotZ));
        }
    }

    @Override
    public float getUp() {
        return pitchToRecovery;
    }

    public static void _debugReloadInstance(IRecoilCameraHandler recoilCameraHandler) {
        if (!GCR.IS_DEVELOPMENT) return;
        INSTANCE = recoilCameraHandler;
    }

    public static IRecoilCameraHandler getInstance() { return INSTANCE; }

    static { INSTANCE = new RecoilCameraHandler(); }
}