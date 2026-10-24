package com.sheridan.gcr.client.model.modular;

import com.sheridan.gcr.client.model.Bone;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ILaserSightModel extends IModularModel {
    Bone getLaserPoseBone();

    LaserSighRenderer getRenderer();
}
