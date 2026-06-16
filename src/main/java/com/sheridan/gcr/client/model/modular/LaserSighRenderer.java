package com.sheridan.gcr.client.model.modular;

import com.sheridan.gcr.client.model.Bone;
import com.sheridan.gcr.client.render.FirstPersonRenderContext;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.client.render.fx.LaserEffectRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LaserSighRenderer {
    private final int color;
    private final ILaserSightModel laserSightModel;

    public LaserSighRenderer(ILaserSightModel laserSightModel, int color) {
        this.laserSightModel = laserSightModel;
        this.color = color;
    }


    public void renderFirstPerson(FirstPersonRenderContext context) {
        //TODO:
        // 1.render long laser ray(if blocked, render short ray)
        // 2.addWorldLaserEffect
        Bone laserPoseBone = laserSightModel.getLaserPoseBone();
        LaserEffectRenderer.recordEffectCall(
                context.currentRenderNode().id,
                laserPoseBone.renderStatus.pose,
                context);
    }

    public void renderGeneric(ModuleRenderContext context) {
        //TODO: render short ray
    }

    private void addWorldLaserEffect() {

    }
}
