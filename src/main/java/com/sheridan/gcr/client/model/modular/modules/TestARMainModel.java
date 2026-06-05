package com.sheridan.gcr.client.model.modular.modules;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.client.model.MeshModelData;
import com.sheridan.gcr.client.model.modular.*;
import com.sheridan.gcr.client.model.modular.state.stateViewers.ARMainViewer;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.client.render.fx.bulletShell.BulletShellDisplay;
import com.sheridan.gcr.modularSys.modules.views.ARView;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TestARMainModel extends ArmHandlerModel<ARView> implements IBulletShellHandlerModel<ARView>, ISightModel, IGunModel {
    private final AssaultRifleBulletShellHandler bulletShellHandler;

    public TestARMainModel(MeshModelData root, BulletShellDisplay display, ARMainViewer viewer) {
        super(root, viewer, GCR.RL("m4a1_main"));
        this.bulletShellHandler = new AssaultRifleBulletShellHandler(this, display);
    }

    @Override
    public IBulletShellHandler<?> getBulletShellHandler() {
        return bulletShellHandler;
    }

    @Override
    public String getFarthestSightZName(ModuleRenderContext context) {
        return DEFAULT_FARTHEST_SIGHT_Z_NAME;
    }
}
