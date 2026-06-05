package com.sheridan.gcr.client.model.modular.modules;

import com.sheridan.gcr.client.model.MeshModelData;
import com.sheridan.gcr.client.model.modular.MuzzleEntry;
import com.sheridan.gcr.client.model.modular.MuzzleFlashRenderer;
import com.sheridan.gcr.client.render.fx.muzzleFlash.MuzzleFlash;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;


@OnlyIn(Dist.CLIENT)
public class BarrelModel extends MuzzleFlashRendererModel {

    public BarrelModel(MeshModelData root, float muzzleFlashScale, MuzzleFlash muzzleFlash) {
        super(root, new MuzzleFlashRenderer(
                new MuzzleEntry("no1", "MUZZLE_FLASH", "MUZZLE", muzzleFlashScale, muzzleFlash)
        ));
    }

}
