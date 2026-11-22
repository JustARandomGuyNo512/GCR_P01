package com.sheridan.gcr.client.recoil;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sheridan.gcr.client.model.modular.IGunModel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector2f;

@OnlyIn(Dist.CLIENT)
public interface IRecoilUpdater {
    void update(double timeDist);
    void onShoot(Player player);
    void applyImpulse(float impulseZ, float pitch, float yaw, float globalPitch, float globalYaw, float shakeRoll);
    void applyTransformPost(PoseStack poseStack, boolean aiming, float particleTicks, IGunModel model);
    float getGunKickPitch();
    float getGunKickYaw();
    void setRecoilData(RecoilData data);
    RecoilData getRecoilData();
    float getRecoilHeat();
    float getCamShakeZ();
    float distFromLastShoot();

    Vector2f getCameraSpeed();

    /**
     * 调试用：返回可以按行显示在屏幕上的状态文本，{@code "\n"} 分隔。
     *
     * <p>默认没有调试信息；具体的 updater 可以覆盖。放在接口上（而不是只放在 demo 实现类上）
     * 是为了热重载后依然能通过接口调用到子类加载器里的新实现。</p>
     */
    default String getDebugInfo() {
        return "";
    }
}
