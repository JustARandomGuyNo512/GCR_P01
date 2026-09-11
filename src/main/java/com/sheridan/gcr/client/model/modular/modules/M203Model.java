package com.sheridan.gcr.client.model.modular.modules;

import com.sheridan.gcr.client.model.MeshModelData;
import com.sheridan.gcr.client.model.modular.MuzzleFlashRenderer;
import com.sheridan.gcr.client.model.modular.state.stateViewers.M203Viewer;
import com.sheridan.gcr.modularSys.modules.views.IM203View;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class M203Model extends GrenadeLauncherModel<IM203View> {

    public M203Model(MeshModelData root, M203Viewer viewer, MuzzleFlashRenderer muzzleFlashRenderer) {
        super(root, viewer, muzzleFlashRenderer);
    }
}
