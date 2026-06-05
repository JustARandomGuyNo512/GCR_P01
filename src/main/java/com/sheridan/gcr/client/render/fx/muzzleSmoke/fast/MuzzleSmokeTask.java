package com.sheridan.gcr.client.render.fx.muzzleSmoke.fast;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MuzzleSmokeTask {
    public PoseStack.Pose pose;
    public long lastShoot;
    public FastMuzzleSmoke effect;
    private final int randomSeed;
    private final int light;

    public MuzzleSmokeTask(PoseStack.Pose pose, long lastShoot, FastMuzzleSmoke effect, int light)  {
        this.pose = pose;
        this.lastShoot = lastShoot;
        this.effect = effect;
        this.randomSeed = (int) (Math.random() * 1000);
        this.light = light;
    }

    public boolean handleRender(MultiBufferSource bufferSource) {
        boolean finished = isFinished();
        if (!finished) {
            effect.render(lastShoot, pose.copy(), bufferSource, randomSeed, light);
        }
        return finished;
    }

    private boolean isFinished() {
        return System.currentTimeMillis() - lastShoot > effect.length;
    }
}
