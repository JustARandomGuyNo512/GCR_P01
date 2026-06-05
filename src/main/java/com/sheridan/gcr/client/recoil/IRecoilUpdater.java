package com.sheridan.gcr.client.recoil;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector2f;

@OnlyIn(Dist.CLIENT)
public interface IRecoilUpdater {
    void update(double timeDist);
    void onShoot(Player player);
    void applyTransformPost(PoseStack poseStack, boolean aiming, float particleTicks);
    void applyTransformPre(PoseStack poseStack, boolean aiming, float particleTicks);
    float getGunKickPitch();
    float getGunKickYaw();
    void setRecoilData(RecoilData data);
    RecoilData getRecoilData();
    float getRecoilHeat();
    float getCamShakeZ();

    Vector2f getCameraSpeed();
}
