package com.sheridan.gcr.client.model.modular.modules;

import com.sheridan.gcr.client.model.MeshModelData;
import com.sheridan.gcr.client.model.modular.MuzzleEntry;
import com.sheridan.gcr.client.model.modular.MuzzleFlashRenderer;
import com.sheridan.gcr.client.render.fx.muzzleFlash.MuzzleFlash;
import com.sheridan.gcr.client.render.fx.muzzleSmoke.fast.FastMuzzleSmoke;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MuzzleModel extends MuzzleFlashRendererModel {

    public MuzzleModel(MeshModelData root, float scale, MuzzleFlash muzzleFlash, float smokeScale, FastMuzzleSmoke muzzleSmoke, float flashLightIntensity) {
        super(root, new MuzzleFlashRenderer(
                new MuzzleEntry("no1", "MUZZLE_FLASH", null, scale, muzzleFlash, smokeScale, muzzleSmoke, flashLightIntensity)
        ));

    }



}