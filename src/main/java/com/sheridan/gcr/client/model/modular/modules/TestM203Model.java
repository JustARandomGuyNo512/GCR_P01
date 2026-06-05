package com.sheridan.gcr.client.model.modular.modules;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.client.model.MeshModelData;
import com.sheridan.gcr.client.model.modular.ArmHandlerModel;
import com.sheridan.gcr.client.model.modular.state.stateViewers.TestM203Viewer;
import com.sheridan.gcr.modularSys.modules.views.IAmmoSourceView;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TestM203Model extends ArmHandlerModel<IAmmoSourceView> {
    public TestM203Model(MeshModelData root, TestM203Viewer viewer) {
        super(root, viewer, GCR.RL(""));
    }
}
