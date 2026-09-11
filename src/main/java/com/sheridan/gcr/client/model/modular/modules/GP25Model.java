package com.sheridan.gcr.client.model.modular.modules;

import com.sheridan.gcr.client.model.MeshModelData;
import com.sheridan.gcr.client.model.modular.MuzzleFlashRenderer;
import com.sheridan.gcr.client.model.modular.state.stateViewers.GP25Viewer;
import com.sheridan.gcr.modularSys.modules.views.IGP25View;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GP25Model extends GrenadeLauncherModel<IGP25View> {

    public GP25Model(MeshModelData root, GP25Viewer viewer, MuzzleFlashRenderer muzzleFlashRenderer) {
        super(root, viewer, muzzleFlashRenderer);
    }
}
