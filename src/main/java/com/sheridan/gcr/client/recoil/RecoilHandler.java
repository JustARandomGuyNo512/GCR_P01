package com.sheridan.gcr.client.recoil;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sheridan.gcr.client.model.modular.IGunModel;
import com.sheridan.gcr.client.recoil.test.DemoRecoilUpdater;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RecoilHandler {
    /**
     * 武器线程（100Hz update / onShoot）和客户端主线程（渲染、热重载）都会读它，
     * 所以必须是 volatile：热重载在主线程换实例后，武器线程要能立刻看到新对象。
     */
    private volatile IRecoilUpdater recoilUpdater;
    public static final RecoilHandler INSTANCE = new RecoilHandler();

    private RecoilHandler() {}

    static {
        //INSTANCE.setRecoilUpdater(new RecoilUpdater());
        INSTANCE.setRecoilUpdater(new DemoRecoilUpdater());
    }

    public void update(float delta) {
        if (recoilUpdater != null) {
            recoilUpdater.update(delta);
        }
    }

    public void onShoot(Player player) {
        if (recoilUpdater != null) {
            try {
                recoilUpdater.onShoot(player);
            } catch (Exception ignored) {}
        }
    }

    public void applyTransformPost(PoseStack poseStack, boolean aiming, float particleTicks, IGunModel model) {
        if (recoilUpdater != null) {
            recoilUpdater.applyTransformPost(poseStack, aiming, particleTicks, model);
        }
    }


    public synchronized void setRecoilUpdater(IRecoilUpdater recoilUpdater) {
        this.recoilUpdater = recoilUpdater;
    }


    public IRecoilUpdater getRecoilUpdater() {
        return recoilUpdater;
    };
}
