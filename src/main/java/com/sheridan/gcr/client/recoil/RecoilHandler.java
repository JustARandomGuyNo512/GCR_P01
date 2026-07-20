package com.sheridan.gcr.client.recoil;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sheridan.gcr.client.model.modular.IGunModel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RecoilHandler {
    private IRecoilUpdater recoilUpdater;
    public static final RecoilHandler INSTANCE = new RecoilHandler();

    private RecoilHandler() {}

    static {
        INSTANCE.setRecoilUpdater(new RecoilUpdater());
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
